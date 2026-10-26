package com.mapherdf.maplenilset.ui.main

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.mapherdf.maplenilset.R
import com.mapherdf.maplenilset.const.AppRoutes
import com.mapherdf.maplenilset.ui.theme.themeContainerColor


@Composable
fun AppBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = themeContainerColor(),
    ) {
        NavigationBarItem(
            selected = currentRoute == AppRoutes.Tab.HOME,
            onClick = { onNavigate(AppRoutes.Tab.HOME) },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_home),
                    contentDescription = null,
                )
            },
            label = { Text(stringResource(R.string.drawer_home)) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
            ),
        )
        NavigationBarItem(
            selected = currentRoute == AppRoutes.Tab.SETTINGS,
            onClick = { onNavigate(AppRoutes.Tab.SETTINGS) },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = null,
                )
            },
            label = { Text(stringResource(R.string.settings_title)) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
            ),
        )
    }
}
