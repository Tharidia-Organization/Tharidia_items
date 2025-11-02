# Pre-Login Name Selection System

## Overview
Sistema di selezione nome che appare **prima** che il giocatore entri nel mondo, durante la fase di connessione al server.

## Flusso Operativo

### 1. Connessione Client → Server
Il client stabilisce connessione con il server.

### 2. Verifica Nome (Server-Side)
- `PreLoginNameHandler` intercetta l'evento `PlayerLoggedInEvent`
- Usa reflection per chiamare `NameService.needsToChooseName()` di Tharidia Tweaks
- Invia `RequestNamePacket` al client con il risultato

### 3. Dialogo Pre-Login (Client-Side)
- `ClientConnectionHandler` riceve il packet
- Se `needsName = true`, mostra `PreLoginNameScreen`
- Lo screen appare **prima** del caricamento del mondo
- Il giocatore inserisce il nome e preme Confirm

### 4. Invio Nome (Client → Server)
- `PreLoginNameScreen` invia `SubmitNamePacket` con il nome scelto
- Il server valida e salva il nome tramite `NameService.submitDisplayName()`

### 5. Completamento
- Se successo: lo screen si chiude e il login continua
- Se errore: il server invia messaggio di errore al giocatore

## File Coinvolti

### Server-Side
- **`PreLoginNameHandler.java`** - Handler che verifica se serve il nome al login
- **`RequestNamePacket.java`** - Packet server→client per richiedere selezione nome
- **`SubmitNamePacket.java`** - Packet client→server per inviare il nome scelto

### Client-Side
- **`PreLoginNameScreen.java`** - Schermo pre-login per inserimento nome
- **`ClientConnectionHandler.java`** - Gestisce la visualizzazione dello screen
- **`ClientPacketHandler.java`** - Handler per `RequestNamePacket`

### Registrazioni
- **`TharidiaThings.java`** - Registra handler e packet

## Caratteristiche

### Pre-Login Screen
- ✅ Appare **prima** del caricamento del mondo
- ✅ Non può essere chiuso con ESC
- ✅ Campo di testo con limite 16 caratteri
- ✅ Pulsante Confirm attivo solo con nome valido
- ✅ Supporto tasto ENTER per confermare
- ✅ Messaggi di errore/successo

### Comunicazione con Tharidia Tweaks
- ✅ Usa **reflection** per evitare dipendenze hard
- ✅ Chiama `NameService.needsToChooseName()` per verificare
- ✅ Chiama `NameService.submitDisplayName()` per salvare
- ✅ Gestisce `ValidationResult` con metodi `ok()`, `sanitized()`, `error()`

### Sicurezza
- ✅ Previene doppia sottomissione
- ✅ Validazione server-side tramite Tharidia Tweaks
- ✅ Gestione errori graceful

## Differenze dal Sistema Precedente

| Aspetto | Sistema Vecchio | Sistema Nuovo |
|---------|----------------|---------------|
| **Quando appare** | Dopo login, nel mondo | Prima del login |
| **Tipo GUI** | Container Menu in-game | Screen pre-login |
| **Chiudibile** | Sì (si riapriva) | No (bloccato) |
| **Caricamento mondo** | Già caricato | Non ancora caricato |
| **Esperienza utente** | Invasiva | Fluida |

## Testing

Per testare il sistema:
1. Connettiti al server senza aver mai scelto un nome
2. Lo screen pre-login dovrebbe apparire immediatamente
3. Inserisci un nome e premi Confirm o ENTER
4. Il nome viene validato e salvato da Tharidia Tweaks
5. Il login continua normalmente

## Note Tecniche

- Il sistema usa `ClientPlayerNetworkEvent.LoggingIn` per intercettare la connessione
- Lo screen viene mostrato tramite `Minecraft.getInstance().setScreen()`
- Il packet `RequestNamePacket` viene inviato subito dopo il login
- La chiusura dello screen avviene automaticamente dopo l'invio del nome
