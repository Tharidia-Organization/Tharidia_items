package com.THproject.tharidia_things.client.video;

import com.THproject.tharidia_things.TharidiaThings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GUI screen for downloading missing video dependencies
 */
public class DependencySetupScreen extends Screen {
    
    private final Screen previousScreen;
    private final List<DependencyDownloader.Dependency> missingDeps;
    private final Map<DependencyDownloader.Dependency, DownloadState> downloadStates = new HashMap<>();
    
    private Button installButton;
    private Button skipButton;
    private boolean isDownloading = false;
    private int completedDownloads = 0;
    
    private static class DownloadState {
        double progress = 0.0;
        boolean completed = false;
        boolean failed = false;
    }
    
    public DependencySetupScreen(Screen previousScreen, List<DependencyDownloader.Dependency> missingDeps) {
        super(Component.literal("Video Dependencies Setup"));
        this.previousScreen = previousScreen;
        this.missingDeps = missingDeps;
        
        for (DependencyDownloader.Dependency dep : missingDeps) {
            downloadStates.put(dep, new DownloadState());
        }
    }
    
    @Override
    protected void init() {
        super.init();
        
        int buttonWidth = 200;
        int buttonHeight = 20;
        int centerX = this.width / 2;
        int startY = this.height / 2 + 60;
        
        // Install All button
        installButton = Button.builder(
            Component.literal("Install All Dependencies"),
            button -> startDownloads()
        )
        .bounds(centerX - buttonWidth - 5, startY, buttonWidth, buttonHeight)
        .build();
        
        // Skip button
        skipButton = Button.builder(
            Component.literal("Skip (Manual Install)"),
            button -> closeScreen()
        )
        .bounds(centerX + 5, startY, buttonWidth, buttonHeight)
        .build();
        
        addRenderableWidget(installButton);
        addRenderableWidget(skipButton);
    }
    
    private void startDownloads() {
        if (isDownloading) return;
        
        isDownloading = true;
        installButton.active = false;
        skipButton.active = false;
        completedDownloads = 0;
        
        TharidiaThings.LOGGER.info("Starting download of {} dependencies", missingDeps.size());
        
        for (DependencyDownloader.Dependency dep : missingDeps) {
            DownloadState state = downloadStates.get(dep);
            
            // Download all dependencies using the same method
            DependencyDownloader.downloadDependency(dep, progress -> {
                state.progress = progress;
            }).thenAccept(success -> {
                state.completed = true;
                state.failed = !success;
                state.progress = 1.0;
                completedDownloads++;
                checkAllCompleted();
            });
        }
    }
    
    private void checkAllCompleted() {
        boolean allDone = downloadStates.values().stream().allMatch(s -> s.completed);
        
        if (allDone) {
            TharidiaThings.LOGGER.info("All downloads completed");
            
            // Wait a bit then close
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    Minecraft.getInstance().execute(this::closeScreen);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }
    
    private void closeScreen() {
        if (minecraft != null) {
            minecraft.setScreen(previousScreen);
        }
    }
    
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Override to prevent blur - render solid black background
        graphics.fill(0, 0, this.width, this.height, 0xFF000000);
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Call parent to render background (which we've overridden above)
        super.render(graphics, mouseX, mouseY, partialTick);
        
        int centerX = this.width / 2;
        int startY = 40;
        
        // Title
        graphics.drawCenteredString(this.font, this.title, centerX, startY, 0xFFFFFF);
        
        // Description
        String desc1 = "Tharidia Things requires external tools to play videos.";
        String desc2 = "Click 'Install All' to download them automatically.";
        graphics.drawCenteredString(this.font, desc1, centerX, startY + 20, 0xAAAAAA);
        graphics.drawCenteredString(this.font, desc2, centerX, startY + 32, 0xAAAAAA);
        
        // Missing dependencies list
        int listY = startY + 60;
        graphics.drawCenteredString(this.font, "Missing Dependencies:", centerX, listY, 0xFFFF55);
        
        listY += 20;
        for (DependencyDownloader.Dependency dep : missingDeps) {
            DownloadState state = downloadStates.get(dep);
            
            String status;
            int color;
            
            if (state.completed) {
                if (state.failed) {
                    status = "✗ Failed";
                    color = 0xFF5555;
                } else {
                    status = "✓ Installed";
                    color = 0x55FF55;
                }
            } else if (isDownloading) {
                int percent = (int) (state.progress * 100);
                status = String.format("Downloading... %d%%", percent);
                color = 0x55FFFF;
            } else {
                status = "Pending";
                color = 0xFFFFFF;
            }
            
            String line = dep.displayName + " - " + status;
            graphics.drawCenteredString(this.font, line, centerX, listY, color);
            
            // Progress bar
            if (isDownloading && !state.completed && state.progress > 0) {
                int barWidth = 200;
                int barHeight = 4;
                int barX = centerX - barWidth / 2;
                int barY = listY + 12;
                
                // Background
                graphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF333333);
                
                // Progress
                int progressWidth = (int) (barWidth * state.progress);
                graphics.fill(barX, barY, barX + progressWidth, barY + barHeight, 0xFF55FF55);
            }
            
            listY += 25;
        }
        
        // Render buttons
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        return !isDownloading;
    }
    
    @Override
    public void onClose() {
        if (!isDownloading) {
            closeScreen();
        }
    }
}
