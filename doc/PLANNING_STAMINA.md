Progettazione Tecnica Dettagliata: Sistema di Stamina per Combat
1. Architettura Generale del Sistema
1.1 Componenti Principali
StaminaSystem (Core)
├── CombatStateTracker
│   ├── EventListener (damage dealt/received/targeting)
│   └── CombatTimer
├── StaminaManager
│   ├── StaminaPool (current/max values)
│   ├── RegenerationController
│   └── ConsumptionCalculator
├── ActionValidator
│   └── ThresholdChecker
└── ProgressionIntegration
    ├── SkillModifiers
    └── TierSystem
1.2 Data Structures Fondamentali
StaminaComponent (attachato al player entity)

currentStamina: float - valore corrente
maxStamina: float - valore massimo (modificabile da progressione)
isRegenerating: boolean - flag stato rigenerazione
isInCombat: boolean - flag stato combat
lastCombatActivity: timestamp - ultimo evento combat
regenerationRate: float - velocità rigenerazione (modificabile)
modifiers: List<StaminaModifier> - modificatori attivi da skill/buff
CombatState

combatTimeout: float - durata timer (configurabile, es. 5-10 secondi)
lastDamageDealt: timestamp
lastDamageReceived: timestamp
lastTargetingUpdate: timestamp
currentTarget: Entity? - riferimento al target corrente
StaminaModifier (per skill tree)

modifierType: enum - (MAX_STAMINA, REGEN_RATE, CONSUMPTION_MULTIPLIER, etc.)
value: float - valore del modificatore
isPercentage: boolean - se è percentuale o valore assoluto
source: string - identificatore skill/buff
2. Sistema di Tracking dello Stato Combat
2.1 Event-Driven Architecture
Eventi da intercettare:

OnPlayerDealDamage
OnPlayerReceiveDamage
OnPlayerTargetEntity
OnPlayerClearTarget
Logica CombatStateTracker:

Pseudocodice:

function onCombatEvent(eventType):
    currentTime = getCurrentTime()
    combatState.updateLastActivity(eventType, currentTime)
    staminaComponent.isInCombat = true
    resetCombatTimer()

function tick(deltaTime):
    if isInCombat:
        timeSinceLastActivity = getCurrentTime() - combatState.getLatestActivity()
        if timeSinceLastActivity >= combatTimeout:
            exitCombatState()

function exitCombatState():
    staminaComponent.isInCombat = false
    onCombatExit() // trigger per eventuali effetti
2.2 Sistema di Targeting
Requisiti:

Tracking del target corrente del player
Refresh automatico del timer combat quando il target è attivo
Gestione perdita target (morte, distanza, cambio dimensione)
Implementazione:

Hook nel sistema di targeting esistente
Tick periodico (ogni 0.5-1 secondo) per verificare validità target
Se target valido → refresh combat timer
3. Sistema di Consumo Stamina
3.1 Curve di Consumo - Integrazione con Sistema Peso Esistente
Assunzione: La mod già implementa un sistema di peso per armi e armature. Il sistema stamina deve leggere questi valori esistenti, non crearli.

Formula Base per Attacchi:

staminaCost = baseConsumption * f(weaponWeight) * playerModifiers

dove:
- weaponWeight = valore letto dalla proprietà peso dell'arma equipaggiata
- f(x) è la curva di consumo configurabile
Implementazione tecnica:

Pseudocodice:

function calculateAttackStaminaCost(player, attackType):
    weapon = player.getMainHandItem()
    weaponWeight = weapon.getWeight()  // Legge peso esistente dalla mod
    
    baseCost = getBaseAttackCost(attackType)  // LIGHT, HEAVY, SPECIAL
    curveCost = applyCurve(weaponWeight, attackCurveConfig)
    modifiers = getPlayerModifiers(player, ATTACK_COST_PERCENT)
    
    return baseCost * curveCost * modifiers
Requisiti per integrazione:

Identificare metodo/proprietà che restituisce peso arma (es. Item.getWeight(), ItemStack.getTag("weight"), etc.)
Gestire armi senza peso definito (fallback a valore default)
Supportare diverse unità di misura se necessario (kg, libbre, valore astratto)
Parametri configurabili per weapon (se peso non sufficiente):

staminaCostOverride: float - override completo del costo (opzionale)
weightMultiplier: float - moltiplicatore del peso per bilanciamento (default 1.0)
attackType: enum - (LIGHT, HEAVY, SPECIAL) con moltiplicatori diversi
3.2 Sistema per Arco (Tensione)
Meccanica:

