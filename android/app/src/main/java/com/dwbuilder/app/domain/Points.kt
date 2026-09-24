package com.dwbuilder.app.domain

import com.dwbuilder.app.domain.model.Build

/**
 * Point/power math — exact port of KwKd8cfB.js (`Pn`, `Jn`, `Ab`, `Ob`, `Wo`,
 * `Vo`). The site computes these over the *effective* attributes (`w` = stored
 * points minus the active shrine-mastery withdrawals), so every entry point
 * here first applies `ShrineRules.effectiveAttributes`.
 */
object Points {

    /** Total attribute budget (site's Rb = 330). */
    const val BUDGET: Int = 330

    private fun attrs(build: Build) = ShrineRules.effectiveAttributes(build)

    /** Sum of the values in a map (treating nulls as 0). */
    fun sum(values: Map<String, Int>): Int = values.values.sum()

    /** Max of the values in a map (site's `Wo`). */
    fun max(values: Map<String, Int>): Int = values.values.maxOrNull() ?: 0

    /**
     * Total points spent:
     *   spent = sum(base) + sum(weapon) + sum(attunement)
     *           − 1 for each extra attunement past the first that has ≥ 1 point
     */
    fun spent(build: Build): Int {
        val a = attrs(build)
        var total = sum(a.base) + sum(a.weapon) + sum(a.attunement)
        var hadAttunement = false
        for (value in a.attunement.values) {
            if (hadAttunement && value > 0) total--
            if (value >= 1) hadAttunement = true
        }
        return total
    }

    /** Power 0..20 = clamp(floor((spent − 15) / 15), 0, 20). */
    fun power(build: Build): Int {
        val t = spent(build)
        val raw = Math.floorDiv(t - 30 + 15, 15)
        return raw.coerceIn(0, 20)
    }

    /** Points remaining until the budget is exhausted. */
    fun pointsLeft(build: Build): Int = BUDGET - spent(build)

    /**
     * Points until the next power-up. Mirrors the site's quirk:
     * at power 0 it displays 15 (even though Power 1 arrives at 30 points).
     */
    fun toNextPower(build: Build): Int {
        val t = spent(build)
        val p = power(build)
        if (p == 1 && 30 - t > 0) return 30 - t
        // NOTE: JS `%` is truncated remainder; Kotlin `%` matches, `.mod()` would not.
        if (p < 20 && 15 - (t - 15) % 15 != 0) return 15 - (t - 15) % 15
        return 0
    }
}