package com.example.data.repository

import android.content.Context
import com.example.data.model.RouteResult
import com.example.data.model.StationDistance
import com.example.data.model.TransitLine
import com.example.data.model.TransitStation
import com.example.data.model.UserLocationState

/**
 * Fachada Android sobre [TransitNetwork]: só lê o JSON dos assets.
 * Toda a lógica (proximidade, roteamento) fica em [TransitNetwork], testável na JVM.
 */
class TransitRepository(private val network: TransitNetwork) {

  constructor(context: Context) : this(
    TransitNetwork.fromJson(
      context.assets.open(ASSET_FILE).bufferedReader().use { it.readText() }
    )
  )

  fun getAllLines(): List<TransitLine> = network.lines

  fun getAllStations(): List<TransitStation> = network.stations

  fun getStationById(id: String): TransitStation? = network.getStationById(id)

  fun getLineById(id: String): TransitLine? = network.getLineById(id)

  fun getLinesForStation(station: TransitStation): List<TransitLine> =
    network.getLinesForStation(station)

  fun getPlannedLinesForStation(station: TransitStation): List<TransitLine> =
    network.getPlannedLinesForStation(station)

  fun isOperationalSegment(lineId: String, aId: String, bId: String): Boolean =
    network.isOperationalSegment(lineId, aId, bId)

  fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float =
    TransitNetwork.calculateDistance(lat1, lon1, lat2, lon2)

  fun getNearestStations(
    userLat: Double,
    userLon: Double,
    limit: Int = 30,
    lineFilter: String? = null,
    searchQuery: String? = null
  ): List<StationDistance> =
    network.getNearestStations(userLat, userLon, limit, lineFilter, searchQuery)

  fun findRoute(originId: String, destId: String): RouteResult? =
    network.findRoute(originId, destId)

  companion object {
    const val ASSET_FILE = "sp_transit.json"

    val PRESET_LOCATIONS = listOf(
      UserLocationState(-23.5614, -46.6559, "Av. Paulista (MASP)", true),
      UserLocationState(-23.5505, -46.6333, "Praça da Sé (Centro)", true),
      UserLocationState(-23.5674, -46.6934, "Faria Lima / Pinheiros", true),
      UserLocationState(-23.5260, -46.6670, "Barra Funda (Memorial)", true),
      UserLocationState(-23.5423, -46.4710, "Itaquera (Arena)", true),
      UserLocationState(-23.6540, -46.7095, "Santo Amaro (Largo 13)", true),
      UserLocationState(-23.6266, -46.6615, "Aeroporto de Congonhas", true),
      UserLocationState(-23.4330, -46.4938, "Aeroporto de Guarulhos", true),
      UserLocationState(-23.5855, -46.6911, "Shopping Cidade Jardim", true),
      UserLocationState(-23.5404, -46.5762, "Tatuapé (Zona Leste)", true)
    )
  }
}
