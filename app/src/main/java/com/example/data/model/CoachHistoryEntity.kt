package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "coach_history")
data class CoachHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "date")
    val date: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "type")
    val type: String, // "QUESTION_SOLUTION" | "LESSON_TEACHER" | "QA_REPEATER"

    @ColumnInfo(name = "display_text")
    val displayText: String,

    @ColumnInfo(name = "audio_script")
    val audioScript: String,

    @ColumnInfo(name = "audio_path")
    val audioPath: String? = null,

    @ColumnInfo(name = "key_takeaways_json")
    val keyTakeawaysJson: String = "[]",

    @ColumnInfo(name = "image_path")
    val imagePath: String? = null,

    @ColumnInfo(name = "user_prompt")
    val userPrompt: String = ""
)
