package solutions.s4y.mldemo.asr.service

enum class AsrModels {
    AndroidFreeForm,
    HuggingfaceTinyAr,
    HuggingfaceTinyEn,
    HuggingfaceBaseAr,
    HuggingfaceBaseEn,
    SergenesEn,
    Sergenes;

    val isAndroid get() = this == AndroidFreeForm
    val isHuggingface get() = !isAndroid
}