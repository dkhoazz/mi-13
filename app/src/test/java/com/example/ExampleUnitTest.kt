package com.example

import com.example.data.AppLanguage
import com.example.data.AppPreferencesState
import com.example.data.ProfileId
import com.example.data.RefreshRateMode
import com.example.data.defaultProfilesMap
import com.example.hyperos.ChinaDebloatCatalog
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
        assertTrue(gaming.gameDriverOptIn)

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

    @Test
    fun china_debloat_blocklist_protects_core_hyperos_and_has_no_catalog_overlap() {
        val requiredCorePackages = listOf(
            "com.miui.securitycenter",
            "com.lbe.security.miui",
            "com.miui.home",
            "com.xiaomi.xmsf",
            "com.xiaomi.account",
            "com.miui.powerkeeper",
            "com.android.systemui",
            "com.android.settings"
        )
        requiredCorePackages.forEach { corePkg ->
            assertTrue(
                "Core package $corePkg must remain in HARD_BLOCKLIST",
                ChinaDebloatCatalog.HARD_BLOCKLIST.contains(corePkg)
            )
        }

        val catalogPackages = ChinaDebloatCatalog.entries.map { it.packageName }.toSet()
        assertEquals("Catalog entries must have unique package names", ChinaDebloatCatalog.entries.size, catalogPackages.size)

        val overlap = catalogPackages.intersect(ChinaDebloatCatalog.HARD_BLOCKLIST)
        assertTrue("Catalog must have zero overlap with HARD_BLOCKLIST, found: $overlap", overlap.isEmpty())

        val validPackageRegex = Regex("^[a-zA-Z0-9._]+$")
        catalogPackages.forEach { pkg ->
            assertTrue("Invalid package name format: $pkg", validPackageRegex.matches(pkg))
        }
    }
}
