package ru.fairlak.antialphakid.features.blocker

import android.content.Context
import android.graphics.*
import android.view.View
import android.view.WindowManager
import android.content.Intent
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
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        )

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
        private val paint = Paint().apply {
            color = Color.GREEN
            textSize = 60f
            typeface = Typeface.MONOSPACE
        }
        private val bgPaint = Paint().apply {
            color = Color.BLACK
            alpha = 0
        }
        private val random = Random()
        private var bgAlpha = 0
        private val columns = mutableListOf<Int>()
        private var frameCounter = 0

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            val columnCount = w / 35
            columns.clear()
            repeat(columnCount) {
                columns.add(-random.nextInt(h))
            }
        }

        override fun onDraw(canvas: Canvas) {
            if (bgAlpha < 240) bgAlpha += 4
            bgPaint.alpha = bgAlpha
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            frameCounter++

            for (i in columns.indices) {
                val x = i * 35f
                val headY = columns[i].toFloat()

                for (j in 0..15) {
                    val symbolY = headY - (j * 60f)

                    if (symbolY > 0 && symbolY < height + 100) {
                        val alphaFade = (255 - (j * 15)).coerceIn(0, 255)
                        paint.alpha = alphaFade


                        val text = random.nextInt(10).toString()
                        canvas.drawText(text, x, symbolY, paint)
                    }
                }

                columns[i] += 25

                if (columns[i] - 900 > height) {
                    columns[i] = -random.nextInt(height / 2)
                }
            }

            if (bgAlpha >= 240) {
                val centerPaint = Paint(paint).apply {
                    textSize = 70f
                    alpha = 255
                    textAlign = Paint.Align.CENTER
                    style = Paint.Style.FILL
                    isFakeBoldText = true
                }
                canvas.drawText("ЛИМИТ ИСЧЕРПАН", width / 2f, height / 2f, centerPaint)
                canvas.drawText("[ ЗАКРЫТЬ ]", width / 2f, height / 2f + 150f, centerPaint)
            }

            postInvalidateDelayed(50)
        }

        init {
            setOnClickListener { if (bgAlpha > 150) onClose() }
        }
    }
}