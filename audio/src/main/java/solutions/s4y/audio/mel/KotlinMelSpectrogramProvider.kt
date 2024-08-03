package solutions.s4y.audio.mel

import android.util.Log
import java.util.Arrays

class KotlinMelSpectrogramProvider: IMelSpectrogramProvider {
    companion object {
        private const val TAG = "WhisperUtil"
        const val WHISPER_N_FFT = 400
        const val WHISPER_N_MEL = 80
        const val WHISPER_HOP_LENGTH = 160
        private fun dft(`in`: FloatArray, out: FloatArray) {
            val N = `in`.size
            for (k in 0 until N) {
                var re = 0f
                var im = 0f
                for (n in 0 until N) {
                    val angle = (2 * Math.PI * k * n / N).toFloat()
                    re += (`in`[n] * Math.cos(angle.toDouble())).toFloat()
                    im -= (`in`[n] * Math.sin(angle.toDouble())).toFloat()
                }
                out[k * 2] = re
                out[k * 2 + 1] = im
            }
        }
    }

    private val mel = WhisperMel()
    private val filters = WhisperFilter()

    private class WhisperMel {
        var nLen = 0
        var nMel = 0
        lateinit var data: FloatArray
    }

    private class WhisperFilter {
        var nMel = 0
        var nFft = 0
        // TODO: https://github.com/farmaker47/Talk_and_execute/blob/whisper_locally/app/src/main/java/com/example/talkandexecute/utils/WhisperUtil.kt
        var data: FloatArray = FloatArray(30000).also {
            Arrays.fill(it, 1.0f)
        }
    }

    // If you want to implement log_mel_spectrogram in kotlin
    override fun getMelSpectrogram(samples: FloatArray, nSamples: Int): FloatArray {
        val nThreads = Runtime.getRuntime().availableProcessors()
        val fftSize = WHISPER_N_FFT
        val fftStep = WHISPER_HOP_LENGTH
        mel.nMel = WHISPER_N_MEL
        mel.nLen = nSamples / fftStep
        mel.data = FloatArray(mel.nMel * mel.nLen)
        val hann = FloatArray(fftSize)
        for (i in 0 until fftSize) {
            hann[i] = (0.5 * (1.0 - Math.cos(2.0 * Math.PI * i / fftSize))).toFloat()
        }
        val nFft = 1 + fftSize / 2

/////////////// UNCOMMENT below block to use multithreaded mel calculation /////////////////////////
        // Calculate mel values using multiple threads
        val workers: MutableList<Thread> = ArrayList()
        for (iw in 0 until nThreads) {
            val thread = Thread {

                // Inside the thread, ith will have the same value as iw (first value is 0)
                Log.d(TAG, "Thread $iw started.")
                val fftIn = FloatArray(fftSize)
                Arrays.fill(fftIn, 0.0f)
                val fftOut = FloatArray(fftSize * 2)
                var i = iw
                while (i < mel.nLen) {

/////////////// END of Block ///////////////////////////////////////////////////////////////////////

/////////////// COMMENT below block to use multithreaded mel calculation ///////////////////////////
//        float[] fftIn = new float[fftSize];
//        Arrays.fill(fftIn, 0.0f);
//        float[] fftOut = new float[fftSize * 2];
//
//        for (int i = 0; i < mel.nLen; i++) {
/////////////// END of Block ///////////////////////////////////////////////////////////////////////
                    val offset = i * fftStep

                    // apply Hanning window
                    for (j in 0 until fftSize) {
                        if (offset + j < nSamples) {
                            fftIn[j] = hann[j] * samples[offset + j]
                        } else {
                            fftIn[j] = 0.0f
                        }
                    }

                    // FFT -> mag^2
                    fft(fftIn, fftOut)
                    for (j in 0 until fftSize) {
                        fftOut[j] =
                            fftOut[2 * j] * fftOut[2 * j] + fftOut[2 * j + 1] * fftOut[2 * j + 1]
                    }
                    for (j in 1 until fftSize / 2) {
                        fftOut[j] += fftOut[fftSize - j]
                    }

                    // mel spectrogram
                    for (j in 0 until mel.nMel) {
                        var sum = 0.0
                        for (k in 0 until nFft) {
                            sum += (fftOut[k] * filters.data[j * nFft + k]).toDouble()
                        }
                        if (sum < 1e-10) {
                            sum = 1e-10
                        }
                        sum = Math.log10(sum)
                        mel.data[j * mel.nLen + i] = sum.toFloat()
                    }
                    i += nThreads
                }
            }
            workers.add(thread)
            thread.start()
        }

        // Wait for all threads to finish
        for (worker in workers) {
            try {
                worker.join()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
        /////////////// END of Block ///////////////////////////////////////////////////////////////////////

        // clamping and normalization
        var mmax = -1e20
        for (i in 0 until mel.nMel * mel.nLen) {
            if (mel.data[i] > mmax) {
                mmax = mel.data[i].toDouble()
            }
        }
        mmax -= 8.0
        for (i in 0 until mel.nMel * mel.nLen) {
            if (mel.data[i] < mmax) {
                mel.data[i] = mmax.toFloat()
            }
            mel.data[i] = ((mel.data[i] + 4.0) / 4.0).toFloat()
        }
        return mel.data
    }

    // Cooley-Tukey FFT
    private fun fft(input: FloatArray, output: FloatArray) {
        val N = input.size
        if (N == 1) {
            output[0] = input[0]
            output[1] = 0f
            return
        }
        if (N % 2 == 1) {
            dft(input, output)
            return
        }
        val even = FloatArray(N / 2)
        val odd = FloatArray(N / 2)
        for (i in 0 until N) {
            if (i % 2 == 0) {
                even[i / 2] = input[i]
            } else {
                odd[i / 2] = input[i]
            }
        }
        val evenFft = FloatArray(N)
        val oddFft = FloatArray(N)
        fft(even, evenFft)
        fft(odd, oddFft)
        for (k in 0 until N / 2) {
            val theta = (2 * Math.PI * k / N).toFloat()
            val re = Math.cos(theta.toDouble()).toFloat()
            val im = -Math.sin(theta.toDouble()).toFloat()
            val reOdd = oddFft[2 * k]
            val imOdd = oddFft[2 * k + 1]
            output[2 * k] = evenFft[2 * k] + re * reOdd - im * imOdd
            output[2 * k + 1] = evenFft[2 * k + 1] + re * imOdd + im * reOdd
            output[2 * (k + N / 2)] = evenFft[2 * k] - re * reOdd + im * imOdd
            output[2 * (k + N / 2) + 1] = evenFft[2 * k + 1] - re * imOdd - im * reOdd
        }
    }
}