package com.example.marketpos.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.marketpos.i18n.LocalStrings
import com.example.marketpos.model.AppLanguage
import com.example.marketpos.model.Product
import com.example.marketpos.model.ProductCategory
import com.example.marketpos.model.VenueMode
import com.example.marketpos.util.CurrencyFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProductEditorDialog(
    existing: Product?,
    venueMode: VenueMode,
    language: AppLanguage,
    rsdPerEur: Double,
    onDismiss: () -> Unit,
    onSave: (
        sku: String,
        nameSr: String,
        nameEn: String,
        category: ProductCategory,
        priceRsd: Double,
        unitSr: String,
        unitEn: String,
        stockQuantity: Int,
        lowStockThreshold: Int,
        isAvailable: Boolean
    ) -> Unit
) {
    val strings = LocalStrings.current
    val availableCategories = remember(venueMode) { ProductCategory.forVenue(venueMode) }
    var sku by rememberSaveable { mutableStateOf(existing?.sku ?: "") }
    var nameSr by rememberSaveable { mutableStateOf(existing?.nameSr ?: "") }
    var nameEn by rememberSaveable { mutableStateOf(existing?.nameEn ?: "") }
    var category by rememberSaveable {
        mutableStateOf(existing?.category ?: availableCategories.first())
    }
    var priceInput by rememberSaveable {
        mutableStateOf(
            existing?.let {
                CurrencyFormatter.formatEditableNumber(it.priceRsd, language, rsdPerEur)
            } ?: ""
        )
    }
    var unitSr by rememberSaveable { mutableStateOf(existing?.unitSr ?: "kom") }
    var unitEn by rememberSaveable { mutableStateOf(existing?.unitEn ?: "pcs") }
    var stockInput by rememberSaveable { mutableStateOf((existing?.stockQuantity ?: 20).toString()) }
    var thresholdInput by rememberSaveable { mutableStateOf((existing?.lowStockThreshold ?: 5).toString()) }
    var isAvailable by rememberSaveable { mutableStateOf(existing?.isAvailable ?: true) }
    var errorMsg by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existing == null) strings.addProductTitle else strings.editProductTitle)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = nameSr,
                    onValueChange = { nameSr = it; errorMsg = null },
                    label = { Text(strings.productNameSrLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { nameEn = it; errorMsg = null },
                    label = { Text(strings.productNameEnLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it; errorMsg = null },
                        label = { Text(strings.productSkuLabel) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { priceInput = it; errorMsg = null },
                        label = { Text(strings.productPriceLabel(strings.activeCurrencyLabel)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stockInput,
                        onValueChange = {
                            if (it.all(Char::isDigit)) {
                                stockInput = it
                                errorMsg = null
                            }
                        },
                        label = { Text(strings.productStockLabel) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = thresholdInput,
                        onValueChange = {
                            if (it.all(Char::isDigit)) {
                                thresholdInput = it
                                errorMsg = null
                            }
                        },
                        label = { Text(strings.productLowStockThresholdLabel) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = unitSr,
                        onValueChange = { unitSr = it },
                        label = { Text(strings.productUnitSrLabel) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unitEn,
                        onValueChange = { unitEn = it },
                        label = { Text(strings.productUnitEnLabel) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = strings.productCategoryLabel,
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    availableCategories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(strings.categoryName(cat)) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.productAvailableLabel,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = isAvailable,
                        onCheckedChange = { isAvailable = it }
                    )
                }

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
                    val parsedPrice = priceInput.replace(',', '.').toDoubleOrNull()
                    val parsedStock = stockInput.toIntOrNull()
                    val parsedThreshold = thresholdInput.toIntOrNull() ?: 5
                    if (nameSr.isBlank() || nameEn.isBlank() || sku.isBlank()) {
                        errorMsg = strings.validationRequiredError
                    } else if (parsedPrice == null || parsedPrice <= 0.0) {
                        errorMsg = strings.validationPriceError
                    } else if (parsedStock == null || parsedStock < 0) {
                        errorMsg = strings.validationStockError
                    } else {
                        val priceRsd = CurrencyFormatter.convertToRsd(parsedPrice, language, rsdPerEur)
                        onSave(
                            sku,
                            nameSr,
                            nameEn,
                            category,
                            priceRsd,
                            unitSr,
                            unitEn,
                            parsedStock,
                            parsedThreshold,
                            isAvailable
                        )
                    }
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
    )
}
