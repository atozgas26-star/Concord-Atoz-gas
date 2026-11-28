package com.example.export

import androidx.room.withTransaction
import com.example.auth.AdminCredentialCrypto
import com.example.data.local.AuthorizedMobileEntity
import com.example.data.local.ConcordDatabase
import com.example.data.local.DifferentGasReturnEntity
import com.example.data.local.OpeningStockEntity
import com.example.data.local.ProductCategoryEntity
import com.example.data.local.ProductEntity
import com.example.data.local.StockTransactionEntity
import com.example.data.local.UserAccountEntity
import com.example.data.local.VehicleEntity
import com.example.data.model.OperationResult
import com.example.data.model.ProductCategory
import com.example.data.model.StaffFeaturePermission
import com.example.data.model.TransactionType
import com.example.data.model.UserRole
import org.json.JSONArray
import org.json.JSONObject

class BackupManager(private val db: ConcordDatabase) {

    suspend fun exportBackupJson(): String {
        val users = db.userDao().getAllUsersOnce()
        val vehicles = db.vehicleDao().getAllVehiclesOnce()
        val categories = db.productCategoryDao().getAllOnce()
        val products = db.productDao().getAllProductsOnce()
        val openings = db.openingStockDao().getAllOpeningsOnce()
        val transactions = db.transactionDao().getAllTransactionsOnce()
        val differentGasReturns = db.differentGasReturnDao().getAllOnce()
        val authorizedMobiles = db.authorizedMobileDao().getAllOnce()

        val root = JSONObject().apply {
            put("app", "Concord & A to Z Gas")
            put("schemaVersion", 5)
            put("exportedAt", System.currentTimeMillis())

            put("users", JSONArray().apply {
                users.forEach { u ->
                    val safeHash = u.pinHash.ifBlank {
                        if (u.pin.isNotBlank()) AdminCredentialCrypto.hashPassword(u.pin) else ""
                    }
                    put(JSONObject().apply {
                        put("id", u.id)
                        put("username", u.username)
                        put("displayName", u.displayName)
                        put("role", u.role.name)
                        put("pinHash", safeHash)
                        put("isActive", u.isActive)
                    })
                }
            })

            put("vehicles", JSONArray().apply {
                vehicles.forEach { v ->
                    put(JSONObject().apply {
                        put("id", v.id)
                        put("name", v.name)
                        put("staffUserId", v.staffUserId)
                        put("isActive", v.isActive)
                    })
                }
            })

            put("productCategories", JSONArray().apply {
                categories.forEach { c ->
                    put(JSONObject().apply {
                        put("id", c.id)
                        put("code", c.code)
                        put("name", c.name)
                        put("description", c.description)
                        put("iconKey", c.iconKey)
                        put("categoryType", c.categoryType)
                        put("legacyEnum", c.legacyEnum.name)
                        put("sortOrder", c.sortOrder)
                        put("isActive", c.isActive)
                        put("isSystemDefault", c.isSystemDefault)
                        put("createdAtMs", c.createdAtMs)
                        put("updatedAtMs", c.updatedAtMs)
                    })
                }
            })

            put("products", JSONArray().apply {
                products.forEach { p ->
                    put(JSONObject().apply {
                        put("id", p.id)
                        put("category", p.category.name)
                        put("categoryId", p.categoryId)
                        put("name", p.name)
                        put("sortOrder", p.sortOrder)
                        put("isActive", p.isActive)
                        put("cylinderSizeKg", p.cylinderSizeKg)
                        put("unit", p.unit)
                        put("description", p.description)
                        put("iconKey", p.iconKey)
                    })
                }
            })

            put("openings", JSONArray().apply {
                openings.forEach { o ->
                    put(JSONObject().apply {
                        put("id", o.id)
                        put("scope", o.scope)
                        put("vehicleId", o.vehicleId)
                        put("productId", o.productId)
                        put("openingFull", o.openingFull)
                        put("openingEmpty", o.openingEmpty)
                        put("updatedAt", o.updatedAt)
                        put("updatedBy", o.updatedBy)
                    })
                }
            })

            put("transactions", JSONArray().apply {
                transactions.forEach { t ->
                    put(JSONObject().apply {
                        put("id", t.id)
                        put("businessDate", t.businessDate)
                        put("timestamp", t.timestamp)
                        put("type", t.type.name)
                        put("productId", t.productId)
                        put("vehicleId", t.vehicleId ?: JSONObject.NULL)
                        put("vehicleName", t.vehicleName ?: JSONObject.NULL)
                        put("staffId", t.staffId)
                        put("staffName", t.staffName)
                        put("quantity", t.quantity)
                        put("purchaseCompanyName", t.purchaseCompanyName)
                        put("customerName", t.customerName)
                        put("notes", t.notes)
                        put("remoteId", t.remoteId ?: JSONObject.NULL)
                    })
                }
            })

            put("differentGasReturns", JSONArray().apply {
                differentGasReturns.forEach { r ->
                    put(JSONObject().apply {
                        put("id", r.id)
                        put("submissionKey", r.submissionKey)
                        put("businessDate", r.businessDate)
                        put("timestamp", r.timestamp)
                        put("customerName", r.customerName)
                        put("customerPhone", r.customerPhone)
                        put("staffId", r.staffId)
                        put("staffName", r.staffName)
                        put("vehicleId", r.vehicleId ?: JSONObject.NULL)
                        put("vehicleName", r.vehicleName ?: JSONObject.NULL)
                        put("deliveredSummary", r.deliveredSummary)
                        put("returnedSummary", r.returnedSummary)
                        put("deliveredItemsJson", r.deliveredItemsJson)
                        put("returnedItemsJson", r.returnedItemsJson)
                        put("totalDeliveredQty", r.totalDeliveredQty)
                        put("totalReturnedQty", r.totalReturnedQty)
                        put("notes", r.notes)
                        put("linkedTxRemoteIds", r.linkedTxRemoteIds)
                        put("updatedAtMs", r.updatedAtMs)
                        put("returnedEmptyCompany", r.returnedEmptyCompany)
                    })
                }
            })

            put("authorizedMobiles", JSONArray().apply {
                authorizedMobiles.forEach { m ->
                    put(JSONObject().apply {
                        put("id", m.id)
                        put("phoneE164", m.phoneE164)
                        put("displayName", m.displayName)
                        put("role", m.role.name)
                        put("staffUserId", m.staffUserId ?: JSONObject.NULL)
                        put("allowedFeaturesCsv", m.allowedFeaturesCsv)
                        put("isActive", m.isActive)
                        put("isProtectedOwner", m.isProtectedOwner)
                        put("createdAtMs", m.createdAtMs)
                        put("updatedAtMs", m.updatedAtMs)
                        put("firstTimeRegEnabled", m.firstTimeRegEnabled)
                        put("isRegistered", m.isRegistered)
                        put("otpVerifiedAtMs", m.otpVerifiedAtMs)
                        put("registeredDeviceId", m.registeredDeviceId)
                    })
                }
            })
        }
        return root.toString(2)
    }

