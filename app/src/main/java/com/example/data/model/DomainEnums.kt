package com.example.data.model

enum class UserRole {
    ADMIN,
    STAFF
}

enum class ProductCategory(val displayName: String, val shortCode: String, val sortOrder: Int) {
    INDUSTRIAL_GAS("INDUSTRIAL GAS", "IND", 1),
    SMALL_48_CFT("48 CFT SMALL", "48CFT", 2),
    COOKING_GAS("COOKING GAS", "LPG", 3),
    MCG_CUTTING_GAS("MCG CUTTING GAS", "MCG", 4)
}

/**
 * All stock movement transaction types across Store and Vehicles.
 * Note:
 * - VEHICLE_FULL_RECEIVED == Store Full Dispatch to Vehicle (reduces Store Full, increases Vehicle Full)
 * - VEHICLE_EMPTY_RETURN_TO_STORE == Vehicle Empty Returned to Store (reduces Vehicle Empty, increases Store Empty)
 */
enum class TransactionType(
    val label: String,
    val isStoreLevel: Boolean,
    val isVehicleLevel: Boolean
) {
    STORE_PURCHASE_FULL("Full Purchase (Supplier)", true, false),
    STORE_PURCHASE_EMPTY("Empty Purchase (Supplier)", true, false),
    STORE_FULL_INWARD("Full Cylinder Inward", true, false),
    STORE_EMPTY_INWARD("Empty Cylinder Inward", true, false),
    STORE_FULL_DISPATCH_DIRECT("Direct Full Dispatch", true, false),
    STORE_EMPTY_DISPATCH("Empty Dispatch (Refill/Plant)", true, false),
    STORE_ADJUST_FULL_IN("Store Full Adjustment (+)", true, false),
    STORE_ADJUST_FULL_OUT("Store Full Adjustment (-)", true, false),
    STORE_ADJUST_EMPTY_IN("Store Empty Adjustment (+)", true, false),
    STORE_ADJUST_EMPTY_OUT("Store Empty Adjustment (-)", true, false),

    VEHICLE_FULL_RECEIVED("Full Received from Store", true, true),
    VEHICLE_CUSTOMER_SALE("Customer Sale", false, true),
    VEHICLE_EMPTY_FROM_CUSTOMER("Customer Empty Received", false, true),
    VEHICLE_EMPTY_RETURN_TO_STORE("Empty Returned to Store", true, true),
    VEHICLE_ADJUST_FULL_IN("Vehicle Full Adjustment (+)", false, true),
    VEHICLE_ADJUST_FULL_OUT("Vehicle Full Adjustment (-)", false, true),
    VEHICLE_ADJUST_EMPTY_IN("Vehicle Empty Adjustment (+)", false, true),
    VEHICLE_ADJUST_EMPTY_OUT("Vehicle Empty Adjustment (-)", false, true)
}

enum class ReportType(val title: String, val subtitle: String) {
    DAILY_STORE("Daily Store Report", "Opening, purchases, returns, dispatch & closing"),
    DAILY_VEHICLE("Daily Vehicle Report", "Vehicle-wise full & empty stock movement"),
    MONTHLY_SALES("Monthly Sales Report", "Day-by-day & monthly cylinder sales summary"),
    PRODUCT_WISE_SALES("Product-Wise Sales", "Sales breakdown by gas category & cylinder"),
    STAFF_WISE_SALES("Staff-Wise Sales", "Performance & balances by staff/vehicle"),
    FULL_EMPTY_STOCK("Full & Empty Stock", "Combined Store + Vehicles inventory balance"),
    PURCHASE_REGISTER("Purchase Register", "Supplier company-wise cylinder purchases")
}
