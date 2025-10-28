# Testing & Troubleshooting Guide v1.0.8

## 🔍 Step 1: Verifica Caricamento Datapack

### Comando Nuovo: `/fatigue config`

Questo comando mostra TUTTI i valori attualmente caricati dal sistema.

**Usa questo comando in-game:**
```
/fatigue config
```

**Output atteso con le tue modifiche:**
```
=== Fatigue Configuration ===
Max Fatigue: 40 minutes (48000 ticks)
Bed Rest Time: 30 seconds (600 ticks)          ← Dovrebbe essere 30!
Bed Proximity Range: 20.0 blocks
Proximity Recovery Interval: 1 seconds          ← Dovrebbe essere 1!
Proximity Recovery Amount: 60 seconds of fatigue
Movement Check Interval: 5 ticks
Bed Check Interval: 20 ticks
Day Cycle Length: 518400 ticks                  ← Dovrebbe essere 518400!
Day End Time: 269004 ticks                      ← Dovrebbe essere 269004!
```

### ⚠️ Se i valori sono DIVERSI:

1. **Il datapack non è stato caricato**
   - Controlla che sia nella cartella `world/datapacks/`
   - Deve avere il file `pack.mcmeta` valido
   - Usa `/reload` per ricaricare

2. **Controllo file path:**
   ```
   world/
     datapacks/
       nome_datapack/
         pack.mcmeta
         data/
           tharidiathings/
             fatigue_config/
               config.json    ← Il tuo file modificato
   ```

3. **Controlla i log del server:**
   ```
   grep "fatigue config" logs/latest.log
   ```
   
   Dovresti vedere:
   ```
   [INFO] Loading fatigue config from: tharidiathings:fatigue_config/config
   [INFO] Set bed rest time to 30 seconds (600 ticks)
   [INFO] Set proximity recovery interval to 1 seconds
   [INFO] Set day cycle length to 518400 ticks
   [INFO] Set day end time to 269004 ticks
   ```

---

## 🛏️ Step 2: Test Sonno e Riposo

### Test Completo del Sistema

```bash
# 1. Imposta poca energia
/fatigue set @s 1

# 2. Vai di giorno
/time set day

# 3. Prova a dormire
# Clicca sul letto
```

### Log Attesi (server log):

```
[INFO] Player YourName trying to sleep during day (time: XXXXX), fatigue: XXX/48000
[INFO] Marked YourName as resting BEFORE sleep
[INFO] Successfully forced player YourName to sleep during day
[INFO] YourName resting: 1/30 seconds
[INFO] YourName resting: 2/30 seconds
...
[INFO] YourName resting: 30/30 seconds
[INFO] YourName has rested enough! Fully restoring energy
```

### Se il Player si Sveglia Immediatamente:

**Nuova Protezione Implementata:**
- Quando il player dorme, la posizione del letto viene salvata
- Se il player si sveglia troppo presto, viene FORZATO a tornare nel letto
- Log da cercare:
  ```
  [WARN] YourName tried to leave bed early! Forcing back to sleep at BlockPos{x=X, y=Y, z=Z}
  ```

---

## ⏰ Step 3: Test Time Skip

### Test Multipla

yer (se possibile):

```bash
# Player 1:
/fatigue set @s 10
# Vai di notte e dormi

# Il tempo NON dovrebbe passare finché Player 1 non è riposato
```

### Log Attesi:

```
[INFO] SleepFinished event triggered, checking resting players...
[INFO] Player YourName is still resting - preventing time skip
[INFO] Blocking time skip because 1 player(s) are resting
```

### Se il Time Skip NON è Bloccato:

1. **Verifica che il player sia marcato come resting:**
   ```
   grep "Marked .* as resting" logs/latest.log
   ```
   
   Dovresti vedere uno di questi:
   - "Marked YourName as resting BEFORE sleep"
   - "Marked YourName as resting (night sleep)"
   - "Marked YourName as resting (night click)"

2. **Verifica SleepFinished event:**
   ```
   grep "SleepFinished" logs/latest.log
   ```

---

## 🔄 Step 4: Test Riposo Passivo (Vicino al Letto)

Con i tuoi settaggi:
- **Interval:** 1 secondo
- **Amount:** 60 secondi di fatigue

Significa: **Ogni 1 secondo vicino al letto, recuperi 1 minuto di energia**

### Test:

```bash
# 1. Imposta poca energia
/fatigue set @s 10

# 2. Stai fermo vicino a un letto (< 20 blocchi)

# 3. Aspetta 10 secondi

# 4. Controlla
/fatigue check @s

# Dovresti avere 10 + 10 minuti = 20 minuti
```

