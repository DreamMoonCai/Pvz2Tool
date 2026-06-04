package io.github.dreammooncai.pvz2tool.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import io.github.dreammooncai.pvz2tool.ui.dialog.AssetExtractorHolder

@Composable
fun AsyncImageFromAssets(
    filePath: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String? = null,
) {
    AsyncImage(
        model = AssetExtractorHolder.open(filePath), // Assets 路径协议
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
        // 可选：配置占位图、错误图等
    )
}