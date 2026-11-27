package com.example.data.model

import com.example.data.local.OpeningStockEntity
import com.example.data.local.ProductEntity
import com.example.data.local.StockTransactionEntity
import com.example.data.local.VehicleEntity

/**
 * Pure deterministic stock calculation engine implementing exact Concord & A to Z Gas formulas:
 *
 * Vehicle:
 * - Total Full = Old Full + Full Received
 * - Full Balance = Total Full - Customer Sales
 * - Empty Balance = Old Empty + Customer Empty Received - Empty Returned to Store
 *
 * Store:
 * - Store Full Balance = Opening Full + Full Purchases + Full Returns - Full Dispatch
 * - Store Empty Balance = Opening Empty + Empty Purchases + Empty Returns to Store - Empty Dispatch
 */
object StockCalculator {

    const val SCOPE_STORE = "STORE"
    const val SCOPE_VEHICLE = "VEHICLE"

    fun calculateStoreRowsForDate(
        date: String,
        products: List<ProductEntity>,
        openings: List<OpeningStockEntity>,
        transactions: List<StockTransactionEntity>
    ): List<StoreStockRow> {
        val openingByProduct = openings
            .filter { it.scope == SCOPE_STORE }
            .associateBy { it.productId }

        val txByProduct = transactions.groupBy { it.productId }

        return products.map { product ->
            val baseOpening = openingByProduct[product.id]
            val productTx = txByProduct[product.id].orEmpty()
            val priorTx = productTx.filter { it.businessDate < date }
            val dayTx = productTx.filter { it.businessDate == date }

            var openingFull = baseOpening?.openingFull ?: 0
            var openingEmpty = baseOpening?.openingEmpty ?: 0
            var openingDamaged = 0

            for (tx in priorTx) {
                openingFull += storeFullDelta(tx)
                openingEmpty += storeEmptyDelta(tx)
                openingDamaged += storeDamagedDelta(tx)
            }

            var fullPurchases = 0
            var fullReturns = 0
            var fullDispatch = 0
            var emptyPurchases = 0
            var emptyReturnsToStore = 0
            var emptyDispatch = 0
            var damagedAddedToday = 0
            var damagedResolvedToday = 0

            for (tx in dayTx) {
                when (tx.type) {
                    TransactionType.STORE_PURCHASE_FULL -> fullPurchases += tx.quantity
                    TransactionType.STORE_PURCHASE_COMPLAINT_REPLACE -> {
                        // Exchange defective empties from yard for fresh full cylinders from supplier
                        fullReturns += tx.quantity
                        emptyDispatch += tx.quantity
                    }
                    TransactionType.VEHICLE_FULL_RETURN_TO_STORE,
                    TransactionType.STORE_CUSTOMER_CHANGE_FULL_IN,
                    TransactionType.STORE_FULL_INWARD,
                    TransactionType.STORE_ADJUST_FULL_IN -> fullReturns += tx.quantity
                    TransactionType.VEHICLE_FULL_RECEIVED,
                    TransactionType.STORE_FULL_DISPATCH_DIRECT,
                    TransactionType.STORE_CUSTOMER_CHANGE_FULL_OUT,
                    TransactionType.STORE_ADJUST_FULL_OUT -> fullDispatch += tx.quantity

                    TransactionType.STORE_PURCHASE_EMPTY -> emptyPurchases += tx.quantity
                    TransactionType.VEHICLE_EMPTY_RETURN_TO_STORE,
                    TransactionType.STORE_EMPTY_INWARD,
                    TransactionType.STORE_EMPTY_FROM_CUSTOMER,
                    TransactionType.STORE_CUSTOMER_CHANGE_EMPTY_IN,
                    TransactionType.STORE_ADJUST_EMPTY_IN -> emptyReturnsToStore += tx.quantity
                    TransactionType.STORE_EMPTY_DISPATCH,
                    TransactionType.STORE_CUSTOMER_CHANGE_EMPTY_OUT,
                    TransactionType.STORE_ADJUST_EMPTY_OUT -> emptyDispatch += tx.quantity

                    TransactionType.STORE_DAMAGED_ADD -> damagedAddedToday += tx.quantity
                    TransactionType.STORE_DAMAGED_REMOVE -> damagedResolvedToday += tx.quantity

                    else -> Unit
                }
            }

            val storeFullBalance = openingFull + fullPurchases + fullReturns - fullDispatch
            val storeEmptyBalance = openingEmpty + emptyPurchases + emptyReturnsToStore - emptyDispatch
            val storeDamagedBalance = (openingDamaged + damagedAddedToday - damagedResolvedToday).coerceAtLeast(0)

            StoreStockRow(
                product = product,
                openingFull = openingFull,
                fullPurchases = fullPurchases,
                fullReturns = fullReturns,
                fullDispatch = fullDispatch,
                storeFullBalance = storeFullBalance,
                openingEmpty = openingEmpty,
                emptyPurchases = emptyPurchases,
                emptyReturnsToStore = emptyReturnsToStore,
                emptyDispatch = emptyDispatch,
                storeEmptyBalance = storeEmptyBalance,
                damagedBalance = storeDamagedBalance,
                damagedAddedToday = damagedAddedToday,
                damagedResolvedToday = damagedResolvedToday
            )
        }
    }

