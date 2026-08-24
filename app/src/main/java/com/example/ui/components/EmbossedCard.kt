package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun EmbossedCard(
    modifier: Modifier = Modifier,
    glowColor: Color = Color.Transparent,
    accentColor: Color? = null,
    borderBrush: Brush? = null,
    onClick: (() -> Unit)? = null,
    isGlass: Boolean = false,
    cornerRadius: Dp = 14.dp,
    elevation: Dp = 6.dp,
    contentPadding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape: Shape = RoundedCornerShape(cornerRadius)
    
    val baseModifier = if (isGlass) {
        modifier
            .background(Color.White.copy(alpha = 0.85f), shape)
            .border(1.5.dp, Color.White, shape)
            .clip(shape)
    } else {
        modifier
            .drawBehind {
                drawIntoCanvas { canvas ->
                    val frameworkPaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                    }
                    
                    // Dark Cast Shadow (Bottom-Right)
                    frameworkPaint.color = android.graphics.Color.TRANSPARENT
                    frameworkPaint.setShadowLayer(
                        elevation.toPx() * 1.5f,
                        elevation.toPx() * 0.7f,
                        elevation.toPx() * 0.7f,
                        android.graphics.Color.argb(70, 140, 135, 125)
                    )
                    canvas.nativeCanvas.drawRoundRect(
                        0f, 0f, size.width, size.height,
                        cornerRadius.toPx(), cornerRadius.toPx(),
                        frameworkPaint
                    )
                    
                    // Light Highlight Shadow (Top-Left)
                    frameworkPaint.setShadowLayer(
                        elevation.toPx() * 1.2f,
                        -elevation.toPx() * 0.5f,
                        -elevation.toPx() * 0.5f,
                        android.graphics.Color.argb(230, 255, 255, 255)
                    )
                    canvas.nativeCanvas.drawRoundRect(
                        0f, 0f, size.width, size.height,
                        cornerRadius.toPx(), cornerRadius.toPx(),
                        frameworkPaint
                    )
                }
            }
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFF7F5F0)
                    )
                ),
                shape = shape
            )
            .border(
                width = if (borderBrush != null) 1.8.dp else 1.dp,
                brush = borderBrush ?: Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFD6D2CA)
                    )
                ),
                shape = shape
            )
            .clip(shape)
    }
    
    val styledModifier = if (borderBrush == null && glowColor != Color.Transparent && !isGlass) {
        baseModifier.border(1.5.dp, glowColor.copy(alpha = 0.5f), shape)
    } else {
        baseModifier
    }
        
    if (onClick != null) {
        Box(
            modifier = styledModifier
                .clickable(onClick = onClick)
                .padding(contentPadding),
            content = content
        )
    } else {
        Box(
            modifier = styledModifier.padding(contentPadding),
            content = content
        )
    }
}
