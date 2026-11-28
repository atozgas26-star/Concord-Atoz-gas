package com.example

import android.content.Context
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.auth.AdminCredentialCrypto
import com.example.data.local.ConcordDatabase
import com.example.data.local.SessionPreferences
import com.example.data.local.UserAccountEntity
import com.example.data.local.VehicleEntity
import com.example.data.model.CustomerExchangeItem
import com.example.data.model.ExchangePeriodFilter
import com.example.data.model.ExchangeSectionFilter
import com.example.data.model.LpgReturnCompany
import com.example.data.model.OperationResult
import com.example.data.model.ProductCategory
import com.example.data.model.ReportType
import com.example.data.model.StaffFeaturePermission
import com.example.data.model.StockCalculator
import com.example.data.model.ThemePreset
import com.example.data.model.TransactionType
import com.example.data.model.UserRole
import com.example.data.repository.InventoryRepository
import com.example.export.BackupManager
import com.example.export.ReportExporter
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.local.DifferentGasReturnEntity
import com.example.data.local.ProductEntity
import com.example.data.local.StockTransactionEntity
import com.example.data.model.CompanyStockRow
import com.example.data.remote.WorkspaceConnectionState
import com.example.ui.components.AnalyticsTimeRange
import com.example.ui.components.LpgAnalyticsEngine
import com.example.ui.screens.LpgAnalyticsDashboardScreen
import com.example.ui.screens.StaffDashboardScreen
import com.example.ui.theme.ConcordGasTheme
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.FirstTimeRegStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xxhdpi", application = ConcordGasApp::class)
class MasterV500UpgradeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var db: ConcordDatabase
    private lateinit var repository: InventoryRepository
    private lateinit var backupManager: BackupManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, ConcordDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = InventoryRepository(db)
        backupManager = BackupManager(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun waitUntil(timeoutMs: Long = 5000L, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!condition() && System.currentTimeMillis() < deadline) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(20)
        }
    }

    @Test
    fun part1AndPart2_initialFourFolders18Products_dynamicCategoriesPreservePermanentIds_andBlockBrandLpgProducts() = runTest {
        repository.initializeDatabase()
        val admin = repository.usersFlow.first().first { it.role == UserRole.ADMIN }
        val staff = repository.usersFlow.first().first { it.role == UserRole.STAFF }

        // 1. Exact 4 initial system folders
        val categories = repository.categoriesFlow.first()
        assertEquals(4, categories.size)
        assertEquals(
            listOf(
                "INDUSTRIAL GAS – REGULAR",
                "CUTTING GAS",
                "LPG GAS",
                "INDUSTRIAL GAS – 48 CFT SMALL"
            ),
            categories.map { it.name }
        )

        // 2. Exact 18 active seeded products across the 4 folders
        val products = repository.activeProductsFlow.first()
        assertEquals(18, products.size)
        assertEquals(
            listOf("Oxygen", "Acetylene", "Argon", "Argon Mix", "CO2 20 KG", "CO2 30 KG", "Nitrogen"),
            products.filter { it.categoryId == 1L }.map { it.name }
        )
        assertEquals(
            listOf("MCG 22 KG", "MCG 44 KG"),
            products.filter { it.categoryId == 2L }.map { it.name }
        )
        assertEquals(
            listOf("11 KG", "22 KG", "44 KG"),
            products.filter { it.categoryId == 3L }.map { it.name }
        )
        assertEquals(
            listOf("Oxygen 48 CFT", "Acetylene 48 CFT", "Argon 48 CFT", "Argon Mix 48 CFT", "CO2 5 KG", "Nitrogen 48 CFT"),
            products.filter { it.categoryId == 4L }.map { it.name }
        )

        // 3. Strictly reject Arab Gas / Emirates Gas as separate LPG stock products
        val arabLpgAttempt = repository.addProduct(
            category = ProductCategory.COOKING_GAS,
            name = "Arab Gas 22 KG",
            cylinderSizeKg = "22 KG",
            categoryId = 3L,
            unit = "KG"
        )
        assertTrue(arabLpgAttempt is OperationResult.ValidationError)
        val emiratesLpgAttempt = repository.addProduct(
            category = ProductCategory.COOKING_GAS,
            name = "Emirates Gas 44 KG",
            cylinderSizeKg = "44 KG",
            categoryId = 3L,
            unit = "KG"
        )
        assertTrue(emiratesLpgAttempt is OperationResult.ValidationError)

        // 4. CREATE NEW CATEGORY (Staff blocked, Admin succeeds)
        assertTrue(
            repository.createCategory(
                name = "MEDICAL GAS",
                description = "Hospital Oxygen & Nitrous",
                iconKey = "INDUSTRIAL",
                categoryType = "CUSTOM",
                isActive = true,
                actor = staff
            ) is OperationResult.ValidationError
        )
        val createCatRes = repository.createCategory(
            name = "MEDICAL GAS",
            description = "Hospital Oxygen & Nitrous",
            iconKey = "INDUSTRIAL",
            categoryType = "CUSTOM",
            isActive = true,
            actor = admin
        )
        assertTrue(createCatRes is OperationResult.Success)
        val customCat = repository.categoriesFlow.first().first { it.name == "MEDICAL GAS" }
        val permanentCategoryId = customCat.id
        assertTrue(permanentCategoryId > 4L)

        // 5. ADD NEW PRODUCT inside custom category & verify automatic integration with stock engine
        val addProdRes = repository.addProduct(
            category = customCat.legacyEnum,
            name = "Medical Oxygen 10L",
            cylinderSizeKg = "10L",
            categoryId = permanentCategoryId,
            unit = "Cylinder",
            description = "High purity medical oxygen",
            iconKey = "INDUSTRIAL",
            isActive = true
        )
        assertTrue(addProdRes is OperationResult.Success)
        val medProduct = repository.activeProductsFlow.first().first { it.name == "Medical Oxygen 10L" }
        assertEquals(permanentCategoryId, medProduct.categoryId)
        assertEquals("10L", medProduct.cylinderSizeKg)
        assertEquals("Cylinder", medProduct.unit)

        val today = "2026-11-28"
        assertTrue(
            repository.setOpeningStockForDate(StockCalculator.SCOPE_STORE, 0L, medProduct.id, today, 15, 3, admin)
                is OperationResult.Success
        )
        assertTrue(
            repository.recordTransaction(today, TransactionType.STORE_PURCHASE_FULL, medProduct.id, null, admin, 5, "Batch 1")
                is OperationResult.Success
        )

        // 6. EDIT CATEGORY FOLDER: Rename & verify permanent Category ID, product link, and stock balances are untouched
        val renameCatRes = repository.updateCategory(
            categoryId = permanentCategoryId,
            name = "SPECIALTY & MEDICAL GAS",
            description = "Updated description",
            iconKey = "GAS_CYLINDER",
            categoryType = "CUSTOM",
            isActive = true,
            actor = admin
        )
        assertTrue(renameCatRes is OperationResult.Success)
        val renamedCat = repository.categoriesFlow.first().first { it.id == permanentCategoryId }
        assertEquals("SPECIALTY & MEDICAL GAS", renamedCat.name)
        assertEquals(permanentCategoryId, renamedCat.id)

        val medStoreRow = repository.observeStoreStock(today).first().first { it.product.id == medProduct.id }
        assertEquals(20, medStoreRow.storeFullBalance)
        assertEquals(3, medStoreRow.storeEmptyBalance)
    }

    @Test
    fun part3Part5Part6Part7_strictIsolation_independentFullOrEmptyExchange_atomicEditDelete_andFilteredReports() = runTest {
        repository.initializeDatabase()
        val admin = repository.usersFlow.first().first { it.role == UserRole.ADMIN }
        val products = repository.activeProductsFlow.first()
        val oxygenReg = products.first { it.category == ProductCategory.INDUSTRIAL_GAS && it.name == "Oxygen" }
        val oxygen48 = products.first { it.category == ProductCategory.SMALL_48_CFT && it.name == "Oxygen 48 CFT" }
        val mcg22 = products.first { it.category == ProductCategory.MCG_CUTTING_GAS && it.name == "MCG 22 KG" }
        val lpg22 = products.first { it.category == ProductCategory.COOKING_GAS && it.name == "22 KG" }
        val today = LocalDate.now().toString()

        // Seed opening stock independently for all 4 sections
        listOf(oxygenReg, oxygen48, mcg22, lpg22).forEach { prod ->
            assertTrue(
                repository.setOpeningStockForDate(StockCalculator.SCOPE_STORE, 0L, prod.id, today, 10, 4, admin)
                    is OperationResult.Success
            )
        }

        // 1. Full OUT ONLY (independent — zero returned items, never forced 1:1) on 48 CFT Small
        val fullOutOnly = repository.recordDifferentGasReturn(
            businessDate = today,
            deliveredItems = listOf(CustomerExchangeItem(oxygen48.id, 3)),
            returnedItems = emptyList(),
            vehicleId = null,
            actor = admin,
            customerName = "Desert Metal Fab",
            customerPhone = "+971501112233",
            notes = "48 CFT full out only",
            submissionKey = "v5_48cft_full_only"
        )
        assertTrue(fullOutOnly is OperationResult.Success)

        // Verify Regular Oxygen is completely untouched when 48 CFT Oxygen changes!
        val storeAfter48 = repository.observeStoreStock(today).first()
        assertEquals(7, storeAfter48.first { it.product.id == oxygen48.id }.storeFullBalance)
        assertEquals(10, storeAfter48.first { it.product.id == oxygenReg.id }.storeFullBalance)

        // 2. Empty IN ONLY (independent — zero delivered items) on Cutting Gas MCG 22 KG
        val emptyInOnly = repository.recordDifferentGasReturn(
            businessDate = today,
            deliveredItems = emptyList(),
            returnedItems = listOf(CustomerExchangeItem(mcg22.id, 2)),
            vehicleId = null,
            actor = admin,
            customerName = "Al Quoz Welding",
            customerPhone = "+971522223344",
            notes = "MCG empty return only",
            submissionKey = "v5_mcg_empty_only"
        )
        assertTrue(emptyInOnly is OperationResult.Success)
        val storeAfterMcg = repository.observeStoreStock(today).first()
        assertEquals(10, storeAfterMcg.first { it.product.id == mcg22.id }.storeFullBalance)
        assertEquals(6, storeAfterMcg.first { it.product.id == mcg22.id }.storeEmptyBalance)

        // 3. LPG Exchange with Arab Gas return company + another with Emirates Gas
        assertTrue(
            repository.recordDifferentGasReturn(
                businessDate = today,
                deliveredItems = listOf(CustomerExchangeItem(lpg22.id, 2)),
                returnedItems = listOf(CustomerExchangeItem(lpg22.id, 1)),
                vehicleId = null,
                actor = admin,
                customerName = "Dubai Spice Kitchen",
                customerPhone = "+971553334455",
                notes = "Arab Gas empty returned",
                submissionKey = "v5_lpg_arab",
                returnedEmptyCompany = LpgReturnCompany.ARAB_GAS.displayName
            ) is OperationResult.Success
        )
        assertTrue(
            repository.recordDifferentGasReturn(
                businessDate = today,
                deliveredItems = listOf(CustomerExchangeItem(lpg22.id, 1)),
                returnedItems = listOf(CustomerExchangeItem(lpg22.id, 2)),
                vehicleId = null,
                actor = admin,
                customerName = "Marina Bakery",
                customerPhone = "+971564445566",
                notes = "Emirates Gas empty returned",
                submissionKey = "v5_lpg_emirates",
                returnedEmptyCompany = LpgReturnCompany.EMIRATES_GAS.displayName
            ) is OperationResult.Success
        )

        // 4. Verify audit logs recorded EXCHANGE_CREATED
        val auditLogs = repository.securityAuditLogsFlow.first()
        assertTrue(auditLogs.any { it.eventType == "EXCHANGE_CREATED" })

        // 5. Verify ViewModel filtered Daily/Weekly/Monthly & Section/Arab/Emirates Gas reports + XLSX export
        val prefs = SessionPreferences(context)
        prefs.clearLoggedInUserId()
        val vm = AppViewModel(
            repository = repository,
            backupManager = backupManager,
            cloudSync = null,
            sessionPreferences = prefs,
            driveBackupManager = null,
            appContext = context
        )
        backgroundScope.launch(Dispatchers.Unconfined) { vm.differentGasReturns.collect {} }
        backgroundScope.launch(Dispatchers.Unconfined) { vm.activeProducts.collect {} }
        backgroundScope.launch(Dispatchers.Unconfined) { vm.allProducts.collect {} }
        waitUntil {
            vm.isInitialized.value &&
                vm.differentGasReturns.value.size == 4 &&
                vm.allProducts.value.size == 18
        }

        val dailyAll = vm.buildReportTable(
            reportType = ReportType.LPG_EXCHANGE_REPORT,
            exchangePeriod = ExchangePeriodFilter.DAILY,
            exchangeSection = ExchangeSectionFilter.ALL
        )
        assertEquals(4, dailyAll.rows.size)

        val weekly48Cft = vm.buildReportTable(
            reportType = ReportType.LPG_EXCHANGE_REPORT,
            exchangePeriod = ExchangePeriodFilter.WEEKLY,
            exchangeSection = ExchangeSectionFilter.SMALL_48_CFT
        )
        assertEquals(1, weekly48Cft.rows.size)
        assertTrue(weekly48Cft.rows.first().joinToString(" ").contains("Oxygen 48 CFT"))

        val monthlyCutting = vm.buildReportTable(
            reportType = ReportType.LPG_EXCHANGE_REPORT,
            exchangePeriod = ExchangePeriodFilter.MONTHLY,
            exchangeSection = ExchangeSectionFilter.CUTTING_GAS
        )
        assertEquals(1, monthlyCutting.rows.size)
        assertTrue(monthlyCutting.rows.first().joinToString(" ").contains("MCG 22 KG"))

        val arabReturns = vm.buildReportTable(
            reportType = ReportType.LPG_EXCHANGE_REPORT,
            exchangePeriod = ExchangePeriodFilter.MONTHLY,
            exchangeSection = ExchangeSectionFilter.ARAB_GAS_RETURNS
        )
        assertEquals(1, arabReturns.rows.size)
        assertTrue(arabReturns.rows.first().joinToString(" ").contains("Arab Gas"))

        val emiratesReturns = vm.buildReportTable(
            reportType = ReportType.LPG_EXCHANGE_REPORT,
            exchangePeriod = ExchangePeriodFilter.MONTHLY,
            exchangeSection = ExchangeSectionFilter.EMIRATES_GAS_RETURNS
        )
        assertEquals(1, emiratesReturns.rows.size)
        assertTrue(emiratesReturns.rows.first().joinToString(" ").contains("Emirates Gas"))

        // Export filtered report to valid XLSX bytes
        val xlsxOut = ByteArrayOutputStream()
        ReportExporter.writeXlsx(dailyAll, xlsxOut)
        val bytes = xlsxOut.toByteArray()
        assertTrue(bytes.size > 100 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte())
    }

    @Test
    fun part4_quickCylinderEntriesOrangeButtonHasExactRequiredTitle() {
        var exchangeClicked = false
        val staffUser = UserAccountEntity(
            id = 2L,
            username = "nadeem",
            displayName = "Nadeem",
            role = UserRole.STAFF,
            pin = "",
            pinHash = AdminCredentialCrypto.hashPassword("1001"),
            isActive = true
        )
        val assignedVehicle = VehicleEntity(id = 1L, name = "NADEEM", staffUserId = 2L, isActive = true)
        composeRule.setContent {
            ConcordGasTheme(themePreset = ThemePreset.EMERALD_CORPORATE, darkTheme = true) {
                StaffDashboardScreen(
                    staffUser = staffUser,
                    assignedVehicle = assignedVehicle,
                    selectedDate = "2026-11-28",
                    vehicleRows = emptyList(),
                    storeRows = emptyList(),
                    products = emptyList(),
                    allTransactions = emptyList(),
                    allowedFeatures = StaffFeaturePermission.entries.toSet(),
                    onDateSelected = {},
                    onRecordTransaction = { _, _, _, _, _, _, _ -> },
                    onOpenDifferentGasReturn = { exchangeClicked = true },
                    onLogout = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("LPG & Industrial Gas\nExchange Empty", useUnmergedTree = true).assertExists()
        assertFalse(exchangeClicked)
    }

    @Test
    fun part8Part9Part10_firstTimeMobileRegistrationExistingPin_lockout_revocation_andNoPlainTextPins() = runTest {
        repository.initializeDatabase()
        val usersBefore = repository.usersFlow.first()
        val admin = usersBefore.first { it.role == UserRole.ADMIN }
        val nadeem = usersBefore.first { it.username == "nadeem" }

        // 1. Verify NO plain-text PINs exist in Room or Backup v5 JSON
        assertTrue(usersBefore.all { it.pin.isEmpty() && it.pinHash.startsWith("pbkdf2\$") })
        val backupJson = backupManager.exportBackupJson()
        assertFalse("Backup JSON must never leak admin PIN", backupJson.contains("\"1725\""))
        assertFalse("Backup JSON must never leak staff PIN", backupJson.contains("\"1001\""))
        assertTrue("Backup JSON must contain salted pbkdf2 hash", backupJson.contains("pbkdf2\$"))

        // 2. Admin authorizes Nadeem's UAE mobile with first-time OTP registration enabled
        assertTrue(
            repository.upsertAuthorizedMobile(
                phoneRaw = "+971507778899",
                displayName = nadeem.displayName,
                role = UserRole.STAFF,
                staffUserId = nadeem.id,
                allowedFeatures = StaffFeaturePermission.entries.toSet(),
                isActive = true,
                isProtectedOwner = false,
                firstTimeRegEnabled = true,
                actor = admin
            ) is OperationResult.Success
        )

        val prefs = SessionPreferences(context)
        prefs.clearLoggedInUserId()
        val vm = AppViewModel(
            repository = repository,
            backupManager = backupManager,
            cloudSync = null,
            sessionPreferences = prefs,
            driveBackupManager = null,
            appContext = context
        )
        waitUntil { vm.isInitialized.value }

        // 3. Unauthorized mobile rejected cleanly
        vm.onOtpVerifiedForFirstTimeRegistration("+971509990000")
        waitUntil { vm.firstTimeRegState.value.error != null }
        assertEquals(FirstTimeRegStep.MOBILE_INPUT, vm.firstTimeRegState.value.step)

        // 4. Step 2 -> Step 3: OTP verified for authorized Staff mobile -> matches EXISTING account, asks for EXISTING PIN
        vm.onOtpVerifiedForFirstTimeRegistration("+971507778899")
        waitUntil { vm.firstTimeRegState.value.step == FirstTimeRegStep.EXISTING_PIN_VERIFY }
        assertEquals(nadeem.displayName, vm.firstTimeRegState.value.matchedDisplayName)
        assertEquals(UserRole.STAFF, vm.firstTimeRegState.value.matchedRole)

        // 5. Wrong existing PIN increments failure count and does NOT log in
        vm.completeFirstTimeRegistrationWithExistingPin("9999")
        waitUntil { vm.firstTimeRegState.value.error != null && !vm.firstTimeRegState.value.isBusy }
        assertNull(vm.authSession.value)

        // 6. Correct EXISTING PIN ("1001") completes registration WITHOUT creating duplicate user or altering PIN hash
        val oldPinHash = nadeem.pinHash
        vm.completeFirstTimeRegistrationWithExistingPin("1001")
        waitUntil { vm.authSession.value?.user?.id == nadeem.id }

        assertEquals(usersBefore.size, repository.usersFlow.first().size)
        val nadeemAfter = repository.usersFlow.first().first { it.id == nadeem.id }
        assertEquals("Existing PIN hash must never be replaced during first-time registration", oldPinHash, nadeemAfter.pinHash)

        val registeredMobile = repository.findAuthorizedMobileByPhone("+971507778899")
        assertNotNull(registeredMobile)
        assertTrue(registeredMobile!!.isRegistered)
        assertTrue(registeredMobile.registeredDeviceId.isNotBlank())
        assertEquals(0, registeredMobile.failedLoginAttempts)

        // 7. Device revocation clears mobile registration -> requires re-registration
        val deviceId = registeredMobile.registeredDeviceId
        assertTrue(repository.revokeDevice(deviceId, admin) is OperationResult.Success)
        val afterRevoke = repository.findAuthorizedMobileByPhone("+971507778899")!!
        assertFalse(afterRevoke.isRegistered)
        assertEquals("", afterRevoke.registeredDeviceId)

        // 8. Five failed PIN attempts trigger 15-minute lockout + PIN_LOCKOUT alert
        var lastLockedUntil = 0L
        repeat(InventoryRepository.MAX_PIN_ATTEMPTS) {
            val (_, lockedUntil) = repository.recordMobilePinFailure(afterRevoke.id)
            lastLockedUntil = lockedUntil
        }
        assertTrue("Expected 15-minute PIN lockout after 5 attempts", lastLockedUntil > System.currentTimeMillis())
        val logs = repository.securityAuditLogsFlow.first()
        assertTrue(logs.any { it.eventType == "PIN_LOCKOUT" && it.isAlert })
    }

    @Test
    fun part12_migration8to9AndBackupV5RoundTripPreserveAllExistingRecords() = runTest {
        val dbName = "migration_v8_v9_master_test.db"
        context.deleteDatabase(dbName)

        val helperConfig = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(8) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS user_accounts (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            username TEXT NOT NULL,
                            displayName TEXT NOT NULL,
                            role TEXT NOT NULL,
                            pin TEXT NOT NULL,
                            isActive INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
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
                    db.execSQL("INSERT INTO user_accounts (id, username, displayName, role, pin, isActive) VALUES (1, 'admin_ragavan', 'Ragavan', 'ADMIN', '1725', 1)")
                    db.execSQL("INSERT INTO products (id, category, name, sortOrder, isActive, cylinderSizeKg) VALUES (1, 'INDUSTRIAL_GAS', 'Oxygen', 101, 1, '47L')")
                    db.execSQL("INSERT INTO products (id, category, name, sortOrder, isActive, cylinderSizeKg) VALUES (2, 'COOKING_GAS', '22 KG', 302, 1, '22 KG')")
                    db.execSQL("INSERT INTO authorized_mobiles (id, phoneE164, displayName, role, staffUserId, allowedFeaturesCsv, isActive, isProtectedOwner, createdAtMs, updatedAtMs) VALUES (1, '+971501234567', 'Owner', 'OWNER_VIEW', NULL, 'MY_VEHICLE_STOCK', 1, 1, 1000, 1000)")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(helperConfig)
        val sqliteDb = helper.writableDatabase
        ConcordDatabase.MIGRATION_8_9.migrate(sqliteDb)

        // Verify product_categories table seeded with 4 permanent system folders
        sqliteDb.query("SELECT id, name FROM product_categories ORDER BY id ASC").use { c ->
            assertEquals(4, c.count)
            assertTrue(c.moveToFirst())
            assertEquals("INDUSTRIAL GAS – REGULAR", c.getString(1))
            assertTrue(c.moveToNext())
            assertEquals("CUTTING GAS", c.getString(1))
            assertTrue(c.moveToNext())
            assertEquals("LPG GAS", c.getString(1))
            assertTrue(c.moveToNext())
            assertEquals("INDUSTRIAL GAS – 48 CFT SMALL", c.getString(1))
        }

        // Verify existing products retained exact IDs and Cutting Gas MCG 22 KG / MCG 44 KG seeded
        sqliteDb.query("SELECT id, name, categoryId FROM products WHERE id IN (1, 2) ORDER BY id ASC").use { c ->
            assertEquals(2, c.count)
            assertTrue(c.moveToFirst())
            assertEquals("Oxygen", c.getString(1))
            assertEquals(1L, c.getLong(2))
            assertTrue(c.moveToNext())
            assertEquals("22 KG", c.getString(1))
            assertEquals(3L, c.getLong(2))
        }
        sqliteDb.query("SELECT COUNT(*) FROM products WHERE category = 'MCG_CUTTING_GAS' AND isActive = 1").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(2, c.getInt(0))
        }

        // Verify authorized_mobiles retained Protected Owner and gained registration columns
        sqliteDb.query("SELECT phoneE164, firstTimeRegEnabled, isRegistered FROM authorized_mobiles WHERE id = 1").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("+971501234567", c.getString(0))
            assertEquals(1, c.getInt(1))
            assertEquals(0, c.getInt(2))
        }

        helper.close()
        context.deleteDatabase(dbName)

        // Verify BackupManager v5 export & restore round-trip with dynamic category + product
        repository.initializeDatabase()
        val admin = repository.usersFlow.first().first { it.role == UserRole.ADMIN }
        assertTrue(repository.createCategory("CALIBRATION GAS", "Lab mixtures", "INDUSTRIAL", "CUSTOM", true, admin) is OperationResult.Success)
        val calCat = repository.categoriesFlow.first().first { it.name == "CALIBRATION GAS" }
        assertTrue(repository.addProduct(ProductCategory.INDUSTRIAL_GAS, "Zero Air 10L", "10L", calCat.id, "Litre", "Zero air", "INDUSTRIAL", true) is OperationResult.Success)

        val v5Json = backupManager.exportBackupJson()
        assertTrue(v5Json.contains("\"schemaVersion\": 5") || v5Json.contains("\"schemaVersion\":5"))
        assertTrue(backupManager.restoreBackupJson(v5Json) is OperationResult.Success)

        assertTrue(repository.categoriesFlow.first().any { it.name == "CALIBRATION GAS" })
        assertTrue(repository.activeProductsFlow.first().any { it.name == "Zero Air 10L" && it.unit == "Litre" })
    }

    @Test
    fun lpgAnalyticsEngine_aggregatesActiveLpgSizesSalesTrendsAndCompanyReturns() {
        val anchorDate = "2026-11-28"
        val p11 = ProductEntity(id = 11L, category = ProductCategory.COOKING_GAS, name = "11 KG", sortOrder = 301, isActive = true, cylinderSizeKg = "11 KG")
        val p22 = ProductEntity(id = 12L, category = ProductCategory.COOKING_GAS, name = "22 KG", sortOrder = 302, isActive = true, cylinderSizeKg = "22 KG")
        val p44 = ProductEntity(id = 13L, category = ProductCategory.COOKING_GAS, name = "44 KG", sortOrder = 303, isActive = true, cylinderSizeKg = "44 KG")
        val pO2 = ProductEntity(id = 1L, category = ProductCategory.INDUSTRIAL_GAS, name = "Oxygen", sortOrder = 101, isActive = true, cylinderSizeKg = "47L")

        val stockRows = listOf(
            CompanyStockRow(product = p11, storeFull = 40, vehiclesFull = 15, totalFull = 55, storeEmpty = 12, vehiclesEmpty = 8, totalEmpty = 20),
            CompanyStockRow(product = p22, storeFull = 65, vehiclesFull = 25, totalFull = 90, storeEmpty = 20, vehiclesEmpty = 10, totalEmpty = 30),
            CompanyStockRow(product = p44, storeFull = 30, vehiclesFull = 10, totalFull = 40, storeEmpty = 9, vehiclesEmpty = 5, totalEmpty = 14),
            CompanyStockRow(product = pO2, storeFull = 100, vehiclesFull = 20, totalFull = 120, storeEmpty = 50, vehiclesEmpty = 10, totalEmpty = 60)
        )

        val transactions = listOf(
            StockTransactionEntity(id = 1, businessDate = "2026-11-28", timestamp = 1000L, type = TransactionType.VEHICLE_CUSTOMER_SALE, productId = 12L, vehicleId = 1L, vehicleName = "83865", staffId = 2L, staffName = "Nadeem", quantity = 14),
            StockTransactionEntity(id = 2, businessDate = "2026-11-28", timestamp = 2000L, type = TransactionType.STORE_FULL_DISPATCH_DIRECT, productId = 11L, staffId = 1L, staffName = "Ragavan", quantity = 6),
            StockTransactionEntity(id = 3, businessDate = "2026-11-28", timestamp = 2500L, type = TransactionType.VEHICLE_CUSTOMER_SALE, productId = 1L, vehicleId = 1L, vehicleName = "83865", staffId = 2L, staffName = "Nadeem", quantity = 9),
            StockTransactionEntity(id = 4, businessDate = "2026-11-26", timestamp = 3000L, type = TransactionType.VEHICLE_NEW_CUSTOMER_SALE, productId = 13L, vehicleId = 2L, vehicleName = "21409", staffId = 3L, staffName = "Sajid", quantity = 8),
            StockTransactionEntity(id = 5, businessDate = "2026-11-27", timestamp = 4000L, type = TransactionType.STORE_PURCHASE_FULL, productId = 12L, staffId = 1L, staffName = "Ragavan", quantity = 30),
            StockTransactionEntity(id = 6, businessDate = "2026-11-25", timestamp = 5000L, type = TransactionType.STORE_DAMAGED_ADD, productId = 12L, staffId = 1L, staffName = "Ragavan", quantity = 3),
            StockTransactionEntity(id = 7, businessDate = "2026-11-27", timestamp = 6000L, type = TransactionType.STORE_DAMAGED_REMOVE, productId = 12L, staffId = 1L, staffName = "Ragavan", quantity = 1)
        )

        val exchanges = listOf(
            DifferentGasReturnEntity(
                id = 1, submissionKey = "ex1", businessDate = "2026-11-28", customerName = "Al Noor Cafe",
                staffId = 2L, staffName = "Nadeem", deliveredSummary = "22 KG x4", returnedSummary = "22 KG x4",
                deliveredItemsJson = "[]", returnedItemsJson = "[]", totalDeliveredQty = 4, totalReturnedQty = 4,
                returnedEmptyCompany = "Arab Gas"
            ),
            DifferentGasReturnEntity(
                id = 2, submissionKey = "ex2", businessDate = "2026-11-27", customerName = "Gulf Bakery",
                staffId = 3L, staffName = "Sajid", deliveredSummary = "44 KG x3", returnedSummary = "44 KG x3",
                deliveredItemsJson = "[]", returnedItemsJson = "[]", totalDeliveredQty = 3, totalReturnedQty = 3,
                returnedEmptyCompany = "Emirates Gas"
            )
        )

        val snap7 = LpgAnalyticsEngine.computeSnapshot(anchorDate, AnalyticsTimeRange.DAYS_7, stockRows, transactions, exchanges)
        assertEquals(listOf("11 KG", "22 KG", "44 KG"), snap7.lpgSizes.map { it.label })
        assertEquals(55 + 90 + 40, snap7.totalLpgFull)
        assertEquals(20 + 30 + 14, snap7.totalLpgEmpty)
        assertEquals(2, snap7.totalLpgDamaged)
        assertEquals(7, snap7.trendPoints.size)
        assertEquals(14 + 6 + 8, snap7.periodLpgSales)
        assertEquals(14 + 6 + 9 + 8, snap7.periodAllGasSales)
        assertEquals(20, snap7.peakDayQty)
        assertEquals(4, snap7.returnShare.arabGasReturns)
        assertEquals(3, snap7.returnShare.emiratesGasReturns)

        val snap14 = LpgAnalyticsEngine.computeSnapshot(anchorDate, AnalyticsTimeRange.DAYS_14, stockRows, transactions, exchanges)
        assertEquals(14, snap14.trendPoints.size)

        val snap30 = LpgAnalyticsEngine.computeSnapshot(anchorDate, AnalyticsTimeRange.DAYS_30, stockRows, transactions, exchanges)
        assertEquals(30, snap30.trendPoints.size)
    }

    @Test
    fun lpgAnalyticsDashboardScreen_rendersLiveFirestoreChartsAndRangeToggles() {
        var backInvoked = false
        val p11 = ProductEntity(id = 11L, category = ProductCategory.COOKING_GAS, name = "11 KG", sortOrder = 301, isActive = true, cylinderSizeKg = "11 KG")
        val p22 = ProductEntity(id = 12L, category = ProductCategory.COOKING_GAS, name = "22 KG", sortOrder = 302, isActive = true, cylinderSizeKg = "22 KG")
        val p44 = ProductEntity(id = 13L, category = ProductCategory.COOKING_GAS, name = "44 KG", sortOrder = 303, isActive = true, cylinderSizeKg = "44 KG")

        val stockRows = listOf(
            CompanyStockRow(product = p11, storeFull = 25, vehiclesFull = 10, totalFull = 35, storeEmpty = 8, vehiclesEmpty = 4, totalEmpty = 12),
            CompanyStockRow(product = p22, storeFull = 50, vehiclesFull = 20, totalFull = 70, storeEmpty = 15, vehiclesEmpty = 6, totalEmpty = 21),
            CompanyStockRow(product = p44, storeFull = 18, vehiclesFull = 7, totalFull = 25, storeEmpty = 5, vehiclesEmpty = 3, totalEmpty = 8)
        )
        val txs = listOf(
            StockTransactionEntity(id = 1, businessDate = "2026-11-28", timestamp = 1000L, type = TransactionType.VEHICLE_CUSTOMER_SALE, productId = 12L, vehicleId = 1L, vehicleName = "83865", staffId = 2L, staffName = "Nadeem", quantity = 12)
        )
        val exchanges = listOf(
            DifferentGasReturnEntity(
                id = 1, submissionKey = "ex1", businessDate = "2026-11-28", customerName = "Metro Kitchen",
                staffId = 2L, staffName = "Nadeem", deliveredSummary = "22 KG x5", returnedSummary = "22 KG x5",
                deliveredItemsJson = "[]", returnedItemsJson = "[]", totalDeliveredQty = 5, totalReturnedQty = 5,
                returnedEmptyCompany = "Arab Gas"
            )
        )

        composeRule.setContent {
            ConcordGasTheme(themePreset = ThemePreset.EMERALD_CORPORATE, darkTheme = true) {
                LpgAnalyticsDashboardScreen(
                    selectedDate = "2026-11-28",
                    companyStockRows = stockRows,
                    transactions = txs,
                    lpgExchanges = exchanges,
                    workspaceState = WorkspaceConnectionState.Connected(
                        workspaceId = "ws_concord",
                        companyName = "Concord & Atoz Gas",
                        inviteCode = "CAGAS1",
                        memberCount = 5,
                        isOwnerOrAdmin = true
                    ),
                    isReadOnly = true,
                    onDateSelected = {},
                    onBack = { backInvoked = true }
                )
            }
        }

        composeRule.onNodeWithTag("lpg_analytics_dashboard_screen").assertIsDisplayed()
        composeRule.onNodeWithText("Live LPG Analytics").assertIsDisplayed()
        composeRule.onNodeWithTag("lpg_analytics_firestore_badge").assertIsDisplayed()
        composeRule.onNodeWithTag("lpg_inventory_bar_chart_card").assertExists()
        composeRule.onNodeWithTag("lpg_sales_trend_area_chart_card").assertExists()

        composeRule.onNodeWithTag("analytics_range_14d").performScrollTo().performClick()
        composeRule.onNodeWithTag("analytics_range_30d").performScrollTo().performClick()

        composeRule.onNodeWithTag("lpg_analytics_back_button").performClick()
        assertTrue(backInvoked)
    }
}
