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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.BrandGreenContainer
import com.example.ui.theme.BrandGreenOnContainer
import com.example.ui.theme.BrandGreenPrimary
import com.example.ui.theme.BrandRedContainer
import com.example.ui.theme.BrandRedOnContainer
import com.example.ui.theme.BrandRedSecondary
import com.example.ui.theme.JetBrainsMono
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.roundToInt

data class QuickCalculatorState(
    val display: String = "0",
    val expression: String = "",
    val accumulator: BigDecimal? = null,
    val pendingOperator: String? = null,
    val resetDisplayOnNextDigit: Boolean = false,
    val hasError: Boolean = false,
    val history: List<String> = emptyList()
)

/**
 * Pure, safe arithmetic engine for the C&A Gas Quick Pop-up Calculator.
 * Uses BigDecimal with zero dynamic code evaluation and safe divide-by-zero handling.
 */
object QuickCalculatorEngine {

    private val mathContext = MathContext(12, RoundingMode.HALF_UP)

    fun onKeyPress(state: QuickCalculatorState, key: String): QuickCalculatorState {
        if (key == "C") {
            return QuickCalculatorState(history = state.history)
        }
        val active = if (state.hasError) QuickCalculatorState(history = state.history) else state

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
 * Users can smoothly drag and park the circular calculator button anywhere across the entire screen
 * (top, bottom, left, right, middle) without blocking any content, and tap it anytime to pop up the calculator.
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
 * Compact small circular calculator icon button (with full 48dp accessible hit target)
 * that pops up the Quick Calculator Dialog from any screen in C&A Gas.
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
            .clickable(onClick = onClick)
            .testTag("floating_calculator_button")
    ) {
        Surface(
            shape = CircleShape,
            color = BrandGreenPrimary,
            shadowElevation = 8.dp,
            border = BorderStroke(1.8.dp, Color.White.copy(alpha = 0.9f)),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Calculate,
                    contentDescription = "Open Pop-up Calculator (Drag to Move)",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * Freely movable, smoothly scrollable pop-up calculator modal for quick cylinder count,
 * billing, and balance math. Drag the top bar to reposition anywhere across the full screen,
 * and scroll smoothly inside on compact displays.
 */
@Composable
fun QuickCalculatorPopupDialog(
    onDismiss: () -> Unit,
    onResultCopied: (String) -> Unit = {}
) {
    var calcState by remember { mutableStateOf(QuickCalculatorState()) }
    val clipboardManager = LocalClipboardManager.current
    val density = LocalDensity.current

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
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .offset { IntOffset(dialogOffsetX.roundToInt(), dialogOffsetY.roundToInt()) }
                    .testTag("quick_calculator_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Draggable Top Grip + Header Bar (Drag anywhere on screen comfortably!)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                            .pointerInput(maxDragX, maxDragY) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    dialogOffsetX = (dialogOffsetX + dragAmount.x).coerceIn(-maxDragX, maxDragX)
                                    dialogOffsetY = (dialogOffsetY + dragAmount.y).coerceIn(-maxDragY, maxDragY)
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(38.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = BrandGreenContainer,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Calculate,
                                            contentDescription = null,
                                            tint = BrandGreenPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Quick Calculator",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.DragIndicator,
                                            contentDescription = "Drag to move calculator",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    Text(
                                        text = "Drag header to move anywhere • Scrollable",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (!calcState.hasError) {
                                            clipboardManager.setText(AnnotatedString(calcState.display))
                                            onResultCopied("Copied ${calcState.display}")
                                        }
                                    },
                                    modifier = Modifier.testTag("calc_copy_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy result",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.testTag("calc_close_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close calculator"
                                    )
                                }
                            }
                        }
                    }

                    // Display Panel
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = calcState.expression.ifBlank { " " },
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = JetBrainsMono,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("calc_expression_text")
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = calcState.display,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = JetBrainsMono,
                                color = if (calcState.hasError) BrandRedSecondary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.End,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("calc_display_text")
                            )
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
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.clickable {
                                        calcState = QuickCalculatorEngine.applyHistoryResult(calcState, entry)
                                    }
                                ) {
                                    Text(
                                        text = entry,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = JetBrainsMono,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 4x5 Tactile Keypad
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
                                    val containerColor = when {
                                        isEquals -> BrandGreenPrimary
                                        isClear -> BrandRedContainer
                                        isOperator -> BrandGreenContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                    val contentColor = when {
                                        isEquals -> Color.White
                                        isClear -> BrandRedOnContainer
                                        isOperator -> BrandGreenOnContainer
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = containerColor,
                                        border = if (!isEquals) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) else null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(50.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                calcState = QuickCalculatorEngine.onKeyPress(calcState, key)
                                            }
                                            .testTag("calc_key_$key")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (key == "DEL") {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                                                    contentDescription = "Backspace",
                                                    tint = contentColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            } else {
                                                Text(
                                                    text = key,
                                                    fontSize = if (isOperator || isEquals) 20.sp else 18.sp,
                                                    fontWeight = FontWeight.Bold,
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
