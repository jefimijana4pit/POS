package com.example.marketpos.ui.screens.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.marketpos.i18n.LocalStrings
import com.example.marketpos.model.AppLanguage
import com.example.marketpos.model.ReportPeriodType
import com.example.marketpos.model.VenueLocation
import com.example.marketpos.util.CurrencyFormatter

internal enum class PickingReportDate {
    NONE,
    SINGLE_DAY,
    RANGE_START,
    RANGE_END
}

@Composable
internal fun ReportLocationSelectorRow(
    locations: List<VenueLocation>,
    selectedLocationId: String?,
    onSelectLocation: (String?) -> Unit
) {
    val strings = LocalStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = strings.reportSelectLocationLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedLocationId == null,
                    onClick = { onSelectLocation(null) },
                    label = { Text(strings.reportAllLocationsOption) },
                    modifier = Modifier.testTag("report_loc_global")
                )
            }
            items(locations, key = { it.id }) { loc ->
                FilterChip(
                    selected = selectedLocationId == loc.id,
                    onClick = { onSelectLocation(loc.id) },
                    label = { Text(loc.name) },
                    modifier = Modifier.testTag("report_loc_${loc.id}")
                )
            }
        }
    }
}

@Composable
internal fun ReportPeriodSelectorSection(
    periodType: ReportPeriodType,
    anchorDayMillis: Long,
    rangeStartMillis: Long,
    rangeEndMillis: Long,
    language: AppLanguage,
    onSelectPeriodType: (ReportPeriodType) -> Unit,
    onShiftMonth: (Int) -> Unit,
    onRequestDatePick: (PickingReportDate) -> Unit
) {
    val strings = LocalStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                ReportPeriodType.DAILY to strings.reportPeriodDaily,
                ReportPeriodType.MONTHLY to strings.reportPeriodMonthly,
                ReportPeriodType.DATE_RANGE to strings.reportPeriodDateRange
            ).forEach { (type, label) ->
                FilterChip(
                    selected = periodType == type,
                    onClick = { onSelectPeriodType(type) },
                    label = { Text(label) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("report_period_${type.name.lowercase()}")
                )
            }
        }

        when (periodType) {
            ReportPeriodType.DAILY -> {
                OutlinedButton(
                    onClick = { onRequestDatePick(PickingReportDate.SINGLE_DAY) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("report_pick_single_day_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = CurrencyFormatter.formatFullDate(anchorDayMillis, language),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }

            ReportPeriodType.MONTHLY -> {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onShiftMonth(-1) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("report_prev_month_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = strings.back
                            )
                        }
                        Text(
                            text = formatMonthYear(anchorDayMillis, language),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        IconButton(
                            onClick = { onShiftMonth(1) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("report_next_month_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = strings.viewLabel
                            )
                        }
                    }
                }
            }

            ReportPeriodType.DATE_RANGE -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onRequestDatePick(PickingReportDate.RANGE_START) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("report_pick_range_start_btn")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = strings.reportStartDateLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatShortDate(minOf(rangeStartMillis, rangeEndMillis), language),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { onRequestDatePick(PickingReportDate.RANGE_END) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("report_pick_range_end_btn")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = strings.reportEndDateLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatShortDate(maxOf(rangeStartMillis, rangeEndMillis), language),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReportDatePickerModal(
    initialLocalMillis: Long,
    onDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit
) {
    val strings = LocalStrings.current
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = CurrencyFormatter.localDayStartToUtcDatePickerMillis(initialLocalMillis)
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    datePickerState.selectedDateMillis?.let { utcMillis ->
                        onDateSelected(CurrencyFormatter.utcDatePickerMillisToLocalDayStart(utcMillis))
                    }
                    onDismiss()
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
    ) {
        DatePicker(state = datePickerState)
    }
}
