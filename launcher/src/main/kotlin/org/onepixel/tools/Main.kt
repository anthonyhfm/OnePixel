package org.onepixel.tools

import kotlin.system.exitProcess

fun main(args: Array<String>) {
    try {
        val environment = LauncherEnvironment()
        val runner = ProcessRunner(directory = environment.module)
        val tools = AndroidTools(
            environment = environment,
            runner = runner
        )

        LauncherCli(
            environment = environment,
            runner = runner,
            tools = tools
        )
            .execute(arguments = args)
    } catch (exception: Exception) {
        System.err.println("OnePixel: ${exception.message}")
        exitProcess(status = 1)
    }
}
