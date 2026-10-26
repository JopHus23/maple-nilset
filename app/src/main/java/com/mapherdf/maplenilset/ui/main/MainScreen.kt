package com.mapherdf.maplenilset.ui.main

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import com.mapherdf.maplenilset.const.AppRoutes
import com.mapherdf.maplenilset.ui.home.HomeScreen
import com.mapherdf.maplenilset.ui.settings.SettingsScreen

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
) {
    var selectedRoute by rememberSaveable { mutableStateOf(AppRoutes.Tab.HOME) }
    val targetPage = routePage(selectedRoute)
     val pageProgress by animateFloatAsState(
        targetValue = targetPage.toFloat(),
        animationSpec = PageSwitchSpec,
        label = "mainPageProgress",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
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
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
