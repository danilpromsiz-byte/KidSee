package com.kidsee.app.ui.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidsee.app.ui.components.PatternLockView
import com.kidsee.core.ui.R
import com.kidsee.core.ui.components.KidSeeMascot
import com.kidsee.core.ui.theme.KidSeeTypography
import com.kidsee.core.ui.theme.WazeBlue
import com.kidsee.core.ui.theme.WazeCardBg
import com.kidsee.core.ui.theme.WazeRed
import com.kidsee.core.ui.theme.WazeTextPrimary
import com.kidsee.core.ui.theme.WazeTextSecondary
import com.kidsee.core.ui.theme.WazeWarmWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ParentLockScreen(
    correctPin: String,
    correctPattern: String = "",
    lockType: String = "PIN",
    isBiometricAvailable: Boolean = false,
    isBiometricEnabled: Boolean = true,
    onBiometricRequest: () -> Unit = {},
    onUnlock: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var currentMode by remember(lockType) { mutableStateOf(lockType) }

    // Auto-prompt biometric dialog on screen entry if supported & enabled
    LaunchedEffect(isBiometricAvailable, isBiometricEnabled) {
        if (isBiometricAvailable && isBiometricEnabled) {
            delay(250) // Small delay to let Compose UI settle
            onBiometricRequest()
        }
    }

    fun onDigitPress(digit: String) {
        if (enteredPin.length < 4) {
            isError = false
            val newPin = enteredPin + digit
            enteredPin = newPin
            if (newPin.length == 4) {
                if (newPin == correctPin) {
                    onUnlock()
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WazeWarmWhite)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(0.2f))

        KidSeeMascot(size = 80.dp, isHappy = !isError)

        Spacer(modifier = Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = if (isError) WazeRed else WazeBlue,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = stringResource(R.string.parent_lock_title),
                style = KidSeeTypography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = WazeTextPrimary
                )
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = when {
                isError && currentMode == "PATTERN" -> stringResource(R.string.pattern_wrong_error)
                isError -> stringResource(R.string.wrong_pin_error)
                currentMode == "PATTERN" -> stringResource(R.string.pattern_draw_hint)
                else -> stringResource(R.string.enter_pin_to_access)
            },
            style = KidSeeTypography.bodyMedium.copy(
                color = if (isError) WazeRed else WazeTextSecondary,
                fontSize = 14.sp
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (currentMode == "PATTERN") {
            // Pattern Lock Mode
            PatternLockView(
                modifier = Modifier.size(280.dp),
                isError = isError,
                onPatternEntered = { pattern ->
                    val effectivePattern = if (correctPattern.isNotBlank()) correctPattern else "01258"
                    if (pattern == effectivePattern) {
                        onUnlock()
                    } else {
                        isError = true
                        scope.launch {
                            delay(900)
                            isError = false
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Biometric quick button in pattern mode
            if (isBiometricAvailable && isBiometricEnabled) {
                Surface(
                    onClick = onBiometricRequest,
                    shape = CircleShape,
                    color = WazeBlue.copy(alpha = 0.12f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = stringResource(R.string.biometric_settings_title),
                            tint = WazeBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        } else {
            // PIN Lock Mode
            // 4 PIN Dots
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

            Spacer(modifier = Modifier.height(24.dp))

            // Keypad
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf(if (isBiometricAvailable && isBiometricEnabled) "BIO" else "C", "0", "⌫")
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
                            Surface(
                                onClick = {
                                    when (key) {
                                        "BIO" -> onBiometricRequest()
                                        "C" -> {
                                            enteredPin = ""
                                            isError = false
                                        }
                                        "⌫" -> onBackspace()
                                        else -> onDigitPress(key)
                                    }
                                },
                                shape = CircleShape,
                                color = if (key == "BIO") WazeBlue.copy(alpha = 0.12f) else Color(0xFFF3F1EA),
                                shadowElevation = 0.dp,
                                tonalElevation = 0.dp,
                                modifier = Modifier
                                    .size(66.dp)
                                    .clip(CircleShape)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    when (key) {
                                        "⌫" -> {
                                            Icon(
                                                imageVector = Icons.Default.Backspace,
                                                contentDescription = null,
                                                tint = WazeTextPrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        "BIO" -> {
                                            Icon(
                                                imageVector = Icons.Default.Fingerprint,
                                                contentDescription = stringResource(R.string.biometric_settings_title),
                                                tint = WazeBlue,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                        else -> {
                                            Text(
                                                text = key,
                                                style = KidSeeTypography.titleLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = WazeTextPrimary,
                                                    fontSize = 24.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Switch lock mode button
        TextButton(
            onClick = {
                isError = false
                enteredPin = ""
                currentMode = if (currentMode == "PIN") "PATTERN" else "PIN"
            }
        ) {
            Text(
                text = stringResource(if (currentMode == "PIN") R.string.switch_to_pattern else R.string.switch_to_pin),
                color = WazeBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.weight(0.3f))
    }
}
