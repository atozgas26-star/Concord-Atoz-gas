package com.example.data.remote

import androidx.annotation.Keep
import com.example.data.local.OpeningStockEntity
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
    val activeWorkspaceId: String = "",
    val linkedStaffUsername: String = "",
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
    val pin: String = "1001",
    val active: Boolean = true,
    val memberUids: List<String> = emptyList(),
    val updatedByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): UserAccountEntity = UserAccountEntity(
        id = localId,
        username = username,
        displayName = displayName,
        role = UserRole.entries.firstOrNull { it.name == role } ?: UserRole.STAFF,
        pin = pin,
        isActive = active
    )

    companion object {
        fun toCreateMap(
            entity: UserAccountEntity,
            memberUids: List<String>,
            uid: String
        ): Map<String, Any> = mapOf(
            "localId" to entity.id,
            "username" to entity.username,
            "displayName" to entity.displayName,
            "role" to entity.role.name,
            "pin" to entity.pin,
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
data class CloudProduct(
    @DocumentId val id: String = "",
    val localId: Long = 0L,
    val category: String = ProductCategory.INDUSTRIAL_GAS.name,
    val name: String = "",
    val sortOrder: Int = 0,
    val active: Boolean = true,
    val memberUids: List<String> = emptyList(),
    val updatedByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): ProductEntity = ProductEntity(
        id = localId,
        category = ProductCategory.entries.firstOrNull { it.name == category } ?: ProductCategory.INDUSTRIAL_GAS,
        name = name,
        sortOrder = sortOrder,
        isActive = active
    )

    companion object {
        fun toCreateMap(
            entity: ProductEntity,
            memberUids: List<String>,
            uid: String
        ): Map<String, Any> = mapOf(
            "localId" to entity.id,
            "category" to entity.category.name,
            "name" to entity.name,
            "sortOrder" to entity.sortOrder,
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
            "notes" to entity.notes,
            "createdByUid" to uid,
            "memberUids" to memberUids,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>
    }
}
