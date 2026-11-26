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
        Index(value = ["vehicleId"]),
        Index(value = ["remoteId"], unique = true)
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
    val purchaseCompanyName: String = "",
    val customerName: String = "",
    val notes: String = "",
    val remoteId: String? = null
)

@Entity(
    tableName = "staff_attendance",
    indices = [
        Index(value = ["businessDate", "staffId"], unique = true),
        Index(value = ["remoteId"], unique = true)
    ]
)
data class StaffAttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: String,
    val businessDate: String, // ISO yyyy-MM-dd
    val staffId: Long,
    val staffName: String,
    val vehicleName: String = "",
    val dutyStartMs: Long = 0L,
    val lunchOutMs: Long = 0L,
    val lunchInMs: Long = 0L,
    val dutyCloseMs: Long = 0L,
    val updatedAtMs: Long = System.currentTimeMillis()
) {
    fun breakDurationMinutes(): Long {
        if (lunchOutMs <= 0L || lunchInMs <= lunchOutMs) return 0L
        return ((lunchInMs - lunchOutMs) / 60_000L).coerceAtLeast(0L)
    }

    fun netWorkMinutes(nowMs: Long = System.currentTimeMillis()): Long {
        if (dutyStartMs <= 0L) return 0L
        val endMs = when {
            dutyCloseMs > dutyStartMs -> dutyCloseMs
            lunchOutMs > dutyStartMs && lunchInMs <= 0L -> lunchOutMs
            else -> nowMs.coerceAtLeast(dutyStartMs)
        }
        val grossMinutes = ((endMs - dutyStartMs) / 60_000L).coerceAtLeast(0L)
        return (grossMinutes - breakDurationMinutes()).coerceAtLeast(0L)
    }

    fun statusLabel(): String = when {
        dutyStartMs <= 0L -> "Not Punched In"
        dutyCloseMs > 0L -> "Duty Closed"
        lunchOutMs > 0L && lunchInMs <= 0L -> "On Lunch / Break"
        lunchInMs > 0L -> "On Duty (Afternoon)"
        else -> "On Duty (Morning)"
    }
}

@Entity(
    tableName = "pending_store_transfers",
    indices = [
        Index(value = ["remoteId"], unique = true),
        Index(value = ["status"]),
        Index(value = ["businessDate"])
    ]
)
data class PendingStoreTransferEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: String,
    val businessDate: String,
    val direction: String, // StoreTransferDirection.name
    val vehicleId: Long,
    val vehicleName: String,
    val productId: Long,
    val productName: String,
    val productCategory: String,
    val quantity: Int,
    val staffId: Long,
    val staffName: String,
    val notes: String = "",
    val status: String = "PENDING", // ApprovalStatus.name
    val requestedAtMs: Long = System.currentTimeMillis(),
    val reviewedAtMs: Long = 0L,
    val reviewedBy: String = "",
    val reviewNotes: String = ""
)

@Entity(
    tableName = "app_notifications",
    indices = [
        Index(value = ["remoteId"], unique = true),
        Index(value = ["createdAtMs"]),
        Index(value = ["targetScope"])
    ]
)
data class AppNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: String,
    val targetScope: String, // ALL, ADMIN_OWNER, STAFF
    val targetStaffId: Long = 0L, // >0 when directed to a specific staff member
    val category: String, // NotificationCategory.name
    val title: String,
    val message: String,
    val isUrgent: Boolean = false,
    val isRead: Boolean = false,
    val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "chat_messages",
    indices = [
        Index(value = ["remoteId"], unique = true),
        Index(value = ["channel"]),
        Index(value = ["createdAtMs"])
    ]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: String,
    val channel: String, // TEAM_CHAT or ADMIN_OWNER
    val senderId: Long,
    val senderName: String,
    val senderRole: String, // ADMIN, OWNER_VIEW, STAFF
    val message: String,
    val isImportant: Boolean = false,
    val createdAtMs: Long = System.currentTimeMillis()
)
