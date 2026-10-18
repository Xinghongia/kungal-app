package com.kungal.app.ui.news

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

/** 可直接上传的封面字节与对应的文件名/类型。 */
data class PreparedBanner(
    val bytes: ByteArray,
    val fileName: String,
    val contentType: String,
)

/**
 * 把相册图片处理成网页同款的 16:9 横图封面：按 EXIF 摆正、居中裁切、
 * 缩到 1280×720 以内并压缩为 JPEG，保证不超过服务端 4,000,000 字节上限。
 * 解码失败时退回原图字节（仍受大小上限约束）。
 */
fun prepareNewsBanner(context: Context, uri: Uri): PreparedBanner {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        val raw = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("无法读取这张图片")
        require(raw.size <= NEWS_BANNER_MAX_BYTES) { "封面不能超过 4 MB" }
        val mime = resolver.getType(uri) ?: "image/jpeg"
        val ext = when (mime) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        return PreparedBanner(raw, "banner.$ext", mime)
    }

    val options = BitmapFactory.Options().apply {
        inSampleSize = bannerSampleSize(bounds.outWidth, bounds.outHeight, 2560)
    }
    val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        ?: error("无法读取这张图片")
    val upright = applyExifRotation(resolver, uri, decoded)
    val cropped = centerCropToWide(upright)
    val scaled = scaleDown(cropped, 1280)
    val bytes = compressJpeg(scaled)
    require(bytes.size <= NEWS_BANNER_MAX_BYTES) { "封面压缩后仍超过 4 MB，请换一张图片" }
    return PreparedBanner(bytes, "banner.jpg", "image/jpeg")
}

private fun bannerSampleSize(width: Int, height: Int, max: Int): Int {
    var sample = 1
    while (width / sample > max || height / sample > max) sample *= 2
    return sample
}

private fun applyExifRotation(
    resolver: ContentResolver,
    uri: Uri,
    bitmap: Bitmap,
): Bitmap {
    val orientation = runCatching {
        resolver.openInputStream(uri)?.use {
            @Suppress("DEPRECATION")
            ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        }
    }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.postRotate(90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.postRotate(270f)
            matrix.postScale(-1f, 1f)
        }
        else -> return bitmap
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun centerCropToWide(bitmap: Bitmap): Bitmap {
    val target = 16f / 9f
    val current = bitmap.width.toFloat() / bitmap.height
    return when {
        current > target -> {
            val width = (bitmap.height * target).roundToInt()
            Bitmap.createBitmap(bitmap, (bitmap.width - width) / 2, 0, width, bitmap.height)
        }
        current < target -> {
            val height = (bitmap.width / target).roundToInt()
            Bitmap.createBitmap(bitmap, 0, (bitmap.height - height) / 2, bitmap.width, height)
        }
        else -> bitmap
    }
}

private fun scaleDown(bitmap: Bitmap, maxWidth: Int): Bitmap {
    if (bitmap.width <= maxWidth) return bitmap
    val height = (bitmap.height.toFloat() * maxWidth / bitmap.width).roundToInt()
    return Bitmap.createScaledBitmap(bitmap, maxWidth, height, true)
}

private fun compressJpeg(bitmap: Bitmap): ByteArray {
    // 透明区域按白底合成，避免 JPEG 变黑。
    val source = if (bitmap.hasAlpha()) {
        Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888).also { out ->
            Canvas(out).apply {
                drawColor(Color.WHITE)
                drawBitmap(bitmap, 0f, 0f, null)
            }
        }
    } else {
        bitmap
    }
    for (quality in intArrayOf(88, 70, 55)) {
        val out = ByteArrayOutputStream()
        source.compress(Bitmap.CompressFormat.JPEG, quality, out)
        if (out.size() <= NEWS_BANNER_MAX_BYTES) return out.toByteArray()
    }
    val out = ByteArrayOutputStream()
    source.compress(Bitmap.CompressFormat.JPEG, 40, out)
    return out.toByteArray()
}
