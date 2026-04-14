package ru.fairlak.antialphakid.features.effects

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

fun Modifier.crtEffect(activeColor: Color): Modifier = this.composed {
    var time by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis { time = it }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "CRT Flicker")
    val flickerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(Random.nextInt(50, 150), easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flicker"
    )

    this.drawWithContent {
        drawContent()

        drawRect(color = Color.Black.copy(alpha = 1f - flickerAlpha))

        val random = Random(time)

        val lineSpacing = 6f
        for (y in 0 until size.height.toInt() step lineSpacing.toInt()) {
            drawRect(
                color = Color.Black.copy(alpha = 0.3f),
                topLeft = Offset(0f, y.toFloat()),
                size = Size(size.width, 2f)
            )
        }

        if (random.nextFloat() < 0.05f) {
            val glitchY = random.nextFloat() * size.height
            val glitchHeight = random.nextFloat() * 20f + 2f
            drawRect(
                color = activeColor.copy(alpha = 0.1f),
                topLeft = Offset(0f, glitchY),
                size = Size(size.width, glitchHeight)
            )
        }

        val noiseAlpha = 0.04f
        repeat(800) {
            val x = random.nextFloat() * size.width
            val y = random.nextFloat() * size.height
            drawRect(
                color = activeColor.copy(alpha = noiseAlpha),
                topLeft = Offset(x, y),
                size = Size(1.5f, 1.5f)
            )
        }

        val vignette = Brush.radialGradient(
            0.0f to Color.Transparent,
            0.5f to Color.Transparent,
            1.2f to Color.Black.copy(alpha = 0.7f),
            center = Offset(size.width / 2, size.height / 2),
            radius = size.maxDimension
        )
        drawRect(brush = vignette)
    }
}