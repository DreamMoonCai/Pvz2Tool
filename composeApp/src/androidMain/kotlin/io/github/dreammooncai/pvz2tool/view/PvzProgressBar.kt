package io.github.dreammooncai.pvz2tool.view

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.controller.SoundController
import io.github.dreammooncai.pvz2tool.icon.Pvz2Icon
import io.github.dreammooncai.pvz2tool.icon.progress.ProgressBarHaystack
import io.github.dreammooncai.pvz2tool.icon.progress.ProgressBarLawn
import io.github.dreammooncai.pvz2tool.icon.progress.ProgressBarMud
import io.github.dreammooncai.pvz2tool.icon.progress.ProgressBarSoilFragments
import io.github.dreammooncai.pvz2tool.icon.progress.ProgressBarSoilPile
import io.github.dreammooncai.pvz2tool.icon.progress.ProgressBarSpinner
import io.github.dreammooncai.pvz2tool.icon.progress.ProgressBarZombieHand
import io.github.dreammooncai.pvz2tool.ui.dialog.PvzStyledDialog
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun PvzProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    label: String? = null, // 这个就是你要显示的文字
    animated: Boolean = true,
    animationDuration: Int = 600,
    onLabelClick: (() -> Unit)? = null,
) {
    val targetProgress = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = if (animated) animationDuration else 0),
        label = "pvz_progress"
    )

    val density = LocalDensity.current

    // ═══════════════════════════════════════════════════════════
    // 僵尸手冲出动画配置
    // 素材尺寸：僵尸手 52x83, 小泥土碎 28x7, 大泥土碎 60x21
    // ═══════════════════════════════════════════════════════════
    val zombieAppearProgress = 0.85f      // 开始时机
    val dirtShakeStartProgress = 0.85f     // 小泥土碎抖动开始
    val dirtShakeEndProgress = 0.88f       // 小泥土碎抖动结束
    val bigDirtStartProgress = 0.86f       // 大泥土碎开始出现
    val bigDirtEndProgress = 0.92f         // 大泥土碎完全消失
    val zombieEmergeStartProgress = 0.88f  // 僵尸手开始冲出

    // 计算各阶段进度
    val zombieVisible = animatedProgress >= zombieAppearProgress
    val zombieOverallProgress = if (zombieVisible) {
        ((animatedProgress - zombieAppearProgress) / (1f - zombieAppearProgress)).coerceIn(0f, 1f)
    } else 0f

    // 小泥土碎抖动进度
    val dirtShakeCurrentProgress = when {
        animatedProgress < dirtShakeStartProgress -> 0f
        animatedProgress < dirtShakeEndProgress -> {
            ((animatedProgress - dirtShakeStartProgress) / (dirtShakeEndProgress - dirtShakeStartProgress)).coerceIn(0f, 1f)
        }
        else -> 1f
    }

    // 大泥土碎：从小到大再到消失
    val bigDirtCurrentProgress = when {
        animatedProgress < bigDirtStartProgress -> 0f
        animatedProgress < (bigDirtStartProgress + bigDirtEndProgress) / 2 -> {
            // 0 -> 1：从小到大
            ((animatedProgress - bigDirtStartProgress) / ((bigDirtStartProgress + bigDirtEndProgress) / 2 - bigDirtStartProgress))
        }
        else -> 1f
    }.coerceIn(0f, 1f)

    // 僵尸手冲出进度
    val zombieEmergeCurrentProgress = when {
        animatedProgress < zombieEmergeStartProgress -> 0f
        else -> ((animatedProgress - zombieEmergeStartProgress) / (1f - zombieEmergeStartProgress)).coerceIn(0f, 1f)
    }

    // 抖动效果：sin波动的偏移
    val shakeOffset = if (dirtShakeCurrentProgress in 0f..1f && bigDirtCurrentProgress < 0.5f) {
        val shakeIntensity = (1f - dirtShakeCurrentProgress) * 8f // 逐渐减弱
        sin(dirtShakeCurrentProgress * 30f) * shakeIntensity
    } else 0f

    // 菊花位置列表：每一项是 (相对位置, 垂直偏移)
    // 相对位置：0.0 = 最左边，1.0 = 最右边
    val flowerPositions = listOf(
        0.12f to 0.3f,
        0.28f to 0.5f,
        0.45f to 0.25f,
        0.62f to 0.4f,
        0.78f to 0.35f,
        0.92f to 0.5f,
    )
    // 菊花大小根据进度动态变化：进度越小越缩小，进度越大越接近原始大小
    val flowerMinScale = 0.4f  // 最小缩放
    val flowerMaxScale = 1.0f  // 最大缩放
    val flowerScale = flowerMinScale + (flowerMaxScale - flowerMinScale) * animatedProgress

    BoxWithConstraints(modifier = modifier) {
        val containerWidth = maxWidth
        val progressBarHeight = containerWidth * 0.18f
        val horizontalPadding = containerWidth * 0.06f
        val flowerDisplaySize = progressBarHeight * 0.35f * flowerScale

        val ballSize = progressBarHeight * 0.9f
        val halfBall = ballSize / 2

        val usableWidth = containerWidth - horizontalPadding * 2
        val startX = -halfBall + (horizontalPadding * 0.3f)
        val endX = usableWidth + (halfBall / 2)

        val ballOffsetX = with(density) {
            startX.toPx() + (endX.toPx() - startX.toPx()) * animatedProgress
        }
        val ballCenterX = with(density) {
            ballOffsetX + halfBall.toPx()
        }

        val scale = when {
            animatedProgress < 0.9f -> {
                val t = animatedProgress / 0.9f
                1f - (t * t) * 0.4f
            }
            else -> {
                val t = (animatedProgress - 0.9f) / 0.1f
                0.6f * (1f - t * t)
            }
        }.coerceAtLeast(0f)

        val rotation = when {
            animatedProgress < 0.9f -> {
                val t = animatedProgress / 0.9f
                t * 720f
            }
            else -> {
                val t = (animatedProgress - 0.9f) / 0.1f
                720f + t * t * 1800f
            }
        }

        Box(
            modifier = Modifier
                .padding(top = progressBarHeight * 0.32f)
                .fillMaxWidth()
                .height(progressBarHeight)
        ) {
            // 泥土
            Image(
                Pvz2Icon.ProgressBarMud,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .height(progressBarHeight * 0.9f)
                    .padding(horizontal = horizontalPadding)
            )

            // --------------------------
            // 【新增】泥土中心文字（完全居中）
            // --------------------------
            if (!label.isNullOrBlank()) {
                var isButtonPressed by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = horizontalPadding)
                        // 关键：按比例向下偏移，自适应所有大小
                        .offset(y = progressBarHeight * 0.08f),
                    contentAlignment = Alignment.Center
                ) {
                    PvzRichText(
                        text = label,
                        fontSize = (progressBarHeight.value * 0.28f).sp,
                        defaultStyle = if (isButtonPressed && onLabelClick != null) {
                            PvzTextGoldStyle // 悬停金色
                        } else {
                            PvzTextWhiteStyle
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .then(if (onLabelClick != null) Modifier.pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        // 1. 手指按下：改变状态并播放按下音效
                                        isButtonPressed = true
                                        SoundController.playSoundFromAssets(InitializePvz2.config.ui.sounds.switchClickPress)

                                        // 等待手指释放
                                        tryAwaitRelease()

                                        // 2. 手指释放：改变状态并播放释放音效
                                        isButtonPressed = false
                                        SoundController.playSoundFromAssets(InitializePvz2.config.ui.sounds.switchClickRelease)
                                    },
                                    onTap = { onLabelClick() }
                                )
                            } else Modifier)
                    )
                }
            }

            // 草坪
            Image(
                Pvz2Icon.ProgressBarLawn,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .height(progressBarHeight * 0.8f)
                    .offset(y = -(progressBarHeight * 0.25f))
                    .drawWithContent {
                        if (scale > 0) {
                            clipRect(
                                left = 0f,
                                top = 0f,
                                right = ballCenterX,
                                bottom = size.height
                            ) {
                                this@drawWithContent.drawContent()
                            }
                        } else {
                            this@drawWithContent.drawContent()
                        }
                    }
            )

            // ═══════════════════════════════════════════════════
            // 菊花层：跟随草坪一起显示，根据进度动态调整大小
            // 只显示在草垛中心点左边
            // ═══════════════════════════════════════════════════
            flowerPositions.forEach { (relativeX, relativeY) ->
                val flowerX = with(density) {
                    horizontalPadding.toPx() + usableWidth.toPx() * relativeX
                }
                val flowerY = with(density) {
                    (progressBarHeight * 0.8f).toPx() * (1f - relativeY) + (-(progressBarHeight * 0.25f)).toPx()
                }
                // 只在草垛左边的菊花才显示
                if (flowerX < ballCenterX) {
                    Image(
                        Pvz2Icon.ProgressBarSpinner,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(flowerDisplaySize)
                            .offset {
                                IntOffset(
                                    x = flowerX.roundToInt() - (flowerDisplaySize.toPx() / 2).roundToInt(),
                                    y = flowerY.roundToInt() - flowerDisplaySize.toPx().roundToInt()
                                )
                            }
                    )
                }
            }

            // 草垛
            Image(
                Pvz2Icon.ProgressBarHaystack,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(ballSize)
                    .offset(y = -(progressBarHeight * 0.4f))
                    .offset {
                        IntOffset(
                            x = ballOffsetX.roundToInt(),
                            y = 0
                        )
                    }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        rotationZ = rotation
                    }
            )

            if (zombieOverallProgress > 0f) {
                // 统一密度，减少重复计算
                val density = LocalDensity.current
                // 僵尸X坐标（原逻辑完全保留）
                val zombieX = with(density) {
                    horizontalPadding.toPx() + usableWidth.toPx() * zombieAppearProgress
                }

                if (zombieEmergeCurrentProgress > 0f) {
                    val zombieHeight = progressBarHeight * 0.7f
                    val zombieWidth = zombieHeight * (52f / 83f) // 原图比例

                    // 手的Y轴动画（原逻辑完全保留）
                    val finalY = -(progressBarHeight * 0.45f)
                    val startY = finalY + progressBarHeight * 0.6f
                    val currentY = startY + (finalY - startY) * zombieEmergeCurrentProgress
                    val handCurrentYPx = with(density) { currentY.toPx() }

                    // ==============================================
                    // 🔴 核心：精准计算 大泥土碎的底部坐标（裁剪边界）
                    // ==============================================
                    val dirtTopPx = with(density) { (progressBarHeight * 0.1f).toPx() } // 泥土顶部Y
                    val bigDirtHeightPx = with(density) { (progressBarHeight * 0.15f).toPx() } // 泥土高度
                    val dirtBottomPx = dirtTopPx + bigDirtHeightPx // 🔥 泥土底部（裁剪线，绝对精准）

                    Image(
                        Pvz2Icon.ProgressBarZombieHand,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(width = zombieWidth, height = zombieHeight)
                            .offset {
                                IntOffset(
                                    x = (zombieX + with(density) { (progressBarHeight * 0.1f).toPx() }).roundToInt(),
                                    y = handCurrentYPx.roundToInt()
                                )
                            }
                            // ==============================================
                            // ✅ 终极自适应裁剪：只显示泥土上方的手，下方全裁剪
                            // 原理：裁剪掉 泥土底部以下 的所有区域
                            // ==============================================
                            .drawWithContent {
                                // 相对于图片自身的裁剪顶部 = 泥土底部 - 图片当前的Y偏移
                                val clipTop = (dirtBottomPx - handCurrentYPx).coerceAtLeast(0f)
                                // 裁剪：top=裁剪线，bottom=图片底部，只保留上半部分
                                clipRect(
                                    top = clipTop,
                                    bottom = size.height,
                                    clipOp = ClipOp.Difference
                                ) {
                                    this@drawWithContent.drawContent()
                                }
                            }
                    )
                }

                // 小泥土碎（原逻辑完全保留）
                val showSmallDirt = dirtShakeCurrentProgress > 0f && bigDirtCurrentProgress < 0.3f
                if (showSmallDirt) {
                    val smallDirtScale = 1f + dirtShakeCurrentProgress * 0.3f
                    val zombieHeight = progressBarHeight * 0.1f
                    val zombieWidth = zombieHeight * (28f / 7f)
                    Image(
                        Pvz2Icon.ProgressBarSoilFragments,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(
                                width = zombieWidth * smallDirtScale,
                                height = zombieHeight * smallDirtScale
                            )
                            .offset {
                                IntOffset(
                                    x = (zombieX + shakeOffset).roundToInt(),
                                    y = with(density) { (progressBarHeight * 0.1f).toPx().roundToInt() }
                                )
                            }
                            .graphicsLayer {
                                alpha = 1f - dirtShakeCurrentProgress * 0.5f
                            }
                    )
                }


                val zombieBigDirtHeight = progressBarHeight * 0.18f
                val zombieBigDirtWidth = zombieBigDirtHeight * (60f / 21f) // 按原图比例
                // 大泥土碎：从小到大，覆盖小泥土碎，然后消失
                Image(
                    Pvz2Icon.ProgressBarSoilPile,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(
                            width = zombieBigDirtWidth * bigDirtCurrentProgress,
                            height = zombieBigDirtHeight * bigDirtCurrentProgress
                        )
                        .offset {
                            IntOffset(
                                x = zombieX.roundToInt(),
                                y = ((progressBarHeight * 0.1f).toPx()).roundToInt()
                            )
                        }
                        .graphicsLayer {
                            alpha = bigDirtCurrentProgress
                        }
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF2D5016, widthDp = 860)
@Composable
private fun PvzProgressBarPreview_Multiple() {
    Column(modifier = Modifier.padding(16.dp)) {
        PvzProgressBar(progress = 0.0f, label = "尚未开始", modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(20.dp))
        PvzProgressBar(progress = 0.3f, label = "第 3 关", modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(20.dp))
        PvzProgressBar(progress = 0.65f, label = "65%", modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(20.dp))
        PvzProgressBar(progress = 1.0f, label = "已完成", modifier = Modifier.fillMaxWidth())
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1B3A0A, widthDp = 860)
@Composable
fun PvzExtractorDialog() {
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { while (progress < 1.1f) { progress += 0.01f; delay(100) } }
    PvzStyledDialog(isVisible = true, titleText = "工具箱", {},modifier = Modifier.padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally, bottomContent = {
        Spacer(Modifier.height(15.dp))
        PvzProgressBar(progress = progress, label = "动画演示-$progress", modifier = Modifier.fillMaxWidth())
    }) {
        PvzRichText(text = "测试 (进度%)", defaultStyle = PvzTextStyle(Color(0xFF423F00)), fontSize = 16.sp, textAlign = TextAlign.Center)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1B3A0A, widthDp = 860)
@Composable
private fun PvzProgressBarPreview_Animated() {
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { while (progress < 1.1f) { progress += 0.01f; delay(100) } }
    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        PvzProgressBar(progress = progress, label = "动画演示", modifier = Modifier.fillMaxWidth())
    }
}