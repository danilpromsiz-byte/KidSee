package com.kidsee.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidsee.core.ui.theme.KidSeeTypography
import com.kidsee.core.ui.theme.WazeBlue
import com.kidsee.core.ui.theme.WazeBlueDark
import com.kidsee.core.ui.theme.WazeBorder
import com.kidsee.core.ui.theme.WazeCardBg
import com.kidsee.core.ui.theme.WazeGreen
import com.kidsee.core.ui.theme.WazeGreenLight
import com.kidsee.core.ui.theme.WazeOrange
import com.kidsee.core.ui.theme.WazeOrangeLight
import com.kidsee.core.ui.theme.WazePurple
import com.kidsee.core.ui.theme.WazeRed
import com.kidsee.core.ui.theme.WazeRedLight
import com.kidsee.core.ui.theme.WazeTextPrimary
import com.kidsee.core.ui.theme.WazeTextSecondary

/**
 * Waze signature pill-shaped button with bouncy tactile feel.
 */
@Composable
fun KidSeePillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = WazeBlue,
    contentColor: Color = Color.White,
    icon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    height: Dp = 56.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "btn_scale"
    )

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(percent = 50),
        color = if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.5f),
        contentColor = contentColor,
        shadowElevation = if (isPressed) 2.dp else 6.dp,
        interactionSource = interactionSource,
        modifier = modifier
            .scale(scale)
            .height(height)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = KidSeeTypography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            )
        }
    }
}

/**
 * Waze-style rounded modular card with friendly shadow and border.
 */
@Composable
fun KidSeeCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = WazeCardBg,
    borderColor: Color = WazeBorder,
    cornerRadius: Dp = 24.dp,
    elevation: Dp = 4.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        shape = RoundedCornerShape(cornerRadius),
        color = backgroundColor,
        border = BorderStroke(1.5.dp, borderColor),
        shadowElevation = elevation
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

/**
 * Friendly KidSee Eye Mascot - Waze character style.
 * Expressive eye with cute wheels, smile, and bright blue shell.
 */
@Composable
fun KidSeeMascot(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    isHappy: Boolean = true,
    isWatching: Boolean = true,
    pupilOffsetX: Float = 0f,
    pupilOffsetY: Float = 0f
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Wheels at bottom (Waze car character DNA)
        val wheelRadius = w * 0.10f
        drawCircle(
            color = Color(0xFF22262A),
            radius = wheelRadius,
            center = Offset(w * 0.30f, h * 0.90f)
        )
        drawCircle(
            color = Color(0xFF22262A),
            radius = wheelRadius,
            center = Offset(w * 0.70f, h * 0.90f)
        )
        // Wheel rims
        drawCircle(
            color = Color.White,
            radius = wheelRadius * 0.45f,
            center = Offset(w * 0.30f, h * 0.90f)
        )
        drawCircle(
            color = Color.White,
            radius = wheelRadius * 0.45f,
            center = Offset(w * 0.70f, h * 0.90f)
        )

        // Main body (smooth friendly blob)
        drawRoundRect(
            color = WazeBlue,
            topLeft = Offset(w * 0.10f, h * 0.12f),
            size = Size(w * 0.80f, h * 0.72f),
            cornerRadius = CornerRadius(w * 0.36f, h * 0.36f)
        )

        // Eye background (Big friendly white ocular visor)
        val eyeWidth = w * 0.62f
        val eyeHeight = h * 0.46f
        val eyeLeft = w * 0.19f
        val eyeTop = h * 0.20f
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(eyeLeft, eyeTop),
            size = Size(eyeWidth, eyeHeight),
            cornerRadius = CornerRadius(eyeWidth * 0.42f, eyeHeight * 0.42f)
        )

        // Eyeball pupil
        val pupilRadius = eyeHeight * 0.28f
        val pupilCenterX = (eyeLeft + eyeWidth / 2f) + (pupilOffsetX * eyeWidth * 0.18f)
        val pupilCenterY = (eyeTop + eyeHeight / 2f) + (pupilOffsetY * eyeHeight * 0.18f)

        drawCircle(
            color = Color(0xFF16191D),
            radius = pupilRadius,
            center = Offset(pupilCenterX, pupilCenterY)
        )

        // Pupil highlight (sparkle of life)
        drawCircle(
            color = Color.White,
            radius = pupilRadius * 0.36f,
            center = Offset(pupilCenterX - pupilRadius * 0.32f, pupilCenterY - pupilRadius * 0.32f)
        )

        // Cute smile under eye
        if (isHappy) {
            drawArc(
                color = Color.White,
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(w * 0.36f, h * 0.66f),
                size = Size(w * 0.28f, h * 0.12f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.04f)
            )
        }
    }
}

/**
 * Pill status badge (Online, Streaming, Locked, Offline).
 */
@Composable
fun KidSeeStatusBadge(
    statusText: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(percent = 50),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = statusText,
                style = KidSeeTypography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            )
        }
    }
}
