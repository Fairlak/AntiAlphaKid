package ru.fairlak.antialphakid.features.dashboard.ui

import android.content.pm.ApplicationInfo
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    onManagePermissions: () -> Unit
) {
    val limits by viewModel.appLimits.collectAsState()
    val filteredApps by viewModel.filteredApps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val showDialog = remember { mutableStateOf(false) }
    var editingEntity = remember { mutableStateOf<AppUsageEntity?>(null) }

    if (showDialog.value) {
        AppSelectionDialog(
            installedApps = filteredApps,
            searchQuery = searchQuery,
            onSearchChange = { viewModel.onSearchQueryChange(it) },
            getAppName = { viewModel.getAppName(it) },
            onAppSelected = { pkg ->
                viewModel.saveLimit(pkg, 30)
                showDialog.value = false
                viewModel.onSearchQueryChange("")
            },
            onDismiss = {
                showDialog.value = false
                viewModel.onSearchQueryChange("")
            }
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
            onDismiss = { editingEntity.value = null }
        )
    }


    DashboardContent(
        limits = limits,
        getAppName = { viewModel.getAppName(it) },
        onDelete = { viewModel.removeLimit(it) },
        onAddClick = { showDialog.value = true },
        onItemClick = { editingEntity.value = it },
        onManagePermissions = onManagePermissions
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    limits: List<AppUsageEntity>,
    getAppName: (String) -> String,
    onDelete: (String) -> Unit,
    onAddClick: () -> Unit,
    onItemClick: (AppUsageEntity) -> Unit,
    onManagePermissions: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("AntiAlpha Control") })
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = onManagePermissions,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Разрешения")
                }

                Spacer(modifier = Modifier.height(16.dp))

                ExtendedFloatingActionButton(
                    onClick = onAddClick,
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("Добавить") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Лимиты приложений",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            if (limits.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Список пуст. Добавьте лимит.", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(limits) { item ->
                        AppLimitItem(
                            appName = getAppName(item.packageName),
                            packageName = item.packageName,
                            minutes = item.limitMinutes,
                            onDelete = { onDelete(item.packageName) },
                            onClick = { onItemClick(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppLimitItem(appName: String, packageName: String, minutes: Int, onDelete: () -> Unit, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = appName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)

                if (appName != packageName) {
                    Text(text = packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }

                Text(
                    text = "Лимит: $minutes мин.",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
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
    onAppSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
        title = { Text("Выберите приложение") },
        text = {
            Column(modifier = Modifier.fillMaxHeight(0.8f)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    placeholder = { Text("Поиск...") },
                    singleLine = true
                )

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(installedApps) { app ->
                        ListItem(
                            headlineContent = { Text(getAppName(app.packageName)) },
                            supportingContent = { Text(app.packageName) },
                            modifier = Modifier.clickable { onAppSelected(app.packageName) }
                        )
                    }
                }
            }
        }
    )
}


@Composable
fun EditLimitDialog(
    appName: String,
    currentLimit: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var textValue by remember { mutableStateOf(currentLimit.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Лимит для $appName") },
        text = {
            Column {
                Text("Введите время в минутах:", modifier = Modifier.padding(bottom = 8.dp))
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { if (it.all { char -> char.isDigit() }) textValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Минуты") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val newLimit = textValue.toIntOrNull() ?: 30
                onConfirm(newLimit)
            }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}


@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    MaterialTheme {
        DashboardContent(
            limits = listOf(
                AppUsageEntity("com.zhiliaoapp.musically", 15),
                AppUsageEntity("com.google.android.youtube", 30)
            ),
            getAppName = { pkg ->
                if (pkg.contains("musically")) "TikTok" else "YouTube"
            },
            onDelete = {},
            onAddClick = {},
            onItemClick = {},
            onManagePermissions = {}
        )
    }
}
