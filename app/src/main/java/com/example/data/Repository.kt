package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.selectAsFlow
import kotlinx.serialization.Serializable

@Serializable
data class VoucherDto(
    val code: String,
    val days: Int,
    val is_redeemed: Boolean
)

class AppRepository(
    private val appDao: AppDao,
    private val supabase: SupabaseClient?
) {

    val users: Flow<List<UserEntity>> = appDao.getAllUsers()
    val vouchers: Flow<List<VoucherEntity>> = appDao.getAllVouchers()
    val assets: Flow<List<AssetEntity>> = appDao.getAllAssets()
    val pricingPlans: Flow<List<PricePlanEntity>> = appDao.getAllPricePlans()
    val signalHistory: Flow<List<SignalHistoryEntity>> = appDao.getAllSignalHistory()

    fun getAnnouncementFlow(): Flow<SystemConfigEntity?> = appDao.getConfigFlow("announcement")

    suspend fun getAnnouncementValue(): String {
        return appDao.getConfig("announcement")?.value ?: "🚨 LIVE DIDSBOLT TERMINAL SECURE — REDEEM ACCESS CODE TO BEGIN GENERATING AUTOMATED SIGNALS"
    }

    suspend fun setAnnouncementValue(value: String) {
        appDao.insertConfig(SystemConfigEntity("announcement", value))
    }

    fun getPocketOptionLinkFlow(): Flow<SystemConfigEntity?> = appDao.getConfigFlow("pocket_option_link")

    suspend fun getPocketOptionLinkValue(): String {
        return appDao.getConfig("pocket_option_link")?.value ?: "https://pocketoption.com/register/"
    }

    suspend fun setPocketOptionLinkValue(value: String) {
        appDao.insertConfig(SystemConfigEntity("pocket_option_link", value.trim()))
    }

    suspend fun getUserByUsername(username: String): UserEntity? {
        return appDao.getUserByUsername(username)
    }

    suspend fun insertUser(user: UserEntity) {
        appDao.insertUser(user)
    }

    suspend fun deleteUserById(id: String) {
        appDao.deleteUserById(id)
    }

    suspend fun insertVoucher(voucher: VoucherEntity) {
        appDao.insertVoucher(voucher)
        if (!com.example.MyApplication.isSupabaseEnabled || supabase == null) return
        try {
            val dto = VoucherDto(
                code = voucher.code,
                days = voucher.days,
                is_redeemed = voucher.isRedeemed
            )
            supabase.postgrest["vouchers"].upsert(dto)
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Supabase voucher upsert failed for ${voucher.code}", e)
        }
    }

    suspend fun deleteVoucherByCode(code: String) {
        appDao.deleteVoucherByCode(code)
    }

    suspend fun getVoucherByCode(code: String): VoucherEntity? {
        if (com.example.MyApplication.isSupabaseEnabled && supabase != null) {
            try {
                val dto = supabase.postgrest["vouchers"]
                    .select {
                        filter {
                            eq("code", code)
                        }
                    }.decodeSingleOrNull<VoucherDto>()
                if (dto != null) {
                    return VoucherEntity(
                        code = dto.code,
                        days = dto.days,
                        isRedeemed = dto.is_redeemed
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("AppRepository", "Supabase lookup failed for code $code (falling back to Room DB)", e)
            }
        }
        return appDao.getVoucherByCode(code)
    }

    suspend fun insertAsset(asset: AssetEntity) {
        appDao.insertAsset(asset)
    }

    suspend fun deleteAssetByName(name: String) {
        appDao.deleteAssetByName(name)
    }

    suspend fun insertPricePlan(plan: PricePlanEntity) {
        appDao.insertPricePlan(plan)
    }

    suspend fun deletePricePlanById(id: Int) {
        appDao.deletePricePlanById(id)
    }

    suspend fun insertSignal(signal: SignalHistoryEntity) {
        appDao.insertSignal(signal)
        if (!com.example.MyApplication.isSupabaseEnabled || supabase == null) return
        try {
            supabase.postgrest["signal_history"].insert(signal)
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Supabase signal insert failed for ${signal.pair}", e)
        }
    }

    // Exposes a database real-time pipeline directly into Flow!
    @OptIn(io.github.jan.supabase.annotations.SupabaseExperimental::class)
    fun listenToLiveSignals(): Flow<List<SignalHistoryEntity>> {
        if (!com.example.MyApplication.isSupabaseEnabled || supabase == null) {
            return appDao.getAllSignalHistory()
        }
        return try {
            supabase.postgrest["signal_history"]
                .selectAsFlow(SignalHistoryEntity::id)
                .catch { e ->
                    android.util.Log.e("AppRepository", "Supabase live signals stream connection failed", e)
                    emitAll(appDao.getAllSignalHistory())
                }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Supabase listenToLiveSignals failed (falling back to Room flow)", e)
            appDao.getAllSignalHistory()
        }
    }

    suspend fun clearSignalHistory() {
        appDao.clearSignalHistory()
    }

    // Seeding logic called on app launch
    suspend fun checkAndSeedInitialData() {
        try {
            // Core check: if no pricing plans exist, we seed everything.
            val existingPlans = pricingPlans.firstOrNull() ?: emptyList()
            if (existingPlans.isNotEmpty()) return // Seeded already

            // 1. Seed Pricing Plans
            listOf(
                PricePlanEntity(name = "24 Hour Pass", price = "$5.00", days = 1, desc = "Test the algorithm accuracy"),
                PricePlanEntity(name = "3 Days Pro Pass", price = "$10.00", days = 3, desc = "Most selected package"),
                PricePlanEntity(name = "Weekly Access Pass", price = "$20.00", days = 7, desc = "Best value configuration")
            ).forEach { appDao.insertPricePlan(it) }

            // 2. Seed Announcement Config
            appDao.insertConfig(SystemConfigEntity("announcement", "🚨 LIVE DIDSBOLT TERMINAL SECURE — REDEEM ACCESS CODE TO BEGIN GENERATING AUTOMATED SIGNALS"))
            appDao.insertConfig(SystemConfigEntity("pocket_option_link", "https://pocketoption.com/register/"))

            // 3. Seed Users
            val now = System.currentTimeMillis()
            listOf(
                UserEntity(id = "U1", username = "@trader_joe", accessExpiresAt = now + 86400000 * 2, isActive = true),
                UserEntity(id = "U2", username = "@binary_king", accessExpiresAt = null, isActive = true),
                UserEntity(id = "U3", username = "@dids_fan", accessExpiresAt = now - 3600000, isActive = false)
            ).forEach { appDao.insertUser(it) }

            // 4. Seed Vouchers
            listOf(
                VoucherEntity(code = "DIDS-FREE-PASS", days = 1, isRedeemed = false),
                VoucherEntity(code = "DIDS-PRO-WK99", days = 7, isRedeemed = false)
            ).forEach { appDao.insertVoucher(it) }

            // 5. Seed Standard Assets
            listOf(
                "EUR/USD", "GBP/USD", "AUD/USD", "USD/JPY", "USD/CAD", "GBP/JPY", "BTC/USD", "ETH/USD", "SOL/USD", "XAU/USD"
            ).forEach { appDao.insertAsset(AssetEntity(it, isOtc = false)) }

            // 6. Seed OTC Assets
            listOf(
                "EUR/USD OTC", "GBP/USD OTC", "AUD/CAD OTC", "USD/JPY OTC", "GBP/JPY OTC", "NZD/USD OTC", "EUR/JPY OTC",
                "EUR/GBP OTC", "USD/CHF OTC", "CAD/JPY OTC", "AUD/USD OTC", "NZD/CAD OTC", "GBP/CHF OTC", "USD/INR OTC"
            ).forEach { appDao.insertAsset(AssetEntity(it, isOtc = true)) }

            // 7. Seed Signal History
            listOf(
                SignalHistoryEntity(pair = "EUR/USD OTC", direction = "CALL", result = "WIN", time = "10:45 AM", timestamp = now - 600000),
                SignalHistoryEntity(pair = "AUD/CAD OTC", direction = "PUT", result = "LOSS", time = "09:30 AM", timestamp = now - 1800000),
                SignalHistoryEntity(pair = "GBP/JPY OTC", direction = "CALL", result = "WIN", time = "Yesterday", timestamp = now - 86400000)
            ).forEach { appDao.insertSignal(it) }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Error seeding initial application database state", e)
        }
    }
}
