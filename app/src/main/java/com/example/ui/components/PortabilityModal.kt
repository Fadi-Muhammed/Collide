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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.SemesterEntity
import com.example.ui.theme.CollideType
import com.example.ui.theme.Ink
import com.example.ui.theme.Ink2
import com.example.ui.theme.Ink3
import com.example.ui.theme.Paper
import com.example.ui.theme.Plate
import com.example.ui.theme.Rule

@Composable
fun PortabilityModal(
    semester: SemesterEntity?,
    hasApiKey: Boolean,
    notificationMessage: String?,
    onCopyJson: () -> Unit,
    onShareJson: () -> Unit,
    onCopyIcs: () -> Unit,
    onShareIcs: () -> Unit,
    onImportJson: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var importJsonText by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0: Export, 1: Import, 2: System Status

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .border(1.dp, Ink, RoundedCornerShape(2.dp))
                .testTag("portability_modal"),
            color = Paper,
            shape = RoundedCornerShape(2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PERSISTENCE & PORTABILITY",
                            style = CollideType.title24,
                            color = Ink
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (semester != null) "ACTIVE SEMESTER: ${semester.name.uppercase()}" else "NO ACTIVE SEMESTER",
                            style = CollideType.measured11,
                            color = Ink2
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_portability_modal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Ink
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Rule, RoundedCornerShape(2.dp))
                        .background(Plate)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("EXPORT", "IMPORT", "OFFLINE & API").forEachIndexed { index, label ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = index }
                                .background(
                                    if (isSelected) Ink else Color.Transparent,
                                    RoundedCornerShape(2.dp)
                                )
                                .padding(vertical = 8.dp)
                                .testTag("portability_tab_$index"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = CollideType.measured11,
                                color = if (isSelected) Paper else Ink2,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Notification Toast / Message if present
                if (!notificationMessage.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Ink, RoundedCornerShape(2.dp))
                            .background(Plate)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Ink,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = notificationMessage,
                            style = CollideType.interface13,
                            color = Ink
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Main Content Body based on tab
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            // --- EXPORT TAB ---
                            if (semester == null) {
                                Text(
                                    text = "Create or open a semester first to export its data.",
                                    style = CollideType.interface13,
                                    color = Ink2
                                )
                            } else {
                                // 1. Unified JSON Export
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Rule, RoundedCornerShape(2.dp))
                                        .background(Paper)
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "FULL SEMESTER (JSON)",
                                            style = CollideType.measured13,
                                            fontWeight = FontWeight.Bold,
                                            color = Ink
                                        )
                                        Text(
                                            text = ".JSON",
                                            style = CollideType.measured11,
                                            color = Ink2,
                                            modifier = Modifier
                                                .background(Plate, RoundedCornerShape(2.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Single complete file containing semester calendar dates, study capacity, extracted courses, grade breakdown rules, confirmed and unconfirmed assessments, citations, and full conversation history.",
                                        style = CollideType.prose15,
                                        color = Ink2
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = onCopyJson,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Ink,
                                                contentColor = Paper
                                            ),
                                            shape = RoundedCornerShape(2.dp),
                                            modifier = Modifier.testTag("copy_json_export_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Copy JSON", style = CollideType.interface13)
                                        }
                                        OutlinedButton(
                                            onClick = onShareJson,
                                            shape = RoundedCornerShape(2.dp),
                                            modifier = Modifier.testTag("share_json_export_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = Ink
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Share JSON file", style = CollideType.interface13, color = Ink)
                                        }
                                    }
                                }

                                // 2. Calendar .ics Export
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Rule, RoundedCornerShape(2.dp))
                                        .background(Paper)
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "CONFIRMED DEADLINES (iCALENDAR)",
                                            style = CollideType.measured13,
                                            fontWeight = FontWeight.Bold,
                                            color = Ink
                                        )
                                        Text(
                                            text = ".ICS",
                                            style = CollideType.measured11,
                                            color = Ink2,
                                            modifier = Modifier
                                                .background(Plate, RoundedCornerShape(2.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Standard RFC 5545 iCalendar file of all confirmed deadlines for Apple Calendar, Google Calendar, Outlook, or Thunderbird. Each event includes the course code, weight, estimated load, and syllabus source quote in its description.",
                                        style = CollideType.prose15,
                                        color = Ink2
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = onCopyIcs,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Ink,
                                                contentColor = Paper
                                            ),
                                            shape = RoundedCornerShape(2.dp),
                                            modifier = Modifier.testTag("copy_ics_export_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DateRange,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Copy .ics", style = CollideType.interface13)
                                        }
                                        OutlinedButton(
                                            onClick = onShareIcs,
                                            shape = RoundedCornerShape(2.dp),
                                            modifier = Modifier.testTag("share_ics_export_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = Ink
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Share .ics", style = CollideType.interface13, color = Ink)
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            // --- IMPORT TAB ---
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Rule, RoundedCornerShape(2.dp))
                                    .background(Paper)
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "RESTORE OR IMPORT SEMESTER",
                                    style = CollideType.measured13,
                                    fontWeight = FontWeight.Bold,
                                    color = Ink
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Paste a Collide semester JSON export below to restore your semester, courses, assessments, policies, and chat records across browser resets or devices.",
                                    style = CollideType.prose15,
                                    color = Ink2
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = importJsonText,
                                    onValueChange = { importJsonText = it },
                                    label = { Text("Collide Semester JSON", style = CollideType.interface13) },
                                    placeholder = { Text("{\n  \"collideVersion\": \"1.0.0\",\n  \"semester\": { ... }\n}", style = CollideType.measured11, color = Ink3) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .testTag("import_json_input"),
                                    textStyle = CollideType.measured11,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Ink,
                                        unfocusedBorderColor = Rule,
                                        cursorColor = Ink
                                    )
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = { onImportJson(importJsonText) },
                                    enabled = importJsonText.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Ink,
                                        contentColor = Paper
                                    ),
                                    shape = RoundedCornerShape(2.dp),
                                    modifier = Modifier.testTag("submit_import_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FileUpload,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Import Semester", style = CollideType.interface13)
                                }
                            }
                        }
                        2 -> {
                            // --- SYSTEM & OFFLINE STATUS ---
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Rule, RoundedCornerShape(2.dp))
                                    .background(Paper)
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "ENVIRONMENT & NETWORK DEGRADATION",
                                    style = CollideType.measured13,
                                    fontWeight = FontWeight.Bold,
                                    color = Ink
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Plate, RoundedCornerShape(2.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Gemini API Connection",
                                            style = CollideType.interface13,
                                            color = Ink
                                        )
                                        Text(
                                            text = if (hasApiKey) "Active (Gemini 3.5 Flash)" else "No API Key / Offline Mode",
                                            style = CollideType.measured11,
                                            color = if (hasApiKey) Ink else Ink2
                                        )
                                    }
                                    Text(
                                        text = if (hasApiKey) "ONLINE" else "LOCAL ONLY",
                                        style = CollideType.measured11,
                                        color = if (hasApiKey) Ink else Ink2,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .border(1.dp, if (hasApiKey) Ink else Rule, RoundedCornerShape(2.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Text(
                                    text = "Offline Fallback Architecture:",
                                    style = CollideType.interface13,
                                    fontWeight = FontWeight.Medium,
                                    color = Ink
                                )
                                Text(
                                    text = "• Syllabus Extraction: If an API call fails or the network is unavailable, Collide immediately switches to its local deterministic regex & heuristic parser with 100% offline support.\n• Semester Chat: Runs a deterministic rule-based query engine that performs triage, what-if recomputes, and syllabus policy lookup directly on your local Room database.\n• Persistence, Load Modeling & Timeline: All core workloads, collision warnings, and calendars function entirely offline on-device.",
                                    style = CollideType.prose15,
                                    color = Ink2
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Plate,
                            contentColor = Ink
                        ),
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.testTag("dismiss_portability_modal_button")
                    ) {
                        Text("Done", style = CollideType.interface13)
                    }
                }
            }
        }
    }
}
