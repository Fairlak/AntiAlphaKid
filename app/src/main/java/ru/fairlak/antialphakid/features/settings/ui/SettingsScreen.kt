package ru.fairlak.antialphakid.features.settings.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.fairlak.antialphakid.core.ui.theme.TerminalRed
import ru.fairlak.antialphakid.features.settings.viewmodel.SettingsViewModel

private val TerminalGreen @Composable get() = MaterialTheme.colorScheme.primary
private val TerminalBackground = Color.Black

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onBack: () -> Unit,
) {
    LaunchedEffect(Unit) {
        viewModel.refreshState()
    }

    val isSystemActive by viewModel.isSystemActive.collectAsState()
    val activeColor = if (isSystemActive) TerminalGreen else TerminalRed

    val isNotificationsEnabled by viewModel.isNotificEnabled.collectAsState()

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(TerminalBackground)) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "> SETTINGS",
                            color = activeColor,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        var lastClickTime by remember { mutableLongStateOf(0L) }
                        Text(
                            text = "[ BACK ]",
                            color = activeColor,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier
                                .padding(start = 16.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime - lastClickTime > 500) {
                                        onBack()
                                        lastClickTime = currentTime
                                    }
                                }
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = TerminalBackground,
                        titleContentColor = activeColor
                    )
                )
                HorizontalDivider(thickness = 1.dp, color = activeColor)
            }
        },
        containerColor = TerminalBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                SettingsModuleItem(
                    label = "[ + ADD_PASSWORD ]",
                    activeColor = activeColor,
                    onClick = {  }
                )
            }

            item {
                SettingsModuleItem(
                    label = "> BLOCKER_TEXT: Лимит исчерпан",
                    activeColor = activeColor,
                    onClick = {  }
                )
            }

            item {
                SettingsModuleItem(
                    label = "> NOTIFICATIONS: ${if (isNotificationsEnabled) "ON" else "OFF"}",
                    activeColor = activeColor,
                    onClick = { viewModel.toggleNotifications() }
                )
            }
        }
    }
}

@Composable
fun SettingsModuleItem(
    label: String,
    activeColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        shape = RectangleShape,
        border = BorderStroke(1.dp, activeColor),
        colors = CardDefaults.cardColors(containerColor = TerminalBackground)
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = label,
                color = activeColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}