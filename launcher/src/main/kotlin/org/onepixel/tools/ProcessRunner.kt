package org.onepixel.tools

import java.io.File

internal class ProcessRunner(private val directory: File) {
    fun execute(
        arguments: List<String>,
        capture: Boolean = false,
        allowFailure: Boolean = false
    ): String {
        val builder = ProcessBuilder(arguments)
            .directory(directory)
            .redirectErrorStream(true)

        if (!capture) {
            builder.inheritIO()
        }

        val process = builder.start()
        val output = if (capture) {
            process.inputStream.bufferedReader().use { reader ->
                reader.readText()
            }
        } else {
            ""
        }

        val status = process.waitFor()

        check(value = allowFailure || status == 0) {
            "${File(arguments.first()).name} failed ($status). $output"
        }

        return output
    }
}

internal fun shellQuote(value: String): String {
    return "'" + value.replace(oldValue = "'", newValue = "'\\''") + "'"
}
