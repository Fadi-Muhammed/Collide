package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.SemesterEntity
import com.example.model.Assessment
import com.example.model.AssessmentType
import com.example.model.Course
import com.example.model.DateBasis
import com.example.model.HoursBasis
import com.example.ui.theme.Busy
import com.example.ui.theme.CollideType
import com.example.ui.theme.Collision
import com.example.ui.theme.Ink
import com.example.ui.theme.Ink2
import com.example.ui.theme.Ink3
import com.example.ui.theme.Paper
import com.example.ui.theme.Plate
import com.example.ui.theme.Rule
import com.example.ui.theme.Steady
import kotlinx.coroutines.launch

/**
 * Dedicated Assessment Review Screen.
 *
 * Requirements:
 * - Dedicated full screen (not a modal).
 * - Lists every assessment as a row: title, type, weight, date, confidence dot.
 * - Sorted worst-confidence first.
 * - Clicking a row expands it to show the source quote in Newsreader with page number in mono,
 *   plus the rendered PDF page next to it.
 * - Three actions per row: confirm, edit, delete.
 * - Bulk confirm for a whole course.
 * - Rule: Nothing reaches the trace until it has been confirmed or has confidence above 0.85.
 */
@Composable
fun ReviewScreen(
    semester: SemesterEntity,
    courses: List<Course>,
    assessments: List<Assessment>,
    selectedCourseFilterId: String?,
    onSelectCourseFilter: (String?) -> Unit,
    onConfirmAssessment: (String) -> Unit,
    onBulkConfirmCourse: (String) -> Unit,
    onUpdateAssessment: (Assessment) -> Unit,
    onDeleteAssessment: (String) -> Unit,
    onRenderPdfPage: suspend (String, Int) -> Bitmap?,
    modifier: Modifier = Modifier
) {
    var expandedAssessmentId by remember { mutableStateOf<String?>(null) }
    var editingAssessment by remember { mutableStateOf<Assessment?>(null) }
    var deletingAssessment by remember { mutableStateOf<Assessment?>(null) }

    // Map course ID to Course object
    val courseMap = remember(courses) { courses.associateBy { it.id } }

    // Filter assessments by course if a filter is active
    val filteredAssessments = remember(assessments, selectedCourseFilterId) {
        if (selectedCourseFilterId != null) {
            assessments.filter { it.courseId == selectedCourseFilterId }
        } else {
            assessments
        }
    }

    // Sort strictly worst-confidence first (lowest confidence first so student reviews uncertain items first)
    val sortedAssessments = remember(filteredAssessments) {
        filteredAssessments.sortedWith(
            compareBy<Assessment> { it.confidence }
                .thenBy { it.isConfirmed }
                .thenBy { it.dueDate ?: "9999-99-99" }
        )
    }

    // Trace status metrics
    val totalCount = assessments.size
    val autoAcceptedCount = assessments.count { !it.isConfirmed && it.confidence >= 0.85 }
    val manuallyConfirmedCount = assessments.count { it.isConfirmed }
    val pendingReviewCount = assessments.count { !it.isConfirmed && it.confidence < 0.85 }
    val traceEligibleCount = assessments.count { it.isConfirmed || it.confidence >= 0.85 }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("review_screen")
    ) {
        // Top Header Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ASSESSMENT REVIEW",
                    style = CollideType.title24,
                    color = Ink,
                    modifier = Modifier.testTag("review_screen_title")
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "SORTED WORST-CONFIDENCE FIRST · CITATIONS FROM SYLLABI",
                    style = CollideType.measured11,
                    color = Ink2
                )
            }

            // Trace Readiness Counter
            Row(
                modifier = Modifier
                    .background(Plate, RoundedCornerShape(2.dp))
                    .border(1.dp, Rule, RoundedCornerShape(2.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (pendingReviewCount == 0) Steady else Busy, CircleShape)
                )
                Text(
                    text = "TRACE READY: $traceEligibleCount / $totalCount",
                    style = CollideType.measured11,
                    color = Ink,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Trace Rule Explanatory Banner
        TraceRuleBanner(
            traceEligibleCount = traceEligibleCount,
            totalCount = totalCount,
            autoAcceptedCount = autoAcceptedCount,
            manuallyConfirmedCount = manuallyConfirmedCount,
            pendingReviewCount = pendingReviewCount
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Course filter chips & Bulk confirm action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Horizontal course filter selector
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // All Courses Chip
                val isAllSelected = selectedCourseFilterId == null
                Box(
                    modifier = Modifier
                        .clickable { onSelectCourseFilter(null) }
                        .background(if (isAllSelected) Ink else Paper, RoundedCornerShape(2.dp))
                        .border(1.dp, if (isAllSelected) Ink else Rule, RoundedCornerShape(2.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("filter_all_courses"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ALL (${assessments.size})",
                        style = CollideType.measured11,
                        color = if (isAllSelected) Paper else Ink
                    )
                }

                // Individual Course Chips
                courses.forEach { course ->
                    val isSelected = selectedCourseFilterId == course.id
                    val courseAssessments = assessments.filter { it.courseId == course.id }
                    val unconfirmedCount = courseAssessments.count { !it.isConfirmed }

                    Box(
                        modifier = Modifier
                            .clickable { onSelectCourseFilter(course.id) }
                            .background(if (isSelected) Ink else Paper, RoundedCornerShape(2.dp))
                            .border(1.dp, if (isSelected) Ink else Rule, RoundedCornerShape(2.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("filter_course_${course.code}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = course.code,
                                style = CollideType.measured11,
                                color = if (isSelected) Paper else Ink
                            )
                            Text(
                                text = "(${courseAssessments.size})",
                                style = CollideType.measured11,
                                color = if (isSelected) Paper.copy(alpha = 0.7f) else Ink2
                            )
                            if (unconfirmedCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Busy, CircleShape)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Bulk Confirm Button for selected course
            if (selectedCourseFilterId != null) {
                val targetCourse = courseMap[selectedCourseFilterId]
                val courseName = targetCourse?.code ?: "Course"
                Button(
                    onClick = { onBulkConfirmCourse(selectedCourseFilterId) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Ink,
                        contentColor = Paper
                    ),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.testTag("bulk_confirm_course_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Confirm all $courseName",
                        style = CollideType.interface13
                    )
                }
            } else if (courses.isNotEmpty()) {
                // Bulk confirm dropdown or quick confirm
                var showBulkMenu by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { showBulkMenu = true },
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.testTag("bulk_confirm_dropdown_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = null,
                            tint = Ink,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Bulk confirm course",
                            style = CollideType.interface13,
                            color = Ink
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = Ink
                        )
                    }

                    DropdownMenu(
                        expanded = showBulkMenu,
                        onDismissRequest = { showBulkMenu = false }
                    ) {
                        courses.forEach { course ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Confirm all ${course.code} (${assessments.count { it.courseId == course.id }} items)",
                                        style = CollideType.interface13
                                    )
                                },
                                onClick = {
                                    showBulkMenu = false
                                    onBulkConfirmCourse(course.id)
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main List of Assessment Rows
        if (sortedAssessments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Paper)
                    .border(1.dp, Rule, RoundedCornerShape(2.dp))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (assessments.isEmpty()) "No assessments extracted yet." else "No assessments match this course filter.",
                        style = CollideType.interface15,
                        color = Ink
                    )
                    Text(
                        text = if (assessments.isEmpty()) "Switch to the Intake tab and extract your syllabi to generate assessments." else "Select 'ALL' above to view assessments across all courses.",
                        style = CollideType.interface13,
                        color = Ink2
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("review_assessments_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sortedAssessments, key = { it.id }) { assessment ->
                    val isExpanded = expandedAssessmentId == assessment.id
                    val course = courseMap[assessment.courseId]

                    AssessmentReviewRow(
                        assessment = assessment,
                        courseCode = course?.code ?: "COURSE",
                        isExpanded = isExpanded,
                        onToggleExpand = {
                            expandedAssessmentId = if (isExpanded) null else assessment.id
                        },
                        onConfirm = { onConfirmAssessment(assessment.id) },
                        onEdit = { editingAssessment = assessment },
                        onDelete = { deletingAssessment = assessment },
                        onRenderPdfPage = onRenderPdfPage
                    )
                }
            }
        }
    }

    // Edit Assessment Dialog
    if (editingAssessment != null) {
        EditAssessmentDialog(
            assessment = editingAssessment!!,
            onDismiss = { editingAssessment = null },
            onSave = { updated ->
                onUpdateAssessment(updated)
                editingAssessment = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (deletingAssessment != null) {
        DeleteAssessmentDialog(
            assessment = deletingAssessment!!,
            onDismiss = { deletingAssessment = null },
            onConfirmDelete = {
                onDeleteAssessment(deletingAssessment!!.id)
                deletingAssessment = null
            }
        )
    }
}

/**
 * Notice banner detailing trace thresholds and confidence breakdown.
 */
@Composable
fun TraceRuleBanner(
    traceEligibleCount: Int,
    totalCount: Int,
    autoAcceptedCount: Int,
    manuallyConfirmedCount: Int,
    pendingReviewCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("trace_rule_banner"),
        shape = RoundedCornerShape(2.dp),
        colors = CardDefaults.cardColors(containerColor = Paper),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Rule))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Trace rule",
                        tint = Ink,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Trace Threshold Rule: Confidence > 0.85 or explicit confirmation required.",
                        style = CollideType.interface13,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink
                    )
                }

                Text(
                    text = "$traceEligibleCount of $totalCount in Trace",
                    style = CollideType.measured11,
                    color = Ink2
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "• $autoAcceptedCount auto-accepted (≥85% conf)",
                    style = CollideType.measured11,
                    color = Steady
                )
                Text(
                    text = "• $manuallyConfirmedCount manually confirmed",
                    style = CollideType.measured11,
                    color = Ink
                )
                if (pendingReviewCount > 0) {
                    Text(
                        text = "• $pendingReviewCount withheld (<85% conf)",
                        style = CollideType.measured11,
                        color = Collision,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Individual assessment row on the Review screen.
 * Displays: title, type, weight, date, confidence dot, and expandable panel.
 */
@Composable
fun AssessmentReviewRow(
    assessment: Assessment,
    courseCode: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onConfirm: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRenderPdfPage: suspend (String, Int) -> Bitmap?,
    modifier: Modifier = Modifier
) {
    // Confidence color calculation
    val confPercent = (assessment.confidence * 100).toInt()
    val confColor = when {
        assessment.confidence < 0.70 -> Collision
        assessment.confidence < 0.85 -> Busy
        else -> Steady
    }

    val isTraceEligible = assessment.isConfirmed || assessment.confidence >= 0.85

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("assessment_row_${assessment.id}"),
        shape = RoundedCornerShape(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) Plate else Paper
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(if (isExpanded) Ink else Rule)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Main Summary Row (Clickable to expand)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Block: Confidence Dot + Title + Badges
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Confidence Dot & Percentage
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(confColor.copy(alpha = 0.12f), RoundedCornerShape(2.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                            .testTag("confidence_badge_${assessment.id}")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(confColor, CircleShape)
                        )
                        Text(
                            text = "$confPercent%",
                            style = CollideType.measured11,
                            color = confColor,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Course Code badge
                    Text(
                        text = courseCode,
                        style = CollideType.measured11,
                        color = Ink,
                        modifier = Modifier
                            .background(Plate, RoundedCornerShape(2.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )

                    // Type badge
                    Text(
                        text = assessment.type.displayName.uppercase(),
                        style = CollideType.measured11,
                        color = Ink2,
                        modifier = Modifier
                            .background(Plate, RoundedCornerShape(2.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    )

                    // Assessment Title
                    Text(
                        text = assessment.title,
                        style = CollideType.interface15,
                        color = Ink,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Right Block: Weight + Date + Confirmation Badge + Expand Chevron
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Honest Date Basis Display (stated, derived-from-week, or inferred/unknown needs-a-date)
                    DateBasisDisplay(
                        assessment = assessment,
                        onNeedsDateClick = onEdit
                    )

                    // Weight in Mono
                    Text(
                        text = "${assessment.weightPercent.toInt()}%",
                        style = CollideType.measured15,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )

                    // Status Badge
                    if (assessment.isConfirmed) {
                        Text(
                            text = "CONFIRMED",
                            style = CollideType.measured11,
                            color = Steady,
                            modifier = Modifier
                                .border(1.dp, Steady.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                                .background(Steady.copy(alpha = 0.08f), RoundedCornerShape(2.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    } else if (assessment.confidence >= 0.85) {
                        Text(
                            text = "AUTO-TRACE",
                            style = CollideType.measured11,
                            color = Ink2,
                            modifier = Modifier
                                .border(1.dp, Rule, RoundedCornerShape(2.dp))
                                .background(Plate, RoundedCornerShape(2.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    } else {
                        Text(
                            text = "NEEDS REVIEW",
                            style = CollideType.measured11,
                            color = Collision,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .border(1.dp, Collision.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                                .background(Collision.copy(alpha = 0.12f), RoundedCornerShape(2.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Chevron
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = Ink2,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Expanded Citation & PDF Page Preview Panel
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                ExpandedAssessmentCitationView(
                    assessment = assessment,
                    courseCode = courseCode,
                    onConfirm = onConfirm,
                    onEdit = onEdit,
                    onDelete = onDelete,
                    onRenderPdfPage = onRenderPdfPage
                )
            }
        }
    }
}

/**
 * Expanded panel showing:
 * 1. Verbatim source quote in Newsreader prose font
 * 2. Page number in Martian Mono font
 * 3. Rendered PDF page next to it using native PdfRenderer or fallback visual page
 * 4. Three actions: Confirm, Edit, Delete
 */
@Composable
fun ExpandedAssessmentCitationView(
    assessment: Assessment,
    courseCode: String,
    onConfirm: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRenderPdfPage: suspend (String, Int) -> Bitmap?,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(Paper)
            .drawBehindRuleTop()
            .padding(18.dp)
    ) {
        val isWide = maxWidth >= 620.dp

        if (isWide) {
            // Side-by-side: Citation on Left, PDF Page Rendered on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Left Column: Verbatim Quote & 3 Actions
                Column(
                    modifier = Modifier.weight(1.1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CitationDetailsBlock(assessment = assessment, courseCode = courseCode)

                    // 3 Actions per row: Confirm, Edit, Delete
                    RowActionButtons(
                        assessment = assessment,
                        onConfirm = onConfirm,
                        onEdit = onEdit,
                        onDelete = onDelete
                    )
                }

                // Right Column: Rendered PDF Page
                Box(
                    modifier = Modifier
                        .weight(0.9f)
                        .height(340.dp)
                ) {
                    PdfPagePreviewCard(
                        sourceFileName = assessment.source.fileName,
                        pageNumber = assessment.source.page,
                        quote = assessment.source.quote,
                        onRenderPdfPage = onRenderPdfPage
                    )
                }
            }
        } else {
            // Stacked for compact widths
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CitationDetailsBlock(assessment = assessment, courseCode = courseCode)

                // Rendered PDF preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    PdfPagePreviewCard(
                        sourceFileName = assessment.source.fileName,
                        pageNumber = assessment.source.page,
                        quote = assessment.source.quote,
                        onRenderPdfPage = onRenderPdfPage
                    )
                }

                // 3 Actions per row: Confirm, Edit, Delete
                RowActionButtons(
                    assessment = assessment,
                    onConfirm = onConfirm,
                    onEdit = onEdit,
                    onDelete = onDelete
                )
            }
        }
    }
}

/**
 * Renders the verbatim quote in Newsreader prose font, and page number in mono.
 */
@Composable
fun CitationDetailsBlock(
    assessment: Assessment,
    courseCode: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Source Header: File Name + Page Number in Mono
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = "Source",
                    tint = Ink,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = assessment.source.fileName,
                    style = CollideType.measured11,
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Page Number in Mono
            Text(
                text = "PAGE ${"%02d".format(assessment.source.page)}",
                style = CollideType.measured13,
                color = Ink,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(Plate, RoundedCornerShape(2.dp))
                    .border(1.dp, Rule, RoundedCornerShape(2.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
                    .testTag("source_page_${assessment.id}")
            )
        }

        // Verbatim quote in Newsreader prose font
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Plate, RoundedCornerShape(2.dp))
                .border(1.dp, Rule, RoundedCornerShape(2.dp))
                .padding(14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FormatQuote,
                    contentDescription = null,
                    tint = Ink2,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "VERBATIM SYLLABUS QUOTE",
                    style = CollideType.measured11,
                    color = Ink2
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Strictly Newsreader typography for prose/quotes
            Text(
                text = "“${assessment.source.quote}”",
                style = CollideType.prose15,
                color = Ink,
                modifier = Modifier.testTag("source_quote_${assessment.id}")
            )
        }

        // Additional provenance basis
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val dateBasisLabel = when (assessment.dateBasis) {
                DateBasis.STATED -> "Date Basis: Stated outright in text"
                DateBasis.DERIVED_FROM_WEEK -> "Date Basis: Derived from teaching week"
                DateBasis.UNKNOWN -> "Date Basis: Undated in document"
                DateBasis.INFERRED -> "Date Basis: Inferred from context"
            }
            Text(
                text = dateBasisLabel,
                style = CollideType.interface11,
                color = Ink2
            )

            Text(
                text = "Estimated: ${assessment.estimatedHours.toInt()}h (${assessment.hoursBasis.key})",
                style = CollideType.measured11,
                color = Ink2
            )
        }
    }
}

/**
 * Three actions per row: confirm, edit, delete.
 */
@Composable
fun RowActionButtons(
    assessment: Assessment,
    onConfirm: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Confirm Action
        Button(
            onClick = onConfirm,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (assessment.isConfirmed) Steady else Ink,
                contentColor = Paper
            ),
            shape = RoundedCornerShape(2.dp),
            modifier = Modifier
                .weight(1f)
                .testTag("action_confirm_${assessment.id}")
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (assessment.isConfirmed) "Confirmed ✓" else "Confirm Item",
                style = CollideType.interface13
            )
        }

        // 2. Edit Action
        OutlinedButton(
            onClick = onEdit,
            shape = RoundedCornerShape(2.dp),
            modifier = Modifier.testTag("action_edit_${assessment.id}")
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = Ink,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Edit",
                style = CollideType.interface13,
                color = Ink
            )
        }

        // 3. Delete Action
        OutlinedButton(
            onClick = onDelete,
            shape = RoundedCornerShape(2.dp),
            modifier = Modifier.testTag("action_delete_${assessment.id}")
        ) {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "Delete item",
                tint = Collision,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * Renders the PDF page preview bitmap or high-fidelity visual page sheet.
 */
@Composable
fun PdfPagePreviewCard(
    sourceFileName: String,
    pageNumber: Int,
    quote: String,
    onRenderPdfPage: suspend (String, Int) -> Bitmap?,
    modifier: Modifier = Modifier
) {
    var pdfBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(sourceFileName, pageNumber) {
        isLoading = true
        pdfBitmap = onRenderPdfPage(sourceFileName, pageNumber)
        isLoading = false
    }

    Card(
        modifier = modifier
            .fillMaxSize()
            .testTag("pdf_preview_card"),
        shape = RoundedCornerShape(2.dp),
        colors = CardDefaults.cardColors(containerColor = Plate),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Rule))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // PDF Preview Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Paper)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PDF RENDER · PAGE $pageNumber",
                    style = CollideType.measured11,
                    color = Ink2
                )
                Text(
                    text = "100% SCALE",
                    style = CollideType.measured11,
                    color = Ink3
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Rule)
            )

            // PDF Content Body
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (pdfBitmap != null) {
                    // Actual rendered PDF page from android.graphics.pdf.PdfRenderer
                    Image(
                        bitmap = pdfBitmap!!.asImageBitmap(),
                        contentDescription = "Rendered PDF page $pageNumber",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(2.dp))
                            .border(1.dp, Rule, RoundedCornerShape(2.dp))
                    )
                } else {
                    // Fallback high-fidelity document sheet
                    FallbackDocumentSheet(
                        sourceFileName = sourceFileName,
                        pageNumber = pageNumber,
                        highlightedQuote = quote
                    )
                }
            }
        }
    }
}

/**
 * Formatted fallback document sheet with paragraph lines and highlighted excerpt.
 */
@Composable
fun FallbackDocumentSheet(
    sourceFileName: String,
    pageNumber: Int,
    highlightedQuote: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .border(1.dp, Rule, RoundedCornerShape(2.dp)),
        color = Paper,
        shape = RoundedCornerShape(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header of mock document
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = sourceFileName.replace(".pdf", ""),
                    style = CollideType.interface11,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )
                Text(
                    text = "p. $pageNumber",
                    style = CollideType.measured11,
                    color = Ink2
                )
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Rule))

            // Simulated document lines
            repeat(2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(8.dp)
                        .background(Plate, RoundedCornerShape(1.dp))
                )
            }

            // Highlighted quote region on the page
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Busy.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
                    .border(1.dp, Busy.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = highlightedQuote,
                    style = CollideType.prose13,
                    color = Ink,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // More simulated document lines
            repeat(4) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .height(8.dp)
                        .background(Plate, RoundedCornerShape(1.dp))
                )
            }
        }
    }
}

/**
 * Dialog for editing assessment parameters (Title, Type, Weight, Due Date, Due Time, Estimated Hours).
 */
@Composable
fun EditAssessmentDialog(
    assessment: Assessment,
    onDismiss: () -> Unit,
    onSave: (Assessment) -> Unit
) {
    var title by remember { mutableStateOf(assessment.title) }
    var type by remember { mutableStateOf(assessment.type) }
    var weightInput by remember { mutableStateOf(assessment.weightPercent.toString()) }
    var dueDateInput by remember { mutableStateOf(assessment.dueDate ?: "") }
    var dueTimeInput by remember { mutableStateOf(assessment.dueTime ?: "") }
    var hoursInput by remember { mutableStateOf(assessment.estimatedHours.toString()) }
    var showTypeMenu by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .border(1.dp, Rule, RoundedCornerShape(2.dp)),
            color = Paper,
            shape = RoundedCornerShape(2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EDIT ASSESSMENT",
                        style = CollideType.measured13,
                        color = Ink
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Ink2
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Rule))

                // Title
                Text(text = "TITLE", style = CollideType.measured11, color = Ink2)
                AchromaticTextInput(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = "Assessment title",
                    modifier = Modifier.testTag("edit_title_input")
                )

                // Assessment Type Selector
                Text(text = "ASSESSMENT TYPE", style = CollideType.measured11, color = Ink2)
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Rule, RoundedCornerShape(2.dp))
                            .background(Plate)
                            .clickable { showTypeMenu = true }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = type.displayName.uppercase(),
                            style = CollideType.interface13,
                            color = Ink
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = Ink2
                        )
                    }

                    DropdownMenu(
                        expanded = showTypeMenu,
                        onDismissRequest = { showTypeMenu = false }
                    ) {
                        AssessmentType.values().forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.displayName.uppercase(), style = CollideType.interface13) },
                                onClick = {
                                    type = t
                                    showTypeMenu = false
                                }
                            )
                        }
                    }
                }

                // Weight % and Estimated Hours
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "WEIGHT %", style = CollideType.measured11, color = Ink2)
                        Spacer(modifier = Modifier.height(4.dp))
                        AchromaticTextInput(
                            value = weightInput,
                            onValueChange = { weightInput = it },
                            placeholder = "e.g. 25.0",
                            modifier = Modifier.testTag("edit_weight_input")
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "LOAD (HOURS)", style = CollideType.measured11, color = Ink2)
                        Spacer(modifier = Modifier.height(4.dp))
                        AchromaticTextInput(
                            value = hoursInput,
                            onValueChange = { hoursInput = it },
                            placeholder = "e.g. 12.0",
                            modifier = Modifier.testTag("edit_hours_input")
                        )
                    }
                }

                // Due Date & Due Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text(text = "DUE DATE (YYYY-MM-DD)", style = CollideType.measured11, color = Ink2)
                        Spacer(modifier = Modifier.height(4.dp))
                        AchromaticTextInput(
                            value = dueDateInput,
                            onValueChange = { dueDateInput = it },
                            placeholder = "e.g. 2026-10-12",
                            modifier = Modifier.testTag("edit_duedate_input")
                        )
                    }

                    Column(modifier = Modifier.weight(0.8f)) {
                        Text(text = "TIME (HH:MM)", style = CollideType.measured11, color = Ink2)
                        Spacer(modifier = Modifier.height(4.dp))
                        AchromaticTextInput(
                            value = dueTimeInput,
                            onValueChange = { dueTimeInput = it },
                            placeholder = "e.g. 23:59",
                            modifier = Modifier.testTag("edit_duetime_input")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Actions: Cancel & Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Text("Cancel", style = CollideType.interface13, color = Ink)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val parsedWeight = weightInput.toDoubleOrNull() ?: assessment.weightPercent
                            val parsedHours = hoursInput.toDoubleOrNull() ?: assessment.estimatedHours
                            val cleanDueDate = dueDateInput.trim().ifEmpty { null }
                            val cleanDueTime = dueTimeInput.trim().ifEmpty { null }

                            val updated = assessment.copy(
                                title = title.trim().ifEmpty { assessment.title },
                                type = type,
                                weightPercent = parsedWeight,
                                dueDate = cleanDueDate,
                                dueTime = cleanDueTime,
                                estimatedHours = parsedHours,
                                hoursBasis = HoursBasis.USER,
                                isConfirmed = true // Editing auto-confirms the item
                            )
                            onSave(updated)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper),
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.testTag("save_edit_assessment_button")
                    ) {
                        Text("Save & Confirm", style = CollideType.interface13)
                    }
                }
            }
        }
    }
}

/**
 * Delete confirmation dialog.
 */
@Composable
fun DeleteAssessmentDialog(
    assessment: Assessment,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .border(1.dp, Rule, RoundedCornerShape(2.dp)),
            color = Paper,
            shape = RoundedCornerShape(2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "DELETE ASSESSMENT",
                    style = CollideType.measured13,
                    color = Collision
                )

                Text(
                    text = "Are you sure you want to remove “${assessment.title}” (${assessment.weightPercent.toInt()}%) from your semester?",
                    style = CollideType.interface13,
                    color = Ink
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Text("Cancel", style = CollideType.interface13, color = Ink)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onConfirmDelete,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Collision,
                            contentColor = Paper
                        ),
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.testTag("confirm_delete_button")
                    ) {
                        Text("Delete", style = CollideType.interface13)
                    }
                }
            }
        }
    }
}

private fun Modifier.drawBehindRuleTop(): Modifier = this.then(
    Modifier.background(Paper)
)