Consumo stamina = f(tensionTime) dove:
- tensionTime: tempo in secondi di tensione arco
- threshold: tempo minimo prima che inizi consumo (es. 0.5s)
- consumptionRate: stamina/secondo dopo threshold

Formula proposta:
if tensionTime < threshold:
    cost = 0
else:
    cost = baseCost + (tensionTime - threshold) * consumptionRate * curve(tensionTime)

curve(t) = min(1.0, t / maxTensionTime) // cap al massimo
Parametri bilanciamento:

tensionThreshold: 0.3-0.5s - grazia iniziale
baseCost: float - costo minimo al rilascio
consumptionRate: float/s - consumo per secondo di tensione
maxTensionTime: float - tempo massimo tensione (dopo questo, costo non aumenta)
Implementazione tecnica:

Timer locale quando player inizia a tendere
Tick ogni frame per calcolare consumo
Consumo applicato al rilascio della freccia
Feedback visivo (shader/UI) per indicare consumo crescente
3.3 Sistema per Balestra
Meccanica semplice:

Consumo stamina solo alla ricarica
Costo fisso o basato su peso balestra
Nessun consumo al rilascio
3.4 Roll e Movimento
Roll - Integrazione con Peso Armatura:

rollCost = baseRollCost * f(totalArmorWeight) * modifiers

dove:
- totalArmorWeight = somma peso di tutti i pezzi armatura equipaggiati
- f(x) è la curva di consumo configurabile (simile ad attacchi)
Implementazione tecnica:

Pseudocodice:

function calculateRollStaminaCost(player):
    totalArmorWeight = 0.0
    
    for armorSlot in [HEAD, CHEST, LEGS, FEET]:
        armorPiece = player.getArmorInSlot(armorSlot)
        if armorPiece != null:
            totalArmorWeight += armorPiece.getWeight()  // Legge peso esistente
    
    baseCost = config.baseRollCost
    curveCost = applyCurve(totalArmorWeight, rollCurveConfig)
    modifiers = getPlayerModifiers(player, ROLL_COST_PERCENT)
    
    return baseCost * curveCost * modifiers
Gestione edge cases:

Armatura senza peso definito → fallback a peso default per tipo (leather=1, iron=2, diamond=3, etc.)
Slot vuoti → peso 0
Armature custom → leggere peso da NBT/proprietà
Curve suggerite:

Armatura leggera (peso < 5): roll spam possibile (3-4 roll)
Armatura media (peso 5-10): 2-3 roll
Armatura pesante (peso > 10): 1-2 roll
3.5 Difesa (Scudo)
Scudo alzato:

NO consumo attivo
Blocco rigenerazione mentre attivo
Flag isBlockingRegeneration su StaminaComponent
Rigenerazione riprende dopo regenDelayAfterBlock (es. 0.5-1s)
Parry:

NO consumo stamina
Meccanica skill-based pura
Eventuale bonus: parry perfetto → burst rigenerazione stamina (opzionale)
4. Sistema di Rigenerazione
4.1 Logica Base
Pseudocodice:

function tick(deltaTime):
    if !isInCombat:
        return // NO rigenerazione fuori combat
    
    if isBlockingRegeneration:
        return
    
    if currentStamina >= maxStamina:
        return
    
    // Calcola rigenerazione
    regenAmount = baseRegenRate * deltaTime * modifiers
    currentStamina = min(currentStamina + regenAmount, maxStamina)
    
    onStaminaChanged()
4.2 Parametri Configurabili
Base:

baseRegenRate: float/s - rigenerazione base (es. 10-20% max stamina/secondo)
regenDelayAfterConsumption: float - delay dopo consumo (es. 0.5-1s)
regenDelayAfterBlock: float - delay dopo aver abbassato scudo
Modificatori da Skill:

Aumento percentuale regen rate
Riduzione delay
Rigenerazione burst (instant recovery parziale)
Rigenerazione su eventi (es. kill, parry perfetto)
4.3 Rigenerazione Attiva (Skill-Based)
Skill possibili:

Meditation: player fermo + input → regen accelerata
Second Wind: cooldown → instant recovery X%
Combat Focus: regen continua anche durante block (passive)
Adrenaline Rush: regen aumentata sotto Y% HP
Implementazione:

Sistema di buff temporanei che modificano regenerationRate
Stack di modificatori applicati in ordine
Calcolo finale: effectiveRegen = baseRegen * Π(multipliers) + Σ(additives)
5. Sistema di Validazione Azioni
5.1 ActionValidator Component
Responsabilità:

Verificare se player ha stamina sufficiente per azione
Bloccare azioni se stamina insufficiente
Fornire feedback visivo/audio
Interfaccia:

function canPerformAction(actionType, actionData) -> boolean:
    requiredStamina = calculateStaminaCost(actionType, actionData)
    return currentStamina >= requiredStamina

function tryConsumeStamina(actionType, actionData) -> boolean:
    if canPerformAction(actionType, actionData):
        cost = calculateStaminaCost(actionType, actionData)
        currentStamina -= cost
        onStaminaConsumed(cost)
        return true
    else:
        onInsufficientStamina(actionType)
        return false
5.2 Integrazione con Sistema di Combat
Hook nei sistemi esistenti:

Attack System: pre-check stamina prima di swing
Movement System: pre-check per roll, disabilita sprint
Bow System: check continuo durante tensione
Shield System: nessun check, ma blocca regen
Gestione fallimenti:

Azione non eseguita
Feedback audio (suono "stamina vuota")
Feedback visivo (flash rosso UI, shake camera leggero)
Animazione "affaticato" se stamina a 0
6. - Integrazione con Sistema di Tag (Skill Tree Esterno)
6.1 Architettura Tag-Based per Modificatori
Principio: Il sistema stamina deve essere agnostico rispetto alla provenienza dei modificatori. Le skill non sono gestite internamente ma tramite tag della mod esterna.

StaminaModifier Structure (invariata ma tag-driven):

StaminaModifier:
- modifierType: enum (MAX_STAMINA, REGEN_RATE, CONSUMPTION_MULTIPLIER, etc.)
- value: float
- isPercentage: boolean
- source: string (tag identifier dalla mod esterna)
- priority: int (per ordine applicazione)
6.2 Tag Listener System
Componente nuovo: TagModifierBridge

Responsabilità:
- Ascoltare eventi di aggiunta/rimozione tag dal player
- Mappare tag specifici a StaminaModifier
- Applicare/rimuovere modificatori automaticamente
Implementazione:

Pseudocodice:

function onPlayerTagAdded(player, tag):
    if isStaminaRelatedTag(tag):
        modifier = createModifierFromTag(tag)
        staminaSystem.addModifier(player, modifier)

function onPlayerTagRemoved(player, tag):
    if isStaminaRelatedTag(tag):
        staminaSystem.removeModifierBySource(player, tag.identifier)
6.3 Tag Mapping Configuration
File: stamina_tag_mappings.json

json
{
  "tagMappings": [
    {
      "tagId": "skill:iron_lungs_1",
      "modifierType": "MAX_STAMINA_PERCENT",
      "value": 10.0,
      "description": "Iron Lungs Tier 1"
    },
    {
      "tagId": "skill:quick_recovery_2",
      "modifierType": "REGEN_RATE_PERCENT",
      "value": 30.0,
      "description": "Quick Recovery Tier 2"
    },
    {
      "tagId": "skill:efficient_movement_3",
      "modifierType": "ROLL_COST_PERCENT",
      "value": -30.0,
      "description": "Efficient Movement Tier 3"
    },
    {
      "tagId": "skill:weapon_mastery_sword",
      "modifierType": "ATTACK_COST_PERCENT",
      "value": -15.0,
      "weaponFilter": "sword",
      "description": "Sword Mastery"
    }
  ],
  "activeSkills": [
    {
      "tagId": "skill:second_wind",
      "abilityType": "INSTANT_RECOVERY",
      "value": 50.0,
      "cooldown": 60.0
    },
    {
      "tagId": "skill:focus",
      "abilityType": "CONSUMPTION_REDUCTION_BUFF",
      "value": 50.0,
      "duration": 10.0,
      "cooldown": 30.0
    }
  ]
}
6.4 Modifier Types Estesi
Enum ModifierType completo:

MAX_STAMINA_FLAT          // +X stamina
MAX_STAMINA_PERCENT       // +X% stamina
REGEN_RATE_FLAT           // +X stamina/s
REGEN_RATE_PERCENT        // +X% regen
CONSUMPTION_MULTIPLIER    // Tutte le azioni: X% costo
ATTACK_COST_PERCENT       // Solo attacchi: X% costo
ROLL_COST_PERCENT         // Solo roll: X% costo
BOW_TENSION_COST_PERCENT  // Solo arco: X% costo
SPRINT_THRESHOLD_PERCENT  // Modifica threshold sprint
REGEN_DELAY_FLAT          // +/- X secondi delay regen
BLOCK_REGEN_OVERRIDE      // Permette regen durante block (boolean)
6.5 Active Skills via Tag
Sistema di Abilità Attivabili:

ActiveSkillComponent (attachato al player se ha tag skill attive)

ActiveSkill:
- skillId: string (tag identifier)
- abilityType: enum
- cooldownRemaining: float
- isActive: boolean (per buff con durata)
- durationRemaining: float
Trigger abilità:

Input binding dedicato (es. keybind "Use Stamina Skill")
Menu radiale se multiple skill attive
Sistema di cooldown locale (non persistente)
Ability Types:

INSTANT_RECOVERY          // Recupero X% stamina istantaneo
CONSUMPTION_REDUCTION_BUFF // Riduce consumo per Y secondi
INFINITE_STAMINA_BUFF     // Stamina infinita per Y secondi
REGEN_BOOST_BUFF          // Regen X% più veloce per Y secondi
MEDITATION_CHANNEL        // Channeling: regen accelerata (interrompibile)
6.6 Sviluppo Posticipato
Cosa implementare SUBITO (core system):

TagModifierBridge con listener eventi tag
Sistema applicazione modificatori generici
File configurazione tag mappings (vuoto o con esempi)
API per aggiungere/rimuovere modificatori via tag
Cosa POSTICIPARE (quando mod esterna è pronta):

Population completa del file tag mappings
Testing integrazione con tag reali
Bilanciamento valori modificatori
Active skills implementation completa
Interfaccia per mod esterna:

API pubblica che la mod tag deve chiamare:

StaminaTagIntegration.registerTagMapping(tagId, modifierData)
StaminaTagIntegration.registerActiveSkill(tagId, skillData)
StaminaTagIntegration.unregisterTag(tagId)

// Oppure caricamento automatico da file config condiviso
7. Sistema di Configurazione e Bilanciamento
7.1 File di Configurazione (JSON/YAML)
stamina_config.json:

json
{
  "baseValues": {
    "maxStamina": 100.0,
    "baseRegenRate": 15.0,
    "sprintThreshold": 0.2,
    "combatTimeout": 7.0
  },
  "weightSystem": {
    "enabled": true,
    "weaponWeightProperty": "weight",
    "armorWeightProperty": "weight",
    "defaultWeaponWeight": 5.0,
    "defaultArmorWeightPerSlot": {
      "head": 2.0,
      "chest": 4.0,
      "legs": 3.0,
      "feet": 1.5
    },
    "weightUnit": "abstract"
  },
  "consumption": {
    "attacks": {
      "baseCost": 15.0,
      "curveType": "quadratic",
      "coefficients": [0.5, 1.0, 5.0],
      "useWeaponWeight": true,
      "lightAttackMultiplier": 0.8,
      "heavyAttackMultiplier": 1.5
    },
    "roll": {
      "baseCost": 20.0,
      "curveType": "linear",
      "coefficients": [1.2, 10.0],
      "useArmorWeight": true
    },
    "bow": {
      "baseCost": 10.0,
      "tensionThreshold": 0.4,
      "consumptionRate": 5.0,
      "maxTensionTime": 3.0,
      "useWeaponWeight": false
    },
    "crossbow": {
      "reloadCost": 25.0,
      "useWeaponWeight": true
    }
  },
  "regeneration": {
    "delayAfterConsumption": 0.8,
    "delayAfterBlock": 1.0
  }
}
Spiegazione parametri peso:

weaponWeightProperty: nome proprietà/metodo per leggere peso arma
armorWeightProperty: nome proprietà/metodo per leggere peso armatura
defaultWeaponWeight: fallback se arma non ha peso definito
defaultArmorWeightPerSlot: fallback per slot armatura
useWeaponWeight/useArmorWeight: flag per abilitare/disabilitare uso peso
7.2 Sistema di Telemetria
Metriche da tracciare:

Stamina media in combat
Frequenza stamina a 0
Win rate per livello stamina medio
Tool di debug:

Log eventi consumo/regen
Comandi console per test (set stamina, toggle infinite, etc.)
7.3 Debug Tools (Solo Runtime, No Persistence)
Comandi console per development:

/stamina set <value>           // Imposta stamina corrente
/stamina toggle_infinite       // Toggle stamina infinita
/stamina show_debug            // Mostra overlay debug
/stamina reload_config         // Ricarica configurazione
/stamina test_modifier <type> <value>  // Applica modificatore temporaneo
/stamina clear_modifiers       // Rimuove tutti modificatori
Debug Overlay (solo se abilitato):

Stamina corrente/max
Regen rate effettivo
Modificatori attivi (lista)
Ultimo consumo
Combat state (in/out)
NO salvataggio dati, solo visualizzazione real-time
7.4 Dati Runtime Necessari (Permessi)
Dati che DEVONO essere mantenuti in memoria durante gameplay:

