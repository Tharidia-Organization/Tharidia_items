# Fix Menu Selezione Rango - GUI Pietro

## 🎯 Problema Risolto

### Problema Originale
Quando il proprietario del regno cliccava sul pulsante "Cambia" per modificare il ruolo di un altro giocatore, il menu di selezione rango non appariva o appariva in una posizione sbagliata.

### Causa
Il menu era posizionato con coordinate fisse hardcoded (`leftPos + 5`, `topPos + 5`) invece di essere posizionato dinamicamente sotto al pulsante cliccato.

---

## ✅ Soluzione Implementata

### 1. **Posizionamento Dinamico del Menu**
- Il menu ora viene posizionato **direttamente sotto** al pulsante "Cambia" cliccato
- Coordinate calcolate dinamicamente al momento del click
- Salvate in variabili di istanza `rankMenuX` e `rankMenuY`

### 2. **Protezione Bordi Schermo**
- Se il menu esce dallo schermo a destra, viene spostato a sinistra
- Se il menu esce dallo schermo in basso, viene mostrato **sopra** il pulsante invece che sotto

### 3. **Miglioramenti Visivi**
- Aggiunto effetto ombra al menu
- Aggiunto highlight al passaggio del mouse sulle opzioni
- Migliorato contrasto colori per migliore leggibilità

---

## 📝 Modifiche al Codice

### File: `PietroScreen.java`

#### 1. Aggiunte Variabili di Istanza
```java
private int rankMenuX = 0;
private int rankMenuY = 0;
```
Salvano la posizione del menu quando viene aperto.

#### 2. Metodo `renderRankSelectionMenu()` Modificato

**Prima**:
```java
int menuX = this.leftPos + 90;  // Posizione fissa
int menuY = this.topPos + 90;   // Posizione fissa
```

**Dopo**:
```java
// Use stored menu position (set when button is clicked)
int menuX = rankMenuX;
int menuY = rankMenuY;
```

**Miglioramenti Visivi Aggiunti**:
```java
// Background with shadow effect
guiGraphics.fill(menuX + 2, menuY + 2, menuX + menuWidth + 2, menuY + menuHeight + 2, 0x88000000); // Shadow
guiGraphics.fill(menuX, menuY, menuX + menuWidth, menuY + menuHeight, 0xEE222222); // Dark background
guiGraphics.renderOutline(menuX, menuY, menuWidth, menuHeight, 0xFFAAAAAA); // Light border
```

**Highlight al Hover**:
```java
// Highlight on hover
int relMouseX = (int)(Minecraft.getInstance().mouseHandler.xpos() * ...);
int relMouseY = (int)(Minecraft.getInstance().mouseHandler.ypos() * ...);

if (relMouseX >= menuX && relMouseX <= menuX + menuWidth && 
    relMouseY >= optionY && relMouseY <= optionY + 15) {
    guiGraphics.fill(menuX + 2, optionY, menuX + menuWidth - 2, optionY + 14, 0x55FFFFFF);
}
```

#### 3. Metodo `mouseClicked()` - Calcolo Posizione Dinamica

**Prima**:
```java
// Open rank selection menu
selectedPlayerForRankChange = entry.playerUUID;
showRankSelectionMenu = true;
return true;
```

**Dopo**:
```java
// Open rank selection menu positioned right below the button
selectedPlayerForRankChange = entry.playerUUID;
showRankSelectionMenu = true;

// Position menu directly below the clicked button
rankMenuX = buttonX - 20; // Slightly to the left to center it better
rankMenuY = buttonY + buttonHeight + 2; // Just below the button

// Make sure menu doesn't go off screen
int menuWidth = 80;
int menuHeight = 90;
if (rankMenuX + menuWidth > this.leftPos + this.imageWidth) {
    rankMenuX = this.leftPos + this.imageWidth - menuWidth - 5;
}
if (rankMenuY + menuHeight > this.topPos + this.imageHeight) {
    rankMenuY = buttonY - menuHeight - 2; // Show above if no space below
}

return true;
```

#### 4. Aggiornato Click Detection del Menu

**Prima**:
```java
int menuX = this.leftPos + 5; //TODO fix the values
int menuY = this.topPos + 5;
```

**Dopo**:
```java
// Use stored menu position
int menuX = rankMenuX;
int menuY = rankMenuY;
```

---

## 🎨 Miglioramenti Visivi

