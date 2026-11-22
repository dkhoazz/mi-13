package com.example

import com.example.data.AppLanguage
import com.example.data.AppPreferencesState
import com.example.data.ProfileId
import com.example.data.RefreshRateMode
import com.example.data.defaultProfilesMap
import com.example.shizuku.ShellCommandResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun default_profiles_match_hyperos_specs() {
        val profiles = defaultProfilesMap()
        assertEquals(4, profiles.size)

        val gaming = profiles.getValue(ProfileId.GAMING)
        assertEquals(RefreshRateMode.HZ_120, gaming.refreshRate)
        assertTrue(gaming.speedMode)
        assertEquals(0.5f, gaming.animationScale)
        assertTrue(gaming.phantomGuard)
        assertTrue(gaming.thermalOverride)

        val cool = profiles.getValue(ProfileId.COOL_CHARGING)
        assertEquals(RefreshRateMode.HZ_60, cool.refreshRate)
        assertFalse(cool.speedMode)
        assertFalse(cool.thermalOverride)
    }

    @Test
    fun preferences_defaults_match_battery_protection_targets() {
        val prefs = AppPreferencesState()
        assertEquals(80, prefs.upperChargeTarget)
        assertEquals(20, prefs.lowBatteryAlertLevel)
        assertEquals(AppLanguage.SYSTEM, prefs.appLanguage)
        assertEquals(ProfileId.BALANCED, prefs.activeProfileId)
    }

    @Test
    fun shell_command_result_success_matches_exit_zero() {
        assertTrue(ShellCommandResult(0, "ok").isSuccess)
        assertFalse(ShellCommandResult(1, "err").isSuccess)
        assertFalse(ShellCommandResult(-1, "fail").isSuccess)
    }
}
