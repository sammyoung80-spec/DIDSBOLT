package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

// --- ENTITIES ---

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val accessExpiresAt: Long?, // Nullable: null means no access, value means expiration timestamp
    val isActive: Boolean = true
)

@Entity(tableName = "vouchers")
data class VoucherEntity(
    @PrimaryKey val code: String,
    val days: Int,
    val isRedeemed: Boolean = false,
    val redeemedBy: String? = null
)

@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey val name: String,
    val isOtc: Boolean
)

@Entity(tableName = "pricing_plans")
data class PricePlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val price: String,
    val days: Int,
    val desc: String
)

@Entity(tableName = "system_config")
data class SystemConfigEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Serializable
@Entity(tableName = "signal_history")
data class SignalHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pair: String,
    val direction: String, // CALL of PUT
    val result: String,    // WIN or LOSS
    val time: String,      // Visual label e.g., "10:45 AM"
    val timestamp: Long = System.currentTimeMillis()
)

// --- DAO ---

@Dao
interface AppDao {
    // Users
    @Query("SELECT * FROM users ORDER BY username ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUserById(id: String)

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    // Vouchers
    @Query("SELECT * FROM vouchers ORDER BY code ASC")
    fun getAllVouchers(): Flow<List<VoucherEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoucher(voucher: VoucherEntity)

    @Query("DELETE FROM vouchers WHERE code = :code")
    suspend fun deleteVoucherByCode(code: String)

    @Query("SELECT * FROM vouchers WHERE code = :code LIMIT 1")
    suspend fun getVoucherByCode(code: String): VoucherEntity?

    // Assets
    @Query("SELECT * FROM assets ORDER BY name ASC")
    fun getAllAssets(): Flow<List<AssetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: AssetEntity)

    @Query("DELETE FROM assets WHERE name = :name")
    suspend fun deleteAssetByName(name: String)

    // Pricing Plans
    @Query("SELECT * FROM pricing_plans ORDER BY id ASC")
    fun getAllPricePlans(): Flow<List<PricePlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPricePlan(plan: PricePlanEntity)

    @Query("DELETE FROM pricing_plans WHERE id = :id")
    suspend fun deletePricePlanById(id: Int)

    // System Config
    @Query("SELECT * FROM system_config WHERE `key` = :key LIMIT 1")
    suspend fun getConfig(key: String): SystemConfigEntity?

    @Query("SELECT * FROM system_config WHERE `key` = :key LIMIT 1")
    fun getConfigFlow(key: String): Flow<SystemConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: SystemConfigEntity)

    // Signal History
    @Query("SELECT * FROM signal_history ORDER BY timestamp DESC LIMIT 50")
    fun getAllSignalHistory(): Flow<List<SignalHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignal(signal: SignalHistoryEntity)

    @Query("DELETE FROM signal_history")
    suspend fun clearSignalHistory()
}

// --- DATABASE ACCESS ---

@Database(
    entities = [
        UserEntity::class,
        VoucherEntity::class,
        AssetEntity::class,
        PricePlanEntity::class,
        SystemConfigEntity::class,
        SignalHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
}
