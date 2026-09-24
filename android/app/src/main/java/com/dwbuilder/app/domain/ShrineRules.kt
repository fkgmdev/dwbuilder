package com.dwbuilder.app.domain

import com.dwbuilder.app.domain.model.Attributes
import com.dwbuilder.app.domain.model.Build
import com.dwbuilder.app.domain.model.Talent
import kotlin.math.floor

/**
 * Shrine engine — exact port of the shrine code in `www/_nuxt/KwKd8cfB.js`
 * (store) and `C74yZVe_.js` (mastery dialog):
 *
 *  - `$b`  shrine-of-order redistribution: mean-split the disposable points,
 *         pin any non-attunement stat that would lose more than `qo = 25`,
 *         drain the overflow from the others, floor to integers, hand out the
 *         fractional surplus +1/stat while nothing exceeds `Db = 100`.
 *  - `Pn`  total points spent, with the second-attunement discount.
 *  - `qb`  effective attributes = attributes minus a mastery-withdrawal map.
 *  - `Ee`  applyShrineOrder: snapshot pre-shrine, redistribute the
 *         pre-mastery-effective points, switch to "post" mode.
 *  - `ct`/`Qe`/`yn`/`ss`  load/save pre-post shrine attribute snapshots.
 *  - `nt`/`Tr`/`gr`/`id`/`os`  mastery-map sanitizing, applying and reset.
 *  - `ie`/`ge`/`ve`/`ue`  SoM stepper guards: phase locks, 0..stat bounds,
 *         talent-threshold and racial-floor pins, the shared 100-pt budget.
 */
object ShrineRules {

    /** Shared mastery budget across pre + post phases (site's `Db = 100`). */
    const val MASTERY_LIMIT = 100

    /** Drain cap: a non-attunement stat may lose at most 25 points (site's `qo`). */
    const val DRAIN_CAP = 25

    /** Per-stat cap for the final surplus distribution (site's `Db`). */
    const val STAT_CAP = 100

    /** Pseudo-stats excluded from talent-threshold pin checks (site's `X`). */
    private val PSEUDO_STATS = setOf("Body", "Mind", "Weapon", "Weapons", "Attunement")

    // ------------------------------------------------------------- helpers --

    private fun minus(map: Map<String, Int>, mastery: Map<String, Int>): Map<String, Int> =
        map.mapValues { (k, v) -> maxOf(0, v - (mastery[k] ?: 0)) }

    /** `qb`: attributes minus a mastery-withdrawal map. */
    fun effective(attrs: Attributes, mastery: Map<String, Int>): Attributes =
        Attributes(
            minus(attrs.base, mastery),
            minus(attrs.weapon, mastery),
            minus(attrs.attunement, mastery),
        )

    /** `Ss`: true when no attribute holds any point. */
    fun isEmpty(attrs: Attributes): Boolean =
        attrs.base.values.none { it > 0 } &&
            attrs.weapon.values.none { it > 0 } &&
            attrs.attunement.values.none { it > 0 }

    fun hasPreShrine(build: Build): Boolean = build.preShrine?.let { !isEmpty(it) } ?: false
    fun hasPostShrine(build: Build): Boolean = build.postShrine?.let { !isEmpty(it) } ?: false

    /** `Tr`: total withdrawn points in a mastery map. */
    fun totalWithdrawn(mastery: Map<String, Int>): Int = mastery.values.filter { it > 0 }.sum()

    /**
     * `nt`: keep only non-positive-truncated values whose stat actually exists
     * in the build's attribute maps (drops typos and fake keys).
     */
    fun sanitize(build: Build, map: Map<String, Int>): Map<String, Int> {
        val out = mutableMapOf<String, Int>()
        for ((name, raw) in map) {
            val v = raw
            if (v <= 0) continue
            if (name in build.attributes.base ||
                name in build.attributes.weapon ||
                name in build.attributes.attunement
            ) {
                out[name] = v
            }
        }
        return out
    }

    /** `v` (store): the mastery map active for the current shrine mode. */
    fun activeMastery(build: Build): Map<String, Int> =
        if (build.shrineMode == "post") build.postMastery else build.preMastery

    /** `w` (store): effective attributes = attributes minus the active mastery. */
    fun effectiveAttributes(build: Build): Attributes {
        val mastery = activeMastery(build)
        if (mastery.isEmpty()) return build.attributes
        return effective(build.attributes, mastery)
    }

    /**
     * `Pn`: total points spent — sum of every attribute, minus 1 for each extra
     * attunement past the first that holds at least one point.
     */
    fun totalPoints(attrs: Attributes): Int {
        var total = attrs.base.values.sum() + attrs.weapon.values.sum() + attrs.attunement.values.sum()
        var hadAttunement = false
        for (v in attrs.attunement.values) {
            if (hadAttunement && v > 0) total--
            if (v >= 1) hadAttunement = true
        }
        return total
    }

