package org.onepixel.compat

import android.app.SearchManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import java.util.regex.Pattern

internal object ContactsSearch {
    private val contactSearchName = Pattern.compile("contactsearchactivity$", Pattern.CASE_INSENSITIVE)
    private val searchName = Pattern.compile("search", Pattern.CASE_INSENSITIVE)
    private val peopleName = Pattern.compile("people", Pattern.CASE_INSENSITIVE)

    fun intent(
        context: Context,
        input: String
    ): Intent? {
        val packages = arrayOf(
            "com.google.android.contacts",
            "com.samsung.android.app.contacts",
            "com.android.contacts"
        )

        for (packageName in packages) {
            val intent = Intent(Intent.ACTION_SEARCH)
                .setPackage(packageName)
                .putExtra(SearchManager.QUERY, input)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

            val best = bestActivity(context = context, intent = intent)

            if (best != null) {
                return intent.setComponent(
                    ComponentName(best.activityInfo.packageName, best.activityInfo.name)
                )
            }
        }

        return null
    }

    private fun bestActivity(
        context: Context,
        intent: Intent
    ): ResolveInfo? {
        val candidates = context.packageManager.queryIntentActivities(intent, 0)
        var best: ResolveInfo? = null
        var bestScore = -1
        var index = 0

        while (index < candidates.size) {
            val candidate = candidates[index]
            index++

            if (!candidate.activityInfo.exported) {
                continue
            }

            val score = activityScore(name = candidate.activityInfo.name)

            if (score > bestScore) {
                best = candidate
                bestScore = score
            }
        }

        return best
    }

    private fun activityScore(name: String): Int {
        return when {
            contactSearchName.matcher(name).find() -> 20
            searchName.matcher(name).find() -> 10
            peopleName.matcher(name).find() -> 5
            else -> 0
        }
    }
}
