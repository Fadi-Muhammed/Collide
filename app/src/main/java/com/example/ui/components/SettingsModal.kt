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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.model.AssessmentType
import com.example.model.AssessmentTypeConfig
import com.example.model.DEFAULT_TYPE_CONFIGS
import com.example.ui.theme.CollideType
import com.example.ui.theme.Ink
import com.example.ui.theme.Ink2
import com.example.ui.theme.Ink3
import com.example.ui.theme.Paper
import com.example.ui.theme.Plate
import com.example.ui.theme.Rule

/**
 * Settings Modal: "How long things take you"
 * "Every number here is a starting guess and every one is editable per assessment.
 * Expose them in settings as 'How long things take you,' and let the student tune them once
 * in first year and never again."
 */
@Composable
fun SettingsModal(
    currentConfigs: Map<AssessmentType, AssessmentTypeConfig>,
    onSaveConfigs: (Map<AssessmentType, AssessmentTypeConfig>) -> Unit,
    onResetDefaults: () -> Unit,
    onDismiss: () -> Unit
) {
    var workingConfigs by remember(currentConfigs) {
        mutableStateOf(currentConfigs.toMutableMap())
    }

    val orderedTypes = listOf(
        AssessmentType.READING_RESPONSE,
        AssessmentType.QUIZ,
        AssessmentType.PROBLEM_SET,
        AssessmentType.LAB_REPORT,
        AssessmentType.PRESENTATION,
        AssessmentType.ESSAY,
        AssessmentType.MIDTERM,
        AssessmentType.PROJECT_MILESTONE,
        AssessmentType.FINAL_EXAM,
        AssessmentType.PARTICIPATION,
        AssessmentType.OTHER
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .border(1.dp, Rule, RoundedCornerShape(2.dp))
                .testTag("settings_modal_surface"),
            shape = RoundedCornerShape(2.dp),
            color = Paper
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "HOW LONG THINGS TAKE YOU",
                            style = CollideType.interface18,
                            fontWeight = FontWeight.Bold,
                            color = Ink
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Deterministic Load Model Settings · Step 1 & 2 Parameters",
                            style = CollideType.measured11,
                            color = Ink2
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Ink)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Every number here is a baseline guess. The load model calculates:\n" +
                            "estimatedHours = baseHours × clamp(weight/10, 0.5, 3.0) × (credits/3)\n" +
                            "Then spreads work backwards across the window ending on the due date.",
                    style = CollideType.prose13,
                    color = Ink2
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Type configuration list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(orderedTypes, key = { it.name }) { type ->
                        val config = workingConfigs[type] ?: DEFAULT_TYPE_CONFIGS[type] ?: AssessmentTypeConfig(type, 6.0, 7)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Rule, RoundedCornerShape(2.dp))
                                .background(Plate)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Type Name
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = type.displayName,
                                    style = CollideType.measured13,
                                    fontWeight = FontWeight.Bold,
                                    color = Ink
                                )
                                Text(
                                    text = "${config.baseHours.toInt()}h base · ${config.windowDays}d window",
                                    style = CollideType.interface11,
                                    color = Ink2
                                )
                            }

                            // Base Hours Stepper
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Base Hours", style = CollideType.measured11, color = Ink2)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                val newHours = (config.baseHours - 1.0).coerceAtLeast(1.0)
                                                workingConfigs = workingConfigs.toMutableMap().apply {
                                                    put(type, config.copy(baseHours = newHours))
                                                }
                                            },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(Paper, RoundedCornerShape(2.dp))
                                                .border(1.dp, Rule, RoundedCornerShape(2.dp))
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Decrease hours", tint = Ink, modifier = Modifier.size(14.dp))
                                        }

                                        Text(
                                            text = "${config.baseHours.toInt()}h",
                                            style = CollideType.measured13,
                                            fontWeight = FontWeight.Bold,
                                            color = Ink,
                                            modifier = Modifier.width(28.dp)
                                        )

                                        IconButton(
                                            onClick = {
                                                val newHours = (config.baseHours + 1.0).coerceAtMost(60.0)
                                                workingConfigs = workingConfigs.toMutableMap().apply {
                                                    put(type, config.copy(baseHours = newHours))
                                                }
                                            },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(Paper, RoundedCornerShape(2.dp))
                                                .border(1.dp, Rule, RoundedCornerShape(2.dp))
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Increase hours", tint = Ink, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Window Days Stepper
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Window", style = CollideType.measured11, color = Ink2)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                val newDays = (config.windowDays - 1).coerceAtLeast(1)
                                                workingConfigs = workingConfigs.toMutableMap().apply {
                                                    put(type, config.copy(windowDays = newDays))
                                                }
                                            },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(Paper, RoundedCornerShape(2.dp))
                                                .border(1.dp, Rule, RoundedCornerShape(2.dp))
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Decrease days", tint = Ink, modifier = Modifier.size(14.dp))
                                        }

                                        Text(
                                            text = "${config.windowDays}d",
                                            style = CollideType.measured13,
                                            fontWeight = FontWeight.Bold,
                                            color = Ink,
                                            modifier = Modifier.width(28.dp)
                                        )

                                        IconButton(
                                            onClick = {
                                                val newDays = (config.windowDays + 1).coerceAtMost(30)
                                                workingConfigs = workingConfigs.toMutableMap().apply {
                                                    put(type, config.copy(windowDays = newDays))
                                                }
                                            },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(Paper, RoundedCornerShape(2.dp))
                                                .border(1.dp, Rule, RoundedCornerShape(2.dp))
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Increase days", tint = Ink, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            onResetDefaults()
                            workingConfigs = DEFAULT_TYPE_CONFIGS.toMutableMap()
                        },
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Plate)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = Ink2)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset to Defaults", style = CollideType.interface13, color = Ink2)
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(2.dp)
                        ) {
                            Text("Cancel", style = CollideType.interface13, color = Ink)
                        }

                        Button(
                            onClick = {
                                onSaveConfigs(workingConfigs)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(2.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Ink),
                            modifier = Modifier.testTag("save_load_settings_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp), tint = Paper)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Estimates", style = CollideType.interface13, color = Paper)
                        }
                    }
                }
            }
        }
    }
}
