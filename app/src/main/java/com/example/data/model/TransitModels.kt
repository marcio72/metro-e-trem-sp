package com.example.data.model

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

data class TransitLine(
  val id: String,
  val slug: String,
  val name: String,
  val number: String?,
  val colorHex: String,
  val mode: String,
  val operator: String,
  val status: String,
  /** Tronco principal, em ordem. */
  val stationIds: List<String>,
  /** Ramais (ex.: L17 Brooklin Paulista → Washington Luís). Cada ramal é uma sequência em ordem. */
  val branches: List<List<String>> = emptyList(),
  /** Serviço com horários reduzidos (ex.: Expresso Aeroporto) — a espera é maior. */
  val limitedSchedule: Boolean = false
) {
  val composeColor: Color
    get() = parseHexColor(colorHex)

  val displayLabel: String
    get() = if (number != null && number.isNotEmpty()) "Linha $number - $name" else name

  val isUnderConstruction: Boolean
    get() = status.equals("planned", ignoreCase = true) || status.equals("partial", ignoreCase = true)

  val isPlanned: Boolean
    get() = status.equals("planned", ignoreCase = true)

  /** Tronco + ramais, cada um como uma sequência contínua. */
  val allSequences: List<List<String>>
    get() = listOf(stationIds) + branches

  /** Todas as estações distintas da linha (tronco + ramais). */
  val allStationIds: List<String>
    get() = allSequences.flatten().distinct()
}

data class TransitStation(
  val id: String,
  val slug: String,
  val name: String,
  val lat: Double,
  val lon: Double,
  /** Linhas que ATENDEM a estação hoje. */
  val lineIds: List<String>,
  val status: String,
  val isInterchange: Boolean,
  /** Linhas que vão passar pela estação, mas ainda não operam nela. */
  val plannedLineIds: List<String> = emptyList()
) {
  val isOperational: Boolean
    get() = status.equals("operational", ignoreCase = true) && lineIds.isNotEmpty()

  fun servesLine(lineId: String): Boolean = isOperational && lineIds.contains(lineId)
}

/** Ligação a pé entre duas estações com IDs diferentes (ex.: Paulista ↔ Consolação). */
data class WalkingTransfer(
  val fromId: String,
  val toId: String,
  val walkMinutes: Int
)

data class StationDistance(
  val station: TransitStation,
  val distanceMeters: Float,
  val walkingMinutes: Int,
  val lines: List<TransitLine>
) {
  val formattedDistance: String
    get() = if (distanceMeters < 1000f) {
      "${distanceMeters.roundToInt()} m"
    } else {
      String.format(java.util.Locale.US, "%.1f km", distanceMeters / 1000f)
    }

  val formattedWalkTime: String
    get() = when {
      walkingMinutes <= 1 -> "1 min a pé"
      walkingMinutes < 60 -> "$walkingMinutes min a pé"
      else -> "${walkingMinutes / 60} h ${walkingMinutes % 60} min"
    }
}

data class UserLocationState(
  val lat: Double,
  val lon: Double,
  val label: String,
  val isSimulated: Boolean
)

data class RouteStep(
  /** Linha usada no trecho. Em transferência a pé, é a linha que será pega em seguida. */
  val line: TransitLine,
  val fromStation: TransitStation,
  val toStation: TransitStation,
  /** Estações entre [fromStation] e [toStation], sem incluir nenhuma das duas. */
  val intermediateStations: List<TransitStation>,
  /** Quantidade de trechos percorridos (paradas até o desembarque). 0 em transferência a pé. */
  val stopCount: Int,
  val minutes: Int = 0,
  val isWalkingTransfer: Boolean = false
)

data class RouteResult(
  val origin: TransitStation,
  val destination: TransitStation,
  val steps: List<RouteStep>,
  val totalStops: Int,
  val totalTransfers: Int,
  val estimatedMinutes: Int
)

fun parseHexColor(hexString: String?): Color {
  if (hexString.isNullOrEmpty()) return Color(0xFF0455A1)
  val cleanHex = hexString.replace("#", "")
  return try {
    when (cleanHex.length) {
      6 -> Color(android.graphics.Color.parseColor("#$cleanHex"))
      8 -> Color(android.graphics.Color.parseColor("#$cleanHex"))
      else -> Color(0xFF0455A1)
    }
  } catch (e: Exception) {
    Color(0xFF0455A1)
  }
}
