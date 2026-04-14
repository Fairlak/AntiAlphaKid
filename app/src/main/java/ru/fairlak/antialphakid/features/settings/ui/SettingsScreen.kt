package ru.fairlak.antialphakid.features.settings.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ru.fairlak.antialphakid.features.effects.crtEffect
import ru.fairlak.antialphakid.features.settings.viewmodel.SettingsViewModel


private val TerminalBackground @Composable get() = MaterialTheme.colorScheme.background
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
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
                            fontWeight = FontWeight.Bold,
                            style = TextStyle(
                                fontSize = 25.sp,
                                shadow = Shadow(color = activeColor, blurRadius = 15f)
                            )
                        )
                    },
                    navigationIcon = {
                        var lastClickTime by remember { mutableLongStateOf(0L) }
                        Text(
                            text = "[ BACK ]",
                            color = activeColor,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            style = TextStyle(
                                shadow = Shadow(color = activeColor, blurRadius = 15f)
                            ),
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 1.dp)
                        .terminalGlow(activeColor, blurRadius = 10f)
                ) {
                    HorizontalDivider(thickness = 1.dp, color = activeColor)
                }
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
                isValidationEnabled = true,
                onValidate = { input -> viewModel.checkPassword(input) },
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
                isValidationEnabled = true,
                onValidate = { input -> viewModel.checkPassword(input) },
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
                isValidationEnabled = false,
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
            .padding(4.dp)
            .terminalGlow(activeColor)
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
                modifier = Modifier.weight(1f),
                style = TextStyle(
                    shadow = Shadow(color = activeColor, blurRadius = 15f)
                )
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
            .padding(4.dp)
            .terminalGlow(activeColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        shape = RectangleShape,
        border = BorderStroke(1.dp, activeColor),
        colors = CardDefaults.cardColors(containerColor = TerminalBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "> THEME_ENGINE",
                color = activeColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                style = TextStyle(
                    fontSize = 18.sp,
                    shadow = Shadow(color = activeColor, blurRadius = 15f)
                )
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "MODE 'ON'  COLOR: [ < $onColorKey > ]",
                color = activeColor,
                fontFamily = FontFamily.Monospace,
                style = TextStyle(
                    fontSize = 17.sp,
                    shadow = Shadow(color = activeColor, blurRadius = 15f)
                )
            )
            Spacer(modifier = Modifier.height(15.dp))
            Text(
                text = "MODE 'OFF' COLOR: [ < $offColorKey > ]",
                color = activeColor,
                fontFamily = FontFamily.Monospace,
                style = TextStyle(
                    fontSize = 17.sp,
                    shadow = Shadow(color = activeColor, blurRadius = 15f)
                )
            )
        }
    }
}


