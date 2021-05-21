package com.medyear.idvalidation.ext.perm

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import timber.log.Timber

internal object PermissionUtils {

    /**
     * Permission: Manifest.permission.CAMERA
     */
    @JvmStatic
    fun requestCameraPermission(context: Context, runnable: Runnable) {
        requestSinglePermission(context, Manifest.permission.CAMERA, runnable)
    }

    /**
     * Permissions: listOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
     */
    @JvmStatic
    fun requestCameraStoragePermissions(context: Context, runnable: Runnable) {
        requestMultiplePermission(
            context,
            listOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
            runnable
        )
    }

    /**
     * Permissions: listOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
     */
    @JvmStatic
    fun requestStoragePermissions(context: Context, runnable: Runnable) {
        requestMultiplePermission(
            context,
            listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            runnable
        )
    }

    /**
     * Permissions: listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
     */
    @JvmStatic
    fun requestLocationPermissions(context: Context, runnable: Runnable) {
        requestMultiplePermission(
            context,
            listOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            runnable
        )
    }

    /**
     * Permissions: listOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
     */
    @JvmStatic
    fun requestZoomPermissions(context: Context, runnable: Runnable) {
        requestMultiplePermission(
            context, listOf(
                Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO
            ), runnable
        )
    }


    private fun requestSinglePermission(context: Context, permission: String, runnable: Runnable) {
        requestSinglePermission(context, permission, object : PermissionCallback {
            override fun onPermissionRequest(granted: Boolean) {
                if (granted) runnable.run()
            }
        })
    }

    private fun requestSinglePermission(
        context: Context,
        permission: String,
        callback: PermissionCallback
    ) {
        Dexter.withContext(context)
            .withPermission(permission)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                    // User has granted the permission
                    callback.onPermissionRequest(granted = true)
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest?,
                    token: PermissionToken?
                ) {
                    // User previously denied the permission, request them again
                    token?.continuePermissionRequest()
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                    // User has denied the permission
                    callback.onPermissionRequest(granted = false)
                }
            })
            .check()
    }

    private fun requestMultiplePermission(
        context: Context,
        permissions: List<String>,
        runnable: Runnable
    ) {
        requestMultiplePermission(context, permissions, object : PermissionCallback {
            override fun onPermissionRequest(granted: Boolean) {
                if (granted) runnable.run()
            }
        })
    }

    private fun requestMultiplePermission(
        context: Context,
        permissions: List<String>,
        callback: PermissionCallback
    ) {
        Dexter.withContext(context)
            .withPermissions(permissions)
            .withListener(CDialogOnAnyDeniedMultiplePermissionsListener.Builder
                .withContext(context)
                .withCallback(callback)
                .withMessage("Please allow the required permissions to continue")
                .withButtonText(android.R.string.ok) {
                    showSettings(context)
                }
                .build()
            )
            .check()
    }

    private fun showSettings(context: Context) {
        try {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                )
            )
        } catch (ex: Exception) {
            Timber.e(ex)
        }
    }

}