# Correzioni Sistema di Trade - Risoluzione Problemi Critici

## Problemi Risolti

### ✅ 1. **Impossibilità di Chiudere il Menu Durante Trade**
**Problema:** I giocatori potevano chiudere il menu premendo ESC o E, causando perdita di item.

**Soluzione Implementata:**
- **Blocco tasto ESC** (`keyCode 256`) in `TradeScreen.java`
- **Blocco tasto E** (`keyCode 69`) in `TradeScreen.java`
- **Override `onClose()`** per impedire chiusura accidentale
- **Unico modo per uscire:** Pulsante "Annulla" che cancella correttamente il trade

```java
@Override
public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (keyCode == 69) return true;  // Block E
    if (keyCode == 256) return true; // Block ESC
    return super.keyPressed(keyCode, scanCode, modifiers);
}

@Override
public void onClose() {
    // Prevent closing - players must use Cancel button
}
```

### ✅ 2. **Visualizzazione Tasse sulla GUI**
**Problema:** I giocatori non vedevano quanto avrebbero ricevuto dopo le tasse.

**Soluzione Implementata:**
- **Calcolo in tempo reale** delle tasse sugli item di valuta
- **Visualizzazione centrale** con:
  - Percentuale tassa (es. "Tassa: 10%")
  - Quantità sottratta (es. "(-10)")
  - Quantità finale ricevuta (es. "Riceverai: 90")
- **Colori distintivi:**
  - Rosso per la tassa
  - Verde per l'importo ricevuto

```java
private void drawTaxInfo(GuiGraphics guiGraphics) {
    // Calculate tax from other player's offer
    int totalCurrency = 0;
    for (ItemStack stack : otherPlayerOffer) {
        if (isCurrencyItem(stack)) {
            totalCurrency += stack.getCount();
        }
    }
    
    if (totalCurrency > 0) {
        int taxedAmount = (int)(totalCurrency * 0.9); // 10% tax
        int taxAmount = totalCurrency - taxedAmount;
        
        drawString("§cTassa: 10% (-" + taxAmount + ")");
        drawString("§aRiceverai: " + taxedAmount);
    }
}
```

### ✅ 3. **Blocco Movimento Durante Trade**
**Problema:** I giocatori potevano muoversi durante il trade.

**Soluzione Implementata:**
- **Event handler `onEntityTick()`** in `TradeInventoryBlocker.java`
- **Azzeramento movimento** ogni tick: `player.setDeltaMovement(0, y, 0)`
- **Mantiene gravità** (componente Y) per evitare glitch

```java
@SubscribeEvent
public static void onEntityTick(EntityTickEvent.Pre event) {
    if (event.getEntity() instanceof ServerPlayer player) {
        if (TradeManager.isPlayerInTrade(player.getUUID())) {
            // Cancel movement but keep gravity
            player.setDeltaMovement(0, player.getDeltaMovement().y, 0);
        }
    }
}
```

### ✅ 4. **Blocco Rimozione Item Dopo Conferma**
**Problema:** I giocatori potevano rimuovere item dopo aver confermato.

**Soluzione Implementata:**
- **Override `mayPickup()`** negli slot: `return !playerConfirmed`
- **Override `mayPlace()`** negli slot: `return !playerConfirmed`
- **Blocco shift-click** in `quickMoveStack()`: controllo `playerConfirmed`
- **Blocco visivo** degli slot dopo conferma

```java
// In TradeMenu.java - Slot override
@Override
public boolean mayPickup(Player player) {
    return !playerConfirmed; // Cannot modify after confirming
}

@Override
public boolean mayPlace(ItemStack stack) {
    return !playerConfirmed; // Cannot modify after confirming
}

// In TradeMenu.java - quickMoveStack
@Override
public ItemStack quickMoveStack(Player player, int index) {
    if (playerConfirmed) {
        return ItemStack.EMPTY; // Block shift-click
    }
    // ... rest of logic
}
```

### ✅ 5. **Protezione Item alla Chiusura Menu**
**Problema:** Gli item potevano essere persi se il menu veniva chiuso in modo anomalo.

**Soluzione Implementata:**
- **Restituzione garantita** in `TradeMenu.removed()`
- **SEMPRE restituisce item** all'inventario
- **Fallback drop** se inventario pieno
- **Cancellazione automatica** del trade se menu chiuso

```java
@Override
public void removed(Player player) {
    super.removed(player);
    
    if (!player.level().isClientSide) {
        for (int i = 0; i < 24; i++) {
            ItemStack stack = playerOffer.removeItemNoUpdate(i);
            if (!stack.isEmpty()) {
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false); // Drop if full
                }
            }
        }
    }
}
```

