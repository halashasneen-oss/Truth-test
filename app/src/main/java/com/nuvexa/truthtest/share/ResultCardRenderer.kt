package com.nuvexa.truthtest.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.nuvexa.truthtest.R
import java.io.File
import java.io.FileOutputStream

object ResultCardRenderer {
    fun render(context: Context, question: String, score: Int, secondScore: Int? = null): android.net.Uri {
        val bitmap = Bitmap.createBitmap(1080, 1350, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(context.getColor(R.color.bg_dark))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        paint.color = context.getColor(R.color.cyan); paint.textSize = 46f; canvas.drawText(context.getString(R.string.app_name), 540f, 120f, paint)
        paint.color = 0xFFF8FAFC.toInt(); paint.textSize = 54f; drawWrapped(canvas, question, paint, 540f, 260f, 900f, 70f)
        paint.color = context.getColor(R.color.pink); paint.textSize = 180f; canvas.drawText("$score%", 540f, 760f, paint)
        if (secondScore != null) { paint.color = context.getColor(R.color.cyan); paint.textSize = 76f; canvas.drawText("VS  $secondScore%", 540f, 880f, paint) }
        paint.color = 0xFFA7A7B5.toInt(); paint.textSize = 30f; paint.typeface = Typeface.DEFAULT; canvas.drawText(context.getString(R.string.entertainment_notice), 540f, 1160f, paint)
        paint.color = 0x66FFFFFF; paint.textSize = 28f; canvas.drawText("Truth Test • Nuvexa", 540f, 1280f, paint)
        val dir = File(context.cacheDir, "shares").apply { mkdirs() }
        val file = File(dir, "truth_test_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 95, it) }
        bitmap.recycle()
        return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }

    private fun drawWrapped(canvas: Canvas, text: String, paint: Paint, x: Float, y: Float, maxWidth: Float, lineHeight: Float) {
        val words = text.split(" "); var line = ""; var yy = y
        for (word in words) {
            val test = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(test) > maxWidth && line.isNotEmpty()) { canvas.drawText(line, x, yy, paint); yy += lineHeight; line = word } else line = test
        }
        if (line.isNotEmpty()) canvas.drawText(line, x, yy, paint)
    }
}
