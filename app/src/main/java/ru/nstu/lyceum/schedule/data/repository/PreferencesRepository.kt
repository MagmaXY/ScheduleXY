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

    var favoriteTargetType: String?
        get() = prefs.getString(KEY_FAV_TARGET_TYPE, null)
        set(value) = prefs.edit().putString(KEY_FAV_TARGET_TYPE, value).apply()

    var favoriteTargetName: String?
        get() = prefs.getString(KEY_FAV_TARGET_NAME, null)
        set(value) = prefs.edit().putString(KEY_FAV_TARGET_NAME, value).apply()

    var adminPassword: String
        get() = prefs.getString(KEY_ADMIN_PASSWORD, DEFAULT_ADMIN_PASS) ?: DEFAULT_ADMIN_PASS
        set(value) = prefs.edit().putString(KEY_ADMIN_PASSWORD, value).apply()

    var widgetDayOffset: Int
        get() = prefs.getInt(KEY_WIDGET_DAY_OFFSET, 0)
        set(value) = prefs.edit().putInt(KEY_WIDGET_DAY_OFFSET, value).apply()

    fun isFavorite(type: String, name: String): Boolean {
        return favoriteTargetType == type && favoriteTargetName == name
    }

    fun toggleFavorite(type: String, name: String) {
        if (isFavorite(type, name)) {
            favoriteTargetType = null
            favoriteTargetName = null
        } else {
            favoriteTargetType = type
            favoriteTargetName = name
        }
    }

    companion object {
        private const val PREFS_NAME = "lyceum_schedule_prefs"
        const val TYPE_CLASS = "class"
        const val TYPE_TEACHER = "teacher"
        private const val DEFAULT_CLASS = "10-1"
        const val DEFAULT_ADMIN_PASS = "licey2026"

        private const val KEY_TARGET_TYPE = "target_type"
        private const val KEY_TARGET_NAME = "target_name"
        private const val KEY_FAV_TARGET_TYPE = "fav_target_type"
        private const val KEY_FAV_TARGET_NAME = "fav_target_name"
        private const val KEY_ADMIN_PASSWORD = "admin_password"
        private const val KEY_WIDGET_DAY_OFFSET = "widget_day_offset"
    }
}
