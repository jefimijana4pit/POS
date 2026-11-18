package com.example.marketpos.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.LocalBar
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.marketpos.i18n.LocalStrings
import com.example.marketpos.model.AppLanguage
import com.example.marketpos.model.PairedPrinterDevice
import com.example.marketpos.model.PrinterType
import com.example.marketpos.model.UserRole
import com.example.marketpos.model.VenueMode
import com.example.marketpos.ui.components.PrintFeedbackDialog
import com.example.marketpos.ui.screens.settings.BluetoothPrinterControls
import com.example.marketpos.ui.screens.settings.PrinterOptionRow
import com.example.marketpos.ui.screens.settings.SettingsSectionCard
import com.example.marketpos.ui.theme.FontScaleOption
import com.example.marketpos.ui.theme.JetBrainsMonoFamily
import com.example.marketpos.ui.theme.ThemeMode
import com.example.marketpos.viewmodel.SettingsUiState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onUpdateVenueMode: (VenueMode) -> Unit,
    onUpdateLanguage: (AppLanguage) -> Unit,
    onUpdateExchangeRate: (Double) -> Unit,
    onUpdateThemeMode: (ThemeMode) -> Unit,
    onUpdateDynamicColor: (Boolean) -> Unit,
    onUpdateFontScale: (FontScaleOption) -> Unit,
    onUpdatePrinterType: (PrinterType) -> Unit,
    onSelectPrinterDevice: (PairedPrinterDevice) -> Unit,
    onRefreshPrinters: () -> Unit,
    onTestPrint: () -> Unit,
    onDismissPrintFeedback: () -> Unit
) {
    val strings = LocalStrings.current
    val settings = uiState.settings

    var rateText by rememberSaveable(settings.rsdPerEur) {
        mutableStateOf(String.format(Locale.US, "%.2f", settings.rsdPerEur))
    }
    var rateStatus by rememberSaveable { mutableStateOf<String?>(null) }
    var rateError by rememberSaveable { mutableStateOf(false) }

    PrintFeedbackDialog(
        printResult = uiState.printFeedback,
        onDismiss = onDismissPrintFeedback,
        onBluetoothPermissionsResult = { granted ->
            if (granted) onRefreshPrinters()
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.settingsTitle) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = strings.back
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (uiState.currentUser?.role == UserRole.ADMIN) {
                    SettingsSectionCard(
                        title = strings.venueModeSection,
                        icon = if (settings.venueMode == VenueMode.MARKET) {
                            Icons.Rounded.Storefront
                        } else {
                            Icons.Rounded.LocalBar
                        }
                    ) {
                        Text(
                            text = strings.venueModeLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = settings.venueMode == VenueMode.MARKET,
                                onClick = { onUpdateVenueMode(VenueMode.MARKET) },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                icon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Storefront,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                modifier = Modifier.testTag("venue_mode_market_btn")
                            ) {
                                Text(strings.venueMarketLabel, maxLines = 1)
                            }
                            SegmentedButton(
                                selected = settings.venueMode == VenueMode.BAR_RESTAURANT,
                                onClick = { onUpdateVenueMode(VenueMode.BAR_RESTAURANT) },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                icon = {
                                    Icon(
                                        imageVector = Icons.Rounded.LocalBar,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                modifier = Modifier.testTag("venue_mode_bar_btn")
                            ) {
                                Text(strings.venueBarLabel, maxLines = 1)
                            }
                        }
                        Text(
                            text = if (settings.venueMode == VenueMode.MARKET) {
                                strings.venueModeMarketDesc
                            } else {
                                strings.venueModeBarDesc
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                SettingsSectionCard(
                    title = strings.languageCurrencySection,
                    icon = Icons.Rounded.CurrencyExchange
                ) {
                    Text(
                        text = strings.languageLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = settings.language == AppLanguage.SERBIAN_LATIN,
                            onClick = { onUpdateLanguage(AppLanguage.SERBIAN_LATIN) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            modifier = Modifier.testTag("lang_sr_btn")
                        ) {
                            Text(strings.serbianOption, maxLines = 1)
                        }
                        SegmentedButton(
                            selected = settings.language == AppLanguage.ENGLISH,
                            onClick = { onUpdateLanguage(AppLanguage.ENGLISH) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            modifier = Modifier.testTag("lang_en_btn")
                        ) {
                            Text(strings.englishOption, maxLines = 1)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        OutlinedTextField(
                            value = rateText,
                            onValueChange = {
                                rateText = it
                                rateError = false
                                rateStatus = null
                            },
                            label = { Text(strings.exchangeRateLabel) },
                            singleLine = true,
                            isError = rateError,
                            supportingText = rateStatus?.let { msg -> { Text(msg) } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = JetBrainsMonoFamily),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("exchange_rate_input")
                        )
                        Button(
                            onClick = {
                                val parsed = rateText.replace(',', '.').toDoubleOrNull()
                                if (parsed != null && parsed > 0.0) {
                                    onUpdateExchangeRate(parsed)
                                    rateError = false
                                    rateStatus = strings.exchangeRateApplied
                                } else {
                                    rateError = true
                                    rateStatus = strings.exchangeRateInvalid
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .height(52.dp)
                                .testTag("apply_rate_btn")
                        ) {
                            Text(strings.applyRateBtn)
                        }
                    }
                }

                SettingsSectionCard(
                    title = strings.appearanceSection,
                    icon = Icons.Rounded.Palette
                ) {
                    Text(
                        text = strings.themeModeLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        ThemeMode.entries.forEachIndexed { idx, mode ->
                            val label = when (mode) {
                                ThemeMode.SYSTEM -> strings.themeSystem
                                ThemeMode.LIGHT -> strings.themeLight
                                ThemeMode.DARK -> strings.themeDark
                            }
                            SegmentedButton(
                                selected = settings.themeMode == mode,
                                onClick = { onUpdateThemeMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = idx,
                                    count = ThemeMode.entries.size
                                ),
                                modifier = Modifier.testTag("theme_${mode.name.lowercase()}")
                            ) {
                                Text(label, maxLines = 1)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onUpdateDynamicColor(!settings.dynamicColor) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.dynamicColorLabel,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = strings.dynamicColorSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = settings.dynamicColor,
                            onCheckedChange = onUpdateDynamicColor,
                            modifier = Modifier.testTag("dynamic_color_switch")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.FormatSize,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.fontScaleLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        FontScaleOption.entries.forEachIndexed { idx, option ->
                            val label = when (option) {
                                FontScaleOption.NORMAL -> strings.fontNormal
                                FontScaleOption.LARGE -> strings.fontLarge
                                FontScaleOption.EXTRA_LARGE -> strings.fontExtraLarge
                            }
                            SegmentedButton(
                                selected = settings.fontScale == option,
                                onClick = { onUpdateFontScale(option) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = idx,
                                    count = FontScaleOption.entries.size
                                ),
                                modifier = Modifier.testTag("font_scale_${option.name.lowercase()}")
                            ) {
                                Text(label, maxLines = 1)
                            }
                        }
                    }
                }

                SettingsSectionCard(
                    title = strings.printerSection,
                    icon = Icons.Rounded.Print
                ) {
                    Text(
                        text = strings.printerTypeLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    PrinterOptionRow(
                        selected = settings.printerType == PrinterType.ANDROID_SYSTEM,
                        title = strings.printerSystemTitle,
                        subtitle = strings.printerSystemSubtitle,
                        onClick = { onUpdatePrinterType(PrinterType.ANDROID_SYSTEM) },
                        testTag = "printer_system_radio"
                    )

                    PrinterOptionRow(
                        selected = settings.printerType == PrinterType.BLUETOOTH_THERMAL,
                        title = strings.printerThermalTitle,
                        subtitle = strings.printerThermalSubtitle,
                        onClick = { onUpdatePrinterType(PrinterType.BLUETOOTH_THERMAL) },
                        testTag = "printer_thermal_radio"
                    )

                    AnimatedVisibility(visible = settings.printerType == PrinterType.BLUETOOTH_THERMAL) {
                        BluetoothPrinterControls(
                            hasBluetoothPermission = uiState.hasBluetoothPermission,
                            pairedPrinters = uiState.pairedPrinters,
                            selectedPrinterAddress = settings.selectedPrinterAddress,
                            onRefreshPrinters = onRefreshPrinters,
                            onSelectPrinterDevice = onSelectPrinterDevice
                        )
                    }

                    FilledTonalButton(
                        onClick = onTestPrint,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("settings_test_print_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Print,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.testPrintBtn)
                    }
                }
            }
        }
    }
}
