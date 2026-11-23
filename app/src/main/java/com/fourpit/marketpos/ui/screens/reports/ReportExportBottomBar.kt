package com.fourpit.marketpos.ui.screens.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.fourpit.marketpos.i18n.LocalAppStrings

@Composable
fun ReportExportBottomBar(
    isExporting: Boolean,
    onDismiss: () -> Unit,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(onClick = onDismiss) {
            Text(strings.close)
        }
        Button(
            onClick = onExportClick,
            enabled = !isExporting,
            modifier = Modifier.testTag("export_pdf_confirm_button")
        ) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
            Text("  ${strings.generateAndSharePdf}")
        }
    }
}
