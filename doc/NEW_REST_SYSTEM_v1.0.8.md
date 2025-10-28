# New Rest System v1.0.8 - Stay Near Bed

## 🛏️ Il Problema del Wake Up Automatico

Minecraft forza automaticamente il wake up quando:
1. **Tutti i player** nel mondo dormono
2. L'evento `SleepFinishedTimeEvent` viene triggerato
3. Tutti i player vengono svegliati

**Non possiamo impedire questo comportamento!** È hardcoded in Minecraft.

---

## ✅ La Soluzione: "Rest Near Bed"

Invece di impedire il wake up (impossibile), il sistema ora **continua il riposo** anche quando il player è sveglio.

### Come Funziona

1. **Inizi a Dormire**
   - Entri nel letto normalmente
   - Sistema inizia a tracciare il riposo
   - Posizione del letto salvata

2. **Vieni Svegliato** (da `SleepFinishedTimeEvent`)
   - Messaggio: *"Rimani vicino al letto per riposare (X secondi rimanenti)"*
   - Il conteggio del riposo **continua** automaticamente
   - **Devi rimanere entro 3 blocchi dal letto**

3. **Due Possibilità:**

   **A) Rimani Vicino al Letto (< 3 blocchi):**
   - ✅ Riposo continua (1 tick ogni tick, come se stessi dormendo)
   - ✅ Ogni 5 secondi: reminder con secondi rimanenti
   - ✅ Quando finito: *"Completamente riposato! Energie ripristinate."*
   - ✅ Time skip rimane bloccato finché non sei riposato

   **B) Ti Allontani (> 3 blocchi):**
   - ❌ Riposo interrotto!
   - ❌ Messaggio: *"Riposo interrotto! Ti sei allontanato troppo dal letto."*
   - ❌ Progresso azzerato
   - ❌ Devi ricominciare da capo

---

## 🎯 Vantaggi del Nuovo Sistema

### 1. Nessun Loop Infinito
- ✅ Non forza più il player a dormire
- ✅ Player può muoversi (ma deve rimanere vicino)
- ✅ Sistema stabile

### 2. Continua Anche Sveglio
- ✅ Se Minecraft ti sveglia, il riposo continua
- ✅ Stesso rate di recupero (1 tick/tick)
- ✅ Funziona anche in multiplayer

### 3. Time Skip Ancora Bloccato
- ✅ Finché sei "resting", il tempo non passa
- ✅ Gli altri player devono aspettare
- ✅ Sistema giusto ed equo

### 4. Flessibilità
- ✅ Puoi fare piccoli movimenti
- ✅ Puoi guardare intorno
- ✅ Non sei completamente bloccato

---

## 📊 Esempio di Utilizzo

### Scenario Tipico (30 secondi per riposo completo):

```
[0s]  Player clicca sul letto di notte
      → Entra nel letto
      → "Resting: 0/30 seconds"

[5s]  SleepFinishedTimeEvent triggera
      → Player viene svegliato da Minecraft
      → Messaggio: "Rimani vicino al letto per riposare (25 secondi rimanenti)"
      → Player rimane vicino al letto

[10s] Messaggio: "Rimani vicino al letto per riposare (20 secondi rimanenti)"

[15s] Messaggio: "Rimani vicino al letto per riposare (15 secondi rimanenti)"

[20s] Messaggio: "Rimani vicino al letto per riposare (10 secondi rimanenti)"

[25s] Messaggio: "Rimani vicino al letto per riposare (5 secondi rimanenti)"

[30s] ✅ "Completamente riposato! Energie ripristinate."
      → Energia 100%
      → Può allontanarsi
```

### Scenario Interrotto:

```
[0s]  Player clicca sul letto
      → Entra nel letto

[5s]  Player viene svegliato (SleepFinishedTimeEvent)
      → Messaggio: "Rimani vicino al letto (25 secondi rimanenti)"

[10s] Player cammina via dal letto (>3 blocchi)
      → ❌ "Riposo interrotto! Ti sei allontanato troppo dal letto."
      → Progresso azzerato
      → Deve ricominciare
```

---

## 🔧 Dettagli Tecnici

### Distanza Massima: 3 Blocchi

Il player deve rimanere entro **3 blocchi** dalla posizione del letto.

```
         N
         |
    XXXXX|XXXXX
    XXXXX|XXXXX
W---XXXXX🛏️XXXXX---E
    XXXXX|XXXXX
    XXXXX|XXXXX
         |
         S
         
X = Area valida (entro 3 blocchi)
🛏️ = Letto
```

