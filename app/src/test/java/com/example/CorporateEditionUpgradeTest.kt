package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.auth.UaePhoneValidator
import com.example.data.local.ConcordDatabase
import com.example.data.model.CustomerExchangeItem
import com.example.data.model.GasNamingSanitizer
import com.example.data.model.LpgReturnCompany
import com.example.data.model.OperationResult
import com.example.data.model.ProductCategory
import com.example.data.model.StaffFeaturePermission
import com.example.data.model.StockCalculator
import com.example.data.model.TransactionType
import com.example.data.model.UserRole
import com.example.data.repository.InventoryRepository
import com.example.export.BackupManager
import com.example.ui.components.CalculatorMode
import com.example.ui.components.QuickCalculatorEngine
import com.example.ui.components.QuickCalculatorState
import com.example.ui.components.STARTUP_LAUNCH_DURATION_SECONDS
import com.example.ui.components.calculateVat5Breakdown
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xxhdpi", application = ConcordGasApp::class)
class CorporateEditionUpgradeTest {

    private lateinit var context: Context
    private lateinit var db: ConcordDatabase
    private lateinit var repository: InventoryRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, ConcordDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = InventoryRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun gasNamingSanitizer_andMigration6to7_preserveDataAndSanitizeLegacyNames() {
        assertEquals("Arab Gas 22kg", GasNamingSanitizer.sanitizeCompanyOrProductName("Arabic Gas 22kg"))
        assertEquals("Emirates Gas 44kg", GasNamingSanitizer.sanitizeCompanyOrProductName("Emergency Gas 44kg"))

        val dbName = "migration_v6_v7_corporate_test.db"
        context.deleteDatabase(dbName)

        val helperConfig = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(6) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS products (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            category TEXT NOT NULL,
                            name TEXT NOT NULL,
                            sortOrder INTEGER NOT NULL,
                            isActive INTEGER NOT NULL,
                            cylinderSizeKg TEXT NOT NULL DEFAULT ''
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS stock_transactions (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            businessDate TEXT NOT NULL,
                            timestamp INTEGER NOT NULL,
                            type TEXT NOT NULL,
                            productId INTEGER NOT NULL,
                            vehicleId INTEGER,
                            vehicleName TEXT,
                            staffId INTEGER NOT NULL,
                            staffName TEXT NOT NULL,
                            quantity INTEGER NOT NULL,
                            purchaseCompanyName TEXT NOT NULL DEFAULT '',
                            customerName TEXT NOT NULL DEFAULT '',
                            notes TEXT NOT NULL DEFAULT '',
                            remoteId TEXT NOT NULL DEFAULT ''
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS different_gas_returns (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            businessDate TEXT NOT NULL,
                            timestamp INTEGER NOT NULL,
                            staffId INTEGER NOT NULL,
                            staffName TEXT NOT NULL,
                            vehicleId INTEGER,
                            vehicleName TEXT,
                            customerName TEXT NOT NULL,
                            customerPhone TEXT NOT NULL,
                            deliveredItemsJson TEXT NOT NULL,
                            returnedItemsJson TEXT NOT NULL,
                            deliveredSummary TEXT NOT NULL,
                            returnedSummary TEXT NOT NULL,
                            totalDeliveredQty INTEGER NOT NULL,
                            totalReturnedQty INTEGER NOT NULL,
                            notes TEXT NOT NULL,
                            submissionKey TEXT NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL("INSERT INTO products (id, category, name, sortOrder, isActive, cylinderSizeKg) VALUES (1, 'COOKING_GAS', 'Arabic Gas 22kg', 1, 1, '22 KG')")
                    db.execSQL("INSERT INTO products (id, category, name, sortOrder, isActive, cylinderSizeKg) VALUES (2, 'COOKING_GAS', 'Emergency Gas 44kg', 2, 1, '44 KG')")
                    db.execSQL("INSERT INTO stock_transactions (businessDate, timestamp, type, productId, staffId, staffName, quantity, purchaseCompanyName, customerName, notes, remoteId) VALUES ('2026-11-27', 1000, 'STORE_PURCHASE_FULL', 1, 1, 'Admin', 10, 'Arabic Gas Trading', '', '', 'tx_1')")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(helperConfig)
        val sqliteDb = helper.writableDatabase
        ConcordDatabase.MIGRATION_6_7.migrate(sqliteDb)

        // Verify legacy names sanitized and rows preserved alongside newly seeded corporate products
        sqliteDb.query("SELECT id, name FROM products WHERE id IN (1, 2) ORDER BY id ASC").use { cursor ->
            assertEquals(2, cursor.count)
            assertTrue(cursor.moveToFirst())
            assertEquals("Arab Gas 22kg", cursor.getString(1))
            assertTrue(cursor.moveToNext())
            assertEquals("Emirates Gas 44kg", cursor.getString(1))
        }
        sqliteDb.query("SELECT COUNT(*) FROM products").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue("Expected corporate products seeded during migration", cursor.getInt(0) >= 9)
        }
        sqliteDb.query("SELECT purchaseCompanyName FROM stock_transactions").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Arab Gas Trading", cursor.getString(0))
        }

        // Verify new V7 tables exist
        sqliteDb.query("SELECT name FROM sqlite_master WHERE type='table' AND name IN ('authorized_mobiles','registered_devices','security_audit_logs')").use { cursor ->
            assertEquals(3, cursor.count)
        }

        helper.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun seededProducts_containExactV4LpgIndustrialCatalog_andExcludeMcgCutting() = runTest {
        repository.initializeDatabase()
        val products = repository.activeProductsFlow.first()
        val names = products.map { it.name }

        assertEquals(16, products.size)
        val lpgProducts = products.filter { it.category == ProductCategory.COOKING_GAS }
        assertEquals(listOf("11kg", "22kg", "44kg"), lpgProducts.map { it.name })
        assertFalse("Must never contain Arabic Gas", names.any { it.contains("Arabic Gas", ignoreCase = true) })
        assertFalse("Must never contain Emergency Gas", names.any { it.contains("Emergency Gas", ignoreCase = true) })
        assertFalse("Arab Gas must be a returned empty company option, not a standalone LPG product", names.any { it.contains("Arab Gas", ignoreCase = true) })
        assertFalse("Emirates Gas must be a returned empty company option, not a standalone LPG product", names.any { it.contains("Emirates Gas", ignoreCase = true) })

        val requiredIndustrial = listOf("Oxygen", "Acetylene", "Argon", "Argon Mix", "CO2 20kg", "CO2 30kg", "Nitrogen")
        requiredIndustrial.forEach { req ->
            assertTrue("Missing industrial gas $req", names.any { it.equals(req, ignoreCase = true) })
        }
        assertTrue("MCG and Cutting Gas must be completely removed from active catalog", products.none { it.category == ProductCategory.MCG_CUTTING_GAS })
        assertFalse(ProductCategory.activeEntries.contains(ProductCategory.MCG_CUTTING_GAS))
        assertEquals(listOf("Arab Gas", "Emirates Gas"), LpgReturnCompany.entries.map { it.displayName })
    }

    @Test
    fun damagedStock_tracksSeparatelyWithoutAlteringFullOrEmptyBalances() = runTest {
        repository.initializeDatabase()
        val admin = repository.usersFlow.first().first { it.role == UserRole.ADMIN }
        val vehicle = repository.vehiclesFlow.first().first()
        val lpg22 = repository.activeProductsFlow.first().first { it.category == ProductCategory.COOKING_GAS && it.name == "22kg" }
        val today = "2026-11-27"

        repository.setOpeningStockForDate(StockCalculator.SCOPE_STORE, 0L, lpg22.id, today, 20, 10, admin)
        repository.setOpeningStockForDate(StockCalculator.SCOPE_VEHICLE, vehicle.id, lpg22.id, today, 8, 4, admin)

        // Store damaged add + remove
        assertTrue(
            repository.recordTransaction(today, TransactionType.STORE_DAMAGED_ADD, lpg22.id, null, admin, 3, notes = "Valve leak") is OperationResult.Success
        )
        assertTrue(
            repository.recordTransaction(today, TransactionType.STORE_DAMAGED_REMOVE, lpg22.id, null, admin, 1, notes = "Repaired") is OperationResult.Success
        )
        // Guard against removing more damaged than available
        val overRemoveStore = repository.recordTransaction(today, TransactionType.STORE_DAMAGED_REMOVE, lpg22.id, null, admin, 5, notes = "")
        assertTrue(overRemoveStore is OperationResult.InsufficientStock)

        val storeRow = repository.observeStoreStock(today).first().first { it.product.id == lpg22.id }
        assertEquals(20, storeRow.storeFullBalance)
        assertEquals(10, storeRow.storeEmptyBalance)
        assertEquals(2, storeRow.damagedBalance)

        // Vehicle damaged add + remove
        assertTrue(
            repository.recordTransaction(today, TransactionType.VEHICLE_DAMAGED_ADD, lpg22.id, vehicle.id, admin, 2, notes = "") is OperationResult.Success
        )
        val overRemoveVeh = repository.recordTransaction(today, TransactionType.VEHICLE_DAMAGED_REMOVE, lpg22.id, vehicle.id, admin, 3, notes = "")
        assertTrue(overRemoveVeh is OperationResult.InsufficientStock)

        val vehRow = repository.observeVehicleStock(vehicle.id, today).first().first { it.product.id == lpg22.id }
        assertEquals(8, vehRow.fullBalance)
        assertEquals(4, vehRow.emptyBalance)
        assertEquals(2, vehRow.damagedBalance)
    }

    @Test
    fun lpgExchange_atomicEditAndDelete_andReturnedEmptyCompany_reverseAndReapplyWithoutDoubleDeduction() = runTest {
        repository.initializeDatabase()
        val admin = repository.usersFlow.first().first { it.role == UserRole.ADMIN }
        val products = repository.activeProductsFlow.first()
        val lpg22 = products.first { it.category == ProductCategory.COOKING_GAS && it.name == "22kg" }
        val lpg11 = products.first { it.category == ProductCategory.COOKING_GAS && it.name == "11kg" }
        val today = "2026-11-27"

        repository.setOpeningStockForDate(StockCalculator.SCOPE_STORE, 0L, lpg22.id, today, 10, 5, admin)
        repository.setOpeningStockForDate(StockCalculator.SCOPE_STORE, 0L, lpg11.id, today, 10, 5, admin)

        // 1. Create non-1:1 LPG exchange: Deliver 22kg Full (2), Receive 11kg Empty (1) with Returned Empty Company = Arab Gas
        val createRes = repository.recordDifferentGasReturn(
            businessDate = today,
            deliveredItems = listOf(CustomerExchangeItem(lpg22.id, 2)),
            returnedItems = listOf(CustomerExchangeItem(lpg11.id, 1)),
            vehicleId = null,
            actor = admin,
            customerName = "Al Majid Catering",
            customerPhone = "+971501234567",
            notes = "Initial delivery",
            submissionKey = "corp_exch_1",
            returnedEmptyCompany = LpgReturnCompany.ARAB_GAS.displayName
        )
        assertTrue(createRes is OperationResult.Success)
        val createdRecord = repository.customerReturnHistoryFlow.first()
            .flatMap { it.records }
            .first { it.submissionKey == "corp_exch_1" }
        assertEquals("Arab Gas", createdRecord.returnedEmptyCompany)

        var store = repository.observeStoreStock(today).first()
        assertEquals(8, store.first { it.product.id == lpg22.id }.storeFullBalance)
        assertEquals(6, store.first { it.product.id == lpg11.id }.storeEmptyBalance)

        // 2. Edit exchange to 22kg Full (3) & 11kg Empty (2) with Returned Empty Company = Emirates Gas -> atomic reversal + apply once
        val editRes = repository.updateDifferentGasReturn(
            recordId = createdRecord.id,
            deliveredItems = listOf(CustomerExchangeItem(lpg22.id, 3)),
            returnedItems = listOf(CustomerExchangeItem(lpg11.id, 2)),
            vehicleId = null,
            actor = admin,
            customerName = "Al Majid Catering",
            customerPhone = "+971501234567",
            notes = "Corrected count",
            returnedEmptyCompany = LpgReturnCompany.EMIRATES_GAS.displayName
        )
        assertTrue(editRes is OperationResult.Success)

        val editedRecord = repository.customerReturnHistoryFlow.first()
            .flatMap { it.records }
            .first { it.id == createdRecord.id }
        assertEquals("Emirates Gas", editedRecord.returnedEmptyCompany)

        // Verify BackupManager v4 round-trip preserves returnedEmptyCompany
        val backupMgr = BackupManager(db)
        val json = backupMgr.exportBackupJson()
        assertTrue(backupMgr.restoreBackupJson(json) is OperationResult.Success)
        val restoredRecord = repository.customerReturnHistoryFlow.first()
            .flatMap { it.records }
            .first { it.submissionKey == "corp_exch_1" }
        assertEquals("Emirates Gas", restoredRecord.returnedEmptyCompany)

        store = repository.observeStoreStock(today).first()
        assertEquals("Should be 10 - 3 = 7 (no double deduction)", 7, store.first { it.product.id == lpg22.id }.storeFullBalance)
        assertEquals("Should be 5 + 2 = 7 (no double addition)", 7, store.first { it.product.id == lpg11.id }.storeEmptyBalance)

        // 3. Delete exchange -> cleanly reverses stock back to 10F / 5E
        val delRes = repository.deleteDifferentGasReturn(restoredRecord.id, admin)
        assertTrue(delRes is OperationResult.Success)

        store = repository.observeStoreStock(today).first()
        assertEquals(10, store.first { it.product.id == lpg22.id }.storeFullBalance)
        assertEquals(5, store.first { it.product.id == lpg11.id }.storeEmptyBalance)
        assertTrue(repository.customerReturnHistoryFlow.first().isEmpty())
    }

    @Test
    fun v4FuturisticUpgrade_migration7to8_retiresLegacyMcgNonDestructively_andSplashDurationIn4To6Seconds() {
        assertTrue(
            "3D Cinematic opening must last 4-6 seconds",
            STARTUP_LAUNCH_DURATION_SECONDS in 4.0f..6.0f
        )
        assertTrue(StaffFeaturePermission.entries.contains(StaffFeaturePermission.INDUSTRIAL_DELIVERY))

        val dbName = "migration_v7_v8_futuristic_test.db"
        context.deleteDatabase(dbName)
        val helperConfig = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(7) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS products (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            category TEXT NOT NULL,
                            name TEXT NOT NULL,
                            sortOrder INTEGER NOT NULL,
                            isActive INTEGER NOT NULL,
                            cylinderSizeKg TEXT NOT NULL DEFAULT ''
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS different_gas_returns (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            businessDate TEXT NOT NULL,
                            timestamp INTEGER NOT NULL,
                            staffId INTEGER NOT NULL,
                            staffName TEXT NOT NULL,
                            vehicleId INTEGER,
                            vehicleName TEXT,
                            customerName TEXT NOT NULL,
                            customerPhone TEXT NOT NULL,
                            deliveredItemsJson TEXT NOT NULL,
                            returnedItemsJson TEXT NOT NULL,
                            deliveredSummary TEXT NOT NULL,
                            returnedSummary TEXT NOT NULL,
                            totalDeliveredQty INTEGER NOT NULL,
                            totalReturnedQty INTEGER NOT NULL,
                            notes TEXT NOT NULL,
                            submissionKey TEXT NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL("INSERT INTO products (id, category, name, sortOrder, isActive, cylinderSizeKg) VALUES (1, 'MCG_CUTTING_GAS', 'MCG', 401, 1, 'Standard')")
                    db.execSQL("INSERT INTO products (id, category, name, sortOrder, isActive, cylinderSizeKg) VALUES (2, 'COOKING_GAS', 'Arab Gas 22kg', 305, 1, '22 KG')")
                    db.execSQL("INSERT INTO products (id, category, name, sortOrder, isActive, cylinderSizeKg) VALUES (3, 'COOKING_GAS', '22kg', 302, 1, '22 KG')")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(helperConfig)
        val sqliteDb = helper.writableDatabase
        ConcordDatabase.MIGRATION_7_8.migrate(sqliteDb)

        // Legacy MCG and Arab Gas product rows are retired (isActive = 0), NEVER deleted
        sqliteDb.query("SELECT id, isActive FROM products WHERE id IN (1, 2, 3) ORDER BY id ASC").use { c ->
            assertEquals(3, c.count)
            assertTrue(c.moveToFirst())
            assertEquals(0, c.getInt(1)) // MCG retired
            assertTrue(c.moveToNext())
            assertEquals(0, c.getInt(1)) // Standalone Arab Gas 22kg retired
            assertTrue(c.moveToNext())
            assertEquals(1, c.getInt(1)) // 22kg LPG stays active
        }

        // Verify returnedEmptyCompany column added cleanly
        sqliteDb.execSQL(
            """
            INSERT INTO different_gas_returns (
                businessDate, timestamp, staffId, staffName, vehicleId, vehicleName,
                customerName, customerPhone, deliveredItemsJson, returnedItemsJson,
                deliveredSummary, returnedSummary, totalDeliveredQty, totalReturnedQty,
                notes, submissionKey, returnedEmptyCompany
            ) VALUES (
                '2026-11-27', 1795680000000, 1, 'Admin', NULL, NULL,
                'Test Kitchen', '+971501234567', '[]', '[]',
                '22kg x1', '11kg x1', 1, 1,
                '', 'v8_mig_key', 'Arab Gas'
            )
            """.trimIndent()
        )
        sqliteDb.query("SELECT returnedEmptyCompany FROM different_gas_returns WHERE submissionKey = 'v8_mig_key'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("Arab Gas", c.getString(0))
        }

        helper.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun calculator_normalNeverAddsVat_andBillVat5ComputesExactUaeBreakdown() {
        // Normal mode: 50 + 50 = 100, vatBreakdown MUST be null
        var state = QuickCalculatorState(mode = CalculatorMode.NORMAL)
        listOf("5", "0", "+", "5", "0", "=").forEach { key ->
            state = QuickCalculatorEngine.onKeyPress(state, key)
        }
        assertEquals("100", state.display)
        assertNull("Normal calculator must never auto-add VAT", state.vatBreakdown)

        // Bill 5% VAT mode: 100 -> Bill AED 100.00, VAT AED 5.00, Grand Total AED 105.00
        val vatState = QuickCalculatorEngine.setMode(state, CalculatorMode.BILL_VAT_5)
        val breakdown = vatState.vatBreakdown
        assertNotNull(breakdown)
        assertEquals("AED 100.00", breakdown!!.billAmountFormatted)
        assertEquals("AED 5.00", breakdown.vatAmountFormatted)
        assertEquals("AED 105.00", breakdown.grandTotalFormatted)

        // Fractional rounding accuracy (HALF_UP scale 2)
        val fractional = calculateVat5Breakdown(BigDecimal("123.45"))
        assertEquals(BigDecimal("123.45"), fractional.billAmount)
        assertEquals(BigDecimal("6.17"), fractional.vatAmount)
        assertEquals(BigDecimal("129.62"), fractional.grandTotal)
    }

    @Test
    fun uaePhoneValidator_ownerReadOnly_protectedOwnerImmutability_andDeviceRevocation() = runTest {
        repository.initializeDatabase()
        val users = repository.usersFlow.first()
        val admin = users.first { it.role == UserRole.ADMIN }
        val staff = users.first { it.role == UserRole.STAFF }
        val owner = users.firstOrNull { it.role == UserRole.OWNER_VIEW }
            ?: admin.copy(id = 999L, role = UserRole.OWNER_VIEW, displayName = "Owner")

        // 1. UAE +971 Phone Validator
        assertEquals("+971501234567", UaePhoneValidator.normalizeUaePhone("+971 50 123 4567"))
        assertEquals("+971551234567", UaePhoneValidator.normalizeUaePhone("0551234567"))
        assertEquals("+971521234567", UaePhoneValidator.normalizeUaePhone("521234567"))
        assertEquals("+971561234567", UaePhoneValidator.normalizeUaePhone("00971561234567"))
        assertNull(UaePhoneValidator.normalizeUaePhone("+14155551234"))
        assertNull(UaePhoneValidator.normalizeUaePhone("+97141234567")) // Dubai landline, not mobile 5x
        assertNull(UaePhoneValidator.normalizeUaePhone("12345"))

        // 2. Owner View strictly blocked from all security mutations
        val ownerUpsert = repository.upsertAuthorizedMobile(
            phoneRaw = "+971501112233",
            displayName = "Blocked",
            role = UserRole.STAFF,
            staffUserId = staff.id,
            allowedFeatures = setOf(StaffFeaturePermission.MY_VEHICLE_STOCK),
            isActive = true,
            isProtectedOwner = false,
            actor = owner
        )
        assertTrue(ownerUpsert is OperationResult.ValidationError)
        assertTrue(repository.logoutAllDevices(owner) is OperationResult.ValidationError)

        // 3. Protected Owner registration & immutability against Admin modification/deletion
        val createProtectedOwner = repository.upsertAuthorizedMobile(
            phoneRaw = "+971509998877",
            displayName = "Executive Owner",
            role = UserRole.OWNER_VIEW,
            staffUserId = null,
            allowedFeatures = StaffFeaturePermission.entries.toSet(),
            isActive = true,
            isProtectedOwner = true,
            actor = admin
        )
        assertTrue(createProtectedOwner is OperationResult.Success)
        val ownerMobile = repository.findAuthorizedMobileByPhone("+971509998877")
        assertNotNull(ownerMobile)
        assertTrue(ownerMobile!!.isProtectedOwner)

        // Admin CANNOT demote or deactivate Protected Owner
        val adminDemoteOwner = repository.upsertAuthorizedMobile(
            phoneRaw = "+971509998877",
            displayName = "Demoted",
            role = UserRole.STAFF,
            staffUserId = staff.id,
            allowedFeatures = setOf(StaffFeaturePermission.MY_VEHICLE_STOCK),
            isActive = false,
            isProtectedOwner = false,
            actor = admin
        )
        assertTrue("Protected Owner must reject Admin modification", adminDemoteOwner is OperationResult.ValidationError)

        // Admin CANNOT delete Protected Owner
        val adminDeleteOwner = repository.deleteAuthorizedMobile(ownerMobile.id, admin)
        assertTrue("Protected Owner must reject Admin deletion", adminDeleteOwner is OperationResult.ValidationError)

        // 4. Staff mobile + feature permissions persistence
        val allowedStaffSet = setOf(
            StaffFeaturePermission.MY_VEHICLE_STOCK,
            StaffFeaturePermission.LPG_EXCHANGE
        )
        val staffAuthRes = repository.upsertAuthorizedMobile(
            phoneRaw = "+971521112233",
            displayName = staff.displayName,
            role = UserRole.STAFF,
            staffUserId = staff.id,
            allowedFeatures = allowedStaffSet,
            isActive = true,
            isProtectedOwner = false,
            actor = admin
        )
        assertTrue(staffAuthRes is OperationResult.Success)
        val loadedPerms = repository.getStaffPermissionsForUser(staff.id)
        assertEquals(allowedStaffSet, loadedPerms)

        // 5. Device registration, single revoke, and logout all devices
        repository.registerOrTouchDevice("dev_1", "Samsung S24", "+971521112233", staff.id, staff.displayName, UserRole.STAFF)
        repository.registerOrTouchDevice("dev_2", "Pixel 9", "+971501234567", admin.id, admin.displayName, UserRole.ADMIN)
        assertFalse(repository.isDeviceRevoked("dev_1"))

        assertTrue(repository.revokeDevice("dev_1", admin) is OperationResult.Success)
        assertTrue(repository.isDeviceRevoked("dev_1"))
        assertFalse(repository.isDeviceRevoked("dev_2"))

        assertTrue(repository.logoutAllDevices(admin) is OperationResult.Success)
        assertTrue(repository.isDeviceRevoked("dev_2"))
    }
}
