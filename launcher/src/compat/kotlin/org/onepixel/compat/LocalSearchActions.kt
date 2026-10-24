package org.onepixel.compat

import android.app.SearchManager
import android.app.search.Query
import android.app.search.SearchTarget
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import java.util.ArrayList

internal object LocalSearchActions {
    fun augment(
        context: Context,
        rawQuery: Any,
        original: List<*>
    ): List<*> {
        val targets = ArrayList<Any?>(original)

        try {
            val query = rawQuery as Query? ?: return original
            val input = query.input

            if (SearchText.trim(value = input).isEmpty()) {
                return original
            }

            val firstAction = firstAppAction(targets = targets)

            if (firstAction < 0) {
                return original
            }

            targets[firstAction] = SearchTargets.app(
                context = context,
                query = query,
                packageName = SearchTargets.GOOGLE_PACKAGE,
                labelResource = "fallback_search_google",
                intent = SearchTargets.googleIntent(input = input)
            )

            val header = Class.forName("com.android.app.search.SearchTargetGenerator")
                .getField("SECTION_HEADER_TARGET")
                .get(null)

            targets.add(firstAction, header)
            addDestinations(context = context, query = query, input = input, targets = targets)
        } catch (exception: Exception) {
            Log.w("OnePixelSearch", "App search actions unavailable", exception)
            return original
        }

        return targets
    }

    private fun firstAppAction(targets: ArrayList<Any?>): Int {
        var index = 0

        while (index < targets.size) {
            val item = targets[index]

            if (item is SearchTarget && item.resultType == SearchTargets.APP_RESULT_TYPE) {
                return index
            }

            index++
        }

        return -1
    }

    private fun addDestinations(
        context: Context,
        query: Query,
        input: String,
        targets: ArrayList<Any?>
    ) {
        val youtube = Intent(Intent.ACTION_SEARCH)
            .setPackage("com.google.android.youtube")
            .putExtra(SearchManager.QUERY, input)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        addAppAction(
            context = context,
            query = query,
            packageName = "com.google.android.youtube",
            labelResource = "fallback_search_youtube",
            intent = youtube,
            targets = targets
        )

        val maps = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(input)))
            .setPackage("com.google.android.apps.maps")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        addAppAction(
            context = context,
            query = query,
            packageName = "com.google.android.apps.maps",
            labelResource = "fallback_search_maps",
            intent = maps,
            targets = targets
        )

        val playUri = Uri.Builder()
            .scheme("market")
            .authority("search")
            .appendQueryParameter("q", input)
            .appendQueryParameter("c", "apps")
            .build()

        val play = Intent(Intent.ACTION_VIEW, playUri)
            .setPackage("com.android.vending")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        addAppAction(
            context = context,
            query = query,
            packageName = "com.android.vending",
            labelResource = "onepixel_search_play_store",
            intent = play,
            targets = targets
        )

        val settings = Intent("android.settings.APP_SEARCH_SETTINGS")
            .putExtra(SearchManager.QUERY, input)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        addAppAction(
            context = context,
            query = query,
            packageName = "com.android.settings",
            labelResource = "fallback_search_settings",
            intent = settings,
            targets = targets
        )

        val contacts = ContactsSearch.intent(context = context, input = input)
        val component = contacts?.component

        if (contacts != null && component != null) {
            addAppAction(
                context = context,
                query = query,
                packageName = component.packageName,
                labelResource = "fallback_search_contacts",
                intent = contacts,
                targets = targets
            )
        }
    }

    private fun addAppAction(
        context: Context,
        query: Query,
        packageName: String,
        labelResource: String,
        intent: Intent,
        targets: ArrayList<Any?>
    ) {
        if (intent.resolveActivity(context.packageManager) != null) {
            targets.add(
                SearchTargets.app(
                    context = context,
                    query = query,
                    packageName = packageName,
                    labelResource = labelResource,
                    intent = intent
                )
            )
        }
    }
}