---

## 🐛 Troubleshooting Problemi Comuni

### Problema: "Datapack caricato ma valori default"

**Soluzione:**
1. Il file JSON potrebbe avere errori di sintassi
2. Usa un JSON validator online
3. Controlla virgole, parentesi graffe

**Il tuo config.json attuale ha commenti (`_comment`)** che sono OK in JSON!

### Problema: "Player esce dal letto comunque"

**Cause possibili:**
1. **Server lag estremo** - Il sistema forza il player ogni 5 tick. Con lag pesante potrebbe non bastare.
   
   **Soluzione:** Riduci `player_batch_size` nel config:
   ```json
   "player_batch_size": 1
   ```
   Questo forza il check ogni tick (più carico sul server ma più responsivo)

2. **Mod conflit to** - Altre mod potrebbero interferire con il sonno.
   
   **Verifica:** Testa in un mondo con solo questa mod.

### Problema: "Time skip passa sempre"

**Debug:**
```bash
# Cerca questi log:
grep "playersResting" logs/latest.log
grep "SleepFinished" logs/latest.log
```

Se vedi:
```
[INFO] SleepFinished event triggered, checking resting players...
[INFO] No players resting - allowing time skip normally
```

Significa che il player NON è stato marcato come resting quando è entrato nel letto.

**Soluzione:** Verifica che l'evento `onBedInteract` sia triggerato:
```bash
grep "trying to sleep" logs/latest.log
grep "Marked .* as resting" logs/latest.log
```

---

## 📊 Comandi Utili per Debug

```bash
# Verifica config caricata
/fatigue config

# Controlla energia player
/fatigue check @s

# Imposta energia specifica (minuti)
/fatigue set @s 10

# Ripristina completamente
/fatigue reset @s

# Controlla tutti i player
/fatigue checkall
```

---

## 🎯 Checklist Finale

Prima di dichiarare che "non funziona":

- [ ] Ho usato `/reload` dopo aver modificato il datapack?
- [ ] Ho controllato `/fatigue config` e i valori sono corretti?
- [ ] Ho aspettato il tempo completo configurato (30 secondi nel tuo caso)?
- [ ] Ho controllato i log del server per errori?
- [ ] Il datapack è nella cartella giusta con `pack.mcmeta`?
- [ ] Ho testato con un solo player prima?
- [ ] Il server ha lag? (controlla TPS con `/forge tps`)

---

## 📝 Log File Locations

```
server_folder/
  logs/
    latest.log          ← Log corrente
    debug.log           ← Log dettagliati (se abilitati)
```

Per vedere solo log fatigue:
```bash
tail -f logs/latest.log | grep -i fatigue
```

---

## ⚙️ Modifiche Recenti v1.0.8

1. **Sistema di Force-Back al Letto:**
   - Salva posizione letto quando dormi
   - Forza player a tornare se si sveglia troppo presto
   - Log warning quando succede

2. **Comando `/fatigue config`:**
   - Mostra TUTTI i valori caricati
   - Utile per verificare datapack

3. **Logging Completo:**
   - Ogni azione è loggata
   - Facile diagnosticare problemi

4. **Sleep PRIMA del Batching:**
   - Player che dormono sono processati ogni tick
   - Non più delay di 5 tick

---

## 🆘 Supporto

Se dopo aver seguito questa guida il problema persiste:

1. **Raccogli queste informazioni:**
   - Output di `/fatigue config`
   - Screenshot del problema
   - Ultimi 100 righe di `logs/latest.log`
   - Lista mod installate

2. **Cerca questi pattern nei log:**
   - "ERROR" o "WARN" con "fatigue" o "sleep"
   - "Successfully forced" seguito da sveglio immediato
   - Mancanza completa di log "resting: X/Y seconds"

---

## 📦 File Config Tuo (Riepilogo)

```json
{
  "bed_rest_time_seconds": 30,                  ← 30 sec per riposo completo
  "proximity_recovery_interval_seconds": 1,     ← Check ogni 1 sec
  "proximity_recovery_amount_seconds": 60,      ← 1 min recuperato
  "day_cycle_length": 518400,                   ← Giorno lunghissimo!
  "day_end_time": 269004                        ← ~52% del ciclo
}
```

**Questo significa:**
- 30 secondi nel letto = energia 100%
- 1 secondo vicino al letto = +1 minuto energia  
- Recupero MOLTO veloce!
