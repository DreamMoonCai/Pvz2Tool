package io.github.dreammooncai.pvz2tool.pop.core.rsb.model.convert

import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.File
import java.io.FileWriter
import javax.xml.parsers.DocumentBuilderFactory

object XmlConvert {

    suspend fun xmlToDat(inFile: String, bs: CoroutineBinaryStream) {
        val xmlData = File(inFile).readText()
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val xml: Document = dBuilder.parse(xmlData.byteInputStream())
        xml.documentElement.normalize()

        val root = xml.getElementsByTagName("ResourceManifest").item(0)
        val childList: NodeList = root.childNodes
        val xmlInfo = mutableListOf<XmlCompositeResourcesInfo>()
        val stringPool = hashMapOf<String, Int>()

        CoroutineBinaryStream.allocateBytes().use { bsxmlpart1 ->
            CoroutineBinaryStream.allocateBytes().use { bsxmlpart2 ->
                CoroutineBinaryStream.allocateBytes().use { bsxmlpart3 ->

                    suspend fun throwInPool(poolKey: String): Int {
                        if (poolKey !in stringPool) {
                            stringPool[poolKey] = bsxmlpart3.writePosition.toInt()
                            bsxmlpart3.writeStringByEmpty(poolKey)
                        }
                        return stringPool[poolKey]!!
                    }
                    bsxmlpart1.endian = bs.endian
                    bsxmlpart2.endian = bs.endian
                    bsxmlpart3.endian = bs.endian

                    bsxmlpart3.writeByte(0x0)
                    stringPool[""] = 0

                    for (i in 0 until childList.length) {
                        val child = childList.item(i)
                        if (child.nodeType != Node.ELEMENT_NODE) continue
                        val childEl = child as Element
                        val childChildList = child.childNodes

                        val info = XmlCompositeResourcesInfo()
                        xmlInfo.add(info)
                        info.idOffsetInPart3 = throwInPool(childEl.getAttribute("id"))
                        info.rsgpNumber = childChildList.length
                        info.rsgpInfoLibrary = arrayOfNulls(info.rsgpNumber)

                        for (j in 0 until childChildList.length) {
                            val childChild = childChildList.item(j)
                            if (childChild.nodeType != Node.ELEMENT_NODE) continue
                            val childChildEl = childChild as Element
                            val childChildChildList = childChild.childNodes

                            val xmlRsgp = XmlRsgpInfo()
                            info.rsgpInfoLibrary[j] = xmlRsgp
                            xmlRsgp.resolutionRatio = childChildEl.getAttribute("res").toInt()
                            xmlRsgp.language = childChildEl.getAttribute("loc") ?: ""
                            xmlRsgp.idOffsetInPart3 = throwInPool(childChildEl.getAttribute("id"))
                            xmlRsgp.resourcesNumber = childChildChildList.length
                            xmlRsgp.resourcesInfoLibrary = arrayOfNulls(xmlRsgp.resourcesNumber)

                            for (k in 0 until childChildChildList.length) {
                                val ccc = childChildChildList.item(k)
                                if (ccc.nodeType != Node.ELEMENT_NODE) continue
                                val cccEl = ccc as Element

                                val xmlRes = XmlResourcesInfo()
                                xmlRsgp.resourcesInfoLibrary[k] = xmlRes
                                xmlRes.type = cccEl.getAttribute("type").toUShort()
                                xmlRes.idOffsetInPart3 = throwInPool(cccEl.getAttribute("id"))
                                xmlRes.pathOffsetInPart3 = throwInPool(cccEl.getAttribute("path") ?: "")

                                cccEl.removeAttribute("type")
                                cccEl.removeAttribute("id")
                                cccEl.removeAttribute("path")

                                if (xmlRes.type == 0.toUShort()) {
                                    xmlRes.ptxInfo = XmlPtxInfo()
                                    val ptx = xmlRes.ptxInfo!!
                                    cccEl.getAttribute("imagetype").takeIf { it.isNotEmpty() }?.let {
                                        ptx.type = it.toUShort()
                                        cccEl.removeAttribute("imagetype")
                                    }
                                    cccEl.getAttribute("aflags").takeIf { it.isNotEmpty() }?.let {
                                        ptx.aflags = it.toUShort()
                                        cccEl.removeAttribute("aflags")
                                    }
                                    cccEl.getAttribute("x").takeIf { it.isNotEmpty() }?.let {
                                        ptx.x = it.toUShort()
                                        cccEl.removeAttribute("x")
                                    }
                                    cccEl.getAttribute("y").takeIf { it.isNotEmpty() }?.let {
                                        ptx.y = it.toUShort()
                                        cccEl.removeAttribute("y")
                                    }
                                    cccEl.getAttribute("ax").takeIf { it.isNotEmpty() }?.let {
                                        ptx.ax = it.toUShort()
                                        cccEl.removeAttribute("ax")
                                    }
                                    cccEl.getAttribute("ay").takeIf { it.isNotEmpty() }?.let {
                                        ptx.ay = it.toUShort()
                                        cccEl.removeAttribute("ay")
                                    }
                                    cccEl.getAttribute("aw").takeIf { it.isNotEmpty() }?.let {
                                        ptx.aw = it.toUShort()
                                        cccEl.removeAttribute("aw")
                                    }
                                    cccEl.getAttribute("ah").takeIf { it.isNotEmpty() }?.let {
                                        ptx.ah = it.toUShort()
                                        cccEl.removeAttribute("ah")
                                    }
                                    cccEl.getAttribute("rows").takeIf { it.isNotEmpty() }?.let {
                                        ptx.rows = it.toUShort()
                                        cccEl.removeAttribute("rows")
                                    }
                                    cccEl.getAttribute("cols").takeIf { it.isNotEmpty() }?.let {
                                        ptx.cols = it.toUShort()
                                        cccEl.removeAttribute("cols")
                                    }
                                    ptx.parentOffsetInPart3 = throwInPool(cccEl.getAttribute("parent") ?: "")
                                    cccEl.removeAttribute("parent")
                                }

                                val attrs = cccEl.attributes
                                xmlRes.propertiesNumber = attrs.length
                                xmlRes.propertiesInfoLibrary = arrayOfNulls(xmlRes.propertiesNumber)
                                for (l in 0 until xmlRes.propertiesNumber) {
                                    val attr = attrs.item(l)
                                    val prop = XmlPropertiesInfo()
                                    xmlRes.propertiesInfoLibrary[l] = prop
                                    prop.keyOffsetInPart3 = throwInPool(attr.nodeName)
                                    prop.valueOffsetInPart3 = throwInPool(attr.nodeValue)
                                }

                                xmlRes.infoOffsetInPart2 = bsxmlpart2.writePosition.toInt()
                                xmlRes.writePart2(bsxmlpart2)
                            }
                        }
                        info.write(bsxmlpart1)
                    }

                    bsxmlpart1.readPosition = 0
                    bsxmlpart2.readPosition = 0
                    bsxmlpart3.readPosition = 0

                    bs.writeInt32(1919251249)
                    bs.writeInt32(1)
                    bs.writeInt32(0x14)
                    bs.writeInt32((0x14 + bsxmlpart1.length).toInt())
                    bs.writeInt32((0x14 + bsxmlpart1.length + bsxmlpart2.length).toInt())

                    bsxmlpart1.copyTo(bs)
                    bsxmlpart2.copyTo(bs)
                    bsxmlpart3.copyTo(bs)
                }
            }
        }
    }

