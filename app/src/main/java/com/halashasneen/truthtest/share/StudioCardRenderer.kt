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
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object StudioCardRenderer {
    suspend fun render(
        context: Context,
        content: StudioShareContent,
        template: ShareTemplate,
        format: ShareFormat,
        showProfile: Boolean,
        showMode: Boolean,
        includeQr: Boolean
    ): Uri = withContext(Dispatchers.Default) {
        val appContext = context.applicationContext
        ShareCacheManager.cleanup(appContext)
        val bitmap = Bitmap.createBitmap(format.width, format.height, Bitmap.Config.ARGB_8888)
        try {
            draw(
                context = context,
                bitmap = bitmap,
                content = content,
                template = template,
                showProfile = showProfile,
                showMode = showMode,
                includeQr = includeQr
            )
            val dir = File(appContext.cacheDir, "shares").apply { mkdirs() }
            val file = File(
                dir,
                "studio_" + format.storageKey + "_" + System.currentTimeMillis() + ".jpg"
            )
            FileOutputStream(file).use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 93, it)
            }
            FileProvider.getUriForFile(
                appContext,
                appContext.packageName + ".files",
                file
            )
        } finally {
            bitmap.recycle()
        }
    }

    private fun draw(
        context: Context,
        bitmap: Bitmap,
        content: StudioShareContent,
        template: ShareTemplate,
        showProfile: Boolean,
        showMode: Boolean,
        includeQr: Boolean
    ) {
        val canvas = Canvas(bitmap)
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()
        val p = template.theme.palette
        val light = template == ShareTemplate.MINIMAL
        val primaryText = if (light) 0xFF17131F.toInt() else 0xFFF8F9FF.toInt()
        val secondaryText = if (light) 0xFF625E6D.toInt() else 0xC8FFFFFF.toInt()

        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                w,
                h,
                intArrayOf(
                    p.background,
                    blend(p.background, p.primary, if (light) 0.06f else 0.18f),
                    blend(p.background, p.secondary, if (light) 0.04f else 0.11f)
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w, h, bg)

        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = p.primary
            alpha = if (light) 16 else 46
        }
        canvas.drawOval(
            RectF(w * 0.08f, h * 0.02f, w * 0.92f, h * 0.28f),
            glow
        )

        val center = w / 2f
        val side = w * 0.08f
        val textWidth = (w - side * 2).toInt()

        drawText(
            canvas = canvas,
            text = "TRUTH TEST",
            x = side,
            y = h * 0.055f,
            width = textWidth,
            size = w * 0.037f,
            color = p.accent,
            bold = true,
            align = Layout.Alignment.ALIGN_CENTER
        )

        drawText(
            canvas = canvas,
            text = template.emoji + "  " + context.getString(template.labelRes).uppercase(),
            x = side,
            y = h * 0.095f,
            width = textWidth,
            size = w * 0.021f,
            color = secondaryText,
            bold = true,
            align = Layout.Alignment.ALIGN_CENTER
        )

        val questionY = if (h / w > 1.55f) h * 0.19f else h * 0.18f
        drawText(
            canvas = canvas,
            text = content.question,
            x = side,
            y = questionY,
            width = textWidth,
            size = w * if (h / w > 1.55f) 0.052f else 0.048f,
            color = primaryText,
            bold = true,
            align = Layout.Alignment.ALIGN_CENTER,
            maxLines = 4
        )

        val scoreCenterY = h * if (h / w > 1.55f) 0.48f else 0.51f
        val radius = w * 0.16f
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = w * 0.014f
            color = p.primary
        }
        canvas.drawCircle(center, scoreCenterY, radius, ring)
        val inner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = blend(p.background, p.primary, if (light) 0.08f else 0.20f)
        }
        canvas.drawCircle(center, scoreCenterY, radius * 0.88f, inner)

        val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            color = primaryText
            textSize = w * 0.10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(
            content.score.coerceIn(0, 100).toString() + "%",
            center,
            scoreCenterY + scorePaint.textSize * 0.34f,
            scorePaint
        )

        content.badge?.takeIf { it.isNotBlank() }?.let {
            drawText(
                canvas = canvas,
                text = it,
                x = side,
                y = scoreCenterY + radius + h * 0.025f,
                width = textWidth,
                size = w * 0.026f,
                color = p.accent,
                bold = true,
                align = Layout.Alignment.ALIGN_CENTER,
                maxLines = 1
            )
        }

        val metaParts = buildList {
            if (showMode && content.modeLabel.isNotBlank()) add(content.modeLabel)
            if (showProfile && !content.playerName.isNullOrBlank()) add(content.playerName!!)
        }
        if (metaParts.isNotEmpty()) {
            drawText(
                canvas = canvas,
                text = metaParts.joinToString("  •  "),
                x = side,
                y = h * 0.68f,
                width = textWidth,
                size = w * 0.025f,
                color = secondaryText,
                bold = true,
                align = Layout.Alignment.ALIGN_CENTER,
                maxLines = 2
            )
        }

        content.secondaryText?.takeIf { it.isNotBlank() }?.let {
            drawText(
                canvas = canvas,
                text = it,
                x = side,
                y = h * 0.725f,
                width = textWidth,
                size = w * 0.025f,
                color = secondaryText,
                bold = false,
                align = Layout.Alignment.ALIGN_CENTER,
                maxLines = 3
            )
        }

        content.cta?.takeIf { it.isNotBlank() }?.let {
            drawText(
                canvas = canvas,
                text = it,
                x = side,
                y = h * if (includeQr && content.challengeUri != null) 0.805f else 0.84f,
                width = textWidth,
                size = w * 0.031f,
                color = p.accent,
                bold = true,
                align = Layout.Alignment.ALIGN_CENTER,
                maxLines = 2
            )
        }

        if (includeQr && !content.challengeUri.isNullOrBlank()) {
            val qrSize = (w * 0.20f).toInt().coerceAtLeast(180)
            val qr = QrBitmapRenderer.render(content.challengeUri, qrSize)
            try {
                val left = center - qrSize / 2f
                val top = h * 0.86f - qrSize / 2f
                val plate = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF9F9FB.toInt() }
                val pad = w * 0.018f
                canvas.drawRoundRect(
                    RectF(left - pad, top - pad, left + qrSize + pad, top + qrSize + pad),
                    w * 0.025f,
                    w * 0.025f,
                    plate
                )
                canvas.drawBitmap(qr, left, top, null)
            } finally {
                qr.recycle()
            }
        }

        val watermark = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            color = if (light) 0x663B3644 else 0x66FFFFFF
            textSize = w * 0.022f
            typeface = Typeface.DEFAULT
        }
        canvas.drawText(
            context.getString(com.halashasneen.truthtest.R.string.p4_watermark),
            center,
            h * 0.965f,
            watermark
        )
    }

    private fun drawText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        width: Int,
        size: Float,
        color: Int,
        bold: Boolean,
        align: Layout.Alignment,
        maxLines: Int = Int.MAX_VALUE
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
            .setAlignment(align)
            .setIncludePad(false)
            .setMaxLines(maxLines)
            .setEllipsize(android.text.TextUtils.TruncateAt.END)
            .build()
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun blend(first: Int, second: Int, ratio: Float): Int {
        val r = ratio.coerceIn(0f, 1f)
        fun channel(shift: Int): Int {
            val a = first shr shift and 0xFF
            val b = second shr shift and 0xFF
            return (a + (b - a) * r).toInt().coerceIn(0, 255)
        }
        return (0xFF shl 24) or
            (channel(16) shl 16) or
            (channel(8) shl 8) or
            channel(0)
    }
}