### Processing Ogni Tick

I player che stanno riposando (ma non dormendo) vengono processati **ogni tick** invece che ogni 5 tick.

Questo garantisce:
- ✅ Check distanza accurato
- ✅ Conteggio preciso del riposo
- ✅ Nessun "salto" nei secondi rimanenti

### Time Skip Bloccato

Finché **almeno 1 player** è marcato come "resting":
```java
if (playersResting.contains(player)) {
    event.setTimeAddition(0); // No time skip
}
```

Anche se il player è sveglio, se è vicino al letto e sta riposando, il tempo non passa.

---

## 🧪 Testing

### Test 1: Riposo Normale
```bash
/tharidia fatigue set @s 10
/time set night
# Dormi nel letto
# Quando vieni svegliato, RIMANI VICINO al letto
# Aspetta i 30 secondi (o il tempo configurato)
# Dovresti diventare completamente riposato
```

### Test 2: Riposo Interrotto
```bash
/tharidia fatigue set @s 10
/time set night
# Dormi nel letto
# Quando vieni svegliato, CAMMINA VIA dal letto
# Dovresti vedere: "Riposo interrotto!"
# Il progresso viene azzerato
```

### Test 3: Time Skip Bloccato
```bash
# Con 2 player:
# Player 1: set fatigue low, sleep
# Player 2: sleep normally
# Il tempo NON dovrebbe passare
# Player 1 si sveglia ma rimane vicino al letto
# Il tempo rimane fermo fino a quando Player 1 è riposato
```

---

## 📝 Log da Cercare

### Riposo Continuato:
```
[INFO] Frenk012 resting: 5/30 seconds (25 remaining)
[INFO] SleepFinished event triggered, checking resting players...
[INFO] Blocking time skip because 1 player(s) are resting
[INFO] Frenk012 woke up early! Must stay near bed for 25 more seconds
[INFO] Frenk012 finished resting while near bed!
```

### Riposo Interrotto:
```
[WARN] Frenk012 moved too far from bed! Resetting rest progress
```

---

## 🎮 Esperienza Utente

### Messaggi In-Game

**Quando Vieni Svegliato:**
```
"Rimani vicino al letto per riposare (25 secondi rimanenti)"
```

**Ogni 5 Secondi (Reminder):**
```
"Rimani vicino al letto per riposare (20 secondi rimanenti)"
"Rimani vicino al letto per riposare (15 secondi rimanenti)"
...
```

**Quando Completato:**
```
"Completamente riposato! Energie ripristinate."
```

**Se Ti Allontani:**
```
"Riposo interrotto! Ti sei allontanato troppo dal letto."
```

---

## ⚙️ Configurazione

Il tempo di riposo è configurabile nel datapack:

```json
{
  "bed_rest_time_seconds": 30  // Tempo per recupero completo
}
```

La distanza massima è hardcoded a **3 blocchi** per bilanciamento.

---

## 🔄 Confronto con il Sistema Vecchio

| Aspetto | Vecchio | Nuovo |
|---------|---------|-------|
| **Wake Up Forzato** | Loop infinito 💀 | Player può svegliarsi ✅ |
| **Riposo Continua** | ❌ Reset | ✅ Continua se vicino |
| **Time Skip** | ✅ Bloccato | ✅ Bloccato |
| **Flessibilità** | ❌ Bloccato nel letto | ✅ Può muoversi vicino |
| **Stabilità** | ❌ Crash possibili | ✅ Stabile |

---

## 📦 Build Info

**Version:** 1.0.8  
**File:** tharidiathings-1.0.8.jar  
**Build Time:** 2025-10-19 20:51  

**Changelog:**
- ✅ Nuovo sistema "Rest Near Bed"
- ✅ Player può svegliarsi ma deve rimanere vicino
- ✅ Nessun loop infinito
- ✅ Time skip ancora bloccato
- ✅ Messaggi informativi
- ✅ Processing ogni tick per player resting

---

## 🚀 Pronto per il Test!

Il nuovo sistema è:
- **Stabile** - Nessun loop
- **Funzionale** - Riposo continua anche sveglio
- **Intuitivo** - Messaggi chiari
- **Configurabile** - Tempo nel datapack

**Sostituisci il JAR e testa!** 🎉

Il player può ora essere svegliato da Minecraft senza problemi, basta che rimanga vicino al letto! 🛏️
