package com.medyear.idvalidation.helper

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.core.util.forEach
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.face.FaceDetector
import com.google.android.gms.vision.label.ImageLabeler
import com.medyear.idvalidation.ext.cropForIdCard
import com.medyear.idvalidation.ext.rotate
import com.medyear.idvalidation.ext.toBitmap
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

internal open class IDCardFrontAnalyzer(
    private val context: Context,
    private val callback: AnalyzerCallback
) : ImageAnalysis.Analyzer {

    private var average = 0f
    private var faceCount = 0

    private var isAnalyzing = AtomicBoolean(false)
    private var isFaceAnalyzing = AtomicBoolean(false)

    private var isCompleted = AtomicBoolean(false)

    private val imageLabeler: ImageLabeler by lazy {
        val detector = ImageLabeler.Builder(context).build()
        if (!detector.isOperational) {
            callback.onAnalyzeFailed(IOException("Could not set up the image labeler"))
        }
        detector
    }

    private val faceDetector: FaceDetector by lazy {
        val detector = FaceDetector.Builder(context)
            .setTrackingEnabled(false)
            .setClassificationType(FaceDetector.FAST_MODE)
            .build()
        if (!detector.isOperational) {
            callback.onAnalyzeFailed(IOException("Could not set up the face detector"))
        }
        detector
    }


    override fun analyze(imageProxy: ImageProxy) {
        try {
            if (isFaceAnalyzing.get() || isAnalyzing.get() || isCompleted.get()) return
            this.isAnalyzing.set(true)
            this.isFaceAnalyzing.set(true)

            val rotation = imageProxy.imageInfo.rotationDegrees
            val bitmap = imageProxy.toBitmap()?.rotate(rotation.toFloat()) ?: return

            processFaceDetect(bitmap)

            processImageLabel(bitmap)

        } catch (ex: Exception) {
            isCompleted.set(true)
            callback.onAnalyzeFailed(ex)
        } finally {
            imageProxy.close()
        }
    }

    private fun processImageLabel(bitmap: Bitmap) {
        val cropped = bitmap.cropForIdCard()
        val frame = Frame.Builder().setBitmap(cropped).setRotation(Frame.ROTATION_0).build()

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

        if (!isCompleted.get() && average > 50 && faceCount > 0) {
            Timber.v("detected ID Card")
            isCompleted.set(true)
            callback.onAnalyzeSuccess()
        }

        this.isAnalyzing.set(false)
    }

    private fun processFaceDetect(bitmap: Bitmap) {
        val frame = Frame.Builder().setBitmap(bitmap).setRotation(Frame.ROTATION_0).build()
        val faces = faceDetector.detect(frame)
        faceCount = faces.size()
        this.isFaceAnalyzing.set(false)
    }

}

