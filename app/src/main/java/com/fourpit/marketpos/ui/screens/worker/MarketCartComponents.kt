package com.fourpit.marketpos.ui.screens.worker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fourpit.marketpos.i18n.LocalAppStrings
import com.fourpit.marketpos.model.CartItem
import com.fourpit.marketpos.model.CurrencyCode
import com.fourpit.marketpos.ui.components.EmptyStatePlaceholder
import com.fourpit.marketpos.ui.components.MonoPriceText
import com.fourpit.marketpos.util.CurrencyFormatter

@Composable
fun MarketCartPanel(
    cart: List<CartItem>,
    totalRsd: Double,
    currency: CurrencyCode,
    eurRate: Double,
    onIncrement: (CartItem) -> Unit,
    onDecrement: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onClear: () -> Unit,
    onCheckoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current

    Surface(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${strings.cart} (${cart.sumOf { it.quantity }})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (cart.isNotEmpty()) {
                    TextButton(onClick = onClear, modifier = Modifier.testTag("clear_cart_button")) {
                        Text(strings.clearCart)
                    }
                }
            }

            if (cart.isEmpty()) {
                EmptyStatePlaceholder(
                    icon = Icons.Default.ShoppingCart,
                    title = strings.emptyCart,
                    subtitle = strings.emptyCartHint,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(cart, key = { it.product.id }) { item ->
                        CartLineItemCard(
                            item = item,
                            currency = currency,
                            eurRate = eurRate,
                            onIncrement = { onIncrement(item) },
                            onDecrement = { onDecrement(item.product.id) },
                            onRemove = { onRemove(item.product.id) }
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.total,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                MonoPriceText(
                    text = CurrencyFormatter.format(totalRsd, currency, eurRate),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = onCheckoutClick,
                enabled = cart.isNotEmpty(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("open_checkout_button")
            ) {
                Text(strings.checkout, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun CartLineItemCard(
    item: CartItem,
    currency: CurrencyCode,
    eurRate: Double,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit
) {
    val strings = LocalAppStrings.current
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                MonoPriceText(
                    text = CurrencyFormatter.format(item.lineTotalRsd, currency, eurRate),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrement, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Remove, contentDescription = "-")
                }
                Text(
                    text = item.quantity.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                IconButton(onClick = onIncrement, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "+")
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = strings.delete,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
