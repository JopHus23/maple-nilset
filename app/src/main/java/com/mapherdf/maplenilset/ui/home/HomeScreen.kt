package com.mapherdf.maplenilset.ui.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.mapherdf.maplenilset.R
import com.mapherdf.maplenilset.ui.calendar.CalendarScreen
import com.mapherdf.maplenilset.ui.bili.BiliNilScreen
import com.mapherdf.maplenilset.ui.schedule.ScheduleScreen
import com.mapherdf.maplenilset.ui.theme.themeContainerColor
import com.mapherdf.maplenilset.ui.theme.themeDrawerMaskColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    isVisible: Boolean,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()
    var drawerAnimationJob by remember { mutableStateOf<Job?>(null) }
    var selectedDestination by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(isVisible) {
        if (!isVisible) {
            drawerAnimationJob?.cancelAndJoin()
            drawerAnimationJob = drawerScope.launch { drawerState.close() }
        }
    }

    fun animateDrawer(targetValue: DrawerValue) {
        val previousJob = drawerAnimationJob
        drawerAnimationJob = drawerScope.launch {
            previousJob?.cancelAndJoin()
            if (targetValue == DrawerValue.Open) {
                drawerState.openElastic()
            } else {
                drawerState.close()
            }
        }
    }

    val destinationTitles = listOf(
        stringResource(R.string.drawer_home),
        stringResource(R.string.drawer_calendar),
        stringResource(R.string.drawer_note),
        stringResource(R.string.drawer_todo),
        stringResource(R.string.drawer_schedule),
        stringResource(R.string.drawer_bili_nil),
    )
    val destinationDetails = listOf(
        stringResource(R.string.home_empty),
        stringResource(R.string.calendar_empty),
        stringResource(R.string.home_note_empty),
        stringResource(R.string.home_todo_empty),
        stringResource(R.string.home_schedule_empty),
        stringResource(R.string.bili_nil_description),
    )
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !drawerState.isAnimationRunning,
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f),
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .fillMaxWidth(0.80f)
                    .widthIn(max = 320.dp),
                drawerContainerColor = Color.Transparent,
                windowInsets = WindowInsets(0),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(themeDrawerMaskColor()),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_nilset),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.drawer_header_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        destinationTitles.forEachIndexed { index, title ->
                            NavigationDrawerItem(
                                label = { Text(title) },
                                selected = selectedDestination == index,
                                shape = MaterialTheme.shapes.extraSmall,
                                icon = {
                                    when (index) {
                                        0 -> DrawerIcon(R.drawable.ic_home, index == selectedDestination)
                                        1 -> DrawerIcon(R.drawable.ic_event, index == selectedDestination)
                                        2 -> DrawerIcon(R.drawable.ic_note, index == selectedDestination)
                                        3 -> DrawerIcon(R.drawable.ic_todo, index == selectedDestination)
                                        4 -> DrawerIcon(R.drawable.ic_schedule, index == selectedDestination)
                                        else -> DrawerIcon(R.drawable.ic_bilibili, index == selectedDestination, useBrandColor = true)
                                    }
                                },
                                onClick = {
                                    selectedDestination = index
                                    animateDrawer(DrawerValue.Closed)
                                },
                                modifier = Modifier.padding(horizontal = 12.dp),
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = if (selectedDestination == index) {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                    } else {
                                        Color.Transparent
                                    },
                                ),
                            )
                        }
                    }
                }
            }
        },
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(destinationTitles[selectedDestination]) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = themeContainerColor(),
                    ),
                    navigationIcon = {
                    IconButton(onClick = { animateDrawer(DrawerValue.Open) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_menu),
                                contentDescription = stringResource(R.string.drawer_open),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
            if (selectedDestination == 1) {
                    CalendarScreen()
            } else if (selectedDestination == 4) {
                ScheduleScreen()
            } else if (selectedDestination == 5) {
                BiliNilScreen()
            } else if (selectedDestination == 0) {
                HomeTextContent()
            } else {
                    Text(
                        text = destinationDetails[selectedDestination],
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeTextContent(
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val textState by viewModel.homeText.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(
            text = when (textState) {
                HomeTextUiState.Loading -> stringResource(R.string.home_text_loading)
                is HomeTextUiState.Success -> (textState as HomeTextUiState.Success).text
                HomeTextUiState.Error -> stringResource(R.string.home_text_load_failed)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        )
    }
}

@Suppress("DEPRECATION")
private suspend fun DrawerState.openElastic() {
    animateTo(
        targetValue = DrawerValue.Open,
        anim = spring<Float>(
            dampingRatio = 0.86f,
            stiffness = 380f,
        ),
    )
}

@Composable
private fun DrawerIcon(
    iconResource: Int,
    isSelected: Boolean,
    useBrandColor: Boolean = false,
) {
    Icon(
        painter = painterResource(iconResource),
        contentDescription = null,
        tint = when {
            useBrandColor -> Color(0xFFFB7299)
            isSelected -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
}
