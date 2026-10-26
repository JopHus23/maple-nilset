package com.mapherdf.maplenilset.ui.bili

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mapherdf.maplenilset.R
import com.mapherdf.maplenilset.bili.download.BiliTaskState
import com.mapherdf.maplenilset.bili.model.BiliMergeOutcome
import com.mapherdf.maplenilset.bili.model.BiliQuality
import com.mapherdf.maplenilset.ui.theme.themeContainerBorderColor
import com.mapherdf.maplenilset.ui.theme.themeContainerColor

@Composable
fun BiliNilScreen(
    modifier: Modifier = Modifier,
    coverViewModel: BiliNilViewModel = hiltViewModel(),
    videoViewModel: BiliVideoViewModel = hiltViewModel(),
) {
    val coverState by coverViewModel.uiState.collectAsStateWithLifecycle()
    val videoState by videoViewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    var showBiliLogin by remember { mutableStateOf(false) }

    if (showBiliLogin) {
        BiliWebLoginScreen(
            onNavigateBack = { showBiliLogin = false },
            viewModel = videoViewModel,
        )
        return
    }

    if (showSettings) {
        BiliNilSettingsScreen(
            onNavigateBack = { showSettings = false },
            onOpenBiliLogin = { showBiliLogin = true },
            viewModel = videoViewModel,
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BiliNilTabRow(
                selectedIndex = selectedTab,
                onTabSelected = { selectedTab = it },
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { showSettings = true },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = stringResource(R.string.settings_title),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        when (selectedTab) {
            0 -> BiliCoverContent(state = coverState, viewModel = coverViewModel)
            else -> BiliVideoContent(
                state = videoState,
                viewModel = videoViewModel,
                onOpenCoverDownload = { bvid ->
                    selectedTab = 0
                    coverViewModel.updateInput(bvid)
                    coverViewModel.resolve()
                },
            )
        }
    }
}

@Composable
private fun BiliVideoContent(
    state: BiliVideoUiState,
    viewModel: BiliVideoViewModel,
    onOpenCoverDownload: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (state.showLoginPrompt) {
            BiliNilCard {
                Text(
                    stringResource(R.string.bili_nil_video_not_logged_in),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        OutlinedTextField(
            value = state.inputText,
            onValueChange = viewModel::updateInput,
            label = { Text(stringResource(R.string.bili_nil_video_input_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Button(
            onClick = viewModel::resolve,
            enabled = !state.isResolving && state.inputText.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
        ) {
            if (state.isResolving) {
                CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text(stringResource(R.string.bili_nil_video_resolving))
            } else {
                Text(stringResource(R.string.bili_nil_video_resolve))
            }
        }
        state.errorMessage?.let { msg ->
            BiliNilCard { Text(msg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
        }
        state.videoInfo?.let { info ->
            BiliNilCard {
                Text(info.title ?: "", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(info.bvid ?: "", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                val coverBitmap = state.coverPreview
                if (coverBitmap == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (state.isCoverLoading) {
                            CircularProgressIndicator(modifier = Modifier.height(24.dp), strokeWidth = 3.dp)
                        }
                    }
                } else {
                    CoverImage(bitmap = coverBitmap) {
                        info.bvid?.let(onOpenCoverDownload)
                    }
                }
                if (state.availableQualities.isNotEmpty()) {
                    Text(stringResource(R.string.bili_nil_video_quality), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    BiliQualitySelector(
                        qualities = state.availableQualities,
                        availableCodes = state.downloadableQualityCodes,
                        selected = state.selectedQuality,
                        onSelect = viewModel::selectQuality,
                    )
                }
                Button(
                    onClick = viewModel::enqueueDownload,
                    enabled = !state.isEnqueuing,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                ) { Text(stringResource(R.string.bili_nil_video_download)) }
            }
        }
        if (state.taskState != null && state.taskState != BiliTaskState.QUEUED) {
            BiliNilCard {
                val progress = state.taskProgress
                when (state.taskState) {
                    BiliTaskState.DOWNLOADING -> {
                        Text(stringResource(R.string.bili_nil_video_downloading), style = MaterialTheme.typography.titleSmall)
                        val vd = progress?.videoBytesDownloaded ?: 0
                        val vt = progress?.videoBytesTotal ?: 0
                        val fraction = if (vt > 0) vd.toFloat() / vt else 0f
                        LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                        Text("${formatBytes(vd)} / ${formatBytes(vt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = viewModel::pauseTask, shape = RoundedCornerShape(8.dp)) { Text(stringResource(R.string.bili_nil_video_pause)) }
                            Button(onClick = viewModel::cancelTask, shape = RoundedCornerShape(8.dp), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.bili_nil_video_cancel)) }
                        }
                    }
                    BiliTaskState.PAUSED -> {
                        Text(stringResource(R.string.bili_nil_video_paused), style = MaterialTheme.typography.titleSmall)
                        Button(onClick = viewModel::resumeTask, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) { Text(stringResource(R.string.bili_nil_video_resume)) }
                        Button(onClick = viewModel::cancelTask, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.bili_nil_video_cancel)) }
                    }
                    BiliTaskState.MERGING, BiliTaskState.EXPORTING -> {
                        Text(stringResource(R.string.bili_nil_video_merging), style = MaterialTheme.typography.titleSmall)
                    }
                    BiliTaskState.COMPLETED -> {
                        Text(stringResource(R.string.bili_nil_video_completed), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        when (state.mergeOutcome) {
                            BiliMergeOutcome.SEPARATE -> Text(stringResource(R.string.bili_nil_video_separate_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            BiliMergeOutcome.MERGED -> Text(stringResource(R.string.bili_nil_video_merge_done), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            else -> {}
                        }
                        state.downgradeReason?.let { reason ->
                            Text(reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    BiliTaskState.FAILED -> {
                        Text(stringResource(R.string.bili_nil_video_failed), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
                        state.errorMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                        Button(onClick = viewModel::cancelTask, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) { Text(stringResource(R.string.bili_nil_video_resolve)) }
                    }
                    else -> {
                        CircularProgressIndicator(modifier = Modifier.height(24.dp), strokeWidth = 3.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun BiliQualitySelector(
    qualities: List<BiliQuality>,
    availableCodes: Set<Int>,
    selected: BiliQuality,
    onSelect: (BiliQuality) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        qualities.filter { it.code in availableCodes }.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { quality ->
                    val isSelected = quality == selected
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else themeContainerColor(),
                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else themeContainerBorderColor()),
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { onSelect(quality) },
                    ) {
                        Text(
                            quality.label,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 0 -> "--"
    bytes < 1024 -> "${bytes}B"
    bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024f)}KB"
    else -> "${"%.1f".format(bytes / (1024f * 1024f))}MB"
}
@Composable
private fun BiliCoverContent(
    state: BiliNilUiState,
    viewModel: BiliNilViewModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = state.inputText,
            onValueChange = viewModel::updateInput,
            label = { Text(text = stringResource(R.string.bili_nil_input_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Button(
            onClick = viewModel::resolve,
            enabled = !state.isResolving && state.inputText.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
        ) {
            if (state.isResolving) {
                CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text(text = stringResource(R.string.bili_nil_resolving))
            } else {
                Text(text = stringResource(R.string.bili_nil_resolve))
            }
        }
        state.errorMessageRes?.let { messageRes ->
            BiliNilCard {
                Text(text = stringResource(messageRes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
        }
        state.details?.let { details ->
            BiliNilCard {
                state.preview?.let { bitmap -> CoverImage(bitmap = bitmap) }
                Text(text = details.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(text = details.input.displayName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                details.author?.let { author -> Text(text = author, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                details.uid?.let { uid -> Text(text = stringResource(R.string.bili_nil_uid, uid), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                details.description?.let { desc -> Text(text = desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Button(
                    onClick = viewModel::download,
                    enabled = !state.isDownloading && !state.isResolving,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp),
                ) {
                    if (state.isDownloading) {
                        CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Text(text = stringResource(R.string.bili_nil_downloading))
                    } else {
                        Text(text = stringResource(R.string.bili_nil_download))
                    }
                }
                state.savedFileName?.let { fileName ->
                    Text(text = stringResource(R.string.bili_nil_saved, fileName), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun BiliNilTabRow(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = themeContainerColor(),
        border = BorderStroke(1.dp, themeContainerBorderColor()),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            BiliNilTabItem(
                label = stringResource(R.string.bili_nil_tab_cover),
                isSelected = selectedIndex == 0,
                modifier = Modifier.weight(1f),
                onClick = { onTabSelected(0) },
            )
            BiliNilTabItem(
                label = stringResource(R.string.bili_nil_tab_video),
                isSelected = selectedIndex == 1,
                modifier = Modifier.weight(1f),
                onClick = { onTabSelected(1) },
            )
        }
    }
}

@Composable
private fun BiliNilTabItem(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun BiliNilCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = themeContainerColor(),
        border = BorderStroke(1.dp, themeContainerBorderColor()),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}

@Composable
private fun CoverImage(
    bitmap: androidx.compose.ui.graphics.ImageBitmap,
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
