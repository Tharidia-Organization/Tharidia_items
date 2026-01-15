

# Changelog 0.5.0

### Info generiche
- **QUESTO CHANGELOG RACCHIUDE TUTTE LE NOVITÀ E IN PIÙ TUTTE LE MODALITÀ/FUNZIONALITÀ AD OGGI IMPLEMENTATE E FUNZIONANTI**.
- Rimosse **15** mod, queste erano mod per cibo e simili, abbiamo aggiunto e tenuto solo le mod specifiche per ciò che ci serve.
- Il client ora conta 266 mod, dalle 327 da cui eravamo partiti.
- Dev server aggiornato, a breve aggiornerò il main, per permettere a chi ha piacere di accedere alla **Taverna**.
- I dettagli tecnici dei dungeon stanno venendo scritti in questi giorni, di seguito troverete quello che è stato deciso sin ad oggi.
- Stanno venendo revisionati tutti i concetti fondamentali sin dal principio, ci saranno successivi changelog in merito, al completamento di ciascuna definizione.
- Abbiamo una stima di completamento per una prima release **ALPHA**, questa versione verrà aperta a tutti e servirà come primo debug, test e review del lavoro fatto, **Avrà la durata di un mese, dopo il quale il server verrà chiuso per completare lo sviluppo e risolvere bug**. -- **SEGUIRANNO COMUNICAZIONI**

## 🎮 Nuove Funzionalità

### Sistema Dieta
- **Nuovo sistema nutrizionale completo** - Ogni cibo ha profili nutrizionali (Grano, Proteine, Verdure, Frutta, Zuccheri, Acqua)
- GUI dedicata per visualizzare lo stato della dieta
- Buff e debuff applicati in base ai valori nutrizionali
- Compatibilità con mod esterne per i cibi
- Cache persistente per ottimizzare le performance

### Sistema Stamina
- **Stamina di combattimento** integrata con Epic Fight
- Consumo stamina per attacchi (basato sul peso dell'arma)
- Roll consuma stamina in base al peso dell'armatura
- Scudo alzato blocca la rigenerazione stamina
- Archi consumano stamina in base al tempo di tensione
- Rigenerazione ridotta durante il combattimento

### Sistema Peso
- Velocità movimento influenzata dalla pesantezza dell'inventario
- Sistema di merge configurazioni da file multipli
- Sincronizzazione config peso tra server e client

### Battle Gauntlet (Guanto da Duello)
- Nuovo item per sfidare altri giocatori a duello
- GUI di invito/accettazione battaglia
- Effetti visivi e sonori all'inizio e fine battaglia
- Sistema freeze durante il duello
- Gestione logout durante battaglia

### Sistema Fallen/Revive
- Logica "caduto" per morte in battaglia
- Sistema di revive per player morti
- Comandi per gestione fallen

### Realm & Claims
- **Nuovo modello 3D animato** per il blocco Realm (GeckoLib)
- Fix collision e boundaries dei regni
- Fix claim piazzabili: max 4 claim per player, 1 solo regno per mondo
- Blocco PvP dentro i claim
- Blocco esplosioni dentro i claim

### Video In-Game
- Riproduzione video YouTube e stream Twitch in gioco
- Supporto Windows e Linux
- Installer automatico dipendenze (FFmpeg, yt-dlp)

### Character Creation
- Nuova dimensione dedicata alla creazione personaggio
- Piattaforma con bordo world border personalizzato
- Teleport automatico per nuovi player

## 🔧 Fix Importanti

- Fix dieta che si resettava a 0 al relog
- Fix nametag visibili attraverso i muri
- Fix crash vector realm boundaries
- Fix database chat sempre vuoto
- Fix sonno di giorno
- Fix ambient model occlusion
- Fix HashMap → ConcurrentHashMap (thread safety)

## 🎨 Assets

- Nuova texture dado
- Nuova texture e modello Realm Stage 1
- Nuova texture Battle Gauntlet

---

# 📋 Regole e Decisioni di Design

## Sistema Stamina - Regole Core
- **Scopo**: Combat più "action" e skill-based, stile Dark Souls
- La stamina è attiva **solo in combat** (ultimi X secondi di danno dato/ricevuto)
- **Correre**: non consuma stamina, ma disabilitato sotto una % minima
- **Roll**: consuma stamina con curva basata su peso armatura
- **Parry**: non consuma stamina
- **Scudo alzato**: blocca rigenerazione stamina
- **Nessun consumabile** ripristina stamina
- Progressione lineare con passive e stat che modificano la stamina

## Movimento
- Velocità base vanilla, non incrementabile con tier
- Roll: sì | Dash: no (categorico)
- Velocità influenzata dal peso inventario
- Armatura peso standardizzato indipendente da classe/tier
- Movimento equiparato per tutte le taglie

## Dungeon PvE
- **Adventure mode** obbligatorio
- Alla morte: perdi consumabili, loot dungeon, % durabilità equip, % monete
- Moduli consumabili con wave di mob
- Loot chest sbloccata dopo completamento modulo
- Mob scelti randomicamente con sistema peso/difficoltà
- Piani superiori con difficoltà e loot aumentati
- Stanze boss opzionali con mini-boss
- XP al passaggio di piano, bonus XP completando piano 2

---

||@Colono||