package com.example.ui

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class ToastType { SUCCESS, ERROR, INFO }

data class ToastState(
    val message: String,
    val type: ToastType = ToastType.INFO
)

data class ActiveSignal(
    val direction: String, // CALL or PUT
    val confidence: Int,   // 82 to 98
    val time: String,      // visual stamp "10:45 AM"
    val pair: String,
    val tf: String,
    val durationSeconds: Int
)

class DidsBoltViewModel(private val repository: AppRepository) : ViewModel() {

    // --- REPOSITORY FLOW CONNECTORS Exposing to UI ---
    val users: StateFlow<List<UserEntity>> = repository.users
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vouchers: StateFlow<List<VoucherEntity>> = repository.vouchers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val assets: StateFlow<List<AssetEntity>> = repository.assets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pricingPlans: StateFlow<List<PricePlanEntity>> = repository.pricingPlans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val signalHistory: StateFlow<List<SignalHistoryEntity>> = repository.listenToLiveSignals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val announcement: StateFlow<SystemConfigEntity?> = repository.getAnnouncementFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val pocketOptionLink: StateFlow<SystemConfigEntity?> = repository.getPocketOptionLinkFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- SESSION STATE MANAGEMENT WITH COMPOSE GETTERS/SETTERS ---
    var currentUser by mutableStateOf<UserEntity?>(null)
        private set

    var currentView by mutableStateOf("login")
        private set

    var toastMessage by mutableStateOf<ToastState?>(null)
        private set

    var pendingPlan by mutableStateOf<PricePlanEntity?>(null)
        private set

    // Analytical Signalling State Parameters
    var activeSignalState by mutableStateOf("idle") // "idle", "analyzing", "result", "expired"
        private set

    var currentSignal by mutableStateOf<ActiveSignal?>(null)
        private set

    var signalTimeLeft by mutableIntStateOf(0)
        private set

    var signalTotalDuration by mutableIntStateOf(0)
        private set

    var selectedAsset by mutableStateOf("")
    var selectedTimeframe by mutableStateOf("M1")
    var currentMarketType by mutableStateOf("OTC") // "Standard" or "OTC"

    private var countdownJob: Job? = null
    private var paymentTimerJob: Job? = null
    private var generateSignalJob: Job? = null

    init {
        // Run database initialization and pre-seeding logic
        viewModelScope.launch {
            repository.checkAndSeedInitialData()
        }
    }

    // Toast Utility Trigger
    fun showToast(message: String, type: ToastType = ToastType.INFO) {
        toastMessage = ToastState(message, type)
        viewModelScope.launch {
            delay(3500)
            if (toastMessage?.message == message) {
                toastMessage = null
            }
        }
    }

    fun clearToast() {
        toastMessage = null
    }

    fun setView(view: String) {
        currentView = view
    }

    // --- USER ROUTINES ---
    fun login(username: String, isAdminMode: Boolean, accessKey: String) {
        viewModelScope.launch {
            val trimmedName = username.trim()
            if (trimmedName.length < 3) {
                showToast("Profile identifier is too short.", ToastType.ERROR)
                return@launch
            }

            if (isAdminMode) {
                // Strict Admin authentication matching web admin block
                if (trimmedName.lowercase(Locale.ROOT) == "admin" && accessKey.trim() == "DIDSOWN'SIT") {
                    currentUser = UserEntity(
                        id = "ADMIN_ROOT",
                        username = "Administrator",
                        accessExpiresAt = null,
                        isActive = true
                    )
                    setView("admin")
                    showToast("Admin override confirmed", ToastType.SUCCESS)
                } else {
                    showToast("Invalid Secret Key credentials", ToastType.ERROR)
                }
                return@launch
            }

            // Standard Trader credentials
            val formattedName = if (trimmedName.startsWith("@")) trimmedName else "@$trimmedName"
            var userRecord = repository.getUserByUsername(formattedName)

            if (userRecord == null) {
                // Register account automatically
                val randomId = "TRD_" + UUID.randomUUID().toString().substring(0, 6).uppercase(Locale.ROOT)
                userRecord = UserEntity(
                    id = randomId,
                    username = formattedName,
                    accessExpiresAt = null,
                    isActive = true
                )
                repository.insertUser(userRecord)
            }

            if (!userRecord.isActive) {
                showToast("Account deactivated by administration.", ToastType.ERROR)
                return@launch
            }

            currentUser = userRecord
            setView("dashboard")
            showToast("Session established: $formattedName", ToastType.SUCCESS)
        }
    }

