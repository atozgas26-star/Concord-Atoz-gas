package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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
    suspend fun insertAll(vehicles: List<VehicleEntity>): List<Long>

    @Query("DELETE FROM vehicles")
    suspend fun deleteAll()
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY sortOrder ASC, id ASC")
    fun observeActiveProducts(): Flow<List<ProductEntity>>

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

    @Query("DELETE FROM products")
    suspend fun deleteAll()
}

@Dao
interface OpeningStockDao {
    @Query("SELECT * FROM opening_stocks")
    fun observeAllOpenings(): Flow<List<OpeningStockEntity>>

    @Query("SELECT * FROM opening_stocks")
    suspend fun getAllOpeningsOnce(): List<OpeningStockEntity>

    @Query("SELECT * FROM opening_stocks WHERE scope = :scope AND vehicleId = :vehicleId AND productId = :productId LIMIT 1")
    suspend fun getOpening(scope: String, vehicleId: Long, productId: Long): OpeningStockEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(opening: OpeningStockEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(openings: List<OpeningStockEntity>)

    @Query("DELETE FROM opening_stocks")
    suspend fun deleteAll()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM stock_transactions ORDER BY businessDate DESC, timestamp DESC, id DESC")
    fun observeAllTransactions(): Flow<List<StockTransactionEntity>>

    @Query("SELECT * FROM stock_transactions ORDER BY businessDate ASC, timestamp ASC, id ASC")
    suspend fun getAllTransactionsOnce(): List<StockTransactionEntity>

    @Query("SELECT * FROM stock_transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): StockTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tx: StockTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(txs: List<StockTransactionEntity>)

    @Query("DELETE FROM stock_transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM stock_transactions")
    suspend fun deleteAll()
}
