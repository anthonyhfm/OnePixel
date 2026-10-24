package org.onepixel.tools

import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

internal class EmulatorPull(private val environment: LauncherEnvironment) {
    fun execute(device: AndroidDevice) {
        check(value = device.property(name = "ro.kernel.qemu") == "1") {
            "Pull requires an emulator."
        }

        device.requireSupportedApi()

        val staging = environment.state.resolve(relative = "pull-${UUID.randomUUID()}")
        staging.mkdirs()

        try {
            val paths = device.shell(
                arguments = listOf("pm", "path", environment.source.packageName),
                capture = true
            )
                .lineSequence()
                .filter { line ->
                    line.startsWith(prefix = "package:")
                }
                .map { line ->
                    line.removePrefix(prefix = "package:")
                }
                .toList()

            check(value = paths.size == 1) {
                "Expected one launcher APK, found $paths"
            }

            device.execute(
                arguments = listOf("pull", paths.single(), staging.resolve(relative = "launcher.apk").path)
            )

            device.execute(
                arguments = listOf(
                    "pull",
                    "/system/framework/framework-res.apk",
                    staging.resolve(relative = "framework-res.apk").path
                )
            )

            environment.source.validate(
                file = staging.resolve(relative = "launcher.apk"),
                kind = "launcher"
            )

            environment.source.validate(
                file = staging.resolve(relative = "framework-res.apk"),
                kind = "framework"
            )

            environment.input.mkdirs()

            for (name in listOf("launcher.apk", "framework-res.apk")) {
                Files.move(
                    staging.resolve(relative = name).toPath(),
                    environment.input.resolve(relative = name).toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }

            println(message = "Validated emulator inputs: ${environment.input}")
        } finally {
            staging.deleteRecursively()
        }
    }
}
