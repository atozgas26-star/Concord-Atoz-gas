package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.ArcticWhite
import com.example.ui.theme.BrandRedSecondary
import com.example.ui.theme.ElectricBlueBright
import com.example.ui.theme.ElectricBlueGlow
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.MetallicOrange
import com.example.ui.theme.MetallicOrangeBright
import com.example.ui.theme.MetallicOrangeGold
import com.example.ui.theme.PlatinumMuted
import com.example.ui.theme.PlatinumSilver
import com.example.ui.theme.RoyalBlueDeep
import com.example.ui.theme.RoyalElectricBlue
import com.example.ui.theme.SignatureRoyalBackground
import com.example.ui.theme.SignatureRoyalBorder
import com.example.ui.theme.SignatureRoyalElevated
import com.example.ui.theme.SignatureRoyalSurface
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.roundToInt

enum class CalculatorMode(val label: String) {
    NORMAL("Normal"),
    BILL_VAT_5("Bill • 5% VAT")
}

data class VatBillBreakdown(
    val billAmount: BigDecimal,
    val vatAmount: BigDecimal,
    val grandTotal: BigDecimal
) {
    val billAmountFormatted: String get() = "AED ${billAmount.setScale(2, RoundingMode.HALF_UP).toPlainString()}"
    val vatAmountFormatted: String get() = "AED ${vatAmount.setScale(2, RoundingMode.HALF_UP).toPlainString()}"
    val grandTotalFormatted: String get() = "AED ${grandTotal.setScale(2, RoundingMode.HALF_UP).toPlainString()}"
}

/**
 * Pure UAE 5% VAT Bill calculator using BigDecimal with scale 2 (HALF_UP).
 * Example: 100 -> Bill Amount: 100.00, VAT (5%): 5.00, Grand Total: 105.00.
 */
fun calculateVat5Breakdown(billAmount: BigDecimal): VatBillBreakdown {
    val safeBill = if (billAmount.compareTo(BigDecimal.ZERO) < 0) BigDecimal.ZERO else billAmount
    val normalizedBill = safeBill.setScale(2, RoundingMode.HALF_UP)
    val vat = normalizedBill.multiply(BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP)
    val grandTotal = normalizedBill.add(vat).setScale(2, RoundingMode.HALF_UP)
    return VatBillBreakdown(
        billAmount = normalizedBill,
        vatAmount = vat,
        grandTotal = grandTotal
    )
}

data class QuickCalculatorState(
    val display: String = "0",
    val expression: String = "",
    val accumulator: BigDecimal? = null,
    val pendingOperator: String? = null,
    val resetDisplayOnNextDigit: Boolean = false,
    val hasError: Boolean = false,
    val history: List<String> = emptyList(),
    val mode: CalculatorMode = CalculatorMode.NORMAL
) {
    val currentNumericValue: BigDecimal
        get() = if (hasError) BigDecimal.ZERO else (display.toBigDecimalOrNull() ?: BigDecimal.ZERO)

    val vatBreakdown: VatBillBreakdown?
        get() = if (mode == CalculatorMode.BILL_VAT_5 && !hasError) {
            calculateVat5Breakdown(currentNumericValue)
        } else {
            null
        }
}

/**
 * Pure, safe arithmetic engine for the C&A Gas Quick Pop-up Calculator.
 * Normal mode performs pure arithmetic and NEVER auto-adds VAT.
 * Bill 5% VAT mode computes a live UAE 5% VAT breakdown alongside standard calculation.
 */
object QuickCalculatorEngine {

    private val mathContext = MathContext(12, RoundingMode.HALF_UP)

    fun setMode(state: QuickCalculatorState, mode: CalculatorMode): QuickCalculatorState {
        return state.copy(mode = mode)
    }

    fun onKeyPress(state: QuickCalculatorState, key: String): QuickCalculatorState {
        if (key == "C") {
            return QuickCalculatorState(history = state.history, mode = state.mode)
        }
        val active = if (state.hasError) QuickCalculatorState(history = state.history, mode = state.mode) else state

        return when (key) {
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9" -> handleDigit(active, key)
            "." -> handleDecimal(active)
            "DEL" -> handleBackspace(active)
            "±" -> handleToggleSign(active)
            "%" -> handlePercent(active)
            "+", "-", "×", "÷" -> handleOperator(active, key)
            "=" -> handleEquals(active)
            else -> active
        }
    }

