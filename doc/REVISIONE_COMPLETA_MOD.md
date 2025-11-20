# Revisione Completa Mod - Changelog

## 🔒 Sicurezza Lobby - NUOVE FUNZIONALITÀ

### 1. **Blocco Chat per Non-OP in Lobby**
- **File**: `LobbyChatBlocker.java` (NUOVO)
- **Funzione**: Blocca completamente la chat per i giocatori non-OP quando `isLobbyServer = true`
- **Comportamento**:
  - Giocatori non-OP: **NON possono scrivere in chat** nella lobby
  - Giocatori OP (livello 2+): Possono chattare normalmente
  - Messaggio di errore: "§c§l[LOBBY] §7Chat is disabled in the lobby. Please wait to be transferred to the main server."

### 2. **Protezione Modalità Spettatore in Lobby**
- **File**: `LobbyProtectionHandler.java` (NUOVO)
- **Funzione**: Impedisce ai giocatori non-OP di uscire dalla modalità spettatore o di volare
- **Comportamento**:
  - Controlla ogni secondo (20 tick) che i non-OP siano in modalità spettatore
  - Se un giocatore cambia modalità, viene **forzato** di nuovo in spettatore
  - Rimuove abilità di volo non autorizzate
  - Al respawn, i non-OP vengono **sempre** messi in spettatore
- **Risolve**: Il problema di giocatori che potevano volare o non prendere danno da caduta

### 3. **Blocco Comando /server**
- **File**: `ServerCommandBlocker.java` (NUOVO)
- **Funzione**: Blocca il comando `/server` (da Velocity o altri plugin) per tutti tranne OP livello 4
- **Comportamento**:
  - Blocca `/server main`, `/server lobby`, ecc.
  - Solo OP livello 4 può usarlo
  - Messaggio di errore: "§c§l[BLOCKED] §7The /server command is disabled. Use §6/thqueueadmin play§7 or §6/thqueueadmin send§7 instead."

---

## 🎮 Comandi - MODIFICHE IMPORTANTI

### **RIMOSSO: `/thqueue` per giocatori normali**
- Il comando `/thqueue` è stato **completamente rimosso**
- **Nessun giocatore non-OP** può più controllare la coda
- Solo gli admin possono usare `/thqueueadmin queue`

### **NUOVO: `/thqueueadmin sendtolobby <player>`**
- Permette agli admin di spostare giocatori dal main alla lobby
- Sintassi: `/thqueueadmin sendtolobby NomeGiocatore`
- Richiede OP livello 4

### **NUOVO: `/thqueueadmin queue`**
- Versione admin del vecchio `/thqueue`
- Mostra la posizione nella coda (solo per admin)
- Richiede OP livello 4

---

## 🔐 Livelli di Permesso - UNIFORMATI

### **Livello 4 (Admin/Owner) - Comandi Amministrativi**
Tutti i comandi admin ora richiedono **OP livello 4**:

#### Lobby/Queue Management
- `/thqueueadmin enable` - Abilita sistema coda
- `/thqueueadmin disable` - Disabilita sistema coda
- `/thqueueadmin clear` - Pulisce la coda
- `/thqueueadmin list` - Lista giocatori in coda
- `/thqueueadmin send <player>` - Invia giocatore al main
- `/thqueueadmin sendtolobby <player>` - Invia giocatore alla lobby (NUOVO)
- `/thqueueadmin sendnext` - Invia prossimo in coda
- `/thqueueadmin sendall` - Invia tutti al main
- `/thqueueadmin autotransfer <on|off>` - Auto-trasferimento
- `/thqueueadmin maxplayers <numero>` - Imposta max giocatori
- `/thqueueadmin info` - Info sistema coda
- `/thqueueadmin lobbymode <on|off>` - Modalità lobby
- `/thqueueadmin play` - Admin entra/coda manualmente
- `/thqueueadmin queue` - Controlla posizione coda (admin)

#### Claim Management
- `/claimadmin info <pos>` - Info claim
- `/claimadmin remove <pos>` - Rimuovi claim
- `/claimadmin tp <pos>` - Teleport a claim
- `/claimadmin playerinfo <player>` - Info claim giocatore
- `/claimadmin stats` - Statistiche claim
- `/claimadmin clearexpired` - Pulisce claim scaduti

#### Fatigue Management
- `/tharidia fatigue check <player>` - Controlla fatica
- `/tharidia fatigue checkall` - Controlla tutti
- `/tharidia fatigue config` - Mostra config
- `/tharidia fatigue set <player> <minuti>` - Imposta fatica
- `/tharidia fatigue reset <player>` - Reset fatica
- `/tharidia fatigue resetall` - Reset tutti
- `/tharidia fatigue bypass enable/disable <players>` - Bypass fatica
- `/tharidia fatigue bypass list` - Lista bypass

### **Livello 2 (Giocatori Normali) - Comandi Base**
Comandi disponibili per tutti i giocatori (nessun OP richiesto):

#### Claim Management (Giocatori)
- `/claim info` - Info claim alla tua posizione
- `/claim trust <player>` - Aggiungi giocatore fidato
- `/claim untrust <player>` - Rimuovi giocatore fidato
- `/claim abandon` - Abbandona claim
- `/claim list` - Lista tuoi claim

**RIMOSSO**: `/claim rent <giorni>` - I claim sono già tuoi quando li piazzi e devi pagarli con patate

