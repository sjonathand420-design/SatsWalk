package com.example.network

import retrofit2.http.Body
import retrofit2.http.POST

// API Request/Response Data Classes

data class RegisterRequest(
    val deviceId: String,
    val alias: String = ""
)

data class RegisterResponse(
    val deviceId: String,
    val level: Int,
    val currentPoints: Long,
    val lifetimePoints: Long,
    val withdrawalLimitSats: Int,
    val currentSteps: Int,
    val status: String,
    val message: String
)

data class UpgradeLevelRequest(
    val deviceId: String
)

data class UpgradeLimitRequest(
    val deviceId: String
)

data class UpgradeResponse(
    val success: Boolean,
    val level: Int,
    val withdrawalLimitSats: Int,
    val currentPoints: Long,
    val message: String
)

data class ClaimStepsRequest(
    val deviceId: String,
    val milestone: Int,
    val watchAd: Boolean,
    val hmacVerify: String // Secure SHA-256 HMAC signature of parameters
)

data class ClaimStepsResponse(
    val success: Boolean,
    val pointsAwarded: Int,
    val currentPoints: Long,
    val message: String
)

data class GameWinRequest(
    val deviceId: String,
    val watchAd: Boolean,
    val timestamp: Long,
    val clientToken: String // Verification token generated on the client
)

data class GameWinResponse(
    val success: Boolean,
    val pointsAwarded: Int,
    val currentPoints: Long,
    val message: String
)

data class CashoutRequest(
    val deviceId: String,
    val invoice: String,
    val satoshis: Int, // Number of satoshis to cashout
    val dateString: String, // Date check
    val usdPrice: Double,
    val cadPrice: Double
)

data class CashoutResponse(
    val success: Boolean,
    val transactionId: String,
    val satoshisWithdrawn: Int,
    val remainingSatsLimit: Int,
    val currentPoints: Long,
    val message: String
)

data class UpgradeStepLimitRequest(
    val deviceId: String
)

data class UpgradeStepLimitResponse(
    val success: Boolean,
    val maxClaimableSteps: Int,
    val currentPoints: Long,
    val message: String
)

// Retrofit API Service Interface
interface SatsWalkApiService {
    @POST("v1/user/register")
    suspend fun registerDevice(@Body request: RegisterRequest): RegisterResponse

    @POST("v1/user/upgrade-level")
    suspend fun upgradeLevel(@Body request: UpgradeLevelRequest): UpgradeResponse

    @POST("v1/user/upgrade-limit")
    suspend fun upgradeLimit(@Body request: UpgradeLimitRequest): UpgradeResponse

    @POST("v1/user/upgrade-step-limit")
    suspend fun upgradeStepLimit(@Body request: UpgradeStepLimitRequest): UpgradeStepLimitResponse

    @POST("v1/steps/claim")
    suspend fun claimStepsMilestone(@Body request: ClaimStepsRequest): ClaimStepsResponse

    @POST("v1/game/win")
    suspend fun reportGameWin(@Body request: GameWinRequest): GameWinResponse

    @POST("v1/cashout")
    suspend fun requestCashout(@Body request: CashoutRequest): CashoutResponse

    @POST("v1/auth/create-temp-code")
    suspend fun createTempCode(@Body request: CreateTempCodeRequest): CreateTempCodeResponse

    @POST("v1/auth/link-device")
    suspend fun linkDevice(@Body request: LinkDeviceRequest): SessionResponse

    @POST("v1/auth/get-linked-devices")
    suspend fun getLinkedDevices(@Body request: LinkedDevicesRequest): LinkedDevicesResponse

    @POST("v1/auth/unlink-device")
    suspend fun unlinkDevice(@Body request: UnlinkDeviceRequest): SessionResponse
}

// Linked devices api schemas
data class CreateTempCodeRequest(
    val deviceId: String
)

data class CreateTempCodeResponse(
    val success: Boolean,
    val code: String,
    val expiresAt: Long,
    val message: String
)

data class LinkDeviceRequest(
    val deviceId: String,
    val alias: String,
    val code: String
)

data class SessionResponse(
    val success: Boolean,
    val message: String
)

data class LinkedDevicesRequest(
    val deviceId: String
)

data class LinkedDeviceModel(
    val deviceId: String,
    val alias: String,
    val linkedAt: Long
)

data class LinkedDevicesResponse(
    val success: Boolean,
    val devices: List<LinkedDeviceModel>,
    val message: String
)

data class UnlinkDeviceRequest(
    val currentDeviceId: String,
    val targetDeviceId: String
)

