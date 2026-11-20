package com.sara.assistant.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sara.assistant.ui.theme.LocalSaraColors

enum class SaraOrbState {
    IDLE,
    ACTIVE_GREETING,
    LISTENING_READY
}

/**
 * Futuristic animated SARA AI Orb rendered cleanly via Jetpack Compose Canvas.
 * Respects user Accent Color and Animations preference from Settings.
 */
@Composable
fun SaraOrb(
    state: SaraOrbState,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    orbSize: Dp = 216.dp,
    animationsEnabled: Boolean = true
) {
    val colors = LocalSaraColors.current
    val primaryGlow = colors.accentPrimary
    val secondaryGlow = colors.accentSecondary

    val infiniteTransition = rememberInfiniteTransition(label = "sara_orb_loop")

    val rotationOuterAnimated by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_ring_rotation"
    )

    val rotationInnerAnimated by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_ring_rotation"
    )

    val breatheAnimated by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_breathe"
    )

    val rotationOuter = if (animationsEnabled) rotationOuterAnimated else 24f
    val rotationInner = if (animationsEnabled) rotationInnerAnimated else 140f
    val breathe = if (animationsEnabled) breatheAnimated else 1.0f

    val stateScale by animateFloatAsState(
        targetValue = when (state) {
            SaraOrbState.IDLE -> 1.0f
            SaraOrbState.ACTIVE_GREETING -> 1.08f
            SaraOrbState.LISTENING_READY -> 1.12f
        },
        animationSpec = tween(durationMillis = if (animationsEnabled) 320 else 0, easing = FastOutSlowInEasing),
        label = "state_scale"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(orbSize)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onTap
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(orbSize)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension * 0.28f * breathe * stateScale

            // 1. Ambient outer radial aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryGlow.copy(alpha = if (state == SaraOrbState.IDLE) 0.20f else 0.34f),
                        secondaryGlow.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension * 0.50f
                ),
                radius = size.minDimension * 0.50f,
                center = center
            )

            // 2. Outer primary orbital arc
            val outerArcRadius = size.minDimension * 0.42f
            rotate(degrees = rotationOuter, pivot = center) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            primaryGlow.copy(alpha = 0.75f),
                            primaryGlow,
                            Color.Transparent
                        ),
                        center = center
                    ),
                    startAngle = 0f,
                    sweepAngle = 220f,
                    useCenter = false,
                    topLeft = Offset(center.x - outerArcRadius, center.y - outerArcRadius),
                    size = Size(outerArcRadius * 2f, outerArcRadius * 2f),
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 3. Inner counter-rotating secondary arc
            val innerArcRadius = size.minDimension * 0.35f
            rotate(degrees = rotationInner, pivot = center) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            secondaryGlow.copy(alpha = 0.85f),
                            secondaryGlow,
                            Color.Transparent
                        ),
                        center = center
                    ),
                    startAngle = 40f,
                    sweepAngle = 190f,
                    useCenter = false,
                    topLeft = Offset(center.x - innerArcRadius, center.y - innerArcRadius),
                    size = Size(innerArcRadius * 2f, innerArcRadius * 2f),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 4. Precision core ring
            drawCircle(
                color = primaryGlow.copy(alpha = 0.40f),
                radius = baseRadius * 1.12f,
                center = center,
                style = Stroke(width = 1.2.dp.toPx())
            )

            // 5. Luminous core sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.90f),
                        primaryGlow.copy(alpha = 0.82f),
                        secondaryGlow.copy(alpha = 0.48f),
                        colors.canvas
                    ),
                    center = Offset(center.x - baseRadius * 0.22f, center.y - baseRadius * 0.22f),
                    radius = baseRadius * 1.25f
                ),
                radius = baseRadius,
                center = center
            )
        }
    }
}
