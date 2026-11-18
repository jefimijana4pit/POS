package com.example.marketpos.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.marketpos.i18n.LocalStrings
import com.example.marketpos.model.VenueLocation
import com.example.marketpos.model.VenueMode

@Composable
internal fun LocationEditorDialog(
    existing: VenueLocation?,
    venueMode: VenueMode,
    onDismiss: () -> Unit,
    onSave: (name: String, address: String, copyStarterData: Boolean) -> Unit
) {
    val strings = LocalStrings.current
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var address by rememberSaveable { mutableStateOf(existing?.address ?: "") }
    var copyStarterData by rememberSaveable { mutableStateOf(true) }
    var showError by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existing == null) strings.addLocationTitle(venueMode) else strings.editLocationTitle)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        showError = false
                    },
                    label = { Text(strings.locationNameLabel) },
                    isError = showError && name.isBlank(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("loc_name_input")
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = {
                        address = it
                        showError = false
                    },
                    label = { Text(strings.locationAddressLabel) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("loc_address_input")
                )
                if (existing == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.copyStarterCatalogLabel,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )
                        Switch(
                            checked = copyStarterData,
                            onCheckedChange = { copyStarterData = it }
                        )
                    }
                }
                if (showError) {
                    Text(
                        text = strings.validationRequiredError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        showError = true
                    } else {
                        onSave(name.trim(), address.trim(), copyStarterData)
                    }
                },
                modifier = Modifier.testTag("loc_save_btn")
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
