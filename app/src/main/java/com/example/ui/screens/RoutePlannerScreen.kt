package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RouteResult
import com.example.data.model.RouteStep
import com.example.data.model.TransitStation
import com.example.data.repository.TransitNetwork
import com.example.ui.TransitViewModel
import com.example.ui.components.LineBadge

@Composable
fun RoutePlannerScreen(
  viewModel: TransitViewModel,
  modifier: Modifier = Modifier
) {
  val routeOrigin by viewModel.routeOrigin.collectAsState()
  val routeDestination by viewModel.routeDestination.collectAsState()
  val routeResult by viewModel.routeResult.collectAsState()
  val nearestStations by viewModel.nearestStations.collectAsState()

  var isSelectingOrigin by remember { mutableStateOf(false) }
  var isSelectingDestination by remember { mutableStateOf(false) }

  // Origem padrão = estação mais próxima (efeito, não chamada direta durante a composição)
  val nearestId = nearestStations.firstOrNull()?.station?.id
  LaunchedEffect(nearestId) {
    if (viewModel.routeOrigin.value == null && nearestStations.isNotEmpty()) {
      viewModel.setRouteOrigin(nearestStations.first().station)
    }
  }

  // Só estações em operação podem ser origem/destino
  val selectableStations = remember(viewModel.allStations) {
    viewModel.allStations.filter { it.isOperational }.sortedBy { it.name }
  }

  if (isSelectingOrigin) {
    StationPickerDialog(
      title = "Selecione a Estação de Origem",
      stations = selectableStations,
      onDismiss = { isSelectingOrigin = false },
      onSelect = {
        viewModel.setRouteOrigin(it)
        isSelectingOrigin = false
      }
    )
  }

  if (isSelectingDestination) {
    StationPickerDialog(
      title = "Selecione a Estação de Destino",
      stations = selectableStations,
      onDismiss = { isSelectingDestination = false },
      onSelect = {
        viewModel.setRouteDestination(it)
        isSelectingDestination = false
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
      text = "Planejador de Trajeto",
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground
    )
    Text(
      text = "Calcule o menor caminho e baldeações de linha",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Origin and destination selectors card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
      ),
      border = androidx.compose.foundation.BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
      )
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        // Origin row
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { isSelectingOrigin = true }
            .padding(vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Place,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text("Origem (Embarque)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
              text = routeOrigin?.name ?: "Toque para escolher origem",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
          horizontalArrangement = Arrangement.Center
        ) {
          IconButton(
            onClick = { viewModel.swapRouteEndpoints() },
            modifier = Modifier
              .size(36.dp)
              .testTag("btn_swap_endpoints")
          ) {
            Icon(
              imageVector = Icons.Default.SwapVert,
              contentDescription = "Inverter pontos",
              tint = MaterialTheme.colorScheme.primary
            )
          }
        }

        // Destination row
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { isSelectingDestination = true }
            .padding(vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.PinDrop,
            contentDescription = null,
            tint = Color(0xFFEF4444),
            modifier = Modifier.size(22.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text("Destino (Desembarque)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
              text = routeDestination?.name ?: "Toque para escolher destino",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Results or Empty state
    if (routeResult != null) {
      RouteResultView(result = routeResult!!)
    } else {
      if (routeOrigin != null && routeDestination != null && routeOrigin?.id != routeDestination?.id) {
        Text(
          text = "Não encontramos um trajeto em operação entre essas estações.",
          color = MaterialTheme.colorScheme.error,
          fontSize = 13.sp,
          modifier = Modifier.padding(bottom = 8.dp)
        )
      }
      SuggestedRoutesSection(
        onSelectRoute = { origName, destName ->
          // Prefere nome exato ("Paulista" não pode virar "Brooklin Paulista")
          fun pick(name: String) = selectableStations.find { it.name.equals(name, true) }
            ?: selectableStations.find { it.name.contains(name, true) }
          val orig = pick(origName)
          val dest = pick(destName)
          if (orig != null && dest != null) {
            viewModel.setRouteOrigin(orig)
            viewModel.setRouteDestination(dest)
          }
        }
      )
    }
  }
}

@Composable
fun RouteResultView(result: RouteResult) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    item {
      // Summary card
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceAround
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "~${result.estimatedMinutes} min",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
              )
            }
            Text("Tempo estimado", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }

          Box(
            modifier = Modifier
              .width(1.dp)
              .height(30.dp)
              .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
          )

          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "${result.totalStops} estações",
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = if (result.totalTransfers == 0) "Linha direta" else "${result.totalTransfers} baldeação(ões)",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }

    item {
      Text(
        text = "Passo a Passo do Trajeto:",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 4.dp)
      )
    }

    items(result.steps) { step ->
      if (step.isWalkingTransfer) WalkingTransferCard(step = step) else RouteStepCard(step = step)
    }
  }
}

