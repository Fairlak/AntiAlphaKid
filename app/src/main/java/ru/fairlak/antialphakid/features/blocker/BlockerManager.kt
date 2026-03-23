package ru.fairlak.antialphakid.features.blocker

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class BlockerManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null


    fun showOverlay() {
        if (overlayView != null) return

        closeCurrentApp()
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )


        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            gravity = Gravity.CENTER
        }

        val textView = TextView(context).apply {
            text = "Сьебалось, чудище"
            setTextColor(Color.GREEN)
            textSize = 24f
            typeface = android.graphics.Typeface.MONOSPACE
            setPadding(0, 0, 0, 50)
        }

        val closeButton = Button(context).apply {
            text = "Закрыть"
            setBackgroundColor(Color.BLACK)
            setTextColor(Color.GREEN)
            setOnClickListener {
                hideOverlay()

            }
        }

        layout.addView(textView)
        layout.addView(closeButton)


        overlayView = layout
        try {
            windowManager.addView(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun closeCurrentApp() {
        val startMain = Intent(Intent.ACTION_MAIN)
        startMain.addCategory(Intent.CATEGORY_HOME)
        startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(startMain)

    }

    fun hideOverlay() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
            overlayView = null
        }
    }
}