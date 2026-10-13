# Metrô & Trem SP - Localizador de Estações e Mapa da Rede

Aplicativo Android nativo em Jetpack Compose para localizar instantaneamente as estações de metrô e trem mais próximas da localização atual do usuário em São Paulo, com mapa interativo diagramático de alta precisão inspirado no Mapa Oficial do Transporte Metropolitano (Metrô, CPTM, ViaMobilidade, ViaQuatro).

---

### User Review & Critical Decisions

> [!IMPORTANT]
> O app integra cálculo de geolocalização com a malha metroferroviária de São Paulo, além de um mapa diagramático interativo vetorial navegável com zoom e pan para recriar a experiência do mapa oficial fornecido.

- **Localização**: Suporte a GPS em tempo real (com solicitação de permissão em tempo de execução) e modo de simulação/seleção rápida de pontos de referência paulistanos (Paulista, Sé, Pinheiros, Itaquera, Brás, Aeroporto) ideal para testes no emulador e usuários fora da estação.
- **Visualização do Mapa**: Mapa diagramático interativo renderizado em Jetpack Compose Canvas com navegação tátil fluida (zoom/pan), conexões coloridas com a identidade oficial de cada linha (Linhas 1 a 15) e nós de transferência clicáveis.
- **Catálogo de Estações e Rotas**: Base de dados integrada com coordenadas geográficas reais de estações do Metrô e CPTM, conexões entre linhas, transferências gratuitas, bicicletários e acessibilidade.

---

### 1. Overview & Core Concept

- **O que faz**:
  1. Detecta a localização GPS do usuário e elenca as estações de metrô e trem mais próximas, ordenadas por distância métrica e tempo estimado de caminhada.
  2. Apresenta um mapa interativo diagramático e visual que espelha as linhas e nós de conexão do Transporte Metropolitano de SP.
  3. Permite buscar qualquer estação ou linha, visualizar detalhes de integração, horários e planejar rotas entre estações (indicando onde trocar de linha).
- **Público-alvo**: Passageiros, turistas e moradores da Região Metropolitana de São Paulo que utilizam Metrô, CPTM e concessionárias privadas.
- **Valor Principal**: Rapidez para descobrir "qual metrô/trem está mais perto de mim agora", qual linha pegar e como navegar visualmente pela malha metropolitana.

---

### 2. User Experience & Visual Design

#### Fluxos Principais
1. **Tela "Perto de Mim" (Home)**:
   - Card hero com a estação mais próxima em destaque: nome da estação, distância (ex: 450 m), tempo a pé (~6 min), badges com as cores e nomes das linhas que atendem a estação.
   - Lista rolante das próximas 5 estações nas imediações com indicadores de proximidade.
   - Seletor rápido de localização simulada (ex: Centro/Sé, Paulista, Faria Lima, Tatuapé, Morumbi) para demonstração ou teste quando o GPS estiver indisponível.
2. **Tela "Mapa da Rede"**:
   - Canvas diagramático interativo com suporte a gestos multitoque (pinch-to-zoom e arrasto).
   - Renderização das linhas com as cores oficiais:
     - Linha 1 - Azul (`#00539F`)
     - Linha 2 - Verde (`#008060`)
     - Linha 3 - Vermelha (`#EE3E34`)
     - Linha 4 - Amarela (`#FED304`)
     - Linha 5 - Lilás (`#9B3894`)
     - Linha 7 - Rubi (`#CA005D`)
     - Linha 8 - Diamante (`#97999B`)
     - Linha 9 - Esmeralda (`#00A88F`)
     - Linha 10 - Turquesa (`#007C8F`)
     - Linha 11 - Coral (`#F04E23`)
     - Linha 12 - Safira (`#1C146B`)
     - Linha 13 - Jade (`#00B050`)
     - Linha 15 - Prata (`#999999`)
   - Toque em nós de estação para abrir modal / bottom sheet com detalhes imediatos e botão "Traçar Rota".
