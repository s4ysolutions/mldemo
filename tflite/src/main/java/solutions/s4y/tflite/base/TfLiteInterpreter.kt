package solutions.s4y.tflite.base

import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.CoroutineContext
import kotlin.math.min

abstract class TfLiteInterpreter(
    private val inferenceContext: CoroutineContext,
    inputs: List<Pair<IntArray, DataType>>,
    output: Pair<IntArray, DataType>,
    private val onClose: () -> Unit,
) : Closeable {
    private val createThreadId = Thread.currentThread().id
    private var lastIntOutput: IntArray? = null
    private var lastFloatOutput: FloatArray? = null

    protected val inputBuffers: List<ByteBuffer> = inputs.map { (shape, dataType) ->
        createByteBuffer(shape, dataType)
    }

    protected val outputBuffer: ByteBuffer = createByteBuffer(output.first, output.second)

    val intOutput: IntArray
        get() = lastIntOutput ?: run {
            outputBuffer.rewind()
            val buffer = outputBuffer.asIntBuffer()
            val array = IntArray(buffer.capacity()) {
                buffer.get()
            }
            lastIntOutput = array
            array
        }

    val floatOutput: FloatArray
        get() = lastFloatOutput ?: run {
            outputBuffer.rewind()
            val buffer = outputBuffer.asFloatBuffer()
            val array = FloatArray(buffer.capacity()) {
                buffer.get()
            }
            lastFloatOutput = array
            array
        }

    protected abstract fun runInference(input: FloatArray)

    abstract val lastInferenceDuration: Long

    suspend fun run(input: FloatArray) = withContext(inferenceContext) {
        assert(Thread.currentThread().id == createThreadId) {
            "TFLiteInterpreter should be used from the same thread it was created"
        }
        lastFloatOutput = null
        lastIntOutput = null
        val buffer = inputBuffers[0].asFloatBuffer()
        val size = min(buffer.capacity(), input.size)
        for (i in 0..<size) {
            buffer.put(input[i])
        }
        for (i in size until buffer.capacity()) {
            buffer.put(0f)
        }
        runInference(input)
    }

    override fun close() {
        onClose()
    }


    companion object {
        fun createByteBuffer(shape: IntArray, dataType: DataType): ByteBuffer {
            val itemSize = when (dataType) {
                DataType.FLOAT32 -> Float.SIZE_BYTES
                DataType.INT32 -> Int.SIZE_BYTES
                DataType.INT8 -> Byte.SIZE_BYTES
                else -> throw IllegalArgumentException("Unsupported data type: $dataType")
            }
            return ByteBuffer.allocateDirect(shape.reduce { acc, i -> acc * i } * itemSize).apply {
                order(ByteOrder.nativeOrder())
            }
        }
    }
}