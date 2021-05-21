package com.medyear.idvalidation.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Size
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.android.gms.vision.barcode.Barcode
import com.google.common.util.concurrent.ListenableFuture
import com.medyear.idvalidation.R
import com.medyear.idvalidation.databinding.ActivityPhotoCaptureBinding
import com.medyear.idvalidation.ext.externalPicturesDirectory
import com.medyear.idvalidation.helper.*
import com.medyear.idvalidation.model.DriverLicense
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Suppress("FunctionName")
class PhotoCaptureActivity : AppCompatActivity(), AnalyzerCallback {
    private lateinit var binding: ActivityPhotoCaptureBinding

    private var captureMode: Int = CAPTURE_MODE_ID_CARD_FRONT

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var imageCapture: ImageCapture
    private lateinit var imageAnalyzer: ImageAnalysis

    private lateinit var cameraExecutor: ExecutorService


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.binding = ActivityPhotoCaptureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        this.binding.close.setOnClickListener { onBackPressed() }

        captureMode = intent?.getIntExtra(ARG_CAPTURE_MODE, CAPTURE_MODE_ID_CARD_FRONT)
            ?: CAPTURE_MODE_ID_CARD_FRONT

        cameraExecutor = Executors.newSingleThreadExecutor()
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        when (captureMode) {
            CAPTURE_MODE_ID_CARD_FRONT -> {
                binding.apply {
                    title.text = getString(R.string.driving_licence_or_state_id_front)
                    instructionText.text = getString(R.string.message_hold_your_id_card_front)
                }
            }
            CAPTURE_MODE_ID_CARD_BACK -> {
                binding.apply {
                    title.text = getString(R.string.driving_licence_or_state_id_back)
                    instructionText.text = getString(R.string.message_hold_your_id_card_back)
                }
            }
            CAPTURE_MODE_SELFIE_FRONT -> {
                binding.apply {
                    title.text = getString(R.string.selfie_photo)
                    instructionText.text = getString(R.string.message_position_your_face)
                }
            }
        }

