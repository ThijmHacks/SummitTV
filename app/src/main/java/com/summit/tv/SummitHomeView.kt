package com.summit.tv

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.os.Handler
import android.os.Looper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SummitHomeView(context: Context) : View(context) {

    private val backBitmap = loadAsset("mountain_back.png")
    private val frontBitmap = loadAsset("mountain_front.png")
    private val referenceBitmap = backBitmap ?: frontBitmap

    private val handler = Handler(Looper.getMainLooper())

    // 0 = Home, 1 = Screensaver
    private var transitionProgress = 0f

    private var animator: ValueAnimator? = null

    private val bitmapPaint = Paint(
        Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG
    )

    private val clockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(185, 225, 230, 235)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        setShadowLayer(12f, 0f, 4f, Color.argb(45, 0, 0, 0))
    }

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }

    private val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(155, 165, 175)
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }

    private val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(35, 43, 52)
    }

    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
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

    // Animate into screensaver
    fun showScreensaver() {
        animateTo(1f)
    }

    // Animate back into launcher
    fun showHome() {
        animateTo(0f)
    }

    private fun animateTo(target: Float) {
        animator?.cancel()

        val start = transitionProgress

        animator = ValueAnimator.ofFloat(start, target).apply {
            duration = 1600L
            interpolator = AccelerateDecelerateInterpolator()

            addUpdateListener {
                transitionProgress = it.animatedValue as Float
                invalidate()
            }

            start()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler.post(clockUpdater)
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(clockUpdater)
        animator?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        if (w <= 0f || h <= 0f) return

        val p = transitionProgress

        canvas.drawColor(Color.BLACK)

        // Smoothly change mountain area and zoom.
        val mountainHeight = lerp(h * 0.76f, h, p)
        val zoom = lerp(1.20f, 1.00f, p)

        // 1. Background mountain
        if (backBitmap != null) {
            drawMountain(canvas, backBitmap, w, h, mountainHeight, zoom)
        } else {
            drawFallbackBackground(canvas, w, h)
        }

        // 2. Clock grows during transition.
        drawClock(canvas, w, h, p)

        // 3. Foreground mountain
        if (frontBitmap != null) {
            drawMountain(canvas, frontBitmap, w, h, mountainHeight, zoom)
        }

        // 4. Gradient retreats downwards and disappears.
        drawGradient(canvas, w, h, p)

        // 5. Apps fade out.
        drawAppsSection(canvas, w, h, p)
    }

    private fun lerp(start: Float, end: Float, progress: Float): Float {
        return start + (end - start) * progress
    }

    private fun drawMountain(
        canvas: Canvas,
        bitmap: Bitmap,
        w: Float,
        h: Float,
        mountainHeight: Float,
        zoom: Float
    ) {
        val reference = referenceBitmap ?: bitmap

        val scale = maxOf(
            w / reference.width,
            mountainHeight / reference.height
        ) * zoom

        val imageW = reference.width * scale
        val imageH = reference.height * scale

        val homeTop = -h * 0.025f
        val saverTop = (h - imageH) / 2f

        val top = lerp(homeTop, saverTop, transitionProgress)
        val left = (w - imageW) / 2f

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
        h: Float,
        progress: Float
    ) {
        clockPaint.textSize = h * lerp(0.125f, 0.30f, progress)

        val time = clockFormat.format(Date())
        val metrics = clockPaint.fontMetrics

        val clockY = lerp(0.25f, 0.30f, progress)

        val baseline = h * clockY -
            (metrics.ascent + metrics.descent) / 2f

        canvas.drawText(
            time,
            w * 0.50f,
            baseline,
            clockPaint
        )
    }

    
    private fun drawGradient(
        canvas: Canvas,
        w: Float,
        h: Float,
        progress: Float
    ) {
        // Keep gradient fully visible in launcher mode.
        // Fade it out during screensaver transition.
        val alpha = (255f * (1f - progress)).toInt()
            .coerceIn(0, 255)

        if (alpha == 0) return

        val gradient = LinearGradient(
            0f,
            h * 0.35f,
            0f,
            h,
            intArrayOf(
                Color.TRANSPARENT,
                Color.argb(80, 0, 0, 0),
                Color.argb(190, 0, 0, 0),
                Color.BLACK
            ),
            floatArrayOf(0f, 0.40f, 0.75f, 1f),
            Shader.TileMode.CLAMP
        )

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = gradient
            this.alpha = alpha
        }

        canvas.drawRect(0f, h * 0.35f, w, h, paint)
    }


    private fun drawAppsSection(
        canvas: Canvas,
        w: Float,
        h: Float,
        progress: Float
    ) {
        val alpha = (255 * (1f - progress)).toInt()

        if (alpha <= 0) return

        val saveCount = canvas.save()

        canvas.translate(0f, h * 0.06f * progress)
        canvas.saveLayerAlpha(
            0f, 0f, w, h, alpha
        )

        val left = w * 0.075f

        titlePaint.alpha = alpha
        titlePaint.textSize = h * 0.030f

        canvas.drawText(
            "Favourites",
            left,
            h * 0.765f,
            titlePaint
        )

        val tileSize = h * 0.105f
        val gap = h * 0.025f
        val top = h * 0.805f

        val icons = listOf("▶", "♪", "⌂", "⚙")

        tilePaint.alpha = alpha
        iconPaint.alpha = alpha
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

        subtitlePaint.alpha = alpha
        subtitlePaint.textSize = h * 0.020f

        canvas.drawText(
            "Your apps will appear here",
            left,
            h * 0.965f,
            subtitlePaint
        )

        canvas.restore()
        canvas.restoreToCount(saveCount)
    }

    private fun drawFallbackBackground(
        canvas: Canvas,
        w: Float,
        h: Float
    ) {
        val gradient = LinearGradient(
            0f, 0f, 0f, h * 0.76f,
            Color.rgb(30, 48, 65),
            Color.rgb(85, 105, 120),
            Shader.TileMode.CLAMP
        )

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = gradient
        }

        canvas.drawRect(0f, 0f, w, h * 0.76f, paint)
    }
}