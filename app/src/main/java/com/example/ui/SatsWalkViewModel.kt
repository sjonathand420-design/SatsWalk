package com.example.ui

import android.annotation.SuppressLint
import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.*
import com.example.network.*
import com.example.sensor.PedometerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class SatsWalkViewModel(application: Application) : AndroidViewModel(application) {

    @SuppressLint("HardwareIds")
    val deviceId: String = Settings.Secure.getString(
        application.contentResolver,
        Settings.Secure.ANDROID_ID
    ) ?: "emulator_uid_98216"

    private val db = SatsWalkDatabase.getDatabase(application)
    private val dao = db.satsWalkDao()
    private val apiService = SatsWalkApiClient.getService(application)
    private val pedometerManager = PedometerManager(application)

    // UI State variables
    private val _isRegistering = MutableStateFlow(false)
    val isRegistering: StateFlow<Boolean> = _isRegistering.asStateFlow()

    private val _btcPriceUsd = MutableStateFlow(92518.40)
    val btcPriceUsd: StateFlow<Double> = _btcPriceUsd.asStateFlow()

    private val _btcPriceCad = MutableStateFlow(127453.15)
    val btcPriceCad: StateFlow<Double> = _btcPriceCad.asStateFlow()

    private val _btcYesterday1PmUsd = MutableStateFlow<Double?>(null)
    val btcYesterday1PmUsd: StateFlow<Double?> = _btcYesterday1PmUsd.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val prefs = application.getSharedPreferences("predict_game_prefs", android.content.Context.MODE_PRIVATE)

    private val _predTargetDate = MutableStateFlow<String?>(null)
    val predTargetDate: StateFlow<String?> = _predTargetDate.asStateFlow()

    private val _predChoice = MutableStateFlow<String?>(null) // "HIGHER" or "LOWER"
    val predChoice: StateFlow<String?> = _predChoice.asStateFlow()

    private val _predBaselinePrice = MutableStateFlow<Double>(0.0)
    val predBaselinePrice: StateFlow<Double> = _predBaselinePrice.asStateFlow()

    private val _predStatus = MutableStateFlow<String>("NONE") // "NONE", "PENDING", "RESOLVED_WIN", "RESOLVED_LOSE", "CLAIMED"
    val predStatus: StateFlow<String> = _predStatus.asStateFlow()

    private val _predResolvedPrice = MutableStateFlow<Double>(0.0)
    val predResolvedPrice: StateFlow<Double> = _predResolvedPrice.asStateFlow()

    private val _isAdPlaying = MutableStateFlow(false)
    val isAdPlaying: StateFlow<Boolean> = _isAdPlaying.asStateFlow()

    private val _adPlacement = MutableStateFlow<String?>(null) // "STEP_CLAIM" or "GAME_PLAY"
    val adPlacement: StateFlow<String?> = _adPlacement.asStateFlow()

    private val _activeMilestoneAd = MutableStateFlow<Int?>(null)
    val activeMilestoneAd: StateFlow<Int?> = _activeMilestoneAd.asStateFlow()

    // Spinning Wheel state variables
    private val _wheelUpgradePurchased = MutableStateFlow(false)
    val wheelUpgradePurchased: StateFlow<Boolean> = _wheelUpgradePurchased.asStateFlow()

    private val _wheelSpinsRemainingToday = MutableStateFlow(10)
    val wheelSpinsRemainingToday: StateFlow<Int> = _wheelSpinsRemainingToday.asStateFlow()

    private val _wheelMaxDailySpins = MutableStateFlow(10)
    val wheelMaxDailySpins: StateFlow<Int> = _wheelMaxDailySpins.asStateFlow()

    // Dice Game state variables
    private val _dicePlaysUsedOfToday = MutableStateFlow(0)
    val dicePlaysUsedOfToday: StateFlow<Int> = _dicePlaysUsedOfToday.asStateFlow()

    // Coin Flip state variables
    private val _coinFlipPlaysUsedOfToday = MutableStateFlow(0)
    val coinFlipPlaysUsedOfToday: StateFlow<Int> = _coinFlipPlaysUsedOfToday.asStateFlow()

    // Legal Disclaimer agreement state
    private val _isDisclaimerAccepted = MutableStateFlow(false)
    val isDisclaimerAccepted: StateFlow<Boolean> = _isDisclaimerAccepted.asStateFlow()

    private val _simulatedDeviceId = MutableStateFlow<String?>(null)
    val simulatedDeviceId: StateFlow<String?> = _simulatedDeviceId.asStateFlow()

    val activeDeviceId: String
        get() = _simulatedDeviceId.value ?: deviceId

    // Database reactive streams for the client ui
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val userProgress: StateFlow<UserProgressEntity?> = _simulatedDeviceId
        .flatMapLatest { simId ->
            dao.getUserProgressByIdFlow(simId ?: deviceId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val todayClaims: StateFlow<List<MilestoneClaimEntity>> = dao.getClaimsForDateFlow(getTodayDateString())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val withdrawals: StateFlow<List<WithdrawalEntity>> = _simulatedDeviceId
        .flatMapLatest { simId ->
            dao.getWithdrawalsByIdFlow(simId ?: deviceId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val pointTransactions: StateFlow<List<PointsTransactionEntity>> = _simulatedDeviceId
        .flatMapLatest { simId ->
            dao.getPointsTransactionsByIdFlow(simId ?: deviceId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dicePlaysRemainingToday: StateFlow<Int> = combine(userProgress, _dicePlaysUsedOfToday) { progress, used ->
        val steps = progress?.currentSteps ?: 0
        if (steps >= 3000) {
            (3 - used).coerceAtLeast(0)
        } else {
            0
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        pedometerManager.startTracking()
        syncUserProfile()
        fetchBtcPrices()
        loadPredictionData()
        loadWheelData()
        loadDiceData()
        loadCoinFlipData()
        loadDisclaimerState()
    }

    fun fetchBtcPrices() {
        viewModelScope.launch {
            val prices = fetchRealBtcPrices()
            if (prices != null) {
                _btcPriceUsd.value = prices.first
                _btcPriceCad.value = prices.second
            }
            val yesterdayPrice = fetchHistoricalYesterday1PmPrice()
            if (yesterdayPrice != null) {
                _btcYesterday1PmUsd.value = yesterdayPrice
            } else {
                if (_btcYesterday1PmUsd.value == null && _btcPriceUsd.value > 0.0) {
                    _btcYesterday1PmUsd.value = _btcPriceUsd.value * 1.002 // reasonable default fallback
                }
            }
        }
    }

    private suspend fun fetchHistoricalYesterday1PmPrice(): Double? = withContext(Dispatchers.IO) {
        return@withContext try {
            val urlString = "https://min-api.cryptocompare.com/data/v2/histohour?fsym=BTC&tsym=USD&limit=72"
            val connection = URL(urlString).openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseText)
            if (json.optString("Response") == "Success") {
                val dataObj = json.getJSONObject("Data")
                val dataArray = dataObj.getJSONArray("Data")
                
                val tz = TimeZone.getTimeZone("America/New_York")
                val yesterdayCal = Calendar.getInstance(tz).apply {
                    add(Calendar.DAY_OF_YEAR, -1)
                }
                val yesterdayYear = yesterdayCal.get(Calendar.YEAR)
                val yesterdayDayOfYear = yesterdayCal.get(Calendar.DAY_OF_YEAR)
                
                var matchedPrice: Double? = null
                for (i in 0 until dataArray.length()) {
                    val item = dataArray.getJSONObject(i)
                    val timeSeconds = item.getLong("time")
                    val itemCal = Calendar.getInstance(tz).apply {
                        timeInMillis = timeSeconds * 1000L
                    }
                    if (itemCal.get(Calendar.YEAR) == yesterdayYear &&
                        itemCal.get(Calendar.DAY_OF_YEAR) == yesterdayDayOfYear &&
                        itemCal.get(Calendar.HOUR_OF_DAY) == 13) {
                        matchedPrice = item.getDouble("close")
                        break
                    }
                }
                
                if (matchedPrice == null && dataArray.length() > 0) {
                    for (i in 0 until dataArray.length()) {
                        val item = dataArray.getJSONObject(i)
                        val timeSeconds = item.getLong("time")
                        val itemCal = Calendar.getInstance(tz).apply {
                            timeInMillis = timeSeconds * 1000L
                        }
                        if (itemCal.get(Calendar.HOUR_OF_DAY) == 13) {
                            matchedPrice = item.getDouble("close")
                        }
                    }
                }
                matchedPrice
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun fetchRealBtcPrices(): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        return@withContext try {
            val urlString = "https://min-api.cryptocompare.com/data/price?fsym=BTC&tsyms=USD,CAD"
            val connection = URL(urlString).openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseText)
            val usd = json.getDouble("USD")
            val cad = json.getDouble("CAD")
            Pair(usd, cad)
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                val urlString2 = "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=usd,cad"
                val connection2 = URL(urlString2).openConnection() as java.net.HttpURLConnection
                connection2.connectTimeout = 5000
                connection2.readTimeout = 5000
                val responseText2 = connection2.inputStream.bufferedReader().use { it.readText() }
                val json2 = JSONObject(responseText2)
                val btcJson = json2.getJSONObject("bitcoin")
                val usd = btcJson.getDouble("usd")
                val cad = btcJson.getDouble("cad")
                Pair(usd, cad)
            } catch (ex: Exception) {
                ex.printStackTrace()
                try {
                    val urlString3 = "https://api.coinbase.com/v2/prices/BTC-USD/spot"
                    val responseText3 = URL(urlString3).readText()
                    val usd = JSONObject(responseText3).getJSONObject("data").getDouble("amount")

                    val urlString4 = "https://api.coinbase.com/v2/prices/BTC-CAD/spot"
                    val responseText4 = URL(urlString4).readText()
                    val cad = JSONObject(responseText4).getJSONObject("data").getDouble("amount")
                    Pair(usd, cad)
                } catch (ex2: Exception) {
                    ex2.printStackTrace()
                    null
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pedometerManager.stopTracking()
    }

    // 1. Sync User Profile from local nodes
    fun syncUserProfile() {
        viewModelScope.launch {
            _isRegistering.value = true
            _errorMessage.value = null
            try {
                val response = apiService.registerDevice(RegisterRequest(activeDeviceId))
                LogSync("Sync Successful: ${response.message}")
                
                var localProgress = dao.getUserProgressById(activeDeviceId)
                if (localProgress == null) {
                    localProgress = UserProgressEntity(
                        deviceId = response.deviceId,
                        level = response.level,
                        currentPoints = response.currentPoints,
                        lifetimePoints = response.lifetimePoints,
                        withdrawalLimitSats = response.withdrawalLimitSats,
                        currentSteps = response.currentSteps,
                        lastUpdatedDate = getTodayDateString()
                    )
                } else {
                    localProgress = localProgress.copy(
                        level = response.level,
                        currentPoints = response.currentPoints,
                        lifetimePoints = response.lifetimePoints,
                        withdrawalLimitSats = response.withdrawalLimitSats,
                        currentSteps = response.currentSteps
                    )
                }
                dao.insertUserProgress(localProgress)
            } catch (e: Exception) {
                _errorMessage.value = "Node Connection Offline: ${e.localizedMessage}. Operating in local fallback mode."
                // Seed local profile if first time
                val current = dao.getUserProgressById(activeDeviceId)
                if (current == null) {
                    dao.insertUserProgress(
                        UserProgressEntity(
                            deviceId = activeDeviceId,
                            level = 1,
                            currentPoints = 500,
                            lifetimePoints = 500,
                            withdrawalLimitSats = 100,
                            currentSteps = 0,
                            lastUpdatedDate = getTodayDateString()
                        )
                    )
                }
            } finally {
                _isRegistering.value = false
            }
        }
    }

    private val _tempCode = MutableStateFlow<CreateTempCodeResponse?>(null)
    val tempCode: StateFlow<CreateTempCodeResponse?> = _tempCode.asStateFlow()

    private val _isGeneratingTempCode = MutableStateFlow(false)
    val isGeneratingTempCode: StateFlow<Boolean> = _isGeneratingTempCode.asStateFlow()

    private val _linkedDevicesList = MutableStateFlow<List<LinkedDeviceModel>>(emptyList())
    val linkedDevicesList: StateFlow<List<LinkedDeviceModel>> = _linkedDevicesList.asStateFlow()

    private val _isLoadingDevices = MutableStateFlow(false)
    val isLoadingDevices: StateFlow<Boolean> = _isLoadingDevices.asStateFlow()

    fun setSimulatedDeviceId(id: String?) {
        _simulatedDeviceId.value = id
        _tempCode.value = null
        syncUserProfile()
        fetchLinkedDevices()
    }

    fun createTempCode() {
        viewModelScope.launch {
            _isGeneratingTempCode.value = true
            _successMessage.value = null
            _errorMessage.value = null
            try {
                val response = apiService.createTempCode(CreateTempCodeRequest(activeDeviceId))
                if (response.success) {
                    _tempCode.value = response
                    _successMessage.value = "Temporary link code generated: ${response.code}"
                } else {
                    _errorMessage.value = response.message
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create code: ${e.localizedMessage}"
            } finally {
                _isGeneratingTempCode.value = false
            }
        }
    }

    fun linkDevice(code: String, alias: String) {
        viewModelScope.launch {
            _successMessage.value = null
            _errorMessage.value = null
            try {
                val response = apiService.linkDevice(
                    LinkDeviceRequest(
                        deviceId = activeDeviceId,
                        alias = alias,
                        code = code
                    )
                )
                if (response.success) {
                    _successMessage.value = response.message
                    syncUserProfile()
                    fetchLinkedDevices()
                } else {
                    _errorMessage.value = response.message
                }
            } catch (e: Exception) {
                _errorMessage.value = "Link failed: ${e.localizedMessage}"
            }
        }
    }

    fun fetchLinkedDevices() {
        viewModelScope.launch {
            _isLoadingDevices.value = true
            try {
                val response = apiService.getLinkedDevices(LinkedDevicesRequest(activeDeviceId))
                if (response.success) {
                    _linkedDevicesList.value = response.devices
                } else {
                    _errorMessage.value = response.message
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to fetch linked devices: ${e.localizedMessage}"
            } finally {
                _isLoadingDevices.value = false
            }
        }
    }

    fun unlinkDevice(targetDeviceId: String) {
        viewModelScope.launch {
            _successMessage.value = null
            _errorMessage.value = null
            try {
                val response = apiService.unlinkDevice(
                    UnlinkDeviceRequest(
                        currentDeviceId = activeDeviceId,
                        targetDeviceId = targetDeviceId
                    )
                )
                if (response.success) {
                    _successMessage.value = response.message
                    fetchLinkedDevices()
                    if (targetDeviceId == activeDeviceId) {
                        setSimulatedDeviceId(null)
                    }
                } else {
                    _errorMessage.value = response.message
                }
            } catch (e: Exception) {
                _errorMessage.value = "Unlink failed: ${e.localizedMessage}"
            }
        }
    }

    // 2. Add / Set steps via sensor module
    fun addWalkSteps(steps: Int) {
        pedometerManager.addSimulatedSteps(steps, getTodayDateString())
    }

    fun setWalkSteps(steps: Int) {
        pedometerManager.setSimulatedSteps(steps, getTodayDateString())
    }

    // Helper: Reset all testing bounds (for user testing convenience)
    fun devResetProgress() {
        viewModelScope.launch {
            dao.clearMilestoneClaims()
            dao.clearTransactions()
            dao.clearWithdrawals()
            dao.insertUserProgress(
                UserProgressEntity(
                    deviceId = deviceId,
                    level = 1,
                    currentPoints = 500,
                    lifetimePoints = 500,
                    withdrawalLimitSats = 100,
                    currentSteps = 0,
                    lastUpdatedDate = getTodayDateString()
                )
            )
            _successMessage.value = "Ecosystem reset complete! Ready for fresh walkthrough testing."
        }
    }

    // 3. Upgrade Account Level (Max 10)
    fun upgradeAccountLevel() {
        viewModelScope.launch {
            _errorMessage.value = null
            _successMessage.value = null
            try {
                val response = apiService.upgradeLevel(UpgradeLevelRequest(activeDeviceId))
                if (response.success) {
                    _successMessage.value = response.message
                } else {
                    _errorMessage.value = response.message
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to upgrade level: ${e.localizedMessage}"
            }
        }
    }

    // 4. Upgrade Daily Withdrawal limit (Costs 50,000 pts per 10 Sats upgrade)
    fun upgradeWithdrawalLimit() {
        viewModelScope.launch {
            _errorMessage.value = null
            _successMessage.value = null
            try {
                val response = apiService.upgradeLimit(UpgradeLimitRequest(activeDeviceId))
                if (response.success) {
                    _successMessage.value = response.message
                } else {
                    _errorMessage.value = response.message
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to upgrade withdrawal limit: ${e.localizedMessage}"
            }
        }
    }

    // 5. Claim Step Milestones (50 pts or 250 pts with 5x Ad Multiplier)
    fun claimMilestoneReward(milestone: Int, watchAd: Boolean) {
        viewModelScope.launch {
            _errorMessage.value = null
            _successMessage.value = null

            if (watchAd) {
                // Open full ad simulator player
                _adPlacement.value = "STEP_CLAIM"
                _activeMilestoneAd.value = milestone
                _isAdPlaying.value = true
            } else {
                executeMilestoneClaimOnBackend(milestone, false)
            }
        }
    }

    fun completeAdPlayback() {
        viewModelScope.launch {
            _isAdPlaying.value = false
            val placement = _adPlacement.value
            val milestone = _activeMilestoneAd.value

            _adPlacement.value = null
            _activeMilestoneAd.value = null

            if (placement == "STEP_CLAIM" && milestone != null) {
                executeMilestoneClaimOnBackend(milestone, true)
            } else if (placement == "GAME_PLAY") {
                executeGameWinOnBackend(true)
            } else if (placement == "WHEEL_SPIN") {
                incrementSpinsUsedToday()
                _wheelReadyToSpin.value = true
            }
        }
    }

    fun cancelAdPlayback() {
        _isAdPlaying.value = false
        val placement = _adPlacement.value
        _adPlacement.value = null
        _activeMilestoneAd.value = null
        if (placement == "WHEEL_SPIN") {
            _errorMessage.value = "Ad skipped. Spin cancelled."
        } else {
            _errorMessage.value = "Video ad skipped. Multiplier rewards cancelled."
        }
    }

    private suspend fun executeMilestoneClaimOnBackend(milestone: Int, watchedAd: Boolean) {
        try {
            val response = apiService.claimStepsMilestone(
                ClaimStepsRequest(
                    deviceId = activeDeviceId,
                    milestone = milestone,
                    watchAd = watchedAd,
                    hmacVerify = "verified_payout_hash_${System.currentTimeMillis()}"
                )
            )

            if (response.success) {
                _successMessage.value = response.message
            } else {
                _errorMessage.value = response.message
            }
        } catch (e: Exception) {
            _errorMessage.value = "Network processing failure: ${e.localizedMessage}"
        }
    }

    // 6. Report Win Points from Game Win
    fun reportGameWinReward(watchAd: Boolean) {
        viewModelScope.launch {
            _errorMessage.value = null
            _successMessage.value = null

            if (watchAd) {
                _adPlacement.value = "GAME_PLAY"
                _isAdPlaying.value = true
            } else {
                executeGameWinOnBackend(false)
            }
        }
    }

    private suspend fun executeGameWinOnBackend(watchedAd: Boolean) {
        try {
            val response = apiService.reportGameWin(
                GameWinRequest(
                    deviceId = activeDeviceId,
                    watchAd = watchedAd,
                    timestamp = System.currentTimeMillis(),
                    clientToken = "verification_win_token_${System.currentTimeMillis()}"
                )
            )

            if (response.success) {
                _successMessage.value = response.message
            } else {
                _errorMessage.value = response.message
            }
        } catch (e: Exception) {
            _errorMessage.value = "Network failure while claim victory: ${e.localizedMessage}"
        }
    }

    // 7. Request Lightning Network Cashout
    fun cashoutToLightning(invoice: String, satoshis: Int) {
        viewModelScope.launch {
            _errorMessage.value = null
            _successMessage.value = null

            if (invoice.trim().isEmpty()) {
                _errorMessage.value = "Lightning Invoice / Address cannot be empty."
                return@launch
            }

            if (satoshis <= 0) {
                _errorMessage.value = "Withdrawal amount must be greater than zero."
                return@launch
            }

            try {
                val response = apiService.requestCashout(
                    CashoutRequest(
                        deviceId = activeDeviceId,
                        invoice = invoice.trim(),
                        satoshis = satoshis,
                        dateString = getTodayDateString(),
                        usdPrice = btcPriceUsd.value,
                        cadPrice = btcPriceCad.value
                    )
                )

                if (response.success) {
                    _successMessage.value = response.message
                    // Play ad automatic multiplier/playback simulator after payment completes!
                    _adPlacement.value = "WITHDRAWAL"
                    _isAdPlaying.value = true
                } else {
                    _errorMessage.value = response.message
                }
            } catch (e: Exception) {
                _errorMessage.value = "Lightning checkout error: ${e.localizedMessage}"
            }
        }
    }

    // 8. Upgrade Step Milestone Rewards Cap (Up to 20,000 steps max, 50k points cost)
    fun upgradeStepLimit() {
        viewModelScope.launch {
            _errorMessage.value = null
            _successMessage.value = null
            try {
                val response = apiService.upgradeStepLimit(UpgradeStepLimitRequest(activeDeviceId))
                if (response.success) {
                    _successMessage.value = response.message
                } else {
                    _errorMessage.value = response.message
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to upgrade step Rewards cap: ${e.localizedMessage}"
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    private fun LogSync(msg: String) {
        android.util.Log.d("SatsWalkViewModel", msg)
    }

    private fun loadPredictionData() {
        _predTargetDate.value = prefs.getString("PRED_TARGET_DATE", null)
        _predChoice.value = prefs.getString("PRED_CHOICE", null)
        _predBaselinePrice.value = prefs.getFloat("PRED_BASELINE_PRICE", 0f).toDouble()
        _predStatus.value = prefs.getString("PRED_STATUS", "NONE") ?: "NONE"
        _predResolvedPrice.value = prefs.getFloat("PRED_RESOLVED_PRICE", 0f).toDouble()
    }

    private fun savePredictionData(
        targetDate: String?,
        choice: String?,
        baseline: Double,
        status: String,
        resolvedPrice: Double
    ) {
        prefs.edit().apply {
            putString("PRED_TARGET_DATE", targetDate)
            putString("PRED_CHOICE", choice)
            putFloat("PRED_BASELINE_PRICE", baseline.toFloat())
            putString("PRED_STATUS", status)
            putFloat("PRED_RESOLVED_PRICE", resolvedPrice.toFloat())
            apply()
        }
        _predTargetDate.value = targetDate
        _predChoice.value = choice
        _predBaselinePrice.value = baseline
        _predStatus.value = status
        _predResolvedPrice.value = resolvedPrice
    }

    fun placePrediction(choice: String) {
        viewModelScope.launch {
            val tz = TimeZone.getTimeZone("America/New_York")
            val nowCal = Calendar.getInstance(tz)
            val currentHour = nowCal.get(Calendar.HOUR_OF_DAY)
            
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            sdf.timeZone = tz
            
            val targetDateStr: String
            val baseline: Double
            
            if (currentHour < 13) {
                targetDateStr = sdf.format(nowCal.time)
                baseline = _btcYesterday1PmUsd.value ?: _btcPriceUsd.value
            } else {
                val tomorrowCal = (nowCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
                targetDateStr = sdf.format(tomorrowCal.time)
                val histToday = fetchHistoricalPriceForDateAndHour(nowCal, 13)
                baseline = histToday ?: _btcPriceUsd.value
            }
            
            savePredictionData(
                targetDate = targetDateStr,
                choice = choice,
                baseline = baseline,
                status = "PENDING",
                resolvedPrice = 0.0
            )
        }
    }

    fun claimPredictionReward(watchAd: Boolean) {
        viewModelScope.launch {
            val status = _predStatus.value
            if (status == "RESOLVED_WIN") {
                reportGameWinReward(watchAd)
                savePredictionData(null, null, 0.0, "NONE", 0.0)
            }
        }
    }

    fun resetPredictionAfterLoss() {
        savePredictionData(null, null, 0.0, "NONE", 0.0)
    }

    private suspend fun fetchHistoricalPriceForDateAndHour(cal: Calendar, targetHour: Int): Double? = withContext(Dispatchers.IO) {
        return@withContext try {
            val urlString = "https://min-api.cryptocompare.com/data/v2/histohour?fsym=BTC&tsym=USD&limit=72"
            val connection = URL(urlString).openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseText)
            if (json.optString("Response") == "Success") {
                val dataObj = json.getJSONObject("Data")
                val dataArray = dataObj.getJSONArray("Data")
                
                val tz = TimeZone.getTimeZone("America/New_York")
                val year = cal.get(Calendar.YEAR)
                val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
                
                for (i in 0 until dataArray.length()) {
                    val item = dataArray.getJSONObject(i)
                    val timeSeconds = item.getLong("time")
                    val itemCal = Calendar.getInstance(tz).apply {
                        timeInMillis = timeSeconds * 1000L
                    }
                    if (itemCal.get(Calendar.YEAR) == year &&
                        itemCal.get(Calendar.DAY_OF_YEAR) == dayOfYear &&
                        itemCal.get(Calendar.HOUR_OF_DAY) == targetHour) {
                        return@withContext item.getDouble("close")
                    }
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun checkAndResolvePrediction() {
        viewModelScope.launch {
            _errorMessage.value = null
            _successMessage.value = null
            
            val targetDateStr = _predTargetDate.value
            val choice = _predChoice.value
            val baseline = _predBaselinePrice.value
            
            if (targetDateStr == null || choice == null || baseline <= 0.0) {
                _errorMessage.value = "No active prediction found to resolve."
                return@launch
            }

            val tz = TimeZone.getTimeZone("America/New_York")
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = tz }
            
            try {
                val targetDate = sdf.parse(targetDateStr) ?: return@launch
                val targetCal = Calendar.getInstance(tz).apply { time = targetDate }
                
                val nowCal = Calendar.getInstance(tz)
                val targetCalAt1Pm = (targetCal.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, 13)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }
                
                if (nowCal.before(targetCalAt1Pm)) {
                    val diffMs = targetCalAt1Pm.timeInMillis - nowCal.timeInMillis
                    val diffMinutes = diffMs / (1000 * 60)
                    val hours = diffMinutes / 60
                    val minutes = diffMinutes % 60
                    _errorMessage.value = "Target payout time (1:00 PM EST) has not reached yet! Remaining: ${hours}h ${minutes}m."
                    return@launch
                }
                
                val historicalPrice = fetchHistoricalPriceForDateAndHour(targetCal, 13)
                if (historicalPrice == null || historicalPrice <= 0.0) {
                    _errorMessage.value = "The 1:00 PM EST price on $targetDateStr is not published/indexed yet. Please wait a few minutes."
                    return@launch
                }
                
                val isWin = if (choice == "HIGHER") {
                    historicalPrice >= baseline
                } else {
                    historicalPrice <= baseline
                }
                
                savePredictionData(
                    targetDate = targetDateStr,
                    choice = choice,
                    baseline = baseline,
                    status = if (isWin) "RESOLVED_WIN" else "RESOLVED_LOSE",
                    resolvedPrice = historicalPrice
                )
                
                if (isWin) {
                    _successMessage.value = "Victory! The actual price was $historicalPrice USD (Baseline: $baseline USD). Claim your points!"
                } else {
                    _errorMessage.value = "Matched opposite. The actual price was $historicalPrice USD (Baseline: $baseline USD)."
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "Verification error: ${e.localizedMessage}"
            }
        }
    }

    fun devSimulateResolution(simulateWin: Boolean) {
        val choice = _predChoice.value ?: "HIGHER"
        val baseline = _predBaselinePrice.value
        val simulatedPrice = if (simulateWin) {
            if (choice == "HIGHER") baseline + 500.0 else baseline - 500.0
        } else {
            if (choice == "HIGHER") baseline - 500.0 else baseline + 500.0
        }
        
        savePredictionData(
            targetDate = _predTargetDate.value ?: "2026-06-13",
            choice = choice,
            baseline = baseline,
            status = if (simulateWin) {
                "RESOLVED_WIN"
            } else {
                "RESOLVED_LOSE"
            },
            resolvedPrice = simulatedPrice
        )
        
        if (simulateWin) {
            _successMessage.value = "Dev Cheat: Prediction simulated as WIN! Actual: $simulatedPrice, Baseline: $baseline."
        } else {
            _errorMessage.value = "Dev Cheat: Prediction simulated as LOSE! Actual: $simulatedPrice, Baseline: $baseline."
        }
    }

    // --- SPINNING WHEEL GAME STATE & METHODS ---
    private val _wheelReadyToSpin = MutableStateFlow(false)
    val wheelReadyToSpin: StateFlow<Boolean> = _wheelReadyToSpin.asStateFlow()

    fun loadWheelData() {
        val upgraded = prefs.getBoolean("WHEEL_UPGRADED_LIMIT", false)
        _wheelUpgradePurchased.value = upgraded
        val maxSpins = if (upgraded) 15 else 10
        _wheelMaxDailySpins.value = maxSpins
        _wheelSpinsRemainingToday.value = maxSpins - getSpinsUsedToday()
    }

    fun getSpinsUsedToday(): Int {
        val today = getTodayDateString()
        return prefs.getInt("WHEEL_SPINS_USED_TODAY_$today", 0)
    }

    fun incrementSpinsUsedToday() {
        val today = getTodayDateString()
        val current = getSpinsUsedToday()
        prefs.edit().putInt("WHEEL_SPINS_USED_TODAY_$today", current + 1).apply()
        _wheelSpinsRemainingToday.value = _wheelMaxDailySpins.value - (current + 1)
    }

    fun playWheelAd() {
        if (_wheelSpinsRemainingToday.value <= 0) {
            _errorMessage.value = "No spins remaining today! Resets in 24 hours."
            return
        }
        _errorMessage.value = null
        _successMessage.value = null
        _adPlacement.value = "WHEEL_SPIN"
        _isAdPlaying.value = true
    }

    fun clearWheelReadyToSpin() {
        _wheelReadyToSpin.value = false
    }

    fun awardWheelPoints(awardAmount: Int) {
        viewModelScope.launch {
            _errorMessage.value = null
            _successMessage.value = null
            val progress = dao.getUserProgressById(activeDeviceId) ?: return@launch
            val updated = progress.copy(
                currentPoints = progress.currentPoints + awardAmount,
                lifetimePoints = progress.lifetimePoints + awardAmount
            )
            dao.insertUserProgress(updated)
            dao.insertPointsTransaction(
                PointsTransactionEntity(
                    deviceId = activeDeviceId,
                    type = "WHEEL_SPIN_WIN",
                    pointsChange = awardAmount.toLong(),
                    detail = "Spin Wheel Game won: +$awardAmount PT!"
                )
            )
            _successMessage.value = if (awardAmount > 0) {
                "Congratulations! Landed on $awardAmount Points!"
            } else {
                "Hard luck! Landed on 0 Points (Try Again)."
            }
        }
    }

    fun buyWheelUpgrade() {
        viewModelScope.launch {
            _errorMessage.value = null
            _successMessage.value = null
            val progress = dao.getUserProgressById(activeDeviceId)
            if (progress == null) {
                _errorMessage.value = "Device profile not synced yet."
                return@launch
            }
            if (progress.currentPoints < 20000) {
                _errorMessage.value = "Insufficient points! Upgrade costs 20,000 credit points."
                return@launch
            }
            if (prefs.getBoolean("WHEEL_UPGRADED_LIMIT", false)) {
                _errorMessage.value = "Daily spin cap is already upgraded to 15!"
                return@launch
            }

            val updated = progress.copy(
                currentPoints = progress.currentPoints - 20000
            )
            dao.insertUserProgress(updated)
            dao.insertPointsTransaction(
                PointsTransactionEntity(
                    deviceId = activeDeviceId,
                    type = "WHEEL_UPGRADE",
                    pointsChange = -20000L,
                    detail = "Purchased Spinning Wheel Limit Upgrade (daily spin cap raised to 15)."
                )
            )

            prefs.edit().putBoolean("WHEEL_UPGRADED_LIMIT", true).apply()
            _wheelUpgradePurchased.value = true
            _wheelMaxDailySpins.value = 15
            _wheelSpinsRemainingToday.value = 15 - getSpinsUsedToday()
            _successMessage.value = "Spinning Wheel Upgrade Unlocked! Daily limit is now 15 spins."
        }
    }

    fun loadDiceData() {
        val today = getTodayDateString()
        _dicePlaysUsedOfToday.value = prefs.getInt("DICE_PLAYS_USED_TODAY_$today", 0)
    }

    fun playDiceGame(rolledNumber: Int) {
        viewModelScope.launch {
            _errorMessage.value = null
            _successMessage.value = null

            val progress = dao.getUserProgressById(activeDeviceId)
            if (progress == null) {
                _errorMessage.value = "Device profile not synced yet."
                return@launch
            }
            if (progress.currentSteps < 3000) {
                _errorMessage.value = "Dice game locked! You need to walk 3,000 steps first today."
                return@launch
            }

            val today = getTodayDateString()
            val used = prefs.getInt("DICE_PLAYS_USED_TODAY_$today", 0)
            if (used >= 3) {
                _errorMessage.value = "No plays remaining today."
                return@launch
            }

            val nextUsed = used + 1
            prefs.edit().putInt("DICE_PLAYS_USED_TODAY_$today", nextUsed).apply()
            _dicePlaysUsedOfToday.value = nextUsed

            val pointsToAward = rolledNumber * 50
            val updated = progress.copy(
                currentPoints = progress.currentPoints + pointsToAward,
                lifetimePoints = progress.lifetimePoints + pointsToAward
            )
            dao.insertUserProgress(updated)

            dao.insertPointsTransaction(
                PointsTransactionEntity(
                    deviceId = activeDeviceId,
                    type = "DICE_GAME_WIN",
                    pointsChange = pointsToAward.toLong(),
                    detail = "Rolled a $rolledNumber: won $pointsToAward points!"
                )
            )
            _successMessage.value = "You rolled a $rolledNumber and won $pointsToAward points!"
        }
    }

    fun getCoinFlipPlaysUsedToday(): Int {
        val today = getTodayDateString()
        return prefs.getInt("COIN_FLIP_PLAYS_USED_TODAY_$today", 0)
    }

    fun loadCoinFlipData() {
        _coinFlipPlaysUsedOfToday.value = getCoinFlipPlaysUsedToday()
    }

    fun incrementCoinFlipPlaysUsedToday() {
        val today = getTodayDateString()
        val current = getCoinFlipPlaysUsedToday()
        prefs.edit().putInt("COIN_FLIP_PLAYS_USED_TODAY_$today", current + 1).apply()
        _coinFlipPlaysUsedOfToday.value = current + 1
    }

    fun loadDisclaimerState() {
        _isDisclaimerAccepted.value = prefs.getBoolean("LEGAL_DISCLAIMER_ACCEPTED", false)
    }

    fun setDisclaimerAccepted(accepted: Boolean) {
        prefs.edit().putBoolean("LEGAL_DISCLAIMER_ACCEPTED", accepted).apply()
        _isDisclaimerAccepted.value = accepted
    }
}

