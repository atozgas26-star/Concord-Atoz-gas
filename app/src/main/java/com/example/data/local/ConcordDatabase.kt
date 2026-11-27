package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.withTransaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.ProductCategory
import com.example.data.model.UserRole

@Database(
    entities = [
        UserAccountEntity::class,
        VehicleEntity::class,
        ProductEntity::class,
        OpeningStockEntity::class,
        StockTransactionEntity::class,
        StaffAttendanceEntity::class,
        PendingStoreTransferEntity::class,
        AppNotificationEntity::class,
        ChatMessageEntity::class,
        DifferentGasReturnEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ConcordDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun vehicleDao(): VehicleDao
    abstract fun productDao(): ProductDao
    abstract fun openingStockDao(): OpeningStockDao
    abstract fun transactionDao(): TransactionDao
    abstract fun staffAttendanceDao(): StaffAttendanceDao
    abstract fun pendingStoreTransferDao(): PendingStoreTransferDao
    abstract fun appNotificationDao(): AppNotificationDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun differentGasReturnDao(): DifferentGasReturnDao

    suspend fun ensureSeeded() {
        val existingUsers = userDao().getAllUsersOnce()
        val existingProducts = productDao().getAllProductsOnce()
        if (existingUsers.isNotEmpty() && existingProducts.isNotEmpty()) return

        withTransaction {
            if (userDao().getAllUsersOnce().isEmpty()) {
                val adminId = userDao().insert(
                    UserAccountEntity(
                        username = "admin_ragavan",
                        displayName = "Ragavan",
                        role = UserRole.ADMIN,
                        pin = "1725"
                    )
                )
                // Keep unused variable check happy while maintaining clarity
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
                    val (username, displayName, pin) = staffTriple
                    val userId = userDao().insert(
                        UserAccountEntity(
                            username = username,
                            displayName = displayName,
                            role = UserRole.STAFF,
                            pin = pin
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
            }

            if (productDao().getAllProductsOnce().isEmpty()) {
                val seededProducts = listOf(
                    // INDUSTRIAL GAS
                    ProductEntity(category = ProductCategory.INDUSTRIAL_GAS, name = "Oxygen", sortOrder = 101, cylinderSizeKg = "47L Industrial"),
                    ProductEntity(category = ProductCategory.INDUSTRIAL_GAS, name = "Acetylene", sortOrder = 102, cylinderSizeKg = "40L Industrial"),
                    ProductEntity(category = ProductCategory.INDUSTRIAL_GAS, name = "Argon", sortOrder = 103, cylinderSizeKg = "47L Industrial"),
                    ProductEntity(category = ProductCategory.INDUSTRIAL_GAS, name = "Argon Mix", sortOrder = 104, cylinderSizeKg = "47L Industrial"),
                    ProductEntity(category = ProductCategory.INDUSTRIAL_GAS, name = "CO2 20kg", sortOrder = 105, cylinderSizeKg = "20 KG"),
                    ProductEntity(category = ProductCategory.INDUSTRIAL_GAS, name = "CO2 30kg", sortOrder = 106, cylinderSizeKg = "30 KG"),
                    ProductEntity(category = ProductCategory.INDUSTRIAL_GAS, name = "Nitrogen", sortOrder = 107, cylinderSizeKg = "47L Industrial"),

                    // 48 CFT SMALL
                    ProductEntity(category = ProductCategory.SMALL_48_CFT, name = "Oxygen", sortOrder = 201, cylinderSizeKg = "48 CFT"),
                    ProductEntity(category = ProductCategory.SMALL_48_CFT, name = "Acetylene", sortOrder = 202, cylinderSizeKg = "48 CFT"),
                    ProductEntity(category = ProductCategory.SMALL_48_CFT, name = "Argon", sortOrder = 203, cylinderSizeKg = "48 CFT"),
                    ProductEntity(category = ProductCategory.SMALL_48_CFT, name = "Argon Mix", sortOrder = 204, cylinderSizeKg = "48 CFT"),
                    ProductEntity(category = ProductCategory.SMALL_48_CFT, name = "CO2 5kg", sortOrder = 205, cylinderSizeKg = "5 KG"),
                    ProductEntity(category = ProductCategory.SMALL_48_CFT, name = "Nitrogen", sortOrder = 206, cylinderSizeKg = "48 CFT"),

                    // COOKING GAS
                    ProductEntity(category = ProductCategory.COOKING_GAS, name = "11kg", sortOrder = 301, cylinderSizeKg = "11 KG"),
                    ProductEntity(category = ProductCategory.COOKING_GAS, name = "22kg", sortOrder = 302, cylinderSizeKg = "22 KG"),
                    ProductEntity(category = ProductCategory.COOKING_GAS, name = "44kg", sortOrder = 303, cylinderSizeKg = "44 KG"),

                    // MCG CUTTING GAS
                    ProductEntity(category = ProductCategory.MCG_CUTTING_GAS, name = "22kg", sortOrder = 401, cylinderSizeKg = "22 KG"),
                    ProductEntity(category = ProductCategory.MCG_CUTTING_GAS, name = "44kg", sortOrder = 402, cylinderSizeKg = "44 KG")
                )
                productDao().insertAll(seededProducts)
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

        fun getInstance(context: Context): ConcordDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ConcordDatabase::class.java,
                    "concord_gas_inventory.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration(false)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
