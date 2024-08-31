@file:Suppress("SameParameterValue", "unused")

package solutions.s4y.mldemo.asr.service.gcs

import solutions.s4y.mldemo.asr.service.whisper.EncoderDecoder.Models

private fun gcsPath(
    project: String,
    group: String,
    model: String,
    version: String,
    language: String,
    blob: String,
): String {
    return "ml/$project/$group/$model/$version/$language/$blob"
}

private fun gcsPath(
    project: String,
    group: String,
    model: String,
    version: String,
    blob: String,
): String {
    return "ml/$project/$group/$model/$version/$blob"
}

private fun gcsPath(
    project: String,
    group: String,
    version: String,
    blob: String,
): String {
    return "ml/$project/$group/$version/$blob"
}

fun gcsModelPath(
    project: String,
    group: String,
    model: String,
    version: String,
    language: String
): String {
    return "ml/$project/$group/$model/$version/$language/model.tflite"
}

fun gcsHuggingfaceWhisperModelPath(author: String, size: String, language: String): String {
    return gcsModelPath(
        "whisper",
        "models",
        "huggingface/$author/whisper-$size",
        "dev",
        language
    )
}

fun gcsSergenesWhisperModelPath(sub: String? = null): String {
    return if (sub == null)
        "ml/whisper/models/sergenes/whisper-tiny.tflite"
    else
        "ml/whisper/models/sergenes/whisper-tiny-${sub}.tflite"
}

fun gcsFeaturesExtractorPath(): String {
    return gcsPath(
        "whisper",
        "features-extractor",
        "dev",
        "features-extractor.tflite"
    )
}

fun gcsTokenizerPath(): String {
    return gcsPath(
        "whisper",
        "tokenizer",
        "dev",
        "tokenizer.json"
    )
}

fun gcsEncoderDecoderPath(model: Models): String = when (model) {
    Models.HuggingfaceTinyAr -> gcsHuggingfaceWhisperModelPath("openai", "tiny", "ar")
    Models.HuggingfaceTinyEn -> gcsHuggingfaceWhisperModelPath("openai", "tiny", "en")
    Models.HuggingfaceBaseAr -> gcsHuggingfaceWhisperModelPath("openai", "base", "ar")
    Models.HuggingfaceBaseEn -> gcsHuggingfaceWhisperModelPath("openai", "base", "en")
    Models.Sergenes -> gcsSergenesWhisperModelPath()
    Models.SergenesEn -> gcsSergenesWhisperModelPath("en")
}
