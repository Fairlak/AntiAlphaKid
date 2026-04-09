package ru.fairlak.antialphakid.features.settings.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.fairlak.antialphakid.core.ui.theme.TerminalRed
import ru.fairlak.antialphakid.features.settings.viewmodel.SettingsViewModel

private val TerminalBackground @Composable get() = MaterialTheme.colorScheme.background
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onBack: () -> Unit,
) {
    LaunchedEffect(Unit) {
        viewModel.refreshState()
    }

    val isNotificationsEnabled by viewModel.isNotificEnabled.collectAsState()

    val hasPassword by viewModel.hasPassword.collectAsState()
    var showPassDialog by remember { mutableStateOf(false) }
    var isVerifyingOldPassword by remember { mutableStateOf(false) }
    var isVerifyingForDelete by remember { mutableStateOf(false) }
    val blockerText by viewModel.blockerText.collectAsState()
    var showBlockerTextDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    val activeColor by viewModel.activeColor.collectAsState()
    val onColorKey by viewModel.onColorKey.collectAsState()
    val offColorKey by viewModel.offColorKey.collectAsState()

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
                    label = if (hasPassword) "[ * CHANGE_PASSWORD ]" else "[ + ADD_PASSWORD ]",
                    activeColor = activeColor,
                    showTrailingIcon = hasPassword,
                    onTrailingIconClick = {
                        isVerifyingForDelete = true
                    },
                    onClick = {
                        if (hasPassword) isVerifyingOldPassword = true
                        else showPassDialog = true
                    }
                )
            }

            item {
                SettingsModuleItem(
                    label = "> BLOCKER_TEXT: $blockerText",
                    activeColor = activeColor,
                    onClick = { showBlockerTextDialog = true }
                )
            }

            item {
                SettingsModuleItem(
                    label = "> NOTIFICATIONS: ${if (isNotificationsEnabled) "ON" else "OFF"}",
                    activeColor = activeColor,
                    onClick = { viewModel.toggleNotifications() }
                )
            }

            item {
                ThemeEngineModule(
                    activeColor = activeColor,
                    onColorKey = onColorKey,
                    offColorKey = offColorKey,
                    onClick = { showThemeDialog = true }
                )
            }
        }
        if (isVerifyingOldPassword) {
            PasswordInputDialog(
                title = "ENTER OLD PASSWORD",
                onConfirm = { input ->
                    if (viewModel.checkPassword(input)) {
                        isVerifyingOldPassword = false
                        showPassDialog = true
                    }
                },
                onDismiss = { isVerifyingOldPassword = false },
                activeColor = activeColor
            )
        }
        if (isVerifyingForDelete) {
            PasswordInputDialog(
                title = "CONFIRM DELETE PASSWORD",
                onConfirm = { input ->
                    if (viewModel.checkPassword(input)) {
                        viewModel.clearPassword()
                        isVerifyingForDelete = false
                    }
                },
                onDismiss = { isVerifyingForDelete = false },
                activeColor = activeColor
            )
        }
        if (showPassDialog) {
            PasswordInputDialog(
                title = if (hasPassword) "ENTER NEW PASSWORD" else "SET PASSWORD",
                onConfirm = {
                    viewModel.savePassword(it)
                    showPassDialog = false
                },
                onDismiss = { showPassDialog = false },
                activeColor = activeColor
            )
        }

        if (showBlockerTextDialog) {
            TextInputDialog(
                title = "BLOCKER_TEXT",
                initialText = blockerText,
                onConfirm = { newText ->
                    viewModel.updateBlockerText(newText)
                    showBlockerTextDialog = false
                },
                onDismiss = { showBlockerTextDialog = false },
                activeColor = activeColor
            )
        }

        if (showThemeDialog) {
            Dialog(onDismissRequest = { showThemeDialog = false }) {
                ThemePaletteDialog(
                    currentOnKey = onColorKey,
                    currentOffKey = offColorKey,
                    activeColor = activeColor,
                    onSave = { newOn, newOff ->
                        viewModel.setOnColor(newOn)
                        viewModel.setOffColor(newOff)
                        showThemeDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsModuleItem(
    label: String,
    activeColor: Color,
    showTrailingIcon: Boolean = false,
    onTrailingIconClick: (() -> Unit)? = null,
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
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = activeColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            if (showTrailingIcon) {
                Icon(
                    painter = painterResource(id = ru.fairlak.antialphakid.R.drawable.ic_trash),
                    contentDescription = "Delete",
                    tint = activeColor,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onTrailingIconClick?.invoke() }
                )
            }
        }
    }
}

@Composable
fun ThemeEngineModule(
    activeColor: Color,
    onColorKey: String,
    offColorKey: String,
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
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("> THEME_ENGINE", color = activeColor, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text("MODE 'ON'  COLOR: [ < $onColorKey > ]", color = activeColor, fontFamily = FontFamily.Monospace)
            Text("MODE 'OFF' COLOR: [ < $offColorKey > ]", color = activeColor, fontFamily = FontFamily.Monospace)
        }
    }
}


