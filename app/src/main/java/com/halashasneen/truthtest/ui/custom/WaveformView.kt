package com.halashasneen.truthtest.ui.custom

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import com.halashasneen.truthtest.R
import kotlin.math.max

class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = resources.displayMetrics.density * 2f
        strokeCap = Paint.Cap.ROUND
        color = context.getColor(R.color.p2_cyan)
    }
    private val values = ArrayDeque<Float>()
    private val maxPoints = 80

    fun addAmplitude(value: Float) {
        val boosted = (value * 7f).coerceIn(0.03f, 1f)
        if (values.size >= maxPoints) values.removeFirst()
        values.addLast(boosted)
        invalidate()
    }

    fun snapshot(): List<Float> = values.toList()

    fun setValues(newValues: List<Float>) {
        values.clear()
        newValues.takeLast(maxPoints).forEach { values.addLast(it.coerceIn(0.03f, 1f)) }
        invalidate()
    }

    fun reset() {
        values.clear()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0) {
            paint.shader = LinearGradient(
                0f,
                0f,
                w.toFloat(),
                0f,
                context.getColor(R.color.p2_purple),
                context.getColor(R.color.p2_cyan),
                Shader.TileMode.CLAMP
            )
        }
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
