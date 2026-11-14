package com.mapherdf.maplenilset.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mapherdf.maplenilset.R
import com.mapherdf.maplenilset.data.transfer.PageTransferMode

@Composable
fun HomePageTransferMenu(
    onExport: () -> Unit,
    onImport: () -> Unit,
    onExportJson: () -> Unit = {},
    onImportJson: () -> Unit = {},
    showJsonActions: Boolean = false,
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { isMenuExpanded = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_more),
                contentDescription = stringResource(R.string.page_transfer_more),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        DropdownMenu(
            expanded = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.page_transfer_export)) },
                onClick = {
                    isMenuExpanded = false
                    onExport()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.page_transfer_import)) },
                onClick = {
                    isMenuExpanded = false
                    onImport()
                },
            )
            if (showJsonActions) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.page_transfer_export_json)) },
                    onClick = {
                        isMenuExpanded = false
                        onExportJson()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.page_transfer_import_json)) },
                    onClick = {
                        isMenuExpanded = false
                        onImportJson()
                    },
                )
            }
        }
    }
}

@Composable
fun HomePageExportJsonDialog(
    onDismiss: () -> Unit,
    onConfirm: (isLienzo: Boolean) -> Unit,
) {
    var isLienzo by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.page_transfer_export_json_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.page_transfer_export_json_message))
                TransferModeRow(
                    title = stringResource(R.string.page_transfer_export_style_maple),
                    isSelected = !isLienzo,
                    onClick = { isLienzo = false },
                )
                TransferModeRow(
                    title = stringResource(R.string.page_transfer_export_style_lienzo),
                    isSelected = isLienzo,
                    onClick = { isLienzo = true },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(isLienzo) }) {
                Text(stringResource(R.string.page_transfer_export_json_confirm))
            }
        },
    )
}

@Composable
fun HomePageImportDialog(
    onDismiss: () -> Unit,
    onConfirm: (PageTransferMode) -> Unit,
) {
    var selectedMode by remember { mutableStateOf(PageTransferMode.OVERWRITE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.page_transfer_import_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.page_transfer_import_message))
                TransferModeRow(
                    title = stringResource(R.string.page_transfer_overwrite),
                    isSelected = selectedMode == PageTransferMode.OVERWRITE,
                    onClick = { selectedMode = PageTransferMode.OVERWRITE },
                )
                TransferModeRow(
                    title = stringResource(R.string.page_transfer_merge),
                    isSelected = selectedMode == PageTransferMode.MERGE,
                    onClick = { selectedMode = PageTransferMode.MERGE },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedMode) }) {
                Text(stringResource(R.string.page_transfer_confirm))
            }
        },
    )
}

@Composable
private fun TransferModeRow(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        RadioButton(selected = isSelected, onClick = null)
        Text(text = title)
    }
}
