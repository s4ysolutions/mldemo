@file:Suppress("SameParameterValue", "unused")

package solutions.s4y.mldemo.asr.service.gcs

import solutions.s4y.mldemo.asr.service.AsrModels

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

fun gcsEncoderDecoderPath(model: AsrModels): String = when (model) {
    AsrModels.HuggingfaceTinyAr -> gcsHuggingfaceWhisperModelPath("openai", "tiny", "ar")
    AsrModels.HuggingfaceTinyEn -> gcsHuggingfaceWhisperModelPath("openai", "tiny", "en")
    AsrModels.HuggingfaceBaseAr -> gcsHuggingfaceWhisperModelPath("openai", "base", "ar")
    AsrModels.HuggingfaceBaseEn -> gcsHuggingfaceWhisperModelPath("openai", "base", "en")
    AsrModels.Sergenes -> gcsSergenesWhisperModelPath()
    AsrModels.SergenesEn -> gcsSergenesWhisperModelPath("en")
    AsrModels.AndroidFreeForm -> ""
}
