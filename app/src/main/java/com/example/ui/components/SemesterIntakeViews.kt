package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.ExtractionProgress
import com.example.data.SemesterEntity
import com.example.data.SyllabusFileEntity
import com.example.model.Assessment
import com.example.model.Course
import com.example.model.SemesterBreak
import com.example.model.SourceRef
import com.example.ui.theme.CollideType
import com.example.ui.theme.Ink
import com.example.ui.theme.Ink2
import com.example.ui.theme.Ink3
import com.example.ui.theme.Paper
import com.example.ui.theme.Plate
import com.example.ui.theme.Rule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/**
 * Semester Creation Modal.
 * Prompts for name, Monday start date, number of weeks, reading/break weeks,
 * and honest weekly capacity with the exact pedagogical explanation.
 */
@Composable
fun CreateSemesterDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, startDate: String, weekCount: Int, breaks: List<SemesterBreak>, capacityHours: Double) -> Unit
) {
    var name by remember { mutableStateOf("Autumn 2026") }
    var startDateInput by remember { mutableStateOf("2026-09-07") }
    var weekCountInput by remember { mutableStateOf("14") }
    var capacityHoursInput by remember { mutableStateOf("25") }
    
    var breakLabel by remember { mutableStateOf("Reading Week") }
    var breakStart by remember { mutableStateOf("2026-10-19") }
    var breakEnd by remember { mutableStateOf("2026-10-25") }
    var hasBreak by remember { mutableStateOf(true) }

    // Computed pinned Monday preview
    val pinnedMonday = remember(startDateInput) {
        try {
            val parsed = LocalDate.parse(startDateInput.trim())
            if (parsed.dayOfWeek == DayOfWeek.MONDAY) {
                "${parsed} (Monday)"
            } else {
                val pinned = parsed.with(TemporalAdjusters.previous(DayOfWeek.MONDAY))
                "${pinned} (Adjusted to Monday)"
            }
        } catch (e: Exception) {
            "Please enter YYYY-MM-DD"
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Rule, RoundedCornerShape(2.dp)),
            color = Paper,
            shape = RoundedCornerShape(2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NEW SEMESTER",
                        style = CollideType.measured13,
                        color = Ink
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Ink2
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Rule))
                Spacer(modifier = Modifier.height(20.dp))

                // Semester Name
                Text(
                    text = "SEMESTER NAME",
                    style = CollideType.measured11,
                    color = Ink2
                )
                Spacer(modifier = Modifier.height(6.dp))
                AchromaticTextInput(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "e.g. Autumn 2026",
                    modifier = Modifier.testTag("semester_name_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Start Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "START DATE (WEEK 1 MONDAY)",
                        style = CollideType.measured11,
                        color = Ink2
                    )
                    Text(
                        text = pinnedMonday,
                        style = CollideType.measured11,
                        color = Ink3
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                AchromaticTextInput(
                    value = startDateInput,
                    onValueChange = { startDateInput = it },
                    placeholder = "YYYY-MM-DD",
                    modifier = Modifier.testTag("semester_start_date_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Duration Weeks
                Text(
                    text = "SEMESTER LENGTH (WEEKS)",
                    style = CollideType.measured11,
                    color = Ink2
                )
                Spacer(modifier = Modifier.height(6.dp))
                AchromaticTextInput(
                    value = weekCountInput,
                    onValueChange = { weekCountInput = it },
                    placeholder = "14",
                    modifier = Modifier.testTag("semester_weeks_input")
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Reading / Break Weeks
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BREAK / READING WEEK",
                        style = CollideType.measured11,
                        color = Ink2
                    )
                    Text(
                        text = if (hasBreak) "Included" else "None",
                        style = CollideType.measured11,
                        color = Ink3,
                        modifier = Modifier.clickable { hasBreak = !hasBreak }
                    )
                }

                if (hasBreak) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AchromaticTextInput(
                        value = breakLabel,
                        onValueChange = { breakLabel = it },
                        placeholder = "Label (e.g. Reading Week)"
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            AchromaticTextInput(
                                value = breakStart,
                                onValueChange = { breakStart = it },
                                placeholder = "Start YYYY-MM-DD"
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            AchromaticTextInput(
                                value = breakEnd,
                                onValueChange = { breakEnd = it },
                                placeholder = "End YYYY-MM-DD"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Weekly Capacity Hours (with mandatory explanation)
                Text(
                    text = "WEEKLY CAPACITY (HOURS)",
                    style = CollideType.measured11,
                    color = Ink2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Hours you can actually study in a normal week, after classes, work, and sleep. Be pessimistic. Most people say 30 and manage 18.",
                    style = CollideType.interface13,
                    color = Ink2
                )
                Spacer(modifier = Modifier.height(8.dp))
                AchromaticTextInput(
                    value = capacityHoursInput,
                    onValueChange = { capacityHoursInput = it },
                    placeholder = "e.g. 25",
                    modifier = Modifier.testTag("capacity_hours_input")
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Submit CTA
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val weeks = weekCountInput.toIntOrNull() ?: 14
                            val capacity = capacityHoursInput.toDoubleOrNull() ?: 25.0
                            val breaksList = if (hasBreak && breakLabel.isNotBlank()) {
                                listOf(SemesterBreak(breakLabel, breakStart, breakEnd))
                            } else emptyList()

                            onCreate(name, startDateInput, weeks, breaksList, capacity)
                        }
                        .background(Ink, RoundedCornerShape(2.dp))
                        .padding(vertical = 14.dp)
                        .testTag("submit_create_semester_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Create semester",
                        style = CollideType.interface15,
                        color = Paper
                    )
                }
            }
        }
    }
}

/**
 * Drop zone and PDF intake table view with Extraction triggers and course results inspection.
 */
@Composable
fun SemesterIntakeView(
    semester: SemesterEntity,
    files: List<SyllabusFileEntity>,
    courses: List<Course>,
    assessments: List<Assessment>,
    extractionProgress: ExtractionProgress?,
    intakeError: String?,
    onFilesDropped: (List<Uri>) -> Unit,
    onLoadSampleFiles: () -> Unit,
    onDeleteFile: (String) -> Unit,
    onStartExtraction: () -> Unit,
    onClearError: () -> Unit,
    onNavigateToReview: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(if (courses.isNotEmpty()) "courses" else "intake") }
    var inspectingSource by remember { mutableStateOf<Pair<SourceRef, String>?>(null) }

    // Multi-document PDF picker
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            onFilesDropped(uris)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Semester Top Summary Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Rule, RoundedCornerShape(2.dp))
                .background(Paper)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = semester.name.uppercase(),
                    style = CollideType.title24,
                    color = Ink
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "STARTS ${semester.startDate}  ·  ${semester.weekCount} WEEKS",
                    style = CollideType.measured11,
                    color = Ink2
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "AVAILABLE CAPACITY",
                    style = CollideType.measured11,
                    color = Ink2
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${semester.capacityHoursPerWeek.toInt()} HRS / WK",
                    style = CollideType.measured18,
                    color = Ink
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Navigation tab between Intake & Extracted Results
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab: Intake
                Row(
                    modifier = Modifier
                        .clickable { activeTab = "intake" }
                        .background(if (activeTab == "intake") Ink else Plate, RoundedCornerShape(2.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("tab_intake"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = if (activeTab == "intake") Paper else Ink2,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "SYLLABI INTAKE (${files.size})",
                        style = CollideType.measured11,
                        color = if (activeTab == "intake") Paper else Ink2
                    )
                }

                // Tab: Extracted Courses
                Row(
                    modifier = Modifier
                        .clickable { activeTab = "courses" }
                        .background(if (activeTab == "courses") Ink else Plate, RoundedCornerShape(2.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("tab_extracted_courses"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = if (activeTab == "courses") Paper else Ink2,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "EXTRACTED COURSES (${courses.size})",
                        style = CollideType.measured11,
                        color = if (activeTab == "courses") Paper else Ink2
                    )
                }
            }

            // Action CTAs: Review Assessments & Extract
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (assessments.isNotEmpty() && onNavigateToReview != null) {
                    OutlinedButton(
                        onClick = onNavigateToReview,
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.testTag("intake_go_to_review_button")
                    ) {
                        Text(
                            text = "Review Screen (${assessments.size}) →",
                            style = CollideType.interface13,
                            color = Ink
                        )
                    }
                }

                if (files.isNotEmpty()) {
                    Button(
                        onClick = onStartExtraction,
                        colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper),
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.testTag("extract_all_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (courses.isEmpty()) "Extract All Syllabi (${files.size})" else "Re-Extract All",
                            fontSize = 12.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Extraction Progress Card
        if (extractionProgress != null && extractionProgress.isRunning) {
            ExtractionProgressCard(
                progress = extractionProgress,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Error message banner (Strict Design Law error message)
        if (intakeError != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Ink, RoundedCornerShape(2.dp))
                    .background(Paper)
                    .padding(12.dp)
                    .testTag("intake_error_banner"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Ink,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = intakeError,
                        style = CollideType.interface13,
                        color = Ink
                    )
                }

                IconButton(
                    onClick = onClearError,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss error",
                        tint = Ink2
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Content switching based on Tab
        if (activeTab == "intake") {
            // Dropzone Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Rule, RoundedCornerShape(2.dp))
                    .background(Paper)
                    .clickable { launcher.launch(arrayOf("application/pdf", "*/*")) }
                    .padding(20.dp)
                    .testTag("pdf_dropzone"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.UploadFile,
                        contentDescription = null,
                        tint = Ink,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Drop syllabus PDFs here, or click to browse",
                        style = CollideType.interface15,
                        color = Ink
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select all courses for the semester. PDF format required.",
                        style = CollideType.interface11,
                        color = Ink2
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick sample syllabus load action for instant evaluation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "INTAKE: ${files.size} FILES LOADED",
                    style = CollideType.measured11,
                    color = Ink2
                )

                Row(
                    modifier = Modifier
                        .clickable { onLoadSampleFiles() }
                        .background(Plate, RoundedCornerShape(2.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("load_sample_syllabi_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Ink2,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Load 6 sample syllabi PDFs",
                        style = CollideType.measured11,
                        color = Ink
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Syllabi Files Table
            if (files.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(1.dp, Rule, RoundedCornerShape(2.dp))
                        .background(Paper)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "This semester has no syllabi files.",
                            style = CollideType.interface15,
                            color = Ink
                        )
                        Text(
                            text = "Drop your course PDFs into the dropzone above, or load sample syllabi to inspect the pipeline.",
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
                        .border(1.dp, Rule, RoundedCornerShape(2.dp))
                        .background(Paper),
                    verticalArrangement = Arrangement.Top
                ) {
                    items(files, key = { it.id }) { file ->
                        FileRowItem(
                            file = file,
                            onDelete = { onDeleteFile(file.id) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Honest footer copy on separate intake / extraction phases
            Text(
                text = "Files are stored locally in offline database. Intake and extraction are separate steps so you can inspect all files before starting extraction.",
                style = CollideType.interface11,
                color = Ink3
            )
        } else {
            // Extracted Courses View
            if (courses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(1.dp, Rule, RoundedCornerShape(2.dp))
                        .background(Paper)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (files.isEmpty()) "This semester has no syllabi to extract." else "Files are staged but not yet extracted.",
                            style = CollideType.interface15,
                            color = Ink
                        )
                        Text(
                            text = if (files.isEmpty()) "Switch to the Intake tab to upload your course syllabi." else "Click 'Extract All Syllabi' in the top right to parse grading schemes and dates.",
                            style = CollideType.interface13,
                            color = Ink2
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(courses, key = { it.id }) { course ->
                        val courseAssessments = assessments.filter { it.courseId == course.id }
                        ExtractedCourseCard(
                            course = course,
                            assessments = courseAssessments,
                            onInspectSource = { source, title ->
                                inspectingSource = Pair(source, title)
                            }
                        )
                    }
                }
            }
        }
    }

    // Provenance Inspector Modal
    if (inspectingSource != null) {
        val (src, title) = inspectingSource!!
        SourceProvenanceModal(
            source = src,
            title = title,
            onDismiss = { inspectingSource = null }
        )
    }
}

/**
 * Single file row displaying PDF name, page count in Martian Mono, size, and status.
 */
@Composable
fun FileRowItem(
    file: SyllabusFileEntity,
    onDelete: () -> Unit
) {
    val sizeKb = (file.fileSizeBytes / 1024).coerceAtLeast(1)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = Rule,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("file_row_${file.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = Ink2,
                modifier = Modifier.size(18.dp)
            )

            Column {
                Text(
                    text = file.fileName,
                    style = CollideType.interface13,
                    color = Ink
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "${file.pageCount} PAGES",
                        style = CollideType.measured11,
                        color = Ink2
                    )
                    Text(
                        text = "${sizeKb} KB",
                        style = CollideType.measured11,
                        color = Ink3
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .border(1.dp, Rule, RoundedCornerShape(2.dp))
                    .background(Plate)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = file.status.uppercase(),
                    style = CollideType.measured11,
                    color = Ink
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove file",
                    tint = Ink3
                )
            }
        }
    }
}

/**
 * Clean 1px achromatic text input field.
 */
@Composable
fun AchromaticTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Rule, RoundedCornerShape(2.dp))
            .background(Plate)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                style = CollideType.interface13,
                color = Ink3
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = CollideType.interface13.copy(color = Ink),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}
