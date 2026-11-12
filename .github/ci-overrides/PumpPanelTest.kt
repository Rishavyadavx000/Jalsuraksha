package com.jalsuraksha.app

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jalsuraksha.app.data.AppSettings
import com.jalsuraksha.app.data.DeviceSnapshot
import com.jalsuraksha.app.data.DeviceStatus
import com.jalsuraksha.app.data.FirmwarePumpPermission
import com.jalsuraksha.app.data.LinkState
import com.jalsuraksha.app.data.PumpState
import com.jalsuraksha.app.ui.UiSnapshot
import com.jalsuraksha.app.ui.screens.PumpControlPanel
import com.jalsuraksha.app.ui.theme.JalTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Needs an emulator or device: ./gradlew connectedDebugAndroidTest. Not run during delivery. */
@RunWith(AndroidJUnit4::class)
class PumpPanelTest {

    @get:Rule
    val rule = createComposeRule()

    private fun snapshot(
        permission: FirmwarePumpPermission,
        pump: PumpState = PumpState.OFF,
        link: LinkState = LinkState.ONLINE,
        requestsEnabled: Boolean = true,
    ): UiSnapshot {
        val status = DeviceStatus(
            wifi = null, ip = null, rssiDbm = -55,
            pump = pump, pumpPermission = permission,
            flowLpm = 0.0, totalLiters = 1.0,
            deviceId = null, apiVersion = null, receivedAtMs = 1_000L,
        )
        return UiSnapshot(
            settingsLoaded = true,
            settings = AppSettings(host = "192.168.1.50", pumpRequestsEnabled = requestsEnabled),
            device = DeviceSnapshot(status = status, lastSuccessMs = 1_000L),
            link = link,
            nowMs = 1_500L,
        )
    }

    @Test
    fun lockedFirmwareDisablesTurnOnAndSaysWhy() {
        rule.setContent {
            JalTheme(darkTheme = false) {
                PumpControlPanel(ui = snapshot(FirmwarePumpPermission.LOCKED), onRequest = {})
            }
        }
        rule.onNodeWithTag("pump_on_button").assertIsNotEnabled()
        rule.onNodeWithTag("pump_on_reason").assertTextContains("locked by the firmware", substring = true)
    }

    @Test
    fun appSideSwitchOffDisablesBothButtonsEvenWhenFirmwareAllowsThePump() {
        rule.setContent {
            JalTheme(darkTheme = false) {
                PumpControlPanel(
                    ui = snapshot(FirmwarePumpPermission.ENABLED, PumpState.ON, requestsEnabled = false),
                    onRequest = {},
                )
            }
        }
        rule.onNodeWithTag("pump_on_button").assertIsNotEnabled()
        rule.onNodeWithTag("pump_off_button").assertIsNotEnabled()
        rule.onNodeWithTag("pump_on_reason").assertTextContains("switched off in this app", substring = true)
    }

    @Test
    fun staleDataShowsUnknownNotTheLastState() {
        rule.setContent {
            JalTheme(darkTheme = false) {
                PumpControlPanel(
                    ui = snapshot(FirmwarePumpPermission.ENABLED, PumpState.ON, LinkState.STALE),
                    onRequest = {},
                )
            }
        }
        rule.onNodeWithText("Pump state unknown").assertExists()
    }

    @Test
    fun staleDataDisablesBothButtons() {
        rule.setContent {
            JalTheme(darkTheme = false) {
                PumpControlPanel(
                    ui = snapshot(FirmwarePumpPermission.ENABLED, PumpState.ON, LinkState.STALE),
                    onRequest = {},
                )
            }
        }
        rule.onNodeWithTag("pump_on_button").assertIsNotEnabled()
        rule.onNodeWithTag("pump_off_button").assertIsNotEnabled()
    }

    @Test
    fun turnOnNeedsAnExplicitConfirmationBeforeAnythingIsSent() {
        var requested: Boolean? = null
        rule.setContent {
            JalTheme(darkTheme = false) {
                PumpControlPanel(
                    ui = snapshot(FirmwarePumpPermission.ENABLED),
                    onRequest = { requested = it },
                )
            }
        }
        rule.onNodeWithTag("pump_on_button").assertIsEnabled().performClick()
        assertNull(requested)
        rule.onNodeWithText("Request ON").assertIsNotEnabled()
        rule.onNodeWithText("I have checked", substring = true).performClick()
        rule.onNodeWithText("Request ON").assertIsEnabled().performClick()
        assertEquals(true, requested)
    }

    @Test
    fun turnOffIsOneTapWhenThePumpIsOn() {
        var requested: Boolean? = null
        rule.setContent {
            JalTheme(darkTheme = false) {
                PumpControlPanel(
                    ui = snapshot(FirmwarePumpPermission.ENABLED, PumpState.ON),
                    onRequest = { requested = it },
                )
            }
        }
        rule.onNodeWithTag("pump_off_button").assertIsEnabled().performClick()
        assertEquals(false, requested)
    }
}
