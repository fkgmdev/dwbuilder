package com.dwbuilder.app.data

import android.content.Context
import com.dwbuilder.app.domain.model.Build

/** A locally persisted build draft. */
data class Draft(
    val version: Int,
    val build: Build,
    val phase: String,
    val timestamp: Long,
)

/**
 * Local draft persistence — mirrors the site's
 * `localStorage._dwb.draft.v1.{buildId} = {version, build, phase, timestamp}`.
 * Single draft per install (no accounts/server), written on every build change
 * (debounced by the ViewModel) and restored at startup.
 */
object DraftStore {

    private const val PREFS = "dwb_draft"
    private const val KEY_VERSION = "version"
    private const val KEY_TIMESTAMP = "timestamp"
    private const val KEY_BUILD = "build"
    private const val KEY_PHASE = "phase"

    fun save(context: Context, build: Build, phase: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        try {
            prefs.edit()
                .putInt(KEY_VERSION, 1)
                .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
                .putString(KEY_BUILD, BuildJson.encode(build))
                .putString(KEY_PHASE, phase)
                .apply()
        } catch (_: Throwable) {
            // Persistence is best-effort; a failed save must never crash the UI.
        }
    }

    fun load(context: Context): Draft? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val text = prefs.getString(KEY_BUILD, null) ?: return null
        return try {
            Draft(
                version = prefs.getInt(KEY_VERSION, 0),
                build = BuildJson.decode(text),
                phase = prefs.getString(KEY_PHASE, "") ?: "",
                timestamp = prefs.getLong(KEY_TIMESTAMP, 0L),
            )
        } catch (_: Throwable) {
            prefs.edit().clear().apply()
            null
        }
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}