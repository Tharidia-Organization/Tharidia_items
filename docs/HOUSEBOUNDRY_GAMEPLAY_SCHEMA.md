# HOUSEBOUNDRY - SCHEMA DI GAMEPLAY

## OVERVIEW: FILOSOFIA DEL SISTEMA

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         FOCUS: PRODOTTI SECONDARI                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Gli animali esistono per PRODURRE, non per essere uccisi.                 │
│                                                                             │
│  PRODOTTI PRIMARI (carne, pelle, ossa):                                    │
│  └─ Ottenuti SOLO a fine vita produttiva                                   │
│  └─ Reward finale, non obiettivo principale                                │
│                                                                             │
│  PRODOTTI SECONDARI (uova, latte, lana, etc):                              │
│  └─ Core gameplay loop                                                      │
│  └─ Frequenza produzione influenzata da cura dell'animale                  │
│  └─ Finestra produttiva LIMITATA (10 giorni reali)                         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      COMPATIBILITÀ: TUTTI GLI ANIMALI                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  IMPORTANTE: Questo sistema è progettato per funzionare con QUALUNQUE      │
│  animale, non solo quelli vanilla.                                          │
│                                                                             │
│  ANIMALI SUPPORTATI:                                                        │
│  ├─ Vanilla: Cow, Pig, Chicken, Sheep, Goat, etc.                          │
│  ├─ Moddati: Qualunque entità che estende Animal o LivingEntity            │
│  └─ Custom: Animali aggiunti da altri mod o datapack                       │
│                                                                             │
│  ARCHITETTURA:                                                              │
│  ├─ Sistema basato su CAPABILITY/ATTACHMENT, non hardcoded                 │
│  ├─ Stats (comfort, stress, hygiene) attaccati dinamicamente               │
│  ├─ Configurazione prodotti via JSON/datapack per ogni tipo animale        │
│  └─ Default sensati per animali non configurati                            │
│                                                                             │
│  CONFIGURAZIONE PER TIPO ANIMALE (via datapack):                           │
│  ├─ production_interval: tempo tra produzioni                              │
│  ├─ products: lista di item producibili                                    │
│  ├─ baby_duration: durata fase BABY                                        │
│  ├─ productive_duration: durata fase PRODUCTIVE                            │
│  ├─ primary_loot: loot alla macellazione                                   │
│  └─ breeding_item: item richiesto per breeding                             │
│                                                                             │
│  ANIMALI SENZA CONFIGURAZIONE:                                             │
│  └─ Usano valori default, possono comunque beneficiare di                  │
│     comfort/stress/hygiene ma senza produzione secondaria                  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 0. CICLO DI VITA ANIMALE

### 0.1 Fasi del Ciclo di Vita

```
LIFECYCLE PHASES (tempo REALE, non MC ticks):
────────────────────────────────────────────────────────────────────────────────

┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   BABY      │──▶│   ADULT     │───▶│ PRODUCTIVE  │───▶│   BARREN    │
│  (Cucciolo) │    │  (Adulto)   │    │(Produttivo) │    │  (Sterile)  │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
   1 ora         Istantaneo         10 giorni          Permanente

BABY (0-1 ore reali):
├─ Non produce nulla
├─ Consuma risorse (cibo, spazio), consuma più cibo dell'adulto
├─ Può morire se trascurato (disease)
├─ Stats influenzano velocità crescita
└─ Visual: modello piccolo, suoni acuti

ADULT → PRODUCTIVE (transizione automatica):
├─ Avviene automaticamente dopo fase Baby
├─ Inizia il timer di 10 giorni reali
└─ Notifica al player: "[Animale] è ora produttivo!"

PRODUCTIVE (10 giorni reali):
├─ Produce prodotti secondari regolarmente
├─ Frequenza produzione influenzata da stato animale
└─ Visual: modello adulto normale

BARREN (dopo 10 giorni):
├─ NON produce più nulla
├─ Continua a consumare risorse
├─ Mantiene stats (può ancora ammalarsi)
├─ Unica utilità: macellazione per loot primario
├─ Visual: texture leggermente sbiadita, particelle grigie occasionali, modello dell'animale sdraiato a terra
└─ Notifica: "[Animale] non è più produttivo"
```