    suspend fun restoreBackupJson(jsonText: String): OperationResult {
        return try {
            val root = JSONObject(jsonText)
            if (root.optString("app") != "Concord & A to Z Gas") {
                return OperationResult.ValidationError("Invalid backup file: not a Concord & A to Z Gas backup.")
            }

            val usersArr = root.getJSONArray("users")
            val vehiclesArr = root.getJSONArray("vehicles")
            val productsArr = root.getJSONArray("products")
            val openingsArr = root.getJSONArray("openings")
            val txArr = root.getJSONArray("transactions")

            val users = (0 until usersArr.length()).map { i ->
                val o = usersArr.getJSONObject(i)
                val rawPin = o.optString("pin", "")
                val existingHash = o.optString("pinHash", "")
                val resolvedHash = existingHash.ifBlank {
                    if (rawPin.isNotBlank()) AdminCredentialCrypto.hashPassword(rawPin) else ""
                }
                UserAccountEntity(
                    id = o.getLong("id"),
                    username = o.getString("username"),
                    displayName = o.getString("displayName"),
                    role = UserRole.valueOf(o.getString("role")),
                    pin = "",
                    isActive = o.optBoolean("isActive", true),
                    pinHash = resolvedHash
                )
            }

            if (users.none { it.role == UserRole.ADMIN }) {
                return OperationResult.ValidationError("Backup is missing an Admin account.")
            }

            val vehicles = (0 until vehiclesArr.length()).map { i ->
                val o = vehiclesArr.getJSONObject(i)
                VehicleEntity(
                    id = o.getLong("id"),
                    name = o.getString("name"),
                    staffUserId = o.getLong("staffUserId"),
                    isActive = o.optBoolean("isActive", true)
                )
            }

            val categoriesArr = root.optJSONArray("productCategories")
            val categories = if (categoriesArr != null) {
                (0 until categoriesArr.length()).map { i ->
                    val o = categoriesArr.getJSONObject(i)
                    ProductCategoryEntity(
                        id = o.getLong("id"),
                        code = o.getString("code"),
                        name = o.getString("name"),
                        description = o.optString("description", ""),
                        iconKey = o.optString("iconKey", "INDUSTRIAL"),
                        categoryType = o.optString("categoryType", "INDUSTRIAL"),
                        legacyEnum = runCatching { ProductCategory.valueOf(o.optString("legacyEnum", "INDUSTRIAL_GAS")) }
                            .getOrDefault(ProductCategory.INDUSTRIAL_GAS),
                        sortOrder = o.optInt("sortOrder", 100),
                        isActive = o.optBoolean("isActive", true),
                        isSystemDefault = o.optBoolean("isSystemDefault", false),
                        createdAtMs = o.optLong("createdAtMs", System.currentTimeMillis()),
                        updatedAtMs = o.optLong("updatedAtMs", System.currentTimeMillis())
                    )
                }
            } else {
                null
            }

            val products = (0 until productsArr.length()).map { i ->
                val o = productsArr.getJSONObject(i)
                val cat = ProductCategory.valueOf(o.getString("category"))
                ProductEntity(
                    id = o.getLong("id"),
                    category = cat,
                    name = o.getString("name"),
                    sortOrder = o.optInt("sortOrder", 0),
                    isActive = o.optBoolean("isActive", true),
                    cylinderSizeKg = o.optString("cylinderSizeKg", ""),
                    categoryId = o.optLong("categoryId", cat.defaultCategoryId),
                    unit = o.optString("unit", "Cylinder"),
                    description = o.optString("description", ""),
                    iconKey = o.optString("iconKey", "CYLINDER")
                )
            }

            val openings = (0 until openingsArr.length()).map { i ->
                val o = openingsArr.getJSONObject(i)
                OpeningStockEntity(
                    id = o.getLong("id"),
                    scope = o.getString("scope"),
                    vehicleId = o.getLong("vehicleId"),
                    productId = o.getLong("productId"),
                    openingFull = o.getInt("openingFull"),
                    openingEmpty = o.getInt("openingEmpty"),
                    updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
                    updatedBy = o.optString("updatedBy", "Restore")
                )
            }

            val transactions = (0 until txArr.length()).map { i ->
                val o = txArr.getJSONObject(i)
                StockTransactionEntity(
                    id = o.getLong("id"),
                    businessDate = o.getString("businessDate"),
                    timestamp = o.getLong("timestamp"),
                    type = TransactionType.valueOf(o.getString("type")),
                    productId = o.getLong("productId"),
                    vehicleId = if (o.isNull("vehicleId")) null else o.getLong("vehicleId"),
                    vehicleName = if (o.isNull("vehicleName")) null else o.getString("vehicleName"),
                    staffId = o.getLong("staffId"),
                    staffName = o.getString("staffName"),
                    quantity = o.getInt("quantity"),
                    purchaseCompanyName = o.optString("purchaseCompanyName", ""),
                    customerName = o.optString("customerName", ""),
                    notes = o.optString("notes", ""),
                    remoteId = if (!o.has("remoteId") || o.isNull("remoteId")) null else o.optString("remoteId").takeIf { it.isNotBlank() }
                )
            }

            val diffReturnsArr = root.optJSONArray("differentGasReturns")
            val diffReturns = if (diffReturnsArr != null) {
                (0 until diffReturnsArr.length()).map { i ->
                    val o = diffReturnsArr.getJSONObject(i)
                    DifferentGasReturnEntity(
                        id = o.optLong("id", 0L),
                        submissionKey = o.getString("submissionKey"),
                        businessDate = o.getString("businessDate"),
                        timestamp = o.optLong("timestamp", System.currentTimeMillis()),
                        customerName = o.getString("customerName"),
                        customerPhone = o.optString("customerPhone", ""),
                        staffId = o.getLong("staffId"),
                        staffName = o.getString("staffName"),
                        vehicleId = if (o.isNull("vehicleId")) null else o.getLong("vehicleId"),
                        vehicleName = if (o.isNull("vehicleName")) null else o.getString("vehicleName"),
                        deliveredSummary = o.optString("deliveredSummary", ""),
                        returnedSummary = o.optString("returnedSummary", ""),
                        deliveredItemsJson = o.optString("deliveredItemsJson", "[]"),
                        returnedItemsJson = o.optString("returnedItemsJson", "[]"),
                        totalDeliveredQty = o.optInt("totalDeliveredQty", 0),
                        totalReturnedQty = o.optInt("totalReturnedQty", 0),
                        notes = o.optString("notes", ""),
                        linkedTxRemoteIds = o.optString("linkedTxRemoteIds", ""),
                        updatedAtMs = o.optLong("updatedAtMs", 0L),
                        returnedEmptyCompany = o.optString("returnedEmptyCompany", "")
                    )
                }
            } else {
                emptyList()
            }

            val mobilesArr = root.optJSONArray("authorizedMobiles")
            val authorizedMobiles = if (mobilesArr != null) {
                (0 until mobilesArr.length()).map { i ->
                    val o = mobilesArr.getJSONObject(i)
                    AuthorizedMobileEntity(
                        id = o.optLong("id", 0L),
                        phoneE164 = o.getString("phoneE164"),
                        displayName = o.getString("displayName"),
                        role = UserRole.valueOf(o.getString("role")),
                        staffUserId = if (!o.has("staffUserId") || o.isNull("staffUserId")) null else o.getLong("staffUserId"),
                        allowedFeaturesCsv = o.optString("allowedFeaturesCsv", StaffFeaturePermission.ALL_DEFAULT_CSV),
                        isActive = o.optBoolean("isActive", true),
                        isProtectedOwner = o.optBoolean("isProtectedOwner", false),
                        createdAtMs = o.optLong("createdAtMs", System.currentTimeMillis()),
                        updatedAtMs = o.optLong("updatedAtMs", System.currentTimeMillis()),
                        firstTimeRegEnabled = o.optBoolean("firstTimeRegEnabled", true),
                        isRegistered = o.optBoolean("isRegistered", false),
                        otpVerifiedAtMs = o.optLong("otpVerifiedAtMs", 0L),
                        registeredDeviceId = o.optString("registeredDeviceId", "")
                    )
                }
            } else {
                null
            }

            db.withTransaction {
                db.differentGasReturnDao().deleteAll()
                db.transactionDao().deleteAll()
                db.openingStockDao().deleteAll()
                db.productDao().deleteAll()
                if (categories != null) {
                    db.productCategoryDao().deleteAll()
                }
                db.vehicleDao().deleteAll()
                db.userDao().deleteAll()

                db.userDao().insertAll(users)
                db.vehicleDao().insertAll(vehicles)
                if (categories != null && categories.isNotEmpty()) {
                    db.productCategoryDao().insertAll(categories)
                }
                db.productDao().insertAll(products)
                db.openingStockDao().insertAll(openings)
                db.transactionDao().insertAll(transactions)
                if (diffReturns.isNotEmpty()) {
                    db.differentGasReturnDao().insertAll(diffReturns)
                }
                if (authorizedMobiles != null) {
                    db.authorizedMobileDao().deleteAll()
                    if (authorizedMobiles.isNotEmpty()) {
                        db.authorizedMobileDao().insertAll(authorizedMobiles)
                    }
                }
            }

            OperationResult.Success(
                "Restored ${products.size} products, ${vehicles.size} vehicles, and ${transactions.size} transactions."
            )
        } catch (e: Exception) {
            OperationResult.ValidationError("Restore failed: ${e.message ?: "Corrupted JSON file"}")
        }
    }
}

