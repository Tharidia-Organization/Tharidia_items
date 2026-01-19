# Stamina (guida utente + admin)

Questa mod introduce un sistema di **Stamina** usato principalmente per limitare le azioni “da combattimento” (in particolare gli attacchi).

## 1) Per i giocatori

### A cosa serve
- Ogni player ha una **Stamina corrente** e una **Stamina massima**.
- Gli **attacchi consumano Stamina**.
- Gli **archi consumano Stamina** mentre tendi la corda.
- Se non hai abbastanza Stamina, l’attacco viene **bloccato** (non colpisci).

### Come si consuma (attacchi)
Quando provi ad attaccare un’entità:
- viene calcolato un **costo**;
- se `staminaCorrente < costo`, l’attacco viene annullato.

Il costo dipende da:
- un costo base configurabile;
- opzionalmente dal **peso dell’arma** impugnata (armi più pesanti = costo più alto);
- eventuali modificatori (vedi “tag/abilità”).

### Come si consuma (archi)
Quando usi un arco:
- la Stamina viene consumata **progressivamente** mentre tendi la corda (dopo una piccola “grazia” iniziale);
- al rilascio della freccia viene applicato anche un costo base proporzionale alla potenza del tiro;
- se non hai abbastanza Stamina per continuare a tendere o per rilasciare, l’azione viene **interrotta/bloccata**.

Il costo può scalare col **peso dell’arco** e con i modificatori `CONSUMPTION_MULTIPLIER` e `BOW_TENSION_COST_PERCENT`.

### Come si rigenera
- La Stamina si rigenera **solo fuori dal combattimento**.
- Dopo un’azione che consuma Stamina, parte un piccolo **ritardo di rigenerazione** (delay) configurabile.
- La rigenerazione avviene in modo continuo (ogni tick) finché non raggiunge il massimo.

### Stato “In combattimento”
Quando sei “in combattimento”:
- la rigenerazione è bloccata;
- rimani in combattimento per un tempo configurabile dopo aver attaccato o subito danno.

In pratica: se fai scambi di colpi continui, la Stamina non risale finché non esci dal combattimento.

### Note pratiche
- Se ti sembra che “non attacchi”, di solito è perché la Stamina è sotto il costo richiesto.
- La Stamina è gestita server-side: anche in presenza di lag, è il server a decidere se l’attacco passa.

## 2) Per admin / tecnico

