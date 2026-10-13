package com.example.ui.components

import android.graphics.Paint
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransitLine
import com.example.data.model.TransitStation
import com.example.data.model.UserLocationState
import kotlin.math.hypot

@Composable
fun NetworkMapCanvas(
  lines: List<TransitLine>,
  stations: List<TransitStation>,
  userLocation: UserLocationState,
  selectedStation: TransitStation?,
  selectedLineFilter: String?,
  onSelectStation: (TransitStation) -> Unit,
  isOperationalSegment: (lineId: String, fromId: String, toId: String) -> Boolean = { _, _, _ -> true },
  modifier: Modifier = Modifier
) {
  var canvasSize by remember { mutableStateOf(IntSize.Zero) }
  val labelColor = MaterialTheme.colorScheme.onBackground
  val labelShadow = MaterialTheme.colorScheme.background
  val isDarkBackground = MaterialTheme.colorScheme.background.luminance() < 0.5f
  val textPaint = remember {
    Paint().apply {
      isAntiAlias = true
      typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
  }
  var scale by remember { mutableFloatStateOf(1.2f) }
  var offset by remember { mutableStateOf(Offset.Zero) }

  val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
    scale = (scale * zoomChange).coerceIn(0.6f, 6.0f)
    offset += panChange
  }

  // Animation for user location pulse
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseRadius by infiniteTransition.animateFloat(
    initialValue = 12f,
    targetValue = 28f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200),
      repeatMode = RepeatMode.Restart
    ),
    label = "pulseRadius"
  )
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.6f,
    targetValue = 0.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200),
      repeatMode = RepeatMode.Restart
    ),
    label = "pulseAlpha"
  )

  // Bounding box calculado a partir das estações (Jundiaí e Mogi ficavam fora do retângulo fixo)
  val bounds = remember(stations) {
    if (stations.isEmpty()) doubleArrayOf(-23.78, -23.25, -46.99, -46.22)
    else doubleArrayOf(
      stations.minOf { it.lat } - 0.02, stations.maxOf { it.lat } + 0.02,
      stations.minOf { it.lon } - 0.02, stations.maxOf { it.lon } + 0.02
    )
  }
  val minLat = bounds[0]
  val maxLat = bounds[1]
  val minLon = bounds[2]
  val maxLon = bounds[3]

  fun geoToCanvas(lat: Double, lon: Double, width: Float, height: Float): Offset {
    // Mercator-like normalized mapping
    val normX = ((lon - minLon) / (maxLon - minLon)).toFloat()
    val normY = ((maxLat - lat) / (maxLat - minLat)).toFloat() // inverted Y

    val baseWidth = width * 1.6f
    val baseHeight = height * 1.6f

    val cx = width / 2f
    val cy = height / 2f

    val rawX = (normX - 0.5f) * baseWidth
    val rawY = (normY - 0.5f) * baseHeight

    val transformedX = cx + rawX * scale + offset.x
    val transformedY = cy + rawY * scale + offset.y
    return Offset(transformedX, transformedY)
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .onSizeChanged { canvasSize = it }
        .transformable(state = transformableState)
        .pointerInput(stations) {
          detectTapGestures { tapOffset ->
            // Find closest station within touch threshold
            var closest: TransitStation? = null
            var minDist = Float.MAX_VALUE

            for (st in stations) {
              val screenPos = geoToCanvas(st.lat, st.lon, size.width.toFloat(), size.height.toFloat())
              val dist = hypot(screenPos.x - tapOffset.x, screenPos.y - tapOffset.y)
              val hitRadius = (28f * scale).coerceAtLeast(40f)
              if (dist < hitRadius && dist < minDist) {
                minDist = dist
                closest = st
              }
            }

            if (closest != null) {
              onSelectStation(closest)
            }
          }
        }
    ) {
      val canvasWidth = size.width
      val canvasHeight = size.height

      // 1. Linhas: tronco + ramais. Trecho em operação = traço cheio; em obras = tracejado e esmaecido.
      val stationMap = stations.associateBy { it.id }
      val visibleLines = if (selectedLineFilter.isNullOrEmpty()) lines else lines.filter { it.id == selectedLineFilter }
      val strokeWidth = (5.5f * scale).coerceIn(3f, 16f)
      val dashed = PathEffect.dashPathEffect(floatArrayOf(strokeWidth * 1.6f, strokeWidth * 1.2f))

      for (line in visibleLines) {
        // Expresso Aeroporto: traço fino para não esconder as linhas que ele acompanha
        val lineWidth = if (line.mode == "airport_express") strokeWidth * 0.45f else strokeWidth
        for (sequence in line.allSequences) {
          val pts = sequence.mapNotNull { id -> stationMap[id] }
          for (i in 0 until pts.size - 1) {
            val a = pts[i]
            val b = pts[i + 1]
            val operational = isOperationalSegment(line.id, a.id, b.id)
            val start = geoToCanvas(a.lat, a.lon, canvasWidth, canvasHeight)
            val end = geoToCanvas(b.lat, b.lon, canvasWidth, canvasHeight)
            // No tema escuro, linhas escuras (12-Safira, Expresso Aeroporto) ganham contorno claro
            if (isDarkBackground && line.composeColor.luminance() < 0.08f) {
              drawLine(
                color = Color.White.copy(alpha = 0.55f),
                start = start,
                end = end,
                strokeWidth = (if (operational) lineWidth else lineWidth * 0.7f) + 3f,
                cap = StrokeCap.Round,
                pathEffect = if (operational) null else dashed
              )
            }
            drawLine(
              color = if (operational) line.composeColor else line.composeColor.copy(alpha = 0.45f),
              start = start,
              end = end,
              strokeWidth = if (operational) lineWidth else lineWidth * 0.7f,
              cap = StrokeCap.Round,
              pathEffect = if (operational) null else dashed
            )
          }
        }
      }

      // 2. Estações
      textPaint.color = labelColor.toArgb()
      textPaint.textSize = (11f * scale).coerceIn(16f, 32f)
      textPaint.setShadowLayer(4f, 0f, 0f, labelShadow.toArgb())

      for (st in stations) {
        if (!selectedLineFilter.isNullOrEmpty() &&
          !st.lineIds.contains(selectedLineFilter) && !st.plannedLineIds.contains(selectedLineFilter)
        ) continue

        val pt = geoToCanvas(st.lat, st.lon, canvasWidth, canvasHeight)
        val isSelected = selectedStation?.id == st.id
        val nodeRadius = (if (st.isInterchange) 6.5f else 4.5f) * scale.coerceIn(0.8f, 2.5f)

        if (isSelected) {
          drawCircle(color = Color(0xFFFED304), radius = nodeRadius * 2.2f, center = pt)
        }

        when {
          !st.isOperational -> {
            // Em obras: anel cinza vazado
            drawCircle(color = Color(0xFF9CA3AF), radius = nodeRadius * 0.85f, center = pt, style = Stroke(width = 2f))
          }
          st.isInterchange -> {
            drawCircle(color = Color.Black, radius = nodeRadius + 2.5f, center = pt)
            drawCircle(color = Color.White, radius = nodeRadius, center = pt)
            drawCircle(color = Color(0xFF1E293B), radius = nodeRadius * 0.5f, center = pt)
          }
          else -> {
            drawCircle(color = Color.Black, radius = nodeRadius + 1.5f, center = pt)
            drawCircle(color = Color.White, radius = nodeRadius, center = pt)
          }
        }

        if (scale >= 1.6f || (st.isInterchange && scale >= 1.0f) || isSelected) {
          drawContext.canvas.nativeCanvas.drawText(st.name, pt.x + nodeRadius + 6f, pt.y + 4f, textPaint)
        }
      }
    }

    // 3. Localização do usuário em camada separada: o pulso animado redesenha só esta camada,
    //    não o mapa inteiro a cada frame.
    Canvas(modifier = Modifier.fillMaxSize()) {
      val userPt = geoToCanvas(userLocation.lat, userLocation.lon, size.width, size.height)
      drawCircle(
        color = Color(0xFF38BDF8).copy(alpha = pulseAlpha),
        radius = pulseRadius * scale.coerceIn(0.8f, 2.0f),
        center = userPt
      )
      drawCircle(color = Color.White, radius = (9f * scale).coerceIn(7f, 18f), center = userPt)
      drawCircle(color = Color(0xFF0284C7), radius = (6f * scale).coerceIn(4f, 13f), center = userPt)
    }

    // Floating map controls
    Column(
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(16.dp),
      horizontalAlignment = Alignment.End
    ) {
      SmallFloatingActionButton(
        onClick = { scale = (scale * 1.35f).coerceAtMost(6.0f) },
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        modifier = Modifier.testTag("btn_zoom_in")
      ) {
        Icon(Icons.Default.Add, contentDescription = "Aumentar zoom")
      }

      SmallFloatingActionButton(
        onClick = { scale = (scale / 1.35f).coerceAtLeast(0.6f) },
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        modifier = Modifier
          .padding(top = 8.dp)
          .testTag("btn_zoom_out")
      ) {
        Icon(Icons.Default.Remove, contentDescription = "Diminuir zoom")
      }

      SmallFloatingActionButton(
        onClick = {
          // Centraliza de fato no usuário: desloca para que o ponto dele caia no centro do canvas
          scale = 1.8f
          if (canvasSize != IntSize.Zero) {
            val w = canvasSize.width.toFloat()
            val h = canvasSize.height.toFloat()
            val normX = ((userLocation.lon - minLon) / (maxLon - minLon)).toFloat()
            val normY = ((maxLat - userLocation.lat) / (maxLat - minLat)).toFloat()
            offset = Offset(-(normX - 0.5f) * w * 1.6f * scale, -(normY - 0.5f) * h * 1.6f * scale)
          } else {
            offset = Offset.Zero
          }
        },
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = Color.White,
        modifier = Modifier
          .padding(top = 8.dp)
          .testTag("btn_center_user")
      ) {
        Icon(Icons.Default.MyLocation, contentDescription = "Centralizar em mim")
      }

      SmallFloatingActionButton(
        onClick = {
          scale = 1.0f
          offset = Offset.Zero
        },
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
          .padding(top = 8.dp)
          .testTag("btn_reset_map")
      ) {
        Icon(Icons.Default.RestartAlt, contentDescription = "Redefinir visualização")
      }
    }

    // Map Legend / Instructions chip
    Surface(
      modifier = Modifier
        .align(Alignment.TopStart)
        .padding(14.dp),
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
      border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(10.dp)
            .background(Color(0xFF0284C7), CircleShape)
        )
        Text(
          text = " Você está aqui",
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = " • Toque nas estações para ver linhas",
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}
