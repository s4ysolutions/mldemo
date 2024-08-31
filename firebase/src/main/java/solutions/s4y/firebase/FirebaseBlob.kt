package solutions.s4y.firebase

import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class FirebaseBlob(private val blobPath: String, private val localFile: File) {
    companion object {
        private const val TAG = "FirebaseBlob"
        private val root: StorageReference by lazy {
            FirebaseStorage.getInstance().reference
        }
    }

    private var _isLocal: Boolean? = null

    val isLocal: Boolean? get() = _isLocal

    val flow: Flow<File> = flow {
        val file = get()
        emit(file)
    }

    internal suspend fun getMetaData(): StorageMetadata = suspendCoroutine { cont ->
        root.child(blobPath)
            .metadata
            .addOnSuccessListener {
                cont.resume(it)
            }
            .addOnFailureListener { ex ->
                cont.resumeWithException(ex)
            }
    }

    private suspend fun download(): File = suspendCoroutine { cont ->
        val ts = System.currentTimeMillis()
        Log.d(TAG, "download: $blobPath")
        if (localFile.parentFile?.exists() != true)
            localFile.parentFile?.mkdirs()
        root.child(blobPath)
            .getFile(localFile)
            .addOnSuccessListener {
                _isLocal = false
                Log.d(TAG, "download: $blobPath success in ${System.currentTimeMillis() - ts} ms")
                cont.resume(localFile)
            }
            .addOnFailureListener { ex ->
                Log.w(TAG, "download: $blobPath", ex)
                cont.resumeWithException(ex)
            }
    }

    suspend fun get(): File {
        if (localFile.exists()) {
            Log.d(TAG, "get: $localFile exists")
            try {
                val ts = System.currentTimeMillis()
                val metadata = getMetaData()
                Log.d(TAG, "get: $blobPath metadata in ${System.currentTimeMillis() - ts} ms")
                if (localFile.lastModified() >= metadata.updatedTimeMillis) {
                    _isLocal = true
                    return localFile
                }
            }catch (ex: Exception) {
                Log.w(TAG, "could not get metadata for: $blobPath, fallback to the existing $localFile", ex)
                _isLocal = true
                return localFile
            }
        }
        return download()
    }
}