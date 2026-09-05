package com.example

import com.example.data.model.CoachHistoryEntity
import com.example.data.util.LessonStatisticsCalculator
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {
    @Test
    fun testEmptyStatistics() {
        val stats = LessonStatisticsCalculator.calculate(emptyList())
        assertEquals(0, stats.totalStudies)
        assertEquals(0, stats.questionSolutionsCount)
        assertEquals(0, stats.lessonTeacherCount)
        assertEquals(0, stats.qaRepeaterCount)
        assertEquals(0, stats.totalTacticsLearned)
        assertEquals(0, stats.streakDays)
        assertTrue(stats.subjectBreakdown.isEmpty())
        assertTrue(stats.coachMotivationalNote.isNotBlank())
    }

    @Test
    fun testStatisticsCalculationWithEntities() {
        val now = System.currentTimeMillis()
        val sampleList = listOf(
            CoachHistoryEntity(
                id = 1,
                timestamp = now,
                date = "Bugün",
                type = "QUESTION_SOLUTION",
                title = "Türevde Ekstremum Sorusu",
                userPrompt = "Matematik türev sorusu",
                displayText = "Çözüm adımları...",
                audioScript = "Sesli metin",
                keyTakeawaysJson = "[\"Türev sıfırdır\", \"Tuzağa düşme\"]"
            ),
            CoachHistoryEntity(
                id = 2,
                timestamp = now - 3600_000,
                date = "Bugün",
                type = "LESSON_TEACHER",
                title = "Fizik Newton Hareket Yasaları",
                userPrompt = "Fizik dinamik notları",
                displayText = "Ders notu...",
                audioScript = "Sesli anlatım",
                keyTakeawaysJson = "[\"F=m*a\", \"Sürtünme katsayısı\"]"
            ),
            CoachHistoryEntity(
                id = 3,
                timestamp = now - 7200_000,
                date = "Bugün",
                type = "QA_REPEATER",
                title = "Kimya Mol Kavramı",
                userPrompt = "Mol nedir?",
                displayText = "Cevap...",
                audioScript = "Sesli cevap",
                keyTakeawaysJson = "[\"Avogadro sayısı\"]"
            )
        )

        val stats = LessonStatisticsCalculator.calculate(sampleList)
        assertEquals(3, stats.totalStudies)
        assertEquals(1, stats.questionSolutionsCount)
        assertEquals(1, stats.lessonTeacherCount)
        assertEquals(1, stats.qaRepeaterCount)
        assertEquals(5, stats.totalTacticsLearned)
        assertEquals(1, stats.streakDays)
        assertTrue(stats.subjectBreakdown.any { it.subject.contains("Matematik") })
        assertTrue(stats.subjectBreakdown.any { it.subject.contains("Fizik") })
        assertTrue(stats.subjectBreakdown.any { it.subject.contains("Kimya") })
    }
}