    fun logout() {
        currentUser = null
        currentView = "login"
        activeSignalState = "idle"
        currentSignal = null
        countdownJob?.cancel()
        countdownJob = null
        generateSignalJob?.cancel()
        generateSignalJob = null
    }

    fun hasAccess(): Boolean {
        val user = currentUser ?: return false
        if (user.id == "ADMIN_ROOT") return true
        val expiry = user.accessExpiresAt ?: return false
        return expiry > System.currentTimeMillis()
    }

    // --- VOUCHER REDEMPTION ROUTINES ---
    fun redeemVoucher(codeStr: String) {
        viewModelScope.launch {
            val user = currentUser ?: return@launch
            val uppercaseCode = codeStr.trim().uppercase(Locale.ROOT)
            val voucher = repository.getVoucherByCode(uppercaseCode)

            if (voucher == null || voucher.isRedeemed) {
                showToast("Voucher invalid, expired, or already used.", ToastType.ERROR)
                return@launch
            }

            // Mark voucher as redeemed
            val updatedVoucher = voucher.copy(isRedeemed = true, redeemedBy = user.username)
            repository.insertVoucher(updatedVoucher)

            // Grant subscription days
            val currentExp = user.accessExpiresAt ?: System.currentTimeMillis()
            val baseTime = if (currentExp > System.currentTimeMillis()) currentExp else System.currentTimeMillis()
            val newExpiry = baseTime + (updatedVoucher.days * 86400000L)

            val updatedUser = user.copy(accessExpiresAt = newExpiry)
            repository.insertUser(updatedUser)
            currentUser = updatedUser

            showToast("Success! Verified Voucher added ${updatedVoucher.days} Premium Days.", ToastType.SUCCESS)
        }
    }

    // --- PRICING PLAN ROUTINES ---
    fun selectPlan(plan: PricePlanEntity) {
        pendingPlan = plan
        showToast("Opening @DIDSBOLT configuration channel...", ToastType.INFO)
    }

    fun cancelPendingPayment() {
        pendingPlan = null
    }

    fun confirmPayment(accessCode: String) {
        val plan = pendingPlan ?: return
        val user = currentUser ?: return
        val codeClean = accessCode.trim().uppercase(Locale.ROOT)
        if (codeClean.isEmpty()) {
            showToast("Verification failed: Please enter your Access Code.", ToastType.ERROR)
            return
        }

        paymentTimerJob?.cancel()
        paymentTimerJob = viewModelScope.launch {
            showToast("Verifying reference ID with @DIDSBOLT...", ToastType.INFO)
            delay(1500)

            val voucher = repository.getVoucherByCode(codeClean)
            if (voucher == null || voucher.isRedeemed) {
                showToast("Verification failed: Invalid or already used Access Code.", ToastType.ERROR)
                return@launch
            }

            // Mark voucher as redeemed
            val updatedVoucher = voucher.copy(isRedeemed = true, redeemedBy = user.username)
            repository.insertVoucher(updatedVoucher)

            val daysToGrant = if (voucher.days > 0) voucher.days else plan.days
            val expiryBase = user.accessExpiresAt ?: System.currentTimeMillis()
            val baseTime = if (expiryBase > System.currentTimeMillis()) expiryBase else System.currentTimeMillis()
            val newExpiry = baseTime + (daysToGrant * 86400000L)

            val updatedUser = user.copy(accessExpiresAt = newExpiry)
            repository.insertUser(updatedUser)
            currentUser = updatedUser

            pendingPlan = null
            setView("dashboard")
            showToast("Access Authorized! $daysToGrant Days premium activated.", ToastType.SUCCESS)
        }
    }

