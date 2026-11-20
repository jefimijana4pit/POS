package com.example.marketpos.ui.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BakeryDining
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Cookie
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Egg
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.LocalBar
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marketpos.i18n.LocalStrings
import com.example.marketpos.model.PrintResult
import com.example.marketpos.model.ProductCategory
import com.example.marketpos.ui.theme.JetBrainsMonoFamily

fun categoryIcon(category: ProductCategory): ImageVector = when (category) {
    ProductCategory.BAKERY -> Icons.Rounded.BakeryDining
    ProductCategory.DAIRY -> Icons.Rounded.Egg
    ProductCategory.BEVERAGES -> Icons.Rounded.LocalDrink
    ProductCategory.PRODUCE -> Icons.Rounded.Eco
    ProductCategory.SNACKS -> Icons.Rounded.Cookie
    ProductCategory.HOUSEHOLD -> Icons.Rounded.CleaningServices
    ProductCategory.COFFEE_TEA -> Icons.Rounded.LocalCafe
    ProductCategory.BAR_DRINKS -> Icons.Rounded.LocalBar
    ProductCategory.MAIN_DISHES -> Icons.Rounded.Restaurant
    ProductCategory.DESSERTS -> Icons.Rounded.Cake
}

@Composable
fun CategoryAvatar(
    category: ProductCategory,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val (bg, fg) = when (category) {
        ProductCategory.BAKERY, ProductCategory.DESSERTS -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        ProductCategory.DAIRY, ProductCategory.COFFEE_TEA -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        ProductCategory.BEVERAGES, ProductCategory.BAR_DRINKS -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        ProductCategory.PRODUCE -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
        ProductCategory.SNACKS, ProductCategory.MAIN_DISHES -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.tertiary
        ProductCategory.HOUSEHOLD -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = categoryIcon(category),
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosTopAppBar(
    title: String,
    subtitle: String? = null,
    currencyBadge: String,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit,
    onBack: (() -> Unit)? = null,
    extraActions: @Composable () -> Unit = {}
) {
    val strings = LocalStrings.current

    TopAppBar(
        navigationIcon = {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("top_bar_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = strings.back
                    )
                }
            }
        },
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        actions = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Text(
                    text = currencyBadge,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            extraActions()
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.testTag("top_bar_settings_btn")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = strings.settings
                )
            }
            IconButton(
                onClick = onLogout,
                modifier = Modifier.testTag("top_bar_logout_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                    contentDescription = strings.logout
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    )
}

@Composable
fun AddActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = LocalStrings.current.add,
    testTag: String? = null
) {
    FilledTonalButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
        modifier = modifier
            .height(40.dp)
            .minimumInteractiveComponentSize()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1
        )
    }
}

@Composable
fun AddFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = LocalStrings.current.add,
    contentDescription: String = label,
    testTag: String? = null
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = contentDescription
            )
        },
        text = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1
            )
        },
        modifier = modifier.then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    )
}

@Composable
fun EditActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String = LocalStrings.current.edit,
    testTag: String? = null
) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
        ),
        modifier = modifier.then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        Icon(
            imageVector = Icons.Rounded.Edit,
            contentDescription = contentDescription,
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
fun DeleteActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String = LocalStrings.current.delete,
    testTag: String? = null
) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.78f),
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
        ),
        modifier = modifier.then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        Icon(
            imageVector = Icons.Rounded.DeleteOutline,
            contentDescription = contentDescription,
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PrintFeedbackDialog(
    printResult: PrintResult?,
    onDismiss: () -> Unit,
    onBluetoothPermissionsResult: (Boolean) -> Unit
) {
    if (printResult == null) return
    val strings = LocalStrings.current

    val btLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[Manifest.permission.BLUETOOTH_CONNECT] == true
        onBluetoothPermissionsResult(granted)
        onDismiss()
    }

    when (printResult) {
        is PrintResult.Success -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = {
                    Icon(
                        imageVector = if (printResult.escPosPreview != null) Icons.Rounded.Print else Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = {
                    Text(
                        text = if (printResult.escPosPreview != null) {
                            strings.printSimulatedTitle
                        } else {
                            strings.printReceiptBtn
                        }
                    )
                },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            text = printResult.message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (printResult.escPosPreview != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                tonalElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = printResult.escPosPreview,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = onDismiss) {
                        Text(strings.close)
                    }
                }
            )
        }

        is PrintResult.PermissionRequired -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Bluetooth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = { Text(strings.printerThermalTitle) },
                text = { Text(strings.bluetoothPermissionNeeded) },
                confirmButton = {
                    Button(
                        onClick = {
                            btLauncher.launch(printResult.permissions.toTypedArray())
                        }
                    ) {
                        Text(strings.grantPermissionBtn)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(strings.cancel)
                    }
                }
            )
        }

        is PrintResult.Error -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = { Text(strings.printReceiptBtn) },
                text = { Text(printResult.message) },
                confirmButton = {
                    Button(onClick = onDismiss) {
                        Text(strings.close)
                    }
                }
            )
        }
    }
}
