package com.example.data.repository

import androidx.room.withTransaction
import com.example.auth.AdminCredentialCrypto
import com.example.auth.UaePhoneValidator
import com.example.data.local.AppNotificationEntity
import com.example.data.local.AuthorizedMobileEntity
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ConcordDatabase
import com.example.data.local.DifferentGasReturnEntity
import com.example.data.local.OpeningStockEntity
import com.example.data.local.PendingStoreTransferEntity
import com.example.data.local.ProductCategoryEntity
import com.example.data.local.ProductEntity
import com.example.data.local.RegisteredDeviceEntity
import com.example.data.local.SecurityAuditLogEntity
import com.example.data.local.StaffAttendanceEntity
import com.example.data.local.StockTransactionEntity
import com.example.data.local.UserAccountEntity
import com.example.data.local.VehicleEntity
import com.example.data.model.AuthSession
import com.example.data.model.BatchProductEntry
import com.example.data.model.CompanyStockRow
import com.example.data.model.CustomerChangeMode
import com.example.data.model.CustomerExchangeItem
import com.example.data.model.CustomerPendingRow
import com.example.data.model.CustomerReturnHistoryGroup
import com.example.data.model.DashboardSummary
import com.example.data.model.DifferentGasLineItem
import com.example.data.model.GasNamingSanitizer
import com.example.data.model.MonthlyDailySaleRow
import com.example.data.model.OperationResult
import com.example.data.model.ProductCategory
import com.example.data.model.ProductSalesRow
import com.example.data.model.StaffFeaturePermission
import com.example.data.model.StaffReturnHistoryGroup
import com.example.data.model.StaffSalesRow
import com.example.data.model.StockCalculator
import com.example.data.model.StoreStockRow
import com.example.data.model.TransactionDisplayItem
import com.example.data.model.TransactionType
import com.example.data.model.UserRole
import com.example.data.model.VehicleOverview
import com.example.data.model.VehicleStockRow
import com.example.data.remote.CloudSyncRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InventoryRepository(
    private val db: ConcordDatabase,
    private val cloudSync: CloudSyncRepository? = null
) {

    private val userDao = db.userDao()
    private val vehicleDao = db.vehicleDao()
    private val productCategoryDao = db.productCategoryDao()
    private val productDao = db.productDao()
    private val openingDao = db.openingStockDao()
    private val txDao = db.transactionDao()
    private val attendanceDao = db.staffAttendanceDao()
    private val pendingStoreTransferDao = db.pendingStoreTransferDao()
    private val notificationDao = db.appNotificationDao()
    private val chatMessageDao = db.chatMessageDao()
    private val differentGasReturnDao = db.differentGasReturnDao()
    private val authorizedMobileDao = db.authorizedMobileDao()
    private val registeredDeviceDao = db.registeredDeviceDao()
    private val securityAuditLogDao = db.securityAuditLogDao()

    val usersFlow: Flow<List<UserAccountEntity>> = userDao.observeAllUsers()
    val vehiclesFlow: Flow<List<VehicleEntity>> = vehicleDao.observeActiveVehicles()
    val categoriesFlow: Flow<List<ProductCategoryEntity>> = productCategoryDao.observeAll()
    val activeCategoriesFlow: Flow<List<ProductCategoryEntity>> = productCategoryDao.observeActive()
    val activeProductsFlow: Flow<List<ProductEntity>> = productDao.observeActiveProducts()
    val allProductsFlow: Flow<List<ProductEntity>> = productDao.observeAllProducts()
    val openingsFlow: Flow<List<OpeningStockEntity>> = openingDao.observeAllOpenings()
    val transactionsFlow: Flow<List<StockTransactionEntity>> = txDao.observeAllTransactions()
    val allAttendanceFlow: Flow<List<StaffAttendanceEntity>> = attendanceDao.observeAllAttendance()
    val allPendingStoreTransfersFlow: Flow<List<PendingStoreTransferEntity>> = pendingStoreTransferDao.observeAll()
    val allNotificationsFlow: Flow<List<AppNotificationEntity>> = notificationDao.observeAll()
    val allChatMessagesFlow: Flow<List<ChatMessageEntity>> = chatMessageDao.observeAll()
    val differentGasReturnsFlow: Flow<List<DifferentGasReturnEntity>> = differentGasReturnDao.observeAll()
    val authorizedMobilesFlow: Flow<List<AuthorizedMobileEntity>> = authorizedMobileDao.observeAll()
    val registeredDevicesFlow: Flow<List<RegisteredDeviceEntity>> = registeredDeviceDao.observeAll()
    val securityAuditLogsFlow: Flow<List<SecurityAuditLogEntity>> = securityAuditLogDao.observeRecent()

    val customerReturnHistoryFlow: Flow<List<CustomerReturnHistoryGroup>> = differentGasReturnsFlow.map { records ->
        records
            .groupBy { it.customerName.trim().lowercase(Locale.US) }
            .values
            .map { group ->
                val sorted = group.sortedByDescending { it.timestamp }
                val latest = sorted.first()
                CustomerReturnHistoryGroup(
                    customerName = latest.customerName,
                    customerPhone = sorted.firstOrNull { it.customerPhone.isNotBlank() }?.customerPhone.orEmpty(),
                    totalTransactions = sorted.size,
                    totalDeliveredFull = sorted.sumOf { it.totalDeliveredQty },
                    totalReturnedEmpty = sorted.sumOf { it.totalReturnedQty },
                    lastTimestampMs = latest.timestamp,
                    records = sorted
                )
            }
            .sortedByDescending { it.lastTimestampMs }
    }.flowOn(Dispatchers.Default)

    val staffReturnHistoryFlow: Flow<List<StaffReturnHistoryGroup>> = differentGasReturnsFlow.map { records ->
        records
            .groupBy { it.staffId }
            .values
            .map { group ->
                val sorted = group.sortedByDescending { it.timestamp }
                val latest = sorted.first()
                StaffReturnHistoryGroup(
                    staffId = latest.staffId,
                    staffName = latest.staffName,
                    vehicleName = sorted.firstOrNull { !it.vehicleName.isNullOrBlank() }?.vehicleName,
                    totalTransactions = sorted.size,
                    totalDeliveredFull = sorted.sumOf { it.totalDeliveredQty },
                    totalReturnedEmpty = sorted.sumOf { it.totalReturnedQty },
                    lastTimestampMs = latest.timestamp,
                    records = sorted
                )
            }
            .sortedByDescending { it.lastTimestampMs }
    }.flowOn(Dispatchers.Default)

    val recentPurchaseCompaniesFlow: Flow<List<String>> = transactionsFlow.map { txs ->
        val seen = LinkedHashSet<String>()
        val dedupLower = HashSet<String>()
        for (tx in txs) {
            val company = tx.purchaseCompanyName.trim()
            if (company.isNotEmpty() && dedupLower.add(company.lowercase(Locale.US))) {
                seen.add(company)
                if (seen.size >= 12) break
            }
        }
        seen.toList()
    }.flowOn(Dispatchers.Default)

    val recentCustomerNamesFlow: Flow<List<String>> = transactionsFlow.map { txs ->
        val seen = LinkedHashSet<String>()
        val dedupLower = HashSet<String>()
        for (tx in txs) {
            val customer = tx.customerName.trim()
            if (customer.isNotEmpty() && dedupLower.add(customer.lowercase(Locale.US))) {
                seen.add(customer)
                if (seen.size >= 16) break
            }
        }
        seen.toList()
    }.flowOn(Dispatchers.Default)

    suspend fun initializeDatabase() {
        db.ensureSeeded()
    }

    suspend fun getUserById(userId: Long): UserAccountEntity? =
        userDao.getUserById(userId)

    fun verifyUserPin(user: UserAccountEntity, inputPin: String): Boolean {
        val clean = inputPin.trim()
        if (clean.isEmpty()) return false
        if (user.pinHash.isNotBlank()) {
            return AdminCredentialCrypto.verifyPassword(clean, user.pinHash)
        }
        return user.pin.isNotBlank() && user.pin == clean
    }

    suspend fun authenticate(
        userId: Long,
        pin: String,
        expectedRole: UserRole? = null
    ): AuthSession? {
        val user = userDao.getUserById(userId) ?: return null
        if (!user.isActive || !verifyUserPin(user, pin)) return null
        if (expectedRole != null && user.role != expectedRole) return null
        // Enforce unique PIN invariant across active accounts
        val activeWithSamePin = userDao.getAllUsersOnce().filter { it.isActive && verifyUserPin(it, pin) }
        if (activeWithSamePin.size > 1) return null
        val vehicle = if (user.role == UserRole.STAFF) {
            vehicleDao.getVehicleByStaffUserId(user.id)
        } else {
            null
        }
        return AuthSession(user = user, assignedVehicle = vehicle)
    }

    suspend fun getSessionForUserId(userId: Long): AuthSession? {
        val user = userDao.getUserById(userId) ?: return null
        if (!user.isActive) return null
        val vehicle = if (user.role == UserRole.STAFF) {
            vehicleDao.getVehicleByStaffUserId(user.id)
        } else {
            null
        }
        return AuthSession(user = user, assignedVehicle = vehicle)
    }

    suspend fun updateUserPin(targetUserId: Long, newPin: String, actorName: String = "Admin"): OperationResult {
        if (newPin.length != 4 || !newPin.all { it.isDigit() }) {
            return OperationResult.ValidationError("PIN must be exactly 4 digits.")
        }
        val allUsers = userDao.getAllUsersOnce()
        val targetUser = allUsers.firstOrNull { it.id == targetUserId }
            ?: return OperationResult.ValidationError("Account not found.")
        val localCollision = allUsers.firstOrNull { it.id != targetUserId && verifyUserPin(it, newPin) }
        if (localCollision != null) {
            return OperationResult.ValidationError(
                "This PIN is already assigned to ${localCollision.displayName} (${localCollision.role.name}). Each Admin and Staff must have a unique PIN."
            )
        }
        val cloudCollision = cloudSync?.findCloudPinCollisionDisplayName(targetUser.username, newPin)
        if (cloudCollision != null) {
            return OperationResult.ValidationError(
                "This PIN is already registered in cloud for $cloudCollision. Choose a unique PIN."
            )
        }
        val saltedHash = AdminCredentialCrypto.hashPassword(newPin)
        userDao.updatePinHash(targetUserId, saltedHash)
        userDao.getUserById(targetUserId)?.let { updatedUser ->
            cloudSync?.pushStaffAccount(updatedUser)
        }
        logSecurityEvent(
            eventType = "PIN_UPDATED",
            role = UserRole.ADMIN.name,
            actorName = actorName,
            details = "Individual PIN securely updated (salted PBKDF2 hash) for ${targetUser.displayName}.",
            isAlert = false
        )
        return OperationResult.Success("Individual PIN updated securely for ${targetUser.displayName}.")
    }

    suspend fun setUserActiveStatus(
        targetUserId: Long,
        isActive: Boolean,
        actor: UserAccountEntity
    ): OperationResult {
        if (actor.role != UserRole.ADMIN) {
            return OperationResult.ValidationError("Only Admin can change account status.")
        }
        val target = userDao.getUserById(targetUserId)
            ?: return OperationResult.ValidationError("Account not found.")
        if (target.role == UserRole.ADMIN || target.role == UserRole.OWNER_VIEW) {
            return OperationResult.ValidationError("Protected Admin/Owner accounts cannot be deactivated here.")
        }
        userDao.updateIsActive(targetUserId, isActive)
        userDao.getUserById(targetUserId)?.let { updated ->
            cloudSync?.pushStaffAccount(updated)
        }
        logSecurityEvent(
            eventType = if (isActive) "ACCOUNT_ACTIVATED" else "ACCOUNT_DEACTIVATED",
            role = actor.role.name,
            actorName = actor.displayName,
            details = "${target.displayName} account set to active=$isActive",
            isAlert = !isActive
        )
        return OperationResult.Success("${target.displayName} is now ${if (isActive) "Active" else "Inactive"}.")
    }

    suspend fun updateStaffName(
        targetUserId: Long,
        newDisplayName: String,
        newVehicleName: String? = null
    ): OperationResult {
        val cleanName = newDisplayName.trim().take(100)
        if (cleanName.isEmpty()) {
            return OperationResult.ValidationError("Staff name cannot be empty.")
        }

        var updatedUser: UserAccountEntity? = null
        var updatedVehicle: VehicleEntity? = null

        val result = db.withTransaction {
            val user = userDao.getUserById(targetUserId)
                ?: return@withTransaction OperationResult.ValidationError("Staff account not found.")

            val linkedVehicle = if (user.role == UserRole.STAFF) {
                vehicleDao.getVehicleByStaffUserId(user.id)
            } else {
                null
            }

            if (linkedVehicle != null && newVehicleName != null) {
                val cleanVehicle = newVehicleName.trim().take(64)
                if (cleanVehicle.isEmpty()) {
                    return@withTransaction OperationResult.ValidationError("Vehicle name cannot be empty.")
                }
                val otherVehicles = vehicleDao.getAllVehiclesOnce().filter { it.id != linkedVehicle.id }
                if (otherVehicles.any { it.name.equals(cleanVehicle, ignoreCase = true) }) {
                    return@withTransaction OperationResult.ValidationError("Vehicle name '$cleanVehicle' is already in use.")
                }
                if (cleanVehicle != linkedVehicle.name) {
                    vehicleDao.updateName(linkedVehicle.id, cleanVehicle)
                    updatedVehicle = linkedVehicle.copy(name = cleanVehicle)
                }
            }

            userDao.updateDisplayName(user.id, cleanName)
            updatedUser = user.copy(displayName = cleanName)
            val vehNote = updatedVehicle?.let { " & vehicle '${it.name}'" } ?: ""
            OperationResult.Success("Updated name to '$cleanName'$vehNote.")
        }

        if (result is OperationResult.Success && cloudSync != null) {
            updatedUser?.let { cloudSync.pushStaffAccount(it) }
            updatedVehicle?.let { cloudSync.pushVehicle(it) }
        }
        return result
    }

    suspend fun addStaffAndVehicle(
        staffName: String,
        vehicleName: String,
        pin: String
    ): OperationResult {
        val cleanStaff = staffName.trim().take(100)
        val cleanVehicle = vehicleName.trim().uppercase().take(64)
        val cleanPin = pin.trim()
        if (cleanStaff.isEmpty()) {
            return OperationResult.ValidationError("Staff name cannot be empty.")
        }
        if (cleanVehicle.isEmpty()) {
            return OperationResult.ValidationError("Vehicle name cannot be empty.")
        }
        if (cleanPin.length != 4 || !cleanPin.all { it.isDigit() }) {
            return OperationResult.ValidationError("PIN must be exactly 4 digits.")
        }

        val cloudPinCollision = cloudSync?.findCloudPinCollisionDisplayName(null, cleanPin)
        if (cloudPinCollision != null) {
            return OperationResult.ValidationError(
                "PIN '$cleanPin' is already registered in cloud for $cloudPinCollision. Choose a unique 4-digit PIN."
            )
        }

        var createdUser: UserAccountEntity? = null
        var createdVehicle: VehicleEntity? = null

        val result = db.withTransaction {
            val existingUsers = userDao.getAllUsersOnce()
            val pinCollision = existingUsers.firstOrNull { verifyUserPin(it, cleanPin) }
            if (pinCollision != null) {
                return@withTransaction OperationResult.ValidationError(
                    "This PIN is already assigned to ${pinCollision.displayName} (${pinCollision.role.name}). Each login requires a unique PIN."
                )
            }
            val existingVehicles = vehicleDao.getAllVehiclesOnce()
            if (existingVehicles.any { it.name.equals(cleanVehicle, ignoreCase = true) }) {
                return@withTransaction OperationResult.ValidationError("Vehicle '$cleanVehicle' already exists.")
            }

            val baseSlug = cleanStaff.lowercase(Locale.US)
                .replace(Regex("[^a-z0-9]+"), "_")
                .trim('_')
                .ifEmpty { "staff" }
                .take(40)
            var candidateUsername = baseSlug
            var suffix = 2
            while (existingUsers.any { it.username.equals(candidateUsername, ignoreCase = true) }) {
                candidateUsername = "${baseSlug}_$suffix"
                suffix++
            }

            val newUserDraft = UserAccountEntity(
                username = candidateUsername,
                displayName = cleanStaff,
                role = UserRole.STAFF,
                pin = "",
                pinHash = AdminCredentialCrypto.hashPassword(cleanPin),
                isActive = true
            )
            val newUserId = userDao.insert(newUserDraft)
            val savedUser = newUserDraft.copy(id = newUserId)

            val newVehicleDraft = VehicleEntity(
                name = cleanVehicle,
                staffUserId = newUserId,
                isActive = true
            )
            val newVehicleId = vehicleDao.insert(newVehicleDraft)
            val savedVehicle = newVehicleDraft.copy(id = newVehicleId)

            createdUser = savedUser
            createdVehicle = savedVehicle
            OperationResult.Success("Added staff '$cleanStaff' with vehicle '$cleanVehicle' (secure hashed PIN set).")
        }

        if (result is OperationResult.Success && cloudSync != null) {
            createdUser?.let { cloudSync.pushStaffAccount(it) }
            createdVehicle?.let { cloudSync.pushVehicle(it) }
        }
        return result
    }

    // --- Dynamic Product Master: Category Folders (Permanent Category IDs) ---

    suspend fun createCategory(
        name: String,
        description: String = "",
        iconKey: String = "GAS_CYLINDER",
        categoryType: String = "INDUSTRIAL_REGULAR",
        isActive: Boolean = true,
        actor: UserAccountEntity
    ): OperationResult {
        if (actor.role != UserRole.ADMIN) {
            return OperationResult.ValidationError("Only Admin can create product category folders.")
        }
        val cleanName = GasNamingSanitizer.sanitizeCompanyOrProductName(name).take(100)
        val cleanDesc = description.trim().take(240)
        val cleanIcon = iconKey.trim().uppercase(Locale.US).take(40).ifBlank { "GAS_CYLINDER" }
        val cleanType = categoryType.trim().uppercase(Locale.US).take(40).ifBlank { "CUSTOM" }
        if (cleanName.isEmpty()) {
            return OperationResult.ValidationError("Category name cannot be empty.")
        }
        val mappedEnum = when (cleanType) {
            "LPG", "COOKING_GAS" -> ProductCategory.COOKING_GAS
            "CUTTING_GAS", "MCG_CUTTING_GAS" -> ProductCategory.MCG_CUTTING_GAS
            "SMALL_48_CFT" -> ProductCategory.SMALL_48_CFT
            else -> ProductCategory.INDUSTRIAL_GAS
        }
        var createdCategory: ProductCategoryEntity? = null
        val result = db.withTransaction {
            val allCats = productCategoryDao.getAllOnce()
            if (allCats.any { it.name.equals(cleanName, ignoreCase = true) }) {
                return@withTransaction OperationResult.ValidationError("Category '$cleanName' already exists.")
            }
            val nextSort = (allCats.maxOfOrNull { it.sortOrder } ?: 4) + 1
            val baseCode = cleanName.uppercase(Locale.US)
                .replace(Regex("[^A-Z0-9]+"), "_")
                .trim('_')
                .ifEmpty { "CUSTOM_CAT" }
                .take(48)
            var candidateCode = baseCode
            var suffix = 2
            while (allCats.any { it.code.equals(candidateCode, ignoreCase = true) }) {
                candidateCode = "${baseCode}_$suffix"
                suffix++
            }
            val nowMs = System.currentTimeMillis()
            val draft = ProductCategoryEntity(
                code = candidateCode,
                name = cleanName,
                description = cleanDesc,
                iconKey = cleanIcon,
                categoryType = cleanType,
                legacyEnum = mappedEnum,
                sortOrder = nextSort,
                isActive = isActive,
                isSystemDefault = false,
                createdAtMs = nowMs,
                updatedAtMs = nowMs
            )
            val newId = productCategoryDao.insert(draft)
            createdCategory = draft.copy(id = newId)
            OperationResult.Success("Created category folder '$cleanName'.")
        }
        if (result is OperationResult.Success && createdCategory != null) {
            cloudSync?.pushProductCategory(createdCategory!!)
            logSecurityEvent(
                eventType = "CATEGORY_CREATED",
                role = actor.role.name,
                actorName = actor.displayName,
                details = "Created category folder '${createdCategory!!.name}' (ID=${createdCategory!!.id})",
                isAlert = false
            )
        }
        return result
    }

    suspend fun updateCategory(
        categoryId: Long,
        name: String,
        description: String,
        iconKey: String,
        categoryType: String,
        isActive: Boolean,
        actor: UserAccountEntity
    ): OperationResult {
        if (actor.role != UserRole.ADMIN) {
            return OperationResult.ValidationError("Only Admin can edit product category folders.")
        }
        val cleanName = GasNamingSanitizer.sanitizeCompanyOrProductName(name).take(100)
        val cleanDesc = description.trim().take(240)
        val cleanIcon = iconKey.trim().uppercase(Locale.US).take(40).ifBlank { "GAS_CYLINDER" }
        val cleanType = categoryType.trim().uppercase(Locale.US).take(40).ifBlank { "CUSTOM" }
        if (cleanName.isEmpty()) {
            return OperationResult.ValidationError("Category name cannot be empty.")
        }
        var updatedCategory: ProductCategoryEntity? = null
        val result = db.withTransaction {
            val allCats = productCategoryDao.getAllOnce()
            val target = allCats.firstOrNull { it.id == categoryId }
                ?: return@withTransaction OperationResult.ValidationError("Category folder not found.")
            if (allCats.any { it.id != categoryId && it.name.equals(cleanName, ignoreCase = true) }) {
                return@withTransaction OperationResult.ValidationError("Another category named '$cleanName' already exists.")
            }
            // Permanent Category ID and code remain immutable so stock & historical records stay intact
            val updated = target.copy(
                name = cleanName,
                description = cleanDesc,
                iconKey = cleanIcon,
                categoryType = if (target.isSystemDefault) target.categoryType else cleanType,
                isActive = isActive,
                updatedAtMs = System.currentTimeMillis()
            )
            productCategoryDao.update(updated)
            updatedCategory = updated
            OperationResult.Success("Updated category folder '$cleanName' (ID #${target.id} preserved).")
        }
        if (result is OperationResult.Success && updatedCategory != null) {
            cloudSync?.pushProductCategory(updatedCategory!!)
            logSecurityEvent(
                eventType = "CATEGORY_UPDATED",
                role = actor.role.name,
                actorName = actor.displayName,
                details = "Updated category folder ID=${updatedCategory!!.id} -> '${updatedCategory!!.name}' (active=${updatedCategory!!.isActive})",
                isAlert = false
            )
        }
        return result
    }

    suspend fun toggleCategoryActive(
        category: ProductCategoryEntity,
        actor: UserAccountEntity
    ): OperationResult {
        return updateCategory(
            categoryId = category.id,
            name = category.name,
            description = category.description,
            iconKey = category.iconKey,
            categoryType = category.categoryType,
            isActive = !category.isActive,
            actor = actor
        )
    }

    /**
     * Reorders Product Master category folders via Drag-and-Drop or Up/Down arrows.
     * Updates only [ProductCategoryEntity.sortOrder] and [ProductCategoryEntity.updatedAtMs];
     * permanent category IDs, product IDs, stock balances, and historical transactions remain untouched.
     */
    suspend fun reorderCategories(
        orderedCategoryIds: List<Long>,
        actor: UserAccountEntity
    ): OperationResult {
        if (actor.role != UserRole.ADMIN) {
            return OperationResult.ValidationError("Only Admin can reorder product category folders.")
        }
        if (orderedCategoryIds.isEmpty()) {
            return OperationResult.ValidationError("No category order specified.")
        }
        val updatedCategories = mutableListOf<ProductCategoryEntity>()
        val nowMs = System.currentTimeMillis()
        val result = db.withTransaction {
            val allCats = productCategoryDao.getAllOnce()
            if (allCats.isEmpty()) {
                return@withTransaction OperationResult.ValidationError("No categories found to reorder.")
            }
            val byId = allCats.associateBy { it.id }
            val explicitOrder = orderedCategoryIds.distinct().mapNotNull { byId[it] }
            val remaining = allCats.filter { cat -> explicitOrder.none { it.id == cat.id } }
            val finalOrder = explicitOrder + remaining
            finalOrder.forEachIndexed { idx, cat ->
                val newSortOrder = (idx + 1) * 10
                if (cat.sortOrder != newSortOrder) {
                    val updated = cat.copy(
                        sortOrder = newSortOrder,
                        updatedAtMs = nowMs
                    )
                    productCategoryDao.update(updated)
                    updatedCategories.add(updated)
                }
            }
            OperationResult.Success("Category folder order saved and synced across all screens.")
        }
        if (result is OperationResult.Success && updatedCategories.isNotEmpty()) {
            updatedCategories.forEach { cat ->
                cloudSync?.pushProductCategory(cat)
            }
            val orderSummary = updatedCategories.sortedBy { it.sortOrder }.joinToString(" → ") { it.name }
            logSecurityEvent(
                eventType = "CATEGORY_REORDERED",
                role = actor.role.name,
                actorName = actor.displayName,
                details = "Updated category folder order: $orderSummary",
                isAlert = false
            )
        }
        return result
    }

    suspend fun addProduct(
        category: ProductCategory,
        name: String,
        cylinderSizeKg: String = "",
        categoryId: Long = category.defaultCategoryId,
        unit: String = "Cylinder",
        description: String = "",
        iconKey: String = "CYLINDER",
        isActive: Boolean = true
    ): OperationResult {
        val cleanName = GasNamingSanitizer.sanitizeCompanyOrProductName(name).take(100)
        val cleanSize = cylinderSizeKg.trim().take(40)
        val cleanUnit = unit.trim().take(30).ifBlank { "Cylinder" }
        val cleanDesc = description.trim().take(240)
        val cleanIcon = iconKey.trim().uppercase(Locale.US).take(40).ifBlank { "CYLINDER" }
        if (cleanName.isEmpty()) {
            return OperationResult.ValidationError("Product name cannot be empty.")
        }
        // Strictly block Arab Gas / Emirates Gas from being added as separate LPG stock products
        val lowerName = cleanName.lowercase(Locale.US)
        if ((category == ProductCategory.COOKING_GAS || categoryId == 3L) &&
            (lowerName.contains("arab gas") || lowerName.contains("emirates gas"))
        ) {
            return OperationResult.ValidationError(
                "Arab Gas and Emirates Gas are empty-return company identifiers in LPG Exchange, not separate LPG stock products."
            )
        }
        val catEntity = productCategoryDao.getById(categoryId)
        val resolvedEnum = catEntity?.legacyEnum ?: category
        val resolvedCatId = catEntity?.id ?: categoryId
        val catLabel = catEntity?.name ?: resolvedEnum.displayName

        val existing = productDao.getAllProductsOnce()
        if (existing.any { it.categoryId == resolvedCatId && it.name.equals(cleanName, ignoreCase = true) }) {
            return OperationResult.ValidationError("$catLabel - $cleanName already exists.")
        }
        val nextSort = (existing.filter { it.categoryId == resolvedCatId }.maxOfOrNull { it.sortOrder }
            ?: ((catEntity?.sortOrder ?: resolvedEnum.sortOrder) * 100)) + 1
        val newProduct = ProductEntity(
            category = resolvedEnum,
            categoryId = resolvedCatId,
            name = cleanName,
            sortOrder = nextSort,
            isActive = isActive,
            cylinderSizeKg = cleanSize,
            unit = cleanUnit,
            description = cleanDesc,
            iconKey = cleanIcon
        )
        val newId = productDao.insert(newProduct)
        cloudSync?.pushProduct(newProduct.copy(id = newId))
        return OperationResult.Success("Product '$cleanName' added to $catLabel.")
    }

    suspend fun updateProduct(
        productId: Long,
        category: ProductCategory,
        name: String,
        cylinderSizeKg: String,
        isActive: Boolean,
        actor: UserAccountEntity,
        categoryId: Long? = null,
        unit: String? = null,
        description: String? = null,
        iconKey: String? = null
    ): OperationResult {
        if (actor.role != UserRole.ADMIN) {
            return OperationResult.ValidationError("Only Admin can edit gas products.")
        }
        val cleanName = GasNamingSanitizer.sanitizeCompanyOrProductName(name).take(100)
        val cleanSize = cylinderSizeKg.trim().take(40)
        if (cleanName.isEmpty()) {
            return OperationResult.ValidationError("Product name cannot be empty.")
        }
        var updatedProduct: ProductEntity? = null
        val result = db.withTransaction {
            val allProducts = productDao.getAllProductsOnce()
            val target = allProducts.firstOrNull { it.id == productId }
                ?: return@withTransaction OperationResult.ValidationError("Product not found.")
            val resolvedCatId = categoryId ?: if (category != target.category) category.defaultCategoryId else target.categoryId
            val catEntity = productCategoryDao.getById(resolvedCatId)
            val resolvedEnum = catEntity?.legacyEnum ?: category
            val catLabel = catEntity?.name ?: resolvedEnum.displayName
            val duplicate = allProducts.firstOrNull {
                it.id != productId && it.categoryId == resolvedCatId && it.name.equals(cleanName, ignoreCase = true)
            }
            if (duplicate != null) {
                return@withTransaction OperationResult.ValidationError("$catLabel - $cleanName already exists.")
            }
            val updated = target.copy(
                category = resolvedEnum,
                categoryId = resolvedCatId,
                name = cleanName,
                cylinderSizeKg = cleanSize,
                unit = unit?.trim()?.take(30)?.ifBlank { target.unit } ?: target.unit,
                description = description?.trim()?.take(240) ?: target.description,
                iconKey = iconKey?.trim()?.uppercase(Locale.US)?.take(40)?.ifBlank { target.iconKey } ?: target.iconKey,
                isActive = isActive
            )
            productDao.update(updated)
            updatedProduct = updated
            OperationResult.Success("Updated '$cleanName' ($catLabel).")
        }
        if (result is OperationResult.Success) {
            updatedProduct?.let { cloudSync?.pushProduct(it) }
        }
        return result
    }

    /**
     * Product deletion safety rule:
     * - Permanently delete ONLY products with zero stock (opening & current across store and vehicles)
     *   AND zero transaction history.
     * - Products with existing stock or transaction history MUST be marked Inactive instead,
     *   preserving all historical stock and transaction records.
     */
    suspend fun deleteOrDeactivateProduct(
        productId: Long,
        actor: UserAccountEntity
    ): OperationResult {
        if (actor.role != UserRole.ADMIN) {
            return OperationResult.ValidationError("Only Admin can delete or deactivate products.")
        }
        var permanentlyDeleted = false
        var deactivatedEntity: ProductEntity? = null

        val result = db.withTransaction {
            val product = productDao.getProductById(productId)
                ?: return@withTransaction OperationResult.ValidationError("Product not found.")

            val txCount = txDao.countTransactionsForProduct(productId)
            val productOpenings = openingDao.getOpeningsForProduct(productId)
            val hasOpeningStock = productOpenings.any { it.openingFull != 0 || it.openingEmpty != 0 }

            val allOpenings = openingDao.getAllOpeningsOnce()
            val allTxs = txDao.getAllTransactionsOnce()
            val (storeFull, storeEmpty) = StockCalculator.currentStoreBalance(productId, allOpenings, allTxs)
            val vehicles = vehicleDao.getAllVehiclesOnce()
            val hasVehicleStock = vehicles.any { veh ->
                val (vFull, vEmpty) = StockCalculator.currentVehicleBalance(veh.id, productId, allOpenings, allTxs)
                vFull != 0 || vEmpty != 0
            }
            val hasAnyStockOrHistory = txCount > 0 || hasOpeningStock || storeFull != 0 || storeEmpty != 0 || hasVehicleStock

            if (!hasAnyStockOrHistory) {
                openingDao.deleteByProductId(productId)
                productDao.deleteById(productId)
                permanentlyDeleted = true
                OperationResult.Success("Permanently deleted '${product.name}' (no stock or transaction history).")
            } else {
                val inactive = product.copy(isActive = false)
                productDao.update(inactive)
                deactivatedEntity = inactive
                OperationResult.Success(
                    "'${product.name}' has existing stock or transaction history — marked Inactive to preserve all records."
                )
            }
        }

        if (result is OperationResult.Success && cloudSync != null) {
            if (permanentlyDeleted) {
                cloudSync.deleteProduct(productId)
            } else {
                deactivatedEntity?.let { cloudSync.pushProduct(it) }
            }
        }
        return result
    }

    /**
     * Admin-only Product Drag & Drop Ordering.
     * Persists updated sortOrder atomically in Room and syncs to cloud so Admin, Owner, and Staff
     * screens immediately display and retain the exact custom order across app restarts.
     */
    suspend fun reorderProducts(
        orderedProductIds: List<Long>,
        actor: UserAccountEntity
    ): OperationResult {
        if (actor.role != UserRole.ADMIN) {
            return OperationResult.ValidationError("Only Admin can rearrange product display order.")
        }
        val cleanIds = orderedProductIds.distinct()
        if (cleanIds.isEmpty()) {
            return OperationResult.ValidationError("No products supplied for ordering.")
        }

        val updatedProducts = mutableListOf<ProductEntity>()
        val result = db.withTransaction {
            val allProducts = productDao.getAllProductsOnce()
            val byId = allProducts.associateBy { it.id }

            // Preserve ordering per category base if subset supplied, or assign contiguous order
            val orderedExisting = cleanIds.mapNotNull { byId[it] }
            if (orderedExisting.isEmpty()) {
                return@withTransaction OperationResult.ValidationError("Selected products not found.")
            }

            val categoriesPresent = orderedExisting.map { it.category }.distinct()
            if (categoriesPresent.size == 1) {
                val cat = categoriesPresent.first()
                val base = cat.sortOrder * 100
                orderedExisting.forEachIndexed { idx, prod ->
                    val newOrder = base + idx + 1
                    if (prod.sortOrder != newOrder) {
                        val updated = prod.copy(sortOrder = newOrder)
                        productDao.updateSortOrder(prod.id, newOrder)
                        updatedProducts.add(updated)
                    }
                }
            } else {
                // Global reorder across categories: assign sequential sortOrder within each category
                val perCategoryCounter = mutableMapOf<ProductCategory, Int>()
                orderedExisting.forEachIndexed { idx, prod ->
                    val count = (perCategoryCounter[prod.category] ?: 0) + 1
                    perCategoryCounter[prod.category] = count
                    val newOrder = idx + 1
                    if (prod.sortOrder != newOrder) {
                        val updated = prod.copy(sortOrder = newOrder)
                        productDao.updateSortOrder(prod.id, newOrder)
                        updatedProducts.add(updated)
                    }
                }
            }
            OperationResult.Success("Custom product order saved (${orderedExisting.size} products).")
        }

        if (result is OperationResult.Success && cloudSync != null) {
            updatedProducts.forEach { cloudSync.pushProduct(it) }
        }
        return result
    }

    suspend fun toggleProductActive(product: ProductEntity): OperationResult {
        val updated = product.copy(isActive = !product.isActive)
        productDao.update(updated)
        cloudSync?.pushProduct(updated)
        val status = if (updated.isActive) "activated" else "marked Inactive"
        return OperationResult.Success("${product.name} $status.")
    }

    /**
     * Different Gas Cylinder Return Management (Admin & Staff, NO Admin approval required).
     * Example: Deliver Oxygen Full (1) + Receive Argon Empty (1), or multiple gas types/quantities
     * in a single atomic transaction.
     *
     * Rules enforced:
     * - Deducts each delivered gas Full quantity strictly from its exact gas Full Stock.
     * - Adds each returned gas Empty quantity strictly to its exact returned gas Empty Stock.
     * - Never mixes gas categories.
     * - Updates stock immediately after save without requiring Admin approval.
     * - Prevents duplicate transactions and duplicate stock updates via unique [submissionKey].
     */
    suspend fun recordDifferentGasReturn(
        businessDate: String,
        deliveredItems: List<CustomerExchangeItem>,
        returnedItems: List<CustomerExchangeItem>,
        vehicleId: Long?,
        actor: UserAccountEntity,
        customerName: String,
        customerPhone: String,
        notes: String = "",
        submissionKey: String = java.util.UUID.randomUUID().toString(),
        returnedEmptyCompany: String = ""
    ): OperationResult {
        if (actor.role == UserRole.OWNER_VIEW) {
            return OperationResult.ValidationError("Owner View is read-only and cannot record stock transactions.")
        }
        val cleanKey = submissionKey.trim().take(120)
        if (cleanKey.isEmpty()) {
            return OperationResult.ValidationError("Missing transaction key.")
        }
        val cleanCustomer = customerName.trim().take(120)
        if (cleanCustomer.isEmpty()) {
            return OperationResult.ValidationError("Please enter customer name.")
        }
        val cleanPhone = customerPhone.trim().take(40)
        val cleanNotes = notes.trim().take(240)
        val cleanReturnCompany = GasNamingSanitizer.sanitizeCompanyOrProductName(returnedEmptyCompany).take(60)

        val mergedDelivered = deliveredItems
            .filter { it.quantity > 0 }
            .groupBy { it.productId }
            .map { (pid, list) -> CustomerExchangeItem(pid, list.sumOf { it.quantity }) }

        val mergedReturned = returnedItems
            .filter { it.quantity > 0 }
            .groupBy { it.productId }
            .map { (pid, list) -> CustomerExchangeItem(pid, list.sumOf { it.quantity }) }

        if (mergedDelivered.isEmpty() && mergedReturned.isEmpty()) {
            return OperationResult.ValidationError("Enter at least one Full Cylinder OUT or Empty Cylinder IN quantity.")
        }
        for (item in mergedDelivered + mergedReturned) {
            if (item.quantity <= 0 || item.quantity > 100000) {
                return OperationResult.ValidationError("Cylinder quantity must be between 1 and 100,000.")
            }
        }

        val insertedTransactions = mutableListOf<StockTransactionEntity>()
        var successMessage = ""

        val localResult = db.withTransaction {
            // Strict idempotency check prevents duplicate saves & duplicate stock updates
            if (differentGasReturnDao.getBySubmissionKey(cleanKey) != null) {
                return@withTransaction OperationResult.ValidationError(
                    "Duplicate transaction prevented — this gas return was already saved."
                )
            }

            val vehicle = if (vehicleId != null) {
                vehicleDao.getVehicleById(vehicleId)
                    ?: return@withTransaction OperationResult.ValidationError("Selected vehicle not found.")
            } else {
                null
            }
            if (actor.role == UserRole.STAFF && vehicle == null) {
                return@withTransaction OperationResult.ValidationError("Staff must have an assigned vehicle for cylinder return.")
            }

            val productMap = mutableMapOf<Long, ProductEntity>()
            for (item in mergedDelivered + mergedReturned) {
                if (!productMap.containsKey(item.productId)) {
                    val prod = productDao.getProductById(item.productId)
                        ?: return@withTransaction OperationResult.ValidationError("Selected gas product not found.")
                    productMap[item.productId] = prod
                }
            }

            val openings = openingDao.getAllOpeningsOnce()
            val allTxs = txDao.getAllTransactionsOnce()
            val scopeLabel = vehicle?.name ?: "Main Store"

            // Strictly validate Full stock on every delivered gas product without mixing categories
            for (deliv in mergedDelivered) {
                val prod = productMap[deliv.productId]!!
                val (availFull, _) = if (vehicle != null) {
                    StockCalculator.currentVehicleBalance(vehicle.id, prod.id, openings, allTxs)
                } else {
                    StockCalculator.currentStoreBalance(prod.id, openings, allTxs)
                }
                if (availFull < deliv.quantity) {
                    return@withTransaction OperationResult.InsufficientStock(
                        title = "Insufficient ${if (vehicle != null) "Vehicle" else "Store"} Full Stock",
                        message = "$scopeLabel does not have enough Full ${prod.category.displayName} • ${prod.name}.\n\nAvailable Full: $availFull, Requested: ${deliv.quantity}"
                    )
                }
            }

            val nowMs = System.currentTimeMillis()
            var offsetMs = 0L

            val deliveredLines = mergedDelivered.map { item ->
                val p = productMap[item.productId]!!
                DifferentGasLineItem(
                    productId = p.id,
                    productName = p.name,
                    categoryName = p.category.displayName,
                    quantity = item.quantity
                )
            }
            val returnedLines = mergedReturned.map { item ->
                val p = productMap[item.productId]!!
                DifferentGasLineItem(
                    productId = p.id,
                    productName = p.name,
                    categoryName = p.category.displayName,
                    quantity = item.quantity
                )
            }

            val deliveredSummary = if (deliveredLines.isEmpty()) {
                "None"
            } else {
                deliveredLines.joinToString(", ") { "${it.productName} Full ×${it.quantity}" }
            }
            val returnedSummary = if (returnedLines.isEmpty()) {
                "None"
            } else {
                returnedLines.joinToString(", ") {
                    val coPrefix = if (cleanReturnCompany.isNotBlank()) "$cleanReturnCompany " else ""
                    "$coPrefix${it.productName} Empty ×${it.quantity}"
                }
            }
            val phoneTag = if (cleanPhone.isNotBlank()) " • Tel: $cleanPhone" else ""
            val coTag = if (cleanReturnCompany.isNotBlank()) " • Empty Co: $cleanReturnCompany" else ""
            val noteTag = if (cleanNotes.isNotBlank()) " • $cleanNotes" else ""

            val fullOutType = if (vehicle != null) {
                TransactionType.VEHICLE_CUSTOMER_CHANGE_FULL_OUT
            } else {
                TransactionType.STORE_CUSTOMER_CHANGE_FULL_OUT
            }
            val emptyInType = if (vehicle != null) {
                TransactionType.VEHICLE_CUSTOMER_CHANGE_EMPTY_IN
            } else {
                TransactionType.STORE_CUSTOMER_CHANGE_EMPTY_IN
            }

            // 1. Deduct delivered Full quantity strictly from each delivered gas Full stock
            for (item in mergedDelivered) {
                val tx = StockTransactionEntity(
                    businessDate = businessDate,
                    timestamp = nowMs + offsetMs++,
                    type = fullOutType,
                    productId = item.productId,
                    vehicleId = vehicle?.id,
                    vehicleName = vehicle?.name,
                    staffId = actor.id,
                    staffName = actor.displayName,
                    quantity = item.quantity,
                    customerName = cleanCustomer,
                    notes = "Diff Gas Return • Delivered vs ($returnedSummary)$phoneTag$noteTag".take(300),
                    remoteId = CloudSyncRepository.generateRemoteTxId()
                )
                val rowId = txDao.insert(tx)
                insertedTransactions.add(tx.copy(id = rowId))
            }

            // 2. Add returned Empty quantity strictly to each returned gas Empty stock
            for (item in mergedReturned) {
                val tx = StockTransactionEntity(
                    businessDate = businessDate,
                    timestamp = nowMs + offsetMs++,
                    type = emptyInType,
                    productId = item.productId,
                    vehicleId = vehicle?.id,
                    vehicleName = vehicle?.name,
                    staffId = actor.id,
                    staffName = actor.displayName,
                    quantity = item.quantity,
                    customerName = cleanCustomer,
                    notes = "Diff Gas Return • Returned vs ($deliveredSummary)$coTag$phoneTag$noteTag".take(300),
                    remoteId = CloudSyncRepository.generateRemoteTxId()
                )
                val rowId = txDao.insert(tx)
                insertedTransactions.add(tx.copy(id = rowId))
            }

            // 3. Persist audit & history record (unique submissionKey guarantees no duplicates)
            val linkedRemoteIdsCsv = insertedTransactions.mapNotNull { it.remoteId }.joinToString(",")
            val returnRecord = DifferentGasReturnEntity(
                submissionKey = cleanKey,
                businessDate = businessDate,
                timestamp = nowMs,
                customerName = cleanCustomer,
                customerPhone = cleanPhone,
                staffId = actor.id,
                staffName = actor.displayName,
                vehicleId = vehicle?.id,
                vehicleName = vehicle?.name,
                deliveredSummary = deliveredSummary,
                returnedSummary = returnedSummary,
                deliveredItemsJson = serializeDifferentGasLineItems(deliveredLines),
                returnedItemsJson = serializeDifferentGasLineItems(returnedLines),
                totalDeliveredQty = deliveredLines.sumOf { it.quantity },
                totalReturnedQty = returnedLines.sumOf { it.quantity },
                notes = cleanNotes,
                linkedTxRemoteIds = linkedRemoteIdsCsv,
                updatedAtMs = nowMs,
                returnedEmptyCompany = cleanReturnCompany
            )
            differentGasReturnDao.insert(returnRecord)

            successMessage = "Saved Gas Exchange ($scopeLabel): Delivered $deliveredSummary | Returned $returnedSummary."
            OperationResult.Success(successMessage)
        }

        if (localResult is OperationResult.Success) {
            logSecurityEvent(
                eventType = "EXCHANGE_CREATED",
                role = actor.role.name,
                actorName = actor.displayName,
                details = successMessage.take(300),
                isAlert = false
            )
            if (cloudSync != null && insertedTransactions.isNotEmpty()) {
                var anyFailed = false
                for (tx in insertedTransactions) {
                    if (cloudSync.pushTransaction(tx).isFailure) {
                        anyFailed = true
                    }
                }
                if (anyFailed) {
                    return OperationResult.Success("$successMessage Saved locally (will sync when online).")
                }
            }
        }
        return localResult
    }

    /**
     * Atomically edits an existing LPG / Different Gas Exchange record:
     * 1. Locates and reverses the previous linked stock transactions.
     * 2. Validates the corrected quantities against post-reversal stock (strictly preventing negative balances).
     * 3. Applies the corrected Full OUT and Empty IN stock movements exactly once.
     * 4. Updates the exchange history record and syncs deletions & new entries to Firestore.
     */
    suspend fun updateDifferentGasReturn(
        recordId: Long,
        deliveredItems: List<CustomerExchangeItem>,
        returnedItems: List<CustomerExchangeItem>,
        vehicleId: Long?,
        actor: UserAccountEntity,
        customerName: String,
        customerPhone: String,
        notes: String = "",
        returnedEmptyCompany: String = ""
    ): OperationResult {
        if (actor.role == UserRole.OWNER_VIEW) {
            return OperationResult.ValidationError("Owner View is strictly read-only and cannot edit gas exchanges.")
        }
        val cleanCustomer = customerName.trim().take(120)
        if (cleanCustomer.isEmpty()) {
            return OperationResult.ValidationError("Please enter customer name.")
        }
        val cleanPhone = customerPhone.trim().take(40)
        val cleanNotes = notes.trim().take(240)
        val cleanReturnCompany = GasNamingSanitizer.sanitizeCompanyOrProductName(returnedEmptyCompany).take(60)

        val mergedDelivered = deliveredItems
            .filter { it.quantity > 0 }
            .groupBy { it.productId }
            .map { (pid, list) -> CustomerExchangeItem(pid, list.sumOf { it.quantity }) }
        val mergedReturned = returnedItems
            .filter { it.quantity > 0 }
            .groupBy { it.productId }
            .map { (pid, list) -> CustomerExchangeItem(pid, list.sumOf { it.quantity }) }

        if (mergedDelivered.isEmpty() && mergedReturned.isEmpty()) {
            return OperationResult.ValidationError("Enter at least one Full Cylinder OUT or Empty Cylinder IN quantity.")
        }
        for (item in mergedDelivered + mergedReturned) {
            if (item.quantity <= 0 || item.quantity > 100000) {
                return OperationResult.ValidationError("Cylinder quantity must be between 1 and 100,000.")
            }
        }

        val deletedRemoteIds = mutableListOf<String>()
        val insertedTransactions = mutableListOf<StockTransactionEntity>()
        var summaryMsg = ""

        val localResult = db.withTransaction {
            val existing = differentGasReturnDao.getById(recordId)
                ?: return@withTransaction OperationResult.ValidationError("Exchange record not found.")
            if (actor.role == UserRole.STAFF && existing.staffId != actor.id) {
                return@withTransaction OperationResult.ValidationError("Staff can only edit their own exchange entries.")
            }

            val vehicle = if (vehicleId != null) {
                vehicleDao.getVehicleById(vehicleId)
                    ?: return@withTransaction OperationResult.ValidationError("Selected vehicle not found.")
            } else {
                null
            }
            if (actor.role == UserRole.STAFF && vehicle == null) {
                return@withTransaction OperationResult.ValidationError("Staff must have an assigned vehicle for cylinder exchange.")
            }

            val productMap = mutableMapOf<Long, ProductEntity>()
            for (item in mergedDelivered + mergedReturned) {
                if (!productMap.containsKey(item.productId)) {
                    val prod = productDao.getProductById(item.productId)
                        ?: return@withTransaction OperationResult.ValidationError("Selected gas product not found.")
                    productMap[item.productId] = prod
                }
            }

            val allTxs = txDao.getAllTransactionsOnce()
            val oldLinkedTxs = findLinkedTransactionsForReturn(existing, allTxs)
            val oldLinkedIds = oldLinkedTxs.map { it.id }.toSet()
            val baseTxsWithoutOld = allTxs.filter { it.id !in oldLinkedIds }
            val openings = openingDao.getAllOpeningsOnce()
            val scopeLabel = vehicle?.name ?: "Main Store"

            // Build candidate new transactions
            val nowMs = System.currentTimeMillis()
            var offsetMs = 0L
            val deliveredLines = mergedDelivered.map { item ->
                val p = productMap[item.productId]!!
                DifferentGasLineItem(p.id, p.name, p.category.displayName, item.quantity)
            }
            val returnedLines = mergedReturned.map { item ->
                val p = productMap[item.productId]!!
                DifferentGasLineItem(p.id, p.name, p.category.displayName, item.quantity)
            }
            val deliveredSummary = if (deliveredLines.isEmpty()) {
                "None"
            } else {
                deliveredLines.joinToString(", ") { "${it.productName} Full ×${it.quantity}" }
            }
            val returnedSummary = if (returnedLines.isEmpty()) {
                "None"
            } else {
                returnedLines.joinToString(", ") {
                    val coPrefix = if (cleanReturnCompany.isNotBlank()) "$cleanReturnCompany " else ""
                    "$coPrefix${it.productName} Empty ×${it.quantity}"
                }
            }
            val phoneTag = if (cleanPhone.isNotBlank()) " • Tel: $cleanPhone" else ""
            val coTag = if (cleanReturnCompany.isNotBlank()) " • Empty Co: $cleanReturnCompany" else ""
            val noteTag = if (cleanNotes.isNotBlank()) " • $cleanNotes" else ""

            val fullOutType = if (vehicle != null) TransactionType.VEHICLE_CUSTOMER_CHANGE_FULL_OUT else TransactionType.STORE_CUSTOMER_CHANGE_FULL_OUT
            val emptyInType = if (vehicle != null) TransactionType.VEHICLE_CUSTOMER_CHANGE_EMPTY_IN else TransactionType.STORE_CUSTOMER_CHANGE_EMPTY_IN

            val candidateNewTxs = mutableListOf<StockTransactionEntity>()
            for (item in mergedDelivered) {
                candidateNewTxs.add(
                    StockTransactionEntity(
                        businessDate = existing.businessDate,
                        timestamp = existing.timestamp + offsetMs++,
                        type = fullOutType,
                        productId = item.productId,
                        vehicleId = vehicle?.id,
                        vehicleName = vehicle?.name,
                        staffId = existing.staffId,
                        staffName = existing.staffName,
                        quantity = item.quantity,
                        customerName = cleanCustomer,
                        notes = "Diff Gas Return (Edited) • Delivered vs ($returnedSummary)$phoneTag$noteTag".take(300),
                        remoteId = CloudSyncRepository.generateRemoteTxId()
                    )
                )
            }
            for (item in mergedReturned) {
                candidateNewTxs.add(
                    StockTransactionEntity(
                        businessDate = existing.businessDate,
                        timestamp = existing.timestamp + offsetMs++,
                        type = emptyInType,
                        productId = item.productId,
                        vehicleId = vehicle?.id,
                        vehicleName = vehicle?.name,
                        staffId = existing.staffId,
                        staffName = existing.staffName,
                        quantity = item.quantity,
                        customerName = cleanCustomer,
                        notes = "Diff Gas Return (Edited) • Returned vs ($deliveredSummary)$coTag$phoneTag$noteTag".take(300),
                        remoteId = CloudSyncRepository.generateRemoteTxId()
                    )
                )
            }

            // Simulate post-edit balances across ALL affected products and scopes (old + new)
            val simulatedTxs = baseTxsWithoutOld + candidateNewTxs
            val affectedProducts = (oldLinkedTxs.map { it.productId } + candidateNewTxs.map { it.productId }).toSet()
            val affectedVehicleIds = (oldLinkedTxs.mapNotNull { it.vehicleId } + listOfNotNull(vehicle?.id)).toSet()

            for (pid in affectedProducts) {
                val (sFull, sEmpty) = StockCalculator.currentStoreBalance(pid, openings, simulatedTxs)
                if (sFull < 0 || sEmpty < 0) {
                    val pName = productMap[pid]?.name ?: productDao.getProductById(pid)?.name ?: "Product"
                    return@withTransaction OperationResult.InsufficientStock(
                        title = "Negative Store Balance Prevented",
                        message = "Editing this exchange would make Main Store $pName negative (Full=$sFull, Empty=$sEmpty)."
                    )
                }
                for (vid in affectedVehicleIds) {
                    val (vFull, vEmpty) = StockCalculator.currentVehicleBalance(vid, pid, openings, simulatedTxs)
                    if (vFull < 0 || vEmpty < 0) {
                        val pName = productMap[pid]?.name ?: productDao.getProductById(pid)?.name ?: "Product"
                        val vName = vehicleDao.getVehicleById(vid)?.name ?: "Vehicle"
                        return@withTransaction OperationResult.InsufficientStock(
                            title = "Negative Vehicle Balance Prevented",
                            message = "Editing this exchange would make $vName $pName negative (Full=$vFull, Empty=$vEmpty)."
                        )
                    }
                }
            }

            // Atomically reverse old transactions & insert corrected transactions once
            for (oldTx in oldLinkedTxs) {
                oldTx.remoteId?.let { deletedRemoteIds.add(it) }
                txDao.deleteById(oldTx.id)
            }
            for (newTx in candidateNewTxs) {
                val rowId = txDao.insert(newTx)
                insertedTransactions.add(newTx.copy(id = rowId))
            }

            val updatedRecord = existing.copy(
                customerName = cleanCustomer,
                customerPhone = cleanPhone,
                vehicleId = vehicle?.id,
                vehicleName = vehicle?.name,
                deliveredSummary = deliveredSummary,
                returnedSummary = returnedSummary,
                deliveredItemsJson = serializeDifferentGasLineItems(deliveredLines),
                returnedItemsJson = serializeDifferentGasLineItems(returnedLines),
                totalDeliveredQty = deliveredLines.sumOf { it.quantity },
                totalReturnedQty = returnedLines.sumOf { it.quantity },
                notes = cleanNotes,
                linkedTxRemoteIds = insertedTransactions.mapNotNull { it.remoteId }.joinToString(","),
                updatedAtMs = nowMs,
                returnedEmptyCompany = cleanReturnCompany
            )
            differentGasReturnDao.update(updatedRecord)
            summaryMsg = "Updated Gas Exchange ($scopeLabel): Delivered $deliveredSummary | Returned $returnedSummary."
            OperationResult.Success(summaryMsg)
        }

        if (localResult is OperationResult.Success) {
            logSecurityEvent(
                eventType = "EXCHANGE_EDITED",
                role = actor.role.name,
                actorName = actor.displayName,
                details = "Record #$recordId: ${summaryMsg.take(260)}",
                isAlert = false
            )
            if (cloudSync != null) {
                deletedRemoteIds.forEach { cloudSync.deleteCloudTransaction(it) }
                insertedTransactions.forEach { cloudSync.pushTransaction(it) }
            }
        }
        return localResult
    }

    /**
     * Atomically deletes an LPG / Different Gas Exchange record and safely reverses its linked
     * stock movements while verifying no subsequent transaction becomes negative.
     */
    suspend fun deleteDifferentGasReturn(
        recordId: Long,
        actor: UserAccountEntity
    ): OperationResult {
        if (actor.role == UserRole.OWNER_VIEW) {
            return OperationResult.ValidationError("Owner View is strictly read-only and cannot delete gas exchanges.")
        }
        val deletedRemoteIds = mutableListOf<String>()

        val localResult = db.withTransaction {
            val existing = differentGasReturnDao.getById(recordId)
                ?: return@withTransaction OperationResult.ValidationError("Exchange record not found.")
            if (actor.role == UserRole.STAFF && existing.staffId != actor.id) {
                return@withTransaction OperationResult.ValidationError("Staff can only delete their own exchange entries.")
            }

            val allTxs = txDao.getAllTransactionsOnce()
            val oldLinkedTxs = findLinkedTransactionsForReturn(existing, allTxs)
            val oldLinkedIds = oldLinkedTxs.map { it.id }.toSet()
            val remainingTxs = allTxs.filter { it.id !in oldLinkedIds }
            val openings = openingDao.getAllOpeningsOnce()

            val affectedProducts = oldLinkedTxs.map { it.productId }.toSet()
            val affectedVehicleIds = oldLinkedTxs.mapNotNull { it.vehicleId }.toSet()
            for (pid in affectedProducts) {
                val (sFull, sEmpty) = StockCalculator.currentStoreBalance(pid, openings, remainingTxs)
                if (sFull < 0 || sEmpty < 0) {
                    return@withTransaction OperationResult.InsufficientStock(
                        title = "Cannot Reverse Exchange",
                        message = "Deleting this exchange would cause negative Store stock (Full=$sFull, Empty=$sEmpty) due to subsequent transactions."
                    )
                }
                for (vid in affectedVehicleIds) {
                    val (vFull, vEmpty) = StockCalculator.currentVehicleBalance(vid, pid, openings, remainingTxs)
                    if (vFull < 0 || vEmpty < 0) {
                        return@withTransaction OperationResult.InsufficientStock(
                            title = "Cannot Reverse Exchange",
                            message = "Deleting this exchange would cause negative Vehicle stock (Full=$vFull, Empty=$vEmpty) due to subsequent transactions."
                        )
                    }
                }
            }

            for (oldTx in oldLinkedTxs) {
                oldTx.remoteId?.let { deletedRemoteIds.add(it) }
                txDao.deleteById(oldTx.id)
            }
            differentGasReturnDao.deleteById(existing.id)
            OperationResult.Success("Deleted Gas Exchange and reversed ${oldLinkedTxs.size} stock movement(s).")
        }

        if (localResult is OperationResult.Success) {
            logSecurityEvent(
                eventType = "EXCHANGE_DELETED",
                role = actor.role.name,
                actorName = actor.displayName,
                details = "Deleted exchange record #$recordId and reversed ${deletedRemoteIds.size} movement(s).",
                isAlert = true
            )
            if (cloudSync != null) {
                deletedRemoteIds.forEach { cloudSync.deleteCloudTransaction(it) }
            }
        }
        return localResult
    }

    private fun findLinkedTransactionsForReturn(
        record: DifferentGasReturnEntity,
        allTxs: List<StockTransactionEntity>
    ): List<StockTransactionEntity> {
        val remoteIds = record.linkedTxRemoteIds
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
        if (remoteIds.isNotEmpty()) {
            val matched = allTxs.filter { it.remoteId != null && it.remoteId in remoteIds }
            if (matched.isNotEmpty()) return matched
        }
        // Fallback for legacy V2.3 records created before linkedTxRemoteIds existed
        val allowedTypes = setOf(
            TransactionType.VEHICLE_CUSTOMER_CHANGE_FULL_OUT,
            TransactionType.VEHICLE_CUSTOMER_CHANGE_EMPTY_IN,
            TransactionType.STORE_CUSTOMER_CHANGE_FULL_OUT,
            TransactionType.STORE_CUSTOMER_CHANGE_EMPTY_IN
        )
        return allTxs.filter { tx ->
            tx.type in allowedTypes &&
                tx.businessDate == record.businessDate &&
                tx.staffId == record.staffId &&
                tx.vehicleId == record.vehicleId &&
                tx.customerName.equals(record.customerName, ignoreCase = true) &&
                tx.notes.startsWith("Diff Gas Return") &&
                kotlin.math.abs(tx.timestamp - record.timestamp) <= 10_000L
        }
    }

    private fun serializeDifferentGasLineItems(items: List<DifferentGasLineItem>): String {
        val arr = org.json.JSONArray()
        items.forEach { line ->
            arr.put(org.json.JSONObject().apply {
                put("productId", line.productId)
                put("productName", line.productName)
                put("categoryName", line.categoryName)
                put("quantity", line.quantity)
            })
        }
        return arr.toString()
    }

    /**
     * Reactive Store Stock rows for a given date.
     */
    fun observeStoreStock(date: String): Flow<List<StoreStockRow>> =
        combine(activeProductsFlow, openingsFlow, transactionsFlow) { products, openings, txs ->
            StockCalculator.calculateStoreRowsForDate(date, products, openings, txs)
        }.flowOn(Dispatchers.Default)

    /**
     * Reactive Vehicle Stock rows for a specific vehicle and date.
     */
    fun observeVehicleStock(vehicleId: Long, date: String): Flow<List<VehicleStockRow>> =
        combine(vehiclesFlow, activeProductsFlow, openingsFlow, transactionsFlow) { vehicles, products, openings, txs ->
            val vehicle = vehicles.firstOrNull { it.id == vehicleId } ?: return@combine emptyList()
            StockCalculator.calculateVehicleRowsForDate(date, vehicle, products, openings, txs)
        }.flowOn(Dispatchers.Default)

    /**
     * Reactive Admin Dashboard Summary for a selected date.
     */
    fun observeDashboardSummary(date: String): Flow<DashboardSummary> =
        combine(
            vehiclesFlow,
            activeProductsFlow,
            openingsFlow,
            transactionsFlow
        ) { vehicles, products, openings, txs ->
            val storeRows = StockCalculator.calculateStoreRowsForDate(date, products, openings, txs)
            val productMap = products.associateBy { it.id }

            val storeFull = storeRows.sumOf { it.storeFullBalance }
            val storeEmpty = storeRows.sumOf { it.storeEmptyBalance }
            val storeDamaged = storeRows.sumOf { it.damagedBalance }
            val todayFullPurchases = storeRows.sumOf { it.fullPurchases }
            val todayEmptyPurchases = storeRows.sumOf { it.emptyPurchases }
            val todayFullDispatch = storeRows.sumOf { it.fullDispatch }
            val todayEmptyDispatch = storeRows.sumOf { it.emptyDispatch }

            val vehicleOverviews = vehicles.map { vehicle ->
                val vRows = StockCalculator.calculateVehicleRowsForDate(
                    date = date,
                    vehicle = vehicle,
                    products = products,
                    openings = openings,
                    transactions = txs
                )
                VehicleOverview(
                    vehicle = vehicle,
                    totalOldFull = vRows.sumOf { it.oldFull },
                    totalFullReceived = vRows.sumOf { it.fullReceived },
                    totalFull = vRows.sumOf { it.totalFull },
                    totalCustomerSales = vRows.sumOf { it.customerSales },
                    totalFullBalance = vRows.sumOf { it.fullBalance },
                    totalOldEmpty = vRows.sumOf { it.oldEmpty },
                    totalCustomerEmptyReceived = vRows.sumOf { it.customerEmptyReceived },
                    totalEmptyReturnedToStore = vRows.sumOf { it.emptyReturnedToStore },
                    totalEmptyBalance = vRows.sumOf { it.emptyBalance },
                    totalComplaintReplaced = vRows.sumOf { it.complaintReplaced },
                    totalFullTransferredOut = vRows.sumOf { it.fullTransferredOut },
                    totalEmptyTransferredOut = vRows.sumOf { it.emptyTransferredOut },
                    totalFullReturnedToStore = vRows.sumOf { it.fullReturnedToStore },
                    totalDamagedBalance = vRows.sumOf { it.damagedBalance }
                )
            }

            val allVehiclesFull = vehicleOverviews.sumOf { it.totalFullBalance }
            val allVehiclesEmpty = vehicleOverviews.sumOf { it.totalEmptyBalance }
            val allVehiclesDamaged = vehicleOverviews.sumOf { it.totalDamagedBalance }
            val todaySales = vehicleOverviews.sumOf { it.totalCustomerSales }

            val dayTxs = txs
                .filter { it.businessDate == date }
                .mapNotNull { tx ->
                    val product = productMap[tx.productId] ?: return@mapNotNull null
                    TransactionDisplayItem(
                        transaction = tx,
                        productName = product.name,
                        productCategory = product.category
                    )
                }
            val recentTxs = dayTxs.take(30)
            val storeActivityTxs = dayTxs.filter { it.transaction.type.isStoreLevel }

            DashboardSummary(
                selectedDate = date,
                totalFullCylinders = storeFull + allVehiclesFull,
                totalEmptyCylinders = storeEmpty + allVehiclesEmpty,
                storeFullBalance = storeFull,
                storeEmptyBalance = storeEmpty,
                allVehiclesFullBalance = allVehiclesFull,
                allVehiclesEmptyBalance = allVehiclesEmpty,
                todayFullPurchases = todayFullPurchases,
                todayEmptyPurchases = todayEmptyPurchases,
                todayFullDispatch = todayFullDispatch,
                todayEmptyDispatch = todayEmptyDispatch,
                todaySales = todaySales,
                vehicleOverviews = vehicleOverviews,
                recentTransactions = recentTxs,
                pendingDeliveryCylinders = allVehiclesFull,
                activeDeliveryVehiclesCount = vehicleOverviews.count { it.totalFullBalance > 0 },
                todayStoreActivityCount = storeActivityTxs.size,
                recentStoreActivity = storeActivityTxs.take(6),
                storeDamagedBalance = storeDamaged,
                allVehiclesDamagedBalance = allVehiclesDamaged
            )
        }.flowOn(Dispatchers.Default)

    /**
     * Records a stock transaction atomically while strictly preventing negative stock.
     */
    suspend fun recordTransaction(
        businessDate: String,
        type: TransactionType,
        productId: Long,
        vehicleId: Long?,
        actor: UserAccountEntity,
        quantity: Int,
        notes: String,
        purchaseCompanyName: String = "",
        customerName: String = ""
    ): OperationResult {
        if (actor.role == UserRole.OWNER_VIEW) {
            return OperationResult.ValidationError("Owner View is strictly read-only and cannot modify stock.")
        }
        if (quantity <= 0 || quantity > 100000) {
            return OperationResult.ValidationError("Quantity must be between 1 and 100,000.")
        }

        val remoteId = CloudSyncRepository.generateRemoteTxId()
        var insertedEntity: StockTransactionEntity? = null
        var productNameLabel = ""

        val localResult = db.withTransaction {
            val product = productDao.getProductById(productId)
                ?: return@withTransaction OperationResult.ValidationError("Selected product not found.")

            val vehicle = if (vehicleId != null) {
                vehicleDao.getVehicleById(vehicleId)
                    ?: return@withTransaction OperationResult.ValidationError("Selected vehicle not found.")
            } else {
                null
            }

            if (type.isVehicleLevel && vehicle == null) {
                return@withTransaction OperationResult.ValidationError("Please select a vehicle for this transaction.")
            }

            val openings = openingDao.getAllOpeningsOnce()
            val allTxs = txDao.getAllTransactionsOnce()

            val (liveStoreFull, liveStoreEmpty) = StockCalculator.currentStoreBalance(
                productId = productId,
                openings = openings,
                transactions = allTxs
            )
            val (liveVehicleFull, liveVehicleEmpty) = if (vehicle != null) {
                StockCalculator.currentVehicleBalance(
                    vehicleId = vehicle.id,
                    productId = productId,
                    openings = openings,
                    transactions = allTxs
                )
            } else {
                0 to 0
            }
            val liveStoreDamaged = StockCalculator.currentStoreDamagedBalance(productId, allTxs)
            val liveVehicleDamaged = if (vehicle != null) {
                StockCalculator.currentVehicleDamagedBalance(vehicle.id, productId, allTxs)
            } else {
                0
            }

            val productLabel = "${product.category.displayName} • ${product.name}"
            productNameLabel = product.name

            when (type) {
                TransactionType.VEHICLE_FULL_RECEIVED -> {
                    if (liveStoreFull < quantity) {
                        return@withTransaction OperationResult.InsufficientStock(
                            title = "Insufficient Store Full Stock",
                            message = "Cannot dispatch $quantity × $productLabel to ${vehicle!!.name}.\n\nAvailable Store Full Balance: $liveStoreFull"
                        )
                    }
                }
                TransactionType.STORE_FULL_DISPATCH_DIRECT,
                TransactionType.STORE_CUSTOMER_CHANGE_FULL_OUT,
                TransactionType.STORE_ADJUST_FULL_OUT -> {
                    if (liveStoreFull < quantity) {
                        return@withTransaction OperationResult.InsufficientStock(
                            title = "Insufficient Store Full Stock",
                            message = "Cannot dispatch/deduct $quantity × $productLabel.\n\nAvailable Store Full Balance: $liveStoreFull"
                        )
                    }
                }
                TransactionType.STORE_PURCHASE_COMPLAINT_REPLACE,
                TransactionType.STORE_EMPTY_DISPATCH,
                TransactionType.STORE_CUSTOMER_CHANGE_EMPTY_OUT,
                TransactionType.STORE_ADJUST_EMPTY_OUT -> {
                    if (liveStoreEmpty < quantity) {
                        return@withTransaction OperationResult.InsufficientStock(
                            title = "Insufficient Store Empty Stock",
                            message = "Cannot dispatch/replace $quantity × $productLabel empty cylinders.\n\nAvailable Store Empty Balance: $liveStoreEmpty"
                        )
                    }
                }
                TransactionType.STORE_DAMAGED_REMOVE -> {
                    if (liveStoreDamaged < quantity) {
                        return@withTransaction OperationResult.InsufficientStock(
                            title = "Insufficient Store Damaged Stock",
                            message = "Cannot resolve $quantity damaged $productLabel cylinders.\n\nTracked Store Damaged Balance: $liveStoreDamaged"
                        )
                    }
                }
                TransactionType.VEHICLE_FULL_RETURN_TO_STORE,
                TransactionType.VEHICLE_CUSTOMER_SALE,
                TransactionType.VEHICLE_NEW_CUSTOMER_SALE,
                TransactionType.VEHICLE_CUSTOMER_COMPLAINT_REPLACE,
                TransactionType.VEHICLE_CUSTOMER_CHANGE_FULL_OUT,
                TransactionType.VEHICLE_TRANSFER_FULL_OUT,
                TransactionType.VEHICLE_ADJUST_FULL_OUT -> {
                    if (liveVehicleFull < quantity) {
                        return@withTransaction OperationResult.InsufficientStock(
                            title = "Insufficient Vehicle Full Stock",
                            message = "${vehicle!!.name} does not have enough full cylinders for $productLabel.\n\nAvailable Vehicle Full Balance: $liveVehicleFull, Requested: $quantity"
                        )
                    }
                }
                TransactionType.VEHICLE_EMPTY_RETURN_TO_STORE,
                TransactionType.VEHICLE_CUSTOMER_CHANGE_EMPTY_OUT,
                TransactionType.VEHICLE_TRANSFER_EMPTY_OUT,
                TransactionType.VEHICLE_ADJUST_EMPTY_OUT -> {
                    if (liveVehicleEmpty < quantity) {
                        return@withTransaction OperationResult.InsufficientStock(
                            title = "Insufficient Vehicle Empty Stock",
                            message = "${vehicle!!.name} cannot move $quantity empty $productLabel cylinders.\n\nAvailable Vehicle Empty Balance: $liveVehicleEmpty"
                        )
                    }
                }
                TransactionType.VEHICLE_DAMAGED_REMOVE -> {
                    if (liveVehicleDamaged < quantity) {
                        return@withTransaction OperationResult.InsufficientStock(
                            title = "Insufficient Vehicle Damaged Stock",
                            message = "${vehicle!!.name} cannot resolve $quantity damaged $productLabel cylinders.\n\nTracked Vehicle Damaged Balance: $liveVehicleDamaged"
                        )
                    }
                }
                else -> Unit
            }

            val newEntity = StockTransactionEntity(
                businessDate = businessDate,
                timestamp = System.currentTimeMillis(),
                type = type,
                productId = productId,
                vehicleId = vehicle?.id,
                vehicleName = vehicle?.name,
                staffId = actor.id,
                staffName = actor.displayName,
                quantity = quantity,
                purchaseCompanyName = GasNamingSanitizer.sanitizeCompanyOrProductName(purchaseCompanyName).take(120),
                customerName = customerName.trim().take(120),
                notes = notes.trim().take(300),
                remoteId = remoteId
            )
            val rowId = txDao.insert(newEntity)
            insertedEntity = newEntity.copy(id = rowId)
            OperationResult.Success("Recorded $quantity × $productNameLabel (${type.label}).")
        }

        val txToSync = insertedEntity
        if (localResult is OperationResult.Success && txToSync != null && cloudSync != null) {
            val cloudResult = cloudSync.pushTransaction(txToSync)
            if (cloudResult.isFailure) {
                return OperationResult.Success(
                    "Saved $quantity × $productNameLabel locally (will sync when online)."
                )
            }
        }
        return localResult
    }

    /**
     * Atomically records a batch of transactions across multiple products/categories in one database
     * transaction while strictly preventing negative stock on any Store or Vehicle cylinder balance.
     */
    suspend fun recordBatchTransactions(
        businessDate: String,
        type: TransactionType,
        vehicleId: Long?,
        actor: UserAccountEntity,
        items: List<BatchProductEntry>,
        notes: String = "",
        purchaseCompanyName: String = "",
        customerName: String = ""
    ): OperationResult {
        if (actor.role == UserRole.OWNER_VIEW) {
            return OperationResult.ValidationError("Owner View is strictly read-only and cannot modify stock.")
        }
        val mergedItems = items
            .filter { it.quantity > 0 || it.emptyCollectedNow > 0 }
            .groupBy { it.productId }
            .map { (prodId, entries) ->
                BatchProductEntry(
                    productId = prodId,
                    quantity = entries.sumOf { it.quantity },
                    emptyCollectedNow = entries.sumOf { it.emptyCollectedNow }
                )
            }
        if (mergedItems.isEmpty()) {
            return OperationResult.ValidationError("Select at least one product with quantity greater than 0.")
        }
        for (entry in mergedItems) {
            if (entry.quantity <= 0 || entry.quantity > 100000) {
                return OperationResult.ValidationError("Each product quantity must be between 1 and 100,000.")
            }
            if (entry.emptyCollectedNow < 0 || entry.emptyCollectedNow > 100000) {
                return OperationResult.ValidationError("Collected empty quantity must be between 0 and 100,000.")
            }
        }

        val insertedEntities = mutableListOf<StockTransactionEntity>()
        var summaryMessage = ""

        val localResult = db.withTransaction {
            val vehicle = if (vehicleId != null) {
                vehicleDao.getVehicleById(vehicleId)
                    ?: return@withTransaction OperationResult.ValidationError("Selected vehicle not found.")
            } else {
                null
            }
            if (type.isVehicleLevel && vehicle == null) {
                return@withTransaction OperationResult.ValidationError("Please select a vehicle for this transaction.")
            }

            val openings = openingDao.getAllOpeningsOnce()
            val allTxs = txDao.getAllTransactionsOnce()
            val cleanCompany = GasNamingSanitizer.sanitizeCompanyOrProductName(purchaseCompanyName).take(120)
            val cleanCustomer = customerName.trim().take(120)
            val cleanNotes = notes.trim().take(300)
            val nowMs = System.currentTimeMillis()
            var offsetMs = 0L

            val validatedEntries = mutableListOf<Pair<ProductEntity, BatchProductEntry>>()
            for (entry in mergedItems) {
                val product = productDao.getProductById(entry.productId)
                    ?: return@withTransaction OperationResult.ValidationError("Selected product (ID ${entry.productId}) not found.")
                val productLabel = "${product.category.displayName} • ${product.name}"

                val (liveStoreFull, liveStoreEmpty) = StockCalculator.currentStoreBalance(
                    productId = product.id,
                    openings = openings,
                    transactions = allTxs
                )
                val (liveVehicleFull, liveVehicleEmpty) = if (vehicle != null) {
                    StockCalculator.currentVehicleBalance(
                        vehicleId = vehicle.id,
                        productId = product.id,
                        openings = openings,
                        transactions = allTxs
                    )
                } else {
                    0 to 0
                }

                when (type) {
                    TransactionType.VEHICLE_FULL_RECEIVED -> {
                        if (liveStoreFull < entry.quantity) {
                            return@withTransaction OperationResult.InsufficientStock(
                                title = "Insufficient Store Full Stock",
                                message = "Cannot dispatch ${entry.quantity} × $productLabel to ${vehicle!!.name}.\n\nAvailable Store Full Balance: $liveStoreFull"
                            )
                        }
                    }
                    TransactionType.STORE_FULL_DISPATCH_DIRECT,
                    TransactionType.STORE_CUSTOMER_CHANGE_FULL_OUT,
                    TransactionType.STORE_ADJUST_FULL_OUT -> {
                        if (liveStoreFull < entry.quantity) {
                            return@withTransaction OperationResult.InsufficientStock(
                                title = "Insufficient Store Full Stock",
                                message = "Cannot dispatch/deduct ${entry.quantity} × $productLabel.\n\nAvailable Store Full Balance: $liveStoreFull"
                            )
                        }
                    }
                    TransactionType.STORE_PURCHASE_COMPLAINT_REPLACE,
                    TransactionType.STORE_EMPTY_DISPATCH,
                    TransactionType.STORE_CUSTOMER_CHANGE_EMPTY_OUT,
                    TransactionType.STORE_ADJUST_EMPTY_OUT -> {
                        if (liveStoreEmpty < entry.quantity) {
                            return@withTransaction OperationResult.InsufficientStock(
                                title = "Insufficient Store Empty Stock",
                                message = "Cannot dispatch/replace ${entry.quantity} × $productLabel empty cylinders.\n\nAvailable Store Empty Balance: $liveStoreEmpty"
                            )
                        }
                    }
                    TransactionType.VEHICLE_FULL_RETURN_TO_STORE,
                    TransactionType.VEHICLE_CUSTOMER_SALE,
                    TransactionType.VEHICLE_NEW_CUSTOMER_SALE,
                    TransactionType.VEHICLE_CUSTOMER_COMPLAINT_REPLACE,
                    TransactionType.VEHICLE_CUSTOMER_CHANGE_FULL_OUT,
                    TransactionType.VEHICLE_TRANSFER_FULL_OUT,
                    TransactionType.VEHICLE_ADJUST_FULL_OUT -> {
                        if (liveVehicleFull < entry.quantity) {
                            return@withTransaction OperationResult.InsufficientStock(
                                title = "Insufficient Vehicle Full Stock",
                                message = "${vehicle!!.name} does not have enough full cylinders for $productLabel.\n\nAvailable Vehicle Full Balance: $liveVehicleFull, Requested: ${entry.quantity}"
                            )
                        }
                    }
                    TransactionType.VEHICLE_EMPTY_RETURN_TO_STORE,
                    TransactionType.VEHICLE_CUSTOMER_CHANGE_EMPTY_OUT,
                    TransactionType.VEHICLE_TRANSFER_EMPTY_OUT,
                    TransactionType.VEHICLE_ADJUST_EMPTY_OUT -> {
                        if (liveVehicleEmpty < entry.quantity) {
                            return@withTransaction OperationResult.InsufficientStock(
                                title = "Insufficient Vehicle Empty Stock",
                                message = "${vehicle!!.name} cannot move ${entry.quantity} empty $productLabel cylinders.\n\nAvailable Vehicle Empty Balance: $liveVehicleEmpty"
                            )
                        }
                    }
                    else -> Unit
                }

                validatedEntries.add(product to entry)
            }

            for ((product, entry) in validatedEntries) {
                val newEntity = StockTransactionEntity(
                    businessDate = businessDate,
                    timestamp = nowMs + offsetMs++,
                    type = type,
                    productId = product.id,
                    vehicleId = vehicle?.id,
                    vehicleName = vehicle?.name,
                    staffId = actor.id,
                    staffName = actor.displayName,
                    quantity = entry.quantity,
                    purchaseCompanyName = cleanCompany,
                    customerName = cleanCustomer,
                    notes = cleanNotes,
                    remoteId = CloudSyncRepository.generateRemoteTxId()
                )
                val rowId = txDao.insert(newEntity)
                insertedEntities.add(newEntity.copy(id = rowId))

                if (type == TransactionType.VEHICLE_CUSTOMER_SALE && entry.emptyCollectedNow > 0 && vehicle != null) {
                    val emptyEntity = StockTransactionEntity(
                        businessDate = businessDate,
                        timestamp = nowMs + offsetMs++,
                        type = TransactionType.VEHICLE_EMPTY_FROM_CUSTOMER,
                        productId = product.id,
                        vehicleId = vehicle.id,
                        vehicleName = vehicle.name,
                        staffId = actor.id,
                        staffName = actor.displayName,
                        quantity = entry.emptyCollectedNow,
                        purchaseCompanyName = "",
                        customerName = cleanCustomer,
                        notes = cleanNotes,
                        remoteId = CloudSyncRepository.generateRemoteTxId()
                    )
                    val empRowId = txDao.insert(emptyEntity)
                    insertedEntities.add(emptyEntity.copy(id = empRowId))
                }
            }

            val totalQty = mergedItems.sumOf { it.quantity }
            summaryMessage = if (validatedEntries.size == 1) {
                val prodName = validatedEntries.first().first.name
                "Recorded $totalQty × $prodName (${type.label})."
            } else {
                "Saved ${validatedEntries.size} products ($totalQty total cylinders) for ${type.label}."
            }
            OperationResult.Success(summaryMessage)
        }

        if (localResult is OperationResult.Success && cloudSync != null && insertedEntities.isNotEmpty()) {
            var anySyncFailed = false
            for (tx in insertedEntities) {
                if (cloudSync.pushTransaction(tx).isFailure) {
                    anySyncFailed = true
                }
            }
            if (anySyncFailed) {
                return OperationResult.Success("$summaryMessage Saved locally (will sync when online).")
            }
        }
        return localResult
    }

    /**
     * Atomically records Customer Product Change (Full -> Full, Empty -> Empty) or Customer Empty Return
     * for either a Vehicle (Staff or Admin) or the Main Store (Admin, vehicleId == null).
     * Strictly prevents negative balances on the outgoing cylinder product.
     */
    suspend fun recordCustomerProductChange(
        businessDate: String,
        mode: CustomerChangeMode,
        returnedProductId: Long,
        givenProductId: Long?,
        vehicleId: Long?,
        actor: UserAccountEntity,
        quantity: Int,
        customerName: String = "",
        notes: String = ""
    ): OperationResult {
        if (quantity <= 0 || quantity > 100000) {
            return OperationResult.ValidationError("Quantity must be between 1 and 100,000.")
        }
        val givenList = if (mode == CustomerChangeMode.RETURN_EMPTY) {
            emptyList()
        } else {
            val targetGivenId = givenProductId
                ?: return OperationResult.ValidationError("Please select the replacement product to give to the customer.")
            listOf(CustomerExchangeItem(targetGivenId, quantity))
        }
        return recordMultiCustomerProductChange(
            businessDate = businessDate,
            mode = mode,
            returnedItems = listOf(CustomerExchangeItem(returnedProductId, quantity)),
            givenItems = givenList,
            vehicleId = vehicleId,
            actor = actor,
            customerName = customerName,
            notes = notes
        )
    }

    /**
     * Atomically records multi-product Customer Cylinder Exchange (Full -> Full, Empty -> Empty)
     * or multi-product Customer Return Empty across any categories for either Vehicle or Main Store.
     */
    suspend fun recordMultiCustomerProductChange(
        businessDate: String,
        mode: CustomerChangeMode,
        returnedItems: List<CustomerExchangeItem>,
        givenItems: List<CustomerExchangeItem>,
        vehicleId: Long?,
        actor: UserAccountEntity,
        customerName: String = "",
        notes: String = ""
    ): OperationResult {
        if (actor.role == UserRole.OWNER_VIEW) {
            return OperationResult.ValidationError("Owner View is strictly read-only and cannot modify stock.")
        }
        val mergedReturned = returnedItems
            .filter { it.quantity > 0 }
            .groupBy { it.productId }
            .map { (prodId, items) -> CustomerExchangeItem(prodId, items.sumOf { it.quantity }) }

        val mergedGiven = if (mode == CustomerChangeMode.RETURN_EMPTY) {
            emptyList()
        } else {
            givenItems
                .filter { it.quantity > 0 }
                .groupBy { it.productId }
                .map { (prodId, items) -> CustomerExchangeItem(prodId, items.sumOf { it.quantity }) }
        }

        if (mergedReturned.isEmpty()) {
            return OperationResult.ValidationError("Select at least one returned cylinder with quantity greater than 0.")
        }
        for (item in mergedReturned + mergedGiven) {
            if (item.quantity <= 0 || item.quantity > 100000) {
                return OperationResult.ValidationError("Each cylinder quantity must be between 1 and 100,000.")
            }
        }
        if (mode != CustomerChangeMode.RETURN_EMPTY && mergedGiven.isEmpty()) {
            return OperationResult.ValidationError("Select at least one replacement cylinder to give to the customer.")
        }

        val cleanCustomer = customerName.trim().take(120)
        val cleanNote = notes.trim()
        val noteSuffix = if (cleanNote.isNotBlank()) " • $cleanNote" else ""
        val insertedEntities = mutableListOf<StockTransactionEntity>()
        var summaryMessage = ""

        val localResult = db.withTransaction {
            val vehicle = if (vehicleId != null) {
                vehicleDao.getVehicleById(vehicleId)
                    ?: return@withTransaction OperationResult.ValidationError("Selected vehicle not found.")
            } else {
                null
            }
            val scopeLabel = vehicle?.name ?: "Main Store"

            val productMap = mutableMapOf<Long, ProductEntity>()
            for (item in mergedReturned + mergedGiven) {
                if (!productMap.containsKey(item.productId)) {
                    val prod = productDao.getProductById(item.productId)
                        ?: return@withTransaction OperationResult.ValidationError("Product (ID ${item.productId}) not found.")
                    productMap[item.productId] = prod
                }
            }

            if (mode == CustomerChangeMode.RETURN_EMPTY) {
                val returnType = if (vehicle != null) {
                    TransactionType.VEHICLE_EMPTY_FROM_CUSTOMER
                } else {
                    TransactionType.STORE_EMPTY_FROM_CUSTOMER
                }
                val nowMs = System.currentTimeMillis()
                mergedReturned.forEachIndexed { idx, item ->
                    val entity = StockTransactionEntity(
                        businessDate = businessDate,
                        timestamp = nowMs + idx,
                        type = returnType,
                        productId = item.productId,
                        vehicleId = vehicle?.id,
                        vehicleName = vehicle?.name,
                        staffId = actor.id,
                        staffName = actor.displayName,
                        quantity = item.quantity,
                        customerName = cleanCustomer,
                        notes = buildString {
                            append("Customer Return Empty")
                            if (cleanNote.isNotBlank()) append(" • $cleanNote")
                        }.take(300),
                        remoteId = CloudSyncRepository.generateRemoteTxId()
                    )
                    val id = txDao.insert(entity)
                    insertedEntities.add(entity.copy(id = id))
                }
                val totalRet = mergedReturned.sumOf { it.quantity }
                summaryMessage = if (mergedReturned.size == 1) {
                    val prodName = productMap[mergedReturned.first().productId]?.name ?: "Cylinder"
                    "Customer Return Empty ($scopeLabel): $totalRet × $prodName."
                } else {
                    "Customer Return Empty ($scopeLabel): ${mergedReturned.size} products ($totalRet empty)."
                }
                return@withTransaction OperationResult.Success(summaryMessage)
            }

            val openings = openingDao.getAllOpeningsOnce()
            val allTxs = txDao.getAllTransactionsOnce()
            val returnedByProduct = mergedReturned.associate { it.productId to it.quantity }

            for (item in mergedGiven) {
                val givenProd = productMap[item.productId]!!
                val (availFull, availEmpty) = if (vehicle != null) {
                    StockCalculator.currentVehicleBalance(
                        vehicleId = vehicle.id,
                        productId = givenProd.id,
                        openings = openings,
                        transactions = allTxs
                    )
                } else {
                    StockCalculator.currentStoreBalance(
                        productId = givenProd.id,
                        openings = openings,
                        transactions = allTxs
                    )
                }
                val sameReturnedQty = returnedByProduct[givenProd.id] ?: 0
                val givenLabel = "${givenProd.category.displayName} • ${givenProd.name}"

                if (mode == CustomerChangeMode.FULL_TO_FULL) {
                    val effectiveAvail = availFull + sameReturnedQty
                    if (effectiveAvail < item.quantity) {
                        return@withTransaction OperationResult.InsufficientStock(
                            title = "Insufficient Full Stock",
                            message = "$scopeLabel does not have ${item.quantity} Full $givenLabel to give in Full → Full exchange.\n\nAvailable Full: $availFull"
                        )
                    }
                } else if (mode == CustomerChangeMode.EMPTY_TO_EMPTY) {
                    val effectiveAvail = availEmpty + sameReturnedQty
                    if (effectiveAvail < item.quantity) {
                        return@withTransaction OperationResult.InsufficientStock(
                            title = "Insufficient Empty Stock",
                            message = "$scopeLabel does not have ${item.quantity} Empty $givenLabel to give in Empty → Empty exchange.\n\nAvailable Empty: $availEmpty"
                        )
                    }
                }
            }

            val (inType, outType, modeTag) = when {
                mode == CustomerChangeMode.FULL_TO_FULL && vehicle != null -> Triple(
                    TransactionType.VEHICLE_CUSTOMER_CHANGE_FULL_IN,
                    TransactionType.VEHICLE_CUSTOMER_CHANGE_FULL_OUT,
                    "Full→Full Change"
                )
                mode == CustomerChangeMode.FULL_TO_FULL && vehicle == null -> Triple(
                    TransactionType.STORE_CUSTOMER_CHANGE_FULL_IN,
                    TransactionType.STORE_CUSTOMER_CHANGE_FULL_OUT,
                    "Store Full→Full Change"
                )
                mode == CustomerChangeMode.EMPTY_TO_EMPTY && vehicle != null -> Triple(
                    TransactionType.VEHICLE_CUSTOMER_CHANGE_EMPTY_IN,
                    TransactionType.VEHICLE_CUSTOMER_CHANGE_EMPTY_OUT,
                    "Empty→Empty Change"
                )
                else -> Triple(
                    TransactionType.STORE_CUSTOMER_CHANGE_EMPTY_IN,
                    TransactionType.STORE_CUSTOMER_CHANGE_EMPTY_OUT,
                    "Store Empty→Empty Change"
                )
            }

            val retDesc = mergedReturned.joinToString(", ") { "${it.quantity}×${productMap[it.productId]?.name ?: ""}" }
            val givDesc = mergedGiven.joinToString(", ") { "${it.quantity}×${productMap[it.productId]?.name ?: ""}" }
            val nowMs = System.currentTimeMillis()
            var offsetMs = 0L

            mergedReturned.forEach { item ->
                val prod = productMap[item.productId]!!
                val inEntity = StockTransactionEntity(
                    businessDate = businessDate,
                    timestamp = nowMs + offsetMs++,
                    type = inType,
                    productId = prod.id,
                    vehicleId = vehicle?.id,
                    vehicleName = vehicle?.name,
                    staffId = actor.id,
                    staffName = actor.displayName,
                    quantity = item.quantity,
                    customerName = cleanCustomer,
                    notes = "$modeTag: In $retDesc → Out $givDesc$noteSuffix".take(300),
                    remoteId = CloudSyncRepository.generateRemoteTxId()
                )
                val inId = txDao.insert(inEntity)
                insertedEntities.add(inEntity.copy(id = inId))
            }

            mergedGiven.forEach { item ->
                val prod = productMap[item.productId]!!
                val outEntity = StockTransactionEntity(
                    businessDate = businessDate,
                    timestamp = nowMs + offsetMs++,
                    type = outType,
                    productId = prod.id,
                    vehicleId = vehicle?.id,
                    vehicleName = vehicle?.name,
                    staffId = actor.id,
                    staffName = actor.displayName,
                    quantity = item.quantity,
                    customerName = cleanCustomer,
                    notes = "$modeTag: Out $givDesc ← In $retDesc$noteSuffix".take(300),
                    remoteId = CloudSyncRepository.generateRemoteTxId()
                )
                val outId = txDao.insert(outEntity)
                insertedEntities.add(outEntity.copy(id = outId))
            }

            val totalIn = mergedReturned.sumOf { it.quantity }
            val totalOut = mergedGiven.sumOf { it.quantity }
            summaryMessage = if (mergedReturned.size == 1 && mergedGiven.size == 1) {
                "$modeTag ($scopeLabel): $retDesc → $givDesc."
            } else {
                "$modeTag ($scopeLabel): In ${mergedReturned.size} items ($totalIn cyl) → Out ${mergedGiven.size} items ($totalOut cyl)."
            }
            OperationResult.Success(summaryMessage)
        }

        if (localResult is OperationResult.Success && cloudSync != null && insertedEntities.isNotEmpty()) {
            var anySyncFailed = false
            for (tx in insertedEntities) {
                if (cloudSync.pushTransaction(tx).isFailure) {
                    anySyncFailed = true
                }
            }
            if (anySyncFailed) {
                return OperationResult.Success("$summaryMessage Saved locally (will sync when online).")
            }
        }
        return localResult
    }

    /**
     * Records Customer Sale and optional immediate Customer Empty Return atomically.
     */
    suspend fun recordCustomerSaleWithEmpty(
        businessDate: String,
        productId: Long,
        vehicleId: Long,
        actor: UserAccountEntity,
        saleQty: Int,
        emptyCollectedNow: Int,
        customerName: String,
        notes: String
    ): OperationResult {
        val saleRes = recordTransaction(
            businessDate = businessDate,
            type = TransactionType.VEHICLE_CUSTOMER_SALE,
            productId = productId,
            vehicleId = vehicleId,
            actor = actor,
            quantity = saleQty,
            notes = notes,
            customerName = customerName
        )
        if (saleRes !is OperationResult.Success || emptyCollectedNow <= 0) {
            return saleRes
        }
        val emptyRes = recordTransaction(
            businessDate = businessDate,
            type = TransactionType.VEHICLE_EMPTY_FROM_CUSTOMER,
            productId = productId,
            vehicleId = vehicleId,
            actor = actor,
            quantity = emptyCollectedNow,
            notes = notes,
            customerName = customerName
        )
        return if (emptyRes is OperationResult.Success) {
            val pendingDelta = saleQty - emptyCollectedNow
            val pendingSuffix = when {
                pendingDelta > 0 -> " • Pending +$pendingDelta empty"
                pendingDelta < 0 -> " • Extra ${-pendingDelta} empty settled"
                else -> " • Settled"
            }
            OperationResult.Success("Recorded Sale $saleQty & Empty Collected $emptyCollectedNow$pendingSuffix.")
        } else {
            emptyRes
        }
    }

    /**
     * Atomically transfers Full and/or Empty cylinders directly from [sourceVehicleId] to [targetVehicleId].
     * Supports optional Extra Full / Extra Empty cylinders collected and auto-sums total movement:
     * - When no extra cylinders are entered, records standard transfer note with no extra mention.
     * - When extra cylinders are entered, auto-sums Base + Extra and records explicit extra details.
     */
    suspend fun transferBetweenVehicles(
        businessDate: String,
        sourceVehicleId: Long,
        targetVehicleId: Long,
        productId: Long,
        fullQuantity: Int,
        emptyQuantity: Int,
        actor: UserAccountEntity,
        notes: String = "",
        extraFullQuantity: Int = 0,
        extraEmptyQuantity: Int = 0
    ): OperationResult {
        if (actor.role == UserRole.OWNER_VIEW) {
            return OperationResult.ValidationError("Owner View is strictly read-only and cannot transfer stock.")
        }
        if (sourceVehicleId == targetVehicleId) {
            return OperationResult.ValidationError("Source and destination vehicles must be different.")
        }
        if (fullQuantity < 0 || emptyQuantity < 0 || extraFullQuantity < 0 || extraEmptyQuantity < 0) {
            return OperationResult.ValidationError("Cylinder quantities cannot be negative.")
        }
        val totalFull = fullQuantity + extraFullQuantity
        val totalEmpty = emptyQuantity + extraEmptyQuantity
        if ((totalFull + totalEmpty) <= 0) {
            return OperationResult.ValidationError("Enter at least 1 Full or Empty cylinder to transfer.")
        }
        if (totalFull > 100000 || totalEmpty > 100000) {
            return OperationResult.ValidationError("Transfer quantity must be 100,000 or less.")
        }

        val insertedEntities = mutableListOf<StockTransactionEntity>()
        var summaryMessage = ""

        val localResult = db.withTransaction {
            val product = productDao.getProductById(productId)
                ?: return@withTransaction OperationResult.ValidationError("Selected product not found.")
            val sourceVehicle = vehicleDao.getVehicleById(sourceVehicleId)
                ?: return@withTransaction OperationResult.ValidationError("Source vehicle not found.")
            val targetVehicle = vehicleDao.getVehicleById(targetVehicleId)
                ?: return@withTransaction OperationResult.ValidationError("Destination vehicle not found.")

            val openings = openingDao.getAllOpeningsOnce()
            val allTxs = txDao.getAllTransactionsOnce()
            val (liveSourceFull, liveSourceEmpty) = StockCalculator.currentVehicleBalance(
                vehicleId = sourceVehicle.id,
                productId = product.id,
                openings = openings,
                transactions = allTxs
            )

            val productLabel = "${product.category.displayName} • ${product.name}"
            if (totalFull > liveSourceFull) {
                return@withTransaction OperationResult.InsufficientStock(
                    title = "Insufficient Vehicle Full Stock",
                    message = "${sourceVehicle.name} does not have $totalFull Full $productLabel cylinders to transfer.\n\nAvailable Full: $liveSourceFull"
                )
            }
            if (totalEmpty > liveSourceEmpty) {
                return@withTransaction OperationResult.InsufficientStock(
                    title = "Insufficient Vehicle Empty Stock",
                    message = "${sourceVehicle.name} does not have $totalEmpty Empty $productLabel cylinders to transfer.\n\nAvailable Empty: $liveSourceEmpty"
                )
            }

            val cleanNote = notes.trim()
            val hasExtraFull = extraFullQuantity > 0
            val hasExtraEmpty = extraEmptyQuantity > 0
            val fullExtraSuffix = if (hasExtraFull) " (Base $fullQuantity + Extra $extraFullQuantity = $totalFull)" else ""
            val emptyExtraSuffix = if (hasExtraEmpty) " (Base $emptyQuantity + Extra $extraEmptyQuantity = $totalEmpty)" else ""
            val noteSuffix = if (cleanNote.isNotBlank()) " • $cleanNote" else ""
            val nowMs = System.currentTimeMillis()

            suspend fun insertPair(
                outType: TransactionType,
                inType: TransactionType,
                label: String,
                qty: Int,
                extraTag: String,
                offsetMs: Long
            ) {
                val outEntity = StockTransactionEntity(
                    businessDate = businessDate,
                    timestamp = nowMs + offsetMs,
                    type = outType,
                    productId = product.id,
                    vehicleId = sourceVehicle.id,
                    vehicleName = sourceVehicle.name,
                    staffId = actor.id,
                    staffName = actor.displayName,
                    quantity = qty,
                    notes = "Transfer $label → ${targetVehicle.name}$extraTag$noteSuffix".take(300),
                    remoteId = CloudSyncRepository.generateRemoteTxId()
                )
                val outId = txDao.insert(outEntity)
                insertedEntities.add(outEntity.copy(id = outId))

                val inEntity = StockTransactionEntity(
                    businessDate = businessDate,
                    timestamp = nowMs + offsetMs + 1L,
                    type = inType,
                    productId = product.id,
                    vehicleId = targetVehicle.id,
                    vehicleName = targetVehicle.name,
                    staffId = actor.id,
                    staffName = actor.displayName,
                    quantity = qty,
                    notes = "Transfer $label ← ${sourceVehicle.name}$extraTag$noteSuffix".take(300),
                    remoteId = CloudSyncRepository.generateRemoteTxId()
                )
                val inId = txDao.insert(inEntity)
                insertedEntities.add(inEntity.copy(id = inId))
            }

            if (totalFull > 0) {
                insertPair(
                    outType = TransactionType.VEHICLE_TRANSFER_FULL_OUT,
                    inType = TransactionType.VEHICLE_TRANSFER_FULL_IN,
                    label = "Full",
                    qty = totalFull,
                    extraTag = fullExtraSuffix,
                    offsetMs = 0L
                )
            }
            if (totalEmpty > 0) {
                insertPair(
                    outType = TransactionType.VEHICLE_TRANSFER_EMPTY_OUT,
                    inType = TransactionType.VEHICLE_TRANSFER_EMPTY_IN,
                    label = "Empty",
                    qty = totalEmpty,
                    extraTag = emptyExtraSuffix,
                    offsetMs = 2L
                )
            }

            val parts = buildList {
                if (totalFull > 0) {
                    add(if (hasExtraFull) "$totalFull Full (+$extraFullQuantity Extra)" else "$totalFull Full")
                }
                if (totalEmpty > 0) {
                    add(if (hasExtraEmpty) "$totalEmpty Empty (+$extraEmptyQuantity Extra)" else "$totalEmpty Empty")
                }
            }.joinToString(" & ")
            summaryMessage = "Transferred $parts ${product.name}: ${sourceVehicle.name} → ${targetVehicle.name}."
            OperationResult.Success(summaryMessage)
        }

        if (localResult is OperationResult.Success && cloudSync != null && insertedEntities.isNotEmpty()) {
            var anySyncFailed = false
            for (tx in insertedEntities) {
                if (cloudSync.pushTransaction(tx).isFailure) {
                    anySyncFailed = true
                }
            }
            if (anySyncFailed) {
                return OperationResult.Success("$summaryMessage Saved locally (will sync when online).")
            }
        }
        return localResult
    }

    fun observeAttendanceByDate(date: String): Flow<List<StaffAttendanceEntity>> =
        attendanceDao.observeAttendanceByDate(date)

    suspend fun getAttendanceForStaffAndDate(date: String, staffId: Long): StaffAttendanceEntity? =
        attendanceDao.getForStaffAndDate(date, staffId)

    suspend fun upsertStaffAttendance(entity: StaffAttendanceEntity): StaffAttendanceEntity {
        val rowId = attendanceDao.upsert(entity)
        val saved = entity.copy(id = if (entity.id > 0L) entity.id else rowId)
        cloudSync?.upsertStaffAttendance(saved)
        return saved
    }

    suspend fun getPendingStoreTransferById(id: Long): PendingStoreTransferEntity? =
        pendingStoreTransferDao.getById(id)

    suspend fun createPendingStoreTransfer(entity: PendingStoreTransferEntity): PendingStoreTransferEntity {
        val rowId = pendingStoreTransferDao.upsert(entity)
        val saved = entity.copy(id = rowId)
        cloudSync?.createPendingStoreTransfer(saved)
        return saved
    }

    suspend fun updatePendingStoreTransferStatus(
        entity: PendingStoreTransferEntity,
        newStatus: String,
        reviewedAtMs: Long,
        reviewedBy: String,
        reviewNotes: String
    ): PendingStoreTransferEntity {
        pendingStoreTransferDao.updateStatus(
            id = entity.id,
            status = newStatus,
            reviewedAtMs = reviewedAtMs,
            reviewedBy = reviewedBy,
            reviewNotes = reviewNotes
        )
        val updated = entity.copy(
            status = newStatus,
            reviewedAtMs = reviewedAtMs,
            reviewedBy = reviewedBy,
            reviewNotes = reviewNotes
        )
        cloudSync?.updatePendingStoreTransferStatus(updated)
        return updated
    }

    suspend fun createAppNotification(entity: AppNotificationEntity): AppNotificationEntity {
        val rowId = notificationDao.upsert(entity)
        val saved = entity.copy(id = if (entity.id > 0L) entity.id else rowId)
        cloudSync?.createAppNotification(saved)
        return saved
    }

    suspend fun markNotificationRead(id: Long) {
        notificationDao.markRead(id)
    }

    suspend fun markAllNotificationsRead() {
        notificationDao.markAllRead()
    }

    suspend fun sendChatMessage(entity: ChatMessageEntity): ChatMessageEntity {
        val rowId = chatMessageDao.insertIgnore(entity)
        val saved = entity.copy(id = if (rowId > 0L) rowId else entity.id)
        cloudSync?.createChatMessage(saved)
        return saved
    }

    suspend fun getVehicleById(vehicleId: Long): VehicleEntity? =
        vehicleDao.getVehicleById(vehicleId)

    suspend fun getProductById(productId: Long): ProductEntity? =
        productDao.getProductById(productId)

    /**
     * Admin-only: sets desired Opening Full & Opening Empty for Store or a Vehicle on [date],
     * automatically calculating base opening offset and preventing negative balances.
     */
    suspend fun setOpeningStockForDate(
        scope: String,
        vehicleId: Long,
        productId: Long,
        date: String,
        desiredOpeningFull: Int,
        desiredOpeningEmpty: Int,
        admin: UserAccountEntity
    ): OperationResult {
        if (admin.role != UserRole.ADMIN) {
            return OperationResult.ValidationError("Only Admin can modify opening stock.")
        }
        if (desiredOpeningFull < 0 || desiredOpeningEmpty < 0) {
            return OperationResult.ValidationError("Opening stock cannot be negative.")
        }

        return db.withTransaction {
            val openings = openingDao.getAllOpeningsOnce()
            val allTxs = txDao.getAllTransactionsOnce()
            val existingBase = openingDao.getOpening(scope, vehicleId, productId)

            var priorFull = 0
            var priorEmpty = 0
            var futureOrTodayFull = 0
            var futureOrTodayEmpty = 0

            for (tx in allTxs) {
                if (tx.productId != productId) continue
                if (scope == StockCalculator.SCOPE_VEHICLE && tx.vehicleId != vehicleId) continue

                val df = if (scope == StockCalculator.SCOPE_STORE) StockCalculator.storeFullDelta(tx)
                else StockCalculator.vehicleFullDelta(tx)
                val de = if (scope == StockCalculator.SCOPE_STORE) StockCalculator.storeEmptyDelta(tx)
                else StockCalculator.vehicleEmptyDelta(tx)

                if (tx.businessDate < date) {
                    priorFull += df
                    priorEmpty += de
                } else {
                    futureOrTodayFull += df
                    futureOrTodayEmpty += de
                }
            }

            val projectedEndFull = desiredOpeningFull + futureOrTodayFull
            val projectedEndEmpty = desiredOpeningEmpty + futureOrTodayEmpty
            if (projectedEndFull < 0 || projectedEndEmpty < 0) {
                return@withTransaction OperationResult.InsufficientStock(
                    title = "Negative Balance Prevented",
                    message = "Setting opening stock to Full=$desiredOpeningFull, Empty=$desiredOpeningEmpty would make current balance negative (Projected Full=$projectedEndFull, Empty=$projectedEndEmpty)."
                )
            }

            val newBaseFull = desiredOpeningFull - priorFull
            val newBaseEmpty = desiredOpeningEmpty - priorEmpty

            val savedOpening = OpeningStockEntity(
                id = existingBase?.id ?: 0L,
                scope = scope,
                vehicleId = vehicleId,
                productId = productId,
                openingFull = newBaseFull,
                openingEmpty = newBaseEmpty,
                updatedAt = System.currentTimeMillis(),
                updatedBy = admin.displayName
            )
            openingDao.upsert(savedOpening)
            cloudSync?.pushOpeningStock(savedOpening)
            OperationResult.Success("Opening stock updated.")
        }
    }

    /**
     * Admin-only: deletes a transaction if doing so does not cause negative balances.
     */
    suspend fun deleteTransaction(transactionId: Long, admin: UserAccountEntity): OperationResult {
        if (admin.role != UserRole.ADMIN) {
            return OperationResult.ValidationError("Only Admin can delete transactions.")
        }
        var deletedRemoteId: String? = null
        val localResult = db.withTransaction {
            val target = txDao.getTransactionById(transactionId)
                ?: return@withTransaction OperationResult.ValidationError("Transaction not found.")

            val openings = openingDao.getAllOpeningsOnce()
            val remainingTxs = txDao.getAllTransactionsOnce().filter { it.id != transactionId }

            val (storeFull, storeEmpty) = StockCalculator.currentStoreBalance(
                productId = target.productId,
                openings = openings,
                transactions = remainingTxs
            )
            if (storeFull < 0 || storeEmpty < 0) {
                return@withTransaction OperationResult.InsufficientStock(
                    title = "Cannot Delete Transaction",
                    message = "Deleting this entry would result in negative Store stock (Full=$storeFull, Empty=$storeEmpty) due to subsequent dispatches."
                )
            }

            if (target.vehicleId != null) {
                val (vFull, vEmpty) = StockCalculator.currentVehicleBalance(
                    vehicleId = target.vehicleId,
                    productId = target.productId,
                    openings = openings,
                    transactions = remainingTxs
                )
                if (vFull < 0 || vEmpty < 0) {
                    return@withTransaction OperationResult.InsufficientStock(
                        title = "Cannot Delete Transaction",
                        message = "Deleting this entry would result in negative stock on ${target.vehicleName} (Full=$vFull, Empty=$vEmpty)."
                    )
                }
            }

            deletedRemoteId = target.remoteId
            txDao.deleteById(transactionId)
            OperationResult.Success("Transaction deleted.")
        }
        if (localResult is OperationResult.Success && deletedRemoteId != null) {
            cloudSync?.deleteCloudTransaction(deletedRemoteId!!)
        }
        return localResult
    }

    /**
     * Admin-only: edits an existing transaction (product, quantity, supplier company, customer name, notes)
     * while strictly enforcing the negative-stock guard across all subsequent transactions.
     */
    suspend fun updateTransaction(
        transactionId: Long,
        newProductId: Long,
        newQuantity: Int,
        newPurchaseCompanyName: String,
        newCustomerName: String,
        newNotes: String,
        admin: UserAccountEntity
    ): OperationResult {
        if (admin.role != UserRole.ADMIN) {
            return OperationResult.ValidationError("Only Admin can edit transactions.")
        }
        if (newQuantity <= 0 || newQuantity > 100000) {
            return OperationResult.ValidationError("Quantity must be between 1 and 100,000.")
        }

        var updatedEntity: StockTransactionEntity? = null
        val localResult = db.withTransaction {
            val existing = txDao.getTransactionById(transactionId)
                ?: return@withTransaction OperationResult.ValidationError("Transaction not found.")
            productDao.getProductById(newProductId)
                ?: return@withTransaction OperationResult.ValidationError("Selected product not found.")

            val candidate = existing.copy(
                productId = newProductId,
                quantity = newQuantity,
                purchaseCompanyName = GasNamingSanitizer.sanitizeCompanyOrProductName(newPurchaseCompanyName).take(120),
                customerName = newCustomerName.trim().take(120),
                notes = newNotes.trim().take(300)
            )

            val openings = openingDao.getAllOpeningsOnce()
            val simulatedTxs = txDao.getAllTransactionsOnce().map { tx ->
                if (tx.id == transactionId) candidate else tx
            }

            val affectedProductIds = setOf(existing.productId, newProductId)
            for (pId in affectedProductIds) {
                val (storeFull, storeEmpty) = StockCalculator.currentStoreBalance(
                    productId = pId,
                    openings = openings,
                    transactions = simulatedTxs
                )
                if (storeFull < 0 || storeEmpty < 0) {
                    return@withTransaction OperationResult.InsufficientStock(
                        title = "Invalid Edit (Negative Store Stock)",
                        message = "Editing this entry would cause negative Store balance (Full=$storeFull, Empty=$storeEmpty)."
                    )
                }
                if (candidate.vehicleId != null) {
                    val (vFull, vEmpty) = StockCalculator.currentVehicleBalance(
                        vehicleId = candidate.vehicleId,
                        productId = pId,
                        openings = openings,
                        transactions = simulatedTxs
                    )
                    if (vFull < 0 || vEmpty < 0) {
                        return@withTransaction OperationResult.InsufficientStock(
                            title = "Invalid Edit (Negative Vehicle Stock)",
                            message = "Editing this entry would cause negative stock on ${candidate.vehicleName ?: "vehicle"} (Full=$vFull, Empty=$vEmpty)."
                        )
                    }
                }
            }

            txDao.update(candidate)
            updatedEntity = candidate
            OperationResult.Success("Transaction updated successfully.")
        }

        val toPush = updatedEntity
        if (localResult is OperationResult.Success && toPush != null && cloudSync != null) {
            val cloudRes = cloudSync.pushTransactionUpdate(toPush)
            if (cloudRes.isFailure) {
                return OperationResult.Success("Updated locally (will sync when online).")
            }
        }
        return localResult
    }

    fun observeCustomerPendingBalances(vehicleIdFilter: Long? = null): Flow<List<CustomerPendingRow>> =
        combine(transactionsFlow, allProductsFlow, vehiclesFlow) { txs, products, vehicles ->
            val productMap = products.associateBy { it.id }
            val vehicleMap = vehicles.associateBy { it.id }

            val relevantTypes = setOf(
                TransactionType.VEHICLE_CUSTOMER_SALE,
                TransactionType.VEHICLE_EMPTY_FROM_CUSTOMER,
                TransactionType.CUSTOMER_EMPTY_PENDING_ADD,
                TransactionType.STORE_FULL_DISPATCH_DIRECT,
                TransactionType.STORE_EMPTY_INWARD,
                TransactionType.STORE_EMPTY_FROM_CUSTOMER
            )

            val grouped = linkedMapOf<String, MutableList<StockTransactionEntity>>()
            for (tx in txs) {
                if (tx.type !in relevantTypes) continue
                if (vehicleIdFilter != null && tx.vehicleId != vehicleIdFilter) continue
                val cleanCustomer = tx.customerName.trim()
                if (cleanCustomer.isEmpty()) continue
                grouped.getOrPut(cleanCustomer.lowercase(Locale.US)) { mutableListOf() }.add(tx)
            }

            grouped.values.map { list ->
                val sorted = list.sortedWith(
                    compareBy<StockTransactionEntity> { it.businessDate }
                        .thenBy { it.timestamp }
                        .thenBy { it.id }
                )
                val latest = sorted.last()
                val displayName = latest.customerName.trim()
                val fullSold = sorted.filter {
                    it.type == TransactionType.VEHICLE_CUSTOMER_SALE ||
                        it.type == TransactionType.STORE_FULL_DISPATCH_DIRECT
                }.sumOf { it.quantity }
                val emptyCollected = sorted.filter {
                    it.type == TransactionType.VEHICLE_EMPTY_FROM_CUSTOMER ||
                        it.type == TransactionType.STORE_EMPTY_INWARD ||
                        it.type == TransactionType.STORE_EMPTY_FROM_CUSTOMER
                }.sumOf { it.quantity }
                val manualPending = sorted.filter {
                    it.type == TransactionType.CUSTOMER_EMPTY_PENDING_ADD
                }.sumOf { it.quantity }

                val pendingBalance = fullSold + manualPending - emptyCollected
                val latestVehicleId = latest.vehicleId
                val latestVehicleName = latest.vehicleName
                    ?: latestVehicleId?.let { vehicleMap[it]?.name }
                    ?: "Store"
                val latestProd = productMap[latest.productId]

                CustomerPendingRow(
                    customerName = displayName,
                    primaryVehicleId = latestVehicleId,
                    primaryVehicleName = latestVehicleName,
                    fullSold = fullSold,
                    emptyCollected = emptyCollected,
                    manualPendingAdded = manualPending,
                    pendingEmptyBalance = pendingBalance,
                    lastActivityDate = latest.businessDate,
                    lastProductId = latest.productId,
                    lastProductName = latestProd?.let { "${it.category.shortCode} - ${it.name}" } ?: ""
                )
            }.sortedWith(
                compareByDescending<CustomerPendingRow> { it.pendingEmptyBalance > 0 }
                    .thenByDescending { it.pendingEmptyBalance }
                    .thenByDescending { it.lastActivityDate }
                    .thenBy { it.customerName.lowercase(Locale.US) }
            )
        }.flowOn(Dispatchers.Default)

    // --- Report Queries ---

    fun observeMonthlySalesReport(yearMonth: String): Flow<List<MonthlyDailySaleRow>> =
        transactionsFlow.combine(activeProductsFlow) { txs, _ ->
            val monthTxs = txs.filter { it.businessDate.startsWith(yearMonth) }
            monthTxs.groupBy { it.businessDate }
                .entries
                .sortedBy { it.key }
                .map { (date, dayList) ->
                    MonthlyDailySaleRow(
                        date = date,
                        totalSold = dayList.filter {
                            it.type == TransactionType.VEHICLE_CUSTOMER_SALE ||
                                it.type == TransactionType.VEHICLE_NEW_CUSTOMER_SALE
                        }.sumOf { it.quantity },
                        totalPurchases = dayList.filter {
                            it.type == TransactionType.STORE_PURCHASE_FULL ||
                                it.type == TransactionType.STORE_PURCHASE_EMPTY ||
                                it.type == TransactionType.STORE_PURCHASE_COMPLAINT_REPLACE
                        }.sumOf { it.quantity },
                        totalDispatch = dayList.filter {
                            it.type == TransactionType.VEHICLE_FULL_RECEIVED ||
                                it.type == TransactionType.STORE_FULL_DISPATCH_DIRECT
                        }.sumOf { it.quantity },
                        totalEmptyReturned = dayList.filter {
                            it.type == TransactionType.VEHICLE_EMPTY_RETURN_TO_STORE
                        }.sumOf { it.quantity }
                    )
                }
        }.flowOn(Dispatchers.Default)

    fun observeProductWiseSales(yearMonth: String): Flow<List<ProductSalesRow>> =
        combine(activeProductsFlow, transactionsFlow) { products, txs ->
            val monthTxs = txs.filter { it.businessDate.startsWith(yearMonth) }
            products.map { product ->
                val pTxs = monthTxs.filter { it.productId == product.id }
                ProductSalesRow(
                    product = product,
                    totalSold = pTxs.filter {
                        it.type == TransactionType.VEHICLE_CUSTOMER_SALE ||
                            it.type == TransactionType.VEHICLE_NEW_CUSTOMER_SALE
                    }.sumOf { it.quantity },
                    emptyCollected = pTxs.filter { it.type == TransactionType.VEHICLE_EMPTY_FROM_CUSTOMER }
                        .sumOf { it.quantity }
                )
            }
        }.flowOn(Dispatchers.Default)

    fun observeStaffWiseSales(yearMonth: String, asOfDate: String): Flow<List<StaffSalesRow>> =
        combine(vehiclesFlow, activeProductsFlow, openingsFlow, transactionsFlow) { vehicles, products, openings, txs ->
            val monthTxs = txs.filter { it.businessDate.startsWith(yearMonth) }
            vehicles.map { vehicle ->
                val vMonthTxs = monthTxs.filter { it.vehicleId == vehicle.id }
                val vTodayRows = StockCalculator.calculateVehicleRowsForDate(
                    date = asOfDate,
                    vehicle = vehicle,
                    products = products,
                    openings = openings,
                    transactions = txs
                )
                StaffSalesRow(
                    vehicle = vehicle,
                    totalFullReceived = vMonthTxs.filter { it.type == TransactionType.VEHICLE_FULL_RECEIVED }
                        .sumOf { it.quantity },
                    totalSold = vMonthTxs.filter {
                        it.type == TransactionType.VEHICLE_CUSTOMER_SALE ||
                            it.type == TransactionType.VEHICLE_NEW_CUSTOMER_SALE
                    }.sumOf { it.quantity },
                    totalEmptyCollected = vMonthTxs.filter { it.type == TransactionType.VEHICLE_EMPTY_FROM_CUSTOMER }
                        .sumOf { it.quantity },
                    totalEmptyReturned = vMonthTxs.filter { it.type == TransactionType.VEHICLE_EMPTY_RETURN_TO_STORE }
                        .sumOf { it.quantity },
                    closingFullBalance = vTodayRows.sumOf { it.fullBalance },
                    closingEmptyBalance = vTodayRows.sumOf { it.emptyBalance }
                )
            }
        }.flowOn(Dispatchers.Default)

    fun observeCompanyStockReport(asOfDate: String): Flow<List<CompanyStockRow>> =
        combine(vehiclesFlow, activeProductsFlow, openingsFlow, transactionsFlow) { vehicles, products, openings, txs ->
            val storeRows = StockCalculator.calculateStoreRowsForDate(asOfDate, products, openings, txs)
                .associateBy { it.product.id }
            val vehicleRowsByProduct = mutableMapOf<Long, MutableList<VehicleStockRow>>()
            for (vehicle in vehicles) {
                val rows = StockCalculator.calculateVehicleRowsForDate(asOfDate, vehicle, products, openings, txs)
                for (row in rows) {
                    vehicleRowsByProduct.getOrPut(row.product.id) { mutableListOf() }.add(row)
                }
            }
            val newCustOutByProduct = txs
                .filter {
                    it.type == TransactionType.VEHICLE_NEW_CUSTOMER_SALE &&
                        it.businessDate <= asOfDate
                }
                .groupBy { it.productId }
                .mapValues { (_, list) -> list.sumOf { it.quantity } }

            products.map { product ->
                val store = storeRows[product.id]
                val vRows = vehicleRowsByProduct[product.id].orEmpty()
                val sFull = store?.storeFullBalance ?: 0
                val vFull = vRows.sumOf { it.fullBalance }
                val sEmpty = store?.storeEmptyBalance ?: 0
                val vEmpty = vRows.sumOf { it.emptyBalance }
                CompanyStockRow(
                    product = product,
                    storeFull = sFull,
                    vehiclesFull = vFull,
                    totalFull = sFull + vFull,
                    storeEmpty = sEmpty,
                    vehiclesEmpty = vEmpty,
                    totalEmpty = sEmpty + vEmpty,
                    newCustomerSoldOut = newCustOutByProduct[product.id] ?: 0
                )
            }
        }.flowOn(Dispatchers.Default)

    // --- Corporate Security: UAE (+971) Authorized Mobiles, Devices & Audit Logs ---

    suspend fun findAuthorizedMobileByPhone(rawPhone: String): AuthorizedMobileEntity? {
        val normalized = UaePhoneValidator.normalizeUaePhone(rawPhone) ?: return null
        return authorizedMobileDao.getByPhone(normalized)
    }

    suspend fun getStaffPermissionsForUser(staffUserId: Long): Set<StaffFeaturePermission> {
        val mobile = authorizedMobileDao.getActiveByStaffUserId(staffUserId)
            ?: return StaffFeaturePermission.entries.toSet()
        return StaffFeaturePermission.parseCsv(mobile.allowedFeaturesCsv)
    }

    /**
     * Admin-only registration & management of UAE (+971) authorized mobile numbers.
     * Enforces strict Protected Owner immutability: Admin can NEVER alter, demote,
     * deactivate, or overwrite an existing Protected Owner mobile record.
     */
    suspend fun upsertAuthorizedMobile(
        phoneRaw: String,
        displayName: String,
        role: UserRole,
        staffUserId: Long?,
        allowedFeatures: Set<StaffFeaturePermission>,
        isActive: Boolean,
        isProtectedOwner: Boolean,
        actor: UserAccountEntity,
        mobileId: Long? = null,
        firstTimeRegEnabled: Boolean = true
    ): OperationResult {
        if (actor.role != UserRole.ADMIN) {
            return OperationResult.ValidationError("Only Admin can manage authorized UAE mobile numbers.")
        }
        val normalizedPhone = UaePhoneValidator.normalizeUaePhone(phoneRaw)
            ?: return OperationResult.ValidationError("Enter a valid UAE mobile number (+971 5X XXX XXXX).")
        val cleanName = displayName.trim().take(100)
        if (cleanName.isEmpty()) {
            return OperationResult.ValidationError("Display name is required.")
        }

        val nowMs = System.currentTimeMillis()
        var savedEntity: AuthorizedMobileEntity? = null
        var replacedOldPhone: String? = null
        val result = db.withTransaction {
            val existingById = mobileId?.let { authorizedMobileDao.getById(it) }
            val existingByPhone = authorizedMobileDao.getByPhone(normalizedPhone)
            val existing = existingById ?: existingByPhone
            if (existing != null && existing.isProtectedOwner) {
                return@withTransaction OperationResult.ValidationError(
                    "Protected Owner account cannot be modified, demoted, or deactivated by Admin."
                )
            }
            if (existingById != null && existingByPhone != null && existingByPhone.id != existingById.id) {
                return@withTransaction OperationResult.ValidationError(
                    "Mobile ${UaePhoneValidator.formatDisplay(normalizedPhone)} is already registered to ${existingByPhone.displayName}."
                )
            }
            val phoneChanged = existingById != null && existingById.phoneE164 != normalizedPhone
            if (phoneChanged) {
                replacedOldPhone = existingById!!.phoneE164
            }
            val effectiveProtectedOwner = if (role == UserRole.OWNER_VIEW) isProtectedOwner else false
            val featuresCsv = if (role == UserRole.STAFF) {
                StaffFeaturePermission.toCsv(allowedFeatures.ifEmpty { setOf(StaffFeaturePermission.MY_VEHICLE_STOCK) })
            } else {
                StaffFeaturePermission.ALL_DEFAULT_CSV
            }
            val entity = AuthorizedMobileEntity(
                id = existing?.id ?: 0L,
                phoneE164 = normalizedPhone,
                displayName = cleanName,
                role = role,
                staffUserId = if (role == UserRole.STAFF) staffUserId else null,
                allowedFeaturesCsv = featuresCsv,
                isActive = isActive,
                isProtectedOwner = effectiveProtectedOwner,
                createdAtMs = existing?.createdAtMs ?: nowMs,
                updatedAtMs = nowMs,
                firstTimeRegEnabled = firstTimeRegEnabled,
                isRegistered = if (phoneChanged) false else (existing?.isRegistered ?: false),
                otpVerifiedAtMs = if (phoneChanged) 0L else (existing?.otpVerifiedAtMs ?: 0L),
                registeredDeviceId = if (phoneChanged) "" else (existing?.registeredDeviceId ?: ""),
                failedLoginAttempts = existing?.failedLoginAttempts ?: 0,
                lockedUntilMs = existing?.lockedUntilMs ?: 0L
            )
            val rowId = authorizedMobileDao.upsert(entity)
            savedEntity = entity.copy(id = if (entity.id > 0L) entity.id else rowId)
            OperationResult.Success("Authorized ${UaePhoneValidator.formatDisplay(normalizedPhone)} ($cleanName • ${role.name}).")
        }

        if (result is OperationResult.Success && savedEntity != null) {
            replacedOldPhone?.let { cloudSync?.deleteAuthorizedMobile(it) }
            cloudSync?.pushAuthorizedMobile(savedEntity!!)
            logSecurityEvent(
                eventType = "MOBILE_ACCESS_UPDATED",
                role = actor.role.name,
                actorName = actor.displayName,
                phoneE164 = savedEntity!!.phoneE164,
                deviceLabel = "",
                details = "Authorized ${savedEntity!!.displayName} (${savedEntity!!.role.name}, active=${savedEntity!!.isActive}, firstTimeReg=${savedEntity!!.firstTimeRegEnabled})",
                isAlert = false
            )
        }
        return result
    }

    suspend fun markMobileRegistered(
        mobileId: Long,
        deviceId: String,
        otpVerifiedAtMs: Long = System.currentTimeMillis()
    ) {
        val nowMs = System.currentTimeMillis()
        authorizedMobileDao.updateRegistrationStatus(
            id = mobileId,
            isRegistered = true,
            otpVerifiedAtMs = otpVerifiedAtMs,
            deviceId = deviceId.take(120),
            updatedAtMs = nowMs
        )
        authorizedMobileDao.updateLoginAttempts(
            id = mobileId,
            attempts = 0,
            lockedUntilMs = 0L,
            updatedAtMs = nowMs
        )
        authorizedMobileDao.getById(mobileId)?.let { updated ->
            cloudSync?.pushAuthorizedMobile(updated)
        }
    }

    suspend fun recordMobilePinFailure(mobileId: Long): Pair<Int, Long> {
        val existing = authorizedMobileDao.getById(mobileId) ?: return 0 to 0L
        val nowMs = System.currentTimeMillis()
        val nextAttempts = existing.failedLoginAttempts + 1
        val lockedUntil = if (nextAttempts >= MAX_PIN_ATTEMPTS) nowMs + PIN_LOCKOUT_MS else 0L
        authorizedMobileDao.updateLoginAttempts(
            id = mobileId,
            attempts = nextAttempts,
            lockedUntilMs = lockedUntil,
            updatedAtMs = nowMs
        )
        if (lockedUntil > nowMs) {
            logSecurityEvent(
                eventType = "PIN_LOCKOUT",
                role = existing.role.name,
                actorName = existing.displayName,
                phoneE164 = existing.phoneE164,
                details = "Account temporarily locked for 15 minutes after $nextAttempts failed PIN attempts.",
                isAlert = true
            )
        }
        return nextAttempts to lockedUntil
    }

    suspend fun resetMobilePinFailures(mobileId: Long) {
        authorizedMobileDao.updateLoginAttempts(
            id = mobileId,
            attempts = 0,
            lockedUntilMs = 0L,
            updatedAtMs = System.currentTimeMillis()
        )
    }

    suspend fun getRegisteredDeviceById(deviceId: String): RegisteredDeviceEntity? =
        registeredDeviceDao.getByDeviceId(deviceId)

    suspend fun deleteAuthorizedMobile(mobileId: Long, actor: UserAccountEntity): OperationResult {
        if (actor.role != UserRole.ADMIN) {
            return OperationResult.ValidationError("Only Admin can remove authorized mobile numbers.")
        }
        var deletedPhone: String? = null
        val result = db.withTransaction {
            val existing = authorizedMobileDao.getById(mobileId)
                ?: return@withTransaction OperationResult.ValidationError("Authorized mobile record not found.")
            if (existing.isProtectedOwner) {
                return@withTransaction OperationResult.ValidationError(
                    "Protected Owner mobile number cannot be deleted or overridden by Admin."
                )
            }
            deletedPhone = existing.phoneE164
            authorizedMobileDao.deleteById(mobileId)
            OperationResult.Success("Removed authorized mobile ${UaePhoneValidator.formatDisplay(existing.phoneE164)}.")
        }
        if (result is OperationResult.Success && deletedPhone != null) {
            cloudSync?.deleteAuthorizedMobile(deletedPhone!!)
            logSecurityEvent(
                eventType = "MOBILE_ACCESS_REMOVED",
                role = actor.role.name,
                actorName = actor.displayName,
                phoneE164 = deletedPhone!!,
                deviceLabel = "",
                details = "Removed authorized UAE mobile $deletedPhone",
                isAlert = true
            )
        }
        return result
    }

    suspend fun registerOrTouchDevice(
        deviceId: String,
        deviceLabel: String,
        phoneE164: String,
        userId: Long,
        displayName: String,
        role: UserRole
    ): RegisteredDeviceEntity {
        val nowMs = System.currentTimeMillis()
        val existing = registeredDeviceDao.getByDeviceId(deviceId)
        val entity = RegisteredDeviceEntity(
            id = existing?.id ?: 0L,
            deviceId = deviceId.take(100),
            deviceLabel = deviceLabel.take(120),
            phoneE164 = phoneE164.take(30),
            userId = userId,
            displayName = displayName.take(100),
            role = role,
            sessionVersion = (existing?.sessionVersion ?: 0L) + 1L,
            isRevoked = false,
            lastActiveMs = nowMs,
            registeredAtMs = existing?.registeredAtMs ?: nowMs
        )
        val rowId = registeredDeviceDao.upsert(entity)
        val saved = entity.copy(id = if (entity.id > 0L) entity.id else rowId)
        if (role != UserRole.OWNER_VIEW) {
            cloudSync?.pushRegisteredDevice(saved)
        }
        return saved
    }

    suspend fun isDeviceRevoked(deviceId: String): Boolean {
        return registeredDeviceDao.getByDeviceId(deviceId)?.isRevoked == true
    }

    suspend fun revokeDevice(deviceId: String, actor: UserAccountEntity): OperationResult {
        if (actor.role != UserRole.ADMIN) {
            return OperationResult.ValidationError("Only Admin can revoke registered devices.")
        }
        val existing = registeredDeviceDao.getByDeviceId(deviceId)
            ?: return OperationResult.ValidationError("Device not found.")
        val nowMs = System.currentTimeMillis()
        registeredDeviceDao.revokeDevice(deviceId)
        authorizedMobileDao.clearRegistrationForDevice(deviceId, nowMs)
        val updated = existing.copy(isRevoked = true, sessionVersion = existing.sessionVersion + 1L)
        cloudSync?.pushRegisteredDevice(updated)
        logSecurityEvent(
            eventType = "DEVICE_REVOKED",
            role = actor.role.name,
            actorName = actor.displayName,
            phoneE164 = existing.phoneE164,
            deviceLabel = existing.deviceLabel,
            details = "Revoked device ${existing.deviceLabel} (${existing.displayName}); mobile OTP + PIN re-registration required.",
            isAlert = true
        )
        return OperationResult.Success("Revoked device ${existing.deviceLabel}. Mobile + OTP + PIN required on next login.")
    }

    suspend fun logoutAllDevices(actor: UserAccountEntity): OperationResult {
        if (actor.role != UserRole.ADMIN) {
            return OperationResult.ValidationError("Only Admin can sign out all registered devices.")
        }
        val activeDevices = registeredDeviceDao.getAllOnce().filter { !it.isRevoked }
        val nowMs = System.currentTimeMillis()
        registeredDeviceDao.revokeAllDevices()
        authorizedMobileDao.clearAllRegistrations(nowMs)
        activeDevices.forEach { dev ->
            cloudSync?.pushRegisteredDevice(dev.copy(isRevoked = true, sessionVersion = dev.sessionVersion + 1L))
        }
        logSecurityEvent(
            eventType = "LOGOUT_ALL_DEVICES",
            role = actor.role.name,
            actorName = actor.displayName,
            phoneE164 = "",
            deviceLabel = "All Devices (${activeDevices.size})",
            details = "Admin terminated all active device sessions (${activeDevices.size}); mobile OTP + PIN re-registration required.",
            isAlert = true
        )
        return OperationResult.Success("Signed out all ${activeDevices.size} registered device(s).")
    }

    suspend fun logSecurityEvent(
        eventType: String,
        role: String,
        actorName: String,
        phoneE164: String = "",
        deviceLabel: String = "",
        details: String = "",
        isAlert: Boolean = false
    ) {
        val entity = SecurityAuditLogEntity(
            eventType = eventType.take(60),
            role = role.take(30),
            actorName = actorName.take(100),
            phoneE164 = phoneE164.take(30),
            deviceLabel = deviceLabel.take(120),
            details = details.take(300),
            isAlert = isAlert,
            timestampMs = System.currentTimeMillis()
        )
        securityAuditLogDao.insert(entity)
        if (role != UserRole.OWNER_VIEW.name) {
            cloudSync?.pushSecurityAuditLog(entity)
        }
    }

    companion object {
        const val MAX_PIN_ATTEMPTS = 5
        const val PIN_LOCKOUT_MS = 15 * 60 * 1000L // 15 minutes

        fun todayIsoDate(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return sdf.format(Date())
        }

        fun currentYearMonth(): String {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
            return sdf.format(Date())
        }
    }
}

