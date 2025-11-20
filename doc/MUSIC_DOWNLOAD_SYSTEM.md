# Sistema di Download Automatico della Musica

## Panoramica

Il sistema di download automatico della musica permette ai client di scaricare automaticamente i file musicali MP3 dal server quando non sono presenti nella cache locale.

## Come Funziona

### Lato Client

1. Quando il client riceve un `ZoneMusicPacket` per riprodurre una musica:
   - Controlla se il file esiste nella cache locale (`minecraft/zone_music_cache/`)
   - Se esiste, lo riproduce direttamente
   - Se non esiste, invia un `RequestMusicFilePacket` al server

2. Il server risponde con uno o più `MusicFileDataPacket` contenenti i chunk del file

3. Il client assembla i chunk e salva il file nella cache

4. Una volta completato il download, il file viene riprodotto automaticamente

### Lato Server

1. Il server riceve il `RequestMusicFilePacket` dal client

2. Cerca il file nella directory `zone_music/` (nella root del server)

3. Divide il file in chunk da massimo 1MB ciascuno

4. Invia ogni chunk al client tramite `MusicFileDataPacket`

## Configurazione Server

### Posizionamento dei File Musicali

I file MP3 devono essere posizionati nella directory `zone_music/` nella root del server.

**IMPORTANTE**: La directory `zone_music/` viene creata automaticamente al primo avvio del server. Devi solo copiare i file MP3 al suo interno.

#### Struttura Directory

```
server/
├── zone_music/          ← Crea questa directory e metti i file MP3 qui
│   ├── rossi.mp3
│   ├── bianchi.mp3
│   └── verdi.mp3
├── world/
├── mods/
└── ...
```

#### Come Trovare la Directory

Quando un client richiede un file che non esiste, il server logga il path completo:

```
[Server thread/WARN]: Requested music file does not exist: /path/to/server/zone_music/rossi.mp3
[Server thread/WARN]: Please place MP3 files in: /path/to/server/zone_music
```

Usa questo path per sapere dove copiare i file MP3.

### Sicurezza

Il sistema include controlli di sicurezza:
- I file devono essere nella directory `zone_music/`
- Solo file con estensione `.mp3` possono essere scaricati
- Path traversal attacks sono prevenuti

## Packet

### RequestMusicFilePacket (Client → Server)

```java
public record RequestMusicFilePacket(String musicFile)
```

Inviato dal client per richiedere un file musicale.

### MusicFileDataPacket (Server → Client)

```java
public record MusicFileDataPacket(
    String musicFile,
    byte[] data,
    int chunkIndex,
    int totalChunks,
    boolean isLastChunk
)
```

Inviato dal server per trasferire i dati del file in chunk.

## Cache Client

I file scaricati vengono salvati in:
- **Linux/Mac**: `~/.minecraft/zone_music_cache/`
- **Windows**: `%APPDATA%/.minecraft/zone_music_cache/`
- **Flatpak (PrismLauncher)**: `~/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/instances/[instance]/minecraft/zone_music_cache/`

## Logging

Il sistema produce log dettagliati:

```
[MUSIC DOWNLOAD] Requesting music file from server: rossi.mp3
[MUSIC DOWNLOAD] Received chunk 1/3 for rossi.mp3
[MUSIC DOWNLOAD] Received chunk 2/3 for rossi.mp3
[MUSIC DOWNLOAD] Received chunk 3/3 for rossi.mp3
[MUSIC DOWNLOAD] Download complete for rossi.mp3, saving to cache...
[MUSIC DOWNLOAD] Saved rossi.mp3 (2458624 bytes) to cache
[MUSIC DOWNLOAD] Starting playback of downloaded file: rossi.mp3
```

## Controllo del Volume

Il volume della musica è controllato dalle impostazioni di Minecraft:

1. Apri le **Impostazioni** → **Musica e Suoni**
2. Regola il cursore **Musica**
3. Il volume si aggiorna automaticamente ogni ~0.25 secondi durante la riproduzione

Il sistema usa due metodi per applicare il volume:
- **Hardware control** (se supportato dalla scheda audio)
- **Software scaling** (applicato ai sample audio)

## Limitazioni

- Dimensione massima chunk: 1MB
- Solo file MP3 sono supportati
- I file devono essere nella directory `zone_music/` del server
- Il volume si aggiorna ogni 10 frame (~0.25 secondi)
