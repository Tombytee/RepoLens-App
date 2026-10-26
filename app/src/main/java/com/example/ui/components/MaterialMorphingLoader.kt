package com.example.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.M3VioletContainer
import com.example.ui.theme.M3VioletPrimary
import com.example.ui.theme.PlusJakartaSansFamily
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Organic Shape-Morphing Material 3 Expressive Loading Indicator.
 * Exactly implements the organic scalloped star/flower/blob morphing animation
 * demonstrated in the reference design with soft lavender halo and solid deep violet core.
 */
@Composable
fun MaterialMorphingLoader(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    showHalo: Boolean = true,
    strokeWidth: Dp = 0.dp,
    primaryColor: Color = M3VioletPrimary,
    haloColor: Color = M3VioletContainer
) {
    val infiniteTransition = rememberInfiniteTransition(label = "m3_organic_morph")

    // Slow, soothing continuous rotation
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Morph cycle transitioning smoothly between organic lobes (10-point scallop -> 7-point scallop -> 4-point organic blob -> 12-point scallop)
    val morphProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 3600,
                easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "morphProgress"
    )

    // Organic breathing scale
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = CubicBezierEasing(0.35f, 0.0f, 0.65f, 1.0f)),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Canvas(
        modifier = modifier.size(size)
    ) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val outerRadius = this.size.minDimension / 2f

        // Draw soft lavender halo circle (as shown on the right side of the reference image)
        if (showHalo) {
            drawCircle(
                color = haloColor,
                radius = outerRadius,
                center = center
            )
        }

        // Inner scalloped shape radius (around 68% of outer radius if halo is present, or 90% if solo)
        val innerBaseRadius = if (showHalo) outerRadius * 0.68f * pulseScale else outerRadius * 0.90f * pulseScale

        rotate(rotation, pivot = center) {
            val path = Path()
            generateOrganicScallopPath(path, center, innerBaseRadius, morphProgress)
            drawPath(
                path = path,
                color = primaryColor,
                style = Fill
            )
        }
    }
}

/**
 * Centered state container using the organic shape-morphing loader with an optional label.
 */
@Composable
fun MorphingLoadingBox(
    modifier: Modifier = Modifier,
    message: String? = null,
    size: Dp = 52.dp
) {
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            MaterialMorphingLoader(
                size = size,
                showHalo = true
            )
            if (!message.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.5.sp
                    )
                )
            }
        }
    }
}

/**
 * Generates an organic, continuously morphing multi-lobed scalloped path
 * using radial harmonics connected with smooth Catmull-Rom cubic Béziers.
 */
private fun generateOrganicScallopPath(
    path: Path,
    center: Offset,
    baseRadius: Float,
    morphProgress: Float
) {
    path.reset()
    val nodeCount = 36
    val step = (2 * PI / nodeCount).toFloat()

    val phase = morphProgress % 4f
    val currentPhaseIndex = phase.toInt()
    val t = phase - currentPhaseIndex

    // Smooth hermite ease for organic liquid blending
    val blend = t * t * (3f - 2f * t)

    val points = ArrayList<Offset>(nodeCount)

    for (i in 0 until nodeCount) {
        val angle = i * step

        val rCurrent = computeScallopRadius(angle, baseRadius, currentPhaseIndex)
        val rNext = computeScallopRadius(angle, baseRadius, (currentPhaseIndex + 1) % 4)
        val r = rCurrent + (rNext - rCurrent) * blend

        val x = center.x + r * cos(angle)
        val y = center.y + r * sin(angle)
        points.add(Offset(x, y))
    }

    if (points.isNotEmpty()) {
        val first = points[0]
        path.moveTo(first.x, first.y)

        for (i in 0 until nodeCount) {
            val p0 = points[(i - 1 + nodeCount) % nodeCount]
            val p1 = points[i]
            val p2 = points[(i + 1) % nodeCount]
            val p3 = points[(i + 2) % nodeCount]

            // Convert Catmull-Rom points to smooth Cubic Bezier control points
            val cp1X = p1.x + (p2.x - p0.x) / 6f
            val cp1Y = p1.y + (p2.y - p0.y) / 6f

            val cp2X = p2.x - (p3.x - p1.x) / 6f
            val cp2Y = p2.y - (p3.y - p1.y) / 6f

            path.cubicTo(cp1X, cp1Y, cp2X, cp2Y, p2.x, p2.y)
        }
        path.close()
    }
}

/**
 * Computes organic scalloped radial wave at angle:
 * Phase 0: 10-pointed scalloped star (exact match to reference image left shape)
 * Phase 1: 12-pointed rounded wavy starburst
 * Phase 2: 7-pointed organic rounded flower
 * Phase 3: 4-pointed organic rounded pill blob
 */
private fun computeScallopRadius(angle: Float, baseRadius: Float, phase: Int): Float {
    return when (phase) {
        0 -> {
            // 10-point scalloped star from reference image
            val wave = cos(10f * angle)
            baseRadius * (0.86f + 0.18f * wave)
        }
        1 -> {
            // 12-point wave starburst
            val wave = cos(12f * angle)
            baseRadius * (0.88f + 0.15f * wave)
        }
        2 -> {
            // 7-point rounded flower
            val wave = cos(7f * angle)
            baseRadius * (0.87f + 0.17f * wave)
        }
        3 -> {
            // 4-point soft rounded pill blob
            val wave = cos(4f * angle)
            baseRadius * (0.90f + 0.14f * wave)
        }
        else -> baseRadius
    }
}
