package solutions.s4y.pcm

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaExtractor
import android.os.Build
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PCMAssetWavProvider(context: Context, asset: String): IPCMProvider {

    override val floats: FloatArray by lazy {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
            throw UnsupportedOperationException("Requires API 28")
        val fd: AssetFileDescriptor = context.assets.openFd(asset)
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
        var floatsSize = 0
        for (sampleBuffer in sampleBuffers) {
            floatsSize += sampleBuffer.capacity() / 2
        }
        val floats = FloatArray(floatsSize)
        var shortN = 0
        for (i in sampleBuffers.indices) {
            val sampleBuffer = sampleBuffers[i]
            val shortBuffer = sampleBuffer.asFloatBuffer()
            val shortBufferSize = shortBuffer.capacity()
            shortBuffer[floats, shortN, shortBufferSize]
            shortN += shortBufferSize
        }
        floats
    }
}