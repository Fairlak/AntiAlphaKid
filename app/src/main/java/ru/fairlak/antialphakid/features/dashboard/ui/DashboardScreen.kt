package ru.fairlak.antialphakid.features.dashboard.ui

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.fairlak.antialphakid.core.database.AppUsageEntity
import ru.fairlak.antialphakid.features.dashboard.viewmodel.DashboardViewModel
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import ru.fairlak.antialphakid.core.ui.theme.TerminalRed
import ru.fairlak.antialphakid.features.settings.ui.terminalGlow
import ru.fairlak.antialphakid.features.settings.viewmodel.SettingsViewModel

private val TerminalGreen @Composable get() = MaterialTheme.colorScheme.primary
private val TerminalBackground @Composable get() = MaterialTheme.colorScheme.background

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    settingsViewModel: SettingsViewModel,
    onManagePermissions: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val activeColor by settingsViewModel.activeColor.collectAsState()
    val hasPassword by settingsViewModel.hasPassword.collectAsState()
    var isAuthenticated by rememberSaveable { mutableStateOf(false) }

    if (hasPassword && !isAuthenticated) {
        AppLockScreen(
            viewModel = settingsViewModel,
            onCorrectPassword = { isAuthenticated = true },
            activeColor = activeColor
        )
    } else {
        var isPermissionGranted by remember {
            mutableStateOf(hasUsageStatsPermission(context) && hasOverlayPermission(context))
        }

        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    isPermissionGranted =
                        hasUsageStatsPermission(context) && hasOverlayPermission(context)
                    viewModel.updateUsageStats()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        if (!isPermissionGranted) {
            PermissionRequiredScreen(onSafeClick = onManagePermissions)
        } else {

            val limits by viewModel.appLimits.collectAsState()
            val filteredApps by viewModel.filteredApps.collectAsState()
            val searchQuery by viewModel.searchQuery.collectAsState()
            val usageStats by viewModel.usageStats.collectAsState()
            val isSystemActive by settingsViewModel.isSystemActive.collectAsState()

            val showDialog = remember { mutableStateOf(false) }
            var editingEntity = remember { mutableStateOf<AppUsageEntity?>(null) }

            LaunchedEffect(Unit) {
                viewModel.updateUsageStats()
            }

            DisposableEffect(Unit) {
                viewModel.startStatsUpdates()
                onDispose {
                    viewModel.stopStatsUpdates()
                }
            }

            if (showDialog.value) {
                AppSelectionDialog(
                    installedApps = filteredApps,
                    onSearchChange = { viewModel.onSearchQueryChange(it) },
                    getAppName = { viewModel.getAppName(it) },
                    getAppIcon = { viewModel.getAppIcon(it) },
                    onAppSelected = { pkg ->
                        viewModel.saveLimit(pkg, 30)
                        showDialog.value = false
                        viewModel.onSearchQueryChange("")
                    },
                    onDismiss = {
                        showDialog.value = false
                        viewModel.onSearchQueryChange("")
                    },
                    activeColor = activeColor,

                )
            }

            editingEntity.value?.let { entity ->
                EditLimitDialog(
                    appName = viewModel.getAppName(entity.packageName),
                    currentLimit = entity.limitMinutes,
                    onConfirm = { newMinutes ->
                        viewModel.saveLimit(entity.packageName, newMinutes)
                        editingEntity.value = null
                    },
                    onDismiss = { editingEntity.value = null },
                    activeColor = activeColor
                )
            }


            DashboardContent(
                limits = limits,
                getAppName = { viewModel.getAppName(it) },
                usageStats = usageStats,
                getAppIcon = { viewModel.getAppIcon(it) },
                onDelete = { viewModel.removeLimit(it) },
                onAddClick = { showDialog.value = true },
                onItemClick = { editingEntity.value = it },
                isSystemActive = isSystemActive,
                onToggleSystem = { settingsViewModel.toggleSystemState() },
                activeColor = activeColor,
                onOpenSettings = onOpenSettings
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    limits: List<AppUsageEntity>,
    getAppName: (String) -> String,
    getAppIcon: (String) -> Drawable?,
    usageStats: Map<String, Long>?,
    onDelete: (String) -> Unit,
    onAddClick: () -> Unit,
    onItemClick: (AppUsageEntity) -> Unit,
    isSystemActive: Boolean,
    onToggleSystem: () -> Unit,
    activeColor: Color,
    onOpenSettings: () -> Unit
) {

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
    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "> Anti Alpha",
                            color = activeColor,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            style = TextStyle(
                                fontSize = 25.sp,
                                shadow = Shadow(color = activeColor, blurRadius = 15f)
                            )
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = TerminalBackground,
                        titleContentColor = activeColor
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "[ SETTINGS ]",
                        color = activeColor,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(
                            fontSize = 18.sp,
                            shadow = Shadow(color = activeColor, blurRadius = 15f)
                        ),
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onOpenSettings()
                        }
                    )

                    Text(
                        text = buildAnnotatedString {
                            append("> SYSTEM_MODE: ")
                            val stateText = if (isSystemActive) "ON" else "OFF"

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
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(
                            fontSize = 16.sp,
                            shadow = Shadow(color = activeColor, blurRadius = 15f)
                        ),
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onToggleSystem()
                        }
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 1.dp)
                        .terminalGlow(activeColor, blurRadius = 1f)
                ) {
                    HorizontalDivider(thickness = 1.dp, color = activeColor)
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Application limits",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                style = TextStyle(
                    fontSize = 25.sp,
                    shadow = Shadow(color = activeColor, blurRadius = 15f)
                ),
                modifier = Modifier.padding(vertical = 16.dp),
                color = activeColor
            )

            if (limits.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "The list is empty. Add a limit.",
                        color = activeColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        style = TextStyle(
                            shadow = Shadow(color = activeColor, blurRadius = 15f)
                        ),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    AddNewModuleItem(
                        onClick = onAddClick,
                        activeColor = activeColor
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(limits, { it.packageName }) { item ->
                        val usedMs = usageStats?.get(item.packageName)
                        if (usedMs == null) {
                            AppLimitItemPlaceholder(appName = getAppName(item.packageName), activeColor)
                        } else {
                            AppLimitItem(
                                appName = getAppName(item.packageName),
                                minutes = item.limitMinutes,
                                icon = getAppIcon(item.packageName),
                                usedMs = usedMs,
                                onDelete = { onDelete(item.packageName) },
                                onClick = { onItemClick(item) },
                                activeColor = activeColor,
                            )
                        }
                    }
                    item {
                        AddNewModuleItem(
                            onClick = onAddClick,
                            activeColor = activeColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppLimitItem(
    appName: String,
    icon: Drawable?,
    minutes: Int,
    usedMs: Long,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    activeColor: Color,
) {
    val usedMin = usedMs / 1000 / 60
    val infiniteTransition = rememberInfiniteTransition(label = "BorderBlink")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BorderAlpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .terminalGlow(activeColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        shape = RectangleShape,
        border = BorderStroke(
            width = 1.dp,
            color = if (usedMin >= minutes) activeColor.copy(alpha = borderAlpha) else activeColor
        ),
        colors = CardDefaults.cardColors(containerColor = TerminalBackground)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let { drawable ->
                Image(
                    bitmap = drawable.toBitmap().asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .padding(end = 12.dp),
                    colorFilter = ColorFilter.colorMatrix(
                        getTerminalColorMatrix(activeColor)
                    )
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appName,
                    fontWeight = FontWeight.Bold,
                    color = activeColor,
                    style = TextStyle(
                        fontSize = 19.sp,
                        shadow = Shadow(color = activeColor, blurRadius = 15f)
                    ),
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = getTerminalProgressBar(usedMs, minutes),
                    color = activeColor,
                    style = TextStyle(
                        fontSize = 15.sp,
                        shadow = Shadow(color = activeColor, blurRadius = 3f)
                    ),
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                Icon(
                    painter = painterResource(id = ru.fairlak.antialphakid.R.drawable.ic_trash),
                    contentDescription = "Удалить",
                    tint = activeColor,
                    modifier = Modifier.size(40.dp))
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionDialog(
    installedApps: List<ApplicationInfo>,
    onSearchChange: (String) -> Unit,
    getAppName: (String) -> String,
    getAppIcon: (String) -> Drawable?,
    onAppSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    activeColor: Color,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        containerColor = TerminalBackground,
        modifier = Modifier.border(1.dp, activeColor, RectangleShape).terminalGlow(activeColor),
        shape = RectangleShape,
        dismissButton = {
            Box(
                modifier = Modifier
                    .border(1.dp, activeColor, RectangleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onDismiss() }
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "CANCEL",
                    color = activeColor,
                    fontFamily = FontFamily.Monospace,
                    style = TextStyle(
                        fontSize = 16.sp,
                        shadow = Shadow(color = activeColor, blurRadius = 15f)
                    ),

                )
            }
        },
        title = {
            Text(
                text = "Select an application",
                color = activeColor,
                fontFamily = FontFamily.Monospace,
                style = TextStyle(
                    fontSize = 29.sp,
                    shadow = Shadow(color = activeColor, blurRadius = 15f)
                )
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxHeight(0.8f)) {
                var localSearchQuery by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = localSearchQuery,
                    onValueChange = {
                        localSearchQuery = it
                        onSearchChange(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    placeholder = {
                        Text(
                            text = "Search...",
                            color = activeColor.copy(alpha = 0.5f),
                            fontFamily = FontFamily.Monospace,
                            style = TextStyle(
                                fontSize = 18.sp,
                                shadow = Shadow(color = activeColor, blurRadius = 10f)
                            )
                        )
                    },
                    singleLine = true,
                    shape = RectangleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = activeColor,
                        unfocusedTextColor = activeColor,
                        focusedBorderColor = activeColor,
                        unfocusedBorderColor = activeColor.copy(alpha = 0.5f),
                        cursorColor = activeColor,
                        selectionColors = TextSelectionColors(
                            handleColor = activeColor,
                            backgroundColor = activeColor.copy(alpha = 0.4f)
                        ),
                        focusedPlaceholderColor = activeColor.copy(alpha = 0.5f),
                        unfocusedPlaceholderColor = activeColor.copy(alpha = 0.5f)
                    ),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        shadow = Shadow(color = activeColor, blurRadius = 15f)
                    )
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(installedApps) { app ->
                        val icon = getAppIcon(app.packageName)
                        ListItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, activeColor, RectangleShape)
                                .terminalGlow(activeColor, blurRadius = 10f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onAppSelected(app.packageName) },
                            colors = ListItemDefaults.colors(
                                containerColor = TerminalBackground,
                                headlineColor = activeColor,
                            ),
                            headlineContent = { Text(
                                text = getAppName(app.packageName),
                                fontFamily = FontFamily.Monospace,
                                style = TextStyle(
                                    fontSize = 17.sp,
                                    shadow = Shadow(color = activeColor, blurRadius = 15f)
                                )
                            ) },
                            leadingContent = {
                                icon?.let { drawable ->
                                    Image(
                                        bitmap = drawable.toBitmap().asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        colorFilter = ColorFilter.colorMatrix(
                                            getTerminalColorMatrix(activeColor)
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    )
}


@Composable
fun AddNewModuleItem(
    onClick: () -> Unit,
    activeColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .terminalGlow(activeColor)
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
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "[ + ADD_NEW_MODULE ]",
                color = activeColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                style = TextStyle(
                    fontSize = 18.sp,
                    shadow = Shadow(color = activeColor, blurRadius = 15f)
                )
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLimitDialog(
    appName: String,
    currentLimit: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
    activeColor: Color
) {
    var textValue by remember { mutableStateOf(currentLimit.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TerminalBackground,
        modifier = Modifier.border(1.dp, activeColor, RectangleShape).terminalGlow(activeColor),
        shape = RectangleShape,
        title = {
            Text(
                text = "> EDIT: $appName",
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
                Text(
                    text = "Enter new limit (min):",
                    color = activeColor,
                    fontFamily = FontFamily.Monospace,
                    style = TextStyle(
                        fontSize = 16.sp,
                        shadow = Shadow(color = activeColor, blurRadius = 15f)
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { if (it.all { char -> char.isDigit() }) textValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        color = activeColor,
                        fontSize = 17.sp,
                        shadow = Shadow(color = activeColor, blurRadius = 15f)
                    ),
                    singleLine = true,
                    shape = RectangleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = activeColor,
                        unfocusedTextColor = activeColor,
                        focusedBorderColor = activeColor,
                        selectionColors = TextSelectionColors(
                            handleColor = activeColor,
                            backgroundColor = activeColor.copy(alpha = 0.4f)
                        ),
                        unfocusedBorderColor = activeColor.copy(alpha = 0.5f),
                        cursorColor = activeColor
                    )
                )
            }
        },
        confirmButton = {
            CompositionLocalProvider(LocalRippleConfiguration provides null) {
                Button(
                    onClick = {
                        val newLimit = textValue.toIntOrNull() ?: 30
                        onConfirm(newLimit)
                    },
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TerminalBackground,
                        contentColor = activeColor
                    ),
                    modifier = Modifier.border(1.dp, activeColor, RectangleShape)
                ) {
                    Text(
                        text = "SAVE",
                        fontFamily = FontFamily.Monospace,
                        style = TextStyle(
                            fontSize = 16.sp,
                            shadow = Shadow(color = activeColor, blurRadius = 15f)
                        )
                    )
                }
            }
        },
        dismissButton = {
            CompositionLocalProvider(LocalRippleConfiguration provides null) {
                TextButton(
                    onClick = onDismiss,
                    shape = RectangleShape
                ) {
                    Text(
                        "CANCEL",
                        color = activeColor.copy(alpha = 0.7f),
                        fontFamily = FontFamily.Monospace,
                        style = TextStyle(
                            fontSize = 16.sp,
                            shadow = Shadow(color = activeColor, blurRadius = 15f)
                        )
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionRequiredScreen(onSafeClick: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasUsage by remember { mutableStateOf(hasUsageStatsPermission(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsage = hasUsageStatsPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val headerText = if (!hasUsage) "> SYSTEM ERROR: ACCESS_DENIED" else "> SYSTEM WARNING: OVERLAY_REQUIRED"
    val descriptionText = if (!hasUsage) {
        "Для работы мониторинга требуется доступ к статистике использования. Без этого система не узнает, когда открыто ваше приложение."
    } else {
        "Доступ к статистике получен. Теперь необходимо разрешить наложение поверх окон, чтобы система могла блокировать экран."
    }
    val buttonText = if (!hasUsage) "[ ПРЕДОСТАВИТЬ ДОСТУП ]" else "[ РАЗРЕШИТЬ НАЛОЖЕНИЕ ]"

    var showDescription by remember(headerText) { mutableStateOf(false) }
    var showButton by remember(headerText) { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
            TypingText(
                text = headerText,
                key = headerText,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    shadow = Shadow(color = TerminalGreen, blurRadius = 15f)
                ),
                color = TerminalGreen,
                delayMillis = 40,
                onFinished = { showDescription = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.heightIn(min = 120.dp)) {
                if (showDescription) {
                    TypingText(
                        text = descriptionText,
                        key = descriptionText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 18.sp,
                            shadow = Shadow(color = TerminalGreen, blurRadius = 15f)
                        ),
                        color = TerminalGreen,
                        delayMillis = 15,
                        onFinished = { showButton = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (showButton) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CompositionLocalProvider(LocalRippleConfiguration provides null) {
                        OutlinedButton(
                            onClick = onSafeClick,
                            shape = RectangleShape,
                            border = BorderStroke(1.dp, TerminalGreen),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TerminalGreen),
                            modifier = Modifier
                                .wrapContentWidth()
                                .height(56.dp),
                            contentPadding = PaddingValues(horizontal = 48.dp)
                        ) {
                            Box(contentAlignment = Alignment.CenterStart) {
                                Text(
                                    text = buttonText,
                                    style = TextStyle(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Transparent
                                )
                                TypingText(
                                    text = buttonText,
                                    key = buttonText,
                                    style = TextStyle(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        shadow = Shadow(color = TerminalGreen, blurRadius = 15f)
                                    ),
                                    color = TerminalGreen,
                                    delayMillis = 50,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockScreen(
    onCorrectPassword: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
    activeColor: Color
) {
    var passwordInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var showInput by remember { mutableStateOf(false) }

    val headerText = "> SECURITY_CHECK: DATABASE_ENCRYPTED"
    val subText = "Enter the access key to initialize the management interface..."

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            TypingText(
                text = headerText,
                key = headerText,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    shadow = Shadow(color = activeColor, blurRadius = 15f)
                ),
                color = activeColor,
                delayMillis = 40,
                onFinished = { showInput = true }
            )

            if (showInput) {
                Spacer(modifier = Modifier.height(16.dp))

                TypingText(
                    text = subText,
                    key = subText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 16.sp,
                        shadow = Shadow(color = activeColor, blurRadius = 15f)
                    ),
                    color = activeColor,
                    delayMillis = 20
                )

                Spacer(modifier = Modifier.height(32.dp))

                TextField(
                    value = passwordInput,
                    onValueChange = {
                        passwordInput = it
                        isError = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (isError) TerminalRed else activeColor, RectangleShape),
                    textStyle = TextStyle(
                        color = if (isError) TerminalRed else activeColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 20.sp,
                        shadow = Shadow(color = activeColor, blurRadius = 15f)
                    ),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        cursorColor = activeColor,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        selectionColors = TextSelectionColors(
                            handleColor = activeColor,
                            backgroundColor = activeColor.copy(alpha = 0.3f)
                        )
                    ),
                    placeholder = {
                        Text(
                            text = "PASSWORD_REQUIRED_",
                            color = activeColor.copy(alpha = 0.3f),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                )

                if (isError) {
                    Text(
                        text = "!! ACCESS_DENIED: INVALID_KEY !!",
                        color = TerminalRed,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 8.dp),
                        style = TextStyle(
                            fontSize = 17.sp,
                            shadow = Shadow(color = TerminalRed, blurRadius = 15f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                CompositionLocalProvider(LocalRippleConfiguration provides null) {
                    OutlinedButton(
                        onClick = {
                            if (viewModel.checkPassword(passwordInput)) {
                                onCorrectPassword()
                            } else {
                                isError = true
                                passwordInput = ""
                            }
                        },
                        shape = RectangleShape,
                        border = BorderStroke(1.dp, activeColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = "[ INITIALIZE_LOGIN ]",
                            color = activeColor,
                            fontFamily = FontFamily.Monospace,
                            style = TextStyle(
                                fontSize = 18.sp,
                                shadow = Shadow(color = activeColor, blurRadius = 10f)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TypingText(
    text: String,
    key: Any? = null,
    style: TextStyle,
    color: Color,
    textAlign: TextAlign = TextAlign.Start,
    delayMillis: Long = 30,
    onFinished: () -> Unit = {}
) {
    var displayedText by remember(key) { mutableStateOf("") }

    LaunchedEffect(key, text) {
        displayedText = ""
        text.forEachIndexed { index, _ ->
            displayedText = text.substring(0, index + 1)
            delay(delayMillis)
        }
        onFinished()
    }

    Text(
        text = displayedText,
        style = style,
        color = color,
        textAlign = textAlign,
        fontFamily = FontFamily.Monospace,
        softWrap = true
    )
}

fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
    } else {
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

fun hasOverlayPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else true
}

fun getTerminalProgressBar(currentMs: Long, limitMinutes: Int): String {
    val currentMinutes = (currentMs / 1000 / 60).toInt()
    val totalBars = 10

    val progress = when {
        limitMinutes <= 0 -> totalBars
        else -> (currentMinutes.toFloat() / limitMinutes.toFloat() * totalBars)
            .toInt()
            .coerceIn(0, totalBars)
    }

    val filled = "#".repeat(progress)
    val empty = "-".repeat(totalBars - progress)

    return "[$filled$empty] $currentMinutes/$limitMinutes MIN"
}

fun getTerminalColorMatrix(targetColor: Color): ColorMatrix {
    val r = targetColor.red
    val g = targetColor.green
    val b = targetColor.blue

    return ColorMatrix(floatArrayOf(
        0.33f * r, 0.59f * r, 0.11f * r, 0f, 0f,
        0.33f * g, 0.59f * g, 0.11f * g, 0f, 0f,
        0.33f * b, 0.59f * b, 0.11f * b, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    ))
}

@Composable
fun AppLimitItemPlaceholder(appName: String, activeColor: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RectangleShape,
        border = BorderStroke(1.dp, activeColor.copy(alpha = 0.3f)),
        colors = CardDefaults.cardColors(containerColor = activeColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = appName, fontFamily = FontFamily.Monospace, color = activeColor.copy(alpha = 0.5f))
            Text(
                text = "[..........] LOADING...",
                fontFamily = FontFamily.Monospace,
                color = activeColor.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
