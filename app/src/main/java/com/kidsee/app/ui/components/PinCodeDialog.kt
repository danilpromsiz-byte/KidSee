package com.kidsee.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidsee.core.ui.R
import com.kidsee.core.ui.theme.KidSeeTypography
import com.kidsee.core.ui.theme.WazeBlue
import com.kidsee.core.ui.theme.WazeCardBg
import com.kidsee.core.ui.theme.WazeRed
import com.kidsee.core.ui.theme.WazeTextPrimary
import com.kidsee.core.ui.theme.WazeTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinCodeDialog(
    correctPin: String? = null,
    title: String = stringResource(R.string.enter_parent_pin_title),
    description: String = stringResource(R.string.enter_parent_pin_desc),
    canDismiss: Boolean = true,
    onSuccess: (String) -> Unit,
    onDismiss: () -> Unit = {}
) {
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    fun onDigitPress(digit: String) {
        if (enteredPin.length < 4) {
            isError = false
            val newPin = enteredPin + digit
            enteredPin = newPin
            if (newPin.length == 4) {
                if (correctPin == null || newPin == correctPin) {
                    onSuccess(newPin)
                } else {
                    isError = true
                    enteredPin = ""
                }
            }
        }
    }

    fun onBackspace() {
        if (enteredPin.isNotEmpty()) {
            isError = false
            enteredPin = enteredPin.dropLast(1)
        }
    }

    BasicAlertDialog(
        onDismissRequest = {
            if (canDismiss) onDismiss()
        }
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = WazeCardBg,
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isError) WazeRed.copy(alpha = 0.15f) else WazeBlue.copy(alpha = 0.15f),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (isError) WazeRed else WazeBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = title,
                    style = KidSeeTypography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = WazeTextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isError) stringResource(R.string.wrong_pin_error) else description,
                    style = KidSeeTypography.bodyMedium.copy(
                        color = if (isError) WazeRed else WazeTextSecondary,
                        fontSize = 13.sp
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 4 PIN Dot Indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { index ->
                        val isFilled = index < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isError -> WazeRed
                                        isFilled -> WazeBlue
                                        else -> Color(0xFFD6D3CC)
                                    }
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Keypad 1-9, 0, Backspace
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "⌫")
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    for (row in keys) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (key in row) {
                                KeypadButton(
                                    text = key,
                                    onClick = {
                                        when (key) {
                                            "C" -> {
                                                enteredPin = ""
                                                isError = false
                                            }
                                            "⌫" -> onBackspace()
                                            else -> onDigitPress(key)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                if (canDismiss) {
                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = onDismiss) {
                        Text(
                            text = stringResource(R.string.btn_cancel),
                            style = KidSeeTypography.bodyMedium.copy(
                                color = WazeTextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color(0xFFF3F1EA),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        modifier = Modifier
            .size(62.dp)
            .clip(CircleShape)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (text == "⌫") {
                Icon(
                    imageVector = Icons.Default.Backspace,
                    contentDescription = "Backspace",
                    tint = WazeTextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Text(
                    text = text,
                    style = KidSeeTypography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = WazeTextPrimary,
                        fontSize = 22.sp
                    )
                )
            }
        }
    }
}
