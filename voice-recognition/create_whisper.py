import os
import tensorflow as tf
from transformers import WhisperProcessor, TFWhisperForConditionalGeneration, TFForceTokensLogitsProcessor
import tensorflow_io as tfio
from typing import List
import numpy as np


# Patching methods of class TFForceTokensLogitsProcessor(TFLogitsProcessor):

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


def wav_audio(wav_file_path):
    waveform, sample_rate = tf.audio.decode_wav(tf.io.read_file(wav_file_path))
    if sample_rate != 16000:
        print(f"sample rate is {sample_rate}, resampling...")
        waveform = tfio.audio.resample(waveform, rate_in=sample_rate, rate_out=16000)
        print("ok")
    audio = waveform[:, 0]
    return audio


_dir = os.path.dirname(os.path.abspath(__file__))
model_name = 'openai/whisper-base'
tflite_model_path = os.path.join(_dir, f"src/androidTest/assets/models/{model_name}.tflite")
saved_model_dir = 'tf_whisper_saved'
skip_convert = False


class GenerateModel(tf.Module):
    def __init__(self, model, forced_decoder_ids):
        super(GenerateModel, self).__init__()
        self.model = model
        self.forced_decoder_ids = forced_decoder_ids

    @tf.function(
        # shouldn't need static batch size, but throws exception without it (needs to be fixed)
        input_signature=[
            tf.TensorSpec((1, 80, 3000), tf.float32, name="input_features"),
        ],
    )
    def serving(self, input_features):
        outputs = self.model.generate(
            input_features,
            forced_decoder_ids=self.forced_decoder_ids,
            #max_new_tokens=223,  # change as needed
            return_dict_in_generate=True,
        )
        return {"sequences": outputs["sequences"]}


processor = WhisperProcessor.from_pretrained(model_name)
forced_decoder_ids = processor.get_decoder_prompt_ids(language="ar", task="transcribe")

if not skip_convert:
    model = TFWhisperForConditionalGeneration.from_pretrained(model_name)
    generate_model = GenerateModel(model=model, forced_decoder_ids=forced_decoder_ids)
    tf.saved_model.save(generate_model, saved_model_dir, signatures={"serving_default": generate_model.serving})

    # Convert the model
    converter = tf.lite.TFLiteConverter.from_saved_model(saved_model_dir)
    converter.target_spec.supported_ops = [
        tf.lite.OpsSet.TFLITE_BUILTINS,  # enable TensorFlow Lite ops.
        tf.lite.OpsSet.SELECT_TF_OPS  # enable TensorFlow ops.
    ]
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    converter.inference_input_type = tf.float32
    converter.inference_output_type = tf.float32
    tflite_model = converter.convert()

    # Save the model
    dir_path = os.path.dirname(tflite_model_path)
    os.makedirs(dir_path, exist_ok=True)
    with open(tflite_model_path, 'wb') as f:
        f.write(tflite_model)

# model check
_wav_3_14 = os.path.join(_dir, 'src/androidTest/assets/adam/3-14.wav')
_wav_1_1 = os.path.join(_dir, 'src/androidTest/assets/adam/1-1.wav')
audio = wav_audio(_wav_3_14)
inputs = processor(audio, sampling_rate=16000, return_tensors="tf")
input_features = inputs.input_features

#model = TFWhisperForConditionalGeneration.from_pretrained(model_name)
#generated_ids = model.generate(input_features, forced_decoder_ids = forced_decoder_ids)
#model = GenerateModel(model=model, forced_decoder_ids=forced_decoder_ids)
#generated_ids = model.generate(input_features)

interpreter = tf.lite.Interpreter(tflite_model_path)
tflite_generate = interpreter.get_signature_runner()
output = tflite_generate(input_features=input_features)
generated_ids = output["sequences"]
transcription = processor.batch_decode(generated_ids, skip_special_tokens=True)[0]
print(transcription)
