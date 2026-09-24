package com.dwbuilder.app

import com.dwbuilder.app.data.GameData

/** Lazily loads the bundled dataset (shipped under src/test/resources/data). */
object TestData {
    val data: GameData by lazy {
        GameData.load { name ->
            val stream = TestData::class.java.classLoader.getResourceAsStream("data/$name")
                ?: error("missing test resource data/$name")
            stream.bufferedReader().use { it.readText() }
        }
    }
}
