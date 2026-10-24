package org.onepixel.tools

internal class AndroidDevice(
    private val serial: String,
    private val environment: LauncherEnvironment,
    private val runner: ProcessRunner
) {
    fun execute(
        arguments: List<String>,
        capture: Boolean = false,
        allowFailure: Boolean = false
    ): String {
        return runner.execute(
            arguments = listOf(environment.adb, "-s", serial) + arguments,
            capture = capture,
            allowFailure = allowFailure
        )
    }

    fun shell(
        arguments: List<String>,
        capture: Boolean = false,
        allowFailure: Boolean = false
    ): String {
        val command = arguments.joinToString(separator = " ") { argument ->
            shellQuote(value = argument)
        }

        return execute(
            arguments = listOf("shell", command),
            capture = capture,
            allowFailure = allowFailure
        )
    }

    fun property(name: String): String {
        return shell(
            arguments = listOf("getprop", name),
            capture = true
        ).trim()
    }

    fun requireSupportedApi() {
        check(value = property(name = "ro.build.version.sdk") == environment.source.api) {
            "Use an Android device with API ${environment.source.api}."
        }
    }
}
