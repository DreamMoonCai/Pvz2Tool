@file:JvmSynthetic
@file:JvmName("---")
package io.github.dreammooncai.pvz2tool.pop.core.rsb

import io.github.dreammooncai.pvz2tool.pop.image.ptx.Ptx
import io.github.dreammooncai.pvz2tool.pop.image.ptx.PtxFormat
import io.github.dreammooncai.pvz2tool.pop.core.rsb.model.*
import io.github.dreammooncai.pvz2tool.pop.core.rsb.model.compress.*
import io.github.dreammooncai.pvz2tool.pop.core.rsb.model.convert.XmlConvert
import io.github.dreammooncai.pvz2tool.pop.core.rsb.util.*
import io.github.dreammooncai.pvz2tool.pop.plugin.io.Endian
import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream
import io.github.dreammooncai.pvz2tool.pop.rton.RTON
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

val Long.alignFourKSize: Long get() {
    if (this == 0L) return 0L
    return if (this % Rsb.FOUR_K_ALIGNMENT == 0L) this else this + Rsb.FOUR_K_ALIGNMENT - (this % Rsb.FOUR_K_ALIGNMENT)
}

private suspend fun CoroutineBinaryStream.alignFourK() {
    val aligned = length.alignFourKSize
    setLength(aligned)
    setWritePosition(aligned)
}

/**
 * 通用协程重试装饰器
 * 支持指数退避、详细日志、可配置重试策略
 */
suspend fun <T> retrySuspend(
    maxRetries: Int,
    baseDelayMs: Long,
    onRetry: suspend (Int, Throwable) -> Unit = { _, _ -> },
    block: suspend () -> T
): T {
    var lastException: Throwable? = null
    repeat(maxRetries + 1) { attempt ->
        try {
            return block()
        } catch (e: Throwable) {
            lastException = e
            if (attempt >= maxRetries) {
                throw e
            }
            onRetry(attempt + 1, e)
            // 指数退避延迟
            val delay = baseDelayMs * (1L shl attempt)
            delay(delay.milliseconds)
        }
    }
    throw lastException!!
}

// ======================================================================================
// 配置基类
// ======================================================================================
open class RsbCommonConfig {
    var version: Int = 3
    var bigEndian: Boolean = false
    var compressionLevel: CompressionLevel = CompressionLevel.Optimal
    var ptxInfoLength: UInt = 0x10u
    var smfCompress: Boolean = false
    var specialPool: Boolean = false
    var compressPart0: Boolean = false
    var compressPart1: Boolean = true

    val emptyCompressHead: Byte
        get() = when (compressionLevel) {
            CompressionLevel.Fastest -> 0x01
            CompressionLevel.Smallest -> 0xDA.toByte()
            else -> 0x9C.toByte()
        }

    var endian: Endian
        get() = if (bigEndian) Endian.Big else Endian.Small
        set(value) { bigEndian = value == Endian.Big }

    /**
     * 最大重试次数
     */
    var maxRetries: Int = 1

    /**
     * 重试基础延迟（毫秒，默认 500ms）
     * 采用指数退避：第1次 500ms，第2次 1000ms，第3次 2000ms...
     */
    var retryDelayMs: Long = 500L

    private var _onStart: (suspend () -> Unit)? = null
    private var _onResourceGroupStart: (suspend (index: Int, id: String) -> Unit)? = null
    private var _onResourceGroupEnd: (suspend (index: Int, id: String) -> Unit)? = null
    private var _onProgress: (suspend (progress: Float, message: String) -> Unit)? = null
    private var _onError: (suspend (error: Throwable, message: String) -> Unit)? = null
    private var _onFinish: (suspend (file: File) -> Unit)? = null
    private var _onLog: (suspend (level: LogLevel,log: String) -> Unit)? = null

    // ==================== 事件设置方法 ====================

    /**
     * 设置开始处理监听器
     */
    fun setOnStart(listener: suspend () -> Unit) { _onStart = listener }

    /**
     * 设置资源组开始处理监听器
     *
     * 单个资源组开始处理时的回调
     *
     * @param listener index 资源组索引
     * @param listener id 资源组ID
     */
    fun setOnResourceGroupStart(listener: suspend (index: Int, id: String) -> Unit) {
        _onResourceGroupStart = listener
    }

    /**
     * 设置资源组处理完成监听器
     *
     * @param listener index 资源组索引
     * @param listener id 资源组ID
     */
    fun setOnResourceGroupEnd(listener: suspend (index: Int, id: String) -> Unit) {
        _onResourceGroupEnd = listener
    }

    /**
     * 设置进度更新监听器
     *
     * @param listener progress 进度值（0.0 ~ 1.0）
     * @param listener message 进度描述信息
     */
    fun setOnProgress(listener: suspend (progress: Float, message: String) -> Unit) {
        _onProgress = listener
    }

    /**
     * 设置错误监听器
     * @param listener error 异常对象
     * @param listener message 错误描述信息
     */
    fun setOnError(listener: suspend (error: Throwable, message: String) -> Unit) {
        _onError = listener
    }

    /**
     * 设置处理完成监听器
     * @param listener file 输出文件/文件夹
     */
    fun setOnFinish(listener: suspend (file: File) -> Unit) {
        _onFinish = listener
    }

    /**
     * 设置日志监听器
     * @param listener log 日志内容
     */
    fun setOnLog(listener: suspend (level: LogLevel,log: String) -> Unit) {
        _onLog = listener
    }

    // ==================== 内部调用方法 ====================

    internal suspend fun invokeOnStart() = _onStart?.invoke()
    internal suspend fun invokeOnResourceGroupStart(index: Int, id: String) =
        _onResourceGroupStart?.invoke(index, id)
    internal suspend fun invokeOnResourceGroupEnd(index: Int, id: String) =
        _onResourceGroupEnd?.invoke(index, id)
    internal suspend fun invokeOnProgress(progress: Float, msg: String) =
        _onProgress?.invoke(progress, msg)
    internal suspend fun invokeOnError(error: Throwable, msg: String) =
        _onError?.invoke(error, msg)
    internal suspend fun invokeOnFinish(file: File) =
        _onFinish?.invoke(file)
    internal suspend fun invokeOnLog(level: LogLevel,log: String) {
        _onLog?.invoke(level,log) ?: println(log)
    }

    enum class LogLevel {
        INFO,WARN,ERROR
    }
}

// ======================================================================================
// 打包配置
// ======================================================================================
class RsbPackConfig : RsbCommonConfig() {
    /**
     * 是否自动将JSON转换为RTON
     *
     * 开启后即使存在RTON也会优先使用JSON转换并覆盖RTON
     */
    var autoJsonToRton: Boolean = true

    /**
     * 转换为RTON后是否需要删除JSON
     *
     * 关闭时不会保存RTON只会保留JSON
     * 开启时会删除JSON并保存RTON
     */
    var deleteOriginalJson: Boolean = false

    /**
     * 是否自动将PNG转换为PTX
     *
     * 开启后即使存在PTX也会优先使用PNG转换并覆盖PTX
     */
    var autoPngToPtx: Boolean = true

    /**
     * 转换为PTX后是否需要删除PNG
     *
     * 关闭时不会保存PTX只会保留PNG
     * 开启时会删除PNG并保存PTX
     */
    var deleteOriginalPng: Boolean = false

    private var _onXmlParsed: (suspend (count: Int) -> Unit)? = null

