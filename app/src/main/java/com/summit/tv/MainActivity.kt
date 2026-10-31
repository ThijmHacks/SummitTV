package com.summit.tv

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import android.view.WindowManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        setContentView(SummitScreen(this))
    }
}

class SummitScreen(context: android.content.Context) : View(context) {

    private val backBitmap = loadAsset("mountain_back.png")
    private val frontBitmap = loadAsset("mountain_front.png")

    private val handler = Handler(Looper.getMainLooper())

    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    private val clockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 225, 230, 235)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        setShadowLayer(12f, 0f, 4f, Color.argb(45, 0, 0, 0))
    }

    private val clockFormat =
        SimpleDateFormat("HH:mm", Locale.getDefault())

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

        // Layer 1: Background
        if (backBitmap != null) {
            drawCover(canvas, backBitmap, w, h)
        } else {
            drawFallbackBackground(canvas, w, h)
        }

        // Layer 2: Clock (behind the foreground mountain)
        drawClock(canvas, w, h)

        // Layer 3: Foreground mountain
        if (frontBitmap != null) {
            drawCover(canvas, frontBitmap, w, h)
        } else {
            drawFallbackForeground(canvas, w, h)
        }
    }

    private fun drawCover(
        canvas: Canvas,
        bitmap: Bitmap,
        w: Float,
        h: Float
    ) {
        val scale = maxOf(
            w / bitmap.width,
            h / bitmap.height
        )

        val imageW = bitmap.width * scale
        val imageH = bitmap.height * scale

        val left = (w - imageW) / 2f
        val top = (h - imageH) / 2f

        val destination = RectF(
            left,
            top,
            left + imageW,
            top + imageH
        )

        canvas.drawBitmap(bitmap, null, destination, bitmapPaint)
    }

    private fun drawClock(
        canvas: Canvas,
        w: Float,
        h: Float
    ) {
        clockPaint.textSize = h * 0.30f

        val time = clockFormat.format(Date())
        val metrics = clockPaint.fontMetrics

        val baseline = h * 0.3f -
            (metrics.ascent + metrics.descent) / 2f

        canvas.drawText(
            time,
            w / 2f,
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
            0f, 0f, 0f, h,
            Color.rgb(28, 48, 67),
            Color.rgb(145, 163, 174),
            Shader.TileMode.CLAMP
        )

        val paint = Paint().apply {
            shader = gradient
        }

        canvas.drawRect(0f, 0f, w, h, paint)

        val mountain = Path().apply {
            moveTo(0f, h * 0.65f)
            lineTo(w * 0.25f, h * 0.35f)
            lineTo(w * 0.43f, h * 0.57f)
            lineTo(w * 0.68f, h * 0.25f)
            lineTo(w, h * 0.65f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }

        canvas.drawPath(
            mountain,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(72, 94, 112)
            }
        )
    }

    private fun drawFallbackForeground(
        canvas: Canvas,
        w: Float,
        h: Float
    ) {
        val mountain = Path().apply {
            moveTo(0f, h * 0.82f)
            lineTo(w * 0.19f, h * 0.66f)
            lineTo(w * 0.35f, h * 0.78f)
            lineTo(w * 0.58f, h * 0.47f)
            lineTo(w * 0.78f, h * 0.76f)
            lineTo(w, h * 0.61f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }

        canvas.drawPath(
            mountain,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(26, 43, 57)
            }
        )
    }
}