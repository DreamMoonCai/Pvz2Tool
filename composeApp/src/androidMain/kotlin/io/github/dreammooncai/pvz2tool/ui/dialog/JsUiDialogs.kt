package io.github.dreammooncai.pvz2tool.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.view.PvzGreenButton
import io.github.dreammooncai.pvz2tool.view.PvzProgressBar
import io.github.dreammooncai.pvz2tool.view.PvzRedButton
import io.github.dreammooncai.pvz2tool.view.PvzRichText
import io.github.dreammooncai.pvz2tool.view.PvzTextOliveStyle
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ======================== UI 状态类 ========================

/** 确认弹窗状态 */
data class JsConfirmState(
    val isVisible: Boolean = false,
    val title: String = "",
    val message: String = "",
    val deferred: CompletableDeferred<Boolean>? = null
)

/** 输入弹窗状态 */
data class JsPromptState(
    val isVisible: Boolean = false,
    val title: String = "",
    val message: String = "",
    val defaultValue: String = "",
    val deferred: CompletableDeferred<String?>? = null
)

/** 进度弹窗状态 */
data class JsProgressState(
    val isVisible: Boolean = false,
    val title: String = "",
    val message: String = "",
    val progress: Int = 0,
    val isIndeterminate: Boolean = false,
    val showCancel: Boolean = true
)

/** 解压根弹窗状态（复用 ExtractorUiState） */
typealias JsExtractorState = ExtractorUiState

/** 提示弹窗状态（单按钮弹窗） */
data class JsAlertState(
    val isVisible: Boolean = false,
    val title: String = "",
    val message: String = "",
    val deferred: CompletableDeferred<Unit>? = null
)

// ======================== JS UI 管理器 ========================

object JsUiManager {
    // 确认弹窗状态流
    private val _confirmState = MutableStateFlow(JsConfirmState())
    val confirmState: StateFlow<JsConfirmState> = _confirmState.asStateFlow()

    // 输入弹窗状态流
    private val _promptState = MutableStateFlow(JsPromptState())
    val promptState: StateFlow<JsPromptState> = _promptState.asStateFlow()

    // 进度弹窗状态流
    private val _progressState = MutableStateFlow(JsProgressState())
    val progressState: StateFlow<JsProgressState> = _progressState.asStateFlow()

    // 解压根弹窗状态流
    private val _extractorState = MutableStateFlow(ExtractorUiState())
    val extractorState: StateFlow<ExtractorUiState> = _extractorState.asStateFlow()

    // 提示弹窗状态流
    private val _alertState = MutableStateFlow(JsAlertState())
    val alertState: StateFlow<JsAlertState> = _alertState.asStateFlow()

    // 解压器实例（由 JS 调用时创建）
    private var extractorHolder: AssetExtractorHolder? = null
    private val extractorScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** 显示确认弹窗，返回 CompletableDeferred<Boolean> */
    fun showConfirm(title: String, message: String): CompletableDeferred<Boolean> {
        val deferred = CompletableDeferred<Boolean>()
        _confirmState.value = JsConfirmState(
            isVisible = true,
            title = title,
            message = message,
            deferred = deferred
        )
        return deferred
    }

    /** 隐藏确认弹窗 */
    fun hideConfirm() {
        _confirmState.value = JsConfirmState()
    }

    /** 显示提示弹窗（单按钮），返回 CompletableDeferred<Unit> */
    fun showAlert(title: String, message: String): CompletableDeferred<Unit> {
        val deferred = CompletableDeferred<Unit>()
        _alertState.value = JsAlertState(
            isVisible = true,
            title = title,
            message = message,
            deferred = deferred
        )
        return deferred
    }

    /** 隐藏提示弹窗 */
    fun hideAlert() {
        _alertState.value = JsAlertState()
    }

    /** 显示输入弹窗，返回 CompletableDeferred<String?> */
    fun showPrompt(title: String, message: String, defaultValue: String = ""): CompletableDeferred<String?> {
        val deferred = CompletableDeferred<String?>()
        _promptState.value = JsPromptState(
            isVisible = true,
            title = title,
            message = message,
            defaultValue = defaultValue,
            deferred = deferred
        )
        return deferred
    }

    /** 隐藏输入弹窗 */
    fun hidePrompt() {
        _promptState.value = JsPromptState()
    }

    /** 显示进度弹窗 */
    fun showProgress(
        title: String,
        message: String = "",
        isIndeterminate: Boolean = false,
        showCancel: Boolean = false
    ) {
        _progressState.value = JsProgressState(
            isVisible = true,
            title = title,
            message = message,
            progress = 0,
            isIndeterminate = isIndeterminate,
            showCancel = showCancel
        )
    }

