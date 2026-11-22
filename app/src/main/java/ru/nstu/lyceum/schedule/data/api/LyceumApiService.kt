package ru.nstu.lyceum.schedule.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface LyceumApiService {

    @GET("schedule/classes")
    suspend fun getClasses(): ClassesResponse

    @GET("schedule/teachers")
    suspend fun getTeachers(): TeachersResponse

    @GET("schedule/class/{name}")
    suspend fun getClassSchedule(
        @Path("name") name: String,
        @Query("week") week: Int = 0
    ): ScheduleResponse

    @GET("schedule/teacher/{name}")
    suspend fun getTeacherSchedule(
        @Path("name") name: String,
        @Query("week") week: Int = 0
    ): ScheduleResponse

    @GET("schedule/free-rooms")
    suspend fun getFreeRooms(
        @Header("X-Admin-Password") adminPassword: String = "licey2026"
    ): FreeRoomsResponse

    @GET("schedule/free-teachers")
    suspend fun getFreeTeachers(
        @Header("X-Admin-Password") adminPassword: String = "licey2026"
    ): FreeTeachersResponse

    @GET("schedule/archive")
    suspend fun getArchiveWeeks(): ArchiveResponse

    @GET("events")
    suspend fun getEvents(): EventsResponse

    @GET("admin/check")
    suspend fun checkAdminPassword(
        @Header("X-Admin-Password") adminPassword: String
    ): AdminCheckResponse

    @POST("schedule/refresh")
    suspend fun refreshSchedule(): Response<Unit>

    companion object {
        const val BASE_URL = "https://lyceum.nstu.ru/info/api/"

        fun create(): LyceumApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("Cache-Control", "no-cache, no-store")
                        .header("Pragma", "no-cache")
                        .build()
                    chain.proceed(request)
                }
                .addInterceptor(logging)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(LyceumApiService::class.java)
        }
    }
}
