package io.github.dreammooncai.pvz2tool.ui.popup

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

// 定义弹窗路由
sealed interface PvzPopupRoute {
    val title: String
}

// 主弹窗
data class MainPopup(override val title: String) : PvzPopupRoute

// 子页面弹窗
data class SubPopup(override val title: String, val data: Any? = null) : PvzPopupRoute

@Composable
fun PvzPopupHost(
    startDestination: PvzPopupRoute,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    content: @Composable (
        currentRoute: PvzPopupRoute,
        navigator: PvzPopupNavigator
    ) -> Unit
) {
    // 1. 弹窗栈状态
    val backStack = remember { mutableStateListOf<PvzPopupRoute>().apply { add(startDestination) } }
    val currentRoute = backStack.last()

    // 2. 导航器接口
    val navigator = remember(backStack) {
        object : PvzPopupNavigator {
            override fun navigate(route: PvzPopupRoute) {
                backStack.add(route)
            }

            override fun pop() {
                if (backStack.size > 1) {
                    backStack.removeAt(backStack.lastIndex)
                } else {
                    onDismiss() // 如果是最后一页，关闭整个弹窗
                }
            }
        }
    }

    // 3. 动画容器
    AnimatedContent(
        targetState = currentRoute,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize(),
        transitionSpec = {
            // 判断是进入(Push)还是退出(Pop)
            val isPush = backStack.size > 1 && backStack.last() == targetState && backStack[backStack.lastIndex - 1] == initialState

            if (isPush) {
                // ================= 进入新页面 (Push) =================
                // 新页面(target) 从 右边 滑入
                slideInHorizontally(animationSpec = tween(300)) { it } togetherWith
                        // 旧页面(initial) 向 左边 稍微移出 (视差效果)
                        slideOutHorizontally(animationSpec = tween(300)) { -it / 3 }
            } else {
                // ================= 返回上一页 (Pop) =================
                // 旧页面(target) 从 左边 稍微滑入
                slideInHorizontally(animationSpec = tween(300)) { -it / 3 } togetherWith
                        // 当前页面(initial) 向 右边 完全滑出 (修正这里)
                        slideOutHorizontally(animationSpec = tween(300)) { it }
            }
        },
        label = "PopupAnimation"
    ) { route ->
        Box(modifier = modifier) {
            content(route, navigator)
        }
    }
}

interface PvzPopupNavigator {
    fun navigate(route: PvzPopupRoute)
    fun pop()
}