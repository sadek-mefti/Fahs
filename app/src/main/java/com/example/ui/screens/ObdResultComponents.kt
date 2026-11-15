package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.i18n.AppLanguage
import com.example.data.model.ItemSeverity
import com.example.domain.AutomaticScanVerdictLevel
import com.example.domain.ClearedCodesAlertLevel
import com.example.domain.DtcInfo
import com.example.domain.EcuVinComparisonChoice
import com.example.domain.MilKeyOnUserAnswer
import com.example.domain.ObdDiagnosticEngine
import com.example.domain.ObdScanResult
import com.example.domain.ObdVerdictThresholds
import com.example.ui.components.EngineWarningLightVectorIllustration
import com.example.ui.theme.FahsConnectionSlate
import com.example.ui.theme.FahsInk
import com.example.ui.theme.FahsInstrumentCard
import com.example.ui.theme.FahsInstrumentDivider
import com.example.ui.theme.FahsInstrumentMuted
import com.example.ui.theme.FahsInstrumentText
import com.example.ui.theme.FahsTypography
import com.example.ui.theme.StatusErrorRed
import com.example.ui.theme.StatusErrorRedContainer
import com.example.ui.theme.StatusErrorRedOnContainer
import com.example.ui.theme.StatusNeutralGrey
import com.example.ui.theme.StatusNeutralGreyContainer
import com.example.ui.theme.StatusPassGreen
import com.example.ui.theme.StatusPassGreenContainer
import com.example.ui.theme.StatusPassGreenOnContainer
import com.example.ui.theme.StatusWarningAmber
import com.example.ui.theme.StatusWarningAmberContainer
import com.example.ui.theme.StatusWarningAmberOnContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class ExplainSheetInfo(
    val title: String,
    val body: String,
    val mechanicTip: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ObdScanResultSection(
    scanResult: ObdScanResult,
    language: AppLanguage,
    carHeaderLabel: String = "",
    expectedVin: String = "",
    ecuVinComparison: EcuVinComparisonChoice = EcuVinComparisonChoice.COULD_NOT_COMPARE,
    onEcuVinComparisonChange: (EcuVinComparisonChoice) -> Unit = {},
    milBulbLitOnDashboard: Boolean = true,
    onMilBulbToggle: (Boolean) -> Unit = {},
    milKeyOnAnswer: MilKeyOnUserAnswer = if (milBulbLitOnDashboard) {
        MilKeyOnUserAnswer.LIT_UP_NORMAL
    } else {
        MilKeyOnUserAnswer.NEVER_LIT_UP
    },
    onMilKeyOnAnswerChange: ((MilKeyOnUserAnswer) -> Unit)? = null,
    hasActiveInspection: Boolean,
    onSaveToInspection: () -> Unit,
    onSaveAndGetReport: (() -> Unit)? = null,
    onAddCarModelForTips: (() -> Unit)? = null,
    onContinueThreeMinuteVisualCheck: (() -> Unit)? = null,
    onStartFullManualInspection: (() -> Unit)? = null,
    isDeveloperModeEnabled: Boolean = false,
    onExportRawLog: () -> Unit = {}
) {
    var activeExplainInfo by remember { mutableStateOf<ExplainSheetInfo?>(null) }
    var mechanicDetailsExpanded by remember { mutableStateOf(false) }

    val verdictLevel = remember(scanResult, milKeyOnAnswer, ecuVinComparison) {
        ObdDiagnosticEngine.evaluateAutomaticScanVerdict(
            scanResult = scanResult,
            milKeyOnAnswer = milKeyOnAnswer,
            ecuVinComparison = ecuVinComparison
        )
    }
    val verdictSummaryLine = remember(scanResult, milKeyOnAnswer, ecuVinComparison, language) {
        ObdDiagnosticEngine.buildVerdictSummaryLine(
            scanResult = scanResult,
            milKeyOnAnswer = milKeyOnAnswer,
            ecuVinComparison = ecuVinComparison,
            language = language
        )
    }

    val explainTarget = activeExplainInfo
    if (explainTarget != null) {
        AlertDialog(
            onDismissRequest = { activeExplainInfo = null },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = {
                Text(
                    text = explainTarget.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = explainTarget.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 21.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = when (language) {
                                    AppLanguage.AR -> "ماذا تسأل الميكانيكي؟"
                                    AppLanguage.FR -> "Que demander au mécanicien ?"
                                    AppLanguage.EN -> "What to ask a mechanic"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = explainTarget.mechanicTip,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeExplainInfo = null }) {
                    Text(
                        text = when (language) {
                            AppLanguage.AR -> "فهمت"
                            AppLanguage.FR -> "Compris"
                            AppLanguage.EN -> "Got it"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }

    val timestampLabel = remember {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(Date())
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("obd_scan_result_root"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // =========================================================================
        // 1. TOP DARK INSTRUMENT PANEL VERDICT HEADER (#0E1F1A)
        // =========================================================================
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("obd_verdict_header_card"),
            shape = RoundedCornerShape(18.dp),
            color = FahsInk,
            border = BorderStroke(1.dp, FahsInstrumentDivider)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Car / Date-Time Metadata Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val headerText = if (carHeaderLabel.isNotBlank()) {
                        "$carHeaderLabel · $timestampLabel"
                    } else {
                        when (language) {
                            AppLanguage.AR -> "فحص المحرك والإلكترونيات · $timestampLabel"
                            AppLanguage.FR -> "Scan moteur et électronique · $timestampLabel"
                            AppLanguage.EN -> "Engine & electronics scan · $timestampLabel"
                        }
                    }
                    Text(
                        text = headerText,
                        style = MaterialTheme.typography.labelMedium,
                        color = FahsInstrumentMuted,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (onAddCarModelForTips != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onAddCarModelForTips)
                            .testTag("verdict_add_car_model_chip"),
                        shape = RoundedCornerShape(8.dp),
                        color = FahsInstrumentCard,
                        border = BorderStroke(1.dp, FahsInstrumentDivider)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = FahsConnectionSlate,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (language) {
                                    AppLanguage.AR -> "أضف موديل السيارة للحصول على النصائح الخاصة بها"
                                    AppLanguage.FR -> "Ajouter le modèle de la voiture pour les conseils adaptés"
                                    AppLanguage.EN -> "Add the car model to get tips specific to it"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = FahsConnectionSlate
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Verdict Band (Strictly one of the 4 levels: Green, Orange, Red, Grey)
                val (verdictBg, verdictDotColor, verdictTitle) = when (verdictLevel) {
                    AutomaticScanVerdictLevel.NO_PROBLEM_DETECTED -> Triple(
                        StatusPassGreen.copy(alpha = 0.18f),
                        StatusPassGreen,
                        when (language) {
                            AppLanguage.AR -> "لم يكشف الفحص عن أي مشكلة"
                            AppLanguage.FR -> "Aucun problème détecté par le scan"
                            AppLanguage.EN -> "No problem detected by the scan"
                        }
                    )
                    AutomaticScanVerdictLevel.POINTS_TO_CHECK -> Triple(
                        StatusWarningAmber.copy(alpha = 0.20f),
                        StatusWarningAmber,
                        when (language) {
                            AppLanguage.AR -> "بعض النقاط تستوجب التحقق"
                            AppLanguage.FR -> "Quelques points à vérifier"
                            AppLanguage.EN -> "Some points to check"
                        }
                    )
                    AutomaticScanVerdictLevel.SERIOUS_WARNING -> Triple(
                        StatusErrorRed.copy(alpha = 0.22f),
                        StatusErrorRed,
                        when (language) {
                            AppLanguage.AR -> "تنبيه هام"
                            AppLanguage.FR -> "Alerte importante"
                            AppLanguage.EN -> "Serious warning"
                        }
                    )
                    AutomaticScanVerdictLevel.INCOMPLETE -> Triple(
                        StatusNeutralGrey.copy(alpha = 0.22f),
                        StatusNeutralGrey,
                        when (language) {
                            AppLanguage.AR -> "فحص غير مكتمل"
                            AppLanguage.FR -> "Scan incomplet"
                            AppLanguage.EN -> "Incomplete scan"
                        }
                    )
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("obd_verdict_band"),
                    shape = RoundedCornerShape(14.dp),
                    color = verdictBg,
                    border = BorderStroke(1.5.dp, verdictDotColor)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val dialGlyph = when (verdictLevel) {
                            AutomaticScanVerdictLevel.NO_PROBLEM_DETECTED -> "✓"
                            AutomaticScanVerdictLevel.POINTS_TO_CHECK,
                            AutomaticScanVerdictLevel.SERIOUS_WARNING -> "!"
                            AutomaticScanVerdictLevel.INCOMPLETE -> "?"
                        }
                        Surface(
                            modifier = Modifier.size(38.dp),
                            shape = CircleShape,
                            color = verdictDotColor.copy(alpha = 0.24f),
                            border = BorderStroke(2.dp, verdictDotColor)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = dialGlyph,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = FahsInstrumentText
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = verdictTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = FahsInstrumentText
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = verdictSummaryLine,
                                style = MaterialTheme.typography.bodySmall,
                                color = FahsInstrumentText.copy(alpha = 0.88f),
                                lineHeight = 18.sp,
                                modifier = Modifier.testTag("obd_verdict_summary_line")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mandatory Fixed Scope Line (Always shown directly underneath the verdict)
                Text(
                    text = when (language) {
                        AppLanguage.AR -> "يغطي هذا الفحص المحرك والإلكترونيات فقط. لا يمكنه رؤية الهيكل أو الدبرياج أو نظام التعليق أو الوسائد الهوائية (Airbag) أو الفرامل (ABS)."
                        AppLanguage.FR -> "Ce résultat concerne uniquement le moteur et l'électronique. Le scan ne voit ni la carrosserie, ni l'embrayage, ni la suspension, ni les airbags ou l'ABS."
                        AppLanguage.EN -> "This result covers the engine and electronics only. The scan cannot see bodywork, clutch, suspension, airbags, or ABS."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = FahsInstrumentText.copy(alpha = 0.90f),
                    lineHeight = 19.sp,
                    modifier = Modifier.testTag("obd_mandatory_scope_line")
                )

                if (!scanResult.isCommunicationSuccess) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = FahsInstrumentCard,
                        border = BorderStroke(1.dp, FahsInstrumentDivider)
                    ) {
                        Text(
                            text = when (language) {
                                AppLanguage.AR -> "لم يستجب كمبيوتر المحرك لبروتوكول الفحص القياسي (شائع في بعض السيارات قبل 2004). يمكنك متابعة الفحص البصري الموجه لمدة 3 دقائق أدناه."
                                AppLanguage.FR -> "Le calculateur moteur n'a pas répondu au standard OBD (fréquent sur certaines voitures d'avant 2004). Vous pouvez poursuivre avec la vérification visuelle de 3 minutes ci-dessous."
                                AppLanguage.EN -> "The engine computer did not respond to the standard scan (common on some pre-2004 cars). You can continue with the 3-minute visual check below."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = FahsInstrumentMuted,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        if (!scanResult.isCommunicationSuccess) {
            if (onContinueThreeMinuteVisualCheck != null) {
                OutlinedButton(
                    onClick = onContinueThreeMinuteVisualCheck,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("continue_visual_check_after_scan_btn"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        text = when (language) {
                            AppLanguage.AR -> "متابعة الفحص البصري السريع (الهيكل، الإطارات، الوثائق)"
                            AppLanguage.FR -> "Continuer avec le contrôle visuel de 3 min (carrosserie, pneus, papiers)"
                            AppLanguage.EN -> "Continue with 3-minute visual check (bodywork, tyres, papers)"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            if (isDeveloperModeEnabled && scanResult.rawExchangeLog.isNotEmpty()) {
                DeveloperRawExchangeLogCard(
                    exchangeCount = scanResult.rawExchangeLog.size,
                    language = language,
                    onExportRawLog = onExportRawLog
                )
            }
            return@Column
        }

        // =========================================================================
        // 2. THE 8 PLAIN-LANGUAGE RESULT ROWS SEPARATED BY 1DP HAIRLINES
        // =========================================================================
        val notAvailableText = ObdDiagnosticEngine.notAvailableLabel(language)

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("obd_eight_rows_container"),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // -----------------------------------------------------------------
                // ROW 1: Stored faults (Défauts enregistrés / الأعطال المسجلة)
                // -----------------------------------------------------------------
                val dtcCount = scanResult.dtcList.size
                val hasCriticalDtc = scanResult.dtcList.any { it.severity == ItemSeverity.CRITICAL }
                val row1Dot = when {
                    dtcCount == 0 -> StatusPassGreen
                    hasCriticalDtc -> StatusErrorRed
                    else -> StatusWarningAmber
                }
                val row1Value = when {
                    dtcCount == 0 -> when (language) {
                        AppLanguage.AR -> "0 عطل"
                        AppLanguage.FR -> "Aucun (0)"
                        AppLanguage.EN -> "None (0)"
                    }
                    else -> when (language) {
                        AppLanguage.AR -> "$dtcCount أعطال مسجلة"
                        AppLanguage.FR -> "$dtcCount défaut(s)"
                        AppLanguage.EN -> "$dtcCount fault(s)"
                    }
                }
                val row1Subtitle = when {
                    dtcCount == 0 -> when (language) {
                        AppLanguage.AR -> "لا توجد أعطال مسجلة في كمبيوتر المحرك."
                        AppLanguage.FR -> "Aucun défaut enregistré dans le calculateur du moteur."
                        AppLanguage.EN -> "No fault codes stored in the engine computer."
                    }
                    else -> scanResult.dtcList.joinToString(" · ") { it.getTitle(language) }
                }

                PlainScanResultRow(
                    rowId = "stored_faults",
                    dotColor = row1Dot,
                    title = when (language) {
                        AppLanguage.AR -> "الأعطال المسجلة"
                        AppLanguage.FR -> "Défauts enregistrés"
                        AppLanguage.EN -> "Stored faults"
                    },
                    subtitle = row1Subtitle,
                    valueBadgeText = row1Value,
                    language = language,
                    onExplainClick = {
                        activeExplainInfo = ExplainSheetInfo(
                            title = when (language) {
                                AppLanguage.AR -> "الأعطال المسجلة في كمبيوتر المحرك"
                                AppLanguage.FR -> "Défauts enregistrés dans le calculateur"
                                AppLanguage.EN -> "Stored faults in the engine computer"
                            },
                            body = when (language) {
                                AppLanguage.AR -> "يسجل كمبيوتر المحرك كود عطل كلما اكتشف حساسٌ قراءة غير طبيعية (في الحقن، التيربو، الإشعال أو التلوث). تشمل هذه القراءة الأعطال المؤكدة والمؤقتة والدائمة."
                                AppLanguage.FR -> "Le calculateur garde en mémoire un code chaque fois qu'un capteur détecte une anomalie (injection, turbo, allumage, antipollution). Ce contrôle inclut les défauts confirmés, en attente et permanents."
                                AppLanguage.EN -> "The engine computer stores a fault code whenever a sensor detects an abnormal reading (injection, turbo, ignition, emissions). This check covers confirmed, pending, and permanent faults."
                            },
                            mechanicTip = when (language) {
                                AppLanguage.AR -> "إذا وُجد عطل، اعرض قسم «تفاصيل للميكانيكي» في الأسفل على الميكانيكي ليفحص القطعة المعنية قبل الشراء."
                                AppLanguage.FR -> "S'il y a un défaut, montrez la section « Détails pour un mécanicien » en bas de l'écran pour vérifier la pièce concernée avant d'acheter."
                                AppLanguage.EN -> "If a fault is listed, show the 'Details for a mechanic' section at the bottom to your mechanic so they can inspect the affected part before you buy."
                            }
                        )
                    }
                ) {
                    if (scanResult.dtcList.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            scanResult.dtcList.forEach { dtc ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = dtc.getTitle(language),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = dtc.getExplanation(language),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

                // -----------------------------------------------------------------
                // ROW 2: Faults erased recently? (Défauts effacés récemment ?)
                // -----------------------------------------------------------------
                val alertLevel = scanResult.clearedCodesAlertLevel
                val distKm = scanResult.distanceSinceCodesClearedKm
                val warmups = scanResult.warmupsSinceCodesCleared
                val row2Dot = when (alertLevel) {
                    ClearedCodesAlertLevel.NONE_NORMAL -> StatusPassGreen
                    ClearedCodesAlertLevel.STRONG_WARNING,
                    ClearedCodesAlertLevel.CANNOT_RULE_OUT -> StatusWarningAmber
                    ClearedCodesAlertLevel.NOT_AVAILABLE -> StatusNeutralGrey
                }
                val row2Value = when (alertLevel) {
                    ClearedCodesAlertLevel.NONE_NORMAL -> {
                        if (distKm != null) {
                            when (language) {
                                AppLanguage.AR -> "لا ($distKm كم)"
                                AppLanguage.FR -> "Non ($distKm km)"
                                AppLanguage.EN -> "No ($distKm km)"
                            }
                        } else {
                            when (language) {
                                AppLanguage.AR -> "لا (طبيعي)"
                                AppLanguage.FR -> "Non (normal)"
                                AppLanguage.EN -> "No (normal)"
                            }
                        }
                    }
                    ClearedCodesAlertLevel.STRONG_WARNING -> {
                        when {
                            distKm != null -> when (language) {
                                AppLanguage.AR -> "نعم (قبل $distKm كم)"
                                AppLanguage.FR -> "Oui (il y a $distKm km)"
                                AppLanguage.EN -> "Yes ($distKm km ago)"
                            }
                            warmups != null -> when (language) {
                                AppLanguage.AR -> "نعم ($warmups تسخين)"
                                AppLanguage.FR -> "Oui ($warmups cycles)"
                                AppLanguage.EN -> "Yes ($warmups warm-ups)"
                            }
                            else -> when (language) {
                                AppLanguage.AR -> "مسح حديث"
                                AppLanguage.FR -> "Effacé récemment"
                                AppLanguage.EN -> "Erased recently"
                            }
                        }
                    }
                    ClearedCodesAlertLevel.CANNOT_RULE_OUT -> when (language) {
                        AppLanguage.AR -> "للتحقق"
                        AppLanguage.FR -> "À vérifier"
                        AppLanguage.EN -> "To check"
                    }
                    ClearedCodesAlertLevel.NOT_AVAILABLE -> notAvailableText
                }
                val row2Subtitle = when (alertLevel) {
                    ClearedCodesAlertLevel.NONE_NORMAL -> when (language) {
                        AppLanguage.AR -> "لم يتم مسح ذاكرة الأعطال مؤخراً."
                        AppLanguage.FR -> "La mémoire des défauts n'a pas été remise à zéro récemment."
                        AppLanguage.EN -> "Fault memory has not been erased recently."
                    }
                    ClearedCodesAlertLevel.STRONG_WARNING -> when (language) {
                        AppLanguage.AR -> "تمت إعادة ضبط الذاكرة منذ مسافة قصيرة (بعد صيانة أو فصل البطارية). قُد السيارة 20 دقيقة وأعد الفحص."
                        AppLanguage.FR -> "La mémoire a été remise à zéro il y a peu (entretien ou batterie débranchée). Roulez 20 min puis relancez le scan."
                        AppLanguage.EN -> "Memory was reset a short distance ago (service or battery disconnect). Drive for 20 minutes and scan again."
                    }
                    ClearedCodesAlertLevel.CANNOT_RULE_OUT -> when (language) {
                        AppLanguage.AR -> "بعض اختبارات الفحص الذاتي لم تكتمل بعد؛ يُنصح بتجربة قيادة السيارة."
                        AppLanguage.FR -> "Certains tests internes ne sont pas encore terminés ; faites un essai sur route."
                        AppLanguage.EN -> "Some internal self-tests are not yet complete; take a road test."
                    }
                    ClearedCodesAlertLevel.NOT_AVAILABLE -> when (language) {
                        AppLanguage.AR -> "لا توفر هذه السيارة عداد المسافة منذ آخر مسح (لا يؤثر على النتيجة)."
                        AppLanguage.FR -> "Cette voiture ne fournit pas la distance depuis le dernier effacement (sans effet sur le verdict)."
                        AppLanguage.EN -> "This car does not report distance since last reset (does not affect the verdict)."
                    }
                }

                PlainScanResultRow(
                    rowId = "cleared_recently",
                    dotColor = row2Dot,
                    title = when (language) {
                        AppLanguage.AR -> "هل تم مسح الأعطال مؤخراً؟"
                        AppLanguage.FR -> "Défauts effacés récemment ?"
                        AppLanguage.EN -> "Faults erased recently?"
                    },
                    subtitle = row2Subtitle,
                    valueBadgeText = row2Value,
                    language = language,
                    rowTestTag = "obd_cleared_fraud_card",
                    onExplainClick = {
                        activeExplainInfo = ExplainSheetInfo(
                            title = when (language) {
                                AppLanguage.AR -> "هل تم مسح الأعطال مؤخراً؟"
                                AppLanguage.FR -> "Vérification d'un effacement récent"
                                AppLanguage.EN -> "Checking if faults were erased recently"
                            },
                            body = when (language) {
                                AppLanguage.AR -> "عند مسح الأعطال بجهاز فحص أو فصل البطارية، يعود عداد المسافة منذ المسح إلى الصفر. إذا كانت المسافة قصيرة جداً (أقل من 50 كم)، فقد لا تكون الأعطال المخفية قد ظهرت بعد."
                                AppLanguage.FR -> "Quand on efface les défauts ou qu'on débranche la batterie, le compteur de distance depuis remise à zéro repart à 0 km. Si la voiture a très peu roulé depuis (< 50 km), un défaut intermittent peut ne pas être encore réapparu."
                                AppLanguage.EN -> "When fault codes are erased or the battery is disconnected, the distance-since-reset counter drops to 0 km. If the car has barely been driven since (< 50 km), an intermittent fault may not have returned yet."
                            },
                            mechanicTip = when (language) {
                                AppLanguage.AR -> "اطلب قيادة السيارة لمدة 20 إلى 30 دقيقة ثم أعد الفحص التلقائي للتأكد من عدم عودة أي عطل."
                                AppLanguage.FR -> "Demandez à faire un essai routier de 20 à 30 minutes puis relancez le scan pour vérifier qu'aucun défaut ne revient."
                                AppLanguage.EN -> "Ask to take a 20–30 minute test drive and run the scan again afterwards to confirm no fault returns."
                            }
                        )
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

                // -----------------------------------------------------------------
                // ROW 3: Engine warning light (Voyant moteur / لمبة تحذير المحرك)
                // Rule #5: 3 answers ("Yes, it lit up", "No, it never lights up", "I did not look")
                // -----------------------------------------------------------------
                val isMilCommandedOn = scanResult.milCommandedOn == true
                val row3Dot = when {
                    isMilCommandedOn || milKeyOnAnswer == MilKeyOnUserAnswer.NEVER_LIT_UP -> StatusErrorRed
                    milKeyOnAnswer == MilKeyOnUserAnswer.DID_NOT_LOOK -> StatusNeutralGrey
                    else -> StatusPassGreen
                }
                val row3Value = when {
                    isMilCommandedOn -> when (language) {
                        AppLanguage.AR -> "مضاءة (تنبيه)"
                        AppLanguage.FR -> "Allumé (alerte)"
                        AppLanguage.EN -> "ON (warning)"
                    }
                    milKeyOnAnswer == MilKeyOnUserAnswer.NEVER_LIT_UP -> when (language) {
                        AppLanguage.AR -> "لا تضيء أبداً"
                        AppLanguage.FR -> "Jamais allumé"
                        AppLanguage.EN -> "Never lights up"
                    }
                    milKeyOnAnswer == MilKeyOnUserAnswer.DID_NOT_LOOK -> when (language) {
                        AppLanguage.AR -> "لم يتم التحقق"
                        AppLanguage.FR -> "Non vérifié"
                        AppLanguage.EN -> "Not checked"
                    }
                    else -> when (language) {
                        AppLanguage.AR -> "طبيعي (منطفئة)"
                        AppLanguage.FR -> "Normal (éteint)"
                        AppLanguage.EN -> "Normal (OFF)"
                    }
                }
                val row3Subtitle = when {
                    isMilCommandedOn -> when (language) {
                        AppLanguage.AR -> "كمبيوتر المحرك يطلب إضاءة لمبة التحذير في الطابلوه."
                        AppLanguage.FR -> "Le calculateur commande l'allumage du voyant moteur au tableau de bord."
                        AppLanguage.EN -> "The engine computer is commanding the dashboard warning light ON."
                    }
                    milKeyOnAnswer == MilKeyOnUserAnswer.NEVER_LIT_UP -> when (language) {
                        AppLanguage.AR -> "تنبيه هام: لمبة المحرك لا تضيء عند فتح الكونتاكت (قد تكون معطلة أو مغطاة)."
                        AppLanguage.FR -> "Alerte : le voyant ne s'allume pas au contact (ampoule/LED possiblement hors service ou masquée)."
                        AppLanguage.EN -> "Warning: the warning light never comes on at key-on (bulb/LED may be faulty or covered)."
                    }
                    milKeyOnAnswer == MilKeyOnUserAnswer.DID_NOT_LOOK -> when (language) {
                        AppLanguage.AR -> "لم يتم التحقق من إضاءة اللمبة عند فتح الكونتاكت (لا يؤثر على النتيجة)."
                        AppLanguage.FR -> "Présence du voyant au contact non vérifiée (sans effet sur le verdict)."
                        AppLanguage.EN -> "Key-on bulb check not performed (does not affect the verdict)."
                    }
                    else -> when (language) {
                        AppLanguage.AR -> "تضيء عند فتح الكونتاكت وتنطفئ بعد تشغيل المحرك."
                        AppLanguage.FR -> "S'allume au contact puis s'éteint normalement moteur tournant."
                        AppLanguage.EN -> "Lights up at key-on and turns off normally with the engine running."
                    }
                }

                PlainScanResultRow(
                    rowId = "warning_light",
                    dotColor = row3Dot,
                    title = when (language) {
                        AppLanguage.AR -> "لمبة تحذير المحرك"
                        AppLanguage.FR -> "Voyant moteur"
                        AppLanguage.EN -> "Engine warning light"
                    },
                    subtitle = row3Subtitle,
                    valueBadgeText = row3Value,
                    language = language,
                    onExplainClick = {
                        activeExplainInfo = ExplainSheetInfo(
                            title = when (language) {
                                AppLanguage.AR -> "فحص لمبة تحذير المحرك في الطابلوه"
                                AppLanguage.FR -> "Voyant moteur au tableau de bord"
                                AppLanguage.EN -> "Dashboard engine warning light"
                            },
                            body = when (language) {
                                AppLanguage.AR -> "في كل سيارة سليمة، يجب أن تضيء لمبة المحرك البرتقالية عند إدارة المفتاح قبل تشغيل المحرك ثم تنطفئ بعد التشغيل. إذا لم تضئ أبداً حتى عند فتح الكونتاكت، فقد تكون اللمبة معطلة أو مغطاة."
                                AppLanguage.FR -> "Sur toute voiture normale, le voyant moteur orange doit s'allumer quand on met le contact (moteur arrêté), puis s'éteindre quand le moteur tourne. S'il ne s'allume jamais au contact, le voyant est hors service ou masqué."
                                AppLanguage.EN -> "On every normal car, the orange engine light must come on when you turn the key before starting, then turn off once the engine is running. If it never lights up at key-on, the light may be broken or covered."
                            },
                            mechanicTip = when (language) {
                                AppLanguage.AR -> "إذا لم تضئ اللمبة عند فتح الكونتاكت، اطلب من كهربائي سيارات فحص لوحة العدادات."
                                AppLanguage.FR -> "Si le voyant ne s'allume jamais au contact, faites contrôler le combiné d'instruments par un électricien auto."
                                AppLanguage.EN -> "If the light never comes on at key-on, ask an auto electrician to inspect the instrument cluster."
                            }
                        )
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            EngineWarningLightVectorIllustration(
                                modifier = Modifier.size(34.dp),
                                strokeColor = MaterialTheme.colorScheme.onSurface,
                                accentColor = StatusWarningAmber
                            )
                            Text(
                                text = when (language) {
                                    AppLanguage.AR -> "عند وضع المفتاح في الكونتاكت (قبل تشغيل المحرك)، هل أضاءت لمبة المحرك؟"
                                    AppLanguage.FR -> "En mettant le contact (avant de démarrer), le voyant moteur s'est-il allumé ?"
                                    AppLanguage.EN -> "When you turned the key before starting the engine, did the warning light come on?"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val selectAnswer: (MilKeyOnUserAnswer) -> Unit = { ans ->
                                onMilKeyOnAnswerChange?.invoke(ans)
                                when (ans) {
                                    MilKeyOnUserAnswer.LIT_UP_NORMAL -> onMilBulbToggle(true)
                                    MilKeyOnUserAnswer.NEVER_LIT_UP -> onMilBulbToggle(false)
                                    MilKeyOnUserAnswer.DID_NOT_LOOK -> onMilBulbToggle(true)
                                }
                            }

                            // Option 1: Yes, it lit up
                            val yesSelected = milKeyOnAnswer == MilKeyOnUserAnswer.LIT_UP_NORMAL
                            ChoicePillButton(
                                label = when (language) {
                                    AppLanguage.AR -> "نعم، أضاءت"
                                    AppLanguage.FR -> "Oui, il s'allume"
                                    AppLanguage.EN -> "Yes, it lit up"
                                },
                                selected = yesSelected,
                                selectedColor = StatusPassGreen,
                                testTag = "mil_bulb_normal_btn",
                                onClick = { selectAnswer(MilKeyOnUserAnswer.LIT_UP_NORMAL) },
                                modifier = Modifier.weight(1f)
                            )

                            // Option 2: No, it never lights up (Red)
                            val noSelected = milKeyOnAnswer == MilKeyOnUserAnswer.NEVER_LIT_UP
                            ChoicePillButton(
                                label = when (language) {
                                    AppLanguage.AR -> "لا، لا تضيء أبداً"
                                    AppLanguage.FR -> "Non, jamais allumé"
                                    AppLanguage.EN -> "No, never lights up"
                                },
                                selected = noSelected,
                                selectedColor = StatusErrorRed,
                                testTag = "mil_bulb_tampered_btn",
                                onClick = { selectAnswer(MilKeyOnUserAnswer.NEVER_LIT_UP) },
                                modifier = Modifier.weight(1f)
                            )

                            // Option 3: I did not look (Not checked — does not affect verdict)
                            val didNotLookSelected = milKeyOnAnswer == MilKeyOnUserAnswer.DID_NOT_LOOK
                            ChoicePillButton(
                                label = when (language) {
                                    AppLanguage.AR -> "لم أنتبه"
                                    AppLanguage.FR -> "Je n'ai pas regardé"
                                    AppLanguage.EN -> "I did not look"
                                },
                                selected = didNotLookSelected,
                                selectedColor = StatusNeutralGrey,
                                testTag = "mil_bulb_did_not_look_btn",
                                onClick = { selectAnswer(MilKeyOnUserAnswer.DID_NOT_LOOK) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

                // -----------------------------------------------------------------
                // ROW 4: Engine temperature (Température du moteur / حرارة المحرك)
                // Rule #1: normal 75 to 104 °C, orange 105 to 111 °C, red >= 112 °C, cold < 75 °C
                // -----------------------------------------------------------------
                val tempGauge = scanResult.liveParameters.firstOrNull { it.id == "temp" }
                val tempStatus = tempGauge?.status ?: "UNAVAILABLE"
                val row4Dot = when (tempStatus) {
                    "GREEN" -> StatusPassGreen
                    "ORANGE" -> StatusWarningAmber
                    "RED" -> StatusErrorRed
                    else -> StatusNeutralGrey // COLD or UNAVAILABLE
                }
                val row4Value = tempGauge?.valueText ?: notAvailableText
                val row4Subtitle = when (tempStatus) {
                    "GREEN" -> when (language) {
                        AppLanguage.AR -> "حرارة تشغيل طبيعية (${ObdVerdictThresholds.COOLANT_NORMAL_MIN_C} إلى ${ObdVerdictThresholds.COOLANT_NORMAL_MAX_C} °C)."
                        AppLanguage.FR -> "Température normale de fonctionnement (${ObdVerdictThresholds.COOLANT_NORMAL_MIN_C} à ${ObdVerdictThresholds.COOLANT_NORMAL_MAX_C} °C)."
                        AppLanguage.EN -> "Normal operating temperature (${ObdVerdictThresholds.COOLANT_NORMAL_MIN_C} to ${ObdVerdictThresholds.COOLANT_NORMAL_MAX_C} °C)."
                    }
                    "COLD" -> when (language) {
                        AppLanguage.AR -> "المحرك لا يزال بارداً (أقل من ${ObdVerdictThresholds.COOLANT_NORMAL_MIN_C} °C). اتركه يسخن للتحقق من استقرار الحرارة."
                        AppLanguage.FR -> "Moteur encore froid (moins de ${ObdVerdictThresholds.COOLANT_NORMAL_MIN_C} °C). Laissez-le chauffer pour vérifier la régulation."
                        AppLanguage.EN -> "Engine is still cold (below ${ObdVerdictThresholds.COOLANT_NORMAL_MIN_C} °C). Let it warm up to check temperature regulation."
                    }
                    "ORANGE" -> when (language) {
                        AppLanguage.AR -> "حرارة مرتفعة قليلاً (${ObdVerdictThresholds.COOLANT_ORANGE_MIN_C}–${ObdVerdictThresholds.COOLANT_ORANGE_MAX_C} °C). تحقق من عمل المروحة ومستوى الماء."
                        AppLanguage.FR -> "Température un peu haute (${ObdVerdictThresholds.COOLANT_ORANGE_MIN_C}–${ObdVerdictThresholds.COOLANT_ORANGE_MAX_C} °C). Vérifiez le ventilateur et le niveau d'eau."
                        AppLanguage.EN -> "Slightly high temperature (${ObdVerdictThresholds.COOLANT_ORANGE_MIN_C}–${ObdVerdictThresholds.COOLANT_ORANGE_MAX_C} °C). Check the cooling fan and coolant level."
                    }
                    "RED" -> when (language) {
                        AppLanguage.AR -> "تنبيه سخونة زائدة (${ObdVerdictThresholds.COOLANT_RED_MIN_C} °C أو أكثر)! أوقف المحرك وتحقق من دائرة التبريد."
                        AppLanguage.FR -> "Alerte surchauffe (${ObdVerdictThresholds.COOLANT_RED_MIN_C} °C ou plus) ! Contrôlez le circuit de refroidissement."
                        AppLanguage.EN -> "Overheating warning (${ObdVerdictThresholds.COOLANT_RED_MIN_C} °C or higher)! Check the cooling system immediately."
                    }
                    else -> when (language) {
                        AppLanguage.AR -> "غير متوفر في هذه السيارة."
                        AppLanguage.FR -> "Non disponible sur cette voiture."
                        AppLanguage.EN -> "Not available on this car."
                    }
                }

                PlainScanResultRow(
                    rowId = "engine_temperature",
                    dotColor = row4Dot,
                    title = when (language) {
                        AppLanguage.AR -> "حرارة المحرك"
                        AppLanguage.FR -> "Température du moteur"
                        AppLanguage.EN -> "Engine temperature"
                    },
                    subtitle = row4Subtitle,
                    valueBadgeText = row4Value,
                    language = language,
                    onExplainClick = {
                        activeExplainInfo = ExplainSheetInfo(
                            title = when (language) {
                                AppLanguage.AR -> "حرارة سائل تبريد المحرك"
                                AppLanguage.FR -> "Température du moteur"
                                AppLanguage.EN -> "Engine coolant temperature"
                            },
                            body = when (language) {
                                AppLanguage.AR -> "تكون حرارة المحرك الدافئ الطبيعية بين ${ObdVerdictThresholds.COOLANT_NORMAL_MIN_C} و ${ObdVerdictThresholds.COOLANT_NORMAL_MAX_C} °C. بين ${ObdVerdictThresholds.COOLANT_ORANGE_MIN_C} و ${ObdVerdictThresholds.COOLANT_ORANGE_MAX_C} °C تستوجب مراقبة المروحة، ومن ${ObdVerdictThresholds.COOLANT_RED_MIN_C} °C فما فوق تعتبر سخونة خطيرة. ملاحظة: قراءة واحدة لا تكفي لتأكيد سلامة المروحة."
                                AppLanguage.FR -> "À chaud, la température normale se situe entre ${ObdVerdictThresholds.COOLANT_NORMAL_MIN_C} et ${ObdVerdictThresholds.COOLANT_NORMAL_MAX_C} °C. Entre ${ObdVerdictThresholds.COOLANT_ORANGE_MIN_C} et ${ObdVerdictThresholds.COOLANT_ORANGE_MAX_C} °C, elle est à surveiller ; à ${ObdVerdictThresholds.COOLANT_RED_MIN_C} °C ou plus, le moteur surchauffe. Une seule mesure ne suffit pas à prouver que le ventilateur se déclenche."
                                AppLanguage.EN -> "Warm engine temperature is normally ${ObdVerdictThresholds.COOLANT_NORMAL_MIN_C} to ${ObdVerdictThresholds.COOLANT_NORMAL_MAX_C} °C. From ${ObdVerdictThresholds.COOLANT_ORANGE_MIN_C} to ${ObdVerdictThresholds.COOLANT_ORANGE_MAX_C} °C it needs checking, and ${ObdVerdictThresholds.COOLANT_RED_MIN_C} °C or higher indicates overheating. Note: a single reading cannot prove that the radiator fan cycles properly."
                            },
                            mechanicTip = when (language) {
                                AppLanguage.AR -> "تأكد بعد تجربة القيادة من أن المروحة تشتغل وتنطفئ تلقائياً وأن أنابيب التبريد ليست متحجرة."
                                AppLanguage.FR -> "Après un essai sur route, laissez tourner au ralenti quelques minutes pour vérifier que le ventilateur démarre et que la température reste stable."
                                AppLanguage.EN -> "After a road test, let the car idle for a few minutes to confirm the cooling fan kicks in and keeps temperature steady."
                            }
                        )
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

                // -----------------------------------------------------------------
                // ROW 5: Engine idle (Ralenti du moteur / استقرار دوران المحرك)
                // Rule #2: Judge steadiness (±60 rpm) and warm range (550–1,100 rpm)
                // -----------------------------------------------------------------
                val rpmGauge = scanResult.liveParameters.firstOrNull { it.id == "rpm" }
                val rpmStatus = rpmGauge?.status ?: "UNAVAILABLE"
                val row5Dot = when (rpmStatus) {
                    "GREEN" -> StatusPassGreen
                    "ORANGE" -> StatusWarningAmber
                    "RED" -> StatusErrorRed
                    else -> StatusNeutralGrey
                }
                val row5Value = rpmGauge?.valueText ?: notAvailableText
                val row5Subtitle = when (rpmStatus) {
                    "GREEN" -> when (language) {
                        AppLanguage.AR -> "دوران المحرك مستقر (أقل من ±${ObdVerdictThresholds.IDLE_MAX_HALF_SPREAD_RPM} د/د مع إيقاف المكيف)."
                        AppLanguage.FR -> "Ralenti régulier et stable (variation inférieure à ±${ObdVerdictThresholds.IDLE_MAX_HALF_SPREAD_RPM} tr/min, clim coupée)."
                        AppLanguage.EN -> "Steady engine idle (variation within ±${ObdVerdictThresholds.IDLE_MAX_HALF_SPREAD_RPM} rpm with A/C off)."
                    }
                    "ORANGE" -> when (language) {
                        AppLanguage.AR -> "تذبذب في الدوران (أكثر من ±${ObdVerdictThresholds.IDLE_MAX_HALF_SPREAD_RPM} د/د) أو سرعة خارج النطاق المعتاد (${ObdVerdictThresholds.WARM_IDLE_MIN_RPM}–${ObdVerdictThresholds.WARM_IDLE_MAX_RPM})."
                        AppLanguage.FR -> "Ralenti irrégulier (plus de ±${ObdVerdictThresholds.IDLE_MAX_HALF_SPREAD_RPM} tr/min) ou hors plage à chaud (${ObdVerdictThresholds.WARM_IDLE_MIN_RPM}–${ObdVerdictThresholds.WARM_IDLE_MAX_RPM} tr/min)."
                        AppLanguage.EN -> "Idle varies by more than ±${ObdVerdictThresholds.IDLE_MAX_HALF_SPREAD_RPM} rpm or sits outside ${ObdVerdictThresholds.WARM_IDLE_MIN_RPM}–${ObdVerdictThresholds.WARM_IDLE_MAX_RPM} rpm."
                    }
                    else -> when (language) {
                        AppLanguage.AR -> "غير متوفر في هذه السيارة."
                        AppLanguage.FR -> "Non disponible sur cette voiture."
                        AppLanguage.EN -> "Not available on this car."
                    }
                }

                PlainScanResultRow(
                    rowId = "engine_idle",
                    dotColor = row5Dot,
                    title = when (language) {
                        AppLanguage.AR -> "استقرار دوران المحرك"
                        AppLanguage.FR -> "Ralenti du moteur"
                        AppLanguage.EN -> "Engine idle"
                    },
                    subtitle = row5Subtitle,
                    valueBadgeText = row5Value,
                    language = language,
                    onExplainClick = {
                        activeExplainInfo = ExplainSheetInfo(
                            title = when (language) {
                                AppLanguage.AR -> "استقرار دوران المحرك (Ralenti)"
                                AppLanguage.FR -> "Stabilité du ralenti moteur"
                                AppLanguage.EN -> "Engine idle steadiness"
                            },
                            body = when (language) {
                                AppLanguage.AR -> "يقيس التطبيق مدى ثبات سرعة المحرك أثناء التوقف (مع إيقاف المكيف). الأهم هو ثبات الدوران وليس الرقم وحده: إذا تذبذب بأكثر من ±${ObdVerdictThresholds.IDLE_MAX_HALF_SPREAD_RPM} دورة/دقيقة أو كان خارج ${ObdVerdictThresholds.WARM_IDLE_MIN_RPM}–${ObdVerdictThresholds.WARM_IDLE_MAX_RPM} د/د والسيارة دافئة، يظهر تنبيه برتقالي."
                                AppLanguage.FR -> "L'application mesure la régularité du moteur au point mort (climatisation éteinte). C'est la stabilité qui compte : si le régime oscille de plus de ±${ObdVerdictThresholds.IDLE_MAX_HALF_SPREAD_RPM} tr/min ou sort de ${ObdVerdictThresholds.WARM_IDLE_MIN_RPM}–${ObdVerdictThresholds.WARM_IDLE_MAX_RPM} tr/min à chaud, un point à vérifier est signalé."
                                AppLanguage.EN -> "The app measures how steady the engine spins at a standstill (with air conditioning off). Steadiness matters more than exact speed: if it fluctuates by more than ±${ObdVerdictThresholds.IDLE_MAX_HALF_SPREAD_RPM} rpm or sits outside ${ObdVerdictThresholds.WARM_IDLE_MIN_RPM}–${ObdVerdictThresholds.WARM_IDLE_MAX_RPM} rpm when warm, it is marked to check."
                            },
                            mechanicTip = when (language) {
                                AppLanguage.AR -> "التذبذب في الدوران قد ينتج عن اتساخ بوابة الهواء (Papillon)، شمعات الاحتراق، أو تسرب هواء بسيط."
                                AppLanguage.FR -> "Un ralenti instable vient souvent d'un boîtier papillon encrassé, de bougies/bobines fatiguées ou d'un injecteur."
                                AppLanguage.EN -> "An unsteady idle is often caused by a dirty throttle body, worn spark plugs/coils, or a small vacuum leak."
                            }
                        )
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

                // -----------------------------------------------------------------
                // ROW 6: Battery and charging (Batterie et charge / البطارية والشحن)
                // Rule #3: normal 13.0 to 14.8 V, orange outside, NEVER red alone, Explain says approximate
                // -----------------------------------------------------------------
                val voltGauge = scanResult.liveParameters.firstOrNull { it.id == "volt" }
                val voltStatus = voltGauge?.status ?: "UNAVAILABLE"
                val row6Dot = when (voltStatus) {
                    "GREEN" -> StatusPassGreen
                    "ORANGE", "RED" -> StatusWarningAmber // Never Red from voltage alone
                    else -> StatusNeutralGrey
                }
                val row6Value = voltGauge?.valueText ?: notAvailableText
                val row6Subtitle = when (voltStatus) {
                    "GREEN" -> when (language) {
                        AppLanguage.AR -> "شحن الدينامو طبيعي (${ObdVerdictThresholds.CHARGING_VOLT_NORMAL_MIN_V} إلى ${ObdVerdictThresholds.CHARGING_VOLT_NORMAL_MAX_V} V · قراءة تقريبية)."
                        AppLanguage.FR -> "Charge alternateur normale (${ObdVerdictThresholds.CHARGING_VOLT_NORMAL_MIN_V} à ${ObdVerdictThresholds.CHARGING_VOLT_NORMAL_MAX_V} V · mesure approximative)."
                        AppLanguage.EN -> "Normal alternator charging (${ObdVerdictThresholds.CHARGING_VOLT_NORMAL_MIN_V} to ${ObdVerdictThresholds.CHARGING_VOLT_NORMAL_MAX_V} V · approximate reading)."
                    }
                    "ORANGE", "RED" -> when (language) {
                        AppLanguage.AR -> "خارج النطاق المعتاد (${ObdVerdictThresholds.CHARGING_VOLT_NORMAL_MIN_V}–${ObdVerdictThresholds.CHARGING_VOLT_NORMAL_MAX_V} V). قراءة الفيشة تقريبية — يُفحص بجهاز ملتيميتر."
                        AppLanguage.FR -> "Hors plage habituelle (${ObdVerdictThresholds.CHARGING_VOLT_NORMAL_MIN_V}–${ObdVerdictThresholds.CHARGING_VOLT_NORMAL_MAX_V} V). Lecture boîtier approximative — à vérifier au multimètre."
                        AppLanguage.EN -> "Outside usual range (${ObdVerdictThresholds.CHARGING_VOLT_NORMAL_MIN_V}–${ObdVerdictThresholds.CHARGING_VOLT_NORMAL_MAX_V} V). Dongle reading is approximate — check with a multimeter."
                    }
                    else -> when (language) {
                        AppLanguage.AR -> "غير متوفر في هذه السيارة."
                        AppLanguage.FR -> "Non disponible sur cette voiture."
                        AppLanguage.EN -> "Not available on this car."
                    }
                }

                PlainScanResultRow(
                    rowId = "battery_charging",
                    dotColor = row6Dot,
                    title = when (language) {
                        AppLanguage.AR -> "البطارية والشحن"
                        AppLanguage.FR -> "Batterie et charge"
                        AppLanguage.EN -> "Battery and charging"
                    },
                    subtitle = row6Subtitle,
                    valueBadgeText = row6Value,
                    language = language,
                    onExplainClick = {
                        activeExplainInfo = ExplainSheetInfo(
                            title = when (language) {
                                AppLanguage.AR -> "جهد البطارية وشحن الدينامو"
                                AppLanguage.FR -> "Batterie et charge de l'alternateur"
                                AppLanguage.EN -> "Battery and alternator charging"
                            },
                            body = when (language) {
                                AppLanguage.AR -> "عندما يكون المحرك شغالاً، يشحن الدينامو البطارية عادةً بجهد بين ${ObdVerdictThresholds.CHARGING_VOLT_NORMAL_MIN_V} و ${ObdVerdictThresholds.CHARGING_VOLT_NORMAL_MAX_V} فولط. ملاحظة هامة: قراءة الفولتية التي يقيسها جهاز ELM327 عبر منفذ الفحص هي قراءة تقريبية قد تختلف قليلاً عن أقطاب البطارية."
                                AppLanguage.FR -> "Moteur tournant, l'alternateur charge normalement entre ${ObdVerdictThresholds.CHARGING_VOLT_NORMAL_MIN_V} et ${ObdVerdictThresholds.CHARGING_VOLT_NORMAL_MAX_V} V. Important : la tension mesurée par le boîtier ELM327 sur la prise diagnostic est approximative et peut différer légèrement de la tension aux bornes de la batterie."
                                AppLanguage.EN -> "With the engine running, the alternator normally charges between ${ObdVerdictThresholds.CHARGING_VOLT_NORMAL_MIN_V} and ${ObdVerdictThresholds.CHARGING_VOLT_NORMAL_MAX_V} V. Important: the voltage reading from the ELM327 dongle at the diagnostic socket is approximate and can read slightly lower than the battery terminals."
                            },
                            mechanicTip = when (language) {
                                AppLanguage.AR -> "إذا ظهرت القراءة أقل من 13.0 V أو أعلى من 14.8 V، اطلب من الكهربائي قياس الجهد مباشرة على البطارية بجهاز ملتيميتر."
                                AppLanguage.FR -> "Si la valeur est sous 13,0 V ou au-dessus de 14,8 V, demandez à mesurer directement sur les cosses de la batterie avec un multimètre."
                                AppLanguage.EN -> "If the reading is below 13.0 V or above 14.8 V, ask a mechanic to measure directly across the battery terminals with a multimeter."
                            }
                        )
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

                // -----------------------------------------------------------------
                // ROW 7: Fuel mixture (Mélange carburant / خليط الوقود والهواء)
                // Rule #4: Judge long-term (LTFT) only: normal within ±10%, orange beyond
                // -----------------------------------------------------------------
                val trimGauge = scanResult.liveParameters.firstOrNull { it.id == "trim" }
                val trimStatus = trimGauge?.status ?: "UNAVAILABLE"
                val row7Dot = when (trimStatus) {
                    "GREEN" -> StatusPassGreen
                    "ORANGE", "RED" -> StatusWarningAmber
                    else -> StatusNeutralGrey
                }
                val row7Value = trimGauge?.valueText ?: notAvailableText
                val row7Subtitle = when (trimStatus) {
                    "GREEN" -> when (language) {
                        AppLanguage.AR -> "نسبة الوقود والهواء متوازنة (ضمن ±10%)."
                        AppLanguage.FR -> "Dosage air/carburant équilibré (dans la limite de ±10%)."
                        AppLanguage.EN -> "Fuel-air mixture is balanced (within ±10%)."
                    }
                    "ORANGE", "RED" -> when (language) {
                        AppLanguage.AR -> "تصحيح الخليط يتجاوز ±10% (قد يعود لتسرب هواء، فلتر، أو ضبط غاز GPL)."
                        AppLanguage.FR -> "Correction au-delà de ±10% (prise d'air, injecteur ou réglage GPL à vérifier)."
                        AppLanguage.EN -> "Long-term fuel adjustment is beyond ±10% (check for air leak, injector, or LPG calibration)."
                    }
                    else -> when (language) {
                        AppLanguage.AR -> "غير متوفر في هذه السيارة (شائع في كثير من محركات الديزل)."
                        AppLanguage.FR -> "Non disponible sur cette voiture (normal sur beaucoup de diesels)."
                        AppLanguage.EN -> "Not available on this car (common on many diesel engines)."
                    }
                }

                PlainScanResultRow(
                    rowId = "fuel_mixture",
                    dotColor = row7Dot,
                    title = when (language) {
                        AppLanguage.AR -> "خليط الوقود والهواء"
                        AppLanguage.FR -> "Mélange carburant"
                        AppLanguage.EN -> "Fuel mixture"
                    },
                    subtitle = row7Subtitle,
                    valueBadgeText = row7Value,
                    language = language,
                    onExplainClick = {
                        activeExplainInfo = ExplainSheetInfo(
                            title = when (language) {
                                AppLanguage.AR -> "تصحيح خليط الوقود والهواء"
                                AppLanguage.FR -> "Mélange air / carburant"
                                AppLanguage.EN -> "Fuel and air mixture"
                            },
                            body = when (language) {
                                AppLanguage.AR -> "يعتمد التقييم على التصحيح طويل المدى (LTFT): القراءة الطبيعية تكون بين -10% و +10%. إذا تجاوزت هذا الحد، فهذا يعني أن الكمبيوتر يضطر لزيادة أو إنقاص الوقود لتعويض تسرب هواء أو ضعف ضخ أو عيار غاز GPL."
                                AppLanguage.FR -> "Le verdict juge la correction à long terme : elle est normale entre -10% et +10%. Au-delà, le calculateur doit compenser en permanence une prise d'air, un filtre/injecteur encrassé ou un réglage GPL approximatif."
                                AppLanguage.EN -> "This check judges the long-term fuel adjustment: within ±10% is normal. Beyond ±10%, the computer is constantly compensating for an air leak, dirty injector, or LPG gas map."
                            },
                            mechanicTip = when (language) {
                                AppLanguage.AR -> "في السيارات المزودة بغاز GPL، غالباً ما يظهر هذا التنبيه إذا كان جهاز الغاز يحتاج إعادة ضبط عند مختص GPL."
                                AppLanguage.FR -> "Sur une voiture équipée au GPL, un écart supérieur à ±10% indique souvent que le calculateur GPL a besoin d'un réglage."
                                AppLanguage.EN -> "On LPG-equipped cars, a value beyond ±10% usually means the LPG system needs a calibration check."
                            }
                        )
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

                // -----------------------------------------------------------------
                // ROW 8: Chassis number in computer (Numéro de châssis / رقم الشاسيه)
                // -----------------------------------------------------------------
                val ecuVin = scanResult.ecuVin?.trim()?.takeIf { it.isNotEmpty() }
                val row8Dot = when {
                    ecuVin == null -> StatusNeutralGrey
                    ecuVinComparison == EcuVinComparisonChoice.DIFFERENT -> StatusErrorRed
                    ecuVinComparison == EcuVinComparisonChoice.SAME -> StatusPassGreen
                    else -> StatusNeutralGrey
                }
                val row8Value = when {
                    ecuVin == null -> notAvailableText
                    ecuVinComparison == EcuVinComparisonChoice.SAME -> when (language) {
                        AppLanguage.AR -> "متطابق"
                        AppLanguage.FR -> "Identique"
                        AppLanguage.EN -> "Same"
                    }
                    ecuVinComparison == EcuVinComparisonChoice.DIFFERENT -> when (language) {
                        AppLanguage.AR -> "مختلف!"
                        AppLanguage.FR -> "Différent !"
                        AppLanguage.EN -> "Different!"
                    }
                    else -> when (language) {
                        AppLanguage.AR -> "قارن مع البطاقة"
                        AppLanguage.FR -> "À comparer"
                        AppLanguage.EN -> "Compare"
                    }
                }
                val row8Subtitle = when {
                    ecuVin == null -> when (language) {
                        AppLanguage.AR -> "غير متوفر إلكترونياً في هذه السيارة — قارن الرقم المطبوع على الهيكل مع البطاقة الرمادية."
                        AppLanguage.FR -> "Non disponible électroniquement sur cette voiture — vérifiez le numéro frappé sur la carrosserie."
                        AppLanguage.EN -> "Not available electronically on this car — check the number stamped on the body."
                    }
                    ecuVinComparison == EcuVinComparisonChoice.DIFFERENT -> when (language) {
                        AppLanguage.AR -> "تنبيه هام: رقم الشاسيه في الكمبيوتر يختلف عن البطاقة الرمادية!"
                        AppLanguage.FR -> "Alerte importante : le numéro du calculateur diffère de la carte grise !"
                        AppLanguage.EN -> "Serious warning: the computer's chassis number differs from the registration card!"
                    }
                    else -> when (language) {
                        AppLanguage.AR -> "قارن هذا الرقم مع الرقم المكتوب في البطاقة الرمادية للسيارة."
                        AppLanguage.FR -> "Comparez ce numéro avec celui inscrit sur la carte grise."
                        AppLanguage.EN -> "Compare this number with the car's registration card (carte grise)."
                    }
                }

                PlainScanResultRow(
                    rowId = "chassis_vin",
                    dotColor = row8Dot,
                    title = when (language) {
                        AppLanguage.AR -> "رقم الشاسيه في الكمبيوتر"
                        AppLanguage.FR -> "Numéro de châssis (calculateur)"
                        AppLanguage.EN -> "Chassis number in computer"
                    },
                    subtitle = row8Subtitle,
                    valueBadgeText = row8Value,
                    language = language,
                    rowTestTag = "obd_ecu_vin_card",
                    onExplainClick = {
                        activeExplainInfo = ExplainSheetInfo(
                            title = when (language) {
                                AppLanguage.AR -> "رقم الشاسيه المخزن في كمبيوتر المحرك"
                                AppLanguage.FR -> "Numéro de châssis dans le calculateur"
                                AppLanguage.EN -> "Chassis number (VIN) in the computer"
                            },
                            body = when (language) {
                                AppLanguage.AR -> "تحفظ كثير من السيارات الحديثة رقم الشاسيه المكون من 17 حرفاً ورقماً داخل كمبيوتر المحرك. يجب أن يتطابق هذا الرقم تماماً مع البطاقة الرمادية ومع الرقم المنقوش على هيكل السيارة."
                                AppLanguage.FR -> "Beaucoup de voitures récentes enregistrent leur numéro de série à 17 caractères (VIN) dans le calculateur moteur. Il doit correspondre exactement à la carte grise et à la frappe sur le châssis."
                                AppLanguage.EN -> "Many modern cars store their 17-character chassis number (VIN) inside the engine computer. It should match the registration card (carte grise) and the number stamped on the body."
                            },
                            mechanicTip = when (language) {
                                AppLanguage.AR -> "إذا كان الرقم مختلفاً، فقد يكون كمبيوتر المحرك قد استُبدل بقطعة مستعملة من سيارة أخرى، أو توجد مشكلة في مطابقة الوثائق."
                                AppLanguage.FR -> "Si le numéro diffère, le calculateur moteur a peut-être été remplacé par une pièce d'occasion non reprogrammée, ou les documents présentent une anomalie."
                                AppLanguage.EN -> "If the number is different, the engine computer may have been swapped from another car, or there may be a paperwork discrepancy."
                            }
                        )
                    }
                ) {
                    if (ecuVin != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Text(
                                    text = ecuVin,
                                    style = FahsTypography.vinMonospaceStyle,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 10.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ChoicePillButton(
                                    label = when (language) {
                                        AppLanguage.AR -> "متطابق"
                                        AppLanguage.FR -> "Identique"
                                        AppLanguage.EN -> "Same"
                                    },
                                    selected = ecuVinComparison == EcuVinComparisonChoice.SAME,
                                    selectedColor = StatusPassGreen,
                                    testTag = "ecu_vin_same_btn",
                                    onClick = { onEcuVinComparisonChange(EcuVinComparisonChoice.SAME) },
                                    modifier = Modifier.weight(1f)
                                )
                                ChoicePillButton(
                                    label = when (language) {
                                        AppLanguage.AR -> "مختلف"
                                        AppLanguage.FR -> "Différent"
                                        AppLanguage.EN -> "Different"
                                    },
                                    selected = ecuVinComparison == EcuVinComparisonChoice.DIFFERENT,
                                    selectedColor = StatusErrorRed,
                                    testTag = "ecu_vin_different_btn",
                                    onClick = { onEcuVinComparisonChange(EcuVinComparisonChoice.DIFFERENT) },
                                    modifier = Modifier.weight(1f)
                                )
                                ChoicePillButton(
                                    label = when (language) {
                                        AppLanguage.AR -> "لم أتحقق"
                                        AppLanguage.FR -> "Non vérifié"
                                        AppLanguage.EN -> "Not checked"
                                    },
                                    selected = ecuVinComparison == EcuVinComparisonChoice.COULD_NOT_COMPARE,
                                    selectedColor = StatusNeutralGrey,
                                    testTag = "ecu_vin_could_not_compare_btn",
                                    onClick = { onEcuVinComparisonChange(EcuVinComparisonChoice.COULD_NOT_COMPARE) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

                // -----------------------------------------------------------------
                // COLLAPSED ROW: "Details for a mechanic" (Détails pour un mécanicien)
                // -----------------------------------------------------------------
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { mechanicDetailsExpanded = !mechanicDetailsExpanded }
                        .testTag("mechanic_details_toggle_row")
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when (language) {
                                    AppLanguage.AR -> "تفاصيل للميكانيكي"
                                    AppLanguage.FR -> "Détails pour un mécanicien"
                                    AppLanguage.EN -> "Details for a mechanic"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = when (language) {
                                    AppLanguage.AR -> "أكواد الأعطال الخام، نسب التصحيح، البروتوكول وعدادات الجاهزية"
                                    AppLanguage.FR -> "Codes bruts, corrections STFT/LTFT, sonde lambda et compteurs"
                                    AppLanguage.EN -> "Raw fault codes, STFT/LTFT trims, O2 sensor, and readiness counters"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = if (mechanicDetailsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(visible = mechanicDetailsExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val rawCodesText = if (scanResult.dtcList.isEmpty()) {
                                "0 DTC (Modes 03 / 07 / 0A)"
                            } else {
                                scanResult.dtcList.joinToString(", ") { "${it.code} (${it.dtcType.getLabel(language)})" }
                            }
                            MechanicKeyValueLine(
                                label = when (language) {
                                    AppLanguage.AR -> "الأكواد المسجلة"
                                    AppLanguage.FR -> "Codes défauts (DTC)"
                                    AppLanguage.EN -> "Trouble codes (DTC)"
                                },
                                value = rawCodesText
                            )
                            MechanicKeyValueLine(
                                label = when (language) {
                                    AppLanguage.AR -> "المسافة منذ المسح"
                                    AppLanguage.FR -> "Distance depuis effacement"
                                    AppLanguage.EN -> "Distance since clear"
                                },
                                value = scanResult.distanceSinceCodesClearedKm?.let { "$it km" } ?: notAvailableText
                            )
                            MechanicKeyValueLine(
                                label = when (language) {
                                    AppLanguage.AR -> "دورات التسخين منذ المسح"
                                    AppLanguage.FR -> "Cycles de chauffe"
                                    AppLanguage.EN -> "Warm-up cycles"
                                },
                                value = scanResult.warmupsSinceCodesCleared?.toString() ?: notAvailableText
                            )
                            MechanicKeyValueLine(
                                label = when (language) {
                                    AppLanguage.AR -> "المسافة مع لمبة المحرك"
                                    AppLanguage.FR -> "Distance avec voyant allumé"
                                    AppLanguage.EN -> "Distance with MIL on"
                                },
                                value = scanResult.distanceWithMilOnKm?.let { "$it km" } ?: notAvailableText
                            )
                            MechanicKeyValueLine(
                                label = when (language) {
                                    AppLanguage.AR -> "اختبارات الجاهزية غير المكتملة"
                                    AppLanguage.FR -> "Moniteurs incomplets"
                                    AppLanguage.EN -> "Incomplete monitors"
                                },
                                value = if (scanResult.incompleteReadinessMonitors != null && scanResult.totalReadinessMonitors != null) {
                                    "${scanResult.incompleteReadinessMonitors} / ${scanResult.totalReadinessMonitors}"
                                } else {
                                    notAvailableText
                                }
                            )
                            MechanicKeyValueLine(
                                label = "LTFT / STFT",
                                value = buildString {
                                    val lt = scanResult.ltftPercent?.let { String.format(Locale.US, "LT %+.1f%%", it) }
                                    val st = scanResult.stftPercent?.let { String.format(Locale.US, "ST %+.1f%%", it) }
                                    val combined = listOfNotNull(lt, st).joinToString(" · ")
                                    append(combined.ifBlank { notAvailableText })
                                }
                            )
                            val o2Gauge = scanResult.liveParameters.firstOrNull { it.id == "o2" }
                            MechanicKeyValueLine(
                                label = when (language) {
                                    AppLanguage.AR -> "حساس الأكسجين (Lambda)"
                                    AppLanguage.FR -> "Sonde Lambda (O2)"
                                    AppLanguage.EN -> "O2 sensor voltage"
                                },
                                value = o2Gauge?.valueText ?: notAvailableText
                            )
                            MechanicKeyValueLine(
                                label = when (language) {
                                    AppLanguage.AR -> "البروتوكول والجهاز"
                                    AppLanguage.FR -> "Protocole & boîtier"
                                    AppLanguage.EN -> "Protocol & adapter"
                                },
                                value = "${scanResult.detectedProtocol} · ${scanResult.adapterVersion}"
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 3. AUTO-ANSWERED CHECKLIST PREVIEW & 3 NEXT-STEP ACTIONS (Never shown for simulated data)
        // =========================================================================
        if (!scanResult.isSimulated) {
            if (scanResult.isCommunicationSuccess) {
                ProposedScanChecklistAnswersCard(
                    scanResult = scanResult,
                    milBulbLitOnDashboard = milBulbLitOnDashboard,
                    milKeyOnAnswer = milKeyOnAnswer,
                    ecuVinComparison = ecuVinComparison,
                    language = language
                )
            }

            // 1. Primary (highlighted): Add a 3-minute visual check (10 points)
            if (onContinueThreeMinuteVisualCheck != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onContinueThreeMinuteVisualCheck)
                        .testTag("continue_visual_check_after_scan_btn"),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when (language) {
                                AppLanguage.AR -> "أضف فحصاً بصرياً في 3 دقائق (10 نقاط)"
                                AppLanguage.FR -> "Ajouter un contrôle visuel de 3 min (10 points)"
                                AppLanguage.EN -> "Add a 3-minute visual check (10 points)"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = when (language) {
                                AppLanguage.AR -> "لا يرى الفحص التلقائي هذه النقاط العشر. افحصها بنفسك."
                                AppLanguage.FR -> "Le scan ne voit pas ces dix points. Vérifiez-les vous-même."
                                AppLanguage.EN -> "The scan cannot see these ten points. Check them yourself."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.90f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // 2. Secondary: Save and get report
            OutlinedButton(
                onClick = onSaveAndGetReport ?: onSaveToInspection,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_obd_to_inspection_btn"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (language) {
                        AppLanguage.AR -> "حفظ والحصول على التقرير"
                        AppLanguage.FR -> "Enregistrer et obtenir le rapport"
                        AppLanguage.EN -> "Save and get report"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // 3. Tertiary: Full manual inspection
            if (onStartFullManualInspection != null) {
                TextButton(
                    onClick = onStartFullManualInspection,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("start_full_manual_inspection_after_scan_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = when (language) {
                            AppLanguage.AR -> "الفحص اليدوي الكامل (مع ملء نقاط الفحص التلقائي)"
                            AppLanguage.FR -> "Inspection manuelle complète"
                            AppLanguage.EN -> "Full manual inspection"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Developer Mode Raw Exchange Log Export (only in Developer Mode)
        if (isDeveloperModeEnabled && scanResult.rawExchangeLog.isNotEmpty()) {
            DeveloperRawExchangeLogCard(
                exchangeCount = scanResult.rawExchangeLog.size,
                language = language,
                onExportRawLog = onExportRawLog
            )
        }
    }
}

@Composable
private fun PlainScanResultRow(
    rowId: String,
    dotColor: Color,
    title: String,
    subtitle: String,
    valueBadgeText: String,
    language: AppLanguage,
    onExplainClick: () -> Unit,
    rowTestTag: String = "obd_row_$rowId",
    extraContent: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(rowTestTag)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val badgeBg = when (dotColor) {
                    StatusPassGreen -> StatusPassGreenContainer
                    StatusWarningAmber -> StatusWarningAmberContainer
                    StatusErrorRed -> StatusErrorRedContainer
                    else -> StatusNeutralGreyContainer
                }
                val badgeFg = when (dotColor) {
                    StatusPassGreen -> StatusPassGreenOnContainer
                    StatusWarningAmber -> StatusWarningAmberOnContainer
                    StatusErrorRed -> StatusErrorRedOnContainer
                    else -> Color(0xFF2B3531)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = valueBadgeText,
                        style = FahsTypography.tabularValueStyle.copy(fontSize = 13.sp),
                        color = badgeFg,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onExplainClick)
                        .testTag("explain_row_$rowId"),
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.HelpOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (language) {
                                AppLanguage.AR -> "اشرح لي"
                                AppLanguage.FR -> "Expliquer"
                                AppLanguage.EN -> "Explain"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        if (extraContent != null) {
            extraContent()
        }
    }
}

@Composable
private fun ChoicePillButton(
    label: String,
    selected: Boolean,
    selectedColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(9.dp),
        color = if (selected) selectedColor else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) selectedColor else MaterialTheme.colorScheme.outline
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 9.dp, horizontal = 6.dp)
        )
    }
}

@Composable
private fun MechanicKeyValueLine(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .weight(0.45f)
                .padding(end = 8.dp)
        )
        Text(
            text = value,
            style = FahsTypography.tabularValueStyle.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.55f)
        )
    }
}

@Composable
private fun DeveloperRawExchangeLogCard(
    exchangeCount: Int,
    language: AppLanguage,
    onExportRawLog: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dev_export_raw_obd_log_card"),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (language) {
                        AppLanguage.AR -> "[وضع المطورين] سجل التبادل الخام مع جهاز ELM327 ($exchangeCount أمر)"
                        AppLanguage.FR -> "[Mode Développeur] Journal brut d'échange ELM327 ($exchangeCount commandes)"
                        AppLanguage.EN -> "[Developer Mode] Raw ELM327 Exchange Log ($exchangeCount commands)"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = when (language) {
                    AppLanguage.AR -> "احفظ سجل الأوامر والردود الخام في ملف نصي (.txt) لإرساله عند فحص سيارة حقيقية."
                    AppLanguage.FR -> "Enregistrez et partagez l'échange brut avec le boîtier dans un fichier texte (.txt) pour diagnostic."
                    AppLanguage.EN -> "Save and share the raw command/reply exchange with the dongle as a .txt file for troubleshooting."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onExportRawLog,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dev_export_raw_obd_log_btn"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (language) {
                        AppLanguage.AR -> "حفظ ومشاركة ملف السجل الخام (.txt)"
                        AppLanguage.FR -> "Enregistrer / Envoyer le fichier brut (.txt)"
                        AppLanguage.EN -> "Save & Share Raw Exchange Log (.txt)"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ProposedScanChecklistAnswersCard(
    scanResult: ObdScanResult,
    milBulbLitOnDashboard: Boolean,
    milKeyOnAnswer: MilKeyOnUserAnswer,
    ecuVinComparison: EcuVinComparisonChoice,
    language: AppLanguage
) {
    val proposals = remember(scanResult, milBulbLitOnDashboard, milKeyOnAnswer, ecuVinComparison) {
        com.example.ui.viewmodel.FahsViewModel.buildAutoAnsweredChecklistItemsFromScan(
            scanResult = scanResult,
            milBulbLitOnDashboard = milBulbLitOnDashboard,
            ecuVinComparison = ecuVinComparison,
            adaptiveItems = com.example.domain.ChecklistEngine.fullLibrary,
            milKeyOnAnswer = milKeyOnAnswer
        )
    }
    if (proposals.isEmpty()) return

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("obd_proposed_checklist_answers_card"),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = when (language) {
                    AppLanguage.AR -> "نقاط الفحص اليدوي التي يجيب عنها هذا الفحص (${proposals.size})"
                    AppLanguage.FR -> "Points de l'inspection remplis par ce scan (${proposals.size})"
                    AppLanguage.EN -> "Inspection items answered by this scan (${proposals.size})"
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when (language) {
                    AppLanguage.AR -> "عند الحفظ، تُعلَّم هذه النقاط كـ «تمت الإجابة بواسطة الفحص» ولن يُعاد سؤالك عنها:"
                    AppLanguage.FR -> "En enregistrant, ces questions seront marquées « Répondu par le scan » et ne seront plus reposées :"
                    AppLanguage.EN -> "When saved, these items are marked 'Answered by the scan' so you won't be asked again:"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            proposals.forEach { prop ->
                val isGood = prop.status == "GOOD"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = if (isGood) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isGood) StatusPassGreen else StatusWarningAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = prop.item.getQuestion(language),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = prop.getReason(language),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SaveObdToInspectionButton(
    language: AppLanguage,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("save_obd_to_inspection_btn"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        )
    ) {
        Icon(
            imageVector = Icons.Default.Save,
            contentDescription = null,
            tint = Color.White
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = when (language) {
                AppLanguage.AR -> "حفظ نتيجة الفحص في تقاريري"
                AppLanguage.FR -> "Enregistrer le résultat dans Mes rapports"
                AppLanguage.EN -> "Save scan result to My reports"
            },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
