

# Changelog 0.5.7

> **I dungeon prendono vita.** Ogni istanza viene ora generata proceduralmente: stanze, corridoi e ponti non saranno mai gli stessi. Scendete al secondo piano, affrontate il boss, e preparatevi — il terzo piano è in costruzione. Nel frattempo, in superficie, un nuovo sistema minerario vi mette in mano un martello per frantumare i minerali colpo dopo colpo, e la stalla vi aspetta con animali da accudire e un intero ciclo di gestione da padroneggiare.

---

## ⚔️ Dungeon Procedurale

**Ogni dungeon è unico.** 
La generazione procedurale costruisce stanze, corridoi e ponti in modo diverso ad ogni nuova istanza. Al momento abbiamo progettato 5 istanza contemporanee. Nessuna run sarà mai uguale alla precedente.

- **Primo piano** — Accessibile da un anello che corre attorno al dungeon
- **Secondo piano** — raggiungibile attraverso portali animati che collegano i livelli
- **Stanza Boss** — Un nemico vi aspetta in fondo. Spawn dedicato, meccaniche di sconfitta e ricompense
- **Coda di gruppo** — Entrate nel dungeon con la vostra squadra. GUI dedicata direttamente dalla schermata del Realm
- **Istanze multiple** in contemporanea con sistema di coda d'attesa
- **Audio immersivo** — Suoni ambientali all'interno del dungeon e feedback sonoro all'entrata e all'uscita

## 🎨 Assets & GUI

- **GUI medievale completamente rinnovata** — Nuovo font Crimson Text, barre di progresso grafiche, pulsanti con texture dedicata per Claims, Dungeon ed Espansione
- Slot inventario con **texture PNG personalizzate**
- Nuove texture e modelli: **Stalla** (con 10 livelli di letame), **Portale Dungeon** animato, **Minerale di Ferro** (5 stadi), **Martello da Frantumazione**
- Nuove texture: **animali baby** (5 specie), **strumenti stalla** (forcone, spazzola, paglia, letame, mangime)
- Nuovi suoni: **martello** (5 varianti), **rottura minerale**, **ambientali dungeon**, **entrata/uscita dungeon**

## 🔧 Fix

- Fix dieta che non si rigenerava correttamente dopo il login
- Fix teletrasporto dal Realm al Dungeon
- Fix blocchi minerali che a volte non rilasciavano il drop
- Fix lettura nomi per l'integrazione Discord
- Fix permessi admin mancanti su alcuni comandi
- Fix errore database nel salvataggio eventi di combattimento
- Fix bioma della dimensione Dungeon

---

# Changelog 0.5.6

## ⛏️ Sistema Minerario — Ore Chunks

**Dimenticate il mining tradizionale.** I minerali ora si frantumano colpo dopo colpo con un martello dedicato. Ogni impatto è visibile, udibile e costa stamina.

- **Blocchi minerali di Ferro** frantumabili attraverso **5 stadi progressivi** — il modello 3D cambia ad ogni colpo
- **Martello da Frantumazione** — Nuovo strumento esclusivo per la raccolta dei minerali
- **Scintille e particelle** ad ogni impatto, **5 varianti di suoni** per il martello e un suono dedicato alla rottura finale
- **Consumo stamina** reale durante la frantumazione — non è solo estetico, è gameplay

### 🔜 In arrivo
- **Blocco minerale di Rame** con modello e texture dedicati
- **Vene Minerarie e Sedimenti** — Nuovi blocchi naturali nel mondo
- **Lavatore** (Washer) — Blocco per processare i minerali frantumati in risorse raffinate, con **drop a probabilità variabile**
- **Retina Filtrante** (Mesh) — Componente per il Lavatore
- **Integrazione JEI** — Tutte le ricette del Lavatore consultabili nel browser ricette

---

# Changelog 0.5.5

## 🐄 Sistema Stalla

**Un intero ciclo di gestione animale.** Mucche, pecore, maiali, capre e galline possono essere allevati nella vostra stalla. Ogni animale ha bisogno di cure, e trascurarli ha conseguenze.

- **Blocco Stalla** con modello 3D multicomponente — fieno, abbeveratoio, latte e letame visibili direttamente sul blocco
- **Benessere animale** — Fame, sete e pulizia da tenere sotto controllo
- **Animali baby** trasportabili come oggetti e piazzabili nella stalla
- **HUD informativo** sopra la stalla per monitorare lo stato degli animali
- **Effetti meteo** — Il tempo atmosferico influenza il benessere
- **Nuovi strumenti e materiali:**
  - Forcone, Spazzola Animali, Paglia Fresca, Paglia Sporca
  - Letame (raccoglibile), Mangime, Kit Potenziamento Riparo

---

# Changelog 0.5.4

## 🎭 Identità del Personaggio

- **Il vostro nome scelto in creazione personaggio ora è ovunque** — In chat, nella tab list, in tutto il gioco. Nessun nickname Minecraft visibile
- I nomi dei giocatori **non appaiono più nei suggerimenti chat** — Solo gli admin possono vederli

---

## 💀 Sistema Fallen/Revive — Potenziato

- **Sfocatura visiva** quando siete caduti — sentite il peso della sconfitta
- **Invulnerabilità** in stato caduto — nessuno può finirvi mentre siete a terra
- L'oggetto revive **si consuma** all'uso
- Al posto di usare una mod per la revive abbiano scritto noi la funzionalità

---

## 🕳️ Grotte Procedurali [WIP]

- **Nuova dimensione grotta** con generazione basata su rumore OpenSimplex2
- Preset configurabili: **caverne ampie, tunnel densi, compatte, ricche di minerali**

---


## 🌍 Mondo

- **Semi rimossi** dal drop di tutte le foglie
- Miglioramenti al **blocco spawn mob** nelle zone protette


---

||@Colono||
