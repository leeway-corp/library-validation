package com.medyear.idvalidation.helper

import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.core.util.forEach
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.google.android.gms.vision.label.ImageLabeler
import com.medyear.idvalidation.ext.cropForIdCard
import com.medyear.idvalidation.ext.rotate
import com.medyear.idvalidation.ext.toBitmap
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

internal open class IDCardBackAnalyzer(
    private val context: Context,
    private val callback: AnalyzerCallback
) : ImageAnalysis.Analyzer {

    private var average = 0f

    private var isAnalyzing = AtomicBoolean(false)
    private var isLabelAnalyzing = AtomicBoolean(false)

    private var isCompleted = AtomicBoolean(false)

    private val barcodeDetector: BarcodeDetector by lazy {
        val detector = BarcodeDetector.Builder(context)
            .setBarcodeFormats(Barcode.PDF417)
            .build()
        if (!detector.isOperational) {
            callback.onAnalyzeFailed(IOException("Could not set up the barcode detector"))
        }
        detector
    }

    private val imageLabeler: ImageLabeler by lazy {
        val detector = ImageLabeler.Builder(context).build()
        if (!detector.isOperational) {
            callback.onAnalyzeFailed(IOException("Could not set up the image labeler"))
        }
        detector
    }


    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        try {
            if (isLabelAnalyzing.get() || isAnalyzing.get() || isCompleted.get()) return
            this.isAnalyzing.set(true)
            this.isLabelAnalyzing.set(true)

            val rotation = imageProxy.imageInfo.rotationDegrees
            val bitmap = imageProxy.toBitmap()?.rotate(rotation.toFloat()) ?: return

            val cropped = bitmap.cropForIdCard()
            val frame = Frame.Builder().setBitmap(cropped).setRotation(Frame.ROTATION_0).build()

            processImageLabel(frame)

            processBarCode(frame)

        } catch (ex: Exception) {
            isCompleted.set(true)
            callback.onAnalyzeFailed(ex)
        } finally {
            imageProxy.close()
        }
    }

    private fun processBarCode(frame: Frame) {
        val barCodes = barcodeDetector.detect(frame)

        barCodes.forEach() { _, barCode ->
            if (barCode.format == Barcode.PDF417) {
                Timber.v("---- Barcode.PDF417 [${barCode.rawValue}]")
                val license = barCode.driverLicense
                if (!isCompleted.get() && average > 50 && license != null) {
                    isCompleted.set(true)
                    callback.onAnalyzeBackSuccess(license)
                }
            }
        }

        this.isAnalyzing.set(false)
    }

    private fun processImageLabel(frame: Frame) {
        val labels = imageLabeler.detect(frame)

        var posterConfidence = 0f
        var posterCount = 0

        labels.forEach { _, label ->
            if (label.label == "Poster" || label.label == "Paper") {
                posterConfidence += label.confidence
                posterCount++
            }
        }

        this.average = (posterConfidence / posterCount) * 100

        Timber.v("--------- average: $average")
        if (!isCompleted.get() && average > 50) {
            Timber.v("detected ID Card")
        }

        this.isLabelAnalyzing.set(false)
    }

}

