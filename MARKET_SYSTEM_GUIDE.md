# Sistema di Mercato Virtuale - Guida Completa

## Panoramica
Sistema di mercato virtuale medievale implementato tra due mod:
- **Tharidia Items** (client+server): Gestisce le transazioni tra giocatori
- **Tharidia Features** (server-only): Registra e analizza i dati di mercato

## Caratteristiche Principali

### 1. Scambio tra Giocatori (Tharidia Items)
- **Iniziare uno scambio**: Tenere in mano un oggetto valuta (default: patata) e fare click destro su un altro giocatore
- **GUI di richiesta**: Il giocatore target riceve una schermata per accettare o rifiutare
- **GUI di scambio**: Interfaccia divisa verticalmente con 24 slot per giocatore
- **Conferma sicura**: Entrambi i giocatori devono confermare prima che lo scambio sia completato
- **Tema medievale**: Tutte le interfacce utilizzano terminologia e design medievale

### 2. Comandi Admin (Tharidia Features)
Tutti i comandi richiedono permessi admin (livello 2) e sono accessibili solo da console.

#### `/mercato registra <venditore> <compratore> <merce> <quantità>`
Crea una transazione fittizia per testare il sistema.
```
/mercato registra Giovanni Marco minecraft:diamond 10
```

#### `/mercato resoconto [ore]`
Mostra un resoconto dettagliato del mercato per il periodo specificato (default: 24 ore).
```
/mercato resoconto
/mercato resoconto 48
```

#### `/mercato mercati`
Lista tutti i mercati attivi con statistiche.

#### `/mercato dettagli <merce>`
Mostra dettagli specifici per un mercato di un item.
```
/mercato dettagli minecraft:diamond
```

#### `/mercato statistiche [ore]`
Mostra statistiche generali del mercato.

#### `/mercato attivi [ore]`
Lista i mercati più attivi nel periodo specificato.

#### `/mercato volume`
Mostra i mercati ordinati per volume totale scambiato.

## Configurazione

### Config Tharidia Items
File: `config/tharidiathings-common.toml`

```toml
# Lista di oggetti che possono essere usati come valuta per iniziare scambi
tradeCurrencyItems = ["minecraft:potato"]
```

Puoi aggiungere più oggetti:
```toml
tradeCurrencyItems = ["minecraft:potato", "minecraft:gold_nugget", "minecraft:emerald"]
```

## Architettura Tecnica

### Tharidia Items - Componenti

#### Network Packets
- `TradeRequestPacket`: Richiesta di scambio
- `TradeResponsePacket`: Risposta (accetta/rifiuta)
- `TradeUpdatePacket`: Aggiornamento offerta
- `TradeCompletePacket`: Completamento scambio
- `TradeCancelPacket`: Annullamento scambio

#### GUI Components
- `TradeRequestScreen`: Schermata di richiesta scambio
- `TradeScreen`: Interfaccia principale di scambio
- `TradeMenu`: Container menu per lo scambio

#### Core Systems
- `TradeManager`: Gestisce tutte le sessioni di scambio attive
- `TradeSession`: Rappresenta una sessione di scambio tra due giocatori
- `TradeInteractionHandler`: Gestisce le interazioni player-to-player
- `TradePacketHandler`: Processa i pacchetti di rete lato server
- `MarketBridge`: Comunica le transazioni a Tharidia Features

### Tharidia Features - Componenti

#### Market Core
- `MarketRegistry`: Registro centrale di tutti i mercati (SavedData)
- `MarketInstance`: Rappresenta il mercato per un singolo item
- `Transaction`: Rappresenta una singola transazione
- `TransactionProcessor`: Processa le transazioni dalla coda

#### Commands
- `MarketCommands`: Tutti i comandi admin per gestire il mercato

## Flusso di una Transazione

1. **Iniziazione**: Giocatore A con valuta in mano fa click destro su Giocatore B
2. **Richiesta**: Giocatore B riceve schermata di richiesta
3. **Accettazione**: Se accettato, entrambi vedono la GUI di scambio
4. **Negoziazione**: Entrambi inseriscono oggetti nei propri 24 slot
5. **Conferma**: Entrambi premono "Conferma" (possono annullare)
6. **Esecuzione**: 
   - Verifica che gli oggetti siano ancora disponibili
   - Rimuove oggetti da entrambi i giocatori
   - Scambia gli oggetti
   - Invia dati a Tharidia Features
7. **Registrazione**: Tharidia Features registra la transazione e aggiorna i mercati

## Comunicazione Cross-Mod

Il sistema utilizza un file NBT condiviso per comunicare tra le mod:
- **File**: `world/tharidia_market_transactions.dat`
- **Tharidia Items**: Scrive le transazioni in coda
- **Tharidia Features**: Legge e processa la coda ogni 5 secondi

Questo approccio garantisce:
- Nessuna dipendenza diretta tra le mod
- Persistenza dei dati anche in caso di crash
- Elaborazione asincrona

## Sistema di Quotazione

Ogni `MarketInstance` calcola automaticamente la quotazione basandosi su:
- Volume degli scambi
- Frequenza delle transazioni
- Valore relativo degli oggetti scambiati

La quotazione viene aggiornata ad ogni transazione usando una media ponderata.

## Sicurezza

- **Protezione anti-furto**: Gli oggetti dell'altro giocatore non possono essere presi fino alla conferma
- **Verifica inventario**: Il sistema verifica che gli oggetti siano disponibili prima di completare
- **Timeout sessioni**: Le sessioni scadono dopo 5 minuti di inattività
- **Solo admin**: Tutti i comandi di gestione richiedono permessi admin

## Note di Sviluppo

### Estensioni Future Possibili
1. Sistema di reputazione mercanti
2. Tasse sulle transazioni
3. Mercati regionali (per dimensione)
4. Grafici di andamento prezzi
5. Sistema di aste
6. Contratti di commercio

### Manutenzione
- I dati di mercato sono salvati in `world/data/tharidia_market_registry.dat`
- Backup regolari consigliati per preservare lo storico
- Il file di coda viene automaticamente eliminato dopo l'elaborazione

## Troubleshooting

### Lo scambio non si avvia
- Verificare di avere un oggetto valuta in mano
- Controllare che nessuno dei due giocatori sia già in uno scambio
- Verificare la configurazione `tradeCurrencyItems`

### I comandi non funzionano
- Verificare di avere permessi admin (livello 2)
- Controllare che Tharidia Features sia installato sul server
- Verificare i log per errori

### Le transazioni non vengono registrate
- Verificare che entrambe le mod siano installate
- Controllare i permessi di scrittura nella cartella world
- Verificare i log di entrambe le mod

## Crediti
Sistema sviluppato per un contesto medievale roleplay.
Tutte le interfacce e messaggi sono contestualizzati al periodo storico.
