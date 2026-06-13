package com.example.data.database

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// 1. User Progress Entity
@Entity(tableName = "user_progress")
data class UserProgressEntity(
    @PrimaryKey val deviceId: String,
    val level: Int = 1,
    val currentPoints: Long = 0,
    val lifetimePoints: Long = 0,
    val withdrawalLimitSats: Int = 100, // Baseline daily cashout limit (100 satoshis, upgradable to 300)
    val currentSteps: Int = 0,
    val lastUpdatedDate: String = "", // tracks "yyyy-MM-dd" to check for daily resets
    val maxClaimableSteps: Int = 5000
)

// 2. Claimed Step Milestones
@Entity(
    tableName = "milestone_claims",
    indices = [Index(value = ["dateString", "stepMilestone"], unique = true)]
)
data class MilestoneClaimEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateString: String,
    val stepMilestone: Int, // e.g. 500, 1000, ..., 20000
    val pointsAwarded: Int, // 50 or 250 (if ad watched)
    val adWatched: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

// 3. Ad Watch Logs for Backend Verification
@Entity(tableName = "ad_watches")
data class AdWatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val deviceId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val placement: String, // "STEP_CLAIM" or "GAME_PLAY" or "LIMIT_UPGRADE"
    val earnedPoints: Int,
    val verificationHash: String // Secure verification SHA-256 hash
)

// 4. Withdrawal Transaction History
@Entity(tableName = "withdrawals")
data class WithdrawalEntity(
    @PrimaryKey val id: String, // Invoice hash or secure hash
    val deviceId: String,
    val invoice: String,
    val satoshis: Int,
    val dateString: String, // For checking daily payout limit bounds
    val timestamp: Long = System.currentTimeMillis(),
    val status: String, // "COMPLETED", "FAILED"
    val usdValue: Double = 0.0,
    val cadValue: Double = 0.0
)

// 5. Points Balance Ledger Transactions
@Entity(tableName = "points_transactions")
data class PointsTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val deviceId: String,
    val type: String, // "STEP_CLAIM", "GAME_WIN", "LEVEL_UP", "LIMIT_UPGRADE"
    val pointsChange: Long, // Positive for earnings, negative for costs
    val detail: String,
    val timestamp: Long = System.currentTimeMillis()
)

// 6. Data Access Object
@Dao
interface SatsWalkDao {
    @Query("SELECT * FROM user_progress LIMIT 1")
    suspend fun getUserProgress(): UserProgressEntity?

    @Query("SELECT * FROM user_progress LIMIT 1")
    fun getUserProgressFlow(): Flow<UserProgressEntity?>

    @Query("SELECT * FROM user_progress WHERE deviceId = :deviceId LIMIT 1")
    suspend fun getUserProgressById(deviceId: String): UserProgressEntity?

    @Query("SELECT * FROM user_progress WHERE deviceId = :deviceId LIMIT 1")
    fun getUserProgressByIdFlow(deviceId: String): Flow<UserProgressEntity?>

    @Query("SELECT * FROM withdrawals WHERE deviceId = :deviceId ORDER BY timestamp DESC")
    fun getWithdrawalsByIdFlow(deviceId: String): Flow<List<WithdrawalEntity>>

    @Query("SELECT * FROM points_transactions WHERE deviceId = :deviceId ORDER BY timestamp DESC")
    fun getPointsTransactionsByIdFlow(deviceId: String): Flow<List<PointsTransactionEntity>>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProgress(userProgress: UserProgressEntity)

    @Query("SELECT * FROM milestone_claims WHERE dateString = :dateString")
    suspend fun getClaimsForDate(dateString: String): List<MilestoneClaimEntity>

    @Query("SELECT * FROM milestone_claims WHERE dateString = :dateString")
    fun getClaimsForDateFlow(dateString: String): Flow<List<MilestoneClaimEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMilestoneClaim(claim: MilestoneClaimEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdWatch(adWatch: AdWatchEntity)

    @Query("SELECT * FROM ad_watches ORDER BY timestamp DESC")
    fun getAdWatchesFlow(): Flow<List<AdWatchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWithdrawal(withdrawal: WithdrawalEntity)

    @Query("SELECT * FROM withdrawals ORDER BY timestamp DESC")
    fun getWithdrawalsFlow(): Flow<List<WithdrawalEntity>>

    @Query("SELECT SUM(satoshis) FROM withdrawals WHERE dateString = :dateString AND status = 'COMPLETED'")
    suspend fun getSatsWithdrawnOnDate(dateString: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPointsTransaction(transaction: PointsTransactionEntity)

    @Query("SELECT * FROM points_transactions ORDER BY timestamp DESC")
    fun getPointsTransactionsFlow(): Flow<List<PointsTransactionEntity>>

    @Query("DELETE FROM milestone_claims")
    suspend fun clearMilestoneClaims()

    @Query("DELETE FROM points_transactions")
    suspend fun clearTransactions()

    @Query("DELETE FROM withdrawals")
    suspend fun clearWithdrawals()
}

// 7. Database Holder
@Database(
    entities = [
        UserProgressEntity::class,
        MilestoneClaimEntity::class,
        AdWatchEntity::class,
        WithdrawalEntity::class,
        PointsTransactionEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class SatsWalkDatabase : RoomDatabase() {
    abstract fun satsWalkDao(): SatsWalkDao

    companion object {
        @Volatile
        private var INSTANCE: SatsWalkDatabase? = null

        fun getDatabase(context: Context): SatsWalkDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SatsWalkDatabase::class.java,
                    "satswalk_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