    // ----------------------------------------------------------- shrine of order --

    /** A stat keyed by its attribute block — mirrors the JS `{type, name}` refs. */
    data class StatRef(val block: String, val name: String)

    /** The stats present in a set of attributes (`Object.keys` of each block). */
    fun allStats(attrs: Attributes): List<StatRef> = buildList {
        attrs.base.keys.forEach { add(StatRef("base", it)) }
        attrs.weapon.keys.forEach { add(StatRef("weapon", it)) }
        attrs.attunement.keys.forEach { add(StatRef("attunement", it)) }
    }

    /**
     * `$b`: redistribute attribute points. All disposable stats (holding more
     * than their racial floor) are set to the mean of their total; any
     * non-attunement stat whose loss would exceed [DRAIN_CAP] is pinned at
     * original − 25 and the overflow drains from the rest (attunements absorb,
     * never pin). Floor to integers, then distribute the fractional surplus
     * +1 at a time while no undrained stat would pass [STAT_CAP].
     */
    fun redistribute(attrs: Attributes, raceBonuses: Map<String, Int>): Attributes {
        val work = mutableMapOf(
            "base" to attrs.base.mapValues { it.value.toDouble() }.toMutableMap(),
            "weapon" to attrs.weapon.mapValues { it.value.toDouble() }.toMutableMap(),
            "attunement" to attrs.attunement.mapValues { it.value.toDouble() }.toMutableMap(),
        )
        val all = allStats(attrs)

        fun current(s: StatRef): Double = work.getValue(s.block)[s.name] ?: 0.0
        fun set(s: StatRef, v: Double) { work.getValue(s.block)[s.name] = v }
        fun floorOf(s: StatRef): Double =
            (if (s.block == "base") raceBonuses[s.name] ?: 0 else 0).toDouble()
        fun original(s: StatRef): Double = when (s.block) {
            "base" -> (attrs.base[s.name] ?: 0).toDouble()
            "weapon" -> (attrs.weapon[s.name] ?: 0).toDouble()
            else -> (attrs.attunement[s.name] ?: 0).toDouble()
        }

        val disposable = all.filter { current(it) - floorOf(it) > 0.0 }
        if (disposable.isEmpty()) return toAttributes(work)

        var total = 0.0
        for (s in disposable) total += current(s)
        val mean = total / disposable.size
        for (s in disposable) set(s, mean)

        val drained = mutableSetOf<StatRef>()
        var snapshot = deepCopy(work)
        var iterations = 32
        var again = true
        while (again && iterations-- > 0) {
            again = false
            var overflow = 0.0
            for (s in disposable) {
                if (s.block == "attunement" || s in drained) continue
                val atIterationStart = snapshot.getValue(s.block)[s.name] ?: 0.0
                val orig = original(s)
                if (orig - current(s) > DRAIN_CAP) {
                    val pin = orig - DRAIN_CAP
                    set(s, pin)
                    overflow += pin - atIterationStart
                    drained += s
                }
            }
            val remaining = disposable.size - drained.size
            if (remaining > 0 && overflow != 0.0) {
                val share = overflow / remaining
                for (s in disposable) {
                    if (s in drained) continue
                    set(s, current(s) - share)
                    if (s.block != "attunement" && original(s) - current(s) > DRAIN_CAP) again = true
                }
            }
            snapshot = deepCopy(work)
        }

        for (s in disposable) set(s, floor(current(s)))
        var totalAfterFloor = 0.0
        for (s in disposable) totalAfterFloor += current(s)
        val surplus = total - totalAfterFloor

        val rest = disposable.filter { it !in drained }
        if (rest.isNotEmpty()) {
            var spare = surplus
            while (spare >= rest.size && !rest.any { current(it) + 1.0 > STAT_CAP }) {
                for (s in rest) set(s, current(s) + 1.0)
                spare -= rest.size
            }
        }
        return toAttributes(work)
    }

    /** Deep copy of the working map (`ws` = JSON clone in the JS). */
    private fun deepCopy(work: Map<String, Map<String, Double>>): Map<String, Map<String, Double>> =
        work.mapValues { (_, m) -> m.toMap() }

    private fun toAttributes(work: Map<String, Map<String, Double>>): Attributes = Attributes(
        work.getValue("base").mapValues { (_, v) -> floor(v).toInt() },
        work.getValue("weapon").mapValues { (_, v) -> floor(v).toInt() },
        work.getValue("attunement").mapValues { (_, v) -> floor(v).toInt() },
    )

    // ---------------------------------------------------------- apply / load ---

