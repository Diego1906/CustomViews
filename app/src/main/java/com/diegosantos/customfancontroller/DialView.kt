package com.diegosantos.customfancontroller

import android.content.Context
import android.graphics.*
import android.support.annotation.ColorInt
import android.support.annotation.StringRes
import android.support.v4.view.AccessibilityDelegateCompat
import android.support.v4.view.ViewCompat
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat
import android.util.AttributeSet
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private enum class FanSpeed(@StringRes val label: Int) {
    OFF(R.string.fan_off),
    LOW(R.string.fan_low),
    MEDIUM(R.string.fan_medium),
    HIGH(R.string.fan_high);

    fun next() = when (this) {
        OFF -> LOW
        LOW -> MEDIUM
        MEDIUM -> HIGH
        HIGH -> OFF
    }
}

private const val RADIUS_OFFSET_LABEL = 30
private const val RADIUS_OFFSET_INDICATOR = -35

class DialView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var radius = 0.0f                   // Radius of the circle.
    private var fanSpeed = FanSpeed.OFF         // The active selection.

    // Cache AttributeSet
    @ColorInt
    private var fanSpeedLowColor = 0

    @ColorInt
    private var fanSpeedMediumColor = 0

    @ColorInt
    private var fanSpeedMaxColor = 0

    // position variable which will be used to draw label and indicator circle position
    private val pointPosition: PointF = PointF(0.0f, 0.0f)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        textSize = 55.0f
        typeface = Typeface.create("", Typeface.BOLD)
    }

    init {
        isClickable = true

        /* Get AttributeSet values and cache them */
        context.obtainStyledAttributes(attrs, R.styleable.DialView).also { typedArray ->
            fanSpeedLowColor = typedArray.getColor(R.styleable.DialView_fanColor1, 0)
            fanSpeedMediumColor = typedArray.getColor(R.styleable.DialView_fanColor2, 0)
            fanSpeedMaxColor = typedArray.getColor(R.styleable.DialView_fanColor3, 0)
            typedArray.recycle()
        }

        updateContentDescription()

        ViewCompat.setAccessibilityDelegate(this, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View?,
                info: AccessibilityNodeInfoCompat?
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                val customClick = AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    AccessibilityNodeInfo.ACTION_CLICK,
                    context.getString(if (fanSpeed != FanSpeed.HIGH) R.string.change else R.string.reset)
                )
                info?.addAction(customClick)
            }
        })
    }

    override fun performClick(): Boolean {
        if (super.performClick()) return true
        fanSpeed = fanSpeed.next()
        updateContentDescription()
        invalidate()
        return true
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        radius = (min(width, height) / 2.0 * 0.8).toFloat()
    }

    private fun PointF.computeXYForSpeed(pos: FanSpeed, radius: Float) {
        // Angles are in radians.
        val startAngle = Math.PI * (9 / 8.0)
        val angle = startAngle + pos.ordinal * (Math.PI / 4)
        x = (radius * cos(angle)).toFloat() + width / 2
        y = (radius * sin(angle)).toFloat() + height / 2
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        // Set dial background color to green if selection not off
        paint.color = when (fanSpeed) {
            FanSpeed.OFF -> Color.GRAY
            FanSpeed.LOW -> fanSpeedLowColor
            FanSpeed.MEDIUM -> fanSpeedMediumColor
            FanSpeed.HIGH -> fanSpeedMaxColor
        }

        // Draw the dial
        canvas?.drawCircle((width / 2).toFloat(), (height / 2).toFloat(), radius, paint)

        // Draw the indicator circle
        val markerRadius = radius + RADIUS_OFFSET_INDICATOR
        pointPosition.computeXYForSpeed(fanSpeed, markerRadius)
        paint.color = Color.BLACK
        canvas?.drawCircle(pointPosition.x, pointPosition.y, radius / 12, paint)

        // Draw the text labels
        val labelRadius = radius + RADIUS_OFFSET_LABEL
        for (i in FanSpeed.values()) {
            pointPosition.computeXYForSpeed(i, labelRadius)
            val label = resources.getString(i.label)
            canvas?.drawText(label, pointPosition.x, pointPosition.y, paint)
        }
    }

    private fun updateContentDescription() {
        contentDescription = resources.getString(fanSpeed.label)
    }
}