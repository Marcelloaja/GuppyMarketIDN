package com.stiki.marcelloilham.guppymarketidn.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.Size
import android.widget.ImageView
import androidx.annotation.DrawableRes
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Safely decodes image URIs and video thumbnails so SecurityException, FileNotFoundException,
 * hardware-bitmap issues, or unsupported codec frames never crash or leave blank views.
 */
object SafeImageLoader {

    fun loadUriSafely(
        context: Context,
        imageView: ImageView,
        uriString: String?,
        @DrawableRes fallbackResId: Int,
        maxDimPx: Int = 512
    ): Boolean {
        val bitmap = decodeBitmapSafely(context, uriString, maxDimPx)
        return if (bitmap != null) {
            imageView.setImageBitmap(bitmap)
            true
        } else {
            imageView.setImageResource(fallbackResId)
            false
        }
    }

    fun loadWithFallbackSafely(
        context: Context,
        imageView: ImageView,
        primaryUri: String?,
        secondaryDataUrl: String?,
        @DrawableRes fallbackResId: Int,
        maxDimPx: Int = 512
    ): Boolean {
        val bitmap = decodeBitmapSafely(context, primaryUri, maxDimPx)
            ?: decodeBitmapSafely(context, secondaryDataUrl, maxDimPx)
        return if (bitmap != null) {
            imageView.setImageBitmap(bitmap)
            true
        } else {
            imageView.setImageResource(fallbackResId)
            false
        }
    }

    fun encodeBitmapToDataUrl(
        bitmap: Bitmap,
        maxDimPx: Int = 420,
        initialQuality: Int = 62,
        maxBase64Length: Int = 150_000
    ): String {
        return try {
            var current = scaleDownIfNeeded(bitmap, maxDimPx)
            var quality = initialQuality
            val baos = ByteArrayOutputStream()
            current.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            var bytes = baos.toByteArray()

            while (bytes.size > (maxBase64Length * 3) / 4 && quality > 35) {
                quality -= 10
                baos.reset()
                current.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                bytes = baos.toByteArray()
            }
            if (bytes.size > (maxBase64Length * 3) / 4) {
                current = scaleDownIfNeeded(current, 320)
                baos.reset()
                current.compress(Bitmap.CompressFormat.JPEG, 45, baos)
                bytes = baos.toByteArray()
            }
            "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (_: Throwable) {
            ""
        }
    }

    fun encodeImageUriToDataUrl(context: Context, uriString: String?): String {
        val bmp = decodeBitmapSafely(context, uriString, 420) ?: return ""
        return encodeBitmapToDataUrl(bmp)
    }

    fun encodeVideoThumbnailToDataUrl(context: Context, uriString: String?): String {
        val bmp = decodeVideoThumbnailSafely(context, uriString, 420) ?: return ""
        return encodeBitmapToDataUrl(bmp)
    }

    fun decodeBitmapSafely(
        context: Context,
        uriString: String?,
        maxDimPx: Int = 512
    ): Bitmap? {
        if (uriString.isNullOrBlank()) return null
        return try {
            val uri = Uri.parse(uriString)
            decodeBitmap(context, uri, uriString, maxDimPx)
        } catch (_: Throwable) {
            null
        }
    }

