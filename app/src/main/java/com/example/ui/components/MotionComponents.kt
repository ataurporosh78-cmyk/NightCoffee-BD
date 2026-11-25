package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CaramelGold
import com.example.ui.theme.CocoaBorder
import com.example.ui.theme.EspressoBlack
import com.example.ui.theme.MochaCardSurface
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal val EaseOutMotion = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

@Composable
fun AnimatedSteamingCoffeeEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 136.dp
) {
    val transition = rememberInfiniteTransition(label = "coffeeHeroTransition")
    val glowAlpha by transition.animateFloat(
        initialValue = 0.24f,
        targetValue = 0.60f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coffeeGlowAlpha"
    )
    val steamPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "coffeeSteamPhase"
    )

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sx = w / 200f
        val sy = h / 200f

        // Warm pulsing ambient gold glow behind the cup
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    CaramelGold.copy(alpha = glowAlpha),
                    CaramelGold.copy(alpha = glowAlpha * 0.25f),
                    Color.Transparent
                ),
                center = Offset(100f * sx, 118f * sy),
                radius = 96f * sx
            ),
            radius = 96f * sx,
            center = Offset(100f * sx, 118f * sy)
        )

        // Saucer shadow / coaster base
        drawOval(
            color = Color(0xFF8A6A38),
            topLeft = Offset(24f * sx, 162f * sy),
            size = Size(152f * sx, 16f * sy)
        )

        // Cup handle arc on right side
        val handlePath = Path().apply {
            arcTo(
                rect = Rect(
                    left = 128f * sx,
                    top = 114f * sy,
                    right = 174f * sx,
                    bottom = 156f * sy
                ),
                startAngleDegrees = -90f,
                sweepAngleDegrees = 180f,
                forceMoveTo = true
            )
        }
        drawPath(
            path = handlePath,
            color = Color(0xFFD9B26F),
            style = Stroke(width = 7.5f * sx, cap = StrokeCap.Round)
        )

        // Cup body with metallic gold gradient
        val cupPath = Path().apply {
            moveTo(44f * sx, 104f * sy)
            lineTo(156f * sx, 104f * sy)
            cubicTo(
                156f * sx, 160f * sy,
                132f * sx, 176f * sy,
                100f * sx, 176f * sy
            )
            cubicTo(
                68f * sx, 176f * sy,
                44f * sx, 160f * sy,
                44f * sx, 104f * sy
            )
            close()
        }
        drawPath(
            path = cupPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFE8C98A), Color(0xFFB88A45)),
                startY = 104f * sy,
                endY = 176f * sy
            )
        )

        // Dark espresso surface + gold rim highlight
        drawOval(
            color = Color(0xFF3B2314),
            topLeft = Offset(44f * sx, 95f * sy),
            size = Size(112f * sx, 14f * sy)
        )
        drawOval(
            color = Color(0xFFD9B26F),
            topLeft = Offset(44f * sx, 95f * sy),
            size = Size(112f * sx, 14f * sy),
            style = Stroke(width = 2.2f * sx)
        )

        // Three staggered rising steam wisps
        val steamXOffsets = listOf(76f, 100f, 124f)
        steamXOffsets.forEachIndexed { idx, baseX ->
            val t = (steamPhase + idx * 0.333f) % 1f
            val alpha = sin(t * Math.PI).toFloat().coerceIn(0f, 1f) * 0.62f
            val riseY = 8f * sy - t * 42f * sy
            val x = baseX * sx
            val yStart = 88f * sy + riseY
            val path = Path().apply {
                moveTo(x, yStart)
                quadraticTo(x - 8f * sx, yStart - 12f * sy, x, yStart - 24f * sy)
                quadraticTo(x + 8f * sx, yStart - 36f * sy, x, yStart - 46f * sy)
            }
            drawPath(
                path = path,
                color = Color(0xFFF3E3C3).copy(alpha = alpha),
                style = Stroke(width = 4.2f * sx, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun PulsingStatusDot(
    color: Color,
    isPulsing: Boolean,
    modifier: Modifier = Modifier,
    dotSize: Dp = 8.dp
) {
    Box(
        modifier = modifier.size(dotSize * 2.2f),
        contentAlignment = Alignment.Center
    ) {
        if (isPulsing) {
            val infiniteTransition = rememberInfiniteTransition(label = "statusDotPulse")
            val progress by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1800, easing = EaseOutMotion),
                    repeatMode = RepeatMode.Restart
                ),
                label = "pulseProgress"
            )
            val ringScale = 1f + progress * 2.2f
            val ringAlpha = (1f - progress) * 0.55f
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .graphicsLayer {
                        scaleX = ringScale
                        scaleY = ringScale
                        alpha = ringAlpha
                    }
                    .clip(CircleShape)
                    .background(color)
            )
        }

        Box(
            modifier = Modifier
                .size(dotSize)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
fun StaggeredEntranceItem(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val initialOffsetPx = remember(density) { with(density) { 26.dp.toPx() } }
    val alphaAnim = remember { Animatable(0f) }
    val translateAnim = remember { Animatable(initialOffsetPx) }

    LaunchedEffect(Unit) {
        val staggerDelayMs = minOf(index, 6) * 45L
        if (staggerDelayMs > 0L) {
            delay(staggerDelayMs)
        }
        launch {
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 400, easing = EaseOutMotion)
            )
        }
        launch {
            translateAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 400, easing = EaseOutMotion)
            )
        }
    }

    Box(
        modifier = modifier.graphicsLayer {
            alpha = alphaAnim.value
            translationY = translateAnim.value
        }
    ) {
        content()
    }
}

@Composable
fun ShimmerShopSkeletonList(
    modifier: Modifier = Modifier,
    itemCount: Int = 4,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeletonShimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    val shimmerBrush = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF2B1A12),
            Color(0xFF5A3E2B),
            Color(0xFF2B1A12)
        ),
        startX = shimmerOffset * 600f,
        endX = (shimmerOffset + 1f) * 600f
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 720.dp),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        userScrollEnabled = false
    ) {
        items(itemCount) {
            ShimmerShopSkeletonCard(brush = shimmerBrush)
        }
    }
}

@Composable
fun ShimmerShopSkeletonCard(
    brush: Brush,
    modifier: Modifier = Modifier
) {
    LiquidGlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        borderColor = CocoaBorder
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MochaCardSurface.copy(alpha = 0.55f))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(brush)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.62f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(brush)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.42f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(brush)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(EspressoBlack.copy(alpha = 0.5f))
                    )
                }
            }
        }
    }
}
