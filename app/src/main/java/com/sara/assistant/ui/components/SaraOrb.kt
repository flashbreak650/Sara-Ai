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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
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
import kotlin.math.cos
import kotlin.math.sin

enum class SaraOrbState {
    IDLE,
    ACTIVE_GREETING,
    LISTENING,
    SPEAKING
}

private data class VortexParticle(
    val baseAngleRad: Float,
    val radiusFactor: Float,
    val speedFactor: Float,
    val dotDp: Float,
    val alpha: Float
)

/**
 * Glowing multi-ring Energy Vortex Orb with orbiting particle sparks and center Mic trigger,
 * reacting smoothly to IDLE, ACTIVE_GREETING, LISTENING, and SPEAKING states.
 */
@Composable
fun SaraOrb(
    state: SaraOrbState,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    orbSize: Dp = 204.dp,
    animationsEnabled: Boolean = true,
    showCenterIcon: Boolean = true
) {
    val colors = LocalSaraColors.current
    val primaryGlow = colors.accentPrimary
    val secondaryGlow = colors.accentSecondary

    val infiniteTransition = rememberInfiniteTransition(label = "sara_vortex_loop")

    val spinBaseAnimated by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    SaraOrbState.SPEAKING -> 4200
                    SaraOrbState.LISTENING -> 6200
                    else -> 11500
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "vortex_spin_outer"
    )

    val spinInnerAnimated by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    SaraOrbState.SPEAKING -> 3100
                    SaraOrbState.LISTENING -> 4800
                    else -> 8200
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "vortex_spin_inner"
    )

    val breatheAnimated by infiniteTransition.animateFloat(
        initialValue = if (state == SaraOrbState.SPEAKING) 0.93f else 0.96f,
        targetValue = if (state == SaraOrbState.SPEAKING) 1.09f else 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == SaraOrbState.SPEAKING) 520 else 2400,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vortex_breathe"
    )

    val spinOuter = if (animationsEnabled) spinBaseAnimated else 24f
    val spinInner = if (animationsEnabled) spinInnerAnimated else 140f
    val breathe = if (animationsEnabled) breatheAnimated else 1.0f

    val stateScale by animateFloatAsState(
        targetValue = when (state) {
            SaraOrbState.IDLE -> 1.0f
            SaraOrbState.ACTIVE_GREETING -> 1.05f
            SaraOrbState.LISTENING -> 1.09f
            SaraOrbState.SPEAKING -> 1.12f
        },
        animationSpec = tween(durationMillis = if (animationsEnabled) 240 else 0, easing = FastOutSlowInEasing),
        label = "vortex_state_scale"
    )

    val particles = remember {
        List(16) { i ->
            VortexParticle(
                baseAngleRad = (i.toFloat() / 16f) * (Math.PI * 2.0).toFloat(),
                radiusFactor = 0.30f + ((i * 7) % 11) * 0.014f,
                speedFactor = if (i % 2 == 0) 1f else -1f,
                dotDp = 1.2f + (i % 3) * 0.7f,
                alpha = 0.45f + (i % 4) * 0.14f
            )
        }
    }

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
            val coreRadius = size.minDimension * 0.215f * breathe * stateScale

            // 1. Deep Atmospheric Halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryGlow.copy(alpha = if (state == SaraOrbState.IDLE) 0.34f else 0.54f),
                        primaryGlow.copy(alpha = 0.14f),
                        secondaryGlow.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension * 0.50f
                ),
                radius = size.minDimension * 0.50f,
                center = center
            )

            // 2. Concentric Vortex Rings (4 counter-rotating energy layers)
            val ringRadii = floatArrayOf(0.45f, 0.405f, 0.36f, 0.315f)
            val ringWidths = floatArrayOf(1.2f, 1.8f, 1.6f, 1.4f)
            val ringSweeps = floatArrayOf(210f, 240f, 190f, 255f)
            val ringAlphas = floatArrayOf(0.42f, 0.72f, 0.86f, 0.64f)

            for (i in ringRadii.indices) {
                val r = size.minDimension * ringRadii[i]
                val angle = if (i % 2 == 0) spinOuter + i * 48f else spinInner - i * 35f
                rotate(degrees = angle, pivot = center) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color.Transparent,
                                primaryGlow.copy(alpha = ringAlphas[i]),
                                Color.White.copy(alpha = ringAlphas[i] * 0.65f),
                                Color.Transparent
                            ),
                            center = center
                        ),
                        startAngle = 0f,
                        sweepAngle = ringSweeps[i],
                        useCenter = false,
                        topLeft = Offset(center.x - r, center.y - r),
                        size = Size(r * 2f, r * 2f),
                        style = Stroke(width = ringWidths[i].dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = secondaryGlow.copy(alpha = ringAlphas[i] * 0.45f),
                        startAngle = 195f,
                        sweepAngle = ringSweeps[i] * 0.52f,
                        useCenter = false,
                        topLeft = Offset(center.x - r + 2.dp.toPx(), center.y - r + 2.dp.toPx()),
                        size = Size((r - 2.dp.toPx()) * 2f, (r - 2.dp.toPx()) * 2f),
                        style = Stroke(width = (ringWidths[i] * 0.8f).dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // 3. Orbiting Spark Particles
            val angleRadSpin = Math.toRadians(spinOuter.toDouble()).toFloat()
            particles.forEachIndexed { idx, p ->
                val a = p.baseAngleRad + angleRadSpin * p.speedFactor
                val dist = size.minDimension * p.radiusFactor
                val px = center.x + cos(a) * dist
                val py = center.y + sin(a) * dist
                drawCircle(
                    color = if (idx % 3 == 0) Color(0xFFFFDCDC).copy(alpha = p.alpha) else primaryGlow.copy(alpha = p.alpha),
                    radius = p.dotDp.dp.toPx(),
                    center = Offset(px, py)
                )
            }

            // 4. Crisp Rim & Radiant Core Sphere
            drawCircle(
                color = Color.White.copy(alpha = 0.35f),
                radius = coreRadius * 1.09f,
                center = center,
                style = Stroke(width = 1.3.dp.toPx())
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFEEEE),
                        primaryGlow,
                        secondaryGlow,
                        Color(0xFF14080C)
                    ),
                    center = Offset(center.x - coreRadius * 0.18f, center.y - coreRadius * 0.22f),
                    radius = coreRadius * 1.06f
                ),
                radius = coreRadius,
                center = center
            )
        }

        if (showCenterIcon) {
            Icon(
                imageVector = if (state == SaraOrbState.SPEAKING) Icons.Rounded.GraphicEq else Icons.Rounded.Mic,
                contentDescription = "Talk to SARA",
                tint = Color.White,
                modifier = Modifier.size(orbSize * 0.145f)
            )
        }
    }
}
