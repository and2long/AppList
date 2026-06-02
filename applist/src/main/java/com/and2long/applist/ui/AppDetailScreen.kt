package com.and2long.applist.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.and2long.applist.AppInfo
import com.and2long.applist.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    appInfo: AppInfo,
    onBack: () -> Unit,
    onOpenApp: () -> Unit,
    onOpenSystemDetail: () -> Unit
) {
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onOpenSystemDetail) {
                        Text(text = stringResource(R.string.system_detail))
                    }
                    TextButton(onClick = onOpenApp) {
                        Text(text = stringResource(R.string.open))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                DetailHeader(appInfo = appInfo)
            }
            item {
                HorizontalDivider()
            }
            item {
                DetailLine(label = stringResource(R.string.package_name), value = appInfo.packageName)
            }
            item {
                DetailLine(label = stringResource(R.string.version_name), value = appInfo.versionName)
            }
            item {
                DetailLine(label = stringResource(R.string.version_code), value = appInfo.versionCode)
            }
            item {
                DetailLine(label = stringResource(R.string.min_sdk_version), value = appInfo.minSdkVersion)
            }
            item {
                DetailLine(label = stringResource(R.string.target_sdk_version), value = appInfo.targetSdkVersion)
            }
            item {
                HorizontalDivider()
            }
            item {
                SectionTitle(text = stringResource(R.string.signature))
            }
            item {
                DetailLine(label = stringResource(R.string.md5), value = appInfo.signatureMd5)
            }
            item {
                DetailLine(label = stringResource(R.string.sha1), value = appInfo.signatureSha1)
            }
            item {
                DetailLine(label = stringResource(R.string.sha256), value = appInfo.signatureSha256)
            }
        }
    }
}

@Composable
private fun DetailHeader(appInfo: AppInfo) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AppIcon(
            drawable = appInfo.appIcon,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = appInfo.appName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        SelectionContainer {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
