package ru.fairlak.antialphakid.features.dashboard.ui

import android.R
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
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
import ru.fairlak.antialphakid.features.settings.viewmodel.SettingsViewModel

private val TerminalGreen @Composable get() = MaterialTheme.colorScheme.primary
private val TerminalBackground @Composable get() = MaterialTheme.colorScheme.background

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    onManagePermissions: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val hasPassword by settingsViewModel.hasPassword.collectAsState()
    var isAuthenticated by rememberSaveable { mutableStateOf(false) }

    if (hasPassword && !isAuthenticated) {
        AppLockScreen(
            viewModel = settingsViewModel,
            onCorrectPassword = { isAuthenticated = true }
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
            val isSystemActive by viewModel.isSystemActive.collectAsState()
            val activeColor = if (isSystemActive) TerminalGreen else TerminalRed

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
                    searchQuery = searchQuery,
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
                    isSystemActive = isSystemActive

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
                onToggleSystem = { viewModel.toggleSystemState() },
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
                            fontWeight = FontWeight.Bold
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
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onToggleSystem()
                        }
                    )
                }
                HorizontalDivider(
                    thickness = 1.dp,
                    color = activeColor
                )
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
                style = MaterialTheme.typography.headlineSmall,
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
                            AppLimitItemPlaceholder(appName = getAppName(item.packageName))
                        } else {
                            AppLimitItem(
                                appName = getAppName(item.packageName),
                                packageName = item.packageName,
                                minutes = item.limitMinutes,
                                icon = getAppIcon(item.packageName),
                                usedMs = usedMs,
                                onDelete = { onDelete(item.packageName) },
                                onClick = { onItemClick(item) },
                                activeColor = activeColor,
                                isSystemActive = isSystemActive
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
    packageName: String,
    icon: Drawable?,
    minutes: Int,
    usedMs: Long,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    activeColor: Color,
    isSystemActive: Boolean
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
            .clickable { onClick() },
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
                        getTerminalColorMatrix(isSystemActive)
                    )
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = appName,
                    fontWeight = FontWeight.Bold,
                    color = activeColor,
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = FontFamily.Monospace)

                Text(
                    text = getTerminalProgressBar(usedMs, minutes),
                    color = activeColor,
                    style = MaterialTheme.typography.bodyMedium,
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
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    getAppName: (String) -> String,
    getAppIcon: (String) -> Drawable?,
    onAppSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    activeColor: Color,
    isSystemActive: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        containerColor = TerminalBackground,
        modifier = Modifier.border(1.dp, activeColor, RectangleShape),
        shape = RectangleShape,
        dismissButton = {
            Button(
                onClick = onDismiss,
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TerminalBackground,
                    contentColor = activeColor
                ),
                modifier = Modifier.border(1.dp, activeColor, RectangleShape),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "ОТМЕНА",
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        title = {
            Text(
                text = "Select an application",
                color = activeColor,
                fontFamily = FontFamily.Monospace
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
                            text = "Поиск...",
                            color = activeColor.copy(alpha = 0.5f),
                            fontFamily = FontFamily.Monospace
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
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace)
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
                                .clickable { onAppSelected(app.packageName) },
                            colors = ListItemDefaults.colors(
                                containerColor = TerminalBackground,
                                headlineColor = activeColor,
                            ),
                            headlineContent = { Text(getAppName(app.packageName)) },
                            leadingContent = {
                                icon?.let { drawable ->
                                    Image(
                                        bitmap = drawable.toBitmap().asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        colorFilter = ColorFilter.colorMatrix(
                                            getTerminalColorMatrix(isSystemActive)
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
            .clickable { onClick() },
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
                style = MaterialTheme.typography.bodyLarge
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
        modifier = Modifier.border(1.dp, activeColor, RectangleShape),
        shape = RectangleShape,
        title = {
            Text(
                text = "> EDIT: $appName",
                color = activeColor,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter new limit (min):",
                    color = activeColor,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { if (it.all { char -> char.isDigit() }) textValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = activeColor),
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
                Text("SAVE", fontFamily = FontFamily.Monospace)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RectangleShape
            ) {
                Text(
                    "CANCEL",
                    color = activeColor.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    )
}

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
            .background(Color.Black)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
            TypingText(
                text = headerText,
                key = headerText,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
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
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 18.sp),
                        color = TerminalGreen,
                        delayMillis = 15,
                        onFinished = { showButton = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (showButton) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
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
                                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                fontFamily = FontFamily.Monospace,
                                color = Color.Transparent
                            )
                            TypingText(
                                text = buttonText,
                                key = buttonText,
                                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold),
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


@Composable
fun AppLockScreen(
    onCorrectPassword: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    var passwordInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var showInput by remember { mutableStateOf(false) }

    val headerText = "> SECURITY_CHECK: DATABASE_ENCRYPTED"
    val subText = "Enter the access key to initialize the management interface..."

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
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
                    fontFamily = FontFamily.Monospace
                ),
                color = TerminalGreen,
                delayMillis = 40,
                onFinished = { showInput = true }
            )

            if (showInput) {
                Spacer(modifier = Modifier.height(16.dp))

                TypingText(
                    text = subText,
                    key = subText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                    color = TerminalGreen,
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
                        .border(1.dp, if (isError) TerminalRed else TerminalGreen, RectangleShape),
                    textStyle = TextStyle(
                        color = if (isError) TerminalRed else TerminalGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 20.sp
                    ),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        cursorColor = TerminalGreen,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    placeholder = {
                        Text(
                            "PASSWORD_REQUIRED_",
                            color = TerminalGreen.copy(alpha = 0.3f),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                )

                if (isError) {
                    Text(
                        text = "!! ACCESS_DENIED: INVALID_KEY !!",
                        color = TerminalRed,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

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
                    border = BorderStroke(1.dp, TerminalGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = "[ INITIALIZE_LOGIN ]",
                        color = TerminalGreen,
                        fontFamily = FontFamily.Monospace
                    )
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
            android.os.Process.myUid(),
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

fun getTerminalColorMatrix(isSystemActive: Boolean): ColorMatrix {
    return if (isSystemActive) {
        ColorMatrix(floatArrayOf(
            0f, 0.5f, 0f, 0f, 0f,
            0f, 1f,   0f, 0f, 0f,
            0f, 0.5f, 0f, 0f, 0f,
            0f, 0f,   0f, 1f, 0f
        ))
    } else {
        ColorMatrix(floatArrayOf(
            0f, 1f,   0f, 0f, 0f,
            0f, 0f,   0f, 0f, 0f,
            0f, 0f,   0f, 0f, 0f,
            0f, 0f,   0f, 1f, 0f
        ))
    }
}

@Composable
fun TypingText(
    text: String,
    key: Any? = null, // Добавляем ключ
    modifier: Modifier = Modifier,
    style: TextStyle,
    color: Color,
    textAlign: TextAlign = TextAlign.Start,
    delayMillis: Long = 30,
    startDelay: Long = 0,
    onFinished: () -> Unit = {}
) {
    // remember(key) очистит переменную, как только ключ изменится
    var displayedText by remember(key) { mutableStateOf("") }

    LaunchedEffect(key, text) {
        displayedText = "" // Принудительно очищаем
        delay(startDelay)
        text.forEachIndexed { index, _ ->
            displayedText = text.substring(0, index + 1)
            delay(delayMillis)
        }
        onFinished()
    }

    Text(
        text = displayedText,
        modifier = modifier,
        style = style,
        color = color,
        textAlign = textAlign,
        fontFamily = FontFamily.Monospace
    )
}


@Composable
fun AppLimitItemPlaceholder(appName: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RectangleShape,
        border = BorderStroke(1.dp, TerminalGreen.copy(alpha = 0.3f)),
        colors = CardDefaults.cardColors(containerColor = TerminalBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = appName, fontFamily = FontFamily.Monospace, color = TerminalGreen.copy(alpha = 0.5f))
            Text(
                text = "[..........] LOADING...",
                fontFamily = FontFamily.Monospace,
                color = TerminalGreen.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

