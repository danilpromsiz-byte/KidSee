package com.kidsee.app.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.kidsee.core.domain.model.UserRole
import com.kidsee.core.ui.R
import com.kidsee.core.ui.components.KidSeeCard
import com.kidsee.core.ui.components.KidSeeMascot
import com.kidsee.core.ui.components.KidSeePillButton
import com.kidsee.core.ui.theme.KidSeeTypography
import com.kidsee.core.ui.theme.WazeBlue
import com.kidsee.core.ui.theme.WazeBlueLight
import com.kidsee.core.ui.theme.WazeBorder
import com.kidsee.core.ui.theme.WazeCardBg
import com.kidsee.core.ui.theme.WazeGreen
import com.kidsee.core.ui.theme.WazeRed
import com.kidsee.core.ui.theme.WazeTeal
import com.kidsee.core.ui.theme.WazeTealLight
import com.kidsee.core.ui.theme.WazeTextPrimary
import com.kidsee.core.ui.theme.WazeTextSecondary
import com.kidsee.core.ui.theme.WazeWarmWhite

enum class OnboardingStep {
    LANGUAGE,
    ROLE,
    PARENT_AGE,
    PARENT_PIN
}

@Composable
fun OnboardingScreen(
    currentLanguage: String = "ru",
    onLanguageChanged: (String) -> Unit,
    onRoleSelected: (UserRole) -> Unit,
    onParentRegistered: (pin: String) -> Unit
) {
    var step by remember { mutableStateOf(OnboardingStep.LANGUAGE) }
    var selectedLang by remember { mutableStateOf(currentLanguage) }
    var selectedRole by remember { mutableStateOf<UserRole?>(null) }

    AnimatedContent(
        targetState = step,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "onboarding_steps"
    ) { currentStep ->
        when (currentStep) {
            OnboardingStep.LANGUAGE -> {
                LanguageSelectionStep(
                    selectedLang = selectedLang,
                    onLangSelect = { langCode ->
                        selectedLang = langCode
                        onLanguageChanged(langCode)
                    },
                    onContinue = {
                        step = OnboardingStep.ROLE
                    }
                )
            }
            OnboardingStep.ROLE -> {
                RoleSelectionStep(
                    selectedRole = selectedRole,
                    onRoleSelect = { selectedRole = it },
                    onBack = { step = OnboardingStep.LANGUAGE },
                    onContinue = {
                        if (selectedRole == UserRole.CHILD) {
                            onRoleSelected(UserRole.CHILD)
                        } else if (selectedRole == UserRole.PARENT) {
                            step = OnboardingStep.PARENT_AGE
                        }
                    }
                )
            }
            OnboardingStep.PARENT_AGE -> {
                ParentAgeVerificationStep(
                    onBack = { step = OnboardingStep.ROLE },
                    onVerifiedAdult = {
                        step = OnboardingStep.PARENT_PIN
                    },
                    onRedirectToChild = {
                        onRoleSelected(UserRole.CHILD)
                    }
                )
            }
            OnboardingStep.PARENT_PIN -> {
                ParentCreatePinStep(
                    onBack = { step = OnboardingStep.PARENT_AGE },
                    onPinCreated = { pin ->
                        onParentRegistered(pin)
                    }
                )
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 1: LANGUAGE SELECTION
// -------------------------------------------------------------
@Composable
private fun LanguageSelectionStep(
    selectedLang: String,
    onLangSelect: (String) -> Unit,
    onContinue: () -> Unit
) {
    val languages = listOf(
        Triple("ru", "Русский", "🇷🇺"),
        Triple("uk", "Українська", "🇺🇦"),
        Triple("en", "English", "🇬🇧"),
        Triple("es", "Español", "🇪🇸"),
        Triple("fr", "Français", "🇫🇷")
    )

    // Dynamic localized strings that update instantly upon language click
    val (titleText, subtitleText, continueButtonText) = when (selectedLang) {
        "uk" -> Triple("Оберіть мову", "Мова інтерфейсу додатка", "Продовжити")
        "en" -> Triple("Select Language", "Application interface language", "Continue")
        "es" -> Triple("Selecciona el idioma", "Idioma de la interfaz de la aplicación", "Continuar")
        "fr" -> Triple("Choisissez la langue", "Langue de l'interface de l'application", "Continuer")
        else -> Triple("Выберите язык", "Язык интерфейса приложения", "Продолжить")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WazeWarmWhite)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        KidSeeMascot(size = 96.dp)
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "KidSee",
            style = KidSeeTypography.displayLarge.copy(
                fontWeight = FontWeight.Black,
                color = WazeBlue,
                fontSize = 36.sp
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = titleText,
            style = KidSeeTypography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = WazeTextPrimary
            ),
            textAlign = TextAlign.Center
        )

        Text(
            text = subtitleText,
            style = KidSeeTypography.bodyMedium.copy(color = WazeTextSecondary),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            for ((code, name, flag) in languages) {
                val isSelected = selectedLang == code
                KidSeeCard(
                    cornerRadius = 20.dp,
                    backgroundColor = if (isSelected) WazeBlueLight else Color.White,
                    borderColor = if (isSelected) WazeBlue else WazeBorder,
                    elevation = if (isSelected) 6.dp else 2.dp,
                    onClick = { onLangSelect(code) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = flag, fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = name,
                                style = KidSeeTypography.titleMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) WazeBlue else WazeTextPrimary
                                )
                            )
                        }

                        if (isSelected) {
                            Surface(
                                shape = CircleShape,
                                color = WazeBlue,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        KidSeePillButton(
            text = continueButtonText,
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// -------------------------------------------------------------
// STEP 2: ROLE SELECTION
// -------------------------------------------------------------
@Composable
private fun RoleSelectionStep(
    selectedRole: UserRole?,
    onRoleSelect: (UserRole) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WazeWarmWhite)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = WazeTextPrimary
                )
            }
        }

        KidSeeMascot(size = 80.dp)
        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = stringResource(R.string.choose_mode_title),
            style = KidSeeTypography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = WazeTextPrimary
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.choose_mode_subtitle),
            style = KidSeeTypography.bodyMedium.copy(color = WazeTextSecondary),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Parent Role Card
        RoleCard(
            title = stringResource(R.string.mode_parent),
            description = stringResource(R.string.mode_parent_desc),
            icon = Icons.Default.SupervisorAccount,
            accentColor = WazeBlue,
            bgColor = if (selectedRole == UserRole.PARENT) WazeBlueLight else Color.White,
            isSelected = selectedRole == UserRole.PARENT,
            onClick = { onRoleSelect(UserRole.PARENT) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Child Role Card
        RoleCard(
            title = stringResource(R.string.mode_child),
            description = stringResource(R.string.mode_child_desc),
            icon = Icons.Default.ChildCare,
            accentColor = WazeTeal,
            bgColor = if (selectedRole == UserRole.CHILD) WazeTealLight else Color.White,
            isSelected = selectedRole == UserRole.CHILD,
            onClick = { onRoleSelect(UserRole.CHILD) }
        )

        Spacer(modifier = Modifier.height(32.dp))

        KidSeePillButton(
            text = stringResource(R.string.btn_continue),
            onClick = onContinue,
            enabled = selectedRole != null,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RoleCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    bgColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    KidSeeCard(
        backgroundColor = bgColor,
        borderColor = if (isSelected) accentColor else WazeBorder,
        elevation = if (isSelected) 6.dp else 2.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = accentColor.copy(alpha = 0.15f),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = KidSeeTypography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) accentColor else WazeTextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = KidSeeTypography.bodyMedium.copy(color = WazeTextSecondary, fontSize = 13.sp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 3: PARENT AGE VERIFICATION
// -------------------------------------------------------------
@Composable
private fun ParentAgeVerificationStep(
    onBack: () -> Unit,
    onVerifiedAdult: () -> Unit,
    onRedirectToChild: () -> Unit
) {
    var birthYear by remember { mutableIntStateOf(1990) }
    val currentYear = 2026
    val calculatedAge = currentYear - birthYear
    val isAdult = calculatedAge >= 18

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WazeWarmWhite)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = WazeTextPrimary
                )
            }
        }

        Surface(
            shape = CircleShape,
            color = WazeBlue.copy(alpha = 0.12f),
            modifier = Modifier.size(68.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Cake,
                    contentDescription = null,
                    tint = WazeBlue,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Проверка возраста родителя",
            style = KidSeeTypography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = WazeTextPrimary
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Укажите ваш год рождения. Режим родительского контроля доступен только совершеннолетним пользователям (18+).",
            style = KidSeeTypography.bodyMedium.copy(color = WazeTextSecondary),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Year Selector Card
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, WazeBorder),
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ГОД РОЖДЕНИЯ",
                    style = KidSeeTypography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = WazeTextSecondary,
                        letterSpacing = 1.sp
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = { if (birthYear > 1930) birthYear -= 1 },
                        modifier = Modifier
                            .size(46.dp)
                            .background(Color(0xFFF1EFEA), CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "-1 год", tint = WazeTextPrimary)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = "$birthYear",
                        style = KidSeeTypography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = WazeBlue,
                            fontSize = 38.sp
                        )
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    IconButton(
                        onClick = { if (birthYear < currentYear) birthYear += 1 },
                        modifier = Modifier
                            .size(46.dp)
                            .background(Color(0xFFF1EFEA), CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "+1 год", tint = WazeTextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Decade Selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1975, 1985, 1990, 1995, 2005).forEach { yr ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (birthYear == yr) WazeBlue else Color(0xFFF5F3ED),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { birthYear = yr }
                        ) {
                            Text(
                                text = "$yr",
                                style = KidSeeTypography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (birthYear == yr) Color.White else WazeTextPrimary
                                ),
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Age Verification Status Badge
        if (isAdult) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = WazeGreen.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = WazeGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Возраст подтверждён ($calculatedAge лет)",
                            style = KidSeeTypography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = WazeGreen
                            )
                        )
                        Text(
                            text = "Вы можете создать пароль и профиль родителя",
                            style = KidSeeTypography.bodySmall.copy(color = WazeTextSecondary)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            KidSeePillButton(
                text = "Продолжить",
                onClick = onVerifiedAdult,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = WazeRed.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Вам $calculatedAge лет — доступ ограничен",
                        style = KidSeeTypography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = WazeRed
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Режим родителя предназначен исключительно для совершеннолетних (18+). Для этого устройства выберите режим ребёнка.",
                        style = KidSeeTypography.bodySmall.copy(color = WazeTextPrimary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            KidSeePillButton(
                text = "Настроить как Ребёнок",
                onClick = onRedirectToChild,
                backgroundColor = WazeTeal,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// -------------------------------------------------------------
// STEP 4: CREATE PARENT MASTER PIN (CUSTOM, NOT HARDCODED)
// -------------------------------------------------------------
@Composable
private fun ParentCreatePinStep(
    onBack: () -> Unit,
    onPinCreated: (String) -> Unit
) {
    var firstPin by remember { mutableStateOf("") }
    var enteredPin by remember { mutableStateOf("") }
    var isConfirming by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }

    fun onDigit(digit: String) {
        if (enteredPin.length < 4) {
            isError = false
            val newPin = enteredPin + digit
            enteredPin = newPin
            if (newPin.length == 4) {
                if (!isConfirming) {
                    firstPin = newPin
                    enteredPin = ""
                    isConfirming = true
                } else {
                    if (newPin == firstPin) {
                        onPinCreated(newPin)
                    } else {
                        isError = true
                        enteredPin = ""
                        firstPin = ""
                        isConfirming = false
                    }
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
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (isConfirming) {
                    isConfirming = false
                    enteredPin = ""
                    firstPin = ""
                } else {
                    onBack()
                }
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = WazeTextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            shape = CircleShape,
            color = if (isError) WazeRed.copy(alpha = 0.15f) else WazeBlue.copy(alpha = 0.12f),
            modifier = Modifier.size(68.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (isError) WazeRed else WazeBlue,
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (!isConfirming) "Придумайте PIN-код" else "Подтвердите PIN-код",
            style = KidSeeTypography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = WazeTextPrimary
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (isError) {
                "PIN-коды не совпали! Придумайте PIN заново."
            } else if (!isConfirming) {
                "Задайте ваш собственный 4-значный PIN-код для защиты настроек от детей"
            } else {
                "Введите те же 4 цифры повторно для подтверждения"
            },
            style = KidSeeTypography.bodyMedium.copy(
                color = if (isError) WazeRed else WazeTextSecondary,
                fontSize = 14.sp
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

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

        Spacer(modifier = Modifier.height(36.dp))

        // Keypad
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("C", "0", "⌫")
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            for (row in keys) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (key in row) {
                        Surface(
                            shape = CircleShape,
                            color = WazeCardBg,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .size(68.dp)
                                .clickable {
                                    when (key) {
                                        "C" -> {
                                            enteredPin = ""
                                            isError = false
                                        }
                                        "⌫" -> onBackspace()
                                        else -> onDigit(key)
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (key == "⌫") {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = "Backspace",
                                        tint = WazeTextPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
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

        Spacer(modifier = Modifier.weight(1f))
    }
}
