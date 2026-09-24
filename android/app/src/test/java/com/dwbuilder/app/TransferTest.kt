package com.dwbuilder.app

import com.dwbuilder.app.domain.Transfer
import com.dwbuilder.app.domain.model.Attributes
import com.dwbuilder.app.domain.model.Build
import kotlin.test.Test
import kotlin.test.assertEquals

/** Round-trip and site-parity checks for the in-game transfer format. */
class TransferTest {

    private fun baseBuild() = Build(
        name = "Testchar",
        level = 20,
        race = "Canor",
        origin = "Castaway",
        oath = "Oathless",
        attributes = Attributes(
            base = mapOf(
                "Strength" to 10, "Fortitude" to 4, "Agility" to 0,
                "Intelligence" to 0, "Willpower" to 0, "Charisma" to 0,
            ),
            weapon = mapOf("Heavy Weapon" to 0, "Medium Weapon" to 1, "Light Weapon" to 0),
            attunement = mapOf(
                "Flamecharm" to 10, "Frostdraw" to 0, "Thundercall" to 0,
                "Galebreathe" to 2, "Shadowcast" to 0, "Ironsing" to 6, "Bloodrend" to 1,
            ),
        ),
        talents = listOf("Sightless", "Exoskeleton"),
        mantras = listOf("Flame Waltz"),
    )

    @Test
    fun roundTripPreservesEverything() {
        val parsed = Transfer.parse(Transfer.export(baseBuild()))
        assertEquals("Testchar", parsed.name)
        assertEquals(20, parsed.level)
        assertEquals("Canor", parsed.race)
        assertEquals("Castaway", parsed.origin)
        assertEquals("Oathless", parsed.oath)
        assertEquals(10, parsed.base["Strength"])
        assertEquals(4, parsed.base["Fortitude"])
        assertEquals(1, parsed.weapon["Medium Weapon"])
        assertEquals(10, parsed.attunement["Flamecharm"])
        assertEquals(6, parsed.attunement["Ironsing"])
        assertEquals(1, parsed.attunement["Bloodrend"])
        // site `an` quirk: mantra names leak into the talents slice; the app
        // filters them out at apply time by catalog lookup
        assertEquals(listOf("Sightless", "Exoskeleton", "Flame Waltz"), parsed.talents)
        assertEquals(listOf("Flame Waltz"), parsed.mantras)
    }

    @Test
    fun parsesInGameStyleText() {
        val text = """
            Deepwoken Character
            LVL 20 Ganymede Castaway Oathless

            5 STR; 5 FTD; 0 AGL; 0 INT; 0 WLL; 0 CHA

            1 HVY; 0 MED; 0 LHT

            0 FIR; 0 ICE; 0 LTN; 2 WND; 0 SDW; 0 MTL; 0 BLD

            == TALENTS ==
            Sightless

            == MANTRAS ==
            Flame Waltz
        """.trimIndent()
        val p = Transfer.parse(text)
        assertEquals("Deepwoken Character", p.name)
        assertEquals(5, p.base["Strength"])
        assertEquals(5, p.base["Fortitude"])
        assertEquals(1, p.weapon["Heavy Weapon"])
        assertEquals(2, p.attunement["Galebreathe"])
        assertEquals(listOf("Sightless", "Flame Waltz"), p.talents)
        assertEquals(listOf("Flame Waltz"), p.mantras)
    }

    @Test
    fun sectionSlicingMirrorsSiteQuirk() {
        // `an` takes everything after == TALENTS == minus section markers, so
        // mantra names leak into the talents list; the app filters them at apply
        // time via catalog lookup. `hs` slices only up to the next marker.
        val text = """
            Name
            LVL 20 A B C
            == TALENTS ==
            Sightless
            == MANTRAS ==
            Flame Waltz
        """.trimIndent()
        val p = Transfer.parse(text)
        assertEquals(listOf("Sightless", "Flame Waltz"), p.talents)
        assertEquals(listOf("Flame Waltz"), p.mantras)
    }

    @Test
    fun parsesUpperAndLowercaseAbbreviations() {
        val p = Transfer.parse(
            """
            Cap
            LVL 20 A B C
            20 str; 30 ftd; 40 agl; 50 int; 60 wll; 70 cha
            """.trimIndent(),
        )
        assertEquals(20, p.base["Strength"])
        assertEquals(70, p.base["Charisma"])
    }

    @Test
    fun defaultsWhenHeaderMissing() {
        val p = Transfer.parse("just a name")
        assertEquals("just a name", p.name)
        assertEquals(0, p.level)
        assertEquals("None", p.race)
        assertEquals("Castaway", p.origin)
        assertEquals("None", p.oath)
    }
}