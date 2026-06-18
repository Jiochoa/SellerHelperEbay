package com.example.sellerhelperebay.data.images

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * Owns all photo files. Every picked or captured image is re-encoded into
 * filesDir/photos/<entryId>/<uuid>.jpg, downscaled so its longest edge is at most
 * [MAX_DIMENSION]px with EXIF rotation baked in. The one stored file serves both
 * the UI (Coil) and the Gemini upload.
 */
class ImageStore(private val context: Context) {

    /** Copies the content [uri] into the entry's photo dir; returns the relative path. */
    fun importUri(entryId: Long, uri: Uri): String {
        val bitmap = decodeDownscaled(uri)
            ?: throw IOException("Could not decode image: $uri")
        return save(entryId, bitmap)
    }

    /** Fresh temp file + content URI for ActivityResultContracts.TakePicture. */
    fun newCameraTempUri(): Pair<Uri, File> {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.jpg")
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        return uri to file
    }

    /** Imports a completed camera capture (from [newCameraTempUri]) and deletes the temp file. */
    fun importCameraCapture(entryId: Long, tempFile: File): String {
        try {
            val bitmap = decodeDownscaled(Uri.fromFile(tempFile))
                ?: throw IOException("Could not decode camera capture")
            return save(entryId, bitmap)
        } finally {
            tempFile.delete()
        }
    }

    fun fileFor(relativePath: String): File = File(context.filesDir, relativePath)

    fun loadBitmap(relativePath: String): Bitmap? =
        BitmapFactory.decodeFile(fileFor(relativePath).absolutePath)

    fun delete(relativePath: String) {
        fileFor(relativePath).delete()
    }

    fun deleteEntryDir(entryId: Long) {
        File(context.filesDir, "photos/$entryId").deleteRecursively()
    }

    private fun save(entryId: Long, bitmap: Bitmap): String {
        val relativePath = "photos/$entryId/${UUID.randomUUID()}.jpg"
        val file = fileFor(relativePath)
        file.parentFile?.mkdirs()
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }
        return relativePath
    }

    private fun decodeDownscaled(uri: Uri): Bitmap? {
        val resolver = context.contentResolver

        // First pass: read dimensions only. In inJustDecodeBounds mode decodeStream
        // always returns null and only populates the bounds, so we check outWidth/
        // outHeight here rather than the (always-null) return value.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        runCatching {
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(maxOf(bounds.outWidth, bounds.outHeight))
        }
        val sampled = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: return null

        val rotation = resolver.openInputStream(uri)?.use { stream ->
            when (ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f

        return scaleAndRotate(sampled, rotation)
    }

    private fun sampleSize(longestEdge: Int): Int {
        var sample = 1
        var edge = longestEdge
        while (edge / 2 >= MAX_DIMENSION) {
            sample *= 2
            edge /= 2
        }
        return sample
    }

    private fun scaleAndRotate(bitmap: Bitmap, rotationDegrees: Float): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        val scale = if (longest > MAX_DIMENSION) MAX_DIMENSION.toFloat() / longest else 1f
        if (scale == 1f && rotationDegrees == 0f) return bitmap

        val matrix = Matrix().apply {
            if (scale != 1f) postScale(scale, scale)
            if (rotationDegrees != 0f) postRotate(rotationDegrees)
        }
        val result = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (result != bitmap) bitmap.recycle()
        return result
    }

    companion object {
        const val MAX_DIMENSION = 1536
        const val JPEG_QUALITY = 85
    }
}
