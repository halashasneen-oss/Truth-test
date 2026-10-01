package com.halashasneen.truthtest.share

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QrBitmapRenderer {
    fun render(value: String, size: Int = 640): Bitmap {
        require(value.length <= 2_200) { "QR value is too large" }
        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
        )
        val matrix = QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, size, size, hints)
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
            val pixels = IntArray(size * size)
            var offset = 0
            for (y in 0 until size) {
                for (x in 0 until size) {
                    pixels[offset++] = if (matrix[x, y]) Color.BLACK else Color.WHITE
                }
            }
            setPixels(pixels, 0, size, 0, 0, size, size)
        }
    }
}
