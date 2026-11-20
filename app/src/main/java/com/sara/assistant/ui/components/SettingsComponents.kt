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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
        shape = RoundedCornerShape(18.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.accentPrimary.copy(alpha = 0.14f)),
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
            HorizontalDivider(color = colors.border)
            Spacer(modifier = Modifier.height(6.dp))
            content()
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
                checkedThumbColor = colors.canvas,
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
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = colors.accentPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            )
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
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.elevated)
                    .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedLabel,
                    style = MaterialTheme.typography.labelMedium.copy(color = colors.textPrimary)
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
                .clip(RoundedCornerShape(12.dp))
                .background(colors.canvas)
                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { (option, label) ->
                val isSelected = option == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) colors.accentPrimary.copy(alpha = 0.18f)
                            else Color.Transparent
                        )
                        .clickable { onSelect(option) }
                        .padding(vertical = 9.dp, horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = if (isSelected) colors.accentPrimary else colors.textSecondary,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
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

        TextButton(
            onClick = onAction,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(colors.elevated)
                .border(1.dp, colors.border, RoundedCornerShape(10.dp))
        ) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = colors.accentPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}
