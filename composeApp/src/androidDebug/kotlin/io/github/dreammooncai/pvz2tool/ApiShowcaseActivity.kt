package io.github.dreammooncai.pvz2tool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dreammooncai.pvz2tool.js.JsLogger
import io.github.dreammooncai.pvz2tool.ui.dialog.JsDialogsHost

/**
 * 演示状态：当前正在演示哪一步（由 [ApiShowcaseTest] 在主线程更新）。
 */
object ApiShowcaseState {
    var step: String by mutableStateOf("正在准备演示环境…")
}

/**
 * **演示用 Activity（仅存在于 androidTest，不打进正式包）**。
 *
 * 用途：让「运行一次测试」就能在模拟器屏幕上**肉眼看到 API 调用效果**——
 * - 上半屏：当前演示步骤 + 实时日志面板（与正式 `log` 面板同源，订阅 [JsLogger.logs]）；
 * - 下半屏：`JsDialogsHost()`，`video.play()` 弹出的全屏视频弹窗就挂在这里，
 *   因此视频是真的在屏幕上演、而不是只断言一个状态。
 *
 * 初始化与正式运行环境对齐：注入 `InitializePvz2.context` 并加载内置 `dream.yml`，
 * 这样 `video.play("video/opening.mp4")` 能走正常的资源层解析（APK assets 兜底）。
 *
 * 运行方式：`./gradlew :composeApp:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=io.github.dreammooncai.pvz2tool.ApiShowcaseTest`
 */
class ApiShowcaseActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        InitializePvz2.context = applicationContext
        // 演示需要资源层（video/opening.mp4 在 APK assets 下）能正常解析
        runCatching { InitializePvz2.initConfig() }
            .onFailure { ApiShowcaseState.step = "配置加载失败：${it.message}" }

        setContent {
            MaterialTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0E1116))
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Text(
                            text = "Pvz2Tool · JS API 调用演示",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = ApiShowcaseState.step,
                            color = Color(0xFF58D68D),
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "—— 实时日志（log API 写入的就是这份）——",
                            color = Color(0xFF8A9199),
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(6.dp))

                        val logs by JsLogger.logs.collectAsState()
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(logs) { entry ->
                                Text(
                                    text = "[${entry.timestamp}] ${entry.level.name} ${entry.message}",
                                    color = when (entry.level.name) {
                                        "ERROR" -> Color(0xFFE74C3C)
                                        "WARN" -> Color(0xFFF1C40F)
                                        "SUCCESS" -> Color(0xFF2ECC71)
                                        else -> Color(0xFFBDC3C7)
                                    },
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }
                        }
                    }

                    // 🔴 视频弹窗宿主：video.play() 的全屏播放器挂在这里，覆盖整屏
                    JsDialogsHost()
                }
            }
        }
    }
}