3. **Tela "Linhas & Catálogo"**:
   - Acordeom ou lista de todas as linhas de SP com status operacional fictício/informativo, número de estações e pesquisa rápida por texto.
4. **Tela "Rotas & Conexões"**:
   - Seletor de Origem e Destino com cálculo do melhor trajeto e estações de transferência (ex: de Faria Lima para Sé via Linha 4-Amarela e Linha 1-Azul/Luz).

#### Identidade Visual & Tema
- **Estilo**: Utilitário moderno, inspirado no design de sinalização de transporte público internacional (Mass Transit Design Language) e Material Design 3.
- **Cores**: Fundo neutro sofisticado (modo escuro elegante `#121316` e modo claro `#F8F9FA`) com uso vibrante dos tons oficiais das linhas para forte reconhecimento visual imediato.
- **Tipografia**: Hierarquia limpa, números destacados em formato tabular para distâncias e tempos de caminhada.

---

### 3. Key Product Decisions & Trade-Offs

- **Decisão 1: Renderização de Mapa Diagramático via Canvas Compose**:
  - *Abordagem*: Implementar um renderizador gráfico vetorial usando `androidx.compose.foundation.Canvas` com geometria diagramática esquemática escalável.
  - *Por que*: Permite renderização fluida a 60fps sem dependência de chaves de API pagas ou consumo pesado de dados móveis, mantendo fidelidade estética com o mapa esquemático oficial da CPTM/Metrô fornecido na imagem.
- **Decisão 2: Base de Estações Georreferenciada Embarcada**:
  - *Abordagem*: Dataset em Kotlin com as principais estações e nós de integração da malha de SP contendo latitude, longitude, linhas atendidas e conexões.
  - *Por que*: Funciona 100% offline em túneis e subsolos de estações onde não há sinal de internet, calculando distância por Haversine instantaneamente.
- **Decisão 3: Gerenciamento Gracioso de Permissão de Localização**:
  - *Abordagem*: Solicitação de permissão de GPS em Compose (`rememberLauncherForActivityResult`) com fallback para predefinições de localização em São Paulo.

---

### 4. Technical Architecture & Data Strategy

```
┌──────────────────────────────────────────────────────────────┐
│                      UI Layer (Compose)                      │
│                                                              │
│  ┌─────────────────┐  ┌──────────────────┐  ┌──────────────┐ │
│  │ NearestStations │  │ InteractiveMap   │  │ LinesCatalog │ │
│  │ Screen          │  │ Canvas View      │  │ & RouteCalc  │ │
│  └────────┬────────┘  └────────┬─────────┘  └──────┬───────┘ │
│           └────────────────────┼───────────────────┘         │
│                                ▼                             │
│                  TransitViewModel (StateFlow)                │
└────────────────────────────────┬─────────────────────────────┘
                                 │
                                 ▼
┌──────────────────────────────────────────────────────────────┐
│                      Data & Domain Layer                     │
│                                                              │
│  ┌──────────────────────────┐   ┌──────────────────────────┐ │
│  │ LocationService / Manager│   │ StationRepository        │ │
│  │ (Fused / Android GPS)    │   │ (São Paulo Network Data) │ │
│  └──────────────────────────┘   └──────────────────────────┘ │
│                                                              │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │ RouteCalculator (BFS / Dijkstra para conexões de linha) │ │
│  └─────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────┘
```

#### Entidades de Dados
- `Station`: ID, nome, latitude, longitude, linhas (`List<LineType>`), transferências disponíveis (`List<StationTransfer>`), acessibilidade, bicicletário.
- `MetroLine`: ID, número/código, nome, cor hexadecimal, operadora (Metrô, CPTM, ViaMobilidade, ViaQuatro), lista ordenada de estações.
- `RouteSuggestion`: Estação de embarque, conexões/trocas de linha intermediárias, estação de desembarque, número de paradas e tempo estimado.
