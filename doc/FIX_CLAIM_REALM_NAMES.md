# Fix Claim e Realm - Nomi e Storage

## 🎯 Problemi Risolti

### 1. ✅ Rimosso `/claim rent`
- **Problema**: Il comando `/claim rent` non aveva senso perché i claim sono già tuoi quando li piazzi
- **Soluzione**: Comando completamente rimosso da `ClaimCommands.java`
- **Risultato**: I giocatori non possono più affittare claim, devono solo pagarli con patate

### 2. ✅ Nomi "Unknown" nei Realm
- **Problema**: Quando un giocatore si allontanava dal regno, il suo nome diventava "Unknown" anche se il proprietario del regno rimaneva vicino
- **Causa**: Il nome veniva salvato come nickname Minecraft invece del nome scelto dal giocatore, e quando il chunk veniva scaricato, il nome veniva perso
- **Soluzione**: 
  - Creato `PlayerNameHelper.java` che usa reflection per chiamare `NameService.getChosenName()` da `tharidia_tweaks`
  - Modificato `PietroBlock.setPlacedBy()` per salvare il nome scelto invece del nickname
  - Modificato `ClaimBlockEntity` per salvare e caricare `ownerName` dal NBT
  - Aggiunto campo `ownerName` a `ClaimRegistry.ClaimData` per storage persistente

### 3. ✅ Protezione Claim Rafforzata
- **Problema**: Giocatori potevano aprire casse in claim non loro dopo essersi allontanati e tornati
- **Causa**: Il sistema di protezione si basava solo su `ClaimBlockEntity` che poteva essere scaricato
- **Soluzione**: 
  - Aggiunto campo `ownerName` a `ClaimBlockEntity` salvato in NBT
  - Migrazione automatica per claim vecchi senza `ownerName`
  - Storage persistente nel `ClaimRegistry` con nome del proprietario
  - Il nome viene sempre caricato dal NBT o recuperato da `NameService` se mancante

### 4. ✅ GUI Realm con Nomi Scelti
- **Problema**: La GUI del regno mostrava i nickname Minecraft invece dei nomi scelti
- **Soluzione**: 
  - Il campo `ownerName` in `PietroBlockEntity` ora contiene il nome scelto
  - Viene sincronizzato al client tramite `RealmSyncPacket`
  - La GUI mostra automaticamente il nome corretto

---

## 📁 File Creati

### `PlayerNameHelper.java` (NUOVO)
**Path**: `src/main/java/com/tharidia/tharidia_things/util/PlayerNameHelper.java`

**Funzione**: Helper per ottenere il nome scelto dal giocatore usando `NameService` di `tharidia_tweaks`

**Metodi**:
- `getChosenName(ServerPlayer player)` - Ottiene nome scelto per giocatore online
- `getChosenNameByUUID(UUID playerUUID, MinecraftServer server)` - Ottiene nome per giocatore offline

**Fallback**: Se `tharidia_tweaks` non è disponibile, usa il nickname Minecraft

---

## 📝 File Modificati

### 1. `ClaimCommands.java`
**Modifiche**:
- ❌ Rimosso comando `/claim rent <giorni>`
- ✅ Mantenuti solo comandi essenziali per giocatori

**Prima**:
```java
.then(Commands.literal("rent")
    .then(Commands.argument("days", IntegerArgumentType.integer(1, 365))
        .executes(ClaimCommands::executeRent)))
```

**Dopo**: Comando completamente rimosso

---

### 2. `PietroBlock.java`
**Modifiche**:
- ✅ Usa `PlayerNameHelper.getChosenName()` invece di `player.getName().getString()`
- ✅ Salva il nome scelto nel `PietroBlockEntity`

**Prima**:
```java
pietroBlockEntity.setOwner(player.getName().getString(), player.getUUID());
```

**Dopo**:
```java
String chosenName = com.THproject.tharidia_things.util.PlayerNameHelper.getChosenName(serverPlayer);
pietroBlockEntity.setOwner(chosenName, serverPlayer.getUUID());
```

---

### 3. `ClaimBlockEntity.java`
**Modifiche**:
- ✅ Aggiunto campo `private String ownerName`
- ✅ Salvato/caricato in NBT
- ✅ Migrazione automatica per claim vecchi
- ✅ Usa `PlayerNameHelper` per ottenere nome scelto

**Campi Aggiunti**:
```java
private String ownerName = ""; // Chosen name from NameService
```

