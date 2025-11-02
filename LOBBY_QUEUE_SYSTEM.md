# Lobby Queue System - Tharidia Tweaks

Sistema completo di gestione coda per server lobby NeoForge con integrazione Velocity.

## 📋 Panoramica

Il sistema lobby permette di:
- Gestire una coda di giocatori in attesa di entrare nel server principale
- Trasferire automaticamente o manualmente i giocatori tra server via Velocity
- Modalità lobby con spawn in spectator mode
- Comandi admin completi per la gestione

---

## 🎮 Comandi Giocatore

### `/queue`
Mostra la tua posizione nella coda.

**Output:**
```
[QUEUE] You are in position #3
Queue size: 15 | Wait time: 45s
```

### `/play`
Entra nella coda per il server principale (o trasferimento immediato se la coda è disabilitata).

---

## 🛠️ Comandi Admin

Tutti i comandi admin richiedono OP level 2.

### `/queueadmin lobbymode <on|off>`
Abilita/disabilita la modalità lobby.

**Quando abilitata:**
- I giocatori spawnano in spectator mode
- Ricevono un messaggio di benvenuto
- Vengono automaticamente aggiunti alla coda (se abilitata)

**Esempio:**
```
/queueadmin lobbymode on
```

### `/queueadmin enable`
Abilita il sistema di coda.

### `/queueadmin disable`
Disabilita il sistema di coda (trasferimenti immediati).

### `/queueadmin clear`
Svuota completamente la coda.

### `/queueadmin list`
Mostra tutti i giocatori in coda con tempo di attesa.

**Output:**
```
=== Queue List (5 players) ===
1. Frenk012 (waited: 30s)
2. PlayerName (waited: 25s)
3. AnotherPlayer (waited: 20s)
```

### `/queueadmin send <player>`
Trasferisce un giocatore specifico al server principale.

**Esempio:**
```
/queueadmin send Frenk012
```

### `/queueadmin sendnext`
Trasferisce il prossimo giocatore in coda.

### `/queueadmin sendall`
Trasferisce tutti i giocatori in coda al server principale.

### `/queueadmin autotransfer <on|off>`
Abilita/disabilita il trasferimento automatico.

**Quando abilitato:**
- I giocatori vengono trasferiti automaticamente quando entrano nella coda
- Utile per testing o quando il server principale ha sempre spazio

**Esempio:**
```
/queueadmin autotransfer on
```

### `/queueadmin maxplayers <number>`
Imposta il numero massimo di giocatori sul server principale.

**Esempio:**
```
/queueadmin maxplayers 100
```

### `/queueadmin info`
Mostra informazioni complete sul sistema.

**Output:**
```
=== Queue Information ===
Lobby Mode: Enabled
Queue Status: Enabled
Auto-transfer: Disabled
Max players: 100
Current queue size: 5
```

---

## 🔧 Setup e Configurazione

### 1. Configurazione Velocity

Nel file `velocity.toml`:

```toml
[servers]
  lobby = "172.18.0.X:25566"  # IP Docker del server lobby
  main = "172.18.0.2:25772"   # IP Docker del server principale

try = [
  "lobby"
]

[forced-hosts]
  "mainserver.chroniclesrp.it" = [
    "lobby"
  ]
```

### 2. Configurazione Server Lobby

**server.properties:**
```properties
online-mode=false
server-port=25566
gamemode=spectator
difficulty=peaceful
```

**config/paper-global.yml** (se usi Paper) o configurazione NeoForge equivalente:
```yaml
proxies:
  velocity:
    enabled: true
    online-mode: true
    secret: "TUO_SECRET_VELOCITY"
```

### 3. Avvio del Sistema

Quando il server si avvia, il sistema lobby è **disabilitato di default**.

Per attivarlo:
```
/queueadmin lobbymode on
/queueadmin enable
```

---

## 📊 Workflow Tipico

### Scenario 1: Server Pieno (Coda Attiva)

1. **Admin abilita il sistema:**
   ```
   /queueadmin lobbymode on
   /queueadmin enable
   /queueadmin maxplayers 100
   ```

2. **Giocatore si connette:**
   - Spawna in spectator mode
   - Vede messaggio di benvenuto
   - Viene aggiunto automaticamente alla coda

3. **Giocatore controlla posizione:**
   ```
   /queue
   ```

4. **Admin gestisce la coda:**
   ```
   /queueadmin list
   /queueadmin sendnext
   ```

### Scenario 2: Testing/Sviluppo (Trasferimento Immediato)

1. **Admin configura trasferimento automatico:**
   ```
   /queueadmin lobbymode on
   /queueadmin autotransfer on
   ```

2. **Giocatore si connette:**
   - Spawna in spectator
   - Viene immediatamente trasferito al main server

### Scenario 3: Server Normale (No Lobby)

1. **Lobby mode disabilitato (default):**
   ```
   /queueadmin lobbymode off
   ```

2. **Giocatori spawnano normalmente** senza restrizioni

---

## 🔍 Troubleshooting

### I giocatori non vengono trasferiti

**Verifica:**
1. Velocity è configurato correttamente?
2. Il server "main" è raggiungibile da Velocity?
3. Il forwarding secret è identico su tutti i server?

**Test:**
```
/queueadmin send <player>
```

Se questo funziona, il problema è nella coda automatica.

### La coda non si svuota

**Soluzione:**
```
/queueadmin sendall
```

O abilita auto-transfer:
```
/queueadmin autotransfer on
```

### I giocatori non spawnano in spectator

**Verifica che lobby mode sia abilitato:**
```
/queueadmin info
```

Se è disabilitato:
```
/queueadmin lobbymode on
```

---

## 🎯 Best Practices

1. **Usa lobby mode solo sul server lobby**, non sul main server
2. **Disabilita auto-transfer** in produzione per controllare manualmente il flusso
3. **Monitora la coda** regolarmente con `/queueadmin list`
4. **Imposta maxplayers** in base alle capacità del tuo server
5. **Testa sempre** con `/queueadmin autotransfer on` prima di andare in produzione

---

## 📝 Note Tecniche

- Il sistema usa **Velocity's modern forwarding** per i trasferimenti
- La coda è **thread-safe** e supporta operazioni concorrenti
- I giocatori vengono **automaticamente rimossi** dalla coda quando si disconnettono
- Il sistema è **completamente server-side**, non richiede mod client

---

## 🚀 Prossimi Passi

Dopo aver compilato la mod:

1. **Compila:**
   ```bash
   ./gradlew build
   ```

2. **Installa sul server lobby:**
   ```bash
   cp build/libs/tharidiatweaks-1.1.13.jar /path/to/lobby/mods/
   ```

3. **Riavvia il server lobby**

4. **Configura:**
   ```
   /queueadmin lobbymode on
   /queueadmin enable
   ```

5. **Testa la connessione!**
