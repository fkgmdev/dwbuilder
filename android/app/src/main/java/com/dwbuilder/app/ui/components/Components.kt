package com.dwbuilder.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dwbuilder.app.ui.rarityColor
import java.util.Locale

/** Section label, e.g. "ATTRIBUTES". */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier,
    )
}

/** A labeled +/- stepper used for every point value in the builder. */
@Composable
fun StatStepper(
    label: String,
    value: Int,
    accent: Color? = null,
    plusEnabled: Boolean = true,
    minusEnabled: Boolean = true,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = accent ?: MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier.fillMaxWidth().height(44.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        FilledTonalIconButton(onClick = onMinus, enabled = minusEnabled, modifier = Modifier.size(30.dp)) {
            Text("−", style = MaterialTheme.typography.titleMedium)
        }
        Box(Modifier.width(48.dp), contentAlignment = Alignment.Center) {
            Text("$value", style = MaterialTheme.typography.titleMedium)
        }
        FilledTonalIconButton(onClick = onPlus, enabled = plusEnabled, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Filled.Add, contentDescription = "Add")
        }
    }
}

/** A labeled dropdown backed by [DropdownMenu] (avoids the experimental exposed API). */
@Composable
fun <T> DropdownCell(
    label: String,
    selected: String,
    options: List<T>,
    labelOf: (T) -> String = { it.toString() },
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedCard(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    selected,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(labelOf(opt)) },
                    onClick = {
                        expanded = false
                        onSelect(opt)
                    },
                )
            }
        }
    }
}

/** A horizontally scrollable row of single-select chips. */
@Composable
fun ChoiceChipRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { opt ->
            FilterChip(selected = opt == selected, onClick = { onSelect(opt) }, label = { Text(opt) })
        }
    }
}

/** Small colored chip showing a catalog rarity. */
@Composable
fun RarityChip(rarity: String?, modifier: Modifier = Modifier) {
    val r = rarity ?: "Common"
    Surface(
        color = rarityColor(r).copy(alpha = 0.20f),
        contentColor = rarityColor(r),
        shape = MaterialTheme.shapes.small,
        modifier = modifier,
    ) {
        Text(r, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

/** Label/value line used by the weapon breakdown. */
@Composable
fun DetailRow(label: String, value: String, valueColor: Color? = null, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, color = valueColor ?: MaterialTheme.colorScheme.onSurface)
    }
}

/** 2-decimal (or integer) number formatting for damage values. */
fun num(value: Double, decimals: Int = 2): String {
    val format = if (decimals <= 0) "%.0f" else "%.${decimals}f"
    return String.format(Locale.US, format, value)
}

/**
 * Flat, shadow-free card used for catalog list rows. Zero elevation keeps
 * scrolling cheap (no per-row shadow rendering), which matters for the
 * ~1150-talent catalog.
 */
@Composable
fun ListRowCard(
    selected: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = modifier)
}