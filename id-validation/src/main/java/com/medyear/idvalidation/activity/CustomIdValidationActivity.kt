package com.medyear.idvalidation.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.transition.TransitionManager
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import com.medyear.idvalidation.AWSMatchLoader
import com.medyear.idvalidation.R
import com.medyear.idvalidation.databinding.ActivityCustomIdValidationBinding
import com.medyear.idvalidation.ext.UiUtils
import com.medyear.idvalidation.ext.makeToast
import com.medyear.idvalidation.ext.perm.PermissionUtils
import com.medyear.idvalidation.ext.toBitmapWithoutException
import com.medyear.idvalidation.model.*
import com.medyear.idvalidation.viewmodel.IdValidationViewModel
import timber.log.Timber

@Suppress("DEPRECATION", "FunctionName")
class CustomIdValidationActivity : AppCompatActivity(), LoaderManager.LoaderCallbacks<Any> {
    private val vm: IdValidationViewModel by viewModels{
        SavedStateViewModelFactory(application, this)
    }
    private lateinit var binding: ActivityCustomIdValidationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.binding = ActivityCustomIdValidationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        this.binding.backBtn.setOnClickListener { onBackPressed() }
        this.binding.btnFront.setOnClickListener { onFrontScanClicked() }
        this.binding.btnBack.setOnClickListener { onBackScanClicked() }
        this.binding.btnSelfie.setOnClickListener { onSelfieScanClicked() }
        this.binding.btnSubmit.setOnClickListener { onNextClicked() }

        vm.drivingFrontLiveData.observe(this, { v ->
            _showFrontItem(v.isScanned)
        })
        vm.drivingBackLiveData.observe(this, { v ->
            _showBackItem(v.isScanned)
        })
        vm.selfieLiveData.observe(this, { v ->
            _showSelfieItem(v.isScanned)
        })

        vm.statusLiveData.observe(this, {
            binding.btnSubmit.visibility = if (it) View.VISIBLE else View.GONE
        })
    }

    private fun _showFrontItem(scanned: Boolean) {
        TransitionManager.beginDelayedTransition(binding.nsLayout)
        binding.frontContent.setScanned(scanned)
        binding.frontExpandLayout.setScanned(scanned)
        if (scanned) {
            binding.frontExpandLayout.collapseIdLayout()
            binding.backExpandLayout.expandIdLayout()
            binding.selfieExpandLayout.collapseIdLayout()
        }
    }

    private fun _showBackItem(scanned: Boolean) {
        TransitionManager.beginDelayedTransition(binding.nsLayout)
        binding.backContent.setScanned(scanned)
        binding.backExpandLayout.setScanned(scanned)
        if (scanned) {
            binding.frontExpandLayout.collapseIdLayout()
            binding.backExpandLayout.collapseIdLayout()
            binding.selfieExpandLayout.expandIdLayout()
        }
    }

    private fun _showSelfieItem(scanned: Boolean) {
        TransitionManager.beginDelayedTransition(binding.nsLayout)
        binding.selfieContent.setScanned(scanned)
        binding.selfieExpandLayout.setScanned(scanned)
        if (scanned) {
            binding.frontExpandLayout.collapseIdLayout()
            binding.backExpandLayout.collapseIdLayout()
            binding.selfieExpandLayout.collapseIdLayout()
        }
    }

    fun onFrontScanClicked() = showPhotoCapture(PhotoCaptureActivity.CAPTURE_MODE_ID_CARD_FRONT)

    fun onBackScanClicked() = showPhotoCapture(PhotoCaptureActivity.CAPTURE_MODE_ID_CARD_BACK)

    fun onSelfieScanClicked() = showPhotoCapture(PhotoCaptureActivity.CAPTURE_MODE_SELFIE_FRONT)

    fun onNextClicked() = loadAwsImageValidate()

    private fun loadAwsImageValidate() {
        UiUtils.showProgressDialog(this)
        LoaderManager.getInstance(this).restartLoader(LOADER_AWS_IMAGE_VALIDATE, null, this)
    }

    private fun showPhotoCapture(captureMode: Int) {
        PermissionUtils.requestCameraStoragePermissions(this) {
            val intent = Intent(this, PhotoCaptureActivity::class.java)
            intent.putExtra(PhotoCaptureActivity.ARG_CAPTURE_MODE, captureMode)
            startActivityForResult(
                intent, when (captureMode) {
                    PhotoCaptureActivity.CAPTURE_MODE_SELFIE_FRONT -> REQ_CODE_CAPTURE_MODE_SELFIE_FRONT
                    PhotoCaptureActivity.CAPTURE_MODE_ID_CARD_BACK -> REQ_CODE_CAPTURE_MODE_ID_CARD_BACK
                    else -> REQ_CODE_CAPTURE_MODE_ID_CARD_FRONT
                }
            )
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Any> {
        UiUtils.showProgressDialog(this)
        return when (id) {
            LOADER_AWS_IMAGE_VALIDATE -> AWSMatchLoader(
                this,
                vm.drivingFrontLiveData.value?.bitmap,
                vm.selfieLiveData.value?.bitmap
            )
            else -> Loader(this)
        }
    }

    override fun onLoadFinished(loader: Loader<Any>, data: Any?) {
        UiUtils.hideProgressDialog()
        when (loader.id) {
            LOADER_AWS_IMAGE_VALIDATE -> onFetchAWSImageValidate(data as AWSResult?)
        }
        LoaderManager.getInstance(this).destroyLoader(loader.id)
    }

    override fun onLoaderReset(loader: Loader<Any>) = loader.reset()

    private fun onFetchAWSImageValidate(result: AWSResult?) {
        if (result == null) return makeToast(R.string.something_is_wrong)

        if (result.isValid) {
            makeToast(R.string.id_validation_success)
            onValidationDone()
        } else {
            Timber.e(result.message)
            makeToast(R.string.id_validation_failed)
//            finish()
        }
    }

    private fun onValidationDone() {
        val data = IDValidationResult(
            frontImagePath = vm.drivingFrontLiveData.value?.photoPath,
            faceImagePath = vm.selfieLiveData.value?.photoPath,
            license = vm.drivingBackLiveData.value?.driverLicense
        )
        setResult(Activity.RESULT_OK, Intent().apply { putExtra("data", data) })
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQ_CODE_CAPTURE_MODE_ID_CARD_FRONT -> {
                    val path = data?.extras?.getString("output_path")
                    val bitmap = path.toBitmapWithoutException()
                    vm.drivingFrontLiveData.value = PhotoCaptureModel(
                        true, path
                            ?: "", bitmap, null
                    )
                }

                REQ_CODE_CAPTURE_MODE_ID_CARD_BACK -> {
                    val license = data?.extras?.getSerializable("output_license") as DriverLicense
                    vm.drivingBackLiveData.value = PhotoCaptureModel(true, null, null, license)
                }

                REQ_CODE_CAPTURE_MODE_SELFIE_FRONT -> {
                    val path = data?.extras?.getString("output_path")
                    val bitmap = path.toBitmapWithoutException()
                    vm.selfieLiveData.value = PhotoCaptureModel(true, path ?: "", bitmap, null)
                }
            }
        }
    }

    companion object {
        private const val REQ_CODE_CAPTURE_MODE_ID_CARD_FRONT = 1
        private const val REQ_CODE_CAPTURE_MODE_ID_CARD_BACK = 2
        private const val REQ_CODE_CAPTURE_MODE_SELFIE_FRONT = 3

        private const val LOADER_AWS_IMAGE_VALIDATE = 1

    }
}