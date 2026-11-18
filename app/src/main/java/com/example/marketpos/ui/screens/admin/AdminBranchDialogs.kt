package com.example.marketpos.ui.screens.admin

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import com.example.marketpos.i18n.LocalStrings
import com.example.marketpos.model.BarTable
import com.example.marketpos.model.Product
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