**Metodi Aggiunti**:
```java
public String getOwnerName() {
    return ownerName;
}

public void setOwnerName(String ownerName) {
    this.ownerName = ownerName;
    setChanged();
}
```

**NBT Save**:
```java
tag.putString("OwnerName", ownerName);
```

**NBT Load con Migrazione**:
```java
if (tag.contains("OwnerName")) {
    ownerName = tag.getString("OwnerName");
} else if (ownerUUID != null && level instanceof ServerLevel serverLevel) {
    // Migrate old claims without ownerName
    ownerName = PlayerNameHelper.getChosenNameByUUID(ownerUUID, serverLevel.getServer());
}
```

**setOwnerUUID() Modificato**:
```java
// Automatically set claim name and owner name using chosen name from NameService
if (level instanceof ServerLevel serverLevel) {
    ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(ownerUUID);
    if (player != null) {
        this.ownerName = PlayerNameHelper.getChosenName(player);
        this.claimName = this.ownerName + "'s Claim";
    } else {
        // Player is offline, try to get from NameService storage
        this.ownerName = PlayerNameHelper.getChosenNameByUUID(ownerUUID, serverLevel.getServer());
        this.claimName = this.ownerName + "'s Claim";
    }
}
```

---

### 4. `ClaimRegistry.java`
**Modifiche**:
- ✅ Aggiunto campo `ownerName` a `ClaimData`
- ✅ Storage persistente del nome del proprietario

**ClaimData Prima**:
```java
public ClaimData(BlockPos position, UUID ownerUUID, String claimName, long creationTime, String dimension)
```

**ClaimData Dopo**:
```java
public ClaimData(BlockPos position, UUID ownerUUID, String claimName, String ownerName, long creationTime, String dimension)
```

**Metodo Aggiunto**:
```java
public String getOwnerName() {
    return ownerName;
}
```

**registerClaim() Modificato**:
```java
ClaimData data = new ClaimData(
    pos,
    claim.getOwnerUUID(),
    claim.getClaimName(),
    claim.getOwnerName(),  // ← NUOVO
    claim.getCreationTime(),
    dimension
);
```

---

## 🔧 Come Funziona

### Flusso Nome Scelto - Piazzamento Realm

1. Giocatore piazza blocco Pietro
2. `PietroBlock.setPlacedBy()` viene chiamato
3. `PlayerNameHelper.getChosenName(serverPlayer)` ottiene nome da `NameService`
4. Nome salvato in `PietroBlockEntity.ownerName`
5. Nome salvato in NBT quando chunk viene scaricato
6. Nome sincronizzato al client via `RealmSyncPacket`
7. GUI mostra nome scelto invece di nickname

### Flusso Nome Scelto - Piazzamento Claim

1. Giocatore piazza blocco Claim
2. `ClaimBlock.use()` chiama `setOwnerUUID()`
3. `setOwnerUUID()` usa `PlayerNameHelper.getChosenName()`
4. Nome salvato in `ClaimBlockEntity.ownerName`
5. Nome salvato in NBT
6. Nome registrato in `ClaimRegistry.ClaimData`
7. Quando chunk viene ricaricato, nome viene letto da NBT
8. Se NBT non ha nome (claim vecchio), viene recuperato da `NameService`

### Protezione Claim - Flusso Completo

1. Giocatore tenta di interagire con blocco
2. `ClaimProtectionHandler.onRightClickBlock()` viene chiamato
3. `findClaimForPosition()` cerca claim nel `ClaimRegistry`
4. `ClaimRegistry` restituisce `ClaimData` con `ownerUUID` e `ownerName`
5. Se chunk è caricato, `ClaimBlockEntity` viene letto
6. `canPlayerInteract()` verifica se giocatore è owner o trusted
7. Se non autorizzato, evento viene cancellato

**Importante**: Anche se il chunk viene scaricato, `ClaimRegistry` mantiene i dati del claim in memoria e su disco, quindi la protezione funziona sempre.

---

## 🧪 Test da Eseguire

### Test 1: Nome Scelto nel Realm
1. Scegli un nome custom tramite `NameService`
2. Piazza un blocco Pietro
3. Verifica che la GUI mostri il nome scelto, non il nickname
4. Allontanati dal regno (scarica chunk)
5. Torna vicino al regno
6. **Risultato atteso**: Nome ancora corretto, non "Unknown"

