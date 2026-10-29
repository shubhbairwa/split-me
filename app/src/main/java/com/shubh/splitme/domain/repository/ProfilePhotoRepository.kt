package com.shubh.splitme.domain.repository

/** Where profile pictures are stored. */
interface ProfilePhotoRepository {
    /** Stores a JPEG for [uid]; returns the value to save as the member's photoUrl, and a file path if any. */
    suspend fun upload(uid: String, jpeg: ByteArray): UploadedPhoto

    /** Deletes a previously stored picture file. Missing files are ignored. */
    suspend fun delete(path: String)
}

/** [path] is null when the picture isn't a separate file (e.g. it's stored in the member document). */
data class UploadedPhoto(val url: String, val path: String?)
