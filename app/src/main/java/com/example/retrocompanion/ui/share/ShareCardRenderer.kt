package com.example.retrocompanion.ui.share

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils

/** Colors for a card, as ARGB ints, taken from the app theme so cards match light or dark mode. */
data class ShareCardPalette(
    val background: Int,
    val surface: Int,
    val onSurface: Int,
    val onSurfaceVariant: Int,
    val accent: Int
)

/**
 * Draws a [ShareCardContent] onto a bitmap with plain Canvas/StaticLayout calls. Text is
 * wrapped and ellipsized to fixed line limits, so a very long title can't overflow, and a null
 * [icon] falls back to a glyph tile instead of leaving a hole.
 */
object ShareCardRenderer {
    const val WIDTH = 1080

    private const val PADDING = 56f
    private const val ICON_SIZE = 200f
    private const val ICON_RADIUS = 28f
    private const val GAP = 40f

    fun render(content: ShareCardContent, palette: ShareCardPalette, icon: Bitmap?): Bitmap {
        val textLeft = PADDING + ICON_SIZE + GAP
        val textWidth = (WIDTH - PADDING - textLeft).toInt()
        val fullWidth = (WIDTH - 2 * PADDING).toInt()

        val kindPaint = paint(palette.accent, 34f, bold = true).apply { letterSpacing = 0.12f }
        val titleLayout = layout(content.title, paint(palette.onSurface, 60f, bold = true), textWidth, maxLines = 3)
        val subtitleLayout = content.subtitle?.let {
            layout(it, paint(palette.onSurfaceVariant, 38f), textWidth, maxLines = 2)
        }
        val descriptionLayout = content.description?.let {
            layout(it, paint(palette.onSurface, 38f), fullWidth, maxLines = 4)
        }
        val statsLayout = layout(content.stats, paint(palette.accent, 36f, bold = true), fullWidth, maxLines = 2)
        val footerPaint = paint(palette.onSurfaceVariant, 30f)

        val kindBaseline = PADDING + kindPaint.textSize
        val headerTop = kindBaseline + 28f
        val textColumnHeight = titleLayout.height +
            (subtitleLayout?.let { 12f + it.height } ?: 0f)
        var y = headerTop + maxOf(ICON_SIZE, textColumnHeight) + GAP
        val descriptionTop = y
        if (descriptionLayout != null) y += descriptionLayout.height + GAP
        val statsTop = y
        y += statsLayout.height + GAP
        val footerBaseline = y + footerPaint.textSize
        val height = (footerBaseline + PADDING).toInt()

        val bitmap = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(palette.background)

        canvas.drawText(content.kind.label, PADDING, kindBaseline, kindPaint)
        drawIcon(canvas, content, palette, icon, left = PADDING, top = headerTop)

        canvas.withTranslation(textLeft, headerTop) { titleLayout.draw(this) }
        subtitleLayout?.let { layoutToDraw ->
            canvas.withTranslation(textLeft, headerTop + titleLayout.height + 12f) { layoutToDraw.draw(this) }
        }
        descriptionLayout?.let { layoutToDraw ->
            canvas.withTranslation(PADDING, descriptionTop) { layoutToDraw.draw(this) }
        }
        canvas.withTranslation(PADDING, statsTop) { statsLayout.draw(this) }
        canvas.drawText("Shared from RetroCompanion", PADDING, footerBaseline, footerPaint)
        return bitmap
    }

    private fun drawIcon(
        canvas: Canvas,
        content: ShareCardContent,
        palette: ShareCardPalette,
        icon: Bitmap?,
        left: Float,
        top: Float
    ) {
        val rect = RectF(left, top, left + ICON_SIZE, top + ICON_SIZE)
        val clip = Path().apply { addRoundRect(rect, ICON_RADIUS, ICON_RADIUS, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(clip)
        if (icon != null && icon.width > 0 && icon.height > 0) {
            canvas.drawBitmap(icon, null, rect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        } else {
            canvas.drawRect(rect, Paint().apply { color = palette.surface })
            val glyphPaint = paint(palette.onSurface, 96f).apply { textAlign = Paint.Align.CENTER }
            val baseline = rect.centerY() - (glyphPaint.descent() + glyphPaint.ascent()) / 2
            canvas.drawText(content.kind.placeholderGlyph, rect.centerX(), baseline, glyphPaint)
        }
        canvas.restore()
    }

    private fun paint(color: Int, size: Float, bold: Boolean = false) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    private fun layout(text: String, paint: TextPaint, width: Int, maxLines: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setIncludePad(false)
            .build()

    private inline fun Canvas.withTranslation(x: Float, y: Float, block: Canvas.() -> Unit) {
        save()
        translate(x, y)
        block()
        restore()
    }
}
