package com.mapherdf.maplenilset.ui.bili

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt
import com.mapherdf.maplenilset.R
import com.mapherdf.maplenilset.bili.auth.BiliLoginLevel
import com.mapherdf.maplenilset.const.BiliSettings
import com.mapherdf.maplenilset.ui.theme.themeContainerBorderColor
import com.mapherdf.maplenilset.ui.theme.themeContainerColor

@Composable
fun BiliNilSettingsScreen(
    onNavigateBack: () -> Unit,
    onOpenBiliLogin: () -> Unit,
    viewModel: BiliVideoViewModel = hiltViewModel(),
) {
    val concurrentTasks by viewModel.maxConcurrentTasks.collectAsStateWithLifecycle()
    val loginState by viewModel.biliLoginState.collectAsStateWithLifecycle()
    val avatar by viewModel.biliAvatar.collectAsStateWithLifecycle()
    var sliderValue by remember(concurrentTasks) {
        mutableFloatStateOf(concurrentTasks.toFloat())
    }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LaunchedEffect(Unit) {
                viewModel.refreshBiliLoginState()
            }
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Surface(
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, themeContainerBorderColor()),
            color = themeContainerColor(),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = !loginState.isValidating && loginState.level == BiliLoginLevel.LOGGED_OUT,
                    onClick = onOpenBiliLogin,
                ),
        ) {
            ListItem(
                headlineContent = {
                    Text(
                        text = loginState.nickname
                            ?: stringResource(R.string.bili_nil_login_entry),
                    )
                },
                supportingContent = {
                    Text(
                        text = if (loginState.isValidating) {
                            stringResource(R.string.bili_nil_login_checking)
                        } else {
                            when (loginState.level) {
                                BiliLoginLevel.LOGGED_OUT -> stringResource(R.string.bili_nil_login_logged_out)
                                BiliLoginLevel.NORMAL_USER -> stringResource(R.string.bili_nil_login_normal_user)
                                BiliLoginLevel.VIP_MEMBER -> stringResource(R.string.bili_nil_login_vip_member)
                            }
                        },
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                leadingContent = {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (avatar != null) {
                            Image(
                                    bitmap = avatar!!,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.ic_account),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }
                },
                trailingContent = {
                    if (loginState.level != BiliLoginLevel.LOGGED_OUT) {
                        TextButton(onClick = { showLogoutConfirm = true }) {
                            Text(stringResource(R.string.bili_nil_login_logout))
                        }
                    }
                },
            )
        }
        Surface(
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, themeContainerBorderColor()),
            color = themeContainerColor(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.bili_nil_concurrent_tasks)) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingContent = {
                        Text(
                            stringResource(
                                R.string.bili_nil_concurrent_tasks_value,
                                sliderValue.roundToInt(),
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
                Slider(
                    value = sliderValue,
                    onValueChange = { value -> sliderValue = value },
                    onValueChangeFinished = {
                        sliderValue = sliderValue.roundToInt().toFloat()
                        viewModel.setMaxConcurrentTasks(sliderValue.roundToInt())
                    },
                    valueRange = BiliSettings.MIN_CONCURRENT_TASKS.toFloat()..BiliSettings.MAX_CONCURRENT_TASKS.toFloat(),
                    steps = BiliSettings.MAX_CONCURRENT_TASKS - BiliSettings.MIN_CONCURRENT_TASKS - 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text(stringResource(R.string.bili_nil_login_logout)) },
            text = { Text(stringResource(R.string.bili_nil_login_logout_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    viewModel.logoutFromBili()
                }) {
                    Text(stringResource(R.string.bili_nil_login_logout), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text(stringResource(R.string.bili_nil_cancel))
                }
            },
        )
    }
}
