package solutions.s4y.audio.pcm

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaExtractor
import android.os.Build
import java.nio.ByteBuffer
import java.nio.ByteOrder

// TODO: deduplicate code
open class PCMMediaExtractorWavProvider(extractor: MediaExtractor): IPCMProvider {

    override val shorts: ShortArray by lazy {
        extractor.selectTrack(0)

        val shortsArrays: MutableList<ShortArray> = ArrayList()
        var shortsCount = 0
        // requires API 28
        val bytesSize = extractor.sampleSize.toInt()
        val bytesBuffer = ByteBuffer.allocate(bytesSize)
        bytesBuffer.order(ByteOrder.LITTLE_ENDIAN)
        //val  bytesBuffer = ByteArray(bytesSize)
        while (true) {
            val n = extractor.readSampleData(bytesBuffer, 0)
            if (n == 0)
                break

            val sn = n/2
            val shortsArray = ShortArray(sn)
            bytesBuffer.asShortBuffer()[shortsArray, 0, sn]
            shortsArrays.add(shortsArray)

            shortsCount += sn

            if (n<bytesSize) {
                break
            }
            extractor.advance()
        }

        val result = ShortArray(shortsCount)

        var currentIndex = 0
        for (array in shortsArrays) {
            array.copyInto(result, currentIndex)
            currentIndex += array.size
        }

        result
    }

    override val floats: FloatArray by lazy {
        val array = FloatArray(shorts.size)
        val k = Short.MAX_VALUE.toFloat()
        for (i in shorts.indices) {
            array[i] = shorts[i] / k
        }
        array
    }
}