    /** `Ee` result — the updated build plus how many points the shrine freed. */
    data class OrderResult(val build: Build, val sparePoints: Int)

    /**
     * `Ee` (applyShrineOrder): snapshot the current attributes as `preShrine`,
     * redistribute the pre-mastery-effective points, switch to "post" mode.
     */
    fun applyOrder(build: Build, raceBonuses: Map<String, Int>): OrderResult {
        val eff = effective(build.attributes, build.preMastery)
        val before = totalPoints(eff)
        val redistributed = redistribute(eff, raceBonuses)
        val after = totalPoints(redistributed)
        val next = build.copy(preShrine = build.attributes, attributes = redistributed, shrineMode = "post")
        return OrderResult(next, kotlin.math.max(0, before - after))
    }

    /** `ct`: load the pre-shrine snapshot (when present) and switch to "pre". */
    fun loadPre(build: Build): Build? = build.preShrine?.takeIf { !isEmpty(it) }?.let {
        build.copy(attributes = it, shrineMode = "pre")
    }

    /** `Qe`: load the post-shrine snapshot (when present) and switch to "post". */
    fun loadPost(build: Build): Build? = build.postShrine?.takeIf { !isEmpty(it) }?.let {
        build.copy(attributes = it, shrineMode = "post")
    }

    /** `yn`: snapshot current attributes as the pre-shrine state. */
    fun savePre(build: Build): Build? = if (isEmpty(build.attributes)) null
    else build.copy(preShrine = build.attributes, shrineMode = "pre")

    /** `ss`: snapshot current attributes as the post-shrine state. */
    fun savePost(build: Build): Build? = if (isEmpty(build.attributes)) null
    else build.copy(postShrine = build.attributes, shrineMode = "post")

    /** `gr`: set one phase's mastery map, enforcing the shared 100-pt budget. */
    fun applyMastery(build: Build, phase: String, map: Map<String, Int>): Build {
        val clean = sanitize(build, map)
        val other = if (phase == "post") build.preMastery else build.postMastery
        if (totalWithdrawn(clean) + totalWithdrawn(other) > MASTERY_LIMIT) return build
        return if (phase == "post") build.copy(postMastery = clean) else build.copy(preMastery = clean)
    }

    /**
     * `id` (applyMasteries): set both mastery maps at once. `reshrine` restores
     * the pre-shrine snapshot and re-runs the Shrine of Order with the new
     * pre-mastery already applied.
     */
    fun applyMasteries(
        build: Build,
        pre: Map<String, Int>,
        post: Map<String, Int>,
        reshrine: Boolean,
        raceBonuses: Map<String, Int>,
    ): Build {
        val preClean = sanitize(build, pre)
        val postClean = sanitize(build, post)
        if (totalWithdrawn(preClean) + totalWithdrawn(postClean) > MASTERY_LIMIT) return build
        var next = build.copy(preMastery = preClean, postMastery = postClean)
        if (reshrine) {
            next = next.copy(attributes = next.preShrine ?: next.attributes)
            next = applyOrder(next, raceBonuses).build
        }
        return next
    }

    /** `os`: clear the current phase's mastery map. */
    fun resetMastery(build: Build): Build =
        if (build.shrineMode == "post") build.copy(postMastery = emptyMap())
        else build.copy(preMastery = emptyMap())

    // --------------------------------------------------- SoM stepper guards ---

    /** Why a withdrawal change is rejected (`ue` result). */
    enum class WithdrawGuard { OK, POST_LOCKED, PRE_LOCKED, BELOW_ZERO, ABOVE_STAT, PINNED, BUDGET_EXCEEDED }

    /** `f(I)`: the base attribute set a phase edits. */
    fun phaseBase(build: Build, phase: String): Attributes = when (phase) {
        "pre" -> if (hasPreShrine(build)) build.preShrine!! else build.attributes
        else -> if (hasPostShrine(build)) build.postShrine!! else build.attributes
    }

    /** `$(I)`: the mastery map a phase edits. */
    fun phaseMastery(build: Build, phase: String): Map<String, Int> =
        if (phase == "post") build.postMastery else build.preMastery

    /** `p(C,I)`: current withdrawal for a stat in a phase. */
    fun withdrawal(build: Build, phase: String, stat: StatRef): Int =
        phaseMastery(build, phase)[stat.name] ?: 0

    /** `v(C,I)`: displayed stat value in a phase = base − withdrawn. */
    fun displayValue(build: Build, phase: String, stat: StatRef): Int {
        val base = phaseBase(build, phase).stat(stat.name)
        return maxOf(0, base - withdrawal(build, phase, stat))
    }

