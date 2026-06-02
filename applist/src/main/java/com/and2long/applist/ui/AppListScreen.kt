package com.and2long.applist.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.and2long.applist.AppInfo
import com.and2long.applist.R

object AppFilter {
    const val USER = 0
    const val SYSTEM = 1
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    refreshToken: Int,
    onRefresh: () -> Unit,
    onLoadApps: suspend (Int) -> List<AppInfo>,
    onOpenApp: (String) -> Unit,
    onOpenDetail: (String) -> Unit
) {
    var selectedType by remember { mutableIntStateOf(AppFilter.USER) }
    var apps by remember { mutableStateOf(emptyList<AppInfo>()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }

    selectedApp?.let { appInfo ->
        AppDetailScreen(
            appInfo = appInfo,
            onBack = { selectedApp = null },
            onOpenApp = { onOpenApp(appInfo.packageName) },
            onOpenSystemDetail = { onOpenDetail(appInfo.packageName) }
        )
        return
    }

    LaunchedEffect(selectedType, refreshToken) {
        isLoading = true
        apps = onLoadApps(selectedType)
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.app_name)) },
                actions = {
                    AppTypeDropdown(
                        selectedType = selectedType,
                        onSelected = { selectedType = it }
                    )
                    TextButton(onClick = onRefresh) {
                        Text(text = stringResource(R.string.refresh))
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (apps.isEmpty() && !isLoading) {
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
                items(apps, key = { it.packageName }) { appInfo ->
                    AppInfoRow(
                        appInfo = appInfo,
                        onClick = { selectedApp = appInfo }
                    )
                    HorizontalDivider()
                }
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
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
