package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents")
    val contents: List<GeminiContent>,

    @Json(name = "systemInstruction")
    val systemInstruction: GeminiContent? = null,

    @Json(name = "generationConfig")
    val generationConfig: GeminiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts")
    val parts: List<GeminiPart>,

    @Json(name = "role")
    val role: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text")
    val text: String? = null,

    @Json(name = "inlineData")
    val inlineData: GeminiInlineData? = null
)

@JsonClass(generateAdapter = true)
data class GeminiInlineData(
    @Json(name = "mimeType")
    val mimeType: String,

    @Json(name = "data")
    val data: String // Base64 encoded
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @Json(name = "temperature")
    val temperature: Float? = 0.4f,

    @Json(name = "topP")
    val topP: Float? = 0.95f,

    @Json(name = "responseMimeType")
    val responseMimeType: String? = "application/json"
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates")
    val candidates: List<GeminiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content")
    val content: GeminiContent? = null,

    @Json(name = "finishReason")
    val finishReason: String? = null
)
