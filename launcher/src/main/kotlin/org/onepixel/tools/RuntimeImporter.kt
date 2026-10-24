package org.onepixel.tools

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

internal class RuntimeImporter(
    private val environment: LauncherEnvironment,
    private val tools: AndroidTools
) {
    private val forbiddenRuntimeCall = Regex(
        pattern = "(?m)^\\s*(?:invoke-[^\\n]*Lkotlin/|(?:sget|iget)[^\\n]*Lkotlin/)"
    )

    fun import(
        original: File,
        decoded: File
    ) {
        val runtime = environment.work.resolve(relative = "runtime")
        runtime.mkdirs()

        val libraries = checkNotNull(value = environment.runtimeLibraries.listFiles()) {
            "Cannot read runtime libraries: ${environment.runtimeLibraries}"
        }
            .filter { file ->
                file.extension == "jar"
            }
            .sortedBy { file ->
                file.name
            }

        val jars = listOf(environment.compatJar) + libraries

        tools.buildTool(
            name = "d8",
            arguments = listOf(
                "--min-api", environment.source.api,
                "--lib", environment.androidJar.path,
                "--output", runtime.path
            ) + jars.map { file ->
                file.path
            }
        )

        val carrier = runtime.resolve(relative = "runtime.apk")
        createCarrier(original = original, runtime = runtime, carrier = carrier)

        val imported = runtime.resolve(relative = "decoded")

        tools.apktool(arguments = listOf("d", "-r", "-o", imported.path, carrier.path))

        val smali = imported.resolve(relative = "smali")
        validateRuntime(smali = smali)
        copyClasses(smali = smali, destination = decoded.resolve(relative = "smali"))
    }

    private fun createCarrier(
        original: File,
        runtime: File,
        carrier: File
    ) {
        ZipFile(original).use { source ->
            ZipOutputStream(carrier.outputStream()).use { output ->
                for (name in listOf("AndroidManifest.xml", "resources.arsc")) {
                    output.putNextEntry(ZipEntry(name))

                    source.getInputStream(source.getEntry(name)).use { input ->
                        input.copyTo(out = output)
                    }

                    output.closeEntry()
                }

                output.putNextEntry(ZipEntry("classes.dex"))
                output.write(runtime.resolve(relative = "classes.dex").readBytes())
                output.closeEntry()
            }
        }
    }

    private fun validateRuntime(smali: File) {
        val helpers = smali
            .resolve(relative = "org/onepixel")
            .walkTopDown()
            .filter { file ->
                file.extension == "smali"
            }

        for (file in helpers) {
            check(value = !forbiddenRuntimeCall.containsMatchIn(input = file.readText())) {
                "Runtime Kotlin stdlib reference in $file. Use Android/JDK APIs."
            }
        }
    }

    private fun copyClasses(
        smali: File,
        destination: File
    ) {
        for (file in smali.walkTopDown().filter { candidate -> candidate.isFile }) {
            val target = destination.resolve(relative = file.relativeTo(base = smali).path)

            check(value = !target.exists()) {
                "Runtime class collides with original APK: $target"
            }

            target.parentFile.mkdirs()
            file.copyTo(target = target)
        }
    }
}
