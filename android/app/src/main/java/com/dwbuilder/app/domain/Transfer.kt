package com.dwbuilder.app.domain

import com.dwbuilder.app.domain.model.Attributes
import com.dwbuilder.app.domain.model.Build

/**
 * In-game build transfer text — the paste format Deepwoken's builder and
 * deepwoken.co both understand.
 *
 * `parse` is a faithful port of `ys` (plus `nn`/`an`/`hs`) from C74yZVe_.js:
 *  - first non-empty line            -> character name
 *  - `LVL 20 Race Origin Oath`       -> identity header
 *  - `10 STR; 10 FTD; ...`           -> base attributes
 *  - `0 HVY; 1 MED; 0 LHT`           -> weapon skills
 *  - `0 FIR; 0 ICE; ...`             -> attunements
 *  - `== TALENTS ==` / `== MANTRAS ==` sections -> taken lists
 *
 * `export` produces text that round-trips through `parse`.
 */
object Transfer {

    private val LVL = Regex(
        "LVL\\s+(\\d+)\\s+(\\S+)\\s+(\\S+)(?:\\s+(.+))?", RegexOption.IGNORE_CASE,
    )
    private val BASE = Regex(
        "(\\d+)\\s*STR;\\s*(\\d+)\\s*FTD;\\s*(\\d+)\\s*AGL;\\s*(\\d+)\\s*INT;\\s*(\\d+)\\s*WLL;\\s*(\\d+)\\s*CHA",
        RegexOption.IGNORE_CASE,
    )
    private val WEAPON = Regex(
        "(\\d+)\\s*HVY;\\s*(\\d+)\\s*MED;\\s*(\\d+)\\s*LHT", RegexOption.IGNORE_CASE,
    )
    private val ATTUN = Regex(
        "(\\d+)\\s*FIR;\\s*(\\d+)\\s*ICE;\\s*(\\d+)\\s*LTN;\\s*(\\d+)\\s*WND;\\s*(\\d+)\\s*SDW;\\s*(\\d+)\\s*MTL(?:;\\s*(\\d+)\\s*BLD)?",
        RegexOption.IGNORE_CASE,
    )

    private const val TALENTS_MARKER = "== TALENTS =="
    private const val MANTRAS_MARKER = "== MANTRAS =="

    private val WEAPON_KEYS = Attributes.WEAPON_STATS
    private val ATTUN_KEYS = Attributes.ATTUNEMENT_STATS

    /** Parsed transfer text. Weapon/attunement maps use the canonical model names. */
    data class Parsed(
        val name: String,
        val level: Int,
        val race: String,
        val origin: String,
        val oath: String,
        val base: Map<String, Int>,
        val weapon: Map<String, Int>,
        val attunement: Map<String, Int>,
        val talents: List<String>,
        val mantras: List<String>,
    )

    /** site `nn`: non-empty trimmed lines. */
    private fun lines(s: String): List<String> =
        s.split("\n").map { it.trim() }.filter { it.isNotEmpty() }

    /** site `hs`: the lines between `== MANTRAS ==` and the next section marker. */
    private fun mantraSection(ls: List<String>): List<String> {
        val start = ls.indexOf(MANTRAS_MARKER)
        if (start == -1) return emptyList()
        val end = (start + 1 until ls.size).firstOrNull { ls[it].startsWith("==") } ?: ls.size
        return ls.subList(start + 1, end)
    }

    /** site `an`: everything after `== TALENTS ==`, minus section markers. */
    private fun talentSection(ls: List<String>): List<String> {
        val start = ls.indexOf(TALENTS_MARKER)
        if (start == -1) return emptyList()
        return ls.subList(start + 1, ls.size).filter { !it.startsWith("==") }
    }

