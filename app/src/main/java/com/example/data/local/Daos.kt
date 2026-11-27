package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ProductCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM user_accounts ORDER BY role ASC, id ASC")
    fun observeAllUsers(): Flow<List<UserAccountEntity>>

    @Query("SELECT * FROM user_accounts ORDER BY role ASC, id ASC")
    suspend fun getAllUsersOnce(): List<UserAccountEntity>

    @Query("SELECT * FROM user_accounts WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserAccountEntity?

    @Query("SELECT * FROM user_accounts WHERE role = 'ADMIN' LIMIT 1")
    suspend fun getAdminUser(): UserAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<UserAccountEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserAccountEntity): Long

    @Query("UPDATE user_accounts SET pin = :newPin WHERE id = :userId")
    suspend fun updatePin(userId: Long, newPin: String)

    @Query("UPDATE user_accounts SET displayName = :newDisplayName WHERE id = :userId")
    suspend fun updateDisplayName(userId: Long, newDisplayName: String)

    @Query("UPDATE user_accounts SET isActive = :isActive WHERE id = :userId")
    suspend fun updateIsActive(userId: Long, isActive: Boolean)

    @Query("DELETE FROM user_accounts")
    suspend fun deleteAll()
}

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles WHERE isActive = 1 ORDER BY id ASC")
    fun observeActiveVehicles(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles ORDER BY id ASC")
    suspend fun getAllVehiclesOnce(): List<VehicleEntity>

    @Query("SELECT * FROM vehicles WHERE staffUserId = :staffUserId LIMIT 1")
    suspend fun getVehicleByStaffUserId(staffUserId: Long): VehicleEntity?

    @Query("SELECT * FROM vehicles WHERE id = :vehicleId LIMIT 1")
    suspend fun getVehicleById(vehicleId: Long): VehicleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vehicle: VehicleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vehicles: List<VehicleEntity>): List<Long>

    @Query("UPDATE vehicles SET name = :newName WHERE id = :vehicleId")
    suspend fun updateName(vehicleId: Long, newName: String)

    @Query("UPDATE vehicles SET isActive = :isActive WHERE id = :vehicleId")
    suspend fun updateIsActive(vehicleId: Long, isActive: Boolean)

    @Query("DELETE FROM vehicles")
    suspend fun deleteAll()
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY sortOrder ASC, id ASC")
    fun observeActiveProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isActive = 1 AND category = :category ORDER BY sortOrder ASC, id ASC")
    fun observeActiveProductsByCategory(category: ProductCategory): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY sortOrder ASC, id ASC")
    fun observeAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllProductsOnce(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(product: ProductEntity): Long

    @Update
    suspend fun update(product: ProductEntity)

    @Query("UPDATE products SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)

    @Delete
    suspend fun delete(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM products")
    suspend fun deleteAll()
}

@Dao
interface OpeningStockDao {
    @Query("SELECT * FROM opening_stocks")
    fun observeAllOpenings(): Flow<List<OpeningStockEntity>>

    @Query("SELECT * FROM opening_stocks WHERE scope = 'STORE' AND vehicleId = 0")
    fun observeStoreOpenings(): Flow<List<OpeningStockEntity>>

    @Query("SELECT * FROM opening_stocks WHERE scope = 'VEHICLE' AND vehicleId = :vehicleId")
    fun observeVehicleOpenings(vehicleId: Long): Flow<List<OpeningStockEntity>>

    @Query("SELECT * FROM opening_stocks")
    suspend fun getAllOpeningsOnce(): List<OpeningStockEntity>

    @Query("SELECT * FROM opening_stocks WHERE productId = :productId")
    suspend fun getOpeningsForProduct(productId: Long): List<OpeningStockEntity>

    @Query("SELECT * FROM opening_stocks WHERE scope = :scope AND vehicleId = :vehicleId AND productId = :productId LIMIT 1")
    suspend fun getOpening(scope: String, vehicleId: Long, productId: Long): OpeningStockEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(opening: OpeningStockEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(openings: List<OpeningStockEntity>)

    @Query("DELETE FROM opening_stocks WHERE productId = :productId")
    suspend fun deleteByProductId(productId: Long)

    @Query("DELETE FROM opening_stocks")
    suspend fun deleteAll()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM stock_transactions ORDER BY businessDate DESC, timestamp DESC, id DESC")
    fun observeAllTransactions(): Flow<List<StockTransactionEntity>>

    @Query("SELECT * FROM stock_transactions WHERE businessDate = :businessDate ORDER BY timestamp DESC, id DESC")
    fun observeTransactionsByDate(businessDate: String): Flow<List<StockTransactionEntity>>

    @Query("SELECT * FROM stock_transactions WHERE vehicleId = :vehicleId AND businessDate = :businessDate ORDER BY timestamp DESC, id DESC")
    fun observeTransactionsByVehicleAndDate(vehicleId: Long, businessDate: String): Flow<List<StockTransactionEntity>>

    @Query("SELECT * FROM stock_transactions ORDER BY businessDate ASC, timestamp ASC, id ASC")
    suspend fun getAllTransactionsOnce(): List<StockTransactionEntity>

    @Query("SELECT * FROM stock_transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): StockTransactionEntity?

    @Query("SELECT * FROM stock_transactions WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getTransactionByRemoteId(remoteId: String): StockTransactionEntity?

    @Query("SELECT * FROM stock_transactions WHERE remoteId IS NULL")
    suspend fun getUnsyncedTransactionsOnce(): List<StockTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tx: StockTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(tx: StockTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(txs: List<StockTransactionEntity>)

    @Update
    suspend fun update(tx: StockTransactionEntity)

    @Query("UPDATE stock_transactions SET remoteId = :remoteId WHERE id = :id")
    suspend fun updateRemoteId(id: Long, remoteId: String)

    @Query("SELECT COUNT(*) FROM stock_transactions WHERE productId = :productId")
    suspend fun countTransactionsForProduct(productId: Long): Int

    @Query("DELETE FROM stock_transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM stock_transactions WHERE remoteId = :remoteId")
    suspend fun deleteByRemoteId(remoteId: String)

    @Query("DELETE FROM stock_transactions")
    suspend fun deleteAll()
}

@Dao
interface StaffAttendanceDao {
    @Query("SELECT * FROM staff_attendance ORDER BY businessDate DESC, staffName ASC")
    fun observeAllAttendance(): Flow<List<StaffAttendanceEntity>>

    @Query("SELECT * FROM staff_attendance WHERE businessDate = :businessDate ORDER BY staffName ASC")
    fun observeAttendanceByDate(businessDate: String): Flow<List<StaffAttendanceEntity>>

    @Query("SELECT * FROM staff_attendance WHERE businessDate = :businessDate AND staffId = :staffId LIMIT 1")
    suspend fun getForStaffAndDate(businessDate: String, staffId: Long): StaffAttendanceEntity?

    @Query("SELECT * FROM staff_attendance WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): StaffAttendanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: StaffAttendanceEntity): Long

    @Query("DELETE FROM staff_attendance")
    suspend fun deleteAll()
}

@Dao
interface PendingStoreTransferDao {
    @Query("SELECT * FROM pending_store_transfers ORDER BY requestedAtMs DESC")
    fun observeAll(): Flow<List<PendingStoreTransferEntity>>

    @Query("SELECT * FROM pending_store_transfers WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): PendingStoreTransferEntity?

    @Query("SELECT * FROM pending_store_transfers WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): PendingStoreTransferEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PendingStoreTransferEntity): Long

    @Query("UPDATE pending_store_transfers SET status = :status, reviewedAtMs = :reviewedAtMs, reviewedBy = :reviewedBy, reviewNotes = :reviewNotes WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, reviewedAtMs: Long, reviewedBy: String, reviewNotes: String)

    @Query("DELETE FROM pending_store_transfers")
    suspend fun deleteAll()
}

@Dao
interface AppNotificationDao {
    @Query("SELECT * FROM app_notifications ORDER BY createdAtMs DESC LIMIT 120")
    fun observeAll(): Flow<List<AppNotificationEntity>>

    @Query("SELECT * FROM app_notifications WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): AppNotificationEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: AppNotificationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AppNotificationEntity): Long

    @Query("UPDATE app_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: Long)

    @Query("UPDATE app_notifications SET isRead = 1")
    suspend fun markAllRead()

    @Query("DELETE FROM app_notifications")
    suspend fun deleteAll()
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY createdAtMs ASC LIMIT 300")
    fun observeAll(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): ChatMessageEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAll()
}

@Dao
interface DifferentGasReturnDao {
    @Query("SELECT * FROM different_gas_returns ORDER BY timestamp DESC, id DESC")
    fun observeAll(): Flow<List<DifferentGasReturnEntity>>

    @Query("SELECT * FROM different_gas_returns ORDER BY timestamp DESC, id DESC")
    suspend fun getAllOnce(): List<DifferentGasReturnEntity>

    @Query("SELECT * FROM different_gas_returns WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): DifferentGasReturnEntity?

    @Query("SELECT * FROM different_gas_returns WHERE submissionKey = :submissionKey LIMIT 1")
    suspend fun getBySubmissionKey(submissionKey: String): DifferentGasReturnEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: DifferentGasReturnEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<DifferentGasReturnEntity>)

    @Update
    suspend fun update(entity: DifferentGasReturnEntity)

    @Query("DELETE FROM different_gas_returns WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM different_gas_returns")
    suspend fun deleteAll()
}

@Dao
interface AuthorizedMobileDao {
    @Query("SELECT * FROM authorized_mobiles ORDER BY isProtectedOwner DESC, role ASC, displayName ASC")
    fun observeAll(): Flow<List<AuthorizedMobileEntity>>

    @Query("SELECT * FROM authorized_mobiles ORDER BY isProtectedOwner DESC, role ASC, displayName ASC")
    suspend fun getAllOnce(): List<AuthorizedMobileEntity>

    @Query("SELECT * FROM authorized_mobiles WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): AuthorizedMobileEntity?

    @Query("SELECT * FROM authorized_mobiles WHERE phoneE164 = :phoneE164 LIMIT 1")
    suspend fun getByPhone(phoneE164: String): AuthorizedMobileEntity?

    @Query("SELECT * FROM authorized_mobiles WHERE staffUserId = :staffUserId AND isActive = 1 LIMIT 1")
    suspend fun getActiveByStaffUserId(staffUserId: Long): AuthorizedMobileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AuthorizedMobileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<AuthorizedMobileEntity>)

    @Query("DELETE FROM authorized_mobiles WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM authorized_mobiles")
    suspend fun deleteAll()
}

@Dao
interface RegisteredDeviceDao {
    @Query("SELECT * FROM registered_devices ORDER BY isRevoked ASC, lastActiveMs DESC")
    fun observeAll(): Flow<List<RegisteredDeviceEntity>>

    @Query("SELECT * FROM registered_devices ORDER BY lastActiveMs DESC")
    suspend fun getAllOnce(): List<RegisteredDeviceEntity>

    @Query("SELECT * FROM registered_devices WHERE deviceId = :deviceId LIMIT 1")
    suspend fun getByDeviceId(deviceId: String): RegisteredDeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RegisteredDeviceEntity): Long

    @Query("UPDATE registered_devices SET isRevoked = 1, sessionVersion = sessionVersion + 1 WHERE deviceId = :deviceId")
    suspend fun revokeDevice(deviceId: String)

    @Query("UPDATE registered_devices SET isRevoked = 1, sessionVersion = sessionVersion + 1 WHERE isRevoked = 0")
    suspend fun revokeAllDevices()

    @Query("DELETE FROM registered_devices")
    suspend fun deleteAll()
}

@Dao
interface SecurityAuditLogDao {
    @Query("SELECT * FROM security_audit_logs ORDER BY timestampMs DESC, id DESC LIMIT 200")
    fun observeRecent(): Flow<List<SecurityAuditLogEntity>>

    @Query("SELECT * FROM security_audit_logs ORDER BY timestampMs DESC, id DESC LIMIT 200")
    suspend fun getRecentOnce(): List<SecurityAuditLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SecurityAuditLogEntity): Long

    @Query("DELETE FROM security_audit_logs")
    suspend fun deleteAll()
}


