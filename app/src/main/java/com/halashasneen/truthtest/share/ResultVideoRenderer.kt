package com.halashasneen.truthtest.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.opengl.EGL14
import android.opengl.EGLExt
import android.opengl.GLES20
import android.opengl.GLUtils
import android.view.Surface
import androidx.core.content.FileProvider
import com.halashasneen.truthtest.R
import com.halashasneen.truthtest.data.PlayerRanking
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

object ResultVideoRenderer {
    private const val WIDTH = 720
    private const val HEIGHT = 1280
    private const val BIT_RATE = 2_500_000

    suspend fun render(
        context: Context,
        question: String,
        score: Int,
        firstScore: Int? = null,
        secondScore: Int? = null,
        groupScores: List<Int> = emptyList(),
        waveform: List<Float> = emptyList()
    ): android.net.Uri = withContext(Dispatchers.Default) {
        val appContext = context.applicationContext
        val dir = File(appContext.cacheDir, "shares").apply { mkdirs() }
        cleanupOldVideos(dir)
        val output = File(dir, "truth_test_${System.currentTimeMillis()}.mp4")
        encode(appContext, output, question, score, firstScore, secondScore, groupScores, waveform)
        FileProvider.getUriForFile(appContext, "${appContext.packageName}.files", output)
    }

    private fun encode(
        context: Context,
        output: File,
        question: String,
        score: Int,
        firstScore: Int?,
        secondScore: Int?,
        groupScores: List<Int>,
        waveform: List<Float>
    ) {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, WIDTH, HEIGHT).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            setInteger(MediaFormat.KEY_FRAME_RATE, ResultVideoTimeline.FPS)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val codecSurface = encoder.createInputSurface()
        val inputSurface = CodecInputSurface(codecSurface)
        val muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val bufferInfo = MediaCodec.BufferInfo()
        var trackIndex = -1
        var muxerStarted = false
        var reachedEos = false

