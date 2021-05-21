package com.medyear.idvalidation.helper

import com.google.android.gms.vision.barcode.Barcode

internal interface AnalyzerCallback {
    fun onAnalyzeSuccess()

    fun onAnalyzeBackSuccess(license: Barcode.DriverLicense)

    fun onAnalyzeFailed(ex: Throwable)
}