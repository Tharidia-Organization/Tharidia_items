# GUIDA CLAIM E REALM

## INDICE
1. [Cos'è un Realm](#1-cosè-un-realm)
2. [Cos'è un Claim](#2-cosè-un-claim)
3. [Come creare un Realm](#3-come-creare-un-realm)
4. [Come creare un Claim](#4-come-creare-un-claim)
5. [Protezioni del Claim](#5-protezioni-del-claim)
6. [Flag Granulari](#6-flag-granulari)
7. [Sistema Trust](#7-sistema-trust)
8. [Contratto di Fiducia](#8-contratto-di-fiducia)
9. [Gerarchia del Realm](#9-gerarchia-del-realm)
10. [Scadenza e Decay](#10-scadenza-e-decay)
11. [Comandi](#11-comandi)

---

## 1. COS'È UN REALM

```
┌─────────────────────────────────────────┐
│              REALM (Regno)              │
│                                         │
│  - Area grande controllata da un LORD   │
│  - Contiene più CLAIM al suo interno    │
│  - Ha una gerarchia di ranghi           │
│  - Si espande con le monete             │
│                                         │
└─────────────────────────────────────────┘
```

**Caratteristiche:**
- Un Realm è un'area vasta che può contenere più Claim
- Il proprietario è chiamato LORD
- Ha un sistema di ranghi per i membri
- L'area esterna (outer layer) protegge i raccolti

---

## 2. COS'È UN CLAIM

```
┌─────────────────────────────────────────┐
│              CLAIM (Dominio)            │
│                                         │
│  - Protegge 1 CHUNK (16x16 blocchi)     │
│  - Altezza: -20 / +40 dal blocco claim  │
│  - Solo il proprietario può modificare  │
│  - Può dare accesso ad altri (trust)    │
│                                         │
└─────────────────────────────────────────┘
```

**Area protetta:**
```
        16 blocchi
    ┌───────────────┐
    │               │
    │    CHUNK      │  16 blocchi
    │   PROTETTO    │
    │               │
    └───────────────┘

Altezza: dal livello del claim -20 fino a +40
```

---

## 3. COME CREARE UN REALM

1. Crafta il blocco **Pietro** (Realm Block)
2. Piazzalo dove vuoi il centro del regno
3. Interagisci (click destro) per aprire il menu
4. Deposita patate per ottenere monete
5. Usa le monete per espandere il regno

```
Dimensione Realm:
┌────────────────────────────────┐
│ Livello 1: 3x3 chunk           │
│ Livello 2: 5x5 chunk           │
│ Livello 3: 7x7 chunk           │
│ ...e così via                  │
└────────────────────────────────┘
```

---

## 4. COME CREARE UN CLAIM

1. Crafta il blocco **Claim**
2. Piazzalo nel chunk che vuoi proteggere
3. Il claim si attiva automaticamente
4. Interagisci (click destro) per vedere le info

**Nota:** I claim devono essere dentro un Realm per funzionare correttamente.

---

## 5. PROTEZIONI DEL CLAIM

| Azione | Protetta? |
|--------|-----------|
| Rompere blocchi | ✅ Bloccata |
| Piazzare blocchi | ✅ Bloccata |
| Aprire casse | ✅ Bloccata |
| Usare porte | ✅ Bloccata |
| Usare leve/pulsanti | ✅ Bloccata |
| PvP | ✅ Sempre bloccato |
| Esplosioni | ✅ Bloccate |
| Fuoco | ✅ Bloccato |
| Pistoni dall'esterno | ✅ Bloccati |
| Fluidi dall'esterno | ✅ Bloccati |
| Calpestare farmland | ✅ Bloccato |

---

## 6. FLAG GRANULARI

Puoi permettere alcune azioni a TUTTI i giocatori (non solo ai trusted):

| Flag | Comando | Cosa permette |
|------|---------|---------------|
| Contenitori | `/claim containers allow` | Aprire casse, fornaci, barili |
| Porte | `/claim doors allow` | Usare porte, botole, cancelli |
| Interruttori | `/claim switches allow` | Usare leve, pulsanti, piastre |
| Veicoli | `/claim vehicles allow` | Piazzare barche e minecart |
| Animali | `/claim animals allow` | Sellare, guinzagliare, nutrire |

**Per disabilitare:** sostituisci `allow` con `deny`

```
Esempio:
/claim doors allow    → Tutti possono usare le porte
/claim doors deny     → Solo i trusted possono usare le porte
```

---

## 7. SISTEMA TRUST

I giocatori **trusted** (fidati) possono fare tutto nel tuo claim come se fossero proprietari.

```
OWNER (Proprietario)
    │
    ├── TRUSTED (Fidati)
    │   └── Possono: costruire, rompere, usare tutto
    │
    └── ALTRI GIOCATORI
        └── Possono: solo ciò che i flag permettono
```

**Comandi:**
```
/claim trust <giocatore>     → Aggiungi ai fidati
/claim untrust <giocatore>   → Rimuovi dai fidati
```

---

## 8. CONTRATTO DI FIDUCIA

Un modo alternativo per dare trust senza usare comandi.

**Crafting:**
```
┌───┬───┬───┐
│ P │ P │ P │
├───┼───┼───┤
│ P │ I │ P │    P = Carta
├───┼───┼───┤    I = Sacca d'Inchiostro
│ P │ P │ P │
└───┴───┴───┘
```

**Come usarlo:**

```
STEP 1: Lega il contratto
┌─────────────────────────────────┐
│ Contratto Vuoto                 │
│         +                       │
│ Click destro sul TUO Claim      │
│         =                       │
│ Contratto Legato (brillante)    │
└─────────────────────────────────┘

STEP 2: Dai il contratto
┌─────────────────────────────────┐
│ Passa il contratto legato       │
│ ad un altro giocatore           │
└─────────────────────────────────┘

STEP 3: Attivazione
┌─────────────────────────────────┐
│ L'altro giocatore usa il        │
│ contratto (click destro in aria)│
│         =                       │
│ Diventa TRUSTED nel tuo claim!  │
│ (contratto consumato)           │
└─────────────────────────────────┘
```

---

## 9. GERARCHIA DEL REALM

All'interno di un Realm esistono 5 ranghi:

```
┌─────────────────────────────────────────┐
│  RANGO        │ COLORE PARTICELLA       │
├───────────────┼─────────────────────────┤
│  LORD         │ Rosso cremisi           │
│  CONSIGLIERE  │ Oro antico              │
│  GUARDIA      │ Giallo pallido          │
│  MILIZIANO    │ Verde foresta           │
│  COLONO       │ Grigio pietra           │
└───────────────┴─────────────────────────┘
```

**Indicatore visivo:**
- Ogni ~3.5 secondi appare una particella colorata sulla spalla sinistra
- Il colore indica il rango nel realm

**Gestione ranghi:**
- Solo il LORD può cambiare i ranghi
- Menu accessibile dal blocco Pietro (Realm)

---

## 10. SCADENZA E DECAY

I claim hanno una scadenza e un sistema di decadimento:

```
TIMELINE:
═══════════════════════════════════════════════════════════════►

CLAIM ATTIVO          SCADUTO           DECAY
│                     │                 │
│  Protezione ON      │  Protezione OFF │  Blocchi rimossi
│                     │  (3 giorni)     │  (dopo 14 giorni)
│                     │                 │
└─────────────────────┴─────────────────┴─────────────────────►
```

**Fasi:**

| Fase | Durata | Cosa succede |
|------|--------|--------------|
| Attivo | Fino a scadenza | Protezione completa |
| Grace Period | 3 giorni | Protezione DISATTIVATA, claim visibile |
| Decay Period | 14 giorni | Protezione OFF, poi blocchi rimossi |

**Blocchi rimossi dal Decay:**
- Casse, barili, shulker box
- Fornaci, tavoli da lavoro
- Letti, tavoli incantamento
- Anvil, beacon, frame
- E altri blocchi funzionali

**Blocchi NON rimossi:**
- Pietra, legno, mattoni
- Vetro, lana
- E altri blocchi da costruzione

---

## 11. COMANDI

### Comandi Claim

| Comando | Descrizione |
|---------|-------------|
| `/claim trust <player>` | Aggiungi giocatore ai fidati |
| `/claim untrust <player>` | Rimuovi giocatore dai fidati |
| `/claim list` | Lista dei tuoi claim |
| `/claim info` | Info sul claim attuale |
| `/claim flags` | Mostra i flag attuali |

### Comandi Flag

| Comando | Descrizione |
|---------|-------------|
| `/claim containers <allow/deny>` | Accesso contenitori |
| `/claim doors <allow/deny>` | Uso porte |
| `/claim switches <allow/deny>` | Uso interruttori |
| `/claim vehicles <allow/deny>` | Piazzamento veicoli |
| `/claim animals <allow/deny>` | Interazione animali |

---

## RIASSUNTO RAPIDO

```
╔═══════════════════════════════════════════════════════════╗
║                    REALM vs CLAIM                         ║
╠═══════════════════════════════════════════════════════════╣
║  REALM                    │  CLAIM                        ║
║  • Area grande            │  • 1 chunk (16x16)            ║
║  • Contiene più claim     │  • Protezione completa        ║
║  • Sistema ranghi         │  • Sistema trust              ║
║  • Espandibile            │  • Flag granulari             ║
║  • 1 LORD proprietario    │  • Scade e decade             ║
╚═══════════════════════════════════════════════════════════╝
```

```
╔═══════════════════════════════════════════════════════════╗
║                 DARE ACCESSO AD ALTRI                     ║
╠═══════════════════════════════════════════════════════════╣
║  METODO 1: Comando                                        ║
║  └─ /claim trust <nome>                                   ║
║                                                           ║
║  METODO 2: Contratto di Fiducia                          ║
║  └─ Crafta → Lega al claim → Dai al giocatore → Usa      ║
║                                                           ║
║  METODO 3: Flag (accesso limitato a tutti)               ║
║  └─ /claim doors allow (esempio)                          ║
╚═══════════════════════════════════════════════════════════╝
```
