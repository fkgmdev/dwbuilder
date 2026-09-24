package com.dwbuilder.app

import com.dwbuilder.app.domain.BuildContext
import com.dwbuilder.app.domain.Requirements
import com.dwbuilder.app.domain.model.Attributes
import com.dwbuilder.app.domain.model.Build
import com.dwbuilder.app.domain.model.TalentRequirements
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RequirementsTest {

    private fun ctx(
        build: Build = Build(),
        granted: Set<String> = emptySet(),
    ): BuildContext = BuildContext(
        build = build,
        talents = TestData.data.talents,
        weapons = TestData.data.weapons,
        grantedTalents = granted,
    )

    @Test
    fun nullRequirementsAlwaysMet() {
        assertTrue(Requirements.check(null, ctx()).hardMet)
    }

    @Test
    fun statRequirementFailsBelowThresholdWithExactWarning() {
        val req = TalentRequirements(stats = mapOf("Flamecharm" to 60))
        val low = Requirements.check(req, ctx(Build(attributes = Attributes.empty())))
        assertFalse(low.hardMet)
        assertEquals(
            listOf("Flamecharm: requires 60, currently 0 (need 60 more)"),
            low.warnings,
        )
        val high = Requirements.check(
            req,
            ctx(Build(attributes = Attributes.empty().copy(
                attunement = mapOf("Flamecharm" to 60),
            ))),
        )
        assertTrue(high.hardMet)
        assertTrue(high.warnings.isEmpty())
    }

    @Test
    fun powerStatUsesComputedPower() {
        // "Power" requirement resolves against computed power, not stored points
        val req = TalentRequirements(stats = mapOf("Power" to 1))
        val b = Build(attributes = Attributes(mapOf("Strength" to 15), emptyMap(), emptyMap()))
        assertFalse(Requirements.check(req, ctx(b)).hardMet) // 15 points → power 0
        val b2 = Build(attributes = Attributes(mapOf("Strength" to 30), emptyMap(), emptyMap()))
        assertTrue(Requirements.check(req, ctx(b2)).hardMet) // 30 points → power 1
    }

    @Test
    fun orGroupPicksAnyAlternative() {
        val req = TalentRequirements(or = listOf(
            TalentRequirements(stats = mapOf("Willpower" to 40)),
            TalentRequirements(stats = mapOf("Charisma" to 40)),
        ))
        val will = Build(attributes = Attributes(mapOf("Willpower" to 40), emptyMap(), emptyMap()))
        assertTrue(Requirements.check(req, ctx(will)).hardMet)
        val cha = Build(attributes = Attributes(mapOf("Charisma" to 40), emptyMap(), emptyMap()))
        assertTrue(Requirements.check(req, ctx(cha)).hardMet)
        val neither = Build(attributes = Attributes(mapOf("Agility" to 10), emptyMap(), emptyMap()))
        val r = Requirements.check(req, ctx(neither))
        assertFalse(r.hardMet)
        assertTrue(r.warnings.single().startsWith("Requires one alternative:"))
    }

    @Test
    fun prerequisiteTalentMustBeTaken() {
        val spellShout = TestData.data.talent("Spell Shout") ?: error("fixture missing Spell Shout")
        assertEquals(null, spellShout.requirements) // prerequisite with no own requirements

        // not taken → soft warning, still hardMet
        val r = Requirements.check(
            TalentRequirements(talents = listOf("Spell Shout")),
            ctx(Build()),
        )
        assertTrue(r.hardMet)
        assertEquals(listOf("Requires talent: Spell Shout"), r.warnings)

        // taken → met with no warnings
        val taken = Build(talents = listOf("Spell Shout"))
        assertTrue(Requirements.check(TalentRequirements(talents = listOf("Spell Shout")), ctx(taken)).hardMet)
        assertTrue(Requirements.check(TalentRequirements(talents = listOf("Spell Shout")), ctx(taken)).warnings.isEmpty())
    }

    @Test
    fun unmetPrerequisiteTalentRequirementsHardFail() {
        // a not-yet-taken talent whose own requirements are unmet hard-fails with nested warnings
        val r = Requirements.check(
            TalentRequirements(talents = listOf("Adept Flamecharmer")),
            ctx(Build()),
        )
        assertFalse(r.hardMet)
        assertEquals(
            listOf("Adept Flamecharmer: Flamecharm: requires 20, currently 0 (need 20 more)"),
            r.warnings,
        )
    }

    @Test
    fun grantedTalentSatisfiesPrerequisite() {
        val r = Requirements.check(
            TalentRequirements(talents = listOf("Adept Flamecharmer")),
            ctx(Build(), granted = setOf("Adept Flamecharmer")),
        )
        assertTrue(r.hardMet)
    }

    @Test
    fun originMurmurAndRaceMetaChecks() {
        val req = TalentRequirements(origin = "Justicar", murmur = "Tacet", aspect = "Etrean")
        val r = Requirements.check(req, ctx(Build()))
        // meta mismatches are soft warnings per nr() — hardMet stays true (gated by Kw separately)
        assertTrue(r.hardMet)
        assertEquals(
            listOf(
                "Requires Origin: Justicar",
                "Requires Murmur: Tacet",
                "Requires Race: Etrean",
            ),
            r.warnings,
        )
        val ok = Requirements.check(
            req,
            ctx(Build(origin = "Justicar", murmur = "Tacet", race = "Etrean")),
        )
        assertTrue(ok.hardMet)
        assertTrue(ok.warnings.isEmpty())
    }

    @Test
    fun weaponTypeMatchesEquippedWeaponFamily() {
        val sword = TestData.data.weapons.values.first { it.type == "Greatsword" }
        val b = Build(equipment = mapOf("Weapon" to sword.name), weapon = sword.name)
        assertTrue(Requirements.check(TalentRequirements(weaponType = "Greatsword"), ctx(b)).hardMet)
        assertTrue(Requirements.check(TalentRequirements(weaponType = "Heavy Weapon"), ctx(b)).hardMet)
        val r = Requirements.check(TalentRequirements(weaponType = "Bow"), ctx(b))
        assertTrue(r.hardMet) // weaponType mismatches are soft warnings per nr()
        assertEquals(listOf("Requires Weapon Type: Bow"), r.warnings)
    }

    @Test
    fun unarmedBuildFailsWeaponTypeCheck() {
        val r = Requirements.check(TalentRequirements(weaponType = "Greatsword"), ctx(Build()))
        assertTrue(r.hardMet)
        assertEquals(listOf("Requires Weapon Type: Greatsword"), r.warnings)
    }
}
