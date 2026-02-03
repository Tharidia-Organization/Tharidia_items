# THARIDIA THINGS MOD - DOCUMENTO DI RIFERIMENTO COMPLETO

**Versione Documento:** 1.0
**Data Creazione:** 2026-01-10
**Mod ID:** `tharidiathings`
**Versione Mod:** 1.0.8
**Minecraft:** 1.21.1
**Framework:** NeoForge 2.0.112
**Java:** 21

---

## INDICE

1. [Informazioni Generali](#1-informazioni-generali)
2. [Struttura Package](#2-struttura-package)
3. [Sistema Stamina](#3-sistema-stamina)
4. [Sistema Weight](#4-sistema-weight)
5. [Sistema Diet](#5-sistema-diet)
6. [Sistema Fatigue](#6-sistema-fatigue)
7. [Sistema Character](#7-sistema-character)
8. [Sistema Claims](#8-sistema-claims)
9. [Sistema Realm](#9-sistema-realm)
10. [Sistema Video](#10-sistema-video)
11. [Sistema Trade](#11-sistema-trade)
12. [Sistema Database](#12-sistema-database)
13. [Sistema Networking](#13-sistema-networking)
14. [Client-Side](#14-client-side)
15. [Integrazioni Esterne](#15-integrazioni-esterne)
16. [Registrazioni](#16-registrazioni)
17. [Configurazioni](#17-configurazioni)
18. [Risorse](#18-risorse)
19. [Problemi Noti e Fix](#19-problemi-noti-e-fix)
20. [Cronologia Modifiche](#20-cronologia-modifiche)

---

## 1. INFORMAZIONI GENERALI

### Statistiche Codebase
| Metrica | Valore |
|---------|--------|
| File Java | 217 |
| Linee di Codice | ~36,915 |
| Package | 40+ |
| Network Packets | 37 |
| Event Handlers | 14+ |
| Comandi | 10+ |

### File Entry Point
- **Main:** `src/main/java/com/THproject/tharidia_things/TharidiaThings.java` (960 linee)
- **Client:** `src/main/java/com/THproject/tharidia_things/TharidiaThingsClient.java`
- **Config:** `src/main/java/com/THproject/tharidia_things/Config.java`

### Dipendenze Esterne (JarJar)
| Libreria | Versione | Scopo |
|----------|----------|-------|
| MySQL Connector/J | 8.3.0 | Driver database |
| HikariCP | 5.1.0 | Connection pooling |
| JLayer | 1.0.1 | MP3 playback |
| VLCJ | 4.8.2 | Video playback |
| VLCJ-natives | 4.8.0 | Native libraries |

### Dipendenze Mod (compileOnly)
- **Tharidia Tweaks** (`libs/tharidiatweaks-latest.jar`)
- **Tharidia Features** (`libs/tharidiafeatures-latest.jar`)
- **Epic Fight** (optional, runtime detection)

---

## 2. STRUTTURA PACKAGE

```
src/main/java/com/THproject/tharidia_things/
├── TharidiaThings.java          # Entry point (960 linee)
├── TharidiaThingsClient.java    # Client setup
├── Config.java                  # Configurazione globale
│
├── block/                       # Blocchi custom
│   ├── PietroBlock.java
│   ├── ClaimBlock.java
│   ├── HotIronMarkerBlock.java
│   ├── HotGoldMarkerBlock.java
│   ├── HotCopperMarkerBlock.java
│   └── entity/                  # Block Entities
│       ├── PietroBlockEntity.java (700 linee)
│       ├── ClaimBlockEntity.java (642 linee)
│       ├── HotIronAnvilEntity.java
│       ├── HotGoldAnvilEntity.java
│       └── HotCopperAnvilEntity.java
│
├── item/                        # Item custom (15 items)
│   ├── HotIronItem.java, HotGoldItem.java, HotCopperItem.java
│   ├── PinzaItem.java (480 durabilità)
│   ├── LamaLungaItem.java, LamaCortaItem.java, ElsaItem.java
│   ├── GoldLamaLungaItem.java, GoldLamaCortaItem.java
│   ├── CopperLamaLungaItem.java, CopperLamaCortaItem.java, CopperElsaItem.java
│   ├── DiceItem.java
│   └── BattleGauntlet.java
│
├── stamina/                     # Sistema stamina (11 file)
│   ├── StaminaData.java
│   ├── StaminaAttachments.java
│   ├── StaminaComputedStats.java
│   ├── StaminaModifier.java
│   ├── StaminaModifierType.java
│   ├── StaminaModifierEngine.java
│   ├── StaminaTagIntegration.java
│   ├── StaminaTagMappings.java
│   ├── StaminaTagMappingsLoader.java
│   ├── TagModifierBridge.java
│   └── CombatState.java
│
├── fatigue/                     # Sistema fatigue (3 file)
│   ├── FatigueData.java
│   ├── FatigueAttachments.java
│   └── FatigueHandler.java (578 linee)
│
├── diet/                        # Sistema dieta (14 file)
│   ├── DietData.java
│   ├── DietAttachments.java
│   ├── DietCategory.java
│   ├── DietHandler.java
│   ├── DietRegistry.java
│   ├── DietProfile.java
│   ├── DietProfileCache.java
│   ├── ClientDietProfileCache.java
│   ├── DietHeuristics.java
│   ├── DietEffectApplier.java
│   ├── DietDataLoader.java
│   ├── DietSystemSettings.java
│   ├── DietPackConfig.java
│   └── RecipeNutrientAnalyzer.java (662 linee)
│
├── weight/                      # Sistema peso
│   ├── WeightManager.java
│   ├── WeightRegistry.java
│   ├── WeightData.java
│   └── WeightDataLoader.java
│
├── character/                   # Sistema character
│   ├── CharacterData.java
│   ├── CharacterAttachments.java
│   ├── CharacterEventHandler.java
│   └── RaceData.java
│
├── claim/                       # Sistema claim (7 file)
│   ├── ClaimRegistry.java
│   ├── ClaimSavedData.java
│   ├── ClaimBlock.java
│   ├── ClaimBlockEntity.java
│   ├── ClaimProtectionHandler.java (635 linee)
│   ├── ClaimExpirationHandler.java
│   ├── ClaimCommands.java
│   └── ClaimAdminCommands.java
│
├── realm/                       # Sistema realm (6 file)
│   ├── RealmManager.java
│   ├── PietroBlock.java
│   ├── PietroBlockEntity.java
│   ├── RealmPlacementHandler.java
│   └── RealmBoundaryRenderer.java (724 linee)
│
├── video/                       # Sistema video
│   ├── VideoScreen.java
│   ├── VideoPlaybackState.java
│   ├── VideoScreenRegistry.java
│   ├── YouTubeUrlExtractor.java (541 linee)
│   └── VideoScreenCommands.java (554 linee)
│
├── trade/                       # Sistema trade (7 file)
│   ├── TradeMenu.java
│   ├── TradeScreen.java
│   ├── TradeInteractionHandler.java
│   ├── TradeInventoryBlocker.java
│   ├── TradePacketHandler.java
│   └── TradeClientHandler.java
│
├── database/                    # Sistema database
│   └── DatabaseManager.java (206 linee)
│
├── servertransfer/              # Transfer server (4 file)
│   ├── ServerTransferManager.java
│   ├── TransferTokenManager.java
│   ├── DevWhitelistManager.java
│   └── ServerTransferCommands.java
│
├── event/                       # Event handlers (14+)
│   ├── StaminaHandler.java (1,255 linee) [IL PIU' GRANDE]
│   ├── FatigueHandler.java
│   ├── DietHandler.java
│   ├── ClaimProtectionHandler.java
│   ├── ClaimExpirationHandler.java
│   ├── RealmPlacementHandler.java
│   ├── WeightDebuffHandler.java
│   ├── SmithingHandler.java
│   ├── FreezeManager.java
│   ├── PreLoginNameHandler.java
│   ├── ItemAttributeHandler.java
│   ├── PlayerStatsIncrementHandler.java
│   ├── TradeInteractionHandler.java
│   ├── TradeInventoryBlocker.java
│   ├── CurrencyProtectionHandler.java
│   └── BattleLogic.java
│
├── command/                     # Comandi (10 file)
│   ├── ClaimCommands.java
│   ├── ClaimAdminCommands.java
│   ├── DietCommands.java
│   ├── FatigueCommands.java
│   ├── CharacterCommands.java
│   ├── TradeCommands.java
│   ├── BattleCommands.java
│   ├── MarketCommands.java
│   ├── VideoScreenCommands.java
│   ├── ServerTransferCommands.java
│   ├── ItemCatalogueCommand.java
│   └── StatsCommand.java
│
├── network/                     # Packet (37 tipi)
│   ├── [22 Client-Bound Packets]
│   ├── [11 Server-Bound Packets]
│   ├── ClientPacketHandler.java
│   ├── TradePacketHandler.java
│   └── ServerMusicFileHandler.java
│
├── gui/                         # GUI server-side (5 menu)
│   ├── ClaimMenu.java
│   ├── PietroMenu.java
│   ├── ComponentSelectionMenu.java
│   ├── TradeMenu.java
│   └── BattleInviteMenu.java
│
├── client/                      # Client-side (56 file)
│   ├── ClientPacketHandler.java
│   ├── ClientConnectionHandler.java
│   ├── TradeClientHandler.java
│   ├── WeightHudOverlay.java
│   ├── StaminaHudOverlay.java
│   ├── RealmOverlay.java
│   ├── ZoneMusicPlayer.java (591 linee)
│   ├── RealmBoundaryRenderer.java
│   ├── ClaimBoundaryRenderer.java
│   ├── NametagVisibilityHandler.java
│   ├── screen/                  # GUI Screens
│   ├── gui/                     # GUI Components
│   ├── renderer/                # Block Entity Renderers
│   ├── model/                   # 3D Models
│   └── video/                   # Video System
│       ├── VLCVideoPlayer.java (896 linee)
│       ├── ClientVideoScreenManager.java
│       ├── VideoScreenRenderHandler.java
│       ├── DependencyCheckHandler.java
│       └── DependencyDownloader.java (544 linee)
│
├── registry/                    # Registrazioni
│   ├── ModAttributes.java (9 attributi)
│   └── ModStats.java (9 statistiche)
│
├── config/                      # Configurazioni
│   ├── StaminaConfig.java
│   ├── FatigueConfig.java
│   ├── CropProtectionConfig.java
│   └── ItemCatalogueConfig.java
│
├── integration/                 # Integrazioni mod
│   └── GodEyeIntegration.java
│
└── util/                        # Utility
```

---

## 3. SISTEMA STAMINA

### File Coinvolti
| File | Path | Linee | Ruolo |
|------|------|-------|-------|
| StaminaData | `stamina/StaminaData.java` | ~150 | Storage dati NBT |
| StaminaAttachments | `stamina/StaminaAttachments.java` | ~30 | Attachment registration |
| StaminaHandler | `event/StaminaHandler.java` | 1,255 | Event handler principale |
| StaminaModifierEngine | `stamina/StaminaModifierEngine.java` | ~200 | Calcolo modificatori |
| StaminaComputedStats | `stamina/StaminaComputedStats.java` | ~80 | Record stats calcolati |
| CombatState | `stamina/CombatState.java` | ~100 | Tracking combattimento |
| TagModifierBridge | `stamina/TagModifierBridge.java` | ~150 | Bridge per tag system |

### Struttura Dati (StaminaData)
```java
float currentStamina;           // 0 - maxStamina
float maxStamina;               // Calcolato dinamicamente
boolean inCombat;               // Flag combattimento
int combatTicksRemaining;       // Countdown uscita combat
int regenDelayTicksRemaining;   // Ritardo regen dopo consumo
long bowDrawLockUntilGameTime;  // Lock arco
List<StaminaModifier> modifiers; // Modificatori attivi
boolean initialized;            // Flag init
```

### Tipi Modificatori
| Tipo | Operazione |
|------|------------|
| MAX_STAMINA_FLAT | +N stamina max |
| MAX_STAMINA_PERCENT | +N% stamina max |
| REGEN_RATE_FLAT | +N regen/sec |
| REGEN_RATE_PERCENT | +N% regen |
| CONSUMPTION_MULTIPLIER | xN consumo |
| ATTACK_COST_PERCENT | xN costo attacchi |
| BOW_TENSION_COST_PERCENT | xN costo arco |
| SPRINT_THRESHOLD_PERCENT | N% soglia sprint |
| REGEN_DELAY_FLAT | +N sec delay |
| BLOCK_REGEN_OVERRIDE | Override regen |

### Event Handlers
| Evento | Priority | Azione |
|--------|----------|--------|
| AttackEntityEvent | HIGHEST | Consumo stamina melee |
| PlayerInteractEvent.RightClickItem | HIGHEST | Check stamina arco |
| PlayerInteractEvent.LeftClickBlock | HIGHEST | Verifica left-click |
| LivingIncomingDamageEvent | HIGHEST | Ingresso combattimento |
| LivingDamageEvent.Pre | HIGHEST | Nullifica danno se insufficiente |
| ArrowLooseEvent | NORMAL | Consumo rilascio arco |
| PlayerTickEvent.Post | NORMAL | Regen, tick combat |
| PlayerLoggedInEvent | NORMAL | Sync iniziale |
| PlayerLoggedOutEvent | NORMAL | Cleanup |

### Integrazione Epic Fight
- **Condizione:** `ModList.get().isLoaded("epicfight")`
- **Eventi:** ComboAttackEvent, DealDamageEvent.Income, SkillConsumeEvent, SkillCastEvent
- **Metodo:** Reflection dinamica con `registerEpicFightEventListener()`

### Packet Rete
| Packet | Direzione | Dati |
|--------|-----------|------|
| StaminaSyncPacket | S2C | currentStamina, maxStamina, inCombat, bowDrawLockUntilGameTime |
| MeleeSwingPacket | C2S | (trigger swing animation) |

---

## 4. SISTEMA WEIGHT

### File Coinvolti
| File | Path | Ruolo |
|------|------|-------|
| WeightManager | `weight/WeightManager.java` | Calcolo peso player |
| WeightRegistry | `weight/WeightRegistry.java` | Registry centrale |
| WeightData | `weight/WeightData.java` | Record config |
| WeightDataLoader | `weight/WeightDataLoader.java` | Loader JSON |
| WeightDebuffHandler | `event/WeightDebuffHandler.java` | Applicazione debuff |

### Soglie e Debuff (default.json)
| Status | Soglia | Speed Multiplier | Swim Disabled |
|--------|--------|------------------|---------------|
| LIGHT | 0-200 | 0.95 | No |
| MEDIUM | 200-400 | 0.85 | No |
| HEAVY | 400-700 | 0.70 | Yes |
| OVERENCUMBERED | 700+ | 0.50 | Yes |

### Calcolo Peso
```java
// Inventory slots: 0-35 (main), 36-39 (armor), 40 (offhand)
// + Accessories mod slots (via reflection)
double calculatePlayerWeight(Player player) {
    double total = 0;
    for (int slot = 0; slot < 41; slot++) {
        ItemStack stack = inventory.getItem(slot);
        total += WeightRegistry.getItemWeight(stack.getItem()) * stack.getCount();
    }
    // + Accessories se presente
    return total;
}
```

### Performance Optimization
- Debuff applicato ogni 20 tick (1 secondo)
- Staggering giocatori: `UUID.hashCode % 5` per distribuire carico
- Swim check ogni 5 tick (250ms)

---

## 5. SISTEMA DIET

### File Coinvolti (14 file)
| File | Path | Linee | Ruolo |
|------|------|-------|-------|
| DietData | `diet/DietData.java` | ~200 | Storage nutrienti |
| DietRegistry | `diet/DietRegistry.java` | ~300 | Registry + cache |
| DietProfileCache | `diet/DietProfileCache.java` | 13,039 | Cache server |
| ClientDietProfileCache | `diet/ClientDietProfileCache.java` | 12,146 | Cache client |
| DietEffectApplier | `diet/DietEffectApplier.java` | 7,589 | Buff/debuff |
| DietHeuristics | `diet/DietHeuristics.java` | 15,635 | Euristica nutrienti |
| RecipeNutrientAnalyzer | `diet/RecipeNutrientAnalyzer.java` | 662 | Analisi ricette |

### Categorie Nutrienti
```java
enum DietCategory {
    GRAIN,      // Carboidrati
    PROTEIN,    // Proteine
    VEGETABLE,  // Verdure
    FRUIT,      // Frutta
    SUGAR,      // Zuccheri
    WATER       // Idratazione
}
```

### Effetti per Categoria
| Categoria | Basso (< 20%) | Alto (> 80%) |
|-----------|---------------|--------------|
| GRAIN | Slowness I, Mining Fatigue | Speed I |
| PROTEIN | Weakness I, -4 max health | +2 max health |
| VEGETABLE | - | Resistance I |
| FRUIT | -10% movement | +10% movement |
| SUGAR | -15% attack speed | Haste I |
| WATER | Nausea, -10% speed | Water Breathing, Dolphin's Grace |

### Decay System
- **Intervallo default:** 120 secondi
- **Decay rates:** GRAIN 0.15, PROTEIN 0.2, VEGETABLE 0.25, FRUIT 0.25, SUGAR 0.3, WATER 0.35

### Cache Layers
1. **Memory Cache** (Guava): 512 entries, 10 min TTL
2. **Explicit Config**: Item mappati da JSON
3. **Persistent Cache**: Pre-calcolato
4. **On-Demand**: RecipeNutrientAnalyzer

---

## 6. SISTEMA FATIGUE

### File Coinvolti
| File | Path | Ruolo |
|------|------|-------|
| FatigueData | `fatigue/FatigueData.java` | Storage tick stanchezza |
| FatigueAttachments | `fatigue/FatigueAttachments.java` | Attachment |
| FatigueHandler | `fatigue/FatigueHandler.java` | Event handler (578 linee) |

### Configurazione Default
| Parametro | Valore |
|-----------|--------|
| max_fatigue_minutes | 40 |
| bed_rest_time_seconds | 60 |
| proximity_recovery_interval | 10s |
| proximity_recovery_amount | 60s |
| bed_proximity_range | 20 blocchi |
| exhaustion_slowness_level | 3 (-60%) |

### Effetti Esaurimento
- Slowness III
- Nausea (opzionale)
- Warning a 5 minuti e 1 minuto

---

## 7. SISTEMA CHARACTER

### File Coinvolti
| File | Path | Ruolo |
|------|------|-------|
| CharacterData | `character/CharacterData.java` | Flag creazione |
| CharacterAttachments | `character/CharacterAttachments.java` | Attachment |
| CharacterEventHandler | `character/CharacterEventHandler.java` | Teleport logic |

### Character Creation Dimension
- **ID:** `tharidiathings:character_creation`
- **Platform:** 20x20 stone, Y=99
- **Barrier:** 4 blocchi alti
- **Spacing:** 100 chunk tra giocatori
- **Position calc:** UUID hash

### Flow
```
Login (senza character)
  → Delay 2 tick
  → Pre-load 5x5 chunks
  → Create platform + barriers
  → Spawn race points
  → Set ADVENTURE mode
  → Set invulnerable

Selezione completata
  → CharacterData.setCharacterCreated(true)
  → Remove invulnerability
  → Teleport overworld spawn
```

---

## 8. SISTEMA CLAIMS

### File Coinvolti
| File | Path | Linee | Ruolo |
|------|------|-------|-------|
| ClaimRegistry | `claim/ClaimRegistry.java` | ~400 | Registry in-memory |
| ClaimSavedData | `claim/ClaimSavedData.java` | ~300 | Persistent NBT |
| ClaimProtectionHandler | `event/ClaimProtectionHandler.java` | ~1075 | Event protection |
| ClaimBlockEntity | `block/entity/ClaimBlockEntity.java` | ~750 | Block entity |
| ClaimDecayManager | `claim/ClaimDecayManager.java` | ~150 | Decay system |
| ClaimCommands | `command/ClaimCommands.java` | ~300 | Player commands |
| TrustContractItem | `item/TrustContractItem.java` | ~270 | Trust item |

### Struttura Dati
```java
// ClaimRegistry (in-memory)
Map<String, Map<BlockPos, ClaimData>> claims;  // dimension → pos → data
Map<UUID, Set<BlockPos>> playerClaims;         // player → positions

// ClaimBlockEntity
BlockPos position;
UUID ownerUUID;
String claimName;
String ownerName;
long creationTime;
String dimension;
long expirationTime;         // Quando scade il claim
Set<UUID> trustedPlayers;    // Lista giocatori fidati
List<BlockPos> mergedClaims; // Claims unificati

// Granular Flags (tutti default false = solo trusted)
boolean allowContainerAccess;  // Contenitori (casse, fornaci, etc.)
boolean allowDoorUse;          // Porte, botole, cancelli
boolean allowSwitchUse;        // Leve, pulsanti, piastre a pressione
boolean allowVehiclePlace;     // Barche, minecart
boolean allowAnimalInteract;   // Interazione animali (sellare, guinzagliare)
boolean allowExplosions;       // Esplosioni (default false)
boolean allowFireSpread;       // Propagazione fuoco (default false)
boolean allowMobSpawning;      // Spawn mob (non implementato)
```

### Sistema di Scadenza e Decay
```
CLAIM CREATO
    │
    ▼
ATTIVO (Protezione ON)
    │
    │ expirationTime raggiunto
    ▼
SCADUTO - GRACE PERIOD (3 giorni)
│   - Protezione DISATTIVATA (isProtectionActive = false)
│   - Claim ancora visibile, può essere rinnovato
    │
    │ GRACE_PERIOD_MS passati
    ▼
DECAY PERIOD (14 giorni)
│   - Protezione ancora OFF
│   - Dopo 14 giorni: blocchi funzionali rimossi
    │
    │ shouldDecay() = true
    ▼
DECAY (ClaimDecayManager)
    - Rimuove: casse, fornaci, shulker, letti, tavoli lavoro,
      incantamento, anvil, barili, beacon, frame, etc.
    - Preserva: blocchi costruzione (stone, wood, glass, etc.)
```

### Costanti Temporali
```java
GRACE_PERIOD_MS = 3 giorni   // 3L * 24L * 60L * 60L * 1000L
DECAY_PERIOD_MS = 14 giorni  // 14L * 24L * 60L * 60L * 1000L
```

### Protezioni
| Evento | Protezione | Flag Override |
|--------|------------|---------------|
| BlockEvent.BreakEvent | Blocca rottura | - |
| BlockEvent.EntityPlaceEvent | Blocca piazzamento | - |
| PlayerInteractEvent.RightClickBlock | Blocca interazione | Vedi flag granulari |
| PlayerInteractEvent.LeftClickBlock | Blocca interazione | - |
| ExplosionEvent.Detonate | Rimuove blocchi | allowExplosions |
| AttackEntityEvent (PvP) | Blocca sempre | - |
| FarmlandTrampleEvent | Blocca calpestamento | - |
| BlockEvent.EntityPlaceEvent (fire) | Blocca fuoco | allowFireSpread |
| BlockEvent.FluidPlaceBlockEvent | Blocca fluidi | - |
| PistonEvent.Pre | Blocca pistoni | - |
| PlayerInteractEvent.EntityInteract | Blocca animali | allowAnimalInteract |

### Protezioni Flag Granulari
| Flag | Blocchi Controllati |
|------|---------------------|
| allowContainerAccess | ChestBlock, BarrelBlock, ShulkerBoxBlock, HopperBlock, DispenserBlock, DropperBlock, AbstractFurnaceBlock, BrewingStandBlock, BeaconBlock, EnderChest, Jukebox, ChiseledBookshelf, Lectern |
| allowDoorUse | DoorBlock, TrapDoorBlock, FenceGateBlock |
| allowSwitchUse | ButtonBlock, LeverBlock, PressurePlateBlock, WeightedPressurePlateBlock, DaylightDetectorBlock, TripWireHookBlock, NoteBlock, RepeaterBlock, ComparatorBlock |
| allowVehiclePlace | BoatItem, MinecartItem |
| allowAnimalInteract | Animal (entity) |

### Trust Contract (Contratto di Fiducia)
Permette di concedere trust senza usare comandi.

**Flow:**
1. Giocatore crafta "Contratto Vuoto" (8 paper + 1 ink sac)
2. Owner clicca destro sul proprio Claim Block → Contratto legato
3. Owner dà il contratto a un altro giocatore
4. Altro giocatore clicca destro (in aria) → Diventa trusted
5. Contratto consumato

**NBT Dati:**
```java
OwnerUUID, OwnerName, ClaimName, ClaimPos (long), ClaimDimension, CreationTime, Bound
```

### Comandi
```
/claim trust <player>           - Aggiunge giocatore ai fidati
/claim untrust <player>         - Rimuove giocatore dai fidati
/claim list                     - Lista claims del giocatore
/claim info                     - Info sul claim attuale
/claim flags                    - Mostra flag attuali

/claim containers <allow|deny>  - Flag contenitori
/claim doors <allow|deny>       - Flag porte
/claim switches <allow|deny>    - Flag interruttori
/claim vehicles <allow|deny>    - Flag veicoli
/claim animals <allow|deny>     - Flag animali
```

### Caching
- `claimCache`: BlockPos → ClaimBlockEntity, clear ogni 10s
- `outerLayerCache`: ChunkPos → PietroBlockEntity, clear ogni 5s

### Integrazione GodEye
**File:** `integration/GodEyeIntegration.java`
- Sync claim data asincrono con timeout 5s
- ExecutorService daemon thread per non bloccare JVM
- Metodo `syncPlayerClaims()` non blocca mai il server thread
- `syncPlayerClaimsBlocking()` per shutdown

---

## 9. SISTEMA REALM

### File Coinvolti
| File | Path | Linee | Ruolo |
|------|------|-------|-------|
| RealmManager | `realm/RealmManager.java` | ~300 | Gestione realm |
| PietroBlockEntity | `block/entity/PietroBlockEntity.java` | ~750 | Block entity |
| RealmBoundaryRenderer | `client/RealmBoundaryRenderer.java` | 724 | Rendering confini |
| RealmRankIndicator | `realm/RealmRankIndicator.java` | ~100 | Particle badge |
| HierarchyRank | `realm/HierarchyRank.java` | ~50 | Enum ranghi |

### Struttura Dati
```java
BlockPos position;
int realmSize;
String ownerName;
UUID ownerUUID;
int centerChunkX, centerChunkZ;
Map<UUID, Integer> hierarchy;  // player → rank level
```

### Sistema Gerarchia (HierarchyRank)
| Rank | Level | Colore Particella |
|------|-------|-------------------|
| COLONO | 0 | Grigio pietra (0.55, 0.55, 0.55) |
| MILIZIANO | 1 | Verde foresta (0.35, 0.70, 0.35) |
| GUARDIA | 2 | Giallo pallido (0.90, 0.85, 0.20) |
| CONSIGLIERE | 3 | Oro antico (0.85, 0.65, 0.10) |
| LORD | 4 | Cremisi profondo (0.85, 0.12, 0.12) |

### Indicatore Visivo Rank (RealmRankIndicator)
- **Tick interval:** 70 tick (~3.5 secondi)
- **Tipo particella:** DustParticleOptions (polvere colorata)
- **Dimensione:** 0.7
- **Posizione:** Spalla sinistra del giocatore
  - Offset X: -0.3 (sinistra)
  - Offset Y: 1.5 (altezza spalla)
  - Offset Z: 0.0 (centro)
- **Comportamento:**
  - Una particella ogni ~3.5 secondi sulla spalla
  - Colore dipende dal rank nel realm
  - Visibile solo quando il giocatore è in un realm
  - Server-side spawning (visibile a tutti)

### Area del Realm
- **Centro:** Chunk in cui è posizionato il Pietro Block
- **Espansione:** `realmSize` chunks in ogni direzione
- **Outer Layer:** 1 chunk di buffer esterno (protezione crop)
- **Crop Protection:** Nell'outer layer solo giocatori con claim possono interagire con crop

### Sync Packet
```java
// RealmSyncPacket (S2C)
List<RealmData> realms;
boolean fullSync;  // true = replace all, false = partial update

// HierarchySyncPacket (S2C)
BlockPos realmPos;
Map<UUID, Integer> hierarchy;
UUID ownerUUID;
String ownerName;
```

---

## 10. SISTEMA VIDEO

### File Coinvolti
| File | Path | Linee | Ruolo |
|------|------|-------|-------|
| VideoScreen | `video/VideoScreen.java` | ~200 | Dati schermo |
| VLCVideoPlayer | `client/video/VLCVideoPlayer.java` | 896 | Player VLC/FFmpeg |
| YouTubeUrlExtractor | `video/YouTubeUrlExtractor.java` | 541 | Estrazione URL |
| ClientVideoScreenManager | `client/video/ClientVideoScreenManager.java` | ~300 | Manager client |

### Struttura VideoScreen
```java
UUID id;
BlockPos corner1, corner2;
Direction.Axis axis;
Direction facing;
int width, height;
double aspectRatio;
String videoUrl;
VideoPlaybackState playbackState;  // STOPPED, PLAYING, PAUSED
float volume;
AABB bounds;
```

### URL Extraction Flow
```
YouTube/Twitch URL
  → 1. Try yt-dlp CLI (-f best[ext=mp4] -g)
  → 2. Try direct HTML parse (ytInitialPlayerResponse)
  → 3. Try Invidious API (fallback)
  → 4. Try streamlink (Twitch)
```

### Fix Recenti
- Shutdown hook per cleanup processi FFmpeg
- Thread daemon per VideoLoader
- forceCleanupProcesses() per JVM exit

---

## 11. SISTEMA TRADE

### File Coinvolti
| File | Path | Ruolo |
|------|------|-------|
| TradeMenu | `trade/TradeMenu.java` | GUI server |
| TradeScreen | `trade/TradeScreen.java` | GUI client |
| TradePacketHandler | `trade/TradePacketHandler.java` | Handler server |
| TradeClientHandler | `client/TradeClientHandler.java` | Handler client |

### Packet Flow
```
TradeRequestPacket (C2S) → Broadcast
TradeResponsePacket (C2S) → Accept/Decline
TradeUpdatePacket (C2S) → Items update
TradeSyncPacket (S2C) → UI sync
TradeFinalConfirmPacket (C2S) → Confirm
TradeCompletePacket (S2C) → Done
TradeCancelPacket (C2S) → Cancel
```

### Configurazione
- `tradeCurrencyItems`: default ["minecraft:potato"]
- `tradeTaxRate`: default 0.1 (10%)

---

## 12. SISTEMA DATABASE

### File
`database/DatabaseManager.java` (206 linee)

### HikariCP Configuration
```java
maxPoolSize: 5
minIdle: 1
connectionTimeout: 10s
idleTimeout: 5m
maxLifetime: 10m
leakDetectionThreshold: 60s
testQuery: "SELECT 1"
```

### Tabelle
| Tabella | Colonne | Scopo |
|---------|---------|-------|
| transfer_tokens | player_uuid (PK), target_server, expiration_time | Token transfer |
| player_transfers | uuid+server_name (PK), from/to_server, serialized_data (LONGBLOB), pending, timestamp | Log transfer |
| dev_whitelist | uuid (PK), username, added_by, reason | Whitelist dev |

---

## 13. SISTEMA NETWORKING

### Packet S2C (Server → Client) - 22 tipi
| Packet | Dati |
|--------|------|
| StaminaSyncPacket | currentStamina, maxStamina, inCombat, bowLock |
| FatigueSyncPacket | fatigueTicks |
| DietSyncPacket | float[] values (6 categorie) |
| DietProfileSyncPacket | Map<ResourceLocation, DietProfile> |
| WeightConfigSyncPacket | itemWeights, thresholds, debuffs |
| FatigueWarningPacket | minutesLeft |
| RealmSyncPacket | List<RealmData>, fullSync flag |
| HierarchySyncPacket | Map<UUID, Integer>, ownerUUID, ownerName |
| ClaimOwnerSyncPacket | pos, ownerUUID |
| VideoScreenSyncPacket | screenId, dimension, corners, facing, url, state, volume |
| VideoScreenDeletePacket | screenId, dimension |
| ZoneMusicPacket | musicFile, loop, stop |
| MusicFileDataPacket | musicFile, data[], chunkIndex, totalChunks |
| TradeRequestPacket | requesterId, requesterName |
| TradeSyncPacket | otherPlayerItems, confirmed flags, tax |
| TradeCompletePacket | sessionId, success |
| OpenRaceGuiPacket | raceName |
| RequestNamePacket | needsName |
| SyncGateRestrictionsPacket | Set<Item> blocked |

### Packet C2S (Client → Server) - 11 tipi
| Packet | Dati |
|--------|------|
| MeleeSwingPacket | (empty) |
| UpdateHierarchyPacket | realmPos, targetUUID, rankLevel |
| SubmitNamePacket | chosenName |
| SelectRacePacket | (race selection) |
| SelectComponentPacket | componentId |
| TradeResponsePacket | requesterId, accepted |
| TradeUpdatePacket | items, confirmed |
| TradeFinalConfirmPacket | confirmed |
| TradeCancelPacket | (empty) |
| BattleInviteResponsePacket | inviterUuid, accepted |
| RequestMusicFilePacket | musicFile |

### Handler Principali
- `ClientPacketHandler.java` - Hub centrale client
- `TradePacketHandler.java` - Trade server-side
- `TradeClientHandler.java` - Trade client-side

---

## 14. CLIENT-SIDE

### Handler Principali
| File | Ruolo |
|------|-------|
| ClientPacketHandler | Hub centrale packet handling |
| ClientConnectionHandler | Login/logout/dimension change |
| TradeClientHandler | Trade UI handling |
| ZoneMusicPlayer | Music playback (591 linee) |
| ClientVideoScreenManager | Video screen management |

### HUD Overlays
- WeightHudOverlay
- StaminaHudOverlay
- RealmOverlay
- FatigueHudOverlay

### Renderers
- PietroBlockRenderer
- HotIronAnvilRenderer
- HotGoldAnvilRenderer
- HotCopperAnvilRenderer
- DiceEntityRenderer
- RealmBoundaryRenderer
- ClaimBoundaryRenderer
- VideoScreenRenderHandler

### GUI Screens
- PreLoginNameScreen
- RaceSelectionScreen
- TradeScreen / TradeRequestScreen
- ComponentSelectionScreen
- BattleInviteScreen
- DietaScreen
- ClaimScreen
- PietroScreen

---

## 15. INTEGRAZIONI ESTERNE

### GodEye (Tharidia Features)
**File:** `integration/GodEyeIntegration.java`
- Reflection su `com.THproject.tharidia_features.main.getGodEyeDatabase()`
- Metodo `updatePlayerClaims(UUID, int, String json)`
- Sync quando claim creato/modificato/rimosso
- **IMPORTANTE:** Operazioni asincrone con timeout 5s
  - `syncPlayerClaims()` - Async, non blocca mai
  - `syncPlayerClaimsBlocking()` - Solo per shutdown
  - ExecutorService daemon thread (nome: "GodEye-Sync-Thread")
  - Shutdown graceful con 3s timeout in onServerStopping

### Epic Fight
- Runtime detection via `ModList.get().isLoaded("epicfight")`
- Reflection-based event listener registration
- Eventi: ComboAttackEvent, DealDamageEvent, SkillConsumeEvent, SkillCastEvent

### Accessories Mod
- Reflection in WeightManager per calcolo peso slot accessories

### Tharidia Tweaks
- SyncGateRestrictionsPacket per RPG gates
- NameService reflection per display names

---

## 16. REGISTRAZIONI

### Attributi Custom (ModAttributes.java)
| Attributo | Range |
|-----------|-------|
| lama_corta_attack_damage | 0-2048 |
| lancia_attack_damage | 0-2048 |
| martelli_attack_damage | 0-2048 |
| mazze_attack_damage | 0-2048 |
| spade_2_mani_attack_damage | 0-2048 |
| asce_attack_damage | 0-2048 |
| socchi_attack_damage | 0-2048 |
| archi_attack_damage | 0-2048 |
| armi_da_fuoco_attack_damage | 0-2048 |

### Statistiche Custom (ModStats.java)
- lama_corta_kill, lancia_kill, martelli_kill, mazze_kill
- spade_2_mani_kill, asce_kill, socchi_kill, archi_kill, armi_da_fuoco_kill

### Items Registrati (16+)
- HOT_IRON, HOT_GOLD, HOT_COPPER
- PINZA (480 durabilità)
- LAMA_LUNGA, LAMA_CORTA, ELSA
- GOLD_LAMA_LUNGA, GOLD_LAMA_CORTA
- COPPER_LAMA_LUNGA, COPPER_LAMA_CORTA, COPPER_ELSA
- DICE
- BATTLE_GAUNTLET
- TRUST_CONTRACT (Contratto di Fiducia, stack 16)

### Blocchi Registrati (6)
- PIETRO, CLAIM
- HOT_IRON_MARKER, HOT_GOLD_MARKER, HOT_COPPER_MARKER

### Block Entities (6)
- PIETRO_BLOCK_ENTITY, CLAIM_BLOCK_ENTITY
- HOT_IRON_ANVIL_ENTITY, HOT_GOLD_ANVIL_ENTITY, HOT_COPPER_ANVIL_ENTITY

### Menu Types (5)
- CLAIM_MENU, PIETRO_MENU, COMPONENT_SELECTION_MENU
- TRADE_MENU, BATTLE_INVITE_MENU

---

## 17. CONFIGURAZIONI

### Config.java (Globale)
```java
// Debug
DEBUG_STAMINA = false
DEBUG_STAMINA_EPICFIGHT = false
DEBUG_REALM_SYNC = false
DEBUG_CLAIM_REGISTRY = false
DEBUG_BOUNDARY_RENDERING = false
DEBUG_PROTECTION_CHECKS = false

// Stamina Combat
STAMINA_COMBAT_REGEN_REDUCTION_ENABLED = true
STAMINA_COMBAT_REGEN_REDUCTION_PERCENT = 50

// Smithing
SMITHING_MIN_CYCLE_TIME = 0.5s
SMITHING_MAX_CYCLE_TIME = 2.0s
SMITHING_TOLERANCE = 0.3
SMITHING_CAN_LOSE_PIECE = true
SMITHING_MAX_FAILURES = 3

// Database
DATABASE_HOST = "127.0.0.1"
DATABASE_PORT = 3306
DATABASE_NAME = "tharidia_queue"
DATABASE_USERNAME = "tharidia_user"
DATABASE_PASSWORD = "changeme"
DATABASE_ENABLED = false

// Trade
TRADE_CURRENCY_ITEMS = ["minecraft:potato"]
TRADE_TAX_RATE = 0.1

// Server Transfer
SERVER_NAME = "main"
MAIN_SERVER_IP = "172.18.0.10:25772"
DEV_SERVER_IP = "172.18.0.10:25566"
```

### StaminaConfig.java (JSON datapack)
```json
{
  "maxStamina": 100.0,
  "baseRegenRate": 15.0,
  "sprintThreshold": 0.2,
  "combatTimeout": 7.0,
  "attack": {
    "baseCost": 15.0,
    "useWeaponWeight": true,
    "curveType": "quadratic",
    "coefficients": [0.03, 0.05, 0.92]
  },
  "bow": {
    "tensionThreshold": 0.4,
    "baseCost": 4.0,
    "consumptionRate": 8.0,
    "maxTensionTime": 1.0
  },
  "regen": {
    "delayAfterConsumption": 0.8
  }
}
```

### FatigueConfig.java (JSON datapack)
```json
{
  "max_fatigue_minutes": 40,
  "bed_rest_time_seconds": 60,
  "proximity_recovery_interval_seconds": 10,
  "proximity_recovery_amount_seconds": 60,
  "bed_proximity_range": 20.0,
  "warning_threshold_5_minutes": 5,
  "warning_threshold_1_minute": 1,
  "exhaustion_slowness_level": 3
}
```

---

## 18. RISORSE

### Struttura Asset
```
src/main/resources/assets/tharidiathings/
├── blockstates/
│   ├── claim.json
│   └── pietro.json
├── lang/
│   ├── en_us.json
│   └── it_it.json
├── models/
│   ├── block/ (20+ modelli)
│   └── item/ (40+ modelli)
└── textures/
    ├── block/
    ├── item/
    ├── entity/
    └── gui/
```

### Struttura Data
```
src/main/resources/data/tharidiathings/
├── diet_config/
│   └── default.json
├── weight_config/
│   └── default.json
├── crop_protection/
│   └── config.json
├── dimension/
│   └── character_creation.json
└── recipe/
    ├── pinza.json
    └── dice.json
```

---

## 19. PROBLEMI NOTI E FIX

### Fix Applicati (Recenti)
| Problema | Fix | File | Commit |
|----------|-----|------|--------|
| HashMap thread-safety | ConcurrentHashMap | StaminaHandler.java:46-55 | 4b2fd4c |
| FFmpeg process leak | Shutdown hook + daemon thread | VLCVideoPlayer.java | cedae34 |
| System.out.println spam | Rimosso debug | NametagVisibilityHandler.java | cedae34 |
| Diet reset to 0 | Fix initialization | DietHandler.java | 7e3f6d8 |
| Weight config sync | Sync packet system | WeightConfigSyncPacket | 1483ced |
| GodEye blocking server | Async executor + timeout 5s | GodEyeIntegration.java | - |
| Server shutdown hang | @SubscribeEvent + GodEye shutdown | TharidiaThings.java | - |
| Hardcoded Italian text | Component.translatable() | ClaimProtectionHandler.java, TrustContractItem.java | - |

### Problemi Noti (Non Risolti)
| Problema | Severity | Location | Note |
|----------|----------|----------|------|
| RealmSync ArrayList race | CRITICAL | ClientPacketHandler.syncedRealms | Usare synchronizedList |
| ZoneMusic activeDownloads | CRITICAL | ZoneMusicPlayer | HashMap non thread-safe |
| TradeSession item race | HIGH | TradePacketHandler | Sincronizzare item list |
| DB password plaintext | HIGH | Config.java | Usare env variables |

### Raccomandazioni Fix
```java
// 1. RealmSync - ClientPacketHandler.java
private static final List<PietroBlockEntity> syncedRealms =
    Collections.synchronizedList(new ArrayList<>());

// 2. ZoneMusic - ZoneMusicPlayer.java
private static final Map<String, DownloadState> activeDownloads =
    new ConcurrentHashMap<>();
```

---

## 20. CRONOLOGIA MODIFICHE

### Commit Recenti
```
4b2fd4c - Fix hasmap to concurrent (StaminaHandler)
cedae34 - feat(video): add shutdown hook for process cleanup
7e3f6d8 - fix diet reset to 0
1483ced - feat(weight): add weight config sync system
3a9c141 - feat(weight): implement weight config merging
```

### File Modificati (Non Committati)
```
M src/main/java/com/THproject/tharidia_things/client/ClientPacketHandler.java
M src/main/resources/assets/tharidiathings/textures/item/dice.png
M src/main/resources/data/tharidiathings/weight_config/default.json
```

---

## NOTE FINALI

Questo documento serve come riferimento completo per:
1. **Debugging**: Tracciare il flow dei dati attraverso i sistemi
2. **Versionamento**: Monitorare modifiche e potenziali regressioni
3. **Sviluppo**: Capire l'architettura prima di implementare nuove feature
4. **Code Review**: Verificare che modifiche non introducano problemi

**Aggiornare questo documento** ogni volta che:
- Viene aggiunto un nuovo sistema
- Vengono modificati packet di rete
- Vengono risolti bug critici
- Cambiano configurazioni importanti
