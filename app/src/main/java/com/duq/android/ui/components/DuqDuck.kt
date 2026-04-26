package com.duq.android.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.duq.android.DuqState
import com.duq.android.ui.theme.DuqColors
import kotlin.math.sin

/**
 * DuqDuck - Animated rubber duck mascot
 * Changes behavior based on AI state
 */
@Composable
fun DuqDuck(
    state: DuqState?,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "duckAnimation")

    // Bobbing animation (floating on water)
    val bobSpeed = when (state) {
        DuqState.LISTENING, DuqState.RECORDING -> 600
        DuqState.PROCESSING -> 300
        DuqState.PLAYING -> 800
        else -> 1500
    }

    val bob by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(bobSpeed, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bob"
    )

    // Head tilt for listening state
    val headTilt by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == DuqState.LISTENING || state == DuqState.RECORDING) 400 else 2000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "headTilt"
    )

    // Glow pulse
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    DuqState.PROCESSING -> 400
                    DuqState.PLAYING -> 600
                    else -> 1500
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    // Water ripple animation
    val ripple by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple"
    )

    // State-based glow color
    val glowColor = when (state) {
        DuqState.IDLE -> DuqColors.primary
        DuqState.LISTENING, DuqState.RECORDING -> DuqColors.primaryBright
        DuqState.PROCESSING -> DuqColors.accent
        DuqState.PLAYING -> DuqColors.success
        DuqState.ERROR -> DuqColors.error
        null -> DuqColors.textMuted
    }

    Canvas(modifier = modifier.size(160.dp)) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val duckSize = size.minDimension * 0.7f
        val bobOffset = bob * 8f

        // Outer glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    glowColor.copy(alpha = 0.3f * glowPulse),
                    glowColor.copy(alpha = 0.1f * glowPulse),
                    Color.Transparent
                ),
                center = Offset(centerX, centerY),
                radius = duckSize * 0.9f
            ),
            radius = duckSize * 0.9f,
            center = Offset(centerX, centerY)
        )

        // Water ripples
        val rippleRadius1 = duckSize * 0.5f + ripple * duckSize * 0.4f
        val rippleRadius2 = duckSize * 0.5f + ((ripple + 0.5f) % 1f) * duckSize * 0.4f
        val rippleAlpha1 = (1f - ripple) * 0.3f
        val rippleAlpha2 = (1f - ((ripple + 0.5f) % 1f)) * 0.3f

        drawCircle(
            color = DuqColors.primary.copy(alpha = rippleAlpha1),
            radius = rippleRadius1,
            center = Offset(centerX, centerY + duckSize * 0.25f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )
        drawCircle(
            color = DuqColors.primary.copy(alpha = rippleAlpha2),
            radius = rippleRadius2,
            center = Offset(centerX, centerY + duckSize * 0.25f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )

        // Duck body position
        val bodyY = centerY + bobOffset

        // === DUCK BODY ===
        // Main body (yellow oval)
        val bodyPath = Path().apply {
            val bodyWidth = duckSize * 0.6f
            val bodyHeight = duckSize * 0.45f
            val bodyLeft = centerX - bodyWidth / 2
            val bodyTop = bodyY - bodyHeight / 2

            // Rounded body shape
            moveTo(centerX, bodyTop)
            cubicTo(
                centerX + bodyWidth * 0.6f, bodyTop,
                centerX + bodyWidth * 0.5f, bodyY + bodyHeight * 0.5f,
                centerX, bodyY + bodyHeight * 0.4f
            )
            cubicTo(
                centerX - bodyWidth * 0.5f, bodyY + bodyHeight * 0.5f,
                centerX - bodyWidth * 0.6f, bodyTop,
                centerX, bodyTop
            )
            close()
        }

        // Body shadow
        drawPath(
            path = bodyPath,
            color = DuqColors.primaryDim.copy(alpha = 0.5f),
            style = Fill
        )

        // Body gradient
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    DuqColors.primaryBright,
                    DuqColors.primary,
                    DuqColors.primaryDim
                ),
                center = Offset(centerX - duckSize * 0.1f, bodyY - duckSize * 0.1f),
                radius = duckSize * 0.35f
            ),
            radius = duckSize * 0.28f,
            center = Offset(centerX, bodyY)
        )

        // === DUCK HEAD ===
        rotate(headTilt, pivot = Offset(centerX, bodyY - duckSize * 0.15f)) {
            val headCenterX = centerX
            val headCenterY = bodyY - duckSize * 0.25f
            val headRadius = duckSize * 0.18f

            // Head
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        DuqColors.primaryBright,
                        DuqColors.primary
                    ),
                    center = Offset(headCenterX - headRadius * 0.3f, headCenterY - headRadius * 0.3f),
                    radius = headRadius * 1.2f
                ),
                radius = headRadius,
                center = Offset(headCenterX, headCenterY)
            )

            // === BEAK ===
            val beakPath = Path().apply {
                val beakStartX = headCenterX + headRadius * 0.6f
                val beakY = headCenterY + headRadius * 0.1f
                val beakLength = duckSize * 0.12f
                val beakHeight = duckSize * 0.06f

                moveTo(beakStartX, beakY - beakHeight / 2)
                lineTo(beakStartX + beakLength, beakY)
                lineTo(beakStartX, beakY + beakHeight / 2)
                close()
            }

            drawPath(
                path = beakPath,
                brush = Brush.linearGradient(
                    colors = listOf(DuqColors.accent, DuqColors.accentDim),
                    start = Offset(headCenterX + headRadius, headCenterY),
                    end = Offset(headCenterX + headRadius + duckSize * 0.1f, headCenterY)
                ),
                style = Fill
            )

            // === EYE ===
            val eyeX = headCenterX + headRadius * 0.3f
            val eyeY = headCenterY - headRadius * 0.1f
            val eyeRadius = headRadius * 0.25f

            // Eye white
            drawCircle(
                color = Color.White,
                radius = eyeRadius,
                center = Offset(eyeX, eyeY)
            )

            // Eye pupil (looks in direction based on state)
            val pupilOffsetX = when (state) {
                DuqState.LISTENING, DuqState.RECORDING -> eyeRadius * 0.2f
                DuqState.PROCESSING -> sin(bob * Math.PI.toFloat() * 2) * eyeRadius * 0.3f
                else -> 0f
            }
            drawCircle(
                color = Color.Black,
                radius = eyeRadius * 0.5f,
                center = Offset(eyeX + pupilOffsetX, eyeY)
            )

            // Eye highlight
            drawCircle(
                color = Color.White,
                radius = eyeRadius * 0.2f,
                center = Offset(eyeX - eyeRadius * 0.2f, eyeY - eyeRadius * 0.2f)
            )
        }

        // === WING ===
        val wingPath = Path().apply {
            val wingX = centerX - duckSize * 0.15f
            val wingY = bodyY + duckSize * 0.05f
            val wingWidth = duckSize * 0.15f
            val wingHeight = duckSize * 0.12f

            moveTo(wingX, wingY)
            quadraticBezierTo(
                wingX - wingWidth * 0.5f, wingY + wingHeight,
                wingX + wingWidth, wingY + wingHeight * 0.5f
            )
            quadraticBezierTo(
                wingX + wingWidth * 0.3f, wingY,
                wingX, wingY
            )
            close()
        }

        drawPath(
            path = wingPath,
            color = DuqColors.primaryDim,
            style = Fill
        )

        // Processing indicator (spinning dots around duck)
        if (state == DuqState.PROCESSING) {
            val dotCount = 6
            val dotRadius = duckSize * 0.03f
            val orbitRadius = duckSize * 0.45f

            for (i in 0 until dotCount) {
                val angle = (i * 360f / dotCount + ripple * 360f) * (Math.PI / 180f).toFloat()
                val dotX = centerX + kotlin.math.cos(angle) * orbitRadius
                val dotY = centerY + kotlin.math.sin(angle) * orbitRadius

                drawCircle(
                    color = DuqColors.accent.copy(alpha = 0.8f - (i * 0.1f)),
                    radius = dotRadius,
                    center = Offset(dotX, dotY)
                )
            }
        }
    }
}
