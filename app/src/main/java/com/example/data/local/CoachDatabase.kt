package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.CoachHistoryEntity

@Database(entities = [CoachHistoryEntity::class], version = 1, exportSchema = false)
abstract class CoachDatabase : RoomDatabase() {
    abstract fun coachHistoryDao(): CoachHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: CoachDatabase? = null

        fun getInstance(context: Context): CoachDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CoachDatabase::class.java,
                    "coach_history.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
