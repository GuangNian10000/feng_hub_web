package com.example.guangnian.feature.console.utils

import androidx.compose.ui.geometry.Offset
import com.example.guangnian.feature.console.data.bean.MapConfigBean
import com.example.guangnian.feature.console.data.bean.Point2DBean
import com.example.guangnian.feature.console.data.bean.RobotPositionBean
import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapTransformUtilsTest {

    @Test
    fun testWorldToPixelWithYFlip() {
        val config = MapConfigBean(
            resolution = 0.1,
            origin = listOf(0.0, 0.0, 0.0)
        )
        val mapHeightPx = 1000f
        
        // 世界坐标 (10, 10) -> 像素坐标:
        // px = (10 - 0) / 0.1 = 100
        // py = 1000 - (10 - 0) / 0.1 = 900
        val offset = MapTransformUtils.worldToPixel(10.0, 10.0, config, mapHeightPx)
        
        assertEquals(100f, offset.x)
        assertEquals(900f, offset.y)
    }

    @Test
    fun testPixelToWorldWithYFlip() {
        val config = MapConfigBean(
            resolution = 0.1,
            origin = listOf(0.0, 0.0, 0.0)
        )
        val mapHeightPx = 1000f
        
        // 像素坐标 (100, 900) -> 世界坐标 (10, 10)
        val worldPos = MapTransformUtils.pixelToWorld(Offset(100f, 900f), config, mapHeightPx)
        
        assertEquals(10.0, worldPos.x)
        assertEquals(10.0, worldPos.y)
    }
}
