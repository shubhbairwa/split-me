package com.shubh.splitme.ui.common

import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * What to hand Coil's AsyncImage for a member's photoUrl. Photos saved in Firestore are
 * "data:image/…;base64,…" values, which Coil 2 can't load directly, so those are decoded to bytes;
 * anything else (an https or content:// address) is passed through unchanged.
 */
@Composable
fun rememberPhotoModel(photoUrl: String?): Any? = remember(photoUrl) { photoModel(photoUrl) }

fun photoModel(photoUrl: String?): Any? {
    if (photoUrl.isNullOrBlank()) return null
    if (!photoUrl.startsWith("data:")) return photoUrl
    return runCatching { Base64.decode(photoUrl.substringAfter(','), Base64.DEFAULT) }.getOrNull()
}