    suspend fun datToXml(bs: CoroutineBinaryStream, outFile: String) {
        File(outFile).parentFile?.mkdirs()
        val xmlPack = XmlPack().read(bs)
        FileWriter(outFile).use { sw ->
            sw.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
            sw.write("<!-- DO NOT EDIT THIS FILE. Generated by PopStudio. -->\n")
            sw.write("<ResourceManifest version=\"3\">\n")

            for (i in xmlPack.xmlInfoLibrary.indices) {
                sw.write("\n<CompositeResources id=\"${xmlPack.xmlInfoLibrary[i].id}\">\n")
                val rsgpList = xmlPack.xmlInfoLibrary[i].rsgpInfoLibrary
                for (j in rsgpList.indices) {
                    val rsgp = rsgpList[j] ?: continue
                    sw.write("  <Group id=\"${rsgp.id}\" res=\"${rsgp.resolutionRatio}\" loc=\"${rsgp.language}\">\n")
                    val resList = rsgp.resourcesInfoLibrary
                    for (k in resList.indices) {
                        val res = resList[k] ?: continue
                        sw.write("    <Res type=\"${res.type}\" id=\"${res.id}\" path=\"${res.path}\" ")
                        if (res.ptxInfoEndOffsetInPart2 != 0 && res.ptxInfoBeginOffsetInPart2 != 0) {
                            val ptx = res.ptxInfo ?: throw Exception("PTX info is null")
                            sw.write("imagetype=\"${ptx.type}\" aflags=\"${ptx.aflags}\" ")
                            if (ptx.x != 0.toUShort()) sw.write("x=\"${ptx.x}\" ")
                            if (ptx.y != 0.toUShort()) sw.write("y=\"${ptx.y}\" ")
                            if (ptx.ax != 0.toUShort()) sw.write("ax=\"${ptx.ax}\" ")
                            if (ptx.ay != 0.toUShort()) sw.write("ay=\"${ptx.ay}\" ")
                            if (ptx.aw != 0.toUShort()) sw.write("aw=\"${ptx.aw}\" ")
                            if (ptx.ah != 0.toUShort()) sw.write("ah=\"${ptx.ah}\" ")
                            if (ptx.rows != 1.toUShort()) sw.write("rows=\"${ptx.rows}\" ")
                            if (ptx.cols != 1.toUShort()) sw.write("cols=\"${ptx.cols}\" ")
                            sw.write("parent=\"${ptx.parent}\" ")
                        }
                        for (l in res.propertiesInfoLibrary.indices) {
                            val prop = res.propertiesInfoLibrary[l] ?: continue
                            sw.write("${prop.key}=\"${prop.value}\" ")
                        }
                        sw.write("/>\n")
                    }
                    sw.write("  </Group>\n")
                }
                sw.write("</CompositeResources>\n")
            }
            sw.write("\n</ResourceManifest>")
        }
    }
}

