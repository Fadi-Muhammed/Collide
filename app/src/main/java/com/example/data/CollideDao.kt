package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CollideDao {
    // Semesters
    @Query("SELECT * FROM semesters ORDER BY createdAt DESC")
    fun getAllSemesters(): Flow<List<SemesterEntity>>

    @Query("SELECT * FROM semesters WHERE id = :semesterId LIMIT 1")
    suspend fun getSemesterById(semesterId: String): SemesterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSemester(semester: SemesterEntity)

    @Query("UPDATE semesters SET capacityHoursPerWeek = :capacity WHERE id = :semesterId")
    suspend fun updateSemesterCapacity(semesterId: String, capacity: Double)

    @Query("DELETE FROM semesters WHERE id = :semesterId")
    suspend fun deleteSemester(semesterId: String)

    // Syllabus Files
    @Query("SELECT * FROM syllabus_files WHERE semesterId = :semesterId ORDER BY addedAt ASC")
    fun getFilesForSemester(semesterId: String): Flow<List<SyllabusFileEntity>>

    @Query("SELECT * FROM syllabus_files WHERE semesterId = :semesterId")
    suspend fun getFilesListForSemester(semesterId: String): List<SyllabusFileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyllabusFile(file: SyllabusFileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyllabusFiles(files: List<SyllabusFileEntity>)

    @Query("UPDATE syllabus_files SET status = :status, errorMessage = :errorMessage WHERE id = :fileId")
    suspend fun updateFileStatus(fileId: String, status: String, errorMessage: String? = null)

    @Query("DELETE FROM syllabus_files WHERE id = :fileId")
    suspend fun deleteSyllabusFile(fileId: String)

    // Courses
    @Query("SELECT * FROM courses WHERE semesterId = :semesterId ORDER BY code ASC")
    fun getCoursesForSemester(semesterId: String): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE id = :courseId LIMIT 1")
    suspend fun getCourseById(courseId: String): CourseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<CourseEntity>)

    @Query("DELETE FROM courses WHERE semesterId = :semesterId")
    suspend fun deleteCoursesForSemester(semesterId: String)

    @Query("DELETE FROM courses WHERE fileId = :fileId")
    suspend fun deleteCoursesForFile(fileId: String)

    // Assessments
    @Query("SELECT * FROM assessments WHERE semesterId = :semesterId ORDER BY dueDate ASC, title ASC")
    fun getAssessmentsForSemester(semesterId: String): Flow<List<AssessmentEntity>>

    @Query("SELECT * FROM assessments WHERE courseId = :courseId ORDER BY dueDate ASC, title ASC")
    fun getAssessmentsForCourse(courseId: String): Flow<List<AssessmentEntity>>

    @Query("SELECT * FROM assessments WHERE id = :id LIMIT 1")
    suspend fun getAssessmentById(id: String): AssessmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessments(assessments: List<AssessmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessment(assessment: AssessmentEntity)

    @Query("UPDATE assessments SET isConfirmed = 1 WHERE id = :assessmentId")
    suspend fun confirmAssessment(assessmentId: String)

    @Query("UPDATE assessments SET isConfirmed = 1 WHERE courseId = :courseId")
    suspend fun bulkConfirmCourse(courseId: String)

    @Query("DELETE FROM assessments WHERE id = :assessmentId")
    suspend fun deleteAssessment(assessmentId: String)

    @Query("DELETE FROM assessments WHERE semesterId = :semesterId")
    suspend fun deleteAssessmentsForSemester(semesterId: String)

    @Query("DELETE FROM assessments WHERE courseId = :courseId")
    suspend fun deleteAssessmentsForCourse(courseId: String)

    // Extraction Cache
    @Query("SELECT * FROM extraction_cache WHERE fileSha256 = :sha256 LIMIT 1")
    suspend fun getExtractionCache(sha256: String): ExtractionCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExtractionCache(cache: ExtractionCacheEntity)

    // Chat Messages
    @Query("SELECT * FROM chat_messages WHERE semesterId = :semesterId ORDER BY timestamp ASC")
    fun getChatMessagesForSemester(semesterId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE semesterId = :semesterId ORDER BY timestamp ASC")
    suspend fun getChatMessagesListForSemester(semesterId: String): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessages(messages: List<ChatMessageEntity>)

    @Query("DELETE FROM chat_messages WHERE semesterId = :semesterId")
    suspend fun clearChatForSemester(semesterId: String)

    // Synchronous queries for full portability export
    @Query("SELECT * FROM courses WHERE semesterId = :semesterId ORDER BY code ASC")
    suspend fun getCoursesListForSemester(semesterId: String): List<CourseEntity>

    @Query("SELECT * FROM assessments WHERE semesterId = :semesterId ORDER BY dueDate ASC")
    suspend fun getAssessmentsListForSemester(semesterId: String): List<AssessmentEntity>
}