@Composable
fun PasswordInputDialog(
    title: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    activeColor: Color
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TerminalBackground,
        shape = RectangleShape,
        modifier = Modifier.border(1.dp, activeColor),
        title = {
            Text(
                text = "> $title",
                color = activeColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = TerminalBackground,
                    unfocusedContainerColor = TerminalBackground,
                    focusedTextColor = activeColor,
                    unfocusedTextColor = activeColor,
                    unfocusedIndicatorColor = activeColor,
                    cursorColor = activeColor,
                    focusedIndicatorColor = activeColor
                ),
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 18.sp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Text(
                text = "[ CONFIRM ]",
                color = activeColor,
                modifier = Modifier
                    .clickable { onConfirm(text) }
                    .padding(8.dp),
                fontFamily = FontFamily.Monospace
            )
        },
        dismissButton = {
            Text(
                text = "[ CANCEL ]",
                color = activeColor,
                modifier = Modifier
                    .clickable { onDismiss() }
                    .padding(8.dp),
                fontFamily = FontFamily.Monospace
            )
        }
    )
}

@Composable
fun TextInputDialog(
    title: String,
    initialText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    activeColor: Color
) {
    var text by remember { mutableStateOf(initialText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TerminalBackground,
        shape = RectangleShape,
        modifier = Modifier.border(1.dp, activeColor),
        title = {
            Text(text = "> $title", color = activeColor, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                TextField(
                    value = text,
                    onValueChange = { if (it.length <= 60) text = it },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = TerminalBackground,
                        unfocusedContainerColor = TerminalBackground,
                        focusedTextColor = activeColor,
                        unfocusedTextColor = activeColor,
                        cursorColor = activeColor,
                        focusedIndicatorColor = activeColor,
                        unfocusedIndicatorColor = activeColor
                    ),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 18.sp),
                    singleLine = false,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "${text.length}/60",
                    color = activeColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            Text(text = "[ CONFIRM ]", color = activeColor, modifier = Modifier
                .clickable { onConfirm(text) }
                .padding(8.dp), fontFamily = FontFamily.Monospace)
        },
        dismissButton = {
            Text(text = "[ CANCEL ]", color = activeColor, modifier = Modifier
                .clickable { onDismiss() }
                .padding(8.dp), fontFamily = FontFamily.Monospace)
        }
    )
}

@Composable
fun ThemePaletteDialog(
    currentOnKey: String,
    currentOffKey: String,
    activeColor: Color,
    onSave: (String, String) -> Unit
) {
    var selectedOn by remember { mutableStateOf(currentOnKey) }
    var selectedOff by remember { mutableStateOf(currentOffKey) }
    val colorOptions = listOf("GREEN", "RED", "BLUE", "AMBER", "WHITE", "VIOLET", "YELLOW")

    Box(
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .border(1.dp, activeColor)
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "> THEME_PALETTE_SETUP",
                color = activeColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = activeColor)

            Text(text = "[ ACTIVE_SHIELD_COLOR ]", color = activeColor, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            colorOptions.forEach { key ->
                val isActive = selectedOn == key
                Text(
                    text = if (isActive) "> $key (Active)" else "  $key",
                    color = if (isActive) activeColor else activeColor.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedOn = key }
                        .padding(vertical = 4.dp)
                )
            }
            Spacer(Modifier.height(16.dp))

            Text(text = "[ INACTIVE_SYSTEM_COLOR ]", color = activeColor, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            colorOptions.forEach { key ->
                val isActive = selectedOff == key
                Text(
                    text = if (isActive) "> $key (Active)" else "  $key",
                    color = if (isActive) activeColor else activeColor.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedOff = key }
                        .padding(vertical = 4.dp)
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = activeColor)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSave(selectedOn, selectedOff) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "[ SAVE_AND_EXIT ]",
                    color = activeColor,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
    }
}