// ====================== 实体类 ======================

class XmlPack {
    val magic = 1919251249
    var version = 1
    var xmlPart1_BeginOffset = 0
    var xmlPart2_BeginOffset = 0
    var xmlPart3_BeginOffset = 0
    lateinit var xmlInfoLibrary: Array<XmlCompositeResourcesInfo>

    suspend fun read(bs: CoroutineBinaryStream): XmlPack {
        bs.idInt32(magic)
        bs.idInt32(version)
        xmlPart1_BeginOffset = bs.readInt32()
        xmlPart2_BeginOffset = bs.readInt32()
        xmlPart3_BeginOffset = bs.readInt32()
        val list = mutableListOf<XmlCompositeResourcesInfo>()
        bs.readPosition = xmlPart1_BeginOffset.toLong()

        var i = 0
        while (bs.readPosition < xmlPart2_BeginOffset) {
            val item = XmlCompositeResourcesInfo().read(bs, xmlPart3_BeginOffset)
            list.add(item)
            val temp = bs.readPosition
            for (j in 0 until item.rsgpNumber) {
                val rsgp = item.rsgpInfoLibrary[j] ?: continue
                for (k in 0 until rsgp.resourcesNumber) {
                    val res = rsgp.resourcesInfoLibrary[k] ?: continue
                    bs.readPosition = (xmlPart2_BeginOffset + res.infoOffsetInPart2).toLong()
                    res.readPart2(bs, xmlPart3_BeginOffset)
                }
            }
            bs.readPosition = temp
            i++
        }
        xmlInfoLibrary = list.toTypedArray()
        return this
    }
}

class XmlCompositeResourcesInfo {
    var idOffsetInPart3 = 0
    var rsgpNumber = 0
    val rsgpInfoLength = 0x10
    lateinit var rsgpInfoLibrary: Array<XmlRsgpInfo?>
    var id = ""

