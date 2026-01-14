# Handshake Bypass System

## Panoramica
Sistema integrato in Tharidia Things che bypassa i controlli di handshake di NeoForge, permettendo la connessione a server anche con versioni diverse delle mod o server vanilla.

## ⚠️ AVVERTENZA
**Questo sistema bypassa i controlli di sicurezza di NeoForge!**
- Può causare problemi se le versioni delle mod sono veramente incompatibili
- Usare a proprio rischio
- Potrebbe causare crash o comportamenti imprevisti

## Come Funziona

### Implementazione
Il sistema usa un event listener con **priorità HIGHEST** che intercetta l'evento `ClientPlayerNetworkEvent.LoggingIn` prima che NeoForge esegua la validazione dell'handshake.

### File Coinvolti
- **`HandshakeBypass.java`** - Handler principale che bypassa la validazione
- **`TharidiaThings.java`** - Registra l'handler solo lato CLIENT

### Registrazione
```java
// Register handshake bypass (CLIENT ONLY)
if (FMLEnvironment.dist == Dist.CLIENT) {
    NeoForge.EVENT_BUS.register(com.THproject.tharidia_things.client.HandshakeBypass.class);
    LOGGER.warn("Handshake bypass registered - you can connect to servers with different mod versions");
}
```

## Caratteristiche

### ✅ Automatico
- Si attiva automaticamente quando il client si connette a un server
- Non richiede configurazione manuale
- Funziona solo lato client

### ✅ Logging Dettagliato
Quando il bypass si attiva, vedrai nei log:
```
=======================================================
HANDSHAKE BYPASS ACTIVE
Attempting to connect to server bypassing mod checks
This may cause issues with incompatible mod versions
=======================================================
```

### ✅ Disabilitabile
Il sistema può essere disabilitato programmaticamente:
```java
HandshakeBypass.setBypassEnabled(false);
```

## Casi d'Uso

### 1. Connessione a Server Vanilla
Permette di connettersi a server vanilla anche avendo mod installate lato client.

### 2. Versioni Mod Diverse
Permette di connettersi a server con versioni diverse di Tharidia Things o altre mod.

### 3. Testing
Utile per testare la compatibilità tra diverse versioni senza dover ricompilare.

## Limitazioni

### ❌ Non Garantisce Compatibilità
Il bypass permette la connessione ma **non garantisce** che le mod funzionino correttamente se le versioni sono incompatibili.

### ❌ Possibili Problemi
- **Crash:** Se le mod hanno cambiamenti incompatibili nel protocollo
- **Comportamenti strani:** Funzionalità che non funzionano come previsto
- **Desync:** Differenze tra client e server

### ❌ Solo Client-Side
Il bypass funziona solo lato client. Il server deve comunque accettare la connessione.

## Dettagli Tecnici

### Event Priority
Usa `EventPriority.HIGHEST` per eseguire prima dei controlli di NeoForge:
```java
@SubscribeEvent(priority = EventPriority.HIGHEST)
public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event)
```

### Reflection
Tenta di accedere alle classi interne di NeoForge per manipolare lo stato dell'handshake:
```java
Class<?> handshakeHandlerClass = Class.forName("net.neoforged.neoforge.network.handlers.ClientHandshakeHandler");
```

### Graceful Degradation
Se il bypass fallisce, il sistema logga l'errore ma non crasha:
```java
catch (Exception e) {
    LOGGER.error("Failed to bypass handshake check", e);
}
```

## Configurazione Futura

Il sistema è predisposto per essere configurabile tramite config file:
```java
private static boolean bypassEnabled = true; // Can be toggled via config if needed
```

Potrebbe essere aggiunto un'opzione nel config per:
- Abilitare/disabilitare il bypass
- Whitelist di server dove applicare il bypass
- Blacklist di mod da non bypassare

## Testing

Per testare il bypass:

1. **Compila la mod:**
   ```bash
   ./gradlew build
   ```

2. **Installa sul client**

3. **Prova a connetterti a:**
   - Server vanilla
   - Server con versione diversa di Tharidia Things
   - Server senza Tharidia Things

4. **Controlla i log:**
   - Cerca il messaggio "HANDSHAKE BYPASS ACTIVE"
   - Verifica che la connessione venga stabilita

## Note di Sicurezza

### ⚠️ Rischi
- Bypassa i controlli di sicurezza di NeoForge
- Potrebbe permettere connessioni a server malevoli
- Non raccomandato per uso in produzione

### ✅ Mitigazioni
- Funziona solo lato client
- Logging dettagliato di ogni bypass
- Può essere disabilitato facilmente
- Non modifica il comportamento del server

## Compatibilità

- **NeoForge:** 21.1.x
- **Minecraft:** 1.21.1
- **Side:** CLIENT ONLY
- **Dipendenze:** Nessuna (usa solo reflection)

## Alternative

Se il bypass non funziona, alternative includono:
1. Usare la stessa versione delle mod su client e server
2. Rimuovere le mod incompatibili dal client
3. Usare un proxy/wrapper che modifica i packet di handshake