✅ currentStamina - valore corrente
✅ maxStamina - calcolato da base + modificatori
✅ activeModifiers - lista modificatori attivi
✅ combatState - stato combat corrente
✅ cooldowns - cooldown skill attive (se implementate)
Persistenza minima (solo se necessario per save/load):

✅ currentStamina al momento del save
✅ cooldowns attivi (opzionale, potrebbero resettare al load)
❌ NO statistiche storiche
❌ NO metriche aggregate
8. Considerazioni Tecniche e Edge Cases
8.1 Performance
Ottimizzazioni:

Tick stamina solo per player in combat (no NPC se non necessario)
Cache calcoli curve (lookup table per valori comuni)
Event-driven invece di polling dove possibile
Batch updates se multiplayer
Profiling targets:

Calcolo consumo: < 0.1ms per azione
Tick rigenerazione: < 0.05ms per frame
Combat state check: < 0.01ms per tick
8.2 Edge Cases
Caso 1: Player entra/esce combat rapidamente

Soluzione: hysteresis sul timer (minimo 2s in combat)
Caso 2: Stamina a 0 durante animazione attacco

Soluzione: check pre-animazione, non durante
Caso 3: Modificatori sovrapposti (buff/debuff)

Soluzione: sistema di priorità e ordine applicazione
Caso 4: Lag/latency in multiplayer

Soluzione: client prediction + server reconciliation
Caso 5: Player cambia equipment durante combat

Soluzione: ricalcola modificatori, mantieni stamina corrente
Caso 6: Stamina insufficiente mid-combo

Soluzione: combo si interrompe, animazione recovery
8.3 Accessibilità e Difficoltà
Opzioni configurabili:

Casual Mode: +50% max stamina, +30% regen
Normal Mode: valori standard
Hardcore Mode: -20% max stamina, -15% regen
Assistive features:

Indicatore visivo chiaro quando azione non disponibile
Tutorial graduale (primo combat senza stamina, poi attivata)
Skill tree preset per stili di gioco (tank, agile, balanced)
8.4 Networking (se multiplayer)
Sincronizzazione:

Stamina è client-authoritative con server validation
Server traccia consumo azioni e valida
Anti-cheat: server verifica timing e costi
Latency compensation: client predice, server corregge se discrepanza
Packet structure:

StaminaUpdate {
  currentStamina: float,
  timestamp: long,
  lastAction: ActionType,
  isInCombat: boolean
}
9. Roadmap Implementazione
Fase 1: Core System (Priorità Alta)
StaminaComponent base con current/max
CombatStateTracker con timer e eventi
Rigenerazione base (lineare, in combat)
WeightProvider interface + implementation per mod esistente
Consumo attacchi con lettura peso arma
UI base (barra stamina)
Fase 2: Azioni Avanzate (Priorità Alta)
Roll con lettura peso armatura
Sprint disable sotto threshold
Scudo blocca rigenerazione
Arco sistema tensione
Balestra consumo ricarica (con peso)
Fase 3: Tag Integration (Priorità Alta) 
TagModifierBridge component
Tag listener system
Modifier application system generico
Tag mappings config file
API pubblica per mod esterna
Fase 4: Progressione Base (Priorità Media) 
Tier system con scaling max stamina
Modifier calculation engine
Active skills framework
Fase 5: Polish e Bilanciamento (Priorità Media)
Curve consumo avanzate (quadratiche/esponenziali)
Calibrazione curve basata su range pesi reali
Feedback visivo/audio completo
Debug tools (console commands, overlay, test curve tool)
Bilanciamento iterativo basato su playtest manuale
Fase 6: Advanced Features (Priorità Bassa)
10. Dipendenze e Integrazioni
Combat System: hook per pre-check stamina
Movement System: integrazione sprint/roll
Weapon System: aggiungere parametri stamina leggere peso esistente
Armor System: leggere peso esistente per calcolo roll
UI System: nuova barra stamina + feedback
Targeting System: eventi per combat state
Skill Tree System: nuovi nodi e modificatori
Player Stats: max stamina come stat progressiva
API Pubbliche da Esporre:
// Per altri sistemi
StaminaSystem.canPerformAction(actionType, cost) -> boolean
StaminaSystem.consumeStamina(amount) -> boolean
StaminaSystem.addModifier(modifier) -> void
StaminaSystem.removeModifier(modifierId) -> void
StaminaSystem.getCurrentStamina() -> float
StaminaSystem.getMaxStamina() -> float
StaminaSystem.isInCombat() -> boolean
StaminaSystem.getWeaponWeight(itemStack) -> float
StaminaSystem.getTotalArmorWeight(player) -> float
StaminaSystem.calculateWeightedCost(baseValue, weight, curveType) -> float

