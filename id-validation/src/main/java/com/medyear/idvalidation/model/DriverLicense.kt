package com.medyear.idvalidation.model

import com.google.android.gms.vision.barcode.Barcode
import java.io.Serializable

data class DriverLicense(
    val documentType: String? = null,
    val firstName: String? = null,
    val middleName: String? = null,
    val lastName: String? = null,
    val gender: String? = null,
    val addressStreet: String? = null,
    val addressCity: String? = null,
    val addressState: String? = null,
    val addressZip: String? = null,
    val licenseNumber: String? = null,
    val issueDate: String? = null,
    val expiryDate: String? = null,
    val birthDate: String? = null,
    val issuingCountry: String? = null,
) : Serializable {

    companion object {
        fun convert2(license: Barcode.DriverLicense) = DriverLicense(
            documentType = license.documentType,
            firstName = license.firstName,
            middleName = license.middleName,
            lastName = license.lastName,
            gender = license.gender,
            addressStreet = license.addressStreet,
            addressCity = license.addressCity,
            addressState = license.addressState,
            addressZip = license.addressZip,
            licenseNumber = license.licenseNumber,
            issueDate = license.issueDate,
            expiryDate = license.expiryDate,
            birthDate = license.birthDate,
            issuingCountry = license.issuingCountry
        )
    }
}
