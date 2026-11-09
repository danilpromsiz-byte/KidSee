package com.kidsee.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.kidsee.core.ui.theme.WazeBlue
import com.kidsee.core.ui.theme.WazeGreen
import com.kidsee.core.ui.theme.WazeRed
import kotlin.math.sqrt

/**
 * 3x3 Pattern Lock View for Jetpack Compose.
 * Nodes are indexed 0..8:
 *  0  1  2
 *  3  4  5
 *  6  7  8
 */
@Composable
fun PatternLockView(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    isSuccess: Boolean = false,
    onPatternEntered: (String) -> Unit
) {
    val selectedNodes = remember { mutableStateListOf<Int>() }
    var currentTouchPoint by remember { mutableStateOf<Offset?>(null) }

    val activeColor = when {
        isError -> WazeRed
        isSuccess -> WazeGreen
        else -> WazeBlue
    }

    val normalDotColor = Color(0xFFD6D3CC)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(enabled, isError) {
                    if (!enabled) return@pointerInput
                    detectDragGestures(
                        onDragStart = { offset ->
                            selectedNodes.clear()
                            currentTouchPoint = offset
                            findNodeAt(offset, size.width.toFloat(), size.height.toFloat())?.let { node ->
                                if (!selectedNodes.contains(node)) {
                                    selectedNodes.add(node)
                                }
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val pos = change.position
                            currentTouchPoint = pos
                            findNodeAt(pos, size.width.toFloat(), size.height.toFloat())?.let { node ->
                                if (!selectedNodes.contains(node)) {
                                    selectedNodes.add(node)
                                }
                            }
                        },
                        onDragEnd = {
                            currentTouchPoint = null
                            if (selectedNodes.isNotEmpty()) {
                                val pattern = selectedNodes.joinToString("")
                                onPatternEntered(pattern)
                            }
                        },
                        onDragCancel = {
                            currentTouchPoint = null
                            selectedNodes.clear()
                        }
                    )
                }
        ) {
            val width = size.width
            val height = size.height
            val cellWidth = width / 3f
            val cellHeight = height / 3f

            fun getNodeCenter(index: Int): Offset {
                val col = index % 3
                val row = index / 3
                return Offset(
                    x = col * cellWidth + cellWidth / 2f,
                    y = row * cellHeight + cellHeight / 2f
                )
            }

            // Draw connecting lines between selected nodes
            if (selectedNodes.size > 1) {
                for (i in 0 until selectedNodes.size - 1) {
                    val p1 = getNodeCenter(selectedNodes[i])
                    val p2 = getNodeCenter(selectedNodes[i + 1])
                    drawLine(
                        color = activeColor,
                        start = p1,
                        end = p2,
                        strokeWidth = 10.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // Draw line to current touch point
            if (selectedNodes.isNotEmpty() && currentTouchPoint != null) {
                val lastNodeCenter = getNodeCenter(selectedNodes.last())
                drawLine(
                    color = activeColor.copy(alpha = 0.7f),
                    start = lastNodeCenter,
                    end = currentTouchPoint!!,
                    strokeWidth = 10.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Draw all 9 nodes
            for (index in 0 until 9) {
                val center = getNodeCenter(index)
                val isSelected = selectedNodes.contains(index)

                if (isSelected) {
                    // Outer glow/ring
                    drawCircle(
                        color = activeColor.copy(alpha = 0.22f),
                        radius = 28.dp.toPx(),
                        center = center
                    )
                    drawCircle(
                        color = activeColor,
                        radius = 28.dp.toPx(),
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )
                    // Inner dot
                    drawCircle(
                        color = activeColor,
                        radius = 11.dp.toPx(),
                        center = center
                    )
                } else {
                    // Normal idle dot
                    drawCircle(
                        color = normalDotColor,
                        radius = 9.dp.toPx(),
                        center = center
                    )
                }
            }
        }
    }
}

private fun findNodeAt(point: Offset, width: Float, height: Float): Int? {
    val cellWidth = width / 3f
    val cellHeight = height / 3f
    val hitRadius = cellWidth * 0.42f // Generous hit radius for easy finger connection

    for (index in 0 until 9) {
        val col = index % 3
        val row = index / 3
        val centerX = col * cellWidth + cellWidth / 2f
        val centerY = row * cellHeight + cellHeight / 2f

        val dx = point.x - centerX
        val dy = point.y - centerY
        val dist = sqrt(dx * dx + dy * dy)
        if (dist <= hitRadius) {
            return index
        }
    }
    return null
}
