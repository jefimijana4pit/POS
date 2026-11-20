package com.example.marketpos.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.example.marketpos.model.AppLanguage
import com.example.marketpos.model.BarTable
import com.example.marketpos.model.Product
import com.example.marketpos.ui.theme.JetBrainsMonoFamily
import com.example.marketpos.util.CurrencyFormatter

@Composable
internal fun DeleteTableConfirmDialog(
    table: BarTable,
    onDismiss: () -> Unit,
    onConfirmDelete: (String) -> Unit
) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.deleteTableTitle) },
        text = { Text(strings.deleteTableConfirm(table.name)) },
        confirmButton = {
            Button(
                onClick = { onConfirmDelete(table.id) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(strings.delete)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
internal fun DeleteProductConfirmDialog(
    product: Product,
    productDisplayName: String,
    onDismiss: () -> Unit,
    onConfirmDelete: (String) -> Unit
) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.deleteProductTitle) },
        text = { Text(strings.deleteProductConfirm(productDisplayName)) },
        confirmButton = {
            Button(
                onClick = { onConfirmDelete(product.id) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(strings.delete)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdminDashboardDatePickerDialog(
    selectedDashboardDayMillis: Long?,
    onDismiss: () -> Unit,
    onSelectDate: (Long) -> Unit
) {
    val strings = LocalStrings.current
    val initialLocalDay = selectedDashboardDayMillis ?: System.currentTimeMillis()
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = CurrencyFormatter.localDayStartToUtcDatePickerMillis(initialLocalDay)
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    datePickerState.selectedDateMillis?.let { utcMillis ->
                        onSelectDate(CurrencyFormatter.utcDatePickerMillisToLocalDayStart(utcMillis))
                    }
                    onDismiss()
                }
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
internal fun AddStockDialog(
    product: Product,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onConfirmAdd: (quantityToAdd: Int) -> Unit
) {
    val strings = LocalStrings.current
    var quantityInput by rememberSaveable(product.id) { mutableStateOf("10") }
    var showError by rememberSaveable(product.id) { mutableStateOf(false) }

    val parsedQty = quantityInput.toIntOrNull()
    val isValid = parsedQty != null && parsedQty > 0
    val unit = product.displayUnit(language)
    val presets = listOf(5, 10, 20, 50, 100)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addStockTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = product.displayName(language),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${strings.currentStockLabel}: ${product.stockQuantity} $unit",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = quantityInput,
                    onValueChange = { raw ->
                        if (raw.all(Char::isDigit) && raw.length <= 6) {
                            quantityInput = raw
                            showError = false
                        }
                    },
                    label = { Text(strings.addStockQuantityLabel) },
                    singleLine = true,
                    isError = showError && !isValid,
                    supportingText = if (showError && !isValid) {
                        { Text(strings.validationStockError) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_stock_quantity_input")
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presets) { preset ->
                        AssistChip(
                            onClick = {
                                quantityInput = preset.toString()
                                showError = false
                            },
                            label = {
                                Text(
                                    text = "+$preset",
                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily)
                                )
                            },
                            modifier = Modifier.testTag("add_stock_preset_$preset")
                        )
                    }
                }

                if (isValid) {
                    val updatedStock = product.stockQuantity + parsedQty!!
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.newStockLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${product.stockQuantity} → $updatedStock $unit",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        onConfirmAdd(parsedQty!!)
                    } else {
                        showError = true
                    }
                },
                modifier = Modifier.testTag("confirm_add_stock_btn")
            ) {
                Text(strings.add)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
