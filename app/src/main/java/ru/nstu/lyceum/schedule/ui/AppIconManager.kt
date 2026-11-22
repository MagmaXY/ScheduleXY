package ru.nstu.lyceum.schedule.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.service.quicksettings.TileService
import ru.nstu.lyceum.schedule.data.repository.PreferencesRepository
import ru.nstu.lyceum.schedule.widget.ScheduleWidgetProvider

object AppIconManager {
    const val MAIN_ACTIVITY = "ru.nstu.lyceum.schedule.ui.MainActivity"
    const val MAGMA_ALIAS = "ru.nstu.lyceum.schedule.ui.MainActivityMagma"
    private const val TILE_SERVICE = "ru.nstu.lyceum.schedule.tiles.ScheduleTileService"

    fun getActiveComponent(context: Context): ComponentName {
        val prefs = PreferencesRepository(context)
        return ComponentName(
            context,
            if (prefs.useMagmaLogo) MAGMA_ALIAS else MAIN_ACTIVITY
        )
    }

    fun createAppIntent(context: Context): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            component = getActiveComponent(context)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }

    fun setMagmaIcon(context: Context, useMagma: Boolean) {
        val pm = context.packageManager
        val mainComp = ComponentName(context, MAIN_ACTIVITY)
        val magmaComp = ComponentName(context, MAGMA_ALIAS)

        try {
            if (useMagma) {
                pm.setComponentEnabledSetting(
                    magmaComp,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
                pm.setComponentEnabledSetting(
                    mainComp,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            } else {
                pm.setComponentEnabledSetting(
                    mainComp,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
                pm.setComponentEnabledSetting(
                    magmaComp,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }

            // Update app shortcuts for the new active launcher component
            AppShortcutManager.updateShortcuts(context, false)

            // Update quick settings tile icon
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    TileService.requestListeningState(context, ComponentName(context, TILE_SERVICE))
                }
            } catch (_: Exception) {}

            // Update widgets with new open intent
            ScheduleWidgetProvider.notifyWidgetUpdate(context)
        } catch (_: Exception) {}
    }

    fun isMagmaIconActive(context: Context): Boolean {
        val pm = context.packageManager
        val magmaComp = ComponentName(context, MAGMA_ALIAS)
        return try {
            pm.getComponentEnabledSetting(magmaComp) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } catch (_: Exception) {
            false
        }
    }
}