// Per skill/buff system
StaminaSystem.applyBuff(buffData) -> void
StaminaSystem.instantRecover(amount) -> void
StaminaSystem.setRegenMultiplier(multiplier, duration) -> void
11. Rischi e Mitigazioni
Rischio	Probabilità	Impatto	Mitigazione
Bilanciamento percepito come punitivo	Alta	Alto	Playtest iterativi, opzioni difficoltà, tutorial graduale
Performance issues con calcoli curve	Media	Medio	Lookup tables, profiling, ottimizzazioni early
Complessità skill tree crea build OP	Alta	Alto	Cap modificatori, diminishing returns, testing estensivo
Feedback visivo insufficiente	Media	Alto	UI/UX iteration, accessibility testing
Integrazione con sistemi esistenti problematica	Media	Alto	API chiare, refactoring graduale, feature flags
Learning curve troppo ripida	Alta	Critico	Tutorial interattivo, modalità casual, indicatori chiari
12. Success Metrics
Metriche Quantitative:

Engagement: tempo medio in combat +15%
Skill expression: variance win rate tra player skilled/casual < 30%
Retention: player retention a 7 giorni +10%
Balance: uso armi melee vs ranged ratio 60:40 (target)
Metriche Qualitative:

Feedback player: survey post-combat, sentiment analysis
"Momento wow": clip condivise di combat skillato
Frustration index: abbandoni durante combat < 5%
KPI Critici:

Nessun aggettivo negativo ("tedioso", "difficile") in >20% feedback
Sistema percepito come "fair" da >70% player base
Skill tree utilizzato da >80% player (non ignorato)
Conclusione
Questa progettazione fornisce un sistema di stamina modulare, configurabile e scalabile che soddisfa tutti i requisiti del documento di design. L'architettura event-driven garantisce performance, mentre il sistema di modificatori permette ampia personalizzazione via skill tree. L'implementazione a fasi riduce rischi e permette iterazioni basate su feedback reale.

Prossimi step consigliati:

Validare architettura con team tecnico
Creare prototype core system (Fase 1)
Playtest interno per validare "feel"
Iterare su curve e valori di bilanciamento
Espandere gradualmente con fasi 2-5
15. RISCRITTO - Integrazione con Sistema Peso Tharidia
15.1 Architettura Sistema Peso Esistente
Il sistema peso è già implementato in Tharidia Things con la seguente architettura:

Componenti principali:

WeightRegistry - Registry centrale che fornisce accesso ai pesi degli item
WeightData - Struttura dati caricata da datapack JSON
WeightDataLoader - Carica configurazione peso da datapacks
WeightManager - Calcola peso totale player (inventory + armor + accessories)
Metodo chiave per ottenere peso item:

java
WeightRegistry.getItemWeight(Item item) -> double
WeightRegistry.getItemWeight(ResourceLocation itemId) -> double
Caratteristiche:

Pesi definiti in datapack JSON (data/[namespace]/weight_config/[filename].json)
Peso default: 1.0 se item non ha peso configurato
Sistema già gestisce armi, armature e tutti gli item
15.2 Implementazione Integrazione Stamina
Per Armi (Attack Cost):

java
public double calculateAttackStaminaCost(Player player, AttackType attackType) {
    ItemStack weapon = player.getMainHandItem();
    
    // Legge peso dall'item usando WeightRegistry esistente
    double weaponWeight = WeightRegistry.getItemWeight(weapon.getItem());
    
    // Applica curva configurata
    double baseCost = config.getBaseAttackCost(attackType);
    double curveCost = applyCurve(weaponWeight, config.attackCurve);
    
    // Applica modificatori da tag/skill
    double modifiers = getPlayerModifiers(player, ModifierType.ATTACK_COST_PERCENT);
    
    return baseCost * curveCost * modifiers;
}
Per Armature (Roll Cost):

java
public double calculateRollStaminaCost(Player player) {
    double totalArmorWeight = 0.0;
    
    // Itera slot armatura (boots, leggings, chestplate, helmet)
    for (ItemStack armorStack : player.getInventory().armor) {
        if (!armorStack.isEmpty()) {
            // Legge peso da WeightRegistry
            totalArmorWeight += WeightRegistry.getItemWeight(armorStack.getItem());
        }
    }
    
    // Applica curva configurata
    double baseCost = config.baseRollCost;
    double curveCost = applyCurve(totalArmorWeight, config.rollCurve);
    
    // Applica modificatori
    double modifiers = getPlayerModifiers(player, ModifierType.ROLL_COST_PERCENT);
    
    return baseCost * curveCost * modifiers;
}
Per Balestra (Reload Cost):

