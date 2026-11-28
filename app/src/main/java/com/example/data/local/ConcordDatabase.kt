package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.withTransaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.auth.AdminCredentialCrypto
import com.example.data.model.ProductCategory
import com.example.data.model.UserRole

@Database(
    entities = [
        UserAccountEntity::class,
        VehicleEntity::class,
        ProductCategoryEntity::class,
        ProductEntity::class,
        OpeningStockEntity::class,
        StockTransactionEntity::class,
        StaffAttendanceEntity::class,
        PendingStoreTransferEntity::class,
        AppNotificationEntity::class,
        ChatMessageEntity::class,
        DifferentGasReturnEntity::class,
        AuthorizedMobileEntity::class,
        RegisteredDeviceEntity::class,
        SecurityAuditLogEntity::class
    ],
    version = 9,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ConcordDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun vehicleDao(): VehicleDao
    abstract fun productCategoryDao(): ProductCategoryDao
    abstract fun productDao(): ProductDao
    abstract fun openingStockDao(): OpeningStockDao
    abstract fun transactionDao(): TransactionDao
    abstract fun staffAttendanceDao(): StaffAttendanceDao
    abstract fun pendingStoreTransferDao(): PendingStoreTransferDao
    abstract fun appNotificationDao(): AppNotificationDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun differentGasReturnDao(): DifferentGasReturnDao
    abstract fun authorizedMobileDao(): AuthorizedMobileDao
    abstract fun registeredDeviceDao(): RegisteredDeviceDao
    abstract fun securityAuditLogDao(): SecurityAuditLogDao

    suspend fun ensureSeeded() {
        val existingUsers = userDao().getAllUsersOnce()
        val existingCategories = productCategoryDao().getAllOnce()
        val existingProducts = productDao().getAllProductsOnce()

        withTransaction {
            // 1. Seed initial users with salted PBKDF2 PIN hashes (never storing plain-text PINs)
            if (existingUsers.isEmpty()) {
                val adminId = userDao().insert(
                    UserAccountEntity(
                        username = "admin_ragavan",
                        displayName = "Ragavan",
                        role = UserRole.ADMIN,
                        pin = "",
                        pinHash = AdminCredentialCrypto.hashPassword("1725")
                    )
                )
                check(adminId > 0)

                val staffSpecs = listOf(
                    Triple("nadeem", "Nadeem", "1001") to "NADEEM",
                    Triple("fazalamin", "Fazalamin", "1002") to "FAZALAMIN",
                    Triple("fawad", "Fawad", "1003") to "FAWAD",
                    Triple("ragavan", "Ragavan", "1004") to "RAGAVAN",
                    Triple("sivan", "Sivan", "1005") to "SIVAN",
                    Triple("jinu", "Jinu", "1006") to "JINU",
                    Triple("sajan", "SAJAN", "1007") to "SAJAN"
                )

                val vehicles = mutableListOf<VehicleEntity>()
                for ((staffTriple, vehicleName) in staffSpecs) {
                    val (username, displayName, rawPin) = staffTriple
                    val userId = userDao().insert(
                        UserAccountEntity(
                            username = username,
                            displayName = displayName,
                            role = UserRole.STAFF,
                            pin = "",
                            pinHash = AdminCredentialCrypto.hashPassword(rawPin)
                        )
                    )
                    vehicles.add(
                        VehicleEntity(
                            name = vehicleName,
                            staffUserId = userId,
                            isActive = true
                        )
                    )
                }
                vehicleDao().insertAll(vehicles)
            } else {
                // Transparently upgrade any legacy plain-text PINs into salted PBKDF2 hashes while preserving exact PINs
                existingUsers.forEach { user ->
                    if (user.pinHash.isBlank() && user.pin.isNotBlank()) {
                        val saltedHash = AdminCredentialCrypto.hashPassword(user.pin)
                        userDao().updatePinHash(user.id, saltedHash)
                    }
                }
            }

            // 2. Ensure the 4 permanent system Product Master category folders exist
            val defaultCategories = listOf(
                ProductCategoryEntity(
                    id = 1L,
                    code = "INDUSTRIAL_GAS",
                    name = "INDUSTRIAL GAS – REGULAR",
                    description = "Regular Industrial Cylinders (Oxygen, Acetylene, Argon, Argon Mix, CO2 20/30 KG, Nitrogen)",
                    iconKey = "INDUSTRIAL",
                    categoryType = "INDUSTRIAL",
                    legacyEnum = ProductCategory.INDUSTRIAL_GAS,
                    sortOrder = 1,
                    isActive = true,
                    isSystemDefault = true
                ),
                ProductCategoryEntity(
                    id = 2L,
                    code = "CUTTING_GAS",
                    name = "CUTTING GAS",
                    description = "Cutting Gas Cylinders (MCG 22 KG & MCG 44 KG)",
                    iconKey = "CUTTING",
                    categoryType = "CUTTING",
                    legacyEnum = ProductCategory.MCG_CUTTING_GAS,
                    sortOrder = 2,
                    isActive = true,
                    isSystemDefault = true
                ),
                ProductCategoryEntity(
                    id = 3L,
                    code = "COOKING_GAS",
                    name = "LPG GAS",
                    description = "LPG Cylinders (11 KG, 22 KG, 44 KG) — Arab Gas & Emirates Gas tracked as empty return companies",
                    iconKey = "LPG",
                    categoryType = "LPG",
                    legacyEnum = ProductCategory.COOKING_GAS,
                    sortOrder = 3,
                    isActive = true,
                    isSystemDefault = true
                ),
                ProductCategoryEntity(
                    id = 4L,
                    code = "SMALL_48_CFT",
                    name = "INDUSTRIAL GAS – 48 CFT SMALL",
                    description = "Independent 48 CFT Small Industrial Cylinders & CO2 5 KG",
                    iconKey = "SMALL_48",
                    categoryType = "SMALL_48",
                    legacyEnum = ProductCategory.SMALL_48_CFT,
                    sortOrder = 4,
                    isActive = true,
                    isSystemDefault = true
                )
            )
            if (existingCategories.isEmpty()) {
                productCategoryDao().insertAll(defaultCategories)
            } else {
                val existingCodes = existingCategories.map { it.code }.toSet()
                defaultCategories.filter { it.code !in existingCodes }.forEach { cat ->
                    runCatching { productCategoryDao().insert(cat) }
                }
            }

            // 3. Ensure initial 4-folder Product Master catalog (preserving all existing IDs & stock balances)
            if (existingProducts.isEmpty()) {
                val seededProducts = listOf(
                    // 1. INDUSTRIAL GAS – REGULAR (7 active products)
                    ProductEntity(category = ProductCategory.INDUSTRIAL_GAS, categoryId = 1L, name = "Oxygen", sortOrder = 101, cylinderSizeKg = "47L", unit = "Cylinder", description = "Regular Industrial Oxygen"),
                    ProductEntity(category = ProductCategory.INDUSTRIAL_GAS, categoryId = 1L, name = "Acetylene", sortOrder = 102, cylinderSizeKg = "40L", unit = "Cylinder", description = "Regular Industrial Acetylene"),
                    ProductEntity(category = ProductCategory.INDUSTRIAL_GAS, categoryId = 1L, name = "Argon", sortOrder = 103, cylinderSizeKg = "47L", unit = "Cylinder", description = "Regular Industrial Argon"),
                    ProductEntity(category = ProductCategory.INDUSTRIAL_GAS, categoryId = 1L, name = "Argon Mix", sortOrder = 104, cylinderSizeKg = "47L", unit = "Cylinder", description = "Regular Industrial Argon Mix"),
                    ProductEntity(category = ProductCategory.INDUSTRIAL_GAS, categoryId = 1L, name = "CO2 20 KG", sortOrder = 105, cylinderSizeKg = "20 KG", unit = "KG", description = "Regular Industrial CO2 20 KG"),
                    ProductEntity(category = ProductCategory.INDUSTRIAL_GAS, categoryId = 1L, name = "CO2 30 KG", sortOrder = 106, cylinderSizeKg = "30 KG", unit = "KG", description = "Regular Industrial CO2 30 KG"),
                    ProductEntity(category = ProductCategory.INDUSTRIAL_GAS, categoryId = 1L, name = "Nitrogen", sortOrder = 107, cylinderSizeKg = "47L", unit = "Cylinder", description = "Regular Industrial Nitrogen"),

                    // 2. CUTTING GAS (MCG 22 KG & MCG 44 KG)
                    ProductEntity(category = ProductCategory.MCG_CUTTING_GAS, categoryId = 2L, name = "MCG 22 KG", sortOrder = 201, cylinderSizeKg = "22 KG", unit = "KG", description = "Cutting Gas MCG 22 KG", iconKey = "CUTTING"),
                    ProductEntity(category = ProductCategory.MCG_CUTTING_GAS, categoryId = 2L, name = "MCG 44 KG", sortOrder = 202, cylinderSizeKg = "44 KG", unit = "KG", description = "Cutting Gas MCG 44 KG", iconKey = "CUTTING"),

                    // 3. LPG GAS — strictly 11 KG, 22 KG, 44 KG (Arab Gas & Emirates Gas are empty-return company identifiers only)
                    ProductEntity(category = ProductCategory.COOKING_GAS, categoryId = 3L, name = "11 KG", sortOrder = 301, cylinderSizeKg = "11 KG", unit = "KG", description = "LPG Cooking Cylinder 11 KG", iconKey = "LPG"),
                    ProductEntity(category = ProductCategory.COOKING_GAS, categoryId = 3L, name = "22 KG", sortOrder = 302, cylinderSizeKg = "22 KG", unit = "KG", description = "LPG Cooking Cylinder 22 KG", iconKey = "LPG"),
                    ProductEntity(category = ProductCategory.COOKING_GAS, categoryId = 3L, name = "44 KG", sortOrder = 303, cylinderSizeKg = "44 KG", unit = "KG", description = "LPG Cooking Cylinder 44 KG", iconKey = "LPG"),

                    // 4. INDUSTRIAL GAS – 48 CFT SMALL (6 independent small products)
                    ProductEntity(category = ProductCategory.SMALL_48_CFT, categoryId = 4L, name = "Oxygen 48 CFT", sortOrder = 401, cylinderSizeKg = "48 CFT", unit = "CFT", description = "Small 48 CFT Oxygen", iconKey = "SMALL_48"),
                    ProductEntity(category = ProductCategory.SMALL_48_CFT, categoryId = 4L, name = "Acetylene 48 CFT", sortOrder = 402, cylinderSizeKg = "48 CFT", unit = "CFT", description = "Small 48 CFT Acetylene", iconKey = "SMALL_48"),
                    ProductEntity(category = ProductCategory.SMALL_48_CFT, categoryId = 4L, name = "Argon 48 CFT", sortOrder = 403, cylinderSizeKg = "48 CFT", unit = "CFT", description = "Small 48 CFT Argon", iconKey = "SMALL_48"),
                    ProductEntity(category = ProductCategory.SMALL_48_CFT, categoryId = 4L, name = "Argon Mix 48 CFT", sortOrder = 404, cylinderSizeKg = "48 CFT", unit = "CFT", description = "Small 48 CFT Argon Mix", iconKey = "SMALL_48"),
                    ProductEntity(category = ProductCategory.SMALL_48_CFT, categoryId = 4L, name = "CO2 5 KG", sortOrder = 405, cylinderSizeKg = "5 KG", unit = "KG", description = "Small CO2 5 KG Cylinder", iconKey = "SMALL_48"),
                    ProductEntity(category = ProductCategory.SMALL_48_CFT, categoryId = 4L, name = "Nitrogen 48 CFT", sortOrder = 406, cylinderSizeKg = "48 CFT", unit = "CFT", description = "Small 48 CFT Nitrogen", iconKey = "SMALL_48")
                )
                productDao().insertAll(seededProducts)
            } else {
                // Preserve existing IDs while ensuring Cutting Gas (MCG 22 KG / MCG 44 KG),
                // LPG (11/22/44 KG), and all six 48 CFT Small products exist and are active,
                // and standalone Arab/Emirates LPG product rows remain inactive.
                val existingNormalized = existingProducts.map {
                    it.category to it.name.replace(" ", "").lowercase()
                }.toSet()
                val requiredCoreProducts = listOf(
                    ProductEntity(category = ProductCategory.MCG_CUTTING_GAS, categoryId = 2L, name = "MCG 22 KG", sortOrder = 201, cylinderSizeKg = "22 KG", unit = "KG", iconKey = "CUTTING"),
                    ProductEntity(category = ProductCategory.MCG_CUTTING_GAS, categoryId = 2L, name = "MCG 44 KG", sortOrder = 202, cylinderSizeKg = "44 KG", unit = "KG", iconKey = "CUTTING"),
                    ProductEntity(category = ProductCategory.COOKING_GAS, categoryId = 3L, name = "11 KG", sortOrder = 301, cylinderSizeKg = "11 KG", unit = "KG", iconKey = "LPG"),
                    ProductEntity(category = ProductCategory.COOKING_GAS, categoryId = 3L, name = "22 KG", sortOrder = 302, cylinderSizeKg = "22 KG", unit = "KG", iconKey = "LPG"),
                    ProductEntity(category = ProductCategory.COOKING_GAS, categoryId = 3L, name = "44 KG", sortOrder = 303, cylinderSizeKg = "44 KG", unit = "KG", iconKey = "LPG"),
                    ProductEntity(category = ProductCategory.SMALL_48_CFT, categoryId = 4L, name = "Oxygen 48 CFT", sortOrder = 401, cylinderSizeKg = "48 CFT", unit = "CFT", iconKey = "SMALL_48"),
                    ProductEntity(category = ProductCategory.SMALL_48_CFT, categoryId = 4L, name = "Acetylene 48 CFT", sortOrder = 402, cylinderSizeKg = "48 CFT", unit = "CFT", iconKey = "SMALL_48"),
                    ProductEntity(category = ProductCategory.SMALL_48_CFT, categoryId = 4L, name = "Argon 48 CFT", sortOrder = 403, cylinderSizeKg = "48 CFT", unit = "CFT", iconKey = "SMALL_48"),
                    ProductEntity(category = ProductCategory.SMALL_48_CFT, categoryId = 4L, name = "Argon Mix 48 CFT", sortOrder = 404, cylinderSizeKg = "48 CFT", unit = "CFT", iconKey = "SMALL_48"),
                    ProductEntity(category = ProductCategory.SMALL_48_CFT, categoryId = 4L, name = "CO2 5 KG", sortOrder = 405, cylinderSizeKg = "5 KG", unit = "KG", iconKey = "SMALL_48"),
                    ProductEntity(category = ProductCategory.SMALL_48_CFT, categoryId = 4L, name = "Nitrogen 48 CFT", sortOrder = 406, cylinderSizeKg = "48 CFT", unit = "CFT", iconKey = "SMALL_48")
                )
                requiredCoreProducts.forEach { req ->
                    val normKey = req.category to req.name.replace(" ", "").lowercase()
                    val alt48Key = req.category to req.name.replace(" 48 CFT", "", ignoreCase = true).replace(" ", "").lowercase()
                    if (normKey !in existingNormalized && alt48Key !in existingNormalized) {
                        runCatching { productDao().insert(req) }
                    }
                }
                existingProducts.forEach { prod ->
                    val isLegacyBrandLpg = prod.category == ProductCategory.COOKING_GAS && (
                        prod.name.startsWith("Arab Gas", ignoreCase = true) ||
                            prod.name.startsWith("Emirates Gas", ignoreCase = true)
                        )
                    if (isLegacyBrandLpg && prod.isActive) {
                        runCatching { productDao().update(prod.copy(isActive = false)) }
                    } else if (prod.categoryId <= 0L) {
                        runCatching { productDao().update(prod.copy(categoryId = prod.category.defaultCategoryId)) }
                    }
                }
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: ConcordDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE stock_transactions ADD COLUMN remoteId TEXT DEFAULT NULL")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_stock_transactions_remoteId ON stock_transactions(remoteId)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE stock_transactions ADD COLUMN purchaseCompanyName TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE stock_transactions ADD COLUMN customerName TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS staff_attendance (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        remoteId TEXT NOT NULL,
                        businessDate TEXT NOT NULL,
                        staffId INTEGER NOT NULL,
                        staffName TEXT NOT NULL,
                        vehicleName TEXT NOT NULL,
                        dutyStartMs INTEGER NOT NULL,
                        lunchOutMs INTEGER NOT NULL,
                        lunchInMs INTEGER NOT NULL,
                        dutyCloseMs INTEGER NOT NULL,
                        updatedAtMs INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_staff_attendance_businessDate_staffId ON staff_attendance(businessDate, staffId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_staff_attendance_remoteId ON staff_attendance(remoteId)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS pending_store_transfers (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        remoteId TEXT NOT NULL,
                        businessDate TEXT NOT NULL,
                        direction TEXT NOT NULL,
                        vehicleId INTEGER NOT NULL,
                        vehicleName TEXT NOT NULL,
                        productId INTEGER NOT NULL,
                        productName TEXT NOT NULL,
                        productCategory TEXT NOT NULL,
                        quantity INTEGER NOT NULL,
                        staffId INTEGER NOT NULL,
                        staffName TEXT NOT NULL,
                        notes TEXT NOT NULL,
                        status TEXT NOT NULL,
                        requestedAtMs INTEGER NOT NULL,
                        reviewedAtMs INTEGER NOT NULL,
                        reviewedBy TEXT NOT NULL,
                        reviewNotes TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_pending_store_transfers_remoteId ON pending_store_transfers(remoteId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_pending_store_transfers_status ON pending_store_transfers(status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_pending_store_transfers_businessDate ON pending_store_transfers(businessDate)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS app_notifications (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        remoteId TEXT NOT NULL,
                        targetScope TEXT NOT NULL,
                        targetStaffId INTEGER NOT NULL,
                        category TEXT NOT NULL,
                        title TEXT NOT NULL,
                        message TEXT NOT NULL,
                        isUrgent INTEGER NOT NULL,
                        isRead INTEGER NOT NULL,
                        createdAtMs INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_app_notifications_remoteId ON app_notifications(remoteId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_app_notifications_createdAtMs ON app_notifications(createdAtMs)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_app_notifications_targetScope ON app_notifications(targetScope)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS chat_messages (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        remoteId TEXT NOT NULL,
                        channel TEXT NOT NULL,
                        senderId INTEGER NOT NULL,
                        senderName TEXT NOT NULL,
                        senderRole TEXT NOT NULL,
                        message TEXT NOT NULL,
                        isImportant INTEGER NOT NULL,
                        createdAtMs INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_chat_messages_remoteId ON chat_messages(remoteId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_messages_channel ON chat_messages(channel)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_messages_createdAtMs ON chat_messages(createdAtMs)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN cylinderSizeKg TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS different_gas_returns (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        submissionKey TEXT NOT NULL,
                        businessDate TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        customerName TEXT NOT NULL,
                        customerPhone TEXT NOT NULL,
                        staffId INTEGER NOT NULL,
                        staffName TEXT NOT NULL,
                        vehicleId INTEGER,
                        vehicleName TEXT,
                        deliveredSummary TEXT NOT NULL,
                        returnedSummary TEXT NOT NULL,
                        deliveredItemsJson TEXT NOT NULL,
                        returnedItemsJson TEXT NOT NULL,
                        totalDeliveredQty INTEGER NOT NULL,
                        totalReturnedQty INTEGER NOT NULL,
                        notes TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_different_gas_returns_submissionKey ON different_gas_returns(submissionKey)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_different_gas_returns_businessDate ON different_gas_returns(businessDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_different_gas_returns_staffId ON different_gas_returns(staffId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_different_gas_returns_customerName ON different_gas_returns(customerName)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Extend different_gas_returns for atomic edit/delete stock reversal
                db.execSQL("ALTER TABLE different_gas_returns ADD COLUMN linkedTxRemoteIds TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE different_gas_returns ADD COLUMN updatedAtMs INTEGER NOT NULL DEFAULT 0")

                // 2. Authorized UAE (+971) mobile allowlist & staff permissions
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS authorized_mobiles (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        phoneE164 TEXT NOT NULL,
                        displayName TEXT NOT NULL,
                        role TEXT NOT NULL,
                        staffUserId INTEGER,
                        allowedFeaturesCsv TEXT NOT NULL,
                        isActive INTEGER NOT NULL,
                        isProtectedOwner INTEGER NOT NULL,
                        createdAtMs INTEGER NOT NULL,
                        updatedAtMs INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_authorized_mobiles_phoneE164 ON authorized_mobiles(phoneE164)")

                // 3. Registered devices & remote session revocation
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS registered_devices (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        deviceId TEXT NOT NULL,
                        deviceLabel TEXT NOT NULL,
                        phoneE164 TEXT NOT NULL,
                        userId INTEGER NOT NULL,
                        displayName TEXT NOT NULL,
                        role TEXT NOT NULL,
                        sessionVersion INTEGER NOT NULL,
                        isRevoked INTEGER NOT NULL,
                        lastActiveMs INTEGER NOT NULL,
                        registeredAtMs INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_registered_devices_deviceId ON registered_devices(deviceId)")

                // 4. Security & login activity audit trail
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS security_audit_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        eventType TEXT NOT NULL,
                        role TEXT NOT NULL,
                        actorName TEXT NOT NULL,
                        phoneE164 TEXT NOT NULL,
                        deviceLabel TEXT NOT NULL,
                        details TEXT NOT NULL,
                        isAlert INTEGER NOT NULL,
                        timestampMs INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_security_audit_logs_timestampMs ON security_audit_logs(timestampMs)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_security_audit_logs_eventType ON security_audit_logs(eventType)")

                // 5. Sanitize any accidental legacy naming typos (never Arabic Gas or Emergency Gas)
                db.execSQL("UPDATE products SET name = REPLACE(name, 'Arabic Gas', 'Arab Gas') WHERE name LIKE '%Arabic Gas%'")
                db.execSQL("UPDATE products SET name = REPLACE(name, 'Emergency Gas', 'Emirates Gas') WHERE name LIKE '%Emergency Gas%'")
                db.execSQL("UPDATE stock_transactions SET purchaseCompanyName = REPLACE(purchaseCompanyName, 'Arabic Gas', 'Arab Gas') WHERE purchaseCompanyName LIKE '%Arabic Gas%'")
                db.execSQL("UPDATE stock_transactions SET purchaseCompanyName = REPLACE(purchaseCompanyName, 'Emergency Gas', 'Emirates Gas') WHERE purchaseCompanyName LIKE '%Emergency Gas%'")

                // 6. Seed exact Arab Gas, Emirates Gas, MCG & Cutting Gas entries without touching existing stock
                db.execSQL("INSERT OR IGNORE INTO products (category, name, sortOrder, isActive, cylinderSizeKg) VALUES ('COOKING_GAS', 'Arab Gas 11kg', 304, 1, '11 KG')")
                db.execSQL("INSERT OR IGNORE INTO products (category, name, sortOrder, isActive, cylinderSizeKg) VALUES ('COOKING_GAS', 'Arab Gas 22kg', 305, 1, '22 KG')")
                db.execSQL("INSERT OR IGNORE INTO products (category, name, sortOrder, isActive, cylinderSizeKg) VALUES ('COOKING_GAS', 'Arab Gas 44kg', 306, 1, '44 KG')")
                db.execSQL("INSERT OR IGNORE INTO products (category, name, sortOrder, isActive, cylinderSizeKg) VALUES ('COOKING_GAS', 'Emirates Gas 11kg', 307, 1, '11 KG')")
                db.execSQL("INSERT OR IGNORE INTO products (category, name, sortOrder, isActive, cylinderSizeKg) VALUES ('COOKING_GAS', 'Emirates Gas 22kg', 308, 1, '22 KG')")
                db.execSQL("INSERT OR IGNORE INTO products (category, name, sortOrder, isActive, cylinderSizeKg) VALUES ('COOKING_GAS', 'Emirates Gas 44kg', 309, 1, '44 KG')")
                db.execSQL("INSERT OR IGNORE INTO products (category, name, sortOrder, isActive, cylinderSizeKg) VALUES ('MCG_CUTTING_GAS', 'MCG 22kg', 403, 1, '22 KG')")
                db.execSQL("INSERT OR IGNORE INTO products (category, name, sortOrder, isActive, cylinderSizeKg) VALUES ('MCG_CUTTING_GAS', 'MCG 44kg', 404, 1, '44 KG')")
                db.execSQL("INSERT OR IGNORE INTO products (category, name, sortOrder, isActive, cylinderSizeKg) VALUES ('MCG_CUTTING_GAS', 'Cutting Gas', 405, 1, 'Cutting Gas')")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Track LPG Empty Return Company (Arab Gas / Emirates Gas) explicitly on exchange records
                db.execSQL("ALTER TABLE different_gas_returns ADD COLUMN returnedEmptyCompany TEXT NOT NULL DEFAULT ''")

                // 2. Retire MCG & Cutting Gas from active product catalog while preserving historical records
                db.execSQL("UPDATE products SET isActive = 0 WHERE category = 'MCG_CUTTING_GAS'")

                // 3. Keep LPG products strictly 11kg, 22kg, 44kg; retire standalone Arab/Emirates LPG product rows
                db.execSQL("UPDATE products SET isActive = 0 WHERE category = 'COOKING_GAS' AND (name LIKE 'Arab Gas%' OR name LIKE 'Emirates Gas%')")
                db.execSQL("INSERT OR IGNORE INTO products (category, name, sortOrder, isActive, cylinderSizeKg) VALUES ('COOKING_GAS', '11kg', 301, 1, '11 KG')")
                db.execSQL("INSERT OR IGNORE INTO products (category, name, sortOrder, isActive, cylinderSizeKg) VALUES ('COOKING_GAS', '22kg', 302, 1, '22 KG')")
                db.execSQL("INSERT OR IGNORE INTO products (category, name, sortOrder, isActive, cylinderSizeKg) VALUES ('COOKING_GAS', '44kg', 303, 1, '44 KG')")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val now = System.currentTimeMillis()

                // 1. Dynamic Product Master Category Folders table with permanent IDs
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS product_categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        code TEXT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        iconKey TEXT NOT NULL DEFAULT 'INDUSTRIAL',
                        categoryType TEXT NOT NULL DEFAULT 'INDUSTRIAL',
                        legacyEnum TEXT NOT NULL DEFAULT 'INDUSTRIAL_GAS',
                        sortOrder INTEGER NOT NULL DEFAULT 100,
                        isActive INTEGER NOT NULL DEFAULT 1,
                        isSystemDefault INTEGER NOT NULL DEFAULT 0,
                        createdAtMs INTEGER NOT NULL DEFAULT 0,
                        updatedAtMs INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_product_categories_code ON product_categories(code)")

                // Seed the 4 permanent system folders (never overwriting custom edits)
                db.execSQL("INSERT OR IGNORE INTO product_categories (id, code, name, description, iconKey, categoryType, legacyEnum, sortOrder, isActive, isSystemDefault, createdAtMs, updatedAtMs) VALUES (1, 'INDUSTRIAL_GAS', 'INDUSTRIAL GAS – REGULAR', 'Regular Industrial Cylinders', 'INDUSTRIAL', 'INDUSTRIAL', 'INDUSTRIAL_GAS', 1, 1, 1, $now, $now)")
                db.execSQL("INSERT OR IGNORE INTO product_categories (id, code, name, description, iconKey, categoryType, legacyEnum, sortOrder, isActive, isSystemDefault, createdAtMs, updatedAtMs) VALUES (2, 'CUTTING_GAS', 'CUTTING GAS', 'Cutting Gas Cylinders (MCG 22 KG & MCG 44 KG)', 'CUTTING', 'CUTTING', 'MCG_CUTTING_GAS', 2, 1, 1, $now, $now)")
                db.execSQL("INSERT OR IGNORE INTO product_categories (id, code, name, description, iconKey, categoryType, legacyEnum, sortOrder, isActive, isSystemDefault, createdAtMs, updatedAtMs) VALUES (3, 'COOKING_GAS', 'LPG GAS', 'LPG 11 KG, 22 KG & 44 KG', 'LPG', 'LPG', 'COOKING_GAS', 3, 1, 1, $now, $now)")
                db.execSQL("INSERT OR IGNORE INTO product_categories (id, code, name, description, iconKey, categoryType, legacyEnum, sortOrder, isActive, isSystemDefault, createdAtMs, updatedAtMs) VALUES (4, 'SMALL_48_CFT', 'INDUSTRIAL GAS – 48 CFT SMALL', 'Independent 48 CFT Small Industrial Cylinders & CO2 5 KG', 'SMALL_48', 'SMALL_48', 'SMALL_48_CFT', 4, 1, 1, $now, $now)")

                // 2. Extend products table non-destructively (preserving every existing product ID & balance)
                db.execSQL("ALTER TABLE products ADD COLUMN categoryId INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE products ADD COLUMN unit TEXT NOT NULL DEFAULT 'Cylinder'")
                db.execSQL("ALTER TABLE products ADD COLUMN description TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE products ADD COLUMN iconKey TEXT NOT NULL DEFAULT 'CYLINDER'")

                // Link every existing product to its permanent Category ID
                db.execSQL("UPDATE products SET categoryId = 1, iconKey = 'INDUSTRIAL' WHERE category = 'INDUSTRIAL_GAS'")
                db.execSQL("UPDATE products SET categoryId = 2, unit = 'KG', iconKey = 'CUTTING' WHERE category = 'MCG_CUTTING_GAS'")
                db.execSQL("UPDATE products SET categoryId = 3, unit = 'KG', iconKey = 'LPG' WHERE category = 'COOKING_GAS'")
                db.execSQL("UPDATE products SET categoryId = 4, unit = 'CFT', iconKey = 'SMALL_48' WHERE category = 'SMALL_48_CFT'")

                // Upgrade existing 48 CFT Small names in-place (preserving product IDs & historical stock!)
                db.execSQL("UPDATE products SET name = 'Oxygen 48 CFT', cylinderSizeKg = '48 CFT', unit = 'CFT', isActive = 1 WHERE category = 'SMALL_48_CFT' AND LOWER(name) = 'oxygen'")
                db.execSQL("UPDATE products SET name = 'Acetylene 48 CFT', cylinderSizeKg = '48 CFT', unit = 'CFT', isActive = 1 WHERE category = 'SMALL_48_CFT' AND LOWER(name) = 'acetylene'")
                db.execSQL("UPDATE products SET name = 'Argon 48 CFT', cylinderSizeKg = '48 CFT', unit = 'CFT', isActive = 1 WHERE category = 'SMALL_48_CFT' AND LOWER(name) = 'argon'")
                db.execSQL("UPDATE products SET name = 'Argon Mix 48 CFT', cylinderSizeKg = '48 CFT', unit = 'CFT', isActive = 1 WHERE category = 'SMALL_48_CFT' AND LOWER(name) = 'argon mix'")
                db.execSQL("UPDATE products SET name = 'CO2 5 KG', cylinderSizeKg = '5 KG', unit = 'KG', isActive = 1 WHERE category = 'SMALL_48_CFT' AND LOWER(REPLACE(name, ' ', '')) = 'co25kg'")
                db.execSQL("UPDATE products SET name = 'Nitrogen 48 CFT', cylinderSizeKg = '48 CFT', unit = 'CFT', isActive = 1 WHERE category = 'SMALL_48_CFT' AND LOWER(name) = 'nitrogen'")

                // Ensure all 6 48 CFT Small products exist if any were missing
                db.execSQL("INSERT OR IGNORE INTO products (category, categoryId, name, sortOrder, isActive, cylinderSizeKg, unit, description, iconKey) VALUES ('SMALL_48_CFT', 4, 'Oxygen 48 CFT', 401, 1, '48 CFT', 'CFT', 'Small 48 CFT Oxygen', 'SMALL_48')")
                db.execSQL("INSERT OR IGNORE INTO products (category, categoryId, name, sortOrder, isActive, cylinderSizeKg, unit, description, iconKey) VALUES ('SMALL_48_CFT', 4, 'Acetylene 48 CFT', 402, 1, '48 CFT', 'CFT', 'Small 48 CFT Acetylene', 'SMALL_48')")
                db.execSQL("INSERT OR IGNORE INTO products (category, categoryId, name, sortOrder, isActive, cylinderSizeKg, unit, description, iconKey) VALUES ('SMALL_48_CFT', 4, 'Argon 48 CFT', 403, 1, '48 CFT', 'CFT', 'Small 48 CFT Argon', 'SMALL_48')")
                db.execSQL("INSERT OR IGNORE INTO products (category, categoryId, name, sortOrder, isActive, cylinderSizeKg, unit, description, iconKey) VALUES ('SMALL_48_CFT', 4, 'Argon Mix 48 CFT', 404, 1, '48 CFT', 'CFT', 'Small 48 CFT Argon Mix', 'SMALL_48')")
                db.execSQL("INSERT OR IGNORE INTO products (category, categoryId, name, sortOrder, isActive, cylinderSizeKg, unit, description, iconKey) VALUES ('SMALL_48_CFT', 4, 'CO2 5 KG', 405, 1, '5 KG', 'KG', 'Small CO2 5 KG Cylinder', 'SMALL_48')")
                db.execSQL("INSERT OR IGNORE INTO products (category, categoryId, name, sortOrder, isActive, cylinderSizeKg, unit, description, iconKey) VALUES ('SMALL_48_CFT', 4, 'Nitrogen 48 CFT', 406, 1, '48 CFT', 'CFT', 'Small 48 CFT Nitrogen', 'SMALL_48')")

                // Reactivate CUTTING GAS MCG 22 KG & MCG 44 KG (preserving existing IDs if present)
                db.execSQL("UPDATE products SET name = 'MCG 22 KG', cylinderSizeKg = '22 KG', unit = 'KG', isActive = 1, categoryId = 2 WHERE category = 'MCG_CUTTING_GAS' AND LOWER(REPLACE(name, ' ', '')) = 'mcg22kg'")
                db.execSQL("UPDATE products SET name = 'MCG 44 KG', cylinderSizeKg = '44 KG', unit = 'KG', isActive = 1, categoryId = 2 WHERE category = 'MCG_CUTTING_GAS' AND LOWER(REPLACE(name, ' ', '')) = 'mcg44kg'")
                db.execSQL("INSERT OR IGNORE INTO products (category, categoryId, name, sortOrder, isActive, cylinderSizeKg, unit, description, iconKey) VALUES ('MCG_CUTTING_GAS', 2, 'MCG 22 KG', 201, 1, '22 KG', 'KG', 'Cutting Gas MCG 22 KG', 'CUTTING')")
                db.execSQL("INSERT OR IGNORE INTO products (category, categoryId, name, sortOrder, isActive, cylinderSizeKg, unit, description, iconKey) VALUES ('MCG_CUTTING_GAS', 2, 'MCG 44 KG', 202, 1, '44 KG', 'KG', 'Cutting Gas MCG 44 KG', 'CUTTING')")

                // 3. Extend user_accounts with salted PBKDF2 pinHash (plain PINs hashed on next ensureSeeded)
                db.execSQL("ALTER TABLE user_accounts ADD COLUMN pinHash TEXT NOT NULL DEFAULT ''")

                // 4. Extend authorized_mobiles with first-time registration, OTP verification & lockout tracking
                db.execSQL("ALTER TABLE authorized_mobiles ADD COLUMN firstTimeRegEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE authorized_mobiles ADD COLUMN isRegistered INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE authorized_mobiles ADD COLUMN otpVerifiedAtMs INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE authorized_mobiles ADD COLUMN registeredDeviceId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE authorized_mobiles ADD COLUMN failedLoginAttempts INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE authorized_mobiles ADD COLUMN lockedUntilMs INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): ConcordDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ConcordDatabase::class.java,
                    "concord_gas_inventory.db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9
                    )
                    .fallbackToDestructiveMigration(false)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

