package com.medyear.idvalidation.viewmodel

import android.util.Base64
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.medyear.idvalidation.ext.toByteArray
import com.medyear.idvalidation.model.PhotoCaptureModel

internal class IdValidationViewModel : ViewModel() {

    val drivingFrontLiveData: MutableLiveData<PhotoCaptureModel> = MutableLiveData()

    val drivingBackLiveData: MutableLiveData<PhotoCaptureModel> = MutableLiveData()

    val selfieLiveData: MutableLiveData<PhotoCaptureModel> = MutableLiveData()

    val statusLiveData: MediatorLiveData<Boolean> = MediatorLiveData()

    init {
        statusLiveData.addSource(drivingFrontLiveData) { statusLiveData.value = combineLiveData() }
        statusLiveData.addSource(selfieLiveData) { statusLiveData.value = combineLiveData() }
    }

    private fun combineLiveData(): Boolean {
        val frontValue = drivingFrontLiveData.value
        val selfieValue = selfieLiveData.value
        if (frontValue == null) return false
        if (selfieValue == null) return false
        return frontValue.isScanned && selfieValue.isScanned
    }


    fun encodedCurrentImage(): String {
        val selfie = selfieLiveData.value
        if (selfie != null) {
            val imgData = selfie.bitmap.toByteArray()
            return Base64.encodeToString(imgData, Base64.NO_WRAP)
        }
        val front = drivingFrontLiveData.value
        if (front != null) {
            val imgData = front.bitmap.toByteArray()
            return Base64.encodeToString(imgData, Base64.NO_WRAP)
        }
        return "0"
    }
}