package com.dwbuilder.app

import com.dwbuilder.app.domain.Stats
import com.dwbuilder.app.domain.model.Attributes
import com.dwbuilder.app.domain.model.Build
import kotlin.test.Test
import kotlin.test.assertEquals

class StatsTest {

    private val build = Build(
        attributes = Attributes(
            base = mapOf("Strength" to 75, "Fortitude" to 40, "Agility" to 30, "Intelligence" to 60, "Willpower" to 5, "Charisma" to 5),
            weapon = mapOf("Heavy Weapon" to 80, "Medium Weapon" to 0, "Light Weapon" to 0),
            attunement = mapOf("Flamecharm" to 60, "Frostdraw" to 0, "Thundercall" to 0, "Galebreathe" to 0, "Shadowcast" to 0, "Ironsing" to 0, "Bloodrend" to 0),
        ),
    )

    @Test
    fun baseStatsResolveDirectly() {
        assertEquals(75, Stats.statValue(build, "Strength"))
        assertEquals(60, Stats.statValue(build, "Intelligence"))
    }

    @Test
    fun derivedStats() {
        assertEquals(75, Stats.statValue(build, "Body"))          // max(STR, AGL, FTD)
        assertEquals(60, Stats.statValue(build, "Mind"))           // max(INT, WLL, CHA)
        assertEquals(80, Stats.statValue(build, "Weapon"))         // max weapon
        assertEquals(80, Stats.statValue(build, "Weapons"))
        assertEquals(60, Stats.statValue(build, "Attunement"))     // max attunement
        // 215 base + 80 weapon + 60 flame = 355 spent (no 2nd attunement) → power 20 (capped)
        assertEquals(20, Stats.statValue(build, "Power"))
    }

    @Test
    fun unknownStatReturnsZero() {
        assertEquals(0, Stats.statValue(build, "Sanity"))
        assertEquals(0, Stats.statValue(Build(), "Strength"))
    }

    @Test
    fun bonusesOnlyApplyInWeaponMode() {
        val khan = build.copy(race = "Khan")
        assertEquals(0, Stats.bonusToward(khan, "Strength", forWeapons = false))
        assertEquals(3, Stats.bonusToward(khan, "Strength", forWeapons = true))
        assertEquals(0, Stats.bonusToward(khan, "Power", forWeapons = true)) // Power exempt
        val silentheart = build.copy(oath = "Silentheart")
        assertEquals(25, Stats.bonusToward(silentheart, "Heavy Weapon", forWeapons = true))
        assertEquals(25, Stats.bonusToward(silentheart, "Heavy Wep.", forWeapons = true))
        assertEquals(0, Stats.bonusToward(silentheart, "Strength", forWeapons = true))
    }
}
