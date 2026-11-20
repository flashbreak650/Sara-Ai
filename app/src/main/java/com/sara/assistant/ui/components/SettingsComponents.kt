package com.sara.assistant.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sara.assistant.ui.theme.LocalSaraColors
import com.sara.assistant.ui.theme.StatusAmber
import com.sara.assistant.ui.theme.StatusDanger
import com.sara.assistant.ui.theme.StatusNominal

@Composable
fun SettingsCategoryCard(
    icon: ImageVector,
    titleBn: String,
    subtitleEn: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalSaraColors.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = colors.surface.copy(alpha = 0.90f),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    colors.accentPrimary.copy(alpha = 0.34f),
                    colors.border,
                    colors.accentSecondary.copy(alpha = 0.24f)
                )
            )
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    colors.accentPrimary.copy(alpha = 0.22f),
                                    colors.accentSecondary.copy(alpha = 0.16f)
                                )
                            )
                        )
                        .border(1.dp, colors.accentPrimary.copy(alpha = 0.28f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = subtitleEn,
                        tint = colors.accentPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = titleBn,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = subtitleEn,
                        style = MaterialTheme.typography.labelMedium.copy(color = colors.textSecondary)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = colors.border.copy(alpha = 0.8f))
            Spacer(modifier = Modifier.height(6.dp))
            content()
        }
    }
}

@Composable
fun VoiceProviderCard(
    titleBn: String,
    titleEn: String,
    descriptionBn: String,
    badge: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalSaraColors.current
    val borderBrush = if (selected) {
        Brush.linearGradient(listOf(colors.accentPrimary, colors.accentSecondary))
    } else {
        Brush.linearGradient(listOf(colors.border, colors.border))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) colors.elevated.copy(alpha = 0.92f) else colors.canvas.copy(alpha = 0.72f),
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderBrush)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (selected) colors.accentPrimary else colors.textMuted,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = titleBn,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (selected) colors.accentPrimary else colors.textSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Text(
                    text = titleEn,
                    style = MaterialTheme.typography.labelMedium.copy(color = colors.accentPrimary)
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = descriptionBn,
                    style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                )
            }
        }
    }
}

@Composable
fun SettingsSwitchRow(
    titleBn: String,
    subtitleEn: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalSaraColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = titleBn,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = if (enabled) colors.textPrimary else colors.textMuted,
                    fontWeight = FontWeight.Medium
                )
            )
            Text(
                text = subtitleEn,
                style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary)
            )
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF060B18),
                checkedTrackColor = colors.accentPrimary,
                uncheckedThumbColor = colors.textSecondary,
                uncheckedTrackColor = colors.elevated,
                uncheckedBorderColor = colors.border
            )
        )
    }
}

@Composable
fun SettingsSliderRow(
    titleBn: String,
    subtitleEn: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    val colors = LocalSaraColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titleBn,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = subtitleEn,
                    style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary)
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.accentPrimary.copy(alpha = 0.14f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = valueLabel,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = colors.accentPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = colors.accentPrimary,
                activeTrackColor = colors.accentPrimary,
                inactiveTrackColor = colors.elevated
            )
        )
    }
}

@Composable
fun <T> SettingsDropdownRow(
    titleBn: String,
    subtitleEn: String,
    selectedLabel: String,
    options: List<Pair<T, String>>,
    onSelect: (T) -> Unit
) {
    val colors = LocalSaraColors.current
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = titleBn,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Medium
                )
            )
            Text(
                text = subtitleEn,
                style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary)
            )
        }

        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.elevated)
                    .border(1.dp, colors.accentPrimary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedLabel,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = "Select option",
                    tint = colors.accentPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = colors.elevated
            ) {
                options.forEach { (item, label) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary)
                            )
                        },
                        onClick = {
                            expanded = false
                            onSelect(item)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun <T> SettingsSegmentedRow(
    titleBn: String,
    subtitleEn: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    val colors = LocalSaraColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = titleBn,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = colors.textPrimary,
                fontWeight = FontWeight.Medium
            )
        )
        Text(
            text = subtitleEn,
            style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.canvas.copy(alpha = 0.85f))
                .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { (option, label) ->
                val isSelected = option == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) {
                                Brush.horizontalGradient(
                                    listOf(
                                        colors.accentPrimary.copy(alpha = 0.24f),
                                        colors.accentSecondary.copy(alpha = 0.18f)
                                    )
                                )
                            } else {
                                Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) colors.accentPrimary.copy(alpha = 0.45f) else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelect(option) }
                        .padding(vertical = 9.dp, horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = if (isSelected) colors.accentPrimary else colors.textSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

enum class PermissionTone {
    GRANTED,
    WARNING,
    UNAVAILABLE
}

@Composable
fun SettingsPermissionRow(
    titleBn: String,
    subtitleEn: String,
    statusLabel: String,
    tone: PermissionTone,
    actionLabel: String,
    onAction: () -> Unit
) {
    val colors = LocalSaraColors.current
    val toneColor = when (tone) {
        PermissionTone.GRANTED -> StatusNominal
        PermissionTone.WARNING -> StatusAmber
        PermissionTone.UNAVAILABLE -> StatusDanger
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 10.dp)
        ) {
            Text(
                text = titleBn,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Medium
                )
            )
            Text(
                text = subtitleEn,
                style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(toneColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelMedium.copy(color = toneColor)
                )
            }
        }

        SaraGlassButton(
            text = actionLabel,
            onClick = onAction
        )
    }
}
