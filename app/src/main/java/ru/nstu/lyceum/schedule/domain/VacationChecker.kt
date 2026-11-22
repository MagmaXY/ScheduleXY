package ru.nstu.lyceum.schedule.domain

import android.content.Context
import ru.nstu.lyceum.schedule.data.local.CacheManager
import java.util.Calendar
import java.util.Locale

object VacationChecker {
    @Volatile
    private var cachedVacationDates: Set<String>? = null
    @Volatile
    private var lastCheckTime: Long = 0

    fun isVacation(context: Context, cal: Calendar = Calendar.getInstance()): Boolean {
        val dateStr = String.format(
            Locale.US,
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )

        val now = System.currentTimeMillis()
        if (cachedVacationDates == null || now - lastCheckTime > 300_000) {
            try {
                val cache = CacheManager(context)
                val events = cache.getEvents()?.items
                if (!events.isNullOrEmpty()) {
                    cachedVacationDates = events.filter { ev ->
                        ev.color == "orange" || ev.title.contains("каникул", ignoreCase = true)
                    }.map { it.date }.toSet()
                    lastCheckTime = now
                }
            } catch (_: Exception) {}
        }

        if (cachedVacationDates?.contains(dateStr) == true) {
            return true
        }

        return isDefaultVacationDate(dateStr)
    }

    private fun isDefaultVacationDate(dateStr: String): Boolean {
        return (dateStr in "2026-11-01".."2026-11-08") ||
               (dateStr in "2026-12-30".."2027-01-11") ||
               (dateStr in "2027-03-08".."2027-03-15") ||
               (dateStr in "2027-06-01".."2027-08-31")
    }
}
