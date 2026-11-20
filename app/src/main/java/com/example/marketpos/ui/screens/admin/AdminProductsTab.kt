package com.example.marketpos.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.marketpos.i18n.LocalStrings
import com.example.marketpos.model.Product
import com.example.marketpos.ui.components.CategoryAvatar
import com.example.marketpos.ui.components.DeleteActionButton
import com.example.marketpos.ui.components.EditActionButton
import com.example.marketpos.ui.components.EmptyStateView
import com.example.marketpos.ui.theme.JetBrainsMonoFamily
import com.example.marketpos.util.CurrencyFormatter
import com.example.marketpos.viewmodel.AdminBranchUiState

private enum class AdminStockFilter {
    ALL,
    LOW,
    OUT
}

@Composable
internal fun AdminProductsTab(
    uiState: AdminBranchUiState,
    onAdjustStock: (String, Int) -> Unit,
    onEditProduct: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val settings = uiState.settings
    var query by rememberSaveable { mutableStateOf("") }
    var stockFilter by rememberSaveable { mutableStateOf(AdminStockFilter.ALL) }

    val filtered = remember(uiState.products, query, stockFilter) {
        val q = query.trim().lowercase()
        uiState.products.filter { product ->
            val matchesQuery = q.isEmpty() ||
                product.nameSr.lowercase().contains(q) ||
                product.nameEn.lowercase().contains(q) ||
                product.sku.lowercase().contains(q)
            val matchesStock = when (stockFilter) {
                AdminStockFilter.ALL -> true
                AdminStockFilter.LOW -> product.isLowStock
                AdminStockFilter.OUT -> !product.isInStock
            }
            matchesQuery && matchesStock
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(strings.searchProducts) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Rounded.Clear, contentDescription = strings.close)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = stockFilter == AdminStockFilter.ALL,
                    onClick = { stockFilter = AdminStockFilter.ALL },
                    label = { Text("${strings.all} (${uiState.products.size})") }
                )
            }
            item {
                FilterChip(
                    selected = stockFilter == AdminStockFilter.LOW,
                    onClick = {
                        stockFilter = if (stockFilter == AdminStockFilter.LOW) {
                            AdminStockFilter.ALL
                        } else {
                            AdminStockFilter.LOW
                        }
                    },
                    label = {
                        Text("${strings.lowStockFilter} (${uiState.products.count { it.isLowStock }})")
                    }
                )
            }
            item {
                FilterChip(
                    selected = stockFilter == AdminStockFilter.OUT,
                    onClick = {
                        stockFilter = if (stockFilter == AdminStockFilter.OUT) {
                            AdminStockFilter.ALL
                        } else {
                            AdminStockFilter.OUT
                        }
                    },
                    label = {
                        Text("${strings.outOfStockFilter} (${uiState.products.count { !it.isInStock }})")
                    }
                )
            }
        }

        if (filtered.isEmpty()) {
            EmptyStateView(
                icon = Icons.Rounded.Inventory2,
                title = strings.noProductsFound,
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { product ->
                    val (stockBg, stockFg, stockText) = when {
                        !product.isInStock -> Triple(
                            MaterialTheme.colorScheme.errorContainer,
                            MaterialTheme.colorScheme.onErrorContainer,
                            strings.outOfStock
                        )
                        product.isLowStock -> Triple(
                            MaterialTheme.colorScheme.tertiaryContainer,
                            MaterialTheme.colorScheme.onTertiaryContainer,
                            strings.lowStockCount(
                                product.stockQuantity,
                                product.displayUnit(settings.language)
                            )
                        )
                        else -> Triple(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.onPrimaryContainer,
                            strings.inStockCount(
                                product.stockQuantity,
                                product.displayUnit(settings.language)
                            )
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (product.isAvailable) 1f else 0.65f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CategoryAvatar(category = product.category, size = 42.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product.displayName(settings.language),
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "#${product.sku} • ${strings.categoryName(product.category)} • ${
                                            CurrencyFormatter.format(product.priceRsd, settings.language, settings.rsdPerEur)
                                        }",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                EditActionButton(
                                    onClick = { onEditProduct(product) },
                                    testTag = "admin_edit_product_${product.id}"
                                )
                                DeleteActionButton(
                                    onClick = { onDeleteProduct(product) },
                                    testTag = "admin_delete_product_${product.id}"
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = stockBg
                                ) {
                                    Text(
                                        text = stockText,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = stockFg,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = { onAdjustStock(product.id, -1) },
                                        enabled = product.stockQuantity > 0,
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("-1", style = MaterialTheme.typography.labelMedium)
                                    }
                                    OutlinedButton(
                                        onClick = { onAdjustStock(product.id, 10) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text(strings.quickRestockBtn, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
