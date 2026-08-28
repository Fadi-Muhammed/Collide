package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SemesterEntity::class,
        SyllabusFileEntity::class,
        CourseEntity::class,
        AssessmentEntity::class,
        ExtractionCacheEntity::class,
        ChatMessageEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class CollideDatabase : RoomDatabase() {
    abstract fun collideDao(): CollideDao

    companion object {
        @Volatile
        private var INSTANCE: CollideDatabase? = null

        fun getDatabase(context: Context): CollideDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CollideDatabase::class.java,
                    "collide_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
