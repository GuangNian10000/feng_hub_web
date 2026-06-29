package com.example.guangnian.feature.console.ui.panel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.guangnian.feature.console.data.bean.MapConfigBean
import com.example.guangnian.feature.console.data.bean.Point2DBean
import kotlin.math.*

object MapUtils {
    fun worldToPixel(worldX: Double, worldY: Double, config: MapConfigBean, mapHeightPx: Float): Offset {
        val originX = if (config.origin.size >= 1) config.origin[0] else 0.0
        val originY = if (config.origin.size >= 2) config.origin[1] else 0.0
        val res = if (config.resolution > 0) config.resolution else 0.05
        val px = (worldX - originX) / res
        val py = mapHeightPx - (worldY - originY) / res
        return Offset(px.toFloat(), py.toFloat())
    }

    fun pixelToWorld(pixel: Offset, config: MapConfigBean, mapHeightPx: Float): Point2DBean {
        val res = if (config.resolution > 0) config.resolution else 0.05
        val originX = if (config.origin.size >= 1) config.origin[0] else 0.0
        val originY = if (config.origin.size >= 2) config.origin[1] else 0.0
        val worldX = pixel.x * res + originX
        val worldY = (mapHeightPx - pixel.y) * res + originY
        return Point2DBean(worldX, worldY)
    }

    fun DrawScope.drawPathLine(pathData: List<Point2DBean>, config: MapConfigBean, mapHeightPx: Float, color: Color, strokeWidth: Float) {
        if (pathData.size < 2) return
        val path = Path()
        pathData.forEachIndexed { index, point ->
            val px = worldToPixel(point.x, point.y, config, mapHeightPx)
            if (index == 0) path.moveTo(px.x, px.y) else path.lineTo(px.x, px.y)
        }
        drawPath(path = path, color = color, style = Stroke(width = strokeWidth))
    }

    fun DrawScope.drawDirectionArrow(center: Offset, length: Float, yaw: Double, color: Color, offset: Float = 0f) {
        val canvasYaw = -yaw
        val startX = center.x + offset * cos(canvasYaw).toFloat()
        val startY = center.y + offset * sin(canvasYaw).toFloat()
        val tipX = startX + length * cos(canvasYaw).toFloat()
        val tipY = startY + length * sin(canvasYaw).toFloat()
        val baseWidth = length * 0.6f
        val baseAngle1 = canvasYaw + PI / 2
        val baseAngle2 = canvasYaw - PI / 2
        val path = Path().apply {
            moveTo(tipX, tipY)
            lineTo(startX + baseWidth * cos(baseAngle1).toFloat(), startY + baseWidth * sin(baseAngle1).toFloat())
            lineTo(startX + baseWidth * cos(baseAngle2).toFloat(), startY + baseWidth * sin(baseAngle2).toFloat())
            close()
        }
        drawPath(path = path, color = color)
    }

    fun DrawScope.drawFallbackGrid() {
        val gridSize = 50f
        for (i in 0..(size.width / gridSize).toInt()) {
            drawLine(Color(0x22FFFFFF), Offset(i * gridSize, 0f), Offset(i * gridSize, size.height))
        }
        for (i in 0..(size.height / gridSize).toInt()) {
            drawLine(Color(0x22FFFFFF), Offset(0f, i * gridSize), Offset(size.width, i * gridSize))
        }
    }
}
