package com.example

import com.example.data.repository.TransitNetwork
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Valida o sp_transit.json. Se alguém editar o JSON e quebrar a consistência, o CI pega.
 */
class TransitDataIntegrityTest {

  private val json: String = TestData.json()
  private val network = TransitNetwork.fromJson(json)
  private val stationIds = network.stations.map { it.id }.toSet()
  private val lineIds = network.lines.map { it.id }.toSet()

  @Test
  fun `ids de estação são únicos`() {
    val dups = network.stations.groupBy { it.id }.filter { it.value.size > 1 }.keys
    assertTrue("IDs duplicados: $dups", dups.isEmpty())
  }

  @Test
  fun `toda estação citada numa linha existe no cadastro`() {
    val missing = network.lines.flatMap { line ->
      line.allStationIds.filter { it !in stationIds }.map { "${line.id}: $it" }
    }
    assertTrue("Estações inexistentes: $missing", missing.isEmpty())
  }

  @Test
  fun `estação e linha se referenciam nos dois sentidos`() {
    val errors = mutableListOf<String>()
    for (line in network.lines) {
      for (sid in line.allStationIds) {
        val st = network.getStationById(sid)!!
        if (line.id !in st.lineIds && line.id !in st.plannedLineIds) {
          errors += "$sid não lista ${line.id}"
        }
      }
    }
    for (st in network.stations) {
      for (lid in st.lineIds + st.plannedLineIds) {
        val line = network.getLineById(lid)
        if (line == null) errors += "${st.id} cita linha inexistente $lid"
        else if (st.id !in line.allStationIds) errors += "${line.id} não contém ${st.id}"
      }
    }
    assertTrue(errors.joinToString("\n"), errors.isEmpty())
  }

  @Test
  fun `estação em obras não aparece como linha em operação`() {
    val bad = network.stations.filter { it.status != "operational" && it.lineIds.isNotEmpty() }
    assertTrue("Estações planejadas com linhas ativas: ${bad.map { it.id }}", bad.isEmpty())
  }

  @Test
  fun `coordenadas dentro da Grande SP`() {
    val out = network.stations.filter { it.lat !in -24.2..-22.9 || it.lon !in -47.2..-46.0 }
    assertTrue("Fora da região: ${out.map { it.id }}", out.isEmpty())
  }

  @Test
  fun `estações vizinhas não estão absurdamente longe`() {
    val far = mutableListOf<String>()
    // Serviços expressos (Expresso Aeroporto) pulam estações de propósito
    for (line in network.lines.filter { it.mode != "airport_express" }) for (seq in line.allSequences) {
      seq.zipWithNext().forEach { (a, b) ->
        val sa = network.getStationById(a)!!
        val sb = network.getStationById(b)!!
        val d = TransitNetwork.calculateDistance(sa.lat, sa.lon, sb.lat, sb.lon)
        // Expresso Leste (L11) Tatuapé–Itaquera tem ~10,7 km sem parada
        if (d > 12000) far += "${line.id}: $a -> $b (${d.toInt()} m)"
      }
    }
    assertTrue(far.joinToString("\n"), far.isEmpty())
  }

  @Test
  fun `transferências a pé apontam para estações existentes`() {
    val transfers = JSONObject(json).getJSONArray("transfers")
    for (i in 0 until transfers.length()) {
      val t = transfers.getJSONObject(i)
      assertTrue(t.getString("from") in stationIds)
      assertTrue(t.getString("to") in stationIds)
    }
  }

  @Test
  fun `cores das linhas são hex válido`() {
    network.lines.forEach { assertTrue(it.id, Regex("^#[0-9A-Fa-f]{6}$").matches(it.colorHex)) }
  }

  @Test
  fun `toda estação em operação é alcançável a partir da Sé`() {
    val unreachable = network.stations
      .filter { it.isOperational && it.id != "se" }
      .filter { network.findRoute("se", it.id) == null }
      .map { it.id }
    assertEquals("Sem rota a partir da Sé: $unreachable", emptyList<String>(), unreachable)
  }

  @Test
  fun `todas as linhas referenciadas existem`() {
    val unknown = network.stations.flatMap { it.lineIds + it.plannedLineIds }.filter { it !in lineIds }
    assertTrue("Linhas desconhecidas: $unknown", unknown.isEmpty())
  }
}

internal object TestData {
  /** Testes unitários rodam com working dir = módulo app/. */
  fun json(): String = listOf(
    File("src/main/assets/sp_transit.json"),
    File("app/src/main/assets/sp_transit.json")
  ).first { it.exists() }.readText()
}
