package org.onepixel.tools

import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

internal class ResourceOverrides {
    private val factory = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
    }

    fun apply(
        source: File,
        destination: File
    ) {
        for (file in source.walkTopDown().filter { candidate -> candidate.isFile }) {
            val relative = file.relativeTo(base = source)
            val target = destination.resolve(relative = relative)
            target.parentFile.mkdirs()

            if (file.parentFile.name.startsWith(prefix = "values") && file.extension == "xml") {
                mergeValues(source = file, destination = target)
            } else {
                file.copyTo(target = target, overwrite = true)
            }
        }
    }

    private fun mergeValues(
        source: File,
        destination: File
    ) {
        val overrides = elements(document = parse(file = source))
        val documents = linkedMapOf<File, Document>()
        val modified = linkedSetOf<File>()
        val existing = destination.parentFile.listFiles()
            ?: error(message = "Cannot read resource directory: ${destination.parentFile}")

        for (file in existing.sortedBy { candidate -> candidate.name }) {
            if (file.extension == "xml") {
                documents[file] = parse(file = file)
            }
        }

        for (override in overrides) {
            val resourceKey = key(element = override)
            val matches = documents.flatMap { (file, document) ->
                elements(document = document)
                    .filter { element ->
                        key(element = element) == resourceKey
                    }
                    .map { element ->
                        file to element
                    }
            }

            check(value = matches.size <= 1) {
                "Duplicate resource definition for $resourceKey in ${destination.parentFile}"
            }

            val match = matches.singleOrNull()
            val target = match?.first ?: destination
            val document = documents.getOrPut(key = target) {
                newDocument()
            }

            val replacement = document.importNode(override, true)

            if (match != null) {
                document.documentElement.replaceChild(replacement, match.second)
            } else {
                document.documentElement.appendChild(replacement)
            }

            modified.add(element = target)
        }

        for (file in modified) {
            val document = documents.getValue(key = file)

            TransformerFactory.newInstance()
                .newTransformer()
                .apply {
                    setOutputProperty(OutputKeys.INDENT, "yes")
                    setOutputProperty(OutputKeys.ENCODING, "UTF-8")
                }
                .transform(DOMSource(document), StreamResult(file))
        }
    }

    private fun parse(file: File): Document {
        return factory.newDocumentBuilder().parse(file)
    }

    private fun newDocument(): Document {
        val document = factory.newDocumentBuilder().newDocument()
        document.appendChild(document.createElement("resources"))

        return document
    }

    private fun elements(document: Document): List<Element> {
        val children = document.documentElement.childNodes
        val elements = mutableListOf<Element>()

        for (index in 0 until children.length) {
            val child = children.item(index)

            if (child is Element) {
                elements.add(element = child)
            }
        }

        return elements
    }

    private fun key(element: Element): ResourceKey {
        val type = if (element.tagName == "item") {
            element.getAttribute("type")
        } else {
            element.tagName
        }

        return ResourceKey(type = type, name = element.getAttribute("name"))
    }

    private data class ResourceKey(
        val type: String,
        val name: String
    )
}