    fun calculateVehicleRowsForDate(
        date: String,
        vehicle: VehicleEntity,
        products: List<ProductEntity>,
        openings: List<OpeningStockEntity>,
        transactions: List<StockTransactionEntity>
    ): List<VehicleStockRow> {
        val openingByProduct = openings
            .filter { it.scope == SCOPE_VEHICLE && it.vehicleId == vehicle.id }
            .associateBy { it.productId }

        val vehicleTxByProduct = transactions
            .filter { it.vehicleId == vehicle.id }
            .groupBy { it.productId }

        return products.map { product ->
            val baseOpening = openingByProduct[product.id]
            val productTx = vehicleTxByProduct[product.id].orEmpty()
            val priorTx = productTx.filter { it.businessDate < date }
            val dayTx = productTx.filter { it.businessDate == date }

            var oldFull = baseOpening?.openingFull ?: 0
            var oldEmpty = baseOpening?.openingEmpty ?: 0
            var oldDamaged = 0

            for (tx in priorTx) {
                oldFull += vehicleFullDelta(tx)
                oldEmpty += vehicleEmptyDelta(tx)
                oldDamaged += vehicleDamagedDelta(tx)
            }

            var fullReceived = 0
            var customerSales = 0
            var complaintReplaced = 0
            var fullTransferredOut = 0
            var fullReturnedToStore = 0
            var fullChangedOut = 0
            var customerEmptyReceived = 0
            var emptyReturnedToStore = 0
            var emptyTransferredOut = 0
            var emptyChangedOut = 0
            var damagedAddedToday = 0
            var damagedResolvedToday = 0

            for (tx in dayTx) {
                when (tx.type) {
                    TransactionType.VEHICLE_FULL_RECEIVED,
                    TransactionType.VEHICLE_TRANSFER_FULL_IN,
                    TransactionType.VEHICLE_CUSTOMER_CHANGE_FULL_IN,
                    TransactionType.VEHICLE_ADJUST_FULL_IN -> fullReceived += tx.quantity
                    TransactionType.VEHICLE_CUSTOMER_SALE,
                    TransactionType.VEHICLE_NEW_CUSTOMER_SALE,
                    TransactionType.VEHICLE_ADJUST_FULL_OUT -> customerSales += tx.quantity
                    TransactionType.VEHICLE_TRANSFER_FULL_OUT -> fullTransferredOut += tx.quantity
                    TransactionType.VEHICLE_FULL_RETURN_TO_STORE -> fullReturnedToStore += tx.quantity
                    TransactionType.VEHICLE_CUSTOMER_CHANGE_FULL_OUT -> fullChangedOut += tx.quantity
                    TransactionType.VEHICLE_CUSTOMER_COMPLAINT_REPLACE -> {
                        // 1-for-1 defective shell swap: full out (-qty), empty in (+qty)
                        complaintReplaced += tx.quantity
                    }
                    TransactionType.VEHICLE_EMPTY_FROM_CUSTOMER,
                    TransactionType.VEHICLE_TRANSFER_EMPTY_IN,
                    TransactionType.VEHICLE_CUSTOMER_CHANGE_EMPTY_IN,
                    TransactionType.VEHICLE_ADJUST_EMPTY_IN -> customerEmptyReceived += tx.quantity
                    TransactionType.VEHICLE_EMPTY_RETURN_TO_STORE,
                    TransactionType.VEHICLE_ADJUST_EMPTY_OUT -> emptyReturnedToStore += tx.quantity
                    TransactionType.VEHICLE_TRANSFER_EMPTY_OUT -> emptyTransferredOut += tx.quantity
                    TransactionType.VEHICLE_CUSTOMER_CHANGE_EMPTY_OUT -> emptyChangedOut += tx.quantity
                    TransactionType.VEHICLE_DAMAGED_ADD -> damagedAddedToday += tx.quantity
                    TransactionType.VEHICLE_DAMAGED_REMOVE -> damagedResolvedToday += tx.quantity
                    else -> Unit
                }
            }

            val totalFull = oldFull + fullReceived
            val fullBalance = totalFull - customerSales - complaintReplaced - fullTransferredOut - fullReturnedToStore - fullChangedOut
            val emptyBalance = oldEmpty + customerEmptyReceived + complaintReplaced - emptyReturnedToStore - emptyTransferredOut - emptyChangedOut
            val damagedBalance = (oldDamaged + damagedAddedToday - damagedResolvedToday).coerceAtLeast(0)

            VehicleStockRow(
                vehicle = vehicle,
                product = product,
                oldFull = oldFull,
                fullReceived = fullReceived,
                totalFull = totalFull,
                customerSales = customerSales,
                fullBalance = fullBalance,
                oldEmpty = oldEmpty,
                customerEmptyReceived = customerEmptyReceived,
                emptyReturnedToStore = emptyReturnedToStore,
                emptyBalance = emptyBalance,
                complaintReplaced = complaintReplaced,
                fullTransferredOut = fullTransferredOut,
                emptyTransferredOut = emptyTransferredOut,
                fullReturnedToStore = fullReturnedToStore,
                fullChangedOut = fullChangedOut,
                emptyChangedOut = emptyChangedOut,
                damagedBalance = damagedBalance,
                damagedAddedToday = damagedAddedToday,
                damagedResolvedToday = damagedResolvedToday
            )
        }
    }

