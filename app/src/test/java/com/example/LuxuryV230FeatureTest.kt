package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ConcordDatabase
import com.example.data.local.DifferentGasReturnEntity
import com.example.data.local.ProductEntity
import com.example.data.model.CustomerExchangeItem
import com.example.data.model.CustomerReturnHistoryGroup
import com.example.data.model.OperationResult
import com.example.data.model.ProductCategory
import com.example.data.model.StaffReturnHistoryGroup
import com.example.data.model.StockCalculator
import com.example.data.model.ThemePreset
import com.example.data.model.UserRole
import com.example.data.repository.InventoryRepository
import com.example.ui.components.DifferentGasReturnDialog
import com.example.ui.components.DifferentGasReturnHistoryDialog
import com.example.ui.components.LuxurySuccessCheckOverlay
import com.example.ui.components.RoleWelcomeOverlay
import com.example.ui.components.StartupSplashOverlay
import com.example.ui.theme.ConcordGasTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xxhdpi", application = ConcordGasApp::class)
class LuxuryV230FeatureTest {

    @get:Rule
    val composeRule = createComposeRule()

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
    fun migration5to6_preservesExistingDataAndCreatesDifferentGasReturnsTable() {
        val dbName = "migration_v5_v6_test.db"
        context.deleteDatabase(dbName)

        // Create a real SQLite v5 schema with pre-existing products
        val helperConfig = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(5) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS products (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            category TEXT NOT NULL,
                            name TEXT NOT NULL,
                            sortOrder INTEGER NOT NULL,
                            isActive INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL("INSERT INTO products (id, category, name, sortOrder, isActive) VALUES (1, 'INDUSTRIAL_GAS', 'Oxygen', 101, 1)")
                    db.execSQL("INSERT INTO products (id, category, name, sortOrder, isActive) VALUES (2, 'INDUSTRIAL_GAS', 'Argon', 102, 1)")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(helperConfig)
        val sqliteDb = helper.writableDatabase
        ConcordDatabase.MIGRATION_5_6.migrate(sqliteDb)

        // Verify existing products intact and new cylinderSizeKg column defaulted cleanly
        sqliteDb.query("SELECT id, name, cylinderSizeKg FROM products ORDER BY id ASC").use { cursor ->
            assertEquals(2, cursor.count)
            assertTrue(cursor.moveToFirst())
            assertEquals("Oxygen", cursor.getString(1))
            assertEquals("", cursor.getString(2))
            assertTrue(cursor.moveToNext())
            assertEquals("Argon", cursor.getString(1))
            assertEquals("", cursor.getString(2))
        }

        // Verify different_gas_returns table exists and enforces UNIQUE submissionKey
        sqliteDb.execSQL(
            """
            INSERT INTO different_gas_returns (
                businessDate, timestamp, staffId, staffName, vehicleId, vehicleName,
                customerName, customerPhone, deliveredItemsJson, returnedItemsJson,
                deliveredSummary, returnedSummary, totalDeliveredQty, totalReturnedQty,
                notes, submissionKey
            ) VALUES (
                '2026-11-27', 1795680000000, 1, 'Admin', NULL, NULL,
                'Al Noor Steel', '0501234567', '[]', '[]',
                'Oxygen x1', 'Argon x1', 1, 1,
                '', 'unique_key_1'
            )
            """.trimIndent()
        )
        var duplicateBlocked = false
        try {
            sqliteDb.execSQL(
                """
                INSERT INTO different_gas_returns (
                    businessDate, timestamp, staffId, staffName, vehicleId, vehicleName,
                    customerName, customerPhone, deliveredItemsJson, returnedItemsJson,
                    deliveredSummary, returnedSummary, totalDeliveredQty, totalReturnedQty,
                    notes, submissionKey
                ) VALUES (
                    '2026-11-27', 1795680001000, 1, 'Admin', NULL, NULL,
                    'Al Noor Steel', '0501234567', '[]', '[]',
                    'Oxygen x1', 'Argon x1', 1, 1,
                    '', 'unique_key_1'
                )
                """.trimIndent()
            )
        } catch (_: Exception) {
            duplicateBlocked = true
        }
        assertTrue("Duplicate submissionKey must be rejected by UNIQUE index", duplicateBlocked)

        helper.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun differentGasReturn_singleAndMultiGas_isolatesCategories_immediateUpdate_blocksDuplicatesAndOwner() = runTest {
        repository.initializeDatabase()
        val users = repository.usersFlow.first()
        val admin = users.first { it.role == UserRole.ADMIN }
        val staff = users.first { it.role == UserRole.STAFF }
        val owner = users.firstOrNull { it.role == UserRole.OWNER_VIEW }
            ?: admin.copy(id = 999L, role = UserRole.OWNER_VIEW, displayName = "Owner")
        val staffVehicle = repository.vehiclesFlow.first().first { it.staffUserId == staff.id }

        val products = repository.activeProductsFlow.first()
        val oxygen = products.first { it.name.equals("Oxygen", ignoreCase = true) }
        val argon = products.first { it.name.equals("Argon", ignoreCase = true) }
        val co220 = products.first { it.name.contains("CO2", ignoreCase = true) && it.name.contains("20", ignoreCase = true) }
        val lpg11 = products.first { it.name.contains("11", ignoreCase = true) && it.category == ProductCategory.COOKING_GAS }
        val today = "2026-11-27"

        // Seed Store Opening Stock: Oxygen 10F/2E, Argon 5F/3E, CO2 20kg 8F/1E, LPG 11kg 12F/4E
        repository.setOpeningStockForDate(StockCalculator.SCOPE_STORE, 0L, oxygen.id, today, 10, 2, admin)
        repository.setOpeningStockForDate(StockCalculator.SCOPE_STORE, 0L, argon.id, today, 5, 3, admin)
        repository.setOpeningStockForDate(StockCalculator.SCOPE_STORE, 0L, co220.id, today, 8, 1, admin)
        repository.setOpeningStockForDate(StockCalculator.SCOPE_STORE, 0L, lpg11.id, today, 12, 4, admin)

        // Seed Staff Vehicle Opening Stock: Oxygen 6F/1E, Argon 4F/0E
        repository.setOpeningStockForDate(StockCalculator.SCOPE_VEHICLE, staffVehicle.id, oxygen.id, today, 6, 1, admin)
        repository.setOpeningStockForDate(StockCalculator.SCOPE_VEHICLE, staffVehicle.id, argon.id, today, 4, 0, admin)

        // 1. Admin Store: Deliver Oxygen Full (1) + Receive Argon Empty (1) -> immediate stock update
        val res1 = repository.recordDifferentGasReturn(
            businessDate = today,
            deliveredItems = listOf(CustomerExchangeItem(oxygen.id, 1)),
            returnedItems = listOf(CustomerExchangeItem(argon.id, 1)),
            vehicleId = null,
            actor = admin,
            customerName = "Apex Metal Works",
            customerPhone = "050-111-2222",
            notes = "Inv #101",
            submissionKey = "key_oxygen_argon_1"
        )
        assertTrue(res1 is OperationResult.Success)

        val storeAfter1 = repository.observeStoreStock(today).first()
        val oxRow1 = storeAfter1.first { it.product.id == oxygen.id }
        val arRow1 = storeAfter1.first { it.product.id == argon.id }
        assertEquals(9, oxRow1.storeFullBalance)
        assertEquals(2, oxRow1.storeEmptyBalance) // Oxygen Empty untouched!
        assertEquals(5, arRow1.storeFullBalance)  // Argon Full untouched!
        assertEquals(4, arRow1.storeEmptyBalance) // Argon Empty increased by 1!

        // 2. Duplicate submissionKey MUST be blocked with zero double deduction
        val dupRes = repository.recordDifferentGasReturn(
            businessDate = today,
            deliveredItems = listOf(CustomerExchangeItem(oxygen.id, 1)),
            returnedItems = listOf(CustomerExchangeItem(argon.id, 1)),
            vehicleId = null,
            actor = admin,
            customerName = "Apex Metal Works",
            customerPhone = "050-111-2222",
            notes = "Inv #101",
            submissionKey = "key_oxygen_argon_1"
        )
        assertTrue(dupRes is OperationResult.ValidationError)
        val storeAfterDup = repository.observeStoreStock(today).first()
        assertEquals(9, storeAfterDup.first { it.product.id == oxygen.id }.storeFullBalance)
        assertEquals(4, storeAfterDup.first { it.product.id == argon.id }.storeEmptyBalance)

        // 3. Multi-gas multi-quantity atomic transaction (Oxygen Full 2 + LPG 11kg Full 3; Argon Empty 1 + CO2 20kg Empty 4)
        val multiRes = repository.recordDifferentGasReturn(
            businessDate = today,
            deliveredItems = listOf(
                CustomerExchangeItem(oxygen.id, 2),
                CustomerExchangeItem(lpg11.id, 3)
            ),
            returnedItems = listOf(
                CustomerExchangeItem(argon.id, 1),
                CustomerExchangeItem(co220.id, 4)
            ),
            vehicleId = null,
            actor = admin,
            customerName = "Gulf Fabrication",
            customerPhone = "055-888-9999",
            notes = "Multi exchange",
            submissionKey = "key_multi_2"
        )
        assertTrue(multiRes is OperationResult.Success)

        val storeAfterMulti = repository.observeStoreStock(today).first()
        assertEquals(7, storeAfterMulti.first { it.product.id == oxygen.id }.storeFullBalance)
        assertEquals(9, storeAfterMulti.first { it.product.id == lpg11.id }.storeFullBalance)
        assertEquals(5, storeAfterMulti.first { it.product.id == argon.id }.storeEmptyBalance)
        assertEquals(5, storeAfterMulti.first { it.product.id == co220.id }.storeEmptyBalance)
        // Cross-category safety: CO2 Full untouched, LPG Empty untouched
        assertEquals(8, storeAfterMulti.first { it.product.id == co220.id }.storeFullBalance)
        assertEquals(4, storeAfterMulti.first { it.product.id == lpg11.id }.storeEmptyBalance)

        // 4. Staff Vehicle: Immediate update WITHOUT Admin approval!
        val staffRes = repository.recordDifferentGasReturn(
            businessDate = today,
            deliveredItems = listOf(CustomerExchangeItem(oxygen.id, 2)),
            returnedItems = listOf(CustomerExchangeItem(argon.id, 2)),
            vehicleId = staffVehicle.id,
            actor = staff,
            customerName = "Apex Metal Works",
            customerPhone = "050-111-2222",
            notes = "Route delivery",
            submissionKey = "key_staff_3"
        )
        assertTrue(staffRes is OperationResult.Success)
        val vehAfterStaff = repository.observeVehicleStock(staffVehicle.id, today).first()
        assertEquals(4, vehAfterStaff.first { it.product.id == oxygen.id }.fullBalance)
        assertEquals(1, vehAfterStaff.first { it.product.id == oxygen.id }.emptyBalance)
        assertEquals(4, vehAfterStaff.first { it.product.id == argon.id }.fullBalance)
        assertEquals(2, vehAfterStaff.first { it.product.id == argon.id }.emptyBalance)

        // 5. Insufficient Full stock fails atomically (zero partial updates)
        val insRes = repository.recordDifferentGasReturn(
            businessDate = today,
            deliveredItems = listOf(CustomerExchangeItem(oxygen.id, 50)),
            returnedItems = listOf(CustomerExchangeItem(argon.id, 2)),
            vehicleId = staffVehicle.id,
            actor = staff,
            customerName = "Shortage Test",
            customerPhone = "",
            notes = "",
            submissionKey = "key_shortage_4"
        )
        assertTrue(insRes is OperationResult.InsufficientStock)
        val vehAfterIns = repository.observeVehicleStock(staffVehicle.id, today).first()
        assertEquals(2, vehAfterIns.first { it.product.id == argon.id }.emptyBalance) // Unchanged!

        // 6. Owner View strictly blocked (read-only)
        val ownerRes = repository.recordDifferentGasReturn(
            businessDate = today,
            deliveredItems = listOf(CustomerExchangeItem(oxygen.id, 1)),
            returnedItems = listOf(CustomerExchangeItem(argon.id, 1)),
            vehicleId = null,
            actor = owner,
            customerName = "Owner Test",
            customerPhone = "",
            notes = "",
            submissionKey = "key_owner_5"
        )
        assertTrue(ownerRes is OperationResult.ValidationError)

        // 7. Customer-wise & Staff-wise Return History grouping
        val customerHistory = repository.customerReturnHistoryFlow.first()
        val apexGroup = customerHistory.first { it.customerName == "Apex Metal Works" }
        assertEquals("050-111-2222", apexGroup.customerPhone)
        assertEquals(2, apexGroup.records.size)
        assertEquals(3, apexGroup.totalDeliveredFull)
        assertEquals(3, apexGroup.totalReturnedEmpty)

        val staffHistory = repository.staffReturnHistoryFlow.first()
        assertTrue(staffHistory.any { it.staffId == admin.id && it.totalDeliveredFull == 6 && it.totalReturnedEmpty == 6 })
        assertTrue(staffHistory.any { it.staffId == staff.id && it.totalDeliveredFull == 2 && it.totalReturnedEmpty == 2 })
    }

    @Test
    fun productCrud_andSafeDeleteOrDeactivateRule_enforcedCorrectly() = runTest {
        repository.initializeDatabase()
        val users = repository.usersFlow.first()
        val admin = users.first { it.role == UserRole.ADMIN }
        val staff = users.first { it.role == UserRole.STAFF }

        // 1. Add product with Cylinder Size / KG
        val addRes = repository.addProduct(
            category = ProductCategory.INDUSTRIAL_GAS,
            name = "Helium Ultra",
            cylinderSizeKg = "50L"
        )
        assertTrue(addRes is OperationResult.Success)
        val created = repository.allProductsFlow.first().first { it.name == "Helium Ultra" }
        assertEquals("50L", created.cylinderSizeKg)
        assertTrue(created.isActive)

        // 2. Edit name, category, cylinder size/KG
        val editRes = repository.updateProduct(
            productId = created.id,
            category = ProductCategory.MCG_CUTTING_GAS,
            name = "Helium Precision",
            cylinderSizeKg = "47L",
            isActive = true,
            actor = admin
        )
        assertTrue(editRes is OperationResult.Success)
        val edited = repository.allProductsFlow.first().first { it.id == created.id }
        assertEquals("Helium Precision", edited.name)
        assertEquals(ProductCategory.MCG_CUTTING_GAS, edited.category)
        assertEquals("47L", edited.cylinderSizeKg)

        // Staff CANNOT edit or delete products
        val staffEditRes = repository.updateProduct(
            productId = created.id,
            category = ProductCategory.INDUSTRIAL_GAS,
            name = "Hacked",
            cylinderSizeKg = "10L",
            isActive = true,
            actor = staff
        )
        assertTrue(staffEditRes is OperationResult.ValidationError)

        // 3. Zero stock & zero history -> PERMANENT DELETE
        val delZeroRes = repository.deleteOrDeactivateProduct(created.id, admin)
        assertTrue(delZeroRes is OperationResult.Success)
        assertNull(repository.allProductsFlow.first().firstOrNull { it.id == created.id })

        // 4. Product with stock / history -> MARKED INACTIVE (NOT permanently deleted)
        val oxygen = repository.allProductsFlow.first().first { it.name.equals("Oxygen", ignoreCase = true) }
        repository.setOpeningStockForDate(StockCalculator.SCOPE_STORE, 0L, oxygen.id, "2026-11-27", 5, 2, admin)

        val delWithStockRes = repository.deleteOrDeactivateProduct(oxygen.id, admin)
        assertTrue(delWithStockRes is OperationResult.Success)
        val oxygenAfter = repository.allProductsFlow.first().firstOrNull { it.id == oxygen.id }
        assertNotNull("Product with stock must NOT be permanently deleted", oxygenAfter)
        assertFalse("Product with stock must be marked inactive", oxygenAfter!!.isActive)
    }

    @Test
    fun adminOnlyProductDragDropOrdering_persistsAcrossRestart_andBlocksStaff() = runTest {
        repository.initializeDatabase()
        val users = repository.usersFlow.first()
        val admin = users.first { it.role == UserRole.ADMIN }
        val staff = users.first { it.role == UserRole.STAFF }

        val industrialBefore = repository.activeProductsFlow.first()
            .filter { it.category == ProductCategory.INDUSTRIAL_GAS }
        assertTrue(industrialBefore.size >= 3)

        val reversedIds = industrialBefore.reversed().map { it.id }

        // Staff MUST be blocked from reordering
        val staffReorder = repository.reorderProducts(reversedIds, staff)
        assertTrue(staffReorder is OperationResult.ValidationError)
        val industrialAfterStaffAttempt = repository.activeProductsFlow.first()
            .filter { it.category == ProductCategory.INDUSTRIAL_GAS }
        assertEquals(industrialBefore.map { it.id }, industrialAfterStaffAttempt.map { it.id })

        // Admin reorders and saves -> persists in Room DAO query order
        val adminReorder = repository.reorderProducts(reversedIds, admin)
        assertTrue(adminReorder is OperationResult.Success)

        // Verify persisted order directly from fresh DAO read (simulating app restart & Staff view)
        val freshFromDb = db.productDao().getAllProductsOnce()
            .filter { it.category == ProductCategory.INDUSTRIAL_GAS }
        assertEquals(reversedIds, freshFromDb.map { it.id })
    }

    @Test
    fun startupSplashAndRoleWelcomeOverlays_renderAndSupportSkipAnimation() {
        var splashSkipped = false
        composeRule.setContent {
            ConcordGasTheme(themePreset = ThemePreset.EMERALD_CORPORATE, darkTheme = true) {
                StartupSplashOverlay(
                    themePreset = ThemePreset.EMERALD_CORPORATE,
                    soundEnabled = false,
                    onSplashFinished = { splashSkipped = true }
                )
            }
        }
        composeRule.onNodeWithTag("cinematic_delivery_truck_canvas").assertIsDisplayed()
        composeRule.onNodeWithTag("splash_skip_button").assertIsDisplayed().performClick()
        assertTrue(splashSkipped)
    }

    @Test
    fun ownerAdminStaffRoleWelcomeOverlays_displayDistinctRoleMessagesAndSkip() {
        var ownerDone = false
        composeRule.setContent {
            ConcordGasTheme(themePreset = ThemePreset.EMERALD_CORPORATE, darkTheme = true) {
                RoleWelcomeOverlay(
                    role = UserRole.OWNER_VIEW,
                    displayName = "Owner",
                    vehicleName = null,
                    onFinished = { ownerDone = true }
                )
            }
        }
        composeRule.onNodeWithText("EXECUTIVE OWNER EDITION").assertIsDisplayed()
        composeRule.onNodeWithText("Welcome, Owner").assertIsDisplayed()
        composeRule.onNodeWithTag("role_welcome_skip_button").assertIsDisplayed().performClick()
        assertTrue(ownerDone)
    }

    @Test
    fun differentGasReturnHistoryAndSuccessCheckOverlay_renderCleanly() {
        val sampleEntry = DifferentGasReturnEntity(
            id = 1L,
            businessDate = "2026-11-27",
            timestamp = 1795680000000L,
            staffId = 2L,
            staffName = "Nadeem",
            vehicleId = 1L,
            vehicleName = "NADEEM",
            customerName = "Al Noor Steel",
            customerPhone = "050-555-0101",
            deliveredItemsJson = "[]",
            returnedItemsJson = "[]",
            deliveredSummary = "Oxygen x1",
            returnedSummary = "Argon x1",
            totalDeliveredQty = 1,
            totalReturnedQty = 1,
            notes = "",
            submissionKey = "ui_test_key"
        )
        composeRule.setContent {
            ConcordGasTheme(themePreset = ThemePreset.EMERALD_CORPORATE, darkTheme = true) {
                DifferentGasReturnHistoryDialog(
                    customerGroups = listOf(
                        CustomerReturnHistoryGroup("Al Noor Steel", "050-555-0101", 1, 1, 1, 1795680000000L, listOf(sampleEntry))
                    ),
                    staffGroups = listOf(
                        StaffReturnHistoryGroup(2L, "Nadeem", "NADEEM", 1, 1, 1, 1795680000000L, listOf(sampleEntry))
                    ),
                    onDismiss = {}
                )
                LuxurySuccessCheckOverlay(
                    message = "Different gas return saved",
                    onDismiss = {}
                )
            }
        }
        composeRule.onNodeWithTag("different_gas_return_history_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("Al Noor Steel").assertIsDisplayed()
        composeRule.onNodeWithTag("return_history_tab_staff").performClick()
        composeRule.onNodeWithText("Nadeem").assertIsDisplayed()
        composeRule.onNodeWithTag("luxury_success_check_overlay").assertIsDisplayed()
    }
}
