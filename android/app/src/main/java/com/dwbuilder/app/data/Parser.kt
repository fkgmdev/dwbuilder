package com.dwbuilder.app.data

import com.dwbuilder.app.data.arr
import com.dwbuilder.app.data.bool
import com.dwbuilder.app.data.boolOr
import com.dwbuilder.app.data.double
import com.dwbuilder.app.data.doubleOr
import com.dwbuilder.app.data.int
import com.dwbuilder.app.data.intMap
import com.dwbuilder.app.data.intOr
import com.dwbuilder.app.data.numMap
import com.dwbuilder.app.data.obj
import com.dwbuilder.app.data.str
import com.dwbuilder.app.data.strList
import com.dwbuilder.app.data.strOr
import com.dwbuilder.app.domain.model.AddStat
import com.dwbuilder.app.domain.model.Aspect
import com.dwbuilder.app.domain.model.Boon
import com.dwbuilder.app.domain.model.Enchant
import com.dwbuilder.app.domain.model.Enemy
import com.dwbuilder.app.domain.model.Equipment
import com.dwbuilder.app.domain.model.Flaw
import com.dwbuilder.app.domain.model.InnatePip
import com.dwbuilder.app.domain.model.InnateStat
import com.dwbuilder.app.domain.model.Mantra
import com.dwbuilder.app.domain.model.MantraDamageVariant
import com.dwbuilder.app.domain.model.MantraLevel
import com.dwbuilder.app.domain.model.Modifier
import com.dwbuilder.app.domain.model.Oath
import com.dwbuilder.app.domain.model.Outfit
import com.dwbuilder.app.domain.model.Talent
import com.dwbuilder.app.domain.model.TalentRequirements
import com.dwbuilder.app.domain.model.Weapon
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** Parses the bundled data JSON collections (assets/data) into domain models. */
object Parser {

    // ---------- requirements ----------

    fun parseRequirements(o: JsonObject?): TalentRequirements? {
        if (o == null) return null
        return TalentRequirements(
            stats = o.intMap("stats"),
            talents = o.strList("talents"),
            mantras = o.strList("mantras"),
            equipment = o.str("equipment"),
            outfit = o.str("outfit"),
            weapon = o.str("weapon"),
            set = o.str("set"),
            weaponType = o.str("weaponType"),
            origin = o.str("origin"),
            murmur = o.str("murmur"),
            aspect = o.str("aspect"),
            memento = o.str("memento"),
            objectives = o.strList("objectives"),
            quests = o.strList("quests"),
            slay = o.str("slay") ?: (o.arr("slay").firstOrNull() as? JsonPrimitive)?.contentOrNull,
            add = o.arr("add").mapNotNull { el ->
                val a = el as? JsonObject ?: return@mapNotNull null
                AddStat(stats = a.strList("stats"), value = a.intOr("value", 0))
            },
            resonance = o.strList("resonance"),
            unobtainable = if ("unobtainable" in o) o.bool("unobtainable") else null,
            or = o.arr("or").mapNotNull { parseRequirements(it as? JsonObject) },
        )
    }

    // ---------- talents ----------

    fun parseTalent(o: JsonObject): Talent = Talent(
        name = o.strOr("name", ""),
        category = o.str("category"),
        rarity = o.strOr("rarity", "Common"),
        description = o.strOr("description", ""),
        requirements = parseRequirements(o.obj("requirements")),
        mutualExclusives = o.strList("mutualExclusives"),
        additionalInfo = o.strOr("additionalInfo", ""),
        stats = o.numMap("stats"),
        roll2able = o.boolOr("roll2able", false),
        countTowardsTalentTotal = o.boolOr("countTowardsTalentTotal", true),
        vaulted = o.boolOr("vaulted", false),
    )

    // ---------- mantras ----------

    fun parseMantra(o: JsonObject): Mantra = Mantra(
        name = o.strOr("name", ""),
        vaulted = o.boolOr("vaulted", false),
        attributes = o.strList("attributes"),
        stars = o.intOr("stars", 0),
        category = o.strOr("category", "Combat"),
        type = o.strOr("type", "Normal"),
        description = o.strOr("description", ""),
        damage = o.arr("damage").mapNotNull { el ->
            val d = el as? JsonObject ?: return@mapNotNull null
            MantraDamageVariant(
                variant = d.str("variant"),
                levels = d.arr("levels").mapNotNull { l ->
                    val level = l as? JsonObject ?: return@mapNotNull null
                    MantraLevel(
                        level = level.strOr("level", ""),
                        damage = level.double("damage"),
                        postureDamage = level.double("postureDamage"),
                    )
                },
            )
        },
        scaling = o.numMap("scaling"),
        sharedCooldowns = o.strList("sharedCooldowns"),
        requirements = parseRequirements(o.obj("requirements")),
        miscellaneous = o.str("miscellaneous"),
        relatedTalents = o.strList("relatedTalents"),
        sparks = o.strList("sparks"),
        modifiers = o.strList("modifiers"),
    )

    // ---------- weapons ----------

