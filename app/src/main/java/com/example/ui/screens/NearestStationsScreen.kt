package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransitStation
import com.example.ui.TransitViewModel
import com.example.ui.theme.contentColorFor
import com.example.ui.components.CurrentLocationHeader
import com.example.ui.components.LocationPresetDialog
import com.example.ui.components.StationCard
import com.example.ui.components.StationDetailBottomSheet

@Composable
fun NearestStationsScreen(
  viewModel: TransitViewModel,
  onNavigateToMap: (TransitStation?) -> Unit,
  onNavigateToRoute: (TransitStation?) -> Unit,
  onRequestLocationPermission: () -> Unit,
  modifier: Modifier = Modifier
) {
  val userLocation by viewModel.userLocation.collectAsState()
  val isLocating by viewModel.isLocating.collectAsState()
  val nearestStations by viewModel.nearestStations.collectAsState()
  val selectedLineFilter by viewModel.selectedLineFilter.collectAsState()
  val searchQuery by viewModel.searchQuery.collectAsState()
  val selectedStation by viewModel.selectedStation.collectAsState()

  var showLocationDialog by remember { mutableStateOf(false) }

  if (showLocationDialog) {
    LocationPresetDialog(
      currentLocation = userLocation,
      onDismiss = { showLocationDialog = false },
      onSelectRealGps = {
        onRequestLocationPermission()
        viewModel.refreshRealLocation()
      },
      onSelectPreset = { preset ->
        viewModel.setLocation(preset)
      }
    )
  }

  selectedStation?.let { st ->
    val lines = viewModel.repository.getLinesForStation(st)
    StationDetailBottomSheet(
      station = st,
      lines = lines,
      plannedLines = viewModel.repository.getPlannedLinesForStation(st),
      onDismiss = { viewModel.selectStation(null) },
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

    // Location Bar
    CurrentLocationHeader(
      locationState = userLocation,
      isLocating = isLocating,
      onRefreshGps = {
        onRequestLocationPermission()
        viewModel.refreshRealLocation()
      },
      onChangeLocation = { showLocationDialog = true }
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Search text field
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { viewModel.setSearchQuery(it) },
      placeholder = { Text("Buscar estação ou linha...", fontSize = 13.sp) },
      leadingIcon = {
        Icon(
          imageVector = Icons.Default.Search,
          contentDescription = null,
          modifier = Modifier.size(20.dp)
        )
      },
      trailingIcon = {
        if (searchQuery.isNotEmpty()) {
          IconButton(onClick = { viewModel.setSearchQuery("") }) {
            Icon(Icons.Default.Clear, contentDescription = "Limpar")
          }
        }
      },
      singleLine = true,
      shape = RoundedCornerShape(12.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
      ),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("input_search_stations")
    )

    Spacer(modifier = Modifier.height(10.dp))

    // Horizontal filter chips by line
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      FilterChip(
        selected = selectedLineFilter == null,
        onClick = { viewModel.setLineFilter(null) },
        label = { Text("Todas as Linhas", fontSize = 12.sp) },
        shape = RoundedCornerShape(8.dp)
      )

      viewModel.allLines.forEach { line ->
        val isSelected = selectedLineFilter == line.id
        FilterChip(
          selected = isSelected,
          onClick = { viewModel.setLineFilter(line.id) },
          label = {
            Text(
              text = if (!line.number.isNullOrEmpty()) "L${line.number} ${line.name}" else line.name,
              fontSize = 12.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = line.composeColor,
            selectedLabelColor = contentColorFor(line.composeColor)
          ),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.testTag("filter_chip_${line.id}")
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Station list
    if (nearestStations.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            imageVector = Icons.Default.FilterList,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Nenhuma estação encontrada para esse filtro.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        itemsIndexed(nearestStations, key = { _, item -> item.station.id }) { index, stDist ->
          StationCard(
            stationDistance = stDist,
            isHeroNearest = index == 0 && searchQuery.isBlank() && selectedLineFilter == null,
            onStationClick = { viewModel.selectStation(it) },
            onViewOnMap = {
              viewModel.selectStation(it)
              onNavigateToMap(it)
            },
            onRouteClick = {
              viewModel.setRouteFromNearestTo(it)
              onNavigateToRoute(it)
            }
          )
        }
      }
    }
  }
}
