package ru.nstu.lyceum.schedule.tiles

import android.app.PendingIntent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import ru.nstu.lyceum.schedule.R
import ru.nstu.lyceum.schedule.data.repository.PreferencesRepository
import ru.nstu.lyceum.schedule.ui.AppIconManager
import ru.nstu.lyceum.schedule.ui.MainActivity

class ScheduleTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        val prefs = PreferencesRepository(this)
        tile.state = Tile.STATE_INACTIVE
        tile.label = if (prefs.useMagmaLogo) "ScheduleXY" else "Лицей НГТУ"
        val iconRes = if (prefs.useMagmaLogo) R.drawable.ic_tile_magma else R.drawable.ic_tile_lyceum
        tile.icon = Icon.createWithResource(this, iconRes)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val target = if (prefs.targetType == PreferencesRepository.TYPE_TEACHER) {
                prefs.favoriteTeacher ?: prefs.targetName.takeIf { it.isNotBlank() } ?: PreferencesRepository.DEFAULT_TEACHER
            } else {
                prefs.favoriteClass ?: prefs.targetName.takeIf { it.isNotBlank() } ?: PreferencesRepository.DEFAULT_CLASS
            }
            tile.subtitle = target
        }
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val intent = AppIconManager.createAppIntent(this).apply {
            putExtra(MainActivity.EXTRA_TAB_INDEX, 0)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    companion object {
        fun updateTile(context: android.content.Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    requestListeningState(
                        context,
                        android.content.ComponentName(context, ScheduleTileService::class.java)
                    )
                } catch (_: Exception) {}
            }
        }
    }
}
