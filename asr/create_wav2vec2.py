import sys
import os
import torch
from torch import Tensor
from torch.utils.mobile_optimizer import optimize_for_mobile
import torchaudio
from torchaudio.models.wav2vec2.utils.import_huggingface import import_huggingface_model
from transformers import Wav2Vec2ForCTC, Wav2Vec2CTCTokenizer

torch.backends.quantized.engine = 'qnnpack'

if len(sys.argv) <= 1:
    print("Please provide the model name as a first argument")
    print("  english: facebook/wav2vec2-base-960h")
    print("  arabic most trending: AndrewMcDowell/wav2vec2-xls-r-300m-arabic")
    print("  arabic most likes: jonatasgrosman/wav2vec2-large-xlsr-53-arabic")
    sys.exit(1)

# Sanity check
if len(sys.argv) <= 2:
    print("To check the model please provide the wav file as a second argument")
    print(f"  english: src/androidTest/assets/OSR_us_000_0030_16k.wav")
    print(f"  arabic:  src/androidTest/assets/adam/3-14.wav")
    wav_file_path = False
else:
    wav_file_path = sys.argv[2]

# Wav2vec2 model emits sequences of probability (logits) distributions over the characters
# The following class adds steps to decode the transcript (best path)
class SpeechRecognizer(torch.nn.Module):
    def __init__(self, model, labels, blank):
        super().__init__()
        self.model = model
        self.labels = labels
        self.blank = blank

    def forward(self, waveforms: Tensor) -> str:
        """Given a single channel speech data, return transcription.

        Args:
            waveforms (Tensor): Speech tensor. Shape `[1, num_frames]`.

        Returns:
            str: The resulting transcript
        """
        logits, _ = self.model(waveforms)  # [batch, num_seq, num_label]
        predicted_ids = torch.argmax(logits[0], dim=-1)  # [num_seq,]
        predicted_ids = torch.unique_consecutive(predicted_ids, dim=-1)
        tokens: list[str] = []
        for i in predicted_ids:
          if i != self.blank:
            ch = self.labels[i]
            tokens.append(ch)
        text = "".join(tokens)
        return text

# Get the model name from arguments
model_name = sys.argv[1]

# Load Wav2Vec2 pretrained model from Hugging Face Hub
model = Wav2Vec2ForCTC.from_pretrained(model_name)
# Convert the model to torchaudio format, which supports TorchScript.
model = import_huggingface_model(model)
# Remove weight normalization which is not supported by quantization.
model.encoder.transformer.pos_conv_embed.__prepare_scriptable__()
model = model.eval()
# Get Vocabulary and Blank token
tokenizer = Wav2Vec2CTCTokenizer.from_pretrained(model_name)
vocab = tokenizer.get_vocab()
vocabT = {index: symbol for symbol, index in vocab.items()}
blank = vocab[tokenizer.pad_token]
# Attach decoder
model = SpeechRecognizer(model, vocabT, blank)
# Apply quantization / script / optimize for mobile
quantized_model = torch.quantization.quantize_dynamic(
    model, qconfig_spec={torch.nn.Linear}, dtype=torch.qint8)
scripted_model = torch.jit.script(quantized_model)
optimized_model = optimize_for_mobile(scripted_model)

if wav_file_path:
  print(f"Sanity check with file: {wav_file_path}")
  waveform , _ = torchaudio.load(wav_file_path)
  print('Result:', optimized_model(waveform))
else:
  print("Sanity check skipped")

if sys.argv[-1] == "test":
  model_path= f"src/androidTest/assets/models/{model_name}.ptl"
else:
  model_path = "src/main/assets/models/wav2vec2.ptl"

dir_path = os.path.dirname(model_path)
os.makedirs(dir_path, exist_ok=True)
optimized_model._save_for_lite_interpreter(model_path)
print(f"Model saved to {model_path}")

if sys.argv[-1] != "test":
    print("Add 'test' as last argument to save the model to src/androidTest/assets/models")