### 0.2 Timer Produttivo (Real-Time Based)

```
TRACKING DEL TEMPO:
────────────────────────────────────────────────────────────────────────────────

IMPORTANTE: Il timer usa TEMPO REALE, non tick di Minecraft.
├─ Salva timestamp Unix quando animale diventa PRODUCTIVE
├─ Calcola giorni trascorsi: (now - startTimestamp) / 86400000 ms
├─ Il timer CONTINUA anche quando:
│   ├─ Il server è offline
│   ├─ Il chunk non è caricato
│   └─ Il player non è online
└─ Questo previene exploit di "pause" del timer

PERSISTENZA:
├─ productiveStartTime: long (Unix timestamp in ms)
├─ lifecyclePhase: enum (BABY, PRODUCTIVE, BARREN)
└─ Salvati in NBT dell'entità
```

### 0.3 Produzione Durante Fase Produttiva

```
CICLO DI PRODUZIONE:
────────────────────────────────────────────────────────────────────────────────

Ogni animale ha un PRODUCTION_INTERVAL configurabile via datapack.
I valori sono definiti per tipo di entità (ResourceLocation).

ESEMPI VANILLA (configurazione default):
├─ minecraft:chicken → ogni 0.5 ore reali → 1 uovo, max 6/giorno
├─ minecraft:cow     → ogni 1 ora reale   → 1 latte, max 4/giorno
├─ minecraft:sheep   → ogni 1.5 ore reali → 1 lana, max 8/giorno
├─ minecraft:goat    → ogni 1 ora reale   → 1 latte, max 4/giorno

ANIMALI MODDATI (esempi):
├─ modid:custom_cow  → configurato via datapack in data/modid/houseboundry/
├─ alex:elephant     → configurato via datapack
└─ Qualunque entità può essere configurata con lo stesso sistema

ANIMALI NON CONFIGURATI:
└─ Nessuna produzione secondaria, ma beneficiano comunque di stats

MOLTIPLICATORI (basati su Animal State):
├─ GOLD STATE:   Interval x0.7 (produce più spesso)
├─ OK STATE:     Interval x1.0 (normale)
├─ LOW STATE:    Interval x1.3 (produce meno spesso)
├─ CRITICAL:     Interval = ∞ (non produce)
```

### 0.5 Fine Vita Produttiva

```
QUANDO ANIMALE DIVENTA BARREN:
────────────────────────────────────────────────────────────────────────────────

NOTIFICHE:
├─ Particelle grigie sull'animale
├─ Modello dell'animale sdraiato, nel caso di assenza gambe deve esser più basso nel terreno
└─ Suono: verso triste una tantum

OPZIONI PER IL PLAYER:
├─ MACELLAZIONE: Uccidi per loot primario (carne, pelle, ossa)
│
└─ MANTENIMENTO: Tenerlo in vita (scelta del player)
    ├─ Continua a consumare cibo/spazio
    ├─ Può ancora ammalarsi e morire
    └─ Nessun beneficio meccanico (solo roleplay/affetto)

LOOT PRIMARIO ALLA MACELLAZIONE:
├─ Carne: 2-4
├─ Pelle: 0-2
└─ Ossa: 1-2
```

### 0.6 Breeding e Nuovi Cuccioli

```
BREEDING REQUIREMENTS:
────────────────────────────────────────────────────────────────────────────────

PER POTER FARE BREEDING:
├─ Entrambi i genitori in fase PRODUCTIVE
├─ Entrambi in stato OK o GOLD (no LOW/CRITICAL)
├─ Dopo che gli animali si sono riprodotti non possono mai più entrare in breeding
└─ Richiede item specifico (es: wheat per mucche)

RISULTATO:
├─ Spawn cucciolo (fase BABY)
├─ Il cucciolo eredita NESSUNO stat dai genitori
│   └─ Inizia con stats default, sarà il player a curarlo
└─ genitori non possono più riprodursi

NOTA: Animali BARREN non possono fare breeding
```

