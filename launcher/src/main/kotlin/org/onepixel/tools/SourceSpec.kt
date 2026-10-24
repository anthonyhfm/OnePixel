package org.onepixel.tools

import java.io.File
import java.security.MessageDigest
import java.util.Properties

internal class SourceSpec(file: File) {
    private val properties = Properties().apply {
        file.inputStream().use { stream ->
            load(stream)
        }
    }

    val packageName: String = property(name = "package")
    val api: String = property(name = "api")
    val buildTools: String = property(name = "buildTools")

    fun validate(
        file: File,
        kind: String
    ) {
        require(value = file.isFile) {
            "Missing ${file.path}. Run ./onepixel pull EMULATOR first."
        }

        val hash = sha256(file = file)

        check(value = hash == property(name = "$kind.sha256")) {
            "Unsupported $kind SHA-256: $hash. Existing input was not replaced."
        }
    }

    private fun property(name: String): String {
        return properties.getProperty(name)
            ?: error(message = "Missing source property: $name")
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")

        file.inputStream().use { stream ->
            val buffer = ByteArray(size = 65_536)

            while (true) {
                val count = stream.read(buffer)

                if (count < 0) {
                    break
                }

                digest.update(buffer, 0, count)
            }
        }

        return digest
            .digest()
            .joinToString(separator = "") { byte ->
                "%02x".format(byte)
            }
    }
}
