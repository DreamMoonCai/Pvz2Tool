package io.github.dreammooncai.pvz2tool.pop

import io.github.dreammooncai.pvz2tool.pop.core.rsb.Rsb
import io.github.dreammooncai.pvz2tool.pop.core.rsb.util.decompressZLib
import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream
import kotlinx.coroutines.runBlocking
import java.io.File
import java.security.MessageDigest
import kotlin.time.measureTime

fun main() {
    println("=====================================")
    println("       RSB 解压/打包 完整性测试")
    println("=====================================")

    val originalRsb = File("data/main.639.com.ea.game.pvz2_drp.obb")
    if (!originalRsb.exists()) {
        println("❌ 错误：原始RSB文件不存在 -> ${originalRsb.absolutePath}")
        return
    }

    println("✅ 原始文件：${originalRsb.name} (${originalRsb.length() / 1024 / 1024} MB)")

    try {
        // 1. 解压原始文件
        println("\n==== 步骤1：解压原始RSB ====")
        val unpackOriginalDir = File("rsb_unpacked_original")
        measureTime {
            runBlocking {
                Rsb.unpack(originalRsb, unpackOriginalDir) {
                    setOnProgress { progress, message ->
                        autoRtonToJson = false
                        println("解包进度 $progress: $message")
                    }
                }
            }
        }.let {
            println("✅ 解压完成，耗时：$it")
            println("📂 输出目录：${unpackOriginalDir.absolutePath}")
        }

        // 2. 重新打包 ———— 修复这里！！！
        println("\n==== 步骤2：重新打包RSB ====")
        val repackedFolder = File("rsb_output")
        repackedFolder.mkdirs() // 强制创建目录
        val repackedRsb = File(repackedFolder, "rsb_repacked.rsb") // 目录+文件，保证父目录存在

        measureTime {
            runBlocking {
                Rsb.pack(unpackOriginalDir, repackedRsb) {
                    setOnProgress { progress, message ->
                        println("打包进度 $progress: $message")
                    }
                }
            }
        }.let {
            println("✅ 打包完成，耗时：$it")
            println("📦 输出文件：${repackedRsb.name}")
        }

        // 3. 解压重新打包后的文件
        println("\n==== 步骤3：解压重新打包的RSB用于对比 ====")
        val unpackRepackedDir = File("rsb_unpacked_repacked")
        measureTime {
            runBlocking {
                Rsb.unpack(repackedRsb, unpackRepackedDir) {
                    setOnProgress { progress, message ->
                        autoRtonToJson = false
                        println("解包进度 $progress: $message")
                    }
                }
            }
        }.let {
            println("✅ 对比用解压完成，耗时：$it")
        }

        // 4. 对比文件差异
        println("\n==== 步骤4：文件完整性对比（关键）====")
        val (isSame, diffReport) = compareTwoDirectories(unpackOriginalDir, unpackRepackedDir)

        if (isSame) {
            println("🎉 测试完全成功！解压(${originalRsb.length()}) → 打包(${repackedRsb.length()}) → 再解压，所有文件完全一致！")
            println(getFileMd5(originalRsb))
            println(getFileMd5(repackedRsb))
        } else {
            println("⚠️ 测试发现差异：")
            println(diffReport)
        }
    } catch (e: Exception) {
        println("\n❌ 测试崩溃：${e.message}")
        e.printStackTrace()
    }
}

