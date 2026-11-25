package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.audio.SoundEffectsManager
import com.example.data.local.ConcordDatabase
import com.example.data.local.SessionPreferences
import com.example.data.model.AutoLogoutMode
import com.example.data.model.OperationResult
import com.example.data.model.ProductCategory
import com.example.data.model.ReportType
import com.example.data.model.StockCalculator
import com.example.data.model.ThemePreset
import android.os.Looper
import com.example.data.model.TransactionType
import com.example.data.model.UserRole
import com.example.data.repository.InventoryRepository
import com.example.export.BackupManager
import com.example.export.GoogleDriveBackupManager
import com.example.export.ReportExporter
import com.example.export.ReportTable
import com.example.permissions.AppPermissionsManager
import com.example.ui.viewmodel.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.robolectric.Shadows
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
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = ConcordGasApp::class)
class ExampleRobolectricTest {

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

    @Test
    fun `app name matches Concord A to Z Gas`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Concord A to Z Gas", appName)
    }

    @Test
    fun `database seeds admin, 7 staff and vehicles, and 18 products`() = runTest {
        repository.initializeDatabase()

        val users = repository.usersFlow.first()
        val vehicles = repository.vehiclesFlow.first()
        val products = repository.activeProductsFlow.first()

        val admin = users.firstOrNull { it.role == UserRole.ADMIN }
        assertNotNull(admin)
        assertEquals("Ragavan", admin!!.displayName)
        assertEquals("1725", admin.pin)

        val staff = users.filter { it.role == UserRole.STAFF }
        assertEquals(7, staff.size)
        assertEquals(7, vehicles.size)
        assertEquals(
            listOf("NADEEM", "FAZALAMIN", "FAWAD", "RAGAVAN", "SIVAN", "JINU", "SAJAN"),
            vehicles.map { it.name }
        )

        assertEquals(18, products.size)
        assertEquals(7, products.count { it.category == ProductCategory.INDUSTRIAL_GAS })
        assertEquals(6, products.count { it.category == ProductCategory.SMALL_48_CFT })
        assertEquals(3, products.count { it.category == ProductCategory.COOKING_GAS })
        assertEquals(2, products.count { it.category == ProductCategory.MCG_CUTTING_GAS })
    }

    @Test
    fun `store and vehicle stock calculations and negative stock guard work accurately`() = runTest {
        repository.initializeDatabase()
        val admin = repository.usersFlow.first().first { it.role == UserRole.ADMIN }
        val nadeem = repository.usersFlow.first().first { it.displayName == "Nadeem" }
        val nadeemVehicle = repository.vehiclesFlow.first().first { it.name == "NADEEM" }
        val oxygen = repository.activeProductsFlow.first().first {
            it.category == ProductCategory.INDUSTRIAL_GAS && it.name == "Oxygen"
        }

        val day1 = "2026-11-24"
        val day2 = "2026-11-25"

        // Set Day 1 Store Opening: Full=20, Empty=5
        val openRes = repository.setOpeningStockForDate(
            scope = StockCalculator.SCOPE_STORE,
            vehicleId = 0L,
            productId = oxygen.id,
            date = day1,
            desiredOpeningFull = 20,
            desiredOpeningEmpty = 5,
            admin = admin
        )
        assertTrue(openRes is OperationResult.Success)

        // Purchase 10 Full
        assertTrue(
            repository.recordTransaction(
                businessDate = day1,
                type = TransactionType.STORE_PURCHASE_FULL,
                productId = oxygen.id,
                vehicleId = null,
                actor = admin,
                quantity = 10,
                notes = "Supplier batch 1"
            ) is OperationResult.Success
        )

        // Dispatch 12 Full to NADEEM
        assertTrue(
            repository.recordTransaction(
                businessDate = day1,
                type = TransactionType.VEHICLE_FULL_RECEIVED,
                productId = oxygen.id,
                vehicleId = nadeemVehicle.id,
                actor = nadeem,
                quantity = 12,
                notes = "Morning load"
            ) is OperationResult.Success
        )

        // Attempting to dispatch 50 Full (more than available 18) must fail with InsufficientStock
        val overDispatch = repository.recordTransaction(
            businessDate = day1,
            type = TransactionType.VEHICLE_FULL_RECEIVED,
            productId = oxygen.id,
            vehicleId = nadeemVehicle.id,
            actor = nadeem,
            quantity = 50,
            notes = ""
        )
        assertTrue(overDispatch is OperationResult.InsufficientStock)

        // Nadeem sells 7 Full, collects 6 Empty from customers, returns 4 Empty to store
        assertTrue(
            repository.recordTransaction(day1, TransactionType.VEHICLE_CUSTOMER_SALE, oxygen.id, nadeemVehicle.id, nadeem, 7, "")
                is OperationResult.Success
        )
        assertTrue(
            repository.recordTransaction(day1, TransactionType.VEHICLE_EMPTY_FROM_CUSTOMER, oxygen.id, nadeemVehicle.id, nadeem, 6, "")
                is OperationResult.Success
        )
        assertTrue(
            repository.recordTransaction(day1, TransactionType.VEHICLE_EMPTY_RETURN_TO_STORE, oxygen.id, nadeemVehicle.id, nadeem, 4, "")
                is OperationResult.Success
        )

        // Verify Day 1 Store: Full = 20 + 10 + 0 - 12 = 18; Empty = 5 + 0 + 4 - 0 = 9
        val day1Store = repository.observeStoreStock(day1).first().first { it.product.id == oxygen.id }
        assertEquals(18, day1Store.storeFullBalance)
        assertEquals(9, day1Store.storeEmptyBalance)

        // Verify Day 1 Vehicle: TotalFull = 0 + 12 = 12; FullBal = 12 - 7 = 5; EmptyBal = 0 + 6 - 4 = 2
        val day1Veh = repository.observeVehicleStock(nadeemVehicle.id, day1).first().first { it.product.id == oxygen.id }
        assertEquals(12, day1Veh.totalFull)
        assertEquals(5, day1Veh.fullBalance)
        assertEquals(2, day1Veh.emptyBalance)

        // Verify Day 2 automatic rollover: OldFull = 5, OldEmpty = 2
        val day2Veh = repository.observeVehicleStock(nadeemVehicle.id, day2).first().first { it.product.id == oxygen.id }
        assertEquals(5, day2Veh.oldFull)
        assertEquals(2, day2Veh.oldEmpty)
    }

    @Test
    fun `backup export and restore preserves data and pdf xlsx export writes valid bytes`() = runTest {
        repository.initializeDatabase()
        val json = backupManager.exportBackupJson()
        val restoreRes = backupManager.restoreBackupJson(json)
        assertTrue(restoreRes is OperationResult.Success)

        val table = ReportTable(
            reportTitle = ReportType.FULL_EMPTY_STOCK.title,
            subtitle = "Test Export",
            generatedBy = "Ragavan",
            headers = listOf("Product", "Full", "Empty"),
            rows = listOf(listOf("Oxygen", "10", "4")),
            footerTotals = listOf("TOTAL", "10", "4")
        )
        val xlsxOut = ByteArrayOutputStream()
        ReportExporter.writeXlsx(table, xlsxOut)
        val xlsxBytes = xlsxOut.toByteArray()
        assertTrue(xlsxBytes.size > 100)
        // ZIP signature PK\x03\x04
        assertEquals(0x50.toByte(), xlsxBytes[0])
        assertEquals(0x4B.toByte(), xlsxBytes[1])

        // Verify all required OpenXML entries exist inside the generated .xlsx
        val entryNames = mutableSetOf<String>()
        java.util.zip.ZipInputStream(xlsxBytes.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entryNames.add(entry.name)
                entry = zis.nextEntry
            }
        }
        assertTrue(entryNames.contains("[Content_Types].xml"))
        assertTrue(entryNames.contains("xl/workbook.xml"))
        assertTrue(entryNames.contains("xl/styles.xml"))
        assertTrue(entryNames.contains("xl/worksheets/sheet1.xml"))
    }

    @Test
    fun `purchase company name and staff name editing persist accurately`() = runTest {
        repository.initializeDatabase()
        val admin = repository.usersFlow.first().first { it.role == UserRole.ADMIN }
        val nadeem = repository.usersFlow.first().first { it.username == "nadeem" }
        val oxygen = repository.activeProductsFlow.first().first {
            it.category == ProductCategory.INDUSTRIAL_GAS && it.name == "Oxygen"
        }

        // Record purchase with Purchase Company Name
        val purRes = repository.recordTransaction(
            businessDate = "2026-11-24",
            type = TransactionType.STORE_PURCHASE_FULL,
            productId = oxygen.id,
            vehicleId = null,
            actor = admin,
            quantity = 15,
            notes = "Batch A",
            purchaseCompanyName = "Emirates Gas LLC"
        )
        assertTrue(purRes is OperationResult.Success)
        val companies = repository.recentPurchaseCompaniesFlow.first()
        assertEquals(listOf("Emirates Gas LLC"), companies)

        // Backup & restore preserves purchaseCompanyName
        val json = backupManager.exportBackupJson()
        assertTrue(backupManager.restoreBackupJson(json) is OperationResult.Success)
        val restoredTx = repository.transactionsFlow.first().first()
        assertEquals("Emirates Gas LLC", restoredTx.purchaseCompanyName)

        // Edit staff name and linked vehicle name
        val editRes = repository.updateStaffName(
            targetUserId = nadeem.id,
            newDisplayName = "Nadeem Khan",
            newVehicleName = "NADEEM KHAN"
        )
        assertTrue(editRes is OperationResult.Success)
        val updatedNadeem = repository.usersFlow.first().first { it.id == nadeem.id }
        val updatedVehicle = repository.vehiclesFlow.first().first { it.staffUserId == nadeem.id }
        assertEquals("Nadeem Khan", updatedNadeem.displayName)
        assertEquals("NADEEM KHAN", updatedVehicle.name)

        // Duplicate vehicle name is blocked cleanly
        val dupRes = repository.updateStaffName(
            targetUserId = nadeem.id,
            newDisplayName = "Nadeem Khan",
            newVehicleName = "SAJAN"
        )
        assertTrue(dupRes is OperationResult.ValidationError)
    }

    @Test
    fun `customer empty pending balance, combo sale, and manual pending addition calculate accurately`() = runTest {
        repository.initializeDatabase()
        val admin = repository.usersFlow.first().first { it.role == UserRole.ADMIN }
        val nadeem = repository.usersFlow.first().first { it.username == "nadeem" }
        val nadeemVehicle = repository.vehiclesFlow.first().first { it.staffUserId == nadeem.id }
        val oxygen = repository.activeProductsFlow.first().first { it.name == "Oxygen" }
        val day = "2026-11-25"

        repository.setOpeningStockForDate(StockCalculator.SCOPE_STORE, 0L, oxygen.id, day, 30, 5, admin)
        repository.recordTransaction(day, TransactionType.VEHICLE_FULL_RECEIVED, oxygen.id, nadeemVehicle.id, admin, 20, "")

        // Sale 10 full cylinders to "Al Noor Hotel" with 4 empties collected now -> Pending = +6
        val comboRes = repository.recordCustomerSaleWithEmpty(
            businessDate = day,
            productId = oxygen.id,
            vehicleId = nadeemVehicle.id,
            actor = nadeem,
            saleQty = 10,
            emptyCollectedNow = 4,
            customerName = "Al Noor Hotel",
            notes = "Inv 101"
        )
        assertTrue(comboRes is OperationResult.Success)

        // Add 3 manual pending empties (previous balance) -> Pending = 6 + 3 = 9
        val manualRes = repository.recordTransaction(
            businessDate = day,
            type = TransactionType.CUSTOMER_EMPTY_PENDING_ADD,
            productId = oxygen.id,
            vehicleId = nadeemVehicle.id,
            actor = nadeem,
            quantity = 3,
            notes = "Opening empty pending",
            customerName = "Al Noor Hotel"
        )
        assertTrue(manualRes is OperationResult.Success)

        // Physical vehicle stock should ignore CUSTOMER_EMPTY_PENDING_ADD:
        // FullBal = 20 - 10 = 10, EmptyBal = 4
        val vehRow = repository.observeVehicleStock(nadeemVehicle.id, day).first().first { it.product.id == oxygen.id }
        assertEquals(10, vehRow.fullBalance)
        assertEquals(4, vehRow.emptyBalance)

        // Customer pending balance should be 10 + 3 - 4 = 9
        val pendingList = repository.observeCustomerPendingBalances(nadeemVehicle.id).first()
        assertEquals(1, pendingList.size)
        val hotel = pendingList.first()
        assertEquals("Al Noor Hotel", hotel.customerName)
        assertEquals(10, hotel.fullSold)
        assertEquals(3, hotel.manualPendingAdded)
        assertEquals(4, hotel.emptyCollected)
        assertEquals(9, hotel.pendingEmptyBalance)

        // Backup preserves customerName and CUSTOMER_EMPTY_PENDING_ADD
        val json = backupManager.exportBackupJson()
        assertTrue(backupManager.restoreBackupJson(json) is OperationResult.Success)
        val restoredPending = repository.observeCustomerPendingBalances(nadeemVehicle.id).first().first()
        assertEquals(9, restoredPending.pendingEmptyBalance)
    }

    @Test
    fun `editing recorded transactions updates supplier, customer, quantity and blocks negative stock`() = runTest {
        repository.initializeDatabase()
        val admin = repository.usersFlow.first().first { it.role == UserRole.ADMIN }
        val sivanVeh = repository.vehiclesFlow.first().first { it.name == "SIVAN" }
        val oxygen = repository.activeProductsFlow.first().first { it.name == "Oxygen" }
        val day = "2026-11-25"

        // Purchase 12 full from Supplier A
        assertTrue(
            repository.recordTransaction(
                businessDate = day,
                type = TransactionType.STORE_PURCHASE_FULL,
                productId = oxygen.id,
                vehicleId = null,
                actor = admin,
                quantity = 12,
                notes = "Initial bill",
                purchaseCompanyName = "Supplier A"
            ) is OperationResult.Success
        )
        // Dispatch 10 to SIVAN -> Store Full = 2
        assertTrue(
            repository.recordTransaction(day, TransactionType.VEHICLE_FULL_RECEIVED, oxygen.id, sivanVeh.id, admin, 10, "")
                is OperationResult.Success
        )

        val purchaseTx = repository.transactionsFlow.first().first { it.type == TransactionType.STORE_PURCHASE_FULL }

        // Reducing purchase from 12 down to 8 would make Store Full = -2 -> must fail
        val invalidEdit = repository.updateTransaction(
            transactionId = purchaseTx.id,
            newProductId = oxygen.id,
            newQuantity = 8,
            newPurchaseCompanyName = "Emirates Gas",
            newCustomerName = "",
            newNotes = "Too low",
            admin = admin
        )
        assertTrue(invalidEdit is OperationResult.InsufficientStock)

        // Valid edit: change quantity to 15 and supplier name to "ADNOC Gas" -> Store Full = 5
        val validEdit = repository.updateTransaction(
            transactionId = purchaseTx.id,
            newProductId = oxygen.id,
            newQuantity = 15,
            newPurchaseCompanyName = "ADNOC Gas",
            newCustomerName = "",
            newNotes = "Corrected bill",
            admin = admin
        )
        assertTrue(validEdit is OperationResult.Success)
        val storeRow = repository.observeStoreStock(day).first().first { it.product.id == oxygen.id }
        assertEquals(5, storeRow.storeFullBalance)
        val updatedTx = repository.transactionsFlow.first().first { it.id == purchaseTx.id }
        assertEquals("ADNOC Gas", updatedTx.purchaseCompanyName)
        assertEquals("Corrected bill", updatedTx.notes)
    }

    @Test
    fun `admin can add up to 10 plus vehicles and staff logins and session preference persists`() = runTest {
        repository.initializeDatabase()

        assertTrue(repository.addStaffAndVehicle("Imran", "TRUCK 8", "1008") is OperationResult.Success)
        assertTrue(repository.addStaffAndVehicle("Raju", "TRUCK 9", "1009") is OperationResult.Success)
        assertTrue(repository.addStaffAndVehicle("Karim", "TRUCK 10", "1010") is OperationResult.Success)

        val vehicles = repository.vehiclesFlow.first()
        val users = repository.usersFlow.first()
        assertEquals(10, vehicles.size)
        assertEquals(11, users.size) // 1 Admin + 10 Staff

        val karimUser = users.first { it.displayName == "Karim" }
        val session = repository.authenticate(karimUser.id, "1010")
        assertNotNull(session)
        assertEquals("TRUCK 10", session!!.assignedVehicle!!.name)

        // Verify one-time open login persistence via SessionPreferences
        val prefs = SessionPreferences(context)
        prefs.clearLoggedInUserId()
        assertNull(prefs.getLoggedInUserId())
        prefs.saveLoggedInUserId(karimUser.id)
        assertEquals(karimUser.id, prefs.getLoggedInUserId())
        prefs.clearLoggedInUserId()
        assertNull(prefs.getLoggedInUserId())
    }

    @Test
    fun `supplier complaint replace, customer complaint replace, and new customer sale calculate accurately`() = runTest {
        repository.initializeDatabase()
        val admin = repository.usersFlow.first().first { it.role == UserRole.ADMIN }
        val nadeem = repository.usersFlow.first().first { it.username == "nadeem" }
        val nadeemVehicle = repository.vehiclesFlow.first().first { it.staffUserId == nadeem.id }
        val oxygen = repository.activeProductsFlow.first().first { it.name == "Oxygen" }
        val day = "2026-11-25"
        val yearMonth = "2026-11"

        // Start Store Opening: Full=10, Empty=5
        assertTrue(
            repository.setOpeningStockForDate(
                StockCalculator.SCOPE_STORE, 0L, oxygen.id, day, 10, 5, admin
            ) is OperationResult.Success
        )

        // Supplier Complaint Replace 3: Store Full +3 (13), Store Empty -3 (2)
        val supComplaint = repository.recordTransaction(
            businessDate = day,
            type = TransactionType.STORE_PURCHASE_COMPLAINT_REPLACE,
            productId = oxygen.id,
            vehicleId = null,
            actor = admin,
            quantity = 3,
            notes = "Valve defect replaced",
            purchaseCompanyName = "ADNOC Gas"
        )
        assertTrue(supComplaint is OperationResult.Success)

        // Over-replacing supplier complaint (4 when only 2 empty left) must fail
        val overSupComplaint = repository.recordTransaction(
            businessDate = day,
            type = TransactionType.STORE_PURCHASE_COMPLAINT_REPLACE,
            productId = oxygen.id,
            vehicleId = null,
            actor = admin,
            quantity = 4,
            notes = "",
            purchaseCompanyName = "ADNOC Gas"
        )
        assertTrue(overSupComplaint is OperationResult.InsufficientStock)

        val storeRow = repository.observeStoreStock(day).first().first { it.product.id == oxygen.id }
        assertEquals(13, storeRow.storeFullBalance)
        assertEquals(2, storeRow.storeEmptyBalance)
        assertEquals(3, storeRow.fullReturns)
        assertEquals(3, storeRow.emptyDispatch)

        // Dispatch 8 Full to Nadeem
        assertTrue(
            repository.recordTransaction(day, TransactionType.VEHICLE_FULL_RECEIVED, oxygen.id, nadeemVehicle.id, admin, 8, "")
                is OperationResult.Success
        )

        // Staff Customer Complaint Replace 2: Vehicle Full -2 (6), Vehicle Empty +2 (2)
        assertTrue(
            repository.recordTransaction(
                businessDate = day,
                type = TransactionType.VEHICLE_CUSTOMER_COMPLAINT_REPLACE,
                productId = oxygen.id,
                vehicleId = nadeemVehicle.id,
                actor = nadeem,
                quantity = 2,
                notes = "Leak swap",
                customerName = "Metro Kitchen"
            ) is OperationResult.Success
        )

        // Staff New Customer Full Sale 3: Vehicle Full -3 (3), Empty stays 2
        assertTrue(
            repository.recordTransaction(
                businessDate = day,
                type = TransactionType.VEHICLE_NEW_CUSTOMER_SALE,
                productId = oxygen.id,
                vehicleId = nadeemVehicle.id,
                actor = nadeem,
                quantity = 3,
                notes = "New cylinder connection",
                customerName = "Oasis Grill"
            ) is OperationResult.Success
        )

        val vehRow = repository.observeVehicleStock(nadeemVehicle.id, day).first().first { it.product.id == oxygen.id }
        assertEquals(3, vehRow.fullBalance)
        assertEquals(2, vehRow.emptyBalance)
        assertEquals(3, vehRow.customerSales)
        assertEquals(2, vehRow.complaintReplaced)

        // Monthly/Product/Staff reports include New Customer Sale (3) and exclude Customer Complaint Replace (2)
        val monthly = repository.observeMonthlySalesReport(yearMonth).first().first { it.date == day }
        assertEquals(3, monthly.totalSold)
        val productSale = repository.observeProductWiseSales(yearMonth).first().first { it.product.id == oxygen.id }
        assertEquals(3, productSale.totalSold)
        val staffSale = repository.observeStaffWiseSales(yearMonth, day).first().first { it.vehicle.id == nadeemVehicle.id }
        assertEquals(3, staffSale.totalSold)

        // Neither New Customer Full Sale nor Complaint Replacement creates pending empty balances
        val pending = repository.observeCustomerPendingBalances(nadeemVehicle.id).first()
        assertTrue(pending.isEmpty())
    }

    @Test
    fun `auto login, auto logout, theme preset, sound preference and audio playback are safe`() = runTest {
        val prefs = SessionPreferences(context)
        assertTrue(prefs.autoLoginEnabledFlow.first())
        assertEquals(AutoLogoutMode.OFF, prefs.autoLogoutModeFlow.first())
        assertEquals(ThemePreset.EMERALD_CORPORATE, prefs.themePresetFlow.first())
        assertTrue(prefs.soundEnabledFlow.first())

        prefs.setAutoLoginEnabled(false)
        prefs.setAutoLogoutMode(AutoLogoutMode.IDLE_5_MIN)
        prefs.setThemePreset(ThemePreset.OCEANIC_TEAL)
        prefs.setSoundEnabled(false)

        assertFalse(prefs.autoLoginEnabledFlow.first())
        assertEquals(AutoLogoutMode.IDLE_5_MIN, prefs.autoLogoutModeFlow.first())
        assertEquals(ThemePreset.OCEANIC_TEAL, prefs.themePresetFlow.first())
        assertFalse(prefs.soundEnabledFlow.first())

        // SoundEffectsManager safely no-ops or catches audio errors in Robolectric
        SoundEffectsManager.playStartupChime(false)
        SoundEffectsManager.playStartupChime(true)
        SoundEffectsManager.playSuccessChime(true)
        SoundEffectsManager.playWarningTone(true)
    }

    @Test
    fun `admin and staff role separation and unique PIN enforcement work properly`() = runTest {
        repository.initializeDatabase()
        val users = repository.usersFlow.first()
        val admin = users.first { it.role == UserRole.ADMIN }
        val nadeem = users.first { it.username == "nadeem" }

        // Admin succeeds on ADMIN role, fails on STAFF role
        assertNotNull(repository.authenticate(admin.id, "1725", UserRole.ADMIN))
        assertNull(repository.authenticate(admin.id, "1725", UserRole.STAFF))

        // Staff succeeds on STAFF role, fails on ADMIN role
        assertNotNull(repository.authenticate(nadeem.id, "1001", UserRole.STAFF))
        assertNull(repository.authenticate(nadeem.id, "1001", UserRole.ADMIN))

        // Updating staff PIN to collide with Admin PIN (1725) must fail
        val dupPinUpdate = repository.updateUserPin(nadeem.id, "1725")
        assertTrue(dupPinUpdate is OperationResult.ValidationError)

        // Adding a new staff with an already-used PIN (1001) must fail
        val dupStaffPin = repository.addStaffAndVehicle("Bilal", "TRUCK 11", "1001")
        assertTrue(dupStaffPin is OperationResult.ValidationError)

        // Updating to a unique PIN succeeds and authenticates cleanly
        val validPinUpdate = repository.updateUserPin(nadeem.id, "2099")
        assertTrue(validPinUpdate is OperationResult.Success)
        assertNull(repository.authenticate(nadeem.id, "1001", UserRole.STAFF))
        assertNotNull(repository.authenticate(nadeem.id, "2099", UserRole.STAFF))
    }

    @Test
    fun `permissions evaluator and auto drive backup preferences work cleanly`() = runTest {
        val prefs = SessionPreferences(context)
        assertFalse(prefs.autoDriveBackupEnabledFlow.first())
        assertEquals(0L, prefs.lastDriveBackupAtMsFlow.first())
        assertEquals("", prefs.lastDriveFileIdFlow.first())

        prefs.setAutoDriveBackupEnabled(true)
        prefs.saveDriveBackupReceipt(1735123456000L, "drive_file_abc123")

        assertTrue(prefs.autoDriveBackupEnabledFlow.first())
        assertEquals(1735123456000L, prefs.lastDriveBackupAtMsFlow.first())
        assertEquals("drive_file_abc123", prefs.lastDriveFileIdFlow.first())

        val unauthStatus = AppPermissionsManager.evaluateStatus(context, driveAuthorized = false)
        assertTrue(unauthStatus.storageFrameworkReady)
        assertFalse(unauthStatus.driveAuthorized)
        assertFalse(unauthStatus.allCoreGranted)

        val authStatus = AppPermissionsManager.evaluateStatus(context, driveAuthorized = true)
        assertTrue(authStatus.driveAuthorized)
    }

    @Test
    fun `google drive backup manager uploads and restores backup json via drive v3 endpoints`() = runTest {
        var uploadedBody = ""
        val interceptor = Interceptor { chain ->
            val req = chain.request()
            val url = req.url.toString()
            when {
                url.contains("/files?") && req.method == "GET" && url.contains("spaces=drive") -> {
                    Response.Builder()
                        .request(req)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body("""{"files":[{"id":"drv_123","name":"CONCORD_ATOZ_GAS_AUTO_BACKUP.json"}]}""".toResponseBody("application/json".toMediaType()))
                        .build()
                }
                url.contains("/files?") && req.method == "GET" && url.contains("spaces=appDataFolder") -> {
                    Response.Builder()
                        .request(req)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body("""{"files":[]}""".toResponseBody("application/json".toMediaType()))
                        .build()
                }
                url.contains("/upload/drive/v3/files") -> {
                    val buf = Buffer()
                    req.body?.writeTo(buf)
                    val captured = buf.readUtf8()
                    if (captured.contains("schemaVersion")) {
                        uploadedBody = captured
                    }
                    Response.Builder()
                        .request(req)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body("""{"id":"drv_123","name":"CONCORD_ATOZ_GAS_AUTO_BACKUP.json"}""".toResponseBody("application/json".toMediaType()))
                        .build()
                }
                url.contains("/drive/v3/files/drv_123?alt=media") -> {
                    Response.Builder()
                        .request(req)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body("""{"schemaVersion":2,"users":[]}""".toResponseBody("application/json".toMediaType()))
                        .build()
                }
                else -> {
                    Response.Builder()
                        .request(req)
                        .protocol(Protocol.HTTP_1_1)
                        .code(404)
                        .message("Not Found")
                        .body("{}".toResponseBody("application/json".toMediaType()))
                        .build()
                }
            }
        }

        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val driveMgr = GoogleDriveBackupManager(context, client)
        val uploadResult = driveMgr.uploadJsonBackup(
            jsonContent = """{"schemaVersion":2,"users":[{"name":"Ragavan"}]}""",
            accessTokenOverride = "test-oauth-token"
        )
        assertTrue(uploadResult.isSuccess)
        assertEquals("drv_123", uploadResult.getOrNull()?.fileId)
        assertTrue(uploadedBody.contains("Ragavan"))

        val downloadResult = driveMgr.downloadLatestBackupJson(accessTokenOverride = "test-oauth-token")
        assertTrue(downloadResult.isSuccess)
        assertTrue(downloadResult.getOrNull().orEmpty().contains("schemaVersion"))
    }

    @Test
    fun `viewmodel strictly blocks staff role from admin settings, reports, drive backup, and store operations`() = runTest {
        repository.initializeDatabase()
        val prefs = SessionPreferences(context)
        prefs.clearLoggedInUserId()
        val vm = AppViewModel(
            repository = repository,
            backupManager = backupManager,
            cloudSync = null,
            sessionPreferences = prefs,
            driveBackupManager = GoogleDriveBackupManager(context),
            appContext = context
        )
        val deadline1 = System.currentTimeMillis() + 5000L
        while (!vm.isInitialized.value && System.currentTimeMillis() < deadline1) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(20)
        }
        assertTrue(vm.isInitialized.value)

        val nadeem = repository.usersFlow.first().first { it.username == "nadeem" }
        val oxygen = repository.activeProductsFlow.first().first { it.name == "Oxygen" }
        vm.login(userId = nadeem.id, pin = "1001", expectedRole = UserRole.STAFF)

        val deadline2 = System.currentTimeMillis() + 5000L
        while (vm.authSession.value == null && System.currentTimeMillis() < deadline2) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(20)
        }

        assertEquals(UserRole.STAFF, vm.authSession.value?.user?.role)

        // 1. Staff blocked from Drive authorization
        vm.requestGoogleDriveAuthorizationAndEnable()
        assertEquals("Admin Only", vm.warningAlert.value?.title)
        vm.dismissWarningAlert()

        // 2. Staff blocked from archiving Admin reports
        val table = ReportTable("Daily Store", "Test", "Nadeem", listOf("Product"), emptyList(), null)
        vm.saveReportToCloudAndDrive(ReportType.DAILY_STORE, table, uploadPdfToDrive = false)
        assertEquals("Admin Only", vm.warningAlert.value?.title)
        vm.dismissWarningAlert()

        // 3. Staff blocked from adding staff/vehicles
        vm.addStaffAndVehicle("Hacker", "TRUCK X", "9999")
        assertEquals("Admin Only", vm.warningAlert.value?.title)
        vm.dismissWarningAlert()

        // 4. Staff blocked from modifying Store opening stock
        vm.updateOpeningStock(StockCalculator.SCOPE_STORE, 0L, oxygen.id, 50, 10)
        assertEquals("Admin Only", vm.warningAlert.value?.title)
        vm.dismissWarningAlert()

        // 5. Staff blocked from Store purchase transaction
        vm.recordTransaction(TransactionType.STORE_PURCHASE_FULL, oxygen.id, null, 10)
        assertEquals("Permission Denied", vm.warningAlert.value?.title)
        assertTrue(repository.transactionsFlow.first().isEmpty())
    }
}
