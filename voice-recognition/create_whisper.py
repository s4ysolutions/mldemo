import os
import tensorflow as tf
from transformers import WhisperProcessor, TFWhisperForConditionalGeneration, TFForceTokensLogitsProcessor
import tensorflow_io as tfio
from typing import List
import numpy as np


# Patching methods of class TFForceTokensLogitsProcessor(TFLogitsProcessor):
# TFForceTokensLogitsProcessor has a bug which causes lite model to crach
# to fix it, the 2 methods are overriden and replaced
# https://github.com/huggingface/transformers/issues/19691#issuecomment-1791869884
def my__init__(self, force_token_map: List[List[int]]):
    force_token_map = dict(force_token_map)
    # Converts the dictionary of format {index: token} containing the tokens to be forced to an array, where the
    # index of the array corresponds to the index of the token to be forced, for XLA compatibility.
    # Indexes without forced tokens will have an negative value.
    force_token_array = np.ones((max(force_token_map.keys()) + 1), dtype=np.int32) * -1
    for index, token in force_token_map.items():
        if token is not None:
            force_token_array[index] = token
    self.force_token_array = tf.convert_to_tensor(force_token_array, dtype=tf.int32)


def my__call__(self, input_ids: tf.Tensor, scores: tf.Tensor, cur_len: int) -> tf.Tensor:
    def _force_token(generation_idx):
        batch_size = scores.shape[0]
        current_token = self.force_token_array[generation_idx]

        # Original code below generates NaN values when the model is exported to tflite
        # it just needs to be a negative number so that the forced token's value of 0 is the largest
        # so it will get chosen
        #new_scores = tf.ones_like(scores, dtype=scores.dtype) * -float("inf")
        new_scores = tf.ones_like(scores, dtype=scores.dtype) * -float(1)
        indices = tf.stack((tf.range(batch_size), tf.tile([current_token], [batch_size])), axis=1)
        updates = tf.zeros((batch_size,), dtype=scores.dtype)
        new_scores = tf.tensor_scatter_nd_update(new_scores, indices, updates)
        return new_scores

    scores = tf.cond(
        tf.greater_equal(cur_len, tf.shape(self.force_token_array)[0]),
        # If the current length is geq than the length of force_token_array, the processor does nothing.
        lambda: tf.identity(scores),
        # Otherwise, it may force a certain token.
        lambda: tf.cond(
            tf.greater_equal(self.force_token_array[cur_len], 0),
            # Only valid (positive) tokens are forced
            lambda: _force_token(cur_len),
            # Otherwise, the processor does nothing.
            lambda: scores,
        ),
    )
    return scores


TFForceTokensLogitsProcessor.__init__ = my__init__
TFForceTokensLogitsProcessor.__call__ = my__call__

# A helper function to extract array of raw PCM floats from wav file
def wav_audio(wav_file_path):
    waveform, sample_rate = tf.audio.decode_wav(tf.io.read_file(wav_file_path))
    if sample_rate != 16000:
        print(f"sample rate is {sample_rate}, resampling...")
        waveform = tfio.audio.resample(waveform, rate_in=sample_rate, rate_out=16000)
        print("ok")
    audio = waveform[:, 0]
    return audio

# huggingface model name
model_name = 'openai/whisper-base'
# temporary catalog for saving the huggingface model localy
saved_model_dir = 'tf_whisper_saved'
# name of file to contain TF Lite model
_dir = os.path.dirname(os.path.abspath(__file__))
tflite_model_path = os.path.join(_dir, f"src/androidTest/assets/models/{model_name}.tflite")
# True for testing purpose
skip_convert = False

# A wrapper around hugging face model to be used by Lite interpetator
# will have the only function `serving` to be called by the exernal code
class GenerateModel(tf.Module):
    def __init__(self, model, forced_decoder_ids):
        super(GenerateModel, self).__init__()
        # actual Lite model to be used for generation
        self.model = model
        # input data (alongside with audio) for every request to recognize the voice
        # language=ar task=transcribe (not translate)
        self.forced_decoder_ids = forced_decoder_ids

    # signature of the only function of the cla
    @tf.function(
        # shouldn't need static batch size, but throws exception without it (needs to be fixed)
        input_signature=[
            tf.TensorSpec((1, 80, 3000), tf.float32, name="input_features"),
        ],
    )
    def serving(self, input_features):
        # it has only on input, namely tensor with audio data (mel spectrogram)
        # see the @tf.function decorator above
        # ...and pasess the data to the lite model to get the array of tokens
        outputs = self.model.generate(
            input_features,
            forced_decoder_ids=self.forced_decoder_ids,
            #max_new_tokens=223,  # change as needed
            return_dict_in_generate=True,
        )
        # it could return just array of tokes from `outputs["sequences"]`
        # but i was a chiken to change the borrowed code and returns
        # a dictionary with one key-value
        return {"sequences": outputs["sequences"]}

