package com.dwbuilder.app.data

import android.content.Context
import java.io.IOException

/** Reads the bundled data files from assets/data/. */
object Assets {
    fun readDataAssets(context: Context, names: List<String>): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        for (name in names) {
            out[name] = readAsset(context, "data/$name")
        }
        return out
    }

    fun readAsset(context: Context, path: String): String = try {
        context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
    } catch (e: IOException) {
        throw IllegalStateException("bundled asset missing: $path", e)
    }
}