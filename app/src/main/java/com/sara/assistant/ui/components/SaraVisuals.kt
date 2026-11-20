package com.sara.assistant.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sara.assistant.R
import com.sara.assistant.ui.theme.LocalSaraColors
import com.sara.assistant.ui.theme.StatusNominal
import kotlin.math.sin

/**
 * Custom geometric SARA brand emblem rendered via Compose Canvas.
 */
@Composable
fun SaraLogo(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp
) {
    val colors = LocalSaraColors.current
    Canvas(modifier = modifier.size(size)) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val r = this.size.minDimension * 0.44f

        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(colors.accentPrimary, colors.accentSecondary)
            ),
            radius = r,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )

        val diamond = Path().apply {
            val d = r * 0.54f
            moveTo(center.x, center.y - d)
            lineTo(center.x + d, center.y)
            lineTo(center.x, center.y + d)
            lineTo(center.x - d, center.y)
            close()
        }
        drawPath(
            path = diamond,
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.92f), colors.accentPrimary, colors.accentSecondary),
                center = center,
                radius = r * 0.8f
            )
        )
    }
}

/**
 * Animated glowing circular portrait frame showcasing SARA's futuristic AI character portrait.
 */
@Composable
fun SaraAvatarFrame(
    size: Dp,
    animationsEnabled: Boolean = true,
    showOnlineBadge: Boolean = true,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalSaraColors.current
    val infinite = rememberInfiniteTransition(label = "avatar_halo")
    val angleAnimated by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "avatar_ring_angle"
    )
    val haloAngle = if (animationsEnabled) angleAnimated else 30f

    Box(
        modifier = modifier
            .size(size)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val outerR = this.size.minDimension * 0.48f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colors.accentPrimary.copy(alpha = 0.34f),
                        colors.accentSecondary.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = this.size.minDimension * 0.52f
                ),
                radius = this.size.minDimension * 0.52f,
                center = center
            )

            rotate(degrees = haloAngle, pivot = center) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            colors.accentPrimary,
                            colors.accentSecondary,
                            Color.Transparent,
                            colors.accentPrimary
                        ),
                        center = center
                    ),
                    startAngle = 0f,
                    sweepAngle = 300f,
                    useCenter = false,
                    topLeft = Offset(center.x - outerR, center.y - outerR),
                    size = Size(outerR * 2f, outerR * 2f),
                    style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        Image(
            painter = painterResource(id = R.drawable.sara_avatar),
            contentDescription = "SARA AI Portrait",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size * 0.84f)
                .clip(CircleShape)
                .border(1.5.dp, colors.accentPrimary.copy(alpha = 0.65f), CircleShape)
        )

        if (showOnlineBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = size * 0.08f, bottom = size * 0.08f)
                    .size((size.value * 0.18f).coerceIn(8f, 16f).dp)
                    .clip(CircleShape)
                    .background(StatusNominal)
                    .border(2.dp, colors.canvas, CircleShape)
            )
        }
    }
}

/**
 * Glowing multi-bar voice waveform synchronized to IDLE / LISTENING / SPEAKING states.
 */
@Composable
fun VoiceWaveform(
    state: SaraOrbState,
    animationsEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val colors = LocalSaraColors.current
    val infinite = rememberInfiniteTransition(label = "voice_waveform")
    val phaseAnimated by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2.0).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    SaraOrbState.SPEAKING -> 700
                    SaraOrbState.LISTENING -> 900
                    else -> 2400
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )
    val phase = if (animationsEnabled) phaseAnimated else 0.7f

    val intensity by animateFloatAsState(
        targetValue = when (state) {
            SaraOrbState.SPEAKING -> 1.0f
            SaraOrbState.LISTENING -> 0.85f
            SaraOrbState.ACTIVE_GREETING -> 0.68f
            SaraOrbState.IDLE -> 0.25f
        },
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "wave_intensity"
    )

    Canvas(
        modifier = modifier
            .width(184.dp)
            .height(26.dp)
    ) {
        val barCount = 19
        val barWidth = 3.5.dp.toPx()
        val gap = (size.width - barCount * barWidth) / (barCount - 1)
        val centerIdx = (barCount - 1) / 2f

        for (i in 0 until barCount) {
            val distNorm = 1f - kotlin.math.abs(i - centerIdx) / (centerIdx + 1f)
            val wave = (sin(phase + i * 0.65f) * 0.5f + 0.5f)
            val heightRatio = (0.16f + 0.84f * wave * distNorm * intensity).coerceIn(0.14f, 1.0f)
            val barHeight = size.height * heightRatio
            val x = i * (barWidth + gap)
            val y = (size.height - barHeight) / 2f

            val barColor = if (i % 2 == 0) colors.accentPrimary else Color(0xFFFCA5A5)
            drawRoundRect(
                color = barColor.copy(alpha = 0.35f + 0.65f * intensity),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

/**
 * Obsidian glowing primary gradient button with tactile press-scale feedback.
 */
@Composable
fun SaraPrimaryGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val colors = LocalSaraColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.96f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "btn_scale"
    )

    val shape = RoundedCornerShape(14.dp)
    val gradient = if (enabled) {
        Brush.horizontalGradient(
            colors = listOf(colors.accentPrimary, colors.accentSecondary)
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(colors.elevated, colors.elevated)
        )
    }

    Row(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = if (enabled) 10.dp else 0.dp,
                shape = shape,
                ambientColor = colors.accentPrimary.copy(alpha = 0.55f),
                spotColor = colors.accentPrimary.copy(alpha = 0.55f)
            )
            .clip(shape)
            .background(gradient)
            .border(
                width = 1.dp,
                color = if (enabled) Color.White.copy(alpha = 0.18f) else colors.border,
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) Color.White else colors.textMuted,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                color = if (enabled) Color.White else colors.textMuted,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

/**
 * Matte dark obsidian glass button with subtle aura border.
 */
@Composable
fun SaraGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    tint: Color? = null
) {
    val colors = LocalSaraColors.current
    val resolvedTint = tint ?: colors.accentPrimary
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.96f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "glass_btn_scale"
    )
    val shape = RoundedCornerShape(14.dp)

    Row(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        colors.elevated,
                        colors.surface
                    )
                )
            )
            .border(
                width = 1.dp,
                color = if (enabled) resolvedTint.copy(alpha = 0.34f) else colors.border,
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) resolvedTint else colors.textMuted,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                color = if (enabled) colors.textPrimary else colors.textMuted,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