    /** site `ys`: parse in-game build text into a [Parsed] build. */
    fun parse(s: String): Parsed {
        val ls = lines(s)
        var name = ls.getOrNull(0) ?: ""
        var level = 0
        var race = "None"
        var origin = "Castaway"
        var oath = "None"

        ls.getOrNull(1)?.let { header ->
            val m = LVL.find(header)
            if (m != null) {
                level = m.groupValues[1].toIntOrNull() ?: 0
                race = m.groupValues[2].ifEmpty { "None" }
                origin = m.groupValues[3].ifEmpty { "Castaway" }
                oath = m.groupValues[4].trim().ifEmpty { "None" }
            }
        }

        val base = Attributes.BASE_STATS.associateWith { 0 }.toMutableMap()
        val weapon = WEAPON_KEYS.associateWith { 0 }.toMutableMap()
        val attunement = ATTUN_KEYS.associateWith { 0 }.toMutableMap()

        for (line in ls) {
            BASE.find(line)?.let { m ->
                base["Strength"] = m.groupValues[1].toInt()
                base["Fortitude"] = m.groupValues[2].toInt()
                base["Agility"] = m.groupValues[3].toInt()
                base["Intelligence"] = m.groupValues[4].toInt()
                base["Willpower"] = m.groupValues[5].toInt()
                base["Charisma"] = m.groupValues[6].toInt()
                break
            }
        }
        for (line in ls) {
            WEAPON.find(line)?.let { m ->
                weapon["Heavy Weapon"] = m.groupValues[1].toInt()
                weapon["Medium Weapon"] = m.groupValues[2].toInt()
                weapon["Light Weapon"] = m.groupValues[3].toInt()
                break
            }
        }
        for (line in ls) {
            ATTUN.find(line)?.let { m ->
                attunement["Flamecharm"] = m.groupValues[1].toInt()
                attunement["Frostdraw"] = m.groupValues[2].toInt()
                attunement["Thundercall"] = m.groupValues[3].toInt()
                attunement["Galebreathe"] = m.groupValues[4].toInt()
                attunement["Shadowcast"] = m.groupValues[5].toInt()
                attunement["Ironsing"] = m.groupValues[6].toInt()
                if (m.groupValues[7].isNotEmpty()) attunement["Bloodrend"] = m.groupValues[7].toInt()
                break
            }
        }

        return Parsed(
            name = name,
            level = level,
            race = race,
            origin = origin,
            oath = oath,
            base = base,
            weapon = weapon,
            attunement = attunement,
            talents = talentSection(ls),
            mantras = mantraSection(ls),
        )
    }

    /** Serializes a build to transfer text (round-trips through [parse]). */
    fun export(build: Build): String {
        val b = build.attributes.base
        val w = build.attributes.weapon
        val a = build.attributes.attunement
        val sb = StringBuilder()
        sb.append(build.name.ifBlank { "My Build" }).append('\n')
        sb.append("LVL ${build.level} ${build.race} ${build.origin} ${build.oath}").append('\n')
        sb.append('\n')
        sb.append(
            "${b["Strength"] ?: 0} STR; ${b["Fortitude"] ?: 0} FTD; ${b["Agility"] ?: 0} AGL; " +
                "${b["Intelligence"] ?: 0} INT; ${b["Willpower"] ?: 0} WLL; ${b["Charisma"] ?: 0} CHA",
        ).append('\n')
        sb.append('\n')
        sb.append("${w["Heavy Weapon"] ?: 0} HVY; ${w["Medium Weapon"] ?: 0} MED; ${w["Light Weapon"] ?: 0} LHT").append('\n')
        sb.append('\n')
        sb.append(
            "${a["Flamecharm"] ?: 0} FIR; ${a["Frostdraw"] ?: 0} ICE; ${a["Thundercall"] ?: 0} LTN; " +
                "${a["Galebreathe"] ?: 0} WND; ${a["Shadowcast"] ?: 0} SDW; ${a["Ironsing"] ?: 0} MTL; " +
                "${a["Bloodrend"] ?: 0} BLD",
        ).append('\n')
        sb.append('\n')
        if (build.talents.isNotEmpty()) {
            sb.append(TALENTS_MARKER).append('\n')
            build.talents.forEach { sb.append(it).append('\n') }
            sb.append('\n')
        }
        if (build.mantras.isNotEmpty()) {
            sb.append(MANTRAS_MARKER).append('\n')
            build.mantras.forEach { sb.append(it).append('\n') }
        }
        return sb.toString().trimEnd()
    }
}