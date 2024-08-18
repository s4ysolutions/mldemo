package solutions.s4y.audio.pcm

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaExtractor
import android.os.Build
import java.nio.ByteBuffer
import java.nio.ByteOrder

// TODO: deduplicate code
class PCMAssetWavProvider(context: Context, asset: String): PCMMediaExtractorWavProvider(
    getMediaExtractor(context, asset)
) {
    companion object {
        fun getMediaExtractor(context: Context, asset: String): MediaExtractor {
            val fd: AssetFileDescriptor = context.assets.openFd(asset)
            val extractor = MediaExtractor()
            extractor.setDataSource(fd)
            return extractor
        }
    }
}