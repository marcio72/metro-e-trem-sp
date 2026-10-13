package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransitStation
import com.example.ui.TransitViewModel
import com.example.ui.components.LineBadge
import com.example.ui.components.NetworkMapCanvas
import com.example.ui.components.StationDetailBottomSheet

@Composable
fun MapScreen(
  viewModel: TransitViewModel,
  onNavigateToRoute: (TransitStation?) -> Unit,
  modifier: Modifier = Modifier
) {
  val userLocation by viewModel.userLocation.collectAsState()
  val selectedLineFilter by viewModel.selectedLineFilter.collectAsState()
  val selectedStation by viewModel.selectedStation.collectAsState()

  var showDetailSheet by remember { mutableStateOf(false) }

  if (showDetailSheet && selectedStation != null) {
    val lines = viewModel.repository.getLinesForStation(selectedStation!!)
    StationDetailBottomSheet(
      station = selectedStation!!,
      lines = lines,
      plannedLines = viewModel.repository.getPlannedLinesForStation(selectedStation!!),
      onDismiss = { showDetailSheet = false },
      onNavigateToStation = {
        viewModel.setRouteFromNearestTo(selectedStation!!)
        onNavigateToRoute(selectedStation!!)
      }
    )
  }

  Box(modifier = modifier.fillMaxSize()) {
    // Canvas Network Map
    NetworkMapCanvas(
      lines = viewModel.allLines,
      stations = viewModel.allStations,
      userLocation = userLocation,
      selectedStation = selectedStation,
      selectedLineFilter = selectedLineFilter,
      onSelectStation = { station ->
        viewModel.selectStation(station)
      },
      isOperationalSegment = viewModel.repository::isOperationalSegment
    )

    // Top Line Selector Filters
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.TopCenter)
        .padding(top = 8.dp, start = 8.dp, end = 8.dp)
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      FilterChip(
        selected = selectedLineFilter == null,
        onClick = { viewModel.setLineFilter(null) },
        label = { Text("Toda a Rede", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
        shape = RoundedCornerShape(20.dp),
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = MaterialTheme.colorScheme.primary,
          selectedLabelColor = Color.White
        )
      )

      viewModel.allLines.forEach { line ->
        val isSelected = selectedLineFilter == line.id
        FilterChip(
          selected = isSelected,
          onClick = { viewModel.setLineFilter(line.id) },
          label = {
            Text(
              text = if (!line.number.isNullOrEmpty()) "L${line.number} ${line.name}" else line.name,
              fontSize = 11.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
          },
          shape = RoundedCornerShape(20.dp),
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = line.composeColor,
            selectedLabelColor = if (line.number == "4" || line.number == "15") Color.Black else Color.White
          ),
          modifier = Modifier.testTag("map_filter_${line.id}")
        )
      }
    }

    // Bottom Selected Station Floating Preview Card
    selectedStation?.let { st ->
      val lines = viewModel.repository.getLinesForStation(st)
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.BottomCenter)
          .padding(16.dp)
          .testTag("card_map_selected_station"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = st.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = if (st.isInterchange) "Estação de Integração" else "Estação de Linha",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            IconButton(onClick = { viewModel.selectStation(null) }) {
              Icon(Icons.Default.Close, contentDescription = "Fechar")
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            lines.forEach { line ->
              LineBadge(line = line, compact = true)
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = { showDetailSheet = true },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp)
            ) {
              Icon(Icons.Default.Info, contentDescription = null)
              Spacer(modifier = Modifier.width(4.dp))
              Text("Detalhes", fontSize = 12.sp)
            }

            Button(
              onClick = {
                viewModel.setRouteFromNearestTo(st)
                onNavigateToRoute(st)
              },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp)
            ) {
              Icon(Icons.Default.Navigation, contentDescription = null)
              Spacer(modifier = Modifier.width(4.dp))
              Text("Como Ir", fontSize = 12.sp)
            }
          }
        }
      }
    }
  }
}
