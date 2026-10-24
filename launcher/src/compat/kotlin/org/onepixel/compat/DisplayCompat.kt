package org.onepixel.compat

import android.content.Context
import android.graphics.Rect
import android.view.Display
import android.view.WindowManager

object DisplayCompat {
    @JvmStatic
    fun inset(
        rect: Rect,
        insets: Rect
    ) {
        rect.inset(insets.left, insets.top, insets.right, insets.bottom)
    }

    @Suppress("DEPRECATION")
    @JvmStatic
    fun getDisplayId(context: Context): Int {
        val display = contextDisplay(context = context)

        return display?.displayId
            ?: context.getSystemService(WindowManager::class.java).defaultDisplay.displayId
    }

    private fun contextDisplay(context: Context): Display? {
        return try {
            context.display
        } catch (_: UnsupportedOperationException) {
            null
        }
    }
}
