package com.mapherdf.maplenilset.ui.console

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.mapherdf.maplenilset.ui.theme.themeContainerColor
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.mapherdf.maplenilset.core.logging.ConsoleEntry
import com.mapherdf.maplenilset.core.logging.LogLevel
import com.mapherdf.maplenilset.R
import com.mapherdf.maplenilset.ui.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsoleScreen(
    viewModel: ConsoleViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val themeSettings by settingsViewModel.themeSettings.collectAsStateWithLifecycle()
    var commandInput by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    val consoleScrollState = rememberScrollState()
    var consoleInputFocused by remember { mutableStateOf(false) }
    var consoleAtBottom by remember { mutableStateOf(true) }
    var commandSuggestionsExpanded by rememberSaveable { mutableStateOf(false) }
    val commandSuggestions = viewModel.commandSuggestionsFor(
        input = commandInput.text,
        cursor = commandInput.selection.min,
    )
    val latestConsoleEntryTime = uiState.entries.lastOrNull()?.timestampMillis ?: 0L

    LaunchedEffect(consoleScrollState) {
        snapshotFlow { consoleScrollState.value }
            .collect { value ->
                consoleAtBottom = isConsoleScrolledToBottom(
                    scrollValue = value,
                    maxValue = consoleScrollState.maxValue,
                )
            }
    }

    LaunchedEffect(uiState.entries.size, latestConsoleEntryTime) {
        if (consoleAtBottom) {
            consoleScrollState.animateScrollTo(consoleScrollState.maxValue)
        }
    }

    LaunchedEffect(consoleInputFocused, consoleScrollState.maxValue) {
        if (consoleInputFocused && consoleAtBottom) {
            consoleScrollState.animateScrollTo(consoleScrollState.maxValue)
        }
    }
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.console_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = themeContainerColor(),
                ),
                navigationIcon = {
                    IconButton(
                        onClick = {
                            onNavigateBack()
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = stringResource(R.string.console_settings),
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
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(consoleScrollState),
            ) {
                uiState.entries.forEach { entry ->
                    Text(
                        text = entry.displayText(),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = themeSettings.effectiveConsoleOutputFontSize.sp,
                        color = entry.logColor(),
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            ExposedDropdownMenuBox(
                expanded = commandSuggestionsExpanded && commandSuggestions.isNotEmpty(),
                onExpandedChange = { commandSuggestionsExpanded = it },
                modifier = Modifier.weight(1f),
            ) {
                OutlinedTextField(
                    value = commandInput,
                    onValueChange = { input ->
                        commandInput = input
                        commandSuggestionsExpanded = input.text.startsWith("/")
                    },
                    label = { Text(stringResource(R.string.console_input_hint)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            consoleInputFocused = focusState.isFocused
                        }
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                    shape = MaterialTheme.shapes.medium,
                )
                ExposedDropdownMenu(
                    expanded = commandSuggestionsExpanded && commandSuggestions.isNotEmpty(),
                    onDismissRequest = { commandSuggestionsExpanded = false },
                ) {
                    commandSuggestions.forEach { suggestion ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = suggestion.displayText,
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                    Text(
                                        text = suggestion.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                            onClick = {
                                val sourceText = commandInput.text
                                val replacementEnd = suggestion.replacementEnd
                                    .coerceIn(suggestion.replacementStart, sourceText.length)
                                val replacement = suggestion.displayText +
                                    if (suggestion.appendSpace) " " else ""
                                val nextText = sourceText.replaceRange(
                                    startIndex = suggestion.replacementStart,
                                    endIndex = replacementEnd,
                                    replacement = replacement,
                                )
                                val nextCursor = suggestion.replacementStart + replacement.length
                                commandInput = TextFieldValue(
                                    text = nextText,
                                    selection = TextRange(nextCursor),
                                )
                                commandSuggestionsExpanded = nextText.startsWith("/")
                            },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    viewModel.onCommandSent(commandInput.text)
                    commandInput = TextFieldValue("")
                },
                modifier = Modifier
                    .height(56.dp)
                    .offset(y = 4.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(stringResource(R.string.btn_send))
            }
        }
        }
    }
}

@Composable
private fun ConsoleEntry.logColor(): Color = when (level) {
    LogLevel.DEBUG -> Color(0xFF90A4AE)
    LogLevel.INFO -> MaterialTheme.colorScheme.onBackground
    LogLevel.SUCCESS -> Color(0xFF66BB6A)
    LogLevel.WARNING -> Color(0xFFFFB74D)
    LogLevel.ERROR -> Color(0xFFEF5350)
}
