package io.github.dreammooncai.pvz2tool

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import io.github.dreammooncai.pvz2tool.view.PvzTextGoldStyle
import io.github.dreammooncai.pvz2tool.view.PvzTextGrayStyle
import io.github.dreammooncai.pvz2tool.view.PvzTextGreenStyle
import io.github.dreammooncai.pvz2tool.view.PvzTextWhiteStyle

// 在 MainScreen.kt 或 Theme.kt 中

private val Pvz2Typography = Typography(
    // 将默认字体族设置为我们的全局字体
    displayLarge = Typography().displayLarge.copy(fontFamily = InitializePvz2.font),
    displayMedium = Typography().displayMedium.copy(fontFamily = InitializePvz2.font),
    displaySmall = Typography().displaySmall.copy(fontFamily = InitializePvz2.font),

    headlineLarge = Typography().headlineLarge.copy(fontFamily = InitializePvz2.font),
    headlineMedium = Typography().headlineMedium.copy(fontFamily = InitializePvz2.font),
    headlineSmall = Typography().headlineSmall.copy(fontFamily = InitializePvz2.font),

    titleLarge = Typography().titleLarge.copy(fontFamily = InitializePvz2.font),
    titleMedium = Typography().titleMedium.copy(fontFamily = InitializePvz2.font),
    titleSmall = Typography().titleSmall.copy(fontFamily = InitializePvz2.font),

    bodyLarge = Typography().bodyLarge.copy(fontFamily = InitializePvz2.font),
    bodyMedium = Typography().bodyMedium.copy(fontFamily = InitializePvz2.font), // 这是 Text() 的默认样式
    bodySmall = Typography().bodySmall.copy(fontFamily = InitializePvz2.font),

    labelLarge = Typography().labelLarge.copy(fontFamily = InitializePvz2.font), // Button 文字
    labelMedium = Typography().labelMedium.copy(fontFamily = InitializePvz2.font),
    labelSmall = Typography().labelSmall.copy(fontFamily = InitializePvz2.font),
)

@Composable
fun Pvz2ToolTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFFD84315),
            secondary = Color(0xFFA1887F)
        ),
        typography = Pvz2Typography, // 关键：应用我们的字体配置
        content = content
    )
}