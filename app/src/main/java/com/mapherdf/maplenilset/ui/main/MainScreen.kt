package com.mapherdf.maplenilset.ui.main

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mapherdf.maplenilset.const.AppRoutes
import com.mapherdf.maplenilset.R
import com.mapherdf.maplenilset.core.theme.SidebarBlurStyle
import com.mapherdf.maplenilset.ui.home.HomeScreen
import com.mapherdf.maplenilset.ui.settings.SettingsScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val PageSwitchSpec = tween<Float>(
    durationMillis = 360,
    easing = FastOutSlowInEasing,
)

private fun routePage(route: String) = if (route == AppRoutes.Tab.SETTINGS) 1 else 0

@Composable
fun MainScreen(
    onOpenConsole: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenThemeSettings: () -> Unit,
    onOpenCustomSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onAddColor: () -> Unit,
    onEditColor: (String) -> Unit,
    onViewColor: (String) -> Unit,
    sidebarBlurStyle: SidebarBlurStyle = SidebarBlurStyle.NONE,
) {
    var selectedRoute by rememberSaveable { mutableStateOf(AppRoutes.Tab.HOME) }
    var colorSnackbarBarHeight by remember { mutableIntStateOf(0) }
    val colorSnackbarHostState = remember { SnackbarHostState() }
    val colorSnackbarScope = rememberCoroutineScope()
    val context = LocalContext.current
    val transferExportedMessage = stringResource(R.string.page_transfer_exported)
    val exportFileDoneMessage = stringResource(R.string.page_transfer_export_file_done)
    val viewFileActionLabel = stringResource(R.string.page_transfer_view_file)
    val targetPage = routePage(selectedRoute)
     val pageProgress by animateFloatAsState(
        targetValue = targetPage.toFloat(),
        animationSpec = PageSwitchSpec,
        label = "mainPageProgress",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clipToBounds(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = size.width * -pageProgress
                    },
            ) {
                HomeScreen(
                    isVisible = selectedRoute == AppRoutes.Tab.HOME,
                    sidebarBlurStyle = sidebarBlurStyle,
                    onAddColor = onAddColor,
                    onEditColor = onEditColor,
                    onViewColor = onViewColor,
                    onColorCopy = { message ->
                        colorSnackbarScope.launch {
                            colorSnackbarHostState.showSnackbar(message)
                        }
                    },
                    onTransferMessage = { message ->
                        colorSnackbarScope.launch {
                            colorSnackbarHostState.showSnackbar(message)
                        }
                    },
                    onTransferFileExported = { uri ->
                        colorSnackbarScope.launch {
                            val result = colorSnackbarHostState.showSnackbar(
                                message = exportFileDoneMessage,
                                actionLabel = viewFileActionLabel,
                                duration = SnackbarDuration.Long,
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                try {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "application/json")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        },
                                    )
                                } catch (_: ActivityNotFoundException) {
                                }
                            }
                        }
                    },
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = size.width * (1f - pageProgress)
                    },
            ) {
                SettingsScreen(
                    onOpenConsole = onOpenConsole,
                    onOpenNotificationSettings = onOpenNotificationSettings,
                    onOpenThemeSettings = onOpenThemeSettings,
                    onOpenCustomSettings = onOpenCustomSettings,
                    onOpenAbout = onOpenAbout,
                )
            }
        }
        AppBottomBar(
            currentRoute = selectedRoute,
            onNavigate = { route -> selectedRoute = route },
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { size -> colorSnackbarBarHeight = size.height },
        )
        }
        val colorSnackbarBottomSpacing = with(LocalDensity.current) {
            colorSnackbarBarHeight.toDp() + 12.dp
        }
        SnackbarHost(
            hostState = colorSnackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = colorSnackbarBottomSpacing),
            snackbar = { snackbarData ->
                if (snackbarData.visuals.actionLabel == null) {
                    LaunchedEffect(snackbarData) {
                        delay(1_500L)
                        snackbarData.dismiss()
                    }
                }
                Snackbar(snackbarData)
            },
        )
    }
}
