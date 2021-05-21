package com.medyear.idvalidation.ext

import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Environment
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream

fun Context.makeToast(text: CharSequence) = makeToast(text, Toast.LENGTH_SHORT)
fun Context.makeToast(text: CharSequence, duration: Int) =
    Toast.makeText(this, text, duration).show()

fun Context.makeToast(@StringRes resId: Int) = makeToast(resId, Toast.LENGTH_SHORT)
fun Context.makeToast(@StringRes resId: Int, duration: Int) =
    Toast.makeText(this, this.getString(resId), duration).show()


fun Bitmap?.toByteArray(): ByteArray? {
    if (this == null) return null
    val stream = ByteArrayOutputStream()
    stream.use { this.compress(Bitmap.CompressFormat.JPEG, 100, it) }
    return stream.toByteArray()
}

fun ImageProxy.toBitmap(): Bitmap? {
    val nv21 = yuv420888ToNv21(this)
    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    return yuvImage.toBitmap()
}

private fun YuvImage.toBitmap(): Bitmap? {
    val out = ByteArrayOutputStream()
    if (!compressToJpeg(Rect(0, 0, width, height), 100, out))
        return null
    val imageBytes: ByteArray = out.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}

private fun yuv420888ToNv21(image: ImageProxy): ByteArray {
    val pixelCount = image.cropRect.width() * image.cropRect.height()
    val pixelSizeBits = ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888)
    val outputBuffer = ByteArray(pixelCount * pixelSizeBits / 8)
    imageToByteBuffer(image, outputBuffer, pixelCount)
    return outputBuffer
}

private fun imageToByteBuffer(image: ImageProxy, outputBuffer: ByteArray, pixelCount: Int) {
    if (image.format != ImageFormat.YUV_420_888) {
        throw RuntimeException("image.format is not YUV_420_888")
    }

    val imageCrop = image.cropRect
    val imagePlanes = image.planes

    imagePlanes.forEachIndexed { planeIndex, plane ->
        // How many values are read in input for each output value written
        // Only the Y plane has a value for every pixel, U and V have half the resolution i.e.
        //
        // Y Plane            U Plane    V Plane
        // ===============    =======    =======
        // Y Y Y Y Y Y Y Y    U U U U    V V V V
        // Y Y Y Y Y Y Y Y    U U U U    V V V V
        // Y Y Y Y Y Y Y Y    U U U U    V V V V
        // Y Y Y Y Y Y Y Y    U U U U    V V V V
        // Y Y Y Y Y Y Y Y
        // Y Y Y Y Y Y Y Y
        // Y Y Y Y Y Y Y Y
        val outputStride: Int

        // The index in the output buffer the next value will be written at
        // For Y it's zero, for U and V we start at the end of Y and interleave them i.e.
        //
        // First chunk        Second chunk
        // ===============    ===============
        // Y Y Y Y Y Y Y Y    V U V U V U V U
        // Y Y Y Y Y Y Y Y    V U V U V U V U
        // Y Y Y Y Y Y Y Y    V U V U V U V U
        // Y Y Y Y Y Y Y Y    V U V U V U V U
        // Y Y Y Y Y Y Y Y
        // Y Y Y Y Y Y Y Y
        // Y Y Y Y Y Y Y Y
        var outputOffset: Int

        when (planeIndex) {
            0 -> {
                outputStride = 1
                outputOffset = 0
            }
            1 -> {
                outputStride = 2
                // For NV21 format, U is in odd-numbered indices
                outputOffset = pixelCount + 1
            }
            2 -> {
                outputStride = 2
                // For NV21 format, V is in even-numbered indices
                outputOffset = pixelCount
            }
            else -> {
                // Image contains more than 3 planes, something strange is going on
                return@forEachIndexed
            }
        }

        val planeBuffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride

        // We have to divide the width and height by two if it's not the Y plane
        val planeCrop = if (planeIndex == 0) {
            imageCrop
        } else {
            Rect(
                imageCrop.left / 2,
                imageCrop.top / 2,
                imageCrop.right / 2,
                imageCrop.bottom / 2
            )
        }

        val planeWidth = planeCrop.width()
        val planeHeight = planeCrop.height()

        // Intermediate buffer used to store the bytes of each row
        val rowBuffer = ByteArray(plane.rowStride)

        // Size of each row in bytes
        val rowLength = if (pixelStride == 1 && outputStride == 1) {
            planeWidth
        } else {
            // Take into account that the stride may include data from pixels other than this
            // particular plane and row, and that could be between pixels and not after every
            // pixel:
            //
            // |---- Pixel stride ----|                    Row ends here --> |
            // | Pixel 1 | Other Data | Pixel 2 | Other Data | ... | Pixel N |
            //
            // We need to get (N-1) * (pixel stride bytes) per row + 1 byte for the last pixel
            (planeWidth - 1) * pixelStride + 1
        }

        for (row in 0 until planeHeight) {
            // Move buffer position to the beginning of this row
            planeBuffer.position(
                (row + planeCrop.top) * rowStride + planeCrop.left * pixelStride
            )

            if (pixelStride == 1 && outputStride == 1) {
                // When there is a single stride value for pixel and output, we can just copy
                // the entire row in a single step
                planeBuffer.get(outputBuffer, outputOffset, rowLength)
                outputOffset += rowLength
            } else {
                // When either pixel or output have a stride > 1 we must copy pixel by pixel
                planeBuffer.get(rowBuffer, 0, rowLength)
                for (col in 0 until planeWidth) {
                    outputBuffer[outputOffset] = rowBuffer[col * pixelStride]
                    outputOffset += outputStride
                }
            }
        }
    }
}

fun Bitmap.rotate(degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

fun Bitmap.cropForIdCard(): Bitmap {
    val x1 = (width * 0.04f).toInt()
    val y1 = (height * 0.33f).toInt()
    val w = width - 2 * x1
    val h = height - 2 * y1
    return Bitmap.createBitmap(this, x1, y1, w, h)
}

fun Context.externalPicturesDirectory(): File =
    this.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: this.filesDir

fun Bitmap?.encodeBase64(): String? {
    if (this == null) return null
    val imgData = this.toByteArray() ?: return null
    return Base64.encodeToString(imgData, Base64.NO_WRAP)
}

fun String?.toBitmapWithoutException(): Bitmap? {
    if (this == null) return null
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(File(this)))
        }
        return FileInputStream(this).use { BitmapFactory.decodeStream(it) }
    } catch (e: Exception) {
        Timber.e(e)
        return null
    }
}


fun View.compatDrawable(@DrawableRes drawableRes: Int) =
    ContextCompat.getDrawable(context, drawableRes)


fun View.compatColor(@ColorRes colorRes: Int) =
    ContextCompat.getColor(context, colorRes)