### 1. Effetto Ombra
- Ombra nera semi-trasparente offset di 2 pixel
- Dà profondità al menu

### 2. Sfondo Scuro
- Background `0xEE222222` (grigio molto scuro, quasi opaco)
- Migliore contrasto con il testo

### 3. Bordo Chiaro
- Bordo `0xFFAAAAAA` (grigio chiaro)
- Definisce meglio i confini del menu

### 4. Highlight Interattivo
- Highlight bianco semi-trasparente `0x55FFFFFF` al passaggio del mouse
- Feedback visivo immediato

### 5. Testo Bianco
- Cambiato da `0x404040` a `0xFFFFFF`
- Migliore leggibilità su sfondo scuro

---

## 🧪 Come Testare

### Test 1: Posizionamento Base
1. Apri GUI Pietro (tab Rivendicazioni)
2. Clicca su "Cambia" per un giocatore
3. **Risultato atteso**: Menu appare direttamente sotto al pulsante

### Test 2: Protezione Bordo Destro
1. Clicca su "Cambia" per un giocatore in fondo alla lista (vicino al bordo destro)
2. **Risultato atteso**: Menu si sposta a sinistra per rimanere visibile

### Test 3: Protezione Bordo Inferiore
1. Clicca su "Cambia" per un giocatore in fondo alla lista (vicino al bordo inferiore)
2. **Risultato atteso**: Menu appare sopra il pulsante invece che sotto

### Test 4: Highlight Interattivo
1. Apri menu selezione rango
2. Muovi il mouse sulle opzioni
3. **Risultato atteso**: Opzione sotto il mouse si illumina

### Test 5: Selezione Rango
1. Apri menu
2. Clicca su un rango (es. "Guardia")
3. **Risultato atteso**: Menu si chiude, rango del giocatore aggiornato

### Test 6: Chiusura Menu
1. Apri menu
2. Clicca fuori dal menu
3. **Risultato atteso**: Menu si chiude senza modificare nulla

---

## 📊 Coordinate e Dimensioni

### Pulsante "Cambia"
- **X**: `leftPos + 185`
- **Y**: `yPos - 2` (calcolato dinamicamente per ogni giocatore)
- **Larghezza**: `40`
- **Altezza**: `12`

### Menu Selezione Rango
- **Larghezza**: `80`
- **Altezza**: `90`
- **X**: `buttonX - 20` (centrato rispetto al pulsante)
- **Y**: `buttonY + buttonHeight + 2` (sotto al pulsante)

### Protezioni Bordi
- **Bordo Destro**: Se `menuX + menuWidth > leftPos + imageWidth`, sposta a sinistra
- **Bordo Inferiore**: Se `menuY + menuHeight > topPos + imageHeight`, mostra sopra

---

## 🎯 Risultato Finale

### Prima
- ❌ Menu non appariva o appariva in posizione fissa sbagliata
- ❌ Nessun feedback visivo
- ❌ Poteva uscire dallo schermo

### Dopo
- ✅ Menu appare sempre sotto al pulsante cliccato
- ✅ Highlight al passaggio del mouse
- ✅ Effetto ombra e migliore contrasto
- ✅ Protezione bordi schermo automatica
- ✅ Posizionamento sopra se non c'è spazio sotto

---

## 📦 Deploy

**JAR**: `build/libs/tharidiathings-1.1.4.jar`

**Installazione**:
1. Sostituisci JAR sul server
2. Riavvia
3. Testa con `/thqueueadmin lobbymode on` (se in lobby) o apri GUI Pietro

---

## ✅ Checklist Completa

- [x] Aggiunte variabili `rankMenuX` e `rankMenuY`
- [x] Modificato `renderRankSelectionMenu()` per usare posizione dinamica
- [x] Aggiunto calcolo posizione in `mouseClicked()`
- [x] Aggiunta protezione bordi schermo
- [x] Aggiunto effetto ombra
- [x] Aggiunto highlight al hover
- [x] Migliorato contrasto colori
- [x] Aggiornato click detection
- [x] Build riuscita
- [x] Documentazione completa

---

## 🎉 Conclusione

Il menu di selezione rango ora:
- Si apre sempre nella posizione corretta
- È visivamente più accattivante
- Fornisce feedback interattivo
- Non esce mai dallo schermo
- È facile da usare

Il proprietario del regno può ora cambiare i ruoli dei giocatori senza problemi!
