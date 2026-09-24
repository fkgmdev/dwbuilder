package com.dwbuilder.app.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

val dataJson = Json { ignoreUnknownKeys = true }

fun parseJson(text: String): JsonElement = dataJson.parseToJsonElement(text)

// ---- JsonObject conveniences ----
fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
fun JsonObject.strOr(key: String, default: String): String = str(key) ?: default
fun JsonObject.bool(key: String): Boolean = (this[key] as? JsonPrimitive)?.contentOrNull == "true"
fun JsonObject.boolOr(key: String, default: Boolean): Boolean =
    (this[key] as? JsonPrimitive)?.booleanOrNull ?: default
fun JsonObject.double(key: String): Double? = (this[key] as? JsonPrimitive)?.doubleOrNull
fun JsonObject.doubleOr(key: String, default: Double): Double = double(key) ?: default
fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull
fun JsonObject.intOr(key: String, default: Int): Int = int(key) ?: default

/** Object value at `key`, or null. */
fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

/** Array value at `key`, or empty list. */
fun JsonObject.arr(key: String): List<JsonElement> = (this[key] as? JsonArray)?.toList() ?: emptyList()

/** String array at `key`. */
fun JsonObject.strList(key: String): List<String> =
    arr(key).mapNotNull { (it as? JsonPrimitive)?.contentOrNull }

/** Object of numbers at `key` (any numeric type). */
fun JsonObject.numMap(key: String): Map<String, Double> {
    val o = obj(key) ?: return emptyMap()
    return o.mapNotNull { (k, v) -> (v as? JsonPrimitive)?.doubleOrNull?.let { k to it } }.toMap()
}

/** Object of integers at `key`. */
fun JsonObject.intMap(key: String): Map<String, Int> {
    val o = obj(key) ?: return emptyMap()
    return o.mapNotNull { (k, v) -> (v as? JsonPrimitive)?.intOrNull?.let { k to it } }.toMap()
}