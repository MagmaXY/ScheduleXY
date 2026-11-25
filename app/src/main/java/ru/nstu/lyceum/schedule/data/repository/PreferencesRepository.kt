package ru.nstu.lyceum.schedule.data.repository

import android.content.Context
import android.content.SharedPreferences

class PreferencesRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var targetType: String
        get() = prefs.getString(KEY_TARGET_TYPE, TYPE_CLASS) ?: TYPE_CLASS
        set(value) = prefs.edit().putString(KEY_TARGET_TYPE, value).apply()

    var targetName: String
        get() = prefs.getString(KEY_TARGET_NAME, DEFAULT_CLASS) ?: DEFAULT_CLASS
        set(value) = prefs.edit().putString(KEY_TARGET_NAME, value).apply()

    var favoriteClass: String?
        get() = prefs.getString(KEY_FAV_CLASS, null)
        set(value) = prefs.edit().putString(KEY_FAV_CLASS, value).apply()

    var favoriteTeacher: String?
        get() = prefs.getString(KEY_FAV_TEACHER, null)
        set(value) = prefs.edit().putString(KEY_FAV_TEACHER, value).apply()

    var adminPassword: String
        get() = prefs.getString(KEY_ADMIN_PASSWORD, DEFAULT_ADMIN_PASS) ?: DEFAULT_ADMIN_PASS
        set(value) = prefs.edit().putString(KEY_ADMIN_PASSWORD, value).apply()

    var widgetDayOffset: Int
        get() = prefs.getInt(KEY_WIDGET_DAY_OFFSET, 0)
        set(value) = prefs.edit().putInt(KEY_WIDGET_DAY_OFFSET, value).apply()

    var widgetDayIndex: Int
        get() = prefs.getInt(KEY_WIDGET_DAY_INDEX, -1)
        set(value) = prefs.edit().putInt(KEY_WIDGET_DAY_INDEX, value).apply()

    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()

    var themePreset: String
        get() = prefs.getString(KEY_THEME_PRESET, THEME_CLASSIC) ?: THEME_CLASSIC
        set(value) = prefs.edit().putString(KEY_THEME_PRESET, value).apply()

    var lastNotifiedWeekStart: String?
        get() = prefs.getString(KEY_LAST_WEEK_START, null)
        set(value) = prefs.edit().putString(KEY_LAST_WEEK_START, value).apply()

    var lastNotifiedExportDate: String?
        get() = prefs.getString(KEY_LAST_EXPORT_DATE, null)
        set(value) = prefs.edit().putString(KEY_LAST_EXPORT_DATE, value).apply()

    var lastNotifiedChangeKeys: Set<String>
        get() = prefs.getStringSet(KEY_LAST_CHANGE_KEYS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_LAST_CHANGE_KEYS, value).apply()

    var lastNotifiedTargets: Set<String>
        get() = prefs.getStringSet(KEY_LAST_TARGETS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_LAST_TARGETS, value).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()

    var showPairsSchedule: Boolean
        get() = prefs.getBoolean(KEY_SHOW_PAIRS_SCHEDULE, false)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_PAIRS_SCHEDULE, value).apply()

    var statusBarPairsMode: Boolean
        get() = prefs.getBoolean(KEY_STATUS_BAR_PAIRS_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_STATUS_BAR_PAIRS_MODE, value).apply()

    var overlaySwipeAnimation: Boolean
        get() = prefs.getBoolean(KEY_OVERLAY_SWIPE_ANIMATION, false)
        set(value) = prefs.edit().putBoolean(KEY_OVERLAY_SWIPE_ANIMATION, value).apply()

    var studentSubgroup: Int
        get() = prefs.getInt(KEY_STUDENT_SUBGROUP, 1)
        set(value) = prefs.edit().putInt(KEY_STUDENT_SUBGROUP, value).apply()

    var studentTechnologySubgroup: Int
        get() = prefs.getInt(KEY_STUDENT_TECH_SUBGROUP, 1)
        set(value) = prefs.edit().putInt(KEY_STUDENT_TECH_SUBGROUP, value).apply()

    var studentTechSubgroup: Int
        get() = studentTechnologySubgroup
        set(value) { studentTechnologySubgroup = value }

    var copyIncludeHomework: Boolean
        get() = prefs.getBoolean(KEY_COPY_INCLUDE_HOMEWORK, false)
        set(value) = prefs.edit().putBoolean(KEY_COPY_INCLUDE_HOMEWORK, value).apply()

    var copyIncludeNotes: Boolean
        get() = prefs.getBoolean(KEY_COPY_INCLUDE_NOTES, false)
        set(value) = prefs.edit().putBoolean(KEY_COPY_INCLUDE_NOTES, value).apply()

    var ongoingStatusNotificationEnabled: Boolean
        get() = prefs.getBoolean(KEY_ONGOING_STATUS_NOTIFICATION, false)
        set(value) = prefs.edit().putBoolean(KEY_ONGOING_STATUS_NOTIFICATION, value).apply()

    fun isMiddleSchoolClass(className: String?): Boolean {
        if (className.isNullOrBlank()) return false
        return Regex("^[5-7][а-яА-Яa-zA-Z].*").matches(className.trim())
    }

    fun isFavorite(type: String, name: String): Boolean {
        return if (type == TYPE_TEACHER) {
            favoriteTeacher == name
        } else {
            favoriteClass == name
        }
    }

    fun toggleFavorite(type: String, name: String) {
        if (type == TYPE_TEACHER) {
            favoriteTeacher = if (favoriteTeacher == name) null else name
        } else {
            favoriteClass = if (favoriteClass == name) null else name
        }
    }

    var useMagmaLogo: Boolean
        get() = prefs.getBoolean(KEY_USE_MAGMA_LOGO, false)
        set(value) = prefs.edit().putBoolean(KEY_USE_MAGMA_LOGO, value).apply()

    var isMagmaUnlocked: Boolean
        get() = prefs.getBoolean(KEY_IS_MAGMA_UNLOCKED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_MAGMA_UNLOCKED, value).apply()

    // --- Yandex Disk Sync Settings ---
    var yandexOAuthToken: String?
        get() = prefs.getString(KEY_YANDEX_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_YANDEX_TOKEN, value).apply()

    var yandexUserLogin: String?
        get() = prefs.getString(KEY_YANDEX_LOGIN, null)
        set(value) = prefs.edit().putString(KEY_YANDEX_LOGIN, value).apply()

    var yandexLastSyncTime: Long
        get() = prefs.getLong(KEY_YANDEX_LAST_SYNC, 0L)
        set(value) = prefs.edit().putLong(KEY_YANDEX_LAST_SYNC, value).apply()

    // --- App Update Settings ---
    var autoUpdateCheckEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_UPDATE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_UPDATE_ENABLED, value).apply()

    var lastUpdateCheckTime: Long
        get() = prefs.getLong(KEY_LAST_UPDATE_CHECK_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_UPDATE_CHECK_TIME, value).apply()

    var dismissedUpdateHash: String?
        get() = prefs.getString(KEY_DISMISSED_UPDATE_HASH, null)
        set(value) = prefs.edit().putString(KEY_DISMISSED_UPDATE_HASH, value).apply()

    var cachedAvailableReleaseJson: String?
        get() = prefs.getString(KEY_CACHED_AVAILABLE_RELEASE, null)
        set(value) = prefs.edit().putString(KEY_CACHED_AVAILABLE_RELEASE, value).apply()

    companion object {
        private const val PREFS_NAME = "lyceum_schedule_prefs"
        const val TYPE_CLASS = "class"
        const val TYPE_TEACHER = "teacher"
        const val DEFAULT_CLASS = "10-1"
        const val DEFAULT_TEACHER = "Муль П.Ф."
        const val DEFAULT_ADMIN_PASS = ""

        const val THEME_CLASSIC = "classic"
        const val THEME_EMERALD = "emerald"
        const val THEME_DYNAMIC = "dynamic"
        const val THEME_OLED = "oled"
        private const val KEY_THEME_PRESET = "theme_preset"

        private const val KEY_TARGET_TYPE = "target_type"
        private const val KEY_TARGET_NAME = "target_name"
        private const val KEY_FAV_CLASS = "fav_class"
        private const val KEY_FAV_TEACHER = "fav_teacher"
        private const val KEY_ADMIN_PASSWORD = "admin_password"
        private const val KEY_WIDGET_DAY_OFFSET = "widget_day_offset"
        private const val KEY_WIDGET_DAY_INDEX = "widget_day_index"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_LAST_WEEK_START = "last_notified_week_start"
        private const val KEY_LAST_EXPORT_DATE = "last_notified_export_date"
        private const val KEY_LAST_CHANGE_KEYS = "last_notified_change_keys"
        private const val KEY_LAST_TARGETS = "last_notified_targets"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_SHOW_PAIRS_SCHEDULE = "show_pairs_schedule"
        private const val KEY_STATUS_BAR_PAIRS_MODE = "status_bar_pairs_mode"
        private const val KEY_OVERLAY_SWIPE_ANIMATION = "overlay_swipe_animation"
        private const val KEY_STUDENT_SUBGROUP = "student_subgroup"
        private const val KEY_STUDENT_TECH_SUBGROUP = "student_tech_subgroup"
        private const val KEY_COPY_INCLUDE_HOMEWORK = "copy_include_homework"
        private const val KEY_COPY_INCLUDE_NOTES = "copy_include_notes"
        private const val KEY_ONGOING_STATUS_NOTIFICATION = "ongoing_status_notification"
        private const val KEY_USE_MAGMA_LOGO = "use_magma_logo"
        private const val KEY_IS_MAGMA_UNLOCKED = "is_magma_unlocked"

        private const val KEY_YANDEX_TOKEN = "yandex_oauth_token"
        private const val KEY_YANDEX_LOGIN = "yandex_user_login"
        private const val KEY_YANDEX_LAST_SYNC = "yandex_last_sync_time"

        private const val KEY_AUTO_UPDATE_ENABLED = "auto_update_check_enabled"
        private const val KEY_LAST_UPDATE_CHECK_TIME = "last_update_check_time"
        private const val KEY_DISMISSED_UPDATE_HASH = "dismissed_update_hash"
        private const val KEY_CACHED_AVAILABLE_RELEASE = "cached_available_release"
    }
}
