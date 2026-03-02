package com.khadr.forge.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ─── Neumorphic Shadow Modifier ───────────────────────────────────────────────
fun Modifier.neumorphicShadow(
    shadowColorDark: Color,
    shadowColorLight: Color,
    cornerRadius: Dp = 16.dp,
    shadowBlurRadius: Dp = 12.dp,
    shadowOffset: Dp = 6.dp
): Modifier = this.drawBehind {
    drawIntoCanvas { canvas ->
        // Bottom-right dark shadow
        val darkPaint = Paint().apply {
            asFrameworkPaint().apply {
                isAntiAlias = true
                color = android.graphics.Color.TRANSPARENT
                setShadowLayer(
                    shadowBlurRadius.toPx(),
                    shadowOffset.toPx(),
                    shadowOffset.toPx(),
                    shadowColorDark.copy(alpha = 0.8f).toArgb()
                )
            }
        }
        // Top-left light shadow
        val lightPaint = Paint().apply {
            asFrameworkPaint().apply {
                isAntiAlias = true
                color = android.graphics.Color.TRANSPARENT
                setShadowLayer(
                    shadowBlurRadius.toPx(),
                    -shadowOffset.toPx(),
                    -shadowOffset.toPx(),
                    shadowColorLight.copy(alpha = 0.8f).toArgb()
                )
            }
        }
        val r = cornerRadius.toPx()
        canvas.drawRoundRect(0f, 0f, size.width, size.height, r, r, darkPaint)
        canvas.drawRoundRect(0f, 0f, size.width, size.height, r, r, lightPaint)
    }
}

// ─── ForgeCard — Raised Neumorphic Card ───────────────────────────────────────
@Composable
fun ForgeCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    shadowBlur: Dp = 12.dp,
    shadowOffset: Dp = 6.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val neum = MaterialTheme.neumorphic
    val shape: Shape = RoundedCornerShape(cornerRadius)

    val base = modifier
        .neumorphicShadow(
            shadowColorDark  = neum.shadowDark,
            shadowColorLight = neum.shadowLight,
            cornerRadius     = cornerRadius,
            shadowBlurRadius = shadowBlur,
            shadowOffset     = shadowOffset
        )
        .clip(shape)
        .background(neum.background)

    val finalModifier = if (onClick != null) {
        base.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication        = null,
            onClick           = onClick
        )
    } else base

    Box(modifier = finalModifier, content = content)
}

// ─── ForgeSurface — Inset / Pressed Surface ───────────────────────────────────
@Composable
fun ForgeSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val neum = MaterialTheme.neumorphic
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(neum.surfaceElevated)
            .drawBehind {
                drawIntoCanvas { canvas ->
                    val insetPaint = Paint().apply {
                        asFrameworkPaint().apply {
                            isAntiAlias = true
                            color = android.graphics.Color.TRANSPARENT
                            setShadowLayer(
                                8.dp.toPx(),
                                3.dp.toPx(),
                                3.dp.toPx(),
                                neum.shadowDark.copy(alpha = 0.5f).toArgb()
                            )
                        }
                    }
                    val r = cornerRadius.toPx()
                    canvas.drawRoundRect(0f, 0f, size.width, size.height, r, r, insetPaint)
                }
            },
        content = content
    )
}