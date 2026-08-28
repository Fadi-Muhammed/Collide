package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.SemesterEntity
import com.example.model.Assessment
import com.example.model.Course
import com.example.model.PressureBand
import com.example.model.SemesterLoadResult
import com.example.model.WeekContributor
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
import com.example.ui.theme.pressureToColor
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun SemesterTraceScreen(
    semester: SemesterEntity,
    courses: List<Course>,
    assessments: List<Assessment>,
    loadResult: SemesterLoadResult?,
    onUpdateCapacity: (Double) -> Unit,
    onOpenSettings: () -> Unit,
    onNavigateToReview: () -> Unit,
    onUpdateAssessment: (Assessment) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val withheldCount = remember(assessments) {
        assessments.count { !it.isConfirmed && it.confidence < 0.85 }
    }
    val confirmedCount = remember(assessments) {
        assessments.count { it.isConfirmed || it.confidence >= 0.85 }
    }

    var selectedFilterBand by remember { mutableStateOf<PressureBand?>(null) }
    var showOnlyCollisions by remember { mutableStateOf(false) }

    // Trace selection and hover states
    var selectedWeekNumber by remember { mutableStateOf<Int?>(null) }
    var hoveredWeekNumber by remember { mutableStateOf<Int?>(null) }

    // Assessment Detail inspection modal
    var inspectingAssessment by remember { mutableStateOf<Assessment?>(null) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val courseMap = remember(courses) { courses.associateBy { it.id } }

    val weeks = loadResult?.weeks ?: emptyList()
    val filteredWeeks = weeks.filter { week ->
        if (showOnlyCollisions) {
            week.band >= PressureBand.BUSY
        } else if (selectedFilterBand != null) {
            week.band == selectedFilterBand
        } else {
            true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Plate)
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .testTag("semester_trace_screen")
    ) {
        // Trace Header with Title & Quick Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "WORKLOAD TRACE",
                    style = CollideType.title24,
                    color = Ink
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Stepped chromatic trace with back-spread linear ramp. Hover to inspect, click to scroll.",
                    style = CollideType.interface13,
                    color = Ink2
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(2.dp),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        containerColor = Paper
                    ),
                    modifier = Modifier.testTag("trace_settings_button")
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp), tint = Ink)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("How long things take you", style = CollideType.interface13, color = Ink)
                }

                if (withheldCount > 0) {
                    Button(
                        onClick = onNavigateToReview,
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Ink),
                        modifier = Modifier.testTag("trace_review_button")
                    ) {
                        Text("Review ($withheldCount pending)", style = CollideType.interface13, color = Paper)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Capacity Tuning & Trace Statistics Control Bar
        CapacityTuningBar(
            currentCapacity = semester.capacityHoursPerWeek,
            busyOrWorseCount = loadResult?.busyOrWorseWeekCount ?: 0,
            maxPressure = loadResult?.maxPressure ?: 0.0,
            totalSemesterHours = loadResult?.totalSemesterHours ?: 0.0,
            onCapacityChanged = onUpdateCapacity
        )

        Spacer(modifier = Modifier.height(14.dp))

        // SIGNATURE TRACE SVG / VECTOR VISUALIZATION (Part 6)
        SignatureTraceVisualization(
            loadResult = loadResult,
            selectedWeekNumber = selectedWeekNumber,
            hoveredWeekNumber = hoveredWeekNumber,
            onHoverWeek = { hoveredWeekNumber = it },
            onSelectWeek = { weekNum ->
                selectedWeekNumber = weekNum
                val targetIndex = filteredWeeks.indexOfFirst { it.weekNumber == weekNum }
                if (targetIndex >= 0) {
                    coroutineScope.launch {
                        listState.animateScrollToItem(targetIndex)
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Withheld items notice
        if (withheldCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Rule, RoundedCornerShape(2.dp))
                    .background(Paper)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Busy,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Trace rule active: $withheldCount low-confidence extractions are withheld until confirmed.",
                        style = CollideType.interface13,
                        color = Ink
                    )
                }
                Text(
                    text = "Review now →",
                    style = CollideType.interface13,
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToReview() }
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Filter / legend pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                label = "All Weeks (${loadResult?.weeks?.size ?: 0})",
                isSelected = !showOnlyCollisions && selectedFilterBand == null,
                color = Ink,
                onClick = {
                    showOnlyCollisions = false
                    selectedFilterBand = null
                }
            )

            FilterChip(
                label = "Busy or Worse (${loadResult?.busyOrWorseWeekCount ?: 0})",
                isSelected = showOnlyCollisions,
                color = Collision,
                onClick = {
                    showOnlyCollisions = !showOnlyCollisions
                    selectedFilterBand = null
                }
            )

            PressureBand.values().forEach { band ->
                val count = loadResult?.weeks?.count { it.band == band } ?: 0
                val bandColor = when (band) {
                    PressureBand.CALM -> Calm
                    PressureBand.STEADY -> Steady
                    PressureBand.BUSY -> Busy
                    PressureBand.COLLISION -> Collision
                    PressureBand.CRITICAL -> Critical
                }
                FilterChip(
                    label = "${band.label.replaceFirstChar { it.uppercase() }} ($count)",
                    isSelected = selectedFilterBand == band,
                    color = bandColor,
                    onClick = {
                        showOnlyCollisions = false
                        selectedFilterBand = if (selectedFilterBand == band) null else band
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (filteredWeeks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Paper)
                    .border(1.dp, Rule, RoundedCornerShape(2.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (confirmedCount == 0) "No active assessments in the trace." else "No weeks match the selected filter.",
                        style = CollideType.interface15,
                        color = Ink
                    )
                    if (confirmedCount == 0) {
                        Text(
                            text = "Confirm extractions in Review to project your workload trace.",
                            style = CollideType.interface13,
                            color = Ink2
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("trace_weeks_list"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredWeeks, key = { it.weekNumber }) { weekData ->
                    WeekLoadCard(
                        weekData = weekData,
                        allWeeks = weeks,
                        isSelected = selectedWeekNumber == weekData.weekNumber,
                        onSelectThisWeek = { selectedWeekNumber = weekData.weekNumber },
                        onNavigateToReview = onNavigateToReview,
                        onInspectAssessment = { inspectingAssessment = it }
                    )
                }
            }
        }
    }

    // Modal when user clicks an assessment from any week
    if (inspectingAssessment != null) {
        val course = courseMap[inspectingAssessment!!.courseId]
        AssessmentDetailModal(
            assessment = inspectingAssessment!!,
            course = course,
            onSaveAssessment = { updated ->
                onUpdateAssessment(updated)
                inspectingAssessment = null
            },
            onDismiss = { inspectingAssessment = null }
        )
    }
}

/**
 * Top control bar with interactive capacity adjuster and high-level semester stats.
 */
@Composable
private fun CapacityTuningBar(
    currentCapacity: Double,
    busyOrWorseCount: Int,
    maxPressure: Double,
    totalSemesterHours: Double,
    onCapacityChanged: (Double) -> Unit
) {
    var sliderValue by remember(currentCapacity) { mutableDoubleStateOf(currentCapacity) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Rule, RoundedCornerShape(2.dp)),
        color = Paper
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Capacity Stepper / Controller
                Column {
                    Text(
                        text = "WEEKLY STUDY CAPACITY",
                        style = CollideType.measured11,
                        color = Ink2
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${currentCapacity.toInt()}h",
                            style = CollideType.title24,
                            color = Ink,
                            modifier = Modifier.testTag("current_capacity_label")
                        )
                        Text(
                            text = "/ week",
                            style = CollideType.interface13,
                            color = Ink2
                        )
                    }
                }

                // Quick Preset Buttons to easily demonstrate 25h -> 15h repaint
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val presets = listOf(15.0, 20.0, 25.0, 30.0, 35.0)
                    presets.forEach { preset ->
                        val isSelected = Math.abs(currentCapacity - preset) < 0.5
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isSelected) Ink else Plate)
                                .border(1.dp, if (isSelected) Ink else Rule, RoundedCornerShape(2.dp))
                                .clickable {
                                    sliderValue = preset
                                    onCapacityChanged(preset)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("capacity_preset_${preset.toInt()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${preset.toInt()}h",
                                style = CollideType.measured13,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Paper else Ink
                            )
                        }
                    }
                }

                // Micro Stepper Buttons (+ / -)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val newCap = (currentCapacity - 5.0).coerceAtLeast(5.0)
                            sliderValue = newCap
                            onCapacityChanged(newCap)
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Plate, RoundedCornerShape(2.dp))
                            .border(1.dp, Rule, RoundedCornerShape(2.dp))
                            .testTag("capacity_decrease_button")
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease capacity", tint = Ink)
                    }

                    IconButton(
                        onClick = {
                            val newCap = (currentCapacity + 5.0).coerceAtMost(80.0)
                            sliderValue = newCap
                            onCapacityChanged(newCap)
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Plate, RoundedCornerShape(2.dp))
                            .border(1.dp, Rule, RoundedCornerShape(2.dp))
                            .testTag("capacity_increase_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase capacity", tint = Ink)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Continuous Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("5h", style = CollideType.measured11, color = Ink3)
                Slider(
                    value = sliderValue.toFloat(),
                    onValueChange = {
                        sliderValue = it.toDouble()
                    },
                    onValueChangeFinished = {
                        val rounded = Math.round(sliderValue).toDouble()
                        onCapacityChanged(rounded)
                    },
                    valueRange = 5f..60f,
                    steps = 10,
                    colors = SliderDefaults.colors(
                        thumbColor = Ink,
                        activeTrackColor = Ink,
                        inactiveTrackColor = Rule
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("capacity_slider")
                )
                Text("60h", style = CollideType.measured11, color = Ink3)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOTAL SEMESTER: ${totalSemesterHours.toInt()}h study",
                    style = CollideType.measured11,
                    color = Ink2
                )
                Text(
                    text = "PEAK LOAD: ${(maxPressure * 100).toInt()}% capacity",
                    style = CollideType.measured11,
                    color = if (maxPressure >= 1.10) Collision else Ink2
                )
                Text(
                    text = "CRITICAL / BUSY WEEKS: $busyOrWorseCount",
                    style = CollideType.measured11,
                    color = if (busyOrWorseCount > 0) Collision else Steady,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Individual Week Load Card with Deterministic Collision Explanation and Back-Spread visualization.
 */
@Composable
private fun WeekLoadCard(
    weekData: WeekLoadData,
    allWeeks: List<WeekLoadData> = emptyList(),
    isSelected: Boolean = false,
    onSelectThisWeek: () -> Unit = {},
    onNavigateToReview: () -> Unit,
    onInspectAssessment: (Assessment) -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(false) }

    val bandColor = when (weekData.band) {
        PressureBand.CALM -> Calm
        PressureBand.STEADY -> Steady
        PressureBand.BUSY -> Busy
        PressureBand.COLLISION -> Collision
        PressureBand.CRITICAL -> Critical
    }

    val dateFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
    val dateRangeStr = "${weekData.startDate.format(dateFormatter)} – ${weekData.endDate.format(dateFormatter)}"

    val progressFraction = (weekData.totalHours / weekData.capacityHours.coerceAtLeast(1.0)).coerceIn(0.0, 2.5)
    val loadPercent = (weekData.pressure * 100).toInt()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Ink else if (weekData.band >= PressureBand.COLLISION) bandColor.copy(alpha = 0.5f) else Rule,
                shape = RoundedCornerShape(2.dp)
            )
            .clickable { onSelectThisWeek() }
            .testTag("week_card_${weekData.weekNumber}"),
        color = if (isSelected) Paper else Paper
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Drop-line from trace indicator when selected
            if (isSelected) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Ink, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .height(1.dp)
                            .width(20.dp)
                            .background(Ink)
                    )
                    Text(
                        text = "FOCUSED FROM TRACE · WEEK ${weekData.weekNumber}",
                        style = CollideType.measured11,
                        color = Ink,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Header Row: Week Number, Date, Band Badge, Hours vs Capacity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Week Number & Date Range
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "WEEK ${weekData.weekNumber}",
                        style = CollideType.interface18,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )
                    Text(
                        text = dateRangeStr,
                        style = CollideType.measured13,
                        color = Ink2
                    )
                }

                // Right: Band Badge & Ratio
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Band Badge
                    Box(
                        modifier = Modifier
                            .background(bandColor.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
                            .border(1.dp, bandColor, RoundedCornerShape(2.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = weekData.band.label.uppercase(),
                            style = CollideType.measured11,
                            fontWeight = FontWeight.Bold,
                            color = bandColor
                        )
                    }

                    Text(
                        text = "${Math.round(weekData.totalHours)}h / ${weekData.capacityHours.toInt()}h",
                        style = CollideType.measured15,
                        fontWeight = FontWeight.Bold,
                        color = if (weekData.band >= PressureBand.BUSY) bandColor else Ink
                    )

                    Text(
                        text = "(${loadPercent}%)",
                        style = CollideType.measured11,
                        color = Ink2
                    )

                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = Ink2
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Workload Progress Bar (with 100% capacity threshold mark)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Plate)
            ) {
                // Capacity marker at 1.0 fraction (scaled to max 2.0 width)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val capacityX = size.width * (1.0f / 2.0f)
                    val barWidth = (progressFraction.toFloat() / 2.0f).coerceIn(0f, 1f) * size.width

                    // Filled workload bar
                    drawRoundRect(
                        color = bandColor,
                        size = Size(barWidth, size.height),
                        cornerRadius = CornerRadius(2.dp.toPx())
                    )

                    // Vertical hairline threshold at 100% capacity
                    drawLine(
                        color = Ink.copy(alpha = 0.4f),
                        start = Offset(capacityX, 0f),
                        end = Offset(capacityX, size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }

            // Step 4: Deterministic Collision Explanation Sentence
            if (weekData.band >= PressureBand.BUSY && weekData.collisionExplanation != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bandColor.copy(alpha = 0.08f), RoundedCornerShape(2.dp))
                        .border(1.dp, bandColor.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                        .padding(12.dp)
                        .testTag("collision_explanation_week_${weekData.weekNumber}")
                ) {
                    Text(
                        text = weekData.collisionHeadline,
                        style = CollideType.measured13,
                        fontWeight = FontWeight.Bold,
                        color = bandColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = weekData.collisionExplanation,
                        style = CollideType.prose15,
                        color = Ink
                    )
                }
            }

            // Daily 7-Day Ramp Distribution Bar
            Spacer(modifier = Modifier.height(12.dp))
            DailyLoadDistributionBar(
                weekStart = weekData.startDate,
                dailyHours = weekData.dailyHours,
                bandColor = bandColor
            )

            // In-progress workload items in this week (not just due this week)
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ASSESSMENTS IN PROGRESS THIS WEEK (${weekData.contributors.size})",
                        style = CollideType.measured11,
                        color = Ink2,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Click to inspect / edit",
                        style = CollideType.measured11,
                        color = Ink3
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (weekData.contributors.isEmpty()) {
                    val nextPressureWeek = allWeeks.filter { it.weekNumber > weekData.weekNumber && it.band >= PressureBand.BUSY }.minByOrNull { it.weekNumber }
                    val clearCopy = if (nextPressureWeek != null) {
                        "Week ${weekData.weekNumber} is clear. The next pressure is week ${nextPressureWeek.weekNumber} (${nextPressureWeek.band.label})."
                    } else {
                        "Week ${weekData.weekNumber} is clear. No pressure projected for this period."
                    }
                    Text(
                        text = clearCopy,
                        style = CollideType.interface13,
                        color = Ink2
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        weekData.contributors.forEach { c ->
                            ContributorRow(c = c, onClick = { onInspectAssessment(c.assessment) })
                        }
                    }
                }
            }
        }
    }
}

