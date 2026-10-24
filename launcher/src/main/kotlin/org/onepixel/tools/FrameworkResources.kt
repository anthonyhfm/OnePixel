package org.onepixel.tools

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

internal class FrameworkResources(
    private val environment: LauncherEnvironment,
    private val tools: AndroidTools
) {
    fun prepare(
        framework: File,
        decoded: File
    ): File {
        val linker = environment.work.resolve(relative = "framework-linker")
        linker.mkdirs()

        val resources = linker.resolve(relative = "resources.arsc")

        ZipFile(framework).use { archive ->
            archive.getInputStream(archive.getEntry("resources.arsc")).use { input ->
                resources.writeBytes(array = input.readBytes())
            }
        }

        tools.apktool(arguments = listOf("pr", resources.path))
        createLinkerFramework(framework = framework, linker = linker, resources = resources)
        normalizeReferences(decoded = decoded)

        return linker
    }

    private fun createLinkerFramework(
        framework: File,
        linker: File,
        resources: File
    ) {
        ZipFile(framework).use { source ->
            ZipOutputStream(linker.resolve(relative = "1.apk").outputStream()).use { output ->
                for (entry in source.entries()) {
                    output.putNextEntry(ZipEntry(entry.name))

                    if (entry.name == "resources.arsc") {
                        output.write(resources.readBytes())
                    } else {
                        source.getInputStream(entry).use { input ->
                            input.copyTo(out = output)
                        }
                    }

                    output.closeEntry()
                }
            }
        }
    }

    private fun normalizeReferences(decoded: File) {
        val files = decoded
            .resolve(relative = "res")
            .walkTopDown()
            .filter { file ->
                file.extension == "xml"
            }

        for (file in files) {
            val source = file.readText()
            val normalized = source
                .replace(oldValue = "@*android:", newValue = "@android:")
                .replace(oldValue = "?*android:", newValue = "?android:")

            if (source != normalized) {
                file.writeText(text = normalized)
            }
        }
    }
}
