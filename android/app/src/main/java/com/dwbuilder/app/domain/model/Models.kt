package com.dwbuilder.app.domain.model

/**
 * Parsed game-data records. All values mirror the data files in
 * assets/data (themselves captured from deepwoken.co's SSR wiki payloads).
 */

/** A talent (school perk). */
data class Talent(
    val name: String,
    val category: String?,                     // school / archetype ("Bulwark", ...)
    val rarity: String,                        // Common/Rare/Advanced/Faction/Oath/Quest/Equipment/Innate/Origin/Murmur
    val description: String,
    val requirements: TalentRequirements?,
    val mutualExclusives: List<String>,
    val additionalInfo: String,
    /** Extra stats granted by taking the talent ("Sanity": 20, "Carry Load": 5, ...). */
    val stats: Map<String, Double>,
    val roll2able: Boolean,
    val countTowardsTalentTotal: Boolean,
    val vaulted: Boolean,
)

/**
 * A requirement tree. Optional fields are null/empty when absent.
 * `or` holds alternative requirement groups (any one group must be met).
 */
data class TalentRequirements(
    val stats: Map<String, Int> = emptyMap(),      // stat display name -> points needed
    val talents: List<String> = emptyList(),       // prerequisite talent names ("Oath: Blindseer", ...)
    val mantras: List<String> = emptyList(),       // prerequisite mantra names
    val equipment: String? = null,                 // prerequisite equipment item name
    val outfit: String? = null,
    val weapon: String? = null,
    val set: String? = null,
    val weaponType: String? = null,
    val origin: String? = null,
    val murmur: String? = null,
    val aspect: String? = null,                    // race name
    val memento: String? = null,
    val objectives: List<String> = emptyList(),
    val quests: List<String> = emptyList(),
    val slay: String? = null,
    val add: List<AddStat> = emptyList(),          // "X extra points across these stats"
    val resonance: List<String> = emptyList(),
    val unobtainable: Boolean? = null,
    val or: List<TalentRequirements> = emptyList(),
)

/** e.g. `{"stats": ["Light Weapon","Medium Weapon","Heavy Weapon"], "value": 90}` */
data class AddStat(
    val stats: List<String>,
    val value: Int,
)

/** A mantra (spell) record. */
data class Mantra(
    val name: String,
    val vaulted: Boolean,
    val attributes: List<String>,                  // attunement requirements ("Flamecharm", ...)
    val stars: Int,                                // 0-5 upgrade stars
    val category: String,                          // Combat / Mobility / Support / Wisp
    val type: String,                              // Normal / Feat / ...
    val description: String,
    val damage: List<MantraDamageVariant>,
    val scaling: Map<String, Double>,              // attunement -> scaling factor
    val sharedCooldowns: List<String>,
    val requirements: TalentRequirements?,
    val miscellaneous: String?,
    val relatedTalents: List<String>,
    val sparks: List<String>,
    val modifiers: List<String>,
)

/** One damage variant of a mantra (main cast, charged cast, ...). */
data class MantraDamageVariant(
    val variant: String?,
    val levels: List<MantraLevel>,
)

/** Damage/posture per upgrade level (L1..L5, GB). */
data class MantraLevel(
    val level: String,
    val damage: Double?,
    val postureDamage: Double?,
)

/** A weapon record. */
data class Weapon(
    val name: String,
    val type: String,                              // Greatsword / Bow / ...
    val rarity: String,
    val description: String,
    val damageTypes: List<String>,
    val damage: Double,
    val bleedDamage: Double?,
    val chipDamage: Double?,
    val penetration: Double,
    val scaling: Map<String, Double>,              // weapon skill -> scaling coefficient
    val postureDamage: Double,
    val postureMax: Double?,
    val postureRestoration: Double?,
    val range: Double,
    val rangeType: String?,
    val swingSpeed: Double,
    val attackDuration: String?,
    val endlag: String?,
    val enchantable: Boolean,
    val grantedTalents: List<String>,
    val requirements: TalentRequirements?,
)

/** Equipment item (armor/accessory) for the 7 slots. */
data class Equipment(
    val name: String,
    val equippable: Boolean,
    val type: String,                              // Head/Arms/Legs/Torso/Face/Earrings/Rings
    val rarity: String,
    val set: String?,
    val variants: List<String>,
    val description: String,
    val innateTalents: List<String>,
    val innateStats: List<InnateStat>,
    val innatePips: List<InnatePip>,
    val requirements: TalentRequirements?,
)

/** e.g. `{"stat":"Health","value":5,"percentage":false}` */
data class InnateStat(
    val stat: String,
    val value: Double,
    val percentage: Boolean,
)

/** e.g. `{"rarity":"Uncommon","count":1}` */
data class InnatePip(
    val rarity: String,
    val count: Int,
)

/** Outfit (full-body armor) record with per-type resistances. */
data class Outfit(
    val name: String,
    val tier: String,
    val description: String,
    val durability: Int?,
    val physicalResistance: Double?,
    val slashResistance: Double?,
    val bluntResistance: Double?,
    val elementalResistance: Double?,
    val flameResistance: Double?,
    val iceResistance: Double?,
    val thunderResistance: Double?,
    val windResistance: Double?,
    val shadowResistance: Double?,
    val metalResistance: Double?,
    val bloodResistance: Double?,
    val additionalStealth: Double?,
    val etherRegeneration: Double?,
    val grantedTalents: List<String>,
    val requirements: TalentRequirements?,
)

/** Race (aspect) with its stat bonuses. */
data class Aspect(
    val name: String,
    val statBonuses: Map<String, Int>,
)

/** Oath with mantra/utility slots granted. */
data class Oath(
    val name: String,
    val slots: Map<String, Int>,                   // Combat/Mobility/Support/Wildcard
    val mantras: Map<String, List<String>>,
)

/** Boon / Flaw. */
data class Boon(val name: String, val description: String)
data class Flaw(val name: String, val description: String)

/** Weapon enchantment. */
data class Enchant(
    val name: String,
    val type: String,                              // Weapon / ...
    val description: String,
    val effects: String,
)

/** Enemy used by the PvE calculator. */
data class Enemy(
    val name: String,
    val className: String,                         // "World Boss", "Knight", ...
    val aliases: List<String>,
    val health: Double,
    val healthAdditives: String,                   // e.g. "1,000 (Knight)"
    val description: String,
    val locations: List<String>,
    val attacks: List<String>,
)

/** A damage modifier toggle (site constant `G` from CkKCXiBo.js). */
data class Modifier(
    val name: String,
    val bucket: String,                            // "Normal" | "Unique"
    val requires: List<String>,                    // talents; "Mantra: X" entries need the mantra
    val requiresEnchant: String? = null,           // only applies while the weapon carries this enchant
    val alwaysAvailable: Boolean = false,          // ye: alway|Never — name kept, port of `Ye`
    val requiresMetaMurmur: List<String> = emptyList(),
    val requiresMetaOath: List<String> = emptyList(),
    val effectDmg: Double = 0.0,                   // added to the damage multiplier (+0.25 = +25%)
    val effectPen: Double = 0.0,                   // added to penetration (additive or multiplicative)
    val effectBleed: Double = 0.0,                 // added to bleed rate
    val multiplicative: Boolean = false,           // pen applies multiplicatively (rare)
    val weapons: List<String> = emptyList(),       // weaponTypes restrict (none in the catalog)
    val notes: String = "",
)