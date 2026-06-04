package io.github.dreammooncai.pvz2tool.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Pvz2Icon.HookSelect: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "HookSelect",
        defaultWidth = 46.dp,
        defaultHeight = 46.dp,
        viewportWidth = 46f,
        viewportHeight = 46f
    ).apply {
        // 修改点：将背景色从浅米色 0xFFD9D3B3 改为深绿色 0xFF258C23
        path(fill = SolidColor(Color(0xFF258C23))) {
            moveTo(2.5f, 0f)
            lineTo(43.5f, 0f)
            curveTo(44.167f, 0.833f, 44.833f, 1.667f, 45.5f, 2.5f)
            lineTo(45.5f, 43.5f)
            curveTo(44.833f, 44.167f, 44.167f, 44.833f, 43.5f, 45.5f)
            lineTo(2.5f, 45.5f)
            curveTo(1.833f, 44.833f, 1.167f, 44.167f, 0.5f, 43.5f)
            lineTo(0.5f, 2.5f)
            curveTo(1.167f, 1.833f, 1.833f, 1.167f, 2.5f, 0.5f)
            close()
        }
        path(fill = SolidColor(Color(0xFF258C23))) {
            moveTo(42.5f, 3.5f)
            curveTo(29.879f, 2.372f, 17.212f, 2.205f, 4.5f, 3f)
            curveTo(3.619f, 3.708f, 2.953f, 4.542f, 2.5f, 5.5f)
            curveTo(2.167f, 5.5f, 1.833f, 5.5f, 1.5f, 5.5f)
            curveTo(1.511f, 3.983f, 2.177f, 2.816f, 3.5f, 2f)
            curveTo(16.167f, 1.333f, 28.833f, 1.333f, 41.5f, 2f)
            curveTo(42.056f, 2.383f, 42.389f, 2.883f, 42.5f, 3.5f)
            close()
        }
        path(fill = SolidColor(Color(0xFF2BD42A))) {
            moveTo(42.5f, 3.5f)
            curveTo(42.5f, 6.167f, 42.5f, 8.833f, 42.5f, 11.5f)
            curveTo(41.167f, 11.5f, 39.833f, 11.5f, 38.5f, 11.5f)
            curveTo(36.84f, 9.752f, 34.84f, 9.252f, 32.5f, 10f)
            curveTo(27.667f, 14.833f, 22.833f, 19.667f, 18f, 24.5f)
            curveTo(14.063f, 17.896f, 10.229f, 17.896f, 6.5f, 24.5f)
            curveTo(6.863f, 25.184f, 7.196f, 25.85f, 7.5f, 26.5f)
            curveTo(8.15f, 28.435f, 8.483f, 30.435f, 8.5f, 32.5f)
            curveTo(6.5f, 32.5f, 4.5f, 32.5f, 2.5f, 32.5f)
            curveTo(2.5f, 23.5f, 2.5f, 14.5f, 2.5f, 5.5f)
            curveTo(2.953f, 4.542f, 3.619f, 3.708f, 4.5f, 3f)
            curveTo(17.212f, 2.205f, 29.879f, 2.372f, 42.5f, 3.5f)
            close()
        }
        path(fill = SolidColor(Color(0xFF13B012))) {
            moveTo(42.5f, 3.5f)
            curveTo(43.66f, 15.993f, 43.827f, 28.66f, 43f, 41.5f)
            curveTo(42.5f, 42f, 42f, 42.5f, 41.5f, 43f)
            curveTo(28.833f, 43.667f, 16.167f, 43.667f, 3.5f, 43f)
            curveTo(3f, 42.5f, 2.5f, 42f, 2f, 41.5f)
            curveTo(1.5f, 29.505f, 1.333f, 17.505f, 1.5f, 5.5f)
            curveTo(1.833f, 5.5f, 2.167f, 5.5f, 2.5f, 5.5f)
            curveTo(2.5f, 14.5f, 2.5f, 23.5f, 2.5f, 32.5f)
            curveTo(4.5f, 32.5f, 6.5f, 32.5f, 8.5f, 32.5f)
            curveTo(8.483f, 30.435f, 8.15f, 28.435f, 7.5f, 26.5f)
            curveTo(10.631f, 29.129f, 13.631f, 31.962f, 16.5f, 35f)
            curveTo(17.833f, 35.667f, 19.167f, 35.667f, 20.5f, 35f)
            curveTo(26.667f, 28.833f, 32.833f, 22.667f, 39f, 16.5f)
            curveTo(39.483f, 15.552f, 39.65f, 14.552f, 39.5f, 13.5f)
            curveTo(40.788f, 15.035f, 40.788f, 16.701f, 39.5f, 18.5f)
            curveTo(33.515f, 23.985f, 27.681f, 29.652f, 22f, 35.5f)
            curveTo(21.517f, 36.448f, 21.351f, 37.448f, 21.5f, 38.5f)
            curveTo(28.5f, 38.5f, 35.5f, 38.5f, 42.5f, 38.5f)
            curveTo(42.5f, 29.5f, 42.5f, 20.5f, 42.5f, 11.5f)
            curveTo(42.5f, 8.833f, 42.5f, 6.167f, 42.5f, 3.5f)
            close()
        }
        path(fill = SolidColor(Color(0xFF13BE13))) {
            moveTo(38.5f, 11.5f)
            curveTo(39.833f, 11.5f, 41.167f, 11.5f, 42.5f, 11.5f)
            curveTo(42.5f, 20.5f, 42.5f, 29.5f, 42.5f, 38.5f)
            curveTo(35.5f, 38.5f, 28.5f, 38.5f, 21.5f, 38.5f)
            curveTo(21.351f, 37.448f, 21.517f, 36.448f, 22f, 35.5f)
            curveTo(27.681f, 29.652f, 33.515f, 23.985f, 39.5f, 18.5f)
            curveTo(40.788f, 16.701f, 40.788f, 15.035f, 39.5f, 13.5f)
            curveTo(39.167f, 12.833f, 38.833f, 12.167f, 38.5f, 11.5f)
            close()
        }
        path(fill = SolidColor(Color(0xFFEBEFEA))) {
            moveTo(38.5f, 11.5f)
            curveTo(38.833f, 12.167f, 39.167f, 12.833f, 39.5f, 13.5f)
            curveTo(39.65f, 14.552f, 39.483f, 15.552f, 39f, 16.5f)
            curveTo(32.833f, 22.667f, 26.667f, 28.833f, 20.5f, 35f)
            curveTo(19.167f, 35.667f, 17.833f, 35.667f, 16.5f, 35f)
            curveTo(13.631f, 31.962f, 10.631f, 29.129f, 7.5f, 26.5f)
            curveTo(7.196f, 25.85f, 6.863f, 25.184f, 6.5f, 24.5f)
            curveTo(10.229f, 17.896f, 14.063f, 17.896f, 18f, 24.5f)
            curveTo(22.833f, 19.667f, 27.667f, 14.833f, 32.5f, 10f)
            curveTo(34.84f, 9.252f, 36.84f, 9.752f, 38.5f, 11.5f)
            close()
        }
    }.build()
}