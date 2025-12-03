package com.tharidia.tharidia_things.video;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tharidia.tharidia_things.TharidiaThings;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
     * Uses yt-dlp to extract stream URL (most reliable method)
     * Prefers direct URLs over HLS/DASH, but accepts HLS as fallback
     */
    public static String getYtDlpStreamUrl(String youtubeUrl) {
        try {
            TharidiaThings.LOGGER.info("Attempting to extract stream URL using yt-dlp for: {}", youtubeUrl);
            
            // Detect OS to use correct command
            String os = System.getProperty("os.name").toLowerCase();
            boolean isWindows = os.contains("win");
            
            // Try yt-dlp first, then youtube-dl as fallback
            String[] commands = isWindows 
                ? new String[]{"yt-dlp.exe", "youtube-dl.exe"}
                : new String[]{"yt-dlp", "youtube-dl"};
            
            String hlsFallbackUrl = null; // Store HLS URL as fallback
            
            for (String cmd : commands) {
                // Try to get direct URL (not HLS) with cookies
                String result = tryYtDlpExtract(cmd, youtubeUrl, true);
                if (result != null) {
                    if (isDirectUrl(result)) {
                        TharidiaThings.LOGGER.info("Got direct URL using {} with cookies", cmd);
                        return result;
                    } else if (hlsFallbackUrl == null) {
                        hlsFallbackUrl = result;
                        TharidiaThings.LOGGER.info("Got HLS URL (will use as fallback): {}", 
                            result.substring(0, Math.min(80, result.length())));
                    }
                }
                
                // Try without cookies
                result = tryYtDlpExtract(cmd, youtubeUrl, false);
                if (result != null) {
                    if (isDirectUrl(result)) {
                        TharidiaThings.LOGGER.info("Got direct URL using {} without cookies", cmd);
                        return result;
                    } else if (hlsFallbackUrl == null) {
                        hlsFallbackUrl = result;
                        TharidiaThings.LOGGER.info("Got HLS URL (will use as fallback): {}", 
                            result.substring(0, Math.min(80, result.length())));
                    }
                }
            }
            
            // If we only got HLS URL, use it - FFmpeg can handle it
            if (hlsFallbackUrl != null) {
                TharidiaThings.LOGGER.warn("Using HLS URL as fallback - FFmpeg will handle it");
                return hlsFallbackUrl;
            }
        
            TharidiaThings.LOGGER.error("Neither yt-dlp nor youtube-dl returned valid URLs");
            TharidiaThings.LOGGER.error("Install yt-dlp: pip install yt-dlp");
            return null;
        
        } catch (Exception e) {
            TharidiaThings.LOGGER.error("Failed to extract stream URL using yt-dlp", e);
            return null;
        }
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
     */
    private static String tryYtDlpExtract(String cmd, String youtubeUrl, boolean useCookies) {
        try {
            ProcessBuilder pb;
            if (useCookies) {
                pb = new ProcessBuilder(
                    cmd,
                    "-f", "best[ext=mp4]/best",  // Simple format - let yt-dlp choose
                    "-g",  // Get URL only
                    "--no-warnings",
                    "--no-playlist",
                    "--cookies-from-browser", "chrome",
                    youtubeUrl
                );
            } else {
                pb = new ProcessBuilder(
                    cmd,
                    "-f", "best[ext=mp4]/best",
                    "-g",
                    "--no-warnings",
                    "--no-playlist",
                    youtubeUrl
                );
            }
            
            pb.redirectErrorStream(false);
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            
            String streamUrl = null;
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("http://") || line.startsWith("https://")) {
                    streamUrl = line;
                    break;  // Take first URL
                }
            }
            
            process.waitFor();
            reader.close();
            
            return streamUrl;
            
        } catch (Exception e) {
            TharidiaThings.LOGGER.debug("Failed to use {}: {}", cmd, e.getMessage());
            return null;
        }
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
 */
public static String getTwitchStreamUrl(String twitchUrl) {
    try {
        TharidiaThings.LOGGER.info("Attempting to extract Twitch stream URL using streamlink for: {}", twitchUrl);
        
        // Detect OS to use correct streamlink command
        String os = System.getProperty("os.name").toLowerCase();
        String streamlinkCmd = os.contains("win") ? "streamlink.exe" : "streamlink";
        
        // Use streamlink WITHOUT --player-passthrough to get m3u8 URL that VLC can handle
        // VLC can play m3u8 URLs directly when not using callback rendering
        ProcessBuilder pb = new ProcessBuilder(
            streamlinkCmd,
            "--stream-url",
            twitchUrl,
            "720p60,720p,480p,worst"
        );
        pb.redirectErrorStream(false);
        Process process = pb.start();
        
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream())
        );
        
        // Read the stream URL
        String streamUrl = null;
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("http://") || line.startsWith("https://")) {
                streamUrl = line;
                break;
            }
        }
        
        int exitCode = process.waitFor();
        
        if (exitCode == 0 && streamUrl != null && !streamUrl.isEmpty()) {
            TharidiaThings.LOGGER.info("Successfully extracted Twitch stream URL using streamlink");
            return streamUrl;
        } else {
            TharidiaThings.LOGGER.error("Streamlink failed or returned no URL. Exit code: {}", exitCode);
            TharidiaThings.LOGGER.error("Please install streamlink: pip install streamlink (or download from streamlink.github.io on Windows)");
            return null;
        }
        
    } catch (Exception e) {
        TharidiaThings.LOGGER.error("Failed to extract Twitch stream URL", e);
        return null;
    }
}
}
