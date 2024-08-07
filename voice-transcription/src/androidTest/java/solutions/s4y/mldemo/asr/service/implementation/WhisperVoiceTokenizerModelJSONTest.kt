package solutions.s4y.mldemo.asr.service.implementation

/*
import org.junit.Assert.assertArrayEquals
import org.junit.Rule
import org.junit.Test
import solutions.s4y.voice_transcription.rules.WhisperRule

class WhisperVoiceTokenizerModelJSONTest {
    @get:Rule
    val whisperRule = WhisperRule()

    @Test
    fun whisper_shouldTokenizeMel()  {
        // Act
        val tokens = whisperRule.transcriber.runInference(whisperRule.testMel)
        // Assert
        assertArrayEquals(whisperRule.testTokens, tokens)
    }
    @Test
    fun whisper_shouldTokenize() = runBlocking {
        // Arrange
        val pipeline = whisperRule.pcmFeed.flow.map { waveForms ->
            val mel = whisperRule.melSpectrogramTransformer.getMelSpectrogram(waveForms)
            whisperRule.transcriber.getTokens(mel)
        }
        val pcmProvider = PCMAssetWavProvider(whisperRule.context, "adam/1-1.wav")

        // Act
        val job = async {
            var tokens: List<Int>? = null
            pipeline.collect{ result ->
                tokens = result
            }
            tokens
        }
        delay(1)
        val inBuf = ShortArray(16000 * 30).also { inBuf ->
            pcmProvider.shorts.copyInto(inBuf, 0, 0, min(16000 * 30, pcmProvider.shorts.size))
        }
        whisperRule.pcmFeed.add(inBuf)
        //whisperRule.pcmFeed.close()
        // Assert
        delay(10000)
        val result = job.await()
        println(result)
    }
}
 */
