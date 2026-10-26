package com.mapherdf.maplenilset.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mapherdf.maplenilset.BuildConfig
import com.mapherdf.maplenilset.R
import com.mapherdf.maplenilset.ui.theme.themeContainerColor
import com.mapherdf.maplenilset.ui.update.UpdateState
import com.mapherdf.maplenilset.ui.update.UpdateViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    updateViewModel: UpdateViewModel = hiltViewModel(),
) {
    val updateState by updateViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val sourceUrl = stringResource(R.string.about_source_url)

    LaunchedEffect(Unit) {
        updateViewModel.checkForUpdate()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = themeContainerColor(),
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp)
                .padding(top = 8.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AboutCard {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.about_copyright),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            AboutCard {
                Text(
                    text = stringResource(R.string.about_source_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.about_source_url),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(sourceUrl)))
                    },
                )
                Text(
                    text = stringResource(R.string.about_source_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AboutCard {
                Text(
                    text = stringResource(R.string.about_license_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.about_license),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.about_license_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            UpdateCard(
                updateState = updateState,
                currentVersion = updateViewModel.currentVersion,
                onDownload = { url -> updateViewModel.startDownload(url) },
                onInstall = { file ->
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file,
                    )
                    context.startActivity(
                        Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                            setDataAndType(uri, "application/vnd.android.package-archive")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        },
                    )
                },
            )
        }
    }
}

@Composable
private fun AboutCard(
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = themeContainerColor(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content,
        )
    }
}

@Composable
private fun UpdateCard(
    updateState: UpdateState,
    currentVersion: String,
    onDownload: (String) -> Unit,
    onInstall: (File) -> Unit,
) {
    val latestVersion = when (val state = updateState) {
        is UpdateState.UpToDate -> state.currentVersion
        is UpdateState.Available -> state.latestVersion
        else -> null
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = themeContainerColor(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.about_check_update)) },
            supportingContent = {
                UpdateSupportingText(updateState, currentVersion, latestVersion)
            },
            leadingContent = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.height(48.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_update),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.clickable {
                if (updateState is UpdateState.Available) {
                    onDownload(updateState.downloadUrl)
                }
            },
        )
    }

    when (val state = updateState) {
        is UpdateState.Downloading -> UpdateDownloadingDialog(progress = state.progress)
        is UpdateState.Downloaded -> UpdateDownloadedDialog(file = state.file, onInstall = onInstall)
        else -> { }
    }
}

@Composable
private fun UpdateSupportingText(
    updateState: UpdateState,
    currentVersion: String,
    latestVersion: String?,
) {
    when {
        updateState is UpdateState.Checking -> Text(
            text = stringResource(R.string.about_update_checking),
            style = MaterialTheme.typography.bodySmall,
        )
        updateState is UpdateState.Error -> Text(
            text = stringResource(R.string.about_update_check_failed),
            style = MaterialTheme.typography.bodySmall,
        )
        latestVersion != null -> Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.about_update_current, currentVersion),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.about_update_latest, latestVersion),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        else -> Text(
            text = stringResource(R.string.about_update_checking),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun UpdateDownloadingDialog(progress: Int) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text(stringResource(R.string.about_check_update)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.about_update_downloading, progress))
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { },
    )
}

@Composable
private fun UpdateDownloadedDialog(
    file: File,
    onInstall: (File) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text(stringResource(R.string.about_check_update)) },
        text = { Text(stringResource(R.string.about_update_downloaded)) },
        confirmButton = {
            TextButton(onClick = { onInstall(file) }) {
                Text(stringResource(R.string.about_update_install))
            }
        },
    )
}
