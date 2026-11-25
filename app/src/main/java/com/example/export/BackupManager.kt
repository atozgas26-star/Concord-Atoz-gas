package com.example.export

import androidx.room.withTransaction
import com.example.data.local.ConcordDatabase
import com.example.data.local.OpeningStockEntity
import com.example.data.local.ProductEntity
import com.example.data.local.StockTransactionEntity
import com.example.data.local.UserAccountEntity
import com.example.data.local.VehicleEntity
import com.example.data.model.OperationResult
import com.example.data.model.ProductCategory
import com.example.data.model.TransactionType
import com.example.data.model.UserRole
import org.json.JSONArray
import org.json.JSONObject

class BackupManager(private val db: ConcordDatabase) {

    suspend fun exportBackupJson(): String {
        val users = db.userDao().getAllUsersOnce()
        val vehicles = db.vehicleDao().getAllVehiclesOnce()
        val products = db.productDao().getAllProductsOnce()
        val openings = db.openingStockDao().getAllOpeningsOnce()
        val transactions = db.transactionDao().getAllTransactionsOnce()

        val root = JSONObject().apply {
            put("app", "Concord & A to Z Gas")
            put("schemaVersion", 1)
            put("exportedAt", System.currentTimeMillis())

            put("users", JSONArray().apply {
                users.forEach { u ->
                    put(JSONObject().apply {
                        put("id", u.id)
                        put("username", u.username)
                        put("displayName", u.displayName)
                        put("role", u.role.name)
                        put("pin", u.pin)
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

            put("products", JSONArray().apply {
                products.forEach { p ->
                    put(JSONObject().apply {
                        put("id", p.id)
                        put("category", p.category.name)
                        put("name", p.name)
                        put("sortOrder", p.sortOrder)
                        put("isActive", p.isActive)
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
                UserAccountEntity(
                    id = o.getLong("id"),
                    username = o.getString("username"),
                    displayName = o.getString("displayName"),
                    role = UserRole.valueOf(o.getString("role")),
                    pin = o.getString("pin"),
                    isActive = o.optBoolean("isActive", true)
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

            val products = (0 until productsArr.length()).map { i ->
                val o = productsArr.getJSONObject(i)
                ProductEntity(
                    id = o.getLong("id"),
                    category = ProductCategory.valueOf(o.getString("category")),
                    name = o.getString("name"),
                    sortOrder = o.optInt("sortOrder", 0),
                    isActive = o.optBoolean("isActive", true)
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
                    notes = o.optString("notes", "")
                )
            }

            db.withTransaction {
                db.transactionDao().deleteAll()
                db.openingStockDao().deleteAll()
                db.productDao().deleteAll()
                db.vehicleDao().deleteAll()
                db.userDao().deleteAll()

                db.userDao().insertAll(users)
                db.vehicleDao().insertAll(vehicles)
                db.productDao().insertAll(products)
                db.openingStockDao().insertAll(openings)
                db.transactionDao().insertAll(transactions)
            }

            OperationResult.Success(
                "Restored ${products.size} products, ${vehicles.size} vehicles, and ${transactions.size} transactions."
            )
        } catch (e: Exception) {
            OperationResult.ValidationError("Restore failed: ${e.message ?: "Corrupted JSON file"}")
        }
    }
}
