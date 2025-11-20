# Sistema di Protezione Item di Valuta

## Panoramica
Sistema completo per proteggere gli item di valuta configurati, impedendo che vengano droppati o messi in container esterni all'inventario del giocatore.

## Funzionalità Implementate

### 1. **Blocco Drop Item**
- Gli item di valuta non possono essere droppati a terra premendo `Q`
- Se un giocatore tenta di dropparli, vengono automaticamente restituiti all'inventario
- Messaggio di errore: "§cNon puoi droppare gli item di valuta!"

### 2. **Blocco Inserimento in Container**
- Gli item di valuta non possono essere inseriti in:
  - Chest (casse)
  - Barrel (barili)
  - Shulker Box
  - Hopper
  - Furnace (forni)
  - Qualsiasi altro container del gioco
- **Eccezioni permesse:**
  - Inventario del giocatore stesso
  - Menu di trade (`TradeMenu`)

### 3. **Sistema di Recupero Automatico**
- Controllo ogni 10 tick (0.5 secondi) per ottimizzare le performance
- Se un item di valuta viene trovato in un container non permesso:
  - Viene rimosso automaticamente
  - Viene restituito all'inventario del giocatore
  - Messaggio: "§cGli item di valuta non possono essere messi in questo contenitore!"

### 4. **Recupero Item a Terra**
- Se un item di valuta finisce a terra (es. morte del giocatore, bug):
  - Il sistema cerca il giocatore più vicino entro 5 blocchi
  - L'item viene automaticamente aggiunto al suo inventario
  - Messaggio: "§eItem di valuta recuperato automaticamente."

## File Implementati

### **CurrencyProtectionHandler.java**
Event handler principale che gestisce:
- `onItemToss()` - Blocca il drop degli item
- `onPlayerTick()` - Controlla i container aperti
- `onItemEntityTick()` - Recupera item a terra
- `isCurrencyItem()` - Verifica se un item è valuta

## Configurazione

Nel file `tharidiathings-common.toml`:

```toml
# Lista degli item che fungono da valuta (protetti dal sistema)
tradeCurrencyItems = ["minecraft:potato", "minecraft:gold_nugget"]
```

## Comportamento Dettagliato

### Scenario 1: Tentativo di Drop
```
Giocatore preme Q su una patata (item di valuta)
→ Drop cancellato
→ Item restituito all'inventario
→ Messaggio di errore mostrato
```

### Scenario 2: Tentativo di Inserimento in Chest
```
Giocatore apre una chest
Giocatore trascina una patata nella chest
→ Dopo 0.5 secondi (10 tick):
  → Item rimosso dalla chest
  → Item restituito all'inventario del giocatore
  → Messaggio di errore mostrato
```

### Scenario 3: Item a Terra
```
Item di valuta cade a terra (es. morte giocatore)
→ Sistema cerca giocatore entro 5 blocchi
→ Se trovato:
  → Item aggiunto automaticamente all'inventario
  → Item entity rimosso dal mondo
  → Messaggio di conferma mostrato
```

## Ottimizzazioni Performance

1. **Controllo Container**: Ogni 10 tick invece di ogni tick (riduzione 90% carico)
2. **Controllo Item a Terra**: Solo per ItemEntity, non per tutte le entity
3. **Range Limitato**: Recupero automatico solo entro 5 blocchi
4. **Skip Intelligente**: Salta slot dell'inventario del giocatore nei controlli

## Integrazione con Sistema Trade

Il sistema di protezione è completamente integrato con il sistema di trade:
- Gli item di valuta **possono** essere messi nel `TradeMenu`
- Questo permette lo scambio tra giocatori
- La tassazione viene applicata correttamente
- Dopo il trade, gli item rimangono protetti nell'inventario

## Testing

Per testare il sistema:

1. **Test Drop:**
   ```
   /give @s minecraft:potato 64
   Premi Q → Item non viene droppato
   ```

2. **Test Container:**
   ```
   /give @s minecraft:potato 64
   Apri una chest
   Metti le patate nella chest
   Aspetta 1 secondo → Item tornano nell'inventario
   ```

3. **Test Trade:**
   ```
   Giocatore1 tiene una patata
   Click destro su Giocatore2 con la patata
   Trade si apre correttamente
   Le patate possono essere scambiate
   ```

## Note Tecniche

- **Thread-Safe**: Tutti i controlli sono eseguiti sul server thread
- **Client-Server Sync**: I messaggi sono inviati solo lato server
- **Compatibilità**: Funziona con tutti i container vanilla e mod
- **Estensibile**: Facile aggiungere nuovi item di valuta nel config
