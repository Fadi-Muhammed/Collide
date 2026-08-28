package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.SemesterEntity
import com.example.model.Assessment
import com.example.model.Course
import com.example.ui.theme.CollideType
import com.example.ui.theme.Collision
import com.example.ui.theme.Ink
import com.example.ui.theme.Ink2
import com.example.ui.theme.Ink3
import com.example.ui.theme.Paper
import com.example.ui.theme.Plate
import com.example.ui.theme.Rule
import com.example.ui.theme.Steady

/**
 * Course View (Part 7).
 *
 * Requirements:
 * - Grade breakdown with sum shown, prominently flagged if it does NOT sum to 100%.
 * - Policies (late work, attendance, resubmissions) in Newsreader serif prose as quoted text with provenance.
 * - Complete list of every assessment for that course with honest dateBasis.
 * - Ability to inspect or edit any assessment.
 */
@Composable
fun CourseView(
    course: Course,
    assessments: List<Assessment>,
    onBack: () -> Unit,
    onEditAssessment: (Assessment) -> Unit,
    modifier: Modifier = Modifier
) {
    val courseAssessments = remember(assessments, course.id) {
        assessments.filter { it.courseId == course.id }
            .sortedBy { it.dueDate ?: "9999-99-99" }
    }

    val gradeSum = remember(course.gradeBreakdown) {
        course.gradeBreakdown.sumOf { it.weightPercent }
    }
    val isGradeSum100 = Math.abs(gradeSum - 100.0) < 0.01

    var selectedAssessmentForDetail by remember { mutableStateOf<Assessment?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("course_detail_view")
    ) {
        // Top Back Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(32.dp).testTag("course_detail_back_button")
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back to courses", tint = Ink)
                }

                Text(
                    text = course.code,
                    style = CollideType.title24,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )

                if (course.credits != null) {
                    Text(
                        text = "${course.credits} CREDITS",
                        style = CollideType.measured11,
                        color = Ink2,
                        modifier = Modifier
                            .background(Plate, RoundedCornerShape(2.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = course.instructor ?: "Instructor Unlisted",
                style = CollideType.interface15,
                color = Ink2
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = course.title,
            style = CollideType.interface18,
            color = Ink2
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Main Layout: Left Column (Grade Breakdown & Policies), Right Column (Assessments)
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Column: Breakdown + Policies (Scrollable)
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Grade Breakdown Card
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, if (!isGradeSum100) Collision else Rule, RoundedCornerShape(2.dp)),
                        color = Paper
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "GRADE BREAKDOWN",
                                    style = CollideType.measured11,
                                    color = Ink2,
                                    fontWeight = FontWeight.Bold
                                )

                                // Sum Flag
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "SUM: ${gradeSum.toInt()}%",
                                        style = CollideType.measured13,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isGradeSum100) Steady else Collision
                                    )
                                    if (!isGradeSum100) {
                                        Text(
                                            text = "(NOT 100%)",
                                            style = CollideType.measured11,
                                            fontWeight = FontWeight.Bold,
                                            color = Collision
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (course.gradeBreakdown.isEmpty()) {
                                Text(
                                    text = "No explicit percentage breakdown extracted.",
                                    style = CollideType.interface13,
                                    color = Ink3
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    course.gradeBreakdown.forEach { item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Plate, RoundedCornerShape(2.dp))
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = item.label,
                                                style = CollideType.interface13,
                                                color = Ink
                                            )
                                            Text(
                                                text = "${item.weightPercent.toInt()}%",
                                                style = CollideType.measured13,
                                                fontWeight = FontWeight.Bold,
                                                color = Ink
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Course Policies in Newsreader Serif (Quoted Text)
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Rule, RoundedCornerShape(2.dp)),
                        color = Paper
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "COURSE POLICIES (NEWSREADER)",
                                style = CollideType.measured11,
                                color = Ink2,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // Late Policy
                            if (!course.policies.late.isNullOrBlank()) {
                                PolicySection(title = "Late Work Policy", quote = course.policies.late)
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            // Attendance Policy
                            if (!course.policies.attendance.isNullOrBlank()) {
                                PolicySection(title = "Attendance & Participation", quote = course.policies.attendance)
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            // Resubmission Policy
                            if (!course.policies.resubmission.isNullOrBlank()) {
                                PolicySection(title = "Resubmissions & Regrades", quote = course.policies.resubmission)
                            }

                            if (course.policies.late.isNullOrBlank() && course.policies.attendance.isNullOrBlank() && course.policies.resubmission.isNullOrBlank()) {
                                Text(
                                    text = "No specific policy quotes extracted from syllabus.",
                                    style = CollideType.interface13,
                                    color = Ink3
                                )
                            }
                        }
                    }
                }
            }

            // Right Column: Course Assessment List
            Surface(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .border(1.dp, Rule, RoundedCornerShape(2.dp)),
                color = Paper
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ASSESSMENTS (${courseAssessments.size})",
                        style = CollideType.measured11,
                        color = Ink2,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (courseAssessments.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No assessments registered for this course.",
                                style = CollideType.interface13,
                                color = Ink3
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(courseAssessments, key = { it.id }) { a ->
                                CourseAssessmentRow(
                                    assessment = a,
                                    onEditClick = { selectedAssessmentForDetail = a }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal when user clicks an assessment
    if (selectedAssessmentForDetail != null) {
        AssessmentDetailModal(
            assessment = selectedAssessmentForDetail!!,
            course = course,
            onSaveAssessment = { updated ->
                onEditAssessment(updated)
                selectedAssessmentForDetail = null
            },
            onDismiss = { selectedAssessmentForDetail = null }
        )
    }
}

@Composable
private fun PolicySection(title: String, quote: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Plate.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
            .border(1.dp, Rule, RoundedCornerShape(2.dp))
            .padding(10.dp)
    ) {
        Text(
            text = title.uppercase(),
            style = CollideType.measured11,
            color = Ink2
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "\"$quote\"",
            style = CollideType.prose13,
            color = Ink
        )
    }
}

@Composable
private fun CourseAssessmentRow(
    assessment: Assessment,
    onEditClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Rule, RoundedCornerShape(2.dp))
            .background(Plate)
            .clickable { onEditClick() }
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = assessment.title,
                style = CollideType.interface13,
                fontWeight = FontWeight.SemiBold,
                color = Ink
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DateBasisDisplay(assessment = assessment, onNeedsDateClick = onEditClick)
                Text(
                    text = "· ${assessment.type.displayName} · ${assessment.estimatedHours.toInt()}h load",
                    style = CollideType.measured11,
                    color = Ink2
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${assessment.weightPercent.toInt()}%",
                style = CollideType.measured13,
                fontWeight = FontWeight.Bold,
                color = Ink
            )
            IconButton(
                onClick = onEditClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit assessment", tint = Ink2)
            }
        }
    }
}
