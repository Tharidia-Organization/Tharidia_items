# Sistema di Comandi Remoti per la Coda

## Panoramica

Questo sistema permette agli amministratori sul server **main** di gestire la coda del server **lobby** tramite comandi remoti che vengono memorizzati in un database condiviso.

## Architettura

1. **Server Main**: Gli admin eseguono comandi `/thqueueadmin` che vengono salvati nel database
2. **Database MySQL/MariaDB**: Memorizza i comandi in una tabella condivisa
3. **Server Lobby**: Controlla periodicamente il database ed esegue i comandi in coda

## Comandi Disponibili

Sul **server main**, gli admin (OP level 4) possono usare:

- `/thqueueadmin enable` - Abilita il sistema di coda
- `/thqueueadmin disable` - Disabilita il sistema di coda
- `/thqueueadmin clear` - Svuota completamente la coda
- `/thqueueadmin list` - Mostra tutti i giocatori in coda
- `/thqueueadmin sendnext` - Invia il prossimo giocatore in coda al main
- `/thqueueadmin sendall` - Invia tutti i giocatori in coda al main
- `/thqueueadmin send <player>` - Invia un giocatore specifico al main
- `/thqueueadmin autotransfer <on|off>` - Abilita/disabilita il trasferimento automatico
- `/thqueueadmin maxplayers <numero>` - Imposta il numero massimo di giocatori sul main
- `/thqueueadmin info` - Mostra informazioni sul sistema di coda
- `/thqueueadmin lobbymode <on|off>` - Abilita/disabilita la modalità lobby

## Configurazione Database

Per far funzionare il sistema, è necessario configurare un database MySQL/MariaDB condiviso:

### 1. Creare il database

```sql
CREATE DATABASE tharidia_queue;
CREATE USER 'tharidia_user'@'%' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON tharidia_queue.* TO 'tharidia_user'@'%';
FLUSH PRIVILEGES;
```

### 2. Configurare la mod

Nel file `config/tharidiathings-common.toml` su **entrambi i server**:

```toml
# Abilita il database
databaseEnabled = true

# Configurazione connessione
databaseHost = "127.0.0.1"
databasePort = 3306
databaseName = "tharidia_queue"
databaseUsername = "tharidia_user"
databasePassword = "your_password"
```

**IMPORTANTE**: Se i server sono su macchine diverse, assicurati che il database sia accessibile da entrambe.

## File Coinvolti

### File del Sistema Database

1. **DatabaseManager.java** - Gestisce la connessione al database con HikariCP
2. **DatabaseCommandQueue.java** - Gestisce la coda di comandi nel database
3. **CommandPoller.java** - Controlla periodicamente il database per nuovi comandi (solo lobby)

### File dei Comandi

1. **LobbyRemoteCommand.java** - Comandi registrati sul server main
2. **QueueCommandHandler.java** - Handler che esegue i comandi sul lobby

### File Modificati

1. **TharidiaThings.java** - Inizializzazione database e polling
2. **Config.java** - Aggiunta configurazione database
3. **build.gradle** - Aggiunta dipendenze MySQL e HikariCP

## Funzionamento Tecnico

1. Admin sul main esegue `/thqueueadmin <comando>`
2. `LobbyRemoteCommand` salva il comando nel database
3. `CommandPoller` sul lobby controlla il database ogni 2 secondi
4. Quando trova nuovi comandi, li esegue sul server thread
5. `QueueCommandHandler` esegue il comando sulla coda
6. Il comando viene marcato come eseguito nel database
7. I comandi vecchi (>1 ora) vengono eliminati automaticamente

## Configurazione Server

### Server Lobby

Nel file `tharidiathings-common.toml`:
```toml
isLobbyServer = true
```

### Server Main

Nel file `tharidiathings-common.toml`:
```toml
isLobbyServer = false
```

## Note Importanti

- I comandi richiedono OP level 4
- I feedback vengono inviati agli operatori online sul lobby
- Il sistema usa un database MySQL/MariaDB condiviso
- I comandi sono eseguiti in modo asincrono per non bloccare il server
- Il polling avviene ogni 2 secondi sul server lobby
- I comandi vecchi (>1 ora) vengono eliminati automaticamente

## Troubleshooting

### I comandi non funzionano

1. Verifica che `databaseEnabled = true` nel config
2. Controlla i log per errori di connessione al database
3. Assicurati che il database sia accessibile da entrambi i server
4. Verifica che l'admin abbia OP level 4
5. Controlla che il server lobby sia configurato con `isLobbyServer = true`

### Errori di connessione al database

1. Verifica le credenziali nel file di configurazione
2. Assicurati che il database esista e sia accessibile
3. Controlla che l'utente abbia i permessi necessari
4. Se i server sono su macchine diverse, verifica il firewall
5. Controlla i log del database per errori di autenticazione

### I comandi vengono eseguiti in ritardo

Il polling avviene ogni 2 secondi, quindi c'è un ritardo massimo di 2 secondi. Questo è normale e previsto.

## Esempio di Utilizzo

```bash
# Sul server main, un admin esegue:
/thqueueadmin list

# Il comando viene inviato al lobby
# Sul lobby, il sistema esegue il comando e risponde
# L'admin riceve la lista dei giocatori in coda
```