        fun drain(endOfStream: Boolean) {
            var idleCount = 0
            while (true) {
                val status = encoder.dequeueOutputBuffer(bufferInfo, 10_000)
                when {
                    status == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        if (!endOfStream || idleCount++ > 100) return
                    }
                    status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        check(!muxerStarted) { "Video format changed twice" }
                        trackIndex = muxer.addTrack(encoder.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                    status >= 0 -> {
                        val encoded = encoder.getOutputBuffer(status)
                            ?: error("Encoder returned an empty output buffer")
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            bufferInfo.size = 0
                        }
                        if (bufferInfo.size > 0) {
                            check(muxerStarted) { "Muxer has not started" }
                            encoded.position(bufferInfo.offset)
                            encoded.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(trackIndex, encoded, bufferInfo)
                        }
                        reachedEos = bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        encoder.releaseOutputBuffer(status, false)
                        if (reachedEos) return
                    }
                }
            }
        }

        val frameBitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val frameCanvas = Canvas(frameBitmap)

        try {
            inputSurface.makeCurrent()
            encoder.start()
            for (frame in 0 until ResultVideoTimeline.frameCount) {
                val progress = ResultVideoTimeline.progress(frame)
                drawFrame(
                    context = context,
                    canvas = frameCanvas,
                    question = question,
                    score = score,
                    firstScore = firstScore,
                    secondScore = secondScore,
                    groupScores = groupScores,
                    waveform = waveform,
                    progress = progress
                )
                inputSurface.drawBitmap(frameBitmap)
                inputSurface.setPresentationTime(frame * 1_000_000_000L / ResultVideoTimeline.FPS)
                inputSurface.swapBuffers()
                drain(false)
            }
            encoder.signalEndOfInputStream()
            drain(true)
            check(reachedEos) { "Video encoder did not reach end of stream" }
        } finally {
            frameBitmap.recycle()
            runCatching { inputSurface.release() }
            runCatching { codecSurface.release() }
            runCatching { encoder.stop() }
            runCatching { encoder.release() }
            if (muxerStarted) runCatching { muxer.stop() }
            runCatching { muxer.release() }
        }
    }

    private fun drawFrame(
        context: Context,
        canvas: Canvas,
        question: String,
        score: Int,
        firstScore: Int?,
        secondScore: Int?,
        groupScores: List<Int>,
        waveform: List<Float>,
        progress: Float
    ) {
        val background = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(),
                intArrayOf(context.getColor(R.color.bg_dark), 0xFF171022.toInt(), 0xFF071722.toInt()),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), background)

        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(80f, 60f, 640f, 360f, context.getColor(R.color.purple), context.getColor(R.color.pink), Shader.TileMode.CLAMP)
            alpha = 42
        }
        canvas.drawOval(RectF(40f, 10f, 680f, 390f), glow)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val questionAlpha = (ResultVideoTimeline.questionAlpha(progress) * 255).roundToInt().coerceIn(0, 255)
        paint.color = context.getColor(R.color.cyan)
        paint.textSize = 34f
        paint.alpha = questionAlpha
        canvas.drawText(context.getString(R.string.app_name), WIDTH / 2f, 92f, paint)

        paint.color = 0xB3FFFFFF.toInt()
        paint.textSize = 19f
        paint.typeface = Typeface.DEFAULT
        paint.alpha = questionAlpha
        canvas.drawText(context.getString(R.string.share_card_badge), WIDTH / 2f, 130f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = 0xFFF8FAFC.toInt()
        paint.textSize = 39f
        paint.alpha = questionAlpha
        drawWrapped(canvas, question, paint, WIDTH / 2f, 210f, 620f, 50f)
        paint.alpha = 255

        val reveal = ResultVideoTimeline.waveformReveal(progress)
        drawWaveform(canvas, context, waveform, 90f, 390f, 540f, 130f, reveal)

        val animatedScore = (score * ResultVideoTimeline.scoreProgress(progress)).roundToInt().coerceIn(0, 100)
        drawScoreMeter(canvas, context, animatedScore)

        val resultAlpha = (ResultVideoTimeline.resultAlpha(progress) * 255).roundToInt().coerceIn(0, 255)
        val isGroup = groupScores.size in 3..4
        when {
            isGroup -> drawGroupPanel(canvas, context, groupScores, progress)
            firstScore != null && secondScore != null -> drawDuelPanel(canvas, context, firstScore, secondScore, resultAlpha)
            else -> {
                paint.color = 0xE6FFFFFF.toInt()
                paint.alpha = resultAlpha
                paint.textSize = 32f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(scoreLabel(context, score), WIDTH / 2f, 900f, paint)
            }
        }

        val footerAlpha = if (isGroup) {
            (ResultVideoTimeline.groupWinnerAlpha(progress) * 255).roundToInt().coerceIn(0, 255)
        } else resultAlpha
        paint.alpha = footerAlpha
        paint.color = context.getColor(R.color.cyan)
        paint.textSize = if (isGroup) 22f else 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(context.getString(R.string.share_video_cta), WIDTH / 2f, if (isGroup) 1100f else 1030f, paint)

        paint.color = 0x8FFFFFFF.toInt()
        paint.textSize = 17f
        paint.typeface = Typeface.DEFAULT
        drawWrapped(
            canvas,
            context.getString(R.string.entertainment_notice),
            paint,
            WIDTH / 2f,
            if (isGroup) 1160f else 1120f,
            610f,
            25f
        )

        paint.color = 0x72FFFFFF
        paint.textSize = 20f
        canvas.drawText(context.getString(R.string.watermark), WIDTH / 2f, 1225f, paint)
        paint.alpha = 255
    }

    private fun drawScoreMeter(canvas: Canvas, context: Context, score: Int) {
        val centerX = WIDTH / 2f
        val centerY = 700f
        val radius = 142f
        val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 18f
            color = 0x24FFFFFF
        }
        canvas.drawCircle(centerX, centerY, radius, track)

        val meter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 18f
            shader = SweepGradient(
                centerX,
                centerY,
                intArrayOf(context.getColor(R.color.purple), context.getColor(R.color.pink), context.getColor(R.color.cyan), context.getColor(R.color.purple)),
                null
            )
        }
        canvas.drawArc(RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius), -90f, score * 3.6f, false, meter)

        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = 0xFFF8FAFC.toInt()
            textSize = 92f
        }
        canvas.drawText("$score%", centerX, centerY + 30f, text)
        text.typeface = Typeface.DEFAULT
        text.textSize = 21f
        text.color = 0xAFFFFFFF.toInt()
        canvas.drawText(context.getString(R.string.truth_score), centerX, centerY + 72f, text)
    }

    private fun drawDuelPanel(canvas: Canvas, context: Context, firstScore: Int, secondScore: Int, alpha: Int) {
        val panel = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x18FFFFFF; this.alpha = alpha }
        canvas.drawRoundRect(RectF(70f, 875f, 650f, 980f), 28f, 28f, panel)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 27f
            this.alpha = alpha
        }
        paint.color = context.getColor(R.color.purple)
        canvas.drawText("${context.getString(R.string.player_one)}  $firstScore%", 230f, 940f, paint)
        paint.color = context.getColor(R.color.cyan)
        canvas.drawText("${context.getString(R.string.player_two)}  $secondScore%", 490f, 940f, paint)

        paint.color = 0xE6FFFFFF.toInt()
        paint.textSize = 23f
        val winner = when {
            firstScore == secondScore -> "🤝"
            firstScore > secondScore -> "🏆 ${context.getString(R.string.player_one)}"
            else -> "🏆 ${context.getString(R.string.player_two)}"
        }
        canvas.drawText(winner, WIDTH / 2f, 1008f, paint)
    }

    private fun drawGroupPanel(
        canvas: Canvas,
        context: Context,
        groupScores: List<Int>,
        progress: Float
    ) {
        val safeScores = groupScores.take(4).map { it.coerceIn(0, 100) }
        if (safeScores.size < 3) return

        val standings = PlayerRanking.standings(safeScores)
        val winners = PlayerRanking.winners(safeScores)
        val panelAlpha = (
            max(
                ResultVideoTimeline.groupRowAlpha(progress, standings.lastIndex.coerceAtLeast(0)),
                ResultVideoTimeline.groupWinnerAlpha(progress)
            ) * 255
        ).roundToInt().coerceIn(0, 255)

        val outer = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x18FFFFFF
            alpha = panelAlpha
        }
        canvas.drawRoundRect(RectF(62f, 850f, 658f, 1070f), 30f, 30f, outer)

        val winnerAlpha = ResultVideoTimeline.groupWinnerAlpha(progress)
        val winnerScale = ResultVideoTimeline.groupWinnerScale(progress)
        val headline = if (winners.size == 1) {
            context.getString(
                R.string.group_winner_format,
                context.getString(R.string.player_number_format, winners.first())
            )
        } else {
            context.getString(
                R.string.group_tie_format,
                winners.joinToString(" • ") { context.getString(R.string.player_number_format, it) }
            )
        }
        val winnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 27f * winnerScale
            color = context.getColor(R.color.cyan)
            alpha = (winnerAlpha * 255).roundToInt().coerceIn(0, 255)
        }
        canvas.drawText(headline, WIDTH / 2f, 890f, winnerPaint)

        standings.forEachIndexed { index, standing ->
            val rowAlpha = ResultVideoTimeline.groupRowAlpha(progress, index)
            val alpha = (rowAlpha * 255).roundToInt().coerceIn(0, 255)
            val slide = (1f - rowAlpha) * 34f
            val centerY = 930f + index * 38f
            val rowLeft = 84f + slide
            val rowRight = 636f + slide

            val rowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (standing.rank == 1) context.getColor(R.color.purple) else 0x22FFFFFF
                this.alpha = if (standing.rank == 1) (alpha * 0.38f).roundToInt() else (alpha * 0.18f).roundToInt()
            }
            canvas.drawRoundRect(RectF(rowLeft, centerY - 25f, rowRight, centerY + 9f), 17f, 17f, rowPaint)

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, if (standing.rank == 1) Typeface.BOLD else Typeface.NORMAL)
                textSize = if (standing.rank == 1) 22f else 20f
                color = if (standing.rank == 1) 0xFFF8FAFC.toInt() else 0xD9FFFFFF.toInt()
                this.alpha = alpha
            }
            val line = context.getString(
                R.string.group_ranking_line_format,
                standing.rank,
                context.getString(R.string.player_number_format, standing.playerNumber),
                standing.score
            )
            canvas.drawText(line, WIDTH / 2f + slide, centerY, textPaint)
        }
    }

    private fun drawWaveform(
        canvas: Canvas,
        context: Context,
        waveform: List<Float>,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        reveal: Float
    ) {
        val values = if (waveform.isNotEmpty()) waveform else listOf(0.12f, 0.25f, 0.18f, 0.42f, 0.3f, 0.55f, 0.24f, 0.37f, 0.2f)
        val count = max(1, (values.size * reveal).roundToInt().coerceAtMost(values.size))
        val visible = values.take(count)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 6f
            strokeCap = Paint.Cap.ROUND
            shader = LinearGradient(left, 0f, left + width, 0f, context.getColor(R.color.purple), context.getColor(R.color.cyan), Shader.TileMode.CLAMP)
        }
        val center = top + height / 2f
        val step = width / max(values.size - 1, 1)
        visible.forEachIndexed { index, value ->
            val half = value.coerceIn(0.04f, 1f) * height * 0.46f
            val x = left + index * step
            canvas.drawLine(x, center - half, x, center + half, paint)
        }
    }

    private fun scoreLabel(context: Context, score: Int): String = context.getString(
        when {
            score >= 85 -> R.string.result_honest
            score >= 65 -> R.string.result_hesitant
            score >= 45 -> R.string.result_white_lie
            else -> R.string.result_actor
        }
    )

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

    private fun cleanupOldVideos(dir: File) {
        dir.listFiles()
            ?.filter { it.extension.equals("mp4", ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(5)
            ?.forEach { runCatching { it.delete() } }
    }

    private class CodecInputSurface(private val surface: Surface) {
        private val eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        private val eglContext: android.opengl.EGLContext
        private val eglSurface: android.opengl.EGLSurface
        private val program: Int
        private val textureId: Int
        private val positionBuffer: FloatBuffer = floatBufferOf(
            -1f, -1f,
             1f, -1f,
            -1f,  1f,
             1f,  1f
        )
        private val textureBuffer: FloatBuffer = floatBufferOf(
            0f, 1f,
            1f, 1f,
            0f, 0f,
            1f, 0f
        )

        init {
            require(eglDisplay != EGL14.EGL_NO_DISPLAY) { "Unable to get EGL display" }
            val version = IntArray(2)
            check(EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) { "Unable to initialize EGL" }
            val configs = arrayOfNulls<android.opengl.EGLConfig>(1)
            val numConfigs = IntArray(1)
            val attributes = intArrayOf(
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL_RECORDABLE_ANDROID, 1,
                EGL14.EGL_NONE
            )
            check(EGL14.eglChooseConfig(eglDisplay, attributes, 0, configs, 0, 1, numConfigs, 0) && numConfigs[0] > 0) {
                "Unable to choose EGL config"
            }
            val config = configs[0] ?: error("Missing EGL config")
            eglContext = EGL14.eglCreateContext(
                eglDisplay,
                config,
                EGL14.EGL_NO_CONTEXT,
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE),
                0
            )
            check(eglContext != EGL14.EGL_NO_CONTEXT) { "Unable to create EGL context" }
            eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, config, surface, intArrayOf(EGL14.EGL_NONE), 0)
            check(eglSurface != EGL14.EGL_NO_SURFACE) { "Unable to create EGL surface" }
            makeCurrent()
            program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
            textureId = createTexture()
        }

        fun makeCurrent() {
            check(EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) { "Unable to make EGL context current" }
        }

        fun drawBitmap(bitmap: Bitmap) {
            GLES20.glViewport(0, 0, WIDTH, HEIGHT)
            GLES20.glClearColor(0f, 0f, 0f, 1f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            GLES20.glUseProgram(program)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

            val positionLocation = GLES20.glGetAttribLocation(program, "aPosition")
            val textureLocation = GLES20.glGetAttribLocation(program, "aTexCoord")
            val samplerLocation = GLES20.glGetUniformLocation(program, "uTexture")
            positionBuffer.position(0)
            textureBuffer.position(0)
            GLES20.glEnableVertexAttribArray(positionLocation)
            GLES20.glEnableVertexAttribArray(textureLocation)
            GLES20.glVertexAttribPointer(positionLocation, 2, GLES20.GL_FLOAT, false, 0, positionBuffer)
            GLES20.glVertexAttribPointer(textureLocation, 2, GLES20.GL_FLOAT, false, 0, textureBuffer)
            GLES20.glUniform1i(samplerLocation, 0)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            GLES20.glDisableVertexAttribArray(positionLocation)
            GLES20.glDisableVertexAttribArray(textureLocation)
        }

        fun setPresentationTime(nanos: Long) {
            EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, nanos)
        }

        fun swapBuffers() {
            check(EGL14.eglSwapBuffers(eglDisplay, eglSurface)) { "Unable to swap EGL buffers" }
        }

        fun release() {
            makeCurrent()
            GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
            GLES20.glDeleteProgram(program)
            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroySurface(eglDisplay, eglSurface)
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(eglDisplay)
        }

        private fun createTexture(): Int {
            val textures = IntArray(1)
            GLES20.glGenTextures(1, textures, 0)
            val id = textures[0]
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            return id
        }

        private fun createProgram(vertexSource: String, fragmentSource: String): Int {
            val vertex = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
            val fragment = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
            val result = GLES20.glCreateProgram()
            GLES20.glAttachShader(result, vertex)
            GLES20.glAttachShader(result, fragment)
            GLES20.glLinkProgram(result)
            val status = IntArray(1)
            GLES20.glGetProgramiv(result, GLES20.GL_LINK_STATUS, status, 0)
            check(status[0] == GLES20.GL_TRUE) { "Unable to link GL program: ${GLES20.glGetProgramInfoLog(result)}" }
            GLES20.glDeleteShader(vertex)
            GLES20.glDeleteShader(fragment)
            return result
        }

        private fun compileShader(type: Int, source: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)
            val status = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
            check(status[0] == GLES20.GL_TRUE) { "Unable to compile GL shader: ${GLES20.glGetShaderInfoLog(shader)}" }
            return shader
        }

        companion object {
            private const val EGL_RECORDABLE_ANDROID = 0x3142
            private const val VERTEX_SHADER = "attribute vec4 aPosition; attribute vec2 aTexCoord; varying vec2 vTexCoord; void main() { gl_Position = aPosition; vTexCoord = aTexCoord; }"
            private const val FRAGMENT_SHADER = "precision mediump float; uniform sampler2D uTexture; varying vec2 vTexCoord; void main() { gl_FragColor = texture2D(uTexture, vTexCoord); }"

            private fun floatBufferOf(vararg values: Float): FloatBuffer = ByteBuffer
                .allocateDirect(values.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .apply { put(values); position(0) }
        }
    }
}
