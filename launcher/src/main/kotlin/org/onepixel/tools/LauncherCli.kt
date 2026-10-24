package org.onepixel.tools

internal class LauncherCli(
    private val environment: LauncherEnvironment,
    private val runner: ProcessRunner,
    private val tools: AndroidTools
) {
    fun execute(arguments: Array<String>) {
        environment.prepareState()

        environment.state
            .resolve(relative = "operation.lock")
            .outputStream()
            .channel
            .use { channel ->
                val lock = channel.tryLock()
                    ?: error(message = "Another OnePixel operation is running.")

                lock.use {
                    dispatch(arguments = arguments)
                }
            }
    }

    private fun dispatch(arguments: Array<String>) {
        when (arguments.firstOrNull()) {
            "pull" -> {
                EmulatorPull(environment = environment)
                    .execute(device = device(arguments = arguments))
            }

            "patch" -> {
                require(value = arguments.size == 1) {
                    "Usage: ./onepixel patch"
                }

                LauncherPatcher(
                    environment = environment,
                    tools = tools
                )
                    .execute()
            }

            "apply", "check", "home", "restore-home" -> {
                val actions = LauncherDeviceActions(
                    environment = environment,
                    tools = tools,
                    device = device(arguments = arguments)
                )

                when (arguments[0]) {
                    "apply" -> actions.install()
                    "check" -> actions.checkStartup()
                    "home" -> actions.setHome(activity = environment.homeActivity)
                    "restore-home" -> actions.setHome(
                        activity = "com.sec.android.app.launcher/.activities.LauncherActivity"
                    )
                }
            }

            "shizuku" -> {
                ShizukuStarter(device = device(arguments = arguments))
                    .execute()
            }

            null, "help", "--help" -> {
                printHelp()
            }

            else -> {
                error(message = "Unknown command. Run ./onepixel help")
            }
        }
    }

    private fun device(arguments: Array<String>): AndroidDevice {
        require(value = arguments.size == 2 && arguments[1].matches(regex = Regex(pattern = "[A-Za-z0-9_.:-]+"))) {
            "Provide exactly one ADB serial, e.g. ./onepixel ${arguments.firstOrNull()} emulator-5556"
        }

        return AndroidDevice(
            serial = arguments[1],
            environment = environment,
            runner = runner
        )
    }

    private fun printHelp() {
        println(
            message = """
                Usage: ./onepixel <command> [serial]
                  pull EMULATOR   Pull and validate the supported launcher and framework
                  patch           Rebuild from clean input, align and sign locally
                  apply DEVICE    Install an update, grant Shizuku access and check startup
                  check DEVICE    Check startup and save new crash diagnostics
                  shizuku DEVICE  Start the installed Shizuku+ server through ADB
                  home DEVICE     Make Pixel Launcher the default home app
                  restore-home DEVICE  Restore One UI Home on the tested Samsung
                Local state: ${environment.state}
            """.trimIndent()
        )
    }
}
