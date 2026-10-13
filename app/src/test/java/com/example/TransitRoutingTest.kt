package com.example

import com.example.data.model.RouteResult
import com.example.data.repository.TransitNetwork
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransitRoutingTest {

  private val network = TransitNetwork.fromJson(TestData.json())

  private fun route(from: String, to: String): RouteResult =
    network.findRoute(from, to).also { assertNotNull("Sem rota $from -> $to", it) }!!

  private fun RouteResult.lineSequence() = steps.filter { !it.isWalkingTransfer }.map { it.line.id }

  @Test
  fun `mesma linha nao tem baldeacao e intermediarias excluem origem e destino`() {
    val r = route("se", "paraiso")
    assertEquals(listOf("linha-1"), r.lineSequence())
    assertEquals(0, r.totalTransfers)
    val step = r.steps.single()
    assertEquals("se", step.fromStation.id)
    assertEquals("paraiso", step.toStation.id)
    assertEquals(listOf("japao-liberdade", "sao-joaquim", "vergueiro-sebrae"), step.intermediateStations.map { it.id })
    assertEquals(4, step.stopCount)
    assertEquals(4, r.totalStops)
  }

  @Test
  fun `bras ate sao miguel vai direto pela L12 sem troca fantasma em Tatuape`() {
    val r = route("bras", "sao-miguel-paulista")
    assertEquals(listOf("linha-12"), r.lineSequence())
    assertEquals(0, r.totalTransfers)
  }

  @Test
  fun `paraiso ate chacara klabin vai direto pela L2`() {
    val r = route("paraiso", "chacara-klabin")
    assertEquals(listOf("linha-2"), r.lineSequence())
  }

  @Test
  fun `penha nao e estacao da L11`() {
    val penha = network.getStationById("penha-lojas-besni")!!
    assertFalse(penha.lineIds.contains("linha-11"))
    val r = route("bras", "corinthians-itaquera")
    val l11Stations = r.steps.filter { it.line.id == "linha-11" }
      .flatMap { listOf(it.fromStation, it.toStation) + it.intermediateStations }
    assertTrue(l11Stations.none { it.id == "penha-lojas-besni" })
  }

  @Test
  fun `paulista ate consolacao usa transferencia a pe`() {
    val r = route("fradique-coutinho", "clinicas")
    assertTrue("Esperava transferência a pé: ${r.steps}", r.steps.any { it.isWalkingTransfer })
    assertEquals(listOf("linha-4", "linha-2"), r.lineSequence())
  }

  @Test
  fun `rota com troca lista passos encadeados`() {
    val r = route("faria-lima-pagbank", "se")
    for ((a, b) in r.steps.zipWithNext()) assertEquals(a.toStation.id, b.fromStation.id)
    assertEquals("faria-lima-pagbank", r.steps.first().fromStation.id)
    assertEquals("se", r.steps.last().toStation.id)
    assertTrue(r.totalTransfers <= 1)
  }

  @Test
  fun `prefere menos baldeacoes quando o tempo e parecido`() {
    // Luz -> Paulista: L4 direta, sem trocar
    val r = route("luz", "paulista")
    assertEquals(listOf("linha-4"), r.lineSequence())
  }

  @Test
  fun `nao roteia por trecho em obras`() {
    // L2 ainda não chega na Penha: Vila Prudente -> Penha tem que usar outras linhas
    val r = route("vila-prudente", "penha-lojas-besni")
    assertTrue(r.steps.none { it.line.id == "linha-2" && it.toStation.id == "penha-lojas-besni" })
    // L6 não liga Perdizes a Higienópolis ainda
    assertFalse(network.isOperationalSegment("linha-6", "perdizes", "higienopolis-mackenzie"))
  }

  @Test
  fun `estacao planejada nao e origem nem aparece no perto de mim`() {
    assertNull(network.findRoute("brasilandia", "se"))
    val near = network.getNearestStations(-23.47, -46.69, limit = 50)
    assertTrue(near.none { it.station.id == "brasilandia" })
  }

  @Test
  fun `L12 passa direto por estacao em obras no meio da linha`() {
    val r = route("comendador-ermelino", "sao-miguel-paulista")
    assertEquals(listOf("linha-12"), r.lineSequence())
    assertEquals(1, r.totalStops)
  }

  @Test
  fun `L6 opera entre Joao Paulo I e Perdizes`() {
    val r = route("joao-paulo-i", "perdizes")
    assertEquals(listOf("linha-6"), r.lineSequence())
  }

  @Test
  fun `L17 tem ramal Brooklin Paulista - Washington Luis`() {
    val r = route("morumbi-claro", "washington-luis")
    assertEquals(listOf("linha-17"), r.lineSequence())
    assertTrue(r.steps.single().intermediateStations.any { it.id == "brooklin-paulista" })
  }

  @Test
  fun `busca ignora acentos`() {
    val res = network.getNearestStations(-23.55, -46.63, searchQuery = "sao bento")
    assertTrue(res.any { it.station.id == "sao-bento" })
  }

  @Test
  fun `tempo estimado e plausivel`() {
    val r = route("jabaquara-comite-paralimpico-brasileiro", "tucuruvi")
    assertTrue("L1 inteira deveria levar 35-60 min, deu ${r.estimatedMinutes}", r.estimatedMinutes in 35..60)
  }

  // --- Conferências com o Mapa do Transporte Metropolitano (jul/2026) ---

  @Test
  fun `L7 termina em Palmeiras-Barra Funda e quem vai para Luz troca la`() {
    val l7 = network.getLineById("linha-7")!!
    assertEquals("palmeiras-barra-funda", l7.stationIds.last())
    val r = route("jundiai", "luz")
    assertEquals("linha-7", r.lineSequence().first())
    assertEquals("palmeiras-barra-funda", r.steps.first().toStation.id)
  }

  @Test
  fun `L10 e L11 saem de Palmeiras-Barra Funda e vao direto ate Bras`() {
    assertEquals("palmeiras-barra-funda", network.getLineById("linha-10")!!.stationIds.first())
    assertEquals("palmeiras-barra-funda", network.getLineById("linha-11")!!.stationIds.first())
    val r = route("palmeiras-barra-funda", "juventus-mooca")
    assertEquals(listOf("linha-10"), r.lineSequence())
  }

  @Test
  fun `Expresso Aeroporto existe mas nao e usado em trecho curto por causa da espera`() {
    assertTrue(network.getLineById("expresso-aeroporto")!!.limitedSchedule)
    val r = route("palmeiras-barra-funda", "luz")
    assertFalse(r.lineSequence().contains("expresso-aeroporto"))
  }

  @Test
  fun `operadoras conforme legenda do mapa oficial`() {
    val esperado = mapOf(
      "linha-4" to "Motiva", "linha-5" to "Motiva", "linha-11" to "TRIVIA",
      "linha-12" to "TRIVIA", "linha-13" to "TRIVIA", "linha-17" to "Metrô SP", "linha-7" to "TIC Trens"
    )
    esperado.forEach { (id, op) -> assertEquals(id, op, network.getLineById(id)!!.operator) }
  }

  @Test
  fun `origem igual ao destino retorna null`() {
    assertNull(network.findRoute("se", "se"))
  }
}
