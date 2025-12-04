# Windows Setup Guide - Tharidia Things Video Screens

This mod requires external tools to play YouTube and Twitch videos.

## 🚀 Automatic Installation (Recommended)

**The mod will automatically detect missing tools and offer to download them!**

1. Launch Minecraft with the mod installed
2. If tools are missing, a setup screen will appear
3. Click **"Install All Dependencies"**
4. For FFmpeg, a CMD window will open - press **Y** to accept
5. Wait for downloads to complete
6. Done! The screen will close automatically

All tools will be installed in your `.minecraft/bin` folder.

---

## 📋 Manual Installation (Alternative)

If automatic installation fails, follow these steps:

### Required Tools

### 1. FFmpeg (Required for all videos)
FFmpeg handles video decoding and rendering.

**Installation:**
1. Download FFmpeg from: https://www.gyan.dev/ffmpeg/builds/
   - Choose **"ffmpeg-release-essentials.zip"**
2. Extract the ZIP file to `C:\ffmpeg`
3. Add to Windows PATH:
   - Press `Win + R`, type `sysdm.cpl`, press Enter
   - Go to **"Advanced"** tab → **"Environment Variables"**
   - Under **"System variables"**, find `Path` → Click **"Edit"**
   - Click **"New"** → Add: `C:\ffmpeg\bin`
   - Click **OK** on all windows
4. **Restart your computer** (or at least restart Minecraft)

**Quick Test:**
Open Command Prompt (`Win + R` → `cmd`) and type:
```
ffmpeg -version
```
You should see version information.

---

### 2. yt-dlp (Required for YouTube videos)
yt-dlp extracts direct video URLs from YouTube.

**Installation (Easy Method):**
1. Download `yt-dlp.exe` from: https://github.com/yt-dlp/yt-dlp/releases/latest
2. Place it in one of these locations:
   - **Option A (Recommended):** Your Minecraft installation folder
     - Example: `C:\Users\YourName\AppData\Roaming\.minecraft\yt-dlp.exe`
   - **Option B:** Same folder as FFmpeg: `C:\ffmpeg\bin\yt-dlp.exe`
   - **Option C:** Add to PATH (same steps as FFmpeg)

**Quick Test:**
```
yt-dlp --version
```

---

### 3. Streamlink (Required for Twitch streams)
Streamlink extracts live stream URLs from Twitch.

**Installation (Installer - Recommended):**
1. Download the Windows installer from: https://streamlink.github.io/install.html#windows
2. Run the installer (it will automatically add to PATH)
3. Restart Minecraft

**Installation (Portable):**
1. Download the portable version from the same link
2. Extract and place `streamlink.exe` in your Minecraft folder or FFmpeg bin folder

**Quick Test:**
```
streamlink --version
```

---

## File Locations Summary

The mod will automatically search for executables in these locations (in order):

1. **Current Minecraft folder** (where you launch the game)
2. `C:\Users\YourName\AppData\Roaming\.minecraft\`
3. `C:\Program Files\yt-dlp\` or `C:\Program Files\streamlink\`
4. Python Scripts folder (if installed via pip)
5. Windows PATH

**Recommended Setup:**
```
C:\ffmpeg\bin\
├── ffmpeg.exe
├── ffplay.exe
├── yt-dlp.exe
└── streamlink.exe
```
Then add `C:\ffmpeg\bin` to your PATH.

---

## Troubleshooting

### "Failed to extract stream URL"
- Check the Minecraft logs for detailed error messages
- The mod will print installation instructions if tools are missing
- Make sure you restarted Minecraft after installing the tools

### "Screen stays black"
- Verify all three tools are installed and working (run the Quick Tests above)
- Check if the video/stream is actually available (try opening it in a browser)
- Look for error messages in `logs/latest.log`

### "Command not found" errors
- You didn't add the tools to PATH, or
- You didn't restart your computer/Minecraft after adding to PATH

---

## Alternative: Python Installation

If you have Python installed, you can use pip instead:

```cmd
pip install yt-dlp streamlink
```

This will install both tools and add them to PATH automatically.

---

## Need Help?

If you're still having issues:
1. Check `logs/latest.log` for detailed error messages
2. The mod prints helpful installation instructions when tools are missing
3. Make sure you're using the latest version of the mod
