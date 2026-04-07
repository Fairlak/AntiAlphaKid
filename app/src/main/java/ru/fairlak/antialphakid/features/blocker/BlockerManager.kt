package ru.fairlak.antialphakid.features.blocker

import android.content.Context
import android.graphics.*
import android.view.View
import android.view.WindowManager
import android.content.Intent
import android.os.Build
import android.text.StaticLayout
import android.text.TextPaint
import java.util.*

class BlockerManager(private val context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: MatrixBlockerView? = null

    fun showOverlay() {
        if (overlayView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            gravity = android.view.Gravity.TOP
        }

        val view = MatrixBlockerView(context) {
            hideOverlay()
            closeCurrentApp()
        }
        overlayView = view
        windowManager.addView(view, params)
    }

    private fun closeCurrentApp() {
        val startMain = Intent(Intent.ACTION_MAIN)
        startMain.addCategory(Intent.CATEGORY_HOME)
        startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(startMain)
    }

    fun hideOverlay() {
        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
    }

    private class MatrixBlockerView(context: Context, val onClose: () -> Unit) : View(context) {
        private val matrixChars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐﾑﾒﾓﾔﾕﾖﾗﾘﾙﾚﾛﾜﾝ".toCharArray()
        private val paint = Paint().apply {
            textSize = 50f
            typeface = Typeface.MONOSPACE
            isAntiAlias = true
        }
        private val bgPaint = Paint().apply {
            color = Color.BLACK
            alpha = 0
        }
        private val random = Random()
        private var bgAlpha = 0
        private val columns = mutableListOf<Float>()
        private val speeds = mutableListOf<Float>()

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            val columnWidth = 40f
            val columnCount = (w / columnWidth).toInt()

            columns.clear()
            speeds.clear()

            repeat(columnCount) {
                columns.add(-random.nextInt(h).toFloat())
                speeds.add(random.nextInt(25) + 15f)
            }
        }

        override fun onDraw(canvas: Canvas) {
            if (bgAlpha < 300) bgAlpha += 5
            bgPaint.alpha = bgAlpha
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            for (i in columns.indices) {
                val x = i * 40f
                val headY = columns[i]

                for (j in 0..20) {
                    val symbolY = headY - (j * 50f)

                    if (symbolY > -50 && symbolY < height + 50) {

                        paint.color = Color.GREEN
                        val alphaFade = (255 - (j * 12)).coerceIn(0, 255)
                        paint.alpha = alphaFade

                        val charToShow = matrixChars[random.nextInt(matrixChars.size)]
                        canvas.drawText(charToShow.toString(), x, symbolY, paint)
                    }
                }

                columns[i] += speeds[i]

                if (columns[i] - 1000 > height) {
                    columns[i] = -50f
                    speeds[i] = random.nextInt(25) + 15f
                }
            }

            if (bgAlpha >= 240) {
                drawOverlayText(canvas)
            }

            postInvalidateDelayed(40)
        }

        private fun drawOverlayText(canvas: Canvas) {
            val prefs = context.getSharedPreferences("anti_alpha_prefs", Context.MODE_PRIVATE)
            val customText = prefs.getString("blocker_text", "ЛИМИТ ИСЧЕРПАН") ?: "ЛИМИТ ИСЧЕРПАН"

            val textPaint = TextPaint().apply {
                color = Color.GREEN
                textSize = 70f
                typeface = Typeface.MONOSPACE
                isFakeBoldText = true
                setShadowLayer(10f, 0f, 0f, Color.GREEN)
                textAlign = Paint.Align.LEFT
            }

            val maxWidth = (width * 0.8f).toInt()

            val staticLayout = StaticLayout.Builder.obtain(customText, 0, customText.length, textPaint, maxWidth)
                .setAlignment(android.text.Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(0f, 1.2f)
                .build()

            canvas.save()

            val x = (width - maxWidth) / 2f
            val y = height / 2f - (staticLayout.height / 2f)

            canvas.translate(x, y)
            staticLayout.draw(canvas)
            canvas.restore()

            val buttonPaint = Paint(textPaint).apply {
                textSize = 65f
                textAlign = Paint.Align.CENTER
                alpha = 200
            }
            canvas.drawText(
                "[ ЗАКРЫТЬ ]",
                width / 2f,
                height / 2f + (staticLayout.height / 2f) + 200f,
                buttonPaint
            )
        }

        init {
            setOnClickListener { if (bgAlpha > 180) onClose() }
        }
    }
}