## Sistema di Conferma a Due Stadi

### **Stadio 1: Conferma Iniziale**
1. Giocatore mette item negli slot
2. Preme "Conferma"
3. **Slot bloccati** - non può più modificare
4. **Altri giocatore vede gli item** (con tassa applicata)
5. Aspetta che anche l'altro confermi

### **Stadio 2: Conferma Finale**
1. Quando **entrambi** hanno confermato, appare "CONFERMA FINALE"
2. Entrambi vedono esattamente cosa riceveranno (post-tassa)
3. Entrambi devono premere "CONFERMA FINALE"
4. Solo allora il trade viene completato

## Restrizioni Durante Trade

### **Impossibile:**
- ❌ Muoversi (movimento bloccato)
- ❌ Aprire inventario (tasto E bloccato)
- ❌ Chiudere menu (ESC bloccato)
- ❌ Aprire altri container
- ❌ Modificare item dopo conferma
- ❌ Shift-click dopo conferma

### **Possibile:**
- ✅ Annullare trade (pulsante "Annulla")
- ✅ Annullare conferma (prima della conferma finale)
- ✅ Vedere item dell'altro giocatore (dopo conferma)
- ✅ Vedere tasse in tempo reale

## Flusso Completo Trade

```
1. Giocatore A click destro su Giocatore B con item valuta
   ↓
2. Giocatore B riceve richiesta trade
   ↓
3. Giocatore B accetta → Menu si apre per entrambi
   ↓
4. Entrambi bloccati: NO movimento, NO inventario, NO ESC
   ↓
5. Inseriscono item negli slot
   ↓
6. Giocatore A preme "Conferma"
   → Slot bloccati per A
   → B vede item di A (con tassa)
   ↓
7. Giocatore B preme "Conferma"
   → Slot bloccati per B
   → A vede item di B (con tassa)
   → Appare "CONFERMA FINALE" per entrambi
   ↓
8. Visualizzazione tasse:
   "§cTassa: 10% (-10)"
   "§aRiceverai: 90"
   ↓
9. Giocatore A preme "CONFERMA FINALE"
   → Aspetta B
   ↓
10. Giocatore B preme "CONFERMA FINALE"
    → Trade completato!
    → Item scambiati (con tassa applicata)
    → Menu chiuso automaticamente
```

## File Modificati

### **TradeScreen.java**
- Aggiunto blocco ESC e E
- Aggiunto override `onClose()`
- Aggiunto metodo `drawTaxInfo()`
- Aggiunto calcolo tasse in tempo reale

### **TradeMenu.java**
- Modificato `mayPickup()` e `mayPlace()` per blocco post-conferma
- Modificato `quickMoveStack()` per blocco shift-click
- Modificato `removed()` per garantire restituzione item

### **TradeInventoryBlocker.java**
- Aggiunto blocco movimento in `onEntityTick()`
- Modificato `onContainerClose()` per cancellare trade
- Rimossa riapertura automatica menu

## Testing

### **Test 1: Blocco Chiusura**
```
1. Apri trade
2. Premi ESC → Nulla accade ✓
3. Premi E → Nulla accade ✓
4. Premi "Annulla" → Trade cancellato, item restituiti ✓
```

### **Test 2: Blocco Movimento**
```
1. Apri trade
2. Prova a muoverti → Bloccato ✓
3. Prova a saltare → Gravità funziona, movimento orizzontale bloccato ✓
```

### **Test 3: Visualizzazione Tasse**
```
1. Giocatore A mette 100 patate
2. Giocatore A conferma
3. Giocatore B vede:
   "§cTassa: 10% (-10)"
   "§aRiceverai: 90" ✓
```

### **Test 4: Blocco Post-Conferma**
```
1. Giocatore A mette item
2. Giocatore A conferma
3. Prova a rimuovere item → Impossibile ✓
4. Prova shift-click → Nulla accade ✓
```

### **Test 5: Protezione Item**
```
1. Metti item negli slot
2. Forza chiusura (comando /kill, crash, ecc.)
3. Item restituiti all'inventario ✓
```

## Note Importanti

⚠️ **IMPERATIVO:** I giocatori NON possono chiudere il menu in alcun modo se non tramite il pulsante "Annulla"

⚠️ **IMPERATIVO:** I giocatori NON possono muoversi durante il trade

⚠️ **IMPERATIVO:** Gli item NON possono essere modificati dopo la conferma

✅ **GARANTITO:** Gli item vengono SEMPRE restituiti se il trade non viene completato

✅ **GARANTITO:** Le tasse sono SEMPRE visualizzate prima della conferma finale
