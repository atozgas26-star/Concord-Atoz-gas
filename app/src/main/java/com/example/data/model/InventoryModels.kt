package com.example.data.model

import com.example.data.local.ProductEntity
import com.example.data.local.StockTransactionEntity
import com.example.data.local.UserAccountEntity
import com.example.data.local.VehicleEntity

data class StoreStockRow(
    val product: ProductEntity,
    val openingFull: Int,
    val fullPurchases: Int,
    val fullReturns: Int,
    val fullDispatch: Int,
    val storeFullBalance: Int,
    val openingEmpty: Int,
    val emptyPurchases: Int,
    val emptyReturnsToStore: Int,
    val emptyDispatch: Int,
    val storeEmptyBalance: Int
)

data class VehicleStockRow(
    val vehicle: VehicleEntity,
    val product: ProductEntity,
    val oldFull: Int,
    val fullReceived: Int,
    val totalFull: Int,
    val customerSales: Int,
    val fullBalance: Int,
    val oldEmpty: Int,
    val customerEmptyReceived: Int,
    val emptyReturnedToStore: Int,
    val emptyBalance: Int,
    val complaintReplaced: Int = 0
)

data class VehicleOverview(
    val vehicle: VehicleEntity,
    val totalOldFull: Int,
    val totalFullReceived: Int,
    val totalFull: Int,
    val totalCustomerSales: Int,
    val totalFullBalance: Int,
    val totalOldEmpty: Int,
    val totalCustomerEmptyReceived: Int,
    val totalEmptyReturnedToStore: Int,
    val totalEmptyBalance: Int,
    val totalComplaintReplaced: Int = 0
)

data class DashboardSummary(
    val selectedDate: String,
    val totalFullCylinders: Int,
    val totalEmptyCylinders: Int,
    val storeFullBalance: Int,
    val storeEmptyBalance: Int,
    val allVehiclesFullBalance: Int,
    val allVehiclesEmptyBalance: Int,
    val todayFullPurchases: Int,
    val todayEmptyPurchases: Int,
    val todayFullDispatch: Int,
    val todayEmptyDispatch: Int,
    val todaySales: Int,
    val vehicleOverviews: List<VehicleOverview>,
    val recentTransactions: List<TransactionDisplayItem>,
    val pendingDeliveryCylinders: Int = allVehiclesFullBalance,
    val activeDeliveryVehiclesCount: Int = vehicleOverviews.count { it.totalFullBalance > 0 },
    val todayStoreActivityCount: Int = 0,
    val recentStoreActivity: List<TransactionDisplayItem> = emptyList()
)

data class TransactionDisplayItem(
    val transaction: StockTransactionEntity,
    val productName: String,
    val productCategory: ProductCategory
)

data class ProductSalesRow(
    val product: ProductEntity,
    val totalSold: Int,
    val emptyCollected: Int
)

data class StaffSalesRow(
    val vehicle: VehicleEntity,
    val totalFullReceived: Int,
    val totalSold: Int,
    val totalEmptyCollected: Int,
    val totalEmptyReturned: Int,
    val closingFullBalance: Int,
    val closingEmptyBalance: Int
)

data class MonthlyDailySaleRow(
    val date: String,
    val totalSold: Int,
    val totalPurchases: Int,
    val totalDispatch: Int,
    val totalEmptyReturned: Int
)

data class CompanyStockRow(
    val product: ProductEntity,
    val storeFull: Int,
    val vehiclesFull: Int,
    val totalFull: Int,
    val storeEmpty: Int,
    val vehiclesEmpty: Int,
    val totalEmpty: Int
)

data class CustomerPendingRow(
    val customerName: String,
    val primaryVehicleId: Long?,
    val primaryVehicleName: String,
    val fullSold: Int,
    val emptyCollected: Int,
    val manualPendingAdded: Int,
    val pendingEmptyBalance: Int,
    val lastActivityDate: String,
    val lastProductId: Long?,
    val lastProductName: String
)

data class AuthSession(
    val user: UserAccountEntity,
    val assignedVehicle: VehicleEntity?
)

data class LowStockProductItem(
    val productId: Long,
    val productName: String,
    val category: ProductCategory,
    val storeFullBalance: Int,
    val vehiclesFullBalance: Int,
    val totalFullBalance: Int,
    val threshold: Int
)

data class LowStockAlertState(
    val isAlertVisible: Boolean = false,
    val threshold: Int = DEFAULT_LOW_STOCK_THRESHOLD,
    val lowStockItems: List<LowStockProductItem> = emptyList(),
    val isDismissedForCurrentAlert: Boolean = false
) {
    val activeAlertCount: Int
        get() = lowStockItems.size

    val outOfStockCount: Int
        get() = lowStockItems.count { it.storeFullBalance <= 0 }

    val shouldShowBanner: Boolean
        get() = isAlertVisible && !isDismissedForCurrentAlert && lowStockItems.isNotEmpty()

    companion object {
        const val DEFAULT_LOW_STOCK_THRESHOLD = 5
    }
}

sealed class OperationResult {
    data class Success(val message: String) : OperationResult()
    data class InsufficientStock(
        val title: String,
        val message: String
    ) : OperationResult()
    data class ValidationError(val message: String) : OperationResult()
}