    suspend fun write(bs: CoroutineBinaryStream) {
        bs.writeInt32(idOffsetInPart3)
        bs.writeInt32(rsgpNumber)
        bs.writeInt32(rsgpInfoLength)
        for (i in 0 until rsgpNumber) {
            rsgpInfoLibrary[i]?.write(bs)
        }
    }

    suspend fun read(bs: CoroutineBinaryStream, xmlPart3Offset: Int): XmlCompositeResourcesInfo {
        idOffsetInPart3 = bs.readInt32()
        id = bs.getStringByEmpty((xmlPart3Offset + idOffsetInPart3).toLong())
        rsgpNumber = bs.readInt32()
        if (bs.readInt32() != rsgpInfoLength) throw Exception("Data mismatch")
        rsgpInfoLibrary = arrayOfNulls(rsgpNumber)
        for (i in 0 until rsgpNumber) {
            rsgpInfoLibrary[i] = XmlRsgpInfo().read(bs, xmlPart3Offset)
        }
        return this
    }
}

class XmlRsgpInfo {
    var resolutionRatio = 0
    var language = ""
    var idOffsetInPart3 = 0
    var resourcesNumber = 0
    lateinit var resourcesInfoLibrary: Array<XmlResourcesInfo?>
    var id = ""

    suspend fun write(bs: CoroutineBinaryStream) {
        bs.writeInt32(resolutionRatio)
        if (language.isEmpty()) {
            bs.writeInt32(0)
        } else {
            bs.writeString((language + "    ").take(4), bs.endian)
        }
        bs.writeInt32(idOffsetInPart3)
        bs.writeInt32(resourcesNumber)
        for (i in 0 until resourcesNumber) {
            resourcesInfoLibrary[i]?.writePart1(bs)
        }
    }

    suspend fun read(bs: CoroutineBinaryStream, xmlPart3Offset: Int): XmlRsgpInfo {
        resolutionRatio = bs.readInt32()
        language = bs.readString(4, bs.endian).replace("\u0000", "")
        idOffsetInPart3 = bs.readInt32()
        resourcesNumber = bs.readInt32()
        resourcesInfoLibrary = arrayOfNulls(resourcesNumber)
        for (i in 0 until resourcesNumber) {
            resourcesInfoLibrary[i] = XmlResourcesInfo().readPart1(bs)
        }
        id = bs.getStringByEmpty((xmlPart3Offset + idOffsetInPart3).toLong())
        return this
    }
}

class XmlResourcesInfo {
    var infoOffsetInPart2 = 0

    suspend fun writePart1(bs: CoroutineBinaryStream) {
        bs.writeInt32(infoOffsetInPart2)
    }

    suspend fun readPart1(bs: CoroutineBinaryStream): XmlResourcesInfo {
        infoOffsetInPart2 = bs.readInt32()
        return this
    }

    var empty = 0
    var type: UShort = 0u
    var headLength: UShort = 0x1Cu
    var ptxInfoEndOffsetInPart2 = 0
    var ptxInfoBeginOffsetInPart2 = 0
    var idOffsetInPart3 = 0
    var pathOffsetInPart3 = 0
    var propertiesNumber = 0
    var ptxInfo: XmlPtxInfo? = null
    lateinit var propertiesInfoLibrary: Array<XmlPropertiesInfo?>
    var id = ""
    var path = ""

    suspend fun writePart2(bs: CoroutineBinaryStream) {
        bs.writeInt32(empty)
        bs.writeUInt16(type)
        bs.writeUInt16(headLength)
        val bak = bs.writePosition
        bs.setWritePosition(bak + 8)
        bs.writeInt32(idOffsetInPart3)
        bs.writeInt32(pathOffsetInPart3)
        bs.writeInt32(propertiesNumber)

        if (type == 0.toUShort()) {
            ptxInfoBeginOffsetInPart2 = bs.writePosition.toInt()
            ptxInfo?.write(bs)
            ptxInfoEndOffsetInPart2 = bs.writePosition.toInt()
            bs.setWritePosition(bak)
            bs.writeInt32(ptxInfoEndOffsetInPart2)
            bs.writeInt32(ptxInfoBeginOffsetInPart2)
            bs.setWritePosition(ptxInfoEndOffsetInPart2.toLong())
        }

        for (i in 0 until propertiesNumber) {
            propertiesInfoLibrary[i]?.wtire(bs)
        }
    }

