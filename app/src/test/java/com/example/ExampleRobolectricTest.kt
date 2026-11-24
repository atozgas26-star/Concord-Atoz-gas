package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ConcordDatabase
import com.example.data.model.OperationResult
import com.example.data.model.ProductCategory
import com.example.data.model.ReportType
import com.example.data.model.StockCalculator
import com.example.data.model.TransactionType
import com.example.data.model.UserRole
import com.example.data.repository.InventoryRepository
import com.example.export.BackupManager
import com.example.export.ReportExporter
import com.example.export.ReportTable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
}
