package org.onepixel.compat

import android.content.Context
import android.content.res.Resources
import android.view.WindowManager

object WindowCornerCompat {
    @JvmStatic
    fun supportsRoundedCorners(resources: Resources): Boolean {
        val identifier = resources.getIdentifier("config_supportsRoundedCornersOnWindows", "bool", "android")

        return identifier == 0 || resources.getBoolean(identifier)
    }

    @JvmStatic
    fun getRadius(context: Context): Float {
        if (!supportsRoundedCorners(resources = context.resources)) {
            return 0f
        }

        try {
            val insets = context.getSystemService(WindowManager::class.java).currentWindowMetrics.windowInsets
            var radius = Int.MAX_VALUE
            var position = 0

            while (position < 4) {
                val corner = insets.getRoundedCorner(position)

                if (corner != null && corner.radius > 0) {
                    radius = Math.min(radius, corner.radius)
                }

                position++
            }

            if (radius != Int.MAX_VALUE) {
                return radius.toFloat()
            }
        } catch (_: UnsupportedOperationException) {
            return resourceRadius(resources = context.resources)
        }

        return resourceRadius(resources = context.resources)
    }

    private fun resourceRadius(resources: Resources): Float {
        val identifier = resources.getIdentifier("rounded_corner_radius", "dimen", "android")

        return if (identifier == 0) {
            0f
        } else {
            resources.getDimension(identifier)
        }
    }
}
