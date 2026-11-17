package com.example.sudosleep.util

import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.example.sudosleep.data.local.pojo.TimetableEntryWithSubject
import com.example.sudosleep.data.repository.SudoSleepRepository

enum class WallpaperStyle(
    val displayName: String,
    val bgTopColor: Int,
    val bgBottomColor: Int,
    val cardSurfaceColor: Int,
    val textPrimaryColor: Int,
    val textSecondaryColor: Int,
    val gridLineColor: Int
) {
    COZY_PASTEL(
        displayName = "Cozy Pastel 🌸",
        bgTopColor = Color.parseColor("#FDF2F8"),
        bgBottomColor = Color.parseColor("#EEF2FF"),
        cardSurfaceColor = Color.parseColor("#FFFFFF"),
        textPrimaryColor = Color.parseColor("#1E1B4B"),
        textSecondaryColor = Color.parseColor("#64748B"),
        gridLineColor = Color.parseColor("#E2E8F0")
    ),
    MIDNIGHT_SLATE(
        displayName = "Midnight Dark 🌙",
        bgTopColor = Color.parseColor("#0F172A"),
        bgBottomColor = Color.parseColor("#1E1B4B"),
        cardSurfaceColor = Color.parseColor("#1E293B"),
        textPrimaryColor = Color.parseColor("#F8FAFC"),
        textSecondaryColor = Color.parseColor("#94A3B8"),
        gridLineColor = Color.parseColor("#334155")
    ),
    MATCHA_CREAM(
        displayName = "Matcha Cream 🍵",
        bgTopColor = Color.parseColor("#F0FDF4"),
        bgBottomColor = Color.parseColor("#FEFCE8"),
        cardSurfaceColor = Color.parseColor("#FFFFFF"),
        textPrimaryColor = Color.parseColor("#14532D"),
        textSecondaryColor = Color.parseColor("#4D7C0F"),
        gridLineColor = Color.parseColor("#DCFCE7")
    )
}

object TimetableExporter {

    private val DAY_LABELS = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

    /**
     * Renders a crisp, high-resolution phone wallpaper Bitmap (1080 x 2400) independent of current UI scroll/zoom.
     * Leaves lock-screen clock space at the top so the timetable sits cleanly on the Home or Lock screen.
     */
    fun renderWallpaperBitmap(
        entries: List<TimetableEntryWithSubject>,
        style: WallpaperStyle = WallpaperStyle.COZY_PASTEL,
        studentName: String = "Sudo Sleep",
        includeWeekends: Boolean = false,
        lockScreenClockPadding: Boolean = true
    ): Bitmap {
        val width = 1080
        val height = 2400
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawTimetableCanvas(
            canvas = canvas,
            width = width.toFloat(),
            height = height.toFloat(),
            entries = entries,
            style = style,
            studentName = studentName,
            includeWeekends = includeWeekends,
            topPaddingRatio = if (lockScreenClockPadding) 0.24f else 0.10f,
            bottomPaddingRatio = 0.10f
        )
        return bitmap
    }

    /**
     * Saves the rendered Wallpaper PNG into MediaStore.Images and returns its content Uri.
     */
    fun saveWallpaperPngToMediaStore(
        context: Context,
        bitmap: Bitmap
    ): Uri? {
        val timestamp = System.currentTimeMillis()
        val filename = "SudoSleep_Wallpaper_$timestamp.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/SudoSleep")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null

