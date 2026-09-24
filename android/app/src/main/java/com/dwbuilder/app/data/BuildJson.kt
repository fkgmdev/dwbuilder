package com.dwbuilder.app.data

import com.dwbuilder.app.domain.model.Attributes
import com.dwbuilder.app.domain.model.Build
import com.dwbuilder.app.domain.model.Tuning
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Manual JSON codec for [Build] — mirrors the site's draft `build` object so a
 * locally saved draft can be restored exactly (no kotlinx plugins needed for a
 * model with heterogeneous nested maps). Unknown/absent fields fall back to the
 * model defaults, which keeps old drafts forward-compatible.
 */
object BuildJson {

    fun encode(build: Build): String = buildJsonObject {
        put("name", build.name)
        put("level", build.level)
        put("race", build.race)
        put("origin", build.origin)
        put("oath", build.oath)
        put("bell", build.bell)
        put("murmur", build.murmur)
        putAttrs("attributes", build.attributes)
        putJsonArray("talents") { build.talents.forEach { add(it) } }
        putJsonArray("mantras") { build.mantras.forEach { add(it) } }
        put("weapon", build.weapon)
        put("enchant", build.enchant)
        putJsonObject("equipment") { build.equipment.forEach { (k, v) -> put(k, v) } }
        put("outfit", build.outfit)
        putJsonArray("boons") { build.boons.forEach { add(it) } }
        putJsonArray("flaws") { build.flaws.forEach { add(it) } }
        put("multifaceted", build.multifaceted)
        putIntMap("traits", build.traits)
        putJsonObject("tuning") {
            put("starCount", build.tuning.starCount)
            put("starMod", build.tuning.starMod)
            put("resistPct", build.tuning.resistPct)
            putJsonObject("rings") { build.tuning.rings.forEach { (k, v) -> put(k, v) } }
            putJsonArray("enabledMods") { build.tuning.enabledMods.forEach { add(it) } }
            put("bullet", build.tuning.bullet)
            put("airborne", build.tuning.airborne)
        }
        build.preShrine?.let { putAttrs("preShrine", it) }
        build.postShrine?.let { putAttrs("postShrine", it) }
        putIntMap("preMastery", build.preMastery)
        putIntMap("postMastery", build.postMastery)
        if (build.shrineMode.isNotEmpty()) put("shrineMode", build.shrineMode)
    }.toString()

    fun decode(text: String): Build {
        val o = parseJson(text) as? JsonObject ?: error("draft is not a JSON object")
        return Build(
            name = o.strOr("name", ""),
            level = o.intOr("level", 0),
            race = o.strOr("race", "None"),
            origin = o.strOr("origin", "Castaway"),
            oath = o.strOr("oath", "Oathless"),
            bell = o.strOr("bell", "None"),
            murmur = o.strOr("murmur", ""),
            attributes = decodeAttrs(o.obj("attributes") ?: JsonObject(emptyMap())),
            talents = o.strList("talents").distinct(),
            mantras = o.strList("mantras").distinct(),
            weapon = o.strOr("weapon", ""),
            enchant = o.strOr("enchant", ""),
            equipment = o.obj("equipment")?.mapNotNull { (k, v) ->
                (v as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull?.let { k to it }
            }?.toMap() ?: emptyMap(),
            outfit = o.strOr("outfit", ""),
            boons = o.strList("boons").ifEmpty { listOf("None", "None") },
            flaws = o.strList("flaws").ifEmpty { listOf("None", "None", "None") },
            multifaceted = o.boolOr("multifaceted", false),
            traits = o.intMap("traits"),
            tuning = Tuning(
                starCount = o.obj("tuning")?.intOr("starCount", 0) ?: 0,
                starMod = o.obj("tuning")?.strOr("starMod", "") ?: "",
                resistPct = o.obj("tuning")?.doubleOr("resistPct", 35.0) ?: 35.0,
                rings = o.obj("tuning")?.obj("rings")?.mapNotNull { (k, v) ->
                    (v as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull?.let { k to (it == "true") }
                }?.toMap() ?: emptyMap(),
                enabledMods = o.obj("tuning")?.strList("enabledMods")?.toSet() ?: emptySet(),
                bullet = o.obj("tuning")?.strOr("bullet", "None") ?: "None",
                airborne = o.obj("tuning")?.boolOr("airborne", false) ?: false,
            ),
            shrineMode = o.strOr("shrineMode", ""),
            preShrine = o.obj("preShrine")?.let(::decodeAttrs),
            postShrine = o.obj("postShrine")?.let(::decodeAttrs),
            preMastery = o.intMap("preMastery"),
            postMastery = o.intMap("postMastery"),
        )
    }

    private fun JsonObjectBuilder.putAttrs(key: String, attrs: Attributes) {
        putJsonObject(key) {
            putIntMap("base", attrs.base)
            putIntMap("weapon", attrs.weapon)
            putIntMap("attunement", attrs.attunement)
        }
    }

    private fun JsonObjectBuilder.putIntMap(key: String, map: Map<String, Int>) {
        if (map.isEmpty()) return
        putJsonObject(key) { map.forEach { (k, v) -> put(k, v) } }
    }

    private fun decodeAttrs(o: JsonObject): Attributes = Attributes(
        base = o.intMap("base"),
        weapon = o.intMap("weapon"),
        attunement = o.intMap("attunement"),
    )
}