/**
 * Visual 7-day mini ramp bar across Monday to Sunday.
 */
@Composable
private fun DailyLoadDistributionBar(
    weekStart: java.time.LocalDate,
    dailyHours: Map<java.time.LocalDate, Double>,
    bandColor: Color
) {
    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val maxDaily = dailyHours.values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Plate.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        daysOfWeek.forEachIndexed { index, dayName ->
            val date = weekStart.plusDays(index.toLong())
            val hours = dailyHours[date] ?: 0.0
            val fraction = (hours / maxDaily).coerceIn(0.0, 1.0)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = dayName,
                    style = CollideType.measured11,
                    color = Ink2
                )

                // Mini vertical bar
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(Rule.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (hours > 0.0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction.toFloat().coerceAtLeast(0.15f))
                                .background(if (hours > 6.0) bandColor else bandColor.copy(alpha = 0.7f))
                        )
                    }
                }

                Text(
                    text = if (hours > 0.0) "${Math.round(hours * 10.0) / 10.0}h" else "—",
                    style = CollideType.measured11,
                    color = if (hours > 0.0) Ink else Ink3
                )
            }
        }
    }
}

/**
 * Breakdown row for an individual contributing assessment.
 */
@Composable
private fun ContributorRow(
    c: WeekContributor,
    onClick: () -> Unit = {}
) {
    val code = c.course?.code ?: "COURSE"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Rule, RoundedCornerShape(2.dp))
            .background(Plate)
            .clickable { onClick() }
            .padding(10.dp)
            .testTag("contributor_row_${c.assessment.id}"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = code,
                style = CollideType.measured11,
                fontWeight = FontWeight.Bold,
                color = Ink,
                modifier = Modifier
                    .background(Paper, RoundedCornerShape(2.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
            Column {
                Text(
                    text = c.assessment.title,
                    style = CollideType.interface13,
                    fontWeight = FontWeight.Medium,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DateBasisDisplay(assessment = c.assessment, onNeedsDateClick = onClick)
                    Text(
                        text = "· ${c.assessment.type.displayName} · ${c.assessment.weightPercent.toInt()}%",
                        style = CollideType.measured11,
                        color = Ink2
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "${Math.round(c.totalHoursInWeek * 10.0) / 10.0}h this week",
                style = CollideType.measured13,
                fontWeight = FontWeight.Bold,
                color = Ink
            )
            Text(
                text = "of ${c.assessment.estimatedHours.toInt()}h total",
                style = CollideType.measured11,
                color = Ink2
            )
        }
    }
}

/**
 * Filter Chip pill.
 */
@Composable
private fun FilterChip(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(2.dp))
            .background(if (isSelected) color else Paper)
            .border(1.dp, if (isSelected) color else Rule, RoundedCornerShape(2.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = CollideType.measured11,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Paper else Ink
        )
    }
}
