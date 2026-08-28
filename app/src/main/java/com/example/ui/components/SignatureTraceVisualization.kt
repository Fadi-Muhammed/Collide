package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.PressureBand
import com.example.model.SemesterLoadResult
import com.example.model.WeekLoadData
import com.example.ui.theme.Busy
import com.example.ui.theme.Calm
import com.example.ui.theme.CollideType
import com.example.ui.theme.Collision
import com.example.ui.theme.Critical
import com.example.ui.theme.Ink
import com.example.ui.theme.Ink2
import com.example.ui.theme.Ink3
import com.example.ui.theme.Paper
import com.example.ui.theme.Plate
import com.example.ui.theme.Rule
import com.example.ui.theme.Steady
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * The Signature Trace Visualization.
 *
 * Requirements:
 * - SVG/Vector style rendering. Week columns as rects with a 2px gap, height mapped to hours with the tallest week at 100px.
 * - Colour from the band. Stepped fills, no gradient.
 * - Past weeks: 1px stroke in `Rule`, no fill.
 * - Current week: full-height 1px `Ink` line running through the trace.
 * - Hover / active touch a week: all other columns drop to 20% opacity. The week's hours and pressure appear in mono in the fixed slot at the trace's right edge. No floating tooltip.
 * - Click a week: triggers selection, drop-lines connect down to contributing items or highlighted card.
 * - Keyboard: arrow keys move week selection, focus ring visible, aria-label / semantics per column: "Week 10, 34 hours, over capacity."
 * - prefers-reduced-motion: instant transition. Otherwise, 400ms height transition with slight left-to-right stagger.
 * - Legible at 375px wide, fixed right-edge inspect slot.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SignatureTraceVisualization(
    loadResult: SemesterLoadResult?,
    selectedWeekNumber: Int?,
    hoveredWeekNumber: Int?,
    onHoverWeek: (Int?) -> Unit,
    onSelectWeek: (Int) -> Unit,
    prefersReducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val weeks = loadResult?.weeks ?: emptyList()
    val today = remember { LocalDate.now() }

    // Find maximum hours across all weeks to normalize tallest week to 100px (or minimum 1.0)
    val maxHours = remember(weeks) {
        val peak = weeks.maxOfOrNull { it.totalHours } ?: 1.0
        if (peak <= 0.0) 1.0 else peak
    }

    // Determine current week index (where today is between week startDate and endDate)
    val currentWeekNumber = remember(weeks, today) {
        weeks.firstOrNull { week ->
            !today.isBefore(week.startDate) && !today.isAfter(week.endDate)
        }?.weekNumber ?: -1
    }

    // Fixed right slot readout week
    val inspectWeek = remember(hoveredWeekNumber, selectedWeekNumber, weeks) {
        val targetNum = hoveredWeekNumber ?: selectedWeekNumber
        if (targetNum != null) {
            weeks.firstOrNull { it.weekNumber == targetNum }
        } else {
            // Default to peak week or first week if available
            weeks.maxByOrNull { it.totalHours } ?: weeks.firstOrNull()
        }
    }

    // Staggered animated height fractions per week (400ms duration, staggered left-to-right)
    val animatedFractions = remember { mutableMapOf<Int, Animatable<Float, *>>() }

    // Initialize and run animations on loadResult update
    LaunchedEffect(weeks, prefersReducedMotion) {
        weeks.forEachIndexed { index, week ->
            val targetFraction = (week.totalHours / maxHours).toFloat().coerceIn(0.02f, 1f)
            val animatable = animatedFractions.getOrPut(week.weekNumber) { Animatable(0f) }

            if (prefersReducedMotion) {
                animatable.snapTo(targetFraction)
            } else {
                launch {
                    delay(index * 18L) // 18ms left-to-right ripple stagger
                    animatable.animateTo(
                        targetValue = targetFraction,
                        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                    )
                }
            }
        }
    }

    val density = LocalDensity.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Rule, RoundedCornerShape(2.dp))
            .testTag("signature_trace_card"),
        color = Paper
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Top Trace Title & Fixed Right-Edge Inspect Slot
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "SEMESTER TRACE",
                        style = CollideType.measured11,
                        color = Ink2,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${weeks.size} WEEKS",
                        style = CollideType.measured11,
                        color = Ink3
                    )
                }

                // FIXED SLOT AT THE TRACE'S RIGHT EDGE (No floating tooltip)
                Box(
                    modifier = Modifier
                        .background(Plate, RoundedCornerShape(2.dp))
                        .border(1.dp, Rule, RoundedCornerShape(2.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("trace_fixed_inspect_slot"),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    if (inspectWeek != null) {
                        val inspectBandColor = when (inspectWeek.band) {
                            PressureBand.CALM -> Calm
                            PressureBand.STEADY -> Steady
                            PressureBand.BUSY -> Busy
                            PressureBand.COLLISION -> Collision
                            PressureBand.CRITICAL -> Critical
                        }
                        val pressurePercent = (inspectWeek.pressure * 100).toInt()
                        val isPast = inspectWeek.endDate.isBefore(today)
                        val statusSuffix = when {
                            inspectWeek.band >= PressureBand.CRITICAL -> "CRITICAL (OVER CAP)"
                            inspectWeek.band == PressureBand.COLLISION -> "COLLISION"
                            inspectWeek.band == PressureBand.BUSY -> "BUSY"
                            isPast -> "PAST"
                            else -> "STEADY"
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "WK ${inspectWeek.weekNumber}:",
                                style = CollideType.measured13,
                                fontWeight = FontWeight.Bold,
                                color = Ink
                            )
                            Text(
                                text = "${Math.round(inspectWeek.totalHours * 10.0) / 10.0}h",
                                style = CollideType.measured13,
                                fontWeight = FontWeight.Bold,
                                color = inspectBandColor
                            )
                            Text(
                                text = "· $pressurePercent% · $statusSuffix",
                                style = CollideType.measured11,
                                color = if (inspectWeek.band >= PressureBand.BUSY) inspectBandColor else Ink2
                            )
                        }
                    } else {
                        Text(
                            text = "HOVER WEEK TO INSPECT",
                            style = CollideType.measured11,
                            color = Ink3
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Trace Graph Area
            // Height is exactly 120dp (100px bar max + baseline space + headers)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Plate.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                    .border(1.dp, Rule.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .testTag("trace_graph_area")
            ) {
                val availableWidth = constraints.maxWidth.toFloat()
                val totalWeeks = weeks.size.coerceAtLeast(1)

                // 2px gap between columns
                val gapPx = with(density) { 2.dp.toPx() }
                val totalGapsWidth = gapPx * (totalWeeks - 1).coerceAtLeast(0)
                val columnWidthPx = ((availableWidth - totalGapsWidth) / totalWeeks).coerceAtLeast(with(density) { 6.dp.toPx() })

                val maxColumnHeightPx = with(density) { 100.dp.toPx() }

                val anyWeekHovered = hoveredWeekNumber != null
                val focusRequester = remember { FocusRequester() }

                // Interactive Trace Keyboard Navigation Row
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(focusRequester)
                        .focusable()
                        .onKeyEvent { keyEvent ->
                            val currentSelected = selectedWeekNumber ?: 1
                            when (keyEvent.key) {
                                Key.DirectionLeft -> {
                                    val newWeek = (currentSelected - 1).coerceAtLeast(1)
                                    onSelectWeek(newWeek)
                                    onHoverWeek(newWeek)
                                    true
                                }
                                Key.DirectionRight -> {
                                    val newWeek = (currentSelected + 1).coerceAtMost(totalWeeks)
                                    onSelectWeek(newWeek)
                                    onHoverWeek(newWeek)
                                    true
                                }
                                Key.Enter, Key.Spacebar -> {
                                    selectedWeekNumber?.let { onSelectWeek(it) }
                                    true
                                }
                                else -> false
                            }
                        }
                        .pointerInput(weeks) {
                            detectTapGestures(
                                onPress = { offset ->
                                    val weekIndex = (offset.x / (columnWidthPx + gapPx)).toInt().coerceIn(0, weeks.size - 1)
                                    val clickedWeek = weeks.getOrNull(weekIndex)
                                    if (clickedWeek != null) {
                                        onHoverWeek(clickedWeek.weekNumber)
                                    }
                                    tryAwaitRelease()
                                    // Keep selection
                                },
                                onTap = { offset ->
                                    val weekIndex = (offset.x / (columnWidthPx + gapPx)).toInt().coerceIn(0, weeks.size - 1)
                                    val clickedWeek = weeks.getOrNull(weekIndex)
                                    if (clickedWeek != null) {
                                        onSelectWeek(clickedWeek.weekNumber)
                                        onHoverWeek(clickedWeek.weekNumber)
                                    }
                                }
                            )
                        }
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("trace_svg_canvas")
                    ) {
                        val canvasHeight = size.height
                        val baselineY = canvasHeight - 16.dp.toPx()

                        // 1. Draw 100% capacity dashed guideline
                        val capacityHours = loadResult?.capacityHoursPerWeek ?: 25.0
                        val capacityRatio = (capacityHours / maxHours).toFloat().coerceIn(0.05f, 1f)
                        val capacityY = baselineY - (capacityRatio * maxColumnHeightPx)

                        drawLine(
                            color = Rule,
                            start = Offset(0f, capacityY),
                            end = Offset(size.width, capacityY),
                            strokeWidth = 1.dp.toPx()
                        )

                        // 2. Draw Columns for Each Week
                        weeks.forEachIndexed { index, week ->
                            val isPast = week.endDate.isBefore(today)
                            val isCurrentWeek = week.weekNumber == currentWeekNumber
                            val isSelected = week.weekNumber == selectedWeekNumber
                            val isHovered = week.weekNumber == hoveredWeekNumber

                            // Stepped fill color from band
                            val bandColor = when (week.band) {
                                PressureBand.CALM -> Calm
                                PressureBand.STEADY -> Steady
                                PressureBand.BUSY -> Busy
                                PressureBand.COLLISION -> Collision
                                PressureBand.CRITICAL -> Critical
                            }

                            // Height calculation with animation fraction
                            val animFraction = animatedFractions[week.weekNumber]?.value ?: ((week.totalHours / maxHours).toFloat().coerceIn(0.02f, 1f))
                            val columnHeightPx = (animFraction * maxColumnHeightPx).coerceAtLeast(3.dp.toPx())

                            val leftX = index * (columnWidthPx + gapPx)
                            val topY = baselineY - columnHeightPx

                            // Opacity rule: "Hover a week: all other columns drop to 20% opacity"
                            val alpha = if (anyWeekHovered) {
                                if (isHovered) 1.0f else 0.20f
                            } else if (selectedWeekNumber != null) {
                                if (isSelected) 1.0f else 0.40f
                            } else {
                                1.0f
                            }

                            if (isPast) {
                                // "Past weeks: 1px stroke in rule, no fill"
                                drawRect(
                                    color = Rule.copy(alpha = alpha),
                                    topLeft = Offset(leftX, topY),
                                    size = Size(columnWidthPx, columnHeightPx),
                                    style = Stroke(width = 1.dp.toPx())
                                )
                            } else {
                                // Stepped fill, no gradient
                                drawRect(
                                    color = bandColor.copy(alpha = alpha),
                                    topLeft = Offset(leftX, topY),
                                    size = Size(columnWidthPx, columnHeightPx)
                                )

                                // Selected focus ring / outline
                                if (isSelected) {
                                    drawRect(
                                        color = Ink,
                                        topLeft = Offset(leftX - 1.dp.toPx(), topY - 1.dp.toPx()),
                                        size = Size(columnWidthPx + 2.dp.toPx(), columnHeightPx + 2.dp.toPx()),
                                        style = Stroke(width = 1.5.dp.toPx())
                                    )
                                }
                            }

                            // Week column baseline label mark
                            drawRect(
                                color = if (isSelected || isHovered) Ink else Rule,
                                topLeft = Offset(leftX, baselineY + 2.dp.toPx()),
                                size = Size(columnWidthPx, 2.dp.toPx())
                            )

                            // "Current week: full-height 1px ink line running through the trace"
                            if (isCurrentWeek) {
                                val currentLineX = leftX + (columnWidthPx / 2f)
                                drawLine(
                                    color = Ink,
                                    start = Offset(currentLineX, 0f),
                                    end = Offset(currentLineX, canvasHeight),
                                    strokeWidth = 1.5.dp.toPx()
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Week Number Axis & Accessibility Helper Elements
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WK 1",
                    style = CollideType.measured11,
                    color = Ink3
                )
                Text(
                    text = "— 100% CAPACITY (${loadResult?.capacityHoursPerWeek?.toInt() ?: 25}h) —",
                    style = CollideType.measured11,
                    color = Ink3
                )
                Text(
                    text = "WK ${weeks.size}",
                    style = CollideType.measured11,
                    color = Ink3
                )
            }

            // Accessibility Semantic hidden column representations for screen readers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.dp)
            ) {
                weeks.forEach { week ->
                    val isPast = week.endDate.isBefore(today)
                    val statusStr = when {
                        week.band >= PressureBand.COLLISION -> "over capacity"
                        week.band == PressureBand.BUSY -> "at capacity"
                        isPast -> "past week"
                        else -> "within capacity"
                    }
                    val ariaLabel = "Week ${week.weekNumber}, ${week.totalHours.toInt()} hours, $statusStr."

                    Box(
                        modifier = Modifier
                            .size(1.dp)
                            .semantics {
                                contentDescription = ariaLabel
                                role = Role.Button
                                selected = (week.weekNumber == selectedWeekNumber)
                            }
                    )
                }
            }
        }
    }
}
