package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AtharTheme
import kotlin.math.cos
import kotlin.math.sin
import java.util.Locale

/* ---------------------------------------------------------------------------
 *  مكوّنات واجهة مشتركة — تُبنى عليها كل شاشات التطبيق
 * ------------------------------------------------------------------------- */

private val ArabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

/** يحوّل الأرقام الإنجليزية إلى أرقام عربية-هندية. */
fun toArabicDigits(value: Any): String {
    val sb = StringBuilder()
    for (ch in value.toString()) {
        if (ch in '0'..'9') sb.append(ArabicDigits[ch - '0']) else sb.append(ch)
    }
    return sb.toString()
}

/** يحوّل توقيت 24 ساعة (HH:mm) إلى صيغة 12 ساعة عربية. */
fun to12HourArabic(time24: String): String {
    return try {
        val parts = time24.split(":")
        val h = parts[0].toInt()
        val m = parts[1].toInt()
        val suffix = if (h < 12) "ص" else "م"
        var hour12 = h % 12
        if (hour12 == 0) hour12 = 12
        "${toArabicDigits(hour12)}:${toArabicDigits(String.format(Locale.US, "%02d", m))} $suffix"
    } catch (e: Exception) {
        time24
    }
}

// ---------------------------------------------------------------------------
// شريط علوي موحّد
// ---------------------------------------------------------------------------

@Composable
fun AtharTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            } else {
                Spacer(Modifier.width(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (actions != null) {
                Row(verticalAlignment = Alignment.CenterVertically) { actions() }
            }
            Spacer(Modifier.width(4.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// عناوين الأقسام
// ---------------------------------------------------------------------------

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AtharTheme.extra.gold,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onAction() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// بطاقة أساسية
// ---------------------------------------------------------------------------

@Composable
fun AtharCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val clickModifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = color,
        border = border,
    ) {
        Column(
            modifier = Modifier
                .then(clickModifier)
                .padding(contentPadding),
            content = content,
        )
    }
}

// ---------------------------------------------------------------------------
// مربّع ميزة في الشاشة الرئيسية
// ---------------------------------------------------------------------------

@Composable
fun FeatureTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(
            modifier = Modifier
                .clickable { onClick() }
                .padding(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// صف إعداد
// ---------------------------------------------------------------------------

@Composable
fun SettingRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val clickModifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(clickModifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconTint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

// ---------------------------------------------------------------------------
// حالات فارغة / تحميل
// ---------------------------------------------------------------------------

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(18.dp))
            PillButton(text = actionLabel, onClick = onAction)
        }
    }
}

@Composable
fun LoadingState(message: String = "جارٍ التحميل…", modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(14.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---------------------------------------------------------------------------
// أزرار
// ---------------------------------------------------------------------------

@Composable
fun PillButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    container: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    onClick: () -> Unit,
) {
    val alpha by animateFloatAsState(if (enabled) 1f else 0.45f, label = "pill_alpha")
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = container.copy(alpha = alpha),
    ) {
        Row(
            modifier = Modifier
                .clickable(enabled = enabled) { onClick() }
                .padding(horizontal = 22.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
            )
        }
    }
}

@Composable
fun ChoiceChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(200),
        label = "chip_bg",
    )
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = bg,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = fg,
            maxLines = 1,
            modifier = Modifier
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 9.dp),
        )
    }
}

@Composable
fun ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50)),
        color = color,
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
    )
}

// ---------------------------------------------------------------------------
// خلفيات وزخارف
// ---------------------------------------------------------------------------

/** زخرفة نجوم إسلامية ثمانية خفيفة تُستعمل خلف الترويسات. */
@Composable
fun IslamicPattern(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    alpha: Float = 0.10f,
    cell: Dp = 54.dp,
) {
    Canvas(modifier = modifier) {
        val step = cell.toPx()
        var row = 0
        var y = -step / 2f
        while (y < size.height + step) {
            var x = if (row % 2 == 0) 0f else step / 2f
            while (x < size.width + step) {
                drawEightPointStar(Offset(x, y), step * 0.30f, color, alpha)
                x += step
            }
            y += step * 0.88f
            row++
        }
    }
}

private fun DrawScope.drawEightPointStar(center: Offset, radius: Float, color: Color, alpha: Float) {
    val path = Path()
    val points = 8
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) radius else radius * 0.46f
        val angle = (Math.PI / points) * i - Math.PI / 2.0
        val px = center.x + (r * cos(angle)).toFloat()
        val py = center.y + (r * sin(angle)).toFloat()
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    drawPath(path = path, color = color, alpha = alpha, style = Stroke(width = 1.4f))
}

/** ترويسة متدرّجة مع الزخرفة. */
@Composable
fun GradientHero(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp),
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    val extra = AtharTheme.extra
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(extra.heroStart, extra.heroMid, extra.heroEnd),
                ),
            ),
    ) {
        IslamicPattern(
            modifier = Modifier.matchParentSize(),
            color = extra.gold,
            alpha = 0.14f,
        )
        content()
    }
}

@Composable
fun DotSeparator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(4.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)),
    )
}

@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(container.copy(alpha = if (enabled) 1f else 0.4f))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint.copy(alpha = if (enabled) 1f else 0.4f),
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

@Composable
fun FullScreenBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun HeaderTitleText(text: String, weight: FontWeight = FontWeight.Bold) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = weight),
        color = MaterialTheme.colorScheme.onBackground,
    )
}
