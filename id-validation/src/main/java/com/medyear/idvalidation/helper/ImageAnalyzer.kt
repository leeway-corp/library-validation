package com.medyear.idvalidation.helper

import android.annotation.SuppressLint
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.medyear.idvalidation.activity.PhotoCaptureActivity
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

internal abstract class ImageAnalyzer : ImageAnalysis.Analyzer {

    private var isLabelAnalyzing = AtomicBoolean(false)
    private var isBarcodeAnalyzing = AtomicBoolean(false)
    private var isFaceAnalyzing = AtomicBoolean(false)
    private var isCompleted = AtomicBoolean(false)

    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        try {
            if (isLabelAnalyzing.get() || isBarcodeAnalyzing.get() || isFaceAnalyzing.get() || isCompleted.get()) return
            isLabelAnalyzing.set(true)
            isBarcodeAnalyzing.set(true)
            isFaceAnalyzing.set(true)

        } catch (ex: Exception) {
            Timber.e(ex)
        } finally {
            imageProxy.close()
        }
    }

    companion object {
        fun getLensFacing(captureMode: Int?): Int = when (captureMode) {
            PhotoCaptureActivity.CAPTURE_MODE_SELFIE_FRONT -> CameraSelector.LENS_FACING_FRONT
            else -> CameraSelector.LENS_FACING_BACK
        }
    }
}

