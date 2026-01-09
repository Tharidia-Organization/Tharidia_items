# Fatigue System Configuration

## Adattamento a Cicli Giorno/Notte Personalizzati

La mod si adatta automaticamente a server con cicli giorno/notte modificati tramite configurazione datapack.

### Per Server con Giorni Vanilla (24000 tick)
Usa il file `config.json` così com'è:
```json
"day_cycle_length": 24000,
"day_end_time": 12541
```

### Per Server con Giorni Personalizzati (es. 48000 tick)

1. **Copia** il file `config_48000_example.json` e rinominalo in `config.json`

   **OPPURE**

2. **Modifica** i seguenti valori nel `config.json`:

```json
"day_cycle_length": 48000,
"day_end_time": 25082
```

### Come Calcolare i Valori

#### day_cycle_length
Imposta questo valore uguale alla durata completa del ciclo giorno/notte del tuo server in tick.

**Esempi:**
- Vanilla: `24000` tick
- Giorno doppio: `48000` tick
- Giorno triplo: `72000` tick

#### day_end_time
Questo è il momento in cui il giorno finisce e inizia la notte.

**Formula:** `(12541 / 24000) × YOUR_CYCLE_LENGTH`

**Esempi:**
- Vanilla (24000 tick): `12541`
- Doppio (48000 tick): `25082` (12541 × 2)
- Triplo (72000 tick): `37623` (12541 × 3)

### Come Applicare la Configurazione

1. Copia la cartella `example_datapack` nella cartella `datapacks` del tuo mondo
2. Rinomina se necessario (es. da `example_datapack` a `tharidia_fatigue_config`)
3. Modifica il file `config.json` con i valori appropriati
4. Riavvia il server o usa `/reload` in-game

### Note Importanti

⚠️ **La configurazione è essenziale per server con giorni modificati**
- Se i valori non sono corretti, il sistema di sonno diurno potrebbe non funzionare correttamente
- Il giocatore potrebbe non riuscire a dormire di giorno anche se è stanco
- Oppure potrebbe dormire in momenti non appropriati

✅ **Dopo la configurazione:**
- I giocatori stanchi potranno dormire anche di giorno
- Il tempo rimarrà fermo mentre riposano
- Dovranno riposare per il tempo configurato prima di poter uscire dal letto

### Test della Configurazione

Per testare se la configurazione funziona:

1. Usa `/fatigue set @s 5` per impostare poca energia
2. Prova a dormire durante il giorno
3. Dovresti vedere il messaggio "Riposo per recuperare energie..."
4. Il tempo dovrebbe rimanere fermo mentre dormi

### Configurazione Completa

Vedi `config.json` per tutte le opzioni disponibili:
- Durata massima stanchezza
- Tempo di riposo richiesto
- Distanza recupero vicino al letto
- Effetti di esaurimento
- E molto altro!
