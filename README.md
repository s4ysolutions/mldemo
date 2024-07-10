### 2. Prepare the Model

To install PyTorch 1.10.0, torchaudio 0.10.0 and the Hugging Face transformers, you can do something like this:

```
conda create -n wav2vec2 python=3.8.5
conda activate wav2vec2
pip install torch torchaudio transformers
```

Now with PyTorch 1.10.0 and torchaudio 0.10.0 installed, run the following commands on a Terminal:

```
python create_wav2vec2.py
```
This will create the PyTorch mobile interpreter model file `wav2vec2.ptl`. Copy it to the pytorch module:
```

mkdir -p pytorch/src/main/assets
cp wav2vec2.ptl pytorch/src/main/assets
```