    /**
     * Computes live cumulative Store balance (across all dates up to and including [asOfDate],
     * or all dates when [asOfDate] is null).
     */
    fun currentStoreBalance(
        productId: Long,
        openings: List<OpeningStockEntity>,
        transactions: List<StockTransactionEntity>,
        asOfDate: String? = null
    ): Pair<Int, Int> {
        val base = openings.firstOrNull { it.scope == SCOPE_STORE && it.productId == productId }
        var full = base?.openingFull ?: 0
        var empty = base?.openingEmpty ?: 0
        for (tx in transactions) {
            if (tx.productId == productId && (asOfDate == null || tx.businessDate <= asOfDate)) {
                full += storeFullDelta(tx)
                empty += storeEmptyDelta(tx)
            }
        }
        return full to empty
    }

    /**
     * Computes live cumulative Store Damaged cylinder balance.
     */
    fun currentStoreDamagedBalance(
        productId: Long,
        transactions: List<StockTransactionEntity>,
        asOfDate: String? = null
    ): Int {
        var damaged = 0
        for (tx in transactions) {
            if (tx.productId == productId && (asOfDate == null || tx.businessDate <= asOfDate)) {
                damaged += storeDamagedDelta(tx)
            }
        }
        return damaged
    }

    /**
     * Computes live cumulative Vehicle balance (across all dates up to and including [asOfDate],
     * or all dates when [asOfDate] is null).
     */
    fun currentVehicleBalance(
        vehicleId: Long,
        productId: Long,
        openings: List<OpeningStockEntity>,
        transactions: List<StockTransactionEntity>,
        asOfDate: String? = null
    ): Pair<Int, Int> {
        val base = openings.firstOrNull {
            it.scope == SCOPE_VEHICLE && it.vehicleId == vehicleId && it.productId == productId
        }
        var full = base?.openingFull ?: 0
        var empty = base?.openingEmpty ?: 0
        for (tx in transactions) {
            if (tx.vehicleId == vehicleId && tx.productId == productId &&
                (asOfDate == null || tx.businessDate <= asOfDate)
            ) {
                full += vehicleFullDelta(tx)
                empty += vehicleEmptyDelta(tx)
            }
        }
        return full to empty
    }

    /**
     * Computes live cumulative Vehicle Damaged cylinder balance.
     */
    fun currentVehicleDamagedBalance(
        vehicleId: Long,
        productId: Long,
        transactions: List<StockTransactionEntity>,
        asOfDate: String? = null
    ): Int {
        var damaged = 0
        for (tx in transactions) {
            if (tx.vehicleId == vehicleId && tx.productId == productId &&
                (asOfDate == null || tx.businessDate <= asOfDate)
            ) {
                damaged += vehicleDamagedDelta(tx)
            }
        }
        return damaged
    }