@Composable
fun RouteStepCard(step: RouteStep) {
  var showIntermediate by remember { mutableStateOf(false) }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        LineBadge(line = step.line)

        Text(
          text = "${step.stopCount} parada(s) • ~${step.minutes} min",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
      }

      if (step.line.limitedSchedule) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Horários reduzidos — confira a grade da ${step.line.operator}. Tempo inclui ~15 min de espera.",
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.error
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(10.dp)
            .background(step.line.composeColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Embarque: ${step.fromStation.name}",
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp
        )
      }

      if (step.intermediateStations.isNotEmpty()) {
        Spacer(modifier = Modifier.height(4.dp))
        Row(
          modifier = Modifier
            .clickable { showIntermediate = !showIntermediate }
            .padding(vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = if (showIntermediate) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "${step.intermediateStations.size} estações intermediárias",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        AnimatedVisibility(visible = showIntermediate) {
          Column(modifier = Modifier.padding(start = 18.dp, top = 2.dp, bottom = 4.dp)) {
            step.intermediateStations.forEach { inter ->
              Text(
                text = "• ${inter.name}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 2.dp)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(10.dp)
            .background(Color.White, CircleShape)
            .border(2.dp, step.line.composeColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Desembarque / Baldeação: ${step.toStation.name}",
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp
        )
      }
    }
  }
}

@Composable
fun WalkingTransferCard(step: RouteStep) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp)
      )
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text(
          text = "Transferência a pé (~${step.minutes} min)",
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp
        )
        Text(
          text = "${step.fromStation.name} → ${step.toStation.name} pelo corredor de integração",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
fun SuggestedRoutesSection(onSelectRoute: (String, String) -> Unit) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Spacer(modifier = Modifier.height(16.dp))
    Icon(
      imageVector = Icons.Default.DirectionsTransit,
      contentDescription = null,
      modifier = Modifier.size(48.dp),
      tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = "Escolha a origem e destino para ver o trajeto",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(20.dp))
    Text(
      text = "Trajetos frequentes em São Paulo:",
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.align(Alignment.Start)
    )
    Spacer(modifier = Modifier.height(8.dp))

    val suggestions = listOf(
      Pair("Paulista", "Sé"),
      Pair("Faria Lima", "Luz"),
      Pair("Itaquera", "Palmeiras-Barra Funda"),
      Pair("Santo Amaro", "Consolação"),
      Pair("Aeroporto-Guarulhos", "Luz")
    )

    suggestions.forEach { (orig, dest) ->
      OutlinedButton(
        onClick = { onSelectRoute(orig, dest) },
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp),
        shape = RoundedCornerShape(10.dp)
      ) {
        Text("$orig  ➔  $dest", fontSize = 13.sp)
      }
    }
  }
}

@Composable
fun StationPickerDialog(
  title: String,
  stations: List<TransitStation>,
  onDismiss: () -> Unit,
  onSelect: (TransitStation) -> Unit
) {
  var search by remember { mutableStateOf("") }
  val query = TransitNetwork.normalize(search)
  val filtered = stations.filter { TransitNetwork.normalize(it.name).contains(query) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title, fontWeight = FontWeight.Bold) },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
          value = search,
          onValueChange = { search = it },
          placeholder = { Text("Filtrar estação...") },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
          trailingIcon = {
            if (search.isNotEmpty()) {
              IconButton(onClick = { search = "" }) {
                Icon(Icons.Default.Clear, contentDescription = null)
              }
            }
          },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(modifier = Modifier.height(280.dp)) {
          items(filtered) { st ->
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect(st) }
                .padding(vertical = 6.dp),
              color = Color.Transparent
            ) {
              Text(
                text = st.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 8.dp)
              )
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancelar")
      }
    }
  )
}
