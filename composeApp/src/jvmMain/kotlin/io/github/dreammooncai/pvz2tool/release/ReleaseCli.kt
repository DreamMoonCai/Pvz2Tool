package io.github.dreammooncai.pvz2tool.release

import java.io.File

/**
 * 发布工具 CLI（由 release.sh 调用）。
 *
 * 用法：
 *   ReleaseKt adapt  <apk> <version> <notesFile|-> <templateDir> <outZip>
 *   ReleaseKt arsc   <apk> <outZip>
 *   ReleaseKt check  <apk>                       # 打印版本与产物信息
 */
object ReleaseCli {
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.isEmpty()) {
            println("用法: adapt | arsc | check")
            return
        }
        when (args[0]) {
            "arsc" -> {
                require(args.size >= 3) { "arsc <apk> <outZip>" }
                ArscZipExporter.exportFromApk(File(args[1]), File(args[2]))
                println("arsc.zip 已生成: ${args[2]}")
            }
            "adapt" -> {
                require(args.size >= 6) { "adapt <apk> <version> <notesFile|-> <templateDir> <outZip>" }
                val notes = args[3].let { if (it == "-" || it.isBlank()) null else File(it) }
                AdaptationZipBuilder.build(File(args[1]), args[2], notes, File(args[4]), File(args[5]))
                println("适配包已生成: ${args[5]}")
            }
            "check" -> {
                require(args.size >= 2) { "check <apk>" }
                val apk = File(args[1])
                println("APK: ${apk.absolutePath} (${apk.length()} bytes, exists=${apk.exists()})")
            }
            else -> error("未知命令: ${args[0]}")
        }
    }
}