// =============================================
// 核心工具：递归对比两个文件夹
// =============================================
fun compareTwoDirectories(dir1: File, dir2: File): Pair<Boolean, String> {
    val list1 = flattenAllFiles(dir1)
    val list2 = flattenAllFiles(dir2)

    val relPaths1 = list1.keys.toSet()
    val relPaths2 = list2.keys.toSet()

    val onlyInDir1 = relPaths1 - relPaths2
    val onlyInDir2 = relPaths2 - relPaths1
    val commonFiles = relPaths1 intersect relPaths2

    val contentDiffFiles = mutableListOf<String>()
    for (path in commonFiles) {
        val f1 = list1[path]!!
        val f2 = list2[path]!!
        if (f1.length() != f2.length() || getFileMd5(f1) != getFileMd5(f2)) {
            contentDiffFiles.add(path)
        }
    }

    val isAllSame = onlyInDir1.isEmpty() && onlyInDir2.isEmpty() && contentDiffFiles.isEmpty()
    val report = buildString {
        if (onlyInDir1.isNotEmpty()) {
            appendLine("\n【仅存在于 原始解压文件夹】(${onlyInDir1.size}个):")
            onlyInDir1.sorted().take(20).forEach { appendLine("  - $it") }
            if (onlyInDir1.size > 20) appendLine("  ... 还有 ${onlyInDir1.size - 20} 个文件")
        }
        if (onlyInDir2.isNotEmpty()) {
            appendLine("\n【仅存在于 重新打包解压文件夹】(${onlyInDir2.size}个):")
            onlyInDir2.sorted().take(20).forEach { appendLine("  - $it") }
            if (onlyInDir2.size > 20) appendLine("  ... 还有 ${onlyInDir2.size - 20} 个文件")
        }
        if (contentDiffFiles.isNotEmpty()) {
            appendLine("\n【文件内容不一致】(${contentDiffFiles.size}个):")
            contentDiffFiles.sorted().take(20).forEach { appendLine("  - $it") }
            if (contentDiffFiles.size > 20) appendLine("  ... 还有 ${contentDiffFiles.size - 20} 个文件")
        }
    }

    return isAllSame to report
}

fun flattenAllFiles(root: File): Map<String, File> {
    val map = mutableMapOf<String, File>()
    fun scan(dir: File, prefix: String) {
        dir.listFiles()?.forEach { f ->
            val relPath = "$prefix${f.name}".replace("\\", "/")
            if (f.isFile) map[relPath] = f
            if (f.isDirectory) scan(f, "$relPath/")
        }
    }
    scan(root, "")
    return map
}

fun getFileMd5(file: File): String {
    val md = MessageDigest.getInstance("MD5")
    file.inputStream().use { input ->
        val buffer = ByteArray(8192)
        var len: Int
        while (input.read(buffer).also { len = it } != -1) {
            md.update(buffer, 0, len)
        }
    }
    return md.digest().joinToString("") { "%02x".format(it) }
}

fun printFileDifference(file1: File, file2: File) {
    println("\n=====================================")
    println("  文件差异对比：")
    println("  1 -> ${file1.name}")
    println("  2 -> ${file2.name}")
    println("=====================================")

    val bytes1 = file1.readBytes()
    val bytes2 = file2.readBytes()

    val len1 = bytes1.size
    val len2 = bytes2.size

    println("文件1长度：$len1 字节")
    println("文件2长度：$len2 字节")

    if (len1 != len2) {
        println("\n❌ 长度不相等！")
    }

    val maxLen = maxOf(len1, len2)
    var diffCount = 0
    var firstDiff: Int? = null

    for (i in 0 until maxLen) {
        val b1 = if (i < len1) bytes1[i] else -1
        val b2 = if (i < len2) bytes2[i] else -1

        if (b1 != b2) {
            if (firstDiff == null) {
                firstDiff = i
            }

            // 最多打印前 100 处差异，避免输出爆炸
            if (diffCount < 100) {
                println("偏移 %04X → 1: %02X  2: %02X".format(i, b1.toInt() and 0xFF, b2.toInt() and 0xFF))
            }
            diffCount++
        }
    }

    println("\n━━━━━━ 结果 ━━━━━━")
    if (diffCount == 0) {
        println("✅ 两个文件完全相同！")
    } else {
        println("❌ 共 $diffCount 处差异")
        println("🎯 第一个差异位置：偏移 ${firstDiff ?: "未知"} (0x${firstDiff?.toString(16)})")
    }
}