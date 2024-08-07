package solutions.s4y.mldemo.asr.service

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import solutions.s4y.mldemo.asr.service.rules.WhisperRule

class ASRServiceTest {
    @get:Rule
    val whisper = WhisperRule()

    @Ignore("Not clear what this test is supposed to do")
    @Test
    fun flow_shouldEmit(): Unit = runBlocking {
        // Arrange
        whisper.waveFormsAccumulator.batch = 16000 * 30
        val service = ASRService(
            whisper.waveFormsAccumulator,
            whisper.melSpectrogramTransformer,
            whisper.model,
            whisper.tokenizer
        )
        val job = async {
            val decoded = mutableListOf<String>()
            service.decodingFlow
                .toList(decoded)
            decoded.toList()
        }
        delay(10)
        // Act
        // do not need to close the feed, bec
        whisper.waveFormsAccumulator.add(whisper.testPCM)
        whisper.waveFormsAccumulator.close()
        val decoded: List<String> = job.await()
        // Assert
    }
}