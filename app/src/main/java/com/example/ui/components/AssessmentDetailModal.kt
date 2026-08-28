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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.model.Assessment
import com.example.model.AssessmentStatus
import com.example.model.AssessmentType
import com.example.model.Course
import com.example.model.DateBasis
import com.example.model.HoursBasis
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
 * Assessment Detail Modal / View (Part 7).
 *
 * Requirements:
 * - Title, course, type, weight, date with its dateBasis shown honestly.
 * - Estimated hours with an inline edit control (recomputes trace immediately on save).
 * - Source quote in Newsreader prose font with page provenance.
 * - Status toggle (TODO / DOING / DONE).
 * - Editing hours or date immediately propagates to recompute the load model.
 */
@Composable
fun AssessmentDetailModal(
    assessment: Assessment,
    course: Course?,
    onSaveAssessment: (Assessment) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(assessment.title) }
    var selectedType by remember { mutableStateOf(assessment.type) }
    var weightText by remember { mutableStateOf(assessment.weightPercent.toString()) }
    var dueDateText by remember { mutableStateOf(assessment.dueDate ?: "") }
    var selectedDateBasis by remember { mutableStateOf(assessment.dateBasis) }
    var hoursText by remember { mutableStateOf(assessment.estimatedHours.toString()) }
    var selectedStatus by remember { mutableStateOf(assessment.status) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Ink, RoundedCornerShape(2.dp))
                .testTag("assessment_detail_modal"),
            color = Paper,
            shape = RoundedCornerShape(2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = course?.code ?: "COURSE",
                                style = CollideType.measured11,
                                fontWeight = FontWeight.Bold,
                                color = Ink,
                                modifier = Modifier
                                    .background(Plate, RoundedCornerShape(2.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                            Text(
                                text = "ASSESSMENT DETAIL",
                                style = CollideType.measured11,
                                color = Ink2
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Ink)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title Input
                Text(
                    text = "TITLE",
                    style = CollideType.measured11,
                    color = Ink2
                )
                Spacer(modifier = Modifier.height(4.dp))
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    textStyle = CollideType.interface15.copy(color = Ink, fontWeight = FontWeight.SemiBold),
                    cursorBrush = SolidColor(Ink),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Plate, RoundedCornerShape(2.dp))
                        .border(1.dp, Rule, RoundedCornerShape(2.dp))
                        .padding(10.dp)
                        .testTag("assessment_detail_title_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Grid of Attributes: Date & dateBasis, Estimated Hours, Weight, Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Due Date & Basis
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DUE DATE (ISO)",
                            style = CollideType.measured11,
                            color = Ink2
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        BasicTextField(
                            value = dueDateText,
                            onValueChange = {
                                dueDateText = it
                                selectedDateBasis = if (it.isNotBlank()) DateBasis.STATED else DateBasis.UNKNOWN
                            },
                            textStyle = CollideType.measured13.copy(color = Ink),
                            cursorBrush = SolidColor(Ink),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Plate, RoundedCornerShape(2.dp))
                                .border(1.dp, Rule, RoundedCornerShape(2.dp))
                                .padding(10.dp)
                                .testTag("assessment_detail_date_input")
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        DateBasisDisplay(
                            assessment = assessment.copy(
                                dueDate = dueDateText.ifBlank { null },
                                dateBasis = selectedDateBasis
                            )
                        )
                    }

                    // Estimated Hours
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ESTIMATED HOURS",
                            style = CollideType.measured11,
                            color = Ink2
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        BasicTextField(
                            value = hoursText,
                            onValueChange = { hoursText = it },
                            textStyle = CollideType.measured13.copy(color = Ink, fontWeight = FontWeight.Bold),
                            cursorBrush = SolidColor(Ink),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Plate, RoundedCornerShape(2.dp))
                                .border(1.dp, Rule, RoundedCornerShape(2.dp))
                                .padding(10.dp)
                                .testTag("assessment_detail_hours_input")
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Triggers trace recompute",
                            style = CollideType.measured11,
                            color = Ink3
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Weight %
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "WEIGHT (%)",
                            style = CollideType.measured11,
                            color = Ink2
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        BasicTextField(
                            value = weightText,
                            onValueChange = { weightText = it },
                            textStyle = CollideType.measured13.copy(color = Ink),
                            cursorBrush = SolidColor(Ink),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Plate, RoundedCornerShape(2.dp))
                                .border(1.dp, Rule, RoundedCornerShape(2.dp))
                                .padding(10.dp)
                        )
                    }

                    // Status (TODO / DOING / DONE)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "STATUS",
                            style = CollideType.measured11,
                            color = Ink2
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AssessmentStatus.values().forEach { st ->
                                val isSel = selectedStatus == st
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (isSel) Ink else Plate, RoundedCornerShape(2.dp))
                                        .border(1.dp, if (isSel) Ink else Rule, RoundedCornerShape(2.dp))
                                        .clickable { selectedStatus = st }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = st.key.uppercase(),
                                        style = CollideType.measured11,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) Paper else Ink
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Verbatim Source Quote in Newsreader Prose
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Plate.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                        .border(1.dp, Rule, RoundedCornerShape(2.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.FormatQuote,
                            contentDescription = "Source quote",
                            tint = Ink2,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "SOURCE PROVENANCE · ${assessment.source.fileName} (p. ${assessment.source.page})",
                            style = CollideType.measured11,
                            color = Ink2
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "\"${assessment.source.quote}\"",
                        style = CollideType.prose13,
                        color = Ink
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions
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
                            val newHours = hoursText.toDoubleOrNull() ?: assessment.estimatedHours
                            val newWeight = weightText.toDoubleOrNull() ?: assessment.weightPercent
                            val updated = assessment.copy(
                                title = title,
                                type = selectedType,
                                weightPercent = newWeight,
                                dueDate = dueDateText.trim().ifBlank { null },
                                dateBasis = selectedDateBasis,
                                estimatedHours = newHours,
                                hoursBasis = if (newHours != assessment.estimatedHours) HoursBasis.USER else assessment.hoursBasis,
                                status = selectedStatus,
                                isConfirmed = true
                            )
                            onSaveAssessment(updated)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Ink),
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.testTag("assessment_detail_save_button")
                    ) {
                        Text("Save & Recompute", style = CollideType.interface13, color = Paper)
                    }
                }
            }
        }
    }
}
