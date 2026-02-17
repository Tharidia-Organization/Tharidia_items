

# Changelog 0.5.7

> **I dungeon prendono vita.** Ogni istanza viene ora generata proceduralmente: stanze, corridoi e ponti non saranno mai gli stessi. Scendete al secondo piano, affrontate il boss, e preparatevi — il terzo piano è in costruzione. Nel frattempo, in superficie, un nuovo sistema minerario vi mette in mano un martello per frantumare i minerali colpo dopo colpo, e la stalla vi aspetta con animali da accudire e un intero ciclo di gestione da padroneggiare.



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



# Changelog 0.5.4

## 🎭 Identità del Personaggio

- **Il vostro nome scelto in creazione personaggio ora è ovunque** — In chat, nella tab list, in tutto il gioco. Nessun nickname Minecraft visibile
- I nomi dei giocatori **non appaiono più nei suggerimenti chat** — Solo gli admin possono vederli



## 💀 Sistema Fallen/Revive — Potenziato

- **Sfocatura visiva** quando siete caduti — sentite il peso della sconfitta
- **Invulnerabilità** in stato caduto — nessuno può finirvi mentre siete a terra
- L'oggetto revive **si consuma** all'uso
- Al posto di usare una mod per la revive abbiano scritto noi la funzionalità



## 🕳️ Grotte Procedurali [WIP]

- **Nuova dimensione grotta** con generazione basata su rumore OpenSimplex2
- Preset configurabili: **caverne ampie, tunnel densi, compatte, ricche di minerali**




## 🌍 Mondo

- **Semi rimossi** dal drop di tutte le foglie
- Miglioramenti al **blocco spawn mob** nelle zone protette




# Changelog 0.5.8



## ⚔️ Dungeon — Potenziato

>**Il dungeon diventa un'esperienza completa.** Mob, bottino e boss sono ora gestiti con un sistema avanzato pensato per giocare un gruppo.

- **Mob Manager** — Spawn dei mob per giocatore, non per tile. (I mob spawneranno in funzione della posizione, numero e ad ingresso dei giocatori nelle varie sezioni)
- **Loot nelle casse** — Sistema data-driven con tabelle di loot per piano e boss
- **Boss reworkato** — Countdown di spawn, boss bar gialla, cassa reward, exit countdown e pulizia stanza automatica
- **Attivazione tile** — Trigger sulle porte per attivare mob e loot della stanza
- **Scaling difficoltà** — Configurazione della difficoltà in base al numero di giocatori
- **Drop blocker** — Impedisce ai giocatori di droppare oggetti all'interno del dungeon
- **Fix multiplayer** — Gestione corretta del loot al rejoin nella stessa sessione
- **Localizzazione completa** — Tutti i messaggi dungeon tradotti in italiano e inglese

## 🕳️ Grotte — Potenziate

- **Generazione spawn** — Nuovo sistema di spawn point nelle grotte con supporto a punti multipli basati su tag
- **Vegetazione procedurale** — Generazione automatica di piante e vegetazione nelle grotte
- **Nuovi materiali** — Blocchi di layout e materiali aggiuntivi per le caverne
- **Protezione blocchi** — Handler dedicato per impedire la distruzione di blocchi strutturali
- **Mob encounter manager** — Sistema di incontri con mob nelle grotte

|| A breve un leak di entrambi, sono già completamente funzionanti @Colono ||
# Changelog 0.5.9

## 🔨 Fornace da Fabbro

> **Dal minerale grezzo all'oggetto forgiato.** La fornace da fabbro è ora un sistema completo con componenti modulari, fusione nel crogiolo grande e battitura sull'incudine.

- **Modulo Crogiolo grande** — Fondere i metalli e colarli negli stampi. Il renderer mostra la quantità di metallo fuso in tempo reale
- **Minigame di battitura** — Un nuovo minigame sull'incudine per forgiare oggetti colpo dopo colpo
- **Componenti modulari** — Mantice, camino, aspiratore, pinza e porta, ognuno con modello 3D e renderer dedicato
- **Martello da fabbro** — Nuovo strumento con texture dedicata per la lavorazione sull'incudine
- **Frammenti metallici e cenere** — Nuovi materiali di scarto e lavorazione
- **Modello GeckoLib animato** — La fornace prende vita con animazioni di fusione e fiamme
- **Dettagli della forgiatura** — L'accensione della fornace, il consumo di carburante e di cristalli è animato e creato tutto in modelli 3D
- **GUI** — La fornace non avrà gui, è tutta basata su interazioni in gioco, modelli ed effetti visivi, come render liquidi dinamici, oggetti e particelle
- **Casting** — Vi sono 2 modi per castare, o farlo direttamente prendendo il crogiolo piccolo pieno di liquido incandescente e versarlo nel cast oppure accumularlo nel crogiolo più grande per poter caster più lingotti in un colpo solo
- **Deprecazione** — Ogni fase in cui è presente liquido incandescente, a meno che non sia su una fonte di calore, potrebbe solidificarsi costringendovi a riciclare lo scarto creato

