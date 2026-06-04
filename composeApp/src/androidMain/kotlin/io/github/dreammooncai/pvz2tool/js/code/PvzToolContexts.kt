package io.github.dreammooncai.pvz2tool.js.code

import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.VariableType
import io.github.alexzhirkevich.keight.js.FunctionParam
import io.github.alexzhirkevich.keight.js.JSFunction
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.JsObject
import io.github.alexzhirkevich.keight.js.JsPropertyAccessor
import io.github.alexzhirkevich.keight.js.Object
import io.github.alexzhirkevich.keight.js.Undefined
import io.github.alexzhirkevich.keight.js.js
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.js.JsConsole
import io.github.dreammooncai.pvz2tool.js.JsFileAccess
import io.github.dreammooncai.pvz2tool.js.JsFileResolver
import io.github.dreammooncai.pvz2tool.js.PvzToolJsEngine
import io.github.dreammooncai.pvz2tool.js.eq
import io.github.dreammooncai.pvz2tool.js.func
import io.github.dreammooncai.pvz2tool.js.orNull
import io.github.dreammooncai.pvz2tool.js.toBoolean
import io.github.dreammooncai.pvz2tool.pop.core.rsb.Rsb
import io.github.dreammooncai.pvz2tool.pop.core.rsb.RsbCommonConfig
import io.github.dreammooncai.pvz2tool.pop.core.rsb.RsbPackConfig
import io.github.dreammooncai.pvz2tool.pop.core.rsb.RsbUnpackConfig
import io.github.dreammooncai.pvz2tool.pop.core.rsb.model.compress.CompressionLevel
import io.github.dreammooncai.pvz2tool.pop.core.rsb.util.Zlib
import io.github.dreammooncai.pvz2tool.pop.core.rsb.util.decompressZLib
import io.github.dreammooncai.pvz2tool.pop.core.rsb.util.isZlib
import io.github.dreammooncai.pvz2tool.pop.image.ptx.Ptx
import io.github.dreammooncai.pvz2tool.pop.image.ptx.PtxFormat
import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream
import io.github.dreammooncai.pvz2tool.pop.rton.RTON
import java.io.File

