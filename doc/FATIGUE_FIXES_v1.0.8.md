# Fatigue System - Bug Fixes v1.0.8

## Problemi Risolti

### ✅ 1. Riposo in Prossimità del Letto Funziona Anche da Fermo

**Problema:** Il recupero funzionava solo quando il giocatore si muoveva, era inconsistente da fermo.

**Causa:** 
- Sistema di cache che poteva bloccare il rilevamento
- Check condizionali che causavano problemi

**Soluzione:**
- Rimossa cache per il controllo bed proximity (il check avviene ogni 20 tick, non è costoso)
- Ora controlla SEMPRE se sei vicino al letto ogni secondo
- Sincronizzazione immediata con il client dopo ogni recupero
- Aggiunto logging per debug

**Risultato:** Riposo passivo funziona perfettamente anche stando fermi vicino al letto!

---

### ✅ 2. Dormire nel Letto Ora Ripristina l'Energia

**Problema:** Dormire nel letto non faceva recuperare energia.

**Causa:** 
- Il metodo `handleBedProximity()` ritornava immediatamente se il giocatore stava dormendo
- Questo impediva l'incremento dei tick di riposo in `handleBedRest()`

**Soluzione:**
- Separato correttamente bed proximity recovery da bed rest
- `handleBedRest()` viene chiamato OGNI TICK quando dormi
- Incrementa `bedRestTicks` ad ogni tick
- Quando raggiungi il tempo configurato (default: 60 secondi = 1200 tick), energia completamente ripristinata
- Messaggio di conferma quando sei completamente riposato

**Risultato:** Dormire nel letto ripristina completamente l'energia dopo il tempo configurato!

---

### ✅ 3. Sistema di Sonno Diurno Migliorato

**Problema:** Il bypass per dormire di giorno non funzionava correttamente.

**Causa:**
- Approccio troppo semplice che non gestiva correttamente i controlli di Minecraft

**Soluzione:**
- Usa `player.startSleepInBed()` per controllare tutti i problemi possibili
- Verifica se l'UNICO problema è `NOT_POSSIBLE_NOW` (è giorno)
- Solo in quel caso, cancella l'evento e forza `player.startSleeping()`
- Aggiunto logging dettagliato per debug
- Gestione corretta della parte del letto (HEAD vs FOOT)

**Risultato:** 
- Se è giorno E sei stanco → puoi dormire
- Se è notte → funziona normalmente
- Se ci sono altri problemi (letto occupato, mob vicini, etc.) → messaggio vanilla appropriato

---

## Meccaniche di Recupero

### Riposo Attivo (Dormire nel Letto)
- Entri nel letto (giorno o notte se stanco)
- Ogni tick incrementa il contatore di riposo
- Dopo 60 secondi (1200 tick, configurabile): **RIPRISTINO COMPLETO**
- Messaggio: "Completamente riposato! Energie ripristinate."
- Non puoi uscire dal letto prima del ripristino completo

### Riposo Passivo (Vicino al Letto)
- Stai entro 20 blocchi da un letto (configurabile)
- Funziona anche DA FERMO
- Ogni 10 secondi recuperi 1 minuto di energia (configurabile)
- Sincronizzazione immediata con client
- Puoi fare altro mentre recuperi

---

## Configurazioni Importanti

### Per Server con Giorni da 48000 Tick

Modifica `config.json`:
```json
{
  "day_cycle_length": 48000,
  "day_end_time": 25082
}
```

### Tempi di Recupero

```json
{
  "bed_rest_time_seconds": 60,           // Tempo per ripristino completo nel letto
  "proximity_recovery_interval_seconds": 10,  // Frequenza recupero passivo
  "proximity_recovery_amount_seconds": 60,    // Quantità recuperata
  "bed_proximity_range": 20.0                 // Distanza dal letto
}
```

---

## Testing Consigliato

1. **Test Riposo Passivo:**
   - `/fatigue set @s 10` (imposta bassa energia)
   - Vai vicino a un letto e rimani fermo
   - Ogni 10 secondi dovresti recuperare 1 minuto
   - Controlla con `/fatigue check @s`

2. **Test Riposo Attivo:**
   - `/fatigue set @s 10`
   - Dormi nel letto
   - Dopo 60 secondi dovresti essere completamente riposato
   - Prova a uscire prima: messaggio "Devi riposare per recuperare le energie!"

3. **Test Sonno Diurno:**
   - `/time set day`
   - `/fatigue set @s 10`
   - Clicca sul letto
   - Dovresti entrare nel letto con messaggio "Riposo per recuperare energie..."

---

## Log Debug

Ora il sistema logga informazioni utili:
- Quando un giocatore prova a dormire di giorno
- I valori di fatigue e tempo
- Se il sonno è stato forzato con successo
- Caricamento configurazioni

Controlla i log del server per diagnosticare eventuali problemi.

---

## Changelog v1.0.8

- ✅ Fix: Riposo passivo funziona anche da fermo
- ✅ Fix: Dormire nel letto ripristina l'energia correttamente
- ✅ Fix: Sonno diurno quando stanco funziona correttamente
- ✅ Aggiunto: Logging dettagliato per debug
- ✅ Aggiunto: Sincronizzazione immediata dopo recupero passivo
- ✅ Aggiunto: Messaggio conferma "Completamente riposato!"
- ✅ Migliorato: Gestione eventi più robusta
- ✅ Rimosso: Sistema cache che causava inconsistenze
