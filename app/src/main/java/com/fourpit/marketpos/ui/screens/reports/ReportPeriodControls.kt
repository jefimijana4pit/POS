package com.fourpit.marketpos.ui.screens.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fourpit.marketpos.i18n.LocalAppStrings
import com.fourpit.marketpos.model.ReportPeriod

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportPeriodControls(
    selectedPeriod: ReportPeriod,
    onSelectPeriod: (ReportPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val options = listOf(
        ReportPeriod.TODAY to strings.periodToday,
        ReportPeriod.LAST_7_DAYS to strings.period7Days,
        ReportPeriod.THIS_MONTH to strings.periodMonth,
        ReportPeriod.ALL_TIME to strings.periodAll
    )
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (period, label) ->
            FilterChip(
                selected = selectedPeriod == period,
                onClick = { onSelectPeriod(period) },
                label = { Text(label) }
            )
        }
    }
}
