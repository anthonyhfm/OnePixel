package org.onepixel.tools

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import java.io.File

internal class SmaliPatches(private val module: File) {
    private val classPattern = Regex(pattern = "(?m)^\\.class [^\\n]*?(L[^\\s]+;)$")

    fun apply(decoded: File) {
        val patches = module
            .resolve(relative = "patches")
            .walkTopDown()
            .filter { file ->
                file.extension == "patch"
            }
            .sortedBy { file ->
                file.path
            }
            .toList()

        check(value = patches.isNotEmpty()) {
            "No compatibility patches found."
        }

        val classes = decoded
            .resolve(relative = "smali")
            .walkTopDown()
            .filter { file ->
                file.extension == "smali"
            }
            .associateBy { file ->
                classPattern.find(input = file.readText())?.groupValues?.get(index = 1)
                    ?: error(message = "Missing class in $file")
            }

        for (patch in patches) {
            val lines = patch.readLines()

            require(value = lines.firstOrNull()?.startsWith(prefix = "# class: ") == true) {
                "Missing class selector: $patch"
            }

            val descriptor = lines.first().removePrefix(prefix = "# class: ")
            val target = classes[descriptor]
                ?: error(message = "Missing original class $descriptor")

            val diff = UnifiedDiffUtils.parseUnifiedDiff(lines.drop(n = 1))
            val result = try {
                DiffUtils.patch(target.readLines(), diff)
            } catch (exception: Exception) {
                error(message = "Patch ${patch.relativeTo(base = module)} failed: ${exception.message}")
            }

            target.writeText(text = result.joinToString(separator = "\n", postfix = "\n"))
        }

        println(message = "Applied ${patches.size} strict compatibility patches.")
    }
}
