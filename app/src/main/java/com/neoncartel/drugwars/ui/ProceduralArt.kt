package com.neoncartel.drugwars.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neoncartel.drugwars.domain.content.GameCatalog
import com.neoncartel.drugwars.domain.model.CharacterDefinition
import com.neoncartel.drugwars.domain.model.City
import com.neoncartel.drugwars.domain.model.ItemId
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CityBackdrop(city: City, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "city")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Restart),
        label = "drift",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    val textMeasurer = rememberTextMeasurer()
    val palette = city.palette.map { it.toColor() }

    Canvas(modifier = modifier.background(Color(0xFF05060A))) {
        drawRect(
            Brush.verticalGradient(
                listOf(palette[2], palette[0].copy(alpha = 0.42f), Color(0xFF05060A)),
            ),
        )
        drawCircle(
            color = palette[1].copy(alpha = 0.22f + pulse * 0.12f),
            radius = size.minDimension * 0.32f,
            center = Offset(size.width * (0.72f + drift * 0.04f), size.height * 0.28f),
        )
        repeat(34) { index ->
            val x = ((index * 83 + city.skylineSeed * 17) % 1000) / 1000f * size.width
            val y = ((drift * size.height * 0.7f) + index * 37) % size.height
            drawLine(
                color = palette[index % palette.size].copy(alpha = 0.18f),
                start = Offset(x, y),
                end = Offset(x + 18f, y + 46f),
                strokeWidth = 2f,
            )
        }
        val horizon = size.height * 0.78f
        repeat(18) { index ->
            val width = size.width / 24f + ((index * 13 + city.skylineSeed) % 19)
            val height = size.height * (0.18f + ((index * 29 + city.skylineSeed) % 34) / 100f)
            val x = index * size.width / 18f
            val top = horizon - height
            drawRect(
                color = Color(0xFF08111E).copy(alpha = 0.92f),
                topLeft = Offset(x, top),
                size = Size(width, height),
            )
            repeat(4) { light ->
                val lx = x + width * (0.22f + light * 0.18f)
                val ly = top + 14f + ((light * 31 + index * 7) % height.toInt().coerceAtLeast(20))
                drawRect(
                    color = palette[(index + light) % palette.size].copy(alpha = 0.72f),
                    topLeft = Offset(lx, ly),
                    size = Size(4f, 9f),
                )
            }
        }
        drawRect(
            Brush.verticalGradient(
                listOf(Color.Transparent, Color(0xFF05060A).copy(alpha = 0.84f)),
                startY = size.height * 0.45f,
                endY = size.height,
            ),
        )
        drawLine(
            color = palette[0].copy(alpha = 0.8f),
            start = Offset(0f, horizon),
            end = Offset(size.width, horizon - 24f),
            strokeWidth = 3f,
        )
        drawText(
            textMeasurer = textMeasurer,
            text = city.id.label.uppercase(),
            topLeft = Offset(24f, 16f),
            style = TextStyle(
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
            ),
        )
        drawText(
            textMeasurer = textMeasurer,
            text = city.weather,
            topLeft = Offset(26f, 58f),
            style = TextStyle(
                color = palette[3].copy(alpha = 0.86f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

@Composable
fun CharacterPortrait(character: CharacterDefinition, modifier: Modifier = Modifier) {
    val palette = when (character.id.ordinal % 4) {
        0 -> listOf(Color(0xFF28E6FF), Color(0xFFFF3FB4), Color(0xFF111827))
        1 -> listOf(Color(0xFFFDE047), Color(0xFF38BDF8), Color(0xFF1E1B4B))
        2 -> listOf(Color(0xFF34D399), Color(0xFFF472B6), Color(0xFF052E2B))
        else -> listOf(Color(0xFFF97316), Color(0xFFA78BFA), Color(0xFF111111))
    }
    Canvas(modifier = modifier.size(88.dp)) {
        drawCircle(Brush.radialGradient(listOf(palette[0], palette[2])), radius = size.minDimension / 2f)
        drawCircle(color = Color(0xFF05060A), radius = size.minDimension * 0.38f, center = center)
        drawCircle(color = palette[1].copy(alpha = 0.34f), radius = size.minDimension * 0.34f, center = center)
        val head = Rect(center.x - size.width * 0.18f, center.y - size.height * 0.24f, center.x + size.width * 0.18f, center.y + size.height * 0.12f)
        drawOval(color = palette[0].copy(alpha = 0.9f), topLeft = head.topLeft, size = head.size)
        drawCircle(color = Color.White, radius = 3.6f, center = Offset(center.x - 10f, center.y - 6f))
        drawCircle(color = Color.White, radius = 3.6f, center = Offset(center.x + 10f, center.y - 6f))
        drawLine(palette[2], Offset(center.x - 13f, center.y + 10f), Offset(center.x + 13f, center.y + 10f), 4f)
        if (character.archetype.contains("robot", ignoreCase = true) || character.archetype.contains("android", ignoreCase = true)) {
            drawRect(palette[1], topLeft = Offset(center.x - 20f, center.y - 25f), size = Size(40f, 32f), style = Stroke(3f))
        } else if (character.archetype.contains("fox", ignoreCase = true) || character.archetype.contains("lynx", ignoreCase = true)) {
            val left = Path().apply {
                moveTo(center.x - 24f, center.y - 18f)
                lineTo(center.x - 36f, center.y - 39f)
                lineTo(center.x - 10f, center.y - 28f)
                close()
            }
            val right = Path().apply {
                moveTo(center.x + 24f, center.y - 18f)
                lineTo(center.x + 36f, center.y - 39f)
                lineTo(center.x + 10f, center.y - 28f)
                close()
            }
            drawPath(left, palette[0])
            drawPath(right, palette[0])
        }
    }
}

@Composable
fun ItemGlyph(itemId: ItemId, modifier: Modifier = Modifier) {
    val item = GameCatalog.item(itemId)
    val color = Color.hsv((item.iconSeed * 19 % 360).toFloat(), 0.72f, 0.96f)
    Canvas(modifier = modifier.size(36.dp)) {
        drawRoundRect(Color(0xFF101827), size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f))
        val points = 5 + item.iconSeed % 4
        val path = Path()
        repeat(points) { index ->
            val angle = (Math.PI * 2.0 * index / points - Math.PI / 2.0).toFloat()
            val radius = if (index % 2 == 0) size.minDimension * 0.34f else size.minDimension * 0.22f
            val p = Offset(center.x + cos(angle) * radius, center.y + sin(angle) * radius)
            if (index == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        path.close()
        drawPath(path, color.copy(alpha = 0.34f))
        drawPath(path, color, style = Stroke(2.6f))
        drawCircle(Color.White.copy(alpha = 0.78f), radius = 2.5f, center = center)
    }
}

fun String.toColor(): Color = Color(android.graphics.Color.parseColor(this))