    /** 关闭进度弹窗 */
    fun closeProgress() {
        _progressState.value = _progressState.value.copy(isVisible = false)
    }

    /** 更新进度弹窗 */
    fun updateProgress(message: String? = null, progress: Float? = null) {
        _progressState.value = _progressState.value.copy(
            message = message ?: _progressState.value.message,
            progress = progress?.let {
                (if (progress > 1f)
                    progress
                else (it * 100)).toInt().coerceIn(0, 100)
            } ?: _progressState.value.progress
        )
    }

    // ======================== 解压相关方法 ========================

    /**
     * 解压资源到目标目录
     * @param sourcePaths 资源路径列表
     * @param targetDir 目标目录（文件路径）
     * @param sectionName 功能栏名称（用于 UI 显示）
     * @return CompletableDeferred<ExtractorUiState>
     */
    fun extract(
        sourcePaths: List<String>,
        targetDir: String,
        sectionName: String = ""
    ): CompletableDeferred<ExtractorUiState> {
        val deferred = CompletableDeferred<ExtractorUiState>()

        // 创建或重用 extractor
        val holder = extractorHolder ?: AssetExtractorHolder(
            AssetResourceExtractor(
                context = InitializePvz2.context,
                scope = extractorScope
            )
        ).also { extractorHolder = it }

        // 监听 extractor 状态
        holder.extractor.uiState.value = holder.extractor.uiState.value.copy(isVisible = true)

        holder.setOnCompleteListener { state ->
            _extractorState.value = state
            if (!deferred.isCompleted) {
                deferred.complete(state)
            }
        }
        holder.setOnDismissListener { state ->
            _extractorState.value = state
            if (!deferred.isCompleted) {
                deferred.complete(state)
            }
        }

        // 构建 ResourcePair 列表
        val resourcePairs = sourcePaths.mapNotNull { path ->
            try {
                val targetFile = java.io.File(targetDir)
                AssetExtractorHolder.resource(path, targetFile, sectionName = sectionName)
            } catch (e: Exception) {
                null
            }
        }

        if (resourcePairs.isNotEmpty()) {
            holder.extract(*resourcePairs.toTypedArray())
        } else {
            val emptyState = ExtractorUiState(isVisible = false, isComplete = true)
            _extractorState.value = emptyState
            if (!deferred.isCompleted) {
                deferred.complete(emptyState)
            }
        }

        return deferred
    }

    /**
     * 获取解压 UI 状态
     */
    fun getExtractorState(): ExtractorUiState = _extractorState.value

    /**
     * 关闭解压弹窗
     */
    fun closeExtractor() {
        extractorHolder?.extractor?.dismiss()
        _extractorState.value = ExtractorUiState()
    }
}

// ======================== Compose 弹窗组件 ========================

/**
 * JS 提示弹窗（单按钮）
 * 用法: ui.alert("标题", "内容")
 */
@Composable
fun JsAlertDialog() {
    val state by JsUiManager.alertState.collectAsState()

    // 监听 deferred 完成状态，自动隐藏弹窗
    LaunchedEffect(state.deferred) {
        state.deferred?.let { deferred ->
            deferred.invokeOnCompletion {
                JsUiManager.hideAlert()
            }
        }
    }

    if (state.isVisible) {
        PvzStyledDialog(
            isVisible = true,
            titleText = state.title,
            onDismissRequest = {
                state.deferred?.complete(Unit)
            },
            dismissible = false,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            bottomContent = {
                Spacer(modifier = Modifier.height(16.dp))

                // 确认按钮
                PvzGreenButton(
                    text = "确定",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    onClick = {
                        state.deferred?.complete(Unit)
                    }
                )
            }
        ) {
            PvzRichText(
                state.message,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 10.dp),
                defaultStyle = PvzTextOliveStyle.copy(shadowColor = null)
            )
        }
    }
}

/**
 * JS 确认弹窗
 * 用法: ui.confirm("标题", "内容").then(ok => { ... })
 */
@Composable
fun JsConfirmDialog() {
    val state by JsUiManager.confirmState.collectAsState()

    // 监听 deferred 完成状态，自动隐藏弹窗
    LaunchedEffect(state.deferred) {
        state.deferred?.let { deferred ->
            deferred.invokeOnCompletion {
                JsUiManager.hideConfirm()
            }
        }
    }

    if (state.isVisible) {
        PvzStyledDialog(
            isVisible = true,
            titleText = state.title,
            onDismissRequest = {
                state.deferred?.complete(false)
            },
            dismissible = false,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            bottomContent = {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 取消按钮
                    PvzRedButton(
                        text = "取消",
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        onClick = {
                            state.deferred?.complete(false)
                        }
                    )
                    // 确认按钮
                    PvzGreenButton(
                        text = "确认",
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        onClick = {
                            state.deferred?.complete(true)
                        }
                    )
                }
            }
        ) {
            PvzRichText(
                state.message,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 10.dp),
                defaultStyle = PvzTextOliveStyle.copy(shadowColor = null)
            )
        }
    }
}

