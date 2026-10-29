package com.shubh.splitme.data.firebase

import android.util.Base64
import com.shubh.splitme.domain.repository.ProfilePhotoRepository
import com.shubh.splitme.domain.repository.UploadedPhoto

/**
 * Keeps the profile picture inside the member's Firestore document as a "data:image/jpeg;base64,…"
 * value in photoUrl, so no Firebase Storage bucket (which needs the Blaze plan) is required.
 * The JPEG is small (see PROFILE_PHOTO_SIZE), far below Firestore's 1 MB document limit.
 */
class FirestoreProfilePhotoRepository : ProfilePhotoRepository {

    override suspend fun upload(uid: String, jpeg: ByteArray): UploadedPhoto {
        require(jpeg.size <= MAX_PHOTO_BYTES) { "Photo is too large (${jpeg.size} bytes)" }
        val dataUri = DATA_URI_PREFIX + Base64.encodeToString(jpeg, Base64.NO_WRAP)
        return UploadedPhoto(url = dataUri, path = null)
    }

    /** Nothing to delete: the picture lives in the member document and is replaced there. */
    override suspend fun delete(path: String) = Unit

    companion object {
        const val DATA_URI_PREFIX = "data:image/jpeg;base64,"

        /** Keeps the member document well under Firestore's 1 MB limit after base64 (+33%). */
        const val MAX_PHOTO_BYTES = 300 * 1024
    }
}
