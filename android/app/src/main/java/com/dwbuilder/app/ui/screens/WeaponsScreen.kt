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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dwbuilder.app.data.GameData
import com.dwbuilder.app.domain.DamageRules
import com.dwbuilder.app.domain.model.Weapon
import com.dwbuilder.app.ui.BuilderViewModel
import com.dwbuilder.app.ui.components.ChoiceChipRow
import com.dwbuilder.app.ui.components.DetailRow
import com.dwbuilder.app.ui.components.DropdownCell
import com.dwbuilder.app.ui.components.RarityChip
import com.dwbuilder.app.ui.components.SectionHeader
import com.dwbuilder.app.ui.components.StatStepper
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
    val breakdown = selected?.let { w ->
        DamageRules.compute(DamageRules.DamageSource.weapon(w), build, data.mods, tuning = build.tuning)
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
        items(weapons, key = { it.name }) { w ->
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
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
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
    }
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
        }
    }
}