/**
 * Dark obsidian glowing circular icon trigger.
 */
@Composable
fun SaraIconGlowButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    highlighted: Boolean = false
) {
    val colors = LocalSaraColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "icon_btn_scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .shadow(
                elevation = if (highlighted) 10.dp else 4.dp,
                shape = CircleShape,
                ambientColor = colors.accentPrimary.copy(alpha = 0.45f),
                spotColor = colors.accentPrimary.copy(alpha = 0.45f)
            )
            .clip(CircleShape)
            .background(
                if (highlighted) {
                    Brush.linearGradient(listOf(colors.accentPrimary, colors.accentSecondary))
                } else {
                    Brush.linearGradient(
                        listOf(
                            colors.elevated,
                            colors.surface
                        )
                    )
                }
            )
            .border(
                width = 1.dp,
                color = if (highlighted) Color.White.copy(alpha = 0.25f) else colors.accentPrimary.copy(alpha = 0.34f),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (highlighted) Color.White else colors.accentPrimary,
            modifier = Modifier.size(size * 0.46f)
        )
    }
}

enum class SaraNavTab {
    HOME,
    INBOX,
    MEMORY,
    SETTINGS
}

/**
 * Obsidian Bottom Navigation Bar with floating raised Center Glowing Orb trigger,
 * matching the screenshot's bottom bar architecture.
 */
@Composable
fun SaraBottomDockBar(
    activeTab: SaraNavTab,
    unreadInboxCount: Int,
    orbActive: Boolean,
    animationsEnabled: Boolean,
    onHomeClick: () -> Unit,
    onInboxClick: () -> Unit,
    onCenterOrbClick: () -> Unit,
    onMemoryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSaraColors.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
            color = colors.surface.copy(alpha = 0.96f),
            border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.20f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DockNavItem(
                    icon = Icons.Rounded.Home,
                    label = "Home",
                    selected = activeTab == SaraNavTab.HOME,
                    onClick = onHomeClick
                )
                DockNavItem(
                    icon = Icons.Rounded.Inbox,
                    label = "Inbox",
                    selected = activeTab == SaraNavTab.INBOX,
                    badgeCount = unreadInboxCount,
                    onClick = onInboxClick
                )
                Spacer(modifier = Modifier.width(64.dp))
                DockNavItem(
                    icon = Icons.Rounded.Storage,
                    label = "Memory",
                    selected = activeTab == SaraNavTab.MEMORY,
                    onClick = onMemoryClick
                )
                DockNavItem(
                    icon = Icons.Rounded.Settings,
                    label = "Aura",
                    selected = activeTab == SaraNavTab.SETTINGS,
                    onClick = onSettingsClick
                )
            }
        }

        // Raised Glowing Center Orb Button
        Box(
            modifier = Modifier
                .offset(y = (-14).dp)
                .size(56.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    ambientColor = colors.accentPrimary,
                    spotColor = colors.accentPrimary
                )
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFE5E5),
                            colors.accentPrimary,
                            colors.accentSecondary,
                            Color(0xFF1A070C)
                        )
                    )
                )
                .border(
                    width = if (orbActive) 2.dp else 1.5.dp,
                    color = Color.White.copy(alpha = if (orbActive) 0.85f else 0.42f),
                    shape = CircleShape
                )
                .clickable(onClick = onCenterOrbClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = "Quick Talk to SARA",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun DockNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    badgeCount: Int = 0,
    onClick: () -> Unit
) {
    val colors = LocalSaraColors.current
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) colors.accentPrimary else colors.textSecondary,
                modifier = Modifier.size(21.dp)
            )
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 5.dp, y = (-3).dp)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(colors.accentPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (badgeCount > 9) "9+" else badgeCount.toString(),
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (selected) colors.accentPrimary else colors.textMuted,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 10.sp
            )
        )
    }
}
