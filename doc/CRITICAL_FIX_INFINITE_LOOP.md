# CRITICAL FIX - Infinite Loop Removed

## ❌ Problema Critico Identificato

Il sistema di force-back causava un **loop infinito** che impediva al giocatore di fare qualsiasi cosa:

```
Player tries to sleep → Minecraft wakes player → System forces back to sleep → 
Minecraft wakes player → System forces back to sleep → INFINITE LOOP
```

### Log del Loop:
```
[WARN] Frenk012 tried to leave bed early! Forcing back to sleep
[INFO] Frenk012 woke up too early! Rested 101/600 ticks
[WARN] Frenk012 tried to leave bed early! Forcing back to sleep
[INFO] Frenk012 woke up too early! Rested 101/600 ticks
... (ripete all'infinito)
```

---

## ✅ Soluzione Implementata

### Nuovo Approccio: NO Sonno Diurno Forzato

**Prima (NON funzionava):**
- Player stanco clicca sul letto di giorno
- Mod forza `player.startSleeping(bedPos)`
- Minecraft sveglia immediatamente il player (è giorno!)
- Mod forza di nuovo → Loop infinito

**Adesso (FUNZIONA):**
- Player stanco clicca sul letto di giorno
- Mod **cancella** l'azione e mostra messaggio
- Player usa il **sistema di recupero passivo** (molto veloce con il tuo config)

### Messaggi Utente

**Italiano:**
```
"Troppo stanco per dormire ora. Riposa vicino a un letto per recuperare energie."
```

**English:**
```
"Too tired to sleep now. Rest near a bed to recover energy."
```

---

## 🎯 Come Funziona Ora

### Recupero Diurno: Proximity Recovery (Super Veloce)

Con il tuo config:
```json
{
  "proximity_recovery_interval_seconds": 1,     // Check ogni 1 secondo
  "proximity_recovery_amount_seconds": 60       // Recupera 1 minuto di energia
}
```

**Risultato:** Stando vicino al letto (< 20 blocchi):
- **Ogni 1 secondo → +1 minuto di energia**
- **30 secondi vicino al letto → +30 minuti di energia**
- **Praticamente istantaneo con il tuo config!**

### Recupero Notturno: Normal Sleep

- Player dorme di notte normalmente
- Recupero completo in 30 secondi (tuo config: `bed_rest_time_seconds: 30`)
- Time skip BLOCCATO fino al recupero completo
- Player non può uscire dal letto troppo presto (messaggio di avviso)

---

## 🔧 Modifiche Tecniche

### 1. Rimosso Force-Back Loop
```java
// PRIMA (causava loop):
if (player woke early) {
    player.startSleeping(bedPos); // ← Triggers wake up again!
}

// ADESSO (solo avviso):
if (player woke early) {
    serverPlayer.displayClientMessage("must rest");
    // Player can move but gets message
}
```

### 2. Sonno Diurno Bloccato
```java
if (isDaytime && player.isTired()) {
    event.setCanceled(true); // Block bed click
    showMessage("use proximity recovery");
}
```

### 3. Marking Resting Per Notte
```java
// When player sleeps at night
playersResting.put(player.getUUID(), true);

// This blocks time skip in SleepFinishedTimeEvent
if (playersResting.contains(player)) {
    event.setTimeAddition(0); // No time skip!
}
```

---

## 📊 Confronto Velocità Recupero

### Con il Tuo Config Attuale:

| Metodo | Tempo per Recupero Completo (40 min) |
|--------|--------------------------------------|
| **Proximity (giorno)** | ~40 secondi | ← SUPER VELOCE!
| **Sleep (notte)** | 30 secondi | ← Configurato

**Proximity è quasi più veloce del sonno!**

Se vuoi bilanciare:
```json
{
  "proximity_recovery_interval_seconds": 10,    // Check ogni 10 sec
  "proximity_recovery_amount_seconds": 60,      // Recupera 1 min
  "bed_rest_time_seconds": 30                   // Sleep 30 sec
}
```

Questo renderebbe:
- **Proximity:** ~6-7 minuti per recupero completo
- **Sleep:** 30 secondi

Sleep diventa molto più conveniente!

---

## 🧪 Testing

### Test 1: Proximity Recovery (Giorno)
```bash
/fatigue set @s 10
# Vai vicino a un letto di giorno
# Aspetta 30 secondi
/fatigue check @s
# Dovresti avere ~40 minuti (recupero completo!)
```

### Test 2: Sleep Recovery (Notte)
```bash
/fatigue set @s 10
/time set night
# Dormi nel letto
# Aspetta 30 secondi
# Dovresti essere completamente riposato
```

### Test 3: Time Skip Blocked
```bash
# Con 2 player:
# Player 1: /fatigue set @s 10 e dormi
# Player 2: dormi normalmente
# Il tempo NON dovrebbe passare finché Player 1 non è riposato
```

---

## ⚠️ Note Importanti

### Il "Problema" del Time Skip

Il time skip potrebbe ancora non essere bloccato se:

1. **Player non marcato come "resting"**
   - Verifica log: `"Player X starting night sleep, marked as resting"`
   
2. **Evento SleepFinished non triggerato**
   - Verifica log: `"SleepFinished event triggered"`
   
3. **Config datapack non caricato**
   - Usa `/fatigue config` per verificare

### Debug Commands

```bash
# Verifica config
/fatigue config

# Test veloce
/fatigue set @s 1
/time set night

# Monitora log
tail -f logs/latest.log | grep -i fatigue
```

---

## 🎮 Esperienza Utente

### Scenario Diurno:
1. Player è stanco di giorno
2. Clicca sul letto
3. Messaggio: "Troppo stanco per dormire ora..."
4. Sta vicino al letto per ~40 secondi (con tuo config)
5. Completamente riposato!

### Scenario Notturno:
1. Player è stanco di notte
2. Dorme normalmente nel letto
3. Dopo 30 secondi: "Completamente riposato!"
4. Tempo rimasto fermo (se configurato)
5. Può uscire dal letto

---

## 📦 Build Info

**Version:** 1.0.8  
**Build:** tharidiathings-1.0.8.jar (638K)  
**Date:** 2025-10-19 19:17  

**Changes:**
- ❌ Removed infinite loop force-back system
- ✅ Blocked daytime sleeping (use proximity instead)
- ✅ Improved logging (less spam)
- ✅ Added new message for daytime bed clicks
- ✅ Fixed time skip blocking logic

---

## 🔍 Troubleshooting

### "Time skip still happens"

Check logs for:
```
"Player X starting night sleep, marked as resting"
"SleepFinished event triggered"
"Blocking time skip because X player(s) are resting"
```

If missing → datapack not loaded or event not fired.

### "Proximity recovery too slow/fast"

Adjust config:
```json
{
  "proximity_recovery_interval_seconds": 10,  // How often
  "proximity_recovery_amount_seconds": 30     // How much
}
```

Formula: `Total Time = (Max Fatigue / Amount) × Interval`

Example: `(40 min / 30 sec) × 10 sec = ~13 minutes`

---

## ✅ Sistema Stabile

Il loop infinito è stato eliminato completamente. Il sistema ora è:
- **Stabile** - Non può più bloccare il player
- **Funzionale** - Recupero funziona sia di giorno che di notte
- **Configurabile** - Tutti i valori nel datapack
- **Loggato** - Facile diagnosi problemi

Test tutto e fammi sapere! 🎉
