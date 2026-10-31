package com.summit.tv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.view.View
import android.os.Handler
import android.os.Looper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SummitHomeView(context: Context) : View(context) {

    private val backBitmap = loadAsset("mountain_back.png")
    private val frontBitmap = loadAsset("mountain_front.png")

    private val handler = Handler(Looper.getMainLooper())

    private val bitmapPaint = Paint(
        Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG
    )

    private val clockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(185, 225, 230, 235)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(
            "sans-serif",
            Typeface.BOLD
        )
        setShadowLayer(
            10f, 0f, 3f,
            Color.argb(55, 0, 0, 0)
        )
    }

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 30f
        typeface = Typeface.create(
            "sans-serif",
            Typeface.BOLD
        )
    }

    private val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(155, 165, 175)
        textSize = 19f
        typeface = Typeface.create(
            "sans-serif",
            Typeface.NORMAL
        )
    }

    private val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(35, 43, 52)
    }

    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(
            "sans-serif",
            Typeface.BOLD
        )
    }

    private val clockFormat =
        SimpleDateFormat("HH:mm", Locale.getDefault())

    private val clockPositionX = 0.50f
    private val clockPositionY = 0.25f

    private val clockSize = 0.125f

    private val clockUpdater = object : Runnable {
        override fun run() {
            invalidate()
            handler.postDelayed(this, 1000)
        }
    }

    private fun loadAsset(name: String): Bitmap? {
        return try {
            context.assets.open(name).use {
                BitmapFactory.decodeStream(it)
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler.post(clockUpdater)
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(clockUpdater)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        if (w <= 0f || h <= 0f) return

        // Base background
        canvas.drawColor(Color.BLACK)

        // 1. Background mountain
        if (backBitmap != null) {
            drawMountain(canvas, backBitmap, w, h)
        } else {
            drawFallbackBackground(canvas, w, h)
        }

        // 2. Clock
        drawClock(canvas, w, h)

        // 3. Foreground mountain
        if (frontBitmap != null) {
            drawMountain(canvas, frontBitmap, w, h)
        }

        // 4. Gradient into the app section
        drawGradient(canvas, w, h)

        // 5. App section layout
        drawAppsSection(canvas, w, h)
    }

    private fun drawMountain(
        canvas: Canvas,
        bitmap: Bitmap,
        w: Float,
        h: Float
    ) {
        val reference = backBitmap ?: bitmap

        // Only the upper part of the screen contains the artwork.
        val mountainHeight = h * 0.76f

        // Both images use exactly the same scaling and position.
        val scale = maxOf(
            w / reference.width,
            mountainHeight / reference.height
        ) * 1.12f

        val imageW = reference.width * scale
        val imageH = reference.height * scale

        val left = (w - imageW) / 2f
        val top = -h * 0.025f

        val destination = RectF(
            left,
            top,
            left + imageW,
            top + imageH
        )

        canvas.drawBitmap(
            bitmap,
            null,
            destination,
            bitmapPaint
        )
    }

    private fun drawClock(
        canvas: Canvas,
        w: Float,
        h: Float
    ) {
        clockPaint.textSize = h * clockSize

        val time = clockFormat.format(Date())
        val metrics = clockPaint.fontMetrics

        val baseline = h * clockPositionY -
            (metrics.ascent + metrics.descent) / 2f

        canvas.drawText(
            time,
            w * clockPositionX,
            baseline,
            clockPaint
        )
    }

    private fun drawGradient(
        canvas: Canvas,
        w: Float,
        h: Float
    ) {
        val gradient = LinearGradient(
            0f,
            h * 0.43f,
            0f,
            h * 0.83f,
            intArrayOf(
                Color.argb(0, 0, 0, 0),
                Color.argb(190, 0, 0, 0),
                Color.BLACK
            ),
            floatArrayOf(0f, 0.65f, 1f),
            Shader.TileMode.CLAMP
        )

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = gradient
        }

        canvas.drawRect(
            0f,
            h * 0.43f,
            w,
            h,
            paint
        )
    }

    private fun drawAppsSection(
        canvas: Canvas,
        w: Float,
        h: Float
    ) {
        val left = w * 0.075f

        // Section heading
        titlePaint.textSize = h * 0.030f

        canvas.drawText(
            "Favourites",
            left,
            h * 0.765f,
            titlePaint
        )

        // Temporary placeholder tiles
        val tileSize = h * 0.105f
        val gap = h * 0.025f
        val top = h * 0.805f

        val icons = listOf("▶", "♪", "⌂", "⚙")

        iconPaint.textSize = tileSize * 0.43f

        icons.forEachIndexed { index, symbol ->

            val x = left + index * (tileSize + gap)

            val tile = RectF(
                x,
                top,
                x + tileSize,
                top + tileSize
            )

            canvas.drawRoundRect(
                tile,
                18f,
                18f,
                tilePaint
            )

            val metrics = iconPaint.fontMetrics

            val baseline = top + tileSize / 2f -
                (metrics.ascent + metrics.descent) / 2f

            canvas.drawText(
                symbol,
                x + tileSize / 2f,
                baseline,
                iconPaint
            )
        }

        subtitlePaint.textSize = h * 0.020f

        canvas.drawText(
            "Your apps will appear here",
            left,
            h * 0.965f,
            subtitlePaint
        )
    }

    private fun drawFallbackBackground(
        canvas: Canvas,
        w: Float,
        h: Float
    ) {
        val gradient = LinearGradient(
            0f,
            0f,
            0f,
            h * 0.76f,
            Color.rgb(30, 48, 65),
            Color.rgb(85, 105, 120),
            Shader.TileMode.CLAMP
        )

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = gradient
        }

        canvas.drawRect(
            0f,
            0f,
            w,
            h * 0.76f,
            paint
        )
    }
}