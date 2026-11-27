package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.local.ProductEntity
import com.example.data.model.ProductCategory
import com.example.data.model.StoreStockRow
import com.example.data.model.ThemePreset
import com.example.ui.components.QuickCalculatorPopupDialog
import com.example.ui.components.StockCalculatorDialog
import com.example.ui.screens.StoreManagementScreen
import com.example.ui.theme.ConcordGasTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xxhdpi", application = ConcordGasApp::class)
class PremiumV201UiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun quickCalculatorPopup_hasOrangeCloseButtonAndDismissesImmediately() {
        var dismissed = false
        composeRule.setContent {
            ConcordGasTheme(themePreset = ThemePreset.EMERALD_CORPORATE, darkTheme = false) {
                QuickCalculatorPopupDialog(
                    onDismiss = { dismissed = true },
                    onResultCopied = {}
                )
            }
        }

        composeRule.onNodeWithTag("quick_calculator_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("calc_close_button").assertIsDisplayed()
        composeRule.onNodeWithText("CLOSE").assertIsDisplayed()

        // Calculator keys still work before close
        composeRule.onNodeWithTag("calc_key_7").performScrollTo().performClick()
        composeRule.onNodeWithTag("calc_key_+").performScrollTo().performClick()
        composeRule.onNodeWithTag("calc_key_3").performScrollTo().performClick()
        composeRule.onNodeWithTag("calc_key_=").performScrollTo().performClick()
        composeRule.onNodeWithTag("calc_display_text").assertTextEquals("10")

        // Close dismisses immediately
        composeRule.onNodeWithTag("calc_close_button").performScrollTo().performClick()
        assertTrue(dismissed)
    }

    @Test
    fun stockCalculatorDialog_hasCloseButtonAndDismissesImmediately() {
        var dismissed = false
        val sampleProducts = listOf(
            ProductEntity(id = 1L, category = ProductCategory.INDUSTRIAL_GAS, name = "Oxygen", sortOrder = 101)
        )
        composeRule.setContent {
            ConcordGasTheme(themePreset = ThemePreset.EMERALD_CORPORATE, darkTheme = false) {
                StockCalculatorDialog(
                    products = sampleProducts,
                    storeFullProvider = { 12 },
                    storeEmptyProvider = { 4 },
                    onDismiss = { dismissed = true }
                )
            }
        }
        composeRule.onNodeWithTag("stock_calc_close_button").assertIsDisplayed()
        composeRule.onNodeWithTag("stock_calc_close_button").performClick()
        assertTrue(dismissed)
    }

    @Test
    fun storeManagementScreen_rendersFoldersAndOpensCategory() {
        val sampleProducts = listOf(
            ProductEntity(id = 1L, category = ProductCategory.INDUSTRIAL_GAS, name = "Oxygen", sortOrder = 101),
            ProductEntity(id = 2L, category = ProductCategory.COOKING_GAS, name = "11kg", sortOrder = 301)
        )
        val storeRows = listOf(
            StoreStockRow(
                product = sampleProducts[0],
                openingFull = 10,
                openingEmpty = 4,
                fullPurchases = 5,
                emptyPurchases = 0,
                fullReturns = 0,
                fullDispatch = 2,
                emptyReturnsToStore = 1,
                emptyDispatch = 0,
                storeFullBalance = 13,
                storeEmptyBalance = 5
            ),
            StoreStockRow(
                product = sampleProducts[1],
                openingFull = 8,
                openingEmpty = 2,
                fullPurchases = 0,
                emptyPurchases = 0,
                fullReturns = 0,
                fullDispatch = 0,
                emptyReturnsToStore = 0,
                emptyDispatch = 0,
                storeFullBalance = 8,
                storeEmptyBalance = 2
            )
        )

        composeRule.setContent {
            ConcordGasTheme(themePreset = ThemePreset.EMERALD_CORPORATE, darkTheme = false) {
                StoreManagementScreen(
                    selectedDate = "2026-11-27",
                    storeRows = storeRows,
                    products = sampleProducts,
                    vehicles = emptyList(),
                    transactions = emptyList(),
                    onDateSelected = {},
                    onRecordTransaction = { _, _, _, _, _, _, _ -> },
                    onUpdateOpeningStock = { _, _, _ -> },
                    onDeleteTransaction = {}
                )
            }
        }

        composeRule.onNodeWithText("Inside Store Category Folders").assertIsDisplayed()
        composeRule.onNodeWithTag("category_folder_industrial_gas").performClick()
        composeRule.onNodeWithText("Oxygen").performScrollTo().assertIsDisplayed()
    }
}
