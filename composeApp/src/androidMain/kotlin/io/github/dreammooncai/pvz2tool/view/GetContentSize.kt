package io.github.dreammooncai.pvz2tool.view

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.DpSize

@Composable
fun GetContentSize(
    visible: Boolean = true, onSize: @Composable ((size: DpSize) -> Unit)? = null, content: @Composable (size: DpSize?) -> Unit
) {
    val contentSize = remember { mutableStateOf(DpSize.Zero) }

    if (contentSize.value != DpSize.Zero) {
        onSize?.invoke(contentSize.value)
        if (visible) content(contentSize.value)
        return
    }

    Box(
        modifier = Modifier
            .layout { measurable, constraints ->
            // 测量真实尺寸
            val placeable = measurable.measure(constraints)

            // 把尺寸记录下来（转换成 DpSize）
            contentSize.value = DpSize(
                width = placeable.width.toDp(),
                height = placeable.height.toDp()
            )

            // 返回 0x0 ⇒ 不影响布局
            layout(0, 0) {}
        }
    ) {
        content(null)
    }
    if (contentSize.value != DpSize.Zero) {
        onSize?.invoke(contentSize.value)
        if (visible) content(contentSize.value)
    }
}