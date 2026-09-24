package com.dwbuilder.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Parses the full bundled dataset and verifies collection sizes + spot checks. */
class GameDataTest {

    private val data get() = TestData.data

    @Test
    fun allCollectionsParse() {
        // first-wins name index (source data has shrine/purchase variants sharing names)
        assertEquals(1059, data.talents.size)
        assertEquals(238, data.mantras.size)
        assertEquals(271, data.weapons.size)
        assertEquals(105, data.outfits.size)
        assertEquals(433, data.equipment.size)
        assertEquals(16, data.aspects.size)
        assertEquals(18, data.oaths.size)
        assertEquals(9, data.boons.size)
        assertEquals(11, data.flaws.size)
        assertEquals(55, data.enchants.size)
        assertEquals(35, data.mods.size)
        assertEquals(154, data.enemies.size)
    }

    @Test
    fun modCatalogSpotChecks() {
        val grim = assertNotNull(data.mod("Grim"))
        assertEquals("Normal", grim.bucket)
        assertEquals("Grim", grim.requiresEnchant)
        assertEquals(0.25, grim.effectDmg, 1e-9)

        val sear = assertNotNull(data.mod("Sear (full stacks)"))
        assertEquals("Unique", sear.bucket)
        assertEquals(0.4, sear.effectPen, 1e-9)

        val ardour = assertNotNull(data.mod("Ardour Weapon"))
        assertEquals(listOf("Ardour"), ardour.requiresMetaMurmur)
        assertEquals(listOf("Soulbreaker"), ardour.requiresMetaOath)

        // no entry in the catalog restricts by weapon type
        assertTrue(data.mods.none { it.weapons.isNotEmpty() })
    }

    @Test
    fun everyRecordIsKeptInLists() {
        assertEquals(1155, data.allTalents.size)
        assertEquals(267, data.allMantras.size)
        // banner: duplicates exist but resolve to the first record
        val names = data.allMantras.map { it.name }
        assertTrue(names.size > names.toSet().size, "expected mantra duplicates in source data")
    }

    @Test
    fun talentSpotChecks() {
        val t = assertNotNull(data.talent("Against All Odds"))
        assertEquals("Absolute Focus", t.category)
        assertEquals(mapOf("Willpower" to 65), t.requirements?.stats)
        assertTrue(t.roll2able)
        assertTrue(t.countTowardsTalentTotal)

        val oath = assertNotNull(data.talent("Oath: Arcwarder"))
        assertEquals("Oath", oath.rarity)
        assertEquals("Arcwarder", oath.category)
        assertEquals(
            mapOf("Fortitude" to 20, "Flamecharm" to 20, "Thundercall" to 20),
            oath.requirements?.stats,
        )
        assertEquals(listOf("Alpha"), oath.requirements?.quests)
    }

    @Test
    fun mantraSpotCheck() {
        val m = assertNotNull(data.mantra("Burning Servants"))
        assertEquals(listOf("Flamecharm"), m.attributes)
        assertEquals("Combat", m.category)
        assertEquals(0, m.stars)
        val main = m.damage.first()
        assertEquals(44.0, main.levels.first().damage)
        assertEquals(mapOf("Flamecharm" to 2.75), m.scaling)
    }

    @Test
    fun weaponSpotCheck() {
        val w = assertNotNull(data.weapon("Enforcer's Blade"))
        assertEquals("Greatsword", w.type)
        assertEquals(25.5, w.damage)
        assertEquals(0.05, w.penetration)
        assertEquals(mapOf("Heavy Weapon" to 10.0), w.scaling)
        assertTrue(w.enchantable)
    }

    @Test
    fun equipmentSlotsAreComplete() {
        val slots = data.equipment.values.filter { it.equippable }.map { it.type }.toSet()
        assertTrue(
            slots.containsAll(
                listOf("Head", "Arms", "Legs", "Torso", "Face", "Earrings", "Rings"),
            ),
            "missing slots: $slots",
        )
    }

    @Test
    fun jokeItemsAreNotEquippable() {
        val tiedTie = assertNotNull(data.equipmentItem("Tied Tie"))
        assertTrue(!tiedTie.equippable)
    }

    @Test
    fun identityCollections() {
        assertNotNull(data.aspect("Etrean")).statBonuses.isNotEmpty()
        assertNotNull(data.aspect("None"))
        val blindseer = assertNotNull(data.oath("Blindseer"))
        assertEquals(mapOf("Combat" to 1, "Support" to 1, "Wildcard" to 1), blindseer.slots)
        assertEquals(listOf("Sightless Beam"), blindseer.mantras["Combat"])
        assertNotNull(data.boons["Autodidact"])
        assertNotNull(data.flaws["Blind"])
    }

    @Test
    fun enemySpotCheck() {
        val doom = data.enemies.first { it.name == "Doom of Caeranthil" }
        assertEquals(60000.0, doom.health)
        assertEquals("World Boss", doom.className)
    }
}