@Composable
fun PasswordInputDialog(
    title: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    activeColor: Color,
    isValidationEnabled: Boolean = false,
    onValidate: (String) -> Boolean = { true }
) {
    var text by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TerminalBackground,
        shape = RectangleShape,
        modifier = Modifier
            .crtEffect(activeColor)
            .border(1.dp, if (isError) Color.Red else activeColor)
            .terminalGlow(if (isError) Color.Red else activeColor),
        title = {
            Text(
                text = "> $title",
                color = if (isError) Color.Red else activeColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                style = TextStyle(
                    fontSize = 23.sp,
                    shadow = Shadow(color = if (isError) Color.Red else activeColor, blurRadius = 15f)
                )
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = text,
                    onValueChange = { input ->
                        val filtered = input.filter { !it.isWhitespace() }

                        if (filtered.length <= 16) {
                            text = filtered
                            if (isError) isError = false
                        }
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = TerminalBackground,
                        unfocusedContainerColor = TerminalBackground,
                        focusedTextColor = if (isError) Color.Red else activeColor,
                        unfocusedTextColor = if (isError) Color.Red else activeColor,
                        unfocusedIndicatorColor = if (isError) Color.Red else activeColor,
                        cursorColor = if (isError) Color.Red else activeColor,
                        focusedIndicatorColor = if (isError) Color.Red else activeColor,
                        selectionColors = TextSelectionColors(
                            handleColor = if (isError) Color.Red else activeColor,
                            backgroundColor = (if (isError) Color.Red else activeColor).copy(alpha = 0.3f)
                        )
                    ),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        shadow = Shadow(color = if (isError) Color.Red else activeColor, blurRadius = 15f)
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isError) {
                    Text(
                        text = "!! ACCESS_DENIED: INVALID_KEY !!",
                        color = Color.Red,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .align(Alignment.CenterHorizontally),
                        style = TextStyle(
                            shadow = Shadow(color = Color.Red, blurRadius = 10f)
                        )
                    )
                }
            }
        },
        confirmButton = {
            Text(
                text = "[ CONFIRM ]",
                color = activeColor,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (text.isNotBlank()) {
                            if (isValidationEnabled) {
                                if (onValidate(text)) {
                                    onConfirm(text)
                                } else {
                                    isError = true
                                    text = ""
                                }
                            } else {
                                onConfirm(text)
                            }
                        }
                    }
                    .padding(8.dp),
                fontFamily = FontFamily.Monospace,
                style = TextStyle(
                    fontSize = 18.sp,
                    shadow = Shadow(color = if (isError) Color.Red else activeColor, blurRadius = 15f)
                )
            )
        },
        dismissButton = {
            Text(
                text = "[ CANCEL ]",
                color = activeColor,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onDismiss() }
                    .padding(8.dp),
                fontFamily = FontFamily.Monospace,
                style = TextStyle(
                    fontSize = 18.sp,
                    shadow = Shadow(color = activeColor, blurRadius = 15f)
                )
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
        modifier = Modifier
            .crtEffect(activeColor)
            .border(1.dp, activeColor)
            .terminalGlow(activeColor),
        title = {
            Text(
                text = "> $title",
                color = activeColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                style = TextStyle(
                    fontSize = 23.sp,
                    shadow = Shadow(color = activeColor, blurRadius = 15f)
                )
            )
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
                        unfocusedIndicatorColor = activeColor,
                        selectionColors = TextSelectionColors(
                            handleColor = activeColor,
                            backgroundColor = activeColor.copy(alpha = 0.3f)
                        )
                    ),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        shadow = Shadow(color = activeColor, blurRadius = 15f)
                    ),
                    singleLine = false,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "${text.length}/60",
                    color = activeColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    style = TextStyle(
                        shadow = Shadow(color = activeColor, blurRadius = 15f)
                    ),
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            Text(
                text = "[ CONFIRM ]",
                color = activeColor,
                style = TextStyle(
                    fontSize = 18.sp,
                    shadow = Shadow(color = activeColor, blurRadius = 15f)
                ),
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onConfirm(text) }
                    .padding(8.dp),
                fontFamily = FontFamily.Monospace)
        },
        dismissButton = {
            Text(
                text = "[ CANCEL ]",
                color = activeColor,
                style = TextStyle(
                    fontSize = 18.sp,
                    shadow = Shadow(color = activeColor, blurRadius = 15f)
                ),
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onDismiss() }
                    .padding(8.dp),
                fontFamily = FontFamily.Monospace)
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
    var isEditingOn by remember { mutableStateOf(true) }
    val colorOptions = listOf("GREEN", "RED", "BLUE", "AMBER", "WHITE", "VIOLET", "YELLOW")
    val infiniteTransition = rememberInfiniteTransition(label = "Blink")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    Box(
        modifier = Modifier
            .crtEffect(activeColor)
            .fillMaxWidth(0.95f)
            .border(1.dp, activeColor)
            .terminalGlow(activeColor)
            .background(TerminalBackground)
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "> THEME_PALETTE_SETUP",
                color = activeColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                style = TextStyle(
                    shadow = Shadow(activeColor, blurRadius = 10f)
                )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .terminalGlow(activeColor, blurRadius = 13f)
            ) {
                HorizontalDivider(thickness = 1.dp, color = activeColor)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isEditingOn = !isEditingOn }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = buildAnnotatedString {
                        append("> SYSTEM_MODE: ")
                        val stateText = if (isEditingOn) "ON" else "OFF"

                        withStyle(style = SpanStyle(color = activeColor.copy(alpha = alpha))) {
                            append(">> ")
                        }
                        withStyle(style = SpanStyle(color = activeColor)) {
                            append(stateText)
                        }
                        withStyle(style = SpanStyle(color = activeColor.copy(alpha = alpha))) {
                            append(" <<")
                        }
                    },
                    color = activeColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 18.sp,
                    style = TextStyle(shadow = Shadow(activeColor, blurRadius = 15f))
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isEditingOn) "[ SELECT_COLOR_FOR_ACTIVE ]" else "[ SELECT_COLOR_FOR_INACTIVE ]",
                color = activeColor.copy(alpha = 0.7f),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = TextStyle(
                    shadow = Shadow(color = activeColor, blurRadius = 15f)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            colorOptions.forEach { key ->
                val isActive = if (isEditingOn) selectedOn == key else selectedOff == key
                Text(
                    text = if (isActive) "> $key (Selected)" else "  $key",
                    color = if (isActive) activeColor else activeColor.copy(alpha = 0.4f),
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (isEditingOn) selectedOn = key else selectedOff = key
                        }
                        .padding(vertical = 6.dp),
                    style = TextStyle(
                        fontSize = 17.sp,
                        shadow = if (isActive) Shadow(activeColor, blurRadius = 10f) else null
                    )
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .terminalGlow(activeColor, blurRadius = 13f)
            ) {
                HorizontalDivider(thickness = 1.dp, color = activeColor)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSave(selectedOn, selectedOff) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "[ SAVE_AND_EXIT ]",
                    color = activeColor,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    style = TextStyle(shadow = Shadow(activeColor, blurRadius = 15f))
                )
            }
        }
    }
}


fun Modifier.terminalGlow(color: Color, blurRadius: Float = 15f) = this.drawBehind {
    val paint = Paint().asFrameworkPaint().apply {
        this.color = Color.Transparent.toArgb()
        setShadowLayer(blurRadius, 0f, 0f, color.toArgb())
    }
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)
    }
}
fun Modifier.terminalOutlineGlow(
    color: Color,
    radius: Dp = 10.dp,
    strokeWidth: Dp = 1.dp,
    isFocused: Boolean = false
) = this.drawBehind {
    val radiusPx = radius.toPx()
    val strokePx = strokeWidth.toPx()

    val alpha = if (isFocused) 1f else 0.2f
    val effectiveColor = color.copy(alpha = alpha)
    val blurRadius = if (isFocused) radiusPx else radiusPx * 0.3f

    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            this.color = effectiveColor.toArgb()
            this.style = android.graphics.Paint.Style.STROKE
            this.strokeWidth = strokePx
            this.isAntiAlias = true
            this.maskFilter = android.graphics.BlurMaskFilter(
                blurRadius.coerceAtLeast(1f),
                android.graphics.BlurMaskFilter.Blur.NORMAL
            )
        }

        canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)

        if (isFocused) {
            paint.maskFilter = null
            paint.strokeWidth = strokePx * 1.2f
            canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)
        }
    }
}