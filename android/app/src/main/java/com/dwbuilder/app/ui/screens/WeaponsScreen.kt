package com.dwbuilder.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dwbuilder.app.data.GameData
import com.dwbuilder.app.domain.DamageRules
import com.dwbuilder.app.domain.PveRules
import com.dwbuilder.app.domain.model.Weapon
import com.dwbuilder.app.ui.BuilderViewModel
import com.dwbuilder.app.ui.components.ChoiceChipRow
import com.dwbuilder.app.ui.components.DetailRow
import com.dwbuilder.app.ui.components.DropdownCell
import com.dwbuilder.app.ui.components.ListRowCard
import com.dwbuilder.app.ui.components.RarityChip
import com.dwbuilder.app.ui.components.SectionHeader
import com.dwbuilder.app.ui.components.num

/** The five stat rings plus the three special rings (site's weapon-tab ring toggles). */
private val RING_TOGGLES = listOf("Strength", "Agility", "Intelligence", "Willpower", "Charisma") +
    DamageRules.SPECIAL_RINGS

private val STAR_MOD_CHOICES = listOf("DMG%", "PEN%", "WGT%")

@Composable
fun WeaponsScreen(vm: BuilderViewModel) {
    val data = vm.data ?: return
    val build = vm.build
    var family by rememberSaveable { mutableStateOf("All") }

    val families = remember(data) {
        listOf("All") + data.weapons.values.map { it.type }.distinct().sorted()
    }
    val weapons = remember(data, family) {
        data.weapons.values.sortedBy { it.name }.filter { family == "All" || it.type == family }
    }
    val selected = build.weapon.takeIf { it.isNotEmpty() }?.let { data.weapon(it) }
    val breakdown = remember(build, selected) {
        selected?.let { w ->
            DamageRules.compute(DamageRules.DamageSource.weapon(w), build, data.mods, tuning = build.tuning)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(families) { f ->
                    FilterChip(selected = family == f, onClick = { family = f }, label = { Text(f) })
                }
            }
        }
        if (selected != null && breakdown != null) {
            item { WeaponBreakdown(vm, build, data, selected, breakdown) }
        }
        items(weapons, key = { it.name }, contentType = { "weapon" }) { w ->
            WeaponRow(
                weapon = w,
                selected = w.name == build.weapon,
                onClick = { vm.setWeapon(w.name) },
            )
        }
    }
}

