package com.mapherdf.maplenilset

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import dagger.hilt.android.AndroidEntryPoint
import com.mapherdf.maplenilset.const.AppRoutes
import com.mapherdf.maplenilset.ui.console.ConsoleScreen
import com.mapherdf.maplenilset.ui.console.ConsoleSettingsScreen
import com.mapherdf.maplenilset.ui.main.MainScreen
import com.mapherdf.maplenilset.ui.main.popBackStackIfCurrent
import com.mapherdf.maplenilset.ui.settings.ThemeSettingsScreen
import com.mapherdf.maplenilset.ui.settings.CustomSettingsScreen
import com.mapherdf.maplenilset.ui.settings.NotificationSettingsScreen
import com.mapherdf.maplenilset.ui.settings.BackgroundCropScreen
import com.mapherdf.maplenilset.ui.settings.AboutScreen
import com.mapherdf.maplenilset.ui.session.ThemeViewModel
import com.mapherdf.maplenilset.ui.theme.ATOMTheme
import com.mapherdf.maplenilset.ui.theme.rememberCustomBackgroundImage

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val themeSettings by themeViewModel.themeSettings.collectAsStateWithLifecycle()
            val isThemeReady by themeViewModel.isThemeReady.collectAsStateWithLifecycle()

            ATOMTheme(settings = themeSettings) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val backgroundImage = rememberCustomBackgroundImage(themeSettings.backgroundImageUri)
                    if (!isThemeReady) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                        return@Surface
                    }

                    val snackbarHostState = SnackbarHostState()
                    val navController = rememberNavController()
                    val startDestination = AppRoutes.MAIN
                    val currentRoute by navController.currentBackStackEntryAsState()

                    Box(modifier = Modifier.fillMaxSize()) {
                        val currentDestinationRoute = currentRoute?.destination?.route
                        val isConsoleBackgroundRoute = currentDestinationRoute == AppRoutes.CONSOLE ||
                            currentDestinationRoute == AppRoutes.CONSOLE_SETTINGS
                        val showsBackgroundImage = backgroundImage != null &&
                            !(isConsoleBackgroundRoute && !themeSettings.showConsoleBackground)

                        AnimatedVisibility(
                            visible = showsBackgroundImage,
                            enter = fadeIn(animationSpec = tween(durationMillis = 240)),
                            exit = fadeOut(animationSpec = tween(durationMillis = 240)),
                            modifier = Modifier.matchParentSize(),
                        ) {
                            backgroundImage?.let { image ->
                                Image(
                                    bitmap = image,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    alpha = themeSettings.backgroundOpacity,
                                    modifier = Modifier.matchParentSize(),
                                )
                            }
                        }
                        NavHost(
                            navController = navController,
                            startDestination = startDestination,
                        ) {
                            composable(AppRoutes.THEME_SETTINGS) {
                                ThemeSettingsScreen(
                                    onNavigateBack = {
                                        navController.popBackStackIfCurrent(AppRoutes.THEME_SETTINGS)
                                    },
                                    onSelectBackgroundImage = { sourceUri ->
                                        navController.navigate(AppRoutes.backgroundCrop(sourceUri))
                                    },
                                )
                            }
                            composable(AppRoutes.NOTIFICATION_SETTINGS) {
                                NotificationSettingsScreen(
                                    onNavigateBack = {
                                        navController.popBackStackIfCurrent(AppRoutes.NOTIFICATION_SETTINGS)
                                    },
                                )
                            }
                            composable(AppRoutes.CUSTOM_SETTINGS) {
                                CustomSettingsScreen(
                                    onNavigateBack = {
                                        navController.popBackStackIfCurrent(AppRoutes.CUSTOM_SETTINGS)
                                    },
                                )
                            }
                            composable(AppRoutes.ABOUT) {
                                AboutScreen(
                                    onNavigateBack = {
                                        navController.popBackStackIfCurrent(AppRoutes.ABOUT)
                                    },
                                )
                            }
                            composable(
                                route = AppRoutes.BACKGROUND_CROP,
                                arguments = listOf(
                                    navArgument("sourceUri") {
                                        type = NavType.StringType
                                    },
                                ),
                            ) { entry ->
                                BackgroundCropScreen(
                                    sourceUri = entry.arguments?.getString("sourceUri").orEmpty(),
                                    onApplied = {
                                        navController.popBackStackIfCurrent(AppRoutes.BACKGROUND_CROP)
                                    },
                                    onDiscard = {
                                        navController.popBackStackIfCurrent(AppRoutes.BACKGROUND_CROP)
                                    },
                                )
                            }
                            composable(AppRoutes.CONSOLE) {
                                ConsoleScreen(
                                    onNavigateBack = {
                                        navController.popBackStackIfCurrent(AppRoutes.CONSOLE)
                                    },
                                    onOpenSettings = { navController.navigate(AppRoutes.CONSOLE_SETTINGS) },
                                )
                            }
                            composable(AppRoutes.CONSOLE_SETTINGS) {
                                ConsoleSettingsScreen(
                                    onNavigateBack = {
                                        navController.popBackStackIfCurrent(AppRoutes.CONSOLE_SETTINGS)
                                    },
                                )
                            }
                            composable(AppRoutes.MAIN) {
                                MainScreen(
                                    onOpenConsole = {
                                        navController.navigate(AppRoutes.CONSOLE) {
                                            launchSingleTop = true
                                            popUpTo(AppRoutes.MAIN) {
                                                saveState = true
                                            }
                                            restoreState = true
                                        }
                                    },
                                    onOpenNotificationSettings = {
                                        navController.navigate(AppRoutes.NOTIFICATION_SETTINGS)
                                    },
                                    onOpenThemeSettings = {
                                        navController.navigate(AppRoutes.THEME_SETTINGS)
                                    },
                                    onOpenCustomSettings = {
                                        navController.navigate(AppRoutes.CUSTOM_SETTINGS)
                                    },
                                    onOpenAbout = {
                                        navController.navigate(AppRoutes.ABOUT)
                                    },
                                )
                            }
                        }
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .padding(top = 72.dp, start = 16.dp, end = 16.dp),
                        ) { data ->
                            Surface(
                                modifier = Modifier.widthIn(max = 280.dp),
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.inverseSurface,
                                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = data.visuals.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
