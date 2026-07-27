package com.example.guangnian.feature.console.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val LockOpenIcon: ImageVector
    get() = ImageVector.Builder(
        name = "LockOpen",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            // M18 8
            moveTo(18f, 8f)
            // h-1
            horizontalLineToRelative(-1f)
            // V6
            verticalLineTo(6f)
            // c0-2.76-2.24-5-5-5
            curveToRelative(0f, -2.76f, -2.24f, -5f, -5f, -5f)
            // S7 3.24 7 6
            reflectiveCurveTo(7f, 3.24f, 7f, 6f)
            // h2
            horizontalLineToRelative(2f)
            // c0-1.66 1.34-3 3-3
            curveToRelative(0f, -1.66f, 1.34f, -3f, 3f, -3f)
            // s3 1.34 3 3
            reflectiveCurveToRelative(3f, 1.34f, 3f, 3f)
            // v2
            verticalLineToRelative(2f)
            // H6
            horizontalLineTo(6f)
            // c-1.1 0-2 .9-2 2
            curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
            // v10
            verticalLineToRelative(10f)
            // c0 1.1.9 2 2 2
            curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
            // h12
            horizontalLineToRelative(12f)
            // c1.1 0 2-.9 2-2
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            // V10
            verticalLineTo(10f)
            // c0-1.1-.9-2-2-2
            curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
            // z
            close()
            // m0 12
            moveToRelative(0f, 12f)
            // H6
            horizontalLineTo(6f)
            // V10
            verticalLineTo(10f)
            // h12
            horizontalLineToRelative(12f)
            // v10
            verticalLineToRelative(10f)
            // z
            close()
            // m-6-3
            moveToRelative(-6f, -3f)
            // c1.1 0 2-.9 2-2
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            // s-.9-2-2-2
            reflectiveCurveToRelative(-0.9f, -2f, -2f, -2f)
            // -2 .9-2 2
            reflectiveCurveToRelative(-2f, 0.9f, -2f, 2f)
            // .9 2 2 2
            reflectiveCurveToRelative(0.9f, 2f, 2f, 2f)
            // z
            close()
        }
    }.build()

val LockIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Lock",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            // M18 8
            moveTo(18f, 8f)
            // h-1
            horizontalLineToRelative(-1f)
            // V6
            verticalLineTo(6f)
            // c0-2.76-2.24-5-5-5
            curveToRelative(0f, -2.76f, -2.24f, -5f, -5f, -5f)
            // S7 3.24 7 6
            reflectiveCurveTo(7f, 3.24f, 7f, 6f)
            // v2
            verticalLineToRelative(2f)
            // H6
            horizontalLineTo(6f)
            // c-1.1 0-2 .9-2 2
            curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
            // v10
            verticalLineToRelative(10f)
            // c0 1.1.9 2 2 2
            curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
            // h12
            horizontalLineToRelative(12f)
            // c1.1 0 2-.9 2-2
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            // V10
            verticalLineTo(10f)
            // c0-1.1-.9-2-2-2
            curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
            // z
            close()
            // m-6 9
            moveToRelative(-6f, 9f)
            // c-1.1 0-2-.9-2-2
            curveToRelative(-1.1f, 0f, -2f, -0.9f, -2f, -2f)
            // s.9-2 2-2
            reflectiveCurveToRelative(0.9f, -2f, 2f, -2f)
            // 2 .9 2 2
            reflectiveCurveToRelative(2f, 0.9f, 2f, 2f)
            // -.9 2-2 2
            reflectiveCurveToRelative(-0.9f, 2f, -2f, 2f)
            // z
            close()
            // m3.1-9
            moveToRelative(3.1f, -9f)
            // H8.9
            horizontalLineTo(8.9f)
            // V6
            verticalLineTo(6f)
            // c0-1.71 1.39-3.1 3.1-3.1
            curveToRelative(0f, -1.71f, 1.39f, -3.1f, 3.1f, -3.1f)
            // 1.71 0 3.1 1.39 3.1 3.1
            curveToRelative(1.71f, 0f, 3.1f, 1.39f, 3.1f, 3.1f)
            // v2
            verticalLineToRelative(2f)
            // z
            close()
        }
    }.build()