    /**
     * `ie(C,I,W)`: the consequences of setting this stat's withdrawal to
     * `candidate`. Alerts when a currently-satisfied talent's stat requirement
     * would drop below its threshold, or a base stat would fall under the
     * racial bonus. `talents` is the taken-talent catalog; `raceBonuses` the
     * build's race bonus map ({} for multifaceted).
     */
    fun wouldPin(
        build: Build,
        talents: Map<String, Talent>,
        raceBonuses: Map<String, Int>,
        phase: String,
        stat: StatRef,
        candidate: Int,
    ): PinInfo {
        val base = phaseBase(build, phase)
        val mastery = phaseMastery(build, phase)

        /** Ee(me): effective attrs with every stat reduced by its mastery,
         *  using `me` for the target stat. */
        fun effectiveWith(valueForTarget: Int): Attributes {
            val b = base.base.toMutableMap()
            val w = base.weapon.toMutableMap()
            val a = base.attunement.toMutableMap()
            for (other in allStats(base)) {
                val withdrawn = if (other == stat) valueForTarget else mastery[other.name] ?: 0
                val originalValue = when (other.block) {
                    "base" -> base.base[other.name] ?: 0
                    "weapon" -> base.weapon[other.name] ?: 0
                    else -> base.attunement[other.name] ?: 0
                }
                val target = when (other.block) {
                    "base" -> b
                    "weapon" -> w
                    else -> a
                }
                target[other.name] = maxOf(0, originalValue - withdrawn)
            }
            return Attributes(b, w, a)
        }

        val candidateAttrs = effectiveWith(candidate)
        val raceFloor = if (stat.block == "base") raceBonuses[stat.name] ?: 0 else 0
        val candidateStat = candidateAttrs.stat(stat.name)
        val racial = if (raceFloor > 0 && candidateStat < raceFloor) raceFloor else null

        val pinnedTalents = mutableListOf<String>()
        if (build.talents.isNotEmpty()) {
            val currentAttrs = effectiveWith(withdrawal(build, phase, stat))
            for (taken in build.talents) {
                val t = talents[taken] ?: continue
                if (!t.countTowardsTalentTotal) continue
                val reqs = t.requirements ?: continue
                for ((statKey, need) in reqs.stats) {
                    if (statKey in PSEUDO_STATS) continue
                    val threshold = need
                    if (threshold <= 0) continue
                    val candidateValue = candidateAttrs.stat(statKey)
                    val currentValue = currentAttrs.stat(statKey)
                    if (candidateValue < threshold && currentValue >= threshold) {
                        pinnedTalents += taken
                        break
                    }
                }
            }
        }
        return PinInfo(pinnedTalents, racial?.let { stat.name }, racial ?: 0)
    }

    /** Pins detected for a hypothetical withdrawal. */
    data class PinInfo(val talents: List<String>, val raceStat: String?, val raceMin: Int) {
        val blocked: Boolean get() = talents.isNotEmpty() || raceStat != null
    }

    /**
     * `ue`: whether the withdrawal of `stat` in `phase` may be set to
     * `candidate`. Phase locks mirror the dialog: post-phase editing needs a
     * pre-shrine to exist; pre-phase editing locks once the shrine has run
     * unless re-shrining. Increments are refused when pinned; the combined
     * pre + post budget can't exceed [MASTERY_LIMIT].
     */
    fun guard(
        build: Build,
        talents: Map<String, Talent>,
        raceBonuses: Map<String, Int>,
        phase: String,
        stat: StatRef,
        candidate: Int,
        reshrine: Boolean = false,
    ): WithdrawGuard {
        if (phase == "post" && !hasPreShrine(build)) return WithdrawGuard.POST_LOCKED
        if (phase == "pre" && hasPreShrine(build) && !reshrine) return WithdrawGuard.PRE_LOCKED
        if (candidate < 0) return WithdrawGuard.BELOW_ZERO
        if (candidate > phaseBase(build, phase).stat(stat.name)) return WithdrawGuard.ABOVE_STAT
        val current = withdrawal(build, phase, stat)
        if (candidate - current == 1 && wouldPin(build, talents, raceBonuses, phase, stat, candidate).blocked) {
            return WithdrawGuard.PINNED
        }
        val combined = totalWithdrawn(build.preMastery) + totalWithdrawn(build.postMastery)
        if (combined - current + candidate > MASTERY_LIMIT) return WithdrawGuard.BUDGET_EXCEEDED
        return WithdrawGuard.OK
    }

    /** Apply an accepted withdrawal change (drops the key at zero, like the JS). */
    fun setWithdrawal(build: Build, phase: String, stat: StatRef, candidate: Int): Build {
        val map = phaseMastery(build, phase)
        val next = if (candidate == 0) map - stat.name else map + (stat.name to candidate)
        return if (phase == "post") build.copy(postMastery = next) else build.copy(preMastery = next)
    }
}