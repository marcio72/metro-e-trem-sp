package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransitLine
import com.example.data.model.TransitStation
import com.example.ui.TransitViewModel
import com.example.ui.components.LineBadge
import com.example.ui.components.StationDetailBottomSheet

@Composable
fun LinesCatalogScreen(
  viewModel: TransitViewModel,
  onNavigateToRoute: (TransitStation?) -> Unit,
  modifier: Modifier = Modifier
) {
  var expandedLineId by remember { mutableStateOf<String?>("linha-1") }
  var selectedStationForSheet by remember { mutableStateOf<TransitStation?>(null) }

  selectedStationForSheet?.let { st ->
    val lines = viewModel.repository.getLinesForStation(st)
    StationDetailBottomSheet(
      station = st,
      lines = lines,
      plannedLines = viewModel.repository.getPlannedLinesForStation(st),
      onDismiss = { selectedStationForSheet = null },
      onNavigateToStation = {
        viewModel.setRouteFromNearestTo(st)
        onNavigateToRoute(st)
      }
    )
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
  ) {
    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = "Rede Metropolitana de São Paulo",
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground
    )
    Text(
      text = "Metrô, CPTM, ViaQuatro, ViaMobilidade e TIC Trens",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(14.dp))

    LazyColumn(
      modifier = Modifier.weight(1f),
      contentPadding = PaddingValues(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      items(viewModel.allLines, key = { it.id }) { line ->
        LineAccordionItem(
          line = line,
          isExpanded = expandedLineId == line.id,
          onToggleExpand = {
            expandedLineId = if (expandedLineId == line.id) null else line.id
          },
          getStationById = { viewModel.repository.getStationById(it) },
          onStationClick = { selectedStationForSheet = it }
        )
      }
    }
  }
}

@Composable
fun LineAccordionItem(
  line: TransitLine,
  isExpanded: Boolean,
  onToggleExpand: () -> Unit,
  getStationById: (String) -> TransitStation?,
  onStationClick: (TransitStation) -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("line_accordion_${line.id}"),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      if (isExpanded) line.composeColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    )
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onToggleExpand() }
          .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          LineBadge(line = line)

          Column {
            Text(
              text = line.operator,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = run {
                val total = line.allStationIds.size
                val ativas = line.allStationIds.count { id -> getStationById(id)?.servesLine(line.id) == true }
                when {
                  line.status == "operational" && ativas == total -> "$total estações • Operando"
                  ativas == 0 -> "$total estações • Em projeto / obras"
                  else -> "$ativas de $total estações operando"
                }
              },
              fontSize = 11.sp,
              color = if (line.status == "operational") Color(0xFF10B981) else Color(0xFFF59E0B),
              fontWeight = FontWeight.Medium
            )
          }
        }

        Icon(
          imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      AnimatedVisibility(visible = isExpanded) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = 12.dp)
        ) {
          // Tronco + estações de ramais (ex.: Washington Luís na L17)
          val ids = line.allStationIds
          ids.forEachIndexed { index, stId ->
            val station = getStationById(stId)
            if (station != null) {
              StationLineRow(
                station = station,
                lineColor = line.composeColor,
                isFirst = index == 0,
                isLast = index == ids.size - 1,
                isPlanned = !station.servesLine(line.id),
                onClick = { onStationClick(station) }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun StationLineRow(
  station: TransitStation,
  lineColor: Color,
  isFirst: Boolean,
  isLast: Boolean,
  onClick: () -> Unit,
  isPlanned: Boolean = false
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Connector line / bullet
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.width(24.dp)
    ) {
      Box(
        modifier = Modifier
          .size(if (station.isInterchange) 12.dp else 9.dp)
          .background(
            if (station.isInterchange) Color.White else lineColor,
            CircleShape
          )
          .border(2.dp, lineColor, CircleShape)
      )
    }

    Spacer(modifier = Modifier.width(8.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = if (isPlanned) "${station.name} (em obras)" else station.name,
        fontSize = 13.sp,
        fontWeight = if (station.isInterchange) FontWeight.Bold else FontWeight.Normal,
        color = if (isPlanned) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
      )
      if (station.isInterchange) {
        Text(
          text = "Estação de Integração",
          fontSize = 10.sp,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.SemiBold
        )
      }
    }
  }
}