    // --- LIVE SIGNAL ACTION ROUTINES ---
    fun triggerGenerateSignal() {
        if (!hasAccess()) return

        activeSignalState = "analyzing"
        currentSignal = null
        countdownJob?.cancel()
        countdownJob = null
        generateSignalJob?.cancel()

        generateSignalJob = viewModelScope.launch {
            delay(3000)

            val isUp = Math.random() > 0.5
            val direction = if (isUp) "CALL" else "PUT"
            val confidence = (82..98).random()
            val durationSeconds = parseTimeframeToSeconds(selectedTimeframe)

            val format = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
            val timeLabel = format.format(Date())

            val signal = ActiveSignal(
                direction = direction,
                confidence = confidence,
                time = timeLabel,
                pair = selectedAsset,
                tf = selectedTimeframe,
                durationSeconds = durationSeconds
            )

            currentSignal = signal
            signalTimeLeft = durationSeconds
            signalTotalDuration = durationSeconds
            activeSignalState = "result"

            // Insert into Signal History
            val historyItem = SignalHistoryEntity(
                pair = selectedAsset,
                direction = direction,
                result = if (Math.random() > 0.15) "WIN" else "LOSS", // Simulated 85% win-ratio
                time = timeLabel
            )
            repository.insertSignal(historyItem)

            // Run countdown loop
            countdownJob = launch {
                while (signalTimeLeft > 0) {
                    delay(1000)
                    signalTimeLeft -= 1
                }
                activeSignalState = "expired"
            }
        }
    }

    fun resetTerminal() {
        activeSignalState = "idle"
        currentSignal = null
        countdownJob?.cancel()
        countdownJob = null
        generateSignalJob?.cancel()
        generateSignalJob = null
    }

    // Translates standard timeframe string to numeric seconds
    private fun parseTimeframeToSeconds(tf: String): Int {
        val unit = tf.firstOrNull() ?: 'M'
        val valueString = tf.drop(1)
        val value = valueString.toIntOrNull() ?: 60

        return when (unit) {
            'S' -> value
            'M' -> value * 60
            'D' -> value * 30 // Demonstration acceleration: D1 = 30 seconds
            else -> value * 60
        }
    }

    // --- ADMINISTRATIVE DASHBOARD ROUTINES ---
    fun toggleUserActiveStatus(userId: String) {
        viewModelScope.launch {
            val userList = users.value
            val user = userList.find { it.id == userId } ?: return@launch
            val updated = user.copy(isActive = !user.isActive)
            repository.insertUser(updated)
            showToast("User status modified", ToastType.SUCCESS)

            if (currentUser?.id == userId) {
                currentUser = updated
            }
        }
    }

    fun extendUserAccess(userId: String, days: Int) {
        viewModelScope.launch {
            val userList = users.value
            val user = userList.find { it.id == userId } ?: return@launch
            val base = user.accessExpiresAt ?: System.currentTimeMillis()
            val start = if (base > System.currentTimeMillis()) base else System.currentTimeMillis()
            val updated = user.copy(accessExpiresAt = start + (days * 86400000L))
            repository.insertUser(updated)
            showToast("Granted +$days Days access", ToastType.SUCCESS)

            if (currentUser?.id == userId) {
                currentUser = updated
            }
        }
    }

    fun subtractUserAccess(userId: String) {
        viewModelScope.launch {
            val userList = users.value
            val user = userList.find { it.id == userId } ?: return@launch
            val updated = user.copy(accessExpiresAt = null)
            repository.insertUser(updated)
            showToast("Access authorization completely revoked", ToastType.INFO)

            if (currentUser?.id == userId) {
                currentUser = updated
            }
        }
    }