    fun copyUriToInternalStorage(
        context: Context,
        uri: Uri,
        fileName: String
    ): Uri? {
        val targetFile = File(context.filesDir, fileName)
        if (uri.scheme == "file") {
            val path = uri.path ?: return null
            val existing = File(path)
            if (!existing.exists() || !existing.canRead()) return null
            if (existing.absolutePath == targetFile.absolutePath) return uri
            return try {
                existing.inputStream().use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                if (targetFile.exists() && targetFile.length() > 0L) Uri.fromFile(targetFile) else null
            } catch (_: Throwable) {
                null
            }
        }

        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            if (targetFile.exists() && targetFile.length() > 0L) Uri.fromFile(targetFile) else null
        } catch (_: Throwable) {
            null
        }
    }

    fun saveBitmapToInternalStorage(
        context: Context,
        bitmap: Bitmap,
        fileName: String
    ): Uri? {
        return try {
            val targetFile = File(context.filesDir, fileName)
            FileOutputStream(targetFile).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
            }
            if (targetFile.exists() && targetFile.length() > 0L) Uri.fromFile(targetFile) else null
        } catch (_: Throwable) {
            null
        }
    }

    fun loadVideoThumbnailSafely(
        context: Context,
        imageView: ImageView,
        uriString: String?,
        @DrawableRes fallbackResId: Int
    ): Boolean {
        val frame = decodeVideoThumbnailSafely(context, uriString, 512)
        return if (frame != null) {
            imageView.setImageBitmap(frame)
            true
        } else {
            imageView.setImageResource(fallbackResId)
            false
        }
    }

    fun decodeVideoThumbnailSafely(
        context: Context,
        uriString: String?,
        maxDimPx: Int = 512
    ): Bitmap? {
        if (uriString.isNullOrBlank()) return null

        val uri = Uri.parse(uriString)
        val isLocalFile = uri.scheme == "file" || uriString.startsWith("/")
        val localFile = if (isLocalFile) File(uri.path ?: uriString) else null

        if (localFile != null && (!localFile.exists() || !localFile.canRead())) {
            return null
        }

        if (localFile != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val thumb = ThumbnailUtils.createVideoThumbnail(localFile, Size(maxDimPx, maxDimPx), null)
                if (thumb != null) return scaleDownIfNeeded(thumb, maxDimPx)
            } catch (_: Throwable) {
            }
        }

        val retriever = MediaMetadataRetriever()
        return try {
            if (localFile != null) {
                retriever.setDataSource(localFile.absolutePath)
            } else {
                retriever.setDataSource(context, uri)
            }
            val rawFrame = retriever.getFrameAtTime(-1)
                ?: retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST)
                ?: retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST)
                ?: retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.embeddedPicture?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
            rawFrame?.let { scaleDownIfNeeded(it, maxDimPx) }
        } catch (_: Throwable) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Throwable) {
            }
        }
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDimPx: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDimPx && h <= maxDimPx) return bitmap
        val scale = maxDimPx.toFloat() / maxOf(w, h).toFloat()
        val targetW = (w * scale).toInt().coerceAtLeast(1)
        val targetH = (h * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }

    private fun decodeBitmap(
        context: Context,
        uri: Uri,
        rawString: String,
        maxDimPx: Int
    ): Bitmap? {
        if (rawString.startsWith("data:image", ignoreCase = true)) {
            return decodeBase64DataUrl(rawString, maxDimPx)
        }

        val scheme = uri.scheme
        if (scheme == "file" || rawString.startsWith("/")) {
            val path = uri.path ?: rawString
            val file = File(path)
            if (!file.exists() || !file.canRead()) return null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    val source = ImageDecoder.createSource(file)
                    return decodeWithImageDecoder(source, maxDimPx)
                } catch (_: Throwable) {
                }
            }
            return decodeSampledFromFile(file, maxDimPx)
        }

        if (scheme == "content") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    return decodeWithImageDecoder(source, maxDimPx)
                } catch (_: Throwable) {
                }
            }
            return decodeSampledFromContentUri(context, uri, maxDimPx)
        }

        return null
    }

    private fun decodeBase64DataUrl(dataUrl: String, maxDimPx: Int): Bitmap? {
        return try {
            val base64Part = dataUrl.substringAfter(",", "")
            if (base64Part.isBlank()) return null
            val bytes = Base64.decode(base64Part, Base64.DEFAULT)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDimPx)
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        } catch (_: Throwable) {
            null
        }
    }

    private fun decodeWithImageDecoder(source: ImageDecoder.Source, maxDimPx: Int): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val w = info.size.width
                val h = info.size.height
                if (w > maxDimPx || h > maxDimPx) {
                    val scale = maxDimPx.toFloat() / maxOf(w, h).toFloat()
                    decoder.setTargetSize(
                        (w * scale).toInt().coerceAtLeast(1),
                        (h * scale).toInt().coerceAtLeast(1)
                    )
                }
            }
        } else {
            throw UnsupportedOperationException("ImageDecoder requires API 28+")
        }
    }

    private fun decodeSampledFromFile(file: File, maxDimPx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        FileInputStream(file).use { BitmapFactory.decodeStream(it, null, bounds) }

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDimPx)
        }
        return FileInputStream(file).use { BitmapFactory.decodeStream(it, null, options) }
    }

    private fun decodeSampledFromContentUri(
        context: Context,
        uri: Uri,
        maxDimPx: Int
    ): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        } ?: return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDimPx)
        }
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDimPx: Int): Int {
        var inSampleSize = 1
        if (width > maxDimPx || height > maxDimPx) {
            var halfWidth = width / 2
            var halfHeight = height / 2
            while (halfWidth / inSampleSize >= maxDimPx && halfHeight / inSampleSize >= maxDimPx) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