java
public double calculateCrossbowReloadCost(Player player) {
    ItemStack crossbow = player.getMainHandItem();
    double crossbowWeight = WeightRegistry.getItemWeight(crossbow.getItem());
    
    double baseCost = config.crossbowReloadCost;
    double curveCost = applyCurve(crossbowWeight, config.crossbowCurve);
    double modifiers = getPlayerModifiers(player, ModifierType.ATTACK_COST_PERCENT);
    
    return baseCost * curveCost * modifiers;
}
15.3 Nessun Wrapper Necessario
Non serve creare abstraction layer perché:

WeightRegistry.getItemWeight() è già un'API pubblica stabile
Gestisce già fallback a peso default (1.0)
Funziona con tutti gli item (vanilla + custom)
Sistema già in produzione e testato
Integrazione diretta:

java
import com.THproject.tharidia_things.weight.WeightRegistry;

// Uso diretto nel sistema stamina
double weight = WeightRegistry.getItemWeight(item);
15.4 Gestione Edge Cases
Item senza peso configurato:

WeightRegistry restituisce automaticamente 1.0 (default)
Nessuna gestione speciale necessaria
WeightRegistry non inizializzato:

java
public double getItemWeightSafe(Item item) {
    try {
        // WeightRegistry gestisce già il caso null internamente
        return WeightRegistry.getItemWeight(item);
    } catch (Exception e) {
        LOGGER.warn("Error getting weight for item {}, using default", item, e);
        return 1.0; // Fallback sicuro
    }
}
Armi/Armature custom da altre mod:

Funziona automaticamente se hanno peso definito in datapack
Altrimenti usa peso default 1.0
Nessun crash o errore
15.5 Configurazione Peso per Stamina
File: stamina_config.json (sezione peso semplificata):

json
{
  "consumption": {
    "attacks": {
      "baseCost": 15.0,
      "curveType": "quadratic",
      "coefficients": [0.5, 1.0, 5.0],
      "useWeaponWeight": true
    },
    "roll": {
      "baseCost": 20.0,
      "curveType": "linear",
      "coefficients": [1.2, 10.0],
      "useArmorWeight": true
    },
    "crossbow": {
      "reloadCost": 25.0,
      "curveType": "linear",
      "coefficients": [1.0, 15.0],
      "useWeaponWeight": true
    }
  }
}
Note:

useWeaponWeight/useArmorWeight: flag per abilitare/disabilitare uso peso
Se false, usa solo baseCost senza moltiplicazione peso
Permette disabilitare meccanica peso per testing/bilanciamento
15.6 Calibrazione Curve Basata su Pesi Reali
Processo di tuning (Fase 5 implementazione):

Step 1: Analisi range pesi esistenti

java
// Tool debug per raccogliere statistiche peso
/stamina analyze_weights

Output esempio:
Weapons:
  Min: 0.5 (minecraft:wooden_sword)
  Max: 15.0 (tharidia:greatsword)
  Median: 5.0

Armor (total per set):
  Min: 4.0 (minecraft:leather_armor)
  Max: 25.0 (tharidia:plate_armor)
  Median: 12.0
Step 2: Definire target azioni

Target design:
- Arma leggera (peso ~2): 5-6 attacchi
- Arma media (peso ~7): 3-4 attacchi
- Arma pesante (peso ~12): 2-3 attacchi

- Armatura leggera (peso ~5): 4-5 roll
- Armatura media (peso ~12): 2-3 roll
- Armatura pesante (peso ~20): 1-2 roll
Step 3: Calcolare coefficienti curva

Per curva quadratica: y = a*x² + b*x + c
Dove:
- x = peso item
- y = moltiplicatore costo (non costo assoluto)

Esempio attacchi (maxStamina=100, baseCost=15):
- Peso 2: y=0.8 → cost=12 → 8 attacchi ✓
- Peso 7: y=1.5 → cost=22.5 → 4 attacchi ✓
- Peso 12: y=2.5 → cost=37.5 → 2 attacchi ✓

Coefficienti risultanti: a=0.015, b=0.05, c=0.7
Step 4: Testing iterativo

java
// Console command per test rapido
/stamina test_cost weapon <item> <attack_type>
/stamina test_cost roll

