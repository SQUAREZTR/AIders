package com.example.data.model

data class SubjectStat(
    val subject: String,
    val count: Int,
    val percentage: Float
)

data class LessonStatistics(
    val totalStudies: Int = 0,
    val questionSolutionsCount: Int = 0,
    val lessonTeacherCount: Int = 0,
    val qaRepeaterCount: Int = 0,
    val imageBasedCount: Int = 0,
    val textOnlyCount: Int = 0,
    val totalAudioCount: Int = 0,
    val totalTacticsLearned: Int = 0,
    val todayStudiesCount: Int = 0,
    val weeklyStudiesCount: Int = 0,
    val streakDays: Int = 0,
    val subjectBreakdown: List<SubjectStat> = emptyList(),
    val coachMotivationalNote: String = "Çalışmaya hemen başla, netlerini zirveye taşı!",
    val recentTactics: List<String> = emptyList()
)
