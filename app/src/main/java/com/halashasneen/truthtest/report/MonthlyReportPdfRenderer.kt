package com.halashasneen.truthtest.report

import android.content.Context
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.halashasneen.truthtest.R
import com.halashasneen.truthtest.data.QuestionRepository
import com.halashasneen.truthtest.data.model.TestResult
import java.io.File
import java.io.FileOutputStream
import java.text.DateFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MonthlyReportPdfRenderer {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    suspend fun render(
        context: Context,
        results: List<TestResult>,
        month: YearMonth
    ): Uri = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val summary = MonthlyReportCalculator.calculate(results, month)
        require(summary.results.isNotEmpty()) { "No results available for selected month" }

        val dir = File(appContext.cacheDir, "shares").apply { mkdirs() }
        cleanupOldReports(dir)
        val output = File(dir, "truth_test_report_${month}.pdf")
        val document = PdfDocument()
        val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
        val canvas = page.canvas
        val isArabic = appContext.resources.configuration.locales[0].language == "ar"
        val locale = appContext.resources.configuration.locales[0]
        val x = if (isArabic) 535f else 60f
        val align = if (isArabic) Paint.Align.RIGHT else Paint.Align.LEFT
        var y = 58f

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = appContext.getColor(R.color.purple)
            textSize = 25f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = align
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF252532.toInt()
            textSize = 12f
            textAlign = align
        }
        val strongPaint = Paint(bodyPaint).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 14f
        }
        val mutedPaint = Paint(bodyPaint).apply {
            color = 0xFF666675.toInt()
            textSize = 10.5f
        }

        canvas.drawText(appContext.getString(R.string.monthly_report_title), x, y, titlePaint)
        y += 28f
        val monthLabel = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
        canvas.drawText(monthLabel, x, y, strongPaint)
        y += 34f

        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF3F0FF.toInt() }
        canvas.drawRoundRect(RectF(45f, y - 18f, 550f, y + 94f), 18f, 18f, cardPaint)
        y += 8f
        val summaryLines = listOf(
            appContext.getString(R.string.monthly_report_answers) to summary.totalAnswers.toString(),
            appContext.getString(R.string.monthly_report_average) to "${summary.averageScore}%",
            appContext.getString(R.string.monthly_report_best) to "${summary.bestScore}%",
            appContext.getString(R.string.monthly_report_best_streak) to appContext.getString(R.string.stats_days_format, summary.bestStreak)
        )
        summaryLines.forEach { (label, value) ->
            drawLabelValue(canvas, label, value, y, isArabic, strongPaint, bodyPaint)
            y += 23f
        }
        y += 26f

        canvas.drawText(appContext.getString(R.string.monthly_report_challenges), x, y, strongPaint)
        y += 23f
        canvas.drawText(
            appContext.getString(
                R.string.monthly_report_challenge_line,
                summary.duelSessions,
                summary.groupSessions,
                summary.customAnswers
            ),
            x,
            y,
            bodyPaint
        )
        y += 30f

        canvas.drawText(appContext.getString(R.string.monthly_report_intensity), x, y, strongPaint)
        y += 23f
        canvas.drawText(
            appContext.getString(
                R.string.monthly_report_intensity_line,
                summary.lightCount,
                summary.mediumCount,
                summary.boldCount
            ),
            x,
            y,
            bodyPaint
        )
        y += 30f

        summary.favoriteCategory?.let { category ->
            canvas.drawText(appContext.getString(R.string.monthly_report_favorite_category), x, y, strongPaint)
            y += 22f
            canvas.drawText(
                appContext.getString(
                    R.string.monthly_report_favorite_line,
                    categoryName(appContext, category),
                    summary.favoriteCategoryCount
                ),
                x,
                y,
                bodyPaint
            )
            y += 30f
        }

        canvas.drawText(appContext.getString(R.string.monthly_report_recent), x, y, strongPaint)
        y += 22f
        val dateFormatter = DateFormat.getDateInstance(DateFormat.SHORT)
        summary.results.take(10).forEach { result ->
            val question = if (result.question.length > 52) result.question.take(49) + "…" else result.question
            val line = appContext.getString(
                R.string.monthly_report_result_line,
                result.score,
                dateFormatter.format(Date(result.timestamp)),
                question
            )
            canvas.drawText(line, x, y, bodyPaint)
            y += 22f
        }
        if (summary.results.size > 10) {
            canvas.drawText(
                appContext.getString(R.string.monthly_report_more_results, summary.results.size - 10),
                x,
                y,
                mutedPaint
            )
        }

        val disclaimer = appContext.getString(R.string.entertainment_notice)
        canvas.drawText(disclaimer.take(88), x, 785f, mutedPaint)
        canvas.drawText(appContext.getString(R.string.watermark), x, 811f, mutedPaint)

        document.finishPage(page)
        FileOutputStream(output).use(document::writeTo)
        document.close()

        FileProvider.getUriForFile(appContext, "${appContext.packageName}.files", output)
    }

    private fun drawLabelValue(
        canvas: android.graphics.Canvas,
        label: String,
        value: String,
        y: Float,
        isArabic: Boolean,
        labelPaint: Paint,
        valuePaint: Paint
    ) {
        if (isArabic) {
            canvas.drawText(label, 525f, y, labelPaint)
            val valueCopy = Paint(valuePaint).apply { textAlign = Paint.Align.LEFT }
            canvas.drawText(value, 70f, y, valueCopy)
        } else {
            canvas.drawText(label, 70f, y, labelPaint)
            val valueCopy = Paint(valuePaint).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText(value, 525f, y, valueCopy)
        }
    }

    private fun categoryName(context: Context, category: String): String = context.getString(
        when (category) {
            "embarrassing" -> R.string.embarrassing
            "funny" -> R.string.funny
            "bold" -> R.string.bold
            "romantic" -> R.string.romantic
            "friendship" -> R.string.friendship
            "family" -> R.string.family
            "custom" -> R.string.custom_question
            else -> R.string.stats_no_data_short
        }
    )

    private fun cleanupOldReports(dir: File) {
        dir.listFiles()
            ?.filter { it.name.startsWith("truth_test_report_") && it.extension.equals("pdf", true) }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(6)
            ?.forEach { runCatching { it.delete() } }
    }
}
