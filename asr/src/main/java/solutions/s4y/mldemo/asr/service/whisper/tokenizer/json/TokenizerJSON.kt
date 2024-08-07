package solutions.s4y.mldemo.asr.service.whisper.tokenizer.json

import com.google.gson.annotations.SerializedName

class TokenizerJSON {
    data class AddedToken(
        @SerializedName("id") val id: Int,
        @SerializedName("content") val content: String,
        @SerializedName("single_word") val singleWord: Boolean,
        @SerializedName("lstrip") val lstrip: Boolean,
        @SerializedName("rstrip") val rstrip: Boolean,
        @SerializedName("normalized") val normalized: Boolean,
        @SerializedName("special") val special: Boolean
    )

    data class Model(
        @SerializedName("vocab") val vocab: Map<String, Int>,
    )

    data class Tokenizer(
        @SerializedName("version") val version: String,
        @SerializedName("truncation") val truncation: String?,
        @SerializedName("padding") val padding: String?,
        @SerializedName("added_tokens") val addedTokens: List<AddedToken>,
        @SerializedName("model") val model: Model,
    )
}