Output:
Item: tharidia:longsword
Weight: 7.0
Base Cost: 15.0
Curve Multiplier: 1.52
Final Cost: 22.8
Actions Possible: 4.38 attacks
15.7 Esempio Datapack Peso (Riferimento)
Struttura file peso esistente:

data/tharidia/weight_config/items.json
Formato JSON (già esistente in Tharidia Things):

json
{
  "item_weights": {
    "minecraft:wooden_sword": 2.0,
    "minecraft:iron_sword": 5.0,
    "minecraft:diamond_sword": 7.0,
    "tharidia:longsword": 7.5,
    "tharidia:greatsword": 15.0,
    "tharidia:dagger": 1.5,
    
    "minecraft:leather_helmet": 0.5,
    "minecraft:leather_chestplate": 1.5,
    "minecraft:leather_leggings": 1.0,
    "minecraft:leather_boots": 0.5,
    
    "minecraft:iron_helmet": 2.0,
    "minecraft:iron_chestplate": 5.0,
    "minecraft:iron_leggings": 3.5,
    "minecraft:iron_boots": 1.5,
    
    "tharidia:plate_helmet": 4.0,
    "tharidia:plate_chestplate": 10.0,
    "tharidia:plate_leggings": 7.0,
    "tharidia:plate_boots": 3.0
  },
  "thresholds": {
    "light": 100.0,
    "medium": 200.0,
    "heavy": 300.0,
    "overencumbered": 400.0
  },
  "debuffs": {
    "light_speed_multiplier": 0.95,
    "medium_speed_multiplier": 0.85,
    "heavy_speed_multiplier": 0.70,
    "overencumbered_speed_multiplier": 0.50,
    "heavy_disable_swim_up": true,
    "overencumbered_disable_swim_up": true
  }
}
Note:

Sistema stamina legge solo item_weights
thresholds e debuffs sono per sistema peso esistente (non stamina)
Nessuna modifica necessaria a questo file
15.8 Dipendenza da Tharidia Things
Dependency in build.gradle:

gradle
dependencies {
    // Dipendenza da Tharidia Things per WeightRegistry
    implementation fg.deobf("com.THproject:tharidia_things:${tharidia_things_version}")
}
Import necessari:

java
import com.THproject.tharidia_things.weight.WeightRegistry;
Gestione mod non presente (opzionale):

java
// Se si vuole supportare uso standalone senza Tharidia Things
public class WeightIntegration {
    private static boolean THARIDIA_PRESENT = false;
    
    static {
        try {
            Class.forName("com.THproject.tharidia_things.weight.WeightRegistry");
            THARIDIA_PRESENT = true;
        } catch (ClassNotFoundException e) {
            THARIDIA_PRESENT = false;
        }
    }
    
    public static double getItemWeight(Item item) {
        if (THARIDIA_PRESENT) {
            return WeightRegistry.getItemWeight(item);
        }
        // Fallback se Tharidia Things non presente
        return 1.0;
    }
}
Nota: Probabilmente non necessario se stamina è sempre usata con Tharidia Things.

15.9 Vantaggi Integrazione Diretta
✅ Zero duplicazione codice - usa sistema esistente e testato ✅ Consistenza - stesso peso usato per movimento e stamina ✅ Manutenzione semplice - modifiche peso in un solo posto (datapack) ✅ Nessun sync issue - fonte unica di verità per pesi ✅ Datapack-driven - server admin può bilanciare senza ricompilare ✅ Supporto mod esterne - qualsiasi item con peso definito funziona

15.10 Testing Strategy
Unit tests:

java
@Test
public void testWeaponWeightIntegration() {
    // Mock WeightRegistry per test
    when(WeightRegistry.getItemWeight(Items.IRON_SWORD)).thenReturn(5.0);
    
    double cost = staminaSystem.calculateAttackCost(player, AttackType.LIGHT);
    
    // Verifica curva applicata correttamente
    assertThat(cost).isGreaterThan(baseCost);
}
Integration tests:

java
@Test
public void testArmorWeightCalculation() {
    // Equip full iron armor
    player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
    player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
    player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
    player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
    
    double rollCost = staminaSystem.calculateRollCost(player);
    
    // Verifica costo aumentato rispetto a no armor
    assertThat(rollCost).isGreaterThan(baseRollCost);
}
In-game testing:

1. Equipaggiare arma leggera → verificare 5-6 attacchi possibili
2. Equipaggiare arma pesante → verificare 2-3 attacchi possibili
3. Equipaggiare armatura leggera → verificare 4-5 roll possibili
4. Equipaggiare armatura pesante → verificare 1-2 roll possibili
5. Testare con item custom da datapack