    suspend fun readPart2(bs: CoroutineBinaryStream, xmlPart3Offset: Int): XmlResourcesInfo {
        if (bs.readInt32() != empty) throw Exception("Data mismatch")
        type = bs.readUInt16()
        if (bs.readUInt16() != headLength) throw Exception("Data mismatch")
        ptxInfoEndOffsetInPart2 = bs.readInt32()
        ptxInfoBeginOffsetInPart2 = bs.readInt32()
        idOffsetInPart3 = bs.readInt32()
        pathOffsetInPart3 = bs.readInt32()
        id = bs.getStringByEmpty((xmlPart3Offset + idOffsetInPart3).toLong())
        path = bs.getStringByEmpty((xmlPart3Offset + pathOffsetInPart3).toLong())
        propertiesNumber = bs.readInt32()

        if (ptxInfoEndOffsetInPart2 != 0 && ptxInfoBeginOffsetInPart2 != 0) {
            ptxInfo = XmlPtxInfo().read(bs, xmlPart3Offset)
        }

        propertiesInfoLibrary = arrayOfNulls(propertiesNumber)
        for (i in 0 until propertiesNumber) {
            propertiesInfoLibrary[i] = XmlPropertiesInfo().read(bs, xmlPart3Offset)
        }
        return this
    }
}

class XmlPtxInfo {
    var type: UShort = 0u
    var aflags: UShort = 0u
    var x: UShort = 0u
    var y: UShort = 0u
    var ax: UShort = 0u
    var ay: UShort = 0u
    var aw: UShort = 0u
    var ah: UShort = 0u
    var rows: UShort = 1u
    var cols: UShort = 1u
    var parentOffsetInPart3 = 0
    var parent = ""

    suspend fun write(bs: CoroutineBinaryStream) {
        bs.writeUInt16(type)
        bs.writeUInt16(aflags)
        bs.writeUInt16(x)
        bs.writeUInt16(y)
        bs.writeUInt16(ax)
        bs.writeUInt16(ay)
        bs.writeUInt16(aw)
        bs.writeUInt16(ah)
        bs.writeUInt16(rows)
        bs.writeUInt16(cols)
        bs.writeInt32(parentOffsetInPart3)
    }

    suspend fun read(bs: CoroutineBinaryStream, xmlPart3Offset: Int): XmlPtxInfo {
        type = bs.readUInt16()
        aflags = bs.readUInt16()
        x = bs.readUInt16()
        y = bs.readUInt16()
        ax = bs.readUInt16()
        ay = bs.readUInt16()
        aw = bs.readUInt16()
        ah = bs.readUInt16()
        rows = bs.readUInt16()
        cols = bs.readUInt16()
        parentOffsetInPart3 = bs.readInt32()
        parent = bs.getStringByEmpty((xmlPart3Offset + parentOffsetInPart3).toLong())
        return this
    }
}

class XmlPropertiesInfo {
    var keyOffsetInPart3 = 0
    var empty = 0
    var valueOffsetInPart3 = 0
    var key = ""
    var value = ""

    suspend fun wtire(bs: CoroutineBinaryStream) {
        bs.writeInt32(keyOffsetInPart3)
        bs.writeInt32(empty)
        bs.writeInt32(valueOffsetInPart3)
    }

    suspend fun read(bs: CoroutineBinaryStream, xmlPart3Offset: Int): XmlPropertiesInfo {
        keyOffsetInPart3 = bs.readInt32()
        if (bs.readInt32() != empty) throw Exception("Data mismatch")
        valueOffsetInPart3 = bs.readInt32()
        key = bs.getStringByEmpty((xmlPart3Offset + keyOffsetInPart3).toLong())
        value = bs.getStringByEmpty((xmlPart3Offset + valueOffsetInPart3).toLong())
        return this
    }
}