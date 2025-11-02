# Fix Chat Session Packet Error - Velocity + NeoForge

## 🎯 Problema

**Errore**: `Sending unknown packet 'serverbound/minecraft:chat_session_update'`

**Causa**: Incompatibilità tra client NeoForge 1.21.1 e server tramite Velocity. Il client sta inviando pacchetti di chat session che Velocity non riconosce.

**NON è causato da `tharidiathings` mod** - è un problema di configurazione server/proxy.

---

## ✅ Soluzioni (in ordine di priorità)

### 1. **Disabilita Chat Signatures (QUICK FIX)**

**File**: `server.properties` (su entrambi i server: lobby e main)

```properties
enforce-secure-profile=false
```

**Riavvia i server** dopo la modifica.

**Perché funziona**: Disabilita il sistema di chat signatures di Minecraft 1.19+ che richiede l'invio del pacchetto `chat_session_update`.

---

### 2. **Configura Velocity Correttamente**

**File**: `velocity.toml` (sul server Velocity)

```toml
[advanced]
compression-threshold = 256
compression-level = -1
login-ratelimit = 3000

# IMPORTANTE: Usa forwarding moderno
player-info-forwarding-mode = "modern"

[servers]
lobby = "172.18.0.2:25565"
main = "172.18.0.3:25772"
try = ["lobby", "main"]
```

**Genera un nuovo secret** se non l'hai fatto:

```bash
# Velocity genererà automaticamente un secret al primo avvio
# Copialo da velocity.toml
```

---

### 3. **Configura i Server Backend**

**File**: `server.properties` (lobby e main)

```properties
online-mode=false
prevent-proxy-connections=false
enforce-secure-profile=false
```

**File**: `config/paper-global.yml` (se usi Paper/Purpur)

```yaml
proxies:
  velocity:
    enabled: true
    online-mode: true
    secret: "<copia-il-secret-da-velocity.toml>"
```

**File**: `config/neoforge-common.toml` (se usi NeoForge puro)

```toml
[general]
    # Permetti connessioni da proxy
    preventProxyConnections = false
```

---

### 4. **Installa No Chat Reports (Opzionale)**

Se vuoi mantenere le chat signatures ma evitare il crash, installa questa mod:

