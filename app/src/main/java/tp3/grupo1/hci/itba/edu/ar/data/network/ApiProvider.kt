package tp3.grupo1.hci.itba.edu.ar.data.network

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

class ApiProvider(
    initialBaseUrl: String,
    private val apiKeyProvider: () -> String?,
    private val tokenProvider: () -> String?,
    private val onUnauthorized: () -> Unit,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Volatile
    private var services: Services = build(initialBaseUrl)

    val users: UserService get() = services.users
    val homes: HomeService get() = services.homes
    val rooms: RoomService get() = services.rooms
    val devices: DeviceService get() = services.devices
    val deviceTypes: DeviceTypeService get() = services.deviceTypes
    val routines: RoutineService get() = services.routines

    fun setBaseUrl(baseUrl: String) {
        services = build(baseUrl)
    }

    private fun build(baseUrl: String): Services {
        val normalizedUrl = baseUrl.trim().trimEnd('/') + "/"

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val builder = chain.request().newBuilder()
                apiKeyProvider()?.takeIf { it.isNotBlank() }?.let { builder.header("X-API-Key", it) }
                val token = tokenProvider()
                if (token != null) builder.header("Authorization", "Bearer $token")
                val response = chain.proceed(builder.build())
                if (response.code == 401 && token != null) onUnauthorized()
                response
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        return Services(
            users = retrofit.create(UserService::class.java),
            homes = retrofit.create(HomeService::class.java),
            rooms = retrofit.create(RoomService::class.java),
            devices = retrofit.create(DeviceService::class.java),
            deviceTypes = retrofit.create(DeviceTypeService::class.java),
            routines = retrofit.create(RoutineService::class.java),
        )
    }

    private data class Services(
        val users: UserService,
        val homes: HomeService,
        val rooms: RoomService,
        val devices: DeviceService,
        val deviceTypes: DeviceTypeService,
        val routines: RoutineService,
    )
}
