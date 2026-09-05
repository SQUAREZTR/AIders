package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CoachResponse(
    @Json(name = "type")
    val type: String = "QUESTION_SOLUTION", // "QUESTION_SOLUTION" | "LESSON_TEACHER" | "QA_REPEATER"

    @Json(name = "title")
    val title: String = "",

    @Json(name = "display_text")
    val displayText: String = "",

    @Json(name = "audio_script")
    val audioScript: String = "",

    @Json(name = "key_takeaways")
    val keyTakeaways: List<String> = emptyList()
)
