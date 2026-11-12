package ru.nstu.lyceum.schedule

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import ru.nstu.lyceum.schedule.data.api.LyceumApiService
import ru.nstu.lyceum.schedule.data.local.CacheManager
import ru.nstu.lyceum.schedule.data.repository.PreferencesRepository
import ru.nstu.lyceum.schedule.data.repository.ScheduleRepository
import ru.nstu.lyceum.schedule.domain.RealtimeSyncManager

class LyceumApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var apiService: LyceumApiService
        private set
    lateinit var cacheManager: CacheManager
        private set
    lateinit var preferencesRepository: PreferencesRepository
        private set
    lateinit var scheduleRepository: ScheduleRepository
        private set
    lateinit var realtimeSyncManager: RealtimeSyncManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        apiService = LyceumApiService.create()
        cacheManager = CacheManager(this)
        preferencesRepository = PreferencesRepository(this)
        scheduleRepository = ScheduleRepository(apiService, cacheManager, preferencesRepository)
        realtimeSyncManager = RealtimeSyncManager(applicationScope)
    }

    companion object {
        lateinit var instance: LyceumApp
            private set
    }
}
