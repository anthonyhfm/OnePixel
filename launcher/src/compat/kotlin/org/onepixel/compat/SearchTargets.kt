package org.onepixel.compat

import android.app.SearchManager
import android.app.search.Query
import android.app.search.SearchAction
import android.app.search.SearchTarget
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Bundle
import android.os.Process

internal object SearchTargets {
    const val GOOGLE_PACKAGE = "com.google.android.googlequicksearchbox"
    const val APP_RESULT_TYPE = 512
    const val COMPAT_LAUNCH_EXTRA = "onepixel_compat_launch"

    private const val WEB_RESULT_TYPE = 131072
    private const val FALLBACK_GROUP = "NO_FULFILLMENT:0::fallback_header"

    fun web(
        context: Context,
        query: Query,
        text: String,
        group: String,
        index: Int
    ): SearchTarget {
        val iconId = context.resources.getIdentifier("ic_suggest_search", "drawable", context.packageName)
        val icon = if (iconId == 0) {
            null
        } else {
            Icon.createWithResource(context, iconId)
        }

        return create(
            query = query,
            title = text,
            packageName = GOOGLE_PACKAGE,
            intent = googleIntent(input = text),
            id = "$group:$index",
            group = group,
            resultType = WEB_RESULT_TYPE,
            icon = icon
        )
    }

    fun app(
        context: Context,
        query: Query,
        packageName: String,
        labelResource: String,
        intent: Intent
    ): SearchTarget {
        val labelId = context.resources.getIdentifier(labelResource, "string", context.packageName)
        val label = if (labelId == 0) {
            packageName
        } else {
            context.getString(labelId)
        }

        return create(
            query = query,
            title = label,
            packageName = packageName,
            intent = intent,
            id = "onepixel:app:$packageName",
            group = FALLBACK_GROUP,
            resultType = APP_RESULT_TYPE,
            icon = null
        )
    }

    fun googleIntent(input: String): Intent {
        return Intent(Intent.ACTION_WEB_SEARCH)
            .setPackage(GOOGLE_PACKAGE)
            .putExtra(SearchManager.QUERY, input)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun create(
        query: Query,
        title: String,
        packageName: String,
        intent: Intent,
        id: String,
        group: String,
        resultType: Int,
        icon: Icon?
    ): SearchTarget {
        val actionExtras = Bundle()
        actionExtras.putBoolean("should_start_for_result", false)
        actionExtras.putBoolean("skip_logging_in_target_handler", true)

        val action = SearchAction.Builder(id, title)
            .setIntent(intent)
            .setExtras(actionExtras)

        if (icon != null) {
            action.setIcon(icon)
        }

        val extras = Bundle()
        extras.putString("group_id", group)

        if (resultType == WEB_RESULT_TYPE) {
            extras.putString("suggest_raw_text", title)
            extras.putBoolean("support_query_builder", true)
        }

        extras.putBoolean(COMPAT_LAUNCH_EXTRA, resultType == WEB_RESULT_TYPE || GOOGLE_PACKAGE.equals(packageName))
        extras.putParcelable("input_query", query)
        extras.putString("data_donated_source", "onepixel_web_suggest")

        return SearchTarget.Builder(resultType, "short_icon_row", id)
            .setPackageName(packageName)
            .setUserHandle(Process.myUserHandle())
            .setExtras(extras)
            .setSearchAction(action.build())
            .build()
    }
}
