package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LanguagePreferences
import com.example.data.local.InspectionEntity
import com.example.data.model.AlgerianCarCatalog
import com.example.domain.BluetoothElm327Transport
import com.example.domain.EcuVinComparisonChoice
import com.example.domain.MilKeyOnUserAnswer
import com.example.domain.ObdDiagnosticEngine
import com.example.domain.ObdScanResult
import com.example.domain.ObdSimulatorProfile
import com.example.domain.ObdTestDriveMonitorService
import com.example.domain.PairedBluetoothDevice
import com.example.domain.TestDriveMonitoringSummary
import com.example.domain.VinWmiHelper
import com.example.ui.components.DongleVectorIllustration
import com.example.ui.components.ObdPortUnderSteeringVectorIllustration
import com.example.ui.theme.FahsConnectionSlate
import com.example.ui.theme.FahsInk
import com.example.ui.theme.FahsInstrumentCard
import com.example.ui.theme.FahsInstrumentDivider
import com.example.ui.theme.FahsInstrumentMuted
import com.example.ui.theme.FahsInstrumentText
import com.example.ui.theme.FahsTypography
import com.example.ui.theme.StatusErrorRed
import com.example.ui.theme.StatusNeutralGrey
import com.example.ui.theme.StatusPassGreen
import com.example.ui.theme.StatusWarningAmber
import com.example.ui.theme.StatusWarningAmberContainer
import com.example.ui.theme.StatusWarningAmberOnContainer
import com.example.ui.viewmodel.OptionalCarDraft
import kotlinx.coroutines.launch