        return try {
            resolver.openOutputStream(uri)?.use { out: OutputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }

    /**
     * Applies the bitmap directly as the device's system wallpaper (Home, Lock, or Both).
     */
    fun applyAsPhoneWallpaper(
        context: Context,
        bitmap: Bitmap,
        whichFlags: Int = WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
    ): Boolean {
        return try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            wallpaperManager.setBitmap(bitmap, null, true, whichFlags)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Generates an A4 Landscape print-ready PDF of the weekly timetable and saves it to MediaStore.Downloads.
     */
    fun exportWeeklyPdfToMediaStore(
        context: Context,
        entries: List<TimetableEntryWithSubject>,
        studentName: String = "Sudo Sleep"
    ): Uri? {
        // A4 Landscape at 150 DPI: 1754 x 1240
        val pageWidth = 1754
        val pageHeight = 1240
        val pdfDocument = PdfDocument()

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)

        drawTimetableCanvas(
            canvas = page.canvas,
            width = pageWidth.toFloat(),
            height = pageHeight.toFloat(),
            entries = entries,
            style = WallpaperStyle.COZY_PASTEL,
            studentName = studentName,
            includeWeekends = true,
            topPaddingRatio = 0.08f,
            bottomPaddingRatio = 0.06f
        )

        pdfDocument.finishPage(page)

        val timestamp = System.currentTimeMillis()
        val filename = "SudoSleep_Timetable_$timestamp.pdf"
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, filename)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/SudoSleep")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)

        return try {
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    pdfDocument.writeTo(out)
                }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            pdfDocument.close()
            uri
        } catch (e: Exception) {
            if (uri != null) resolver.delete(uri, null, null)
            pdfDocument.close()
            null
        }
    }

    fun shareExportedFile(context: Context, uri: Uri, mimeType: String, title: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun drawTimetableCanvas(
        canvas: Canvas,
        width: Float,
        height: Float,
        entries: List<TimetableEntryWithSubject>,
        style: WallpaperStyle,
        studentName: String,
        includeWeekends: Boolean,
        topPaddingRatio: Float,
        bottomPaddingRatio: Float
    ) {
        // 1. Gradient Background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width, height,
                style.bgTopColor,
                style.bgBottomColor,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width, height, bgPaint)

        val sideMargin = width * 0.045f
        val topOffset = height * topPaddingRatio
        val bottomMargin = height * bottomPaddingRatio

        // 2. Title & Date Header
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.textPrimaryColor
            textSize = width * 0.036f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.textSecondaryColor
            textSize = width * 0.022f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val headerY = topOffset - (height * 0.035f)
        canvas.drawText("✨ $studentName • Weekly Schedule", sideMargin, headerY, titlePaint)
        val dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))
        canvas.drawText(
            "Generated by Sudo Sleep • $dateStr",
            sideMargin,
            headerY + (width * 0.028f),
            subtitlePaint
        )

        // 3. Card Container
        val cardTop = topOffset + (width * 0.015f)
        val cardBottom = height - bottomMargin
        val cardRect = RectF(sideMargin, cardTop, width - sideMargin, cardBottom)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.cardSurfaceColor
            alpha = 235
        }
        canvas.drawRoundRect(cardRect, 36f, 36f, cardPaint)

        // Determine active days (Mon-Fri or Mon-Sun)
        val hasWeekendClasses = entries.any { it.entry.day >= 6 }
        val daysCount = if (includeWeekends || hasWeekendClasses) 7 else 5

        val timeColWidth = cardRect.width() * 0.11f
        val dayColWidth = (cardRect.width() - timeColWidth - 24f) / daysCount
        val headerRowHeight = cardRect.height() * 0.065f
        val startHour = 8
        val endHour = 22
        val totalHours = endHour - startHour
        val hourRowHeight = (cardRect.height() - headerRowHeight - 24f) / totalHours

        // 4. Draw Day Column Headers
        val dayHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.textPrimaryColor
            textSize = width * 0.022f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        for (d in 0 until daysCount) {
            val centerX = cardRect.left + timeColWidth + (d * dayColWidth) + (dayColWidth / 2f)
            val centerY = cardRect.top + (headerRowHeight * 0.65f)
            canvas.drawText(DAY_LABELS[d], centerX, centerY, dayHeaderPaint)
        }

        // 5. Draw Horizontal Hour Lines & Time Labels
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.gridLineColor
            strokeWidth = 2f
            pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
        }
        val timeLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.textSecondaryColor
            textSize = width * 0.018f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val gridTop = cardRect.top + headerRowHeight
        for (h in startHour..endHour) {
            val y = gridTop + ((h - startHour) * hourRowHeight)
            canvas.drawLine(cardRect.left + timeColWidth, y, cardRect.right - 16f, y, gridPaint)
            if (h < endHour) {
                canvas.drawText(
                    String.format(Locale.US, "%02d:00", h),
                    cardRect.left + timeColWidth - 12f,
                    y + (width * 0.018f),
                    timeLabelPaint
                )
            }
        }

        // 6. Draw Event Blocks
        val blockPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val blockTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = width * 0.018f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val blockSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(230, 255, 255, 255)
            textSize = width * 0.015f
        }

        entries.forEach { item ->
            val entry = item.entry
            if (entry.day < 1 || entry.day > daysCount) return@forEach

            val isCanceled = SudoSleepRepository.isCanceledForWeek(entry)
            val colIndex = entry.day - 1
            val left = cardRect.left + timeColWidth + (colIndex * dayColWidth) + 6f
            val right = left + dayColWidth - 12f
            val top = gridTop + ((entry.startHour - startHour) * hourRowHeight) + 3f
            val bottom = top + (entry.duration * hourRowHeight) - 6f

            val baseColor = try {
                Color.parseColor(item.subject.colorHex)
            } catch (_: Exception) {
                Color.parseColor("#4F46E5")
            }

            blockPaint.color = baseColor
            blockPaint.alpha = if (isCanceled) 95 else 235

            val blockRect = RectF(left, top, right, bottom)
            canvas.drawRoundRect(blockRect, 18f, 18f, blockPaint)

            val maxChars = if (daysCount <= 5) 14 else 10
            val shortSubject = if (item.subject.name.length > maxChars) {
                item.subject.name.take(maxChars - 1) + "…"
            } else {
                item.subject.name
            }

            val actType = (item.requirement.customTypeName ?: item.requirement.activityType).take(10)
            val textX = left + 10f
            var textY = top + (width * 0.022f)

            canvas.drawText(shortSubject, textX, textY, blockTitlePaint)
            textY += (width * 0.019f)
            if (textY < bottom - 6f) {
                val statusLabel = if (isCanceled) "CANCELED" else actType
                canvas.drawText(statusLabel, textX, textY, blockSubPaint)
            }
            if (!entry.room.isNullOrBlank()) {
                textY += (width * 0.018f)
                if (textY < bottom - 6f) {
                    canvas.drawText(entry.room.take(12), textX, textY, blockSubPaint)
                }
            }
        }
    }
}
