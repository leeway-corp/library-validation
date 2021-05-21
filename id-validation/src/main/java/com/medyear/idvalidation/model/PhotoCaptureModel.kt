package com.medyear.idvalidation.model

import android.graphics.Bitmap

internal data class PhotoCaptureModel(
    val isScanned: Boolean,
    val photoPath: String?,
    val bitmap: Bitmap?,
    val driverLicense: DriverLicense?
)