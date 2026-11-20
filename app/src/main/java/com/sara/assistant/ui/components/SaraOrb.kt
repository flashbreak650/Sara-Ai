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
    LISTENING,
    SPEAKING
}

/**
 * Large 3D-style futuristic SARA AI Orb with multi-axis orbital arcs,
 * volumetric specular core, and reactive listening / speaking pulses.
 */
@Composable
fun SaraOrb(
    state: SaraOrbState,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    orbSize: Dp = 224.dp,
    animationsEnabled: Boolean = true
) {
    val colors = LocalSaraColors.current
    val primaryGlow = colors.accentPrimary
    val secondaryGlow = colors.accentSecondary
    val electricBlue = Color(0xFF38BDF8)

    val infiniteTransition = rememberInfiniteTransition(label = "sara_orb_loop")

    val rotationOuterAnimated by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    SaraOrbState.SPEAKING -> 5500
                    SaraOrbState.LISTENING -> 7500
                    else -> 14000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_ring_rotation"
    )

    val rotationInnerAnimated by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    SaraOrbState.SPEAKING -> 4200
                    SaraOrbState.LISTENING -> 5800
                    else -> 9000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_ring_rotation"
    )

    val breatheAnimated by infiniteTransition.animateFloat(
        initialValue = if (state == SaraOrbState.SPEAKING) 0.90f else 0.95f,
        targetValue = if (state == SaraOrbState.SPEAKING) 1.11f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == SaraOrbState.SPEAKING) 620 else 2600,
                easing = FastOutSlowInEasing
            ),
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
            SaraOrbState.ACTIVE_GREETING -> 1.06f
            SaraOrbState.LISTENING -> 1.11f
            SaraOrbState.SPEAKING -> 1.14f
        },
        animationSpec = tween(durationMillis = if (animationsEnabled) 280 else 0, easing = FastOutSlowInEasing),
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
            val baseRadius = size.minDimension * 0.27f * breathe * stateScale

            // 1. Deep ambient multi-stop aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryGlow.copy(alpha = if (state == SaraOrbState.IDLE) 0.24f else 0.42f),
                        electricBlue.copy(alpha = 0.16f),
                        secondaryGlow.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension * 0.52f
                ),
                radius = size.minDimension * 0.52f,
                center = center
            )

            // 2. Outer primary cyan orbital arc
            val outerArcRadius = size.minDimension * 0.44f
            rotate(degrees = rotationOuter, pivot = center) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            primaryGlow.copy(alpha = 0.8f),
                            electricBlue,
                            Color.Transparent
                        ),
                        center = center
                    ),
                    startAngle = 0f,
                    sweepAngle = 230f,
                    useCenter = false,
                    topLeft = Offset(center.x - outerArcRadius, center.y - outerArcRadius),
                    size = Size(outerArcRadius * 2f, outerArcRadius * 2f),
                    style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 3. Middle electric-blue/violet counter-rotating arc
            val midArcRadius = size.minDimension * 0.38f
            rotate(degrees = rotationInner, pivot = center) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            secondaryGlow.copy(alpha = 0.88f),
                            primaryGlow.copy(alpha = 0.7f),
                            Color.Transparent
                        ),
                        center = center
                    ),
                    startAngle = 40f,
                    sweepAngle = 200f,
                    useCenter = false,
                    topLeft = Offset(center.x - midArcRadius, center.y - midArcRadius),
                    size = Size(midArcRadius * 2f, midArcRadius * 2f),
                    style = Stroke(width = 2.1.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 4. Inner precision glass rim
            drawCircle(
                color = primaryGlow.copy(alpha = 0.45f),
                radius = baseRadius * 1.15f,
                center = center,
                style = Stroke(width = 1.2.dp.toPx())
            )

            // 5. 3D Volumetric Sphere Body
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.94f),
                        primaryGlow.copy(alpha = 0.88f),
                        electricBlue.copy(alpha = 0.65f),
                        secondaryGlow.copy(alpha = 0.52f),
                        colors.canvas
                    ),
                    center = Offset(center.x - baseRadius * 0.28f, center.y - baseRadius * 0.28f),
                    radius = baseRadius * 1.32f
                ),
                radius = baseRadius,
                center = center
            )

            // 6. Specular 3D lens highlight
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.55f),
                        Color.Transparent
                    ),
                    center = Offset(center.x - baseRadius * 0.34f, center.y - baseRadius * 0.34f),
                    radius = baseRadius * 0.42f
                ),
                radius = baseRadius * 0.42f,
                center = Offset(center.x - baseRadius * 0.34f, center.y - baseRadius * 0.34f)
            )
        }
    }
}
