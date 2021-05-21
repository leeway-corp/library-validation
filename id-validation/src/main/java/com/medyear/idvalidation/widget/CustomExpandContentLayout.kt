package com.medyear.idvalidation.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.medyear.idvalidation.R
import com.medyear.idvalidation.ext.compatColor
import com.medyear.idvalidation.ext.compatDrawable

@Suppress("FunctionName")
internal class CustomExpandContentLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private var mScanned = false
    private var mScannedDrawable: Drawable? = null
    private var mNotScannedDrawable: Drawable? = null

    //--------
    private var centerImage: ImageView? = null
    private var centerButton: Button? = null

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.CustomExpandContentLayout, 0, 0)
            .apply {
                try {
                    mScannedDrawable = getDrawable(R.styleable.CustomExpandContentLayout_ce_scanned)
                    mNotScannedDrawable = getDrawable(R.styleable.CustomExpandContentLayout_ce_not_scanned)
                } finally {
                    recycle()
                }
            }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        this.centerImage = findViewWithTag("centerImage") as? ImageView
        this.centerButton = findViewWithTag("centerButton") as? Button
        _updateUI()
    }

    private fun _updateUI() = if (mScanned) _updateAsScanned() else _updateAsNotScanned()

    private fun _updateAsScanned() {
        this.centerImage?.setImageDrawable(mScannedDrawable)
        this.centerButton?.setBackgroundResource(R.drawable.rounded_white_bg_green_borders)
        this.centerButton?.setText(R.string.scanned)
    }

    private fun _updateAsNotScanned() {
        this.centerImage?.setImageDrawable(mNotScannedDrawable)
        this.centerButton?.setBackgroundResource(R.drawable.rounded_white_bg_blue_borders)
        this.centerButton?.setText(R.string.scan)
    }

    fun setScanned(scanned: Boolean) {
        this.mScanned = scanned
        _updateUI()
    }
}