package org.onepixel.tools

internal class AndroidTools(
    private val environment: LauncherEnvironment,
    private val runner: ProcessRunner
) {
    fun apktool(arguments: List<String>) {
        runner.execute(
            arguments = listOf(
                environment.javaHome.resolve(relative = "bin/java").path,
                "-Xmx2g",
                "-cp",
                System.getProperty("java.class.path"),
                "brut.apktool.Main"
            ) + arguments
        )
    }

    fun buildTool(
        name: String,
        arguments: List<String>
    ) {
        runner.execute(
            arguments = listOf(environment.buildTools.resolve(relative = name).path) + arguments
        )
    }

    fun javaTool(
        name: String,
        arguments: List<String>
    ) {
        runner.execute(
            arguments = listOf(environment.javaHome.resolve(relative = "bin/$name").path) + arguments
        )
    }
}