        when (ImageAnalyzer.getLensFacing(captureMode)) {
            CameraSelector.LENS_FACING_BACK -> {
                binding.cameraFrameOverlay.visibility = View.VISIBLE
                binding.faceFrameOverlay.visibility = View.GONE
            }
            CameraSelector.LENS_FACING_FRONT -> {
                binding.faceFrameOverlay.visibility = View.VISIBLE
                binding.cameraFrameOverlay.visibility = View.GONE
            }
        }
        binding.viewFinder.post { setupCameraX() }
    }

    private fun setupCameraX() {
        try {
//            val resolution = Size(400, 800)
            val resolution = Size(binding.viewFinder.width, binding.viewFinder.height)
            imageCapture = ImageCapture.Builder().apply {
                setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                setTargetRotation(binding.root.display.rotation)
                setTargetResolution(resolution)
            }.build()

            imageAnalyzer = ImageAnalysis.Builder().apply {
                setImageQueueDepth(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                setTargetRotation(binding.root.display.rotation)
                setTargetResolution(resolution)
            }.build()

            val analyzer = when (captureMode) {
                CAPTURE_MODE_ID_CARD_FRONT -> IDCardFrontAnalyzer(this, this)
                CAPTURE_MODE_ID_CARD_BACK -> IDCardBackAnalyzer(this, this)
                else -> SelfieAnalyzer(this, this)
            }
            imageAnalyzer.setAnalyzer(cameraExecutor, analyzer)

            cameraProviderFuture.addListener({
                _bindUseCases(cameraProviderFuture.get())
            }, ContextCompat.getMainExecutor(this))

        } catch (ex: Exception) {
            Timber.e(ex)
        }
    }

    private fun _bindUseCases(cameraProvider: ProcessCameraProvider) {
        cameraProvider.unbindAll()
        val preview = Preview.Builder().build()
        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(ImageAnalyzer.getLensFacing(captureMode))
                .build()
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer, imageCapture)
        preview.setSurfaceProvider(binding.viewFinder.surfaceProvider)
    }

    override fun onAnalyzeSuccess() {
        binding.viewFinder.post {
            val file = createFile(externalPicturesDirectory(), PHOTO_FORMAT, PHOTO_EXTENSION)
            val fileOptions = ImageCapture.OutputFileOptions.Builder(file).build()
            imageCapture.takePicture(
                fileOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        onSuccessCapture(file)
                    }

                    override fun onError(ex: ImageCaptureException) = onAnalyzeFailed(ex)
                })
        }
    }

    override fun onAnalyzeBackSuccess(license: Barcode.DriverLicense) {
        binding.viewFinder.post {
            when (ImageAnalyzer.getLensFacing(captureMode)) {
                CameraSelector.LENS_FACING_BACK -> {
                    binding.cameraFrameOverlay.setImageResource(R.drawable.ic_id_frame_success)
                }
                CameraSelector.LENS_FACING_FRONT -> {
                    binding.faceFrameOverlay.setImageResource(R.drawable.ic_selfie_frame_success)
                }
            }
            binding.instructionText.text = getString(R.string.successfully_scanned)
            binding.doneButton.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.welcome_green
                )
            )
            val intent = Intent()
            intent.putExtra("output_license", DriverLicense.convert2(license))
            setResult(Activity.RESULT_OK, intent)
            cameraProviderFuture.get().unbindAll()
            binding.root.postDelayed({ finish() }, 1500)
        }
    }

    override fun onAnalyzeFailed(ex: Throwable) {
        Timber.e(ex)
        binding.viewFinder.post {
            when (ImageAnalyzer.getLensFacing(captureMode)) {
                CameraSelector.LENS_FACING_BACK -> {
                    binding.cameraFrameOverlay.setImageResource(R.drawable.ic_id_frame_fail)
                }
                CameraSelector.LENS_FACING_FRONT -> {
                    binding.faceFrameOverlay.setImageResource(R.drawable.ic_selfie_frame_fail)
                }
            }
            binding.doneButton.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.gray_text_color
                )
            )
            cameraProviderFuture.get().unbindAll()
            binding.root.postDelayed({ finish() }, 1500)
        }
    }

    private fun onSuccessCapture(file: File) {
        Timber.v("Successfully saved")
        binding.viewFinder.post {
            when (ImageAnalyzer.getLensFacing(captureMode)) {
                CameraSelector.LENS_FACING_BACK -> {
                    binding.cameraFrameOverlay.setImageResource(R.drawable.ic_id_frame_success)
                }
                CameraSelector.LENS_FACING_FRONT -> {
                    binding.faceFrameOverlay.setImageResource(R.drawable.ic_selfie_frame_success)
                }
            }
            binding.instructionText.text = getString(R.string.successfully_scanned)
            binding.doneButton.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.welcome_green
                )
            )
            val intent = Intent()
            intent.putExtra("output_path", file.absolutePath)
            this.setResult(Activity.RESULT_OK, intent)
            cameraProviderFuture.get().unbindAll()
            binding.root.postDelayed({ finish() }, 1500)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        const val CAPTURE_MODE_ID_CARD_FRONT = 1
        const val CAPTURE_MODE_ID_CARD_BACK = 2
        const val CAPTURE_MODE_SELFIE_FRONT = 3

        const val ARG_CAPTURE_MODE = "ARG_CAPTURE_MODE"

        private const val PHOTO_FORMAT = "yyyyMMddHHmmssSSS"
        private const val PHOTO_EXTENSION = ".jpg"

        fun createFile(baseFolder: File, format: String, extension: String) =
            File(
                baseFolder, SimpleDateFormat(format, Locale.US)
                    .format(System.currentTimeMillis()) + extension
            )
    }

}