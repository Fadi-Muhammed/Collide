package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.ExtractionProgress
import com.example.model.Assessment
import com.example.model.AssessmentType
import com.example.model.Course
import com.example.model.DateBasis
import com.example.model.SourceRef
import com.example.ui.theme.Busy
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

/**
 * Live extraction progress bar shown during serial Gemini calls.
 */
@Composable
fun ExtractionProgressCard(
    progress: ExtractionProgress,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("extraction_progress_card"),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = Paper),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Rule))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Extracting",
                        tint = Ink,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Extracting file ${progress.currentFileIndex} of ${progress.totalFiles}",
                        style = CollideType.interface15,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )
                }

                Text(
                    text = "${((progress.currentFileIndex.toFloat() / progress.totalFiles.coerceAtLeast(1)) * 100).toInt()}%",
                    style = CollideType.measured13,
                    color = Ink
                )
            }

            LinearProgressIndicator(
                progress = {
                    if (progress.totalFiles > 0) progress.currentFileIndex.toFloat() / progress.totalFiles else 0f
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Ink,
                trackColor = Plate
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (progress.currentFileName.isNotBlank()) progress.currentFileName else "Preparing extraction...",
                    style = CollideType.measured11,
                    color = Ink,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = progress.statusText,
                    style = CollideType.measured11,
                    color = Ink2
                )
            }
        }
    }
}

/**
 * Displays an extracted course with its assessments, weight check banner, and provenance inspector.
 */
