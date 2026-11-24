package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.ConcordDatabase
import com.example.data.local.OpeningStockEntity
import com.example.data.local.ProductEntity
import com.example.data.local.StockTransactionEntity
import com.example.data.local.UserAccountEntity
import com.example.data.local.VehicleEntity
import com.example.data.model.AuthSession
import com.example.data.model.CompanyStockRow
import com.example.data.model.DashboardSummary
import com.example.data.model.MonthlyDailySaleRow
import com.example.data.model.OperationResult
import com.example.data.model.ProductCategory
import com.example.data.model.ProductSalesRow
import com.example.data.model.StaffSalesRow
import com.example.data.model.StockCalculator
import com.example.data.model.StoreStockRow
import com.example.data.model.TransactionDisplayItem
import com.example.data.model.TransactionType
import com.example.data.model.UserRole
import com.example.data.model.VehicleOverview
import com.example.data.model.VehicleStockRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InventoryRepository(private val db: ConcordDatabase) {

    private val userDao = db.userDao()
    private val vehicleDao = db.vehicleDao()
    private val productDao = db.productDao()
    private val openingDao = db.openingStockDao()
    private val txDao = db.transactionDao()

    val usersFlow: Flow<List<UserAccountEntity>> = userDao.observeAllUsers()
    val vehiclesFlow: Flow<List<VehicleEntity>> = vehicleDao.observeActiveVehicles()
    val activeProductsFlow: Flow<List<ProductEntity>> = productDao.observeActiveProducts()
    val allProductsFlow: Flow<List<ProductEntity>> = productDao.observeAllProducts()
    val openingsFlow: Flow<List<OpeningStockEntity>> = openingDao.observeAllOpenings()
    val transactionsFlow: Flow<List<StockTransactionEntity>> = txDao.observeAllTransactions()

    suspend fun initializeDatabase() {
        db.ensureSeeded()
    }

    suspend fun authenticate(userId: Long, pin: String): AuthSession? {
        val user = userDao.getUserById(userId) ?: return null
        if (!user.isActive || user.pin != pin) return null
        val vehicle = if (user.role == UserRole.STAFF) {
            vehicleDao.getVehicleByStaffUserId(user.id)
        } else {
            null
        }
        return AuthSession(user = user, assignedVehicle = vehicle)
    }

    suspend fun updateUserPin(targetUserId: Long, newPin: String): OperationResult {
        if (newPin.length != 4 || !newPin.all { it.isDigit() }) {
            return OperationResult.ValidationError("PIN must be exactly 4 digits.")
        }
        userDao.updatePin(targetUserId, newPin)
        return OperationResult.Success("PIN updated successfully.")
    }

    suspend fun addProduct(category: ProductCategory, name: String): OperationResult {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) {
            return OperationResult.ValidationError("Product name cannot be empty.")
        }
        val existing = productDao.getAllProductsOnce()
        if (existing.any { it.category == category && it.name.equals(cleanName, ignoreCase = true) }) {
            return OperationResult.ValidationError("${category.displayName} - $cleanName already exists.")
        }
        val nextSort = (existing.filter { it.category == category }.maxOfOrNull { it.sortOrder }
            ?: (category.sortOrder * 100)) + 1
        productDao.insert(
            ProductEntity(
                category = category,
                name = cleanName,
                sortOrder = nextSort,
                isActive = true
            )
        )
        return OperationResult.Success("Product added to ${category.displayName}.")
    }

    suspend fun toggleProductActive(product: ProductEntity): OperationResult {
        productDao.update(product.copy(isActive = !product.isActive))
        val status = if (!product.isActive) "activated" else "archived"
        return OperationResult.Success("${product.name} $status.")
    }

    /**
     * Reactive Store Stock rows for a given date.
     */
    fun observeStoreStock(date: String): Flow<List<StoreStockRow>> =
        combine(activeProductsFlow, openingsFlow, transactionsFlow) { products, openings, txs ->
            StockCalculator.calculateStoreRowsForDate(date, products, openings, txs)
        }

    /**
     * Reactive Vehicle Stock rows for a specific vehicle and date.
     */
    fun observeVehicleStock(vehicleId: Long, date: String): Flow<List<VehicleStockRow>> =
        combine(vehiclesFlow, activeProductsFlow, openingsFlow, transactionsFlow) { vehicles, products, openings, txs ->
            val vehicle = vehicles.firstOrNull { it.id == vehicleId } ?: return@combine emptyList()
            StockCalculator.calculateVehicleRowsForDate(date, vehicle, products, openings, txs)
        }

    /**
     * Reactive Admin Dashboard Summary for a selected date.
     */
    fun observeDashboardSummary(date: String): Flow<DashboardSummary> =
        combine(
            vehiclesFlow,
            activeProductsFlow,
            openingsFlow,
            transactionsFlow
        ) { vehicles, products, openings, txs ->
            val storeRows = StockCalculator.calculateStoreRowsForDate(date, products, openings, txs)
            val productMap = products.associateBy { it.id }

            val storeFull = storeRows.sumOf { it.storeFullBalance }
            val storeEmpty = storeRows.sumOf { it.storeEmptyBalance }
            val todayFullPurchases = storeRows.sumOf { it.fullPurchases }
            val todayEmptyPurchases = storeRows.sumOf { it.emptyPurchases }
            val todayFullDispatch = storeRows.sumOf { it.fullDispatch }
            val todayEmptyDispatch = storeRows.sumOf { it.emptyDispatch }

            val vehicleOverviews = vehicles.map { vehicle ->
                val vRows = StockCalculator.calculateVehicleRowsForDate(
                    date = date,
                    vehicle = vehicle,
                    products = products,
                    openings = openings,
                    transactions = txs
                )
                VehicleOverview(
                    vehicle = vehicle,
                    totalOldFull = vRows.sumOf { it.oldFull },
                    totalFullReceived = vRows.sumOf { it.fullReceived },
                    totalFull = vRows.sumOf { it.totalFull },
                    totalCustomerSales = vRows.sumOf { it.customerSales },
                    totalFullBalance = vRows.sumOf { it.fullBalance },
                    totalOldEmpty = vRows.sumOf { it.oldEmpty },
                    totalCustomerEmptyReceived = vRows.sumOf { it.customerEmptyReceived },
                    totalEmptyReturnedToStore = vRows.sumOf { it.emptyReturnedToStore },
                    totalEmptyBalance = vRows.sumOf { it.emptyBalance }
                )
            }

            val allVehiclesFull = vehicleOverviews.sumOf { it.totalFullBalance }
            val allVehiclesEmpty = vehicleOverviews.sumOf { it.totalEmptyBalance }
            val todaySales = vehicleOverviews.sumOf { it.totalCustomerSales }

            val recentTxs = txs
                .filter { it.businessDate == date }
                .take(30)
                .mapNotNull { tx ->
                    val product = productMap[tx.productId] ?: return@mapNotNull null
                    TransactionDisplayItem(
                        transaction = tx,
                        productName = product.name,
                        productCategory = product.category
                    )
                }

            DashboardSummary(
                selectedDate = date,
                totalFullCylinders = storeFull + allVehiclesFull,
                totalEmptyCylinders = storeEmpty + allVehiclesEmpty,
                storeFullBalance = storeFull,
                storeEmptyBalance = storeEmpty,
                allVehiclesFullBalance = allVehiclesFull,
                allVehiclesEmptyBalance = allVehiclesEmpty,
                todayFullPurchases = todayFullPurchases,
                todayEmptyPurchases = todayEmptyPurchases,
                todayFullDispatch = todayFullDispatch,
                todayEmptyDispatch = todayEmptyDispatch,
                todaySales = todaySales,
                vehicleOverviews = vehicleOverviews,
                recentTransactions = recentTxs
            )
        }

    /**
     * Records a stock transaction atomically while strictly preventing negative stock.
     */
    suspend fun recordTransaction(
        businessDate: String,
        type: TransactionType,
        productId: Long,
        vehicleId: Long?,
        actor: UserAccountEntity,
        quantity: Int,
        notes: String
    ): OperationResult {
        if (quantity <= 0) {
            return OperationResult.ValidationError("Quantity must be greater than 0.")
        }

        return db.withTransaction {
            val product = productDao.getProductById(productId)
                ?: return@withTransaction OperationResult.ValidationError("Selected product not found.")

            val vehicle = if (vehicleId != null) {
                vehicleDao.getVehicleById(vehicleId)
                    ?: return@withTransaction OperationResult.ValidationError("Selected vehicle not found.")
            } else {
                null
            }

            if (type.isVehicleLevel && vehicle == null) {
                return@withTransaction OperationResult.ValidationError("Please select a vehicle for this transaction.")
            }

            val openings = openingDao.getAllOpeningsOnce()
            val allTxs = txDao.getAllTransactionsOnce()

            val (liveStoreFull, liveStoreEmpty) = StockCalculator.currentStoreBalance(
                productId = productId,
                openings = openings,
                transactions = allTxs
            )
            val (liveVehicleFull, liveVehicleEmpty) = if (vehicle != null) {
                StockCalculator.currentVehicleBalance(
                    vehicleId = vehicle.id,
                    productId = productId,
                    openings = openings,
                    transactions = allTxs
                )
            } else {
                0 to 0
            }

            val productLabel = "${product.category.displayName} • ${product.name}"

            when (type) {
                TransactionType.VEHICLE_FULL_RECEIVED -> {
                    if (liveStoreFull < quantity) {
                        return@withTransaction OperationResult.InsufficientStock(
                            title = "Insufficient Store Full Stock",
                            message = "Cannot dispatch $quantity × $productLabel to ${vehicle!!.name}.\n\nAvailable Store Full Balance: $liveStoreFull"
                        )
                    }
                }
                TransactionType.STORE_FULL_DISPATCH_DIRECT,
                TransactionType.STORE_ADJUST_FULL_OUT -> {
                    if (liveStoreFull < quantity) {
                        return@withTransaction OperationResult.InsufficientStock(
                            title = "Insufficient Store Full Stock",
                            message = "Cannot dispatch/deduct $quantity × $productLabel.\n\nAvailable Store Full Balance: $liveStoreFull"
                        )
                    }
                }
                TransactionType.STORE_EMPTY_DISPATCH,
                TransactionType.STORE_ADJUST_EMPTY_OUT -> {
                    if (liveStoreEmpty < quantity) {
                        return@withTransaction OperationResult.InsufficientStock(
                            title = "Insufficient Store Empty Stock",
                            message = "Cannot dispatch/deduct $quantity × $productLabel empty cylinders.\n\nAvailable Store Empty Balance: $liveStoreEmpty"
                        )
                    }
                }
                TransactionType.VEHICLE_CUSTOMER_SALE,
                TransactionType.VEHICLE_ADJUST_FULL_OUT -> {
                    if (liveVehicleFull < quantity) {
                        return@withTransaction OperationResult.InsufficientStock(
                            title = "Insufficient Vehicle Full Stock",
                            message = "${vehicle!!.name} does not have enough full cylinders for $productLabel.\n\nAvailable Vehicle Full Balance: $liveVehicleFull, Requested: $quantity"
                        )
                    }
                }
                TransactionType.VEHICLE_EMPTY_RETURN_TO_STORE,
                TransactionType.VEHICLE_ADJUST_EMPTY_OUT -> {
                    if (liveVehicleEmpty < quantity) {
                        return@withTransaction OperationResult.InsufficientStock(
                            title = "Insufficient Vehicle Empty Stock",
                            message = "${vehicle!!.name} cannot return $quantity empty $productLabel cylinders.\n\nAvailable Vehicle Empty Balance: $liveVehicleEmpty"
                        )
                    }
                }
                else -> Unit
            }

            txDao.insert(
                StockTransactionEntity(
                    businessDate = businessDate,
                    timestamp = System.currentTimeMillis(),
                    type = type,
                    productId = productId,
                    vehicleId = vehicle?.id,
                    vehicleName = vehicle?.name,
                    staffId = actor.id,
                    staffName = actor.displayName,
                    quantity = quantity,
                    notes = notes.trim()
                )
            )
            OperationResult.Success("Recorded $quantity × ${product.name} (${type.label}).")
        }
    }

    /**
     * Admin-only: sets desired Opening Full & Opening Empty for Store or a Vehicle on [date],
     * automatically calculating base opening offset and preventing negative balances.
     */
    suspend fun setOpeningStockForDate(
        scope: String,
        vehicleId: Long,
        productId: Long,
        date: String,
        desiredOpeningFull: Int,
        desiredOpeningEmpty: Int,
        admin: UserAccountEntity
    ): OperationResult {
        if (admin.role != UserRole.ADMIN) {
            return OperationResult.ValidationError("Only Admin can modify opening stock.")
        }
        if (desiredOpeningFull < 0 || desiredOpeningEmpty < 0) {
            return OperationResult.ValidationError("Opening stock cannot be negative.")
        }

        return db.withTransaction {
            val openings = openingDao.getAllOpeningsOnce()
            val allTxs = txDao.getAllTransactionsOnce()
            val existingBase = openingDao.getOpening(scope, vehicleId, productId)

            var priorFull = 0
            var priorEmpty = 0
            var futureOrTodayFull = 0
            var futureOrTodayEmpty = 0

            for (tx in allTxs) {
                if (tx.productId != productId) continue
                if (scope == StockCalculator.SCOPE_VEHICLE && tx.vehicleId != vehicleId) continue

                val df = if (scope == StockCalculator.SCOPE_STORE) StockCalculator.storeFullDelta(tx)
                else StockCalculator.vehicleFullDelta(tx)
                val de = if (scope == StockCalculator.SCOPE_STORE) StockCalculator.storeEmptyDelta(tx)
                else StockCalculator.vehicleEmptyDelta(tx)

                if (tx.businessDate < date) {
                    priorFull += df
                    priorEmpty += de
                } else {
                    futureOrTodayFull += df
                    futureOrTodayEmpty += de
                }
            }

            val projectedEndFull = desiredOpeningFull + futureOrTodayFull
            val projectedEndEmpty = desiredOpeningEmpty + futureOrTodayEmpty
            if (projectedEndFull < 0 || projectedEndEmpty < 0) {
                return@withTransaction OperationResult.InsufficientStock(
                    title = "Negative Balance Prevented",
                    message = "Setting opening stock to Full=$desiredOpeningFull, Empty=$desiredOpeningEmpty would make current balance negative (Projected Full=$projectedEndFull, Empty=$projectedEndEmpty)."
                )
            }

            val newBaseFull = desiredOpeningFull - priorFull
            val newBaseEmpty = desiredOpeningEmpty - priorEmpty

            openingDao.upsert(
                OpeningStockEntity(
                    id = existingBase?.id ?: 0L,
                    scope = scope,
                    vehicleId = vehicleId,
                    productId = productId,
                    openingFull = newBaseFull,
                    openingEmpty = newBaseEmpty,
                    updatedAt = System.currentTimeMillis(),
                    updatedBy = admin.displayName
                )
            )
            OperationResult.Success("Opening stock updated.")
        }
    }

    /**
     * Admin-only: deletes a transaction if doing so does not cause negative balances.
     */
    suspend fun deleteTransaction(transactionId: Long, admin: UserAccountEntity): OperationResult {
        if (admin.role != UserRole.ADMIN) {
            return OperationResult.ValidationError("Only Admin can delete transactions.")
        }
        return db.withTransaction {
            val target = txDao.getTransactionById(transactionId)
                ?: return@withTransaction OperationResult.ValidationError("Transaction not found.")

            val openings = openingDao.getAllOpeningsOnce()
            val remainingTxs = txDao.getAllTransactionsOnce().filter { it.id != transactionId }

            val (storeFull, storeEmpty) = StockCalculator.currentStoreBalance(
                productId = target.productId,
                openings = openings,
                transactions = remainingTxs
            )
            if (storeFull < 0 || storeEmpty < 0) {
                return@withTransaction OperationResult.InsufficientStock(
                    title = "Cannot Delete Transaction",
                    message = "Deleting this entry would result in negative Store stock (Full=$storeFull, Empty=$storeEmpty) due to subsequent dispatches."
                )
            }

            if (target.vehicleId != null) {
                val (vFull, vEmpty) = StockCalculator.currentVehicleBalance(
                    vehicleId = target.vehicleId,
                    productId = target.productId,
                    openings = openings,
                    transactions = remainingTxs
                )
                if (vFull < 0 || vEmpty < 0) {
                    return@withTransaction OperationResult.InsufficientStock(
                        title = "Cannot Delete Transaction",
                        message = "Deleting this entry would result in negative stock on ${target.vehicleName} (Full=$vFull, Empty=$vEmpty)."
                    )
                }
            }

            txDao.deleteById(transactionId)
            OperationResult.Success("Transaction deleted.")
        }
    }

    // --- Report Queries ---

    fun observeMonthlySalesReport(yearMonth: String): Flow<List<MonthlyDailySaleRow>> =
        transactionsFlow.combine(activeProductsFlow) { txs, _ ->
            val monthTxs = txs.filter { it.businessDate.startsWith(yearMonth) }
            monthTxs.groupBy { it.businessDate }
                .entries
                .sortedBy { it.key }
                .map { (date, dayList) ->
                    MonthlyDailySaleRow(
                        date = date,
                        totalSold = dayList.filter { it.type == TransactionType.VEHICLE_CUSTOMER_SALE }
                            .sumOf { it.quantity },
                        totalPurchases = dayList.filter {
                            it.type == TransactionType.STORE_PURCHASE_FULL ||
                                it.type == TransactionType.STORE_PURCHASE_EMPTY
                        }.sumOf { it.quantity },
                        totalDispatch = dayList.filter {
                            it.type == TransactionType.VEHICLE_FULL_RECEIVED ||
                                it.type == TransactionType.STORE_FULL_DISPATCH_DIRECT
                        }.sumOf { it.quantity },
                        totalEmptyReturned = dayList.filter {
                            it.type == TransactionType.VEHICLE_EMPTY_RETURN_TO_STORE
                        }.sumOf { it.quantity }
                    )
                }
        }

    fun observeProductWiseSales(yearMonth: String): Flow<List<ProductSalesRow>> =
        combine(activeProductsFlow, transactionsFlow) { products, txs ->
            val monthTxs = txs.filter { it.businessDate.startsWith(yearMonth) }
            products.map { product ->
                val pTxs = monthTxs.filter { it.productId == product.id }
                ProductSalesRow(
                    product = product,
                    totalSold = pTxs.filter { it.type == TransactionType.VEHICLE_CUSTOMER_SALE }
                        .sumOf { it.quantity },
                    emptyCollected = pTxs.filter { it.type == TransactionType.VEHICLE_EMPTY_FROM_CUSTOMER }
                        .sumOf { it.quantity }
                )
            }
        }

    fun observeStaffWiseSales(yearMonth: String, asOfDate: String): Flow<List<StaffSalesRow>> =
        combine(vehiclesFlow, activeProductsFlow, openingsFlow, transactionsFlow) { vehicles, products, openings, txs ->
            val monthTxs = txs.filter { it.businessDate.startsWith(yearMonth) }
            vehicles.map { vehicle ->
                val vMonthTxs = monthTxs.filter { it.vehicleId == vehicle.id }
                val vTodayRows = StockCalculator.calculateVehicleRowsForDate(
                    date = asOfDate,
                    vehicle = vehicle,
                    products = products,
                    openings = openings,
                    transactions = txs
                )
                StaffSalesRow(
                    vehicle = vehicle,
                    totalFullReceived = vMonthTxs.filter { it.type == TransactionType.VEHICLE_FULL_RECEIVED }
                        .sumOf { it.quantity },
                    totalSold = vMonthTxs.filter { it.type == TransactionType.VEHICLE_CUSTOMER_SALE }
                        .sumOf { it.quantity },
                    totalEmptyCollected = vMonthTxs.filter { it.type == TransactionType.VEHICLE_EMPTY_FROM_CUSTOMER }
                        .sumOf { it.quantity },
                    totalEmptyReturned = vMonthTxs.filter { it.type == TransactionType.VEHICLE_EMPTY_RETURN_TO_STORE }
                        .sumOf { it.quantity },
                    closingFullBalance = vTodayRows.sumOf { it.fullBalance },
                    closingEmptyBalance = vTodayRows.sumOf { it.emptyBalance }
                )
            }
        }

    fun observeCompanyStockReport(asOfDate: String): Flow<List<CompanyStockRow>> =
        combine(vehiclesFlow, activeProductsFlow, openingsFlow, transactionsFlow) { vehicles, products, openings, txs ->
            val storeRows = StockCalculator.calculateStoreRowsForDate(asOfDate, products, openings, txs)
                .associateBy { it.product.id }
            val vehicleRowsByProduct = mutableMapOf<Long, MutableList<VehicleStockRow>>()
            for (vehicle in vehicles) {
                val rows = StockCalculator.calculateVehicleRowsForDate(asOfDate, vehicle, products, openings, txs)
                for (row in rows) {
                    vehicleRowsByProduct.getOrPut(row.product.id) { mutableListOf() }.add(row)
                }
            }
            products.map { product ->
                val store = storeRows[product.id]
                val vRows = vehicleRowsByProduct[product.id].orEmpty()
                val sFull = store?.storeFullBalance ?: 0
                val vFull = vRows.sumOf { it.fullBalance }
                val sEmpty = store?.storeEmptyBalance ?: 0
                val vEmpty = vRows.sumOf { it.emptyBalance }
                CompanyStockRow(
                    product = product,
                    storeFull = sFull,
                    vehiclesFull = vFull,
                    totalFull = sFull + vFull,
                    storeEmpty = sEmpty,
                    vehiclesEmpty = vEmpty,
                    totalEmpty = sEmpty + vEmpty
                )
            }
        }

    companion object {
        fun todayIsoDate(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return sdf.format(Date())
        }

        fun currentYearMonth(): String {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
            return sdf.format(Date())
        }
    }
}
