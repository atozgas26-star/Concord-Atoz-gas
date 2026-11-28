package com.example

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.local.AuthorizedMobileEntity
import com.example.data.local.RegisteredDeviceEntity
import com.example.data.local.SecurityAuditLogEntity
import com.example.data.local.UserAccountEntity
import com.example.data.model.ThemePreset
import com.example.data.model.UserRole
import com.example.ui.components.QuickCalculatorPopupDialog
import com.example.ui.components.SecurityCenterDialog
import com.example.ui.theme.ConcordGasTheme
import com.example.ui.theme.DarkCharcoalImportant
import com.example.ui.theme.DarkSlateSecondary
import com.example.ui.theme.DeepNavyText
import com.example.ui.theme.ElectricBlueTextLight
import com.example.ui.theme.ErrorDarkRed
import com.example.ui.theme.ObsidianMidnight
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SuccessDarkGreen
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.theme.WarningDarkOrange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xxhdpi", application = ConcordGasApp::class)
class GlobalTextVisibilityContrastTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun relativeLuminance(color: Color): Double {
        fun channel(c: Float): Double {
            val v = c.toDouble()
            return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
    }

    private fun contrastRatio(fg: Color, bg: Color): Double {
        val l1 = relativeLuminance(fg)
        val l2 = relativeLuminance(bg)
        return (max(l1, l2) + 0.05) / (min(l1, l2) + 0.05)
    }

    @Test
    fun allLightAndDarkTextColorPairs_meetWcagAaMinimum45To1Contrast() {
        // Light backgrounds (#FFFFFF card)
        assertTrue("Main text Deep Navy (#14233B) on White", contrastRatio(DeepNavyText, PureWhite) >= 4.5)
        assertTrue("Important text Dark Charcoal (#202938) on White", contrastRatio(DarkCharcoalImportant, PureWhite) >= 4.5)
        assertTrue("Secondary text Dark Slate (#475569) on White", contrastRatio(DarkSlateSecondary, PureWhite) >= 4.5)
        assertTrue("Electric Blue accent text on White", contrastRatio(ElectricBlueTextLight, PureWhite) >= 4.5)
        assertTrue("Success Dark Green on White", contrastRatio(SuccessDarkGreen, PureWhite) >= 4.5)
        assertTrue("Warning Dark Orange on White", contrastRatio(WarningDarkOrange, PureWhite) >= 4.5)
        assertTrue("Error Dark Red on White", contrastRatio(ErrorDarkRed, PureWhite) >= 4.5)

        // Dark backgrounds (ObsidianMidnight & ObsidianSurface)
        assertTrue("Main text Pure White on ObsidianMidnight", contrastRatio(TextPrimaryDark, ObsidianMidnight) >= 4.5)
        assertTrue("Secondary text Light Silver on ObsidianMidnight", contrastRatio(TextSecondaryDark, ObsidianMidnight) >= 4.5)
        assertTrue("Main text Pure White on ObsidianSurface", contrastRatio(TextPrimaryDark, ObsidianSurface) >= 4.5)
        assertTrue("Secondary text Light Silver on ObsidianSurface", contrastRatio(TextSecondaryDark, ObsidianSurface) >= 4.5)

        // Verify full 1.0 opacity on all text tokens (never transparent text)
        listOf(
            DeepNavyText,
            DarkCharcoalImportant,
            DarkSlateSecondary,
            ElectricBlueTextLight,
            SuccessDarkGreen,
            WarningDarkOrange,
            ErrorDarkRed,
            TextPrimaryDark,
            TextSecondaryDark
        ).forEach { token ->
            assertEquals("Text color token must have full 1.0f alpha", 1.0f, token.alpha, 0.0001f)
        }
    }

    @Test
    fun themeColorSchemes_lightAndDark_bindExactHighContrastTextTokens() {
        var lightSurface = Color.Transparent
        var lightOnSurface = Color.Transparent
        var lightOnSurfaceVariant = Color.Transparent
        var lightError = Color.Transparent
        var darkOnSurface = Color.Transparent
        var darkOnSurfaceVariant = Color.Transparent

        composeRule.setContent {
            ConcordGasTheme(themePreset = ThemePreset.EMERALD_CORPORATE, darkTheme = false) {
                lightSurface = MaterialTheme.colorScheme.surface
                lightOnSurface = MaterialTheme.colorScheme.onSurface
                lightOnSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
                lightError = MaterialTheme.colorScheme.error
            }
            ConcordGasTheme(themePreset = ThemePreset.EMERALD_CORPORATE, darkTheme = true) {
                darkOnSurface = MaterialTheme.colorScheme.onSurface
                darkOnSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
            }
        }
        composeRule.waitForIdle()

        assertEquals(PureWhite, lightSurface)
        assertEquals(DeepNavyText, lightOnSurface)
        assertEquals(DarkSlateSecondary, lightOnSurfaceVariant)
        assertEquals(ErrorDarkRed, lightError)
        assertEquals(PureWhite, darkOnSurface)
        assertEquals(TextSecondaryDark, darkOnSurfaceVariant)
    }

