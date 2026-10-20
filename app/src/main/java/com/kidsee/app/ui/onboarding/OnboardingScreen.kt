package com.kidsee.app.ui.onboarding

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidsee.core.ui.R
import com.kidsee.core.domain.model.UserRole
import com.kidsee.core.ui.components.KidSeeCard
import com.kidsee.core.ui.components.KidSeeMascot
import com.kidsee.core.ui.components.KidSeePillButton
import com.kidsee.core.ui.theme.KidSeeTypography
import com.kidsee.core.ui.theme.WazeBlue
import com.kidsee.core.ui.theme.WazeBlueLight
import com.kidsee.core.ui.theme.WazeBorder
import com.kidsee.core.ui.theme.WazeTeal
import com.kidsee.core.ui.theme.WazeTealLight
import com.kidsee.core.ui.theme.WazeWarmWhite

@Composable
fun OnboardingScreen(
    onRoleSelected: (UserRole) -> Unit,
    onLanguageChanged: (String) -> Unit
) {
    var selectedRole by remember { mutableStateOf<UserRole?>(null) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WazeWarmWhite)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Language Switcher Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                onClick = { showLanguageDialog = true },
                shape = RoundedCornerShape(percent = 50),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, WazeBorder),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Language",
                        modifier = Modifier.size(18.dp),
                        tint = WazeBlue
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.select_language),
                        style = KidSeeTypography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // KidSee Waze-style Mascot
        KidSeeMascot(size = 110.dp)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.app_name),
            style = KidSeeTypography.displayLarge.copy(
                fontWeight = FontWeight.Black,
                color = WazeBlue,
                fontSize = 38.sp
            )
        )

        Text(
            text = stringResource(R.string.tagline),
            style = KidSeeTypography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.choose_mode_title),
            style = KidSeeTypography.titleLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Parent Role Card
        RoleSelectionCard(
            title = stringResource(R.string.mode_parent),
            description = stringResource(R.string.mode_parent_desc),
            icon = Icons.Default.SupervisorAccount,
            accentColor = WazeBlue,
            bgColor = if (selectedRole == UserRole.PARENT) WazeBlueLight else Color.White,
            isSelected = selectedRole == UserRole.PARENT,
            onClick = { selectedRole = UserRole.PARENT }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Child Role Card
        RoleSelectionCard(
            title = stringResource(R.string.mode_child),
            description = stringResource(R.string.mode_child_desc),
            icon = Icons.Default.ChildCare,
            accentColor = WazeTeal,
            bgColor = if (selectedRole == UserRole.CHILD) WazeTealLight else Color.White,
            isSelected = selectedRole == UserRole.CHILD,
            onClick = { selectedRole = UserRole.CHILD }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Continue Button
        KidSeePillButton(
            text = stringResource(R.string.btn_continue),
            onClick = { selectedRole?.let { onRoleSelected(it) } },
            enabled = selectedRole != null,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showLanguageDialog) {
        LanguageDialog(
            onDismiss = { showLanguageDialog = false },
            onSelect = { lang ->
                showLanguageDialog = false
                onLanguageChanged(lang)
            }
        )
    }
}

@Composable
private fun RoleSelectionCard(
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
        elevation = if (isSelected) 8.dp else 3.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = accentColor.copy(alpha = 0.15f),
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = KidSeeTypography.titleLarge.copy(
                        color = if (isSelected) accentColor else Color.Black
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = KidSeeTypography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun LanguageDialog(
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val languages = listOf(
        Pair("en", stringResource(R.string.lang_en)),
        Pair("fr", stringResource(R.string.lang_fr)),
        Pair("es", stringResource(R.string.lang_es)),
        Pair("ru", stringResource(R.string.lang_ru)),
        Pair("uk", stringResource(R.string.lang_uk))
    )

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.select_language),
                style = KidSeeTypography.titleLarge
            )
        },
        text = {
            Column {
                languages.forEach { (code, name) ->
                    Text(
                        text = name,
                        style = KidSeeTypography.bodyLarge.copy(fontSize = 17.sp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(code) }
                            .padding(vertical = 12.dp)
                    )
                }
            }
        },
        confirmButton = {}
    )
}
