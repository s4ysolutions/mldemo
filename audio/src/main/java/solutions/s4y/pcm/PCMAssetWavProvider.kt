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

    // TODO: rewrite, it is not suspected to work
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