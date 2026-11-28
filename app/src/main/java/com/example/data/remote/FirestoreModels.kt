package com.example.data.remote

import androidx.annotation.Keep
import com.example.auth.AdminCredentialCrypto
import com.example.data.local.OpeningStockEntity
import com.example.data.local.ProductCategoryEntity
import com.example.data.local.ProductEntity
import com.example.data.local.StockTransactionEntity
import com.example.data.local.UserAccountEntity
import com.example.data.local.VehicleEntity
import com.example.data.model.ProductCategory
import com.example.data.model.TransactionType
import com.example.data.model.UserRole
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class CloudUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val role: String = UserRole.STAFF.name,
    val activeWorkspaceId: String = "",
    val linkedStaffUsername: String = "",
    val linkedStaffRole: String = "",
    val autoDriveBackupEnabled: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudWorkspaceInvite(
    @DocumentId val id: String = "",
    val workspaceId: String = "",
    val ownerUid: String = "",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudWorkspace(
    @DocumentId val id: String = "",
    val companyName: String = "",
    val ownerUid: String = "",
    val memberUids: List<String> = emptyList(),
    val activeInviteCode: String = "",
    val lastJoinInviteCode: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudStaffAccount(
    @DocumentId val id: String = "",
    val localId: Long = 0L,
    val username: String = "",
    val displayName: String = "",
    val role: String = UserRole.STAFF.name,
    val pin: String = "",
    val pinHash: String = "",
    val active: Boolean = true,
    val memberUids: List<String> = emptyList(),
    val updatedByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun matchesPin(inputPin: String): Boolean {
        if (pinHash.isNotBlank()) {
            return AdminCredentialCrypto.verifyPassword(inputPin, pinHash)
        }
        return pin.isNotBlank() && pin == inputPin
    }

    fun resolvedPinHash(): String = when {
        pinHash.isNotBlank() -> pinHash
        pin.isNotBlank() -> AdminCredentialCrypto.hashPassword(pin)
        else -> ""
    }

    fun toEntity(): UserAccountEntity = UserAccountEntity(
        id = localId,
        username = username,
        displayName = displayName,
        role = UserRole.entries.firstOrNull { it.name == role } ?: UserRole.STAFF,
        pin = "",
        pinHash = resolvedPinHash(),
        isActive = active
    )

    companion object {
        fun toCreateMap(
            entity: UserAccountEntity,
            memberUids: List<String>,
            uid: String
        ): Map<String, Any> {
            val safeHash = entity.pinHash.ifBlank {
                if (entity.pin.isNotBlank()) AdminCredentialCrypto.hashPassword(entity.pin) else ""
            }
            return mapOf(
                "localId" to entity.id,
                "username" to entity.username,
                "displayName" to entity.displayName,
                "role" to entity.role.name,
                "pin" to "",
                "pinHash" to safeHash,
                "active" to entity.isActive,
                "memberUids" to memberUids,
                "updatedByUid" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        }
    }
}

@Keep
@IgnoreExtraProperties
data class CloudVehicle(
    @DocumentId val id: String = "",
    val localId: Long = 0L,
    val name: String = "",
    val staffUserId: Long = 0L,
    val active: Boolean = true,
    val memberUids: List<String> = emptyList(),
    val updatedByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): VehicleEntity = VehicleEntity(
        id = localId,
        name = name,
        staffUserId = staffUserId,
        isActive = active
    )

    companion object {
        fun toCreateMap(
            entity: VehicleEntity,
            memberUids: List<String>,
            uid: String
        ): Map<String, Any> = mapOf(
            "localId" to entity.id,
            "name" to entity.name,
            "staffUserId" to entity.staffUserId,
            "active" to entity.isActive,
            "memberUids" to memberUids,
            "updatedByUid" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

@Keep
@IgnoreExtraProperties
data class CloudProductCategory(
    @DocumentId val id: String = "",
    val localId: Long = 0L,
    val code: String = "",
    val name: String = "",
    val description: String = "",
    val iconKey: String = "GAS_CYLINDER",
    val categoryType: String = "INDUSTRIAL_REGULAR",
    val legacyEnum: String = ProductCategory.INDUSTRIAL_GAS.name,
    val sortOrder: Int = 10,
    val active: Boolean = true,
    val systemDefault: Boolean = false,
    val memberUids: List<String> = emptyList(),
    val updatedByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): ProductCategoryEntity = ProductCategoryEntity(
        id = localId,
        code = code,
        name = name,
        description = description,
        iconKey = iconKey,
        categoryType = categoryType,
        legacyEnum = ProductCategory.entries.firstOrNull { it.name == legacyEnum } ?: ProductCategory.INDUSTRIAL_GAS,
        sortOrder = sortOrder,
        isActive = active,
        isSystemDefault = systemDefault
    )

    companion object {
        fun toCreateMap(
            entity: ProductCategoryEntity,
            memberUids: List<String>,
            uid: String
        ): Map<String, Any> = mapOf(
            "localId" to entity.id,
            "code" to entity.code.take(64),
            "name" to entity.name.take(100),
            "description" to entity.description.take(240),
            "iconKey" to entity.iconKey.take(40),
            "categoryType" to entity.categoryType.take(40),
            "legacyEnum" to entity.legacyEnum.name,
            "sortOrder" to entity.sortOrder,
            "active" to entity.isActive,
            "systemDefault" to entity.isSystemDefault,
            "memberUids" to memberUids,
            "updatedByUid" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

@Keep
@IgnoreExtraProperties
data class CloudProduct(
    @DocumentId val id: String = "",
    val localId: Long = 0L,
    val category: String = ProductCategory.INDUSTRIAL_GAS.name,
    val categoryId: Long = 0L,
    val name: String = "",
    val sortOrder: Int = 0,
    val active: Boolean = true,
    val cylinderSizeKg: String = "",
    val unit: String = "Cylinder",
    val description: String = "",
    val iconKey: String = "CYLINDER",
    val memberUids: List<String> = emptyList(),
    val updatedByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): ProductEntity {
        val parsedCat = ProductCategory.entries.firstOrNull { it.name == category } ?: ProductCategory.INDUSTRIAL_GAS
        return ProductEntity(
            id = localId,
            category = parsedCat,
            categoryId = if (categoryId > 0L) categoryId else parsedCat.defaultCategoryId,
            name = name,
            sortOrder = sortOrder,
            isActive = active,
            cylinderSizeKg = cylinderSizeKg,
            unit = unit.ifBlank { "Cylinder" },
            description = description,
            iconKey = iconKey.ifBlank { "CYLINDER" }
        )
    }

    companion object {
        fun toCreateMap(
            entity: ProductEntity,
            memberUids: List<String>,
            uid: String
        ): Map<String, Any> = mapOf(
            "localId" to entity.id,
            "category" to entity.category.name,
            "categoryId" to entity.categoryId,
            "name" to entity.name,
            "sortOrder" to entity.sortOrder,
            "active" to entity.isActive,
            "cylinderSizeKg" to entity.cylinderSizeKg.take(40),
            "unit" to entity.unit.take(30),
            "description" to entity.description.take(240),
            "iconKey" to entity.iconKey.take(40),
            "memberUids" to memberUids,
            "updatedByUid" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

@Keep
@IgnoreExtraProperties
data class CloudOpeningStock(
    @DocumentId val id: String = "",
    val scope: String = "STORE",
    val vehicleId: Long = 0L,
    val productId: Long = 0L,
    val openingFull: Int = 0,
    val openingEmpty: Int = 0,
    val updatedBy: String = "System",
    val memberUids: List<String> = emptyList(),
    val updatedByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(existingLocalId: Long = 0L): OpeningStockEntity = OpeningStockEntity(
        id = existingLocalId,
        scope = scope,
        vehicleId = vehicleId,
        productId = productId,
        openingFull = openingFull,
        openingEmpty = openingEmpty,
        updatedAt = updatedAt?.toDate()?.time ?: System.currentTimeMillis(),
        updatedBy = updatedBy
    )

    companion object {
        fun docIdFor(scope: String, vehicleId: Long, productId: Long): String =
            "open_${scope}_${vehicleId}_$productId"

        fun toCreateMap(
            entity: OpeningStockEntity,
            memberUids: List<String>,
            uid: String
        ): Map<String, Any> = mapOf(
            "scope" to entity.scope,
            "vehicleId" to entity.vehicleId,
            "productId" to entity.productId,
            "openingFull" to entity.openingFull,
            "openingEmpty" to entity.openingEmpty,
            "updatedBy" to entity.updatedBy,
            "memberUids" to memberUids,
            "updatedByUid" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

@Keep
@IgnoreExtraProperties
data class CloudStockTransaction(
    @DocumentId val id: String = "",
    val clientTxId: String = "",
    val businessDate: String = "",
    val clientTimestampMs: Long = 0L,
    val type: String = TransactionType.VEHICLE_CUSTOMER_SALE.name,
    val productId: Long = 0L,
    val vehicleId: Long? = null,
    val vehicleName: String? = null,
    val staffId: Long = 0L,
    val staffName: String = "",
    val quantity: Int = 0,
    val purchaseCompanyName: String = "",
    val customerName: String = "",
    val notes: String = "",
    val createdByUid: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): StockTransactionEntity? {
        val parsedType = TransactionType.entries.firstOrNull { it.name == type } ?: return null
        val slug = clientTxId.ifBlank { id }
        if (slug.isBlank()) return null
        return StockTransactionEntity(
            id = 0L,
            businessDate = businessDate,
            timestamp = clientTimestampMs.takeIf { it > 0L } ?: (createdAt?.toDate()?.time ?: System.currentTimeMillis()),
            type = parsedType,
            productId = productId,
            vehicleId = vehicleId,
            vehicleName = vehicleName,
            staffId = staffId,
            staffName = staffName,
            quantity = quantity,
            purchaseCompanyName = purchaseCompanyName,
            customerName = customerName,
            notes = notes,
            remoteId = slug
        )
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun toCreateMap(
            entity: StockTransactionEntity,
            remoteTxId: String,
            memberUids: List<String>,
            uid: String
        ): Map<String, Any> = mapOf(
            "clientTxId" to remoteTxId,
            "businessDate" to entity.businessDate,
            "clientTimestampMs" to entity.timestamp,
            "type" to entity.type.name,
            "productId" to entity.productId,
            "vehicleId" to entity.vehicleId,
            "vehicleName" to entity.vehicleName,
            "staffId" to entity.staffId,
            "staffName" to entity.staffName,
            "quantity" to entity.quantity,
            "purchaseCompanyName" to entity.purchaseCompanyName.takeIf { it.isNotBlank() },
            "customerName" to entity.customerName.takeIf { it.isNotBlank() },
            "notes" to entity.notes,
            "createdByUid" to uid,
            "memberUids" to memberUids,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class CloudAdminSettings(
    @DocumentId val id: String = "",
    val settingId: String = "drive_backup",
    val autoDriveBackupEnabled: Boolean = false,
    val lastDriveBackupAtMs: Long = 0L,
    val lastDriveFileId: String = "",
    val lastDriveBackupStatus: String = "",
    val driveAccountEmail: String = "",
    val themePreset: String = "",
    val autoLogoutMode: String = "",
    val adminLoginUsername: String = "",
    val adminPasswordHash: String = "",
    val adminRecoveryEmail: String = "",
    val ownerViewPasswordHash: String = "",
    val updatedByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudAdminReportSnapshot(
    @DocumentId val id: String = "",
    val reportId: String = "",
    val reportType: String = "",
    val reportTitle: String = "",
    val subtitle: String = "",
    val businessDate: String = "",
    val yearMonth: String = "",
    val generatedBy: String = "",
    val rowCount: Int = 0,
    val summaryTotals: String = "",
    val driveFileId: String = "",
    val createdByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudStaffLoginEvent(
    @DocumentId val id: String = "",
    val eventId: String = "",
    val staffId: Long = 0L,
    val staffName: String = "",
    val vehicleName: String = "",
    val loginTimestampMs: Long = 0L,
    val loginByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudStaffAttendance(
    @DocumentId val id: String = "",
    val attendanceId: String = "",
    val businessDate: String = "",
    val staffId: Long = 0L,
    val staffName: String = "",
    val vehicleName: String = "",
    val dutyStartMs: Long = 0L,
    val lunchOutMs: Long = 0L,
    val lunchInMs: Long = 0L,
    val dutyCloseMs: Long = 0L,
    val updatedAtMs: Long = 0L,
    val updatedByUid: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(existingId: Long = 0L): com.example.data.local.StaffAttendanceEntity =
        com.example.data.local.StaffAttendanceEntity(
            id = existingId,
            remoteId = attendanceId.ifBlank { id },
            businessDate = businessDate,
            staffId = staffId,
            staffName = staffName,
            vehicleName = vehicleName,
            dutyStartMs = dutyStartMs,
            lunchOutMs = lunchOutMs,
            lunchInMs = lunchInMs,
            dutyCloseMs = dutyCloseMs,
            updatedAtMs = updatedAtMs.takeIf { it > 0L } ?: System.currentTimeMillis()
        )
}

@Keep
@IgnoreExtraProperties
data class CloudPendingStoreTransfer(
    @DocumentId val id: String = "",
    val requestId: String = "",
    val businessDate: String = "",
    val direction: String = "",
    val vehicleId: Long = 0L,
    val vehicleName: String = "",
    val productId: Long = 0L,
    val productName: String = "",
    val productCategory: String = "",
    val quantity: Int = 0,
    val staffId: Long = 0L,
    val staffName: String = "",
    val notes: String = "",
    val status: String = "PENDING",
    val requestedAtMs: Long = 0L,
    val reviewedAtMs: Long = 0L,
    val reviewedBy: String = "",
    val reviewNotes: String = "",
    val createdByUid: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(existingId: Long = 0L): com.example.data.local.PendingStoreTransferEntity =
        com.example.data.local.PendingStoreTransferEntity(
            id = existingId,
            remoteId = requestId.ifBlank { id },
            businessDate = businessDate,
            direction = direction,
            vehicleId = vehicleId,
            vehicleName = vehicleName,
            productId = productId,
            productName = productName,
            productCategory = productCategory,
            quantity = quantity,
            staffId = staffId,
            staffName = staffName,
            notes = notes,
            status = status,
            requestedAtMs = requestedAtMs.takeIf { it > 0L } ?: System.currentTimeMillis(),
            reviewedAtMs = reviewedAtMs,
            reviewedBy = reviewedBy,
            reviewNotes = reviewNotes
        )
}

@Keep
@IgnoreExtraProperties
data class CloudAppNotification(
    @DocumentId val id: String = "",
    val notifId: String = "",
    val targetScope: String = "ALL",
    val targetStaffId: Long = 0L,
    val category: String = "INFO",
    val title: String = "",
    val message: String = "",
    val isUrgent: Boolean = false,
    val createdAtMs: Long = 0L,
    val createdByUid: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toEntity(existingId: Long = 0L, existingRead: Boolean = false): com.example.data.local.AppNotificationEntity =
        com.example.data.local.AppNotificationEntity(
            id = existingId,
            remoteId = notifId.ifBlank { id },
            targetScope = targetScope,
            targetStaffId = targetStaffId,
            category = category,
            title = title,
            message = message,
            isUrgent = isUrgent,
            isRead = existingRead,
            createdAtMs = createdAtMs.takeIf { it > 0L } ?: System.currentTimeMillis()
        )
}

@Keep
@IgnoreExtraProperties
data class CloudChatMessage(
    @DocumentId val id: String = "",
    val messageId: String = "",
    val channel: String = "TEAM_CHAT",
    val senderId: Long = 0L,
    val senderName: String = "",
    val senderRole: String = "STAFF",
    val message: String = "",
    val isImportant: Boolean = false,
    val createdAtMs: Long = 0L,
    val createdByUid: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toEntity(): com.example.data.local.ChatMessageEntity =
        com.example.data.local.ChatMessageEntity(
            id = 0L,
            remoteId = messageId.ifBlank { id },
            channel = channel,
            senderId = senderId,
            senderName = senderName,
            senderRole = senderRole,
            message = message,
            isImportant = isImportant,
            createdAtMs = createdAtMs.takeIf { it > 0L } ?: System.currentTimeMillis()
        )
}

@Keep
@IgnoreExtraProperties
data class CloudTypingStatus(
    @DocumentId val id: String = "",
    val typingId: String = "",
    val channel: String = "TEAM_CHAT",
    val senderName: String = "",
    val senderRole: String = "STAFF",
    val isTyping: Boolean = false,
    val updatedAtMs: Long = 0L,
    val updatedByUid: String = ""
)
