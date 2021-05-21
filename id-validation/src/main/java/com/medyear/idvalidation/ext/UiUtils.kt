@file:Suppress("DEPRECATION")

package com.medyear.idvalidation.ext

import android.app.ProgressDialog
import android.content.Context
import com.medyear.idvalidation.R
import timber.log.Timber

object UiUtils {

    private var mProgressDialog: ProgressDialog? = null


    fun showProgressDialog(context: Context) {
        showProgressDialog(context, context.getString(R.string.loading))
    }

    fun showProgressDialog(context: Context?, message: String?) {
        if (mProgressDialog == null) {
            mProgressDialog = ProgressDialog(context, R.style.progressDialog)
        }
        mProgressDialog?.let { d ->
            if (!d.isShowing) {
                d.setMessage(message)
                d.setCancelable(false)
                d.show()
            }
        }
    }

    fun hideProgressDialog() {
        mProgressDialog?.let { d ->
            try {
                d.dismiss()
            } catch (ex: Exception) {
                Timber.e(ex)
            }
            mProgressDialog = null
        }
    }
}