package ru.nstu.lyceum.schedule.ui.util

import ru.nstu.lyceum.schedule.data.api.CalendarEvent
import ru.nstu.lyceum.schedule.data.local.ScheduleNote
import kotlin.math.min

object SmartSearchEngine {

    // English QWERTY to Russian ЙЦУКЕН mapping
    private val EN_TO_RU = mapOf(
        'q' to 'й', 'w' to 'ц', 'e' to 'у', 'r' to 'к', 't' to 'е', 'y' to 'н', 'u' to 'г',
        'i' to 'ш', 'o' to 'щ', 'p' to 'з', '[' to 'х', ']' to 'ъ', 'a' to 'ф', 's' to 'ы',
        'd' to 'в', 'f' to 'а', 'g' to 'п', 'h' to 'р', 'j' to 'о', 'k' to 'л', 'l' to 'д',
        ';' to 'ж', '\'' to 'э', 'z' to 'я', 'x' to 'ч', 'c' to 'с', 'v' to 'м', 'b' to 'и',
        'n' to 'т', 'm' to 'ь', ',' to 'б', '.' to 'ю', '`' to 'ё'
    )

    // Russian ЙЦУКЕН to English QWERTY mapping
    private val RU_TO_EN = EN_TO_RU.entries.associate { (en, ru) -> ru to en }

    // Subject abbreviations and common keywords
    private val SUBJECT_SYNONYMS = mapOf(
        "матем" to listOf("математика", "алгебра", "геометрия"),
        "мат" to listOf("математика", "алгебра", "геометрия"),
        "алг" to listOf("алгебра"),
        "геом" to listOf("геометрия"),
        "физ" to listOf("физика", "физическая"),
        "физра" to listOf("физическая культура", "физкультура"),
        "инф" to listOf("информатика"),
        "инфа" to listOf("информатика"),
        "лит" to listOf("литература"),
        "литра" to listOf("литература"),
        "рус" to listOf("русский"),
        "англ" to listOf("английский"),
        "ин яз" to listOf("иностранный", "английский", "немецкий", "французский"),
        "ист" to listOf("история"),
        "общ" to listOf("обществознание"),
        "общага" to listOf("обществознание"),
        "био" to listOf("биология"),
        "хим" to listOf("химия"),
        "гео" to listOf("география"),
        "тех" to listOf("технология"),
        "обж" to listOf("основы безопасности", "обж")
    )

    /**
     * Converts a string between English and Russian keyboard layouts.
     */
    fun convertLayout(text: String): String {
        val lower = text.lowercase()
        val hasEn = lower.any { it in 'a'..'z' || it in listOf('[', ']', ';', '\'', ',', '.', '`') }
        val hasRu = lower.any { it in 'а'..'я' || it == 'ё' }

        return if (hasEn && !hasRu) {
            lower.map { EN_TO_RU[it] ?: it }.joinToString("")
        } else if (hasRu && !hasEn) {
            lower.map { RU_TO_EN[it] ?: it }.joinToString("")
        } else {
            text
        }
    }

