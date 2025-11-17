# 🎵 Come Risolvere il Problema della Musica

## Problema Attuale

Il client richiede `rossi.mp3` ma il server non lo trova perché il file non esiste nella directory `zone_music/`.

## Soluzione Rapida

### 1. Trova dove si trova il server

Guarda i log del server. Quando richiedi la musica, vedrai:

```
[Server thread/WARN]: Requested music file does not exist: /path/completo/zone_music/rossi.mp3
[Server thread/WARN]: Please place MP3 files in: /path/completo/zone_music
```

Questo ti dice esattamente dove mettere i file.

### 2. Copia i file MP3

Vai nella directory indicata dal log e copia i tuoi file MP3:

```bash
# Esempio (usa il path dal tuo log)
cd /path/to/server
mkdir -p zone_music
cp /percorso/ai/tuoi/mp3/*.mp3 zone_music/
```

### 3. Verifica

Controlla che i file siano presenti:

```bash
ls -lh zone_music/
```

Dovresti vedere:
```
rossi.mp3
bianchi.mp3
verdi.mp3
...
```

### 4. Riprova

Ora quando il client richiede la musica, il server la troverà e la invierà automaticamente.

## Log di Successo

Quando funziona, vedrai questi log:

**Server:**
```
[Server thread/INFO]: Player Frenk012 requested music file: rossi.mp3
[Server thread/INFO]: Sending music file rossi.mp3 (2458624 bytes) to player Frenk012
[Server thread/INFO]: Successfully sent music file rossi.mp3 to player Frenk012
```

**Client:**
```
[Render thread/INFO]: [MUSIC DOWNLOAD] Requesting music file from server: rossi.mp3
[Render thread/INFO]: [MUSIC DOWNLOAD] Received chunk 1/3 for rossi.mp3
[Render thread/INFO]: [MUSIC DOWNLOAD] Received chunk 2/3 for rossi.mp3
[Render thread/INFO]: [MUSIC DOWNLOAD] Received chunk 3/3 for rossi.mp3
[Render thread/INFO]: [MUSIC DOWNLOAD] Download complete for rossi.mp3, saving to cache...
[Render thread/INFO]: [MUSIC DOWNLOAD] Saved rossi.mp3 (2458624 bytes) to cache
[Render thread/INFO]: [MUSIC DOWNLOAD] Starting playback of downloaded file: rossi.mp3
```

## Controllo del Volume

Per regolare il volume della musica:

1. Premi **ESC** per aprire il menu
2. Vai in **Impostazioni** → **Musica e Suoni**
3. Regola il cursore **Musica**
4. Il volume si aggiorna automaticamente in tempo reale (ogni ~0.25 secondi)

## Comportamento

- **Disconnessione**: La musica si ferma automaticamente quando esci dal server
- **Cambio dimensione**: La musica continua se rimani nella stessa zona
- **Morte**: La musica continua dopo il respawn se sei ancora nella zona

## Note

- La directory `zone_music/` viene creata automaticamente dal server
- Solo file `.mp3` sono supportati
- I file vengono scaricati una sola volta e salvati nella cache del client
- Se aggiorni un file sul server, il client deve cancellare la cache locale per riscaricarlo
- Il volume è controllato dalle impostazioni di Minecraft (cursore "Musica")
- La musica si ferma automaticamente quando ti disconnetti dal server
