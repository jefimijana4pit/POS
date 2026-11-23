package com.fourpit.marketpos.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import com.fourpit.marketpos.i18n.LocalAppStrings
import com.fourpit.marketpos.model.PosLocation
import com.fourpit.marketpos.model.VenueType

@Composable
fun LocationEditorDialog(
    initial: PosLocation?,
    onDismiss: () -> Unit,
    onSave: (PosLocation) -> Unit,
    onDelete: ((Long) -> Unit)? = null
) {
    val strings = LocalAppStrings.current
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var address by remember(initial) { mutableStateOf(initial?.address.orEmpty()) }
    var venueType by remember(initial) { mutableStateOf(initial?.venueType ?: VenueType.MARKET) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) strings.addLocation else strings.editLocation,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.locationName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(strings.locationAddress) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(strings.venueType, style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = venueType == VenueType.MARKET,
                        onClick = { venueType = VenueType.MARKET },
                        label = { Text(strings.venueMarket) }
                    )
                    FilterChip(
                        selected = venueType == VenueType.BAR,
                        onClick = { venueType = VenueType.BAR },
                        label = { Text(strings.venueBar) }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        PosLocation(
                            id = initial?.id ?: 0L,
                            name = name.trim(),
                            address = address.trim(),
                            venueType = venueType,
                            isActive = true
                        )
                    )
                },
                enabled = name.isNotBlank()
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
