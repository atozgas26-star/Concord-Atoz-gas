package com.example

import android.content.Context
import android.util.Base64
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.audio.SoundEffectsManager
import com.example.auth.AdminCredentialCrypto
import com.example.auth.GmailVerificationManager
import com.example.auth.RecoveryPurpose
import com.example.auth.RecoveryStep
import com.example.data.local.ConcordDatabase
import com.example.data.local.SessionPreferences
import com.example.data.model.ApprovalStatus
import com.example.data.model.AutoLogoutMode
import com.example.data.model.BatchProductEntry
import com.example.data.model.ChatChannel
import com.example.data.model.CompanyStockRow
import com.example.data.model.CustomerChangeMode
import com.example.data.model.CustomerExchangeItem
import com.example.data.model.NotificationCategory
import com.example.data.model.NotificationTargetScope
import com.example.data.model.OperationResult
import com.example.data.model.ProductCategory
import com.example.data.model.ReportType
import com.example.data.model.StockCalculator
import com.example.data.model.StoreTransferDirection
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
import com.example.ui.components.QuickCalculatorEngine
import com.example.ui.components.QuickCalculatorState
import com.example.ui.viewmodel.AppViewModel
import com.example.update.AppUpdateManager
import com.example.update.AppUpdateStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
    fun `app name matches C&A Gas`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("C&A Gas", appName)
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

        // 6. Staff blocked from changing low stock threshold
        vm.updateLowStockThreshold(12)
        assertEquals("Admin Only", vm.warningAlert.value?.title)
    }

    @Test
    fun `checkLowStockLevels updates StateFlow when product stock drops below threshold and supports dismiss and retrigger`() = runTest {
        repository.initializeDatabase()
        val prefs = SessionPreferences(context)
        prefs.clearLoggedInUserId()
        prefs.setLowStockThreshold(5)

        val vm = AppViewModel(
            repository = repository,
            backupManager = backupManager,
            cloudSync = null,
            sessionPreferences = prefs,
            driveBackupManager = null,
            appContext = context
        )
        val deadline = System.currentTimeMillis() + 5000L
        while (!vm.isInitialized.value && System.currentTimeMillis() < deadline) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(20)
        }

        val products = repository.activeProductsFlow.first()
        val oxygen = products.first { it.name == "Oxygen" }
        val argon = products.first { it.name == "Argon" }

        // 1. All products above threshold -> no alert shown
        val healthyRows = listOf(
            CompanyStockRow(oxygen, storeFull = 10, vehiclesFull = 2, totalFull = 12, storeEmpty = 3, vehiclesEmpty = 1, totalEmpty = 4),
            CompanyStockRow(argon, storeFull = 5, vehiclesFull = 0, totalFull = 5, storeEmpty = 0, vehiclesEmpty = 0, totalEmpty = 0)
        )
        val healthyState = vm.checkLowStockLevels(stockRows = healthyRows, threshold = 5)
        assertFalse(healthyState.isAlertVisible)
        assertFalse(healthyState.shouldShowBanner)
        assertTrue(vm.lowStockAlertState.value.lowStockItems.isEmpty())

        // 2. Oxygen drops to 3 (< 5) -> StateFlow emits active alert
        val droppedRows = listOf(
            CompanyStockRow(oxygen, storeFull = 3, vehiclesFull = 2, totalFull = 5, storeEmpty = 3, vehiclesEmpty = 1, totalEmpty = 4),
            CompanyStockRow(argon, storeFull = 6, vehiclesFull = 0, totalFull = 6, storeEmpty = 0, vehiclesEmpty = 0, totalEmpty = 0)
        )
        vm.checkLowStockLevels(stockRows = droppedRows, threshold = 5)
        val alert1 = vm.lowStockAlertState.value
        assertTrue(alert1.isAlertVisible)
        assertTrue(alert1.shouldShowBanner)
        assertEquals(1, alert1.activeAlertCount)
        assertEquals("Oxygen", alert1.lowStockItems.first().productName)
        assertEquals(3, alert1.lowStockItems.first().storeFullBalance)

        // 3. Dismissing alert keeps banner hidden while stock stays identical
        vm.dismissLowStockAlert()
        assertFalse(vm.lowStockAlertState.value.shouldShowBanner)
        vm.checkLowStockLevels(stockRows = droppedRows, threshold = 5)
        assertFalse(vm.lowStockAlertState.value.shouldShowBanner)

        // 4. Stock drops further (Oxygen -> 0) -> banner automatically re-appears
        val outOfStockRows = listOf(
            CompanyStockRow(oxygen, storeFull = 0, vehiclesFull = 2, totalFull = 2, storeEmpty = 3, vehiclesEmpty = 1, totalEmpty = 4),
            CompanyStockRow(argon, storeFull = 6, vehiclesFull = 0, totalFull = 6, storeEmpty = 0, vehiclesEmpty = 0, totalEmpty = 0)
        )
        vm.checkLowStockLevels(stockRows = outOfStockRows, threshold = 5)
        val alert2 = vm.lowStockAlertState.value
        assertTrue(alert2.shouldShowBanner)
        assertEquals(1, alert2.outOfStockCount)

        // 5. Threshold 0 disables low-stock alerts cleanly
        vm.checkLowStockLevels(stockRows = outOfStockRows, threshold = 0)
        assertFalse(vm.lowStockAlertState.value.isAlertVisible)
        assertFalse(vm.lowStockAlertState.value.shouldShowBanner)
    }

    @Test
    fun `dashboard summary computes current inventory totals, pending deliveries, and recent store activity accurately`() = runTest {
        repository.initializeDatabase()
        val admin = repository.usersFlow.first().first { it.role == UserRole.ADMIN }
        val nadeem = repository.usersFlow.first().first { it.username == "nadeem" }
        val nadeemVehicle = repository.vehiclesFlow.first().first { it.staffUserId == nadeem.id }
        val sivanVehicle = repository.vehiclesFlow.first().first { it.name == "SIVAN" }
        val oxygen = repository.activeProductsFlow.first().first { it.name == "Oxygen" }
        val day = "2026-11-25"

        // Purchase 25 Full cylinders into Store
        assertTrue(
            repository.recordTransaction(
                businessDate = day,
                type = TransactionType.STORE_PURCHASE_FULL,
                productId = oxygen.id,
                vehicleId = null,
                actor = admin,
                quantity = 25,
                notes = "Morning arrival",
                purchaseCompanyName = "Emirates Gas"
            ) is OperationResult.Success
        )

        // Dispatch 10 to NADEEM and 6 to SIVAN -> Store Full = 9, Vehicles Full = 16
        assertTrue(
            repository.recordTransaction(day, TransactionType.VEHICLE_FULL_RECEIVED, oxygen.id, nadeemVehicle.id, admin, 10, "")
                is OperationResult.Success
        )
        assertTrue(
            repository.recordTransaction(day, TransactionType.VEHICLE_FULL_RECEIVED, oxygen.id, sivanVehicle.id, admin, 6, "")
                is OperationResult.Success
        )

        // NADEEM delivers 4 to customer -> NADEEM pending = 6, SIVAN pending = 6 -> Total pending deliveries = 12
        assertTrue(
            repository.recordTransaction(
                businessDate = day,
                type = TransactionType.VEHICLE_CUSTOMER_SALE,
                productId = oxygen.id,
                vehicleId = nadeemVehicle.id,
                actor = nadeem,
                quantity = 4,
                notes = "",
                customerName = "Al Noor Hotel"
            ) is OperationResult.Success
        )

        val summary = repository.observeDashboardSummary(day).first()

        // Current Inventory Totals: Store 9 Full + Fleet 12 Full = 21 Full; 0 Empty
        assertEquals(21, summary.totalFullCylinders)
        assertEquals(0, summary.totalEmptyCylinders)
        assertEquals(9, summary.storeFullBalance)
        assertEquals(12, summary.allVehiclesFullBalance)

        // Pending Deliveries: 12 cylinders remaining across 2 active vehicles
        assertEquals(12, summary.pendingDeliveryCylinders)
        assertEquals(2, summary.activeDeliveryVehiclesCount)
        assertEquals(4, summary.todaySales)

        // Recent Store Activity includes purchase + 2 dispatches (3 store-level entries), excluding pure vehicle sale
        assertEquals(3, summary.todayStoreActivityCount)
        assertEquals(3, summary.recentStoreActivity.size)
        assertTrue(summary.recentStoreActivity.all { it.transaction.type.isStoreLevel })
    }

    @Test
    fun `warehouse dark mode toggle persists across preferences, syncs with midnight preset, and new app logo resource loads`() = runTest {
        repository.initializeDatabase()
        val prefs = SessionPreferences(context)
        prefs.setDarkModeEnabled(false)
        prefs.setThemePreset(com.example.data.model.ThemePreset.EMERALD_CORPORATE)

        val vm = AppViewModel(
            repository = repository,
            backupManager = backupManager,
            cloudSync = null,
            sessionPreferences = prefs,
            driveBackupManager = null,
            appContext = context
        )
        val initDeadline = System.currentTimeMillis() + 5000L
        while (!vm.isInitialized.value && System.currentTimeMillis() < initDeadline) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(20)
        }

        assertFalse(prefs.darkModeEnabledFlow.first())

        // Toggle dark mode ON
        vm.toggleDarkMode()
        val onDeadline = System.currentTimeMillis() + 5000L
        while (!vm.darkModeEnabled.value && System.currentTimeMillis() < onDeadline) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(20)
        }
        assertTrue(vm.darkModeEnabled.value)
        assertTrue(prefs.darkModeEnabledFlow.first())

        // Selecting MIDNIGHT_DARK keeps dark mode ON; toggling OFF resets preset out of MIDNIGHT_DARK cleanly
        vm.setThemePreset(com.example.data.model.ThemePreset.MIDNIGHT_DARK)
        val midnightDeadline = System.currentTimeMillis() + 5000L
        while (vm.themePreset.value != com.example.data.model.ThemePreset.MIDNIGHT_DARK && System.currentTimeMillis() < midnightDeadline) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(20)
        }
        assertEquals(com.example.data.model.ThemePreset.MIDNIGHT_DARK, vm.themePreset.value)

        vm.toggleDarkMode()
        val offDeadline = System.currentTimeMillis() + 5000L
        while ((vm.darkModeEnabled.value || vm.themePreset.value == com.example.data.model.ThemePreset.MIDNIGHT_DARK) &&
            System.currentTimeMillis() < offDeadline
        ) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(20)
        }
        assertFalse(vm.darkModeEnabled.value)
        assertEquals(com.example.data.model.ThemePreset.EMERALD_CORPORATE, vm.themePreset.value)

        // Verify new app logo drawable and adaptive launcher icon resources resolve cleanly
        assertNotNull(context.resources.getDrawable(R.drawable.ic_concord_logo_v2, context.theme))
        assertNotNull(context.resources.getDrawable(R.mipmap.ic_launcher, context.theme))
    }

    @Test
    fun `admin credential crypto hashes passwords with salted PBKDF2, isolates OTP purpose, and strips header injection`() {
        val hash1 = AdminCredentialCrypto.hashPassword("Concord@2026")
        val hash2 = AdminCredentialCrypto.hashPassword("Concord@2026")
        assertTrue(hash1.startsWith("pbkdf2\$"))
        assertFalse(hash1 == hash2) // Unique random salt per hash
        assertFalse(hash1.contains("Concord@2026"))

        assertTrue(AdminCredentialCrypto.verifyPassword("Concord@2026", hash1))
        assertTrue(AdminCredentialCrypto.verifyPassword("Concord@2026", hash2))
        assertFalse(AdminCredentialCrypto.verifyPassword("WrongPass!", hash1))
        assertFalse(AdminCredentialCrypto.verifyPassword("", hash1))
        assertFalse(AdminCredentialCrypto.verifyPassword("Concord@2026", "plaintext"))

        val otp = AdminCredentialCrypto.generateSixDigitCode()
        assertEquals(6, otp.length)
        assertTrue(otp.all { it.isDigit() })

        val salt = AdminCredentialCrypto.generateRandomSalt()
        val pwdHash = AdminCredentialCrypto.hashVerificationCode(
            code = otp,
            salt = salt,
            purpose = RecoveryPurpose.FORGOT_PASSWORD.name,
            email = "atozgas26@gmail.com"
        )
        assertTrue(
            AdminCredentialCrypto.verifyVerificationCode(
                inputCode = otp,
                expectedHash = pwdHash,
                salt = salt,
                purpose = RecoveryPurpose.FORGOT_PASSWORD.name,
                email = "ATOZGAS26@GMAIL.COM"
            )
        )
        // Cross-purpose replay must be rejected
        assertFalse(
            AdminCredentialCrypto.verifyVerificationCode(
                inputCode = otp,
                expectedHash = pwdHash,
                salt = salt,
                purpose = RecoveryPurpose.FORGOT_USERNAME.name,
                email = "atozgas26@gmail.com"
            )
        )
        assertFalse(
            AdminCredentialCrypto.verifyVerificationCode(
                inputCode = "000000",
                expectedHash = pwdHash,
                salt = salt,
                purpose = RecoveryPurpose.FORGOT_PASSWORD.name,
                email = "atozgas26@gmail.com"
            )
        )

        // Header injection CR/LF neutralization
        assertEquals("atozgas26@gmail.comBcc:evil@x.com", AdminCredentialCrypto.sanitizeEmailHeader("atozgas26@gmail.com\r\nBcc:evil@x.com"))
        assertFalse(AdminCredentialCrypto.isValidEmail("atozgas26@gmail.com\r\nBcc:evil@x.com"))
        assertTrue(AdminCredentialCrypto.isValidEmail("atozgas26@gmail.com"))
        assertEquals("at***6@gmail.com", AdminCredentialCrypto.maskEmail("atozgas26@gmail.com"))
    }

    @Test
    fun `admin username password login and gmail verification code forgot password and forgot username flows work end to end`() = runTest {
        repository.initializeDatabase()
        val prefs = SessionPreferences(context)
        prefs.clearLoggedInUserId()
        prefs.clearAdminCredentials()

        val sentMimePayloads = mutableListOf<String>()
        val gmailInterceptor = Interceptor { chain ->
            val req = chain.request()
            assertTrue(req.url.toString().contains("/gmail/v1/users/me/messages/send"))
            assertEquals("Bearer test-gmail-oauth-token", req.header("Authorization"))
            val buf = Buffer()
            req.body?.writeTo(buf)
            val bodyJson = org.json.JSONObject(buf.readUtf8())
            val rawB64 = bodyJson.getString("raw")
            val decodedMime = String(
                Base64.decode(rawB64, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING),
                Charsets.UTF_8
            )
            sentMimePayloads.add(decodedMime)
            Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("""{"id":"gmail_msg_${sentMimePayloads.size}"}""".toResponseBody("application/json".toMediaType()))
                .build()
        }
        val mockGmailClient = OkHttpClient.Builder().addInterceptor(gmailInterceptor).build()
        val gmailMgr = GmailVerificationManager(context, mockGmailClient)

        val vm = AppViewModel(
            repository = repository,
            backupManager = backupManager,
            cloudSync = null,
            sessionPreferences = prefs,
            driveBackupManager = null,
            gmailVerificationManager = gmailMgr,
            appContext = context
        )
        fun waitUntil(timeoutMs: Long = 5000L, condition: () -> Boolean) {
            val deadline = System.currentTimeMillis() + timeoutMs
            while (!condition() && System.currentTimeMillis() < deadline) {
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                Thread.sleep(20)
            }
        }

        waitUntil { vm.isInitialized.value }
        assertFalse(vm.adminCredentialsState.value.isConfigured)

        // 1. First-run Admin Username & Password setup
        vm.setupInitialAdminCredentials(
            username = "concord_admin",
            password = "OldPassword123",
            confirmPassword = "OldPassword123",
            recoveryEmail = "atozgas26@gmail.com"
        )
        waitUntil { vm.authSession.value?.user?.role == UserRole.ADMIN && vm.adminCredentialsState.value.isConfigured }
        assertEquals(UserRole.ADMIN, vm.authSession.value?.user?.role)
        val savedCreds = prefs.adminCredentialsFlow.first()
        assertEquals("concord_admin", savedCreds.username)
        assertEquals("atozgas26@gmail.com", savedCreds.recoveryEmail)
        assertTrue(savedCreds.passwordHash.startsWith("pbkdf2\$"))
        assertFalse(savedCreds.passwordHash.contains("OldPassword123"))

        // 2. Logout & verify username/password login rejects wrong password and accepts right password
        vm.logout()
        waitUntil { vm.authSession.value == null }

        vm.loginAdminWithPassword("concord_admin", "WrongPass999")
        waitUntil { vm.loginError.value != null }
        assertNull(vm.authSession.value)

        vm.loginAdminWithPassword("concord_admin", "OldPassword123")
        waitUntil { vm.authSession.value?.user?.role == UserRole.ADMIN }
        assertEquals(UserRole.ADMIN, vm.authSession.value?.user?.role)

        // 3. Logout & test Forgot Password via real Gmail API verification code
        vm.logout()
        waitUntil { vm.authSession.value == null }

        vm.openAdminRecovery(RecoveryPurpose.FORGOT_PASSWORD, "atozgas26@gmail.com")
        // Unregistered Gmail must be rejected
        vm.sendAdminRecoveryCode("unauthorized@gmail.com", accessTokenOverride = "test-gmail-oauth-token")
        waitUntil { vm.adminRecoveryState.value.errorMessage != null }
        assertEquals(0, sentMimePayloads.size)

        // Registered Gmail sends 6-digit verification code via Gmail API
        vm.clearAdminRecoveryError()
        vm.sendAdminRecoveryCode("atozgas26@gmail.com", accessTokenOverride = "test-gmail-oauth-token")
        waitUntil { vm.adminRecoveryState.value.step == RecoveryStep.VERIFY_CODE }
        assertEquals(1, sentMimePayloads.size)
        val pwdCodeMatch = Regex("VERIFICATION CODE:\\s*(\\d{6})").find(sentMimePayloads.last())
        assertNotNull(pwdCodeMatch)
        val pwdOtp = pwdCodeMatch!!.groupValues[1]

        // Wrong OTP fails cleanly
        vm.verifyCodeAndResetAdminPassword("000000", "NewSecure2026", "NewSecure2026")
        waitUntil { vm.adminRecoveryState.value.errorMessage != null }
        assertNotNull(vm.adminRecoveryState.value.activePurpose)

        // Correct OTP resets password and closes dialog
        vm.verifyCodeAndResetAdminPassword(pwdOtp, "NewSecure2026", "NewSecure2026")
        waitUntil { vm.adminRecoveryState.value.activePurpose == null }

        // Old password now fails; new password signs in Admin
        vm.loginAdminWithPassword("concord_admin", "OldPassword123")
        waitUntil { vm.loginError.value != null }
        assertNull(vm.authSession.value)

        vm.loginAdminWithPassword("concord_admin", "NewSecure2026")
        waitUntil { vm.authSession.value?.user?.role == UserRole.ADMIN }
        assertEquals(UserRole.ADMIN, vm.authSession.value?.user?.role)

        // 4. Logout & test Forgot Username via real Gmail API verification code
        vm.logout()
        waitUntil { vm.authSession.value == null }

        vm.openAdminRecovery(RecoveryPurpose.FORGOT_USERNAME, "atozgas26@gmail.com")
        vm.sendAdminRecoveryCode("atozgas26@gmail.com", accessTokenOverride = "test-gmail-oauth-token")
        waitUntil { vm.adminRecoveryState.value.step == RecoveryStep.VERIFY_CODE }
        assertEquals(2, sentMimePayloads.size)
        val userCodeMatch = Regex("VERIFICATION CODE:\\s*(\\d{6})").find(sentMimePayloads.last())
        assertNotNull(userCodeMatch)
        val userOtp = userCodeMatch!!.groupValues[1]

        vm.verifyCodeAndRecoverAdminUsername(userOtp)
        waitUntil { vm.adminRecoveryState.value.step == RecoveryStep.USERNAME_REVEALED }
        assertEquals("concord_admin", vm.adminRecoveryState.value.revealedUsername)

        // Update username inline after recovery and sign in with updated username
        vm.updateAdminUsernameAfterRecovery("concord_boss")
        waitUntil { vm.adminRecoveryState.value.revealedUsername == "concord_boss" }
        vm.dismissAdminRecovery()

        vm.loginAdminWithPassword("concord_boss", "NewSecure2026")
        waitUntil { vm.authSession.value?.user?.role == UserRole.ADMIN }
        assertEquals(UserRole.ADMIN, vm.authSession.value?.user?.role)
    }

    @Test
    fun `quick calculator engine handles arithmetic, percent, sign toggle, divide by zero, and history reuse`() {
        fun tap(initial: QuickCalculatorState, vararg keys: String): QuickCalculatorState {
            var state = initial
            for (k in keys) {
                state = QuickCalculatorEngine.onKeyPress(state, k)
            }
            return state
        }

        // 12.5 × 4 = 50
        val s1 = tap(QuickCalculatorState(), "1", "2", ".", "5", "×", "4", "=")
        assertEquals("50", s1.display)
        assertEquals(1, s1.history.size)
        assertEquals("12.5 × 4 = 50", s1.history.first())

        // 200 + 10% = 220
        val s2 = tap(s1, "C", "2", "0", "0", "+", "1", "0", "%", "=")
        assertEquals("220", s2.display)
        assertEquals(2, s2.history.size)

        // Sign toggle & backspace: 45 -> -45 -> -4 -> + 9 = 5
        val s3 = tap(s2, "C", "4", "5", "±", "DEL", "+", "9", "=")
        assertEquals("5", s3.display)

        // Divide by zero handled safely without crashing
        val s4 = tap(s3, "C", "8", "÷", "0", "=")
        assertTrue(s4.hasError)
        assertEquals("Cannot ÷ 0", s4.display)

        // Next digit recovers cleanly from error state
        val s5 = tap(s4, "7", "-", "2", "=")
        assertFalse(s5.hasError)
        assertEquals("5", s5.display)

        // Reusing a history entry restores its numeric result
        val s6 = QuickCalculatorEngine.applyHistoryResult(s5, "12.5 × 4 = 50")
        assertEquals("50", s6.display)
        assertFalse(s6.hasError)
    }

    @Test
    fun `staff app login emits notification item and live dashboard banner`() = runTest {
        repository.initializeDatabase()
        val prefs = SessionPreferences(context)
        prefs.clearLoggedInUserId()

        val vm = AppViewModel(
            repository = repository,
            backupManager = backupManager,
            cloudSync = null,
            sessionPreferences = prefs,
            driveBackupManager = null,
            gmailVerificationManager = null,
            appContext = context
        )
        fun waitUntil(timeoutMs: Long = 5000L, condition: () -> Boolean) {
            val deadline = System.currentTimeMillis() + timeoutMs
            while (!condition() && System.currentTimeMillis() < deadline) {
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                Thread.sleep(20)
            }
        }

        waitUntil { vm.isInitialized.value }
        val nadeem = repository.usersFlow.first().first { it.displayName == "Nadeem" }
        assertEquals(UserRole.STAFF, nadeem.role)

        // Staff logs into the app with PIN
        vm.login(nadeem.id, nadeem.pin, expectedRole = UserRole.STAFF)
        waitUntil {
            vm.authSession.value?.user?.id == nadeem.id &&
                vm.latestStaffLoginBanner.value != null &&
                vm.staffLoginNotifications.value.isNotEmpty()
        }

        val banner = vm.latestStaffLoginBanner.value
        assertNotNull(banner)
        assertEquals("Nadeem", banner!!.staffName)
        assertEquals("NADEEM", banner.vehicleName)
        assertTrue(vm.staffLoginNotifications.value.any { it.staffName == "Nadeem" && it.vehicleName == "NADEEM" })

        // Dismissing the banner clears the top live banner while retaining notification history
        vm.dismissStaffLoginBanner()
        assertNull(vm.latestStaffLoginBanner.value)
        assertTrue(vm.staffLoginNotifications.value.isNotEmpty())
    }

    @Test
    fun `owner view creates one hashed password, signs in with password only, and strictly blocks all mutations`() = runTest {
        repository.initializeDatabase()
        val prefs = SessionPreferences(context)
        prefs.clearLoggedInUserId()
        prefs.clearOwnerViewPasswordHash()

        val vm = AppViewModel(
            repository = repository,
            backupManager = backupManager,
            cloudSync = null,
            sessionPreferences = prefs,
            driveBackupManager = null,
            gmailVerificationManager = null,
            appContext = context
        )
        fun waitUntil(timeoutMs: Long = 5000L, condition: () -> Boolean) {
            val deadline = System.currentTimeMillis() + timeoutMs
            while (!condition() && System.currentTimeMillis() < deadline) {
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                Thread.sleep(20)
            }
        }

        waitUntil { vm.isInitialized.value }
        assertFalse(vm.ownerViewConfigured.value)

        // 1. Create single Owner View password
        vm.setupOwnerViewPassword("OwnerView@26", "OwnerView@26")
        waitUntil {
            vm.authSession.value?.user?.role == UserRole.OWNER_VIEW &&
                vm.ownerViewConfigured.value
        }
        assertEquals(UserRole.OWNER_VIEW, vm.authSession.value?.user?.role)
        val storedHash = prefs.ownerViewPasswordHashFlow.first()
        assertTrue(storedHash.startsWith("pbkdf2\$"))
        assertFalse(storedHash.contains("OwnerView@26"))

        // 2. Logout -> wrong password rejected -> correct password logs into Owner View
        vm.logout()
        waitUntil { vm.authSession.value == null }

        vm.loginOwnerViewWithPassword("WrongOwner99")
        waitUntil { vm.loginError.value != null }
        assertNull(vm.authSession.value)

        vm.loginOwnerViewWithPassword("OwnerView@26")
        waitUntil { vm.authSession.value?.user?.role == UserRole.OWNER_VIEW }
        assertEquals(UserRole.OWNER_VIEW, vm.authSession.value?.user?.role)

        // 3. Strictly verify NO entry or modification is permitted in Owner View
        val product = repository.activeProductsFlow.first().first()
        val initialTxCount = repository.transactionsFlow.first().size
        val initialVehicleCount = repository.vehiclesFlow.first().size
        val initialThreshold = vm.lowStockThreshold.value

        vm.recordTransaction(
            type = TransactionType.STORE_PURCHASE_FULL,
            productId = product.id,
            vehicleId = null,
            quantity = 10,
            purchaseCompanyName = "Test Co"
        )
        waitUntil { vm.warningAlert.value != null }
        assertTrue(vm.warningAlert.value!!.title.contains("Owner View"))
        vm.dismissWarningAlert()

        vm.updateOpeningStock(
            scope = "STORE",
            vehicleId = 0L,
            productId = product.id,
            openingFull = 99,
            openingEmpty = 12
        )
        waitUntil { vm.warningAlert.value != null }
        vm.dismissWarningAlert()

        vm.addStaffAndVehicle(
            staffName = "Unauthorized Staff",
            vehicleName = "VAN-X",
            pin = "4321"
        )
        waitUntil { vm.warningAlert.value != null }
        vm.dismissWarningAlert()

        vm.updateLowStockThreshold(initialThreshold + 10)
        waitUntil { vm.warningAlert.value != null }
        assertEquals(initialThreshold, vm.lowStockThreshold.value)
        vm.dismissWarningAlert()

        vm.saveReportToCloudAndDrive(
            reportType = ReportType.DAILY_STORE,
            table = ReportTable(
                reportTitle = "Test",
                subtitle = "Test",
                generatedBy = "Owner View",
                headers = listOf("Col"),
                rows = listOf(listOf("Val"))
            ),
            uploadPdfToDrive = false
        )
        waitUntil { vm.warningAlert.value != null }
        vm.dismissWarningAlert()

        vm.updateOwnerViewPasswordFromSettings("OwnerView@26", "NewPass@26", "NewPass@26")
        waitUntil { vm.warningAlert.value != null }
        vm.dismissWarningAlert()

        // Confirm zero records were created or modified
        assertEquals(initialTxCount, repository.transactionsFlow.first().size)
        assertEquals(initialVehicleCount, repository.vehiclesFlow.first().size)
    }

    @Test
    fun `staff vehicle to vehicle transfer moves full and empty cylinders atomically with negative stock and RBAC guards`() = runTest {
        repository.initializeDatabase()
        val prefs = SessionPreferences(context)
        prefs.clearLoggedInUserId()
        prefs.clearBiometricUnlockState()

        val admin = repository.usersFlow.first().first { it.role == UserRole.ADMIN }
        val nadeem = repository.usersFlow.first().first { it.username == "nadeem" }
        val vehicles = repository.vehiclesFlow.first()
        val nadeemVehicle = vehicles.first { it.staffUserId == nadeem.id }
        val arafathVehicle = vehicles.first { it.id != nadeemVehicle.id }
        val oxygen = repository.activeProductsFlow.first().first { it.name == "Oxygen" }
        val today = java.time.LocalDate.now().toString()

        // Seed store with 20 Full -> dispatch 10 Full to Nadeem -> Nadeem collects 5 Empty from customer
        assertTrue(
            repository.recordTransaction(today, TransactionType.STORE_PURCHASE_FULL, oxygen.id, null, admin, 20, "Opening")
                is OperationResult.Success
        )
        assertTrue(
            repository.recordTransaction(today, TransactionType.VEHICLE_FULL_RECEIVED, oxygen.id, nadeemVehicle.id, admin, 10, "Load")
                is OperationResult.Success
        )
        assertTrue(
            repository.recordTransaction(today, TransactionType.VEHICLE_EMPTY_FROM_CUSTOMER, oxygen.id, nadeemVehicle.id, nadeem, 5, "Collected")
                is OperationResult.Success
        )

        val vm = AppViewModel(
            repository = repository,
            backupManager = backupManager,
            cloudSync = null,
            sessionPreferences = prefs,
            driveBackupManager = null,
            gmailVerificationManager = null,
            appContext = context
        )
        fun waitUntil(timeoutMs: Long = 5000L, condition: () -> Boolean) {
            val deadline = System.currentTimeMillis() + timeoutMs
            while (!condition() && System.currentTimeMillis() < deadline) {
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                Thread.sleep(20)
            }
        }

        waitUntil { vm.isInitialized.value }
        vm.login(nadeem.id, nadeem.pin, expectedRole = UserRole.STAFF)
        waitUntil { vm.authSession.value?.user?.id == nadeem.id }
        vm.clearSnackbar()

        // 1. Transfer 4 Full + 2 Empty from Nadeem -> Arafath
        vm.transferBetweenVehicles(
            sourceVehicleId = nadeemVehicle.id,
            targetVehicleId = arafathVehicle.id,
            productId = oxygen.id,
            fullQuantity = 4,
            emptyQuantity = 2,
            notes = "Mid-route cylinder swap"
        )
        waitUntil { vm.snackbarMessage.value != null }
        assertTrue(
            "Expected transfer confirmation but got: ${vm.snackbarMessage.value}",
            vm.snackbarMessage.value!!.contains("Transferred")
        )
        vm.clearSnackbar()

        val nadeemRow = repository.observeVehicleStock(nadeemVehicle.id, today).first().first { it.product.id == oxygen.id }
        val arafathRow = repository.observeVehicleStock(arafathVehicle.id, today).first().first { it.product.id == oxygen.id }
        val storeRow = repository.observeStoreStock(today).first().first { it.product.id == oxygen.id }

        // Nadeem: 10-4 = 6 Full, 5-2 = 3 Empty; Arafath: 0+4 = 4 Full, 0+2 = 2 Empty; Store unchanged (10 Full, 0 Empty)
        assertEquals(6, nadeemRow.fullBalance)
        assertEquals(3, nadeemRow.emptyBalance)
        assertEquals(4, nadeemRow.fullTransferredOut)
        assertEquals(2, nadeemRow.emptyTransferredOut)
        assertEquals(4, arafathRow.fullBalance)
        assertEquals(2, arafathRow.emptyBalance)
        assertEquals(10, storeRow.storeFullBalance)
        assertEquals(0, storeRow.storeEmptyBalance)

        // 2. Negative stock prevention: trying to transfer 7 Full when only 6 available must fail without modifying stock
        vm.transferBetweenVehicles(
            sourceVehicleId = nadeemVehicle.id,
            targetVehicleId = arafathVehicle.id,
            productId = oxygen.id,
            fullQuantity = 7,
            emptyQuantity = 0,
            notes = "Too many"
        )
        waitUntil { vm.warningAlert.value != null }
        assertEquals("Insufficient Vehicle Full Stock", vm.warningAlert.value!!.title)
        vm.dismissWarningAlert()
        assertEquals(6, repository.observeVehicleStock(nadeemVehicle.id, today).first().first { it.product.id == oxygen.id }.fullBalance)

        // 3. Staff cannot transfer OUT of another staff member's vehicle
        vm.transferBetweenVehicles(
            sourceVehicleId = arafathVehicle.id,
            targetVehicleId = nadeemVehicle.id,
            productId = oxygen.id,
            fullQuantity = 1,
            emptyQuantity = 0,
            notes = "Unauthorized"
        )
        waitUntil { vm.warningAlert.value != null }
        assertEquals("Permission Denied", vm.warningAlert.value!!.title)
        vm.dismissWarningAlert()

        // 4. Owner View is strictly blocked from performing vehicle-to-vehicle transfers
        vm.logout()
        waitUntil { vm.authSession.value == null }
        vm.setupOwnerViewPassword("OwnerLock@26", "OwnerLock@26")
        waitUntil { vm.authSession.value?.user?.role == UserRole.OWNER_VIEW }

        vm.transferBetweenVehicles(
            sourceVehicleId = nadeemVehicle.id,
            targetVehicleId = arafathVehicle.id,
            productId = oxygen.id,
            fullQuantity = 1,
            emptyQuantity = 1,
            notes = "Owner blocked"
        )
        waitUntil { vm.warningAlert.value != null }
        assertTrue(vm.warningAlert.value!!.title.contains("Owner View"))
    }

    @Test
    fun `biometric unlock arms after first password login, unlocks via biometric callback, and resets on explicit logout`() = runTest {
        repository.initializeDatabase()
        val prefs = SessionPreferences(context)
        prefs.clearLoggedInUserId()
        prefs.clearAdminCredentials()
        prefs.clearBiometricUnlockState()

        // Verify branding strings: full company headline & short Play-Policy launcher label
        assertEquals("Concord & Atoz Gas Trading LLC", context.getString(R.string.company_full_name))
        assertEquals("C&A Gas", context.getString(R.string.app_name))

        val vm = AppViewModel(
            repository = repository,
            backupManager = backupManager,
            cloudSync = null,
            sessionPreferences = prefs,
            driveBackupManager = null,
            gmailVerificationManager = null,
            appContext = context
        )
        fun waitUntil(timeoutMs: Long = 5000L, condition: () -> Boolean) {
            val deadline = System.currentTimeMillis() + timeoutMs
            while (!condition() && System.currentTimeMillis() < deadline) {
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                Thread.sleep(20)
            }
        }

        waitUntil { vm.isInitialized.value }
        assertFalse(vm.biometricUnlockState.value.isReadyForBiometric)

        // 1. First-time Admin password setup arms Fingerprint/Face unlock
        vm.setupInitialAdminCredentials(
            username = "concord_boss",
            password = "FirstLogin@26",
            confirmPassword = "FirstLogin@26",
            recoveryEmail = "atozgas26@gmail.com"
        )
        waitUntil {
            vm.authSession.value?.user?.role == UserRole.ADMIN &&
                vm.biometricUnlockState.value.isReadyForBiometric
        }
        assertEquals(UserRole.ADMIN, vm.biometricUnlockState.value.role)
        assertFalse(vm.biometricUnlockState.value.passwordRequiredAfterLogout)

        // 2. Auto-lock (background/minimize) keeps biometric unlock armed -> Fingerprint/Face restores session without password
        vm.lockSessionOnAutoLogout("Locked on minimize")
        waitUntil { vm.authSession.value == null }
        assertTrue(vm.biometricUnlockState.value.isReadyForBiometric)

        vm.completeBiometricUnlock()
        waitUntil { vm.authSession.value?.user?.role == UserRole.ADMIN }
        assertEquals(UserRole.ADMIN, vm.authSession.value?.user?.role)

        // 3. Explicit Logout (1 time) disarms biometric unlock and requires Password once before re-enabling
        vm.logout()
        waitUntil {
            vm.authSession.value == null &&
                !vm.biometricUnlockState.value.armed &&
                vm.biometricUnlockState.value.passwordRequiredAfterLogout
        }
        assertFalse(vm.biometricUnlockState.value.isReadyForBiometric)

        // Attempting biometric unlock while logged out must be rejected
        vm.completeBiometricUnlock()
        waitUntil { vm.loginError.value != null }
        assertNull(vm.authSession.value)

        // 4. Signing in once with Password opens app AND re-arms Fingerprint/Face unlock for next opens
        vm.loginAdminWithPassword("concord_boss", "FirstLogin@26")
        waitUntil {
            vm.authSession.value?.user?.role == UserRole.ADMIN &&
                vm.biometricUnlockState.value.isReadyForBiometric
        }
        assertFalse(vm.biometricUnlockState.value.passwordRequiredAfterLogout)
        assertTrue(vm.biometricUnlockState.value.armed)
    }

    @Test
    fun `staff daily and monthly product cylinder wise sales report and purchase company product cylinder wise report compute accurately`() = runTest {
        repository.initializeDatabase()
        val prefs = SessionPreferences(context)
        prefs.clearLoggedInUserId()
        prefs.clearBiometricUnlockState()

        val admin = repository.usersFlow.first().first { it.role == UserRole.ADMIN }
        val nadeem = repository.usersFlow.first().first { it.username == "nadeem" }
        val vehicles = repository.vehiclesFlow.first()
        val nadeemVehicle = vehicles.first { it.staffUserId == nadeem.id }
        val sivanVehicle = vehicles.first { it.name == "SIVAN" }
        val products = repository.activeProductsFlow.first()
        val oxygen = products.first { it.name == "Oxygen" }
        val argon = products.first { it.name == "Argon" }

        val day1 = "2026-11-10"
        val day2 = "2026-11-25"
        val month = "2026-11"

        // Purchases from two companies across 2 days of the same month
        // Emirates Gas on day1: 20 Full Oxygen, 5 Empty Oxygen; on day2: 12 Full Oxygen, 4 Comp Repl Oxygen, 10 Full Argon
        assertTrue(repository.recordTransaction(day1, TransactionType.STORE_PURCHASE_FULL, oxygen.id, null, admin, 20, "", purchaseCompanyName = "Emirates Gas") is OperationResult.Success)
        assertTrue(repository.recordTransaction(day1, TransactionType.STORE_PURCHASE_EMPTY, oxygen.id, null, admin, 5, "", purchaseCompanyName = "Emirates Gas") is OperationResult.Success)
        assertTrue(repository.recordTransaction(day2, TransactionType.STORE_PURCHASE_FULL, oxygen.id, null, admin, 12, "", purchaseCompanyName = "Emirates Gas") is OperationResult.Success)
        assertTrue(repository.recordTransaction(day2, TransactionType.STORE_PURCHASE_COMPLAINT_REPLACE, oxygen.id, null, admin, 4, "", purchaseCompanyName = "Emirates Gas") is OperationResult.Success)
        assertTrue(repository.recordTransaction(day2, TransactionType.STORE_PURCHASE_FULL, argon.id, null, admin, 10, "", purchaseCompanyName = "Emirates Gas") is OperationResult.Success)
        // ADNOC on day2: 8 Full Argon
        assertTrue(repository.recordTransaction(day2, TransactionType.STORE_PURCHASE_FULL, argon.id, null, admin, 8, "", purchaseCompanyName = "ADNOC Gas") is OperationResult.Success)

        // Dispatch full to Nadeem & Sivan across day1 and day2
        assertTrue(repository.recordTransaction(day1, TransactionType.VEHICLE_FULL_RECEIVED, oxygen.id, nadeemVehicle.id, admin, 10, "") is OperationResult.Success)
        assertTrue(repository.recordTransaction(day2, TransactionType.VEHICLE_FULL_RECEIVED, oxygen.id, nadeemVehicle.id, admin, 8, "") is OperationResult.Success)
        assertTrue(repository.recordTransaction(day2, TransactionType.VEHICLE_FULL_RECEIVED, argon.id, nadeemVehicle.id, admin, 6, "") is OperationResult.Success)
        assertTrue(repository.recordTransaction(day2, TransactionType.VEHICLE_FULL_RECEIVED, argon.id, sivanVehicle.id, admin, 5, "") is OperationResult.Success)

        // Sales by Nadeem & Sivan across day1 & day2
        // Day1: Nadeem sells 4 Reg Oxygen + collects 3 Empty Oxygen
        assertTrue(repository.recordTransaction(day1, TransactionType.VEHICLE_CUSTOMER_SALE, oxygen.id, nadeemVehicle.id, nadeem, 4, "", customerName = "Alpha Fab") is OperationResult.Success)
        assertTrue(repository.recordTransaction(day1, TransactionType.VEHICLE_EMPTY_FROM_CUSTOMER, oxygen.id, nadeemVehicle.id, nadeem, 3, "", customerName = "Alpha Fab") is OperationResult.Success)
        // Day2: Nadeem sells 3 Reg Oxygen, 2 New Cust Oxygen, 1 Comp Repl Oxygen, 5 Reg Argon; Sivan sells 2 Reg Argon
        assertTrue(repository.recordTransaction(day2, TransactionType.VEHICLE_CUSTOMER_SALE, oxygen.id, nadeemVehicle.id, nadeem, 3, "", customerName = "Beta Steel") is OperationResult.Success)
        assertTrue(repository.recordTransaction(day2, TransactionType.VEHICLE_NEW_CUSTOMER_SALE, oxygen.id, nadeemVehicle.id, nadeem, 2, "", customerName = "New Grill") is OperationResult.Success)
        assertTrue(repository.recordTransaction(day2, TransactionType.VEHICLE_CUSTOMER_COMPLAINT_REPLACE, oxygen.id, nadeemVehicle.id, nadeem, 1, "", customerName = "Beta Steel") is OperationResult.Success)
        assertTrue(repository.recordTransaction(day2, TransactionType.VEHICLE_CUSTOMER_SALE, argon.id, nadeemVehicle.id, nadeem, 5, "", customerName = "Welding Hub") is OperationResult.Success)
        assertTrue(repository.recordTransaction(day2, TransactionType.VEHICLE_CUSTOMER_SALE, argon.id, sivanVehicle.id, admin, 2, "", customerName = "Gulf Fab") is OperationResult.Success)

        val vm = AppViewModel(
            repository = repository,
            backupManager = backupManager,
            cloudSync = null,
            sessionPreferences = prefs,
            driveBackupManager = null,
            gmailVerificationManager = null,
            appContext = context
        )
        val txCollector = backgroundScope.launch(Dispatchers.Unconfined) { vm.allTransactionDisplayItems.collect {} }
        val vehCollector = backgroundScope.launch(Dispatchers.Unconfined) { vm.vehicles.collect {} }
        val prodCollector = backgroundScope.launch(Dispatchers.Unconfined) { vm.allProducts.collect {} }

        fun waitUntil(timeoutMs: Long = 5000L, condition: () -> Boolean) {
            val deadline = System.currentTimeMillis() + timeoutMs
            while (!condition() && System.currentTimeMillis() < deadline) {
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                Thread.sleep(20)
            }
        }
        waitUntil {
            vm.isInitialized.value &&
                vm.vehicles.value.isNotEmpty() &&
                vm.allProducts.value.isNotEmpty() &&
                vm.allTransactionDisplayItems.value.size >= 16
        }
        vm.setSelectedDate(day2)
        vm.setSelectedYearMonth(month)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // 1. Staff DAILY Product Cylinder-Wise Report (Nadeem only on day2)
        val staffDailyNadeem = vm.buildReportTable(
            reportType = ReportType.STAFF_PRODUCT_SALES,
            vehicleFilterId = nadeemVehicle.id,
            purchaseDailyMode = true
        )
        val nadeemDailyOx = staffDailyNadeem.rows.first { it[1] == "Oxygen" }
        val nadeemDailyAr = staffDailyNadeem.rows.first { it[1] == "Argon" }
        // Columns: Category, Product, Full Recd, Reg. Sales, New Cust, Total Sold, Cust Empty, Comp. Repl
        assertEquals(listOf("8", "3", "2", "5", "0", "1"), nadeemDailyOx.subList(2, 8))
        assertEquals(listOf("6", "5", "0", "5", "0", "0"), nadeemDailyAr.subList(2, 8))
        assertEquals("10", staffDailyNadeem.footerTotals?.get(5)) // 5 Oxygen + 5 Argon = 10 sold on day2

        // 2. Staff MONTHLY Product Cylinder-Wise Report (Nadeem across day1 + day2)
        val staffMonthlyNadeem = vm.buildReportTable(
            reportType = ReportType.STAFF_PRODUCT_SALES,
            vehicleFilterId = nadeemVehicle.id,
            purchaseDailyMode = false
        )
        val nadeemMonthlyOx = staffMonthlyNadeem.rows.first { it[1] == "Oxygen" }
        // Monthly Oxygen: Full Recd 10+8=18, Reg 4+3=7, New 2, Total Sold 9, Cust Empty 3, Comp Repl 1
        assertEquals(listOf("18", "7", "2", "9", "3", "1"), nadeemMonthlyOx.subList(2, 8))
        assertEquals("14", staffMonthlyNadeem.footerTotals?.get(5)) // 9 Oxygen + 5 Argon = 14

        // 3. Staff MONTHLY Product Cylinder-Wise Report (All Staff includes Nadeem + Sivan)
        val staffMonthlyAll = vm.buildReportTable(
            reportType = ReportType.STAFF_PRODUCT_SALES,
            vehicleFilterId = null,
            purchaseDailyMode = false
        )
        assertEquals("16", staffMonthlyAll.footerTotals?.get(5)) // Nadeem 14 + Sivan 2 = 16

        // 4. Purchase Company DAILY Product Cylinder-Wise Report (All Suppliers on day2)
        val purchaseDailyAll = vm.buildReportTable(
            reportType = ReportType.PURCHASE_COMPANY_PRODUCT,
            vehicleFilterId = null,
            purchaseDailyMode = true,
            purchaseCompanyFilter = null
        )
        // Day2: ADNOC Gas Argon (8 Full), Emirates Gas Oxygen (12 Full + 4 Comp Repl = 16), Emirates Gas Argon (10 Full)
        assertEquals(3, purchaseDailyAll.rows.size)
        assertEquals("30", purchaseDailyAll.footerTotals?.get(2)) // Full Pur: 8 + 12 + 10
        assertEquals("4", purchaseDailyAll.footerTotals?.get(4))  // Comp Repl: 4
        assertEquals("34", purchaseDailyAll.footerTotals?.get(5)) // Total Cyl: 34

        // 5. Purchase Company MONTHLY Product Cylinder-Wise Report (Emirates Gas only across day1 + day2)
        val purchaseMonthlyEmirates = vm.buildReportTable(
            reportType = ReportType.PURCHASE_COMPANY_PRODUCT,
            vehicleFilterId = null,
            purchaseDailyMode = false,
            purchaseCompanyFilter = "Emirates Gas"
        )
        assertEquals(2, purchaseMonthlyEmirates.rows.size) // Oxygen & Argon
        val emiratesOx = purchaseMonthlyEmirates.rows.first { it[1] == "Oxygen" }
        val emiratesAr = purchaseMonthlyEmirates.rows.first { it[1] == "Argon" }
        // Columns: Category, Product, Full Pur, Empty Pur, Comp. Repl, Total Cyl, Entries
        assertEquals(listOf("32", "5", "4", "41", "4"), emiratesOx.subList(2, 7))
        assertEquals(listOf("10", "0", "0", "10", "1"), emiratesAr.subList(2, 7))
        assertEquals("51", purchaseMonthlyEmirates.footerTotals?.get(5))
        txCollector.cancel()
        vehCollector.cancel()
        prodCollector.cancel()

        // 6. Verify both reports export valid XLSX (.xlsx ZIP) bytes
        val staffXlsxOut = ByteArrayOutputStream()
        ReportExporter.writeXlsx(staffMonthlyAll, staffXlsxOut)
        val staffBytes = staffXlsxOut.toByteArray()
        assertTrue(staffBytes.size > 100 && staffBytes[0] == 0x50.toByte() && staffBytes[1] == 0x4B.toByte())

        val purchaseXlsxOut = ByteArrayOutputStream()
        ReportExporter.writeXlsx(purchaseMonthlyEmirates, purchaseXlsxOut)
        val purchaseBytes = purchaseXlsxOut.toByteArray()
        assertTrue(purchaseBytes.size > 100 && purchaseBytes[0] == 0x50.toByte() && purchaseBytes[1] == 0x4B.toByte())
    }

    @Test
    fun `app system update manager compares semver, enforces trusted github release urls, and updates viewmodel state`() = runTest {
        assertEquals("1.11.0", BuildConfig.VERSION_NAME)
        assertEquals(11, BuildConfig.VERSION_CODE)

        // 1. Semantic version comparisons
        assertTrue(AppUpdateManager.compareSemVer("v2.0.0", "1.9.0") > 0)
        assertEquals(0, AppUpdateManager.compareSemVer("v1.9.0", "1.9.0"))
        assertTrue(AppUpdateManager.compareSemVer("v1.8.0", "1.9.0") < 0)
        assertTrue(AppUpdateManager.compareSemVer("1.10.0", "1.9.5") > 0)
        assertEquals("1.9.0", AppUpdateManager.normalizeVersion("v1.9.0-beta1"))

        // 2. Strict trusted HTTPS release URL validation
        assertTrue(
            AppUpdateManager.isTrustedReleaseUrl(
                "https://github.com/atozgas26-star/Concord-Atoz-gas/releases/download/v1.9.0/CA-Gas-v1.9.0.apk"
            )
        )
        assertTrue(
            AppUpdateManager.isTrustedReleaseUrl(
                "https://github.com/atozgas26-star/Concord-Atoz-gas/releases/tag/v1.9.0"
            )
        )
        assertFalse(
            AppUpdateManager.isTrustedReleaseUrl(
                "http://github.com/atozgas26-star/Concord-Atoz-gas/releases/download/v1.9.0/CA-Gas-v1.9.0.apk"
            )
        )
        assertFalse(
            AppUpdateManager.isTrustedReleaseUrl(
                "https://evil.example.com/atozgas26-star/Concord-Atoz-gas/releases/download/v1.9.0/CA-Gas-v1.9.0.apk"
            )
        )
        assertFalse(
            AppUpdateManager.isTrustedReleaseUrl("javascript:alert(1)")
        )
        assertFalse(
            AppUpdateManager.isTrustedReleaseUrl(
                "https://github.com/other-org/other-repo/releases/download/v1.9.0/app.apk"
            )
        )
        assertFalse(
            AppUpdateManager.isTrustedReleaseUrl(
                "https://github.com/atozgas26-star/Concord-Atoz-gas/releases/../../other/evil.apk"
            )
        )
        assertFalse(
            AppUpdateManager.isTrustedReleaseUrl(
                "https://user@github.com/atozgas26-star/Concord-Atoz-gas/releases/tag/v1.9.0"
            )
        )

        // 3. Mocked GitHub Releases API responses driven through AppViewModel
        var mockedCode = 200
        var mockedBody = """
            {
              "tag_name": "v2.0.0",
              "name": "C&A Gas v2.0.0",
              "body": "Added offline sync enhancements and invoice fixes.",
              "published_at": "2026-12-01T10:00:00Z",
              "html_url": "https://github.com/atozgas26-star/Concord-Atoz-gas/releases/tag/v2.0.0",
              "assets": [
                {
                  "name": "tampered.apk",
                  "browser_download_url": "https://evil.example.com/atozgas26-star/Concord-Atoz-gas/releases/download/v2.0.0/tampered.apk",
                  "size": 1048576
                },
                {
                  "name": "CA-Gas-v2.0.0.apk",
                  "browser_download_url": "https://github.com/atozgas26-star/Concord-Atoz-gas/releases/download/v2.0.0/CA-Gas-v2.0.0.apk",
                  "size": 31457280
                }
              ]
            }
        """.trimIndent()

        val mockClient = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(mockedCode)
                    .message(if (mockedCode == 200) "OK" else "Error")
                    .body(mockedBody.toResponseBody("application/json".toMediaType()))
                    .build()
            })
            .build()

        val updateManager = AppUpdateManager(httpClient = mockClient)
        val prefs = SessionPreferences(context)
        val vm = AppViewModel(
            repository = repository,
            backupManager = backupManager,
            cloudSync = null,
            sessionPreferences = prefs,
            driveBackupManager = null,
            gmailVerificationManager = null,
            appContext = context,
            appUpdateManager = updateManager
        )

        fun waitUntil(timeoutMs: Long = 5000L, condition: () -> Boolean) {
            val deadline = System.currentTimeMillis() + timeoutMs
            while (!condition() && System.currentTimeMillis() < deadline) {
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                Thread.sleep(20)
            }
        }

        // Case A: Newer release v2.0.0 available -> skips tampered asset URL, picks trusted official APK URL
        vm.checkForAppUpdate(silent = false)
        waitUntil { !vm.appUpdateState.value.isChecking && vm.appUpdateState.value.status != AppUpdateStatus.IDLE }
        val available = vm.appUpdateState.value
        assertEquals(AppUpdateStatus.UPDATE_AVAILABLE, available.status)
        assertEquals("2.0.0", available.latestVersion)
        assertTrue(available.shouldShowBanner)
        assertEquals(
            "https://github.com/atozgas26-star/Concord-Atoz-gas/releases/download/v2.0.0/CA-Gas-v2.0.0.apk",
            available.downloadUrl
        )
        assertEquals(30.0, available.apkSizeMb ?: 0.0, 0.1)
        assertTrue(available.releaseNotes.contains("offline sync"))

        // Dismiss banner hides banner while keeping update status
        vm.dismissAppUpdateBanner()
        assertFalse(vm.appUpdateState.value.shouldShowBanner)
        assertEquals(AppUpdateStatus.UPDATE_AVAILABLE, vm.appUpdateState.value.status)

        // Case B: Up-to-date release v1.10.0
        mockedBody = """
            {
              "tag_name": "v1.10.0",
              "name": "C&A Gas v1.10.0",
              "body": "Current version.",
              "published_at": "2026-11-26T10:00:00Z",
              "html_url": "https://github.com/atozgas26-star/Concord-Atoz-gas/releases/tag/v1.10.0",
              "assets": []
            }
        """.trimIndent()
        vm.checkForAppUpdate(silent = false)
        waitUntil { !vm.appUpdateState.value.isChecking && vm.appUpdateState.value.status == AppUpdateStatus.UP_TO_DATE }
        assertEquals(AppUpdateStatus.UP_TO_DATE, vm.appUpdateState.value.status)
        assertFalse(vm.appUpdateState.value.shouldShowBanner)

        // Case C: Server HTTP 500 error
        mockedCode = 500
        mockedBody = """{"message": "Internal Error"}"""
        vm.checkForAppUpdate(silent = false)
        waitUntil { !vm.appUpdateState.value.isChecking && vm.appUpdateState.value.status == AppUpdateStatus.ERROR }
        assertEquals(AppUpdateStatus.ERROR, vm.appUpdateState.value.status)
        assertNotNull(vm.appUpdateState.value.errorMessage)
    }

    @Test
    fun `staff 4-stage duty attendance calculates break and net work hours and populates admin work hours report`() = runTest {
        repository.initializeDatabase()
        val nadeem = repository.usersFlow.first().first { it.username == "nadeem" }
        val nadeemVeh = repository.vehiclesFlow.first().first { it.staffUserId == nadeem.id }
        val day = "2026-11-26"

        // 08:00 start -> 13:00 lunch out -> 14:00 break in (60m break) -> 18:00 duty close (9h net work)
        val base = 1_795_680_000_000L
        val tStart = base
        val tLunchOut = base + 5 * 3_600_000L
        val tLunchIn = base + 6 * 3_600_000L
        val tClose = base + 10 * 3_600_000L

        repository.upsertStaffAttendance(
            com.example.data.local.StaffAttendanceEntity(
                remoteId = "att_${day}_${nadeem.id}",
                businessDate = day,
                staffId = nadeem.id,
                staffName = nadeem.displayName,
                vehicleName = nadeemVeh.name,
                dutyStartMs = tStart,
                lunchOutMs = tLunchOut,
                lunchInMs = tLunchIn,
                dutyCloseMs = tClose,
                updatedAtMs = tClose
            )
        )

        val record = repository.allAttendanceFlow.first().first { it.staffId == nadeem.id && it.businessDate == day }
        assertEquals(60L, record.breakDurationMinutes())
        assertEquals(540L, record.netWorkMinutes()) // 9 hours net

        // Build Admin-Only Daily Work Hours Report via ViewModel
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
        val initDeadline = System.currentTimeMillis() + 5000L
        while (!vm.isInitialized.value && System.currentTimeMillis() < initDeadline) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(20)
        }
        vm.setSelectedDate(day)
        val attJob = launch(Dispatchers.Unconfined) { vm.selectedDateAttendance.collect {} }
        val userJob = launch(Dispatchers.Unconfined) { vm.users.collect {} }
        val vehJob = launch(Dispatchers.Unconfined) { vm.vehicles.collect {} }
        val syncDeadline = System.currentTimeMillis() + 3000L
        while ((vm.selectedDateAttendance.value.isEmpty() || vm.users.value.isEmpty() || vm.vehicles.value.isEmpty()) &&
            System.currentTimeMillis() < syncDeadline
        ) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(20)
        }

        val table = vm.buildReportTable(ReportType.STAFF_WORK_HOURS)
        assertEquals("Staff Daily Work Hours Report", table.reportTitle)
        val nadeemRow = table.rows.first { it[0] == "Nadeem" }
        assertEquals("1h 00m", nadeemRow[6])
        assertEquals("9h 00m", nadeemRow[7])
        assertEquals("Duty Closed", nadeemRow[8])

        // Also verify Staff punching through AppViewModel persists dutyStartMs and notifies Admin
        vm.login(nadeem.id, "1001", UserRole.STAFF)
        val loginDl = System.currentTimeMillis() + 5000L
        while (vm.authSession.value?.user?.role != UserRole.STAFF && System.currentTimeMillis() < loginDl) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(20)
        }
        vm.setSelectedDate("2026-11-27")
        vm.punchStaffDuty("DUTY_START")
        val punchDl = System.currentTimeMillis() + 5000L
        while (repository.getAttendanceForStaffAndDate("2026-11-27", nadeem.id) == null && System.currentTimeMillis() < punchDl) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(20)
        }
        val punched = repository.getAttendanceForStaffAndDate("2026-11-27", nadeem.id)
        assertNotNull(punched)
        assertTrue(punched!!.dutyStartMs > 0L)

        attJob.cancel()
        userJob.cancel()
        vehJob.cancel()
    }

    @Test
    fun `vehicle to vehicle extra full and empty collect auto-sums and notifies destination staff bell`() = runTest {
        repository.initializeDatabase()
        val admin = repository.usersFlow.first().first { it.role == UserRole.ADMIN }
        val nadeem = repository.usersFlow.first().first { it.username == "nadeem" }
        val nadeemVeh = repository.vehiclesFlow.first().first { it.staffUserId == nadeem.id }
        val sivanVeh = repository.vehiclesFlow.first().first { it.name == "SIVAN" }
        val sivanUser = repository.usersFlow.first().first { it.id == sivanVeh.staffUserId }
        val oxygen = repository.activeProductsFlow.first().first { it.name == "Oxygen" }
        val day = "2026-11-26"

        // Seed NADEEM with 20 Full and 10 Empty
        assertTrue(repository.setOpeningStockForDate(StockCalculator.SCOPE_VEHICLE, nadeemVeh.id, oxygen.id, day, 20, 10, admin) is OperationResult.Success)

        // 1. Standard transfer (extraFull = 0, extraEmpty = 0) -> no "Extra" mention in notes
        val stdRes = repository.transferBetweenVehicles(
            businessDate = day,
            sourceVehicleId = nadeemVeh.id,
            targetVehicleId = sivanVeh.id,
            productId = oxygen.id,
            actor = nadeem,
            fullQuantity = 4,
            emptyQuantity = 2,
            extraFullQuantity = 0,
            extraEmptyQuantity = 0,
            notes = ""
        )
        assertTrue(stdRes is OperationResult.Success)
        val stdTx = repository.transactionsFlow.first().first { it.type == TransactionType.VEHICLE_TRANSFER_FULL_OUT }
        assertEquals(4, stdTx.quantity)
        assertFalse(stdTx.notes.contains("Extra", ignoreCase = true))

        // 2. Transfer WITH Extra Full (+3) and Extra Empty (+2) through AppViewModel
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
        val deadline = System.currentTimeMillis() + 5000L
        while (!vm.isInitialized.value && System.currentTimeMillis() < deadline) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(20)
        }
        vm.login(nadeem.id, "1001", UserRole.STAFF)
        val loginDeadline = System.currentTimeMillis() + 5000L
        while (vm.authSession.value == null && System.currentTimeMillis() < loginDeadline) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(20)
        }
        vm.setSelectedDate(day)

        vm.transferBetweenVehicles(
            sourceVehicleId = nadeemVeh.id,
            targetVehicleId = sivanVeh.id,
            productId = oxygen.id,
            fullQuantity = 5,
            emptyQuantity = 1,
            notes = "Urgent route help",
            extraFullQuantity = 3,
            extraEmptyQuantity = 2
        )
        val txDeadline = System.currentTimeMillis() + 5000L
        while (repository.allNotificationsFlow.first().none { it.category == NotificationCategory.TRANSFER_EXTRA.name } &&
            System.currentTimeMillis() < txDeadline
        ) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(20)
        }

        // Auto-summed totals: SIVAN Full = 4 + (5+3) = 12; Empty = 2 + (1+2) = 5
        val sivanStock = repository.observeVehicleStock(sivanVeh.id, day).first().first { it.product.id == oxygen.id }
        assertEquals(12, sivanStock.fullBalance)
        assertEquals(5, sivanStock.emptyBalance)

        // Destination staff (Sivan) received urgent TRANSFER_EXTRA bell notification mentioning Extra cylinders
        val notifs = repository.allNotificationsFlow.first()
        val destNotif = notifs.firstOrNull {
            it.targetScope == NotificationTargetScope.STAFF.name &&
                it.targetStaffId == sivanUser.id &&
                it.category == NotificationCategory.TRANSFER_EXTRA.name
        }
        assertNotNull(destNotif)
        assertTrue(destNotif!!.isUrgent)
        assertTrue(destNotif.message.contains("Full: 5 + Extra 3 = 8"))
        assertTrue(destNotif.message.contains("Empty: 1 + Extra 2 = 3"))
        assertTrue(destNotif.message.contains("Total: 11"))
    }

    @Test
    fun `staff store full receive and empty return require admin accept entry before stock updates`() = runTest {
        repository.initializeDatabase()
        val admin = repository.usersFlow.first().first { it.role == UserRole.ADMIN }
        val nadeem = repository.usersFlow.first().first { it.username == "nadeem" }
        val nadeemVeh = repository.vehiclesFlow.first().first { it.staffUserId == nadeem.id }
        val oxygen = repository.activeProductsFlow.first().first { it.name == "Oxygen" }
        val day = "2026-11-26"

        // Store starts with 20 Full, 5 Empty; NADEEM starts with 0 Full, 6 Empty
        assertTrue(repository.setOpeningStockForDate(StockCalculator.SCOPE_STORE, 0L, oxygen.id, day, 20, 5, admin) is OperationResult.Success)
        assertTrue(repository.setOpeningStockForDate(StockCalculator.SCOPE_VEHICLE, nadeemVeh.id, oxygen.id, day, 0, 6, admin) is OperationResult.Success)

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
        suspend fun waitUntil(condition: suspend () -> Boolean) {
            val dl = System.currentTimeMillis() + 5000L
            while (!condition() && System.currentTimeMillis() < dl) {
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                Thread.sleep(20)
            }
        }
        waitUntil { vm.isInitialized.value }

        // 1. Staff logs in and submits Store->Vehicle Full Received (8) & Vehicle->Store Empty Return (4)
        vm.login(nadeem.id, "1001", UserRole.STAFF)
        waitUntil { vm.authSession.value?.user?.role == UserRole.STAFF }
        vm.setSelectedDate(day)

        vm.submitStaffStoreTransferRequest(StoreTransferDirection.STORE_TO_VEHICLE_FULL, oxygen.id, 8, "Morning load")
        vm.submitStaffStoreTransferRequest(StoreTransferDirection.VEHICLE_TO_STORE_EMPTY, oxygen.id, 4, "Evening empties")
        waitUntil {
            repository.allPendingStoreTransfersFlow.first().size == 2 &&
                repository.allNotificationsFlow.first().count {
                    it.targetScope == NotificationTargetScope.ADMIN_OWNER.name &&
                        it.category == NotificationCategory.STORE_APPROVAL.name
                } == 2
        }

        val requests = repository.allPendingStoreTransfersFlow.first()
        assertTrue(requests.all { it.status == ApprovalStatus.PENDING.name })

        // Stock MUST NOT have changed yet prior to Admin approval!
        val storeBefore = repository.observeStoreStock(day).first().first { it.product.id == oxygen.id }
        assertEquals(20, storeBefore.storeFullBalance)
        assertEquals(5, storeBefore.storeEmptyBalance)

        // Urgent Red-Dot Bell notifications sent to Admin
        val adminBellAlerts = repository.allNotificationsFlow.first().filter {
            it.targetScope == NotificationTargetScope.ADMIN_OWNER.name &&
                it.category == NotificationCategory.STORE_APPROVAL.name &&
                it.isUrgent
        }
        assertEquals(2, adminBellAlerts.size)

        // Staff CANNOT self-approve
        val fullReq = requests.first { it.direction == StoreTransferDirection.STORE_TO_VEHICLE_FULL.name }
        val emptyReq = requests.first { it.direction == StoreTransferDirection.VEHICLE_TO_STORE_EMPTY.name }
        vm.approvePendingStoreTransfer(fullReq.id)
        assertEquals("Admin Approval Required", vm.warningAlert.value?.title)
        vm.dismissWarningAlert()

        // 2. Admin logs in and accepts both requests
        vm.logout()
        waitUntil { vm.authSession.value == null }
        vm.login(admin.id, "1725", UserRole.ADMIN)
        waitUntil { vm.authSession.value?.user?.role == UserRole.ADMIN }
        vm.setSelectedDate(day)

        vm.approvePendingStoreTransfer(fullReq.id)
        vm.approvePendingStoreTransfer(emptyReq.id)
        waitUntil {
            repository.allPendingStoreTransfersFlow.first().all { it.status == ApprovalStatus.APPROVED.name } &&
                repository.allNotificationsFlow.first().count {
                    it.targetScope == NotificationTargetScope.STAFF.name &&
                        it.targetStaffId == nadeem.id &&
                        it.category == NotificationCategory.STORE_APPROVAL.name
                } == 2
        }

        // Now stock reflects both accepted entries: Store Full = 12, Store Empty = 9; Vehicle Full = 8, Vehicle Empty = 2
        val storeAfter = repository.observeStoreStock(day).first().first { it.product.id == oxygen.id }
        val vehAfter = repository.observeVehicleStock(nadeemVeh.id, day).first().first { it.product.id == oxygen.id }
        assertEquals(12, storeAfter.storeFullBalance)
        assertEquals(9, storeAfter.storeEmptyBalance)
        assertEquals(8, vehAfter.fullBalance)
        assertEquals(2, vehAfter.emptyBalance)

        // Staff received confirmation Bell notifications
        val staffConfirmAlerts = repository.allNotificationsFlow.first().filter {
            it.targetScope == NotificationTargetScope.STAFF.name &&
                it.targetStaffId == nadeem.id &&
                it.category == NotificationCategory.STORE_APPROVAL.name
        }
        assertEquals(2, staffConfirmAlerts.size)
    }

    @Test
    fun `team chat and admin-owner private chat isolate staff and trigger bell alerts for important messages`() = runTest {
        repository.initializeDatabase()
        val admin = repository.usersFlow.first().first { it.role == UserRole.ADMIN }
        val nadeem = repository.usersFlow.first().first { it.username == "nadeem" }

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
        suspend fun waitUntil(condition: suspend () -> Boolean) {
            val dl = System.currentTimeMillis() + 5000L
            while (!condition() && System.currentTimeMillis() < dl) {
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                Thread.sleep(20)
            }
        }
        waitUntil { vm.isInitialized.value }

        // 1. Staff logs in: cannot post to ADMIN_OWNER channel, CAN post to TEAM_CHAT
        vm.login(nadeem.id, "1001", UserRole.STAFF)
        waitUntil { vm.authSession.value?.user?.role == UserRole.STAFF }

        vm.sendChatMessage(ChatChannel.ADMIN_OWNER, "Staff snooping", isImportant = false)
        assertEquals("Restricted Channel", vm.warningAlert.value?.title)
        vm.dismissWarningAlert()

        vm.sendChatMessage(ChatChannel.TEAM_CHAT, "Reached Sharjah industrial area", isImportant = false)
        waitUntil { repository.allChatMessagesFlow.first().size == 1 }

        // 2. Admin logs in: sends IMPORTANT message to TEAM_CHAT + private message to ADMIN_OWNER
        vm.logout()
        waitUntil { vm.authSession.value == null }
        vm.login(admin.id, "1725", UserRole.ADMIN)
        waitUntil { vm.authSession.value?.user?.role == UserRole.ADMIN }

        vm.sendChatMessage(ChatChannel.TEAM_CHAT, "All trucks return before 7 PM today!", isImportant = true)
        vm.sendChatMessage(ChatChannel.ADMIN_OWNER, "Monthly audit ready for Owner review", isImportant = true)
        waitUntil {
            repository.allChatMessagesFlow.first().size == 3 &&
                repository.allNotificationsFlow.first().count {
                    it.category == NotificationCategory.CHAT_IMPORTANT.name
                } == 2
        }

        val messages = repository.allChatMessagesFlow.first()
        assertEquals(2, messages.count { it.channel == ChatChannel.TEAM_CHAT.name })
        assertEquals(1, messages.count { it.channel == ChatChannel.ADMIN_OWNER.name })

        // Important messages created CHAT_IMPORTANT Bell notifications with proper target scopes
        val chatAlerts = repository.allNotificationsFlow.first().filter { it.category == NotificationCategory.CHAT_IMPORTANT.name }
        assertEquals(2, chatAlerts.size)
        assertTrue(chatAlerts.any { it.targetScope == NotificationTargetScope.ALL.name && it.isUrgent })
        assertTrue(chatAlerts.any { it.targetScope == NotificationTargetScope.ADMIN_OWNER.name && it.isUrgent })

        // Verify Bell & Chat sounds are safe in Robolectric
        SoundEffectsManager.playNotificationBell(true)
        SoundEffectsManager.playChatMessageTone(true)
    }

    @Test
    fun `launch screen is 3 to 5 seconds and category folders plus role headers use distinct visual color palettes`() {
        val duration = com.example.ui.components.STARTUP_LAUNCH_DURATION_SECONDS
        assertTrue(duration in 3f..5f)
        assertEquals(4.2f, duration)

        // All 4 gas category folders have unique vibrant primary colors
        val categoryColors = ProductCategory.entries.map {
            com.example.ui.components.categoryVisualStyle(it).primaryColor
        }.toSet()
        assertEquals(ProductCategory.entries.size, categoryColors.size)

        // Admin, Owner View, Staff, Inside Store, and Outside Fleet each have distinct header palettes
        val headerGradients = setOf(
            com.example.ui.components.RoleHeaderPalettes.AdminGradient,
            com.example.ui.components.RoleHeaderPalettes.OwnerGradient,
            com.example.ui.components.RoleHeaderPalettes.StaffGradient,
            com.example.ui.components.RoleHeaderPalettes.InsideStoreGradient,
            com.example.ui.components.RoleHeaderPalettes.OutsideFleetGradient
        )
        assertEquals(5, headerGradients.size)

        // Inside Store and Outside Fleet zone toggles have distinct accent colors
        assertFalse(
            com.example.ui.components.StockZoneScope.INSIDE_STORE.accentColor ==
                com.example.ui.components.StockZoneScope.OUTSIDE_FLEET.accentColor
        )
    }

    @Test
    fun `owner view accepts 4-digit PIN and rejects shorter PINs while preserving read-only access`() = runTest {
        repository.initializeDatabase()
        val prefs = SessionPreferences(context)
        prefs.clearLoggedInUserId()
        prefs.clearOwnerViewPasswordHash()
        prefs.clearBiometricUnlockState()

        assertTrue(com.example.auth.AdminCredentialCrypto.isValidOwnerViewPin("2468"))
        assertFalse(com.example.auth.AdminCredentialCrypto.isValidOwnerViewPin("123"))
        assertFalse(com.example.auth.AdminCredentialCrypto.isValidOwnerViewPin("12a4"))

        val vm = AppViewModel(
            repository = repository,
            backupManager = backupManager,
            cloudSync = null,
            sessionPreferences = prefs,
            driveBackupManager = null,
            gmailVerificationManager = null,
            appContext = context
        )
        fun waitUntil(timeoutMs: Long = 5000L, condition: () -> Boolean) {
            val deadline = System.currentTimeMillis() + timeoutMs
            while (!condition() && System.currentTimeMillis() < deadline) {
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                Thread.sleep(20)
            }
        }
        waitUntil { vm.isInitialized.value }

        // 3-digit PIN rejected
        vm.setupOwnerViewPassword("123", "123")
        assertNotNull(vm.loginError.value)
        assertNull(vm.authSession.value)

        // 4-digit PIN succeeds and signs into Owner View
        vm.clearLoginError()
        vm.setupOwnerViewPassword("2468", "2468")
        waitUntil { vm.authSession.value?.user?.role == UserRole.OWNER_VIEW }
        assertEquals(UserRole.OWNER_VIEW, vm.authSession.value?.user?.role)

        // Logout & sign back in with 4-digit PIN
        vm.logout()
        waitUntil { vm.authSession.value == null }
        vm.loginOwnerViewWithPassword("9999")
        waitUntil { vm.loginError.value != null }
        assertNull(vm.authSession.value)

        vm.clearLoginError()
        vm.loginOwnerViewWithPassword("2468")
        waitUntil { vm.authSession.value?.user?.role == UserRole.OWNER_VIEW }
        assertEquals(UserRole.OWNER_VIEW, vm.authSession.value?.user?.role)
    }

    @Test
    fun `staff new customer full set sale leaves zero empty pending and auto-minuses overall company cylinders`() = runTest {
        repository.initializeDatabase()
        val products = repository.activeProductsFlow.first()
        val oxygen = products.first { it.name.contains("Oxygen", ignoreCase = true) }
        val vehicles = repository.vehiclesFlow.first()
        val nadeemVehicle = vehicles.first()
        val users = repository.usersFlow.first()
        val admin = users.first { it.role == UserRole.ADMIN }
        val nadeem = users.first { it.role == UserRole.STAFF && it.id == nadeemVehicle.staffUserId }
        val today = "2026-11-26"

        // Store starts with 100 Full Oxygen purchased -> Overall Company Total = 100
        assertTrue(
            repository.recordTransaction(
                businessDate = today,
                type = TransactionType.STORE_PURCHASE_FULL,
                productId = oxygen.id,
                vehicleId = null,
                actor = admin,
                quantity = 100,
                notes = "Initial yard stock"
            ) is OperationResult.Success
        )

        // Dispatch 10 Full from Store to Vehicle -> Store Full 90, Vehicle Full 10, Overall = 100
        assertTrue(
            repository.recordTransaction(
                businessDate = today,
                type = TransactionType.VEHICLE_FULL_RECEIVED,
                productId = oxygen.id,
                vehicleId = nadeemVehicle.id,
                actor = admin,
                quantity = 10,
                notes = "Morning truck load"
            ) is OperationResult.Success
        )

        val beforeSale = repository.observeCompanyStockReport(today).first().first { it.product.id == oxygen.id }
        assertEquals(90, beforeSale.storeFull)
        assertEquals(10, beforeSale.vehiclesFull)
        assertEquals(100, beforeSale.totalFull)
        assertEquals(0, beforeSale.totalEmpty)
        assertEquals(100, beforeSale.overallTotalCylinders)

        // Staff records New Customer Full Set Sale (3 cylinders, empty NOT coming back)
        assertTrue(
            repository.recordTransaction(
                businessDate = today,
                type = TransactionType.VEHICLE_NEW_CUSTOMER_SALE,
                productId = oxygen.id,
                vehicleId = nadeemVehicle.id,
                actor = nadeem,
                quantity = 3,
                notes = "New customer full set",
                customerName = "Al Noor Restaurant"
            ) is OperationResult.Success
        )

        val afterSale = repository.observeCompanyStockReport(today).first().first { it.product.id == oxygen.id }
        // Store Full stays 90, Vehicle Full drops 10 -> 7, Overall Total auto-minuses 100 -> 97!
        assertEquals(90, afterSale.storeFull)
        assertEquals(7, afterSale.vehiclesFull)
        assertEquals(97, afterSale.totalFull)
        assertEquals(0, afterSale.totalEmpty)
        assertEquals(97, afterSale.overallTotalCylinders)
        assertEquals(3, afterSale.newCustomerSoldOut)

        // Customer has ZERO pending empty balance ("empty not coming that's balance no problem")
        val pendingList = repository.observeCustomerPendingBalances(nadeemVehicle.id).first()
        assertTrue(pendingList.none { it.customerName.equals("Al Noor Restaurant", ignoreCase = true) })
    }

    @Test
    fun `full return to store moves full cylinders from vehicle back to store with negative stock guard`() = runTest {
        repository.initializeDatabase()
        val admin = repository.usersFlow.first().first { it.role == UserRole.ADMIN }
        val nadeem = repository.usersFlow.first().first { it.displayName == "Nadeem" }
        val nadeemVehicle = repository.vehiclesFlow.first().first { it.name == "NADEEM" }
        val oxygen = repository.activeProductsFlow.first().first {
            it.category == ProductCategory.INDUSTRIAL_GAS && it.name == "Oxygen"
        }
        val today = "2026-11-26"

        // Store Opening = 30 Full, Dispatch 10 Full to Vehicle -> Store 20, Vehicle 10
        assertTrue(
            repository.setOpeningStockForDate(
                scope = StockCalculator.SCOPE_STORE,
                vehicleId = 0L,
                productId = oxygen.id,
                date = today,
                desiredOpeningFull = 30,
                desiredOpeningEmpty = 0,
                admin = admin
            ) is OperationResult.Success
        )
        assertTrue(
            repository.recordTransaction(
                businessDate = today,
                type = TransactionType.VEHICLE_FULL_RECEIVED,
                productId = oxygen.id,
                vehicleId = nadeemVehicle.id,
                actor = admin,
                quantity = 10,
                notes = "Morning load"
            ) is OperationResult.Success
        )

        // Return 4 Full from Vehicle to Store -> Store 24, Vehicle 6
        assertTrue(
            repository.recordTransaction(
                businessDate = today,
                type = TransactionType.VEHICLE_FULL_RETURN_TO_STORE,
                productId = oxygen.id,
                vehicleId = nadeemVehicle.id,
                actor = nadeem,
                quantity = 4,
                notes = "End of shift full return"
            ) is OperationResult.Success
        )

        val storeRow = repository.observeStoreStock(today).first().first { it.product.id == oxygen.id }
        val vehRow = repository.observeVehicleStock(nadeemVehicle.id, today).first().first { it.product.id == oxygen.id }
        assertEquals(24, storeRow.storeFullBalance)
        assertEquals(6, vehRow.fullBalance)
        assertEquals(4, vehRow.fullReturnedToStore)

        // Attempting to return 9 Full (> 6 available on vehicle) must fail with InsufficientStock
        val overReturn = repository.recordTransaction(
            businessDate = today,
            type = TransactionType.VEHICLE_FULL_RETURN_TO_STORE,
            productId = oxygen.id,
            vehicleId = nadeemVehicle.id,
            actor = nadeem,
            quantity = 9,
            notes = "Too many"
        )
        assertTrue(overReturn is OperationResult.InsufficientStock)
    }

    @Test
    fun `customer product change full-to-full, empty-to-empty, and return empty work for vehicle and store`() = runTest {
        repository.initializeDatabase()
        val admin = repository.usersFlow.first().first { it.role == UserRole.ADMIN }
        val nadeem = repository.usersFlow.first().first { it.displayName == "Nadeem" }
        val nadeemVehicle = repository.vehiclesFlow.first().first { it.name == "NADEEM" }
        val products = repository.activeProductsFlow.first()
        val oxygen = products.first { it.category == ProductCategory.INDUSTRIAL_GAS && it.name == "Oxygen" }
        val nitrogen = products.first { it.category == ProductCategory.INDUSTRIAL_GAS && it.name == "Nitrogen" }
        val today = "2026-11-26"

        // Seed Store Opening (Oxygen: 20F/10E, Nitrogen: 15F/8E) & Vehicle Opening (Oxygen: 6F/4E, Nitrogen: 5F/3E)
        assertTrue(repository.setOpeningStockForDate(StockCalculator.SCOPE_STORE, 0L, oxygen.id, today, 20, 10, admin) is OperationResult.Success)
        assertTrue(repository.setOpeningStockForDate(StockCalculator.SCOPE_STORE, 0L, nitrogen.id, today, 15, 8, admin) is OperationResult.Success)
        assertTrue(repository.setOpeningStockForDate(StockCalculator.SCOPE_VEHICLE, nadeemVehicle.id, oxygen.id, today, 6, 4, admin) is OperationResult.Success)
        assertTrue(repository.setOpeningStockForDate(StockCalculator.SCOPE_VEHICLE, nadeemVehicle.id, nitrogen.id, today, 5, 3, admin) is OperationResult.Success)

        // 1. Vehicle Full -> Full: Customer returns 2 Full Oxygen (+2) and takes 2 Full Nitrogen (-2)
        assertTrue(
            repository.recordCustomerProductChange(
                businessDate = today,
                mode = CustomerChangeMode.FULL_TO_FULL,
                returnedProductId = oxygen.id,
                givenProductId = nitrogen.id,
                vehicleId = nadeemVehicle.id,
                actor = nadeem,
                quantity = 2,
                customerName = "Gulf Welding",
                notes = "Full swap"
            ) is OperationResult.Success
        )

        // 2. Vehicle Empty -> Empty: Customer returns 1 Empty Oxygen (+1) and takes 1 Empty Nitrogen (-1)
        assertTrue(
            repository.recordCustomerProductChange(
                businessDate = today,
                mode = CustomerChangeMode.EMPTY_TO_EMPTY,
                returnedProductId = oxygen.id,
                givenProductId = nitrogen.id,
                vehicleId = nadeemVehicle.id,
                actor = nadeem,
                quantity = 1,
                customerName = "Gulf Welding",
                notes = "Empty swap"
            ) is OperationResult.Success
        )

        // 3. Vehicle Return Empty: Customer returns 2 Empty Nitrogen (+2)
        assertTrue(
            repository.recordCustomerProductChange(
                businessDate = today,
                mode = CustomerChangeMode.RETURN_EMPTY,
                returnedProductId = nitrogen.id,
                givenProductId = null,
                vehicleId = nadeemVehicle.id,
                actor = nadeem,
                quantity = 2,
                customerName = "Gulf Welding",
                notes = "Return empty"
            ) is OperationResult.Success
        )

        val vehRows = repository.observeVehicleStock(nadeemVehicle.id, today).first()
        val vehOxy = vehRows.first { it.product.id == oxygen.id }
        val vehNit = vehRows.first { it.product.id == nitrogen.id }
        assertEquals(8, vehOxy.fullBalance)   // 6 + 2
        assertEquals(5, vehOxy.emptyBalance)  // 4 + 1
        assertEquals(3, vehNit.fullBalance)   // 5 - 2
        assertEquals(4, vehNit.emptyBalance)  // 3 - 1 + 2

        // 4. Negative stock guard: trying to give 10 Full Nitrogen (> 3 available) fails cleanly
        val overSwap = repository.recordCustomerProductChange(
            businessDate = today,
            mode = CustomerChangeMode.FULL_TO_FULL,
            returnedProductId = oxygen.id,
            givenProductId = nitrogen.id,
            vehicleId = nadeemVehicle.id,
            actor = nadeem,
            quantity = 10,
            customerName = "Gulf Welding"
        )
        assertTrue(overSwap is OperationResult.InsufficientStock)

        // 5. Admin Main Store Full -> Full, Empty -> Empty, and Return Empty
        assertTrue(
            repository.recordCustomerProductChange(
                businessDate = today,
                mode = CustomerChangeMode.FULL_TO_FULL,
                returnedProductId = oxygen.id,
                givenProductId = nitrogen.id,
                vehicleId = null,
                actor = admin,
                quantity = 3,
                customerName = "Al Noor Metals"
            ) is OperationResult.Success
        )
        assertTrue(
            repository.recordCustomerProductChange(
                businessDate = today,
                mode = CustomerChangeMode.EMPTY_TO_EMPTY,
                returnedProductId = oxygen.id,
                givenProductId = nitrogen.id,
                vehicleId = null,
                actor = admin,
                quantity = 2,
                customerName = "Al Noor Metals"
            ) is OperationResult.Success
        )
        assertTrue(
            repository.recordCustomerProductChange(
                businessDate = today,
                mode = CustomerChangeMode.RETURN_EMPTY,
                returnedProductId = nitrogen.id,
                givenProductId = null,
                vehicleId = null,
                actor = admin,
                quantity = 4,
                customerName = "Al Noor Metals"
            ) is OperationResult.Success
        )

        val storeRows = repository.observeStoreStock(today).first()
        val storeOxy = storeRows.first { it.product.id == oxygen.id }
        val storeNit = storeRows.first { it.product.id == nitrogen.id }
        assertEquals(23, storeOxy.storeFullBalance)   // 20 + 3
        assertEquals(12, storeOxy.storeEmptyBalance)  // 10 + 2
        assertEquals(12, storeNit.storeFullBalance)   // 15 - 3
        assertEquals(10, storeNit.storeEmptyBalance)  // 8 - 2 + 4
    }

    @Test
    fun `batch multi-category transaction entry saves all 4 categories atomically and rolls back on insufficient stock`() = runTest {
        repository.initializeDatabase()
        val today = "2026-11-26"
        val users = repository.usersFlow.first()
        val admin = users.first { it.role == UserRole.ADMIN }
        val nadeem = users.first { it.displayName == "Nadeem" }
        val nadeemVehicle = repository.vehiclesFlow.first().first { it.name == "NADEEM" }
        val products = repository.activeProductsFlow.first()

        val indProd = products.first { it.category == ProductCategory.INDUSTRIAL_GAS }
        val smallProd = products.first { it.category == ProductCategory.SMALL_48_CFT }
        val cookProd = products.first { it.category == ProductCategory.COOKING_GAS }
        val mcgProd = products.first { it.category == ProductCategory.MCG_CUTTING_GAS }

        // 1. Admin batch purchase across all 4 categories in ONE entry
        val purchaseRes = repository.recordBatchTransactions(
            businessDate = today,
            type = TransactionType.STORE_PURCHASE_FULL,
            vehicleId = null,
            actor = admin,
            items = listOf(
                BatchProductEntry(indProd.id, 30),
                BatchProductEntry(smallProd.id, 15),
                BatchProductEntry(cookProd.id, 25),
                BatchProductEntry(mcgProd.id, 10)
            ),
            notes = "Batch purchase across all 4 categories",
            purchaseCompanyName = "Emirates Gas"
        )
        assertTrue(purchaseRes is OperationResult.Success)

        val storeAfterPurchase = repository.observeStoreStock(today).first()
        assertEquals(30, storeAfterPurchase.first { it.product.id == indProd.id }.storeFullBalance)
        assertEquals(15, storeAfterPurchase.first { it.product.id == smallProd.id }.storeFullBalance)
        assertEquals(25, storeAfterPurchase.first { it.product.id == cookProd.id }.storeFullBalance)
        assertEquals(10, storeAfterPurchase.first { it.product.id == mcgProd.id }.storeFullBalance)

        // 2. Dispatch different quantities of all 4 categories to Nadeem's vehicle
        val dispatchRes = repository.recordBatchTransactions(
            businessDate = today,
            type = TransactionType.VEHICLE_FULL_RECEIVED,
            vehicleId = nadeemVehicle.id,
            actor = admin,
            items = listOf(
                BatchProductEntry(indProd.id, 12),
                BatchProductEntry(smallProd.id, 7),
                BatchProductEntry(cookProd.id, 9),
                BatchProductEntry(mcgProd.id, 4)
            ),
            notes = "Morning load"
        )
        assertTrue(dispatchRes is OperationResult.Success)

        // 3. Staff records batch Customer Sale across all 4 categories with varying Empty Collected Now
        val saleRes = repository.recordBatchTransactions(
            businessDate = today,
            type = TransactionType.VEHICLE_CUSTOMER_SALE,
            vehicleId = nadeemVehicle.id,
            actor = nadeem,
            items = listOf(
                BatchProductEntry(indProd.id, quantity = 5, emptyCollectedNow = 3),
                BatchProductEntry(smallProd.id, quantity = 3, emptyCollectedNow = 3),
                BatchProductEntry(cookProd.id, quantity = 4, emptyCollectedNow = 1),
                BatchProductEntry(mcgProd.id, quantity = 2, emptyCollectedNow = 0)
            ),
            notes = "Combined multi-category delivery",
            customerName = "Al Noor Hotel"
        )
        assertTrue(saleRes is OperationResult.Success)

        val vehRows = repository.observeVehicleStock(nadeemVehicle.id, today).first()
        assertEquals(7, vehRows.first { it.product.id == indProd.id }.fullBalance)    // 12 - 5
        assertEquals(3, vehRows.first { it.product.id == indProd.id }.emptyBalance)
        assertEquals(4, vehRows.first { it.product.id == smallProd.id }.fullBalance)  // 7 - 3
        assertEquals(3, vehRows.first { it.product.id == smallProd.id }.emptyBalance)
        assertEquals(5, vehRows.first { it.product.id == cookProd.id }.fullBalance)   // 9 - 4
        assertEquals(1, vehRows.first { it.product.id == cookProd.id }.emptyBalance)
        assertEquals(2, vehRows.first { it.product.id == mcgProd.id }.fullBalance)    // 4 - 2
        assertEquals(0, vehRows.first { it.product.id == mcgProd.id }.emptyBalance)

        // 4. Atomic rollback: if one item in batch lacks stock, NO items in batch are committed
        val failedBatch = repository.recordBatchTransactions(
            businessDate = today,
            type = TransactionType.VEHICLE_CUSTOMER_SALE,
            vehicleId = nadeemVehicle.id,
            actor = nadeem,
            items = listOf(
                BatchProductEntry(indProd.id, quantity = 2, emptyCollectedNow = 2),
                BatchProductEntry(mcgProd.id, quantity = 99, emptyCollectedNow = 0) // exceeds 2
            ),
            customerName = "Al Noor Hotel"
        )
        assertTrue(failedBatch is OperationResult.InsufficientStock)

        val vehAfterFail = repository.observeVehicleStock(nadeemVehicle.id, today).first()
        assertEquals(7, vehAfterFail.first { it.product.id == indProd.id }.fullBalance)
        assertEquals(3, vehAfterFail.first { it.product.id == indProd.id }.emptyBalance)
    }

    @Test
    fun `multi-cylinder customer full and empty exchanges work for vehicle and store`() = runTest {
        repository.initializeDatabase()
        val today = "2026-11-26"
        val users = repository.usersFlow.first()
        val admin = users.first { it.role == UserRole.ADMIN }
        val nadeem = users.first { it.displayName == "Nadeem" }
        val nadeemVehicle = repository.vehiclesFlow.first().first { it.name == "NADEEM" }
        val products = repository.activeProductsFlow.first()

        val indProd = products.first { it.category == ProductCategory.INDUSTRIAL_GAS }
        val smallProd = products.first { it.category == ProductCategory.SMALL_48_CFT }
        val cookProd = products.first { it.category == ProductCategory.COOKING_GAS }
        val mcgProd = products.first { it.category == ProductCategory.MCG_CUTTING_GAS }

        assertTrue(repository.setOpeningStockForDate(StockCalculator.SCOPE_STORE, 0L, indProd.id, today, 20, 12, admin) is OperationResult.Success)
        assertTrue(repository.setOpeningStockForDate(StockCalculator.SCOPE_STORE, 0L, smallProd.id, today, 15, 10, admin) is OperationResult.Success)
        assertTrue(repository.setOpeningStockForDate(StockCalculator.SCOPE_STORE, 0L, cookProd.id, today, 18, 14, admin) is OperationResult.Success)
        assertTrue(repository.setOpeningStockForDate(StockCalculator.SCOPE_STORE, 0L, mcgProd.id, today, 10, 8, admin) is OperationResult.Success)

        assertTrue(repository.setOpeningStockForDate(StockCalculator.SCOPE_VEHICLE, nadeemVehicle.id, indProd.id, today, 8, 5, admin) is OperationResult.Success)
        assertTrue(repository.setOpeningStockForDate(StockCalculator.SCOPE_VEHICLE, nadeemVehicle.id, smallProd.id, today, 6, 4, admin) is OperationResult.Success)
        assertTrue(repository.setOpeningStockForDate(StockCalculator.SCOPE_VEHICLE, nadeemVehicle.id, cookProd.id, today, 7, 5, admin) is OperationResult.Success)
        assertTrue(repository.setOpeningStockForDate(StockCalculator.SCOPE_VEHICLE, nadeemVehicle.id, mcgProd.id, today, 5, 3, admin) is OperationResult.Success)

        // 1. Staff Vehicle: Full multiple cylinder exchange (return 2 Industrial + 1 Small, give 2 Cooking + 1 MCG)
        assertTrue(
            repository.recordMultiCustomerProductChange(
                businessDate = today,
                mode = CustomerChangeMode.FULL_TO_FULL,
                returnedItems = listOf(CustomerExchangeItem(indProd.id, 2), CustomerExchangeItem(smallProd.id, 1)),
                givenItems = listOf(CustomerExchangeItem(cookProd.id, 2), CustomerExchangeItem(mcgProd.id, 1)),
                vehicleId = nadeemVehicle.id,
                actor = nadeem,
                customerName = "Multi Exchange Customer",
                notes = "Full swap across categories"
            ) is OperationResult.Success
        )

        // 2. Staff Vehicle: Empty multiple cylinder exchange (return 3 Cooking + 2 MCG, give 3 Industrial + 2 Small)
        assertTrue(
            repository.recordMultiCustomerProductChange(
                businessDate = today,
                mode = CustomerChangeMode.EMPTY_TO_EMPTY,
                returnedItems = listOf(CustomerExchangeItem(cookProd.id, 3), CustomerExchangeItem(mcgProd.id, 2)),
                givenItems = listOf(CustomerExchangeItem(indProd.id, 3), CustomerExchangeItem(smallProd.id, 2)),
                vehicleId = nadeemVehicle.id,
                actor = nadeem,
                customerName = "Multi Exchange Customer",
                notes = "Empty swap across categories"
            ) is OperationResult.Success
        )

        val vehRows = repository.observeVehicleStock(nadeemVehicle.id, today).first()
        assertEquals(10, vehRows.first { it.product.id == indProd.id }.fullBalance)   // 8 + 2
        assertEquals(7, vehRows.first { it.product.id == smallProd.id }.fullBalance)  // 6 + 1
        assertEquals(5, vehRows.first { it.product.id == cookProd.id }.fullBalance)   // 7 - 2
        assertEquals(4, vehRows.first { it.product.id == mcgProd.id }.fullBalance)    // 5 - 1

        assertEquals(2, vehRows.first { it.product.id == indProd.id }.emptyBalance)   // 5 - 3
        assertEquals(2, vehRows.first { it.product.id == smallProd.id }.emptyBalance) // 4 - 2
        assertEquals(8, vehRows.first { it.product.id == cookProd.id }.emptyBalance)  // 5 + 3
        assertEquals(5, vehRows.first { it.product.id == mcgProd.id }.emptyBalance)   // 3 + 2

        // 3. Atomic rollback on Store if one given item exceeds stock
        val failStoreSwap = repository.recordMultiCustomerProductChange(
            businessDate = today,
            mode = CustomerChangeMode.FULL_TO_FULL,
            returnedItems = listOf(CustomerExchangeItem(indProd.id, 4)),
            givenItems = listOf(CustomerExchangeItem(smallProd.id, 2), CustomerExchangeItem(mcgProd.id, 50)),
            vehicleId = null,
            actor = admin,
            customerName = "Bad Swap"
        )
        assertTrue(failStoreSwap is OperationResult.InsufficientStock)
        val storeAfterFail = repository.observeStoreStock(today).first()
        assertEquals(20, storeAfterFail.first { it.product.id == indProd.id }.storeFullBalance)

        // 4. Admin Store: Full multiple + Empty multiple exchanges succeed cleanly
        assertTrue(
            repository.recordMultiCustomerProductChange(
                businessDate = today,
                mode = CustomerChangeMode.FULL_TO_FULL,
                returnedItems = listOf(CustomerExchangeItem(indProd.id, 3), CustomerExchangeItem(cookProd.id, 2)),
                givenItems = listOf(CustomerExchangeItem(smallProd.id, 4), CustomerExchangeItem(mcgProd.id, 1)),
                vehicleId = null,
                actor = admin,
                customerName = "Yard Customer"
            ) is OperationResult.Success
        )
        assertTrue(
            repository.recordMultiCustomerProductChange(
                businessDate = today,
                mode = CustomerChangeMode.EMPTY_TO_EMPTY,
                returnedItems = listOf(CustomerExchangeItem(smallProd.id, 2), CustomerExchangeItem(mcgProd.id, 1)),
                givenItems = listOf(CustomerExchangeItem(indProd.id, 2), CustomerExchangeItem(cookProd.id, 1)),
                vehicleId = null,
                actor = admin,
                customerName = "Yard Customer"
            ) is OperationResult.Success
        )

        val storeRows = repository.observeStoreStock(today).first()
        assertEquals(23, storeRows.first { it.product.id == indProd.id }.storeFullBalance)   // 20 + 3
        assertEquals(11, storeRows.first { it.product.id == smallProd.id }.storeFullBalance) // 15 - 4
        assertEquals(20, storeRows.first { it.product.id == cookProd.id }.storeFullBalance)  // 18 + 2
        assertEquals(9, storeRows.first { it.product.id == mcgProd.id }.storeFullBalance)    // 10 - 1

        assertEquals(10, storeRows.first { it.product.id == indProd.id }.storeEmptyBalance)  // 12 - 2
        assertEquals(12, storeRows.first { it.product.id == smallProd.id }.storeEmptyBalance)// 10 + 2
        assertEquals(13, storeRows.first { it.product.id == cookProd.id }.storeEmptyBalance) // 14 - 1
        assertEquals(9, storeRows.first { it.product.id == mcgProd.id }.storeEmptyBalance)   // 8 + 1
    }
}