### Test 2: Nome Scelto nel Claim
1. Piazza un claim
2. Verifica con `/claim info` che mostri il nome scelto
3. Allontanati (scarica chunk)
4. Torna vicino
5. Verifica di nuovo con `/claim info`
6. **Risultato atteso**: Nome sempre corretto

### Test 3: Protezione Claim Persistente
1. Giocatore A piazza claim
2. Giocatore B prova ad aprire cassa nel claim
3. **Risultato atteso**: Bloccato
4. Giocatore A si allontana (scarica chunk)
5. Giocatore B prova di nuovo
6. **Risultato atteso**: Ancora bloccato
7. Giocatore A torna vicino
8. Giocatore B prova di nuovo
9. **Risultato atteso**: Ancora bloccato

### Test 4: Migrazione Claim Vecchi
1. Carica un mondo con claim vecchi (senza `ownerName`)
2. Verifica con `/claim info`
3. **Risultato atteso**: Nome recuperato automaticamente da `NameService`

### Test 5: Comando Rent Rimosso
1. Prova `/claim rent 30`
2. **Risultato atteso**: Comando non trovato

---

## 📊 Compatibilità

### Dipendenze
- ✅ **Richiede**: `tharidia_tweaks` mod con `NameService`
- ✅ **Fallback**: Se `tharidia_tweaks` non disponibile, usa nickname Minecraft

### Migrazione
- ✅ **Claim vecchi**: Migrazione automatica al caricamento
- ✅ **Realm vecchi**: Migrazione automatica al caricamento
- ✅ **Nessuna perdita dati**: Tutti i dati esistenti vengono preservati

### Retrocompatibilità
- ✅ **NBT**: Nuovi campi aggiunti, vecchi campi preservati
- ✅ **Registry**: Nuovi campi aggiunti, vecchi dati preservati
- ✅ **Comandi**: `/claim rent` rimosso (breaking change)

---

## 🚀 Deploy

### Build
```bash
./gradlew build
```

### JAR Location
```
build/libs/tharidiathings-1.1.4.jar
```

### Installazione
1. Assicurati che `tharidia_tweaks` sia installato
2. Sostituisci il JAR su entrambi i server (lobby e main)
3. Riavvia i server
4. I claim e realm vecchi verranno migrati automaticamente

---

## 📝 Note Tecniche

### Reflection per NameService
Il codice usa reflection per chiamare `NameService` da `tharidia_tweaks`:

```java
Class<?> nameServiceClass = Class.forName("com.THproject.tharidia_tweaks.name.NameService");
Method getChosenNameMethod = nameServiceClass.getMethod("getChosenName", ServerPlayer.class);
String chosenName = (String) getChosenNameMethod.invoke(null, player);
```

Questo permette di:
- ✅ Non avere dipendenza hard su `tharidia_tweaks`
- ✅ Fallback automatico se mod non disponibile
- ✅ Nessun crash se `NameService` cambia API

### Storage Triplo
I nomi sono salvati in 3 posti per massima affidabilità:

1. **NBT** (`ClaimBlockEntity`/`PietroBlockEntity`) - Persistente su disco
2. **Memory** (`ClaimRegistry`) - Veloce accesso in runtime
3. **NameService** (`tharidia_tweaks`) - Source of truth

Questo garantisce che il nome sia sempre disponibile anche se:
- Chunk scaricato
- Server riavviato
- Giocatore offline

---

## ✅ Checklist Completa

- [x] Rimosso `/claim rent`
- [x] Creato `PlayerNameHelper.java`
- [x] Modificato `PietroBlock.java` per usare nome scelto
- [x] Aggiunto `ownerName` a `ClaimBlockEntity`
- [x] Salvato `ownerName` in NBT
- [x] Aggiunto migrazione automatica claim vecchi
- [x] Aggiunto `ownerName` a `ClaimRegistry.ClaimData`
- [x] Modificato `registerClaim()` per salvare nome
- [x] Build riuscita
- [x] Documentazione completa

---

## 🎉 Risultato Finale

- ✅ **Nomi sempre corretti**: GUI e comandi mostrano nomi scelti, non nickname
- ✅ **Nessun "Unknown"**: Nomi persistenti anche con chunk scaricati
- ✅ **Protezione rafforzata**: Claim sempre protetti, storage triplo
- ✅ **Migrazione automatica**: Claim e realm vecchi aggiornati automaticamente
- ✅ **Comando rent rimosso**: Sistema più semplice e coerente
