package solutions.s4y.mldemo.asr.whisper

import com.google.gson.Gson
import solutions.s4y.mldemo.asr.whisper.tokenizer.json.TokenizerJSON
import java.io.File
import java.io.InputStreamReader

class WhisperTokenizer(jsonString: String) {
    constructor(jsonFile: File) : this(InputStreamReader(jsonFile.inputStream()))
    constructor(jsonStream: InputStreamReader) : this(jsonStream.use { it.readText() })

    private val specialTokens: Map<Int, String>
    private val unicodeTokens: Map<Int, String>

    private val unicodeToBytes: Map<Char, Int> = run {
        val charRanges = listOf('!'..'~', '¡'..'¬', '®'..'ÿ').flatten()
        val byteList = charRanges.map { it.code }.toMutableList()
        val charList = charRanges.toMutableList()

        var extraCharIndex = 0
        for (byte in 0..255) {
            if (byte !in byteList) {
                byteList.add(byte)
                charList.add(Char(256 + extraCharIndex++))
            }
        }
        charList.zip(byteList).toMap()
    }

    private fun tokens2string(token: String): String {
        val bytes =
            token.map { unicodeToBytes[it]?.toByte() ?: ' '.code.toByte() }.toByteArray()
        return String(bytes, Charsets.UTF_8)
    }

    init {
        val gson = Gson()
        val tokenizerJson = gson.fromJson(jsonString, TokenizerJSON.Tokenizer::class.java)
        specialTokens = mutableMapOf()
        unicodeTokens = mutableMapOf()
        tokenizerJson.addedTokens
            .forEach {it ->
                if (it.special) {
                    specialTokens[it.id] = it.content
                } else {
                    unicodeTokens[it.id] = it.content
                }
            }
        tokenizerJson.model.vocab
            .forEach { (content, id) ->
                unicodeTokens[id] = content
            }
    }

    fun decode(tokens: IntArray, skipSpecial: Boolean = true): String {
        val text = StringBuilder()
        val currentSubText = StringBuilder()
        tokens.forEach { token ->
            val specialToken = specialTokens[token]
            if (specialToken == null) {
                val tokenText = unicodeTokens[token]
                if (tokenText != null) {
                    currentSubText.append(tokenText)
                }
            } else {
                if (currentSubText.isNotEmpty()) {
                    text.append(tokens2string(currentSubText.toString()))
                    currentSubText.clear()
                }
                if (!skipSpecial) {
                    text.append(specialToken)
                }
            }
        }
        if (currentSubText.isNotEmpty()) {
            text.append(tokens2string(currentSubText.toString()))
        }
        return text.toString()
    }
}