    fun setOnXmlParsed(listener: suspend (count: Int) -> Unit) { _onXmlParsed = listener }

    internal suspend fun invokeOnXmlParsed(count: Int) = _onXmlParsed?.invoke(count)
}

// ======================================================================================
// 解压配置
// ======================================================================================
class RsbUnpackConfig : RsbCommonConfig() {
    /**
     * 是否自动将RTON转换为JSON
     */
    var autoRtonToJson: Boolean = true

    /**
     * 转换为JSON后是否需要删除RTON
     */
    var deleteOriginalRton: Boolean = true

    /**
     * 是否自动将PTX转换为PNG
     */
    var autoPtxToPng: Boolean = false

    /**
     * 转换为PNG后是否需要删除PTX
     */
    var deleteOriginalPtx: Boolean = true

    private var _onHeaderRead: (suspend (RsbInfo) -> Unit)? = null

    fun setOnHeaderRead(listener: suspend (RsbInfo) -> Unit) { _onHeaderRead = listener }

    internal suspend fun invokeOnHeaderRead(info: RsbInfo) = _onHeaderRead?.invoke(info)
}

// ======================================================================================
// 数据模型
// ======================================================================================
private sealed class ResourceData(open val index: Int, open val id: String) {
    data class ResData(override val index: Int, override val id: String) : ResourceData(index, id)
    data class ImgData(override val index: Int, override val id: String, val defaultFormat: Int) : ResourceData(index, id)
    val comp: CompressString by lazy { CompressString(id, RsbExtraInfo(index.toUInt())) }
}

private data class ResourceGroupData(
    val id: String,
    val poolIndex: UInt?,
    val compressMethod: UInt?,
    val resources: List<ResourceData>
)

private data class ProcessedGroupResult(
    val index: Int,
    val id: String,
    val flags: UInt,
    val poolIndex: UInt,
    val resources: List<ResourceData>
)

private data class PreprocessedResource(
    val resData: ResourceData,
    val comp: CompressString,
    val fileBytes: CoroutineBinaryStream,
    val fileLength: Long
)

private data class RsgpPreResult(
    val rsgpInfo: RsbRsgpInfo,
    val rsgp: RsgpInfo,
    val autopool: RsbAutoPoolInfo,
    val ptxList: List<RsbPtxInfo>,
    val thisPtxCount: UInt,
    val preprocessedList: List<PreprocessedResource>,
    val bsPResData: CoroutineBinaryStream?,
    val bsPImgData: CoroutineBinaryStream?,
    val bsPResDataSize: Long,
    val bsPImgDataSize: Long
)

private data class RsgpWriteData(
    val pre: RsgpPreResult,
    val fileListBytes: ByteArray,
    val totalSize: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as RsgpWriteData
        if (totalSize != other.totalSize) return false
        if (pre != other.pre) return false
        if (!fileListBytes.contentEquals(other.fileListBytes)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = totalSize.hashCode()
        result = 31 * result + pre.hashCode()
        result = 31 * result + fileListBytes.contentHashCode()
        return result
    }
}

private data class RsgpResult(
    val info: RsbRsgpInfo,
    val data: RsgpInfo,
    val autopool: RsbAutoPoolInfo,
    val ptxList: List<RsbPtxInfo>,
    val addedPtxCount: UInt
)

private data class RsgpProcessResult(
    val rsgpResult: RsgpResult,
    val index: Int,
    val writeData: RsgpWriteData
)

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// ======================================================================================
// 入口类
// ======================================================================================
internal object Rsb {

    // ======================================================================================
    // 公共常量与配置
    // ======================================================================================
    const val AUTO_POOL_NAME = "_AutoPool"
    const val FOUR_K_ALIGNMENT = 0x1000L
    const val PTX_HEADER_SIZE = 0x20L
    const val RSB_MAGIC_1BSR = 1920164401
    const val RSB_MAGIC_RSB1 = 828535666
    const val PTX_MAGIC = 1886681137
    const val SMF_MAGIC = -559022380
    const val XML_MAGIC = 1919251249

    /**
     * Pack
     *
     * 将指定文件夹进行打包为RSB格式文件
     *
     * @param inFolder 需要打包的文件夹
     * @param outFile 输出的RSB文件
     * @param configBlock 配置打包行为
     */
    @JvmSynthetic
    @JvmName("----")
    suspend fun pack(inFolder: File, outFile: File, configBlock:suspend RsbPackConfig.() -> Unit = {}) {
        RsbPacker(inFolder, outFile, configBlock).execute()
    }


    /**
     * Unpack
     *
     * 将指定RSB格式文件解包到指定文件夹
     *
     * @param inFile 需要解包的RSB文件
     * @param outFolder 输出的文件夹
     * @param configBlock 配置解包行为
     */
    @JvmSynthetic
    @JvmName("-----")
    suspend fun unpack(inFile: File, outFolder: File, configBlock:suspend RsbUnpackConfig.() -> Unit = {}) {
        RsbUnpacker(inFile, outFolder, configBlock).execute()
    }


