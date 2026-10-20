package com.example.sudosleep.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.sudosleep.data.local.dao.GradeDao
import com.example.sudosleep.data.local.dao.RequirementDao
import com.example.sudosleep.data.local.dao.SubjectDao
import com.example.sudosleep.data.local.dao.TimetableDao
import com.example.sudosleep.data.local.entity.ActivityRequirementEntity
import com.example.sudosleep.data.local.entity.GradeEntity
import com.example.sudosleep.data.local.entity.SubjectEntity
import com.example.sudosleep.data.local.entity.TimetableEntryEntity

@Database(
    entities = [
        SubjectEntity::class,
        ActivityRequirementEntity::class,
        TimetableEntryEntity::class,
        GradeEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun subjectDao(): SubjectDao
    abstract fun requirementDao(): RequirementDao
    abstract fun timetableDao(): TimetableDao
    abstract fun gradeDao(): GradeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sudosleep_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