    /**
     * Normalizes a string for search comparison.
     */
    fun normalize(text: String?): String {
        if (text.isNullOrBlank()) return ""
        return text.lowercase()
            .replace('ё', 'е')
            .replace(Regex("[^a-zа-я0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Calculates Levenshtein edit distance between two normalized tokens.
     */
    fun levenshteinDistance(s1: String, s2: String): Int {
        if (s1 == s2) return 0
        if (s1.isEmpty()) return s2.length
        if (s2.isEmpty()) return s1.length

        val d = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) d[i][0] = i
        for (j in 0..s2.length) d[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                d[i][j] = min(
                    min(d[i - 1][j] + 1, d[i][j - 1] + 1),
                    d[i - 1][j - 1] + cost
                )
            }
        }
        return d[s1.length][s2.length]
    }

    /**
     * Evaluates a relevance match score (0 = no match, 1..100 = matched).
     */
    fun evaluateScore(
        rawQuery: String,
        title: String?,
        content: String?,
        extra: String? = null
    ): Int {
        val q = normalize(rawQuery)
        if (q.isBlank()) return 100

        val qLayout = normalize(convertLayout(rawQuery))

        val normTitle = normalize(title)
        val normContent = normalize(content)
        val normExtra = normalize(extra)

        val fullText = "$normTitle $normContent $normExtra".trim()
        if (fullText.isEmpty()) return 0

        var bestScore = 0

        // 1. Exact match in title/subject
        if (normTitle.isNotEmpty()) {
            if (normTitle == q) return 100
            if (normTitle.contains(q)) bestScore = maxOf(bestScore, 90)
            if (qLayout.isNotEmpty() && normTitle.contains(qLayout)) bestScore = maxOf(bestScore, 85)
        }

        // 2. Exact match in content
        if (normContent.isNotEmpty()) {
            if (normContent.contains(q)) bestScore = maxOf(bestScore, 80)
            if (qLayout.isNotEmpty() && normContent.contains(qLayout)) bestScore = maxOf(bestScore, 75)
        }

        // 3. Extra text match (e.g. sourceLabel, tag)
        if (normExtra.isNotEmpty()) {
            if (normExtra.contains(q)) bestScore = maxOf(bestScore, 70)
            if (qLayout.isNotEmpty() && normExtra.contains(qLayout)) bestScore = maxOf(bestScore, 65)
        }

        // 4. Word-by-word token matching with typo tolerance & abbreviations
        val queryTokens = q.split(" ").filter { it.length >= 2 }
        val layoutTokens = if (qLayout != q) qLayout.split(" ").filter { it.length >= 2 } else emptyList()
        val allQueryTokens = (queryTokens + layoutTokens).distinct()

        val targetTokens = fullText.split(" ").filter { it.isNotEmpty() }

        if (allQueryTokens.isNotEmpty()) {
            var matchedTokens = 0
            var tokenScoreSum = 0

            for (token in allQueryTokens) {
                var tokenMatched = false
                var tokenBest = 0

                // Check exact word or prefix
                for (tWord in targetTokens) {
                    if (tWord == token) {
                        tokenMatched = true
                        tokenBest = maxOf(tokenBest, 80)
                        break
                    }
                    if (tWord.startsWith(token) && token.length >= 3) {
                        tokenMatched = true
                        tokenBest = maxOf(tokenBest, 70)
                    }
                }

                // Check subject synonyms / abbreviations
                if (!tokenMatched) {
                    val synonyms = SUBJECT_SYNONYMS[token]
                    if (synonyms != null) {
                        for (syn in synonyms) {
                            if (targetTokens.any { it.contains(syn) || syn.contains(it) }) {
                                tokenMatched = true
                                tokenBest = maxOf(tokenBest, 68)
                                break
                            }
                        }
                    }
                }

                // Check Levenshtein typo distance
                if (!tokenMatched && token.length >= 3) {
                    val maxDistance = if (token.length >= 6) 2 else 1
                    for (tWord in targetTokens) {
                        if (kotlin.math.abs(tWord.length - token.length) <= maxDistance) {
                            val dist = levenshteinDistance(token, tWord)
                            if (dist <= maxDistance) {
                                tokenMatched = true
                                val score = if (dist == 1) 60 else 45
                                tokenBest = maxOf(tokenBest, score)
                                break
                            }
                        }
                    }
                }

                if (tokenMatched) {
                    matchedTokens++
                    tokenScoreSum += tokenBest
                }
            }

            if (matchedTokens > 0) {
                val average = tokenScoreSum / matchedTokens
                bestScore = maxOf(bestScore, average)
            }
        }

        return bestScore
    }

    /**
     * Filters and sorts ScheduleNotes by relevance to search query.
     */
    fun searchNotes(notes: List<ScheduleNote>, query: String): List<ScheduleNote> {
        val q = query.trim()
        if (q.isEmpty()) return notes

        return notes
            .map { note ->
                val score = evaluateScore(
                    rawQuery = q,
                    title = note.subject,
                    content = note.text,
                    extra = if (note.type == ru.nstu.lyceum.schedule.data.local.NoteType.HOMEWORK) "домашнее задание дз" else "заметка"
                )
                note to score
            }
            .filter { it.second > 0 }
            .sortedWith(
                compareByDescending<Pair<ScheduleNote, Int>> { it.second }
                    .thenBy { it.first.date }
                    .thenBy { it.first.lessonNum ?: 99 }
            )
            .map { it.first }
    }

    /**
     * Filters and sorts CalendarEvents by relevance to search query.
     */
    fun searchEvents(events: List<CalendarEvent>, query: String): List<CalendarEvent> {
        val q = query.trim()
        if (q.isEmpty()) return events

        return events
            .map { event ->
                val score = evaluateScore(
                    rawQuery = q,
                    title = event.title,
                    content = event.source,
                    extra = event.sourceLabel
                )
                event to score
            }
            .filter { it.second > 0 }
            .sortedWith(
                compareByDescending<Pair<CalendarEvent, Int>> { it.second }
                    .thenBy { it.first.date }
            )
            .map { it.first }
    }
}
