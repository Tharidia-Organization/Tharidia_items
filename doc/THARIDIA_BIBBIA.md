# THARIDIA - LA BIBBIA COMPLETA

**Guida Utente per Tharidia Things, Features e Tweaks**

---

## INDICE GENERALE

### PARTE 1: THARIDIA THINGS
- [1.1 Sistema Claim e Realm](#11-sistema-claim-e-realm)
- [1.2 Sistema Stamina](#12-sistema-stamina)
- [1.3 Sistema Peso](#13-sistema-peso)
- [1.4 Sistema Dieta](#14-sistema-dieta)
- [1.5 Sistema Fatica](#15-sistema-fatica)
- [1.6 Sistema Trade](#16-sistema-trade)
- [1.7 Sistema Battaglia](#17-sistema-battaglia)
- [1.8 Sistema Stalla](#18-sistema-stalla)
- [1.9 Forgiatura](#19-forgiatura)
- [1.10 Video Screen](#110-video-screen)

### PARTE 2: THARIDIA FEATURES
- [2.1 Sistema Cave](#21-sistema-cave)
- [2.2 Sistema Dungeon](#22-sistema-dungeon)
- [2.3 Sistema Mercato](#23-sistema-mercato)
- [2.4 Sistema Schematic](#24-sistema-schematic)
- [2.5 World Border](#25-world-border)
- [2.6 Strumenti Admin](#26-strumenti-admin)

### PARTE 3: THARIDIA TWEAKS
- [3.1 Sistema Zone](#31-sistema-zone)
- [3.2 Sistema Razze](#32-sistema-razze)
- [3.3 Statistiche e Leaderboard](#33-statistiche-e-leaderboard)
- [3.4 Gestione Inventario](#34-gestione-inventario)
- [3.5 Integrazione Discord](#35-integrazione-discord)
- [3.6 Blocchi Spawn/Growth/XP](#36-blocchi-spawngrowthxp)
- [3.7 RPG Gates](#37-rpg-gates)
- [3.8 Comandi Utility](#38-comandi-utility)

---

# PARTE 1: THARIDIA THINGS

---

## 1.1 SISTEMA CLAIM E REALM

### Panoramica

```
╔═══════════════════════════════════════════════════════════════╗
║                      GERARCHIA TERRITORIALE                   ║
╠═══════════════════════════════════════════════════════════════╣
║                                                               ║
║     ┌─────────────────────────────────────┐                   ║
║     │            REALM (Regno)            │                   ║
║     │   • Area grande espandibile         │                   ║
║     │   • Gerarchia di ranghi             │                   ║
║     │   • Contiene più CLAIM              │                   ║
║     │                                     │                   ║
║     │   ┌─────────┐  ┌─────────┐         │                   ║
║     │   │  CLAIM  │  │  CLAIM  │  ...    │                   ║
║     │   │ (16x16) │  │ (16x16) │         │                   ║
║     │   └─────────┘  └─────────┘         │                   ║
║     └─────────────────────────────────────┘                   ║
║                                                               ║
╚═══════════════════════════════════════════════════════════════╝
```

### REALM (Regno)

**Creazione:**
1. Crafta il blocco **Pietro**
2. Piazzalo dove vuoi il centro
3. Apri il menu (click destro)
4. Deposita patate → ottieni monete
5. Espandi il regno con le monete

**Gerarchia Ranghi:**
```
┌────────────────┬───────────────────┐
│ RANGO          │ PARTICELLA        │
├────────────────┼───────────────────┤
│ LORD           │ ● Rosso cremisi   │
│ CONSIGLIERE    │ ● Oro antico      │
│ GUARDIA        │ ● Giallo pallido  │
│ MILIZIANO      │ ● Verde foresta   │
│ COLONO         │ ● Grigio pietra   │
└────────────────┴───────────────────┘
```
*Una particella appare sulla spalla ogni ~3.5 secondi*

### CLAIM (Dominio)

**Area protetta:** 1 chunk (16x16), da -20 a +40 blocchi dal claim

**Protezioni attive:**
| Azione | Protetta |
|--------|----------|
| Rompere/Piazzare blocchi | ✅ |
| Aprire contenitori | ✅ |
| Usare porte/leve | ✅ |
| PvP | ✅ Sempre |
| Esplosioni/Fuoco | ✅ |
| Pistoni/Fluidi esterni | ✅ |

### Flag Granulari

Permetti azioni specifiche a TUTTI:

| Comando | Permette |
|---------|----------|
| `/claim containers allow` | Aprire casse, fornaci |
| `/claim doors allow` | Usare porte, cancelli |
| `/claim switches allow` | Usare leve, pulsanti |
| `/claim vehicles allow` | Piazzare barche/minecart |
| `/claim animals allow` | Interagire con animali |

### Sistema Trust

```
┌─────────────────────────────────────────┐
│ METODO 1: Comando                       │
│ /claim trust <giocatore>                │
├─────────────────────────────────────────┤
│ METODO 2: Contratto di Fiducia          │
│ 1. Crafta (8 carta + 1 inchiostro)      │
│ 2. Click destro sul TUO claim           │
│ 3. Dai il contratto al giocatore        │
│ 4. Lui lo usa (click destro in aria)    │
│ 5. Diventa trusted!                     │
└─────────────────────────────────────────┘
```

### Scadenza Claim

```
ATTIVO ──────► GRACE PERIOD ──────► DECAY
              (3 giorni)           (14 giorni)
              Protezione OFF       Blocchi funzionali rimossi
```

### Comandi Claim
```
/claim trust <player>      Aggiungi fidato
/claim untrust <player>    Rimuovi fidato
/claim list                Lista tuoi claim
/claim info                Info claim attuale
/claim flags               Mostra flag
```

---

## 1.2 SISTEMA STAMINA

### Barra Stamina

```
╔═══════════════════════════════════════╗
║ STAMINA [████████████░░░░░░] 65/100   ║
╠═══════════════════════════════════════╣
║ • Si consuma con: attacchi, arco      ║
║ • Si rigenera: stando fermi           ║
║ • In combattimento: regen ridotta 50% ║
╚═══════════════════════════════════════╝
```

### Consumo Stamina

| Azione | Consumo |
|--------|---------|
| Attacco base | ~15 stamina |
| Tensione arco | 8 stamina/sec |
| Rilascio arco | 4+ stamina |
| Sprint | Blocca sotto 20% |

### Rigenerazione

| Stato | Regen |
|-------|-------|
| Fermo, fuori combattimento | 15/sec |
| Fermo, in combattimento | 7.5/sec |
| Dopo azione | 0.8 sec delay |

### Indicatori

- **In combattimento:** 7 secondi dopo ultimo danno
- **Arco bloccato:** Non puoi tendere sotto soglia
- **Sprint bloccato:** Sotto 20% stamina

---

## 1.3 SISTEMA PESO

### Soglie e Effetti

```
┌──────────────────┬────────────┬────────────────┐
│ PESO             │ VELOCITÀ   │ NUOTO          │
├──────────────────┼────────────┼────────────────┤
│ 0-200   LEGGERO  │ 95%        │ ✅ OK          │
│ 200-400 MEDIO    │ 85%        │ ✅ OK          │
│ 400-700 PESANTE  │ 70%        │ ❌ Bloccato    │
│ 700+    SOVRAC.  │ 50%        │ ❌ Bloccato    │
└──────────────────┴────────────┴────────────────┘
```

### Calcolo

- Ogni item ha un peso
- Peso totale = somma di tutti gli slot (inventario + armatura)
- HUD mostra peso attuale

---

## 1.4 SISTEMA DIETA

### Categorie Nutrienti

```
┌─────────────┬─────────────┬─────────────┐
│ CEREALI     │ PROTEINE    │ VERDURE     │
│ (pane,      │ (carne,     │ (carote,    │
│ patate)     │ pesce)      │ barbab.)    │
├─────────────┼─────────────┼─────────────┤
│ FRUTTA      │ ZUCCHERI    │ IDRATAZIONE │
│ (mele,      │ (dolci,     │ (acqua,     │
│ bacche)     │ miele)      │ zuppe)      │
└─────────────┴─────────────┴─────────────┘
```

### Effetti

| Categoria | Basso (<20%) | Alto (>80%) |
|-----------|--------------|-------------|
| Cereali | Lentezza, Mining Fatigue | Velocità |
| Proteine | Debolezza, -4 HP max | +2 HP max |
| Verdure | - | Resistenza |
| Frutta | -10% movimento | +10% movimento |
| Zuccheri | -15% attack speed | Haste |
| Idratazione | Nausea, -10% speed | Water Breathing |

### Decay

I nutrienti calano nel tempo:
- Cereali: 0.15/tick
- Proteine: 0.2/tick
- Verdure/Frutta: 0.25/tick
- Zuccheri: 0.3/tick
- Idratazione: 0.35/tick

---

## 1.5 SISTEMA FATICA

### Come Funziona

```
SVEGLIO ════════════════════════════════► ESAUSTO
0 min                                     40 min

         ⚠️ Warning 5 min    ⚠️ Warning 1 min
              │                    │
              ▼                    ▼
         "Sei stanco!"      "Devi riposare!"
```

### Effetti Esaurimento

- **Slowness III** (-60% velocità)
- **Nausea** (opzionale)
- Devi riposare per continuare

### Come Riposare

```
┌─────────────────────────────────────────┐
│ METODO: Stare vicino a un letto        │
│                                         │
│ 1. Trova un letto                       │
│ 2. Stai entro 20 blocchi                │
│ 3. Aspetta 60 secondi                   │
│ 4. Fatica ripristinata!                 │
│                                         │
│ ⚠️ Non allontanarti o si interrompe!   │
└─────────────────────────────────────────┘
```

---

## 1.6 SISTEMA TRADE

### Come Commerciare

```
STEP 1: Richiesta
┌─────────────────────────────────────────┐
│ Guarda un giocatore + tieni shift       │
│ + click destro con mano vuota           │
└─────────────────────────────────────────┘
           │
           ▼
STEP 2: Accettazione
┌─────────────────────────────────────────┐
│ L'altro giocatore accetta la richiesta  │
└─────────────────────────────────────────┘
           │
           ▼
STEP 3: Scambio
┌─────────────────────────────────────────┐
│ Entrambi mettono oggetti nella GUI      │
│ Entrambi confermano                     │
│ Conferma finale                         │
│ Scambio completato!                     │
└─────────────────────────────────────────┘
```

### Tassa

- **10%** su valuta (patate di default)
- Mostrata nella GUI prima della conferma

---

## 1.7 SISTEMA BATTAGLIA

### Duello Formale

```
┌─────────────────────────────────────────┐
│ 1. Equipaggia il Guanto di Battaglia    │
│ 2. Click destro su un giocatore         │
│ 3. Lui riceve l'invito                  │
│ 4. Se accetta: BATTAGLIA!               │
│                                         │
│ Durante la battaglia:                   │
│ • Nessun altro può interferire          │
│ • Chi muore perde                       │
│ • Chi vince ottiene gloria              │
└─────────────────────────────────────────┘
```

---

## 1.8 SISTEMA STALLA

### Blocco Stalla

```
┌─────────────────────────────────────────┐
│              STALLA 3x2x3               │
│                                         │
│ Gestisce:                               │
│ • Cibo animali (mangime)                │
│ • Acqua                                 │
│ • Lettiera (paglia)                     │
│ • Rifugio                               │
│ • Produzione (latte, uova)              │
│ • Salute animali                        │
└─────────────────────────────────────────┘
```

### Risorse

| Risorsa | Item | Effetto |
|---------|------|---------|
| Cibo | Mangime | Nutre animali |
| Acqua | Secchio | Idrata animali |
| Lettiera | Paglia Fresca | Comfort |
| Rifugio | Kit Upgrade | Protezione |

### Strumenti

| Item | Uso |
|------|-----|
| Spazzola | Pulisci animali |
| Forcone | Raccogli letame |
| Paglia Fresca | Cambia lettiera |

---

## 1.9 FORGIATURA

### Processo

```
STEP 1: Fondi metallo
┌───────────────────┐
│ Ferro/Oro/Rame    │
│ nella fornace     │
│       ▼           │
│ Metallo Caldo     │
└───────────────────┘

STEP 2: Afferra con Pinza
┌───────────────────┐
│ Click destro con  │
│ Pinza sul tavolo  │
│       ▼           │
│ Pinza + Metallo   │
└───────────────────┘

STEP 3: Martella
┌───────────────────┐
│ Piazza su anvil   │
│ Martella (timing!)│
│       ▼           │
│ Componente forgiato│
└───────────────────┘

STEP 4: Raffredda
┌───────────────────┐
│ Immergi in acqua  │
│ con la Pinza      │
│       ▼           │
│ Componente finito │
└───────────────────┘
```

### Componenti

- **Lama Lunga** - Per spade grandi
- **Lama Corta** - Per pugnali
- **Elsa** - Impugnatura

---

## 1.10 VIDEO SCREEN

### Cos'è

Schermi nel mondo che riproducono video da YouTube/Twitch.

### Comandi (Admin)

```
/videoscreen create <id>              Crea schermo
/videoscreen setcorner1 <id>          Angolo 1
/videoscreen setcorner2 <id>          Angolo 2
/videoscreen seturl <id> <url>        Imposta video
/videoscreen play <id>                Riproduci
/videoscreen pause <id>               Pausa
/videoscreen stop <id>                Ferma
/videoscreen volume <id> <0-100>      Volume
```

---

# PARTE 2: THARIDIA FEATURES

---

## 2.1 SISTEMA CAVE

### Cos'è

Una dimensione speciale con grotte procedurali a 5 livelli.

```
╔═══════════════════════════════════════╗
║            STRUTTURA CAVE             ║
╠═══════════════════════════════════════╣
║                                       ║
║     ████████████████  ← Tier 1 (top)  ║
║       ████████████    ← Tier 2        ║
║         ████████      ← Tier 3        ║
║           ████        ← Tier 4        ║
║            ██         ← Tier 5 (bot)  ║
║                                       ║
║  Piramide invertita: più grande       ║
║  in alto, più piccola in basso        ║
║                                       ║
╚═══════════════════════════════════════╝
```

### Regole

| Regola | Valore |
|--------|--------|
| Tempo massimo | 1 ora |
| Timer | Boss bar visibile |
| Uscita automatica | Dopo 1 ora |
| Uscita manuale | `/thmaster cave exit` |

### Comandi (Admin)

```
/thmaster cave preview [seed]     Anteprima 3D
/thmaster cave generate [seed]    Genera
/thmaster cave teleport           Entra
/thmaster cave exit               Esci
/thmaster cave status             Stato
```

---

## 2.2 SISTEMA DUNGEON

### Cos'è

Istanze dungeon procedurali con sistema a coda.

```
╔═══════════════════════════════════════╗
║           ISTANZE DUNGEON             ║
╠═══════════════════════════════════════╣
║                                       ║
║  [Slot 1] [Slot 2] [Slot 3]          ║
║           [Slot 4] [Slot 5]          ║
║                                       ║
║  • Max 5 giocatori contemporanei      ║
║  • Sistema a coda automatico          ║
║  • Morte = uscita dal dungeon         ║
║  • Drop items bloccato                ║
║                                       ║
╚═══════════════════════════════════════╝
```

### Come Entrare

1. Unisciti alla coda (dal menu Realm)
2. Aspetta uno slot libero
3. Vieni teletrasportato
4. Completa il dungeon o muori

### Regole

| Regola | Effetto |
|--------|---------|
| Morte | Esci e respawni overworld |
| Drop item | Bloccato (anti-exploit) |
| World border | Non puoi uscire dall'area |

### Comandi (Admin)

```
/thmaster dungeon generate        Genera istanze
/thmaster dungeon regenerate      Rigenera
/thmaster dungeon exit            Esci
/thmaster dungeon status          Stato coda
```

---

## 2.3 SISTEMA MERCATO

### Cos'è

Sistema di tracking transazioni commerciali.

### Comandi (Admin)

```
/thmaster mercato registra <venditore> <compratore> <item> <quantità>
/thmaster mercato resoconto [ore]       Report ultime N ore
/thmaster mercato mercati               Lista mercati attivi
/thmaster mercato dettagli <item>       Dettagli item
/thmaster mercato volume                Top 15 per volume
/thmaster mercato statistiche [ore]     Analisi attività
```

### Dati Tracciati

- Prezzo corrente
- Totale transazioni
- Volume scambiato
- Storico recente

---

## 2.4 SISTEMA SCHEMATIC

### Cos'è

Salva e carica strutture nel mondo.

### Comandi (Admin)

```
/thmaster schematic pos1 [pos]        Angolo 1
/thmaster schematic pos2 [pos]        Angolo 2
/thmaster schematic save <nome>       Salva
/thmaster schematic load <nome>       Carica
/thmaster schematic place [pos]       Piazza
/thmaster schematic rotate <nome> <gradi>  Ruota (90/180/270)
/thmaster schematic showarea <nome>   Mostra area (particelle)
/thmaster schematic list              Lista salvate
/thmaster schematic delete <nome>     Elimina
```

---

## 2.5 WORLD BORDER

### Cos'è

Confini invisibili che limitano il movimento.

### Comandi (Admin)

```
/tharidia border create <nome>              Crea (200x200)
/tharidia border set <nome> <x1> <z1> <x2> <z2>  Coordinate
/tharidia border remove <nome>              Rimuovi
/tharidia border list                       Lista
/tharidia border info <nome>                Info
```

---

## 2.6 STRUMENTI ADMIN

### Spawn Entità

```
/thmaster spawn <tipo> <pos> <dimensione> <quantità> [raggio] [attributi]

Esempio:
/thmaster spawn minecraft:zombie "0 100 0" minecraft:overworld 10 32 minecraft:generic.max_health 50
```

### Vanish

```
/thmaster vanish    Diventa invisibile (admin)
```

---

# PARTE 3: THARIDIA TWEAKS

---

## 3.1 SISTEMA ZONE

### Cos'è

Aree protette con flag personalizzabili.

```
╔═══════════════════════════════════════╗
║              ZONA                     ║
╠═══════════════════════════════════════╣
║  Centro: X, Y, Z                      ║
║  Raggio: N blocchi                    ║
║                                       ║
║  FLAG ATTIVI:                         ║
║  ☑ nopvp     ☑ nobreak               ║
║  ☑ nobuild   ☐ nointeract            ║
║  ☐ noexplosion  ☑ nomobspawn         ║
║                                       ║
╚═══════════════════════════════════════╝
```

### Flag Disponibili

| Flag | Effetto |
|------|---------|
| `noxp` | Blocca guadagno XP |
| `nopvp` | Blocca PvP |
| `nobreak` | Blocca rottura blocchi |
| `nobuild` | Blocca piazzamento |
| `nointeract` | Blocca interazione |
| `noexplosion` | Blocca esplosioni |
| `nomobspawn` | Blocca spawn mob |
| `nomobdamage` | Blocca danno da mob |
| `safezone` | Tutti i flag attivi |
| `adminfree` | Admin ignorano flag |

### Comandi (Admin)

```
/tharidia zone create <nome> <raggio>     Crea
/tharidia zone delete <nome>              Elimina
/tharidia zone list                       Lista
/tharidia zone info <nome>                Info
/tharidia zone tp <nome>                  Teletrasporta
/tharidia zone set <nome> <flag> <true/false>  Imposta flag
```

### Musica Zone

```
/tharidia zone music <nome> add <file>    Aggiungi musica
/tharidia zone music <nome> remove <file> Rimuovi
/tharidia zone music <nome> clear         Pulisci
/tharidia zone music <nome> list          Lista
```

---

## 3.2 SISTEMA RAZZE

### Razze Disponibili

```
┌──────────────┬────────────────────────────────┐
│ RAZZA        │ CARATTERISTICHE                │
├──────────────┼────────────────────────────────┤
│ UMANO        │ Bilanciato                     │
│ ELFO         │ Più alto, veloce               │
│ NANO         │ Più basso, resistente          │
│ DRAGONIDE    │ Grande, potente                │
│ ORCO         │ Massiccio, forte               │
└──────────────┴────────────────────────────────┘
```

### Attributi Scalati

- Dimensione base
- Larghezza/Altezza
- Hitbox
- Velocità movimento
- Portata (reach)
- Velocità mining
- Velocità attacco

### Comandi (Admin)

```
/tharidia race set <razza> [player]   Imposta razza
/tharidia race get <player>           Vedi razza
/tharidia race list                   Lista razze
/tharidia race reload                 Ricarica config
```

---

## 3.3 STATISTICHE E LEADERBOARD

### Visualizza Stats

```
/tharidia stats <player>

╔═══════════════════════════════════════╗
║         STATISTICHE PLAYER            ║
╠═══════════════════════════════════════╣
║ Tempo giocato:    12h 34m             ║
║ Uccisioni:        156                 ║
║ Morti:            23                  ║
║ K/D Ratio:        6.78                ║
║ Blocchi minati:   45,231              ║
║ Distanza:         123.4 km            ║
║ Discord:          Collegato ✓         ║
╚═══════════════════════════════════════╝
```

### Leaderboard

```
/tharidia stats leaderboard <categoria>

Categorie:
• playtime   - Tempo giocato
• kills      - Uccisioni
• deaths     - Morti
• mined      - Blocchi minati
• distance   - Distanza percorsa
• level      - Livello classe
```

### Confronta

```
/tharidia stats compare <player1> <player2>
```

---

## 3.4 GESTIONE INVENTARIO

### Backup

```
/tharidia inv backup <player> <etichetta>

Salva:
• Tutti gli item
• Armatura
• Offhand
• Salute
• Fame
```

### Restore

```
/tharidia inv restore <player> <etichetta>
```

### Lista/Elimina

```
/tharidia inv list <player>           Lista backup
/tharidia inv delete <player> <etichetta>  Elimina
```

---

## 3.5 INTEGRAZIONE DISCORD

### Collegamento Account

```
GIOCATORE:
/tharidia discord link <discord_id>

ADMIN:
/tharidia discord admin link <player> <discord_id>
/tharidia discord admin unlink <player>
/tharidia discord admin list
```

### Verifica

```
/tharidia discord check [player]
```

---

## 3.6 BLOCCHI SPAWN/GROWTH/XP

### Mob Spawn Blocker

```
╔═══════════════════════════════════════╗
║        MOB SPAWN BLOCKER              ║
╠═══════════════════════════════════════╣
║ Blocca spawn naturale di:             ║
║ • Mob ostili (zombie, skeleton...)    ║
║ • Mob neutrali (wolf, bee...)         ║
║ • Mob passivi (cow, pig...)           ║
║ • Boss (Wither, Dragon)               ║
║                                       ║
║ ECCEZIONI (configurabili):            ║
║ • Spawn egg ✓                         ║
║ • Comandi /summon ✓                   ║
║ • Spawner ✓                           ║
╚═══════════════════════════════════════╝
```

### Growth Blocker

Blocca crescita di:
- Viti
- Glow berries
- Cave vines
- Sweet berry bush

### XP Blocker

```
/tharidia xp status              Stato
/tharidia xp enable              Blocca XP
/tharidia xp disable             Sblocca XP
/tharidia xp reset [player]      Reset XP player
```

### Sleep Blocker

- I giocatori possono dormire
- MA il tempo NON avanza
- La notte non viene saltata

---

## 3.7 RPG GATES

### Cos'è

Sistema che blocca azioni in base a requisiti.

```
╔═══════════════════════════════════════╗
║            RPG GATES                  ║
╠═══════════════════════════════════════╣
║ Può bloccare:                         ║
║ • Crafting di certi item              ║
║ • Equipaggiamento di armature         ║
║ • Uso di certi blocchi                ║
║ • Interazioni specifiche              ║
║                                       ║
║ Requisiti possibili:                  ║
║ • Livello minimo                      ║
║ • Tag specifici                       ║
║ • Attributi del giocatore             ║
╚═══════════════════════════════════════╝
```

### Feedback

Quando un'azione è bloccata:
- Messaggio personalizzato
- Suono di errore
- Cooldown anti-spam

---

## 3.8 COMANDI UTILITY

### Display Name

```
/tharidia name lookup <player>        Trova nome display
/tharidia name reverse <nome>         Trova chi ha quel nome
/tharidia name set <player> <nome>    Imposta nome
/tharidia name reset <player>         Reset nome
```

### Freeze

```
/tharidia masterFreeze <player>           Blocca movimento
/tharidia masterFreeze unfreeze <player>  Sblocca
```

### Chat Log

```
/tharidia chat showLog    Toggle log login/logout (admin)
```

### Get ID

```
/tharidia getid    Mostra ID di item/blocco/entità puntati
```

---

# APPENDICE: TABELLA COMANDI RAPIDA

## Comandi Giocatore

| Comando | Mod | Descrizione |
|---------|-----|-------------|
| `/claim trust <p>` | Things | Aggiungi trusted |
| `/claim untrust <p>` | Things | Rimuovi trusted |
| `/claim list` | Things | Lista claim |
| `/claim flags` | Things | Mostra flag |
| `/claim containers allow/deny` | Things | Flag contenitori |
| `/claim doors allow/deny` | Things | Flag porte |
| `/tharidia discord link <id>` | Tweaks | Collega Discord |

## Comandi Admin

| Comando | Mod | Descrizione |
|---------|-----|-------------|
| `/thmaster cave generate` | Features | Genera cave |
| `/thmaster cave teleport` | Features | Entra cave |
| `/thmaster dungeon generate` | Features | Genera dungeon |
| `/thmaster schematic save <n>` | Features | Salva struttura |
| `/thmaster spawn <tipo> <pos>` | Features | Spawna entità |
| `/tharidia zone create <n> <r>` | Tweaks | Crea zona |
| `/tharidia zone set <n> <f> <v>` | Tweaks | Imposta flag |
| `/tharidia race set <r> [p]` | Tweaks | Imposta razza |
| `/tharidia stats <p>` | Tweaks | Vedi statistiche |
| `/tharidia inv backup <p> <l>` | Tweaks | Backup inventario |
| `/tharidia xp enable/disable` | Tweaks | Blocca XP |

---

# APPENDICE: CRAFTING

## Tharidia Things

### Contratto di Fiducia
```
┌───┬───┬───┐
│ P │ P │ P │
├───┼───┼───┤
│ P │ I │ P │    P = Carta, I = Sacca Inchiostro
├───┼───┼───┤
│ P │ P │ P │
└───┴───┴───┘
```

### Pinza
```
┌───┬───┬───┐
│ F │   │ F │
├───┼───┼───┤
│   │ F │   │    F = Ferro
├───┼───┼───┤
│ S │   │ S │    S = Stick
└───┴───┴───┘
```

### Dado
```
┌───┬───┬───┐
│ B │ B │ B │
├───┼───┼───┤
│ B │ R │ B │    B = Osso, R = Redstone
├───┼───┼───┤
│ B │ B │ B │
└───┴───┴───┘
```

---

**Fine della Bibbia Tharidia**

*Documento aggiornato: Febbraio 2026*
