package org.onepixel.tools

internal class ShizukuStarter(private val device: AndroidDevice) {
    fun execute() {
        if (isRunning()) {
            println(message = "Shizuku+ already running.")
            return
        }

        for (manager in listOf("af.shizuku.plus.api", "moe.shizuku.privileged.api")) {
            val info = device.shell(
                arguments = listOf("dumpsys", "package", manager),
                capture = true
            )

            if (!info.contains(other = "versionName=Shizuku+ ")) {
                continue
            }

            val apk = managerApk(packageName = manager) ?: continue
            val architecture = architecture()
            val libraryPath = apk.substringBeforeLast(delimiter = '/') + "/lib/$architecture"
            val command = "LD_LIBRARY_PATH=${shellQuote(value = libraryPath)} " +
                "${shellQuote(value = "$libraryPath/libshizuku.so")} --apk=${shellQuote(value = apk)}"

            device.execute(arguments = listOf("shell", command))

            check(value = isRunning()) {
                "Shizuku+ starter returned without a running server."
            }

            println(message = "Shizuku+ running.")
            return
        }

        error(message = "Install the supported Shizuku+ manager first.")
    }

    private fun isRunning(): Boolean {
        return device.shell(
            arguments = listOf("ps", "-A", "-o", "NAME"),
            capture = true
        )
            .lineSequence()
            .any { name ->
                name.trim() == "shizuku_plus_server"
            }
    }

    private fun managerApk(packageName: String): String? {
        return device.shell(
            arguments = listOf("pm", "path", packageName),
            capture = true
        )
            .lineSequence()
            .filter { path ->
                path.startsWith(prefix = "package:")
            }
            .map { path ->
                path.removePrefix(prefix = "package:")
            }
            .firstOrNull { path ->
                path.endsWith(suffix = "/base.apk")
            }
    }

    private fun architecture(): String {
        val abis = device.property(name = "ro.product.cpu.abilist")

        return when {
            abis.contains(other = "arm64-v8a") -> "arm64"
            abis.contains(other = "armeabi") -> "arm"
            else -> error(message = "Unsupported Shizuku+ ABI: $abis")
        }
    }
}
