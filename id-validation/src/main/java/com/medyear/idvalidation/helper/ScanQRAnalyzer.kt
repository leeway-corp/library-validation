package com.medyear.idvalidation.helper

import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.core.util.forEach
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.medyear.idvalidation.ext.cropForIdCard
import com.medyear.idvalidation.ext.rotate
import com.medyear.idvalidation.ext.toBitmap
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

internal abstract class ScanQRAnalyzer(private val context: Context) : ImageAnalysis.Analyzer {

    private var isBarcodeAnalyzing = AtomicBoolean(false)
    private var isCompleted = AtomicBoolean(false)

    private val barcodeDetector: BarcodeDetector by lazy {
        val detector = BarcodeDetector.Builder(context)
            .setBarcodeFormats(Barcode.QR_CODE)
            .build()
        if (!detector.isOperational) {
            isCompleted.set(true)
            onEventFailed(IOException("Could not set up the barcode detector"))
        }
        detector
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        try {
            if (isBarcodeAnalyzing.get() || isCompleted.get()) return
            isBarcodeAnalyzing.set(true)


            val rotation = imageProxy.imageInfo.rotationDegrees
            val bitmap = imageProxy.toBitmap()?.rotate(rotation.toFloat()) ?: return

            val cropped = bitmap.cropForIdCard()
            val frame = Frame.Builder().setBitmap(cropped).setRotation(Frame.ROTATION_0).build()
            val barCodes = barcodeDetector.detect(frame)

            barCodes.forEach() { _, barCode ->
                if (barCode.format == Barcode.QR_CODE) {
                    Timber.v("QR_CODE [${barCode.rawValue}]")
                    barCode.url?.let {
                        isCompleted.set(true)
                        onEventSuccess(it.url)
                    }
                }
            }
            this.isBarcodeAnalyzing.set(false)

        } catch (ex: Exception) {
            isCompleted.set(true)
            onEventFailed(ex)
        } finally {
            imageProxy.close()
        }
    }

    abstract fun onEventSuccess(url: String)

    abstract fun onEventFailed(ex: Exception)
}

