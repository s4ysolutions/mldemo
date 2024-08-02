package solutions.s4y.firebase

import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File

class FirebaseBlob(private val blobPath: String, private val localFile: File) {
    companion object {
        private val root: StorageReference by lazy {
            FirebaseStorage.getInstance().reference
        }
    }

    private var _isLocal: Boolean? = null

    val isLocal: Boolean? get() = _isLocal

    fun get(): Flow<File> = callbackFlow {
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
}