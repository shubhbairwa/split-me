package com.shubh.splitme.ui.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import java.io.ByteArrayOutputStream
import kotlin.math.min

/** Profile pictures are stored as a centre-cropped square JPEG of this size. */
const val PROFILE_PHOTO_SIZE = 256

/**
 * Reads the picked image, crops the centre square, scales it to [PROFILE_PHOTO_SIZE] and returns
 * JPEG bytes (usually 15–40 KB, small enough to keep in Firestore). Call off the main thread.
 */
fun prepareProfilePhoto(context: Context, uri: Uri): ByteArray {
    val source = decode(context, uri)
    val side = min(source.width, source.height)
    val square = Bitmap.createBitmap(source, (source.width - side) / 2, (source.height - side) / 2, side, side)
    val scaled = if (side > PROFILE_PHOTO_SIZE) Bitmap.createScaledBitmap(square, PROFILE_PHOTO_SIZE, PROFILE_PHOTO_SIZE, true) else square
    return ByteArrayOutputStream().use { out ->
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
        out.toByteArray()
    }
}

private fun decode(context: Context, uri: Uri): Bitmap =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        // ImageDecoder applies the photo's rotation and can downsample while decoding.
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val shortSide = min(info.size.width, info.size.height)
            if (shortSide > PROFILE_PHOTO_SIZE * 2) {
                val factor = shortSide / (PROFILE_PHOTO_SIZE * 2)
                decoder.setTargetSize(info.size.width / factor, info.size.height / factor)
            }
        }
    } else {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (min(bounds.outWidth, bounds.outHeight) / (sample * 2) >= PROFILE_PHOTO_SIZE) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("Couldn't read the image")
    }