# huggingface utility to prepare audio data for input and 
# decode output tokens to redable string
processor = WhisperProcessor.from_pretrained(model_name)

# this the inpute to model's `generate` method
# will be used later
forced_decoder_ids = processor.get_decoder_prompt_ids(language="ar", task="transcribe")

if not skip_convert:
    # convert huggingface Tensorflow model to Tensorflow lite

    # Loriginal whisper model itself has `forward` method which recognize just one tocken form 
    # audio data stream. Huggingface adds a wrapper around it with the method `generate`
    # to recognize the 30sec audio data
    model = TFWhisperForConditionalGeneration.from_pretrained(model_name)
    # wrap the model with our class with `serving` method
    generate_model = GenerateModel(model=model, forced_decoder_ids=forced_decoder_ids)
    # and save this (still TensorFlow) model locally (converter can convert only such saved models)
    tf.saved_model.save(generate_model, saved_model_dir, signatures={"serving_default": generate_model.serving})

    # Convert the model
    converter = tf.lite.TFLiteConverter.from_saved_model(saved_model_dir)
    # Magic
    converter.target_spec.supported_ops = [
        tf.lite.OpsSet.TFLITE_BUILTINS,  # enable TensorFlow Lite ops.
        tf.lite.OpsSet.SELECT_TF_OPS  # enable TensorFlow ops.
    ]
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    converter.inference_input_type = tf.float32
    converter.inference_output_type = tf.float32
    # And now we have tflite model
    tflite_model = converter.convert()

    # Save the tflite model to the file
    dir_path = os.path.dirname(tflite_model_path)
    os.makedirs(dir_path, exist_ok=True)
    with open(tflite_model_path, 'wb') as f:
        f.write(tflite_model)

# At this point we already have tflite model and it is good idea to check it works
# model check
_wav_3_14 = os.path.join(_dir, 'src/androidTest/assets/adam/3-14.wav')
_wav_1_1 = os.path.join(_dir, 'src/androidTest/assets/adam/1-1.wav')
# read a "waveform" - an array of the floats forming a voice raw data
audio = wav_audio(_wav_3_14)
# we need to conver wave form to "mel spectrogram"
# namely to turn the 1-dimenstion array of PCM values 
# to n-dimension array of the set of frequneces for very small duration of 
# audio record which being summed restore PCM value at that time
# (Fourier transform if such term is easier)
inputs = processor(audio, sampling_rate=16000, return_tensors="tf")
input_features = inputs.input_features

# this commented out code for testing the original models:
# just call their `generate` method
#model = TFWhisperForConditionalGeneration.from_pretrained(model_name)
#generated_ids = model.generate(input_features, forced_decoder_ids = forced_decoder_ids)
#model = GenerateModel(model=model, forced_decoder_ids=forced_decoder_ids)
#generated_ids = model.generate(input_features)

# our purpose is to check the Lite model
interpreter = tf.lite.Interpreter(tflite_model_path)
# This magic call give us the `serving` method of the wrapper class
# Unless the had the only method we would had to use more complex approach
# to find the needed serving. Google for get_signature_runner if interested
tflite_generate = interpreter.get_signature_runner()
# a workhorse: calls the serving which in turn will call `generate` method 
# of the original model
output = tflite_generate(input_features=input_features)
generated_ids = output["sequences"]
# now we have an array of `tokens` - integer values
# and need to convert them to the string
# use huggingface utility class to do so
transcription = processor.batch_decode(generated_ids, skip_special_tokens=True)[0]
print(transcription)

# it is important to note: only model will be available in the application
# which use the model but `processor` lives only in this python script, so the application
# must implement its own way to conver PCM audio data to log-mel spectrogram
# and map the tokens to characters. 
# For the latter task it is good idea to preload the `vocab` from hugging face
# and either to pass it down to the application or may be better
# add the decoding to the `serving` method of the wrapper and make it to return
# the readable string (see [create_wav2vec2.py](create_wav2vec2.py) for this
# appoach)