---

## TRE STAT PRINCIPALI

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           ANIMAL WELLNESS MODEL                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   COMFORT (0-100)          STRESS (0-100)           HYGIENE (0-100)        │
│   "Benessere fisico"       "Paura/Agitazione"       "Pulizia"              │
│   Default: 50              Default: 30              Default: 100           │
│   ↑ = buono                ↑ = CATTIVO              ↑ = buono              │
│                                                                             │
│   Influenzato da:          Influenzato da:          Influenzato da:        │
│   - Bedding quality        - Weather                - Manure level         │
│   - Brushing               - Thunder                - Rain (bonus)         │
│   - Shelter                - Disease                                       │
│   - Weather                - Random events                                 │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 1. COMFORT SYSTEM

### 1.1 Fonti di Comfort

| Azione | Effetto | Cooldown | Note |
|--------|---------|----------|------|
| **Brushing** | +12 comfort, +4 trust | 60 sec per animale | Se animale "dirty": +6 bonus |
| **Fresh Bedding** | +25 comfort | Istantaneo | Resetta freshness a 100 |
| **Shelter Upgrade** | Blocca penalità pioggia | Permanente | Una volta per stalla |

### 1.2 Decay del Comfort

| Condizione | Decay Rate | Priorità |
|------------|------------|----------|
| Base naturale | -0.5/hour | Sempre attivo |
| Pioggia (no shelter) | -2/hour | Cumulativo |
| Caldo/Freddo estremo | -1/hour | Cumulativo |
| Malato/Parassiti | Decay x2 | Moltiplicatore |

### 1.3 Bedding System (Paglia)

```
GESTIONE PAGLIA - PROCESSO A DUE FASI:
────────────────────────────────────────────────────────────────────────

FASE 1: RIMOZIONE PAGLIA VECCHIA
├─ Tool: Rastrello (per ora: Vanilla Hoe)
├─ Azione: Click destro sulla bedding area
├─ Risultato: Rimuove paglia esistente, freshness → 0
├─ Drop: 0-1 Dirty Straw (compostabile)
└─ OBBLIGATORIO prima di mettere paglia fresca

FASE 2: POSIZIONAMENTO PAGLIA FRESCA
├─ Item: Fresh Straw
├─ Azione: Click destro sulla bedding area (vuota)
├─ Risultato: Freshness → 100
└─ RICHIEDE: bedding area vuota (fase 1 completata)

NOTA: Non puoi mettere paglia fresca sopra paglia vecchia!
```

```
FRESHNESS LIFECYCLE:
────────────────────────────────────────────────────────────────────────

100 ████████████████████████████████  Fresh (bright yellow texture)
    ↓ -1/hour
 70 ████████████████████████          Used (tan texture)
    ↓ -1/hour
 40 ████████████████                  Old (brown texture)
    ↓ -1/hour
 10 ████████                          Dirty (dark brown + flies)
    ↓ -1/hour
  0 ████                              Depleted (no bedding benefit)

EFFETTO SU COMFORT:
- Freshness 70-100: Comfort decay ridotto del 50%
- Freshness 40-69:  Decay normale
- Freshness 10-39:  Decay +25%
- Freshness 0-9:    Decay +50%, eventi negativi non mitigati
```

---

## 2. STRESS SYSTEM

### 2.1 Fonti di Stress

| Evento | Effetto | Durata |
|--------|---------|--------|
| **Thunder** | +15 stress istantaneo | Una volta per temporale |
| **Malattia** | +5 stress/hour | Mentre malato |

### 2.2 Riduzione Stress

