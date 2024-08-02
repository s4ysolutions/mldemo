package solutions.s4y.firebase

import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class FirebaseBlob(private val blobPath: String, private val localFile: File) {
    companion object {
        private val root: StorageReference by lazy {
            FirebaseStorage.getInstance().reference
        }
    }

    private var _isLocal: Boolean? = null

    val isLocal: Boolean? get() = _isLocal

    val flow: Flow<File> = callbackFlow {
        if (localFile.exists()) {
            _isLocal = true
            trySendBlocking(localFile)
            close()
        } else
            root.child(blobPath)
                .getFile(localFile)
                .addOnSuccessListener {
                    _isLocal = false
                    // trySend ?
                    trySendBlocking(localFile)
                    close()
                }
                .addOnFailureListener { ex ->
                    close(ex)
                }
        awaitClose()
    }

    suspend fun get(): File = suspendCoroutine { cont ->
        if (localFile.exists()) {
            _isLocal = true
            cont.resume(localFile)
        } else
            root.child(blobPath)
                .getFile(localFile)
                .addOnSuccessListener {
                    _isLocal = false
                    cont.resume(localFile)
                }
                .addOnFailureListener { ex ->
                    cont.resumeWithException(ex)
                }
    }
}