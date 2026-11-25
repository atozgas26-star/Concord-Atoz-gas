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
        StockTransactionEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ConcordDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun vehicleDao(): VehicleDao
    abstract fun productDao(): ProductDao
    abstract fun openingStockDao(): OpeningStockDao
    abstract fun transactionDao(): TransactionDao

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
                    ProductEntity(category = ProductCategory.INDUSTRIAL_GAS, name = "Oxygen", sortOrder = 101),
                    ProductEntity(category = ProductCategory.INDUSTRIAL_GAS, name = "Acetylene", sortOrder = 102),
                    ProductEntity(category = ProductCategory.INDUSTRIAL_GAS, name = "Argon", sortOrder = 103),
                    ProductEntity(category = ProductCategory.INDUSTRIAL_GAS, name = "Argon Mix", sortOrder = 104),
                    ProductEntity(category = ProductCategory.INDUSTRIAL_GAS, name = "CO2 20kg", sortOrder = 105),
                    ProductEntity(category = ProductCategory.INDUSTRIAL_GAS, name = "CO2 30kg", sortOrder = 106),
                    ProductEntity(category = ProductCategory.INDUSTRIAL_GAS, name = "Nitrogen", sortOrder = 107),

                    // 48 CFT SMALL
                    ProductEntity(category = ProductCategory.SMALL_48_CFT, name = "Oxygen", sortOrder = 201),
                    ProductEntity(category = ProductCategory.SMALL_48_CFT, name = "Acetylene", sortOrder = 202),
                    ProductEntity(category = ProductCategory.SMALL_48_CFT, name = "Argon", sortOrder = 203),
                    ProductEntity(category = ProductCategory.SMALL_48_CFT, name = "Argon Mix", sortOrder = 204),
                    ProductEntity(category = ProductCategory.SMALL_48_CFT, name = "CO2 5kg", sortOrder = 205),
                    ProductEntity(category = ProductCategory.SMALL_48_CFT, name = "Nitrogen", sortOrder = 206),

                    // COOKING GAS
                    ProductEntity(category = ProductCategory.COOKING_GAS, name = "11kg", sortOrder = 301),
                    ProductEntity(category = ProductCategory.COOKING_GAS, name = "22kg", sortOrder = 302),
                    ProductEntity(category = ProductCategory.COOKING_GAS, name = "44kg", sortOrder = 303),

                    // MCG CUTTING GAS
                    ProductEntity(category = ProductCategory.MCG_CUTTING_GAS, name = "22kg", sortOrder = 401),
                    ProductEntity(category = ProductCategory.MCG_CUTTING_GAS, name = "44kg", sortOrder = 402)
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

        fun getInstance(context: Context): ConcordDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ConcordDatabase::class.java,
                    "concord_gas_inventory.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration(false)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
