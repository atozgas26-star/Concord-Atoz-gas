package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.ProductCategory
import com.example.data.model.TransactionType
import com.example.data.model.UserRole

@Entity(
    tableName = "user_accounts",
    indices = [Index(value = ["username", "role"], unique = true)]
)
data class UserAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val displayName: String,
    val role: UserRole,
    val pin: String,
    val isActive: Boolean = true
)

@Entity(
    tableName = "vehicles",
    indices = [Index(value = ["name"], unique = true)]
)
data class VehicleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val staffUserId: Long,
    val isActive: Boolean = true
)

@Entity(
    tableName = "products",
    indices = [Index(value = ["category", "name"], unique = true)]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: ProductCategory,
    val name: String,
    val sortOrder: Int = 0,
    val isActive: Boolean = true
)

@Entity(
    tableName = "opening_stocks",
    indices = [Index(value = ["scope", "vehicleId", "productId"], unique = true)]
)
data class OpeningStockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scope: String, // "STORE" or "VEHICLE"
    val vehicleId: Long, // 0L for STORE
    val productId: Long,
    val openingFull: Int = 0,
    val openingEmpty: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "System"
)

@Entity(
    tableName = "stock_transactions",
    indices = [
        Index(value = ["businessDate"]),
        Index(value = ["productId"]),
        Index(value = ["vehicleId"])
    ]
)
data class StockTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessDate: String, // ISO yyyy-MM-dd
    val timestamp: Long = System.currentTimeMillis(),
    val type: TransactionType,
    val productId: Long,
    val vehicleId: Long? = null,
    val vehicleName: String? = null,
    val staffId: Long,
    val staffName: String,
    val quantity: Int,
    val notes: String = ""
)
