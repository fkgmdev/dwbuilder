package com.dwbuilder.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dwbuilder.app.data.DataProvider
import com.dwbuilder.app.data.GameData
import com.dwbuilder.app.domain.Points
import com.dwbuilder.app.domain.Transfer
import com.dwbuilder.app.domain.model.Attributes
import com.dwbuilder.app.domain.model.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Which attribute store a stepper edits. */
enum class AttrKind { BASE, WEAPON, ATTUNEMENT }

/**
 * UI state holder for the builder. Keeps the loaded dataset plus the working
 * build, and exposes mutation actions that always produce valid states.
 */
class BuilderViewModel(app: Application) : AndroidViewModel(app) {

    var data by mutableStateOf<GameData?>(null)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var build by mutableStateOf(Build.empty())
        private set

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                data = DataProvider.get(getApplication())
            } catch (t: Throwable) {
                error = t.message ?: t.javaClass.simpleName
            }
        }
    }

    val pointsLeft: Int get() = Points.pointsLeft(build)

    private fun patch(transform: (Build) -> Build) {
        build = transform(build)
    }

    // ----- identity -----

    fun setRace(race: String) = patch { it.copy(race = race) }
    fun setOrigin(origin: String) = patch { it.copy(origin = origin) }
    fun setOath(oath: String) = patch { it.copy(oath = oath) }
    fun setBell(bell: String) = patch { it.copy(bell = bell) }
    fun setMurmur(murmur: String) = patch { it.copy(murmur = murmur) }
    fun setMultifaceted(v: Boolean) = patch { it.copy(multifaceted = v) }

    fun setBoon(index: Int, name: String) = patch {
        it.copy(boons = it.boons.toMutableList().also { l -> l[index] = name })
    }

    fun setFlaw(index: Int, name: String) = patch {
        it.copy(flaws = it.flaws.toMutableList().also { l -> l[index] = name })
    }

    // ----- attributes -----

    private fun attrs(kind: AttrKind, b: Build) = when (kind) {
        AttrKind.BASE -> b.attributes.base
        AttrKind.WEAPON -> b.attributes.weapon
        AttrKind.ATTUNEMENT -> b.attributes.attunement
    }

    private fun valueOf(kind: AttrKind, stat: String): Int = attrs(kind, build)[stat] ?: 0

    /**
     * Adding a point is free when it opens a fresh attunement while another
     * attunement already holds points (the site's second-attunement discount).
     */
    private fun canAffordAdd(kind: AttrKind, stat: String): Boolean {
        if (pointsLeft > 0) return true
        if (kind != AttrKind.ATTUNEMENT) return false
        if (valueOf(kind, stat) > 0) return false
        return build.attributes.attunement.entries.any { it.key != stat && it.value > 0 }
    }

    fun incrementAttr(kind: AttrKind, stat: String, delta: Int) {
        if (delta != 1 && delta != -1) return
        val cur = valueOf(kind, stat)
        val next = cur + delta
        if (next < 0) return
        if (delta > 0) {
            if (next > 100) return
            if (!canAffordAdd(kind, stat)) return
        }
        val map = attrs(kind, build) + (stat to next)
        patch {
            when (kind) {
                AttrKind.BASE -> it.copy(attributes = it.attributes.copy(base = map))
                AttrKind.WEAPON -> it.copy(attributes = it.attributes.copy(weapon = map))
                AttrKind.ATTUNEMENT -> it.copy(attributes = it.attributes.copy(attunement = map))
            }
        }
    }

    // ----- traits (per-trait cap 6, shared pool 12) -----

    fun setTrait(name: String, value: Int) {
        val clamped = value.coerceIn(0, 6)
        val others = build.traits - name
        if (others.values.sum() + clamped > 12) return
        patch { it.copy(traits = it.traits + (name to clamped)) }
    }

    // ----- talents / mantras -----

    fun toggleTalent(name: String) = patch {
        if (name in it.talents) it.copy(talents = it.talents - name)
        else it.copy(talents = it.talents + name)
    }

    fun toggleMantra(name: String) = patch {
        if (name in it.mantras) it.copy(mantras = it.mantras - name)
        else it.copy(mantras = it.mantras + name)
    }

    // ----- weapon / tuning -----

    fun setWeapon(name: String) = patch { it.copy(weapon = name) }
    fun setEnchant(name: String) = patch { it.copy(enchant = name) }
    fun setStarCount(count: Int) = patch { it.copy(tuning = it.tuning.copy(starCount = count.coerceIn(0, 3))) }
    fun setStarMod(mod: String) = patch { it.copy(tuning = it.tuning.copy(starMod = mod)) }
    fun setResistPct(pct: Double) = patch { it.copy(tuning = it.tuning.copy(resistPct = pct.coerceIn(0.0, 100.0))) }
    fun setBullet(bullet: String) = patch { it.copy(tuning = it.tuning.copy(bullet = bullet)) }
    fun setAirborne(v: Boolean) = patch { it.copy(tuning = it.tuning.copy(airborne = v)) }

    fun toggleRing(name: String) = patch {
        val r = it.tuning.rings
        it.copy(tuning = it.tuning.copy(rings = if (r[name] == true) r - name else r + (name to true)))
    }

    fun toggleMod(name: String) = patch {
        val m = it.tuning.enabledMods
        it.copy(tuning = it.tuning.copy(enabledMods = if (name in m) m - name else m + name))
    }

    // ----- equipment / outfit -----

    /** Equip or clear (name = "") an item in one of the 7 slots. */
    fun setEquipment(slot: String, name: String) = patch {
        it.copy(equipment = if (name.isEmpty()) it.equipment - slot else it.equipment + (slot to name))
    }

    fun setOutfit(name: String) = patch { it.copy(outfit = name) }

    // ----- import (in-game transfer text) -----

    /**
     * Applies in-game build text onto the current build. Names that don't exist
     * in the bundled catalogs are dropped (transfer text may embed mantra names
     * inside the talents section, mirroring the site's section-slicing quirk).
     */
    fun importTransfer(text: String) {
        val d = data ?: return
        if (text.isBlank()) return
        val parsed = Transfer.parse(text)
        val talents = parsed.talents.distinct().filter { d.talent(it) != null }
        val mantras = parsed.mantras.distinct().filter { d.mantra(it) != null }
        patch { b ->
            b.copy(
                name = parsed.name.ifBlank { b.name },
                level = parsed.level,
                race = parsed.race,
                origin = parsed.origin,
                oath = if (parsed.oath.equals("None", ignoreCase = true)) b.oath else parsed.oath,
                attributes = Attributes(parsed.base, parsed.weapon, parsed.attunement),
                talents = talents,
                mantras = mantras,
            )
        }
    }
}