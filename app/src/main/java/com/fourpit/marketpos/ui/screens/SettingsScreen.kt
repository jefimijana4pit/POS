package com.fourpit.marketpos.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fourpit.marketpos.i18n.LocalAppStrings
import com.fourpit.marketpos.model.AppLanguage
import com.fourpit.marketpos.model.CurrencyCode
import com.fourpit.marketpos.model.PrinterMode
import com.fourpit.marketpos.ui.screens.settings.SettingsSectionCard
import com.fourpit.marketpos.ui.theme.FontScaleOption
import com.fourpit.marketpos.ui.theme.ThemeMode
import com.fourpit.marketpos.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val pairedPrinters by viewModel.pairedPrinters.collectAsStateWithLifecycle()

    var businessNameText by remember(settings.businessName) { mutableStateOf(settings.businessName) }
    var eurRateText by remember(settings.eurExchangeRate) { mutableStateOf(settings.eurExchangeRate.toString()) }
    var taxRateText by remember(settings.taxRatePercent) { mutableStateOf(settings.taxRatePercent.toString()) }
    var hasBtPerm by remember { mutableStateOf(viewModel.hasBluetoothPermission(context)) }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasBtPerm = grants[Manifest.permission.BLUETOOTH_CONNECT] == true || viewModel.hasBluetoothPermission(context)
        if (hasBtPerm) {
            viewModel.refreshBluetoothPrinters(context)
        }
    }

    LaunchedEffect(hasBtPerm, settings.printerMode) {
        if (hasBtPerm && settings.printerMode == PrinterMode.BLUETOOTH_THERMAL) {
            viewModel.refreshBluetoothPrinters(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.settings, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 620.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SettingsSectionCard(
                    title = strings.settingsLocaleCurrency,
                    icon = Icons.Default.Language
                ) {
                    Text(strings.languageLabel, style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = settings.language == AppLanguage.SR_LATIN,
                            onClick = { viewModel.setLanguage(AppLanguage.SR_LATIN) },
                            label = { Text("Srpski (Latinica)") },
                            modifier = Modifier.testTag("lang_sr_chip")
                        )
                        FilterChip(
                            selected = settings.language == AppLanguage.EN,
                            onClick = { viewModel.setLanguage(AppLanguage.EN) },
                            label = { Text("English") },
                            modifier = Modifier.testTag("lang_en_chip")
                        )
                    }

                    Text(strings.currencyLabel, style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = settings.currency == CurrencyCode.RSD,
                            onClick = { viewModel.setCurrency(CurrencyCode.RSD) },
                            label = { Text("RSD (Dinar)") },
                            modifier = Modifier.testTag("currency_rsd_chip")
                        )
                        FilterChip(
                            selected = settings.currency == CurrencyCode.EUR,
                            onClick = { viewModel.setCurrency(CurrencyCode.EUR) },
                            label = { Text("EUR (€)") },
                            modifier = Modifier.testTag("currency_eur_chip")
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = eurRateText,
                            onValueChange = { eurRateText = it },
                            label = { Text(strings.exchangeRateLabel) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                eurRateText.replace(",", ".").toDoubleOrNull()?.let(viewModel::setEurRate)
                            },
                            modifier = Modifier.testTag("save_eur_rate_button")
                        ) {
                            Text(strings.save)
                        }
                    }
                }

                SettingsSectionCard(
                    title = strings.settingsAppearance,
                    icon = Icons.Default.Palette
                ) {
                    Text(strings.themeModeLabel, style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = settings.themeMode == ThemeMode.SYSTEM,
                            onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                            label = { Text(strings.themeSystem) }
                        )
                        FilterChip(
                            selected = settings.themeMode == ThemeMode.LIGHT,
                            onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                            label = { Text(strings.themeLight) }
                        )
                        FilterChip(
                            selected = settings.themeMode == ThemeMode.DARK,
                            onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                            label = { Text(strings.themeDark) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.dynamicColorLabel, style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = settings.dynamicColor,
                            onCheckedChange = viewModel::setDynamicColor,
                            modifier = Modifier.testTag("dynamic_color_switch")
                        )
                    }

                    Text(strings.fontScaleLabel, style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = settings.fontScaleOption == FontScaleOption.NORMAL,
                            onClick = { viewModel.setFontScale(FontScaleOption.NORMAL) },
                            label = { Text(strings.fontNormal) }
                        )
                        FilterChip(
                            selected = settings.fontScaleOption == FontScaleOption.LARGE,
                            onClick = { viewModel.setFontScale(FontScaleOption.LARGE) },
                            label = { Text(strings.fontLarge) }
                        )
                        FilterChip(
                            selected = settings.fontScaleOption == FontScaleOption.EXTRA_LARGE,
                            onClick = { viewModel.setFontScale(FontScaleOption.EXTRA_LARGE) },
                            label = { Text(strings.fontExtraLarge) }
                        )
                    }
                }

                SettingsSectionCard(
                    title = strings.settingsBusiness,
                    icon = Icons.Default.Business
                ) {
                    OutlinedTextField(
                        value = businessNameText,
                        onValueChange = { businessNameText = it },
                        label = { Text(strings.businessNameLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = taxRateText,
                        onValueChange = { taxRateText = it },
                        label = { Text(strings.taxRateLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            viewModel.setBusinessName(businessNameText)
                            taxRateText.replace(",", ".").toDoubleOrNull()?.let(viewModel::setTaxRate)
                        },
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("save_business_button")
                    ) {
                        Text(strings.save)
                    }
                }

                SettingsSectionCard(
                    title = strings.settingsPrinter,
                    icon = Icons.Default.Print
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = settings.printerMode == PrinterMode.ANDROID_SYSTEM,
                            onClick = { viewModel.setPrinterMode(PrinterMode.ANDROID_SYSTEM) },
                            label = { Text(strings.printerSystem) }
                        )
                        FilterChip(
                            selected = settings.printerMode == PrinterMode.BLUETOOTH_THERMAL,
                            onClick = { viewModel.setPrinterMode(PrinterMode.BLUETOOTH_THERMAL) },
                            label = { Text(strings.printerBluetooth) }
                        )
                    }

                    if (settings.printerMode == PrinterMode.BLUETOOTH_THERMAL) {
                        if (!hasBtPerm) {
                            Text(
                                text = strings.bluetoothPermissionRequired,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = {
                                    btPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.BLUETOOTH_CONNECT,
                                            Manifest.permission.BLUETOOTH_SCAN
                                        )
                                    )
                                }
                            ) {
                                Icon(Icons.Default.Bluetooth, contentDescription = null)
                                Text(" ${strings.grantBluetoothPermission}")
                            }
                        } else if (pairedPrinters.isEmpty()) {
                            Text(
                                text = strings.noBluetoothPrinters,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(strings.selectBluetoothPrinter, style = MaterialTheme.typography.labelLarge)
                            pairedPrinters.forEach { device ->
                                FilterChip(
                                    selected = settings.bluetoothPrinterAddress == device.address,
                                    onClick = { viewModel.selectBluetoothPrinter(device) },
                                    label = { Text("${device.name} (${device.address})") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
