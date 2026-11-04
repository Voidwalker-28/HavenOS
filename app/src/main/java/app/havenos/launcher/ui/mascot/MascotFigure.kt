package app.havenos.launcher.ui.mascot

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * HavenOS v3 Track A — original vector mascot figure (prompt §3).
 * Drawn entirely in code as path shapes: no bitmap art, no copied
 * characters. Milo = slightly squarer body with one small tuft;
 * Mila = rounder body with a small ribbon accent. A single fill from
 * theme tokens (monochrome by default); the accent color tints only
 * the tuft/ribbon detail and one cheek dot. Expression comes from
 * squash/stretch, tilt, and eye shapes — not a loud cartoon face.
 *
 * Reduced motion: the caller passes [reducedMotion] and this figure
 * renders static poses (no bounce/particles here at all — the layer
 * owns motion, this file only draws the pose).
 */
@Composable
fun MascotFigure(
    character: MascotCharacter,
    expression: MascotExpression,
    size: Dp,
    body: Color,
    eye: Color,
    accent: Color,
    blink: Boolean = false,
    modifier: Modifier = Modifier,
) {
    // Squash/stretch per expression (transform-only; the layer animates
    // nothing per-frame inside this Canvas). NOTE: the locals are NOT
    // named scaleX/scaleY — inside the graphicsLayer lambda those names
    // resolve to the receiver and `scaleX = scaleX` would be a no-op.
    val bodyScaleX = when (expression) {
        MascotExpression.SURPRISED -> 0.94f
        MascotExpression.SAD -> 1.04f
        MascotExpression.FOUND -> 0.96f
        else -> 1f
    }
    val bodyScaleY = when (expression) {
        MascotExpression.SURPRISED -> 1.12f
        MascotExpression.SAD -> 0.92f
        MascotExpression.FOUND -> 1.06f
        else -> 1f
    }
    val tilt = when (expression) {
        MascotExpression.CURIOUS -> 10f
        MascotExpression.CONFUSED -> -12f
        MascotExpression.THINKING -> 6f
        else -> 0f
    }
    Box(
        modifier
            .size(size)
            .graphicsLayer { scaleX = bodyScaleX; scaleY = bodyScaleY; rotationZ = tilt },
    ) {
        // The Dp param `size` shadows DrawScope.size inside Canvas, so
        // convert once here (the figure is square: w == h).
        val pxSize = with(LocalDensity.current) { size.toPx() }
        Canvas(Modifier.size(size)) {
            val w = pxSize
            val h = pxSize
            // Soft contact shadow (keeps the blob visible on any wallpaper).
            drawOval(
                color = Color.Black.copy(alpha = 0.18f),
                topLeft = Offset(w * 0.14f, h * 0.90f),
                size = Size(w * 0.72f, h * 0.09f),
            )
            // Body.
            val bodyPath = Path()
            if (character == MascotCharacter.MILO) {
                // Slightly squarer blob.
                bodyPath.addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        left = w * 0.08f, top = h * 0.10f,
                        right = w * 0.92f, bottom = h * 0.92f,
                        topLeftCornerRadius = CornerRadius(w * 0.30f, h * 0.30f),
                        topRightCornerRadius = CornerRadius(w * 0.30f, h * 0.30f),
                        bottomLeftCornerRadius = CornerRadius(w * 0.38f, h * 0.38f),
                        bottomRightCornerRadius = CornerRadius(w * 0.38f, h * 0.38f),
                    ),
                )
            } else {
                // Rounder blob.
                bodyPath.addOval(
                    androidx.compose.ui.geometry.Rect(
                        left = w * 0.06f, top = h * 0.08f,
                        right = w * 0.94f, bottom = h * 0.94f,
                    ),
                )
            }
            drawPath(bodyPath, color = body)
            drawPath(bodyPath, color = eye.copy(alpha = 0.55f), style = Stroke(width = 1.5f))
            // Differentiator, accent-tinted only: Milo's tuft, Mila's ribbon.
            if (character == MascotCharacter.MILO) {
                val tuft = Path().apply {
                    moveTo(w * 0.44f, h * 0.12f)
                    lineTo(w * 0.52f, h * 0.00f)
                    lineTo(w * 0.58f, h * 0.13f)
                    close()
                }
                drawPath(tuft, color = accent)
            } else {
                val ribbon = Path().apply {
                    moveTo(w * 0.62f, h * 0.10f)
                    lineTo(w * 0.78f, h * 0.02f)
                    lineTo(w * 0.80f, h * 0.16f)
                    lineTo(w * 0.66f, h * 0.20f)
                    close()
                }
                drawPath(ribbon, color = accent)
                drawCircle(color = accent, radius = w * 0.045f, center = Offset(w * 0.68f, h * 0.11f))
            }
            // Accent cheek dot (the only other accent use).
            drawCircle(color = accent, radius = w * 0.035f, center = Offset(w * 0.80f, h * 0.62f))
            // Eyes: pill/dot pair; shape carries the expression.
            val eyeW = w * 0.11f
            val eyeH = when {
                blink -> h * 0.015f
                expression == MascotExpression.SLEEPY -> h * 0.015f
                expression == MascotExpression.HAPPY || expression == MascotExpression.CELEBRATE -> h * 0.05f
                expression == MascotExpression.CURIOUS -> h * 0.14f
                else -> h * 0.10f
            }
            val eyeY = when (expression) {
                MascotExpression.THINKING -> h * 0.38f
                MascotExpression.CONFUSED -> h * 0.44f
                else -> h * 0.46f
            }
            val leftY = if (expression == MascotExpression.CONFUSED) eyeY - h * 0.03f else eyeY
            val rightY = if (expression == MascotExpression.CONFUSED) eyeY + h * 0.03f else eyeY
            drawRoundRect(
                color = eye,
                topLeft = Offset(w * 0.28f, leftY),
                size = Size(eyeW, eyeH),
                cornerRadius = CornerRadius(eyeW / 2, eyeH / 2),
            )
            drawRoundRect(
                color = eye,
                topLeft = Offset(w * 0.61f, rightY),
                size = Size(eyeW, eyeH),
                cornerRadius = CornerRadius(eyeW / 2, eyeH / 2),
            )
            // Tiny mouth line: up for happy/celebrate, down for sad.
            when (expression) {
                MascotExpression.HAPPY, MascotExpression.CELEBRATE -> {
                    drawArc(
                        color = eye, startAngle = 20f, sweepAngle = 140f, useCenter = false,
                        topLeft = Offset(w * 0.42f, h * 0.62f), size = Size(w * 0.16f, h * 0.10f),
                        style = Stroke(width = 2f),
                    )
                }
                MascotExpression.SAD -> {
                    drawArc(
                        color = eye, startAngle = 200f, sweepAngle = 140f, useCenter = false,
                        topLeft = Offset(w * 0.42f, h * 0.68f), size = Size(w * 0.16f, h * 0.10f),
                        style = Stroke(width = 2f),
                    )
                }
                else -> {
                    drawLine(
                        color = eye,
                        start = Offset(w * 0.46f, h * 0.68f),
                        end = Offset(w * 0.54f, h * 0.68f),
                        strokeWidth = 2f,
                    )
                }
            }
        }
    }
}
