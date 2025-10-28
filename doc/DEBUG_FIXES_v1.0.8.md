# Debug Fixes - Problemi Sonno e Time Skip

## Problemi Identificati

### 1. ❌ Player si sveglia immediatamente
**Problema:** Il player viene messo a dormire ma si sveglia subito dopo (1 tick)

**Causa:** 
- Il batching dei player faceva sì che `handleBedRest()` fosse chiamato solo ogni 5 tick invece che ogni tick
- Questo rallentava il conteggio del riposo di 5x

**Soluzione:**
```java
// PRIMA:
// Batching applicato a TUTTI i player
int playerBatchSize = FatigueConfig.getPlayerBatchSize();
if (player.tickCount % playerBatchSize != playerBatch) {
    return; // Saltava anche i player che dormivano!
}

// DOPO:
// Check sleeping PRIMA del batching
if (player.isSleeping()) {
    handleBedRest(player, data); // Chiamato OGNI TICK
    return;
}
// Batching solo per player NON sleeping
```

---

### 2. ❌ Time Skip non bloccato (giorno passa comunque)
**Problema:** Quando dormi la notte, diventa giorno anche se sei in fase di riposo

**Causa:**
- Il player non veniva marcato come "resting" al momento giusto
- `playersResting.put()` era chiamato DOPO `player.startSleeping()`, troppo tardi per l'evento `SleepFinishedTimeEvent`

**Soluzione:**
```java
// PRIMA:
player.startSleeping(bedPos);
playersResting.put(player.getUUID(), true); // Troppo tardi!

// DOPO:
playersResting.put(player.getUUID(), true); // PRIMA!
LOGGER.info("Marked {} as resting BEFORE sleep", ...);
player.startSleeping(bedPos);
```

---

### 3. ❌ Energia non si riempie completamente
**Problema:** Anche dopo 60 secondi nel letto, l'energia non è al massimo

**Causa:** 
- Il conteggio dei tick era rallentato dal batching (vedi punto 1)
- Il player si svegliava troppo presto (vedi punto 1)

**Soluzione:**
- Ora `handleBedRest()` viene chiamato OGNI TICK quando dormi
- Il conteggio è accurato: 60 secondi = 1200 tick esatti
- Log ogni secondo mostra il progresso

---

## Logging Dettagliato Aggiunto

### Durante il Tentativo di Dormire
```
[INFO] Player Frenk012 trying to sleep during day (time: 75494), fatigue: 47771/48000
[INFO] Marked Frenk012 as resting BEFORE sleep
[INFO] Successfully forced player Frenk012 to sleep during day
```

### Durante il Riposo
```
[INFO] Frenk012 resting: 1/60 seconds
[INFO] Frenk012 resting: 2/60 seconds
...
[INFO] Frenk012 resting: 60/60 seconds
[INFO] Frenk012 has rested enough! Fully restoring energy
```

### Al Wake Up
```
[INFO] Frenk012 fully rested and woke up naturally (rested for 1200/1200 ticks)
```

### Se Svegliato Troppo Presto
```
[INFO] Frenk012 woke up too early! Rested 600/1200 ticks, 600 remaining
```

### Time Skip Check
```
[INFO] SleepFinished event triggered, checking resting players...
[INFO] Player Frenk012 is still resting - preventing time skip
[INFO] Blocking time skip because 1 player(s) are resting
```

---

## Come Testare le Correzioni

### Test 1: Sonno Diurno Completo
```bash
/time set day
/fatigue set @s 10
# Dormi nel letto
# Aspettati:
# - Log "Marked as resting BEFORE sleep"
# - Log "resting: X/60 seconds" ogni secondo
# - Dopo 60 secondi: "has rested enough!"
# - Messaggio in gioco: "Completamente riposato!"
```

### Test 2: Time Skip Bloccato
```bash
/time set night
/fatigue set @s 10
# Dormi con almeno un altro player online
# Aspettati:
# - Log "SleepFinished event triggered"
# - Log "Blocking time skip because X player(s) are resting"
# - Il tempo NON passa (rimane notte)
```

### Test 3: Energia Completa
```bash
/fatigue set @s 10
# Dormi per 60 secondi
/fatigue check @s
# Aspettati: 40/40 minuti (100% energia)
```

---

## Checklist Correzioni

✅ **Player batching** - Sleeping players bypassano il batching
✅ **handleBedRest()** - Chiamato OGNI TICK, non ogni 5 tick  
✅ **playersResting** - Marcato PRIMA di startSleeping()  
✅ **Time skip** - Bloccato correttamente con logging  
✅ **Wake up** - Gestito con logging dettagliato  
✅ **Logging completo** - Ogni passaggio tracciato  

---

## Log da Controllare

Nel server log, cerca questi pattern:

1. **Entrando nel letto:**
   - "trying to sleep during day" (se giorno)
   - "Marked as resting BEFORE sleep"
   - "Successfully forced" / "night sleep"

2. **Durante il riposo:**
   - "resting: X/60 seconds" ogni secondo
   - I numeri devono incrementare ogni secondo

3. **Quando scatta SleepFinished:**
   - "SleepFinished event triggered"
   - "Player X is still resting"
   - "Blocking time skip"

4. **Al risveglio:**
   - "has rested enough! Fully restoring"
   - "fully rested and woke up naturally"

---

## Versione

Build: **tharidiathings-1.0.8.jar**  
Data: 2025-10-19  
Fix: Player batching bypass per sleeping players + Time skip prevention  

---

## Note Tecniche

### Player Batching
Il sistema di batching distribuisce il carico su più tick:
- **Non sleeping players**: Processati 1 ogni 5 tick (ottimizzazione)
- **Sleeping players**: Processati OGNI tick (necessario per accuracy)

### Time Skip Prevention
L'evento `SleepFinishedTimeEvent` viene triggerato quando tutti i player dormono:
- Check della mappa `playersResting`
- Se almeno 1 player sta riposando → `setTimeAddition(0)`
- Il tempo rimane fermo finché il riposo non è completato

### Rest Tracking
- `bedRestTicks` incrementato ogni tick quando `isSleeping() == true`
- `hasRestedEnough()` controlla se `bedRestTicks >= getBedRestTime()`
- Default: 1200 tick = 60 secondi = 1 minuto reale