    @Test
    fun corporateSecurityCenter_rendersAllTabsAndAlertsClearlyInBrightLuxuryMode() {
        val owner = UserAccountEntity(
            id = 10L,
            username = "owner",
            displayName = "Owner Director",
            role = UserRole.OWNER_VIEW,
            pin = "",
            isActive = true
        )
        val staff = UserAccountEntity(
            id = 11L,
            username = "nadeem",
            displayName = "Nadeem Driver",
            role = UserRole.STAFF,
            pin = "",
            isActive = true
        )
        val mobiles = listOf(
            AuthorizedMobileEntity(
                id = 1L,
                phoneE164 = "+971501234567",
                displayName = "Owner Director",
                role = UserRole.OWNER_VIEW,
                staffUserId = owner.id,
                isActive = true,
                isProtectedOwner = true
            ),
            AuthorizedMobileEntity(
                id = 2L,
                phoneE164 = "+971559876543",
                displayName = "Nadeem Driver",
                role = UserRole.STAFF,
                staffUserId = staff.id,
                isActive = true,
                isProtectedOwner = false,
                registeredDeviceId = "DEV-SAMSUNG-A55-991"
            )
        )
        val devices = listOf(
            RegisteredDeviceEntity(
                id = 1L,
                deviceId = "DEV-SAMSUNG-A55-991",
                deviceLabel = "Samsung Galaxy A55",
                phoneE164 = "+971559876543",
                userId = staff.id,
                displayName = "Nadeem Driver",
                role = UserRole.STAFF,
                isRevoked = false,
                lastActiveMs = System.currentTimeMillis()
            )
        )
        val logs = listOf(
            SecurityAuditLogEntity(
                id = 101L,
                eventType = "FAILED_PIN_LOGIN",
                role = "STAFF",
                actorName = "Nadeem Driver",
                phoneE164 = "+971559876543",
                deviceLabel = "Samsung Galaxy A55",
                details = "Incorrect PIN attempt #3 detected from fleet handset.",
                isAlert = true,
                timestampMs = System.currentTimeMillis()
            ),
            SecurityAuditLogEntity(
                id = 102L,
                eventType = "CATEGORY_REORDERED",
                role = "ADMIN",
                actorName = "Admin",
                phoneE164 = "+971501234567",
                deviceLabel = "Office Tablet",
                details = "Category folders reordered: LPG Gas moved to position #1.",
                isAlert = false,
                timestampMs = System.currentTimeMillis() - 60_000L
            )
        )

        composeRule.setContent {
            ConcordGasTheme(themePreset = ThemePreset.EMERALD_CORPORATE, darkTheme = false) {
                SecurityCenterDialog(
                    authorizedMobiles = mobiles,
                    registeredDevices = devices,
                    securityLogs = logs,
                    staffUsers = listOf(owner, staff),
                    isReadOnly = false,
                    onUpsertAuthorizedMobile = { _, _, _, _, _, _ -> },
                    onDeleteAuthorizedMobile = {},
                    onRevokeDevice = {},
                    onLogoutAllDevices = {},
                    onDismiss = {}
                )
            }
        }
        composeRule.waitForIdle()

        // Header & Tab 0 (+971 Access + Staff Permissions)
        composeRule.onNodeWithTag("security_center_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("Corporate Security Center").assertIsDisplayed()
        composeRule.onNodeWithTag("security_tab_mobiles").assertIsDisplayed()

        // Switch to Devices Tab
        composeRule.onNodeWithTag("security_tab_devices").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Samsung Galaxy A55", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Nadeem Driver", substring = true).assertIsDisplayed()

        // Switch to Alerts / Logs Tab
        composeRule.onNodeWithTag("security_tab_logs").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("FAILED_PIN_LOGIN").assertIsDisplayed()
        composeRule.onNodeWithText("Incorrect PIN attempt #3 detected from fleet handset.").assertIsDisplayed()
        composeRule.onNodeWithText("CATEGORY_REORDERED").assertIsDisplayed()
        composeRule.onNodeWithText("Category folders reordered: LPG Gas moved to position #1.").assertIsDisplayed()
    }

    @Test
    fun quickCalculatorPopup_rendersClearlyInBrightLuxuryLightMode() {
        composeRule.setContent {
            ConcordGasTheme(themePreset = ThemePreset.EMERALD_CORPORATE, darkTheme = false) {
                QuickCalculatorPopupDialog(onDismiss = {})
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("quick_calculator_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("calc_mode_normal").assertIsDisplayed()
        composeRule.onNodeWithTag("calc_mode_vat5").assertIsDisplayed()
        composeRule.onNodeWithTag("calc_display_text").assertIsDisplayed()

        composeRule.onNodeWithTag("calc_mode_vat5").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Bill Amount").assertIsDisplayed()
        composeRule.onNodeWithText("VAT (5%)").assertIsDisplayed()
        composeRule.onNodeWithText("Grand Total").assertIsDisplayed()
    }
}