/**
 * JS 输入弹窗
 * 用法: ui.prompt("标题", "请输入内容", "默认值").then(value => { ... })
 */
@Composable
fun JsPromptDialog() {
    val state by JsUiManager.promptState.collectAsState()
    var inputValue by remember { mutableStateOf(state.defaultValue) }

    // 监听 deferred 完成状态，自动隐藏弹窗
    LaunchedEffect(state.deferred) {
        state.deferred?.let { deferred ->
            deferred.invokeOnCompletion {
                JsUiManager.hidePrompt()
            }
        }
    }

    // 当弹窗打开时，用 defaultValue 初始化
    LaunchedEffect(state.isVisible, state.defaultValue) {
        if (state.isVisible) {
            inputValue = state.defaultValue
        }
    }

    if (state.isVisible) {
        PvzStyledDialog(
            isVisible = true,
            titleText = state.title,
            onDismissRequest = {
                state.deferred?.complete(null)
            },
            dismissible = false,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            bottomContent = {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 取消按钮
                    PvzRedButton(
                        text = "取消",
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        onClick = {
                            state.deferred?.complete(null)
                        }
                    )
                    // 确认按钮
                    PvzGreenButton(
                        text = "确定",
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        onClick = {
                            state.deferred?.complete(inputValue)
                        }
                    )
                }
            }
        ) {
            // 提示文本
            if (state.message.isNotEmpty()) {
                PvzRichText(
                    state.message,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 10.dp),
                    defaultStyle = PvzTextOliveStyle.copy(shadowColor = null)
                )
            }

            // 输入框
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(
                        Brush.verticalGradient(colors = listOf(Color(0xFFFFFFFF), Color(0xFFF5F5F5))),
                        RoundedCornerShape(6.dp)
                    )
                    .border(2.dp, Color(0xFF8ED229), RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                BasicTextField(
                    value = inputValue,
                    onValueChange = { inputValue = it },
                    textStyle = TextStyle(
                        color = Color(0xFF423F00),
                        fontSize = 14.sp
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
    }
}

/**
 * JS 进度弹窗
 * 用法:
 *   const progress = ui.progress("正在处理...");
 *   progress.update("已完成 50%", 50);
 *   progress.close();
 */
@Composable
fun JsProgressDialog() {
    val state by JsUiManager.progressState.collectAsState()

    if (state.isVisible) {
        val progress = (state.progress / 100f).coerceIn(0f, 1f)
        PvzStyledDialog(
            isVisible = true,
            titleText = state.title,
            onDismissRequest = {
                JsUiManager.closeProgress()
            },
            dismissible = progress >= 1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            bottomContent = {
                Spacer(modifier = Modifier.height(10.dp))

                // 进度条
                if (state.isIndeterminate) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        color = Color(0xFF32CD32),
                        strokeWidth = 4.dp
                    )
                }
                PvzProgressBar(
                    progress = progress,
                    label = if (progress >= 1) "完成" else if (state.showCancel) "取消" else null,
                    modifier = Modifier.fillMaxWidth(),
                    onLabelClick = {
                        JsUiManager.closeProgress()
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        ) {
            // 进度文本
            if (state.message.isNotEmpty() || !state.isIndeterminate) {
                val displayText = if (state.isIndeterminate) {
                    state.message.ifEmpty { "处理中..." }
                } else {
                    "${state.message.ifEmpty { "处理中..." }} (${state.progress}%)"
                }
                PvzRichText(
                    displayText,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 10.dp),
                    defaultStyle = PvzTextOliveStyle.copy(shadowColor = null)
                )
            }
        }
    }
}

/**
 * JS 解压根弹窗
 * 用法:
 *   const result = ui.extract(["path1", "path2"], "/target/dir");
 *   console.log(result.isComplete);
 */
@Composable
fun JsExtractorDialog() {
    val state by JsUiManager.extractorState.collectAsState()

    if (state.isVisible) {
        PvzExtractorDialog(
            uiState = state,
            isShowNotUpdate = true,
            onDismissRequest = {
                JsUiManager.closeExtractor()
            }
        )
    }
}
