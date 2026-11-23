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
import com.fourpit.marketpos.model.BarTable

@Composable
fun BarTableEditorDialog(
    initial: BarTable?,
    onDismiss: () -> Unit,
    onSave: (label: String, seats: Int, existingId: Long) -> Unit,
    onDelete: ((Long) -> Unit)? = null
) {
    val strings = LocalAppStrings.current
    var label by remember(initial) { mutableStateOf(initial?.label.orEmpty()) }
    var seatsText by remember(initial) { mutableStateOf((initial?.seats ?: 4).toString()) }

    val seats = seatsText.toIntOrNull()
    val valid = label.isNotBlank() && seats != null && seats in 1..24

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addTable, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(strings.tableLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = seatsText,
                    onValueChange = { seatsText = it.filter(Char::isDigit) },
                    label = { Text(strings.seatsCount) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(label.trim(), seats ?: 4, initial?.id ?: 0L) },
                enabled = valid
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
