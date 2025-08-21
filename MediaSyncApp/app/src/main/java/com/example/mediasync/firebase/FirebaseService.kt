package com.example.mediasync.firebase

import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.File

object FirebaseService {
	private val storage by lazy { Firebase.storage }
	private val firestore by lazy { Firebase.firestore }

	fun ensureAuth() {
		val auth = Firebase.auth
		if (auth.currentUser == null) auth.signInAnonymously()
	}

	fun uploadMediaFile(file: File, contentType: String? = null, onComplete: (Boolean, String?) -> Unit) {
		ensureAuth()
		val ref = storage.reference.child("media/${file.name}")
		val uploadTask = if (contentType != null) {
			ref.putFile(android.net.Uri.fromFile(file), com.google.firebase.storage.StorageMetadata.Builder().setContentType(contentType).build())
		} else {
			ref.putFile(android.net.Uri.fromFile(file))
		}
		uploadTask
			.addOnFailureListener { onComplete(false, it.message) }
			.addOnSuccessListener { taskSnapshot ->
				ref.downloadUrl.addOnSuccessListener { uri ->
					val data = hashMapOf(
						"name" to file.name,
						"size" to file.length(),
						"downloadUrl" to uri.toString(),
						"uploadedAt" to Timestamp.now()
					)
					firestore.collection("media").add(data)
						.addOnSuccessListener { onComplete(true, null) }
						.addOnFailureListener { err -> onComplete(false, err.message) }
				}
			}
		)
	}

	fun uploadContactNames(names: List<String>, onComplete: (Boolean, String?) -> Unit) {
		ensureAuth()
		if (names.isEmpty()) { onComplete(true, null); return }
		val batch = firestore.batch()
		names.forEach { name ->
			val doc = firestore.collection("contacts").document()
			batch.set(doc, mapOf("name" to name, "uploadedAt" to Timestamp.now()))
		}
		batch.commit()
			.addOnSuccessListener { onComplete(true, null) }
			.addOnFailureListener { onComplete(false, it.message) }
	}
}