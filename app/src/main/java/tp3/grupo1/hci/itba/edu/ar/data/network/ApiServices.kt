package tp3.grupo1.hci.itba.edu.ar.data.network

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import tp3.grupo1.hci.itba.edu.ar.data.model.AuthResponse
import tp3.grupo1.hci.itba.edu.ar.data.model.ChangePasswordRequest
import tp3.grupo1.hci.itba.edu.ar.data.model.CodeResponse
import tp3.grupo1.hci.itba.edu.ar.data.model.Device
import tp3.grupo1.hci.itba.edu.ar.data.model.DeviceCreateRequest
import tp3.grupo1.hci.itba.edu.ar.data.model.DeviceType
import tp3.grupo1.hci.itba.edu.ar.data.model.DeviceUpdateRequest
import tp3.grupo1.hci.itba.edu.ar.data.model.EmailRequest
import tp3.grupo1.hci.itba.edu.ar.data.model.Home
import tp3.grupo1.hci.itba.edu.ar.data.model.HomeCreateRequest
import tp3.grupo1.hci.itba.edu.ar.data.model.HomeShareRequest
import tp3.grupo1.hci.itba.edu.ar.data.model.HomeSharedUser
import tp3.grupo1.hci.itba.edu.ar.data.model.HomeUpdateRequest
import tp3.grupo1.hci.itba.edu.ar.data.model.LoginRequest
import tp3.grupo1.hci.itba.edu.ar.data.model.RegisterRequest
import tp3.grupo1.hci.itba.edu.ar.data.model.ResetPasswordRequest
import tp3.grupo1.hci.itba.edu.ar.data.model.Room
import tp3.grupo1.hci.itba.edu.ar.data.model.RoomCreateRequest
import tp3.grupo1.hci.itba.edu.ar.data.model.RoomUpdateRequest
import tp3.grupo1.hci.itba.edu.ar.data.model.Routine
import tp3.grupo1.hci.itba.edu.ar.data.model.RoutineActionResult
import tp3.grupo1.hci.itba.edu.ar.data.model.RoutineUpsertRequest
import tp3.grupo1.hci.itba.edu.ar.data.model.UpdateProfileRequest
import tp3.grupo1.hci.itba.edu.ar.data.model.User
import tp3.grupo1.hci.itba.edu.ar.data.model.VerifyAccountRequest

interface UserService {
    @POST("users/register")
    suspend fun register(@Body body: RegisterRequest): User

    @POST("users/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("users/logout")
    suspend fun logout()

    @GET("users/profile")
    suspend fun getProfile(): User

    @PUT("users/profile")
    suspend fun updateProfile(@Body body: UpdateProfileRequest): User

    @POST("users/change-password")
    suspend fun changePassword(@Body body: ChangePasswordRequest)

    @POST("users/verify-account")
    suspend fun verifyAccount(@Body body: VerifyAccountRequest)

    @POST("users/forgot-password")
    suspend fun forgotPassword(@Body body: EmailRequest)

    @POST("users/reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordRequest)

    @POST("users/send-verification")
    suspend fun sendVerification(@Body body: EmailRequest): CodeResponse
}

interface HomeService {
    @GET("homes")
    suspend fun getAll(): List<Home>

    @GET("homes/{id}")
    suspend fun getById(@Path("id") id: String): Home

    @POST("homes")
    suspend fun create(@Body body: HomeCreateRequest): Home

    @PUT("homes/{id}")
    suspend fun update(@Path("id") id: String, @Body body: HomeUpdateRequest): Home

    @DELETE("homes/{id}")
    suspend fun delete(@Path("id") id: String): JsonElement

    @GET("homes/{homeId}/rooms")
    suspend fun getRooms(@Path("homeId") homeId: String): List<Room>

    @POST("homes/{homeId}/share")
    suspend fun share(@Path("homeId") homeId: String, @Body body: HomeShareRequest): List<HomeSharedUser>

    @HTTP(method = "DELETE", path = "homes/{homeId}/share", hasBody = true)
    suspend fun unshare(@Path("homeId") homeId: String, @Body body: HomeShareRequest)
}

interface RoomService {
    @POST("rooms")
    suspend fun create(@Body body: RoomCreateRequest): Room

    @PUT("rooms/{id}")
    suspend fun update(@Path("id") id: String, @Body body: RoomUpdateRequest): Room

    @DELETE("rooms/{id}")
    suspend fun delete(@Path("id") id: String): JsonElement

    @POST("rooms/{roomId}/devices/{deviceId}")
    suspend fun addDevice(@Path("roomId") roomId: String, @Path("deviceId") deviceId: String): Device

    @DELETE("rooms/devices/{deviceId}")
    suspend fun removeDevice(@Path("deviceId") deviceId: String): Device
}

interface DeviceService {
    @GET("devices")
    suspend fun getAll(): List<Device>

    @POST("devices")
    suspend fun create(@Body body: DeviceCreateRequest): Device

    @PUT("devices/{id}")
    suspend fun update(@Path("id") id: String, @Body body: DeviceUpdateRequest): Device

    @DELETE("devices/{id}")
    suspend fun delete(@Path("id") id: String): JsonElement

    /**
     * Executes an action on a device. Params travel as a positional JSON
     * array, which is the format preferred by the API. The return value
     * varies per action (boolean, string or number), so it is kept raw.
     */
    @PATCH("devices/{id}/{action}")
    suspend fun executeAction(
        @Path("id") id: String,
        @Path("action") action: String,
        @Body params: JsonArray,
    ): JsonElement
}

interface DeviceTypeService {
    @GET("devicetypes")
    suspend fun getAll(): List<DeviceType>
}

interface RoutineService {
    @GET("routines")
    suspend fun getAll(): List<Routine>

    @POST("routines")
    suspend fun create(@Body body: RoutineUpsertRequest): Routine

    @PUT("routines/{id}")
    suspend fun update(@Path("id") id: String, @Body body: RoutineUpsertRequest): Routine

    @DELETE("routines/{id}")
    suspend fun delete(@Path("id") id: String): JsonElement

    @PATCH("routines/{id}/execute")
    suspend fun execute(@Path("id") id: String, @Body body: RequestBody): List<RoutineActionResult>
}
