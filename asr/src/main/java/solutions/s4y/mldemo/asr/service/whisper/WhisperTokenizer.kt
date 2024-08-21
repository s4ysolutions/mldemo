package solutions.s4y.mldemo.asr.service.whisper

import android.content.Context
import com.google.gson.Gson
import solutions.s4y.firebase.FirebaseBlob
import solutions.s4y.mldemo.asr.service.gcs.gcsFeaturesExtractorPath
import solutions.s4y.mldemo.asr.service.gcs.gcsTokenizerPath
import solutions.s4y.mldemo.asr.service.whisper.tokenizer.json.TokenizerJSON
import java.io.File
import java.io.InputStreamReader

class WhisperTokenizer private constructor(jsonString: String) {
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
            .forEach {
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

    // TODO: should have a variant with a list of words
    fun decode(
        tokens: IntArray,
        skipSpecial: Boolean = true,
        compactSameSpecialTokens: Boolean = true,
    ): String {
        val text = StringBuilder()
        val currentSubText = StringBuilder()
        var prevSpecialToken: Int = -1
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
                    if (compactSameSpecialTokens) {
                        if (prevSpecialToken == token) {
                            return@forEach
                        }
                        prevSpecialToken = token
                    }
                    text.append(specialToken)
                }
            }
        }
        if (currentSubText.isNotEmpty()) {
            text.append(tokens2string(currentSubText.toString()))
        }
        return text.toString()
    }

    companion object {
        //private const val JSON_PATH = "tokenizer.json"
        suspend fun loadFromGCS( context: Context): WhisperTokenizer {
            val gcsPath = gcsTokenizerPath()
            val jsonFile = File(context.filesDir, gcsPath)
            FirebaseBlob(gcsPath, jsonFile).get()
            InputStreamReader(jsonFile.inputStream()).use {
                return WhisperTokenizer(it.readText())
            }
        }
    }
}