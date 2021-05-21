package com.medyear.idvalidation.model

internal data class AWSResult(val isValid: Boolean, val message: String) {
    companion object {
        fun success() = AWSResult(true, "ok")
        fun error(message: String) = AWSResult(false, message)
    }
}