    fun applyHistoryResult(state: QuickCalculatorState, entry: String): QuickCalculatorState {
        val resultPart = entry.substringAfter("=").trim()
        val parsed = resultPart.toBigDecimalOrNull() ?: return state
        return state.copy(
            display = formatNumber(parsed),
            resetDisplayOnNextDigit = false,
            hasError = false
        )
    }

    private fun handleDigit(state: QuickCalculatorState, digit: String): QuickCalculatorState {
        val nextDisplay = when {
            state.resetDisplayOnNextDigit || state.display == "0" -> digit
            state.display.length >= 14 -> state.display
            else -> state.display + digit
        }
        return state.copy(display = nextDisplay, resetDisplayOnNextDigit = false)
    }

    private fun handleDecimal(state: QuickCalculatorState): QuickCalculatorState {
        if (state.resetDisplayOnNextDigit) {
            return state.copy(display = "0.", resetDisplayOnNextDigit = false)
        }
        if (state.display.contains(".")) return state
        return state.copy(display = "${state.display}.")
    }

    private fun handleBackspace(state: QuickCalculatorState): QuickCalculatorState {
        if (state.resetDisplayOnNextDigit) return state
        val trimmed = state.display.dropLast(1)
        val next = if (trimmed.isEmpty() || trimmed == "-") "0" else trimmed
        return state.copy(display = next)
    }

    private fun handleToggleSign(state: QuickCalculatorState): QuickCalculatorState {
        val value = state.display.toBigDecimalOrNull() ?: return state
        if (value.compareTo(BigDecimal.ZERO) == 0) return state
        return state.copy(display = formatNumber(value.negate()))
    }

    private fun handlePercent(state: QuickCalculatorState): QuickCalculatorState {
        val current = state.display.toBigDecimalOrNull() ?: return state
        val result = if (state.accumulator != null && (state.pendingOperator == "+" || state.pendingOperator == "-")) {
            state.accumulator.multiply(current, mathContext).divide(BigDecimal(100), mathContext)
        } else {
            current.divide(BigDecimal(100), mathContext)
        }
        return state.copy(display = formatNumber(result), resetDisplayOnNextDigit = false)
    }

    private fun handleOperator(state: QuickCalculatorState, op: String): QuickCalculatorState {
        val currentVal = state.display.toBigDecimalOrNull() ?: BigDecimal.ZERO
        if (state.accumulator != null && state.pendingOperator != null && !state.resetDisplayOnNextDigit) {
            val computed = compute(state.accumulator, currentVal, state.pendingOperator)
                ?: return state.copy(
                    display = "Cannot ÷ 0",
                    expression = "",
                    accumulator = null,
                    pendingOperator = null,
                    hasError = true
                )
            val formatted = formatNumber(computed)
            return state.copy(
                display = formatted,
                expression = "$formatted $op",
                accumulator = computed,
                pendingOperator = op,
                resetDisplayOnNextDigit = true
            )
        }
        val formatted = formatNumber(currentVal)
        return state.copy(
            expression = "$formatted $op",
            accumulator = currentVal,
            pendingOperator = op,
            resetDisplayOnNextDigit = true
        )
    }

    private fun handleEquals(state: QuickCalculatorState): QuickCalculatorState {
        val left = state.accumulator ?: return state
        val op = state.pendingOperator ?: return state
        val right = state.display.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val computed = compute(left, right, op)
            ?: return state.copy(
                display = "Cannot ÷ 0",
                expression = "",
                accumulator = null,
                pendingOperator = null,
                hasError = true
            )
        val leftText = formatNumber(left)
        val rightText = formatNumber(right)
        val resultText = formatNumber(computed)
        val historyEntry = "$leftText $op $rightText = $resultText"
        val updatedHistory = (listOf(historyEntry) + state.history).take(5)
        return state.copy(
            display = resultText,
            expression = "$leftText $op $rightText =",
            accumulator = null,
            pendingOperator = null,
            resetDisplayOnNextDigit = true,
            history = updatedHistory
        )
    }

