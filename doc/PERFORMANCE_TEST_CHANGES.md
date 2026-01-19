# Test Performance - Risoluzione Ridotta

## 🎯 Modifiche Applicate

### Problema Persistente
Nonostante tutte le ottimizzazioni precedenti, i problemi continuano:
- Frame che si spostano orizzontalmente
- Colori sfasati
- FPS bassissimi (1-2)

### Ipotesi
Il bottleneck è la **performance di upload texture** - `setPixelRGBA` chiamato 921,600 volte per frame (1280x720) è troppo lento.

### Test Applicato
Ridotto risoluzione da **1280x720** a **640x360** (75% meno pixel):

**VLCVideoPlayer.java**:
```java
// Prima: 1280x720 = 921,600 pixel
videoWidth = 1280;
videoHeight = 720;

// Dopo: 640x360 = 230,400 pixel (4x meno!)
videoWidth = 640;
videoHeight = 360;
```

**FFmpegStreamPlayer.java**:
```java
// Matching resolution
videoWidth = 640;
videoHeight = 360;

// FFmpeg scale filter
"-vf", "scale=640:360:force_original_aspect_ratio=decrease,pad=640:360:(ow-iw)/2:(oh-ih)/2:black"
```

### Logging Aggiunto
Aggiunto timing dettagliato per identificare il bottleneck:

```java
[VLC] Frame update timing: total=XXms (copy=XXms, upload=XXms, gpu=XXms)
```

Questo mostra:
- **copy**: Tempo per copiare buffer (dovrebbe essere ~0ms)
- **upload**: Tempo per `uploadPixelsDirect` (il sospetto bottleneck)
- **gpu**: Tempo per `texture.upload()` (upload GPU)

---

## 📊 Risultati Attesi

### Se il problema è la performance:
- ✅ Con 640x360 dovrebbe funzionare a 25-30 FPS
- ✅ Nessun tearing o shifting
- ✅ Colori stabili
- ✅ Log mostra `upload` time molto alto (>30ms)

### Se il problema NON è la performance:
- ❌ Continua ad avere gli stessi problemi anche a 640x360
- ❌ Log mostra `upload` time basso (<10ms)
- ❌ Il problema è altrove (sync, buffer, ecc.)

---

## 🔍 Prossimi Passi

### Scenario A: Funziona a 640x360
**Conclusione**: Il problema è performance di `setPixelRGBA`

**Soluzioni**:
1. Usare `sun.misc.Unsafe` per accesso diretto memoria
2. Usare JNI per copy nativa
3. Usare OpenGL texture upload diretto (glTexSubImage2D)
4. Mantenere risoluzione bassa (640x360 o 854x480)

### Scenario B: NON funziona nemmeno a 640x360
**Conclusione**: Il problema NON è performance

**Possibili cause**:
1. **Sync issue**: VLC e rendering non sincronizzati
2. **Buffer corruption**: Dati corrotti prima dell'upload
3. **Color format**: Conversione BGRA→ABGR sbagliata
4. **Stride**: Ancora problemi di stride nonostante il fix
5. **Thread safety**: Race condition nel double buffering

---

## 🧪 Come Testare

1. **Compila la mod** con le nuove modifiche
2. **Avvia Minecraft** e carica un video
3. **Osserva**:
   - FPS migliorati?
   - Tearing/shifting risolti?
   - Colori stabili?
4. **Controlla i log** per vedere i timing:
   ```
   [VLC] Frame update timing: total=XXms (copy=XXms, upload=XXms, gpu=XXms)
   ```

---

## 💡 Analisi Timing

### Timing Ideale (60 FPS)
```
total < 16ms
copy < 1ms
upload < 10ms
gpu < 5ms
```

### Timing Problematico
```
total > 50ms  ← Causa FPS bassi
upload > 30ms ← setPixelRGBA è il bottleneck
```

### Esempio Reale (ipotetico)
```
[VLC] Frame update timing: total=45ms (copy=0ms, upload=42ms, gpu=3ms)
                                                    ^^^^
                                                    BOTTLENECK!
```

Questo confermerebbe che `setPixelRGBA` è il problema.

---

## 🔧 Ottimizzazioni Future

### Se upload è il bottleneck:

#### Opzione 1: sun.misc.Unsafe (più veloce)
```java
import sun.misc.Unsafe;

// Get Unsafe instance
Field f = Unsafe.class.getDeclaredField("theUnsafe");
f.setAccessible(true);
Unsafe unsafe = (Unsafe) f.get(null);

// Direct memory copy
long address = ... // NativeImage memory address
unsafe.copyMemory(pixels, 0, null, address, pixels.length * 4);
```

#### Opzione 2: JNI Native Copy
```c
// C code
JNIEXPORT void JNICALL Java_..._copyPixels(JNIEnv *env, jclass cls, jintArray src, jlong dst) {
    jint *pixels = (*env)->GetIntArrayElements(env, src, NULL);
    memcpy((void*)dst, pixels, len * sizeof(jint));
    (*env)->ReleaseIntArrayElements(env, src, pixels, JNI_ABORT);
}
```

#### Opzione 3: OpenGL Direct Upload
```java
// Upload directly to GPU texture
GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, width, height, 
    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixelBuffer);
```

---

## 📝 Note Importanti

### Risoluzione Temporanea
La risoluzione 640x360 è **SOLO PER TEST**. Una volta identificato il problema:
- Se performance: implementare soluzione ottimizzata
- Se altro: ripristinare 1280x720 e fixare la vera causa

### Qualità Visiva
640x360 è accettabile per:
- ✅ Test e debug
- ✅ Stream di bassa qualità
- ✅ Schermi piccoli in-game

Ma NON per:
- ❌ Video HD
- ❌ Schermi grandi
- ❌ Produzione finale

---

## 🎯 Obiettivo Finale

**Target**: 1920x1080 @ 30 FPS con zero tearing

**Requisiti**:
- Upload texture < 10ms per frame
- Total frame time < 33ms (30 FPS)
- Zero buffer corruption
- Perfect color reproduction

**Attuale**: 640x360 @ ?? FPS (da testare)

---

**TESTA ORA E CONDIVIDI I LOG!** 📊

Cerca nel log righe tipo:
```
[VLC] Frame update timing: total=XXms (copy=XXms, upload=XXms, gpu=XXms)
```

Questo ci dirà esattamente dove sta il problema!