| Condizione | Effetto | Note |
|------------|---------|------|
| **Safe Area** (shelter) | -2 stress/hour | Attivo se dentro stalla con upgrade |
| **Brushing** | -3 stress | Effetto minimo|
| **Petting** | -0.01 stress | Effetto minimo|
| **Clean Stall** | -10 stress istantaneo | Quando manure rimosso |
| **Decay naturale** | -1 stress/hour | Solo se comfort > 50 |

---

## 3. HYGIENE SYSTEM

### 3.1 Degradazione (basata su Manure)

```
MANURE LEVEL → HYGIENE DECAY
────────────────────────────────────────────────────────────────────────

Manure 0-30:    Nessun decay          "Stalla pulita"
Manure 31-60:   -1 hygiene/hour       "Sporca"
Manure 61-90:   -2 hygiene/hour       "Molto sporca"
Manure 91-100:  -4 hygiene/hour       "Critica"
```

### 3.2 Ripristino Hygiene

| Azione | Effetto | Note |
|--------|---------|------|
| **Rimuovi Manure** | Hygiene smette di degradare | Unico modo per fermare decay |
| **Pioggia** | +15 hygiene una tantum | NON rimuove manure, solo bonus animale |

### 3.3 Disease Trigger

```
HYGIENE → DISEASE CHANCE (check ogni hour)
────────────────────────────────────────────────────────────────────────

Hygiene 100-60:  0% chance            Sicuro
Hygiene 59-40:   2% chance/hour       Rischio basso
Hygiene 39-20:   8% chance/hour       Rischio medio
Hygiene 19-0:    15% chance/hour      Rischio alto

OUTBREAK EVENT: 2% chance giornaliera (indipendente da hygiene)
```

---

## 4. DISEASE SYSTEM

### 4.1 Effetti Malattia

```
MENTRE MALATO:
────────────────────────────────────────────────────────────────────────

├─ Produzione: ZERO (no eggs, milk, wool)
├─ Comfort decay: x2
├─ Stress: +5/hour
├─ Growth: FERMATA
└─ Visual: green bubbles, suoni tristi
```

### 4.2 Cura

| Metodo | Successo | Costo | Note |
|--------|----------|-------|------|
| **Honey Bottle** | 60% per uso | 1 Honey Bottle | Può richiedere 2-3 tentativi |
| **Natural Recovery** | 100% | Tempo + cura | Mantieni hygiene >80 per 2 giorni reali |

### 4.3 Progressione Malattia

```
TIMELINE MALATTIA:
────────────────────────────────────────────────────────────────────────

0 min     ├─ Contrae malattia (green particles)
          │
60 min    ├─ Sintomi peggiorano (movimento rallentato)
          │
100 min   ├─ WARNING: red particles, suono urgente
          │
120 min   ├─ MORTE se non curato
```

---

## 5. WEATHER EFFECTS

### 5.1 Clear Weather
- Nessun effetto, baseline gameplay

### 5.2 Rain

| Effetto | Valore | Condizione |
|---------|--------|------------|
| Comfort penalty | -2/hour | Se NO shelter upgrade |
| Hygiene bonus | +15 una tantum | Quando inizia pioggia (NON rimuove manure) |

### 5.3 Thunderstorm

| Effetto | Valore | Mitigazione |
|---------|--------|-------------|
| Stress spike | +15 istantaneo | Shelter: ridotto a +8 |
| Comfort drop | -5 istantaneo | Shelter: ridotto a -2 |

---