    /**
     * Unpack
     *
     * 将指定RSB格式文件解包到指定文件夹
     *
     * @param inFileStream 需要解包的RSB文件流
     * @param outFolder 输出的文件夹
     * @param configBlock 配置解包行为
     */
    @JvmSynthetic
    @JvmName("-----")
    suspend fun unpack(inFileStream: CoroutineBinaryStream, outFolder: File, configBlock:suspend RsbUnpackConfig.() -> Unit = {}) {
        RsbUnpacker(inFileStream, outFolder, configBlock).execute()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private val PACK_DISPATCHER = Dispatchers.Default.limitedParallelism(Runtime.getRuntime().availableProcessors() * 2)

// ======================================================================================
// 打包核心实现
// ======================================================================================
private class RsbPacker(
    private val inFolder: File,
    private val outFile: File,
    private val configBlock:suspend RsbPackConfig.() -> Unit
) {
    private val rsbInfo = RsbInfo()
    private lateinit var allPtxList: MutableList<RsbPtxInfo>
    private val studioPath: File by lazy { inFolder.resolve("POPSTUDIOINFO") }
    private val mergeMutex = Mutex()
    private lateinit var config: RsbPackConfig

    private suspend fun logInfo(msg: String) = config.invokeOnLog(RsbCommonConfig.LogLevel.INFO,msg)
    private suspend fun logWarn(msg: String) = config.invokeOnLog(RsbCommonConfig.LogLevel.WARN,msg) // 黄色警告
    private suspend fun logError(msg: String) = config.invokeOnLog(RsbCommonConfig.LogLevel.ERROR,msg)

    @JvmSynthetic
    @JvmName("---")
    suspend fun execute() = withContext(Dispatchers.IO) {
        try {
            config = RsbPackConfig().apply { configBlock() }
            config.invokeOnStart()
            logInfo("开始打包: ${inFolder.absolutePath} -> ${outFile.absolutePath}")
            checkInput()
            initConfig()
            config.invokeOnProgress(0.1f, "配置加载完成")
            processPack()
            config.invokeOnProgress(1.0f, "打包完成")
            config.invokeOnFinish(outFile)
            logInfo("打包完成: ${outFile.absolutePath} (${outFile.length()} bytes)")
        } catch (e: Throwable) {
            config.invokeOnError(e, "打包失败: ${e.message ?: "未知错误"}")
            throw e
        }
    }

    private suspend fun checkInput() {
        if (!inFolder.exists()) {
            logError("输入文件夹不存在: $inFolder")
            error("文件夹不存在: $inFolder")
        }
        outFile.parentFile?.mkdirs()
    }

    private suspend fun initConfig() {
        loadPackConfig()
        rsbInfo.head = RsbHeadInfo().apply {
            version = config.version
            ptxInfo_EachLength = config.ptxInfoLength
        }
        logInfo("配置加载完成")
    }

    private suspend fun processPack() = coroutineScope {
        if (config.specialPool) processSpecialPool() else processSimplePool()
    }

    private suspend fun processResourceGroups() = coroutineScope {
        val groupXml = studioPath.resolve("RESOURCESGROUP.XML")
        if (!groupXml.exists()) {
            val error = "RESOURCESGROUP.XML不存在"
            logError(error)
            config.invokeOnError(IllegalStateException(error), error)
            return@coroutineScope 0u
        }

        val groupDataList = parseGroupXml(groupXml)
        config.invokeOnXmlParsed(groupDataList.size)
        config.invokeOnProgress(0.15f, "解析完成 ${groupDataList.size} 个资源组")

        initRsbInfo(groupDataList.size)
        val fGlobal = (if (config.compressPart0) 0b10u else 0u) or (if (config.compressPart1) 0b1u else 0u)
        val (ptxStartIndexListFinal, totalPtx) = calculatePtxIndices(groupDataList)

        // 🔥 新增：线程安全的原子计数器，跟踪实际完成的任务数
        val completedCount = AtomicInteger(0)


        val deferredResults = groupDataList.mapIndexed { i, groupData ->
            async(PACK_DISPATCHER) {
                retrySuspend(
                    maxRetries = config.maxRetries,
                    baseDelayMs = config.retryDelayMs,
                    onRetry = { attempt, e ->
                        logWarn("资源组[$i] ${groupData.id} 第 $attempt 次重试，原因：${e.message}")
                    }
                ) {
                    config.invokeOnResourceGroupStart(i, groupData.id)
                    try {
                        val pre = prepareSingleRsgp(i, groupData, ptxStartIndexListFinal[i], fGlobal)
                        val writeData = processSingleRsgp(pre)
                        RsgpProcessResult(
                            RsgpResult(pre.rsgpInfo, pre.rsgp, pre.autopool, pre.ptxList, pre.thisPtxCount),
                            i, writeData
                        ).also {
                            config.invokeOnResourceGroupEnd(i, groupData.id)
                            val current = completedCount.incrementAndGet()
                            val progress = 0.15f + 0.75f * current / groupDataList.size
                            config.invokeOnProgress(progress, "处理资源组 ${groupData.id} $current/${groupDataList.size}")
                        }
                    } catch (e: Throwable) {
                        val errorMsg = "处理资源组[$i] ${groupData.id} 失败"
                        logError(errorMsg)
                        config.invokeOnError(e, errorMsg)
                        throw e
                    }
                }
            }
        }

        val processResults = deferredResults.awaitAll().sortedBy { it.index }
        val totalRsgpSize = processResults.sumOf { it.writeData.totalSize }
        config.invokeOnProgress(0.90f, "合并数据结构")

        mergeDataStructures(processResults)
        rsbInfo.head.ptx_Number = totalPtx

        config.invokeOnProgress(0.92f, "准备写入最终文件")
        writeFinalRsb { outFileStream ->
            outFileStream.setLength(outFileStream.length + totalRsgpSize + 1024)
            var offset = outFileStream.writePosition
            processResults.map { (rsgpResult, _, writeData) ->
                rsgpResult.info.offset = offset.toUInt()
                val temp = outFileStream.slice(offset, writeData.totalSize)
                offset += writeData.totalSize
                temp to writeData
            }.map { (temp, writeData) ->
                this@coroutineScope.launch(PACK_DISPATCHER) {
                    temp.use { temp -> writeRsgpToMainStream(temp, writeData) }
                }
            }.joinAll()
            outFileStream.setWritePosition(offset)
        }

        return@coroutineScope totalPtx
    }

    private fun parseGroupXml(groupXml: File): List<ResourceGroupData> {
        val groupRoot = groupXml.readXml().getFirstChildByTag("ResourcesGroupInfo") ?: return emptyList()
        return groupRoot.childes
            .filter { it.getAttr("id")?.isNotEmpty() == true }
            .mapIndexed { index, node ->
                val resources = node.childes.mapNotNull { resNode ->
                    val id = resNode.getAttr("id") ?: return@mapNotNull null
                    val data = when (resNode.nodeName) {
                        "Res" -> ResourceData.ResData(index, id)
                        "Img" -> ResourceData.ImgData(index, id, resNode.getAttr("defaultformat")?.toIntOrNull() ?: 0)
                        else -> null
                    }
                    if (data != null) rsbInfo.fileList.add(data.comp)
                    data
                }
                ResourceGroupData(
                    id = node.getAttr("id") ?: "",
                    poolIndex = node.getAttr("poolindex")?.toUInt(),
                    compressMethod = node.getAttr("compressmethod")?.toUInt(),
                    resources = resources
                )
            }
    }

    private fun initRsbInfo(groupCount: Int) {
        rsbInfo.apply {
            head.rsgp_Number = groupCount.toUInt()
            rsgpInfo = MutableList(groupCount) { RsbRsgpInfo() }
            rsgp = MutableList(groupCount) { RsgpInfo() }
            if (!config.specialPool) {
                head.autopool_Number = groupCount.toUInt()
                autopoolInfo = MutableList(groupCount) { RsbAutoPoolInfo() }
            }
        }
    }

    private fun calculatePtxIndices(groupDataList: List<ResourceGroupData>): Pair<List<UInt>, UInt> {
        val list = mutableListOf<UInt>()
        var cumulativePtx = 0u
        groupDataList.forEach { group ->
            list.add(cumulativePtx)
            cumulativePtx += group.resources.filterIsInstance<ResourceData.ImgData>().size.toUInt()
        }
        allPtxList = mutableListOf()
        return list to cumulativePtx
    }

    private suspend fun mergeDataStructures(processResults: List<RsgpProcessResult>) {
        processResults.forEach { (rsgpResult, i, _) ->
            mergeMutex.withLock {
                rsbInfo.rsgpInfo[i] = rsgpResult.info
                rsbInfo.rsgp[i] = rsgpResult.data
                if (config.specialPool) {
                    rsbInfo.autopoolInfo[rsgpResult.info.pool_Index.toInt()] = rsgpResult.autopool
                } else {
                    rsbInfo.autopoolInfo[i] = rsgpResult.autopool.apply {
                        ID = rsgpResult.info.ID + Rsb.AUTO_POOL_NAME
                    }
                }
                allPtxList.addAll(rsgpResult.ptxList)
                rsbInfo.rsgpList.add(CompressString(
                    rsgpResult.info.ID.uppercase(),
                    RsbExtraInfo(i.toUInt())
                ))
            }
        }
    }

    private suspend fun prepareSingleRsgp(
        index: Int,
        groupData: ResourceGroupData,
        ptxBefore: UInt,
        fGlobal: UInt
    ): RsgpPreResult = withContext(PACK_DISPATCHER) {
        var thisPtxCount = 0u
        val ptxList = mutableListOf<RsbPtxInfo>()

        val rsgpInfo = RsbRsgpInfo().apply {
            val f = groupData.compressMethod ?: fGlobal
            ID = groupData.id
            pool_Index = groupData.poolIndex ?: index.toUInt()
            flags = f
            offset = 0u
            ptx_BeforeNumber = ptxBefore
        }

        val autopool = RsbAutoPoolInfo()
        val rsgp = RsgpInfo().apply {
            head = RsgpHeadInfo().apply {
                flags = rsgpInfo.flags
                version = config.version
            }
        }

        var bsPResData: CoroutineBinaryStream? = null
        var bsPImgData: CoroutineBinaryStream? = null
        var bsPResDataSize = 0L
        var bsPImgDataSize = 0L

        val preprocessedList = groupData.resources.mapNotNull { resData ->
            val comp = resData.comp
            val resFile = inFolder.resolve(comp.fileName ?: return@mapNotNull null)
            var convertStream: CoroutineBinaryStream? = null

            if (config.autoJsonToRton && resFile.extension.equals("rton", ignoreCase = true)) {
                val jsonFile = resFile.resolveSibling("${resFile.nameWithoutExtension}.JSON")
                if (jsonFile.exists()) {
                    convertStream = CoroutineBinaryStream.create(resFile.absolutePath)
                    RTON.encodeAuto(jsonFile, convertStream)
                    if (config.deleteOriginalJson) {
                        jsonFile.delete()
                        convertStream.saveFile()
                    }
                }
            }

            if (config.autoPngToPtx && resData is ResourceData.ImgData && resFile.extension.equals("ptx", ignoreCase = true)) {
                val pngFile = resFile.resolveSibling("${resFile.nameWithoutExtension}.PNG")
                if (pngFile.exists()) {
                    convertStream = CoroutineBinaryStream.create(resFile.absolutePath,pngFile.length().toInt())
                    Ptx.encode(
                        pngFile, convertStream,
                        PtxFormat.form(resData.defaultFormat),
                        config.endian, config.ptxInfoLength != 0x10u
                    )
                    if (config.deleteOriginalPng) {
                        pngFile.delete()
                        convertStream.saveFile()
                    }
                }
            }

            if (!resFile.exists() && convertStream == null) {
                logError("资源文件不存在：${resFile.absolutePath}")
                return@mapNotNull null
            }

            var bytes = convertStream ?: CoroutineBinaryStream.open(resFile)
            when (resData) {
                is ResourceData.ImgData -> {
                    val ptx = RsbPtxInfo(config.ptxInfoLength)
                    bytes.use { bsPtx ->
                        bsPtx.endian = config.endian
                        bsPtx.idInt32(Rsb.PTX_MAGIC)
                        bsPtx.idInt32(1)
                        ptx.apply {
                            width = bsPtx.readUInt32()
                            height = bsPtx.readUInt32()
                            check = bsPtx.readUInt32()
                            format = bsPtx.readUInt32()
                            alphaSize = bsPtx.readUInt32()
                            alphaFormat = bsPtx.readUInt32()
                        }
                        bytes = bsPtx.slice(Rsb.PTX_HEADER_SIZE, bsPtx.length - Rsb.PTX_HEADER_SIZE)
                        rsgp.fileList.addCompress(CompressString(
                            comp.name, RsgpPart1ExtraInfo(
                                bsPImgDataSize.toUInt(), bytes.length.toUInt(),
                                thisPtxCount++, ptx.width, ptx.height
                            )
                        ))
                        bytes.alignFourK()
                        bsPImgDataSize += bytes.length
                    }
                    ptxList.add(ptx)
                }
                is ResourceData.ResData -> {
                    bytes.let { bsRes ->
                        rsgp.fileList.addCompress(CompressString(
                            comp.name, RsgpPart0ExtraInfo(
                                bsPResDataSize.toUInt(), bsRes.length.toUInt()
                            )
                        ))
                        bsRes.alignFourK()
                        bsPResDataSize += bsRes.length
                    }
                }
            }

            PreprocessedResource(resData, comp, bytes, bytes.length)
        }

        if (config.compressPart0) {
            CoroutineBinaryStream.allocateBytes(bsPResDataSize.toInt()).apply { endian = config.endian }.use { tempInputStream ->
                preprocessedList.forEach { pre ->
                    if (pre.resData is ResourceData.ResData) {
                        pre.fileBytes.copyTo(tempInputStream)
                        pre.fileBytes.close()
                    }
                }
                tempInputStream.alignFourK()
                val tempStream = CoroutineBinaryStream.allocateBytes().apply { endian = config.endian }
                compressZLib(tempInputStream, tempStream, config.compressionLevel)
                if (tempStream.length == 0L) {
                    tempStream.writeUInt8(0x78u)
                    tempStream.writeUInt8(config.emptyCompressHead.toUByte())
                    tempStream.writeUInt8(0x03u)
                    tempStream.writeUInt8(0x00u)
                    tempStream.writeUInt8(0x00u)
                    tempStream.writeUInt8(0x00u)
                    tempStream.writeUInt8(0x00u)
                    tempStream.writeUInt8(0x01u)
                }
                tempStream.alignFourK()
                bsPResData = tempStream
            }
        }
        if (config.compressPart1) {
            CoroutineBinaryStream.allocateBytes(bsPImgDataSize.toInt()).apply { endian = config.endian }.use { tempInputStream ->
                preprocessedList.forEach { pre ->
                    if (pre.resData is ResourceData.ImgData) {
                        pre.fileBytes.copyTo(tempInputStream)
                        pre.fileBytes.close()
                    }
                }
                tempInputStream.alignFourK()
                val tempStream = CoroutineBinaryStream.allocateBytes().apply { endian = config.endian }
                compressZLib(tempInputStream, tempStream, config.compressionLevel)

                tempStream.alignFourK()
                bsPImgData = tempStream
            }
        }

        bsPResData?.readPosition = 0
        bsPImgData?.readPosition = 0

        val bsPResDataZSize = bsPResData?.length?.toUInt() ?: bsPResDataSize.toUInt()
        val bsPImgDataZSize = bsPImgData?.length?.toUInt() ?: bsPImgDataSize.toUInt()

        rsgp.apply {
            head.part0_Size = bsPResDataSize.toUInt()
            head.part1_Size = bsPImgDataSize.toUInt()
            head.part0_ZSize = bsPResDataZSize
            head.part1_ZSize = bsPImgDataZSize
        }
        rsgpInfo.apply {
            part0_Size = bsPResDataSize.toUInt()
            part0_Size2 = bsPResDataSize.toUInt()
            part0_ZSize = bsPResDataZSize
            part1_Size = bsPImgDataSize.toUInt()
            part1_ZSize = bsPImgDataZSize
        }

        if (rsgp.head.part1_Size > autopool.part1_MaxSize) {
            autopool.part1_MaxSize = rsgp.head.part1_Size
        }

        return@withContext RsgpPreResult(
            rsgpInfo, rsgp, autopool, ptxList, thisPtxCount,
            preprocessedList, bsPResData, bsPImgData, bsPResDataSize, bsPImgDataSize
        )
    }

    private suspend fun processSingleRsgp(pre: RsgpPreResult): RsgpWriteData = withContext(PACK_DISPATCHER) {
        var rsqpSize = 0L
        val fileListBytes = pre.rsgp.fileList.write()
        pre.rsgp.head.fileList_Length = fileListBytes.size.toUInt()

        val fileListAligned = (pre.rsgp.head.fileList_BeginOffset + fileListBytes.size.toUInt()).toLong().alignFourKSize
        rsqpSize += fileListAligned

        pre.rsgp.head.fileOffset = rsqpSize.toUInt()
        pre.rsgp.head.part0_Offset = rsqpSize.toUInt()
        pre.rsgpInfo.fileOffset = pre.rsgp.head.fileOffset
        pre.rsgpInfo.part0_Offset = pre.rsgp.head.part0_Offset

        val part0Size = if (pre.bsPResData != null) {
            pre.bsPResData.length
        } else {
            pre.preprocessedList.filter { it.resData is ResourceData.ResData }
                .sumOf { it.fileBytes.length }
                .alignFourKSize
        }
        rsqpSize += part0Size
        pre.rsgp.head.part1_Offset = rsqpSize.toUInt()
        pre.rsgpInfo.part1_Offset = pre.rsgp.head.part1_Offset

        val part1Size = if (pre.bsPImgData != null) {
            pre.bsPImgData.length
        } else {
            pre.preprocessedList.filter { it.resData is ResourceData.ImgData }
                .sumOf { it.fileBytes.length }
                .alignFourKSize
        }
        rsqpSize += part1Size

        pre.rsgpInfo.size = rsqpSize.toUInt()
        pre.rsgpInfo.ptx_Number = pre.thisPtxCount

        val poolP1Off = pre.rsgp.head.part0_Offset + pre.rsgp.head.part0_Size
        if (poolP1Off > pre.autopool.part1_MaxOffset_InDecompress) {
            pre.autopool.part1_MaxOffset_InDecompress = poolP1Off
        }

        return@withContext RsgpWriteData(pre, fileListBytes, rsqpSize)
    }

    private suspend fun writeRsgpToMainStream(bsRsgp: CoroutineBinaryStream, writeData: RsgpWriteData) {
        val pre = writeData.pre
        val fileListBytes = writeData.fileListBytes
        val startPos = bsRsgp.writePosition

        bsRsgp.setWritePosition(pre.rsgp.head.fileList_BeginOffset.toLong() + startPos)
        bsRsgp.writeBytes(fileListBytes)
        bsRsgp.alignFourK()

        bsRsgp.setWritePosition(pre.rsgp.head.part0_Offset.toLong() + startPos)
        if (pre.bsPResData != null) {
            pre.bsPResData.copyTo(bsRsgp)
            pre.bsPResData.close()
        } else {
            pre.preprocessedList.forEach { item ->
                if (item.resData is ResourceData.ResData) {
                    item.fileBytes.copyTo(bsRsgp)
                    item.fileBytes.close()
                }
            }
            bsRsgp.alignFourK()
        }

        bsRsgp.setWritePosition(pre.rsgp.head.part1_Offset.toLong() + startPos)
        if (pre.bsPImgData != null) {
            pre.bsPImgData.copyTo(bsRsgp)
            pre.bsPImgData.close()
        } else {
            pre.preprocessedList.forEach { item ->
                if (item.resData is ResourceData.ImgData) {
                    item.fileBytes.copyTo(bsRsgp)
                    item.fileBytes.close()
                }
            }
            bsRsgp.alignFourK()
        }

        bsRsgp.setWritePosition(startPos)
        pre.rsgp.head.write(bsRsgp)
        bsRsgp.setWritePosition(startPos + writeData.totalSize)
    }

    private suspend fun processSimplePool() = processResourceGroups()

    private suspend fun loadPackConfig() {
        val packInfo = studioPath.resolve("PACKINFO.XML")
        if (packInfo.exists()) {
            val xml = packInfo.readXml()
            xml.getFirstChildByTag("PackInfo")?.childes?.forEach { child ->
                when (child.nodeName) {
                    "PackageVersion" -> config.version = child.textContent.toInt()
                    "UseBigEndian" -> config.bigEndian = child.textContent.toBoolean()
                    "CompressMethod" -> {
                        val flags = child.textContent.toUInt()
                        config.compressPart0 = (flags and 0b10u) != 0u
                        config.compressPart1 = (flags and 0b1u) != 0u
                    }
                    "PtxInfoLength" -> config.ptxInfoLength = child.textContent.toUInt()
                    "ZlibAll" -> config.smfCompress = child.textContent.toBoolean()
                    "CompressLevel" -> config.compressionLevel = when (child.textContent) {
                        "Fastest" -> CompressionLevel.Fastest
                        "Smallest" -> CompressionLevel.Smallest
                        else -> CompressionLevel.Optimal
                    }
                    "SpecialPool" -> config.specialPool = child.textContent.toBoolean()
                }
            }
        }
        config.configBlock()
    }

    private suspend fun processSpecialPool() {
        val poolXml = studioPath.resolve("POOL.XML")
        if (!poolXml.exists()) {
            logError("POOL.XML不存在")
            return
        }
        val poolRoot = poolXml.readXml().getFirstChildByTag("PoolInfo") ?: return
        rsbInfo.head.autopool_Number = poolRoot.childes.size.toUInt()
        rsbInfo.autopoolInfo = poolRoot.childes.map { child ->
            RsbAutoPoolInfo().apply {
                ID = child.getAttr("id") ?: ""
                type = child.getAttr("type")?.toInt() ?: 0
            }
        }.toMutableList()
        processResourceGroups()
    }

    private suspend fun writeFinalRsb(writeBsRsgp: suspend (CoroutineBinaryStream) -> Unit) {
        CoroutineBinaryStream.create(outFile.absolutePath).use { bs ->
            bs.endian = config.endian
            rsbInfo.head.write(bs)
            writeGlobalFileList(bs, rsbInfo.fileList, "file")
            writeGlobalFileList(bs, rsbInfo.rsgpList, "rsgp")
            writeCompositeResources(bs)
            writeGlobalFileList(bs, rsbInfo.compositeList, "composite")

            rsbInfo.head.rsgpInfo_BeginOffset = bs.writePosition.toUInt()
            bs.setLength(bs.writePosition + (RsbHeadInfo.rsgpInfo_EachLength * rsbInfo.head.rsgp_Number).toLong())
            bs.setWritePosition(bs.length)

            rsbInfo.head.autopoolInfo_BeginOffset = bs.writePosition.toUInt()
            rsbInfo.autopoolInfo.forEach { it.write(bs) }

            rsbInfo.head.ptxInfo_BeginOffset = bs.writePosition.toUInt()
            allPtxList.forEach { it.write(bs) }
            writeXmlResources(bs)

            bs.alignFourK()
            rsbInfo.head.headLength = bs.writePosition.toUInt()

            config.invokeOnProgress(0.95f, "正在写入资源数据...")
            rsbInfo.rsgpInfo.forEach { it.offset += rsbInfo.head.headLength }
            writeBsRsgp(bs)

            bs.setWritePosition(rsbInfo.head.rsgpInfo_BeginOffset.toLong())
            rsbInfo.rsgpInfo.forEach { it.write(bs) }
            bs.setWritePosition(0)
            rsbInfo.head.write(bs)

            if (config.smfCompress) {
                applySmfCompression(bs, outFile)
            } else bs.saveFile(outFile)
        }
    }

    private suspend fun writeGlobalFileList(bs: CoroutineBinaryStream, list: CompressStringList, type: String) {
        val data = list.write()
        when (type) {
            "file" -> {
                rsbInfo.head.fileList_BeginOffset = bs.writePosition.toUInt()
                rsbInfo.head.fileList_Length = data.size.toUInt()
            }
            "rsgp" -> {
                rsbInfo.head.rsgpList_BeginOffset = bs.writePosition.toUInt()
                rsbInfo.head.rsgpList_Length = data.size.toUInt()
            }
            "composite" -> {
                rsbInfo.head.compositeList_BeginOffset = bs.writePosition.toUInt()
                rsbInfo.head.compositeList_Length = data.size.toUInt()
            }
        }
        bs.writeBytes(data)
    }

    private suspend fun writeCompositeResources(bs: CoroutineBinaryStream) {
        rsbInfo.head.compositeInfo_BeginOffset = bs.writePosition.toUInt()
        val file = studioPath.resolve("COMPOSITERESOURCES.XML")
        if (file.exists()) {
            val root = file.readXml().getFirstChildByTag("CompositeResourcesInfo")
            val children = root?.childes?.filter { it.getAttr("id")?.isNotEmpty() == true }
            if (children != null) {
                rsbInfo.compositeInfo = MutableList(children.size) { RsbCompositeInfo() }
                rsbInfo.head.composite_Number = children.size.toUInt()
                children.forEachIndexed { i, child ->
                    rsbInfo.compositeInfo[i].ID = child.getAttr("id") ?: ""
                    val infos = child.childes.filter { it.getAttr("index")?.isNotEmpty() == true }
                    rsbInfo.compositeInfo[i].child_Number = infos.size.toUInt()
                    rsbInfo.compositeList.add(CompressString(rsbInfo.compositeInfo[i].ID.uppercase(), RsbExtraInfo(i.toUInt())))
                    infos.forEachIndexed { j, c ->
                        rsbInfo.compositeInfo[i].child_Info[j].index = c.getAttr("index")?.toUInt() ?: 0u
                        rsbInfo.compositeInfo[i].child_Info[j].ratio = c.getAttr("res")?.toUInt() ?: 0u
                        rsbInfo.compositeInfo[i].child_Info[j].language = c.getAttr("loc") ?: ""
                    }
                    rsbInfo.compositeInfo[i].write(bs)
                }
            }
        }
    }

    private suspend fun writeXmlResources(bs: CoroutineBinaryStream) {
        val file = studioPath.resolve("RESOURCES.XML")
        if (file.exists()) {
            if (config.endian == Endian.Big) bs.alignFourK()
            CoroutineBinaryStream.allocateBytes().use { bsXml ->
                bsXml.endian = config.endian
                XmlConvert.xmlToDat(file.absolutePath, bsXml)
                bsXml.readPosition = 0
                bsXml.idInt32(Rsb.XML_MAGIC)
                bsXml.idInt32(1)
                rsbInfo.head.xmlPart1_BeginOffset = bs.readPosition.toUInt()
                val k = bsXml.readInt32()
                rsbInfo.head.xmlPart2_BeginOffset = rsbInfo.head.xmlPart1_BeginOffset + bsXml.readInt32().toUInt() - k.toUInt()
                rsbInfo.head.xmlPart3_BeginOffset = rsbInfo.head.xmlPart1_BeginOffset + bsXml.readInt32().toUInt() - k.toUInt()
                bsXml.copyTo(bs)
            }
        }
    }

    private suspend fun applySmfCompression(input: CoroutineBinaryStream, output: File) {
        input.let { bsIn ->
            CoroutineBinaryStream.create(output.absolutePath).use { bsOut ->
                bsIn.readPosition = 0
                bsOut.writeInt32(Rsb.SMF_MAGIC)
                bsOut.writeInt32(bsIn.length.toInt())
                compressZLib(bsIn, bsOut, CompressionLevel.Smallest)
                bsOut.readPosition = 0
                bsOut.saveFile(output)
            }
        }
    }
}

// ======================================================================================
// 解压核心实现
// ======================================================================================
private class RsbUnpacker {
    private val outFolder: File

    private val configBlock:suspend RsbUnpackConfig.() -> Unit

    private val inFile: File?
    private val inFileStream: CoroutineBinaryStream?

    constructor(inFile: File, outFolder: File, configBlock:suspend RsbUnpackConfig.() -> Unit) {
        this.outFolder = outFolder
        this.configBlock = configBlock
        this.inFile = inFile
        this.inFileStream = null
    }

    constructor(inFileStream: CoroutineBinaryStream, outFolder: File, configBlock:suspend RsbUnpackConfig.() -> Unit) {
        this.outFolder = outFolder
        this.configBlock = configBlock
        this.inFile = null
        this.inFileStream = inFileStream
    }

    private var compressLevel = 0
    private var getLevel = true
    private lateinit var bs: CoroutineBinaryStream
    private var useSmf = false
    private lateinit var rsbInfo: RsbInfo
    private var usePoolXml = false
    private val studioDir: File by lazy { outFolder.resolve("POPSTUDIOINFO").apply { mkdirs() } }
    private lateinit var unpackConfig: RsbUnpackConfig

    private suspend fun logInfo(msg: String) = unpackConfig.invokeOnLog(RsbCommonConfig.LogLevel.INFO, msg)
    private suspend fun logWarn(msg: String) = unpackConfig.invokeOnLog(RsbCommonConfig.LogLevel.WARN,msg)
    private suspend fun logError(msg: String) = unpackConfig.invokeOnLog(RsbCommonConfig.LogLevel.ERROR,msg)

    @JvmSynthetic
    @JvmName("---")
    suspend fun execute() {
        try {
            unpackConfig = RsbUnpackConfig().apply { configBlock() }
            unpackConfig.invokeOnStart()
            logInfo("开始解压: ${inFile?.absolutePath ?: inFileStream?.length}")
            checkInput()
            processUnpack()
            unpackConfig.invokeOnProgress(1.0f, "解压完成")
            unpackConfig.invokeOnFinish(outFolder)
            logInfo("解压完成")
        } catch (e: Throwable) {
            unpackConfig.invokeOnError(e, "解压失败: ${e.message ?: "未知错误"}")
            throw e
        } finally {
            if (::bs.isInitialized) bs.close()
        }
    }

    private fun checkInput() {
        if (inFile != null && !inFile.exists()) {
            error("文件不存在: $inFile")
        }
        if (inFileStream != null && inFileStream.length == 0L) {
            error("空流无法读取: $inFile")
        }
        outFolder.mkdirs()
    }

    private suspend fun processUnpack() {
        val bsOrigin = if (inFile != null) CoroutineBinaryStream.open(inFile) else inFileStream ?: return
        bs = decompressSmf(bsOrigin)
        bs.readPosition = 0

        detectEndian()
        rsbInfo = RsbInfo().read(bs)
        unpackConfig.invokeOnHeaderRead(rsbInfo)
        unpackConfig.invokeOnProgress(0.1f, "读取头部信息完成")

        usePoolXml = shouldUsePoolXml()

        extractResourcesXml()
        unpackConfig.invokeOnProgress(0.12f, "提取RESOURCES.XML完成")

        extractCompositeResources()
        unpackConfig.invokeOnProgress(0.15f, "提取复合资源完成")

        val (level, gotLevel) = extractResourceGroups()
        unpackConfig.invokeOnProgress(0.95f, "解压资源组完成")

        extractPoolXmlIfNeeded()
        writePackInfo(level, gotLevel)
    }

    private suspend fun decompressSmf(bsOrigin: CoroutineBinaryStream): CoroutineBinaryStream {
        val isSmfMagic = bsOrigin.readInt32() == Rsb.SMF_MAGIC
        if (isSmfMagic) {
            useSmf = true
            val length = bsOrigin.readInt32()
            return bsOrigin.use { bsOrigin ->
                CoroutineBinaryStream.openStream(length.toLong()) { decompressZLib(bsOrigin,length) }
            }
        }
        bsOrigin.readPosition = 0
        useSmf = bsOrigin.isZlib()
        return if (useSmf) {
            bsOrigin.use { bsOrigin ->
                val bsMem = CoroutineBinaryStream.allocateBytes(bsOrigin.length.toInt())
                decompressZLib(bsOrigin, bsMem)
                bsMem
            }
        } else bsOrigin
    }

    private fun detectEndian() {
        when (bs.peekInt32()) {
            Rsb.RSB_MAGIC_RSB1 -> bs.endian = if (bs.endian == Endian.Small) Endian.Big else Endian.Small
            Rsb.RSB_MAGIC_1BSR -> {}
            else -> error("数据不匹配")
        }
    }

    private fun shouldUsePoolXml(): Boolean {
        if (rsbInfo.rsgpInfo.size != rsbInfo.autopoolInfo.size) return true
        rsbInfo.rsgpInfo.forEachIndexed { i, info ->
            if (info.pool_Index.toInt() != i || rsbInfo.autopoolInfo[i].type != 1) return true
        }
        return false
    }

    private suspend fun extractResourcesXml() {
        if (rsbInfo.head.xmlPart1_BeginOffset != 0u) {
            CoroutineBinaryStream.allocateBytes().use { bs2 ->
                bs2.endian = bs.endian
                bs2.writeInt32(Rsb.XML_MAGIC)
                bs2.writeInt32(1)
                bs2.writeInt32(0x14)
                bs2.writeInt32((rsbInfo.head.xmlPart2_BeginOffset - rsbInfo.head.xmlPart1_BeginOffset + 0x14u).toInt())
                bs2.writeInt32((rsbInfo.head.xmlPart3_BeginOffset - rsbInfo.head.xmlPart1_BeginOffset + 0x14u).toInt())
                bs.readPosition = rsbInfo.head.xmlPart1_BeginOffset.toLong()
                bs.slice((rsbInfo.head.headLength - rsbInfo.head.xmlPart1_BeginOffset).toLong()).use { xml ->
                    xml.copyTo(bs2)
                }
                bs2.readPosition = 0
                XmlConvert.datToXml(bs2, studioDir.resolve("RESOURCES.XML").absolutePath)
            }
        }
    }

    private fun extractCompositeResources() {
        val file = studioDir.resolve("COMPOSITERESOURCES.XML")
        val (doc, root) = createXmlDocument("CompositeResourcesInfo")
        rsbInfo.compositeInfo.forEach { ci ->
            val compositeEle = doc.createElement("CompositeResources")
            compositeEle.setAttr("id", ci.ID)
            root.appendChild(compositeEle)
            repeat(ci.child_Number.toInt()) { j ->
                val child = ci.child_Info[j]
                val groupEle = doc.createElement("Group")
                groupEle.setAttr("index", child.index.toString())
                groupEle.setAttr("res", child.ratio.toString())
                groupEle.setAttr("loc", child.language)
                compositeEle.appendChild(groupEle)
            }
        }
        doc.writeXmlToFile(file)
    }

    private suspend fun extractResourceGroups(): Pair<Int, Boolean> = coroutineScope {
        var level = compressLevel
        var gotLevel = getLevel
        val levelMutex = Mutex()

        val file = studioDir.resolve("RESOURCESGROUP.XML")
        val (doc, root) = createXmlDocument("ResourcesGroupInfo")

        val results = mutableListOf<ProcessedGroupResult>()
        val resultMutex = Mutex()

        val totalGroups = rsbInfo.head.rsgp_Number.toInt()

        val completedCount = AtomicInteger(0)

        (0 until totalGroups).map { i ->
            launch(PACK_DISPATCHER) {
                retrySuspend(
                    maxRetries = unpackConfig.maxRetries,
                    baseDelayMs = unpackConfig.retryDelayMs,
                    onRetry = { attempt, e ->
                        val info = rsbInfo.rsgpInfo[i]
                        logWarn("解压资源组[$i] ${info.ID} 第 $attempt 次重试，原因：${e.message}")
                    }
                ) {
                    val info = rsbInfo.rsgpInfo[i]
                    unpackConfig.invokeOnResourceGroupStart(i, info.ID)
                    runCatching {
                        var rBack = info.offset
                        var bsUsed = bs.createDomain()

                        val rsgp = try {
                            bsUsed.readPosition = rBack.toLong()
                            RsgpInfo().read(bsUsed)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            val guess = findRsgFile(inFile?.parentFile ?: return@runCatching, info.ID) ?: return@runCatching
                            rBack = 0u
                            bsUsed.close()
                            bsUsed = CoroutineBinaryStream.open(guess.absolutePath).apply {
                                endian = when (peekInt32()) {
                                    1885827954 -> Endian.Big
                                    1920165744 -> Endian.Small
                                    else -> return@runCatching
                                }
                            }
                            RsgpInfo().read(bsUsed)
                        }

                        var localLevel = level
                        var localGotLevel = gotLevel
                        levelMutex.withLock {
                            localLevel = level
                            localGotLevel = gotLevel
                        }

                        val (bsP0, bsP1, newLevel, newGotLevel) = decompressParts(bsUsed, rBack, rsgp, localLevel, localGotLevel)

                        try {
                            levelMutex.withLock {
                                if (gotLevel) {
                                    level = newLevel
                                    gotLevel = newGotLevel
                                }
                            }

                            val resources = mutableListOf<ResourceData>()
                            rsgp.fileList.forEach { str ->
                                val outFileItem = outFolder.resolve(str.fileName ?: "").apply { parentFile?.mkdirs() }
                                when (str.type) {
                                    1 -> {
                                        val ex = str.extraInfo as RsgpPart0ExtraInfo
                                        bsP0.readPosition = ex.offset.toLong()
                                        bsP0.slice(ex.size.toLong()).use { outFileStream ->
                                            if (unpackConfig.autoRtonToJson && outFileItem.extension.equals("rton", ignoreCase = true)) {
                                                val jsonFile = outFileItem.resolveSibling("${outFileItem.nameWithoutExtension}.JSON")
                                                RTON.decodeAuto(outFileStream, jsonFile)
                                                if (!unpackConfig.deleteOriginalRton) {
                                                    outFileStream.readPosition = 0
                                                    withContext(Dispatchers.IO) { outFileStream.copyTo(outFileItem) }
                                                }
                                            } else {
                                                withContext(Dispatchers.IO) { outFileStream.copyTo(outFileItem) }
                                            }
                                        }

                                        resources.add(ResourceData.ResData(-1, str.name.orEmpty()))
                                    }
                                    2 -> {
                                        val ex = str.extraInfo as RsgpPart1ExtraInfo
                                        val ptx = rsbInfo.getPtxInfo(bs,info.ptx_BeforeNumber.toInt() + ex.index.toInt())
                                        bsP1.readPosition = ex.offset.toLong()

                                        val header = CoroutineBinaryStream.allocateBytes().apply {
                                            endian = bsUsed.endian
                                            writeInt32(Rsb.PTX_MAGIC)
                                            writeInt32(1)
                                            writeUInt32(ptx.width)
                                            writeUInt32(ptx.height)
                                            writeUInt32(ptx.check)
                                            writeUInt32(ptx.format)
                                            writeUInt32(ptx.alphaSize)
                                            writeUInt32(ptx.alphaFormat)
                                        }
                                        val dataSlice = bsP1.slice(ex.size.toLong())

                                        try {
                                            if (unpackConfig.autoPtxToPng && outFileItem.extension.equals("ptx", ignoreCase = true)) {
                                                val pngFile = outFileItem.resolveSibling("${outFileItem.nameWithoutExtension}.PNG")
                                                Ptx.decode(dataSlice, ptx.toPtxHead(), true).use { png ->
                                                    withContext(Dispatchers.IO) { png.saveToPng(pngFile) }
                                                }
                                                if (!unpackConfig.deleteOriginalPtx) {
                                                    withContext(Dispatchers.IO) {
                                                        FileOutputStream(outFileItem).channel.use { fos ->
                                                            fos.write(header.toBuffer())
                                                            dataSlice.readPosition = 0
                                                            fos.write(dataSlice.toBuffer())
                                                        }
                                                    }
                                                }
                                            } else {
                                                withContext(Dispatchers.IO) {
                                                    FileOutputStream(outFileItem).channel.use { fos ->
                                                        fos.write(header.toBuffer())
                                                        fos.write(dataSlice.toBuffer())
                                                    }
                                                }
                                            }
                                        } finally {
                                            header.close()
                                            dataSlice.close()
                                        }
                                        resources.add(ResourceData.ImgData(-1, str.name.orEmpty(), ptx.format.toInt()))
                                    }
                                }
                            }

                            resultMutex.withLock {
                                results.add(ProcessedGroupResult(i, info.ID, info.flags, info.pool_Index, resources))
                            }
                        } finally {
                            bsUsed.close()
                            bsP0.close()
                            bsP1.close()
                        }

                        unpackConfig.invokeOnResourceGroupEnd(i, info.ID)
                        val current = completedCount.incrementAndGet()
                        val progress = 0.15f + 0.80f * current / totalGroups
                        unpackConfig.invokeOnProgress(progress, "解压资源组 ${info.ID} $current/$totalGroups")
                    }.onFailure { e ->
                        val errorMsg = "解压资源组[$i] ${info.ID} 失败"
                        logError(errorMsg)
                        unpackConfig.invokeOnError(e, errorMsg)
                        throw e
                    }
                }
            }
        }.joinAll()

        results.sortedBy { it.index }.forEach { result ->
            val groupEle = doc.createElement("Group")
            groupEle.setAttr("id", result.id)
            groupEle.setAttr("compressmethod", result.flags.toString())
            if (usePoolXml) groupEle.setAttr("poolindex", result.poolIndex.toString())
            root.appendChild(groupEle)

            result.resources.forEach { res ->
                when (res) {
                    is ResourceData.ResData -> {
                        val resEle = doc.createElement("Res")
                        resEle.setAttr("id", res.id)
                        groupEle.appendChild(resEle)
                    }
                    is ResourceData.ImgData -> {
                        val imgEle = doc.createElement("Img")
                        imgEle.setAttr("id", res.id)
                        imgEle.setAttr("defaultformat", res.defaultFormat.toString())
                        groupEle.appendChild(imgEle)
                    }
                }
            }
        }

        doc.writeXmlToFile(file)
        return@coroutineScope level to gotLevel
    }

    private fun findRsgFile(parent: File, id: String): File? {
        return listOf("", ".rsg", ".rsgp", ".rsg.smf", ".rsgp.smf")
            .firstNotNullOfOrNull { ext -> parent.resolve("$id$ext").takeIf { it.exists() } }
    }

    private fun decompressParts(
        bsUsed: CoroutineBinaryStream,
        rBack: UInt,
        rsgp: RsgpInfo,
        compressLevel: Int,
        getLevel: Boolean
    ): Quad<CoroutineBinaryStream, CoroutineBinaryStream, Int, Boolean> {
        var level = compressLevel
        var gotLevel = getLevel

        fun processPart(offset: Long,size: UInt, zSize: UInt): CoroutineBinaryStream {
            bsUsed.readPosition = offset + zSize.toInt()
            bsUsed.slice(offset, length = zSize.toLong()).use { temp ->
                try {
                    if (gotLevel) {
                        level = temp.peekUInt16(Endian.Small).toInt() shr 14
                    }
                    return if (temp.isZlib()) {
                        val output = CoroutineBinaryStream.openStream(size.toLong()) { decompressZLib(temp,size.toInt()) }
                        gotLevel = false
                        output
                    } else temp
                } catch (e: Exception) {
                    return temp
                }
            }
        }

        val bsP0 = processPart((rBack + rsgp.head.part0_Offset).toLong(), rsgp.head.part0_Size, rsgp.head.part0_ZSize)
        val bsP1 = processPart((rBack + rsgp.head.part1_Offset).toLong(), rsgp.head.part1_Size, rsgp.head.part1_ZSize)

        bsP0.readPosition = 0
        bsP1.readPosition = 0
        return Quad(bsP0, bsP1, level, gotLevel)
    }

    private fun extractPoolXmlIfNeeded() {
        if (usePoolXml) {
            val file = studioDir.resolve("POOL.XML")
            val (doc, root) = createXmlDocument("PoolInfo")
            rsbInfo.autopoolInfo.forEach {
                val poolEle = doc.createElement("Pool")
                poolEle.setAttr("id", it.ID)
                poolEle.setAttr("type", it.type.toString())
                root.appendChild(poolEle)
            }
            doc.writeXmlToFile(file)
        }
    }

    private suspend fun writePackInfo(level: Int, gotLevel: Boolean) {
        unpackConfig.apply {
            version = rsbInfo.head.version
            endian = bs.endian
            smfCompress = useSmf
            specialPool = usePoolXml
            ptxInfoLength = rsbInfo.head.ptxInfo_EachLength
            compressionLevel = if (gotLevel) CompressionLevel.Optimal else when (level) {
                0 -> CompressionLevel.Fastest
                3 -> CompressionLevel.Smallest
                else -> CompressionLevel.Optimal
            }
            val flags = rsbInfo.rsgpInfo.firstOrNull()?.flags ?: 1u
            compressPart0 = (flags and 0b10u) != 0u
            compressPart1 = (flags and 0b1u) != 0u
        }
        unpackConfig.apply { configBlock() }

        val file = studioDir.resolve("PACKINFO.XML")
        val (doc, root) = createXmlDocument("PackInfo")
        fun addNode(tag: String, value: String) {
            val ele = doc.createElement(tag)
            ele.textContent = value
            root.appendChild(ele)
        }

        addNode("PackageVersion", unpackConfig.version.toString())
        addNode("UseBigEndian", unpackConfig.bigEndian.toString())
        addNode("CompressMethod", ((if (unpackConfig.compressPart0) 0b10u else 0u) or (if (unpackConfig.compressPart1) 0b1u else 0u)).toString())
        addNode("CompressLevel", unpackConfig.compressionLevel.name)
        addNode("PtxInfoLength", unpackConfig.ptxInfoLength.toString())
        addNode("ZlibAll", unpackConfig.smfCompress.toString())
        addNode("SpecialPool", unpackConfig.specialPool.toString())

        doc.writeXmlToFile(file)
    }
}