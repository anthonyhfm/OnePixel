package org.onepixel.tools

import java.io.File

internal class ApkSigner(
    private val environment: LauncherEnvironment,
    private val tools: AndroidTools
) {
    fun sign(unsigned: File) {
        val aligned = environment.work.resolve(relative = "aligned.apk")
        val signed = environment.work.resolve(relative = "signed.apk")

        tools.buildTool(
            name = "zipalign",
            arguments = listOf("-f", "-p", "4", unsigned.path, aligned.path)
        )

        ensureKey()

        tools.buildTool(
            name = "apksigner",
            arguments = listOf(
                "sign",
                "--ks", environment.key.path,
                "--ks-key-alias", "onepixel",
                "--ks-pass", "pass:android",
                "--key-pass", "pass:android",
                "--out", signed.path,
                aligned.path
            )
        )

        tools.buildTool(
            name = "apksigner",
            arguments = listOf("verify", signed.path)
        )

        tools.buildTool(
            name = "zipalign",
            arguments = listOf("-c", "-p", "4", signed.path)
        )

        val output = environment.output.resolve(relative = "onepixel.apk")
        signed.copyTo(target = output, overwrite = true)

        println(message = "Patched and verified: $output")
    }

    private fun ensureKey() {
        val key = environment.key

        if (key.exists()) {
            return
        }

        key.parentFile.mkdirs()

        tools.javaTool(
            name = "keytool",
            arguments = listOf(
                "-genkeypair",
                "-keystore", key.path,
                "-storepass", "android",
                "-keypass", "android",
                "-alias", "onepixel",
                "-dname", "CN=OnePixel Development",
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-validity", "10000"
            )
        )

        key.setReadable(false, false)
        key.setWritable(false, false)
        key.setReadable(true, true)
        key.setWritable(true, true)

        println(message = "Created development key. Keep it for future updates: $key")
    }
}