## 6. ANIMAL STATE THRESHOLDS

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         STATO ANIMALE (determinato da stats)                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ★ GOLD STATE                                                               │
│  ├─ Requisiti: Comfort ≥ 70 AND Stress ≤ 20 AND Hygiene ≥ 60               │
│  ├─ Growth Speed: +25%                                                      │
│  ├─ Production Interval: x0.7 (produce più spesso)                         │
│  └─ Visual: cuori, animazione "nuzzle"                                      │
│                                                                             │
│  ● OK STATE                                                                 │
│  ├─ Requisiti: Comfort 40-69 OR Stress 21-49 (e non in altri stati)        │
│  ├─ Growth Speed: normale                                                   │
│  ├─ Production Interval: normale                                            │
│  └─ Visual: idle normale                                                    │
│                                                                             │
│  ▼ LOW STATE                                                                │
│  ├─ Requisiti: Comfort 20-39 OR Stress 50-69                               │
│  ├─ Growth Speed: -25%                                                      │
│  ├─ Production Interval: x1.3 (produce meno spesso)                        │
│  └─ Visual: nuvole grigie, idle nervoso                                     │
│                                                                             │
│  ✖ CRITICAL STATE                                                           │
│  ├─ Requisiti: Comfort < 20 OR Stress ≥ 70 OR Diseased                     │
│  ├─ Growth Speed: FERMATA                                                   │
│  ├─ Production: ZERO                                                        │
│  ├─ Breeding: DISABILITATO                                                  │
│  ├─ Escape chance: +5%/hour se recinto debole                              │
│  └─ Visual: tremore, particelle negative                                    │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 7. RANDOM EVENTS

### 7.1 Eventi Positivi (5% giornaliero)

| Evento | Effetto | Durata |
|--------|---------|--------|
| **Content Animals** | +20 comfort a tutti | Istantaneo |

### 7.2 Eventi Negativi (8% giornaliero)

| Evento | Effetto | Mitigazione |
|--------|---------|-------------|
| **Restless Night** | -15 comfort a tutti | Bedding freshness > 70: evento SALTATO |

---

## 8. NUOVI ITEMS

### 8.1 Crafting Recipes

```
ANIMAL BRUSH
────────────────────────────
Materiali: 1 Stick + 2 Wool
Uso: Click destro su animale
Effetto: +12 comfort, +4 trust
Cooldown: 60 secondi per animale


FRESH STRAW (x4)
────────────────────────────
Materiali: 3 Wheat
Alternativa: Drop da wheat harvest (30% chance, x1-2)
Uso: Click destro su bedding area VUOTA
Effetto: Resetta bedding freshness a 100
NOTA: Prima rimuovere paglia vecchia con rastrello!


RASTRELLO (per ora: Vanilla Hoe)
────────────────────────────
Tool temporaneo: qualunque Hoe vanilla
Uso: Click destro su bedding area con paglia
Effetto: Rimuove paglia esistente, svuota l'area
Drop: 0-1 Dirty Straw (item compostabile)


SHELTER UPGRADE KIT
────────────────────────────
Materiali: 4 Iron Ingot + 6 Planks + 2 Wool
Uso: Click destro su stalla
Effetto: Installa protezione permanente
         - Blocca penalità pioggia
         - Riduce effetti thunder
         - Abilita "safe area" per stress
```

---

## 9. GAMEPLAY LOOP RIASSUNTO

```
DAILY ROUTINE IDEALE:
────────────────────────────────────────────────────────────────────────

MATTINA:
├─ Check bedding freshness (texture visiva)
├─ Se < 40:
│   ├─ 1. Rimuovi paglia vecchia con rastrello (hoe)
│   └─ 2. Metti Fresh Straw sulla bedding area vuota
├─ Brusha animali per boost comfort/trust
└─ Controlla timer produzione (chi sta per produrre?)

DURANTE IL GIORNO:
├─ Raccogli prodotti quando pronti
├─ Monitora manure level
├─ Se piove: verifica shelter upgrade
└─ Controlla lifecycle: chi sta per diventare BARREN?

SERA:
├─ Rimuovi manure con shovel (previene hygiene drop)
├─ Se animale malato: usa Honey Bottle o mantieni hygiene alta
└─ Brushing finale per stress reduction
```

