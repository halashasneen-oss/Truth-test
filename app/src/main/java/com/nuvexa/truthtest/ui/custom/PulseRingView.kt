package com.nuvexa.truthtest.ui.custom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.View
import com.nuvexa.truthtest.R
import kotlin.math.min

class PulseRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private var level = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        paint.shader = SweepGradient(
            w / 2f,
            h / 2f,
            intArrayOf(
                context.getColor(R.color.purple),
                context.getColor(R.color.pink),
                context.getColor(R.color.cyan),
                context.getColor(R.color.purple)
            ),
            null
        )
    }

    fun setAmplitude(rms: Float) {
        val target = (rms * 8f).coerceIn(0f, 1f)
        level = level * 0.62f + target * 0.38f
        alpha = 0.28f + level * 0.72f
        invalidate()
    }

    fun reset() {
        level = 0f
        alpha = 0.28f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val maxRadius = min(width, height) / 2f - 5f * density
        val radius = maxRadius * (0.72f + level * 0.28f)
        paint.strokeWidth = (2.5f + level * 4f) * density
        canvas.drawCircle(cx, cy, radius, paint)

        paint.strokeWidth = 1.2f * density
        paint.alpha = (70 + level * 80).toInt().coerceIn(0, 255)
        canvas.drawCircle(cx, cy, maxRadius * (0.58f + level * 0.18f), paint)
        paint.alpha = 255
    }
}
