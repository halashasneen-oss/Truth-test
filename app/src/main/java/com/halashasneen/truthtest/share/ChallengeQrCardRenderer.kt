package com.halashasneen.truthtest.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.halashasneen.truthtest.R
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ChallengeQrCardRenderer {
    suspend fun render(
        context: Context,
        challengeUri: String,
        title: String,
        question: String?
    ): Uri = withContext(Dispatchers.Default) {
        val app = context.applicationContext
        val bitmap = Bitmap.createBitmap(1080, 1350, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f,
                    0f,
                    1080f,
                    1350f,
                    intArrayOf(
                        0xFF080A0F.toInt(),
                        0xFF171006.toInt(),
                        0xFF130B20.toInt()
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, 1080f, 1350f, bg)

            val header = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER
                color = 0xFFF6B73C.toInt()
                textSize = 42f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("TRUTH TEST", 540f, 105f, header)

            drawBlock(canvas, title, 100f, 160f, 880, 54f, 0xFFF8F9FF.toInt(), true, 2)

            question?.takeIf { it.isNotBlank() }?.let {
                drawBlock(canvas, it, 100f, 270f, 880, 38f, 0xD9FFFFFF.toInt(), false, 3)
            }

            val qr = QrBitmapRenderer.render(challengeUri, 620)
            try {
                val plate = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF9F9FB.toInt() }
                canvas.drawRoundRect(RectF(190f, 480f, 890f, 1180f), 50f, 50f, plate)
                canvas.drawBitmap(qr, null, RectF(230f, 520f, 850f, 1140f), null)
            } finally {
                qr.recycle()
            }

            val footer = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER
                color = 0x99FFFFFF.toInt()
                textSize = 26f
            }
            canvas.drawText(context.getString(R.string.p4_qr_body), 540f, 1240f, footer)
            canvas.drawText(context.getString(R.string.p4_watermark), 540f, 1300f, footer)

            val dir = File(app.cacheDir, "shares").apply { mkdirs() }
            val file = File(dir, "challenge_qr_" + System.currentTimeMillis() + ".jpg")
            FileOutputStream(file).use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 94, it)
            }
            FileProvider.getUriForFile(app, app.packageName + ".files", file)
        } finally {
            bitmap.recycle()
        }
    }

    private fun drawBlock(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        width: Int,
        size: Float,
        color: Int,
        bold: Boolean,
        maxLines: Int
    ) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = Typeface.create(
                Typeface.DEFAULT,
                if (bold) Typeface.BOLD else Typeface.NORMAL
            )
        }
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(false)
            .setMaxLines(maxLines)
            .setEllipsize(android.text.TextUtils.TruncateAt.END)
            .build()
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }
}
