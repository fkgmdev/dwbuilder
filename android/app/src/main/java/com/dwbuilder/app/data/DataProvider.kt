package com.dwbuilder.app.data

import android.content.Context

/** Single in-memory copy of the game dataset, loaded once from bundled assets. */
object DataProvider {
    @Volatile
    private var cache: GameData? = null

    fun get(context: Context): GameData {
        cache?.let { return it }
        synchronized(this) {
            cache?.let { return it }
            val texts = Assets.readDataAssets(context, DataProvider.bundleNames)
            return GameData.load { texts[it] ?: error("missing asset data/$it") }.also { cache = it }
        }
    }

    private val bundleNames = listOf(
        "talents.json", "mantras.json", "weapons.json", "outfits.json",
        "equipment.json", "aspects.json", "oaths.json", "boons.json",
        "flaws.json", "enchants.json", "enemies.json", "mods.json",
    )
}