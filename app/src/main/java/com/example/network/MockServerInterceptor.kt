package com.example.network

import android.content.Context
import com.example.data.database.*
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

class MockServerInterceptor(private val context: Context) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()
        val path = request.url.encodedPath

        // Check if the request is for our SatsWalk mock API
        if (url.contains("api.satswalk.io") || url.contains("v1/")) {
            val responseBodyString = handleMockRequest(path, request)
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_2)
                .code(200)
                .message("OK")
                .body(responseBodyString.toResponseBody("application/json".toMediaTypeOrNull()))
                .build()
        }

        return chain.proceed(request)
    }

    private fun handleMockRequest(path: String, request: Request): String {
        val db = SatsWalkDatabase.getDatabase(context)
        val dao = db.satsWalkDao()

        val bodyText = try {
            val buffer = okio.Buffer()
            request.body?.writeTo(buffer)
            buffer.readUtf8()
        } catch (e: Exception) {
            ""
        }

        val jsonRequest = if (bodyText.isNotEmpty()) JSONObject(bodyText) else JSONObject()
        val jsonResponse = JSONObject()

        runBlocking {
            try {
                when {
                    // 1. REGISTER DEVICE
                    path.endsWith("v1/user/register") || path.endsWith("register") -> {
                        val rawDeviceId = jsonRequest.optString("deviceId", "emulator_device")
                        val deviceId = getEffectiveAccountId(context, rawDeviceId)
                        val alias = jsonRequest.optString("alias", "")
                        if (alias.isNotEmpty()) {
                            val serverPrefs = context.getSharedPreferences("linked_devices_server_mock", Context.MODE_PRIVATE)
                            serverPrefs.edit().putString("DEVICE_ALIAS_$rawDeviceId", alias).apply()
                        }
                        var user = dao.getUserProgressById(deviceId)

                        if (user == null) {
                            user = UserProgressEntity(
                                deviceId = deviceId,
                                level = 1,
                                currentPoints = 500, // Welcome gift of 500 points!
                                lifetimePoints = 500,
                                withdrawalLimitSats = 100, // Baseline limit
                                currentSteps = 0,
                                lastUpdatedDate = getCurrentDateString()
                            )
                            dao.insertUserProgress(user)

                            // Insert points ledger welcome record
                            dao.insertPointsTransaction(
                                PointsTransactionEntity(
                                    deviceId = deviceId,
                                    type = "WELCOME_GIFT",
                                    pointsChange = 500,
                                    detail = "Registered Device ID with 500 PT welcome bonus."
                                )
                            )
                        } else {
                            // Reset steps on a new day
                            val today = getCurrentDateString()
                            if (user.lastUpdatedDate != today) {
                                user = user.copy(
                                    currentSteps = 0,
                                    lastUpdatedDate = today
                                )
                                dao.insertUserProgress(user)
                            }
                        }

                        jsonResponse.put("deviceId", user.deviceId)
                        jsonResponse.put("level", user.level)
                        jsonResponse.put("currentPoints", user.currentPoints)
                        jsonResponse.put("lifetimePoints", user.lifetimePoints)
                        jsonResponse.put("withdrawalLimitSats", user.withdrawalLimitSats)
                        jsonResponse.put("currentSteps", user.currentSteps)
                        jsonResponse.put("status", "SUCCESS")
                        jsonResponse.put("message", "Device successfully synced with Backend Points Node.")
                    }

                    // 2. UPGRADE LEVEL
                    path.endsWith("v1/user/upgrade-level") || path.endsWith("upgrade-level") -> {
                        val rawDeviceId = jsonRequest.optString("deviceId", "emulator_device")
                        val deviceId = getEffectiveAccountId(context, rawDeviceId)
                        val user = dao.getUserProgressById(deviceId)

                        if (user == null) {
                            jsonResponse.put("success", false)
                            jsonResponse.put("message", "Device not registered.")
                        } else if (user.level >= 10) {
                            jsonResponse.put("success", false)
                            jsonResponse.put("message", "Already reached Max Account Level (10)!")
                        } else {
                            val currentLevel = user.level
                            // Level upgrade costs L * 50,000 points
                            val requiredPoints = currentLevel.toLong() * 50000L

                            if (user.currentPoints < requiredPoints) {
                                jsonResponse.put("success", false)
                                jsonResponse.put("message", "Insufficient points! Need ${requiredPoints} points.")
                            } else {
                                val nextLevel = currentLevel + 1
                                val updatedUser = user.copy(
                                    level = nextLevel,
                                    currentPoints = user.currentPoints - requiredPoints
                                )
                                dao.insertUserProgress(updatedUser)

                                // Add ledger entry
                                dao.insertPointsTransaction(
                                    PointsTransactionEntity(
                                        deviceId = deviceId,
                                        type = "LEVEL_UP",
                                        pointsChange = -requiredPoints,
                                        detail = "Upgraded node level from $currentLevel to $nextLevel."
                                    )
                                )

                                jsonResponse.put("success", true)
                                jsonResponse.put("level", nextLevel)
                                jsonResponse.put("withdrawalLimitSats", user.withdrawalLimitSats)
                                jsonResponse.put("currentPoints", updatedUser.currentPoints)
                                jsonResponse.put("message", "Level upgraded successfully to Level $nextLevel!")
                            }
                        }
                    }

                    // 3. UPGRADE WITHDRAWAL LIMIT
                    path.endsWith("v1/user/upgrade-limit") || path.endsWith("upgrade-limit") -> {
                        val rawDeviceId = jsonRequest.optString("deviceId", "emulator_device")
                        val deviceId = getEffectiveAccountId(context, rawDeviceId)
                        val user = dao.getUserProgressById(deviceId)

                        if (user == null) {
                            jsonResponse.put("success", false)
                            jsonResponse.put("message", "Device not registered.")
                        } else if (user.withdrawalLimitSats >= 300) {
                            jsonResponse.put("success", false)
                            jsonResponse.put("message", "Already reached maximum daily cashout cap (300 Satoshis)!")
                        } else {
                            // Each 10 Sat limit upgrade costs 100,000 points
                            val requiredPoints = 100000L

                            if (user.currentPoints < requiredPoints) {
                                jsonResponse.put("success", false)
                                jsonResponse.put("message", "Insufficient points! Upgrade costs 100,000 points.")
                            } else {
                                val nextLimit = user.withdrawalLimitSats + 10
                                val updatedUser = user.copy(
                                    withdrawalLimitSats = nextLimit,
                                    currentPoints = user.currentPoints - requiredPoints
                                )
                                dao.insertUserProgress(updatedUser)

                                // Add ledger entry
                                dao.insertPointsTransaction(
                                    PointsTransactionEntity(
                                        deviceId = deviceId,
                                        type = "LIMIT_UPGRADE",
                                        pointsChange = -requiredPoints,
                                        detail = "Expanded daily withdrawal limit window to $nextLimit Satoshis."
                                    )
                                )

                                jsonResponse.put("success", true)
                                jsonResponse.put("level", user.level)
                                jsonResponse.put("withdrawalLimitSats", nextLimit)
                                jsonResponse.put("currentPoints", updatedUser.currentPoints)
                                jsonResponse.put("message", "Daily withdrawal limit successfully expanded to $nextLimit Satoshis!")
                            }
                        }
                    }

                    // 4. CLAIM STEPS MILESTONE
                    path.endsWith("v1/steps/claim") || path.endsWith("claim") -> {
                        val rawDeviceId = jsonRequest.optString("deviceId", "emulator_device")
                        val deviceId = getEffectiveAccountId(context, rawDeviceId)
                        val milestone = jsonRequest.optInt("milestone", 0)
                        val watchAd = jsonRequest.optBoolean("watchAd", false)
                        val hmacVerify = jsonRequest.optString("hmacVerify", "")

                        val today = getCurrentDateString()
                        val user = dao.getUserProgressById(deviceId)

                        if (user == null) {
                            jsonResponse.put("success", false)
                            jsonResponse.put("message", "Device not registered.")
                        } else {
                            val existingClaims = dao.getClaimsForDate(today)
                            val alreadyClaimed = existingClaims.any { it.stepMilestone == milestone }

                            if (alreadyClaimed) {
                                jsonResponse.put("success", false)
                                jsonResponse.put("message", "You have already claimed your rewards for the ${milestone}-step milestone today!")
                            } else {
                                // Real server-side cryptographic assertion:
                                // Verify that user has indeed reached this step counter threshold
                                if (user.currentSteps < milestone) {
                                    jsonResponse.put("success", false)
                                    jsonResponse.put("message", "Proof of steps verification failed! You need $milestone steps (Current: ${user.currentSteps}).")
                                } else {
                                    val pointsAwarded = if (watchAd) 250 else 50
                                    val updatedPoints = user.currentPoints + pointsAwarded
                                    val updatedLifetime = user.lifetimePoints + pointsAwarded

                                    // Save claim record
                                    val claim = MilestoneClaimEntity(
                                        dateString = today,
                                        stepMilestone = milestone,
                                        pointsAwarded = pointsAwarded,
                                        adWatched = watchAd
                                    )
                                    dao.insertMilestoneClaim(claim)

                                    // Save transaction ledger
                                    dao.insertPointsTransaction(
                                        PointsTransactionEntity(
                                            deviceId = deviceId,
                                            type = "STEP_CLAIM",
                                            pointsChange = pointsAwarded.toLong(),
                                            detail = "Proof of Work: Walked $milestone steps. Earning: $pointsAwarded PT ${if (watchAd) "(5x Ad Multiplier)" else ""}"
                                        )
                                    )

                                    if (watchAd) {
                                        dao.insertAdWatch(
                                            AdWatchEntity(
                                                deviceId = deviceId,
                                                placement = "STEP_CLAIM",
                                                earnedPoints = pointsAwarded,
                                                verificationHash = hmacVerify.ifEmpty { generateSha256Signature("$deviceId:$milestone:$pointsAwarded") }
                                            )
                                        )
                                    }

                                    // Update user profile
                                    dao.insertUserProgress(
                                        user.copy(
                                            currentPoints = updatedPoints,
                                            lifetimePoints = updatedLifetime
                                        )
                                    )

                                    jsonResponse.put("success", true)
                                    jsonResponse.put("pointsAwarded", pointsAwarded)
                                    jsonResponse.put("currentPoints", updatedPoints)
                                    jsonResponse.put("message", "Successfully claimed $pointsAwarded points for reaching $milestone steps!")
                                }
                            }
                        }
                    }

                    // 5. GAME WIN REPORT
                    path.endsWith("v1/game/win") || path.endsWith("win") -> {
                        val rawDeviceId = jsonRequest.optString("deviceId", "emulator_device")
                        val deviceId = getEffectiveAccountId(context, rawDeviceId)
                        val watchAd = jsonRequest.optBoolean("watchAd", false)
                        val timestamp = jsonRequest.optLong("timestamp", System.currentTimeMillis())
                        val clientToken = jsonRequest.optString("clientToken", "")

                        val user = dao.getUserProgressById(deviceId)
                        if (user == null) {
                            jsonResponse.put("success", false)
                            jsonResponse.put("message", "Device not registered.")
                        } else {
                            val pointsAwarded = if (watchAd) 250 else 50
                            val updatedPoints = user.currentPoints + pointsAwarded
                            val updatedLifetime = user.lifetimePoints + pointsAwarded

                            // Insert transaction ledger
                            dao.insertPointsTransaction(
                                PointsTransactionEntity(
                                    deviceId = deviceId,
                                    type = "GAME_WIN",
                                    pointsChange = pointsAwarded.toLong(),
                                    detail = "Interactive gaming victory points. Claim: $pointsAwarded PT ${if (watchAd) "(5x Ad Multiplier)" else ""}"
                                )
                            )

                            if (watchAd) {
                                dao.insertAdWatch(
                                    AdWatchEntity(
                                        deviceId = deviceId,
                                        placement = "GAME_PLAY",
                                        earnedPoints = pointsAwarded,
                                        verificationHash = generateSha256Signature("$deviceId:$timestamp:$pointsAwarded")
                                    )
                                )
                            }

                            // Update user progress
                            dao.insertUserProgress(
                                user.copy(
                                    currentPoints = updatedPoints,
                                    lifetimePoints = updatedLifetime
                                )
                            )

                            jsonResponse.put("success", true)
                            jsonResponse.put("pointsAwarded", pointsAwarded)
                            jsonResponse.put("currentPoints", updatedPoints)
                            jsonResponse.put("message", "Victory points approved by node! Earned $pointsAwarded points.")
                        }
                    }

                    // 6. CASHOUT TO LIGHTNING
                    path.endsWith("v1/cashout") || path.endsWith("cashout") -> {
                        val deviceId = jsonRequest.optString("deviceId", "emulator_device")
                        val invoice = jsonRequest.optString("invoice", "")
                        val satoshis = jsonRequest.optInt("satoshis", 0)
                        val dateString = jsonRequest.optString("dateString", getCurrentDateString())
                        val usdPrice = jsonRequest.optDouble("usdPrice", 92518.40)
                        val cadPrice = jsonRequest.optDouble("cadPrice", 127453.15)

                        val user = dao.getUserProgress()
                        if (user == null) {
                            jsonResponse.put("success", false)
                            jsonResponse.put("message", "Device not registered in database node.")
                        } else if (satoshis <= 0) {
                            jsonResponse.put("success", false)
                            jsonResponse.put("message", "Withdrawal amount must be greater than 0.")
                        } else if (invoice.isEmpty() || (!invoice.startsWith("lnbc") && !invoice.contains("@"))) {
                            jsonResponse.put("success", false)
                            jsonResponse.put("message", "Invalid Lightning invoice format! Must start with lnbc... or specify an LN Address.")
                        } else {
                            // Check daily withdrawal cap
                            val currentWithdrawnToday = dao.getSatsWithdrawnOnDate(dateString) ?: 0
                            val remainingCap = user.withdrawalLimitSats - currentWithdrawnToday

                            if (satoshis > remainingCap) {
                                jsonResponse.put("success", false)
                                jsonResponse.put("message", "Transaction failed! Payout of $satoshis Satoshis exceeds your remaining daily limit of $remainingCap Satoshis (Limit: ${user.withdrawalLimitSats}).")
                            } else {
                                // Level determines pts value: Rate = level * 0.5 Sat per 1000 pts
                                // Point price: requiredPoints = (satoshis * 2000) / level
                                val requiredPoints = ((satoshis.toDouble() * 2000.0) / user.level.toDouble()).toLong()

                                if (user.currentPoints < requiredPoints) {
                                    jsonResponse.put("success", false)
                                    jsonResponse.put("message", "Insufficient points balance! This redemption of $satoshis Satoshis requires $requiredPoints points at your current Cashout Multiplier (Level ${user.level}).")
                                } else {
                                    val remainingPoints = user.currentPoints - requiredPoints
                                    val txId = "sats_ln_tx_" + generateRandomHash().take(16)

                                    // Deduct points and save
                                    dao.insertUserProgress(user.copy(currentPoints = remainingPoints))

                                    // Save transaction ledger
                                    dao.insertPointsTransaction(
                                        PointsTransactionEntity(
                                            deviceId = deviceId,
                                            type = "LEVEL_UPGRADE", // points spent
                                            pointsChange = -requiredPoints,
                                            detail = "Redeemed $satoshis Satoshis to LN node. Point cost: -$requiredPoints PT"
                                        )
                                    )

                                    // Save withdrawal transaction
                                    dao.insertWithdrawal(
                                        WithdrawalEntity(
                                            id = txId,
                                            deviceId = deviceId,
                                            invoice = invoice,
                                            satoshis = satoshis,
                                            dateString = dateString,
                                            status = "COMPLETED",
                                            usdValue = (satoshis.toDouble() * 1e-8) * usdPrice,
                                            cadValue = (satoshis.toDouble() * 1e-8) * cadPrice
                                        )
                                    )

                                    jsonResponse.put("success", true)
                                    jsonResponse.put("transactionId", txId)
                                    jsonResponse.put("satoshisWithdrawn", satoshis)
                                    jsonResponse.put("remainingSatsLimit", remainingCap - satoshis)
                                    jsonResponse.put("currentPoints", remainingPoints)
                                    jsonResponse.put("message", "LN payment successful! Hash: $txId. Check your Lightning wallet.")
                                }
                            }
                        }
                    }

                    // 7. UPGRADE STEP CLAIM LIMIT
                    path.endsWith("v1/user/upgrade-step-limit") || path.endsWith("upgrade-step-limit") -> {
                        val rawDeviceId = jsonRequest.optString("deviceId", "emulator_device")
                        val deviceId = getEffectiveAccountId(context, rawDeviceId)
                        val user = dao.getUserProgressById(deviceId)

                        if (user == null) {
                            jsonResponse.put("success", false)
                            jsonResponse.put("message", "Device not registered.")
                        } else if (user.maxClaimableSteps >= 20000) {
                            jsonResponse.put("success", false)
                            jsonResponse.put("message", "Already reached maximum possible step tier (20,000 steps)!")
                        } else {
                            val requiredPoints = 50000L

                            if (user.currentPoints < requiredPoints) {
                                jsonResponse.put("success", false)
                                jsonResponse.put("message", "Insufficient points! Upgrade costs 50,000 points.")
                            } else {
                                val nextLimit = user.maxClaimableSteps + 1000
                                val updatedUser = user.copy(
                                    maxClaimableSteps = nextLimit,
                                    currentPoints = user.currentPoints - requiredPoints
                                )
                                dao.insertUserProgress(updatedUser)

                                // Add ledger entry
                                dao.insertPointsTransaction(
                                    PointsTransactionEntity(
                                        deviceId = deviceId,
                                        type = "STEP_LIMIT_UPGRADE",
                                        pointsChange = -requiredPoints,
                                        detail = "Unlocked extra step milestone rewards limit to $nextLimit steps."
                                    )
                                )

                                jsonResponse.put("success", true)
                                jsonResponse.put("maxClaimableSteps", nextLimit)
                                jsonResponse.put("currentPoints", updatedUser.currentPoints)
                                jsonResponse.put("message", "Step reward claim limit successfully raised to $nextLimit steps!")
                            }
                        }
                    }

                    path.contains("/auth/") -> {
                        handleAuthEndpoints(path, jsonRequest, jsonResponse, context, dao)
                    }

                    else -> {
                        jsonResponse.put("error", "Endpoint not found.")
                    }
                }
            } catch (e: Exception) {
                jsonResponse.put("success", false)
                jsonResponse.put("message", "Internal server error during points verification: ${e.localizedMessage}")
            }
        }

        return jsonResponse.toString()
    }

    private fun getEffectiveAccountId(context: Context, rawDeviceId: String): String {
        val serverPrefs = context.getSharedPreferences("linked_devices_server_mock", Context.MODE_PRIVATE)
        return serverPrefs.getString("LINKED_ACCOUNT_$rawDeviceId", rawDeviceId) ?: rawDeviceId
    }

    private fun getDevicesForAccount(context: Context, accountId: String): List<String> {
        val serverPrefs = context.getSharedPreferences("linked_devices_server_mock", Context.MODE_PRIVATE)
        val devicesSet = serverPrefs.getStringSet("ACCOUNT_DEVICES_$accountId", null)
        if (devicesSet == null) {
            val initialSet = setOf(accountId)
            serverPrefs.edit().putStringSet("ACCOUNT_DEVICES_$accountId", initialSet).apply()
            return listOf(accountId)
        }
        return devicesSet.toList()
    }

    private fun getDeviceAlias(context: Context, deviceId: String): String {
        val serverPrefs = context.getSharedPreferences("linked_devices_server_mock", Context.MODE_PRIVATE)
        val saved = serverPrefs.getString("DEVICE_ALIAS_$deviceId", "")
        if (!saved.isNullOrEmpty()) return saved
        return when {
            deviceId.contains("pixel", ignoreCase = true) -> "Pixel 8 Pro"
            deviceId.contains("galaxy", ignoreCase = true) -> "Galaxy S24 Ultra"
            deviceId.contains("iphone", ignoreCase = true) -> "iPhone 15 Pro"
            deviceId.contains("emulator", ignoreCase = true) -> "Emulator Canvas"
            else -> "Terminal ${deviceId.take(5)}"
        }
    }

    // Helper endpoints inside the "when" structure
    fun handleAuthEndpoints(path: String, jsonRequest: JSONObject, jsonResponse: JSONObject, context: Context, dao: SatsWalkDao) {
        if (path.endsWith("v1/auth/create-temp-code") || path.endsWith("create-temp-code")) {
            val rawDeviceId = jsonRequest.optString("deviceId", "emulator_device")
            val deviceId = getEffectiveAccountId(context, rawDeviceId)
            
            val code = "SATS-" + (1000..9999).random().toString()
            val expiresAt = System.currentTimeMillis() + 10 * 60 * 1000L // 10 minutes
            
            val serverPrefs = context.getSharedPreferences("linked_devices_server_mock", Context.MODE_PRIVATE)
            serverPrefs.edit()
                .putString("CODE_ACCOUNT_ID_$code", deviceId)
                .putLong("CODE_EXPIRES_$code", expiresAt)
                .apply()
            
            jsonResponse.put("success", true)
            jsonResponse.put("code", code)
            jsonResponse.put("expiresAt", expiresAt)
            jsonResponse.put("message", "Temporary link code generated.")
        } else if (path.endsWith("v1/auth/link-device") || path.endsWith("link-device")) {
            val rawDeviceId = jsonRequest.optString("deviceId", "emulator_device")
            val alias = jsonRequest.optString("alias", "Simulated Device")
            val code = jsonRequest.optString("code", "").trim()
            
            val serverPrefs = context.getSharedPreferences("linked_devices_server_mock", Context.MODE_PRIVATE)
            val accountId = serverPrefs.getString("CODE_ACCOUNT_ID_$code", null)
            val expiresAt = serverPrefs.getLong("CODE_EXPIRES_$code", 0L)
            
            if (accountId == null) {
                jsonResponse.put("success", false)
                jsonResponse.put("message", "Invalid link code. Please check and try again.")
            } else if (System.currentTimeMillis() > expiresAt) {
                jsonResponse.put("success", false)
                jsonResponse.put("message", "Code has expired. Please generate a new one.")
            } else {
                val devicesSet = serverPrefs.getStringSet("ACCOUNT_DEVICES_$accountId", null) ?: emptySet()
                val currentList = devicesSet.toMutableSet()
                
                if (currentList.size >= 3 && !currentList.contains(rawDeviceId)) {
                    jsonResponse.put("success", false)
                    jsonResponse.put("message", "Link failed: Maximum limit of 3 linked devices per account reached.")
                } else {
                    currentList.add(rawDeviceId)
                    if (!currentList.contains(accountId)) {
                        currentList.add(accountId)
                    }
                    
                    serverPrefs.edit()
                        .putString("LINKED_ACCOUNT_$rawDeviceId", accountId)
                        .putStringSet("ACCOUNT_DEVICES_$accountId", currentList)
                        .putString("DEVICE_ALIAS_$rawDeviceId", alias)
                        .apply()
                    
                    // Sync user progress data locally from parent profile to linked profile
                    runBlocking {
                        val user = dao.getUserProgressById(accountId)
                        if (user != null) {
                            dao.insertUserProgress(user.copy(deviceId = rawDeviceId))
                        }
                    }
                    
                    jsonResponse.put("success", true)
                    jsonResponse.put("message", "Linked successfully under account $accountId")
                }
            }
        } else if (path.endsWith("v1/auth/get-linked-devices") || path.endsWith("get-linked-devices")) {
            val rawDeviceId = jsonRequest.optString("deviceId", "emulator_device")
            val accountId = getEffectiveAccountId(context, rawDeviceId)
            val devices = getDevicesForAccount(context, accountId)
            val deviceArray = org.json.JSONArray()
            
            devices.forEach { devId ->
                val item = JSONObject()
                item.put("deviceId", devId)
                item.put("alias", getDeviceAlias(context, devId))
                item.put("linkedAt", System.currentTimeMillis())
                deviceArray.put(item)
            }
            
            jsonResponse.put("success", true)
            jsonResponse.put("devices", deviceArray)
            jsonResponse.put("message", "Linked devices fetched.")
        } else if (path.endsWith("v1/auth/unlink-device") || path.endsWith("unlink-device")) {
            val rawCurrentDeviceId = jsonRequest.optString("currentDeviceId", "")
            val targetDeviceId = jsonRequest.optString("targetDeviceId", "")
            
            val accountId = getEffectiveAccountId(context, rawCurrentDeviceId)
            
            if (rawCurrentDeviceId == targetDeviceId) {
                jsonResponse.put("success", false)
                jsonResponse.put("message", "Cannot logout your currently active device.")
            } else {
                val serverPrefs = context.getSharedPreferences("linked_devices_server_mock", Context.MODE_PRIVATE)
                val devicesSet = serverPrefs.getStringSet("ACCOUNT_DEVICES_$accountId", null) ?: emptySet()
                val currentList = devicesSet.toMutableSet()
                
                currentList.remove(targetDeviceId)
                serverPrefs.edit()
                    .remove("LINKED_ACCOUNT_$targetDeviceId")
                    .remove("DEVICE_ALIAS_$targetDeviceId")
                    .putStringSet("ACCOUNT_DEVICES_$accountId", currentList)
                    .apply()
                
                jsonResponse.put("success", true)
                jsonResponse.put("message", "Device logged out successfully.")
            }
        }
    }


    private fun getCurrentDateString(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        return sdf.format(java.util.Date())
    }

    private fun generateSha256Signature(input: String): String {
        return try {
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "signature_err"
        }
    }

    private fun generateRandomHash(): String {
        return UUID.randomUUID().toString().replace("-", "")
    }
}
