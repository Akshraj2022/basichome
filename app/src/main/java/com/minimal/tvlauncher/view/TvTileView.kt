package com.minimal.tvlauncher.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.minimal.tvlauncher.R

/**
 * Standardized Tile Component across all sections.
 * Clean, logo-centric display without text labels below.
 * Smooth 150ms scale (1.08x) and sibling dimming (0.70x alpha) on focus.
 */
class TvTileView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val tileBox: FrameLayout
    private val iconView: ImageView
    private val labelView: TextView

    private val decelerateInterpolator = DecelerateInterpolator(1.5f)

    var onTileLongClickListener: ((TvTileView) -> Unit)? = null

    init {
        orientation = VERTICAL
        clipChildren = false
        clipToPadding = false
        isFocusable = true
        isFocusableInTouchMode = true

        LayoutInflater.from(context).inflate(R.layout.view_tv_tile, this, true)
        tileBox = findViewById(R.id.tile_box)
        iconView = findViewById(R.id.tile_icon)
        labelView = findViewById(R.id.tile_label)

        setOnFocusChangeListener { _, hasFocus ->
            handleFocusChange(hasFocus)
        }

        setOnLongClickListener {
            onTileLongClickListener?.invoke(this)
            true
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            onTileLongClickListener?.invoke(this)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun handleFocusChange(hasFocus: Boolean) {
        if (hasFocus) {
            bringToFront()
            (parent as? View)?.invalidate()
        }

        val targetScale = if (hasFocus) 1.08f else 1.0f
        animate()
            .scaleX(targetScale)
            .scaleY(targetScale)
            .setDuration(150)
            .setInterpolator(decelerateInterpolator)
            .start()

        // Dim other sibling tiles in the same row container to 70% alpha
        val parentViewGroup = parent as? ViewGroup
        if (parentViewGroup != null) {
            for (i in 0 until parentViewGroup.childCount) {
                val sibling = parentViewGroup.getChildAt(i)
                if (sibling != this) {
                    val targetAlpha = if (hasFocus) 0.70f else 1.0f
                    sibling.animate()
                        .alpha(targetAlpha)
                        .setDuration(150)
                        .setInterpolator(decelerateInterpolator)
                        .start()
                } else {
                    sibling.animate()
                        .alpha(1.0f)
                        .setDuration(150)
                        .setInterpolator(decelerateInterpolator)
                        .start()
                }
            }
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density + 0.5f).toInt()

    fun setTileData(icon: Drawable?, labelText: String) {
        val pad = dpToPx(6)
        iconView.setPadding(pad, pad, pad, pad)
        iconView.setImageDrawable(icon)
        contentDescription = labelText
    }

    fun setTileIcon(resId: Int) {
        val pad = if (resId == R.drawable.ic_settings) dpToPx(6) else dpToPx(26)
        iconView.setPadding(pad, pad, pad, pad)
        iconView.setImageResource(resId)
    }

    fun setTileLabel(labelText: String) {
        contentDescription = labelText
    }

    fun updateTheme(isLight: Boolean) {
        tileBox.setBackgroundResource(if (isLight) R.drawable.bg_tile_box_light else R.drawable.bg_tile_box)
    }
}
