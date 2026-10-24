package org.onepixel.compat

import android.content.Context
import android.view.View

object SearchCompat {
    @JvmStatic
    @Synchronized
    fun queryWeb(
        context: Context,
        rawQuery: Any,
        callback: Any
    ) {
        WebSearch.query(
            context = context,
            rawQuery = rawQuery,
            callback = callback
        )
    }

    @JvmStatic
    fun augmentLocal(
        context: Context,
        rawQuery: Any,
        original: List<*>
    ): List<*> {
        return LocalSearchActions.augment(
            context = context,
            rawQuery = rawQuery,
            original = original
        )
    }

    @JvmStatic
    fun openTarget(
        view: View,
        rawTarget: Any,
        event: Int
    ): Boolean {
        return CompatibleTargetLauncher.open(
            view = view,
            rawTarget = rawTarget,
            event = event
        )
    }
}
