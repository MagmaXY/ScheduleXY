package ru.nstu.lyceum.schedule.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import ru.nstu.lyceum.schedule.R
import ru.nstu.lyceum.schedule.data.repository.PreferencesRepository

object AppShortcutManager {

    /**
     * Updates dynamic app shortcuts on the home screen for all available bottom tabs.
     * Respects admin password authentication (Tab 3 "Кабинеты" is only included if authenticated).
     */
    fun updateShortcuts(context: Context, isAdmin: Boolean) {
        val prefs = PreferencesRepository(context)
        val activeLauncherComponent = ComponentName(
            context,
            if (prefs.useMagmaLogo)
                "ru.nstu.lyceum.schedule.ui.MainActivityMagma"
            else
                "ru.nstu.lyceum.schedule.ui.MainActivity"
        )

        val shortcuts = mutableListOf<ShortcutInfoCompat>()

        fun createShortcutIntent(tabIndex: Int): Intent {
            return Intent(Intent.ACTION_VIEW).apply {
                component = activeLauncherComponent
                putExtra(MainActivity.EXTRA_TAB_INDEX, tabIndex)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }

        // 1. Расписание (Tab 0)
        shortcuts.add(
            ShortcutInfoCompat.Builder(context, "shortcut_schedule")
                .setActivity(activeLauncherComponent)
                .setShortLabel("Расписание")
                .setLongLabel("Расписание уроков")
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_schedule))
                .setIntent(createShortcutIntent(0))
                .setRank(0)
                .build()
        )

        // 2. Календарь (Tab 1)
        shortcuts.add(
            ShortcutInfoCompat.Builder(context, "shortcut_calendar")
                .setActivity(activeLauncherComponent)
                .setShortLabel("Календарь")
                .setLongLabel("Календарь событий")
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_calendar))
                .setIntent(createShortcutIntent(1))
                .setRank(1)
                .build()
        )

        // 3. Заметки (Tab 2)
        shortcuts.add(
            ShortcutInfoCompat.Builder(context, "shortcut_notes")
                .setActivity(activeLauncherComponent)
                .setShortLabel("Заметки")
                .setLongLabel("Заметки и ДЗ")
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_notes))
                .setIntent(createShortcutIntent(2))
                .setRank(2)
                .build()
        )

        // 4. Кабинеты (Tab 3) - Available to everyone
        shortcuts.add(
            ShortcutInfoCompat.Builder(context, "shortcut_rooms")
                .setActivity(activeLauncherComponent)
                .setShortLabel("Кабинеты")
                .setLongLabel("Свободные кабинеты")
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_rooms))
                .setIntent(createShortcutIntent(3))
                .setRank(3)
                .build()
        )

        // 5. Архив (Tab 4)
        shortcuts.add(
            ShortcutInfoCompat.Builder(context, "shortcut_archive")
                .setActivity(activeLauncherComponent)
                .setShortLabel("Архив")
                .setLongLabel("Архив расписания")
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_archive))
                .setIntent(createShortcutIntent(4))
                .setRank(if (isAdmin) 4 else 3)
                .build()
        )

        try {
            val maxShortcuts = ShortcutManagerCompat.getMaxShortcutCountPerActivity(context)
            val limited = shortcuts.take(maxShortcuts)
            ShortcutManagerCompat.setDynamicShortcuts(context, limited)
        } catch (_: Exception) {}
    }
}