    fun parseWeapon(o: JsonObject): Weapon = Weapon(
        name = o.strOr("name", ""),
        type = o.strOr("type", ""),
        rarity = o.strOr("rarity", "Common"),
        description = o.strOr("description", ""),
        damageTypes = o.strList("damageTypes"),
        damage = o.doubleOr("damage", 0.0),
        bleedDamage = o.double("bleedDamage"),
        chipDamage = o.double("chipDamage"),
        penetration = o.doubleOr("penetration", 0.0),
        scaling = o.numMap("scaling"),
        postureDamage = o.doubleOr("postureDamage", 0.0),
        postureMax = o.double("postureMax"),
        postureRestoration = o.double("postureRestoration"),
        range = o.doubleOr("range", 0.0),
        rangeType = o.str("rangeType"),
        swingSpeed = o.doubleOr("swingSpeed", 0.0),
        attackDuration = o.str("attackDuration"),
        endlag = o.str("endlag"),
        enchantable = o.boolOr("enchantable", true),
        grantedTalents = o.strList("grantedTalents"),
        requirements = parseRequirements(o.obj("requirements")),
    )

    // ---------- equipment ----------

    fun parseEquipment(o: JsonObject): Equipment = Equipment(
        name = o.strOr("name", ""),
        equippable = o.boolOr("equippable", true),
        type = o.strOr("type", ""),
        rarity = o.strOr("rarity", "Common"),
        set = o.str("set"),
        variants = o.strList("variants"),
        description = o.strOr("description", ""),
        innateTalents = o.strList("innateTalents"),
        innateStats = o.arr("innateStats").mapNotNull { el ->
            val s = el as? JsonObject ?: return@mapNotNull null
            InnateStat(
                stat = s.strOr("stat", ""),
                value = s.doubleOr("value", 0.0),
                percentage = s.boolOr("percentage", false),
            )
        },
        innatePips = o.arr("innatePips").mapNotNull { el ->
            val p = el as? JsonObject ?: return@mapNotNull null
            InnatePip(rarity = p.strOr("rarity", "Common"), count = p.intOr("count", 0))
        },
        requirements = parseRequirements(o.obj("requirements")),
    )

    // ---------- outfits ----------

    fun parseOutfit(o: JsonObject): Outfit = Outfit(
        name = o.strOr("name", ""),
        tier = o.strOr("tier", ""),
        description = o.strOr("description", ""),
        durability = o.int("durability"),
        physicalResistance = o.double("physicalResistance"),
        slashResistance = o.double("slashResistance"),
        bluntResistance = o.double("bluntResistance"),
        elementalResistance = o.double("elementalResistance"),
        flameResistance = o.double("flameResistance"),
        iceResistance = o.double("iceResistance"),
        thunderResistance = o.double("thunderResistance"),
        windResistance = o.double("windResistance"),
        shadowResistance = o.double("shadowResistance"),
        metalResistance = o.double("metalResistance"),
        bloodResistance = o.double("bloodResistance"),
        additionalStealth = o.double("additionalStealth"),
        etherRegeneration = o.double("etherRegeneration"),
        grantedTalents = o.strList("grantedTalents"),
        requirements = parseRequirements(o.obj("requirements")),
    )

    // ---------- identity ----------

    fun parseAspect(o: JsonObject): Aspect = Aspect(
        name = o.strOr("name", ""),
        statBonuses = o.intMap("statBonuses"),
    )

    fun parseOath(o: JsonObject): Oath = Oath(
        name = o.strOr("name", ""),
        slots = o.intMap("slots"),
        mantras = o.obj("mantras")?.mapValues { (_, v) ->
            (v as? JsonArray)?.mapNotNull { slot -> (slot as? JsonPrimitive)?.contentOrNull } ?: emptyList()
        } ?: emptyMap(),
    )

    fun parseBoon(o: JsonObject): Boon = Boon(name = o.strOr("name", ""), description = o.strOr("description", ""))
    fun parseFlaw(o: JsonObject): Flaw = Flaw(name = o.strOr("name", ""), description = o.strOr("description", ""))
    fun parseEnchant(o: JsonObject): Enchant = Enchant(
        name = o.strOr("name", ""),
        type = o.strOr("type", ""),
        description = o.strOr("description", ""),
        effects = o.strOr("effects", ""),
    )

    // ---------- enemies ----------

    fun parseEnemy(o: JsonObject): Enemy = Enemy(
        name = o.strOr("name", ""),
        className = o.strOr("class", ""),
        aliases = o.strList("aliases"),
        health = o.doubleOr("health", 0.0),
        healthAdditives = o.strOr("healthAdditives", ""),
        description = o.strOr("description", ""),
        locations = o.strList("locations"),
        attacks = o.strList("attacks"),
    )

    // ---------- damage modifiers (site constant G) ----------

    fun parseModifier(o: JsonObject): Modifier {
        val effect = o.obj("effect")
        val meta = o.obj("requiresMeta")
        return Modifier(
            name = o.strOr("name", ""),
            bucket = o.strOr("bucket", "Normal"),
            requires = o.strList("requires"),
            requiresEnchant = o.str("requiresEnchant"),
            alwaysAvailable = o.boolOr("alwaysAvailable", false),
            requiresMetaMurmur = meta?.let { (it["murmur"] as? JsonArray)?.mapNotNull { e -> (e as? JsonPrimitive)?.contentOrNull } } ?: emptyList(),
            requiresMetaOath = meta?.let { (it["oath"] as? JsonArray)?.mapNotNull { e -> (e as? JsonPrimitive)?.contentOrNull } } ?: emptyList(),
            effectDmg = effect?.doubleOr("dmg", 0.0) ?: 0.0,
            effectPen = effect?.doubleOr("pen", 0.0) ?: 0.0,
            effectBleed = effect?.doubleOr("bleed", 0.0) ?: 0.0,
            multiplicative = effect?.boolOr("multiplicative", false) ?: false,
            weapons = o.strList("weaponTypes"),
            notes = o.strOr("notes", ""),
        )
    }
}