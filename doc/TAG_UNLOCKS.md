# Tag unlock (strumenti e abilità)

Questa mod supporta l’uso dei **tag del player** (i tag di Minecraft gestiti dal comando `/tag`) come meccanismo semplice per **sbloccare funzionalità**.

Nel codice attuale, i tag vengono già usati per applicare **modificatori di Stamina** (abilità passive) al giocatore.

## 1) Cosa sono questi tag

Sono stringhe salvate sul player (scoreboard tags), leggibili in gioco e lato server tramite:

- `player.getTags()` (server)
- comandi vanilla `/tag`

Esempio di tag: `skill:iron_lungs_1`

## 2) Dare o togliere un tag (admin)

In gioco (serve permessi):

```mcfunction
/tag <player> add skill:iron_lungs_1
/tag <player> remove skill:iron_lungs_1
/tag <player> list
```

La mod controlla i tag periodicamente e applica/rimuove automaticamente gli effetti associati.

## 3) Abilità passive Stamina tramite tag (implementato)

### Come funziona

- Un file datapack definisce una tabella “tag → modificatore Stamina”.
- Quando un player possiede quel tag, la mod applica il modificatore.
- Quando il tag viene rimosso, la mod rimuove il modificatore.

Riferimenti codice:
- lettura e applicazione tag: [TagModifierBridge.java](file:///c:/Users/franc/IdeaProjects/Tharidia_items/src/main/java/com/THproject/tharidia_things/stamina/TagModifierBridge.java)
- caricamento mapping da datapack: [StaminaTagMappingsLoader.java](file:///c:/Users/franc/IdeaProjects/Tharidia_items/src/main/java/com/THproject/tharidia_things/stamina/StaminaTagMappingsLoader.java)

### Dove mettere i file JSON (datapack)

La directory ascoltata è `stamina_tag_mappings`.

Percorso tipico in un datapack:

```
<world>/datapacks/<nomepack>/data/<namespace>/stamina_tag_mappings/<file>.json
```

Esempio pratico:

```
world/datapacks/my_pack/data/tharidiathings/stamina_tag_mappings/skills.json
```

Poi fai:

```mcfunction
/reload
```

### Struttura del file

Il JSON deve contenere un array `tagMappings`. Ogni mapping ha:

- `tagId` (stringa): il tag del player, identico a quello usato con `/tag`
- `modifierType` (stringa): nome enum del modificatore
- `value` (numero): valore del modificatore
- `priority` (opzionale, int): ordine di applicazione (default `0`)
- `isPercentage` (opzionale, boolean): se non specificato, viene dedotto dal nome del tipo (contiene `PERCENT`)

Esempio:

```json
{
  "tagMappings": [
    {
      "tagId": "skill:iron_lungs_1",
      "modifierType": "MAX_STAMINA_PERCENT",
      "value": 10.0,
      "priority": 0
    },
    {
      "tagId": "skill:quick_recovery_1",
      "modifierType": "REGEN_RATE_PERCENT",
      "value": 20.0,
      "priority": 0
    },
    {
      "tagId": "skill:efficient_attacks_1",
      "modifierType": "ATTACK_COST_PERCENT",
      "value": -10.0,
      "priority": 0
    }
  ]
}
```

### Tipi disponibili (modifierType)

Valori attuali:

- `MAX_STAMINA_FLAT`
- `MAX_STAMINA_PERCENT`
- `REGEN_RATE_FLAT`
- `REGEN_RATE_PERCENT`
- `CONSUMPTION_MULTIPLIER`
- `ATTACK_COST_PERCENT`
- `ROLL_COST_PERCENT`
- `BOW_TENSION_COST_PERCENT`
- `SPRINT_THRESHOLD_PERCENT`
- `REGEN_DELAY_FLAT`
- `BLOCK_REGEN_OVERRIDE`

## 4) Note operative

- I tag sono **case-sensitive** e devono combaciare esattamente con `tagId`.
- La stessa stringa `tagId` viene usata anche come `source` del modificatore: se togli il tag, la mod rimuove i modificatori con quella source.
- Se aggiungi un tag e non vedi effetti immediati, attendi ~1 secondo (il refresh non è per-tick).

