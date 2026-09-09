package com.nuvexa.truthtest.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.nuvexa.truthtest.R
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

object ResultCardRenderer {
    fun render(
        context: Context,
        question: String,
        score: Int,
        firstScore: Int? = null,
        secondScore: Int? = null,
        waveform: List<Float> = emptyList()
    ): android.net.Uri {
        val bitmap = Bitmap.createBitmap(1080, 1350, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val background = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 1080f, 1350f,
                intArrayOf(context.getColor(R.color.bg_dark), 0xFF15101F.toInt(), 0xFF0B1420.toInt()),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, 1080f, 1350f, background)

        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(120f, 80f, 960f, 420f, context.getColor(R.color.purple), context.getColor(R.color.pink), Shader.TileMode.CLAMP)
            alpha = 46
        }
        canvas.drawOval(RectF(90f, 30f, 990f, 450f), glow)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        paint.color = context.getColor(R.color.cyan)
        paint.textSize = 44f
        canvas.drawText(context.getString(R.string.app_name), 540f, 112f, paint)

        paint.color = 0xB3FFFFFF.toInt()
        paint.textSize = 25f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText(context.getString(R.string.share_card_badge), 540f, 158f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = 0xFFF8FAFC.toInt()
        paint.textSize = 52f
        drawWrapped(canvas, question, paint, 540f, 270f, 900f, 66f)

        drawWaveform(canvas, context, waveform, 160f, 490f, 760f, 150f)

        paint.shader = LinearGradient(350f, 0f, 730f, 0f, context.getColor(R.color.purple), context.getColor(R.color.pink), Shader.TileMode.CLAMP)
        paint.textSize = 184f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("$score%", 540f, 850f, paint)
        paint.shader = null

        paint.color = 0xCCFFFFFF.toInt()
        paint.textSize = 31f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText(context.getString(R.string.truth_score), 540f, 905f, paint)

        if (firstScore != null && secondScore != null) {
            drawDuelPanel(canvas, context, firstScore, secondScore, paint)
        }

        paint.color = 0x99FFFFFF.toInt()
        paint.textSize = 27f
        paint.typeface = Typeface.DEFAULT
        drawWrapped(canvas, context.getString(R.string.entertainment_notice), paint, 540f, 1135f, 900f, 37f)

        paint.color = 0x66FFFFFF
        paint.textSize = 28f
        canvas.drawText(context.getString(R.string.watermark), 540f, 1285f, paint)

        val dir = File(context.cacheDir, "shares").apply { mkdirs() }
        val file = File(dir, "truth_test_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 96, it) }
        bitmap.recycle()
        return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }

    private fun drawDuelPanel(canvas: Canvas, context: Context, firstScore: Int, secondScore: Int, paint: Paint) {
        val panel = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x16FFFFFF }
        canvas.drawRoundRect(RectF(135f, 950f, 945f, 1065f), 34f, 34f, panel)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 42f
        paint.color = context.getColor(R.color.purple)
        canvas.drawText("${context.getString(R.string.player_one)}  $firstScore%", 350f, 1018f, paint)
        paint.color = context.getColor(R.color.cyan)
        canvas.drawText("${context.getString(R.string.player_two)}  $secondScore%", 730f, 1018f, paint)
        paint.color = 0xE6FFFFFF.toInt()
        paint.textSize = 28f
        val winner = when {
            firstScore == secondScore -> "🤝"
            firstScore > secondScore -> "🏆 ${context.getString(R.string.player_one)}"
            else -> "🏆 ${context.getString(R.string.player_two)}"
        }
        canvas.drawText(winner, 540f, 1052f, paint)
    }

    private fun drawWaveform(
        canvas: Canvas,
        context: Context,
        waveform: List<Float>,
        left: Float,
        top: Float,
        width: Float,
        height: Float
    ) {
        val values = if (waveform.isNotEmpty()) waveform else listOf(0.12f, 0.25f, 0.18f, 0.42f, 0.3f, 0.55f, 0.24f, 0.37f, 0.2f)
        val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 8f
            strokeCap = Paint.Cap.ROUND
            shader = LinearGradient(left, 0f, left + width, 0f, context.getColor(R.color.purple), context.getColor(R.color.cyan), Shader.TileMode.CLAMP)
        }
        val center = top + height / 2f
        val step = width / max(values.size - 1, 1)
        values.forEachIndexed { index, value ->
            val amp = value.coerceIn(0.04f, 1f)
            val half = amp * height * 0.46f
            val x = left + index * step
            canvas.drawLine(x, center - half, x, center + half, wavePaint)
        }
    }

    private fun drawWrapped(canvas: Canvas, text: String, paint: Paint, x: Float, y: Float, maxWidth: Float, lineHeight: Float) {
        val words = text.split(" ")
        var line = ""
        var yy = y
        for (word in words) {
            val test = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(test) > maxWidth && line.isNotEmpty()) {
                canvas.drawText(line, x, yy, paint)
                yy += lineHeight
                line = word
            } else {
                line = test
            }
        }
        if (line.isNotEmpty()) canvas.drawText(line, x, yy, paint)
    }
}
