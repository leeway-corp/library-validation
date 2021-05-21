package com.medyear.idvalidation.helper

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.core.util.forEach
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.FaceDetector
import com.medyear.idvalidation.ext.rotate
import com.medyear.idvalidation.ext.toBitmap
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

internal open class SelfieAnalyzer(private val context: Context, private val callback: AnalyzerCallback) :
    ImageAnalysis.Analyzer {

    private var isAnalyzing = AtomicBoolean(false)
    private var isCompleted = AtomicBoolean(false)

    private val faceDetector: FaceDetector by lazy {
        val detector = FaceDetector.Builder(context)
            .setTrackingEnabled(false)
            .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
            .setMinFaceSize(0.5f)
            .build()
        if (!detector.isOperational) {
            callback.onAnalyzeFailed(IOException("Could not set up the face detector"))
        }
        detector
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        try {
            if (isAnalyzing.get() || isCompleted.get()) return
            this.isAnalyzing.set(true)

            val rotation = imageProxy.imageInfo.rotationDegrees
            val bitmap = imageProxy.toBitmap()?.rotate(rotation.toFloat()) ?: return

            val frame = Frame.Builder().setBitmap(bitmap).setRotation(Frame.ROTATION_0).build()
            val faces = faceDetector.detect(frame)

            faces.forEach() { _, face ->
                if (!isCompleted.get() && face.isSmilingProbability * 100 > 10) {
                    if (face.isFaceInLocation(bitmap)) {
                        Timber.v("-------- face: detected")
                        isCompleted.set(true)
                        callback.onAnalyzeSuccess()
                    }
                }
            }

            this.isAnalyzing.set(false)

        } catch (ex: Exception) {
            isCompleted.set(true)
            callback.onAnalyzeFailed(ex)
        } finally {
            imageProxy.close()
        }
    }


    private fun Face.isFaceInLocation(b: Bitmap): Boolean {
        val x1 = this.position.x
        val y1 = this.position.y
        val x2 = x1 + this.width
        val y2 = y1 + this.height
        val rectF = RectF(x1, y1, x2, y2)

        val diffWidth = abs(b.width / 2 - rectF.centerX())
        val diffHeight = abs(b.height / 2 - rectF.centerY())
        return (diffWidth > 0 && diffWidth < 20f) &&
                diffHeight > 0 && diffHeight < 20f
    }


}