@Composable
private fun WeaponRow(weapon: Weapon, selected: Boolean, onClick: () -> Unit) {
    ListRowCard(
        selected = selected,
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        content = {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(weapon.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${weapon.type} · ${num(weapon.damage)} dmg · ${weapon.damageTypes.joinToString("/")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                RarityChip(weapon.rarity)
            }
        },
    )
}

@Composable
private fun WeaponBreakdown(
    vm: BuilderViewModel,
    build: com.dwbuilder.app.domain.model.Build,
    data: GameData,
    weapon: Weapon,
    bd: DamageRules.Breakdown,
) {
    val tuning = build.tuning
    val enchantOptions = remember(data) {
        listOf("None") + data.enchants.values.filter { it.type == "Weapon" }.map { it.name }.sorted()
    }
    val availableMods = remember(build, data) {
        data.mods.filter {
            DamageRules.modAvailable(it, build.talents.toSet(), build.mantras.toSet(), build.enchant, build.murmur, build.oath)
        }
    }
    val gun = DamageRules.isGun(weapon.type)

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(weapon.name, style = MaterialTheme.typography.titleMedium)
                    Text("${weapon.type} · ${weapon.damageTypes.joinToString(" / ")}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                RarityChip(weapon.rarity)
            }

            Text("Base damage ${num(weapon.damage)}${weapon.bleedDamage?.let { " + bleed" } ?: ""}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)

            // ----- tuning: stars / enchant / resist / rings / bullet -----
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Stars", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalIconButton(
                            onClick = { vm.setStarCount(tuning.starCount - 1) },
                            enabled = tuning.starCount > 0,
                            modifier = Modifier.size(30.dp),
                        ) {
                            Text("−", style = MaterialTheme.typography.titleMedium)
                        }
                        Text("${tuning.starCount}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 8.dp))
                        FilledTonalIconButton(
                            onClick = { vm.setStarCount(tuning.starCount + 1) },
                            enabled = tuning.starCount < 3,
                            modifier = Modifier.size(30.dp),
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add")
                        }
                    }
                }
                ChoiceChipRow(
                    options = STAR_MOD_CHOICES,
                    selected = tuning.starMod,
                    onSelect = { vm.setStarMod(if (it == tuning.starMod) "" else it) },
                )
            }

            DropdownCell(
                label = "Enchant",
                selected = build.enchant.ifEmpty { "None" },
                options = enchantOptions,
                onSelect = { vm.setEnchant(if (it == "None") "" else it) },
            )

            if (gun) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Bullet", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ChoiceChipRow(
                        options = DamageRules.BULLETS.map { it.name },
                        selected = tuning.bullet,
                        onSelect = vm::setBullet,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Airborne", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = tuning.airborne, onCheckedChange = vm::setAirborne)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Enemy resistance", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${tuning.resistPct.toInt()}%", style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = tuning.resistPct.toFloat(),
                    onValueChange = { vm.setResistPct(it.toDouble()) },
                    valueRange = 0f..100f,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionHeader("Rings")
                ChoiceChipRow(
                    options = RING_TOGGLES,
                    selected = "",
                    onSelect = vm::toggleRing,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionHeader("Damage Modifiers")
                if (availableMods.isEmpty()) {
                    Text("No mods unlocked for this build", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                availableMods.forEach { mod ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(mod.name, style = MaterialTheme.typography.bodyMedium)
                            if (mod.notes.isNotBlank()) {
                                Text(mod.notes, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        FilterChip(
                            selected = mod.name in tuning.enabledMods,
                            onClick = { vm.toggleMod(mod.name) },
                            label = { Text(if (mod.name in tuning.enabledMods) "On" else "Off") },
                        )
                    }
                }
            }

            HorizontalDivider()

            // ----- numbers -----
            Text("Breakdown", style = MaterialTheme.typography.titleMedium)
            DetailRow("Base damage", num(bd.baseDamage))
            bd.scalingContributions.forEach { sc ->
                DetailRow("${sc.stat} (×${num(sc.scalingMultiplier)} on ${num(sc.investment)})", "+${num(sc.contribution)}")
            }
            if (bd.ringContributions.isNotEmpty()) {
                bd.ringContributions.forEach { rc ->
                    DetailRow("${rc.stat} ring (rank ${rc.rank})", "+${num(rc.contribution)}")
                }
            }
            DetailRow("Swing speed", num(weapon.swingSpeed, 2) + "s")
            DetailRow("Proficiency mult", "×${num(bd.proficiencyMultiplier)}")
            DetailRow("Scaled damage", num(bd.scaledDamage))
            val multText = if (bd.damageMultiplierCapped) "${num(bd.totalDamageMultiplierRaw)} → ${num(bd.totalDamageMultiplier)} (capped)" else num(bd.totalDamageMultiplier)
            DetailRow("Damage multiplier", multText)
            if (bd.bleedRate > 0) {
                DetailRow("Bleed", "${num(bd.bleedRate)} rate · ${num(bd.bleedDamage)}/tick")
            }
            DetailRow("Damage with mods", num(bd.damageWithMods))
            DetailRow("Total damage", num(bd.totalDamage))
            if (bd.reqDebuff != 1.0) {
                DetailRow("Req. debuff", "×${num(bd.reqDebuff, 3)}")
            }
            DetailRow("Final (vs ${tuning.resistPct.toInt()}%)", num(bd.finalDamage))
            DetailRow("Effective PEN", "${num(bd.effectivePenetration * 100)}%")
            DetailRow("Resisted damage", num(bd.resistedDamage))
            if (bd.dps > 0) {
                DetailRow("DPS", num(bd.dps))
            }

            HorizontalDivider()
            PveCalculator(data, weapon, bd)
        }
    }
}

/**
 * PvE calculator — the site's weapon-tab tool. Derives the monster-scale power
 * from the weapon's primary scaling stat, the weapon hit from the breakdown's
 * final damage, and DVM effectiveness at the default 100%.
 */
