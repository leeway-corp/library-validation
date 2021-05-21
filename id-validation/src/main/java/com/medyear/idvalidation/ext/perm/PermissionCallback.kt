package com.medyear.idvalidation.ext.perm

internal interface PermissionCallback {

    /**
     * Pass request granted status i.e true or false
     */
    fun onPermissionRequest(granted: Boolean)
}