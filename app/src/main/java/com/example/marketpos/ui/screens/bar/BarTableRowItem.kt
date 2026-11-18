package com.example.marketpos.ui.screens.bar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Chair
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.TableRestaurant
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.marketpos.model.BarTable
import com.example.marketpos.ui.theme.JetBrainsMonoFamily
import com.example.marketpos.ui.theme.LocalSemanticColors
import com.example.marketpos.util.CurrencyFormatter

@Composable
internal fun BarTableRowItem(
    table: BarTable,
    currentWorkerId: String?,
    nowMillis: Long,
    language: AppLanguage,
    rsdPerEur: Double,
    onTapTable: () -> Unit,
    onUnlockTable: () -> Unit,
    onStartNewOrder: () -> Unit
) {
    val strings = LocalStrings.current
    val semanticColors = LocalSemanticColors.current
    val isMine = table.isLockedBy(currentWorkerId)
    val isOther = table.isLockedByOther(currentWorkerId)
    val remainingMillis = table.remainingCleanupMillis(nowMillis)
    val countdownText = CurrencyFormatter.formatCountdown(remainingMillis)

    val containerColor = when {
        table.awaitingCleanup && isMine -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f)
        isMine -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        isOther -> semanticColors.warningContainer.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }

    val borderStroke = when {
        table.awaitingCleanup && isMine -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary)
        isMine -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        isOther -> BorderStroke(1.2.dp, semanticColors.warning)
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }

    val (badgeBg, badgeFg, statusText) = when {
        table.awaitingCleanup -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            strings.tableStatusAwaitingCleanup
        )
        isMine -> Triple(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary,
            strings.tableStatusLockedByYou
        )
        isOther -> Triple(
            semanticColors.warningContainer,
            semanticColors.onWarningContainer,
            strings.tableStatusLockedByOther(table.lockedByWorkerName ?: "")
        )
        else -> Triple(
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.onSurfaceVariant,
            strings.tableStatusFree
        )
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = borderStroke,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isOther) 0.85f else 1f)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onTapTable)
            .testTag("bar_table_card_${table.id}")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        isOther -> semanticColors.warningContainer
                        table.awaitingCleanup -> MaterialTheme.colorScheme.tertiaryContainer
                        isMine -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when {
                                isOther -> Icons.Rounded.Lock
                                table.awaitingCleanup -> Icons.Rounded.CleaningServices
                                isMine -> Icons.Rounded.TableRestaurant
                                else -> Icons.Rounded.LockOpen
                            },
                            contentDescription = null,
                            tint = when {
                                isOther -> semanticColors.onWarningContainer
                                table.awaitingCleanup -> MaterialTheme.colorScheme.onTertiaryContainer
                                isMine -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = table.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = badgeBg
                        ) {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = badgeFg,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = "${table.zone} • ${strings.tableSeatsCount(table.capacity)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    when {
                        table.awaitingCleanup -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Timer,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    text = strings.tableAutoUnlockIn(countdownText),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                        table.isLocked -> {
                            Text(
                                text = if (table.itemCount > 0) {
                                    strings.tableBusyItemsSummary(table.itemCount)
                                } else {
                                    strings.tableEmptyOrderLabel
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        else -> Unit
                    }
                }

                if (table.isLocked && !table.awaitingCleanup && table.itemCount > 0) {
                    Text(
                        text = CurrencyFormatter.format(table.orderTotalRsd, language, rsdPerEur),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isMine) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Chair,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = table.capacity.toString(),
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (table.awaitingCleanup && isMine) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onUnlockTable,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("unlock_cleaned_table_${table.id}")
                    ) {
                        Text(
                            text = strings.tableCleanedUnlockBtn,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    OutlinedButton(
                        onClick = onStartNewOrder,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("new_order_table_${table.id}")
                    ) {
                        Text(
                            text = strings.newGuestsOrderBtn,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
