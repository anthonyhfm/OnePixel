package org.onepixel.compat

import android.app.search.Query
import android.app.search.SearchTarget
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.ArrayList
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

internal object WebSearch {
    private const val TAG = "OnePixelSearch"
    private const val DEBOUNCE_MS = 150L
    private const val MAX_QUERY_LENGTH = 512

    private val main = Handler(Looper.getMainLooper())
    private val generation = AtomicLong()
    private val suggestions = WebSuggestions(generation = generation)

    private val network = ScheduledThreadPoolExecutor(
        1,
        ThreadFactory { task ->
            Thread(task, TAG)
        }
    )

    private var pending: ScheduledFuture<*>? = null

    init {
        network.removeOnCancelPolicy = true
    }

    fun query(
        context: Context,
        rawQuery: Any,
        callback: Any
    ) {
        val current = generation.incrementAndGet()

        pending?.cancel(false)
        suggestions.cancelActive()

        pending = network.schedule(
            Runnable {
                if (current != generation.get()) {
                    return@Runnable
                }

                val targets = ArrayList<SearchTarget>()

                try {
                    val query = rawQuery as Query? ?: return@Runnable
                    populateTargets(
                        context = context,
                        query = query,
                        current = current,
                        targets = targets
                    )
                } catch (exception: Exception) {
                    Log.w(TAG, "Web suggestions unavailable", exception)
                }

                deliver(callback = callback, targets = targets, current = current)
            },
            DEBOUNCE_MS,
            TimeUnit.MILLISECONDS
        )
    }

    private fun populateTargets(
        context: Context,
        query: Query,
        current: Long,
        targets: ArrayList<SearchTarget>
    ) {
        val input = query.input

        if (SearchText.trim(value = input).isEmpty() || input.length > MAX_QUERY_LENGTH) {
            return
        }

        val values = suggestions.query(context = context, input = input, current = current)
        val group = "onepixel:web:$current"
        var index = 0

        while (index < values.size) {
            targets.add(
                SearchTargets.web(
                    context = context,
                    query = query,
                    text = values[index],
                    group = group,
                    index = index
                )
            )

            index++
        }
    }

    private fun deliver(
        callback: Any,
        targets: ArrayList<SearchTarget>,
        current: Long
    ) {
        main.post(
            Runnable {
                if (current != generation.get()) {
                    return@Runnable
                }

                try {
                    callback.javaClass
                        .getMethod("c", java.util.List::class.java)
                        .invoke(callback, targets)
                } catch (exception: Exception) {
                    Log.w(TAG, "Cannot deliver web targets", exception)
                }
            }
        )
    }
}
