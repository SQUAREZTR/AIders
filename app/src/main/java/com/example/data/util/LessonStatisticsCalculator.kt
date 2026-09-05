package com.example.data.util

import com.example.data.model.CoachHistoryEntity
import com.example.data.model.LessonStatistics
import com.example.data.model.SubjectStat
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object LessonStatisticsCalculator {

    fun calculate(history: List<CoachHistoryEntity>): LessonStatistics {
        if (history.isEmpty()) {
            return LessonStatistics(
                coachMotivationalNote = "Henüz bir çalışma kaydın bulunmuyor. Hemen bir soru fotoğrafı çekerek veya ders konusu sorarak çalışmaya başla!"
            )
        }

        val totalStudies = history.size
        var questionSolutionsCount = 0
        var lessonTeacherCount = 0
        var qaRepeaterCount = 0
        var imageBasedCount = 0
        var textOnlyCount = 0
        var totalAudioCount = 0

        val allTactics = mutableListOf<String>()
        val subjectCounts = mutableMapOf<String, Int>()

        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        val dayFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val todayStr = dayFormat.format(Date(now))

        val oneWeekAgo = now - (7L * 24 * 60 * 60 * 1000)
        var todayStudiesCount = 0
        var weeklyStudiesCount = 0

        val studyDays = mutableSetOf<String>()

        for (item in history) {
            when (item.type) {
                "QUESTION_SOLUTION" -> questionSolutionsCount++
                "LESSON_TEACHER" -> lessonTeacherCount++
                "QA_REPEATER" -> qaRepeaterCount++
                else -> questionSolutionsCount++
            }

            if (!item.imagePath.isNullOrBlank()) {
                imageBasedCount++
            } else {
                textOnlyCount++
            }

            if (!item.audioPath.isNullOrBlank() || item.audioScript.isNotBlank()) {
                totalAudioCount++
            }

            // Extract takeaways
            try {
                val array = JSONArray(item.keyTakeawaysJson)
                for (i in 0 until array.length()) {
                    val t = array.getString(i).trim()
                    if (t.isNotBlank() && !allTactics.contains(t)) {
                        allTactics.add(t)
                    }
                }
            } catch (e: Throwable) {
                // Fallback for JVM tests or malformed JSON
                val clean = item.keyTakeawaysJson
                    .trim()
                    .removeSurrounding("[", "]")
                if (clean.isNotBlank()) {
                    clean.split(",")
                        .map { it.trim().trim('"', '\'') }
                        .filter { it.isNotBlank() }
                        .forEach { t ->
                            if (!allTactics.contains(t)) {
                                allTactics.add(t)
                            }
                        }
                }
            }

            // Dates & streak tracking
            val itemDayStr = dayFormat.format(Date(item.timestamp))
            studyDays.add(itemDayStr)

            if (itemDayStr == todayStr) {
                todayStudiesCount++
            }

            if (item.timestamp >= oneWeekAgo) {
                weeklyStudiesCount++
            }

            // Subject classification
            val detectedSubject = detectSubject(item.title, item.userPrompt, item.displayText)
            subjectCounts[detectedSubject] = (subjectCounts[detectedSubject] ?: 0) + 1
        }

        // Calculate streak
        val streakDays = calculateStreak(studyDays, dayFormat)

        // Subject breakdown
        val sortedSubjects = subjectCounts.entries
            .sortedByDescending { it.value }
            .map { entry ->
                SubjectStat(
                    subject = entry.key,
                    count = entry.value,
                    percentage = (entry.value.toFloat() / totalStudies) * 100f
                )
            }

        // Coach note
        val coachNote = generateCoachFeedback(
            total = totalStudies,
            streak = streakDays,
            questions = questionSolutionsCount,
            lessons = lessonTeacherCount,
            topSubject = sortedSubjects.firstOrNull()?.subject
        )

        return LessonStatistics(
            totalStudies = totalStudies,
            questionSolutionsCount = questionSolutionsCount,
            lessonTeacherCount = lessonTeacherCount,
            qaRepeaterCount = qaRepeaterCount,
            imageBasedCount = imageBasedCount,
            textOnlyCount = textOnlyCount,
            totalAudioCount = totalAudioCount,
            totalTacticsLearned = allTactics.size,
            todayStudiesCount = todayStudiesCount,
            weeklyStudiesCount = weeklyStudiesCount,
            streakDays = streakDays,
            subjectBreakdown = sortedSubjects,
            coachMotivationalNote = coachNote,
            recentTactics = allTactics.take(6)
        )
    }

    private fun calculateStreak(
        studyDays: Set<String>,
        dayFormat: SimpleDateFormat
    ): Int {
        if (studyDays.isEmpty()) return 0

        val cal = Calendar.getInstance()
        val todayStr = dayFormat.format(cal.time)

        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = dayFormat.format(cal.time)

        // Streak must be active either today or yesterday
        if (!studyDays.contains(todayStr) && !studyDays.contains(yesterdayStr)) {
            return 0
        }

        var streak = 0
        val checkCal = Calendar.getInstance()
        if (!studyDays.contains(todayStr)) {
            // Started yesterday
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
        }

        while (true) {
            val dateStr = dayFormat.format(checkCal.time)
            if (studyDays.contains(dateStr)) {
                streak++
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        return streak
    }

    private fun detectSubject(title: String, prompt: String, text: String): String {
        val combined = "$title $prompt $text".lowercase(Locale.forLanguageTag("tr-TR"))

        return when {
            combined.containsAny(
                "türev", "integral", "trigonometri", "fonksiyon", "limit", "logaritma",
                "geometri", "üçgen", "açı", "parabol", "polinom", "matematik", "çember",
                "analitik", "kosinüs", "sinüs", "tanjant", "denklem", "eşitsizlik", "oran"
            ) -> "Matematik & Geometri"

            combined.containsAny(
                "fizik", "kuvvet", "dinamik", "newton", "manyetizma", "elektrik", "optik",
                "dalga", "kinetik", "potansiyel", "ivme", "vektör", "moment", "basınç", "termodinamik"
            ) -> "Fizik"

            combined.containsAny(
                "kimya", "mol", "periyodik", "asit", "baz", "çözelti", "tepkime", "organik",
                "bileşik", "iyon", "bağ", "gazlar", "entalpi", "redoks", "titrasyon"
            ) -> "Kimya"

            combined.containsAny(
                "biyoloji", "hücre", "dna", "rna", "mitoz", "mayoz", "fotosentez", "solunum",
                "kalıtım", "genetik", "ekoloji", "enzim", "dolaşım", "sinir", "sindirim", "boşaltım"
            ) -> "Biyoloji"

            combined.containsAny(
                "türkçe", "paragraf", "yazım", "noktalama", "dil bilgisi", "ögeleri", "fiil",
                "edebiyat", "şiir", "roman", "divan", "tanzimat", "akım", "yazar", "anlatım"
            ) -> "Türkçe & Edebiyat"

            combined.containsAny(
                "tarih", "osmanlı", "cumhuriyet", "savaş", "antlaşma", "selçuklu", "coğrafya",
                "iklim", "harita", "nüfus", "felsefe", "mantık", "din"
            ) -> "Sosyal Bilimler"

            combined.containsAny(
                "koç", "turlama", "zaman", "program", "plan", "motivasyon", "hedef", "deneme", "stres"
            ) -> "Sınav Koçluğu & Rehberlik"

            else -> "Genel Dersler"
        }
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { this.contains(it) }
    }

    private fun generateCoachFeedback(
        total: Int,
        streak: Int,
        questions: Int,
        lessons: Int,
        topSubject: String?
    ): String {
        val subjectMention = if (!topSubject.isNullOrBlank()) "Özellikle $topSubject alanında yoğunlaşıyorsun." else ""

        return when {
            streak >= 5 -> "🔥 İnanılmaz bir azim! $streak gündür aralıksız soru çözüp konu çalışıyorsun. Sınav maratonunda bu istikrar seni kesinlikle dereceye taşıyacak! $subjectMention"
            streak in 2..4 -> "🚀 Harika tempo! $streak günlük serin oluştu. Rutinini hiç bozmadan devam et, başarı adım adım geliyor! $subjectMention"
            questions > lessons * 2 -> "🎯 Soru pratiğin çok iyi seviyede. Çözdüğün sorulardaki püf noktalarını ve sınav tuzaklarını sesli dinleyerek zihninde kalıcı hale getirmeyi unutma!"
            lessons > questions * 2 -> "📖 Konu temellerini sağlam kuruyorsun. Şimdi öğrendiklerini bol bol soru fotoğrafı yükleyerek test etme zamanı!"
            total in 1..3 -> "🌟 Harika bir başlangıç yaptın! Her gün en az 3 soru veya ders analizi dinleyerek serini büyütmeye ne dersin?"
            else -> "💪 Ders koçun seninle! Düzenli soru çözümü ve sesli taktik dinlemeleri ile netlerin katlanarak artacak. $subjectMention"
        }
    }
}
