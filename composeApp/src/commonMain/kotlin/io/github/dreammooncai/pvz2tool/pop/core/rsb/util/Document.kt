package io.github.dreammooncai.pvz2tool.pop.core.rsb.util

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

// ------------- 原生 DOM 必备工具方法 -------------


// XML 解析辅助方法
fun File.readXml(): Document {
    val factory = DocumentBuilderFactory.newInstance()
    val builder = factory.newDocumentBuilder()
    return builder.parse(this)
}

/**
 * 原生替代 C# SelectSingleNode(xpath)
 * 只找第一层直接子节点，不做复杂 XPath
 */
fun Document.getFirstChildByTag(tag: String): Node? {
    val nodeList = this.childNodes
    for (i in 0 until nodeList.length) {
        val node = nodeList.item(i)
        if (node.nodeName == tag) {
            return node
        }
    }
    return null
}

/**
 * 遍历 NodeList（原生版）
 */
inline fun NodeList.forEach(action: (Node) -> Unit) {
    for (i in 0 until this.length) {
        val node = this.item(i) ?: continue
        action(node)
    }
}

/**
 * 带索引遍历 NodeList（替代 forEachIndexed）
 */
inline fun NodeList.forEachIndexed(action: (Int, Node) -> Unit) {
    for (i in 0 until this.length) {
        val node = this.item(i) ?: continue
        action(i, node)
    }
}

fun NodeList.toList(): List<Node> = List(length) { item(it) }

fun NodeList.asSequence(): Sequence<Node> = (0 until length).asSequence().map { item(it) }

val Node.childes get() = childNodes.toList()

/**
 * 获取节点属性（安全版）
 */
fun Node.getAttr(name: String): String? {
    val attr = runCatching { attributes?.getNamedItem(name) }.getOrNull()
    return attr?.nodeValue
}

fun createXmlDocument(): Document {
    return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
}

/** 创建带根节点的XML文档 */
fun createXmlDocument(rootTagName: String): Pair<Document, Element> {
    val doc = createXmlDocument()
    val root = doc.createElement(rootTagName)
    doc.appendChild(root)
    return doc to root
}

/** 将XML文档写入文件（自动格式化、UTF-8编码） */
fun Document.writeXmlToFile(file: File) {
    val transformer = TransformerFactory.newInstance().newTransformer()
    // 格式化输出（缩进）
    transformer.setOutputProperty(OutputKeys.INDENT, "yes")
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
    transformer.setOutputProperty(OutputKeys.METHOD, "xml")
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")

    val source = DOMSource(this)
    val result = StreamResult(file)
    transformer.transform(source, result)
}

/** 给元素添加属性 */
fun Element.setAttr(name: String, value: String) {
    this.setAttribute(name, value)
}