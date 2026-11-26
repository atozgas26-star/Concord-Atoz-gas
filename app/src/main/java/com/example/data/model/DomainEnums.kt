package com.example.data.model

enum class UserRole {
    ADMIN,
    STAFF,
    OWNER_VIEW
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
    STORE_PURCHASE_COMPLAINT_REPLACE("Supplier Complaint Replace", true, false),
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
    VEHICLE_NEW_CUSTOMER_SALE("New Customer Full Sale", false, true),
    VEHICLE_CUSTOMER_COMPLAINT_REPLACE("Customer Complaint Replace", false, true),
    VEHICLE_EMPTY_FROM_CUSTOMER("Customer Empty Received", false, true),
    VEHICLE_EMPTY_RETURN_TO_STORE("Empty Returned to Store", true, true),
    VEHICLE_TRANSFER_FULL_OUT("Full Transfer Out (To Vehicle)", false, true),
    VEHICLE_TRANSFER_FULL_IN("Full Transfer In (From Vehicle)", false, true),
    VEHICLE_TRANSFER_EMPTY_OUT("Empty Transfer Out (To Vehicle)", false, true),
    VEHICLE_TRANSFER_EMPTY_IN("Empty Transfer In (From Vehicle)", false, true),
    VEHICLE_ADJUST_FULL_IN("Vehicle Full Adjustment (+)", false, true),
    VEHICLE_ADJUST_FULL_OUT("Vehicle Full Adjustment (-)", false, true),
    VEHICLE_ADJUST_EMPTY_IN("Vehicle Empty Adjustment (+)", false, true),
    VEHICLE_ADJUST_EMPTY_OUT("Vehicle Empty Adjustment (-)", false, true),

    CUSTOMER_EMPTY_PENDING_ADD("Customer Pending Empty (+)", false, false)
}

enum class ReportType(val title: String, val subtitle: String) {
    DAILY_STORE("Daily Store Report", "Opening, purchases, returns, dispatch & closing"),
    DAILY_VEHICLE("Daily Vehicle Report", "Vehicle-wise full & empty stock movement"),
    STAFF_PRODUCT_SALES("Staff Product Sales", "Daily & monthly staff sales breakdown product / cylinder wise"),
    PURCHASE_COMPANY_PRODUCT("Purchase Co. Product", "Daily & monthly purchase company breakdown product / cylinder wise"),
    MONTHLY_SALES("Monthly Sales Report", "Day-by-day & monthly cylinder sales summary"),
    PRODUCT_WISE_SALES("Product-Wise Sales", "Sales breakdown by gas category & cylinder"),
    STAFF_WISE_SALES("Staff-Wise Sales", "Performance & balances by staff/vehicle"),
    FULL_EMPTY_STOCK("Full & Empty Stock", "Combined Store + Vehicles inventory balance"),
    PURCHASE_REGISTER("Purchase Register", "Daily & monthly supplier entry log & complaint replacements"),
    CUSTOMER_PENDING("Customer Pending Empty", "Customer-wise full sold, empty collected & pending balance"),
    STAFF_WORK_HOURS("Staff Work Hours", "Daily punch in, lunch break, duty close & net hours (Admin/Owner)")
}

enum class StoreTransferDirection(val label: String) {
    STORE_TO_VEHICLE_FULL("Full Received (Store → Vehicle)"),
    VEHICLE_TO_STORE_EMPTY("Empty Return (Vehicle → Store)")
}

enum class ApprovalStatus(val label: String) {
    PENDING("Pending Admin Check"),
    APPROVED("Accepted by Admin"),
    REJECTED("Rejected by Admin")
}

enum class NotificationCategory(val label: String) {
    TRANSFER_EXTRA("Vehicle Transfer"),
    STORE_APPROVAL("Store Approval"),
    ATTENDANCE("Staff Duty"),
    CHAT_IMPORTANT("Important Message"),
    SYSTEM_UPDATE("App Update"),
    INFO("Information")
}

enum class NotificationTargetScope {
    ALL,
    ADMIN_OWNER,
    STAFF
}

enum class ChatChannel(val label: String) {
    TEAM_CHAT("Team & Staff Chat"),
    ADMIN_OWNER("Admin & Owner Direct")
}

enum class ThemePreset(val title: String, val subtitle: String) {
    EMERALD_CORPORATE("Emerald Gas", "Classic green & red"),
    OCEANIC_TEAL("Oceanic Teal", "Deep industrial teal"),
    SUNSET_AMBER("Flame Amber", "Warm copper & navy"),
    MIDNIGHT_DARK("Midnight Pro", "High-contrast dark mode")
}

enum class AutoLogoutMode(val label: String, val idleTimeoutMs: Long) {
    OFF("Stay Logged In", 0L),
    ON_MINIMIZE("Lock on App Minimize", 0L),
    IDLE_5_MIN("Auto Logout (5 Min Idle)", 300_000L),
    IDLE_15_MIN("Auto Logout (15 Min Idle)", 900_000L)
}
