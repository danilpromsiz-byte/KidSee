package com.kidsee.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kidsee.core.ui.theme.WazeBlue
import com.kidsee.core.ui.theme.WazeBorder
import com.kidsee.core.ui.theme.WazeCardBg

/**
 * Android TV / Remote control focus-aware card with glowing border and zoom scale.
 */
@Composable
fun TvFocusableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1.0f,
        label = "tv_card_scale"
    )

    val shape = RoundedCornerShape(cornerRadius)

    Surface(
        onClick = onClick,
        shape = shape,
        color = WazeCardBg,
        border = BorderStroke(
            width = if (isFocused) 3.5.dp else 1.5.dp,
            color = if (isFocused) WazeBlue else WazeBorder
        ),
        shadowElevation = if (isFocused) 12.dp else 4.dp,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(shape)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}
