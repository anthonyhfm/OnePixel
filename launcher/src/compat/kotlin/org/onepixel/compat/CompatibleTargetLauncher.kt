package org.onepixel.compat

import android.app.search.SearchTarget
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View

internal object CompatibleTargetLauncher {
    fun open(
        view: View,
        rawTarget: Any,
        event: Int
    ): Boolean {
        if (event != 3 && event != 5 && event != 6) {
            return false
        }

        try {
            val target = rawTarget as SearchTarget? ?: return false
            val extras: Bundle? = target.extras

            if (extras == null || !extras.getBoolean(SearchTargets.COMPAT_LAUNCH_EXTRA)) {
                return false
            }

            val action = target.searchAction ?: return true
            val intent = Intent(action.intent)
            val activityType = Class.forName("com.android.launcher3.views.ActivityContext")
            val activity = activityType
                .getMethod("lookupContext", Context::class.java)
                .invoke(null, view.context)

            val itemType = Class.forName("com.android.launcher3.model.data.ItemInfo")

            activityType
                .getMethod("startActivitySafely", View::class.java, Intent::class.java, itemType)
                .invoke(activity, view, intent, view.tag)

            return true
        } catch (exception: Exception) {
            Log.w("OnePixelSearch", "Could not open compatible search target", exception)
            return true
        }
    }
}
