package com.medyear.library.validation

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.medyear.idvalidation.activity.CustomIdValidationActivity

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        jumpTo()
    }

    fun onButtonCLicked(view: View) = jumpTo()

    private fun jumpTo() {
        startActivityForResult(
            Intent(this, CustomIdValidationActivity::class.java),
            REQ_CODE_VALIDATION
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.v("TAG", "---requestCode ${requestCode == REQ_CODE_VALIDATION}")
        Log.v("TAG", "---resultCode ${resultCode == Activity.RESULT_OK}")
        if (requestCode == REQ_CODE_VALIDATION) {
            if (resultCode == Activity.RESULT_OK) {
                val result = data?.getSerializableExtra("data")
                Log.v("TAG", "---result $result")
            }
        }
    }

    companion object {
        private const val REQ_CODE_VALIDATION = 102
    }
}