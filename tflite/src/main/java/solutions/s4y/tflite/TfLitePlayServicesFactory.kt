/*
08-30 13:58:44.195 16547 16604 W System.err: android.os.RemoteException: Error loading TFLite GPU delegate module
08-30 13:58:44.195 16547 16604 W System.err: 	at m.py.a(:com.google.android.gms.policy_tflite_dynamite_dynamite@242327809@242327804042.653218464.653218464:3)
08-30 13:58:44.195 16547 16604 W System.err: 	at com.google.android.gms.tflite.dynamite.TfLiteDynamiteLoaderImpl.b(:com.google.android.gms.policy_tflite_dynamite_dynamite@242327809@242327804042.653218464.653218464:151)
08-30 13:58:44.195 16547 16604 W System.err: 	at com.google.android.gms.tflite.dynamite.TfLiteDynamiteLoaderImpl.getInternalNativeInitializationHandleWithParams(:com.google.android.gms.policy_tflite_dynamite_dynamite@242327809@242327804042.653218464.653218464:7)
08-30 13:58:44.195 16547 16604 W System.err: 	at m.oj.x(:com.google.android.gms.policy_tflite_dynamite_dynamite@242327809@242327804042.653218464.653218464:50)
08-30 13:58:44.195 16547 16604 W System.err: 	at m.au.onTransact(:com.google.android.gms.policy_tflite_dynamite_dynamite@242327809@242327804042.653218464.653218464:21)
08-30 13:58:44.195 16547 16604 W System.err: 	at android.os.Binder.transact(Binder.java:1345)
08-30 13:58:44.195 16547 16604 W System.err: 	at com.google.android.gms.internal.tflite.zza.zzb(com.google.android.gms:play-services-tflite-impl@@16.1.0:2)
08-30 13:58:44.196 16547 16604 W System.err: 	at com.google.android.gms.tflite.dynamite.zza.zzf(com.google.android.gms:play-services-tflite-impl@@16.1.0:4)
08-30 13:58:44.196 16547 16604 W System.err: 	at com.google.android.gms.tflite.dynamite.internal.zzk.zzc(com.google.android.gms:play-services-tflite-impl@@16.1.0:11)
08-30 13:58:44.196 16547 16604 W System.err: 	at com.google.android.gms.tflite.dynamite.internal.zzi.zza(com.google.android.gms:play-services-tflite-impl@@16.1.0:9)
08-30 13:58:44.196 16547 16604 W System.err: 	at com.google.android.gms.tflite.dynamite.internal.zzf.then(Unknown Source:6)
08-30 13:58:44.196 16547 16604 W System.err: 	at com.google.android.gms.tasks.zzo.run(com.google.android.gms:play-services-tasks@@18.0.2:1)
08-30 13:58:44.196 16547 16604 W System.err: 	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1145)
08-30 13:58:44.196 16547 16604 W System.err: 	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:644)
08-30 13:58:44.196 16547 16604 W System.err: 	at java.lang.Thread.run(Thread.java:1012)
08-30 13:58:44.196 16547 16604 W System.err: Caused by: m.kx: No acceptable module com.google.android.gms.tflite_gpu_dynamite found. Local version is 0 and remote version is 0.
08-30 13:58:44.196 16547 16604 W System.err: 	at m.lb.c(:com.google.android.gms.policy_tflite_dynamite_dynamite@242327809@242327804042.653218464.653218464:154)
08-30 13:58:44.196 16547 16604 W System.err: 	at com.google.android.gms.tflite.dynamite.TfLiteDynamiteLoaderImpl.b(:com.google.android.gms.policy_tflite_dynamite_dynamite@242327809@242327804042.653218464.653218464:33)

package solutions.s4y.tflite

import android.content.Context
import android.util.Log
import com.google.android.gms.tflite.client.TfLiteInitializationOptions
import com.google.android.gms.tflite.gpu.support.TfLiteGpu
import com.google.android.gms.tflite.java.TfLite
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.gpu.GpuDelegateFactory
import solutions.s4y.tflite.base.TfLiteFactory
import solutions.s4y.tflite.base.TfLiteInterpreter
import java.nio.ByteBuffer
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
class TfLitePlayServicesFactory() : TfLiteFactory() {
    override suspend fun initialize(context: Context) = suspendCoroutine { continuation ->
        Log.d(TAG, "initialize TfLite...")
        TfLite.initialize(
            context,
            TfLiteInitializationOptions.builder()
                .setEnableGpuDelegateSupport(true)
                .build()
        )
            .addOnSuccessListener {
                continuation.resume(Unit)
                Log.d(TAG, "initialize TfLite done")
            }
            .addOnFailureListener {
                continuation.resumeWithException(it)
                //Log.e(TAG, "Initialize TfLite error", it)
            }
    }

    private suspend fun useGpu(context: Context): Boolean = suspendCoroutine { continuation ->
        TfLiteGpu.isGpuDelegateAvailable(context)
            .addOnSuccessListener {
                continuation.resume(it)
                Log.d(TAG, "GPU delegate available: $it")
            }
            .addOnFailureListener {
                continuation.resumeWithException(it)
                Log.e(TAG, "GPU delegate availability error", it)
            }
    }

    override suspend fun createInterpreter(
        context: Context,
        inferenceContext: CoroutineContext,
        modelBuffer: ByteBuffer,
        onClose: () -> Unit
    ): TfLiteInterpreter {

        val interpreterOption =
            InterpreterApi.Options()
                .setRuntime(InterpreterApi.Options.TfLiteRuntime.FROM_SYSTEM_ONLY)

        if (useGpu(context)) {
            interpreterOption
                .addDelegateFactory(GpuDelegateFactory())
            Log.d(TAG, "GPU delegate added")
        } else {
            interpreterOption
                .setNumThreads(Runtime.getRuntime().availableProcessors())
            Log.d(
                TAG,
                "Interpter option set to CPU (${Runtime.getRuntime().availableProcessors()})"
            )
        }

        val interpreter = InterpreterApi.create(
            modelBuffer,
            interpreterOption
        )
        Log.d(TAG, "Interpreter created")
        return TfLitePlayServicesInterpreter(interpreter, inferenceContext, onClose)
    }

    companion object {
        private const val TAG = "TfLitePlayServicesFactory"
    }
}
 */