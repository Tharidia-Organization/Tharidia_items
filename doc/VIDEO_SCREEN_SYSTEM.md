# Sistema Video Screen - Documentazione

## Panoramica
Sistema per creare schermi video in-game che riproducono video YouTube direttamente nel mondo di Minecraft.

## Caratteristiche
- ✅ Nessun blocco custom richiesto
- ✅ Creazione schermi tramite selezione di due angoli opposti
- ✅ Schermi piatti (non diagonali) allineati agli assi X, Y o Z
- ✅ Adattamento automatico al rapporto d'aspetto 16:9 (o più vicino possibile)
- ✅ Riproduzione video YouTube in-game
- ✅ Controlli completi: play, stop, restart
- ✅ Sincronizzazione automatica tra tutti i giocatori
- ✅ Persistenza degli schermi (salvati nel mondo)

## Requisiti
- **VLC Media Player** deve essere installato sul client per la riproduzione video
- Permessi OP level 2 per usare i comandi

## Comandi

### Creazione Schermo
1. **Imposta primo angolo**: Guarda un blocco ed esegui
   ```
   /videoscreen pos1
   ```

2. **Imposta secondo angolo e crea schermo**: Guarda un altro blocco ed esegui
   ```
   /videoscreen pos2
   ```
   Lo schermo verrà creato tra i due punti. Deve essere piatto (allineato a un singolo asse).

### Gestione Video
3. **Imposta URL YouTube**: Stai vicino allo schermo ed esegui
   ```
   /videoscreen seturl <youtube_url>
   ```
   Esempio: `/videoscreen seturl https://www.youtube.com/watch?v=dQw4w9WgXcQ`
   
   Il video inizierà automaticamente a riprodursi.

4. **Avvia riproduzione**:
   ```
   /videoscreen play
   ```

5. **Ferma riproduzione**:
   ```
   /videoscreen stop
   ```

6. **Riavvia video**:
   ```
   /videoscreen restart
   ```

### Gestione Schermi
7. **Elimina schermo**: Stai vicino allo schermo ed esegui
   ```
   /videoscreen delete
   ```

8. **Lista schermi**: Mostra tutti gli schermi nella dimensione corrente
   ```
   /videoscreen list
   ```

## Note Tecniche

### Dimensioni Schermo
- Gli schermi si adattano automaticamente al rapporto d'aspetto più vicino
- Rapporti supportati: 16:9, 4:3, 1:1
- Dimensione massima consigliata: 20x20 blocchi

### Orientamento
Gli schermi possono essere orientati su tre assi:
- **Asse X**: Schermo verticale parallelo all'asse Z (parete est/ovest)
- **Asse Y**: Schermo orizzontale (pavimento/soffitto)
- **Asse Z**: Schermo verticale parallelo all'asse X (parete nord/sud)

### Performance
- I video vengono decodificati localmente su ogni client
- Richiede VLC installato sul sistema
- Consumo risorse dipende dalla risoluzione video

### Limitazioni
- Gli schermi devono essere piatti (non diagonali)
- Funziona solo con URL YouTube validi
- Richiede connessione internet per lo streaming
- VLC deve essere installato sul client

## Troubleshooting

### Video non si riproduce
1. Verifica che VLC sia installato sul sistema
2. Controlla che l'URL YouTube sia valido
3. Verifica la connessione internet
4. Controlla i log per errori di VLC

### Schermo non visibile
1. Verifica di essere nella dimensione corretta
2. Controlla che lo schermo sia stato creato correttamente con `/videoscreen list`
3. Riconnettiti al server per forzare la sincronizzazione

### Errore "Screen must be flat"
Lo schermo deve essere allineato a un singolo asse. Assicurati che:
- Due coordinate siano diverse tra i due angoli
- Una coordinata sia identica tra i due angoli

## Esempi d'Uso

### Cinema 16:9
```
/videoscreen pos1    # Guarda angolo in basso a sinistra
/videoscreen pos2    # Guarda angolo in alto a destra (16 blocchi larghezza, 9 altezza)
/videoscreen seturl https://www.youtube.com/watch?v=...
```

### Schermo Quadrato
```
/videoscreen pos1    # Guarda un angolo
/videoscreen pos2    # Guarda l'angolo opposto (stessa larghezza e altezza)
/videoscreen seturl https://www.youtube.com/watch?v=...
```

### Schermo Orizzontale (Tavolo)
```
/videoscreen pos1    # Guarda un punto sul pavimento
/videoscreen pos2    # Guarda un altro punto sullo stesso livello Y
/videoscreen seturl https://www.youtube.com/watch?v=...
```

## Architettura Tecnica

### Componenti Server-Side
- `VideoScreen.java`: Rappresentazione dello schermo
- `VideoScreenRegistry.java`: Registry persistente degli schermi
- `VideoScreenCommands.java`: Gestione comandi
- `VideoScreenSyncPacket.java`: Sincronizzazione dati
- `YouTubeUrlExtractor.java`: Estrazione URL stream

### Componenti Client-Side
- `ClientVideoScreenManager.java`: Gestione schermi lato client
- `VLCVideoPlayer.java`: Player video con VLC
- `VideoScreenRenderer.java`: Rendering schermi nel mondo
- `VideoScreenRenderHandler.java`: Event handler rendering

### Networking
- Sincronizzazione automatica alla creazione/modifica
- Sync completo al login del giocatore
- Packet di delete per rimozione schermi

## Sviluppi Futuri
- [ ] Supporto per altre piattaforme video (Twitch, Vimeo)
- [ ] Controllo volume individuale
- [ ] Playlist e coda video
- [ ] Permessi granulari per utenti
- [ ] UI grafica per gestione schermi
