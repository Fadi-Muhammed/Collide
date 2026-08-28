package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CollideRepository
import com.example.data.ExtractionProgress
import com.example.data.SemesterEntity
import com.example.data.SyllabusFileEntity
import com.example.model.Assessment
import com.example.model.AssessmentType
import com.example.model.AssessmentTypeConfig
import com.example.model.Course
import com.example.model.DEFAULT_TYPE_CONFIGS
import com.example.model.SemesterBreak
import com.example.model.SemesterLoadResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen(val title: String) {
    INTAKE("Intake"),
    REVIEW("Review"),
    COURSES("Courses"),
    TRACE("Trace"),
    SETTINGS("Settings")
}

data class SemesterUiState(
    val semesters: List<SemesterEntity> = emptyList(),
    val currentSemester: SemesterEntity? = null,
    val syllabusFiles: List<SyllabusFileEntity> = emptyList(),
    val courses: List<Course> = emptyList(),
    val assessments: List<Assessment> = emptyList(),
    val currentScreen: AppScreen = AppScreen.INTAKE,
    val selectedCourseFilterId: String? = null,
    val selectedCourseId: String? = null,
    val isLoading: Boolean = false,
    val intakeError: String? = null,
    val showCreateModal: Boolean = false,
    val showSettingsModal: Boolean = false,
    val showPortabilityModal: Boolean = false,
    val isCurrentSemesterPast: Boolean = false,
    val hasGeminiApiKey: Boolean = false,
    val portabilityNotification: String? = null,
    val extractionProgress: ExtractionProgress? = null,
    val loadModelSettings: Map<AssessmentType, AssessmentTypeConfig> = DEFAULT_TYPE_CONFIGS,
    val semesterLoadResult: SemesterLoadResult? = null,
    val chatMessages: List<com.example.data.ChatMessage> = emptyList(),
    val isChatLoading: Boolean = false
)

private data class IntakeConfig(
    val selectedSemesterId: String?,
    val selectedCourseId: String?,
    val currentScreen: AppScreen,
    val selectedCourseFilterId: String?,
    val intakeError: String?,
    val showCreateModal: Boolean,
    val showSettingsModal: Boolean,
    val showPortabilityModal: Boolean,
    val isLoading: Boolean,
    val extractionProgress: ExtractionProgress?,
    val loadModelSettings: Map<AssessmentType, AssessmentTypeConfig>,
    val portabilityNotification: String?,
    val isChatLoading: Boolean
)

class SemesterViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CollideRepository(application)
    private val chatService = com.example.data.SemesterChatService(application)

    private val _selectedSemesterId = MutableStateFlow<String?>(null)
    private val _selectedCourseId = MutableStateFlow<String?>(null)
    private val _currentScreen = MutableStateFlow(AppScreen.INTAKE)
    private val _selectedCourseFilterId = MutableStateFlow<String?>(null)
    private val _intakeError = MutableStateFlow<String?>(null)
    private val _showCreateModal = MutableStateFlow(false)
    private val _showSettingsModal = MutableStateFlow(false)
    private val _showPortabilityModal = MutableStateFlow(false)
    private val _isLoading = MutableStateFlow(false)
    private val _extractionProgress = MutableStateFlow<ExtractionProgress?>(null)
    private val _loadModelSettings = MutableStateFlow(repository.getLoadModelSettings())
    private val _portabilityNotification = MutableStateFlow<String?>(null)
    private val _isChatLoading = MutableStateFlow(false)

    val allSemesters: StateFlow<List<SemesterEntity>> = repository.getAllSemesters()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val intakeConfig = combine(
        combine(_selectedSemesterId, _selectedCourseId, _currentScreen, _selectedCourseFilterId) { semId, courseId, screen, filter ->
            Tuple4(semId, courseId, screen, filter)
        },
        combine(_intakeError, _showCreateModal, _showSettingsModal, _showPortabilityModal) { err, modal, settingsModal, portModal ->
            Tuple4(err, modal, settingsModal, portModal)
        },
        combine(_isLoading, _extractionProgress, _loadModelSettings, combine(_portabilityNotification, _isChatLoading) { notif, chatL -> notif to chatL }) { loading, prog, settings, (notif, chatL) ->
            Tuple4(loading, prog, settings, notif to chatL)
        }
    ) { (semId, courseId, screen, filter), (err, modal, settingsModal, portModal), (loading, prog, settings, pair) ->
        IntakeConfig(
            selectedSemesterId = semId,
            selectedCourseId = courseId,
            currentScreen = screen,
            selectedCourseFilterId = filter,
            intakeError = err,
            showCreateModal = modal,
            showSettingsModal = settingsModal,
            showPortabilityModal = portModal,
            isLoading = loading,
            extractionProgress = prog,
            loadModelSettings = settings,
            portabilityNotification = pair.first,
            isChatLoading = pair.second
        )
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<SemesterUiState> = combine(allSemesters, intakeConfig) { semesters, config ->
        val active = if (config.selectedSemesterId != null) {
            semesters.find { it.id == config.selectedSemesterId }
        } else {
            semesters.firstOrNull()
        }
        val isPast = isPastSemester(active)
        SemesterUiState(
            semesters = semesters,
            currentSemester = active,
            currentScreen = config.currentScreen,
            selectedCourseFilterId = config.selectedCourseFilterId,
            selectedCourseId = config.selectedCourseId,
            isLoading = config.isLoading,
            intakeError = config.intakeError,
            showCreateModal = config.showCreateModal,
            showSettingsModal = config.showSettingsModal,
            showPortabilityModal = config.showPortabilityModal,
            isCurrentSemesterPast = isPast,
            hasGeminiApiKey = chatService.hasApiKey(),
            portabilityNotification = config.portabilityNotification,
            extractionProgress = config.extractionProgress,
            loadModelSettings = config.loadModelSettings,
            isChatLoading = config.isChatLoading
        )
    }.flatMapLatest { state ->
        val sem = state.currentSemester
        if (sem != null) {
            combine(
                repository.getFilesForSemester(sem.id),
                repository.getCoursesForSemester(sem.id),
                repository.getAssessmentsForSemester(sem.id),
                repository.getChatMessagesForSemester(sem.id)
            ) { files, courses, assessments, chatEntities ->
                val loadResult = repository.computeSemesterLoad(
                    semester = sem,
                    courses = courses,
                    assessments = assessments,
                    customConfigs = state.loadModelSettings
                )
                val mappedChat = chatEntities.map { entity ->
                    com.example.data.ChatMessage(
                        id = entity.id,
                        sender = if (entity.sender == "USER") com.example.data.MessageSender.USER else com.example.data.MessageSender.SEMESTER,
                        text = entity.text,
                        timestamp = entity.timestamp
                    )
                }
                state.copy(
                    syllabusFiles = files,
                    courses = courses,
                    assessments = assessments,
                    chatMessages = mappedChat,
                    semesterLoadResult = loadResult
                )
            }
        } else {
            flowOf(state.copy(syllabusFiles = emptyList(), courses = emptyList(), assessments = emptyList(), chatMessages = emptyList(), semesterLoadResult = null))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SemesterUiState())

    fun isPastSemester(semester: SemesterEntity?): Boolean {
        if (semester == null) return false
        return try {
            val start = java.time.LocalDate.parse(semester.startDate)
            val end = start.plusWeeks(semester.weekCount.toLong())
            java.time.LocalDate.now().isAfter(end)
        } catch (e: Exception) {
            false
        }
    }

    fun setScreen(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun setCourseFilter(courseId: String?) {
        _selectedCourseFilterId.value = courseId
    }

    fun openCreateSemesterModal() {
        _intakeError.value = null
        _showCreateModal.value = true
    }

    fun closeCreateSemesterModal() {
        _showCreateModal.value = false
    }

    fun openSettingsModal() {
        _showSettingsModal.value = true
    }

    fun closeSettingsModal() {
        _showSettingsModal.value = false
    }

    fun openPortabilityModal() {
        _showPortabilityModal.value = true
        _portabilityNotification.value = null
    }

    fun closePortabilityModal() {
        _showPortabilityModal.value = false
        _portabilityNotification.value = null
    }

    fun selectSemester(semesterId: String) {
        _selectedSemesterId.value = semesterId
        _selectedCourseId.value = null
        _selectedCourseFilterId.value = null
    }

    fun selectCourse(courseId: String?) {
        _selectedCourseId.value = courseId
    }

    fun clearIntakeError() {
        _intakeError.value = null
    }

    fun clearPortabilityNotification() {
        _portabilityNotification.value = null
    }

    fun updateSemesterCapacity(capacity: Double) {
        val semId = uiState.value.currentSemester?.id ?: return
        viewModelScope.launch {
            repository.updateSemesterCapacity(semId, capacity)
        }
    }

    fun updateLoadModelSettings(newConfigs: Map<AssessmentType, AssessmentTypeConfig>) {
        repository.saveLoadModelSettings(newConfigs)
        _loadModelSettings.value = newConfigs
    }

    fun resetLoadModelSettings() {
        repository.resetLoadModelSettings()
        _loadModelSettings.value = repository.getLoadModelSettings()
    }

    fun createSemester(
        name: String,
        startDate: String,
        weekCount: Int,
        breaks: List<SemesterBreak>,
        capacityHours: Double
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val created = repository.createSemester(
                name = name,
                startDateIso = startDate,
                weekCount = weekCount,
                breaks = breaks,
                capacityHoursPerWeek = capacityHours
            )
            _selectedSemesterId.value = created.id
            _selectedCourseId.value = null
            _currentScreen.value = AppScreen.INTAKE
            _isLoading.value = false
            _showCreateModal.value = false
        }
    }

    // --- Portability & Export / Import ---

    suspend fun getExportJsonString(): String? {
        val semId = uiState.value.currentSemester?.id ?: return null
        return try {
            repository.portabilityService.exportSemesterToJson(semId)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getExportIcsString(): String? {
        val semId = uiState.value.currentSemester?.id ?: return null
        return try {
            repository.portabilityService.exportConfirmedDeadlinesToIcs(semId)
        } catch (e: Exception) {
            null
        }
    }

    fun copyExportJson() {
        val sem = uiState.value.currentSemester ?: return
        viewModelScope.launch {
            try {
                val json = repository.portabilityService.exportSemesterToJson(sem.id)
                repository.portabilityService.copyToClipboard("${sem.name} JSON", json)
                _portabilityNotification.value = "Copied complete semester JSON to clipboard."
            } catch (e: Exception) {
                _portabilityNotification.value = "Failed to export JSON: ${e.message}"
            }
        }
    }

    fun shareExportJson() {
        val sem = uiState.value.currentSemester ?: return
        viewModelScope.launch {
            try {
                val json = repository.portabilityService.exportSemesterToJson(sem.id)
                repository.portabilityService.shareContent(
                    title = "Export ${sem.name} (Collide JSON)",
                    content = json,
                    mimeType = "application/json"
                )
            } catch (e: Exception) {
                _portabilityNotification.value = "Failed to share JSON: ${e.message}"
            }
        }
    }

    fun copyExportIcs() {
        val sem = uiState.value.currentSemester ?: return
        viewModelScope.launch {
            try {
                val ics = repository.portabilityService.exportConfirmedDeadlinesToIcs(sem.id)
                repository.portabilityService.copyToClipboard("${sem.name} iCalendar", ics)
                _portabilityNotification.value = "Copied confirmed deadlines .ics to clipboard."
            } catch (e: Exception) {
                _portabilityNotification.value = "Failed to export .ics: ${e.message}"
            }
        }
    }

    fun shareExportIcs() {
        val sem = uiState.value.currentSemester ?: return
        viewModelScope.launch {
            try {
                val ics = repository.portabilityService.exportConfirmedDeadlinesToIcs(sem.id)
                repository.portabilityService.shareContent(
                    title = "${sem.name} Deadlines.ics",
                    content = ics,
                    mimeType = "text/calendar"
                )
            } catch (e: Exception) {
                _portabilityNotification.value = "Failed to share .ics: ${e.message}"
            }
        }
    }

    fun importSemesterJson(jsonString: String) {
        if (jsonString.isBlank()) {
            _portabilityNotification.value = "Please paste valid Collide semester JSON."
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.portabilityService.importSemesterFromJson(jsonString.trim(), generateNewIds = true)
            _isLoading.value = false
            if (result.isSuccess) {
                val imported = result.getOrNull()
                if (imported != null) {
                    _selectedSemesterId.value = imported.id
                    _selectedCourseId.value = null
                    _selectedCourseFilterId.value = null
                    _currentScreen.value = AppScreen.TRACE
                    _showPortabilityModal.value = false
                    _portabilityNotification.value = "Successfully imported ${imported.name}."
                }
            } else {
                _portabilityNotification.value = "Import failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun handleIncomingFiles(uris: List<Uri>) {
        val currentSemId = uiState.value.currentSemester?.id ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _intakeError.value = null
            var lastError: String? = null

            for (uri in uris) {
                val res = repository.ingestDocument(currentSemId, uri)
                if (res.isFailure) {
                    lastError = res.exceptionOrNull()?.message
                        ?: "Collide reads PDFs. Export your syllabus as PDF and drop it again."
                }
            }

            _intakeError.value = lastError
            _isLoading.value = false
        }
    }

    fun loadSampleSyllabi() {
        val currentSemId = uiState.value.currentSemester?.id ?: return
        viewModelScope.launch {
            _isLoading.value = true
            repository.loadSampleSyllabi(currentSemId)
            _isLoading.value = false
        }
    }

    fun deleteFile(fileId: String) {
        viewModelScope.launch {
            repository.deleteSyllabusFile(fileId)
        }
    }

    fun startExtraction() {
        val currentSemId = uiState.value.currentSemester?.id ?: return
        viewModelScope.launch {
            _isLoading.value = true
            repository.extractAllFiles(currentSemId) { progress ->
                _extractionProgress.value = progress
            }
            _isLoading.value = false
            _currentScreen.value = AppScreen.REVIEW
        }
    }

    fun confirmAssessment(assessmentId: String) {
        viewModelScope.launch {
            repository.confirmAssessment(assessmentId)
        }
    }

    fun bulkConfirmCourse(courseId: String) {
        viewModelScope.launch {
            repository.bulkConfirmCourse(courseId)
        }
    }

    fun updateAssessment(assessment: Assessment) {
        val semId = uiState.value.currentSemester?.id ?: return
        viewModelScope.launch {
            repository.updateAssessment(assessment, semId)
        }
    }

    fun deleteAssessment(assessmentId: String) {
        viewModelScope.launch {
            repository.deleteAssessment(assessmentId)
        }
    }

    suspend fun renderPdfPage(fileName: String, page: Int): Bitmap? {
        return repository.renderPdfPageBitmap(fileName, page)
    }

    fun clearChat() {
        val semId = uiState.value.currentSemester?.id ?: return
        viewModelScope.launch {
            repository.clearChatForSemester(semId)
        }
    }

    fun sendChatMessage(prompt: String) {
        if (prompt.isBlank() || _isChatLoading.value) return
        val currentSem = uiState.value.currentSemester ?: return
        val currentCourses = uiState.value.courses
        val currentAssessments = uiState.value.assessments
        val currentLoad = uiState.value.semesterLoadResult
        val currentMessages = uiState.value.chatMessages

        val userMessage = com.example.data.ChatMessage(
            id = "msg_u_${System.currentTimeMillis()}",
            sender = com.example.data.MessageSender.USER,
            text = prompt.trim()
        )

        _isChatLoading.value = true

        viewModelScope.launch {
            // Save user message in DB
            repository.saveChatMessage(currentSem.id, "USER", prompt.trim())

            try {
                val reply = chatService.sendMessage(
                    userMessage = prompt.trim(),
                    history = currentMessages + userMessage,
                    semester = currentSem,
                    courses = currentCourses,
                    assessments = currentAssessments,
                    loadResult = currentLoad
                )

                val replyText = reply.ifBlank { "That isn't in the syllabus data. Ask the instructor." }
                // Save assistant message in DB
                repository.saveChatMessage(currentSem.id, "SEMESTER", replyText)
            } catch (e: Exception) {
                val errorText = "Error communicating with semester engine: ${e.message ?: "Unknown error"}"
                repository.saveChatMessage(currentSem.id, "SEMESTER", errorText)
            } finally {
                _isChatLoading.value = false
            }
        }
    }
}

private data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
