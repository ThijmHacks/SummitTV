package com.summit.tv

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.os.Handler
import android.os.Looper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SummitHomeView(context: Context) : View(context) {

    private data class SummitApp(
        val packageName: String,
        val label: String,
        val icon: Drawable,
        val activityName: String,
        val category: String
    )

    private val packageManager = context.packageManager

    private val preferences = context.getSharedPreferences(
        "summit_preferences",
        Context.MODE_PRIVATE
    )

    private val apps = mutableListOf<SummitApp>()
    private val fifoApps = mutableListOf<String>()
    private val tileBounds = mutableListOf<RectF>()

    private var selectedIndex = 0

    private val backBitmap = loadAsset("mountain_back.png")
    private val frontBitmap = loadAsset("mountain_front.png")
    private val referenceBitmap = backBitmap ?: frontBitmap

    private val handler = Handler(Looper.getMainLooper())

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

    private val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(35, 43, 52)
    }

    private val selectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val clockFormat =
        SimpleDateFormat("HH:mm", Locale.getDefault())

    private val clockUpdater = object : Runnable {
        override fun run() {
            invalidate()
            handler.postDelayed(this, 1000)
        }
    }

    init {
        isFocusable = true
        isFocusableInTouchMode = true

        discoverApps()
        loadFifoApps()

        requestFocus()
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

    // APP DISCOVERY

    private fun discoverApps() {

        apps.clear()

        val discovered = linkedMapOf<String, SummitApp>()

        val intents = listOf(
            Intent(Intent.ACTION_MAIN).addCategory(
                Intent.CATEGORY_LEANBACK_LAUNCHER
            ) to Intent.CATEGORY_LEANBACK_LAUNCHER,

            Intent(Intent.ACTION_MAIN).addCategory(
                Intent.CATEGORY_LAUNCHER
            ) to Intent.CATEGORY_LAUNCHER
        )

        for ((intent, category) in intents) {

            val results: List<ResolveInfo> =
                packageManager.queryIntentActivities(intent, 0)

            for (resolve in results) {

                val activity = resolve.activityInfo ?: continue
                val packageName = activity.packageName

                if (packageName == context.packageName) continue
                if (discovered.containsKey(packageName)) continue

                try {
                    discovered[packageName] = SummitApp(
                        packageName = packageName,
                        label = resolve.loadLabel(packageManager).toString(),
                        icon = resolve.loadIcon(packageManager),
                        activityName = activity.name,
                        category = category
                    )
                } catch (_: Exception) {
                    // Skip unavailable applications.
                }
            }
        }

        apps.addAll(
            discovered.values.sortedBy {
                it.label.lowercase(Locale.getDefault())
            }
        )
    }

    // FIFO SYSTEM

    private fun loadFifoApps() {

        val saved = preferences.getString("fifo_apps", null)

        fifoApps.clear()

        if (saved != null) {

            fifoApps.addAll(
                saved.split("\n").filter { it.isNotBlank() }
            )

            fifoApps.retainAll(
                apps.map { it.packageName }.toSet()
            )

        } else {

            fifoApps.addAll(
                apps.take(5).map { it.packageName }
            )
        }

        for (app in apps) {

            if (fifoApps.size >= 5) break

            if (app.packageName !in fifoApps) {
                fifoApps.add(app.packageName)
            }
        }

        saveFifoApps()
    }

    private fun saveFifoApps() {

        preferences.edit()
            .putString("fifo_apps", fifoApps.joinToString("\n"))
            .apply()
    }

    private fun addToFifo(packageName: String) {

        if (packageName in fifoApps) return

        if (fifoApps.size >= 5) {
            fifoApps.removeAt(0)
        }

        fifoApps.add(0, packageName)

        saveFifoApps()
        invalidate()
    }

    private fun getFifoApp(index: Int): SummitApp? {

        if (index !in fifoApps.indices) return null

        val packageName = fifoApps[index]

        return apps.firstOrNull {
            it.packageName == packageName
        }
    }

    // APP LAUNCHING

    private fun launchApp(app: SummitApp) {

        try {

            addToFifo(app.packageName)

            val intent = Intent(Intent.ACTION_MAIN).apply {

                addCategory(app.category)

                setClassName(
                    app.packageName,
                    app.activityName
                )

                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)

        } catch (e: Exception) {

            android.util.Log.e(
                "Summit",
                "Could not launch ${app.label}",
                e
            )
        }
    }

    // REMOTE CONTROL

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent?
    ): Boolean {

        when (keyCode) {

            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                selectedIndex = (selectedIndex + 1).coerceAtMost(4)
                invalidate()
                return true
            }

            KeyEvent.KEYCODE_DPAD_LEFT -> {
                selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                invalidate()
                return true
            }

            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {
                getFifoApp(selectedIndex)?.let {
                    launchApp(it)
                }
                return true
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {

        if (event.action == MotionEvent.ACTION_UP) {

            tileBounds.forEachIndexed { index, rect ->

                if (rect.contains(event.x, event.y)) {

                    selectedIndex = index

                    getFifoApp(index)?.let {
                        launchApp(it)
                    }

                    invalidate()
                    return true
                }
            }
        }

        return true
    }

    // SCREENSAVER TRANSITIONS

    fun showScreensaver() {
        animateTo(1f)
    }

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

    // DRAWING

    override fun onDraw(canvas: Canvas) {

        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        if (w <= 0f || h <= 0f) return

        val p = transitionProgress

        canvas.drawColor(Color.BLACK)

        val mountainHeight = lerp(h * 0.76f, h, p)
        val zoom = lerp(1.20f, 1.00f, p)

        if (backBitmap != null) {
            drawMountain(canvas, backBitmap, w, h, mountainHeight, zoom)
        } else {
            drawFallbackBackground(canvas, w, h)
        }

        drawClock(canvas, w, h, p)

        if (frontBitmap != null) {
            drawMountain(canvas, frontBitmap, w, h, mountainHeight, zoom)
        }

        drawGradient(canvas, w, h, p)
        drawAppsSection(canvas, w, h, p)
    }

    private fun lerp(
        start: Float,
        end: Float,
        progress: Float
    ): Float {
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

        val alpha = (255f * (1f - progress))
            .toInt()
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

    // FIVE WIDE RECTANGULAR APP TILES

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

        canvas.saveLayerAlpha(0f, 0f, w, h, alpha)

        // Wide rectangular tiles, original height.
        val margin = w * 0.07f
        val gap = w * 0.015f

        val tileHeight = h * 0.115f
        val availableWidth = w - margin * 2f
        val tileWidth = (availableWidth - gap * 4f) / 5f

        val top = h * 0.825f

        tileBounds.clear()

        for (index in 0 until 5) {

            val x = margin + index * (tileWidth + gap)

            val tile = RectF(
                x,
                top,
                x + tileWidth,
                top + tileHeight
            )

            tileBounds.add(
                RectF(
                    tile.left - 8f,
                    tile.top - 8f,
                    tile.right + 8f,
                    tile.bottom + 8f
                )
            )

            tilePaint.alpha = (150f * (alpha / 255f)).toInt()

            canvas.drawRoundRect(
                tile,
                tileHeight * 0.22f,
                tileHeight * 0.22f,
                tilePaint
            )

            // Selection outline.
            if (index == selectedIndex) {

                selectionPaint.alpha = alpha
                selectionPaint.strokeWidth = 4f

                canvas.drawRoundRect(
                    RectF(
                        tile.left - 5f,
                        tile.top - 5f,
                        tile.right + 5f,
                        tile.bottom + 5f
                    ),
                    tileHeight * 0.24f,
                    tileHeight * 0.24f,
                    selectionPaint
                )
            }

            // Actual app icon, centred and proportional.
            val app = getFifoApp(index)

            if (app != null) {

                val iconSize = tileHeight * 0.76f

                val iconLeft = x + (tileWidth - iconSize) / 2f
                val iconTop = top + (tileHeight - iconSize) / 2f

                app.icon.setBounds(
                    iconLeft.toInt(),
                    iconTop.toInt(),
                    (iconLeft + iconSize).toInt(),
                    (iconTop + iconSize).toInt()
                )

                app.icon.alpha = alpha
                app.icon.draw(canvas)
                app.icon.alpha = 255
            }
        }

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