### Dove viene gestita
- Logica principale: [StaminaHandler.java](file:///c:/Users/franc/IdeaProjects/Tharidia_items/src/main/java/com/THproject/tharidia_things/event/StaminaHandler.java)
- Dati salvati sul player: [StaminaData.java](file:///c:/Users/franc/IdeaProjects/Tharidia_items/src/main/java/com/THproject/tharidia_things/stamina/StaminaData.java)
- Sync server → client: [StaminaSyncPacket.java](file:///c:/Users/franc/IdeaProjects/Tharidia_items/src/main/java/com/THproject/tharidia_things/network/StaminaSyncPacket.java)

La Stamina viene:
- consumata quando il player tenta un attacco su entità;
- rigenerata ogni tick quando il player è fuori combattimento;
- sincronizzata periodicamente al client (e su eventi importanti come login/variazioni).

### Configurazione via datapack (stamina_config)
La configurazione base della Stamina è caricata come JSON da datapack tramite la directory `stamina_config`.

Percorso tipico:

```
<world>/datapacks/<nomepack>/data/<namespace>/stamina_config/<file>.json
```

Esempio pratico:

```
world/datapacks/my_pack/data/tharidiathings/stamina_config/config.json
```

Dopo aver aggiunto/modificato i file:

```mcfunction
/reload
```

Riferimento codice loader: [StaminaConfig.java](file:///c:/Users/franc/IdeaProjects/Tharidia_items/src/main/java/com/THproject/tharidia_things/config/StaminaConfig.java)

### Struttura JSON
Campi supportati:

- `baseValues`
  - `maxStamina` (float): stamina massima base
  - `baseRegenRate` (float): stamina/secondo fuori combattimento
  - `sprintThreshold` (float 0..1): valore disponibile nei calcoli, non ancora usato per bloccare lo sprint
  - `combatTimeout` (float): secondi di permanenza in combattimento dopo eventi rilevanti
- `consumption.attacks`
  - `baseCost` (float): costo base di un attacco
  - `useWeaponWeight` (boolean): se true, il costo scala col peso arma
  - `curveType` (string): `linear` oppure `quadratic`
  - `coefficients` (array float): coefficienti della curva
- `consumption.bows`
  - `tensionThreshold` (float): secondi di grazia prima del consumo in tensione
  - `baseCost` (float): costo base al rilascio (scala con la potenza)
  - `consumptionRate` (float): costo in stamina/secondo mentre tendi (dopo la soglia)
  - `maxTensionTime` (float): tempo massimo usato per scalare il consumo (cap)
  - `useWeaponWeight` (boolean): se true, il costo scala col peso dell’arco
  - `curveType` (string): `linear` oppure `quadratic`
  - `coefficients` (array float): coefficienti della curva
- `regeneration`
  - `delayAfterConsumption` (float): secondi di delay dopo consumo prima di rigenerare

Esempio completo:

```json
{
  "baseValues": {
    "maxStamina": 100.0,
    "baseRegenRate": 15.0,
    "sprintThreshold": 0.2,
    "combatTimeout": 7.0
  },
  "consumption": {
    "attacks": {
      "baseCost": 15.0,
      "useWeaponWeight": true,
      "curveType": "quadratic",
      "coefficients": [0.03, 0.05, 0.92]
    },
    "bows": {
      "tensionThreshold": 0.4,
      "baseCost": 4.0,
      "consumptionRate": 8.0,
      "maxTensionTime": 1.0,
      "useWeaponWeight": true,
      "curveType": "quadratic",
      "coefficients": [0.03, 0.05, 0.92]
    }
  },
  "regeneration": {
    "delayAfterConsumption": 0.8
  }
}
```

### Dettaglio curva costo attacco (peso arma)
Se `useWeaponWeight` è true, viene calcolato un moltiplicatore in base al peso dell’item in mano:
- `linear`: `max(0, a * peso + b)`
- `quadratic`: `max(0, a * peso^2 + b * peso + c)`

Il peso arriva dal sistema peso: [WeightRegistry.java](file:///c:/Users/franc/IdeaProjects/Tharidia_items/src/main/java/com/THproject/tharidia_things/weight/WeightRegistry.java)

### Modificatori e abilità via tag (admin)
Oltre ai valori base, la mod supporta modificatori applicati al player tramite **tag** (vanilla `/tag`) e mapping datapack.

Guida dedicata: [TAG_UNLOCKS.md](file:///c:/Users/franc/IdeaProjects/Tharidia_items/doc/TAG_UNLOCKS.md)

In sintesi:
- assegni un tag al player (`/tag <player> add skill:...`)
- un datapack in `stamina_tag_mappings` mappa `tagId` → modificatore Stamina
- la mod aggiorna i modificatori circa una volta al secondo

Codice:
- refresh tag e applicazione: [TagModifierBridge.java](file:///c:/Users/franc/IdeaProjects/Tharidia_items/src/main/java/com/THproject/tharidia_things/stamina/TagModifierBridge.java)
- loader mapping: [StaminaTagMappingsLoader.java](file:///c:/Users/franc/IdeaProjects/Tharidia_items/src/main/java/com/THproject/tharidia_things/stamina/StaminaTagMappingsLoader.java)
- tipi modificatore: [StaminaModifierType.java](file:///c:/Users/franc/IdeaProjects/Tharidia_items/src/main/java/com/THproject/tharidia_things/stamina/StaminaModifierType.java)

### Cosa è effettivamente usato “oggi”
Nel codice attuale:
- consumo: solo sugli attacchi (AttackEntityEvent)
- consumo: archi (durante la tensione, PlayerTickEvent)
- rigenerazione: fuori combattimento, con delay
- combat state: entra in combat su attacco o quando il player subisce danno

Esistono altri `modifierType` (roll, bow tension, sprint threshold, ecc.), ma non tutte le meccaniche sono già collegate a un’azione in-game.

### Troubleshooting rapido
- Le modifiche a `stamina_config` e `stamina_tag_mappings` richiedono `/reload`.
- Se un player “non riesce ad attaccare”, controlla:
  - costo troppo alto (peso arma + curva + baseCost)
  - regen troppo bassa o combatTimeout troppo alto (stamina non risale tra un fight e l’altro)
