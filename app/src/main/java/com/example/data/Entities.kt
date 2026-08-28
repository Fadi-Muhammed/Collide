package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "semesters")
data class SemesterEntity(
    @PrimaryKey val id: String,
    val name: String,
    val startDate: String,               // ISO date (e.g. 2026-09-07, Monday)
    val weekCount: Int,
    val breaksJson: String,              // JSON list of SemesterBreak
    val capacityHoursPerWeek: Double,    // student honest available study hours
    val createdAt: String
)

@Entity(tableName = "syllabus_files")
data class SyllabusFileEntity(
    @PrimaryKey val id: String,
    val semesterId: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val pageCount: Int,
    val localFilePath: String,
    val fileSha256: String = "",
    val status: String,                  // "READY_FOR_EXTRACTION", "EXTRACTING", "EXTRACTED", "ERROR"
    val errorMessage: String? = null,
    val addedAt: String
)

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey val id: String,
    val semesterId: String,
    val fileId: String,
    val code: String,
    val title: String,
    val instructor: String?,
    val credits: Double?,
    val gradeBreakdownJson: String,
    val latePolicy: String?,
    val attendancePolicy: String?,
    val resubmissionPolicy: String?,
    val sourceFileName: String,
    val sourcePage: Int,
    val sourceQuote: String,
    val confidence: Double
)

@Entity(tableName = "assessments")
data class AssessmentEntity(
    @PrimaryKey val id: String,
    val courseId: String,
    val semesterId: String,
    val title: String,
    val type: String,
    val weightPercent: Double,
    val dueDate: String?,
    val dueTime: String?,
    val dateBasis: String,
    val estimatedHours: Double,
    val hoursBasis: String,
    val status: String,
    val sourceFileName: String,
    val sourcePage: Int,
    val sourceQuote: String,
    val confidence: Double,
    val isConfirmed: Boolean = false
)

@Entity(tableName = "extraction_cache")
data class ExtractionCacheEntity(
    @PrimaryKey val fileSha256: String,
    val resultJson: String,
    val extractedAt: String
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val semesterId: String,
    val sender: String, // "USER" or "SEMESTER"
    val text: String,
    val timestamp: Long
)