|| Attenti alla cenere 😈, e all'implementazione manca solo il cast multiplo simultaneo (il modello 3d), spiegazioni e leak arriveranno @Colono ||
# Changelog 0.6.0
> ## **Giornata magica, perchè oggi vi presentiamo 4 nuovi blocchi**
## 🧪 Tini da Tintura

> **Create i vostri colori.** Un nuovo blocco per mescolare pigmenti e tingere abiti e materiali con tonalità uniche.

- **Blocco Tini da Tintura** — Modello 3D di una vasca (WIP)
- **Miscelazione colori** — Il renderer mostra dinamicamente il colore del liquido all'interno
- **Integrazione vestiario** — I colori creati possono essere applicati a vestiti e materiali
- **Creazione colore** — Permette di miscelare con precisione i colori lanciandoli dentro ottenendo colorazioni uniche

## 🚿 Lavatore — Setaccio, Cisterna e Lavello

> **Tre blocchi, un sistema.** Il lavatore si evolve in una catena di lavorazione completa per processare i minerali scavati in cava.

- **Setaccio (Sieve)** — Blocco con toggle attivo/inattivo, rendering dell'acqua e del blocco in lavorazione
- **Cisterna (Tank)** — Sistema a cascata con rendering dell'acqua e modello con texture dedicata
- **Lavello (Sink)** — Rendering inventario, bounding box personalizzata, otterrete qui gli **ore chunk** che verranno poi lavorati smartellando ||o polverizzando|| (Meccanica che vi verrà mostrata) 
- **Rendering acqua** su tutti e tre i blocchi con effetti visivi dedicati
- **Residui** — Sotto il setaccio è presente un contenitore per raccogliere tutti i residui di materiale, che in gioco si traduce nella produzione collaterale di SABBIA, GHIAIA, COSE e cose più utili, fatto per salvaguardare il mondo **[WIP]**
||@Colono||
# Un buongiorno a tutti con il Changelog 0.6.1

## ⚙️ Polverizzatore
>**Macinate tutto.** Un nuovo blocco da lavorazione che riduce materiali grezzi in risorse più fini, con animazioni e feedback sonoro. Che entra al seguito del minatore

- **Blocco Polverizzatore** — Macchinario con modello animato studiato per avere un comodo input superiore e l'output renderizzato sul blocco (No Gui)
- **Grinder** — Componente con modello 3D dedicato da inserire nel polverizzatore che funge da macina
- **Animazione di lavorazione** — Il polverizzatore si attiva visivamente durante la macinazione
- **Utilizzo** — Il polverizzatore è azionato manualmente tramite una manovella [WIP] animata anch'essa
- **Suono di lavorazione** — Audio dedicato che accompagna il processo di polverizzazione
- **Integrazione JEI** — Categoria ricette dedicata consultabile nel browser ricette

# Changelog 0.6.2

## 💎 Cristalli?

- **5 tipologie di cristalli** con modelli 3D dedicati e stadi di crescita progressivi
- **Texture** — Nuove texture ad alta risoluzione per tutti i cristalli puri [IN REWORK]
- **Blocchi piazzabili** —  tutti i cristalli grezzi sono posizionabili nel mondo per poter esser lavorati
- **Uso** — I cristalli non avranno solo una parte fondamentale nell'estetica per i giocatori, ma altrettanto importante nel mondo
  - I cristalli sono essenziali per alcune lavorazioni, come il corretto funzionamento delle fucine del fabbro, come se catalizzassero l'energia
  - Sono essenziali soprattutto a chiunque voglia usare una station, che sia una semplice crafting, fino al più complesso dei tavoli da lavoro [FUTURO CHANGELOG]

## ⛏️ Ore Chunks — Espansione

- **Chunk di Carbone e Stagno** — Due nuovi minerali frantumabili con modelli a 5 stadi
- **Vene e Sedimenti** — Ora ci sono vene di molti materiali nella cava, come argilla e materiali utili a costruire
- **Tool** — Il martello per lavorare i chunk è stato ricreato, modello e texture

# Changelog 0.6.3

