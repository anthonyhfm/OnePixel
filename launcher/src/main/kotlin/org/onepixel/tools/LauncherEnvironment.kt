package org.onepixel.tools

import java.io.File

internal class LauncherEnvironment {
    val module = File(requiredProperty(name = "onepixel.module"))

    val state = File(
        System.getenv("ONEPIXEL_HOME")
            ?: "${System.getProperty("user.home")}/.local/share/onepixel"
    )
        .canonicalFile
        .resolve(relative = "launcher")

    val source = SourceSpec(file = module.resolve(relative = "source.properties"))
    val input = state.resolve(relative = "input")
    val work = state.resolve(relative = "work")
    val output = state.resolve(relative = "output")
    val key = state.resolve(relative = "keys/onepixel.keystore")
    val homeActivity = "${source.packageName}/.NexusLauncherActivity"

    val sdk = File(
        System.getenv("ANDROID_HOME")
            ?: System.getenv("ANDROID_SDK_ROOT")
            ?: "${System.getProperty("user.home")}/Library/Android/sdk"
    )

    val buildTools = File(
        System.getenv("ONEPIXEL_BUILD_TOOLS")
            ?: sdk.resolve(relative = "build-tools/${source.buildTools}").path
    )

    val androidJar = sdk.resolve(relative = "platforms/android-${source.api}/android.jar")
    val javaHome = File(System.getProperty("java.home"))
    val compatJar = File(requiredProperty(name = "onepixel.compat"))
    val runtimeLibraries = File(requiredProperty(name = "onepixel.libraries"))

    val adb: String = sdk
        .resolve(relative = "platform-tools/adb")
        .let { executable ->
            if (executable.isFile) {
                executable.path
            } else {
                "adb"
            }
        }

    fun prepareState() {
        require(value = !state.toPath().startsWith(module.parentFile.canonicalFile.toPath())) {
            "ONEPIXEL_HOME must be outside the repository."
        }

        state.mkdirs()
    }

    private fun requiredProperty(name: String): String {
        return System.getProperty(name)
            ?: error(message = "Missing JVM property: $name. Run through ./onepixel.")
    }
}
