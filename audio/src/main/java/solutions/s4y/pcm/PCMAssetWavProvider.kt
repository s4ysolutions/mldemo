package solutions.s4y.pcm

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaExtractor
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PCMAssetWavProvider(context: Context, asset: String): IPCMProvider {

    override val shorts: ShortArray by lazy {
        val fd: AssetFileDescriptor = context.getAssets().openFd(asset)
        val extractor = MediaExtractor()
        extractor.setDataSource(fd)
        extractor.selectTrack(0)

        val sampleBuffers: MutableList<ByteBuffer> = ArrayList()
        // requires API 28
        val sampleSize = extractor.sampleSize.toInt()
        while (true) {
            val sampleBuffer = ByteBuffer.allocate(sampleSize)
            sampleBuffer.order(ByteOrder.LITTLE_ENDIAN)
            val n = extractor.readSampleData(sampleBuffer, 0)
            if (n < sampleSize) {
                val sampleBufferLast = ByteBuffer.allocate(n)
                sampleBufferLast.put(sampleBuffer.array(), 0, n)
                sampleBuffers.add(sampleBufferLast)
                break
            } else {
                sampleBuffers.add(sampleBuffer)
            }
            extractor.advance()
        }

        // summary capacities of all sampleBuffers
        var shortsSize = 0
        for (sampleBuffer in sampleBuffers) {
            shortsSize += sampleBuffer.capacity() / 2
        }
        val shorts = ShortArray(shortsSize)
        var shortN = 0
        for (i in sampleBuffers.indices) {
            val sampleBuffer = sampleBuffers[i]
            val shortBuffer = sampleBuffer.asShortBuffer()
            val shortBufferSize = shortBuffer.capacity()
            shortBuffer[shorts, shortN, shortBufferSize]
            shortN += shortBufferSize
        }
        shorts
    }
}