> Non ci è possibile dare una durabilità a tutte le station crafting moddate e non, quindi ci siamo ingegnati, per dar un pizzico di dinamica in più al Manovale (Tra le tante cose che può fare), ora avrà l'onere e l'ONORE di portare in ogni villaggio un po' della sua manovalanza. Così da potersi assicurare che tutte le station continuino a funzionare a dovere e non si logorino.

## 🔮 Station Crystal

- **Nuovo blocco di supporto** — Questo è un blocco creato da un insieme di cristalli e una materia instable
- **Scopo** — Implementata meccanica core, è possibile piazzare le station solo sopra di esso
- **Riparazione o Distruzione** — Il cristallo allo scadere di un timer interno distrugge i blocchi sopra di sé, se non viene riparato prima
- **Riparazione** — Il blocco è riparabile tramite speciali attrezzi soltanto tramite una classe giocante specifica, bisogna fare la manutenzione alle proprie attrezzature
- **Particelle d'allarme** quando il tempo rimanente scende sotto il 25%, il giocatore viene notificato
- **Sostituzione** — è possibile rompere la station posizionata sopra per spostarla, ma non rompere il cristallo quando una station è sopra di lui, pena il rischio di perdere la propria amata attrezzatura
- **Style** — Texture ancora in fase di sviluppo

# Changelog 0.6.4

## 🛡️ Sotto-Armatura & Sistema Equip

**Un nuovo livello sotto l'armatura.** Indossate strati aggiuntivi sotto la corazza, con attributi e rendering dedicati.

- **Slot sotto-armatura** — Elmo, corazza, gambali e stivali con tag dedicati
- **Attributi custom** — Ogni pezzo sotto-armatura applica bonus specifici al personaggio
- **Renderer custom** — Layer di rendering aggiuntivo per visualizzare la sotto-armatura sul modello
- **GUI armatura rinnovata** — Nuova schermata con slot vanilla e sotto-armatura unificati

# Changelog 0.6.5

## 💀 Fallen/Revive — Potenziato

- **Barra di progresso** — Overlay HUD sul giocatore che sta rianimando un compagno
- **Rinuncia (Give Up)** — I giocatori caduti possono arrendersi con UI e gestione network dedicata
- **Sincronizzazione stato** — Lo stato di revive è sincronizzato a tutti i giocatori in tempo reale
- **Tracciamento tempo caduto** — Il sistema tiene traccia di quanto tempo un giocatore è rimasto a terra
- **Titolo caduto** — Visualizzazione a schermo del titolo quando si cade

## 💰 Mercato — Potenziato

- **Sistema di tassazione** — Ogni transazione è tassata in funzione del valore della merce
- **Bronzino minimo garantito** — Ogni transazione garantisce almeno 1 bronzino al venditore
- **Tracciamento completo** — Tipo di blocco, NBT e dettagli registrati per ogni scambio
- **Statistiche giocatore** — Sconti progressivi basati sul volume di vendita
- **Tesoreria** — Contenitore centralizzato per tutte le tasse raccolte
- Fix bug di duplicazione, sparizione e gestione logout

# Changelog 0.6.6


## 🎭 Creazione Personaggio — Fix Maggiore

- **Validazione nome server-side** — Nuovo pacchetto server-client per verificare l'accettazione del nome scelto
- **Flusso di creazione reworkato** — Fix al flusso completo di creazione personaggio con gestione corretta delle risposte

## 🐄 Stalla — GUI Reworkata

- **HUD rinnovato** — La GUI della stalla è stata completamente riscritta con rendering delle quantità migliorato

## 🌍 Claim & Realm

- **Nuovi flag di protezione** — Logica aggiornata per i permessi all'interno di claim e realm
- **Marker status giocatore** — Nuovo indicatore visivo dello stato del giocatore all'interno del realm

## 🎨 Assets & Texture

- Nuove texture: **pinza**, **frammenti metallici**, **cenere**, **martello da fabbro**, **cristalli puri** (5 varianti)
- Nuovi modelli: **tini da tintura**, **setaccio**, **vasca**, **lavandino**, **porta fornace**, **componenti fornace**, **polverizzatore**, **grinder**
- Nuovi suoni: **polverizzatore** in lavorazione
- Texture temporanee per chunk e cristalli in lavorazione

## 🔧 Fix

- Fix singleplayer che non salvava il mondo correttamente
- Fix eccezione crash allo stop del server
- Fix rottura sink che distruggeva tutti i lavandini vicini
- Fix posizione e rendering del lavandino
- Fix interazione con blocco dummy del sink
- Fix inizializzazione fornace da fabbro in singleplayer

