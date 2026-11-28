package com.example

import android.content.Context
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.font.FontWeight
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.auth.AdminCredentialCrypto
import com.example.data.local.ConcordDatabase
import com.example.data.local.SessionPreferences
import com.example.data.local.UserAccountEntity
import com.example.data.model.OperationResult
import com.example.data.model.ProductCategory
import com.example.data.model.ReportType
import com.example.data.model.StockCalculator
import com.example.data.model.ThemePreset
import com.example.data.model.TransactionType
import com.example.data.model.UserRole
import com.example.data.repository.InventoryRepository
import com.example.export.BackupManager
import com.example.ui.components.StartupSplashOverlay
import com.example.ui.components.categoryVisualStyle
import com.example.ui.screens.LoginScreen
import com.example.ui.theme.CategoryElectricBlue
import com.example.ui.theme.CategoryEmeraldGreen
import com.example.ui.theme.CategoryMetallicOrange
import com.example.ui.theme.CategoryVioletBlue
import com.example.ui.theme.ConcordGasTheme
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.Typography
import com.example.ui.viewmodel.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xxhdpi", application = ConcordGasApp::class)
class MasterV510BrightLuxuryTest {

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
    fun pinOnlyLoginScreen_removesMobileAndOtpCompletely_andShowsPinSecurityBadge() {
        val staffUser = UserAccountEntity(
            id = 2L,
            username = "nadeem",
            displayName = "Nadeem",
            role = UserRole.STAFF,
            pin = "",
            pinHash = AdminCredentialCrypto.hashPassword("1001"),
            isActive = true
        )
        composeRule.setContent {
            ConcordGasTheme(themePreset = ThemePreset.EMERALD_CORPORATE, darkTheme = false) {
                LoginScreen(
                    users = listOf(staffUser),
                    loginError = null,
                    onLogin = { _, _, _ -> },
                    onClearError = {}
                )
            }
        }
        composeRule.waitForIdle()

        // Strictly ZERO mobile number or SMS OTP UI nodes on LoginScreen
        composeRule.onNodeWithTag("uae_phone_otp_card").assertDoesNotExist()
        composeRule.onNodeWithTag("uae_phone_input").assertDoesNotExist()
        composeRule.onNodeWithTag("uae_otp_code_input").assertDoesNotExist()
        composeRule.onNodeWithTag("uae_otp_send_button").assertDoesNotExist()
        composeRule.onNodeWithTag("uae_otp_verify_button").assertDoesNotExist()

        // PIN-only security badge is displayed
        composeRule.onNodeWithTag("pin_only_security_badge").assertExists()
    }

    @Test
    fun pinOnlyStaffLogin_enforcesFiveAttemptLockoutWithoutMobileNumber_andLogsPinLockout() = runTest {
        repository.initializeDatabase()
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
        waitUntil { vm.isInitialized.value }

        // 5 consecutive wrong PIN attempts with ZERO mobile numbers registered
        repeat(5) {
            vm.clearLoginError()
            vm.login(nadeem.id, "9999", UserRole.STAFF)
            waitUntil { !vm.isAuthenticating.value && vm.loginError.value != null }
        }

        // 6th attempt even with valid PIN is blocked while temporary lockout is active
        vm.clearLoginError()
        vm.login(nadeem.id, "1001", UserRole.STAFF)
        waitUntil { !vm.isAuthenticating.value && vm.loginError.value != null }
        assertNull(vm.authSession.value)
        assertTrue(vm.loginError.value.orEmpty().contains("locked", ignoreCase = true))

        val logs = repository.securityAuditLogsFlow.first()
        assertTrue(logs.any { it.eventType == "PIN_LOCKOUT" && it.isAlert })
    }