#### Claim Flags (Solo Admin - Livello 4)
- `/claim flag explosions allow/deny` - Flag esplosioni (ADMIN ONLY)
- `/claim flag pvp allow/deny` - Flag PvP (ADMIN ONLY)
- `/claim flag mobs allow/deny` - Flag mob (ADMIN ONLY)
- `/claim flag fire allow/deny` - Flag fuoco (ADMIN ONLY)

### **Nessun Comando per Non-OP in Lobby**
- I giocatori non-OP nella lobby **NON hanno accesso a nessun comando** della mod
- Non possono chattare
- Non possono usare `/thqueue` (rimosso)
- Devono aspettare il trasferimento al main

---

## 📋 Riepilogo Modifiche File

### File Nuovi
1. **`LobbyChatBlocker.java`** - Blocca chat per non-OP in lobby
2. **`LobbyProtectionHandler.java`** - Forza spettatore e previene volo/exploit
3. **`ServerCommandBlocker.java`** - Blocca comando `/server`

### File Modificati
1. **`LobbyCommand.java`**
   - Rimosso `/thqueue` per giocatori normali
   - Aggiunto `/thqueueadmin sendtolobby <player>`
   - Aggiunto `/thqueueadmin queue` (solo admin)
   - Cambiato livello permesso da 2 a 4

2. **`ClaimAdminCommands.java`**
   - Cambiato livello permesso da 2 a 4

3. **`TharidiaThings.java`**
   - Registrati nuovi handler (LobbyChatBlocker, LobbyProtectionHandler, ServerCommandBlocker)
   - Aggiunto log di conferma

---

## ✅ Problemi Risolti

### 1. ✅ Chat Bloccata in Lobby
- **Prima**: I giocatori potevano chattare in lobby
- **Dopo**: Solo OP livello 2+ possono chattare in lobby

### 2. ✅ Volo/No Fall Damage
- **Prima**: Giocatori potevano volare o non prendere danno da caduta dopo respawn
- **Dopo**: Forzati in modalità spettatore ogni secondo, abilità volo rimosse

### 3. ✅ Comando /server Bloccato
- **Prima**: Giocatori potevano usare `/server main` o `/server lobby`
- **Dopo**: Bloccato per tutti tranne OP livello 4

### 4. ✅ Nessun Comando per Non-OP
- **Prima**: `/thqueue` disponibile a tutti
- **Dopo**: Nessun comando disponibile per non-OP

### 5. ✅ Trasferimento Bidirezionale
- **Prima**: Solo main → lobby possibile
- **Dopo**: Aggiunto `/thqueueadmin sendtolobby` per lobby → main

### 6. ✅ Livelli Permesso Uniformati
- **Prima**: Mix di livello 2 e 4
- **Dopo**: 
  - Livello 4: Tutti i comandi admin
  - Livello 2: Comandi giocatori normali (`/claim`)
  - Livello 0: Nessun comando mod

---

## 🚀 Come Testare

### Test 1: Chat Bloccata in Lobby
1. Imposta `isLobbyServer = true` nel config della lobby
2. Entra come giocatore non-OP
3. Prova a scrivere in chat
4. **Risultato atteso**: Messaggio di errore, chat bloccata

### Test 2: Protezione Spettatore
1. Entra in lobby come non-OP
2. Prova a cambiare modalità di gioco (se possibile)
3. Prova a volare
4. **Risultato atteso**: Forzato in spettatore, volo disabilitato

### Test 3: Comando /server Bloccato
1. Come giocatore normale, prova `/server main`
2. **Risultato atteso**: Comando bloccato, messaggio di errore

### Test 4: Nessun Comando per Non-OP
1. Come giocatore non-OP, prova `/thqueue`
2. **Risultato atteso**: Comando non trovato

### Test 5: Trasferimento Bidirezionale
1. Come OP livello 4, usa `/thqueueadmin sendtolobby <player>`
2. **Risultato atteso**: Giocatore trasferito alla lobby

### Test 6: Livelli Permesso
1. Come OP livello 2, prova `/thqueueadmin info`
2. **Risultato atteso**: Permesso negato
3. Come OP livello 4, prova `/thqueueadmin info`
4. **Risultato atteso**: Comando funziona

---

## 📝 Note Importanti

### Configurazione Richiesta

#### Lobby Server
```toml
# config/tharidiathings-common.toml
isLobbyServer = true
```

#### Main Server
```toml
# config/tharidiathings-common.toml
isLobbyServer = false
```

### Livelli OP
- **Livello 0**: Giocatore normale (nessun permesso)
- **Livello 2**: Può usare comandi base (`/claim`)
- **Livello 4**: Admin completo (tutti i comandi)

### Comandi per Dare OP
```
/op <player>           # Livello 4 (default)
/op <player> 2         # Livello 2 (non funziona in vanilla, usa 4)
```

**NOTA**: In Minecraft vanilla, `/op` dà sempre livello 4. Per dare livello 2, devi modificare `ops.json` manualmente.

---

## 🔍 Domande da Confermare

Ho uniformato i livelli come segue:
- **Livello 4**: Tutti i comandi admin (`/thqueueadmin`, `/claimadmin`, `/tharidia fatigue`)
- **Livello 2**: Comandi giocatori normali (`/claim`)

Se hai dubbi su qualche comando specifico, fammi sapere e lo aggiusto!

---

## 📦 Build e Deploy

Il JAR aggiornato è in: `build/libs/tharidiathings-1.1.4.jar`

Deploya lo stesso JAR su:
- ✅ Lobby server (con `isLobbyServer = true`)
- ✅ Main server (con `isLobbyServer = false`)

Riavvia entrambi i server e testa!