    fun storeFullDelta(tx: StockTransactionEntity): Int = when (tx.type) {
        TransactionType.STORE_PURCHASE_FULL,
        TransactionType.STORE_PURCHASE_COMPLAINT_REPLACE,
        TransactionType.VEHICLE_FULL_RETURN_TO_STORE,
        TransactionType.STORE_CUSTOMER_CHANGE_FULL_IN,
        TransactionType.STORE_FULL_INWARD,
        TransactionType.STORE_ADJUST_FULL_IN -> tx.quantity
        TransactionType.VEHICLE_FULL_RECEIVED,
        TransactionType.STORE_FULL_DISPATCH_DIRECT,
        TransactionType.STORE_CUSTOMER_CHANGE_FULL_OUT,
        TransactionType.STORE_ADJUST_FULL_OUT -> -tx.quantity
        else -> 0
    }

    fun storeEmptyDelta(tx: StockTransactionEntity): Int = when (tx.type) {
        TransactionType.STORE_PURCHASE_EMPTY,
        TransactionType.VEHICLE_EMPTY_RETURN_TO_STORE,
        TransactionType.STORE_EMPTY_INWARD,
        TransactionType.STORE_EMPTY_FROM_CUSTOMER,
        TransactionType.STORE_CUSTOMER_CHANGE_EMPTY_IN,
        TransactionType.STORE_ADJUST_EMPTY_IN -> tx.quantity
        TransactionType.STORE_PURCHASE_COMPLAINT_REPLACE,
        TransactionType.STORE_EMPTY_DISPATCH,
        TransactionType.STORE_CUSTOMER_CHANGE_EMPTY_OUT,
        TransactionType.STORE_ADJUST_EMPTY_OUT -> -tx.quantity
        else -> 0
    }

    fun storeDamagedDelta(tx: StockTransactionEntity): Int = when (tx.type) {
        TransactionType.STORE_DAMAGED_ADD -> tx.quantity
        TransactionType.STORE_DAMAGED_REMOVE -> -tx.quantity
        else -> 0
    }

    fun vehicleFullDelta(tx: StockTransactionEntity): Int = when (tx.type) {
        TransactionType.VEHICLE_FULL_RECEIVED,
        TransactionType.VEHICLE_TRANSFER_FULL_IN,
        TransactionType.VEHICLE_CUSTOMER_CHANGE_FULL_IN,
        TransactionType.VEHICLE_ADJUST_FULL_IN -> tx.quantity
        TransactionType.VEHICLE_FULL_RETURN_TO_STORE,
        TransactionType.VEHICLE_CUSTOMER_SALE,
        TransactionType.VEHICLE_NEW_CUSTOMER_SALE,
        TransactionType.VEHICLE_CUSTOMER_COMPLAINT_REPLACE,
        TransactionType.VEHICLE_CUSTOMER_CHANGE_FULL_OUT,
        TransactionType.VEHICLE_TRANSFER_FULL_OUT,
        TransactionType.VEHICLE_ADJUST_FULL_OUT -> -tx.quantity
        else -> 0
    }

    fun vehicleEmptyDelta(tx: StockTransactionEntity): Int = when (tx.type) {
        TransactionType.VEHICLE_EMPTY_FROM_CUSTOMER,
        TransactionType.VEHICLE_CUSTOMER_COMPLAINT_REPLACE,
        TransactionType.VEHICLE_TRANSFER_EMPTY_IN,
        TransactionType.VEHICLE_CUSTOMER_CHANGE_EMPTY_IN,
        TransactionType.VEHICLE_ADJUST_EMPTY_IN -> tx.quantity
        TransactionType.VEHICLE_EMPTY_RETURN_TO_STORE,
        TransactionType.VEHICLE_TRANSFER_EMPTY_OUT,
        TransactionType.VEHICLE_CUSTOMER_CHANGE_EMPTY_OUT,
        TransactionType.VEHICLE_ADJUST_EMPTY_OUT -> -tx.quantity
        TransactionType.CUSTOMER_EMPTY_PENDING_ADD,
        TransactionType.VEHICLE_NEW_CUSTOMER_SALE -> 0
        else -> 0
    }

    fun vehicleDamagedDelta(tx: StockTransactionEntity): Int = when (tx.type) {
        TransactionType.VEHICLE_DAMAGED_ADD -> tx.quantity
        TransactionType.VEHICLE_DAMAGED_REMOVE -> -tx.quantity
        else -> 0
    }
}