    @Test
    fun categoryDragDropReorder_andCategoryRenameAndProductMove_preserveStockAndAutoSyncReports() = runTest {
        repository.initializeDatabase()
        val admin = repository.usersFlow.first().first { it.role == UserRole.ADMIN }
        val staff = repository.usersFlow.first().first { it.role == UserRole.STAFF }
        val today = "2026-11-28"

        val initialCats = repository.categoriesFlow.first()
        assertEquals(listOf(1L, 2L, 3L, 4L), initialCats.map { it.id })

        // Seed opening stock & purchase on LPG 22 KG (product in category 3L)
        val lpg22 = repository.activeProductsFlow.first().first { it.name == "22 KG" }
        assertEquals(3L, lpg22.categoryId)
        assertTrue(
            repository.setOpeningStockForDate(StockCalculator.SCOPE_STORE, 0L, lpg22.id, today, 25, 9, admin)
                is OperationResult.Success
        )
        assertTrue(
            repository.recordTransaction(today, TransactionType.STORE_PURCHASE_FULL, lpg22.id, null, admin, 5, "Stock check")
                is OperationResult.Success
        )

        // 1. Staff is strictly blocked from reordering category folders
        assertTrue(
            repository.reorderCategories(listOf(3L, 1L, 4L, 2L), staff) is OperationResult.ValidationError
        )

        // 2. Admin reorders category folders: LPG (3L) first, then 48 CFT Small (4L), Cutting (2L), Industrial (1L)
        assertTrue(
            repository.reorderCategories(listOf(3L, 4L, 2L, 1L), admin) is OperationResult.Success
        )
        val reorderedCats = repository.categoriesFlow.first()
        assertEquals(listOf(3L, 4L, 2L, 1L), reorderedCats.map { it.id })

        // 3. Admin renames LPG category folder -> permanent ID #3 preserved
        assertTrue(
            repository.updateCategory(
                categoryId = 3L,
                name = "LPG COOKING CYLINDERS",
                description = "Updated LPG description",
                iconKey = "LPG",
                categoryType = "LPG",
                isActive = true,
                actor = admin
            ) is OperationResult.Success
        )

        // 4. Verify stock balance on LPG 22 KG is 100% untouched after reorder & rename
        val storeRow = repository.observeStoreStock(today).first().first { it.product.id == lpg22.id }
        assertEquals(30, storeRow.storeFullBalance)
        assertEquals(9, storeRow.storeEmptyBalance)

        // 5. Verify ViewModel Report Table automatically reflects BOTH the new Admin sort order AND live renamed category
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
        vm.setSelectedDate(today)
        backgroundScope.launch(Dispatchers.Unconfined) { vm.categories.collect {} }
        backgroundScope.launch(Dispatchers.Unconfined) { vm.companyStockReport.collect {} }
        waitUntil {
            vm.isInitialized.value &&
                vm.categories.value.firstOrNull()?.id == 3L &&
                vm.companyStockReport.value.size == 18
        }

        val stockReport = vm.buildReportTable(ReportType.FULL_EMPTY_STOCK)
        assertTrue(stockReport.rows.isNotEmpty())
        // First row must now belong to the reordered #1 category (3L) with its LIVE edited name!
        assertEquals("LPG COOKING CYLINDERS", stockReport.rows.first()[0])

        // 6. Verify CATEGORY_REORDERED audit log exists
        val logs = repository.securityAuditLogsFlow.first()
        assertTrue(logs.any { it.eventType == "CATEGORY_REORDERED" })
    }

    @Test
    fun brightPremium3DTheme_categoryColors_globalBoldTypography_andCinematicSplashWelcome() {
        // 1. Default theme (darkTheme = false) must be Bright Luxury (LocalIsDarkTheme == false)
        var capturedIsDark = true
        composeRule.setContent {
            ConcordGasTheme(themePreset = ThemePreset.EMERALD_CORPORATE, darkTheme = false) {
                capturedIsDark = LocalIsDarkTheme.current
                StartupSplashOverlay(
                    themePreset = ThemePreset.EMERALD_CORPORATE,
                    soundEnabled = false,
                    onSplashFinished = {}
                )
            }
        }
        composeRule.waitForIdle()
        assertFalse("Default theme must be Bright Luxury, not dark!", capturedIsDark)

        // 2. Cinematic splash renders WELCOME TO C&A GAS, Skip button, and Mute toggle
        composeRule.onNodeWithTag("splash_welcome_headline").assertExists()
        composeRule.onNodeWithText("WELCOME TO C&A GAS", substring = true).assertExists()
        composeRule.onNodeWithTag("splash_skip_button").assertIsDisplayed()
        composeRule.onNodeWithTag("splash_sound_mute_toggle").assertIsDisplayed()

        // 3. Exact Section 3 Category Folder Colors
        assertEquals(CategoryElectricBlue, categoryVisualStyle(ProductCategory.INDUSTRIAL_GAS).primaryColor)
        assertEquals(CategoryMetallicOrange, categoryVisualStyle(ProductCategory.COOKING_GAS).primaryColor)
        assertEquals(CategoryEmeraldGreen, categoryVisualStyle(ProductCategory.MCG_CUTTING_GAS).primaryColor)
        assertEquals(CategoryVioletBlue, categoryVisualStyle(ProductCategory.SMALL_48_CFT).primaryColor)

        // 4. Global Bold Typography weights (ExtraBold 800 titles/KPIs, Bold 700 headings/buttons, Normal 400 body)
        assertEquals(FontWeight.ExtraBold, Typography.displaySmall.fontWeight)
        assertEquals(FontWeight.ExtraBold, Typography.headlineMedium.fontWeight)
        assertEquals(FontWeight.ExtraBold, Typography.headlineSmall.fontWeight)
        assertEquals(FontWeight.Bold, Typography.titleLarge.fontWeight)
        assertEquals(FontWeight.Bold, Typography.titleMedium.fontWeight)
        assertEquals(FontWeight.Bold, Typography.titleSmall.fontWeight)
        assertEquals(FontWeight.Bold, Typography.labelLarge.fontWeight)
        assertEquals(FontWeight.Normal, Typography.bodyMedium.fontWeight)
    }
}
