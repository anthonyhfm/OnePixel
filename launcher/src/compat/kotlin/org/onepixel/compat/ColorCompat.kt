package org.onepixel.compat

object ColorCompat {
    @JvmStatic
    fun blendARGB(
        first: Int,
        second: Int,
        ratio: Float
    ): Int {
        var result = 0
        var shift = 24

        while (shift >= 0) {
            val firstChannel = (first ushr shift) and 255
            val secondChannel = (second ushr shift) and 255
            val blendedChannel = (firstChannel * (1f - ratio) + secondChannel * ratio).toInt()

            result = result or (blendedChannel shl shift)
            shift -= 8
        }

        return result
    }
}
