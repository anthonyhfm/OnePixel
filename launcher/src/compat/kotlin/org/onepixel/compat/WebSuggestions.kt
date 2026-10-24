package org.onepixel.compat

import android.content.Context
import android.os.SystemClock
import org.json.JSONArray
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URI
import java.net.URLEncoder
import java.util.ArrayList
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.concurrent.atomic.AtomicLong
import javax.net.ssl.HttpsURLConnection

internal class WebSuggestions(private val generation: AtomicLong) {
    private val cache = object : LinkedHashMap<String, CachedSuggestions>(32, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedSuggestions>?): Boolean {
            return size > 32
        }
    }

    @Volatile
    private var active: HttpsURLConnection? = null

    private var backoffUntil = 0L

    fun cancelActive() {
        active?.disconnect()
    }

    fun query(
        context: Context,
        input: String,
        current: Long
    ): ArrayList<String> {
        val locale = context.resources.configuration.locales[0]
        val key = locale.toLanguageTag() + ":" + input
        val now = SystemClock.elapsedRealtime()
        val cached = cache[key]

        if (cached != null && now - cached.created < 60_000) {
            return cached.values
        }

        if (now < backoffUntil) {
            return ArrayList()
        }

        val address = "https://suggestqueries.google.com/complete/search?client=firefox&hl=" +
            URLEncoder.encode(locale.language, "UTF-8") + "&q=" + URLEncoder.encode(input, "UTF-8")

        val connection = openConnection(address = address)
        active = connection

        try {
            if (current != generation.get()) {
                return ArrayList()
            }

            val status = connection.responseCode

            if (status == 429 || status == 503) {
                backoffUntil = now + 30_000
            }

            if (status != 200) {
                return ArrayList()
            }

            val result = parseResponse(text = readResponse(connection = connection))
            cache.put(key, CachedSuggestions(created = now, values = result))

            return result
        } finally {
            if (active === connection) {
                active = null
            }

            connection.disconnect()
        }
    }

    private fun openConnection(address: String): HttpsURLConnection {
        val connection = URI(address)
            .toURL()
            .openConnection() as HttpsURLConnection?
            ?: throw IOException("No HTTPS connection")

        connection.connectTimeout = 1_500
        connection.readTimeout = 1_500
        connection.useCaches = false
        connection.setRequestProperty("Accept", "application/json")

        return connection
    }

    private fun readResponse(connection: HttpsURLConnection): String {
        val text = StringBuilder()
        val reader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))

        try {
            val buffer = CharArray(size = 2_048)

            while (true) {
                val count = reader.read(buffer)

                if (count == -1) {
                    break
                }

                if (text.length + count > 65_536) {
                    throw IOException("Response too large")
                }

                text.append(buffer, 0, count)
            }
        } finally {
            reader.close()
        }

        return text.toString()
    }

    private fun parseResponse(text: String): ArrayList<String> {
        val values = JSONArray(text).getJSONArray(1)
        val unique = LinkedHashSet<String>()
        var index = 0

        while (index < values.length() && unique.size < 10) {
            val value = SearchText.trim(value = values.optString(index))

            if (value.isNotEmpty()) {
                unique.add(value)
            }

            index++
        }

        return ArrayList(unique)
    }

    private class CachedSuggestions(
        val created: Long,
        val values: ArrayList<String>
    )
}