```
CICLO SETTIMANALE:
────────────────────────────────────────────────────────────────────────

├─ Valuta quali animali sono vicini a BARREN (ultimi 2 giorni)
├─ Prepara breeding per rimpiazzarli (serve coppia PRODUCTIVE + OK/GOLD)
├─ Macella animali BARREN quando hai rimpiazzi pronti
└─ Bilancia: non avere troppi BABY (consumano senza produrre)
```

```
CICLO A LUNGO TERMINE (10+ giorni):
────────────────────────────────────────────────────────────────────────

OBIETTIVO: Produzione continua senza interruzioni

STRATEGIA OTTIMALE:
├─ Mantieni sempre 1-2 animali in fase BABY per ricambio
├─ Core herd: animali PRODUCTIVE ben curati (GOLD state)
├─ Macella BARREN appena hai rimpiazzi adulti
└─ Mai avere tutti gli animali che diventano BARREN insieme!

TIMELINE ESEMPIO (3 mucche):
────────────────────────────────────────────────────────────────────────
Giorno 0:   [PRODUCTIVE-10] [PRODUCTIVE-10] [PRODUCTIVE-10]
            └─ Breed una coppia

Giorno 2:   [PRODUCTIVE-8]  [PRODUCTIVE-8]  [PRODUCTIVE-8]  [BABY-nuovo]

Giorno 4:   [PRODUCTIVE-6]  [PRODUCTIVE-6]  [PRODUCTIVE-6]  [PRODUCTIVE-10]
            └─ Nuovo animale ora produce!

Giorno 8:   [PRODUCTIVE-2]  [PRODUCTIVE-2]  [PRODUCTIVE-2]  [PRODUCTIVE-6]
            └─ Warning: primi 3 quasi BARREN, prepara breeding

Giorno 10:  [BARREN]        [BARREN]        [BARREN]        [PRODUCTIVE-4]
            └─ Macella i BARREN, hai ancora 1 che produce
            └─ Hai già fatto breed? Se no, problema!

ERRORE COMUNE: Aspettare troppo per fare breeding
└─ Risultato: periodo senza produzione mentre BABY crescono
```

---

## 10. FEEDBACK VISIVO RIASSUNTO

| Stato | Particelle | Animazione | Audio |
|-------|------------|------------|-------|
| GOLD | Cuori rosa | Nuzzle, rilassato | Versi felici |
| OK | Nessuna | Idle normale | Versi normali |
| LOW | Nuvole grigie | Nervoso, gratta terra | Versi brevi, inquieti |
| CRITICAL | Tremore nero | Tremore | Versi di paura |
| Diseased | Bolle verdi | Lento, sofferente | Tosse, versi tristi |
| Dirty | Mosche | Normale | Buzz mosche |

---

## 11. NUMERI DI RIFERIMENTO

### 11.1 Conversioni Tempo

```
TEMPO MINECRAFT vs TEMPO REALE:
────────────────────────────────────────────────────────────────────────
1 MC day = 24000 ticks = 20 minuti real time
1 MC hour = 1000 ticks = ~50 secondi real time
1 minuto real time = 1200 ticks

TICK INTERVALS SUGGERITI (per stats che usano MC time):
├─ Stat decay check: ogni 1200 ticks (1 min real)
├─ Weather check: ogni 6000 ticks (5 min real)
├─ Disease check: ogni 24000 ticks (1 MC day)
├─ Random events: ogni 24000 ticks (1 MC day)
└─ Visual feedback: ogni 100 ticks (client-side)
```

### 11.2 Tempi Real-Time (Lifecycle)

```
LIFECYCLE TIMING (TEMPO REALE):
────────────────────────────────────────────────────────────────────────
Fase BABY:              2 giorni reali (172,800,000 ms)
Fase PRODUCTIVE:        10 giorni reali (864,000,000 ms)

PRODUCTION INTERVALS (TEMPO REALE):
├─ Gallina (uovo):      8 ore (28,800,000 ms)
├─ Mucca (latte):       12 ore (43,200,000 ms)
├─ Pecora (lana):       24 ore (86,400,000 ms)
└─ Breeding cooldown:   48 ore (172,800,000 ms)
```

