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

    fun get(): Flow<File> = callbackFlow {
        if (localFile.exists())
            trySend(localFile)
        else
            root.child(blobPath)
                .getFile(localFile)
                .addOnSuccessListener {
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