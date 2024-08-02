package solutions.s4y.pcm

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaExtractor
import android.os.Build
import java.nio.ByteBuffer
import java.nio.ByteOrder

// TODO: deduplicate code
class PCMAssetWavProvider(context: Context, asset: String): IPCMProvider {

    override val shorts: ShortArray by lazy {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
            throw UnsupportedOperationException("Requires API 28")
        val fd: AssetFileDescriptor = context.assets.openFd(asset)
        val extractor = MediaExtractor()
        extractor.setDataSource(fd)
        extractor.selectTrack(0)

        val sampleBuffers: MutableList<ByteBuffer> = ArrayList()
        var samplesCount = 0
        // requires API 28
        val samplesSize = extractor.sampleSize.toInt()
        while (true) {
            val sampleBuffer = ByteBuffer.allocate(samplesSize)
            sampleBuffer.order(ByteOrder.LITTLE_ENDIAN)
            val n = extractor.readSampleData(sampleBuffer, 0)
            if (n < samplesSize) {
                val sampleBufferLast = ByteBuffer.allocate(n)
                sampleBufferLast.put(sampleBuffer.array(), 0, n)
                sampleBuffers.add(sampleBufferLast)
                break
            } else {
                sampleBuffers.add(sampleBuffer)
            }
            samplesCount += n / 2
            extractor.advance()
        }

        // summary capacities of all sampleBuffers
        val shorts = ShortArray(samplesCount)
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

    override val floats: FloatArray by lazy {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
            throw UnsupportedOperationException("Requires API 28")
        val fd: AssetFileDescriptor = context.assets.openFd(asset)
        val extractor = MediaExtractor()
        extractor.setDataSource(fd)
        extractor.selectTrack(0)

        val sampleBuffers: MutableList<ByteBuffer> = ArrayList()
        var samplesCount = 0
        // requires API 28
        val samplesSize = extractor.sampleSize.toInt()
        while (true) {
            val sampleBuffer = ByteBuffer.allocate(samplesSize)
            sampleBuffer.order(ByteOrder.LITTLE_ENDIAN)
            val n = extractor.readSampleData(sampleBuffer, 0)
            if (n < samplesSize) {
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
        val floats = FloatArray(samplesCount)
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