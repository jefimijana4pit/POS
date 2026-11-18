package com.example.marketpos.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.marketpos.i18n.LocalStrings
import com.example.marketpos.model.AppLanguage
import com.example.marketpos.model.AppSettings
import com.example.marketpos.model.Product
import com.example.marketpos.model.ProductCategory
import com.example.marketpos.ui.theme.JetBrainsMonoFamily
import com.example.marketpos.util.CurrencyFormatter

@Composable
internal fun CatalogPane(
    settings: AppSettings,
    searchQuery: String,
    selectedCategory: ProductCategory?,
    showOutOfStockOnly: Boolean,
    venueCategories: List<ProductCategory>,
    products: List<Product>,
    outOfStockProducts: List<Product>,
    cartQuantities: Map<String, Int>,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (ProductCategory?) -> Unit,
    onToggleOutOfStockFilter: () -> Unit,
    onAddProduct: (Product) -> Unit,
    onDecrementProduct: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current

    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text(strings.searchProducts) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Rounded.Clear, contentDescription = strings.close)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("catalog_search_input")
        )

        AnimatedVisibility(visible = outOfStockProducts.isNotEmpty()) {
            val previewNames = remember(outOfStockProducts, settings.language) {
                val maxShown = 3
                val shown = outOfStockProducts.take(maxShown).joinToString(", ") {
                    it.displayName(settings.language)
                }
                if (outOfStockProducts.size > maxShown) {
                    "$shown (+${outOfStockProducts.size - maxShown})"
                } else {
                    shown
                }
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onToggleOutOfStockFilter)
                    .testTag("worker_out_of_stock_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.outOfStockBannerTitle(outOfStockProducts.size),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = previewNames,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                    ) {
                        Text(
                            text = if (showOutOfStockOnly) strings.showAllLabel else strings.viewLabel,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            item {
                FilterChip(
                    selected = !showOutOfStockOnly && selectedCategory == null,
                    onClick = { onSelectCategory(null) },
                    label = { Text(strings.all) }
                )
            }
            if (outOfStockProducts.isNotEmpty()) {
                item {
                    FilterChip(
                        selected = showOutOfStockOnly,
                        onClick = onToggleOutOfStockFilter,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(strings.outOfStockBannerTitle(outOfStockProducts.size)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.testTag("filter_out_of_stock_chip")
                    )
                }
            }
            items(venueCategories) { category ->
                FilterChip(
                    selected = !showOutOfStockOnly && selectedCategory == category,
                    onClick = {
                        onSelectCategory(
                            if (!showOutOfStockOnly && selectedCategory == category) null else category
                        )
                    },
                    label = { Text(strings.categoryName(category)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (products.isEmpty()) {
            EmptyStateView(
                icon = Icons.Rounded.ShoppingBag,
                title = strings.noProductsFound,
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(products, key = { it.id }) { product ->
                    ProductRowItem(
                        product = product,
                        quantityInCart = cartQuantities[product.id] ?: 0,
                        language = settings.language,
                        rsdPerEur = settings.rsdPerEur,
                        onAdd = { onAddProduct(product) },
                        onDecrement = { onDecrementProduct(product.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductRowItem(
    product: Product,
    quantityInCart: Int,
    language: AppLanguage,
    rsdPerEur: Double,
    onAdd: () -> Unit,
    onDecrement: () -> Unit
) {
    val strings = LocalStrings.current
    val enabled = product.isInStock
    val canAddMore = enabled && quantityInCart < product.stockQuantity

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = when {
            !enabled -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.22f)
            quantityInCart > 0 -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        },
        tonalElevation = if (quantityInCart > 0) 2.dp else 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.82f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onAdd)
            .testTag("product_row_${product.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryAvatar(category = product.category, size = 44.dp)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.displayName(language),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${strings.categoryName(product.category)} • #${product.sku}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                StockStatusBadge(product = product, language = language)
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyFormatter.format(product.priceRsd, language, rsdPerEur),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                if (quantityInCart > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onDecrement)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Remove,
                                    contentDescription = strings.removeOne,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = quantityInCart.toString(),
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                        Surface(
                            shape = CircleShape,
                            color = if (canAddMore) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable(enabled = canAddMore, onClick = onAdd)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = strings.addOne,
                                    tint = if (canAddMore) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    },
                                    modifier = Modifier.size(16.dp)
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
private fun StockStatusBadge(
    product: Product,
    language: AppLanguage
) {
    val strings = LocalStrings.current
    val (bg, fg, label) = when {
        !product.isInStock -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            strings.outOfStock
        )
        product.isLowStock -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            strings.lowStockCount(product.stockQuantity, product.displayUnit(language))
        )
        else -> Triple(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.onSurfaceVariant,
            strings.inStockCount(product.stockQuantity, product.displayUnit(language))
        )
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bg
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            if (!product.isInStock) {
                Icon(
                    imageVector = Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Medium
                ),
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