class PvzToolContexts(
    private val runtime: ScriptRuntime,
    private val access: JsFileAccess
) {

    val path = Object("path") {

        // path.app
        listOf("app".js, "应用".js) eq Object("app") {
            listOf("data".js, "数据".js)  eq InitializePvz2.context.dataDir.absolutePath.js
            listOf("files".js, "文件".js) eq InitializePvz2.context.filesDir.absolutePath.js
            listOf("cache".js, "缓存".js) eq InitializePvz2.context.cacheDir.absolutePath.js
        }

        // path.android
        listOf("android".js, "安卓".js) eq Object("android") {
            listOf("data".js, "数据".js)  eq InitializePvz2.context.getExternalFilesDir(null)!!.parent!!.js
            listOf("files".js, "文件".js) eq InitializePvz2.context.getExternalFilesDir(null)!!.absolutePath.js
            listOf("cache".js, "缓存".js) eq InitializePvz2.context.externalCacheDir!!.absolutePath.js
        }

        // path.pvz
        listOf("pvz".js, "植物大战僵尸".js) eq Object("pvz") {
            listOf("saves".js, "存档".js) eq JsFileResolver.GAME_SAVES.js
            listOf("smf".js, "资源".js)   eq JsFileResolver.GAME_SMF.js
        }

        // path.pvz2tool
        listOf("pvz2tool".js, "工具".js) eq Object("pvz2tool") {
            listOf("files".js, "文件".js) eq JsFileResolver.WORK_DIR.js
            listOf("smf".js, "SMF".js)   eq JsFileResolver.SMF.js
            listOf("section".js, "资源".js)   eq JsFileResolver.ITEM.js
            listOf("jsDir".js, "JS目录".js)   eq JsFileResolver.JS_DIR.js
        }

        // path.resolve(placeholder) → 绝对路径字符串
        listOf("resolve".js, "解析路径".js).func("placeholderPath") { args ->
            val p = toString(args[0])
            (access.resolver.resolveToPath(p, InitializePvz2.context) ?: "").js
        }

        // path.resolveUri(placeholder) → URI 字符串
        listOf("resolveUri".js, "解析URI".js).func("placeholderPath") { args ->
            val p = toString(args[0])
            (access.resolver.resolve(p, InitializePvz2.context)?.uri?.toString() ?: "").js
        }
    }

    val rton = Object("rton") {
        listOf("encryptionKey".js, "加密密钥".js) eq JsPropertyAccessor.BackedField(Callable {
            RTON.encryptionKey.js
        },Callable { args ->
            RTON.encryptionKey = toString(args[0])
            Undefined
        })

        listOf("decode".js, "解码".js).func(
            FunctionParam("inputPath"),
            FunctionParam("outputPath")
        ) { args ->
            val inputPath  = toString(args[0])
            val outputPath = toString(args[1])
            val ctx = InitializePvz2.context

            val inputFile = access.resolveInputOrThrow(inputPath, ctx)
            val outputHandle = access.resolveOutputOrThrow(outputPath, ctx)

            try {
                RTON.decodeAuto(inputFile.file.absolutePath, outputHandle.targetFile.absolutePath)
                outputHandle.commit()
            } finally {
                if (inputFile.isCache) inputFile.file.delete()
            }
            Undefined
        }

        listOf("encode".js, "编码".js).func(
            FunctionParam("inputPath"),
            FunctionParam("outputPath")
        ) { args ->
            val inputPath  = toString(args[0])
            val outputPath = toString(args[1])
            val ctx = InitializePvz2.context

            val inputFile = access.resolveInputOrThrow(inputPath, ctx)
            val outputHandle = access.resolveOutputOrThrow(outputPath, ctx)

            try {
                RTON.encodeAuto(inputFile.file.absolutePath, outputHandle.targetFile.absolutePath)
                outputHandle.commit()
            } finally {
                if (inputFile.isCache) inputFile.file.delete()
            }
            Undefined
        }

        listOf("load".js, "加载".js).func(FunctionParam("inputPath"),FunctionParam("outputPath")) { args ->
            val inputPath = toString(args[0])
            val outputPath = args.getOrNull(1).orNull?.let { toString(it) }
            val ctx = InitializePvz2.context

            val inputFile = access.resolveInputOrThrow(inputPath, ctx)

            val tempJson = File.createTempFile("rton_load_", ".json", ctx.cacheDir).apply { deleteOnExit() }
            try {
                val isJson = inputFile.file.extension.lowercase() == "json"
                val jsonContent = if (isJson) {
                    inputFile.file.readText()
                } else {
                    RTON.decodeAuto(inputFile.file.absolutePath, tempJson.absolutePath)
                    tempJson.readText()
                }

                val jsonObj = PvzToolJsEngine.parse(jsonContent) as JsObject

                jsonObj.set("__rtonPath__".js,inputPath.js,this@func)

                // 注入 save()（不可枚举）
                val saveFunc = Callable { args ->
                    val rtonPath = args.getOrNull(0).orNull?.let { toString(it) } ?: outputPath ?: toString(
                        jsonObj.get("__rtonPath__".js, this)
                    )
                    jsonObj.delete("__rtonPath__".js,this)
                    rtonSaveImpl(rtonPath, PvzToolJsEngine.stringify(jsonObj,4))
                }
                listOf("save".js, "保存".js).forEach { key ->
                    jsonObj.setProperty(
                        key,
                        JsPropertyAccessor.Value(saveFunc),
                        this@func,
                        enumerable = false,
                        configurable = true,
                        writable = false
                    )
                }

                jsonObj
            } finally {
                tempJson.delete()
                if (inputFile.isCache) inputFile.file.delete()
            }
        }

        listOf("save".js, "保存".js).func(
            FunctionParam("outputPath"),
            FunctionParam("jsonString")
        ) { args ->
            val outputPath = toString(args[0])
            val jsonString = toString(args[1])
            rtonSaveImpl(outputPath, jsonString)
        }
    }

    val rsb = Object("rsb") {
        listOf("unpack".js, "解包".js).func(
            FunctionParam("inFilePath"),
            FunctionParam("outFolderPath"),
            FunctionParam("options")
        ) { args ->
            val inFilePath  = toString(args[0])
            val outFolderPath = toString(args[1])
            val options    = args[2].orNull as? JsObject
            val ctx = InitializePvz2.context
            JsConsole.info("rsb.unpack: $inFilePath → $outFolderPath")

            val inputFile = access.resolveInputOrThrow(inFilePath, ctx)
            val outputHandle = access.resolveOutputOrThrow(outFolderPath, ctx)

            val entries = options?.entries(runtime)?.associate { (key, value) ->
                key to value
            }
            try {
                Rsb.unpack(inputFile.file, outputHandle.targetFile) {
                    setConfig(this@func,entries)
                }
                outputHandle.commit()
            } finally {
                if (inputFile.isCache) inputFile.file.delete()
            }
            Undefined
        }

        listOf("pack".js, "打包".js).func(
            FunctionParam("inFolderPath"),
            FunctionParam("outFilePath"),
            FunctionParam("options")
        ) { args ->
            val inFolderPath  = toString(args[0])
            val outFilePath = toString(args[1])
            val options    = args[2].orNull as? JsObject
            val ctx = InitializePvz2.context
            JsConsole.info("rsb.pack: $inFolderPath → $outFilePath")

            val inFolder = access.resolveInputOrThrow(inFolderPath, ctx)
            val outputHandle = access.resolveOutputOrThrow(outFilePath, ctx)

            val entries = options?.entries(runtime)?.associate { (key, value) ->
                key to value
            }

            try {
                Rsb.pack(inFolder.file, outputHandle.targetFile) {
                    setConfig(this@func, entries)
                }
                outputHandle.commit()
            } finally {
                if (inFolder.isCache) inFolder.file.delete()
            }
            Undefined
        }
    }

    val file = JsFile(access)

    val ptx = Object("ptx") {
        listOf("PtxABGR8888Mode".js, "像素ABGR8888模式".js) eq JsPropertyAccessor.BackedField(Callable {
            Ptx.PtxABGR8888Mode.js
        },Callable { args ->
            Ptx.PtxABGR8888Mode = args[0].toBoolean()
            Undefined
        })
        listOf("PtxARGB8888PaddingMode".js, "像素ARGB8888填充模式".js) eq JsPropertyAccessor.BackedField(Callable {
            Ptx.PtxARGB8888PaddingMode.js
        },Callable { args ->
            Ptx.PtxARGB8888PaddingMode = args[0].toBoolean()
            Undefined
        })
        listOf("RsbPtxABGR8888Mode".js, "资源像素ABGR8888模式".js) eq JsPropertyAccessor.BackedField(Callable {
            Ptx.RsbPtxABGR8888Mode.js
        },Callable { args ->
            Ptx.RsbPtxABGR8888Mode = args[0].toBoolean()
            Undefined
        })
        listOf("RsbPtxARGB8888PaddingMode".js, "资源像素ARGB8888填充模式".js) eq JsPropertyAccessor.BackedField(Callable {
            Ptx.RsbPtxARGB8888PaddingMode.js
        },Callable { args ->
            Ptx.RsbPtxARGB8888PaddingMode = args[0].toBoolean()
            Undefined
        })

        // ptx.format.ARGB8888 等
        listOf("format".js, "格式".js) eq Object("format") {
            PtxFormat.entries.forEach { fmt ->
                fmt.name.js eq fmt.name.js
            }
        }

        listOf("decode".js, "解码".js).func(
            FunctionParam("inputPath"),
            FunctionParam("outputPath")
        ) { args ->
            val inputPath  = toString(args[0])
            val outputPath = toString(args[1])
            val ctx = InitializePvz2.context

            val inputFile = access.resolveInputOrThrow(inputPath, ctx)
            val outputHandle = access.resolveOutputOrThrow(outputPath, ctx)
            try {
                Ptx.decode(inputFile.file, outputHandle.targetFile, true)
                outputHandle.commit()
            } finally {
                if (inputFile.isCache) inputFile.file.delete()
            }
            Undefined
        }

        listOf("encode".js, "编码".js).func(
            FunctionParam("inputPath"),
            FunctionParam("outputPath"),
            FunctionParam("format")
        ) { args ->
            val inputPath  = toString(args[0])
            val outputPath = toString(args[1])
            val fmtName    = toString(args[2])
            val ctx = InitializePvz2.context

            val inputFile = access.resolveInputOrThrow(inputPath, ctx)
            val outputHandle = access.resolveOutputOrThrow(outputPath, ctx)

            try {
                Ptx.encode(inputFile.file, outputHandle.targetFile, PtxFormat.valueOf(fmtName))
                outputHandle.commit()
            } finally {
                if (inputFile.isCache) inputFile.file.delete()
            }
            Undefined
        }
    }

    val zlib = Object("zlib") {
        listOf("unpack".js, "解包".js).func(
            FunctionParam("inFilePath"),
            FunctionParam("outFilePath")
        ) { args ->
            val inFilePath  = toString(args[0])
            val outFilePath = toString(args[1])
            val ctx = InitializePvz2.context

            val inputFile = access.resolveInputOrThrow(inFilePath, ctx)
            val outputHandle = access.resolveOutputOrThrow(outFilePath, ctx)
            try {
                Zlib.unpack(inputFile.file,outputHandle.targetFile)
                outputHandle.commit()
            } finally {
                if (inputFile.isCache) inputFile.file.delete()
            }
            Undefined
        }

        listOf("pack".js, "打包".js).func(
            FunctionParam("inFilePath"),
            FunctionParam("outFilePath"),
            FunctionParam("level", default = Expression { CompressionLevel.Optimal.name.js }),
            FunctionParam("isChineseMode", default = Expression { false.js }),
        ) { args ->
            val inFilePath  = toString(args[0])
            val outFilePath = toString(args[1])
            val levelName = toString(args[2])
            val isChineseMode = args[3].toBoolean()
            val ctx = InitializePvz2.context

            val inFolder = access.resolveInputOrThrow(inFilePath, ctx)
            val outputHandle = access.resolveOutputOrThrow(outFilePath, ctx)

            try {
                Zlib.pack(inFolder.file,outputHandle.targetFile, CompressionLevel.valueOf(levelName),isChineseMode)
                outputHandle.commit()
            } finally {
                if (inFolder.isCache) inFolder.file.delete()
            }
            Undefined
        }
    }

    /**
     * rton.save 的共享实现，供 rton.save 和 rton.load 注入的 save() 方法复用。
     */
    private suspend fun rtonSaveImpl(
        outputPath: String,
        jsonString: String
    ): JsAny {
        val ctx = InitializePvz2.context
        val outputHandle = access.resolveOutputOrThrow(outputPath, ctx)
        val tempJson = File.createTempFile("rton_save_", ".json", ctx.cacheDir).apply { deleteOnExit() }
        try {
            if (outputPath.endsWith(".json",true)) {
                outputHandle.targetFile.writeText(jsonString)
            } else {
                tempJson.writeText(jsonString)
                RTON.encodeAuto(tempJson.absolutePath, outputHandle.targetFile.absolutePath)
            }
            outputHandle.commit()
        } finally {
            tempJson.delete()
        }
        return Undefined
    }

    private suspend fun RsbCommonConfig.setConfig(runtime: ScriptRuntime, entries: Map<JsAny?, JsAny?>?) {
        entries ?: return
        
        // ==================== 事件回调 ====================

        // onStart / 开始
        val onStartFunc = (entries["onStart".js]?.orNull ?: entries["开始".js]?.orNull) as? JSFunction
        // onLog / 日志
        val onLogFunc = (entries["onLog".js]?.orNull ?: entries["日志".js]?.orNull) as? JSFunction
        // onProgress / 进度
        val onProgressFunc = (entries["onProgress".js]?.orNull ?: entries["进度".js]?.orNull) as? JSFunction
        // onResourceGroupStart / 资源组开始
        val onGroupStartFunc = (entries["onResourceGroupStart".js]?.orNull ?: entries["资源组开始".js]?.orNull) as? JSFunction
        // onResourceGroupEnd / 资源组结束
        val onGroupEndFunc = (entries["onResourceGroupEnd".js]?.orNull ?: entries["资源组结束".js]?.orNull) as? JSFunction
        // onError / 错误
        val onErrorFunc = (entries["onError".js]?.orNull ?: entries["错误".js]?.orNull) as? JSFunction
        // onFinish / 完成
        val onFinishFunc = (entries["onFinish".js]?.orNull ?: entries["完成".js]?.orNull) as? JSFunction

        setOnStart {
            onStartFunc?.invoke(emptyList(), runtime)
        }
        setOnLog { level, msg ->
            onLogFunc?.invoke(listOf(msg.js, level.name.js), runtime)
        }
        setOnProgress { progress, message ->
            onProgressFunc?.invoke(listOf(progress.js, message.js), runtime)
        }
        setOnResourceGroupStart { index, id ->
            onGroupStartFunc?.invoke(listOf(index.js, id.js), runtime)
        }
        setOnResourceGroupEnd { index, id ->
            onGroupEndFunc?.invoke(listOf(index.js, id.js), runtime)
        }
        setOnError { error, message ->
            onErrorFunc?.invoke(listOf(error.js, message.js), runtime)
        }
        setOnFinish { file ->
            onFinishFunc?.invoke(listOf(file.absolutePath.js), runtime)
        }
        
        // ==================== RsbCommonConfig 通用属性 ====================
        
        // version / 版本 (默认 3)
        val version = entries["version".js]?.orNull ?: entries["版本".js]?.orNull
        if (version != null) this.version = runtime.toNumber(version).toInt()
        
        // bigEndian / 大端序 (默认 false)
        val bigEndian = entries["bigEndian".js]?.orNull ?: entries["大端序".js]?.orNull
        if (bigEndian != null) this.bigEndian = bigEndian.toKotlin(runtime).toString().toBoolean()
        
        // compressionLevel / 压缩级别 (默认 Optimal)
        val compressionLevel = entries["compressionLevel".js]?.orNull ?: entries["压缩级别".js]?.orNull
        if (compressionLevel != null) {
            this.compressionLevel = when (compressionLevel.toKotlin(runtime).toString().lowercase()) {
                "fastest" -> CompressionLevel.Fastest
                "smallest" -> CompressionLevel.Smallest
                else -> CompressionLevel.Optimal
            }
        }
        
        // ptxInfoLength / PTX信息长度 (默认 0x10)
        val ptxInfoLength = entries["ptxInfoLength".js]?.orNull ?: entries["PTX信息长度".js]?.orNull
        if (ptxInfoLength != null) this.ptxInfoLength = runtime.toNumber(ptxInfoLength).toInt().toUInt()
        
        // smfCompress / SMF压缩 (默认 false)
        val smfCompress = entries["smfCompress".js]?.orNull ?: entries["smf压缩".js]?.orNull
        if (smfCompress != null) this.smfCompress = smfCompress.toKotlin(runtime).toString().toBoolean()
        
        // specialPool / 特殊池 (默认 false)
        val specialPool = entries["specialPool".js]?.orNull ?: entries["特殊池".js]?.orNull
        if (specialPool != null) this.specialPool = specialPool.toKotlin(runtime).toString().toBoolean()
        
        // compressPart0 / 压缩Part0 (默认 false)
        val compressPart0 = entries["compressPart0".js]?.orNull ?: entries["压缩Part0".js]?.orNull
        if (compressPart0 != null) this.compressPart0 = compressPart0.toKotlin(runtime).toString().toBoolean()
        
        // compressPart1 / 压缩Part1 (默认 true)
        val compressPart1 = entries["compressPart1".js]?.orNull ?: entries["压缩Part1".js]?.orNull
        if (compressPart1 != null) this.compressPart1 = compressPart1.toKotlin(runtime).toString().toBoolean()
        
        // maxRetries / 最大重试次数 (默认 1)
        val maxRetries = entries["maxRetries".js]?.orNull ?: entries["最大重试次数".js]?.orNull
        if (maxRetries != null) this.maxRetries = runtime.toNumber(maxRetries).toInt()
        
        // retryDelayMs / 重试延迟毫秒 (默认 500)
        val retryDelayMs = entries["retryDelayMs".js]?.orNull ?: entries["重试延迟毫秒".js]?.orNull
        if (retryDelayMs != null) this.retryDelayMs = runtime.toNumber(retryDelayMs).toLong()
        
        // ==================== RsbPackConfig 特有属性 ====================
        
        if (this is RsbPackConfig) {
            // autoJsonToRton / 自动JSON转RTON (默认 true)
            val autoJsonToRton = entries["autoJsonToRton".js]?.orNull ?: entries["自动JSON转RTON".js]?.orNull
            if (autoJsonToRton != null) this.autoJsonToRton = autoJsonToRton.toKotlin(runtime).toString().toBoolean()
            
            // deleteOriginalJson / 删除原始JSON (默认 false)
            val deleteOriginalJson = entries["deleteOriginalJson".js]?.orNull ?: entries["删除原始JSON".js]?.orNull
            if (deleteOriginalJson != null) this.deleteOriginalJson = deleteOriginalJson.toKotlin(runtime).toString().toBoolean()
            
            // autoPngToPtx / 自动PNG转PTX (默认 true)
            val autoPngToPtx = entries["autoPngToPtx".js]?.orNull ?: entries["自动PNG转PTX".js]?.orNull
            if (autoPngToPtx != null) this.autoPngToPtx = autoPngToPtx.toKotlin(runtime).toString().toBoolean()
            
            // deleteOriginalPng / 删除原始PNG (默认 false)
            val deleteOriginalPng = entries["deleteOriginalPng".js]?.orNull ?: entries["删除原始PNG".js]?.orNull
            if (deleteOriginalPng != null) this.deleteOriginalPng = deleteOriginalPng.toKotlin(runtime).toString().toBoolean()

            val onXmlParsedFunc = (entries["onXmlParsed".js]?.orNull ?: entries["解析XML完成".js]?.orNull) as? JSFunction
            setOnXmlParsed { count ->
                onXmlParsedFunc?.invoke(listOf(count.js),runtime)
            }
        }
        
        // ==================== RsbUnpackConfig 特有属性 ====================
        
        if (this is RsbUnpackConfig) {
            // autoRtonToJson / 自动RTON转JSON (默认 true)
            val autoRtonToJson = entries["autoRtonToJson".js]?.orNull ?: entries["自动RTON转JSON".js]?.orNull
            if (autoRtonToJson != null) this.autoRtonToJson = autoRtonToJson.toKotlin(runtime).toString().toBoolean()
            
            // deleteOriginalRton / 删除原始RTON (默认 true)
            val deleteOriginalRton = entries["deleteOriginalRton".js]?.orNull ?: entries["删除原始RTON".js]?.orNull
            if (deleteOriginalRton != null) this.deleteOriginalRton = deleteOriginalRton.toKotlin(runtime).toString().toBoolean()
            
            // autoPtxToPng / 自动PTX转PNG (默认 false)
            val autoPtxToPng = entries["autoPtxToPng".js]?.orNull ?: entries["自动PTX转PNG".js]?.orNull
            if (autoPtxToPng != null) this.autoPtxToPng = autoPtxToPng.toKotlin(runtime).toString().toBoolean()
            
            // deleteOriginalPtx / 删除原始PTX (默认 true)
            val deleteOriginalPtx = entries["deleteOriginalPtx".js]?.orNull ?: entries["删除原始PTX".js]?.orNull
            if (deleteOriginalPtx != null) this.deleteOriginalPtx = deleteOriginalPtx.toKotlin(runtime).toString().toBoolean()
            
            // onHeaderRead / 头部读取完成 (RsbUnpackConfig 专用)
            val onHeaderReadFunc = (entries["onHeaderRead".js]?.orNull ?: entries["头部读取完成".js]?.orNull) as? JSFunction
            setOnHeaderRead { rsbInfo ->
                onHeaderReadFunc?.invoke(listOf(rsbInfo.toString().js), runtime)
            }
        }
    }

    suspend fun attached() {
        listOf("path".js, "路径".js).forEach { key ->
            runtime.set(key, path, VariableType.Local)
        }
        listOf("rton".js, "RTON".js).forEach { key ->
            runtime.set(key, rton, VariableType.Local)
        }
        listOf("rsb".js, "RSB".js).forEach { key ->
            runtime.set(key, rsb, VariableType.Local)
        }
        listOf("zlib".js, "ZLIB".js).forEach { key ->
            runtime.set(key, zlib, VariableType.Local)
        }
        listOf("ptx".js, "PTX".js).forEach { key ->
            runtime.set(key, ptx, VariableType.Local)
        }
        listOf("file".js, "File".js, "文件".js).forEach { key ->
            runtime.set(key, file.js, VariableType.Local)
        }
    }
}