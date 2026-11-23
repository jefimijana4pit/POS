package com.fourpit.marketpos.ui.screens.bar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fourpit.marketpos.i18n.LocalAppStrings
import com.fourpit.marketpos.model.BarTable
import com.fourpit.marketpos.model.CurrencyCode
import com.fourpit.marketpos.ui.components.EmptyStatePlaceholder

@Composable
fun BarFloorPane(
    tables: List<BarTable>,
    currency: CurrencyCode,
    eurRate: Double,
    onTableClick: (BarTable) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    if (tables.isEmpty()) {
        EmptyStatePlaceholder(
            icon = Icons.Default.TableRestaurant,
            title = strings.noDataYet,
            modifier = modifier.fillMaxSize()
        )
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 156.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(tables, key = { it.id }) { table ->
            BarTableRowItem(
                table = table,
                currency = currency,
                eurRate = eurRate,
                onClick = { onTableClick(table) }
            )
        }
    }
}
