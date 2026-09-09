package com.nuvexa.truthtest.ui.custom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.nuvexa.truthtest.R
import kotlin.math.max

class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = resources.displayMetrics.density * 2f
        strokeCap = Paint.Cap.ROUND
        color = context.getColor(R.color.cyan)
    }
    private val values = ArrayDeque<Float>()
    private val maxPoints = 80

    fun addAmplitude(value: Float) {
        val boosted = (value * 7f).coerceIn(0.03f, 1f)
        if (values.size >= maxPoints) values.removeFirst()
        values.addLast(boosted)
        invalidate()
    }

    fun reset() {
        values.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (values.isEmpty()) return
        val centerY = height / 2f
        val step = width.toFloat() / max(maxPoints - 1, 1)
        values.forEachIndexed { index, amp ->
            val x = index * step
            val half = amp * height * 0.42f
            canvas.drawLine(x, centerY - half, x, centerY + half, paint)
        }
    }
}
