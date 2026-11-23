package com.example.hyperos

import androidx.annotation.StringRes
import com.example.R

enum class DebloatRiskTier(@StringRes val labelRes: Int) {
    SAFE(R.string.risk_safe),
    MODERATE(R.string.risk_moderate),
    CAUTION(R.string.risk_caution)
}

data class DebloatEntry(
    val packageName: String,
    @StringRes val titleRes: Int,
    @StringRes val descRes: Int,
    val riskTier: DebloatRiskTier
)

object ChinaDebloatCatalog {

    val HARD_BLOCKLIST: Set<String> = setOf(
        "com.miui.securitycenter",
        "com.lbe.security.miui",
        "com.miui.home",
        "com.android.systemui",
        "com.android.settings",
        "com.xiaomi.xmsf",
        "com.xiaomi.account",
        "com.miui.powerkeeper",
        "com.android.phone",
        "com.android.providers.settings",
        "com.miui.packageinstaller",
        "com.google.android.gms"
    )

    val entries: List<DebloatEntry> = listOf(
        // SAFE: Ads, Telemetry & Background Wakeup Bloat on China ROM
        DebloatEntry(
            packageName = "com.miui.systemAdSolution",
            titleRes = R.string.deb_msa_title,
            descRes = R.string.deb_msa_desc,
            riskTier = DebloatRiskTier.SAFE
        ),
        DebloatEntry(
            packageName = "com.miui.analytics",
            titleRes = R.string.deb_analytics_title,
            descRes = R.string.deb_analytics_desc,
            riskTier = DebloatRiskTier.SAFE
        ),
        DebloatEntry(
            packageName = "com.miui.daemon",
            titleRes = R.string.deb_daemon_title,
            descRes = R.string.deb_daemon_desc,
            riskTier = DebloatRiskTier.SAFE
        ),
        DebloatEntry(
            packageName = "com.xiaomi.ab",
            titleRes = R.string.deb_ab_title,
            descRes = R.string.deb_ab_desc,
            riskTier = DebloatRiskTier.SAFE
        ),
        DebloatEntry(
            packageName = "com.miui.bugreport",
            titleRes = R.string.deb_bugreport_title,
            descRes = R.string.deb_bugreport_desc,
            riskTier = DebloatRiskTier.SAFE
        ),
        DebloatEntry(
            packageName = "com.miui.hybrid",
            titleRes = R.string.deb_hybrid_title,
            descRes = R.string.deb_hybrid_desc,
            riskTier = DebloatRiskTier.SAFE
        ),
        DebloatEntry(
            packageName = "com.miui.hybrid.accessory",
            titleRes = R.string.deb_hybrid_acc_title,
            descRes = R.string.deb_hybrid_acc_desc,
            riskTier = DebloatRiskTier.SAFE
        ),
        DebloatEntry(
            packageName = "com.xiaomi.vipaccount",
            titleRes = R.string.deb_vip_title,
            descRes = R.string.deb_vip_desc,
            riskTier = DebloatRiskTier.SAFE
        ),

        // MODERATE: Preinstalled China ROM Apps & Assistants
        DebloatEntry(
            packageName = "com.xiaomi.shop",
            titleRes = R.string.deb_shop_title,
            descRes = R.string.deb_shop_desc,
            riskTier = DebloatRiskTier.MODERATE
        ),
        DebloatEntry(
            packageName = "com.xiaomi.youpin",
            titleRes = R.string.deb_youpin_title,
            descRes = R.string.deb_youpin_desc,
            riskTier = DebloatRiskTier.MODERATE
        ),
        DebloatEntry(
            packageName = "com.miui.yellowpage",
            titleRes = R.string.deb_yellowpage_title,
            descRes = R.string.deb_yellowpage_desc,
            riskTier = DebloatRiskTier.MODERATE
        ),
        DebloatEntry(
            packageName = "com.miui.newhome",
            titleRes = R.string.deb_newhome_title,
            descRes = R.string.deb_newhome_desc,
            riskTier = DebloatRiskTier.MODERATE
        ),
        DebloatEntry(
            packageName = "com.miui.video",
            titleRes = R.string.deb_video_title,
            descRes = R.string.deb_video_desc,
            riskTier = DebloatRiskTier.MODERATE
        ),
        DebloatEntry(
            packageName = "com.miui.player",
            titleRes = R.string.deb_player_title,
            descRes = R.string.deb_player_desc,
            riskTier = DebloatRiskTier.MODERATE
        ),
        DebloatEntry(
            packageName = "com.xiaomi.gamecenter",
            titleRes = R.string.deb_gamecenter_title,
            descRes = R.string.deb_gamecenter_desc,
            riskTier = DebloatRiskTier.MODERATE
        ),
        DebloatEntry(
            packageName = "com.duokan.reader",
            titleRes = R.string.deb_duokan_title,
            descRes = R.string.deb_duokan_desc,
            riskTier = DebloatRiskTier.MODERATE
        ),
        DebloatEntry(
            packageName = "com.miui.voiceassist",
            titleRes = R.string.deb_voiceassist_title,
            descRes = R.string.deb_voiceassist_desc,
            riskTier = DebloatRiskTier.MODERATE
        ),
        DebloatEntry(
            packageName = "com.xiaomi.mibrain.speech",
            titleRes = R.string.deb_mibrain_title,
            descRes = R.string.deb_mibrain_desc,
            riskTier = DebloatRiskTier.MODERATE
        ),

        // CAUTION: Cloud Game FPS / Thermal Service
        DebloatEntry(
            packageName = "com.xiaomi.joyose",
            titleRes = R.string.deb_joyose_title,
            descRes = R.string.deb_joyose_desc,
            riskTier = DebloatRiskTier.CAUTION
        )
    )
}