@Composable
fun ObdGuideScreen(
    language: AppLanguage,
    activeInspection: InspectionEntity? = null,
    optionalCarDraft: OptionalCarDraft? = null,
    initialScanResult: ObdScanResult? = null,
    shopSettings: com.example.data.repository.ShopSettings = com.example.data.repository.ShopSettings(),
    autoConnectRememberedDongle: Boolean = false,
    onConsumeAutoConnect: () -> Unit = {},
    onOpenDongleBuyingGuide: () -> Unit = {},
    isDeveloperModeEnabled: Boolean = false,
    onAddCarModelForTips: (() -> Unit)? = null,
    onDetectedEcuVin: (String?) -> Unit = {},
    onSaveScanToInspection: (ObdScanResult, Boolean, EcuVinComparisonChoice, MilKeyOnUserAnswer) -> Unit = { _, _, _, _ -> },
    onConfirmTestDriveSummary: (TestDriveMonitoringSummary) -> Unit = {},
    onStartThreeMinuteVisualCheck: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 1 = Real Bluetooth ELM327 (default), 2 = Guide & Port Locations, 0 = Simulator (Developer Mode only)
    var selectedSubTab by remember(isDeveloperModeEnabled) {
        mutableIntStateOf(1)
    }
    var selectedProfile by remember { mutableStateOf(ObdSimulatorProfile.HEALTHY) }

    val expectedVin = activeInspection?.vin?.trim().orEmpty()

    var currentScanResult by remember(initialScanResult) {
        mutableStateOf(initialScanResult?.takeIf { !it.isSimulated })
    }

    var milBulbLitOnDashboard by remember { mutableStateOf(true) }
    var milKeyOnAnswer by remember { mutableStateOf(MilKeyOnUserAnswer.LIT_UP_NORMAL) }
    var ecuVinComparison by remember(currentScanResult?.ecuVin) {
        mutableStateOf(EcuVinComparisonChoice.COULD_NOT_COMPARE)
    }

    // Real Bluetooth states
    var hasBtPermission by remember {
        mutableStateOf(BluetoothElm327Transport.hasBluetoothPermissions(context))
    }
    var pairedDevices by remember {
        mutableStateOf(
            if (hasBtPermission) BluetoothElm327Transport.getPairedDevices(context) else emptyList()
        )
    }
    var isScanningBt by remember { mutableStateOf(false) }
    var currentScanStepIndex by remember { mutableIntStateOf(1) }
    var btStatusMessage by remember { mutableStateOf<String?>(null) }
    var showWhereIsPortHelper by remember { mutableStateOf(false) }
    var showOtherBtDevices by remember { mutableStateOf(false) }

    // Remember the dongle after the first successful scan and reconnect automatically
    var rememberedDongleAddress by remember {
        mutableStateOf(LanguagePreferences.getLastSuccessfulDongleAddress(context))
    }
    var rememberedDongleName by remember {
        mutableStateOf(LanguagePreferences.getLastSuccessfulDongleName(context))
    }
    var hasAttemptedAutoReconnect by remember { mutableStateOf(false) }

    fun runBluetoothScanOnDevice(address: String, name: String) {
        if (isScanningBt) return
        isScanningBt = true
        currentScanStepIndex = 1
        scope.launch {
            btStatusMessage = when (language) {
                AppLanguage.AR -> "الاتصال بكمبيوتر المحرك عبر $name..."
                AppLanguage.FR -> "Connexion au calculateur via $name..."
                AppLanguage.EN -> "Connecting to the engine computer via $name..."
            }
            val transport = BluetoothElm327Transport(
                context = context,
                deviceAddress = address,
                transportName = name
            )
            val connected = transport.connect()
            if (connected) {
                LanguagePreferences.saveLastSuccessfulDongle(context, address, name)
                rememberedDongleAddress = address
                rememberedDongleName = name

                val scanRes = ObdDiagnosticEngine.executeRealObdScan(
                    transport = transport,
                    expectedVin = expectedVin,
                    language = language,
                    onProgress = { stepMsg -> btStatusMessage = stepMsg },
                    onStepProgress = { stepNum -> currentScanStepIndex = stepNum.coerceIn(1, 6) }
                )
                currentScanResult = scanRes
                onDetectedEcuVin(scanRes.ecuVin)
                transport.disconnect()
                btStatusMessage = when (language) {
                    AppLanguage.AR -> "اكتمل الفحص بنجاح (تم حفظ الفيشة للاتصال التلقائي)"
                    AppLanguage.FR -> "Scan terminé avec succès (boîtier mémorisé)"
                    AppLanguage.EN -> "Scan completed (dongle remembered for next time)"
                }
            } else {
                btStatusMessage = when (language) {
                    AppLanguage.AR -> "تعذر الاتصال بـ $name. تأكد من تركيب الفيشة جيداً وتشغيل المحرك."
                    AppLanguage.FR -> "Impossible de joindre $name. Vérifiez que le boîtier est bien branché et le moteur démarré."
                    AppLanguage.EN -> "Could not connect to $name. Check that the dongle is plugged in and the engine is running."
                }
            }
            isScanningBt = false
        }
    }

    // Auto-reconnect once when opening the screen if a remembered dongle is paired
    LaunchedEffect(hasBtPermission, pairedDevices, rememberedDongleAddress, autoConnectRememberedDongle) {
        val addr = rememberedDongleAddress
        if (hasBtPermission && !hasAttemptedAutoReconnect && currentScanResult == null && !addr.isNullOrBlank()) {
            val matchedDev = pairedDevices.firstOrNull { it.address.equals(addr, ignoreCase = true) }
            if (matchedDev != null) {
                hasAttemptedAutoReconnect = true
                onConsumeAutoConnect()
                runBluetoothScanOnDevice(matchedDev.address, matchedDev.name)
            }
        }
    }

    // Automatic test-drive monitoring states
    val isMonitoringDrive by ObdTestDriveMonitorService.isMonitoring.collectAsState()
    val monitorStatusMsg by ObdTestDriveMonitorService.statusMessage.collectAsState()
    val monitorSamplesCount by ObdTestDriveMonitorService.samplesCount.collectAsState()
    val completedDriveSummary by ObdTestDriveMonitorService.completedSummary.collectAsState()
    var showNotificationPermExplanation by remember { mutableStateOf(false) }

    val notificationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        val targetDev = pairedDevices.firstOrNull {
            it.address.equals(rememberedDongleAddress, ignoreCase = true)
        } ?: pairedDevices.firstOrNull { it.isLikelyObdAdapter } ?: pairedDevices.firstOrNull()
        if (targetDev != null) {
            ObdTestDriveMonitorService.startMonitoring(
                context = context,
                deviceAddress = targetDev.address,
                deviceName = targetDev.name,
                language = language
            )
        }
    }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants.values.any { it } || BluetoothElm327Transport.hasBluetoothPermissions(context)
        hasBtPermission = granted
        if (granted) {
            pairedDevices = BluetoothElm327Transport.getPairedDevices(context)
        }
    }

    // Determine optional car label & port hint
    val selectedBrand = optionalCarDraft?.brand
        ?: activeInspection?.carBrand?.takeIf { !VinWmiHelper.isUnselectedCar(it, activeInspection.carModel) }
    val selectedModel = optionalCarDraft?.model
        ?: activeInspection?.carModel?.takeIf { !VinWmiHelper.isUnselectedCar(activeInspection.carBrand, it) }
    val selectedYear = optionalCarDraft?.year ?: activeInspection?.carYear
    val carHeaderLabel = remember(selectedBrand, selectedModel, selectedYear) {
        if (!selectedBrand.isNullOrBlank() && !selectedModel.isNullOrBlank()) {
            if (selectedYear != null && selectedYear > 1980) {
                "$selectedBrand $selectedModel ($selectedYear)"
            } else {
                "$selectedBrand $selectedModel"
            }
        } else {
            ""
        }
    }
    val matchedCatalogCar = remember(selectedBrand, selectedModel) {
        if (!selectedBrand.isNullOrBlank() && !selectedModel.isNullOrBlank()) {
            AlgerianCarCatalog.models.firstOrNull { car ->
                car.brand.equals(selectedBrand, ignoreCase = true) &&
                    car.model.equals(selectedModel, ignoreCase = true)
            }
        } else {
            null
        }
    }

    // =========================================================================
    // FULL-SCREEN INSTRUMENT PANEL WHEN SCAN IS IN PROGRESS (02_scan_in_progress)
    // =========================================================================
    if (isScanningBt) {
        ScanInProgressInstrumentPanel(
            language = language,
            currentStepIndex = currentScanStepIndex,
            carHeaderLabel = carHeaderLabel,
            modifier = modifier.fillMaxSize()
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Sub-tab switcher (only shown if Developer Mode is enabled or user switches to Port Guide)
        if (isDeveloperModeEnabled) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ObdModeTabButton(
                        title = when (language) {
                            AppLanguage.AR -> "الفحص التلقائي"
                            AppLanguage.FR -> "Scan automatique"
                            AppLanguage.EN -> "Automatic scan"
                        },
                        selected = selectedSubTab == 1,
                        testTag = "obd_tab_bluetooth",
                        onClick = { selectedSubTab = 1 },
                        modifier = Modifier.weight(1f)
                    )
                    ObdModeTabButton(
                        title = when (language) {
                            AppLanguage.AR -> "أماكن المنفذ"
                            AppLanguage.FR -> "Guide & Prises"
                            AppLanguage.EN -> "Guide & Ports"
                        },
                        selected = selectedSubTab == 2,
                        testTag = "obd_tab_guide",
                        onClick = { selectedSubTab = 2 },
                        modifier = Modifier.weight(1f)
                    )
                    ObdModeTabButton(
                        title = when (language) {
                            AppLanguage.AR -> "[DEV] محاكي"
                            AppLanguage.FR -> "[DEV] Simulateur"
                            AppLanguage.EN -> "[DEV] Simulator"
                        },
                        selected = selectedSubTab == 0,
                        testTag = "obd_tab_simulator",
                        onClick = { selectedSubTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        val effectiveSubTab = if (!isDeveloperModeEnabled && selectedSubTab == 0) 1 else selectedSubTab
        when (effectiveSubTab) {
            0 -> {
                // Developer Mode Simulator 4-Profile Selector
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Science,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when (language) {
                                        AppLanguage.AR -> "محاكي OBD2 المدمج — جرّب الحالات الأربع بدون جهاز:"
                                        AppLanguage.FR -> "Simulateur OBD2 intégré — Testez les 4 scénarios sans boîtier :"
                                        AppLanguage.EN -> "Built-in OBD2 Simulator — Test all 4 car scenarios without hardware:"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            ObdSimulatorProfile.entries.forEach { profile ->
                                val isSelected = selectedProfile == profile
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            selectedProfile = profile
                                            currentScanResult = ObdDiagnosticEngine.generateSimulatedScan(
                                                profile = profile,
                                                expectedVin = expectedVin
                                            )
                                        }
                                        .testTag("sim_profile_${profile.name.lowercase()}"),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outline
                                        }
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val dotColor = when (profile) {
                                            ObdSimulatorProfile.HEALTHY -> StatusPassGreen
                                            ObdSimulatorProfile.STORED_FAULTS -> StatusErrorRed
                                            ObdSimulatorProfile.RECENTLY_CLEARED_FRAUD -> StatusWarningAmber
                                            ObdSimulatorProfile.NO_ECU_RESPONSE -> StatusNeutralGrey
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(dotColor)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = profile.getTitle(language),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = profile.getSubtitle(language),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                lineHeight = 17.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Render Simulated Scan Result (Developer Mode only — never saveable to an inspection or PDF)
                item {
                    val simScan = ObdDiagnosticEngine.generateSimulatedScan(
                        profile = selectedProfile,
                        expectedVin = expectedVin
                    )
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = StatusWarningAmberContainer,
                        border = BorderStroke(1.dp, StatusWarningAmber)
                    ) {
                        Text(
                            text = when (language) {
                                AppLanguage.AR -> "وضع المطورين فقط: هذه بيانات محاكاة للتجربة ولا يمكن حفظها في أي فحص حقيقي أو ملف PDF."
                                AppLanguage.FR -> "Mode Développeur uniquement : résultat simulé impossible à enregistrer dans une inspection ou un PDF."
                                AppLanguage.EN -> "Developer Mode only: simulated result cannot be saved into any inspection or PDF."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = StatusWarningAmberOnContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    ObdScanResultSection(
                        scanResult = simScan,
                        language = language,
                        carHeaderLabel = carHeaderLabel,
                        expectedVin = expectedVin,
                        milBulbLitOnDashboard = milBulbLitOnDashboard,
                        onMilBulbToggle = { milBulbLitOnDashboard = it },
                        milKeyOnAnswer = milKeyOnAnswer,
                        onMilKeyOnAnswerChange = { milKeyOnAnswer = it },
                        hasActiveInspection = false,
                        onSaveToInspection = {}
                    )
                }
            }

            1 -> {
                val realScan = currentScanResult?.takeIf { !it.isSimulated }

                // If we already have a scan result, show the Result Screen first so the Verdict Header is at the very top!
                if (realScan != null) {
                    item {
                        ObdScanResultSection(
                            scanResult = realScan,
                            language = language,
                            carHeaderLabel = carHeaderLabel,
                            expectedVin = expectedVin,
                            ecuVinComparison = ecuVinComparison,
                            onEcuVinComparisonChange = { ecuVinComparison = it },
                            milBulbLitOnDashboard = milBulbLitOnDashboard,
                            onMilBulbToggle = { milBulbLitOnDashboard = it },
                            milKeyOnAnswer = milKeyOnAnswer,
                            onMilKeyOnAnswerChange = { milKeyOnAnswer = it },
                            hasActiveInspection = activeInspection != null,
                            onSaveToInspection = {
                                onSaveScanToInspection(
                                    realScan,
                                    milBulbLitOnDashboard,
                                    ecuVinComparison,
                                    milKeyOnAnswer
                                )
                            },
                            onAddCarModelForTips = if (carHeaderLabel.isBlank()) onAddCarModelForTips else null,
                            onContinueThreeMinuteVisualCheck = onStartThreeMinuteVisualCheck,
                            isDeveloperModeEnabled = isDeveloperModeEnabled,
                            onExportRawLog = {
                                val carCtx = if (carHeaderLabel.isNotBlank()) {
                                    "$carHeaderLabel [${realScan.detectedProtocol}]"
                                } else {
                                    realScan.detectedProtocol
                                }
                                val savedFile = BluetoothElm327Transport.saveRawExchangeLogToFile(
                                    context = context,
                                    exchanges = realScan.rawExchangeLog,
                                    adapterName = realScan.adapterVersion,
                                    carLabel = carCtx
                                )
                                if (savedFile != null) {
                                    BluetoothElm327Transport.shareRawExchangeLogFile(context, savedFile)
                                    btStatusMessage = when (language) {
                                        AppLanguage.AR -> "تم حفظ ملف السجل الخام: ${savedFile.name}"
                                        AppLanguage.FR -> "Journal brut enregistré : ${savedFile.name}"
                                        AppLanguage.EN -> "Raw exchange log saved: ${savedFile.name}"
                                    }
                                }
                            }
                        )
                    }
                }

                // =========================================================================
                // PRE-SCAN PREPARATION & BLUETOOTH CONNECTION CARD
                // =========================================================================
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("obd_preparation_and_connection_card"),
                        shape = RoundedCornerShape(18.dp),
                        color = FahsInk,
                        border = BorderStroke(1.dp, FahsInstrumentDivider)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    DongleVectorIllustration(
                                        modifier = Modifier.size(38.dp),
                                        strokeColor = FahsInstrumentText,
                                        accentColor = FahsConnectionSlate
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = when (language) {
                                                AppLanguage.AR -> "الفحص التلقائي للمحرك والإلكترونيات"
                                                AppLanguage.FR -> "Scan automatique moteur et électronique"
                                                AppLanguage.EN -> "Automatic engine & electronics scan"
                                            },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = FahsInstrumentText
                                        )
                                        Text(
                                            text = when (language) {
                                                AppLanguage.AR -> "قراءة مباشرة وآمنة 100% — لا يمسح ولا يعدل أي شيء في السيارة"
                                                AppLanguage.FR -> "Lecture seule 100% sûre — n'efface et ne modifie rien"
                                                AppLanguage.EN -> "100% safe read-only — never clears or writes anything"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = FahsInstrumentMuted
                                        )
                                    }
                                }
                            }

                            if (carHeaderLabel.isNotBlank() || matchedCatalogCar != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = FahsInstrumentCard,
                                    border = BorderStroke(1.dp, FahsInstrumentDivider)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = carHeaderLabel,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = FahsInstrumentText
                                        )
                                        if (matchedCatalogCar != null) {
                                            val portHint = when (language) {
                                                AppLanguage.AR -> matchedCatalogCar.obdPortHintAr
                                                AppLanguage.FR -> matchedCatalogCar.obdPortHintFr
                                                AppLanguage.EN -> matchedCatalogCar.obdPortHintEn
                                            }
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = when (language) {
                                                    AppLanguage.AR -> "مكان المنفذ: $portHint"
                                                    AppLanguage.FR -> "Emplacement prise : $portHint"
                                                    AppLanguage.EN -> "Socket location: $portHint"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = FahsConnectionSlate
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = FahsInstrumentDivider, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(14.dp))

                            // 3 Plain-Language Preparation Steps (including Rule #2: turn off A/C before reading)
                            PreparationStepLine(
                                number = "1",
                                text = when (language) {
                                    AppLanguage.AR -> "ركّب فيشة ELM327 في منفذ السيارة تحت المقود."
                                    AppLanguage.FR -> "Branchez le boîtier ELM327 dans la prise sous le volant."
                                    AppLanguage.EN -> "Plug the ELM327 dongle into the socket under the steering wheel."
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            PreparationStepLine(
                                number = "2",
                                text = when (language) {
                                    AppLanguage.AR -> "شغّل المحرك واتركه يدور في وضع التوقف."
                                    AppLanguage.FR -> "Démarrez le moteur et laissez-le tourner au point mort."
                                    AppLanguage.EN -> "Start the engine and let it idle in neutral."
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            PreparationStepLine(
                                number = "3",
                                text = when (language) {
                                    AppLanguage.AR -> "أوقف المكيف (Climatiseur) قبل بدء القراءة لقياس استقرار الدوران بدقة."
                                    AppLanguage.FR -> "Coupez la climatisation avant la lecture pour mesurer la stabilité du ralenti."
                                    AppLanguage.EN -> "Switch off the air conditioning before reading so idle steadiness is accurate."
                                }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Expandable "Where is the OBD socket?" helper with line drawing
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showWhereIsPortHelper = !showWhereIsPortHelper }
                                    .testTag("where_is_obd_port_toggle"),
                                shape = RoundedCornerShape(8.dp),
                                color = FahsInstrumentCard,
                                border = BorderStroke(1.dp, FahsInstrumentDivider)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = FahsConnectionSlate,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = when (language) {
                                            AppLanguage.AR -> "أين يوجد منفذ الفحص في السيارة؟"
                                            AppLanguage.FR -> "Où se trouve la prise dans la voiture ?"
                                            AppLanguage.EN -> "Where is the socket in the car?"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = FahsConnectionSlate
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = if (showWhereIsPortHelper) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = FahsConnectionSlate,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            AnimatedVisibility(visible = showWhereIsPortHelper) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = FahsInstrumentCard,
                                    border = BorderStroke(1.dp, FahsInstrumentDivider)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ObdPortUnderSteeringVectorIllustration(
                                            modifier = Modifier.size(56.dp),
                                            strokeColor = FahsInstrumentText,
                                            highlightColor = FahsConnectionSlate
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = when (language) {
                                                AppLanguage.AR -> "يوجد المنفذ (16 فتحة) عادةً أسفل المقود فوق الدواسات، أو خلف غطاء علبة الفيوزات، وفي سيارات Dacia داخل صندوق القفازات (Boîte à gants)."
                                                AppLanguage.FR -> "La prise à 16 broches se trouve généralement sous le volant au-dessus des pédales, derrière la trappe à fusibles, ou dans la boîte à gants (Dacia)."
                                                AppLanguage.EN -> "The 16-pin socket is usually under the steering wheel above the pedals, behind the fuse cover, or inside the glovebox (on Dacia models)."
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = FahsInstrumentText,
                                            lineHeight = 18.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = FahsInstrumentDivider, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(16.dp))

                            // Bluetooth Connection Section inside the Instrument Card
                            if (!hasBtPermission) {
                                Button(
                                    onClick = {
                                        btPermissionLauncher.launch(
                                            BluetoothElm327Transport.requiredBluetoothPermissions()
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("grant_bt_permission_btn"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = FahsConnectionSlate,
                                        contentColor = FahsInk
                                    )
                                ) {
                                    Icon(imageVector = Icons.Default.Bluetooth, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = when (language) {
                                            AppLanguage.AR -> "السماح بالبلوتوث للاتصال بالفيشة"
                                            AppLanguage.FR -> "Autoriser le Bluetooth pour connecter le boîtier"
                                            AppLanguage.EN -> "Allow Bluetooth to connect to the dongle"
                                        },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                val savedAddr = rememberedDongleAddress
                                val savedName = rememberedDongleName ?: "OBDII"
                                val obdDevices = pairedDevices.filter { dev ->
                                    dev.isLikelyObdAdapter || (!savedAddr.isNullOrBlank() && dev.address.equals(savedAddr, ignoreCase = true))
                                }
                                val otherDevices = pairedDevices.filterNot { dev ->
                                    dev.isLikelyObdAdapter || (!savedAddr.isNullOrBlank() && dev.address.equals(savedAddr, ignoreCase = true))
                                }

                                // Remembered Dongle Banner if saved
                                if (!savedAddr.isNullOrBlank()) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp)
                                            .testTag("remembered_dongle_banner"),
                                        shape = RoundedCornerShape(12.dp),
                                        color = FahsInstrumentCard,
                                        border = BorderStroke(1.5.dp, FahsConnectionSlate)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = when (language) {
                                                            AppLanguage.AR -> "الفيشة المحفوظة: $savedName"
                                                            AppLanguage.FR -> "Boîtier mémorisé : $savedName"
                                                            AppLanguage.EN -> "Remembered dongle: $savedName"
                                                        },
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = FahsInstrumentText
                                                    )
                                                    Text(
                                                        text = savedAddr,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = FahsInstrumentMuted
                                                    )
                                                }
                                                OutlinedButton(
                                                    onClick = {
                                                        LanguagePreferences.clearLastSuccessfulDongle(context)
                                                        rememberedDongleAddress = null
                                                        rememberedDongleName = null
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    border = BorderStroke(1.dp, FahsInstrumentDivider),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = when (language) {
                                                            AppLanguage.AR -> "تغيير"
                                                            AppLanguage.FR -> "Changer"
                                                            AppLanguage.EN -> "Change"
                                                        },
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = FahsInstrumentText
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Button(
                                                onClick = {
                                                    runBluetoothScanOnDevice(savedAddr, savedName)
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(48.dp)
                                                    .testTag("connect_remembered_dongle_btn"),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = FahsConnectionSlate,
                                                    contentColor = FahsInk
                                                )
                                            ) {
                                                Text(
                                                    text = when (language) {
                                                        AppLanguage.AR -> "اتصال وبدء الفحص الآن"
                                                        AppLanguage.FR -> "Connecter et lancer le scan"
                                                        AppLanguage.EN -> "Connect and start scan"
                                                    },
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = when (language) {
                                            AppLanguage.AR -> "أجهزة الفحص المقترنة (${obdDevices.size})"
                                            AppLanguage.FR -> "Boîtiers détectés (${obdDevices.size})"
                                            AppLanguage.EN -> "Paired OBD dongles (${obdDevices.size})"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = FahsInstrumentText
                                    )

                                    OutlinedButton(
                                        onClick = {
                                            pairedDevices = BluetoothElm327Transport.getPairedDevices(context)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, FahsInstrumentDivider),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = FahsInstrumentText,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = when (language) {
                                                AppLanguage.AR -> "تحديث"
                                                AppLanguage.FR -> "Actualiser"
                                                AppLanguage.EN -> "Refresh"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = FahsInstrumentText
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (pairedDevices.isEmpty()) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = FahsInstrumentCard,
                                        border = BorderStroke(1.dp, FahsInstrumentDivider)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text(
                                                text = when (language) {
                                                    AppLanguage.AR -> "لا توجد فيشة مقترنة بعد. افتح بلوتوث الهاتف واقترن بالجهاز المسمى OBDII (الرمز 1234 أو 0000)، ثم عد إلى هنا."
                                                    AppLanguage.FR -> "Aucun boîtier appairé pour l'instant. Ouvrez le Bluetooth du téléphone et associez « OBDII » (code 1234 ou 0000), puis revenez ici."
                                                    AppLanguage.EN -> "No dongle paired yet. Open your phone's Bluetooth settings, pair with 'OBDII' (PIN 1234 or 0000), and return here."
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = FahsInstrumentText,
                                                lineHeight = 19.sp
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Button(
                                                onClick = {
                                                    BluetoothElm327Transport.openAndroidBluetoothSettings(context)
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(48.dp)
                                                    .testTag("open_android_bt_settings_btn"),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = FahsConnectionSlate,
                                                    contentColor = FahsInk
                                                )
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Bluetooth,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = when (language) {
                                                        AppLanguage.AR -> "فتح إعدادات البلوتوث للاقتران"
                                                        AppLanguage.FR -> "Ouvrir les paramètres Bluetooth"
                                                        AppLanguage.EN -> "Open Bluetooth settings to pair"
                                                    },
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            OutlinedButton(
                                                onClick = onOpenDongleBuyingGuide,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("obd_open_buying_guide_btn"),
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, FahsInstrumentDivider)
                                            ) {
                                                Text(
                                                    text = when (language) {
                                                        AppLanguage.AR -> "ليس لديك فيشة فحص؟ اطلب فيشة مجربة (توصيل 58 ولاية)"
                                                        AppLanguage.FR -> "Pas de boîtier ? Commander un modèle testé (58 wilayas)"
                                                        AppLanguage.EN -> "Need a dongle? Order a tested unit (58 wilayas)"
                                                    },
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = FahsConnectionSlate
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // Show likely OBD adapters first (or all devices if none matched the name filter)
                                    val primaryList = obdDevices.ifEmpty { pairedDevices }
                                    primaryList.forEach { dev: PairedBluetoothDevice ->
                                        PairedDeviceRow(
                                            dev = dev,
                                            isRemembered = !savedAddr.isNullOrBlank() && dev.address.equals(savedAddr, ignoreCase = true),
                                            isScanningBt = isScanningBt,
                                            language = language,
                                            onScanClick = { runBluetoothScanOnDevice(dev.address, dev.name) }
                                        )
                                    }

                                    // Tuck non-OBD Bluetooth devices into an expandable section when OBD devices exist
                                    if (obdDevices.isNotEmpty() && otherDevices.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { showOtherBtDevices = !showOtherBtDevices }
                                                .padding(vertical = 6.dp, horizontal = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = when (language) {
                                                    AppLanguage.AR -> "أجهزة بلوتوث أخرى (${otherDevices.size})"
                                                    AppLanguage.FR -> "Autres appareils Bluetooth (${otherDevices.size})"
                                                    AppLanguage.EN -> "Other Bluetooth devices (${otherDevices.size})"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = FahsInstrumentMuted
                                            )
                                            Icon(
                                                imageVector = if (showOtherBtDevices) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = null,
                                                tint = FahsInstrumentMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        AnimatedVisibility(visible = showOtherBtDevices) {
                                            Column {
                                                otherDevices.forEach { dev ->
                                                    PairedDeviceRow(
                                                        dev = dev,
                                                        isRemembered = false,
                                                        isScanningBt = isScanningBt,
                                                        language = language,
                                                        onScanClick = { runBluetoothScanOnDevice(dev.address, dev.name) }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = {
                                            BluetoothElm327Transport.openAndroidBluetoothSettings(context)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, FahsInstrumentDivider)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bluetooth,
                                            contentDescription = null,
                                            tint = FahsInstrumentText,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = when (language) {
                                                AppLanguage.AR -> "الفيشة غير ظاهرة؟ افتح إعدادات البلوتوث للاقتران"
                                                AppLanguage.FR -> "Boîtier absent ? Ouvrir les paramètres Bluetooth"
                                                AppLanguage.EN -> "Dongle not listed? Open Bluetooth settings"
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = FahsInstrumentText
                                        )
                                    }
                                }

                                if (btStatusMessage != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        color = FahsInstrumentCard,
                                        border = BorderStroke(1.dp, FahsInstrumentDivider)
                                    ) {
                                        Text(
                                            text = btStatusMessage.orEmpty(),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = FahsConnectionSlate,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // =========================================================================
                // AUTOMATIC TEST-DRIVE MONITORING CARD (Screen-Off Foreground Service)
                // =========================================================================
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("obd_test_drive_monitor_card"),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isMonitoringDrive) 1.5.dp else 1.dp,
                            color = if (isMonitoringDrive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when (language) {
                                        AppLanguage.AR -> "المراقبة التلقائية أثناء تجربة القيادة (الشاشة مغلقة)"
                                        AppLanguage.FR -> "Suivi automatique d'essai routier (écran éteint)"
                                        AppLanguage.EN -> "Automatic test-drive monitoring (screen off)"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = when (language) {
                                    AppLanguage.AR -> "اضغط «بدء تجربة القيادة» وضع الهاتف جانباً. يسجل التطبيق الحرارة، استقرار الدوران، الشحن، وأي عطل يظهر أثناء السير ثم يعرض الملخص لتأكيده."
                                    AppLanguage.FR -> "Appuyez sur « Démarrer l'essai routier » et posez le téléphone. L'application surveille la température, le ralenti, la charge et les défauts pendant le trajet."
                                    AppLanguage.EN -> "Tap 'Start test drive' and put the phone down. The app records temperature, idle steadiness, charging, and any faults during the drive."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )

                            if (showNotificationPermExplanation) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = when (language) {
                                                AppLanguage.AR -> "نطلب صلاحية الإشعارات لإبقاء مراقبة تجربة القيادة تعمل بموثوقية حتى عند إغلاق شاشة الهاتف."
                                                AppLanguage.FR -> "L'autorisation de notification permet de garder le suivi actif même quand l'écran est éteint."
                                                AppLanguage.EN -> "Notification permission keeps the road-test monitor active reliably while your screen is off."
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                showNotificationPermExplanation = false
                                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                                    notificationPermLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Text(
                                                text = when (language) {
                                                    AppLanguage.AR -> "موافق، ابدأ المراقبة"
                                                    AppLanguage.FR -> "Compris, démarrer"
                                                    AppLanguage.EN -> "Got it, start monitoring"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (!isMonitoringDrive) {
                                val targetDev = pairedDevices.firstOrNull {
                                    it.address.equals(rememberedDongleAddress, ignoreCase = true)
                                } ?: pairedDevices.firstOrNull { it.isLikelyObdAdapter } ?: pairedDevices.firstOrNull()

                                OutlinedButton(
                                    onClick = {
                                        if (!hasBtPermission) {
                                            btPermissionLauncher.launch(BluetoothElm327Transport.requiredBluetoothPermissions())
                                            return@OutlinedButton
                                        }
                                        if (targetDev == null) {
                                            btStatusMessage = when (language) {
                                                AppLanguage.AR -> "يرجى إقران فيشة ELM327 أولاً لبدء مراقبة تجربة القيادة."
                                                AppLanguage.FR -> "Veuillez d'abord appairer un boîtier ELM327 pour lancer l'essai routier."
                                                AppLanguage.EN -> "Please pair an ELM327 dongle first to start test-drive monitoring."
                                            }
                                            return@OutlinedButton
                                        }
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                                            androidx.core.content.ContextCompat.checkSelfPermission(
                                                context,
                                                android.Manifest.permission.POST_NOTIFICATIONS
                                            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                                        ) {
                                            showNotificationPermExplanation = true
                                        } else {
                                            ObdTestDriveMonitorService.startMonitoring(
                                                context = context,
                                                deviceAddress = targetDev.address,
                                                deviceName = targetDev.name,
                                                language = language
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("start_test_drive_btn"),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = when (language) {
                                            AppLanguage.AR -> "بدء تجربة القيادة (مراقبة تلقائية)"
                                            AppLanguage.FR -> "Démarrer l'essai routier (suivi auto)"
                                            AppLanguage.EN -> "Start test drive (automatic monitoring)"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = monitorStatusMsg.ifBlank {
                                                    when (language) {
                                                        AppLanguage.AR -> "مراقبة تجربة القيادة جارية ($monitorSamplesCount قراءة)..."
                                                        AppLanguage.FR -> "Suivi d'essai routier en cours ($monitorSamplesCount mesures)..."
                                                        AppLanguage.EN -> "Test-drive monitoring active ($monitorSamplesCount readings)..."
                                                    }
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(
                                            onClick = {
                                                ObdTestDriveMonitorService.stopMonitoring(context)
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("stop_test_drive_btn"),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = StatusErrorRed,
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Text(
                                                text = when (language) {
                                                    AppLanguage.AR -> "إنهاء تجربة القيادة وعرض الملخص"
                                                    AppLanguage.FR -> "Terminer l'essai routier & voir le bilan"
                                                    AppLanguage.EN -> "End test drive & view summary"
                                                },
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            val summary = completedDriveSummary
                            if (summary != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("test_drive_summary_confirmation_card"),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(
                                        1.5.dp,
                                        if (summary.hasAnyProblem) StatusWarningAmber else StatusPassGreen
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = when (language) {
                                                AppLanguage.AR -> "ملخص مراقبة تجربة القيادة (أكد لإضافته للتقرير):"
                                                AppLanguage.FR -> "Bilan de l'essai routier (confirmez pour l'ajouter au rapport) :"
                                                AppLanguage.EN -> "Test-drive monitoring summary (confirm to add to report):"
                                            },
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = summary.getSummaryLine(language),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (activeInspection != null) {
                                                Button(
                                                    onClick = {
                                                        onConfirmTestDriveSummary(summary)
                                                        ObdTestDriveMonitorService.clearCompletedSummary()
                                                    },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("confirm_test_drive_summary_btn"),
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.primary,
                                                        contentColor = Color.White
                                                    )
                                                ) {
                                                    Text(
                                                        text = when (language) {
                                                            AppLanguage.AR -> "تأكيد وإضافة"
                                                            AppLanguage.FR -> "Confirmer & Ajouter"
                                                            AppLanguage.EN -> "Confirm & Add"
                                                        },
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    ObdTestDriveMonitorService.clearCompletedSummary()
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text(
                                                    text = when (language) {
                                                        AppLanguage.AR -> "إغلاق"
                                                        AppLanguage.FR -> "Fermer"
                                                        AppLanguage.EN -> "Dismiss"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Guide & Port Locations Sub-Tab
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = when (language) {
                                    AppLanguage.AR -> "خطوات التوصيل البسيطة"
                                    AppLanguage.FR -> "Étapes simples de connexion"
                                    AppLanguage.EN -> "Simple connection steps"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            StepRowItem(
                                stepNumber = "1",
                                title = when (language) {
                                    AppLanguage.AR -> "ابحث عن منفذ الفحص تحت المقود"
                                    AppLanguage.FR -> "Localisez la prise sous le volant"
                                    AppLanguage.EN -> "Locate the socket under the steering wheel"
                                },
                                desc = when (language) {
                                    AppLanguage.AR -> "منفذ ذو 16 فتحة، يوجد عادة تحت المقود أو داخل صندوق القفازات (Boîte à gants)."
                                    AppLanguage.FR -> "Prise 16 broches trapézoïdale, sous le volant ou dans la boîte à gants."
                                    AppLanguage.EN -> "16-pin trapezoidal connector under the steering wheel or glovebox."
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            StepRowItem(
                                stepNumber = "2",
                                title = when (language) {
                                    AppLanguage.AR -> "شغّل المحرك وأوقف المكيف"
                                    AppLanguage.FR -> "Démarrez le moteur et coupez la clim"
                                    AppLanguage.EN -> "Start the engine and turn off A/C"
                                },
                                desc = when (language) {
                                    AppLanguage.AR -> "اترك المحرك يدور في وضع التوقف مع إيقاف المكيف لقياس استقرار الدوران بدقة."
                                    AppLanguage.FR -> "Laissez tourner au ralenti climatisation éteinte pour mesurer la régularité du moteur."
                                    AppLanguage.EN -> "Let the engine idle with A/C off so idle steadiness is measured accurately."
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            StepRowItem(
                                stepNumber = "3",
                                title = when (language) {
                                    AppLanguage.AR -> "اقترن بالبلوتوث برمز 1234 أو 0000"
                                    AppLanguage.FR -> "Appairez en Bluetooth (code 1234 ou 0000)"
                                    AppLanguage.EN -> "Pair Bluetooth with PIN 1234 or 0000"
                                },
                                desc = when (language) {
                                    AppLanguage.AR -> "في إعدادات البلوتوث بهاتفك، اختر OBDII وأدخل الرمز 1234."
                                    AppLanguage.FR -> "Dans les paramètres Bluetooth, sélectionnez OBDII et saisissez le code 1234."
                                    AppLanguage.EN -> "In your phone's Bluetooth settings, select OBDII and enter PIN 1234."
                                }
                            )
                        }
                    }
                }

                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when (language) {
                                        AppLanguage.AR -> "أماكن المنفذ في أشهر السيارات بالجزائر"
                                        AppLanguage.FR -> "Emplacement de la prise sur les modèles populaires"
                                        AppLanguage.EN -> "Port locations in popular Algerian cars"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            PortLocationItem(
                                model = "Renault Symbol / Clio 4",
                                location = when (language) {
                                    AppLanguage.AR -> "في أسفل الكونسول الأوسط أمام عصا القير، أو تحت غطاء الفيوزات."
                                    AppLanguage.FR -> "Au bas de la console centrale devant le levier, ou boîte à fusibles."
                                    AppLanguage.EN -> "Base of center console in front of gear lever, or behind fuse box."
                                }
                            )

                            PortLocationItem(
                                model = "Peugeot 208 / 301 / Citroën C-Elysée",
                                location = when (language) {
                                    AppLanguage.AR -> "أسفل الطابلوه على يسار المقود (خلف غطاء الفيوزات)."
                                    AppLanguage.FR -> "Sous le tableau de bord à gauche du volant, derrière le cache fusibles."
                                    AppLanguage.EN -> "Under the dashboard to the left of the steering wheel behind fuse panel."
                                }
                            )

                            PortLocationItem(
                                model = "Dacia Logan / Sandero / Duster",
                                location = when (language) {
                                    AppLanguage.AR -> "داخل صندوق القفازات (Boîte à gants) بالجهة اليمنى."
                                    AppLanguage.FR -> "À l'intérieur de la boîte à gants côté passager."
                                    AppLanguage.EN -> "Inside the glovebox compartment on the passenger side."
                                }
                            )

                            PortLocationItem(
                                model = "Volkswagen Golf 7 / Polo / Seat Ibiza",
                                location = when (language) {
                                    AppLanguage.AR -> "أسفل مقود السائق مباشرة بالجهة اليسرى مفتوح بلون بنفسجي."
                                    AppLanguage.FR -> "Sous le volant à gauche, prise violette visible sans démontage."
                                    AppLanguage.EN -> "Under the steering wheel on the left side, visible purple socket."
                                }
                            )

                            PortLocationItem(
                                model = "Hyundai Accent / i10 / Kia Picanto",
                                location = when (language) {
                                    AppLanguage.AR -> "خلف باب علبة الفيوزات الداخلية أسفل يسار المقود."
                                    AppLanguage.FR -> "Derrière la trappe à fusibles sous le volant à gauche."
                                    AppLanguage.EN -> "Behind the interior fuse box door under the steering wheel."
                                }
                            )
                        }
                    }
                }

                item {
                    com.example.ui.components.DongleBuyingGuideContent(
                        language = language,
                        shopSettings = shopSettings
                    )
                }
            }
        }
    }
}

/**
 * Full-screen dark Instrument Panel (#0E1F1A) shown while the scan is running.
 * Uses neutral pale grey-blue (#D6E2EE) for the 6-segment progress arc so green (#1E9E5A)
 * only ever means a result.
 */
@Composable
fun ScanInProgressInstrumentPanel(
    language: AppLanguage,
    currentStepIndex: Int,
    carHeaderLabel: String = "",
    modifier: Modifier = Modifier
) {
    val safeStep = currentStepIndex.coerceIn(1, 6)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val steps = remember(language) {
        listOf(
            when (language) {
                AppLanguage.AR -> "الاتصال بكمبيوتر المحرك"
                AppLanguage.FR -> "Connexion au calculateur du moteur"
                AppLanguage.EN -> "Connecting to the engine computer"
            },
            when (language) {
                AppLanguage.AR -> "قراءة الأعطال المسجلة"
                AppLanguage.FR -> "Lecture des défauts enregistrés"
                AppLanguage.EN -> "Reading stored faults"
            },
            when (language) {
                AppLanguage.AR -> "التحقق من مسح الأعطال مؤخراً"
                AppLanguage.FR -> "Vérification d'un effacement récent"
                AppLanguage.EN -> "Checking if faults were erased recently"
            },
            when (language) {
                AppLanguage.AR -> "قراءة حرارة المحرك واستقرار الدوران"
                AppLanguage.FR -> "Lecture de la température et du ralenti"
                AppLanguage.EN -> "Reading engine temperature and idle steadiness"
            },
            when (language) {
                AppLanguage.AR -> "فحص البطارية والشحن"
                AppLanguage.FR -> "Vérification de la batterie et de la charge"
                AppLanguage.EN -> "Checking battery and charging"
            },
            when (language) {
                AppLanguage.AR -> "قراءة رقم الشاسيه في الكمبيوتر"
                AppLanguage.FR -> "Lecture du numéro de châssis dans le calculateur"
                AppLanguage.EN -> "Reading the chassis number in the computer"
            }
        )
    }

    Column(
        modifier = modifier
            .background(FahsInk)
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .testTag("obd_scan_in_progress_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (language) {
                    AppLanguage.AR -> "فحص المحرك والإلكترونيات"
                    AppLanguage.FR -> "Scan moteur et électronique"
                    AppLanguage.EN -> "Engine & electronics scan"
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = FahsInstrumentText
            )
            if (carHeaderLabel.isNotBlank()) {
                Text(
                    text = carHeaderLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = FahsInstrumentMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 260° 6-Segment Instrument Dial with neutral #D6E2EE progress arc
        Box(
            modifier = Modifier
                .size(210.dp)
                .testTag("obd_instrument_dial"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokePx = 12.dp.toPx()
                val diameter = size.minDimension - strokePx * 2
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                val arcSize = Size(diameter, diameter)

                // Total 260 degrees from 140° to 400° (with 6 segments separated by 6° gaps)
                val totalArcDeg = 260f
                val gapDeg = 7f
                val segmentSweep = (totalArcDeg - gapDeg * 5f) / 6f

                for (i in 0 until 6) {
                    val isFilled = (i + 1) <= safeStep
                    val segColor = if (isFilled) FahsConnectionSlate else FahsInstrumentDivider
                    val startAngle = if (!isRtl) {
                        140f + i * (segmentSweep + gapDeg)
                    } else {
                        // Counter-clockwise sweep in Arabic RTL starting from 40° (bottom-right)
                        40f - i * (segmentSweep + gapDeg) - segmentSweep
                    }
                    drawArc(
                        color = segColor,
                        startAngle = startAngle,
                        sweepAngle = segmentSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$safeStep / 6",
                    style = FahsTypography.instrumentHeroNumberStyle.copy(fontSize = 36.sp),
                    color = FahsInstrumentText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (language) {
                        AppLanguage.AR -> "قراءة مباشرة من السيارة"
                        AppLanguage.FR -> "Lecture directe de la voiture"
                        AppLanguage.EN -> "Direct reading from the car"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = FahsInstrumentMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Active Step Headline + Calm Instruction
        Text(
            text = steps[safeStep - 1],
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = FahsInstrumentText,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = when (language) {
                AppLanguage.AR -> "اترك المحرك شغالاً وأوقف المكيف. يستغرق الفحص حوالي دقيقة واحدة."
                AppLanguage.FR -> "Laissez le moteur tourner et coupez la climatisation. Le scan dure environ une minute."
                AppLanguage.EN -> "Keep the engine running and turn off the air conditioning. The scan takes about one minute."
            },
            style = MaterialTheme.typography.bodySmall,
            color = FahsInstrumentMuted,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp
        )

        Spacer(modifier = Modifier.height(22.dp))

        // Hairline-Separated 6 Plain-Language Steps List
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = FahsInstrumentCard,
            border = BorderStroke(1.dp, FahsInstrumentDivider)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                steps.forEachIndexed { idx, stepTitle ->
                    val stepNumber = idx + 1
                    val isDone = stepNumber < safeStep
                    val isCurrent = stepNumber == safeStep

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isDone -> FahsConnectionSlate.copy(alpha = 0.22f)
                                        isCurrent -> FahsConnectionSlate
                                        else -> FahsInstrumentDivider
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDone) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = FahsConnectionSlate,
                                    modifier = Modifier.size(14.dp)
                                )
                            } else {
                                Text(
                                    text = stepNumber.toString(),
                                    style = FahsTypography.tabularValueStyle.copy(fontSize = 11.sp),
                                    color = if (isCurrent) FahsInk else FahsInstrumentMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = stepTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                isCurrent -> FahsInstrumentText
                                isDone -> FahsInstrumentText.copy(alpha = 0.85f)
                                else -> FahsInstrumentMuted
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (idx < steps.lastIndex) {
                        HorizontalDivider(color = FahsInstrumentDivider, thickness = 1.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PreparationStepLine(
    number: String,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(FahsInstrumentCard),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = FahsTypography.tabularValueStyle.copy(fontSize = 12.sp),
                color = FahsConnectionSlate
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = FahsInstrumentText,
            lineHeight = 19.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PairedDeviceRow(
    dev: PairedBluetoothDevice,
    isRemembered: Boolean,
    isScanningBt: Boolean,
    language: AppLanguage,
    onScanClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = FahsInstrumentCard,
        border = BorderStroke(
            width = if (isRemembered || dev.isLikelyObdAdapter) 1.5.dp else 1.dp,
            color = if (isRemembered || dev.isLikelyObdAdapter) FahsConnectionSlate else FahsInstrumentDivider
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dev.name + if (isRemembered) " ★" else "",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = FahsInstrumentText
                )
                Text(
                    text = dev.address,
                    style = MaterialTheme.typography.labelSmall,
                    color = FahsInstrumentMuted
                )
            }

            Button(
                onClick = onScanClick,
                enabled = !isScanningBt,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FahsConnectionSlate,
                    contentColor = FahsInk
                )
            ) {
                Text(
                    text = when (language) {
                        AppLanguage.AR -> "فحص الآن"
                        AppLanguage.FR -> "Scanner"
                        AppLanguage.EN -> "Scan"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ObdModeTabButton(
    title: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
fun StepRowItem(
    stepNumber: String,
    title: String,
    desc: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun PortLocationItem(
    model: String,
    location: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = model,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = location,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
        )
    }
}
