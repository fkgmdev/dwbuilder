package com.dwbuilder.app.data

import com.dwbuilder.app.data.parseJson
import com.dwbuilder.app.domain.model.Aspect
import com.dwbuilder.app.domain.model.Boon
import com.dwbuilder.app.domain.model.Enchant
import com.dwbuilder.app.domain.model.Enemy
import com.dwbuilder.app.domain.model.Equipment
import com.dwbuilder.app.domain.model.Flaw
import com.dwbuilder.app.domain.model.Mantra
import com.dwbuilder.app.domain.model.Modifier
import com.dwbuilder.app.domain.model.Oath
import com.dwbuilder.app.domain.model.Outfit
import com.dwbuilder.app.domain.model.Talent
import com.dwbuilder.app.domain.model.Weapon
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/**
 * The full parsed game dataset, indexed by name. Loaded once at startup from
 * bundled assets (or test resources); immutable afterwards.
 */
class GameData(
    /** First-wins name index (site `getTalentByName`/`getMantraByName`). */
    val talents: Map<String, Talent>,
    /** Every talent record, duplicates included (shrine/purchase variants share a name). */
    val allTalents: List<Talent>,
    val mantras: Map<String, Mantra>,
    /** Every mantra record, duplicates included (stat vs shrine-purchase variants). */
    val allMantras: List<Mantra>,
    val weapons: Map<String, Weapon>,
    val outfits: Map<String, Outfit>,
    val equipment: Map<String, Equipment>,
    val aspects: Map<String, Aspect>,
    val oaths: Map<String, Oath>,
    val boons: Map<String, Boon>,
    val flaws: Map<String, Flaw>,
    val enchants: Map<String, Enchant>,
    val mods: List<Modifier>,
    val enemies: List<Enemy>,
) {
    fun talent(name: String): Talent? = talents[name]
    fun mantra(name: String): Mantra? = mantras[name]
    fun weapon(name: String): Weapon? = weapons[name]
    fun outfit(name: String): Outfit? = outfits[name]
    fun equipmentItem(name: String): Equipment? = equipment[name]
    fun aspect(name: String): Aspect? = aspects[name]
    fun oath(name: String): Oath? = oaths[name]
    fun mod(name: String): Modifier? = mods.find { it.name == name }

    /** Equippable items per slot. */
    fun equipmentBySlot(slot: String): List<Equipment> =
        equipment.values.filter { it.equippable && it.type == slot }.sortedBy { it.name }

    /** Races (aspects), ordered as in-game; "None" first. */
    val raceNames: List<String>
        get() = aspects.values.sortedBy { if (it.name == "None") 0 else 1 }.map { it.name }

    companion object {
        /**
         * Loads every collection from a source of file contents keyed by filename
         * (e.g. "talents.json"). `source` throws on failure.
         */
        fun load(source: (String) -> String): GameData {
            fun items(file: String): List<JsonObject> {
                val root = parseJson(source("$file.json"))
                val list = root as? JsonArray ?: error("$file.json: expected a JSON array at top level")
                return list.mapNotNull { it as? JsonObject }
            }

            fun <T> index(file: String, parseOne: (JsonObject) -> T): Pair<List<T>, Map<String, T>> {
                val records = items(file).map(parseOne)
                val out = LinkedHashMap<String, T>()
                for (rec in records) {
                    // duplicates exist in the source data (e.g. shrine/purchase variants);
                    // lookups resolve to the first occurrence — matches the site.
                    val name = nameOf(rec) ?: continue
                    out.putIfAbsent(name, rec)
                }
                return records to out
            }

            val (allTalents, talents) = index("talents", Parser::parseTalent)
            val (allMantras, mantras) = index("mantras", Parser::parseMantra)

            return GameData(
                talents = talents,
                allTalents = allTalents,
                mantras = mantras,
                allMantras = allMantras,
                weapons = index("weapons", Parser::parseWeapon).second,
                outfits = index("outfits", Parser::parseOutfit).second,
                equipment = index("equipment", Parser::parseEquipment).second,
                aspects = index("aspects", Parser::parseAspect).second,
                oaths = index("oaths", Parser::parseOath).second,
                boons = index("boons", Parser::parseBoon).second,
                flaws = index("flaws", Parser::parseFlaw).second,
                enchants = index("enchants", Parser::parseEnchant).second,
                mods = items("mods").map(Parser::parseModifier),
                enemies = items("enemies").map(Parser::parseEnemy),
            )
        }

        private fun nameOf(rec: Any?): String? = when (rec) {
                is com.dwbuilder.app.domain.model.Talent -> rec.name
                is com.dwbuilder.app.domain.model.Mantra -> rec.name
                is com.dwbuilder.app.domain.model.Weapon -> rec.name
                is com.dwbuilder.app.domain.model.Outfit -> rec.name
                is com.dwbuilder.app.domain.model.Equipment -> rec.name
                is com.dwbuilder.app.domain.model.Aspect -> rec.name
                is com.dwbuilder.app.domain.model.Oath -> rec.name
                is com.dwbuilder.app.domain.model.Boon -> rec.name
                is com.dwbuilder.app.domain.model.Flaw -> rec.name
                is com.dwbuilder.app.domain.model.Enchant -> rec.name
                else -> null
            }
    }
}