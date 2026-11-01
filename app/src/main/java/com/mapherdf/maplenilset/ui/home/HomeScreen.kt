package com.mapherdf.maplenilset.ui.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.mapherdf.maplenilset.R
import com.mapherdf.maplenilset.core.navigation.HomeDestination
import com.mapherdf.maplenilset.ui.settings.CustomSettingsViewModel
import com.mapherdf.maplenilset.ui.calendar.CalendarScreen
import com.mapherdf.maplenilset.ui.bili.BiliNilScreen
import com.mapherdf.maplenilset.ui.schedule.ScheduleScreen
import com.mapherdf.maplenilset.ui.color.LienzoColorScreen
import com.mapherdf.maplenilset.ui.markdown.MarkdownText
import com.mapherdf.maplenilset.ui.theme.themeContainerColor
import com.mapherdf.maplenilset.ui.theme.themeDrawerMaskColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    isVisible: Boolean,
    onAddColor: () -> Unit = {},
    onEditColor: (String) -> Unit = {},
    onViewColor: (String) -> Unit = {},
    onColorCopy: (String) -> Unit = {},
) {
    val customViewModel: CustomSettingsViewModel = hiltViewModel()
    val uiSettings by customViewModel.uiSettings.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()
    var drawerAnimationJob by remember { mutableStateOf<Job?>(null) }
    var selectedDestinationName by rememberSaveable { mutableStateOf<String?>(null) }
    val visibleDestinations = uiSettings.visiblePages
    val selectedDestination = selectedDestinationName
        ?.let { name -> runCatching { HomeDestination.valueOf(name) }.getOrNull() }
        ?.takeIf { it in visibleDestinations }
        ?: uiSettings.startupPage.takeIf { it in visibleDestinations }
        ?: visibleDestinations.firstOrNull()
        ?: HomeDestination.HOME
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f),
        drawerContent = {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val drawerWidth = minOf(maxWidth * 0.80f, 320.dp)
                val blankWidth = (maxWidth - drawerWidth).coerceAtLeast(0.dp)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(drawerWidth)
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

                        visibleDestinations.forEach { destination ->
                            NavigationDrawerItem(
                                label = { Text(stringResource(destination.titleRes())) },
                                selected = selectedDestination == destination,
                                shape = MaterialTheme.shapes.extraSmall,
                                icon = {
                                    DrawerIcon(destination.iconRes(), destination == selectedDestination)
                                },
                                onClick = {
                                    selectedDestinationName = destination.name
                                    animateDrawer(DrawerValue.Closed)
                                },
                                modifier = Modifier.padding(horizontal = 12.dp),
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = if (selectedDestination == destination) {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                    } else {
                                        Color.Transparent
                                    },
                                ),
                            )
                        }
                    }
                }

                if (drawerState.targetValue == DrawerValue.Open) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .width(blankWidth)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                animateDrawer(DrawerValue.Closed)
                            },
                    )
                }
            }
        },
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(selectedDestination.titleRes())) },
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
                when (selectedDestination) {
                    HomeDestination.CALENDAR -> CalendarScreen()
                    HomeDestination.SCHEDULE -> ScheduleScreen()
                    HomeDestination.BILI -> BiliNilScreen()
                    HomeDestination.LIENZO -> LienzoColorScreen(
                        onAddColor = onAddColor,
                        onEditColor = onEditColor,
                        onViewColor = onViewColor,
                        onCopyMessage = onColorCopy,
                    )
                    HomeDestination.HOME -> HomeTextContent()
                    HomeDestination.NOTE -> PlaceholderDestinationContent(
                        text = stringResource(R.string.home_note_empty),
                    )
                    HomeDestination.TODO -> PlaceholderDestinationContent(
                        text = stringResource(R.string.home_todo_empty),
                    )
                }
            }
        }
    }
}

private fun HomeDestination.titleRes(): Int = when (this) {
    HomeDestination.HOME -> R.string.drawer_home
    HomeDestination.CALENDAR -> R.string.drawer_calendar
    HomeDestination.NOTE -> R.string.drawer_note
    HomeDestination.TODO -> R.string.drawer_todo
    HomeDestination.SCHEDULE -> R.string.drawer_schedule
    HomeDestination.BILI -> R.string.drawer_bili_nil
    HomeDestination.LIENZO -> R.string.drawer_lienzo_color
}

@Composable
private fun PlaceholderDestinationContent(
    text: String,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun HomeDestination.iconRes(): Int = when (this) {
    HomeDestination.HOME -> R.drawable.ic_home
    HomeDestination.CALENDAR -> R.drawable.ic_event
    HomeDestination.NOTE -> R.drawable.ic_note
    HomeDestination.TODO -> R.drawable.ic_todo
    HomeDestination.SCHEDULE -> R.drawable.ic_schedule
    HomeDestination.BILI -> R.drawable.ic_bilibili
    HomeDestination.LIENZO -> R.drawable.ic_color
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
        when (val state = textState) {
            HomeTextUiState.Loading -> Text(
                text = stringResource(R.string.home_text_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            is HomeTextUiState.Success -> MarkdownText(
                markdown = state.text,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            )
            HomeTextUiState.Error -> Text(
                text = stringResource(R.string.home_text_load_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private fun parseInlineMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    var index = 0
    while (index < text.length) {
        when {
            text.startsWith("**", index) -> {
                val end = text.indexOf("**", index + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(index + 2, end))
                    }
                    index = end + 2
                } else {
                    append(text[index])
                    index++
                }
            }
            text[index] == '_' && (index == 0 || text[index - 1].isWhitespace()) -> {
                val end = text.indexOf('_', index + 1)
                if (end != -1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(index + 1, end))
                    }
                    index = end + 1
                } else {
                    append(text[index])
                    index++
                }
            }
            else -> {
                append(text[index])
                index++
            }
        }
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
