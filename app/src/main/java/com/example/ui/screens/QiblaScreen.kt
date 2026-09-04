package com.example.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.AtharViewModel
import com.example.ui.components.AtharCard
import com.example.ui.components.AtharTopBar
import com.example.ui.components.toArabicDigits
import com.example.ui.theme.AtharTheme
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/* ---------------------------------------------------------------------------
 *  بوصلة القبلة
 * ------------------------------------------------------------------------- */

private const val KAABA_LAT = 21.4225
private const val KAABA_LON = 39.8262

@Composable
fun QiblaScreen(vm: AtharViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val lat by vm.currentLatitude.collectAsState()
    val lon by vm.currentLongitude.collectAsState()
    val isLocationFetched by vm.isLocationFetched.collectAsState()
    val fetchedName by vm.fetchedLocationName.collectAsState()
    val city by vm.selectedCity.collectAsState()

    var rawAzimuth by remember { mutableFloatStateOf(0f) }
    var hasSensors by remember { mutableStateOf(true) }
    var lowAccuracy by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        hasSensors = accelerometer != null && magnetometer != null

        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)
        var hasGravity = false
        var hasGeo = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        System.arraycopy(event.values, 0, gravity, 0, 3)
                        hasGravity = true
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                        hasGeo = true
                    }
                }
                if (hasGravity && hasGeo) {
                    val r = FloatArray(9)
                    val i = FloatArray(9)
                    if (SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)) {
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(r, orientation)
                        val deg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                        rawAzimuth = (deg + 360f) % 360f
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    lowAccuracy = accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW
                }
            }
        }

        if (hasSensors && sensorManager != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_GAME)
        }
        onDispose { sensorManager?.unregisterListener(listener) }
    }

    val qiblaBearing = remember(lat, lon) { qiblaBearing(lat, lon) }
    val distanceKm = remember(lat, lon) { distanceToKaabaKm(lat, lon) }

    // تنعيم حركة الإبرة
    val azimuth by animateFloatAsState(
        targetValue = rawAzimuth,
        animationSpec = tween(220),
        label = "azimuth",
    )

    val needleAngle = (qiblaBearing - azimuth + 360f) % 360f
    val deviation = if (needleAngle > 180f) 360f - needleAngle else needleAngle
    val aligned = deviation <= 5f

    val locationLabel = if (isLocationFetched) fetchedName else city.nameAr

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        AtharTopBar(title = "اتجاه القبلة", subtitle = locationLabel, onBack = onBack)

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            CompassDial(
                azimuth = azimuth,
                needleAngle = needleAngle,
                aligned = aligned,
            )
        }

        // حالة المحاذاة
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (aligned) AtharTheme.extra.success.copy(alpha = 0.16f)
            else MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (aligned) Icons.Filled.CheckCircle else Icons.Filled.Explore,
                    contentDescription = null,
                    tint = if (aligned) AtharTheme.extra.success else MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (aligned) "أنت الآن في اتجاه القبلة" else "أدِر جهازك حتى تتوسّط الإبرة",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "الانحراف: ${toArabicDigits(deviation.roundToInt())}°",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InfoTile(
                title = "زاوية القبلة",
                value = "${toArabicDigits(qiblaBearing.roundToInt())}°",
                modifier = Modifier.weight(1f),
            )
            InfoTile(
                title = "المسافة للكعبة",
                value = "${toArabicDigits(distanceKm.roundToInt())} كم",
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(12.dp))

        if (!hasSensors) {
            WarningCard("جهازك لا يحتوي على مستشعر بوصلة، يمكنك الاعتماد على زاوية القبلة الظاهرة أعلاه مع بوصلة خارجية.")
        } else if (lowAccuracy) {
            WarningCard("دقّة البوصلة منخفضة. حرّك الجهاز على شكل الرقم ٨ عدة مرات لمعايرتها، وابتعد عن المعادن والأجهزة.")
        } else {
            AtharCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.MyLocation,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "ضع الجهاز أفقياً بعيداً عن المعادن للحصول على أدقّ اتجاه.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun WarningCard(message: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

@Composable
private fun InfoTile(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun CompassDial(azimuth: Float, needleAngle: Float, aligned: Boolean) {
    val gold = AtharTheme.extra.gold
    val primary = MaterialTheme.colorScheme.primary
    val success = AtharTheme.extra.success
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val surface = MaterialTheme.colorScheme.surfaceContainerLow

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = Modifier
                .size(300.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(surface, MaterialTheme.colorScheme.surfaceContainerHigh),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            // القرص الدوّار (يدور عكس اتجاه الجهاز ليبقى الشمال ثابتاً)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(-azimuth),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    drawCircle(
                        color = onSurfaceVariant.copy(alpha = 0.25f),
                        radius = radius - 4f,
                        center = center,
                        style = Stroke(width = 2f),
                    )
                    drawCircle(
                        color = gold.copy(alpha = 0.35f),
                        radius = radius * 0.72f,
                        center = center,
                        style = Stroke(width = 1.5f),
                    )

                    for (i in 0 until 72) {
                        val angle = Math.toRadians((i * 5).toDouble())
                        val isMajor = i % 6 == 0
                        val len = if (isMajor) radius * 0.10f else radius * 0.05f
                        val startR = radius - 8f
                        val endR = startR - len
                        val sx = center.x + (startR * sin(angle)).toFloat()
                        val sy = center.y - (startR * cos(angle)).toFloat()
                        val ex = center.x + (endR * sin(angle)).toFloat()
                        val ey = center.y - (endR * cos(angle)).toFloat()
                        drawLine(
                            color = if (isMajor) gold.copy(alpha = 0.9f) else onSurfaceVariant.copy(alpha = 0.45f),
                            start = Offset(sx, sy),
                            end = Offset(ex, ey),
                            strokeWidth = if (isMajor) 3f else 1.5f,
                            cap = StrokeCap.Round,
                        )
                    }
                }

                // الاتجاهات الأربعة
                CardinalLabel("ش", Modifier.align(Alignment.TopCenter), gold, true)
                CardinalLabel("ج", Modifier.align(Alignment.BottomCenter), onSurfaceVariant, false)
                CardinalLabel("ق", Modifier.align(Alignment.CenterEnd), onSurfaceVariant, false)
                CardinalLabel("غ", Modifier.align(Alignment.CenterStart), onSurfaceVariant, false)
            }

            // إبرة القبلة
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(needleAngle),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val tip = Offset(center.x, center.y - radius * 0.70f)
                    val needleColor = if (aligned) success else primary

                    drawLine(
                        color = needleColor.copy(alpha = 0.35f),
                        start = center,
                        end = Offset(center.x, center.y + radius * 0.45f),
                        strokeWidth = 6f,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = needleColor,
                        start = center,
                        end = tip,
                        strokeWidth = 8f,
                        cap = StrokeCap.Round,
                    )
                    val head = Path().apply {
                        moveTo(tip.x, tip.y - radius * 0.10f)
                        lineTo(tip.x - radius * 0.075f, tip.y + radius * 0.05f)
                        lineTo(tip.x + radius * 0.075f, tip.y + radius * 0.05f)
                        close()
                    }
                    drawPath(head, color = needleColor)
                    drawCircle(color = needleColor, radius = radius * 0.055f, center = center)
                }
            }

            // مركز البوصلة
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.size(104.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "الكعبة",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${toArabicDigits(azimuth.roundToInt())}°",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (aligned) success else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun CardinalLabel(text: String, modifier: Modifier, color: Color, emphasized: Boolean) {
    Text(
        text = text,
        style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
        color = color,
        modifier = modifier.padding(14.dp),
    )
}

// ---------------------------------------------------------------------------
// حسابات
// ---------------------------------------------------------------------------

fun qiblaBearing(lat: Double, lon: Double): Float {
    val phi = Math.toRadians(lat)
    val lambda = Math.toRadians(lon)
    val phiK = Math.toRadians(KAABA_LAT)
    val lambdaK = Math.toRadians(KAABA_LON)
    val dLambda = lambdaK - lambda
    val y = sin(dLambda)
    val x = cos(phi) * tan(phiK) - sin(phi) * cos(dLambda)
    var bearing = Math.toDegrees(atan2(y, x))
    if (bearing < 0) bearing += 360.0
    return bearing.toFloat()
}

fun distanceToKaabaKm(lat: Double, lon: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(KAABA_LAT - lat)
    val dLon = Math.toRadians(KAABA_LON - lon)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(lat)) * cos(Math.toRadians(KAABA_LAT)) *
        sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}