@Composable
private fun PveCalculator(data: GameData, weapon: Weapon, bd: DamageRules.Breakdown) {
    var enemyQuery by rememberSaveable { mutableStateOf("") }
    var enemyName by rememberSaveable { mutableStateOf("") }
    var custom by rememberSaveable { mutableStateOf(false) }
    var hpMinText by rememberSaveable { mutableStateOf("1000") }
    var hpMaxText by rememberSaveable { mutableStateOf("1000") }
    var dvm by rememberSaveable { mutableStateOf(0) }
    var msOverride by rememberSaveable { mutableStateOf(false) }
    var msManual by rememberSaveable { mutableStateOf(false) }
    var resistMin by rememberSaveable { mutableStateOf(25) }
    var resistMax by rememberSaveable { mutableStateOf(25) }
    var staggered by rememberSaveable { mutableStateOf(false) }
    var astral by rememberSaveable { mutableStateOf(false) }
    var magma by rememberSaveable { mutableStateOf(false) }
    var attunement by rememberSaveable { mutableStateOf("None") }
    var variantId by rememberSaveable { mutableStateOf("") }

    val primary = bd.scalingContributions.maxByOrNull { it.contribution }
    val power = primary?.investment ?: 1.0

    val matches = remember(enemyQuery) {
        val q = enemyQuery.trim()
        if (q.length < 2) emptyList()
        else data.enemies.filter { e ->
            e.name.contains(q, ignoreCase = true) || e.aliases.any { it.contains(q, ignoreCase = true) }
        }.take(24)
    }
    val enemy = remember(enemyName) {
        enemyName.takeIf { it.isNotEmpty() }?.let { n -> data.enemies.find { it.name == n } }
    }
    val info = remember(enemy) { enemy?.let { PveRules.analyze(it) } }
    val monsterScaling = if (msOverride) msManual else (info?.monsterScaling ?: false)

    val hpMin = hpMinText.toIntOrNull() ?: 0
    val hpMax = hpMaxText.toIntOrNull() ?: 0
    val health: List<Double>? = when {
        custom -> if (hpMin <= 0 || hpMax < hpMin) null else listOf(hpMin.toDouble(), hpMax.toDouble())
        else -> info?.let { PveRules.hpFor(it, variantId.takeIf { v -> v.isNotEmpty() }) }
    }

    val result = remember(
        bd, weapon, enemyName, custom, hpMin, hpMax, dvm, msOverride, msManual,
        resistMin, resistMax, staggered, astral, magma, attunement, variantId, health,
    ) {
        val h = health
        if (h == null) null
        else PveRules.hit(
            PveRules.HitInput(
                weaponDamage = bd.finalDamage,
                power = power,
                dvmPct = dvm.toDouble(),
                dvmEffectiveness = 1.0,
                monsterScaling = monsterScaling,
                health = h,
                resistance = listOf(resistMin.toDouble(), resistMax.toDouble()),
                staggered = staggered,
                penetration = bd.effectivePenetration,
                astral = astral,
                magmaGuard = magma,
                attunement = attunement,
                damageTypes = weapon.damageTypes,
            ),
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("PvE Calculator", style = MaterialTheme.typography.titleMedium)
        Text(
            "Weapon hit ${num(bd.finalDamage)}" +
                (primary?.let { " · power ${num(it.investment, 0)} (${it.stat})" } ?: ""),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (custom) "Custom enemy" else "Enemy: ${enemyName.ifEmpty { "none" }}",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text("Custom", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Switch(checked = custom, onCheckedChange = { custom = it })
        }

        if (custom) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = hpMinText,
                    onValueChange = { hpMinText = it.filter(Char::isDigit).take(7) },
                    modifier = Modifier.weight(1f),
                    label = { Text("HP min") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = hpMaxText,
                    onValueChange = { hpMaxText = it.filter(Char::isDigit).take(7) },
                    modifier = Modifier.weight(1f),
                    label = { Text("HP max") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        } else {
            OutlinedTextField(
                value = enemyQuery,
                onValueChange = { enemyQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search enemies…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
            )
            if (matches.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    matches.forEach { e ->
                        val h = PveRules.analyze(e).hp
                        ListRowCard(
                            selected = e.name == enemyName,
                            onClick = {
                                enemyName = e.name
                                enemyQuery = ""
                                variantId = ""
                            },
                            content = {
                                Row(Modifier.fillMaxWidth().padding(10.dp)) {
                                    Column(Modifier.weight(1f)) {
                                        Text(e.name, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            e.className + (h?.let { " · ${num(it[0], 0)} HP" } ?: ""),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            },
                        )
                    }
                }
            }
            if (info != null && info.variants.isNotEmpty()) {
                ChoiceChipRow(
                    options = listOf("Base") + info.variants.map { it.label },
                    selected = variantId.ifEmpty { "Base" },
                    onSelect = { variantId = if (it == "Base") "" else it },
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Damage vs monsters: $dvm%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(value = dvm.toFloat(), onValueChange = { dvm = it.toInt() }, valueRange = 0f..100f)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Monster scaling${if (!msOverride && info != null) " (auto: ${if (info.monsterScaling) "yes" else "no"})" else ""}",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Switch(
                checked = monsterScaling,
                onCheckedChange = {
                    msOverride = true
                    msManual = it
                },
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Enemy resistance: $resistMin% – $resistMax%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("min", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = resistMin.toFloat(),
                    onValueChange = {
                        val v = it.toInt().coerceAtMost(resistMax)
                        resistMin = v
                    },
                    valueRange = 0f..100f,
                    modifier = Modifier.weight(3f),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("max", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = resistMax.toFloat(),
                    onValueChange = {
                        val v = it.toInt().coerceAtLeast(resistMin)
                        resistMax = v
                    },
                    valueRange = 0f..100f,
                    modifier = Modifier.weight(3f),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Text("Attunement", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ChoiceChipRow(
                options = PveRules.ATTUNEMENT_FLAVORS,
                selected = attunement,
                onSelect = { attunement = it },
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Staggered", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Switch(checked = staggered, onCheckedChange = { staggered = it })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Astral", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Switch(checked = astral, onCheckedChange = { astral = it })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Magma Guard", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Switch(checked = magma, onCheckedChange = { magma = it })
        }

        HorizontalDivider()
        Text("Results", style = MaterialTheme.typography.titleMedium)
        val r = result
        if (r == null) {
            Text(
                "Pick an enemy or enable Custom to see results.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            DetailRow("Monster power mult", "×${num(r.powerMultiplier)}")
            DetailRow("DVM applied", "${num(r.effectiveDvm)}%")
            DetailRow("Raw hit", num(r.rawHit))
            DetailRow("Damage per hit", "${num(r.damage[0])} – ${num(r.damage[1])}")
            DetailRow("Hits to kill", "${num(r.hits[0], 0)} – ${num(r.hits[1], 0)}")
        }
    }
}