package com.summit.tv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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

class ScreensaverView(context: Context) : View(context) {

    private val backBitmap = loadAsset("mountain_back.png")
    private val frontBitmap = loadAsset("mountain_front.png")

    private val handler = Handler(Looper.getMainLooper())

    // Image rendering
    private val bitmapPaint = Paint(
        Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG
    )

    // Clock rendering
    private val clockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 225, 230, 235)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(
            "sans-serif",
            Typeface.BOLD
        )
        setShadowLayer(
            12f,
            0f,
            4f,
            Color.argb(45, 0, 0, 0)
        )
    }

    private val clockFormat =
        SimpleDateFormat("HH:mm", Locale.getDefault())

    // Clock position, relative to screen dimensions
    private val clockPositionX = 0.50f
    private val clockPositionY = 0.34f

    private val clockSize = 0.25f

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
        } catch (e: Exception) {
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

        // 1. Background
        if (backBitmap != null) {
            drawMountain(canvas, backBitmap, w, h)
        } else {
            drawFallbackBackground(canvas, w, h)
        }

        // 2. Clock (behind foreground mountain)
        drawClock(canvas, w, h)

        // 3. Foreground mountain
        if (frontBitmap != null) {
            drawMountain(canvas, frontBitmap, w, h)
        }
    }

    private fun drawMountain(
        canvas: Canvas,
        bitmap: Bitmap,
        w: Float,
        h: Float
    ) {
        val reference = backBitmap ?: bitmap

        val scale = maxOf(
            w / reference.width,
            h / reference.height
        )

        val imageW = reference.width * scale
        val imageH = reference.height * scale

        val left = (w - imageW) / 2f
        val top = (h - imageH) / 2f

        // Both transparent layers share the same image canvas.
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

    private fun drawFallbackBackground(
        canvas: Canvas,
        w: Float,
        h: Float
    ) {
        val gradient = LinearGradient(
            0f,
            0f,
            0f,
            h,
            Color.rgb(28, 48, 67),
            Color.rgb(145, 163, 174),
            Shader.TileMode.CLAMP
        )

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = gradient
        }

        canvas.drawRect(0f, 0f, w, h, paint)
    }
}