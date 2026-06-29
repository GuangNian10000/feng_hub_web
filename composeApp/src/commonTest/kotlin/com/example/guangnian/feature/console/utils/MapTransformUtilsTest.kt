package com.example.guangnian.feature.console.utils

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
    fun testCalculatePointCloudWorldCoords() {
        // Robot at (1, 1) facing 90 degrees (PI/2)
        val robot = RobotPositionBean(x = 1.0, y = 1.0, yaw = PI / 2)
        // Point at (1, 0) relative to robot (forward)
        val cloud = listOf(Point2DBean(1.0, 0.0))
        
        val worldCoords = MapTransformUtils.calculatePointCloudWorldCoords(cloud, robot)
        
        assertEquals(1, worldCoords.size)
        // Expected: (1 + 0, 1 + 1) = (1, 2)
        // x = 1 * cos(PI/2) - 0 * sin(PI/2) + 1 = 0 - 0 + 1 = 1
        // y = 1 * sin(PI/2) + 0 * cos(PI/2) + 1 = 1 + 0 + 1 = 2
        assertTrue(abs(worldCoords[0].x - 1.0) < 0.0001)
        assertTrue(abs(worldCoords[0].y - 2.0) < 0.0001)
    }

    @Test
    fun testWorldToPixel() {
        val config = MapConfigBean(
            resolution = 0.05,
            origin = listOf(-10.0, -10.0, 0.0)
        )
        
        val (px, py) = MapTransformUtils.worldToPixel(0.0, 0.0, config)
        
        // (0 - (-10)) / 0.05 = 200
        assertEquals(200f, px)
        assertEquals(200f, py)
    }

    @Test
    fun testPixelToWorld() {
        val config = MapConfigBean(
            resolution = 0.05,
            origin = listOf(-10.0, -10.0, 0.0)
        )
        
        val worldPos = MapTransformUtils.pixelToWorld(200f, 200f, config)
        
        // 200 * 0.05 + (-10) = 0
        assertEquals(0.0, worldPos.x)
        assertEquals(0.0, worldPos.y)
    }
}
