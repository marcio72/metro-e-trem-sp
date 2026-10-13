package com.example.data.repository

import com.example.data.model.RouteResult
import com.example.data.model.RouteStep
import com.example.data.model.StationDistance
import com.example.data.model.TransitLine
import com.example.data.model.TransitStation
import com.example.data.model.WalkingTransfer
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import java.util.PriorityQueue
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Lógica pura da rede (sem dependência de Context), para poder ser testada na JVM.
 *
 * Regras do grafo:
 * - Só entram trechos de linhas não "planned", entre estações que operam naquela linha
 *   ([TransitStation.servesLine]). Estações em obras no meio de uma linha em operação
 *   (ex.: União de Vila Nova na L12) são "puladas" — o trem passa direto.
 * - O roteamento é Dijkstra sobre o estado (estação, linha), então uma baldeação é uma
 *   aresta com custo próprio e trechos compartilhados (Brás–Tatuapé na L11/L12,
 *   Paraíso–Ana Rosa na L1/L2) não geram trocas fantasmas.
 */
class TransitNetwork(
  val lines: List<TransitLine>,
  val stations: List<TransitStation>,
  val walkingTransfers: List<WalkingTransfer>
) {

  private val stationMap: Map<String, TransitStation> = stations.associateBy { it.id }
  private val lineMap: Map<String, TransitLine> = lines.associateBy { it.id }

  /** stationId -> lista de arestas de viagem que saem dela. */
  private val rideEdges: Map<String, List<RideEdge>> = buildRideEdges()

  /** stationId -> transferências a pé que saem dela (bidirecional). */
  private val walkEdges: Map<String, List<WalkingTransfer>> = walkingTransfers
    .flatMap { listOf(it, WalkingTransfer(it.toId, it.fromId, it.walkMinutes)) }
    .filter { stationMap[it.fromId]?.isOperational == true && stationMap[it.toId]?.isOperational == true }
    .groupBy { it.fromId }

  fun getStationById(id: String): TransitStation? = stationMap[id]

  fun getLineById(id: String): TransitLine? = lineMap[id]

  fun getLinesForStation(station: TransitStation): List<TransitLine> =
    station.lineIds.mapNotNull { lineMap[it] }

  fun getPlannedLinesForStation(station: TransitStation): List<TransitLine> =
    station.plannedLineIds.mapNotNull { lineMap[it] }

  // ---------------------------------------------------------------------------
  // Estações próximas
  // ---------------------------------------------------------------------------

  fun getNearestStations(
    userLat: Double,
    userLon: Double,
    limit: Int = 30,
    lineFilter: String? = null,
    searchQuery: String? = null,
    includeNonOperational: Boolean = false
  ): List<StationDistance> {
    val query = searchQuery?.takeIf { it.isNotBlank() }?.let(::normalize)
    return stations
      .asSequence()
      .filter { includeNonOperational || it.isOperational }
      .filter { lineFilter.isNullOrEmpty() || it.lineIds.contains(lineFilter) }
      .filter { station ->
        query == null ||
          normalize(station.name).contains(query) ||
          station.lineIds.any { id -> lineMap[id]?.let { normalize(it.name).contains(query) } == true }
      }
      .map { station ->
        val dist = calculateDistance(userLat, userLon, station.lat, station.lon)
        StationDistance(
          station = station,
          distanceMeters = dist,
          walkingMinutes = (dist / WALK_METERS_PER_MIN).roundToInt().coerceAtLeast(1),
          lines = getLinesForStation(station)
        )
      }
      .sortedBy { it.distanceMeters }
      .take(limit)
      .toList()
  }

  // ---------------------------------------------------------------------------
  // Rotas
  // ---------------------------------------------------------------------------

  fun findRoute(originId: String, destId: String): RouteResult? {
    val origin = stationMap[originId] ?: return null
    val destination = stationMap[destId] ?: return null
    if (origin.id == destination.id) return null
    if (!origin.isOperational || !destination.isOperational) return null

    val best = HashMap<State, Double>()
    val prev = HashMap<State, Pair<State, Move>>()
    val queue = PriorityQueue<Node>(compareBy<Node> { it.cost }.thenBy { it.transfers })

    for (lineId in origin.lineIds) {
      val s = State(origin.id, lineId)
      val c = boardingPenalty(lineId)
      best[s] = c
      queue.add(Node(s, c, 0))
    }

    var target: State? = null
    while (queue.isNotEmpty()) {
      val node = queue.poll()
      val cur = node.state
      if (node.cost > (best[cur] ?: Double.MAX_VALUE)) continue
      if (cur.stationId == destination.id) { target = cur; break }

      fun relax(next: State, move: Move, extraTransfers: Int) {
        val nc = node.cost + move.minutes
        if (nc < (best[next] ?: Double.MAX_VALUE)) {
          best[next] = nc
          prev[next] = cur to move
          queue.add(Node(next, nc, node.transfers + extraTransfers))
        }
      }

      // 1) Seguir na mesma linha
      rideEdges[cur.stationId].orEmpty()
        .filter { it.lineId == cur.lineId }
        .forEach { e -> relax(State(e.toId, e.lineId), Move(MoveKind.RIDE, e.minutes), 0) }

      // 2) Baldeação na mesma estação
      stationMap[cur.stationId]?.lineIds.orEmpty()
        .filter { it != cur.lineId }
        .forEach { other ->
          relax(State(cur.stationId, other), Move(MoveKind.CHANGE, TRANSFER_PENALTY_MIN + boardingPenalty(other)), 1)
        }

      // 3) Transferência a pé para outra estação
      walkEdges[cur.stationId].orEmpty().forEach { w ->
        stationMap[w.toId]?.lineIds.orEmpty().forEach { other ->
          relax(State(w.toId, other), Move(MoveKind.WALK, w.walkMinutes + TRANSFER_PENALTY_MIN + boardingPenalty(other)), 1)
        }
      }
    }

    val end = target ?: return null
    val path = ArrayList<Pair<State, Move?>>()
    var s: State? = end
    while (s != null) {
      val p = prev[s]
      path.add(s to p?.second)
      s = p?.first
    }
    path.reverse()

    val steps = buildSteps(path)
    if (steps.isEmpty()) return null
    val rideSteps = steps.filter { !it.isWalkingTransfer }

    return RouteResult(
      origin = origin,
      destination = destination,
      steps = steps,
      totalStops = rideSteps.sumOf { it.stopCount },
      totalTransfers = (rideSteps.size - 1).coerceAtLeast(0),
      estimatedMinutes = (best[end] ?: 0.0).roundToInt().coerceAtLeast(1)
    )
  }

  /**
   * Converte a sequência de estados em passos. path[i].second é o movimento que levou ao estado i
   * (null no primeiro).
   */
  private fun buildSteps(path: List<Pair<State, Move?>>): List<RouteStep> {
    val steps = mutableListOf<RouteStep>()
    var segment = mutableListOf(path.first().first.stationId)
    var segmentLine = path.first().first.lineId
    var segmentMinutes = 0.0

    fun closeSegment() {
      if (segment.size >= 2) {
        val st = segment.mapNotNull { stationMap[it] }
        steps.add(
          RouteStep(
            line = lineMap.getValue(segmentLine),
            fromStation = st.first(),
            toStation = st.last(),
            intermediateStations = st.subList(1, st.size - 1),
            stopCount = st.size - 1,
            minutes = segmentMinutes.roundToInt().coerceAtLeast(1)
          )
        )
      }
    }

    for (i in 1 until path.size) {
      val (state, move) = path[i]
      when (move!!.kind) {
        MoveKind.RIDE -> {
          segment.add(state.stationId)
          segmentMinutes += move.minutes
        }
        MoveKind.CHANGE -> {
          closeSegment()
          segment = mutableListOf(state.stationId)
          segmentLine = state.lineId
          segmentMinutes = 0.0
        }
        MoveKind.WALK -> {
          closeSegment()
          val from = stationMap.getValue(path[i - 1].first.stationId)
          val to = stationMap.getValue(state.stationId)
          steps.add(
            RouteStep(
              line = lineMap.getValue(state.lineId),
              fromStation = from,
              toStation = to,
              intermediateStations = emptyList(),
              stopCount = 0,
              minutes = (move.minutes - TRANSFER_PENALTY_MIN - boardingPenalty(state.lineId)).roundToInt(),
              isWalkingTransfer = true
            )
          )
          segment = mutableListOf(state.stationId)
          segmentLine = state.lineId
          segmentMinutes = 0.0
        }
      }
    }
    closeSegment()
    return steps
  }

  private fun buildRideEdges(): Map<String, List<RideEdge>> {
    val result = HashMap<String, MutableList<RideEdge>>()
    for (line in lines) {
      if (line.isPlanned) continue
      for (sequence in line.allSequences) {
        val operating = sequence.mapNotNull { stationMap[it] }.filter { it.servesLine(line.id) }
        for (i in 0 until operating.size - 1) {
          val a = operating[i]
          val b = operating[i + 1]
          val minutes = rideMinutes(a, b, line.mode)
          result.getOrPut(a.id) { mutableListOf() }.add(RideEdge(b.id, line.id, minutes))
          result.getOrPut(b.id) { mutableListOf() }.add(RideEdge(a.id, line.id, minutes))
        }
      }
    }
    return result
  }

  /** Pares de estações consecutivas que estão em operação para a linha (útil para desenhar o mapa). */
  fun isOperationalSegment(lineId: String, aId: String, bId: String): Boolean =
    rideEdges[aId].orEmpty().any { it.toId == bId && it.lineId == lineId }

  private fun boardingPenalty(lineId: String): Double =
    if (lineMap[lineId]?.limitedSchedule == true) LIMITED_SCHEDULE_WAIT_MIN else 0.0

  private fun rideMinutes(a: TransitStation, b: TransitStation, mode: String): Double {
    val speed = when (mode) {
      "commuter_rail", "airport_express" -> RAIL_METERS_PER_MIN
      "monorail" -> MONORAIL_METERS_PER_MIN
      else -> SUBWAY_METERS_PER_MIN
    }
    return max(1.0, calculateDistance(a.lat, a.lon, b.lat, b.lon) / speed + DWELL_MIN)
  }

  private data class State(val stationId: String, val lineId: String)
  private data class Node(val state: State, val cost: Double, val transfers: Int)
  private enum class MoveKind { RIDE, CHANGE, WALK }
  private data class Move(val kind: MoveKind, val minutes: Double)
  private data class RideEdge(val toId: String, val lineId: String, val minutes: Double)

  companion object {
    /** ~4,8 km/h */
    const val WALK_METERS_PER_MIN = 80f

    /** Velocidades comerciais médias aproximadas (já incluem aceleração/frenagem). */
    private const val SUBWAY_METERS_PER_MIN = 550.0    // ~33 km/h
    private const val RAIL_METERS_PER_MIN = 650.0      // ~39 km/h
    private const val MONORAIL_METERS_PER_MIN = 480.0  // ~29 km/h
    private const val DWELL_MIN = 0.5

    /** Custo de uma baldeação (caminhar na estação + esperar o próximo trem). */
    const val TRANSFER_PENALTY_MIN = 5.0

    /** Espera média extra em serviços de horário reduzido (Expresso Aeroporto). */
    const val LIMITED_SCHEDULE_WAIT_MIN = 15.0

    fun fromJson(json: String): TransitNetwork {
      val root = JSONObject(json)

      val lines = root.optJSONArray("lines").objects().map { o ->
        TransitLine(
          id = o.optString("id"),
          slug = o.optString("slug"),
          name = o.optString("name"),
          number = o.optString("number").takeIf { it.isNotEmpty() && it != "null" },
          colorHex = o.optString("colour", "#0455A1"),
          mode = o.optString("mode", "subway"),
          operator = o.optString("operator", "Metrô SP"),
          status = o.optString("status", "operational"),
          stationIds = o.optJSONArray("stations").strings(),
          limitedSchedule = o.optBoolean("limited_schedule", false),
          branches = o.optJSONArray("branches").let { arr ->
            if (arr == null) emptyList() else (0 until arr.length()).map { arr.getJSONArray(it).strings() }
          }
        )
      }

      val stations = root.optJSONArray("stations").objects().map { o ->
        val lineIds = o.optJSONArray("lines").strings()
        TransitStation(
          id = o.optString("id"),
          slug = o.optString("slug"),
          name = o.optString("name"),
          lat = o.optDouble("lat", -23.5505),
          lon = o.optDouble("lon", -46.6333),
          lineIds = lineIds,
          status = o.optString("status", "operational"),
          isInterchange = o.optBoolean("is_interchange", false) || lineIds.size > 1,
          plannedLineIds = o.optJSONArray("planned_lines").strings()
        )
      }

      val transfers = root.optJSONArray("transfers").objects().map { o ->
        WalkingTransfer(o.optString("from"), o.optString("to"), o.optInt("walk_minutes", 5))
      }

      // Estações com transferência a pé também contam como integração
      val walkIds = transfers.flatMap { listOf(it.fromId, it.toId) }.toSet()
      val finalStations = stations.map {
        if (it.id in walkIds && it.isOperational) it.copy(isInterchange = true) else it
      }

      return TransitNetwork(lines, finalStations, transfers)
    }

    /** Haversine em metros. */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
      val r = 6371000.0
      val dLat = Math.toRadians(lat2 - lat1)
      val dLon = Math.toRadians(lon2 - lon1)
      val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
      return (r * 2 * atan2(sqrt(a), sqrt(1 - a))).toFloat()
    }

    /** Minúsculas e sem acento: "sao" encontra "São". */
    fun normalize(text: String): String =
      Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase()
        .trim()

    private fun JSONArray?.objects(): List<JSONObject> =
      if (this == null) emptyList() else (0 until length()).map { getJSONObject(it) }

    private fun JSONArray?.strings(): List<String> =
      if (this == null) emptyList() else (0 until length()).map { getString(it) }
  }
}