**Download**: [No Chat Reports](https://modrinth.com/mod/no-chat-reports)

**Installa su**:
- Client (tutti i giocatori)
- Server lobby
- Server main

**Versione**: `NeoForge 1.21.1`

---

### 5. **Verifica Versioni**

Assicurati che tutte le versioni siano compatibili:

| Componente | Versione Richiesta |
|------------|-------------------|
| Client NeoForge | `21.1.213` |
| Server NeoForge | `21.1.213` |
| Velocity | `3.3.0+` (latest) |
| Minecraft | `1.21.1` |

**Controlla versione server**:

```bash
# Nel server, controlla il file JAR
ls -la libraries/net/neoforged/neoforge/
```

---

## 🧪 Test della Soluzione

### Test 1: Disabilita Chat Signatures
1. Aggiungi `enforce-secure-profile=false` a `server.properties`
2. Riavvia server lobby e main
3. Connettiti con il client
4. **Risultato atteso**: Nessun crash, connessione riuscita

### Test 2: Verifica Velocity
1. Controlla log di Velocity durante la connessione
2. Cerca errori relativi a "unknown packet"
3. **Risultato atteso**: Nessun errore di pacchetti sconosciuti

### Test 3: Chat Funzionante
1. Connettiti al server
2. Scrivi in chat
3. **Risultato atteso**: Messaggi inviati senza errori

---

## 📊 Diagnostica

### Log da Controllare

**Velocity** (`logs/latest.log`):
```
[ERROR]: Exception handling packet
io.netty.handler.codec.EncoderException: Sending unknown packet
```

**Server Main/Lobby** (`logs/latest.log`):
```
[WARN]: Player disconnected: Internal Exception
```

**Client** (`logs/latest.log`):
```
io.netty.handler.codec.EncoderException: Sending unknown packet 'serverbound/minecraft:chat_session_update'
```

---

## 🔧 Configurazione Completa Raccomandata

### velocity.toml
```toml
bind = "0.0.0.0:25577"
motd = "Tharidia Server"
show-max-players = 100

[servers]
  lobby = "172.18.0.2:25565"
  main = "172.18.0.3:25772"
  try = ["lobby"]

[forced-hosts]

[advanced]
  compression-threshold = 256
  compression-level = -1
  login-ratelimit = 3000
  connection-timeout = 5000
  read-timeout = 30000
  haproxy-protocol = false
  tcp-fast-open = false
  bungee-plugin-message-channel = true
  show-ping-requests = false
  failover-on-unexpected-server-disconnect = true
  announce-proxy-commands = true
  log-command-executions = false
  log-player-connections = true

[query]
  enabled = false

[metrics]
  enabled = false

player-info-forwarding-mode = "modern"
forwarding-secret-file = "forwarding.secret"
announce-forge = false
kick-existing-players = false
ping-passthrough = "DISABLED"
enable-player-address-logging = true
```

### server.properties (Lobby e Main)
```properties
# Network
server-port=25565  # o 25772 per main
online-mode=false
prevent-proxy-connections=false

# Chat
enforce-secure-profile=false
previews-chat=false

# Performance
max-tick-time=60000
network-compression-threshold=256
```

### config/paper-global.yml (se usi Paper)
```yaml
proxies:
  velocity:
    enabled: true
    online-mode: true
    secret: "<il-tuo-secret>"
  bungee-cord:
    online-mode: false

unsupported-settings:
  allow-permanent-block-break-exploits: false
  allow-piston-duplication: false
  perform-username-validation: false
```

---

## ⚠️ Note Importanti

### 1. Security Secret
Il `forwarding-secret` in Velocity **DEVE** corrispondere al `secret` in `paper-global.yml` su ogni server backend.

### 2. Online Mode
- **Velocity**: `online-mode` può essere `true` o `false` (raccomandato `true` per autenticazione)
- **Server Backend**: `online-mode` **DEVE** essere `false`

### 3. Chat Signatures
Disabilitare `enforce-secure-profile` è sicuro e raccomandato per server con proxy come Velocity.

---

## 🎉 Risultato Atteso

Dopo aver applicato le configurazioni:

- ✅ Client si connette senza crash
- ✅ Nessun errore "unknown packet"
- ✅ Chat funzionante
- ✅ Velocity forwarding corretto
- ✅ UUID e skin dei giocatori preservati

---

## 🚨 Se il Problema Persiste

1. **Controlla i log di Velocity** per altri errori
2. **Verifica che le porte siano corrette** in `velocity.toml`
3. **Assicurati che i server backend siano avviati** prima di Velocity
4. **Controlla firewall/Docker** che non blocchi le connessioni interne
5. **Aggiorna Velocity** all'ultima versione

---

## 📝 Checklist Completa

- [ ] `enforce-secure-profile=false` in `server.properties` (lobby e main)
- [ ] `online-mode=false` in `server.properties` (lobby e main)
- [ ] `player-info-forwarding-mode = "modern"` in `velocity.toml`
- [ ] Secret copiato da Velocity a `paper-global.yml` (se usi Paper)
- [ ] Riavviati tutti i server (Velocity, lobby, main)
- [ ] Testato connessione client
- [ ] Testato chat
- [ ] Verificato UUID giocatori preservati

---

## ✅ Conclusione

Il problema **NON è causato da `tharidiathings-1.1.5.jar`** o `tharidia_tweaks`. È un problema di configurazione Velocity + NeoForge 1.21.1 relativo alle chat signatures.

La soluzione più semplice è **disabilitare `enforce-secure-profile`** su tutti i server backend.
