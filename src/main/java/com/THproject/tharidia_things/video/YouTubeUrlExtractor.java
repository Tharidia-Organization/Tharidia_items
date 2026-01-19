package com.THproject.tharidia_things.video;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.THproject.tharidia_things.TharidiaThings;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts direct video stream URLs from YouTube links
 * Uses yt-dlp style extraction without external dependencies
 */
public class YouTubeUrlExtractor {
    
    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile(
        "(?:youtube\\.com/watch\\?v=|youtu\\.be/|youtube\\.com/embed/)([a-zA-Z0-9_-]{11})"
    );
    
    // Cache for executable paths
    private static String cachedYtDlpPath = null;
    private static String cachedStreamlinkPath = null;
    private static boolean ytDlpSearched = false;
    private static boolean streamlinkSearched = false;
    
    /**
     * Extracts the video ID from a YouTube URL
     */
    public static String extractVideoId(String youtubeUrl) {
        Matcher matcher = VIDEO_ID_PATTERN.matcher(youtubeUrl);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    /**
     * Validates if a URL is a valid YouTube URL
     */
    public static boolean isValidYouTubeUrl(String url) {
        return extractVideoId(url) != null;
    }
    
    /**
     * Gets a direct stream URL from a YouTube video ID
     * Uses YouTube's get_video_info API to extract stream URLs
     */
    public static String getStreamUrl(String videoId) {
        if (videoId == null || videoId.isEmpty()) {
            return null;
        }
        
        try {
            TharidiaThings.LOGGER.info("Attempting to extract stream URL for video ID: {}", videoId);
            
            // Fetch video page to extract player response
            String pageUrl = "https://www.youtube.com/watch?v=" + videoId;
            URL url = new URL(pageUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                TharidiaThings.LOGGER.error("Failed to fetch YouTube page: HTTP {}", responseCode);
                return null;
            }
            
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();
            
            String pageContent = response.toString();
            
            // Extract player response JSON from page
            Pattern playerResponsePattern = Pattern.compile("var ytInitialPlayerResponse = (\\{.+?\\});");
            Matcher matcher = playerResponsePattern.matcher(pageContent);
            
            if (!matcher.find()) {
                TharidiaThings.LOGGER.error("Could not find player response in YouTube page");
                return null;
            }
            
            String playerResponseJson = matcher.group(1);
            JsonObject playerResponse = JsonParser.parseString(playerResponseJson).getAsJsonObject();
            
            // Navigate to streaming data
            if (!playerResponse.has("streamingData")) {
                TharidiaThings.LOGGER.error("No streaming data found in player response");
                return null;
            }
            
            JsonObject streamingData = playerResponse.getAsJsonObject("streamingData");
            
            // Try to get formats (video+audio combined)
            if (streamingData.has("formats") && streamingData.getAsJsonArray("formats").size() > 0) {
                JsonObject format = streamingData.getAsJsonArray("formats").get(0).getAsJsonObject();
                if (format.has("url")) {
                    String streamUrl = format.get("url").getAsString();
                    TharidiaThings.LOGGER.info("Successfully extracted stream URL for video {}", videoId);
                    return streamUrl;
                }
            }
            
            // Try adaptive formats as fallback (video only, needs separate audio)
            if (streamingData.has("adaptiveFormats") && streamingData.getAsJsonArray("adaptiveFormats").size() > 0) {
                // Find best video-only format
                for (int i = 0; i < streamingData.getAsJsonArray("adaptiveFormats").size(); i++) {
                    JsonObject format = streamingData.getAsJsonArray("adaptiveFormats").get(i).getAsJsonObject();
                    if (format.has("mimeType") && format.get("mimeType").getAsString().contains("video/mp4")) {
                        if (format.has("url")) {
                            String streamUrl = format.get("url").getAsString();
                            TharidiaThings.LOGGER.info("Successfully extracted adaptive stream URL for video {}", videoId);
                            return streamUrl;
                        }
                    }
                }
            }
            
            TharidiaThings.LOGGER.error("No suitable stream format found for video {}", videoId);
            return null;
            
        } catch (Exception e) {
            TharidiaThings.LOGGER.error("Failed to extract stream URL for video {}", videoId, e);
            return null;
        }
    }
    
    /**
     * Alternative method using invidious instances (privacy-friendly YouTube frontend)
     * This can be used as a fallback
     */
    public static String getInvidiousStreamUrl(String videoId) {
        try {
            // Use a public invidious instance
            String invidiousUrl = "https://invidious.snopyta.org/api/v1/videos/" + videoId;
            
            URL url = new URL(invidiousUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();
                
                // Parse JSON response
                JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
                
                // Get the best quality stream URL
                if (json.has("formatStreams") && json.getAsJsonArray("formatStreams").size() > 0) {
                    JsonObject stream = json.getAsJsonArray("formatStreams").get(0).getAsJsonObject();
                    return stream.get("url").getAsString();
                }
            }
            
        } catch (Exception e) {
            TharidiaThings.LOGGER.warn("Failed to get Invidious stream URL for video {}", videoId, e);
        }
        
        return null;
    }
    
    /**
     * Find executable in multiple locations (Windows-friendly)
     */
    private static String findExecutable(String execName, boolean isWindows) {
        List<String> searchPaths = new ArrayList<>();
        
        if (isWindows) {
            // Windows-specific paths
            String exeName = execName + ".exe";
            
            // 1. Current working directory (Minecraft folder)
            searchPaths.add(System.getProperty("user.dir") + File.separator + exeName);
            
            // 1b. DependencyDownloader installs tools under /bin
            searchPaths.add(System.getProperty("user.dir") + File.separator + "bin" + File.separator + exeName);
            if (execName.equals("streamlink")) {
                // streamlink has nested bin layout: bin/streamlink/bin/streamlink.exe
                searchPaths.add(System.getProperty("user.dir") + File.separator + "bin" + File.separator + "streamlink"
                        + File.separator + "bin" + File.separator + exeName);
            }

            // 2. .minecraft folder
            String minecraftDir = System.getProperty("user.home") + File.separator + "AppData" + File.separator + "Roaming" + File.separator + ".minecraft";
            searchPaths.add(minecraftDir + File.separator + exeName);
            searchPaths.add(minecraftDir + File.separator + "bin" + File.separator + exeName);
            
            // 3. Common installation paths
            searchPaths.add("C:\\Program Files\\" + execName + "\\" + exeName);
            searchPaths.add("C:\\Program Files (x86)\\" + execName + "\\" + exeName);
            
            // 4. Python Scripts folder (if installed via pip)
            String pythonScripts = System.getProperty("user.home") + File.separator + "AppData" + File.separator + "Local" + File.separator + "Programs" + File.separator + "Python";
            searchPaths.add(pythonScripts + File.separator + "Scripts" + File.separator + exeName);
            
            // 5. Try PATH (just the command name)
            searchPaths.add(exeName);
        } else {
            // Linux/Mac - check common installation paths first, then PATH
            String homeDir = System.getProperty("user.home");
            
            // Common system paths
            searchPaths.add("/usr/bin/" + execName);
            searchPaths.add("/usr/local/bin/" + execName);
            
            // User local paths
            searchPaths.add(homeDir + "/.local/bin/" + execName);
            searchPaths.add(homeDir + "/bin/" + execName);
            
            // Snap packages
            searchPaths.add("/snap/bin/" + execName);
            
            // Flatpak
            searchPaths.add("/var/lib/flatpak/exports/bin/" + execName);
            searchPaths.add(homeDir + "/.local/share/flatpak/exports/bin/" + execName);
            
            // Finally try PATH (just the command name)
            searchPaths.add(execName);
        }
        
        // Check each path
        for (String path : searchPaths) {
            File file = new File(path);
            if (file.exists() && file.canExecute()) {
                TharidiaThings.LOGGER.info("Found {} at: {}", execName, path);
                return path;
            }
        }
        
        // If not found in specific paths, try just the command name (relies on PATH)
        return isWindows ? execName + ".exe" : execName;
    }
    
    /**
     * Uses yt-dlp to extract stream URL (most reliable method)
     * DISABLED: ProcessBuilder execution not allowed for CurseForge compliance
     */
    public static String getYtDlpStreamUrl(String youtubeUrl) {
        // ProcessBuilder (yt-dlp) disabled for CurseForge compliance
        TharidiaThings.LOGGER.warn("[CURSEFORGE MODE] yt-dlp extraction disabled - external process execution not allowed");
        TharidiaThings.LOGGER.info("[CURSEFORGE MODE] URL requested: {}", youtubeUrl);
        return null;
    }
    
    /**
     * Check if URL is a direct stream (not HLS/DASH manifest)
     */
    private static boolean isDirectUrl(String url) {
        return url != null && 
               !url.contains(".m3u8") && 
               !url.contains("manifest") && 
               !url.contains("/dash/");
    }
    
    /**
     * Try to extract URL using yt-dlp with specific settings
     * DISABLED: ProcessBuilder execution not allowed for CurseForge compliance
     */
    private static String tryYtDlpExtract(String cmd, String youtubeUrl, boolean useCookies) {
        // ProcessBuilder (yt-dlp) disabled for CurseForge compliance
        return null;
    }

/**
 * Gets the best available stream URL, trying multiple methods
 */
public static String getBestStreamUrl(String url) {
    // Check if it's a Twitch URL
    if (url.contains("twitch.tv")) {
        TharidiaThings.LOGGER.info("Detected Twitch URL, using streamlink");
        return getTwitchStreamUrl(url);
    }
    
    String videoId = extractVideoId(url);
    if (videoId == null) {
        TharidiaThings.LOGGER.error("Invalid YouTube URL: {}", url);
        return null;
    }

    // Try yt-dlp first (most reliable)
    String streamUrl = getYtDlpStreamUrl(url);
    if (streamUrl != null) {
        return streamUrl;
    }

    // Try direct method
    streamUrl = getStreamUrl(videoId);
    if (streamUrl != null) {
        return streamUrl;
    }

    // Try Invidious as last resort
    streamUrl = getInvidiousStreamUrl(videoId);
    if (streamUrl != null) {
        return streamUrl;
    }

    TharidiaThings.LOGGER.error("Could not extract stream URL for YouTube video: {}", url);
    TharidiaThings.LOGGER.error("Please install yt-dlp: pip install yt-dlp");
    return null;
}

/**
 * Uses streamlink to extract Twitch stream URL
 * DISABLED: ProcessBuilder execution not allowed for CurseForge compliance
 */
public static String getTwitchStreamUrl(String twitchUrl) {
    // ProcessBuilder (streamlink) disabled for CurseForge compliance
    TharidiaThings.LOGGER.warn("[CURSEFORGE MODE] Streamlink extraction disabled - external process execution not allowed");
    TharidiaThings.LOGGER.info("[CURSEFORGE MODE] Twitch URL requested: {}", twitchUrl);
    return null;
}
}