---

## 12. RIEPILOGO DATI ANIMALE

```
DATI DA SALVARE PER OGNI ANIMALE (NBT):
────────────────────────────────────────────────────────────────────────

LIFECYCLE:
├─ lifecyclePhase: String (BABY | PRODUCTIVE | BARREN)
├─ birthTimestamp: Long (Unix ms) - quando è nato
├─ productiveStartTimestamp: Long (Unix ms) - quando è diventato productive
└─ lastProductionTimestamp: Long (Unix ms) - ultima volta che ha prodotto

STATS:
├─ comfort: Int (0-100)
├─ stress: Int (0-100)
├─ hygiene: Int (0-100)
└─ diseased: Boolean

DISEASE:
├─ diseaseStartTimestamp: Long (Unix ms, 0 se non malato)
└─ diseaseType: String (per future espansioni)

BREEDING:
└─ lastBreedingTimestamp: Long (Unix ms)

MISC:
├─ lastBrushTimestamp: Long (Unix ms) - per cooldown brush
└─ customName: String (opzionale)
```

---

## 13. CONFIGURAZIONE ANIMALI (DATAPACK)

```
STRUTTURA DATAPACK:
────────────────────────────────────────────────────────────────────────────────

data/
└─ <namespace>/
   └─ houseboundry/
      └─ animals/
         ├─ cow.json           (per minecraft:cow)
         ├─ chicken.json       (per minecraft:chicken)
         └─ custom_animal.json (per modid:custom_animal)
```

### 13.1 Formato JSON Configurazione

```json
{
  "entity": "minecraft:cow",

  "lifecycle": {
    "baby_duration_hours": 1,
    "productive_duration_days": 10
  },

  "production": {
    "interval_hours": 1.0,
    "max_per_day": 4,
    "products": [
      {
        "item": "minecraft:milk_bucket",
        "count": 1
      }
    ]
  },

  "breeding": {
    "item": "minecraft:wheat",
    "one_time_only": true
  },

  "slaughter_loot": {
    "items": [
      { "item": "minecraft:beef", "min": 2, "max": 4 },
      { "item": "minecraft:leather", "min": 0, "max": 2 }
    ]
  }
}
```

### 13.2 Esempio: Aggiungere Animale Moddato

```json
// File: data/alexsmobs/houseboundry/animals/elephant.json
{
  "entity": "alexsmobs:elephant",

  "lifecycle": {
    "baby_duration_hours": 2,
    "productive_duration_days": 15
  },

  "production": {
    "interval_hours": 4.0,
    "max_per_day": 2,
    "products": [
      {
        "item": "alexsmobs:elephant_tusk",
        "count": 1
      }
    ]
  },

  "breeding": {
    "item": "minecraft:melon_slice",
    "one_time_only": true
  },

  "slaughter_loot": {
    "items": [
      { "item": "minecraft:leather", "min": 4, "max": 8 }
    ]
  }
}
```

### 13.3 Animali Senza Produzione

```json
// File: data/minecraft/houseboundry/animals/wolf.json
// Animale che beneficia di stats ma non produce
{
  "entity": "minecraft:wolf",

  "lifecycle": {
    "baby_duration_hours": 1,
    "productive_duration_days": 0
  },

  "production": null,

  "breeding": {
    "item": "minecraft:bone",
    "one_time_only": false
  }
}
```

```
NOTE IMPLEMENTATIVE:
────────────────────────────────────────────────────────────────────────────────

├─ Il sistema carica automaticamente tutti i JSON in houseboundry/animals/
├─ Entità non configurate: stats attivi, no produzione
├─ Priorità: datapack utente > datapack mod > default vanilla
├─ Hot-reload: /reload aggiorna le configurazioni
└─ Validazione: errori loggati se JSON malformato
```
