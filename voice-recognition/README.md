### 1. Prepare the Wav2vec2 Model

To install PyTorch 1.10.0, torchaudio 0.10.0 and the Hugging Face transformers, you can do something like this:

either with [venv](https://docs.python.org/3/library/venv.html): 
```bash
python3 -m venv .venv
 . ./.venv/bin/activate
 pip install torch torchaudio transformers PySoundFile
```

or with conda:
```bash
conda create -n wav2vec2 python=3.8.5
conda activate wav2vec2
pip install torch torchaudio transformers PySoundFile
```

Now with PyTorch 1.10.0 and torchaudio 0.10.0 installed, run the following commands on a Terminal:

```bash
cd [path to create_wav2vec2-base-960h-postprocessed.py & scent_of_a_woman_future.wav]
python create_wav2vec2-base-960h-postprocessed.py
```
This will create the PyTorch mobile interpreter model file `wav2vec2.ptl`. Copy it to the pytorch module:
```

mkdir -p pytorch/src/main/assets
cp wav2vec2.ptl pytorch/src/main/assets
```
