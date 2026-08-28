package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.SemesterEntity
import com.example.model.Assessment
import com.example.model.Course
import com.example.model.SourceRef
import com.example.ui.SemesterViewModel
import com.example.ui.theme.CollideType
import com.example.ui.theme.Ink
import com.example.ui.theme.Ink2
import com.example.ui.theme.Ink3
import com.example.ui.theme.Paper
import com.example.ui.theme.Plate
import com.example.ui.theme.Rule

/**
 * Main application shell for Collide.
 * Features:
 * - Achromatic chrome (Plate canvas, Paper surfaces, Ink/Ink2/Ink3 text, 1px Rule borders)
 * - 240dp left rail on wide screens, adaptive drawer on compact screens
 * - Main content column with Semester Intake & PDF dropzone / Empty state
 * - Right slide-over chat panel that smoothly animates over the content
 * - Local offline persistence via Room
 */
@Composable
fun CollideShell(
    modifier: Modifier = Modifier,
    viewModel: SemesterViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isChatOpen by remember { mutableStateOf(false) }
    var isMobileRailOpen by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Plate)
            .statusBarsPadding()
    ) {
        val isWide = maxWidth >= 600.dp
        val chatPanelWidth = if (isWide) 360.dp else maxWidth

        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // Fixed 240dp left rail for wide viewports
            if (isWide) {
                LeftRail(
                    currentSemester = uiState.currentSemester,
                    semesters = uiState.semesters,
                    isPastSemester = { viewModel.isPastSemester(it) },
                    onSelectSemester = { viewModel.selectSemester(it) },
                    onAddSemesterClick = { viewModel.openCreateSemesterModal() },
                    onOpenPortability = { viewModel.openPortabilityModal() },
                    modifier = Modifier
                        .width(240.dp)
                        .fillMaxHeight()
                )
            }

            // Main column
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Plate)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header Bar (Achromatic)
                    TopBar(
                        isWide = isWide,
                        isChatOpen = isChatOpen,
                        activeSemester = uiState.currentSemester,
                        isPastSemester = uiState.isCurrentSemesterPast,
                        hasApiKey = uiState.hasGeminiApiKey,
                        currentScreen = uiState.currentScreen,
                        syllabusCount = uiState.syllabusFiles.size,
                        assessmentCount = uiState.assessments.size,
                        courseCount = uiState.courses.size,
                        pendingReviewCount = uiState.assessments.count { !it.isConfirmed && it.confidence < 0.85 },
                        onSelectScreen = { viewModel.setScreen(it) },
                        onOpenPortability = { viewModel.openPortabilityModal() },
                        onToggleRail = { isMobileRailOpen = !isMobileRailOpen },
                        onToggleChat = { isChatOpen = !isChatOpen }
                    )

                    // Past Semester Archive Notice Banner
                    if (uiState.isCurrentSemesterPast && uiState.currentSemester != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Ink)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("past_semester_banner"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PAST SEMESTER RECORD · PRESERVED READ-ONLY TRACE",
                                style = CollideType.measured11,
                                color = Paper
                            )
                            Text(
                                text = "HOW BAD IT ACTUALLY WAS",
                                style = CollideType.measured11,
                                color = Ink3
                            )
                        }
                    }

                    // Main Content Area
                    if (uiState.currentSemester == null) {
                        // Empty State (Part 1 baseline)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyStateView(
                                onCreateSemester = { viewModel.openCreateSemesterModal() }
                            )
                        }
                    } else {
                        // Render screen based on current active screen state
                        when (uiState.currentScreen) {
                            com.example.ui.AppScreen.INTAKE -> {
                                SemesterIntakeView(
                                    semester = uiState.currentSemester!!,
                                    files = uiState.syllabusFiles,
                                    courses = uiState.courses,
                                    assessments = uiState.assessments,
                                    extractionProgress = uiState.extractionProgress,
                                    intakeError = uiState.intakeError,
                                    onFilesDropped = { uris -> viewModel.handleIncomingFiles(uris) },
                                    onLoadSampleFiles = { viewModel.loadSampleSyllabi() },
                                    onDeleteFile = { fileId -> viewModel.deleteFile(fileId) },
                                    onStartExtraction = { viewModel.startExtraction() },
                                    onClearError = { viewModel.clearIntakeError() },
                                    onNavigateToReview = { viewModel.setScreen(com.example.ui.AppScreen.REVIEW) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            com.example.ui.AppScreen.REVIEW -> {
                                ReviewScreen(
                                    semester = uiState.currentSemester!!,
                                    courses = uiState.courses,
                                    assessments = uiState.assessments,
                                    selectedCourseFilterId = uiState.selectedCourseFilterId,
                                    onSelectCourseFilter = { viewModel.setCourseFilter(it) },
                                    onConfirmAssessment = { viewModel.confirmAssessment(it) },
                                    onBulkConfirmCourse = { viewModel.bulkConfirmCourse(it) },
                                    onUpdateAssessment = { viewModel.updateAssessment(it) },
                                    onDeleteAssessment = { viewModel.deleteAssessment(it) },
                                    onRenderPdfPage = { fileName, page -> viewModel.renderPdfPage(fileName, page) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            com.example.ui.AppScreen.COURSES -> {
                                val activeCourse = uiState.courses.find { it.id == uiState.selectedCourseId }
                                if (activeCourse != null) {
                                    CourseView(
                                        course = activeCourse,
                                        assessments = uiState.assessments,
                                        onBack = { viewModel.selectCourse(null) },
                                        onEditAssessment = { viewModel.updateAssessment(it) },
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    CoursesListView(
                                        semester = uiState.currentSemester!!,
                                        courses = uiState.courses,
                                        assessments = uiState.assessments,
                                        onSelectCourse = { viewModel.selectCourse(it) },
                                        onNavigateToReview = { viewModel.setScreen(com.example.ui.AppScreen.REVIEW) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            com.example.ui.AppScreen.TRACE -> {
                                SemesterTraceScreen(
                                    semester = uiState.currentSemester!!,
                                    courses = uiState.courses,
                                    assessments = uiState.assessments,
                                    loadResult = uiState.semesterLoadResult,
                                    onUpdateCapacity = { viewModel.updateSemesterCapacity(it) },
                                    onOpenSettings = { viewModel.openSettingsModal() },
                                    onNavigateToReview = { viewModel.setScreen(com.example.ui.AppScreen.REVIEW) },
                                    onUpdateAssessment = { viewModel.updateAssessment(it) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            com.example.ui.AppScreen.SETTINGS -> {
                                SemesterTraceScreen(
                                    semester = uiState.currentSemester!!,
                                    courses = uiState.courses,
                                    assessments = uiState.assessments,
                                    loadResult = uiState.semesterLoadResult,
                                    onUpdateCapacity = { viewModel.updateSemesterCapacity(it) },
                                    onOpenSettings = { viewModel.openSettingsModal() },
                                    onNavigateToReview = { viewModel.setScreen(com.example.ui.AppScreen.REVIEW) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Chat panel (Slides over content from the right)
                androidx.compose.animation.AnimatedVisibility(
                    visible = isChatOpen,
                    enter = slideInHorizontally(initialOffsetX = { it }),
                    exit = slideOutHorizontally(targetOffsetX = { it }),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(chatPanelWidth)
                        .fillMaxHeight()
                ) {
                    SemesterChatPanel(
                        semester = uiState.currentSemester,
                        courses = uiState.courses,
                        assessments = uiState.assessments,
                        loadResult = uiState.semesterLoadResult,
                        messages = uiState.chatMessages,
                        isLoading = uiState.isChatLoading,
                        onSendMessage = { prompt -> viewModel.sendChatMessage(prompt) },
                        onClearChat = { viewModel.clearChat() },
                        onClose = { isChatOpen = false }
                    )
                }

                // Mobile top sheet / drawer left rail overlay if active on compact screens
                if (!isWide && isMobileRailOpen) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Ink.copy(alpha = 0.35f))
                            .clickable { isMobileRailOpen = false }
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isMobileRailOpen,
                            enter = slideInVertically(initialOffsetY = { -it }),
                            exit = slideOutVertically(targetOffsetY = { -it }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                        ) {
                            LeftRail(
                                currentSemester = uiState.currentSemester,
                                semesters = uiState.semesters,
                                isPastSemester = { viewModel.isPastSemester(it) },
                                onSelectSemester = {
                                    viewModel.selectSemester(it)
                                    isMobileRailOpen = false
                                },
                                onAddSemesterClick = {
                                    isMobileRailOpen = false
                                    viewModel.openCreateSemesterModal()
                                },
                                onOpenPortability = {
                                    isMobileRailOpen = false
                                    viewModel.openPortabilityModal()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Paper)
                                    .clickable(enabled = false) {},
                                onCloseMobile = { isMobileRailOpen = false }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal dialog for Semester creation
    if (uiState.showCreateModal) {
        CreateSemesterDialog(
            onDismiss = { viewModel.closeCreateSemesterModal() },
            onCreate = { name, start, weeks, breaks, capacity ->
                viewModel.createSemester(name, start, weeks, breaks, capacity)
            }
        )
    }

    // Modal dialog for Load Model Settings ("How long things take you")
    if (uiState.showSettingsModal) {
        SettingsModal(
            currentConfigs = uiState.loadModelSettings,
            onSaveConfigs = { viewModel.updateLoadModelSettings(it) },
            onResetDefaults = { viewModel.resetLoadModelSettings() },
            onDismiss = { viewModel.closeSettingsModal() }
        )
    }

    // Modal dialog for Persistence & Portability (Export JSON, Export ICS, Import JSON)
    if (uiState.showPortabilityModal) {
        PortabilityModal(
            semester = uiState.currentSemester,
            hasApiKey = uiState.hasGeminiApiKey,
            notificationMessage = uiState.portabilityNotification,
            onCopyJson = { viewModel.copyExportJson() },
            onShareJson = { viewModel.shareExportJson() },
            onCopyIcs = { viewModel.copyExportIcs() },
            onShareIcs = { viewModel.shareExportIcs() },
            onImportJson = { viewModel.importSemesterJson(it) },
            onDismiss = { viewModel.closePortabilityModal() }
        )
    }
}

/**
 * 240px Left rail holding semester switcher and status.
 */
@Composable
fun LeftRail(
    currentSemester: SemesterEntity?,
    semesters: List<SemesterEntity>,
    isPastSemester: (SemesterEntity?) -> Boolean,
    onSelectSemester: (String) -> Unit,
    onAddSemesterClick: () -> Unit,
    onOpenPortability: () -> Unit,
    modifier: Modifier = Modifier,
    onCloseMobile: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .drawBehind {
                // 1px hairline rule on the right boundary
                drawLine(
                    color = Rule,
                    start = Offset(size.width, 0f),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            },
        color = Paper
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                // Header of the rail
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "COLLIDE",
                        style = CollideType.interface18,
                        modifier = Modifier.testTag("app_brand_title")
                    )

                    if (onCloseMobile != null) {
                        IconButton(
                            onClick = onCloseMobile,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close navigation",
                                tint = Ink2
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Active Semester Selector Tag
                val isCurrentPast = isPastSemester(currentSemester)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Rule, RoundedCornerShape(2.dp))
                        .background(Plate)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .testTag("semester_selector")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentSemester?.name?.uppercase() ?: "NO ACTIVE SEMESTER",
                            style = CollideType.measured11,
                            color = if (currentSemester != null) Ink else Ink3,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (currentSemester != null) {
                            Text(
                                text = "${currentSemester.weekCount}W",
                                style = CollideType.measured11,
                                color = Ink2
                            )
                        }
                    }
                    if (isCurrentPast && currentSemester != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "PAST RECORD (READ-ONLY)",
                            style = CollideType.measured11,
                            color = Ink2
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Semesters List (if multiple)
                if (semesters.size > 1) {
                    Text(
                        text = "SEMESTERS",
                        style = CollideType.measured11,
                        color = Ink2
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(semesters, key = { it.id }) { sem ->
                            val isSelected = sem.id == currentSemester?.id
                            val isPast = isPastSemester(sem)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectSemester(sem.id) }
                                    .border(
                                        1.dp,
                                        if (isSelected) Ink else Rule,
                                        RoundedCornerShape(2.dp)
                                    )
                                    .background(if (isSelected) Plate else Paper)
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .testTag("rail_semester_item_${sem.id}"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Text(
                                        text = sem.name,
                                        style = CollideType.interface13,
                                        color = Ink,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isPast) {
                                        Text(
                                            text = "PAST RECORD",
                                            style = CollideType.measured11,
                                            color = Ink3
                                        )
                                    }
                                }
                                Text(
                                    text = "${sem.weekCount}w",
                                    style = CollideType.measured11,
                                    color = Ink2
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Portability Trigger
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPortability() }
                        .border(1.dp, Rule, RoundedCornerShape(2.dp))
                        .background(Paper)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .testTag("rail_portability_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "EXPORT / IMPORT",
                        style = CollideType.measured11,
                        color = Ink
                    )
                    Text(
                        text = "JSON · .ICS",
                        style = CollideType.measured11,
                        color = Ink2
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Add Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAddSemesterClick() }
                    .border(1.dp, Rule, RoundedCornerShape(2.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("add_semester_rail_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Ink,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "New semester",
                    style = CollideType.interface13,
                    color = Ink
                )
            }
        }
    }
}

/**
 * Top bar housing navigation tabs (Intake, Review, Courses, Trace), Portability button, and Chat trigger.
 */
@Composable
fun TopBar(
    isWide: Boolean,
    isChatOpen: Boolean,
    activeSemester: SemesterEntity?,
    isPastSemester: Boolean,
    hasApiKey: Boolean,
    currentScreen: com.example.ui.AppScreen,
    syllabusCount: Int,
    assessmentCount: Int,
    courseCount: Int,
    pendingReviewCount: Int,
    onSelectScreen: (com.example.ui.AppScreen) -> Unit,
    onOpenPortability: () -> Unit,
    onToggleRail: () -> Unit,
    onToggleChat: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .drawBehind {
                drawLine(
                    color = Rule,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            },
        color = Paper
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!isWide) {
                    IconButton(
                        onClick = onToggleRail,
                        modifier = Modifier.testTag("mobile_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Toggle course menu",
                            tint = Ink
                        )
                    }
                }

                if (activeSemester != null) {
                    // Navigation Tabs: Intake, Review, Courses, Trace
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Intake Tab
                        val isIntake = currentScreen == com.example.ui.AppScreen.INTAKE
                        Row(
                            modifier = Modifier
                                .clickable { onSelectScreen(com.example.ui.AppScreen.INTAKE) }
                                .background(if (isIntake) Ink else Plate, RoundedCornerShape(2.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("top_tab_intake"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "INTAKE ($syllabusCount)",
                                style = CollideType.measured11,
                                color = if (isIntake) Paper else Ink2
                            )
                        }

                        // 2. Review Tab
                        val isReview = currentScreen == com.example.ui.AppScreen.REVIEW
                        Row(
                            modifier = Modifier
                                .clickable { onSelectScreen(com.example.ui.AppScreen.REVIEW) }
                                .background(if (isReview) Ink else Plate, RoundedCornerShape(2.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("top_tab_review"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "REVIEW ($assessmentCount)",
                                style = CollideType.measured11,
                                color = if (isReview) Paper else Ink2
                            )
                            if (pendingReviewCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(com.example.ui.theme.Collision, CircleShape)
                                )
                            }
                        }

                        // 3. Courses Tab
                        val isCourses = currentScreen == com.example.ui.AppScreen.COURSES
                        Row(
                            modifier = Modifier
                                .clickable { onSelectScreen(com.example.ui.AppScreen.COURSES) }
                                .background(if (isCourses) Ink else Plate, RoundedCornerShape(2.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("top_tab_courses"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "COURSES ($courseCount)",
                                style = CollideType.measured11,
                                color = if (isCourses) Paper else Ink2
                            )
                        }

                        // 4. Trace Tab
                        val isTrace = currentScreen == com.example.ui.AppScreen.TRACE
                        Row(
                            modifier = Modifier
                                .clickable { onSelectScreen(com.example.ui.AppScreen.TRACE) }
                                .background(if (isTrace) Ink else Plate, RoundedCornerShape(2.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("top_tab_trace"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TRACE",
                                style = CollideType.measured11,
                                color = if (isTrace) Paper else Ink2
                            )
                        }
                    }
                } else {
                    Text(
                        text = "SEMESTER WORKLOAD",
                        style = CollideType.measured11,
                        color = Ink2
                    )
                }
            }

            // Right actions in TopBar: Portability Button & Chat toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Portability quick export button
                Row(
                    modifier = Modifier
                        .clickable { onOpenPortability() }
                        .border(1.dp, Rule, RoundedCornerShape(2.dp))
                        .background(Plate)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("top_bar_portability_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "EXPORT / ICS",
                        style = CollideType.measured11,
                        color = Ink
                    )
                }

                // Chat button
                Row(
                    modifier = Modifier
                        .clickable { onToggleChat() }
                        .border(1.dp, if (isChatOpen) Ink else Rule, RoundedCornerShape(2.dp))
                        .background(if (isChatOpen) Plate else Paper)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("chat_toggle_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        tint = if (isChatOpen) Ink else Ink2,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isChatOpen) "Close chat" else "Chat",
                        style = CollideType.interface13,
                        color = if (isChatOpen) Ink else Ink2
                    )
                }
            }
        }
    }
}

/**
 * Extracted Courses List view showing grading breakdowns and policies.
 */
@Composable
fun CoursesListView(
    semester: SemesterEntity,
    courses: List<Course>,
    assessments: List<Assessment>,
    onSelectCourse: (String) -> Unit = {},
    onNavigateToReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inspectingSource by remember { mutableStateOf<Pair<com.example.model.SourceRef, String>?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("courses_list_view")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "COURSES & POLICIES",
                    style = CollideType.title24,
                    color = Ink
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${courses.size} EXTRACTED COURSES · ${semester.name.uppercase()}",
                    style = CollideType.measured11,
                    color = Ink2
                )
            }

            androidx.compose.material3.Button(
                onClick = onNavigateToReview,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Ink,
                    contentColor = Paper
                ),
                shape = RoundedCornerShape(2.dp),
                modifier = Modifier.testTag("go_to_review_button")
            ) {
                Text(
                    text = "Review Assessments (${assessments.size})",
                    style = CollideType.interface13
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (courses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Paper)
                    .border(1.dp, Rule, RoundedCornerShape(2.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No extracted courses yet. Upload syllabi and start extraction.",
                    style = CollideType.interface15,
                    color = Ink2
                )
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
                    Box(modifier = Modifier.clickable { onSelectCourse(course.id) }) {
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

    if (inspectingSource != null) {
        SourceProvenanceModal(
            source = inspectingSource!!.first,
            title = inspectingSource!!.second,
            onDismiss = { inspectingSource = null }
        )
    }
}

/**
 * Semester Trace View respecting the rule:
 * "Nothing reaches the trace until it has been confirmed or has confidence above 0.85."
 */
@Composable
fun SemesterTraceView(
    semester: SemesterEntity,
    courses: List<Course>,
    assessments: List<Assessment>,
    onNavigateToReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val courseMap = remember(courses) { courses.associateBy { it.id } }
    val traceAssessments = remember(assessments) {
        assessments.filter { it.isConfirmed || it.confidence >= 0.85 }
            .sortedBy { it.dueDate ?: "9999-99-99" }
    }
    val withheldCount = remember(assessments) {
        assessments.count { !it.isConfirmed && it.confidence < 0.85 }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("semester_trace_view")
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SEMESTER TRACE",
                    style = CollideType.title24,
                    color = Ink
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "WORKLOAD TIMELINE · ${traceAssessments.size} ACTIVE ITEMS",
                    style = CollideType.measured11,
                    color = Ink2
                )
            }

            androidx.compose.material3.OutlinedButton(
                onClick = onNavigateToReview,
                shape = RoundedCornerShape(2.dp)
            ) {
                Text(
                    text = "Review Screen ($withheldCount pending)",
                    style = CollideType.interface13,
                    color = Ink
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Trace Rule Notice
        if (withheldCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Rule, RoundedCornerShape(2.dp))
                    .background(Paper)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Notice: $withheldCount unconfirmed assessments (<85% confidence) are withheld from the trace.",
                    style = CollideType.interface13,
                    color = Ink
                )
                androidx.compose.material3.TextButton(onClick = onNavigateToReview) {
                    Text("Review items →", style = CollideType.interface13, color = Ink)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Trace items list
        if (traceAssessments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Paper)
                    .border(1.dp, Rule, RoundedCornerShape(2.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No assessments in trace. Confirm items on the Review screen.",
                    style = CollideType.interface15,
                    color = Ink2
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(traceAssessments, key = { it.id }) { item ->
                    val course = courseMap[item.courseId]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Rule, RoundedCornerShape(2.dp))
                            .background(Paper)
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.dueDate ?: "WEEK DRAFT",
                                style = CollideType.measured13,
                                color = Ink
                            )
                            Text(
                                text = course?.code ?: "COURSE",
                                style = CollideType.measured11,
                                color = Ink2,
                                modifier = Modifier
                                    .background(Plate, RoundedCornerShape(2.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                            Text(
                                text = item.title,
                                style = CollideType.interface15,
                                color = Ink,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${item.weightPercent.toInt()}%",
                                style = CollideType.measured15,
                                fontWeight = FontWeight.Bold,
                                color = Ink
                            )
                            Text(
                                text = "${item.estimatedHours.toInt()}h load",
                                style = CollideType.measured11,
                                color = Ink2
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Empty state: First thing anyone sees.
 * Single line of copy and one action.
 * "No semester yet. Start one, drop your syllabi in, and Collide will map the term."
 * Button: "Create semester"
 */
@Composable
fun EmptyStateView(
    onCreateSemester: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Single line copy in interface type
        Text(
            text = "No semester yet. Start one, drop your syllabi in, and Collide will map the term.",
            style = CollideType.interface15,
            color = Ink,
            modifier = Modifier.testTag("empty_state_copy")
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Create semester button (Achromatic, ink background, paper text, 1px radius)
        Box(
            modifier = Modifier
                .clickable { onCreateSemester() }
                .background(Ink, RoundedCornerShape(2.dp))
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .testTag("create_semester_button"),
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
