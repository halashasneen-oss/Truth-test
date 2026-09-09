package com.nuvexa.truthtest.ui.statistics

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import com.nuvexa.truthtest.R
import kotlin.math.max

class ScoreTrendView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val density = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity
    private var scores: List<Int> = emptyList()

    private val baselinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.app_text_secondary)
        alpha = 70
        strokeWidth = density
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.app_text_secondary)
        textAlign = Paint.Align.CENTER
        textSize = 11f * scaledDensity
    }

    fun setScores(values: List<Int>) {
        scores = values.takeLast(7).map { it.coerceIn(0, 100) }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (scores.isEmpty()) return

        val left = 18f * density
        val right = width - 18f * density
        val top = 30f * density
        val bottom = height - 24f * density
        val availableWidth = max(1f, right - left)
        val chartHeight = max(1f, bottom - top)

        canvas.drawLine(left, bottom, right, bottom, baselinePaint)

        val barWidth = availableWidth / (scores.size * 1.65f)
        val gap = if (scores.size > 1) {
            (availableWidth - barWidth * scores.size) / (scores.size - 1)
        } else 0f
        val radius = 9f * density

        scores.forEachIndexed { index, score ->
            val x = left + index * (barWidth + gap)
            val barHeight = chartHeight * (score / 100f)
            val y = bottom - barHeight
            val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f,
                    y,
                    0f,
                    bottom,
                    context.getColor(R.color.cyan),
                    context.getColor(R.color.purple),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(RectF(x, y, x + barWidth, bottom), radius, radius, barPaint)
            canvas.drawText("$score%", x + barWidth / 2f, max(14f * density, y - 7f * density), labelPaint)
        }
    }
}
