package com.and2long.applist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.and2long.applist.AppInfo
import com.and2long.applist.R

object AppFilter {
    const val USER = 0
    const val SYSTEM = 1
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun AppListScreen(
    refreshToken: Int,
    onRefresh: () -> Unit,
    onLoadApps: suspend (Int) -> List<AppInfo>,
    onOpenApp: (String) -> Unit,
    onOpenDetail: (String) -> Unit,
    onShareApp: (AppInfo) -> Unit,
    onUninstallApps: (List<String>) -> Unit
) {
    var selectedType by remember { mutableIntStateOf(AppFilter.USER) }
    var apps by remember { mutableStateOf(emptyList<AppInfo>()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var keyword by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val selectedApp = remember { mutableStateOf<AppInfo?>(null) }
    var selectedPackageNames by remember { mutableStateOf(emptySet<String>()) }
    val showUninstallConfirmDialog = remember { mutableStateOf(false) }
    val isSelectionMode = selectedPackageNames.isNotEmpty()
    val filteredApps = apps.filterByKeyword(keyword)

    fun toggleSelection(packageName: String) {
        selectedPackageNames = if (packageName in selectedPackageNames) {
            selectedPackageNames - packageName
        } else {
            selectedPackageNames + packageName
        }
    }

    selectedApp.value?.let { appInfo ->
        AppDetailScreen(
            appInfo = appInfo,
            onBack = { selectedApp.value = null },
            onOpenApp = { onOpenApp(appInfo.packageName) },
            onOpenSystemDetail = { onOpenDetail(appInfo.packageName) },
            onShare = { onShareApp(appInfo) }
        )
        return
    }

    if (showUninstallConfirmDialog.value) {
        AlertDialog(
            onDismissRequest = { showUninstallConfirmDialog.value = false },
            title = { Text(text = stringResource(R.string.uninstall_confirm_title)) },
            text = {
                Text(
                    text = pluralStringResource(
                        R.plurals.uninstall_confirm_message,
                        selectedPackageNames.size,
                        selectedPackageNames.size
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val packageNamesToUninstall = selectedPackageNames.toList()
                        showUninstallConfirmDialog.value = false
                        onUninstallApps(packageNamesToUninstall)
                        selectedPackageNames = emptySet()
                    }
                ) {
                    Text(text = stringResource(R.string.uninstall))
                }
            },
            dismissButton = {
                TextButton(onClick = { showUninstallConfirmDialog.value = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showSearch && !isSelectionMode) {
                        BasicTextField(
                            value = keyword,
                            onValueChange = { keyword = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.extraLarge
                                )
                                .focusRequester(searchFocusRequester),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (keyword.isEmpty()) {
                                            Text(
                                                text = stringResource(R.string.search),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            }
                        )
                    } else {
                        Text(
                            text = if (isSelectionMode) {
                                pluralStringResource(
                                    R.plurals.selected_count,
                                    selectedPackageNames.size,
                                    selectedPackageNames.size
                                )
                            } else {
                                pluralStringResource(R.plurals.app_count, filteredApps.size, filteredApps.size)
                            }
                        )
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        TextButton(onClick = { selectedPackageNames = emptySet() }) {
                            Text(text = stringResource(R.string.cancel))
                        }
                        TextButton(onClick = { showUninstallConfirmDialog.value = true }) {
                            Text(text = stringResource(R.string.uninstall))
                        }
                    } else if (showSearch) {
                        IconButton(
                            onClick = {
                                keyword = ""
                                showSearch = false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.close_search)
                            )
                        }
                    } else {
                        AppTypeDropdown(
                            selectedType = selectedType,
                            onSelected = { selectedType = it }
                        )
                        IconButton(onClick = { showSearch = true }) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = stringResource(R.string.search)
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                if (!isLoading) {
                    isRefreshing = true
                    onRefresh()
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (filteredApps.isEmpty() && !isLoading) {
                Text(
                    text = stringResource(R.string.empty_apps),
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { appInfo ->
                    AppInfoRow(
                        appInfo = appInfo,
                        isSelectionMode = isSelectionMode,
                        isSelected = appInfo.packageName in selectedPackageNames,
                        onClick = {
                            if (isSelectionMode) {
                                toggleSelection(appInfo.packageName)
                            } else {
                                selectedApp.value = appInfo
                            }
                        },
                        onLongClick = { toggleSelection(appInfo.packageName) }
                    )
                    HorizontalDivider()
                }
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }

    LaunchedEffect(selectedType, refreshToken) {
        isLoading = true
        apps = onLoadApps(selectedType)
        selectedPackageNames = emptySet()
        isLoading = false
        isRefreshing = false
    }

    LaunchedEffect(showSearch) {
        if (showSearch) {
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }
}

private fun List<AppInfo>.filterByKeyword(keyword: String): List<AppInfo> {
    val query = keyword.trim()
    if (query.isEmpty()) return this

    return filter {
        it.appName.contains(query, ignoreCase = true) ||
            it.packageName.contains(query, ignoreCase = true) ||
            it.versionName.contains(query, ignoreCase = true) ||
            it.versionCode.contains(query, ignoreCase = true)
    }
}

@Composable
private fun AppTypeDropdown(
    selectedType: Int,
    onSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        AppFilter.USER to stringResource(R.string.user_apps),
        AppFilter.SYSTEM to stringResource(R.string.system_apps)
    )
    val selectedLabel = options.first { it.first == selectedType }.second

    Box {
        TextButton(onClick = { expanded = true }) {
            Text(text = selectedLabel)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (type, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        expanded = false
                        onSelected(type)
                    }
                )
            }
        }
    }
}
