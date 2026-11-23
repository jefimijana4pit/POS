package com.fourpit.marketpos.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fourpit.marketpos.i18n.LocalAppStrings
import com.fourpit.marketpos.model.Product

@Composable
fun ProductEditorDialog(
    locationId: Long,
    initial: Product?,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit,
    onDelete: ((Long) -> Unit)? = null
) {
    val strings = LocalAppStrings.current
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var category by remember(initial) { mutableStateOf(initial?.category.orEmpty()) }
    var sku by remember(initial) { mutableStateOf(initial?.sku.orEmpty()) }
    var priceText by remember(initial) { mutableStateOf(initial?.priceRsd?.toString().orEmpty()) }
    var stockText by remember(initial) { mutableStateOf((initial?.stockQuantity ?: 20).toString()) }
    var thresholdText by remember(initial) { mutableStateOf((initial?.lowStockThreshold ?: 10).toString()) }

    val parsedPrice = priceText.replace(",", ".").toDoubleOrNull()
    val parsedStock = stockText.toIntOrNull()
    val parsedThreshold = thresholdText.toIntOrNull() ?: 10
    val isValid = name.isNotBlank() && parsedPrice != null && parsedPrice >= 0.0 && parsedStock != null && parsedStock >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) strings.addProduct else strings.editProduct,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.productName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text(strings.category) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text(strings.sku) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text(strings.priceRsd) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stockText,
                        onValueChange = { stockText = it.filter(Char::isDigit) },
                        label = { Text(strings.stockQuantity) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = thresholdText,
                        onValueChange = { thresholdText = it.filter(Char::isDigit) },
                        label = { Text(strings.lowStockThreshold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        Product(
                            id = initial?.id ?: 0L,
                            locationId = locationId,
                            name = name.trim(),
                            category = category.trim().ifBlank { "General" },
                            sku = sku.trim(),
                            priceRsd = parsedPrice ?: 0.0,
                            stockQuantity = parsedStock ?: 0,
                            lowStockThreshold = parsedThreshold
                        )
                    )
                },
                enabled = isValid
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            Row {
                if (initial != null && onDelete != null) {
                    TextButton(onClick = { onDelete(initial.id) }) {
                        Text(strings.delete, color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(strings.cancel)
                }
            }
        }
    )
}