@Composable
fun ExtractedCourseCard(
    course: Course,
    assessments: List<Assessment>,
    onInspectSource: (SourceRef, String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Calculate total weights for this course
    val totalWeight = assessments.sumOf { it.weightPercent }
    val roundedTotal = Math.round(totalWeight * 10.0) / 10.0
    val hasDiscrepancy = Math.abs(roundedTotal - 100.0) > 0.5 && assessments.isNotEmpty()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("course_card_${course.code}"),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = Paper),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Rule))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Code, Title, Credits, Instructor
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = course.code,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Ink
                        )
                        if (course.credits != null) {
                            Text(
                                text = "${course.credits.toInt()} CR",
                                color = Ink2,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .background(Plate, RoundedCornerShape(2.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = course.title,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = Ink
                    )

                    if (!course.instructor.isNullOrBlank()) {
                        Text(
                            text = "Instructor: ${course.instructor}",
                            fontSize = 13.sp,
                            color = Ink2,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Confidence badge
                val confPercent = (course.confidence * 100).toInt()
                val isLowConf = course.confidence < 0.70
                Row(
                    modifier = Modifier
                        .background(
                            if (isLowConf) Collision.copy(alpha = 0.12f) else Plate,
                            RoundedCornerShape(2.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isLowConf) Icons.Default.WarningAmber else Icons.Default.CheckCircle,
                        contentDescription = "Confidence",
                        tint = if (isLowConf) Collision else Steady,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isLowConf) "Review (${confPercent}%)" else "${confPercent}% conf",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (isLowConf) Collision else Ink
                    )
                }
            }

            // Real-World Discrepancy Banner (Design Law: Never silently fix. Show banner with link to source)
            if (hasDiscrepancy) {
                val missingOrExcess = if (roundedTotal < 100.0) {
                    val missing = Math.round((100.0 - roundedTotal) * 10.0) / 10.0
                    "Weights total ${roundedTotal.toInt()}%. ${missing.toInt()}% unaccounted for in syllabus."
                } else {
                    val excess = Math.round((roundedTotal - 100.0) * 10.0) / 10.0
                    "Weights total ${roundedTotal.toInt()}%. ${excess.toInt()}% over 100%."
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Busy.copy(alpha = 0.12f), RoundedCornerShape(2.dp))
                        .border(1.dp, Busy.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                        .padding(12.dp)
                        .clickable {
                            onInspectSource(course.source, "Course Grade Breakdown (${course.code})")
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = "Discrepancy",
                            tint = Busy,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = missingOrExcess,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Ink
                            )
                            Text(
                                text = "Source: ${course.source.fileName}, page ${course.source.page}. Tap to verify verbatim syllabus text.",
                                fontSize = 11.sp,
                                color = Ink2
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "View quote",
                        tint = Ink2,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Course Policies snippet
            if (course.policies.late != null || course.policies.attendance != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Plate, RoundedCornerShape(2.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (course.policies.late != null) {
                        Text(
                            text = "Late Policy: ${course.policies.late}",
                            fontSize = 12.sp,
                            color = Ink2
                        )
                    }
                    if (course.policies.attendance != null) {
                        Text(
                            text = "Attendance: ${course.policies.attendance}",
                            fontSize = 12.sp,
                            color = Ink2
                        )
                    }
                }
            }

            // Extracted Assessments List
            Text(
                text = "GRADED ASSESSMENTS (${assessments.size})",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink3,
                letterSpacing = 1.sp
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                assessments.forEach { assessment ->
                    AssessmentRowItem(
                        assessment = assessment,
                        onInspect = {
                            onInspectSource(assessment.source, assessment.title)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Single assessment row with type badges, date basis, workload hours, and provenance click.
 */
@Composable
fun AssessmentRowItem(
    assessment: Assessment,
    onInspect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLowConfidence = assessment.confidence < 0.70

    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Rule, RoundedCornerShape(2.dp))
            .background(Paper)
            .clickable { onInspect() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type badge
                Text(
                    text = assessment.type.displayName.uppercase(),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Ink2,
                    modifier = Modifier
                        .background(Plate, RoundedCornerShape(2.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                )

                // Date basis badge
                val dateBasisText = when (assessment.dateBasis) {
                    DateBasis.STATED -> "STATED"
                    DateBasis.DERIVED_FROM_WEEK -> "WEEK-CALC"
                    DateBasis.UNKNOWN -> "UNDATED"
                    DateBasis.INFERRED -> "INFERRED"
                }
                val dateBasisColor = when (assessment.dateBasis) {
                    DateBasis.STATED -> Steady
                    DateBasis.DERIVED_FROM_WEEK -> Ink2
                    DateBasis.UNKNOWN -> Busy
                    DateBasis.INFERRED -> Collision
                }

                Text(
                    text = dateBasisText,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = dateBasisColor,
                    modifier = Modifier
                        .border(1.dp, dateBasisColor.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )

                if (isLowConfidence) {
                    Text(
                        text = "LOW CONF",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Collision,
                        modifier = Modifier
                            .background(Collision.copy(alpha = 0.12f), RoundedCornerShape(2.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = assessment.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Ink
            )

            Row(
                modifier = Modifier.padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (assessment.dueDate != null) {
                        "Due ${assessment.dueDate}${if (assessment.dueTime != null) " at ${assessment.dueTime}" else ""}"
                    } else {
                        "No due date stated"
                    },
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (assessment.dueDate != null) Ink2 else Busy
                )

                Text(
                    text = "• ${assessment.estimatedHours.toInt()}h est",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Ink3
                )
            }
        }

        // Weight percent + citation icon
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${assessment.weightPercent.toInt()}%",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Ink
            )

            IconButton(
                onClick = onInspect,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FormatQuote,
                    contentDescription = "View quote",
                    tint = Ink2,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Provenance / Citation inspector dialog showing verbatim quotes, page numbers, and files.
 */
@Composable
fun SourceProvenanceModal(
    source: SourceRef,
    title: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = Paper,
            border = androidx.compose.foundation.BorderStroke(1.dp, Rule),
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = "Source",
                            tint = Ink,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Syllabus Provenance",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Ink
                        )
                    }

                    Text(
                        text = "Page ${source.page}",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Ink,
                        modifier = Modifier
                            .background(Plate, RoundedCornerShape(2.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink
                )

                // Verbatim Quote block
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Plate, RoundedCornerShape(2.dp))
                        .border(1.dp, Rule, RoundedCornerShape(2.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "VERBATIM EXTRACT",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Ink3,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "\"${source.quote}\"",
                        fontSize = 13.sp,
                        color = Ink,
                        lineHeight = 19.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Source: ${source.fileName}",
                        fontSize = 11.sp,
                        color = Ink2,
                        fontFamily = FontFamily.Monospace
                    )

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Ink,
                            contentColor = Paper
                        )
                    ) {
                        Text("Done", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
