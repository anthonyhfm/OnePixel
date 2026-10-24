package org.onepixel.tools

internal class LauncherDeviceActions(
    private val environment: LauncherEnvironment,
    private val tools: AndroidTools,
    private val device: AndroidDevice
) {
    fun install() {
        device.requireSupportedApi()

        val apk = environment.output.resolve(relative = "onepixel.apk")

        check(value = apk.isFile) {
            "Run ./onepixel patch first."
        }

        tools.buildTool(
            name = "apksigner",
            arguments = listOf("verify", apk.path)
        )

        device.execute(arguments = listOf("install", "--no-incremental", "-r", apk.path))

        device.shell(
            arguments = listOf(
                "pm",
                "grant",
                environment.source.packageName,
                "moe.shizuku.manager.permission.API_V23"
            )
        )

        checkStartup()
    }

    fun setHome(activity: String) {
        device.shell(
            arguments = listOf("cmd", "package", "set-home-activity", "--user", "0", activity)
        )
    }

    fun checkStartup() {
        val previousCrashes = crashLog()

        device.shell(arguments = listOf("am", "force-stop", environment.source.packageName))

        val launchResult = device.shell(
            arguments = listOf("am", "start", "-W", "-n", environment.homeActivity),
            capture = true
        )

        println(message = launchResult)
        Thread.sleep(3_000)

        val currentCrashes = crashLog()
        val recentCrashes = if (currentCrashes.startsWith(prefix = previousCrashes)) {
            currentCrashes.removePrefix(prefix = previousCrashes)
        } else {
            currentCrashes
        }

        val launcherCrashes = recentCrashes
            .split("FATAL EXCEPTION:")
            .drop(n = 1)
            .filter { crash ->
                crash.contains(other = "Process: ${environment.source.packageName},")
            }

        val diagnostic = environment.state.resolve(relative = "diagnostics/latest-startup-crash.txt")
        diagnostic.parentFile.mkdirs()
        diagnostic.writeText(
            text = launcherCrashes.joinToString(separator = "\n") { crash ->
                "FATAL EXCEPTION:$crash"
            }
        )

        val pid = device.shell(
            arguments = listOf("pidof", environment.source.packageName),
            capture = true,
            allowFailure = true
        ).trim()

        check(value = launcherCrashes.isEmpty() && pid.isNotEmpty()) {
            "Launcher startup failed. See $diagnostic"
        }

        println(message = "Launcher running, PID $pid; no new startup crash.")
    }

    private fun crashLog(): String {
        return device.execute(
            arguments = listOf("logcat", "-d", "-b", "crash"),
            capture = true
        )
    }
}