    private fun compute(left: BigDecimal, right: BigDecimal, op: String): BigDecimal? {
        return when (op) {
            "+" -> left.add(right, mathContext)
            "-" -> left.subtract(right, mathContext)
            "×" -> left.multiply(right, mathContext)
            "÷" -> {
                if (right.compareTo(BigDecimal.ZERO) == 0) return null
                left.divide(right, 8, RoundingMode.HALF_UP)
            }
            else -> right
        }
    }

    internal fun formatNumber(value: BigDecimal): String {
        val stripped = value.stripTrailingZeros()
        val normalized = if (stripped.compareTo(BigDecimal.ZERO) == 0) BigDecimal.ZERO else stripped
        return normalized.toPlainString()
    }
}

/**
 * Full-screen freely movable floating calculator icon overlay.
 */
@Composable
fun DraggableFloatingCalculatorOverlay(
    onClick: () -> Unit,
    initialBottomClearance: Dp = 84.dp,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val buttonHitSizePx = remember(density) { with(density) { 48.dp.toPx() } }
    val edgeMarginPx = remember(density) { with(density) { 10.dp.toPx() } }
    val initialBottomPx = remember(density, initialBottomClearance) {
        with(density) { initialBottomClearance.toPx() }
    }

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var hasUserMoved by remember { mutableStateOf(false) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val maxX = (containerSize.width.toFloat() - buttonHitSizePx - edgeMarginPx).coerceAtLeast(edgeMarginPx)
    val maxY = (containerSize.height.toFloat() - buttonHitSizePx - edgeMarginPx).coerceAtLeast(edgeMarginPx)

    LaunchedEffect(containerSize, initialBottomPx) {
        if (containerSize.width > 0 && containerSize.height > 0) {
            if (!hasUserMoved) {
                offsetX = maxX
                offsetY = (containerSize.height.toFloat() - buttonHitSizePx - initialBottomPx)
                    .coerceIn(edgeMarginPx, maxY)
            } else {
                offsetX = offsetX.coerceIn(edgeMarginPx, maxX)
                offsetY = offsetY.coerceIn(edgeMarginPx, maxY)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .onSizeChanged { newSize ->
                if (newSize != containerSize) {
                    containerSize = newSize
                }
            }
    ) {
        if (containerSize.width > 0 && containerSize.height > 0) {
            FloatingCalculatorIconButton(
                onClick = onClick,
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .pointerInput(maxX, maxY) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            hasUserMoved = true
                            offsetX = (offsetX + dragAmount.x).coerceIn(edgeMarginPx, maxX)
                            offsetY = (offsetY + dragAmount.y).coerceIn(edgeMarginPx, maxY)
                        }
                    }
            )
        }
    }
}

/**
 * Compact circular calculator button (48dp accessible hit target) styled in C&A Royal Blue + Orange.
 */
@Composable
fun FloatingCalculatorIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .luxuryTapScale(pressedScale = 0.94f)
            .clickable(onClick = onClick)
            .testTag("floating_calculator_button")
    ) {
        Surface(
            shape = CircleShape,
            color = RoyalBlueDeep,
            shadowElevation = 10.dp,
            border = BorderStroke(
                1.6.dp,
                Brush.linearGradient(
                    listOf(ElectricBlueBright, MetallicOrangeBright)
                )
            ),
            modifier = Modifier.size(46.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Calculate,
                    contentDescription = "Open Corporate Calculator (Drag to Move)",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * C&A GAS Premium Corporate Calculator Popup.
 * Features two explicit modes:
 * 1) Normal Calculator — standard arithmetic (never auto-adds VAT)
 * 2) Bill Calculator – 5% VAT — live UAE 5% VAT breakdown (Bill Amount, VAT 5%, Grand Total in AED)
 * Clean Midnight Royal Blue surface (zero black reflection box) + prominent Metallic Orange CLOSE (X) button.
 */
@Composable
fun QuickCalculatorPopupDialog(
    onDismiss: () -> Unit,
    onResultCopied: (String) -> Unit = {}
) {
    var calcState by remember { mutableStateOf(QuickCalculatorState()) }
    val clipboardManager = LocalClipboardManager.current
    val isDark = com.example.ui.theme.LocalIsDarkTheme.current

    val dialogBg = if (isDark) com.example.ui.theme.ObsidianMidnight else com.example.ui.theme.SignatureRoyalBackground
    val displayBg = if (isDark) com.example.ui.theme.ObsidianSurface else com.example.ui.theme.PureWhite
    val tabInactiveBg = if (isDark) com.example.ui.theme.ObsidianSurface else com.example.ui.theme.PureWhite
    val tabInactiveText = if (isDark) com.example.ui.theme.PureWhite else com.example.ui.theme.DeepNavyText
    val primaryText = if (isDark) com.example.ui.theme.PureWhite else com.example.ui.theme.DeepNavyText
    val secondaryText = if (isDark) com.example.ui.theme.TextSecondaryDark else com.example.ui.theme.DarkSlateSecondary
    val blueAccent = if (isDark) ElectricBlueGlow else com.example.ui.theme.ElectricBlueTextLight
    val orangeAccent = if (isDark) MetallicOrangeBright else com.example.ui.theme.WarningDarkOrange
    val cardBorder = if (isDark) com.example.ui.theme.ObsidianBorder else SignatureRoyalBorder

    var dialogContainerSize by remember { mutableStateOf(IntSize.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .onSizeChanged { newSize ->
                    if (newSize != dialogContainerSize) {
                        dialogContainerSize = newSize
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val maxDragX = (dialogContainerSize.width.toFloat() * 0.35f).coerceAtLeast(120f)
            val maxDragY = (dialogContainerSize.height.toFloat() * 0.38f).coerceAtLeast(160f)
            var dialogOffsetX by remember { mutableFloatStateOf(0f) }
            var dialogOffsetY by remember { mutableFloatStateOf(0f) }

            Surface(
                shape = RoundedCornerShape(26.dp),
                color = dialogBg,
                tonalElevation = 0.dp,
                shadowElevation = 22.dp,
                border = BorderStroke(
                    1.5.dp,
                    Brush.linearGradient(
                        listOf(
                            RoyalElectricBlue,
                            MetallicOrange
                        )
                    )
                ),
                modifier = Modifier
                    .widthIn(max = 382.dp)
                    .fillMaxWidth()
                    .offset { IntOffset(dialogOffsetX.roundToInt(), dialogOffsetY.roundToInt()) }
                    .testTag("quick_calculator_dialog")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SignatureAccentHairline(height = 2.5.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Draggable Header Bar with Metallic Orange CLOSE (X) Button
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(RoyalBlueDeep, RoyalElectricBlue)
                                    )
                                )
                                .pointerInput(maxDragX, maxDragY) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        dialogOffsetX = (dialogOffsetX + dragAmount.x).coerceIn(-maxDragX, maxDragX)
                                        dialogOffsetY = (dialogOffsetY + dragAmount.y).coerceIn(-maxDragY, maxDragY)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White.copy(alpha = 0.55f))
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, Color.White),
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (calcState.mode == CalculatorMode.BILL_VAT_5) {
                                                    Icons.Default.ReceiptLong
                                                } else {
                                                    Icons.Default.Calculate
                                                },
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (calcState.mode == CalculatorMode.BILL_VAT_5) {
                                                    "Bill VAT Calculator"
                                                } else {
                                                    "C&A Calculator"
                                                },
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.DragIndicator,
                                                contentDescription = "Drag to move calculator",
                                                tint = Color.White,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        Text(
                                            text = if (calcState.mode == CalculatorMode.BILL_VAT_5) {
                                                "UAE 5% VAT Bill Mode • Drag to move"
                                            } else {
                                                "Normal Mode (No VAT) • Drag to move"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = com.example.ui.theme.TextSecondaryDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (!calcState.hasError) {
                                                val copyVal = calcState.vatBreakdown?.grandTotal?.setScale(2, RoundingMode.HALF_UP)?.toPlainString()
                                                    ?: calcState.display
                                                clipboardManager.setText(AnnotatedString(copyVal))
                                                onResultCopied(
                                                    if (calcState.mode == CalculatorMode.BILL_VAT_5) {
                                                        "Copied Grand Total: AED $copyVal"
                                                    } else {
                                                        "Copied $copyVal"
                                                    }
                                                )
                                            }
                                        },
                                        modifier = Modifier.testTag("calc_copy_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy result",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Prominent Metallic Orange CLOSE (X) Button
                                    Surface(
                                        onClick = onDismiss,
                                        shape = RoundedCornerShape(12.dp),
                                        color = MetallicOrange,
                                        border = BorderStroke(1.2.dp, Color.White),
                                        shadowElevation = 6.dp,
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .luxuryTapScale(pressedScale = 0.94f)
                                            .testTag("calc_close_button")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(MetallicOrange, MetallicOrangeBright)
                                                    )
                                                )
                                                .padding(horizontal = 11.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Close calculator",
                                                tint = Color.White,
                                                modifier = Modifier.size(17.dp)
                                            )
                                            Text(
                                                text = "CLOSE",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = 0.6.sp,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Explicit Two-Mode Selector: Normal Calculator vs Bill Calculator – 5% VAT
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val isNormal = calcState.mode == CalculatorMode.NORMAL
                            Surface(
                                onClick = { calcState = QuickCalculatorEngine.setMode(calcState, CalculatorMode.NORMAL) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isNormal) RoyalElectricBlue else tabInactiveBg,
                                border = BorderStroke(
                                    1.2.dp,
                                    if (isNormal) RoyalElectricBlue else cardBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("calc_mode_normal")
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 9.dp, horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Normal Calculator",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isNormal) Color.White else tabInactiveText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            val isVat = calcState.mode == CalculatorMode.BILL_VAT_5
                            Surface(
                                onClick = { calcState = QuickCalculatorEngine.setMode(calcState, CalculatorMode.BILL_VAT_5) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isVat) MetallicOrange else tabInactiveBg,
                                border = BorderStroke(
                                    1.2.dp,
                                    if (isVat) MetallicOrange else cardBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("calc_mode_vat5")
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 9.dp, horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Bill – 5% VAT",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isVat) Color.White else tabInactiveText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // High-Contrast Display Panel
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = displayBg,
                            border = BorderStroke(
                                1.4.dp,
                                if (calcState.mode == CalculatorMode.BILL_VAT_5) MetallicOrange else RoyalElectricBlue
                            ),
                            shadowElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (calcState.mode == CalculatorMode.BILL_VAT_5) "BILL INPUT (AED)" else "STANDARD",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (calcState.mode == CalculatorMode.BILL_VAT_5) orangeAccent else blueAccent
                                    )
                                    Text(
                                        text = calcState.expression.ifBlank { " " },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontFamily = JetBrainsMono,
                                        fontWeight = FontWeight.Medium,
                                        color = secondaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.testTag("calc_expression_text")
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = calcState.display,
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = JetBrainsMono,
                                    color = if (calcState.hasError) {
                                        if (isDark) Color(0xFFFCA5A5) else com.example.ui.theme.ErrorDarkRed
                                    } else {
                                        primaryText
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("calc_display_text")
                                )

                                // Bill Calculator – 5% VAT Live Breakdown Panel
                                val breakdown = calcState.vatBreakdown
                                if (breakdown != null) {
                                    Spacer(Modifier.height(8.dp))
                                    HorizontalDivider(color = cardBorder)
                                    Spacer(Modifier.height(8.dp))
                                    VatBreakdownRow(
                                        label = "Bill Amount",
                                        value = breakdown.billAmountFormatted,
                                        labelColor = secondaryText,
                                        valueColor = primaryText,
                                        tag = "calc_vat_bill_amount"
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    VatBreakdownRow(
                                        label = "VAT (5%)",
                                        value = breakdown.vatAmountFormatted,
                                        labelColor = secondaryText,
                                        valueColor = blueAccent,
                                        tag = "calc_vat_tax_amount"
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = RoyalBlueDeep,
                                        border = BorderStroke(1.2.dp, MetallicOrange),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Grand Total",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFFFFEDD5)
                                            )
                                            Text(
                                                text = breakdown.grandTotalFormatted,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontFamily = JetBrainsMono,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White,
                                                modifier = Modifier.testTag("calc_vat_grand_total")
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Optional recent history chips (horizontally scrollable)
                        if (calcState.history.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                calcState.history.forEach { entry ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = displayBg,
                                        border = BorderStroke(1.dp, cardBorder),
                                        modifier = Modifier.clickable {
                                            calcState = QuickCalculatorEngine.applyHistoryResult(calcState, entry)
                                        }
                                    ) {
                                        Text(
                                            text = entry,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = JetBrainsMono,
                                            fontWeight = FontWeight.SemiBold,
                                            color = primaryText,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 4x5 Tactile Keypad (High-Contrast Bright Luxury & Dark Mode)
                        val rows = listOf(
                            listOf("C", "DEL", "%", "÷"),
                            listOf("7", "8", "9", "×"),
                            listOf("4", "5", "6", "-"),
                            listOf("1", "2", "3", "+"),
                            listOf("±", "0", ".", "=")
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            rows.forEach { rowKeys ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowKeys.forEach { key ->
                                        val isEquals = key == "="
                                        val isClear = key == "C"
                                        val isOperator = key in setOf("÷", "×", "-", "+", "%")
                                        val keyBrush = when {
                                            isEquals -> Brush.linearGradient(listOf(MetallicOrange, com.example.ui.theme.MoltenOrangeCore))
                                            isClear -> if (isDark) {
                                                Brush.linearGradient(listOf(Color(0xFF451624), Color(0xFF2B0E18)))
                                            } else {
                                                Brush.linearGradient(listOf(Color(0xFFFEE2E2), Color(0xFFFECACA)))
                                            }
                                            isOperator -> if (isDark) {
                                                Brush.linearGradient(listOf(Color(0xFF132D72), Color(0xFF0A1B47)))
                                            } else {
                                                Brush.linearGradient(listOf(Color(0xFFDBEAFE), Color(0xFFBFDBFE)))
                                            }
                                            else -> if (isDark) {
                                                Brush.linearGradient(listOf(Color(0xFF12234F), Color(0xFF091433)))
                                            } else {
                                                Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF1F5F9)))
                                            }
                                        }
                                        val contentColor = when {
                                            isEquals -> Color.White
                                            isClear -> if (isDark) Color(0xFFFCA5A5) else com.example.ui.theme.ErrorDarkRed
                                            isOperator -> if (isDark) com.example.ui.theme.NeonCyanGlow else com.example.ui.theme.ElectricBlueTextLight
                                            else -> primaryText
                                        }
                                        val borderBrush = when {
                                            isEquals -> Brush.linearGradient(listOf(Color.White, MetallicOrangeBright))
                                            isClear -> Brush.linearGradient(listOf(com.example.ui.theme.ErrorDarkRed, com.example.ui.theme.ErrorDarkRed))
                                            isOperator -> Brush.linearGradient(listOf(RoyalElectricBlue, ElectricBlueBright))
                                            else -> Brush.linearGradient(listOf(cardBorder, cardBorder))
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color.Transparent,
                                            border = BorderStroke(1.1.dp, borderBrush),
                                            shadowElevation = if (isEquals) 8.dp else 3.dp,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(52.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .luxuryTapScale(pressedScale = 0.94f)
                                                .clickable {
                                                    calcState = QuickCalculatorEngine.onKeyPress(calcState, key)
                                                }
                                                .testTag("calc_key_$key")
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(keyBrush),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (key == "DEL") {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                                        contentDescription = "Backspace",
                                                        tint = primaryText,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                } else {
                                                    Text(
                                                        text = key,
                                                        fontSize = if (isOperator || isEquals) 20.sp else 18.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontFamily = JetBrainsMono,
                                                        color = contentColor
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
            }
        }
    }
}

@Composable
private fun VatBreakdownRow(
    label: String,
    value: String,
    labelColor: Color,
    valueColor: Color,
    tag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = labelColor
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            modifier = Modifier.testTag(tag)
        )
    }
}
