package org.onepixel.compat

import android.text.TextUtils

internal object SearchText {
    fun trim(value: String): String {
        var start = 0
        var end = value.length

        while (start < end && value[start].code <= 32) {
            start++
        }

        while (end > start && value[end - 1].code <= 32) {
            end--
        }

        return TextUtils.substring(value, start, end)
    }
}
