package solutions.s4y.mldemo.asr.whisper.tokenizer.fixtures

import com.google.gson.Gson
import java.nio.file.Files
import java.nio.file.Paths

interface Fixtures {
    companion object {
        private val classLoader = javaClass.classLoader!!

        val tokens1_1: IntArray by lazy {
            val resource = classLoader.getResource("adam/1-1-tokens.json")
            val path = Paths.get(resource.toURI())
            val content = String(Files.readAllBytes(path))

            val gson = Gson()
            val array0: Array<IntArray> = gson.fromJson(content, Array<IntArray>::class.java)
            array0[0]
        }

        val transcription1_1: String by lazy {
            val resource = classLoader.getResource("adam/1-1-transcription.txt")
            val path = Paths.get(resource.toURI())
            String(Files.readAllBytes(path))
        }

        val tokenizerJson: String by lazy {
            val resource = classLoader.getResource("tokenizer.json")
            val path = Paths.get(resource.toURI())
            String(Files.readAllBytes(path))
        }
    }

    val tokens1_1: IntArray get() = Companion.tokens1_1
    val transcription1_1: String get() = Companion.transcription1_1
    val tokenizerJson: String get() = Companion.tokenizerJson
}
