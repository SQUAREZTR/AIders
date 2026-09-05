package com.example.data.model

import android.net.Uri
import java.util.UUID

enum class QuestionStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    ERROR
}

data class QuestionBatchItem(
    val id: String = UUID.randomUUID().toString(),
    val questionNumber: Int,
    val imageUri: Uri? = null,
    val prompt: String = "",
    val status: QuestionStatus = QuestionStatus.PENDING,
    val response: CoachResponse? = null,
    val historyEntity: CoachHistoryEntity? = null,
    val errorMessage: String? = null
)
