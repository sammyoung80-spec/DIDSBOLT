package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class AppRepository(private val appDao: AppDao) {

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
    }

    suspend fun deleteVoucherByCode(code: String) {
        appDao.deleteVoucherByCode(code)
    }

    suspend fun getVoucherByCode(code: String): VoucherEntity? {
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
    }

    suspend fun clearSignalHistory() {
        appDao.clearSignalHistory()
    }

    // Seeding logic called on app launch
    suspend fun checkAndSeedInitialData() {
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
    }
}
