package org.onepixel.compat

import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable

object IconShapeCompat {
    @JvmStatic
    fun getMask(icon: AdaptiveIconDrawable): Path {
        val path = Path()
        path.addOval(RectF(icon.bounds), Path.Direction.CW)

        return path
    }

    @JvmStatic
    fun draw(
        icon: AdaptiveIconDrawable,
        canvas: Canvas
    ) {
        val saved = canvas.save()

        try {
            canvas.clipPath(getMask(icon = icon))
            icon.background?.draw(canvas)
            icon.foreground?.draw(canvas)
        } finally {
            canvas.restoreToCount(saved)
        }
    }
}
