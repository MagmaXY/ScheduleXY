package ru.nstu.lyceum.schedule.data.sync

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import ru.nstu.lyceum.schedule.data.local.NotesRepository
import ru.nstu.lyceum.schedule.data.local.ScheduleNote
import ru.nstu.lyceum.schedule.data.repository.PreferencesRepository
import java.util.concurrent.TimeUnit

sealed class SyncResult {
    data class Success(val message: String) : SyncResult()
    data class Error(val error: String) : SyncResult()
    object Idle : SyncResult()
    object InProgress : SyncResult()
}

class YandexDiskSyncService(
    private val context: Context,
    private val preferencesRepository: PreferencesRepository,
    private val notesRepository: NotesRepository
) {
    companion object {
        const val CLIENT_ID = "2954ef8874cf4e239c98b99a2cbedf36" // Yandex OAuth Client ID
        const val REDIRECT_URI = "ru.nstu.lyceum.schedule://oauth"
        private const val DISK_APP_PATH = "app:/lyceum_notes.json"

        fun getAuthUrl(clientId: String = CLIENT_ID): String {
            return "https://oauth.yandex.ru/authorize?response_type=token&client_id=$clientId&redirect_uri=${Uri.encode(REDIRECT_URI)}"
        }

        fun openAuthInCustomTabs(context: Context, clientId: String = CLIENT_ID) {
            val url = getAuthUrl(clientId)
            try {
                val customTabsIntent = CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .build()
                customTabsIntent.launchUrl(context, Uri.parse(url))
            } catch (e: Exception) {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url))
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val syncMutex = Mutex()

    private val _syncState = MutableStateFlow<SyncResult>(SyncResult.Idle)
    val syncState: StateFlow<SyncResult> = _syncState.asStateFlow()

    fun isAuthorized(): Boolean {
        return !preferencesRepository.yandexOAuthToken.isNullOrBlank()
    }

    fun saveToken(token: String) {
        preferencesRepository.yandexOAuthToken = token
    }

    fun logout() {
        preferencesRepository.yandexOAuthToken = null
        preferencesRepository.yandexUserLogin = null
        preferencesRepository.yandexLastSyncTime = 0L
        _syncState.value = SyncResult.Idle
    }

    suspend fun fetchUserInfo(): String? = withContext(Dispatchers.IO) {
        val token = preferencesRepository.yandexOAuthToken ?: return@withContext null
        try {
            val request = Request.Builder()
                .url("https://login.yandex.ru/info?format=json")
                .header("Authorization", "OAuth $token")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val mapType = object : TypeToken<Map<String, Any>>() {}.type
                        val map = gson.fromJson<Map<String, Any>>(body, mapType)
                        val display = map["display_name"] as? String
                            ?: map["real_name"] as? String
                            ?: map["login"] as? String
                            ?: map["default_email"] as? String
                        if (!display.isNullOrBlank()) {
                            preferencesRepository.yandexUserLogin = display
                            return@withContext display
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return@withContext preferencesRepository.yandexUserLogin
    }

    /**
     * Performs two-way synchronization:
     * 1. Downloads notes from Yandex Disk (app:/lyceum_notes.json).
     * 2. Merges cloud and local notes using updatedAt timestamp per note and deletedAt tombstone.
     * 3. Saves merged list locally.
     * 4. Uploads merged list back to Yandex Disk.
     */
    suspend fun sync(): SyncResult = withContext(Dispatchers.IO) {
        val token = preferencesRepository.yandexOAuthToken
        if (token.isNullOrBlank()) {
            return@withContext SyncResult.Error("Не авторизован в Яндекс")
        }

        syncMutex.withLock {
            _syncState.value = SyncResult.InProgress
            try {
                // 1. Check/fetch user info if login is missing
                if (preferencesRepository.yandexUserLogin.isNullOrBlank()) {
                    fetchUserInfo()
                }

                // 2. Download from Yandex Disk
                val cloudNotes = downloadCloudNotes(token)

                // 3. Get local all notes (including soft deleted)
                val localNotes = notesRepository.getAllNotesIncludingDeleted()

                // 4. Merge strategies:
                // Map of all notes by ID
                val mergedMap = LinkedHashMap<String, ScheduleNote>()

                // Populate with local
                for (loc in localNotes) {
                    mergedMap[loc.id] = loc
                }

                // Merge cloud notes
                for (cloud in cloudNotes) {
                    val local = mergedMap[cloud.id]
                    if (local == null) {
                        mergedMap[cloud.id] = cloud
                    } else {
                        // Compare updatedAt
                        val cloudUpdated = cloud.updatedAt
                        val localUpdated = local.updatedAt
                        if (cloudUpdated > localUpdated) {
                            mergedMap[cloud.id] = cloud
                        }
                    }
                }

                val mergedList = mergedMap.values.toList()

                // 5. Update local repository
                notesRepository.applyMergedNotes(mergedList)

                // 6. Upload merged notes to cloud
                uploadCloudNotes(token, mergedList)

                val now = System.currentTimeMillis()
                preferencesRepository.yandexLastSyncTime = now

                val result = SyncResult.Success("Синхронизировано успешно (${mergedList.count { it.deletedAt == null }} заметок)")
                _syncState.value = result
                result
            } catch (e: Exception) {
                val err = SyncResult.Error(e.message ?: "Ошибка синхронизации")
                _syncState.value = err
                err
            }
        }
    }

    private fun downloadCloudNotes(token: String): List<ScheduleNote> {
        val downloadMetaUrl = "https://cloud-api.yandex.net/v1/disk/resources/download?path=${Uri.encode(DISK_APP_PATH)}"
        val metaRequest = Request.Builder()
            .url(downloadMetaUrl)
            .header("Authorization", "OAuth $token")
            .get()
            .build()

        httpClient.newCall(metaRequest).execute().use { response ->
            if (response.code == 404) {
                // File does not exist in app folder yet
                return emptyList()
            }
            if (!response.isSuccessful) {
                throw IllegalStateException("Ошибка запроса скачивания Яндекс Диска (HTTP ${response.code})")
            }
            val body = response.body?.string() ?: throw IllegalStateException("Пустой ответ метаданных скачивания")
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            val map = gson.fromJson<Map<String, Any>>(body, mapType)
            val fileHref = map["href"] as? String ?: throw IllegalStateException("Ссылка на файл не получена")

            // Download direct file content
            val fileRequest = Request.Builder()
                .url(fileHref)
                .get()
                .build()

            httpClient.newCall(fileRequest).execute().use { fileResponse ->
                if (!fileResponse.isSuccessful) {
                    throw IllegalStateException("Не удалось скачать файл lyceum_notes.json (HTTP ${fileResponse.code})")
                }
                val json = fileResponse.body?.string() ?: return emptyList()
                if (json.isBlank()) return emptyList()

                val notesType = object : TypeToken<List<ScheduleNote>>() {}.type
                return gson.fromJson<List<ScheduleNote>>(json, notesType) ?: emptyList()
            }
        }
    }

    private fun uploadCloudNotes(token: String, notes: List<ScheduleNote>) {
        val uploadMetaUrl = "https://cloud-api.yandex.net/v1/disk/resources/upload?path=${Uri.encode(DISK_APP_PATH)}&overwrite=true"
        val metaRequest = Request.Builder()
            .url(uploadMetaUrl)
            .header("Authorization", "OAuth $token")
            .get()
            .build()

        httpClient.newCall(metaRequest).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Ошибка запроса ссылки на загрузку в Яндекс Диск (HTTP ${response.code})")
            }
            val body = response.body?.string() ?: throw IllegalStateException("Пустой ответ метаданных загрузки")
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            val map = gson.fromJson<Map<String, Any>>(body, mapType)
            val uploadHref = map["href"] as? String ?: throw IllegalStateException("Ссылка для загрузки не получена")

            val json = gson.toJson(notes)
            val putRequest = Request.Builder()
                .url(uploadHref)
                .put(json.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            httpClient.newCall(putRequest).execute().use { putResponse ->
                if (!putResponse.isSuccessful && putResponse.code != 201 && putResponse.code != 202) {
                    throw IllegalStateException("Ошибка загрузки файла в Яндекс Диск (HTTP ${putResponse.code})")
                }
            }
        }
    }
}
