package com.medyear.idvalidation.model

import java.io.Serializable

data class IDValidationResult(
    val frontImagePath: String? = null,
    val faceImagePath: String? = null,
    val license: DriverLicense? = null
) : Serializable
