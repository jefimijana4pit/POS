package com.example.marketpos.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.TableRestaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.marketpos.i18n.LocalStrings
import com.example.marketpos.model.BarTable
import com.example.marketpos.model.sortedByAvailability
import com.example.marketpos.ui.components.DeleteActionButton
import com.example.marketpos.ui.components.EditActionButton
import com.example.marketpos.ui.components.EmptyStateView
import com.example.marketpos.ui.theme.JetBrainsMonoFamily
import com.example.marketpos.ui.theme.LocalSemanticColors
import com.example.marketpos.util.CurrencyFormatter
import com.example.marketpos.viewmodel.AdminBranchUiState

@Composable
internal fun AdminTablesTab(
    uiState: AdminBranchUiState,
    onEditTable: (BarTable) -> Unit,
    onDeleteTable: (BarTable) -> Unit,
    onForceUnlockTable: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val settings = uiState.settings
    val semanticColors = LocalSemanticColors.current
    val orderedTables = remember(uiState.tables) {
        uiState.tables.sortedByAvailability()
    }

    if (orderedTables.isEmpty()) {
        EmptyStateView(
            icon = Icons.Rounded.TableRestaurant,
            title = strings.noTablesDefined,
            modifier = modifier.fillMaxSize()
        )
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(orderedTables, key = { it.id }) { table ->
                val countdown = if (table.awaitingCleanup) {
                    CurrencyFormatter.formatCountdown(table.remainingCleanupMillis(uiState.nowMillis))
                } else {
                    null
                }
                val (statusBg, statusFg, statusText) = when {
                    table.awaitingCleanup && countdown != null -> Triple(
                        MaterialTheme.colorScheme.tertiaryContainer,
                        MaterialTheme.colorScheme.onTertiaryContainer,
                        "${strings.tableStatusAwaitingCleanup} ($countdown)"
                    )
                    table.isLocked -> Triple(
                        semanticColors.warningContainer,
                        semanticColors.onWarningContainer,
                        "${strings.tableStatusLockedByOther(table.lockedByWorkerName ?: "")} • ${
                            strings.tableBusyItemsSummary(table.itemCount)
                        }"
                    )
                    else -> Triple(
                        MaterialTheme.colorScheme.surfaceContainerHighest,
                        MaterialTheme.colorScheme.onSurfaceVariant,
                        strings.tableStatusFree
                    )
                }

                val itemBg = when {
                    table.awaitingCleanup -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.50f)
                    table.isLocked -> semanticColors.warningContainer.copy(alpha = 0.45f)
                    else -> MaterialTheme.colorScheme.surfaceContainerLow
                }
                val itemBorder = when {
                    table.awaitingCleanup -> BorderStroke(1.2.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.65f))
                    table.isLocked -> BorderStroke(1.2.dp, semanticColors.warning.copy(alpha = 0.65f))
                    else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = itemBg,
                    border = itemBorder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_table_row_${table.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = statusBg,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.TableRestaurant,
                                        contentDescription = null,
                                        tint = statusFg,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = table.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${table.zone} • ${strings.tableSeatsCount(table.capacity)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val canModify = !table.isLocked
                            EditActionButton(
                                onClick = { onEditTable(table) },
                                enabled = canModify,
                                testTag = "admin_edit_table_${table.id}"
                            )
                            DeleteActionButton(
                                onClick = { onDeleteTable(table) },
                                enabled = canModify,
                                testTag = "admin_delete_table_${table.id}"
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = statusBg,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    if (table.isLocked) {
                                        Icon(
                                            imageVector = Icons.Rounded.Lock,
                                            contentDescription = null,
                                            tint = statusFg,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = statusFg,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (table.isLocked) {
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = { onForceUnlockTable(table.id) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.LockOpen,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = strings.forceUnlockBtn,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            } else if (table.orderItems.isNotEmpty()) {
                                Text(
                                    text = CurrencyFormatter.format(
                                        table.orderTotalRsd,
                                        settings.language,
                                        settings.rsdPerEur
                                    ),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun TableEditorDialog(
    existing: BarTable?,
    onDismiss: () -> Unit,
    onSave: (name: String, zone: String, capacity: Int) -> Unit
) {
    val strings = LocalStrings.current
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var zone by rememberSaveable { mutableStateOf(existing?.zone ?: "Sala") }
    var capacityInput by rememberSaveable { mutableStateOf((existing?.capacity ?: 4).toString()) }
    var errorMsg by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existing == null) strings.addTableTitle else strings.editTableTitle)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMsg = null },
                    label = { Text(strings.tableNameLabel) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("table_name_input")
                )
                OutlinedTextField(
                    value = zone,
                    onValueChange = { zone = it; errorMsg = null },
                    label = { Text(strings.tableZoneLabel) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("table_zone_input")
                )
                OutlinedTextField(
                    value = capacityInput,
                    onValueChange = {
                        if (it.all(Char::isDigit)) {
                            capacityInput = it
                            errorMsg = null
                        }
                    },
                    label = { Text(strings.tableCapacityLabel) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("table_capacity_input")
                )

                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val seats = capacityInput.toIntOrNull()
                    if (name.isBlank() || zone.isBlank()) {
                        errorMsg = strings.validationRequiredError
                    } else if (seats == null || seats <= 0) {
                        errorMsg = strings.validationStockError
                    } else {
                        onSave(name, zone, seats)
                    }
                },
                modifier = Modifier.testTag("save_table_btn")
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
