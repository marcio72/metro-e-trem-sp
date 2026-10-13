# Mudanças — revisão de dados e roteamento (13/10/2026)

## Conferência com o Mapa do Transporte Metropolitano (jul/2026)
- L7-Rubi termina em Palmeiras-Barra Funda (não vai mais até a Luz).
- L10-Turquesa e L11-Coral começam em Palmeiras-Barra Funda, passando pela Luz.
- Novo serviço Expresso Aeroporto (TRIVIA): Barra Funda – Luz – Guarulhos-Cecap – Aeroporto-Guarulhos,
  marcado como `limited_schedule` (o roteamento soma ~15 min de espera e a tela avisa para conferir horários).
- Operadoras atualizadas: L4/L5 Motiva, L11/L12/L13 TRIVIA, L15/L17 Metrô, L7 TIC Trens, L6 Linha Uni.
- L17 desenhada como no mapa: tronco Morumbi → Washington Luís, ramal Brooklin Paulista → Congonhas.
- Conferidas sem divergência: L1, L2, L3, L4, L5, L6, L9, L12, L13, L15.
- Falta: estação Ambuitá (L8, entre Amador Bueno e Santa Rita) — não há coordenada no JSON.

## Dados (`app/src/main/assets/sp_transit.json`, agora `version: 2`)
- Removidos 42 IDs de estação que eram citados nas linhas mas não existiam no cadastro (sem coordenadas).
- Traçados corrigidos:
  - L5-Lilás termina em Chácara Klabin.
  - L11-Coral não para na Penha (Expresso Leste vai direto Tatuapé → Corinthians-Itaquera).
  - L12-Safira sem "Tiquatira"; L7-Rubi sem "Bom Retiro".
  - L15-Prata começa em Vila Prudente (Ipiranga é extensão futura).
  - L17-Ouro reorganizada: tronco Morumbi → Aeroporto de Congonhas e ramal (`branches`)
    Brooklin Paulista → Washington Luís (→ Jabaquara, futuro).
- Novo campo por estação: `planned_lines` = linhas que vão passar ali mas ainda não operam
  (ex.: L2 na Penha, L6 em Higienópolis-Mackenzie/São Joaquim, L17 em Jabaquara/São Paulo-Morumbi).
  `lines` agora contém apenas linhas em operação na estação.
- Novo bloco `transfers`: transferência a pé Paulista ↔ Consolação (5 min).
- `is_interchange` recalculado.
- L6-Laranja: em operação assistida de João Paulo I a Perdizes (desde jul/2026). L17: 8 estações (Morumbi–Congonhas + ramal).

## Roteamento (`data/repository/TransitNetwork.kt`)
- Lógica separada do Android (`TransitNetwork`), `TransitRepository` só lê o asset.
- Dijkstra sobre estado (estação, linha): acaba com baldeações fantasmas em trechos compartilhados
  (Brás–Tatuapé L11/L12, Paraíso–Ana Rosa L1/L2).
- Custo em minutos: distância real ÷ velocidade média por modo (metrô/CPTM/monotrilho) + 0,5 min por parada,
  5 min por baldeação. Transferência a pé vira um passo próprio na rota.
- Só usa trechos em operação; estações em obras no meio de linha ativa são puladas (ex.: União de Vila Nova na L12).
- `intermediateStations` corrigido (antes pulava a 1ª estação e incluía o destino).
- Grafo montado uma vez só.
- "Perto de mim" não mostra estações em obras; busca ignora acentos ("sao" encontra "São").

## UI
- Rota: card de transferência a pé, tempo por trecho, aviso quando não há trajeto, picker só com estações em operação
  e busca sem acento, sugestão "Paulista" não cai mais em "Brooklin Paulista", origem padrão via `LaunchedEffect`.
- Mapa: trechos em obras tracejados, estações em obras como anel cinza, ramais desenhados,
  pulso do GPS em camada separada (não redesenha o mapa inteiro a cada frame),
  rótulos usam a cor do tema (antes eram brancos e sumiam no tema claro),
  botão "Centralizar em mim" centraliza de verdade, área do mapa calculada pelas estações.
- Catálogo de linhas: mostra "X de Y estações operando", inclui estações de ramais e marca "(em obras)".
- Bottom sheet da estação: mostra linhas futuras; botão de rota desabilitado em estação em obras.
- ViewModel: removido o `typealias TransitDestination`.

## Testes (`app/src/test`)
- `TransitDataIntegrityTest`: IDs únicos, referências linha↔estação, coordenadas, distâncias, cores,
  toda estação em operação alcançável a partir da Sé.
- `TransitRoutingTest`: 15 cenários de rota (sem troca fantasma, transferência a pé, obras, ramal L17, tempo plausível...).
- Adicionado `testImplementation(libs.org.json)` para os testes JVM.

## Pendências conhecidas
- O ramal da L17 hoje exige troca de trem em Brooklin Paulista; a rota mostra como trecho único.
- Texto fixo no bottom sheet ("Horário 04:40 às 00:00", acessibilidade) é genérico — não vem dos dados.
- Linhas futuras (19-Celeste, 20-Rosa, TIC) não estão no JSON.
