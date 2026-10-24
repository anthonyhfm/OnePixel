package org.onepixel.tools

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

internal class ManifestPatcher(private val packageName: String) {
    private val androidNamespace = "http://schemas.android.com/apk/res/android"

    fun apply(file: File) {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        }

        val document = factory.newDocumentBuilder().parse(file)

        for (permission in listOf("moe.shizuku.manager.permission.API_V23", "android.permission.INTERNET")) {
            ensurePermission(document = document, name = permission)
        }

        val application = document.getElementsByTagName("application").item(0)

        appendElement(
            document = document,
            parent = application,
            tag = "meta-data",
            attributes = mapOf(
                "name" to "moe.shizuku.client.V3_SUPPORT",
                "value" to "true"
            )
        )

        appendElement(
            document = document,
            parent = application,
            tag = "provider",
            attributes = mapOf(
                "name" to "rikka.shizuku.ShizukuProvider",
                "authorities" to "$packageName.shizuku",
                "multiprocess" to "false",
                "enabled" to "true",
                "exported" to "true",
                "permission" to "android.permission.INTERACT_ACROSS_USERS_FULL"
            )
        )

        TransformerFactory.newInstance()
            .newTransformer()
            .apply {
                setOutputProperty(OutputKeys.INDENT, "yes")
            }
            .transform(DOMSource(document), StreamResult(file))
    }

    private fun ensurePermission(
        document: Document,
        name: String
    ) {
        val permissions = document.getElementsByTagName("uses-permission")
        val exists = (0 until permissions.length).any { index ->
            (permissions.item(index) as Element).getAttributeNS(androidNamespace, "name") == name
        }

        if (!exists) {
            appendElement(
                document = document,
                parent = document.documentElement,
                tag = "uses-permission",
                attributes = mapOf("name" to name)
            )
        }
    }

    private fun appendElement(
        document: Document,
        parent: Node,
        tag: String,
        attributes: Map<String, String>
    ) {
        val element = document.createElement(tag)

        for ((name, value) in attributes) {
            element.setAttributeNS(androidNamespace, "android:$name", value)
        }

        parent.appendChild(element)
    }
}