    fun registerNewUser(usernameInput: String) {
        viewModelScope.launch {
            val name = usernameInput.trim()
            if (name.isEmpty()) return@launch
            val formatted = if (name.startsWith("@")) name else "@$name"

            val existing = users.value.any { it.username.lowercase(Locale.ROOT) == formatted.lowercase(Locale.ROOT) }
            if (existing) {
                showToast("Account profile name already registered", ToastType.ERROR)
                return@launch
            }

            val randomId = "TRD_" + UUID.randomUUID().toString().substring(0, 6).uppercase(Locale.ROOT)
            val newUser = UserEntity(
                id = randomId,
                username = formatted,
                accessExpiresAt = null,
                isActive = true
            )
            repository.insertUser(newUser)
            showToast("Added profile: $formatted", ToastType.SUCCESS)
        }
    }

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            repository.deleteUserById(userId)
            showToast("Trader profile completely purged from node list", ToastType.INFO)
        }
    }

    fun updateUsername(userId: String, newName: String) {
        viewModelScope.launch {
            val userList = users.value
            val user = userList.find { it.id == userId } ?: return@launch
            val trimmedName = newName.trim()
            if (trimmedName.isEmpty()) return@launch
            val formatted = if (trimmedName.startsWith("@")) trimmedName else "@$trimmedName"

            val updated = user.copy(username = formatted)
            repository.insertUser(updated)
            showToast("Trader profile updated", ToastType.SUCCESS)

            if (currentUser?.id == userId) {
                currentUser = updated
            }
        }
    }

    fun generatePromoVoucher(days: Int) {
        viewModelScope.launch {
            val charPool : List<Char> = ('A'..'Z') + ('0'..'9')
            val section1 = (1..4).map { charPool.random() }.joinToString("")
            val section2 = (1..4).map { charPool.random() }.joinToString("")
            val randomCode = "DIDS-$section1-$section2"

            val voucher = VoucherEntity(
                code = randomCode,
                days = days,
                isRedeemed = false
            )
            repository.insertVoucher(voucher)
            showToast("Voucher code generated: $randomCode", ToastType.SUCCESS)
        }
    }

    fun revokePromoVoucher(code: String) {
        viewModelScope.launch {
            repository.deleteVoucherByCode(code)
            showToast("Promo voucher revoked", ToastType.INFO)
        }
    }

    fun saveAnnouncementConfig(announcementText: String) {
        viewModelScope.launch {
            repository.setAnnouncementValue(announcementText)
            showToast("Scroll Header announcement updated instantly", ToastType.SUCCESS)
        }
    }

    fun savePocketOptionLink(linkText: String) {
        viewModelScope.launch {
            repository.setPocketOptionLinkValue(linkText)
            showToast("Pocket Option referral link updated instantly", ToastType.SUCCESS)
        }
    }

    fun addCustomAsset(name: String, isOtc: Boolean) {
        viewModelScope.launch {
            val formatted = name.trim().uppercase(Locale.ROOT)
            if (formatted.isEmpty()) return@launch

            val isOtcCalculated = isOtc || formatted.endsWith("OTC")
            val finalName = if (isOtcCalculated && !formatted.endsWith(" OTC")) "$formatted OTC" else formatted

            repository.insertAsset(AssetEntity(finalName, isOtc = isOtcCalculated))
            showToast("Added $finalName to ${if (isOtcCalculated) "OTC" else "Standard"} config", ToastType.SUCCESS)
        }
    }

    fun removeCustomAsset(name: String) {
        viewModelScope.launch {
            repository.deleteAssetByName(name)
            showToast("Removed asset $name", ToastType.INFO)
        }
    }

    fun editPricePlan(planId: Int, name: String, price: String, days: Int, desc: String) {
        viewModelScope.launch {
            val plan = PricePlanEntity(
                id = planId,
                name = name,
                price = price,
                days = days,
                desc = desc
            )
            repository.insertPricePlan(plan)
            showToast("Pricing Tier modified", ToastType.SUCCESS)
        }
    }
}
