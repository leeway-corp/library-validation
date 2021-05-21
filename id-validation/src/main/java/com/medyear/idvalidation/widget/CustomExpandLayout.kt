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
internal class CustomExpandLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private var mScanned = false

    //--------
    private var indexBox: FrameLayout? = null
    private var indexText: TextView? = null
    private var indexIcon: ImageView? = null


    override fun onFinishInflate() {
        super.onFinishInflate()
        setOnClickListener(this::_onRootClickListener)
        this.indexBox = findViewWithTag("indexBox") as? FrameLayout
        this.indexText = findViewWithTag("indexText") as? TextView
        this.indexIcon = findViewWithTag("indexIcon") as? ImageView

        this.indexText?.setTextColor(compatColor(R.color.white))
        _updateUI()
    }

    private fun _updateUI() = if (mScanned) _updateAsScanned() else _updateAsNotScanned()

    private fun _updateAsScanned() {
        this.indexBox?.setBackgroundResource(R.drawable.round_green_bg)
        this.indexText?.visibility = GONE
        this.indexIcon?.visibility = VISIBLE
    }

    private fun _updateAsNotScanned() {
        this.indexBox?.setBackgroundResource(R.drawable.round_blue_bg)
        this.indexText?.visibility = VISIBLE
        this.indexIcon?.visibility = GONE
    }

    fun expandIdLayout() {
        val isCollapsed = (this.tag as String).toBoolean()
        if (isCollapsed) this.performClick()
    }

    fun collapseIdLayout() {
        val isCollapsed = (this.tag as String).toBoolean()
        if (!isCollapsed) this.performClick()
    }

    fun setScanned(scanned: Boolean) {
        this.mScanned = scanned
        _updateUI()
    }

    private fun _onRootClickListener(v: View) {
        val isCollapsed = (v.tag as String).toBoolean().not()
        (v as ViewGroup).getChildAt(2).rotation = if (isCollapsed) 90f else 270f
        (v.parent as ViewGroup).getChildAt(1).visibility =
            if (isCollapsed) View.GONE else View.VISIBLE
        v.tag = isCollapsed.toString()
    }
}