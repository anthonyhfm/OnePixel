package org.onepixel.tools

internal class LauncherPatcher(
    private val environment: LauncherEnvironment,
    private val tools: AndroidTools
) {
    fun execute() {
        val original = environment.input.resolve(relative = "launcher.apk")
        val framework = environment.input.resolve(relative = "framework-res.apk")

        environment.source.validate(file = original, kind = "launcher")
        environment.source.validate(file = framework, kind = "framework")
        validateTools()

        check(value = !environment.work.exists() || environment.work.deleteRecursively()) {
            "Cannot clean ${environment.work}"
        }

        environment.work.mkdirs()
        environment.output.mkdirs()

        val rawFramework = environment.work.resolve(relative = "framework-original")
        val decoded = environment.work.resolve(relative = "decoded")

        tools.apktool(arguments = listOf("if", "-p", rawFramework.path, framework.path))
        tools.apktool(arguments = listOf("d", "-p", rawFramework.path, "-o", decoded.path, original.path))

        SmaliPatches(module = environment.module)
            .apply(decoded = decoded)

        RuntimeImporter(
            environment = environment,
            tools = tools
        )
            .import(original = original, decoded = decoded)

        ResourceOverrides()
            .apply(
                source = environment.module.resolve(relative = "resources"),
                destination = decoded.resolve(relative = "res")
            )

        ManifestPatcher(packageName = environment.source.packageName)
            .apply(file = decoded.resolve(relative = "AndroidManifest.xml"))

        val linker = FrameworkResources(
            environment = environment,
            tools = tools
        )
            .prepare(framework = framework, decoded = decoded)

        val unsigned = environment.work.resolve(relative = "unsigned.apk")

        tools.apktool(
            arguments = listOf("b", "-p", linker.path, decoded.path, "-o", unsigned.path)
        )

        ApkSigner(
            environment = environment,
            tools = tools
        )
            .sign(unsigned = unsigned)
    }

    private fun validateTools() {
        for (name in listOf("d8", "zipalign", "apksigner")) {
            check(value = environment.buildTools.resolve(relative = name).isFile) {
                "Missing SDK tool: ${environment.buildTools}/$name"
            }
        }

        check(value = environment.androidJar.isFile) {
            "Missing SDK platform: ${environment.androidJar}"
        }
    }
}
