package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StationDistance
import com.example.data.model.TransitLine
import com.example.data.model.TransitStation
import com.example.data.model.UserLocationState
import com.example.data.repository.TransitRepository
import com.example.ui.theme.contentColorFor

@Composable
fun LineBadge(
  line: TransitLine,
  modifier: Modifier = Modifier,
  compact: Boolean = false
) {
  val bgColor = line.composeColor
  val textColor = contentColorFor(bgColor)

  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(8.dp),
    color = bgColor
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = if (compact) 3.dp else 5.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      if (!line.number.isNullOrEmpty()) {
        Box(
          modifier = Modifier
            .size(18.dp)
            .background(Color.White.copy(alpha = 0.25f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = line.number,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
      Text(
        text = if (compact) line.name else "${line.number?.let { "L$it " } ?: ""}${line.name}",
        color = textColor,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StationCard(
  stationDistance: StationDistance,
  isHeroNearest: Boolean = false,
  onStationClick: (TransitStation) -> Unit,
  onViewOnMap: (TransitStation) -> Unit,
  onRouteClick: (TransitStation) -> Unit,
  modifier: Modifier = Modifier
) {
  val station = stationDistance.station
  val cardColors = if (isHeroNearest) {
    CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    )
  } else {
    CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    )
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("station_card_${station.id}")
      .clickable { onStationClick(station) },
    shape = RoundedCornerShape(16.dp),
    colors = cardColors,
    border = if (isHeroNearest) {
      androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
      androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    }
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      if (isHeroNearest) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.primary
          ) {
            Text(
              text = "MAIS PRÓXIMA DE VOCÊ",
              color = Color.White,
              fontSize = 10.sp,
              fontWeight = FontWeight.ExtraBold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(
              imageVector = Icons.Default.DirectionsWalk,
              contentDescription = "Tempo de caminhada",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp)
            )
            Text(
              text = stationDistance.formattedWalkTime,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = station.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )

          if (!isHeroNearest) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.padding(top = 4.dp)
            ) {
              Icon(
                imageVector = Icons.Default.DirectionsWalk,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
              )
              Text(
                text = "${stationDistance.formattedDistance} (${stationDistance.formattedWalkTime})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        if (isHeroNearest) {
          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = stationDistance.formattedDistance,
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.primary
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Badges for connecting lines
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        stationDistance.lines.forEach { line ->
          LineBadge(line = line, compact = true)
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Action buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = { onViewOnMap(station) },
          modifier = Modifier
            .weight(1f)
            .height(38.dp)
            .testTag("btn_map_${station.id}"),
          shape = RoundedCornerShape(10.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Map,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("No Mapa", fontSize = 12.sp)
        }

        Button(
          onClick = { onRouteClick(station) },
          modifier = Modifier
            .weight(1f)
            .height(38.dp)
            .testTag("btn_route_${station.id}"),
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
          )
        ) {
          Icon(
            imageVector = Icons.Default.Navigation,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("Como Ir", fontSize = 12.sp)
        }
      }
    }
  }
}

@Composable
fun CurrentLocationHeader(
  locationState: UserLocationState,
  isLocating: Boolean,
  onRefreshGps: () -> Unit,
  onChangeLocation: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (locationState.isSimulated) Icons.Default.LocationOn else Icons.Default.GpsFixed,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
        }

        Column {
          Text(
            text = if (locationState.isSimulated) "Local simulado (São Paulo)" else "Localização em tempo real",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = locationState.label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onRefreshGps,
          modifier = Modifier.testTag("btn_refresh_gps")
        ) {
          if (isLocating) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.primary
            )
          } else {
            Icon(
              imageVector = Icons.Default.Sync,
              contentDescription = "Atualizar GPS",
              tint = MaterialTheme.colorScheme.primary
            )
          }
        }

        FilledTonalButton(
          onClick = onChangeLocation,
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.testTag("btn_change_location")
        ) {
          Text("Trocar", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
      }
    }
  }
}

@Composable
fun LocationPresetDialog(
  currentLocation: UserLocationState,
  onDismiss: () -> Unit,
  onSelectRealGps: () -> Unit,
  onSelectPreset: (UserLocationState) -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Ponto de Partida",
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item {
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 10.dp)
              .clickable {
                onSelectRealGps()
                onDismiss()
              },
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(
                imageVector = Icons.Default.GpsFixed,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
              )
              Column {
                Text(
                  text = "Usar meu GPS real",
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp
                )
                Text(
                  text = "Detecta automaticamente por satélite/rede",
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }

        items(TransitRepository.PRESET_LOCATIONS) { preset ->
          val isSelected = currentLocation.label == preset.label
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp)
              .clip(RoundedCornerShape(8.dp))
              .clickable {
                onSelectPreset(preset)
                onDismiss()
              },
            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
              )
              Text(
                text = preset.label,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp
              )
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Fechar")
      }
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StationDetailBottomSheet(
  station: TransitStation,
  lines: List<TransitLine>,
  onDismiss: () -> Unit,
  onNavigateToStation: () -> Unit,
  plannedLines: List<TransitLine> = emptyList()
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp, vertical = 12.dp)
        .padding(bottom = 24.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = station.name,
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.weight(1f)
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = if (station.isOperational) Color(0xFF10B981) else Color(0xFFF59E0B)
        ) {
          Text(
            text = if (station.isOperational) "EM OPERAÇÃO" else "EM PROJETO / OBRAS",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
          )
        }

        if (station.isInterchange) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFF6366F1)
          ) {
            Text(
              text = "INTEGRAÇÃO ENTRE LINHAS",
              color = Color.White,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = "Linhas atendidas nesta estação:",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
      )
      Spacer(modifier = Modifier.height(8.dp))

      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        lines.forEach { line ->
          LineBadge(line = line)
        }
        if (lines.isEmpty()) {
          Text("Nenhuma linha em operação ainda", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }

      if (plannedLines.isNotEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "Em obras / futuras:",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          plannedLines.forEach { line -> LineBadge(line = line, modifier = Modifier.alpha(0.5f)) }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(
            text = "Informações do Sistema",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "• Horário padrão: das 04:40 às 00:00 (Sábados até 01:00)",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "• Transferência gratuita entre Metrô e CPTM em estações integradas",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "• Acessibilidade: Elevadores, piso tátil e escadas rolantes",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      Button(
        enabled = station.isOperational,
        onClick = {
          onNavigateToStation()
          onDismiss()
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("btn_plan_route_to_station"),
        shape = RoundedCornerShape(12.dp)
      ) {
        Icon(imageVector = Icons.Default.Navigation, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Calcular Trajeto até aqui", fontWeight = FontWeight.Bold)
      }
    }
  }
}
