package com.tharidia.tharidia_things.client.video;

import com.tharidia.tharidia_things.TharidiaThings;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Swing GUI for installing missing video tools on Windows
 */
public class VideoToolsInstallerGUI {
    private JFrame frame;
    private JProgressBar overallProgressBar;
    private JProgressBar currentProgressBar;
    private JLabel statusLabel;
    private JButton installButton;
    private JButton skipButton;
    private JTextArea detailsArea;
    private List<VideoToolsManager.ToolStatus> toolStatuses;
    private VideoToolsManager manager;
    private boolean isInstalling = false;
    
    public VideoToolsInstallerGUI(List<VideoToolsManager.ToolStatus> toolStatuses, VideoToolsManager manager) {
        this.toolStatuses = toolStatuses;
        this.manager = manager;
    }
    
    public void show() {
        TharidiaThings.LOGGER.info("[VIDEO TOOLS] show() called, isEventDispatchThread: {}", SwingUtilities.isEventDispatchThread());
        TharidiaThings.LOGGER.info("[VIDEO TOOLS] Headless mode: {}", System.getProperty("java.awt.headless"));
        
        // Try to force synchronous execution to see if it throws errors
        try {
            if (!SwingUtilities.isEventDispatchThread()) {
                TharidiaThings.LOGGER.info("[VIDEO TOOLS] Not on EDT, using invokeAndWait");
                SwingUtilities.invokeAndWait(() -> {
                    TharidiaThings.LOGGER.info("[VIDEO TOOLS] Inside invokeAndWait callback");
                    createAndShowGUI();
                });
                TharidiaThings.LOGGER.info("[VIDEO TOOLS] invokeAndWait completed successfully");
            } else {
                TharidiaThings.LOGGER.info("[VIDEO TOOLS] Already on EDT, creating GUI directly");
                createAndShowGUI();
            }
        } catch (Exception e) {
            TharidiaThings.LOGGER.error("[VIDEO TOOLS] Error showing GUI: {}", e.getMessage(), e);
        }
    }
    
    private void createAndShowGUI() {
        // CRITICAL: Disable headless mode to allow GUI creation
        System.setProperty("java.awt.headless", "false");
        
        // Fix for Linux OpenGL compositing issues
        System.setProperty("sun.java2d.opengl", "true");
        System.setProperty("sun.java2d.xrender", "true");
        System.setProperty("sun.java2d.d3d", "false");
        
        TharidiaThings.LOGGER.info("[VIDEO TOOLS] Creating JFrame with headless={}", System.getProperty("java.awt.headless"));
        
        frame = new JFrame("Tharidia Video Tools Installer");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setSize(550, 650);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setIconImage(createIcon());
        
        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(new Color(240, 240, 240));
        
        // Title panel with icon
        JPanel titlePanel = new JPanel(new BorderLayout(10, 0));
        titlePanel.setOpaque(false);
        
        JLabel iconLabel = new JLabel(new ImageIcon(createIcon()));
        titlePanel.add(iconLabel, BorderLayout.WEST);
        
        JLabel titleLabel = new JLabel("Video Tools Required", JLabel.CENTER);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 20));
        titleLabel.setForeground(new Color(50, 50, 50));
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        
        // Center panel with content
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setOpaque(false);
        
        // Status text
        statusLabel = new JLabel("<html><body style='width: 450px'>" +
            "The following tools are required for video playback in Tharidia:<br><br>" +
            "<b>FFmpeg:</b> Video decoding and processing<br>" +
            "<b>FFplay:</b> Audio playback<br>" +
            "<b>yt-dlp:</b> YouTube and Twitch stream extraction<br>" +
            "<b>streamlink:</b> Twitch stream support</body></html>");
        statusLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        centerPanel.add(statusLabel, BorderLayout.NORTH);
        
        // Tool status details
        detailsArea = new JTextArea();
        detailsArea.setEditable(false);
        detailsArea.setBackground(Color.WHITE);
        detailsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        detailsArea.setBorder(BorderFactory.createLoweredBevelBorder());
        updateToolStatus();
        
        JScrollPane scrollPane = new JScrollPane(detailsArea);
        scrollPane.setPreferredSize(new Dimension(450, 120));
        scrollPane.setBorder(BorderFactory.createTitledBorder("Tool Status"));
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Progress bars panel
        JPanel progressPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        progressPanel.setOpaque(false);
        
        overallProgressBar = new JProgressBar(0, 100);
        overallProgressBar.setStringPainted(true);
        overallProgressBar.setString("Overall Progress");
        overallProgressBar.setBorder(BorderFactory.createTitledBorder("Installation Progress"));
        progressPanel.add(overallProgressBar);
        
        currentProgressBar = new JProgressBar(0, 100);
        currentProgressBar.setStringPainted(true);
        currentProgressBar.setString("Waiting...");
        progressPanel.add(currentProgressBar);
        
        centerPanel.add(progressPanel, BorderLayout.SOUTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        
        // Check if all tools are already installed
        boolean allInstalled = toolStatuses.stream().allMatch(status -> status.isInstalled);
        
        installButton = new JButton(allInstalled ? "All Tools Installed" : "Install Missing Tools");
        installButton.setPreferredSize(new Dimension(160, 35));
        installButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        installButton.setBackground(new Color(76, 175, 80));
        installButton.setForeground(Color.WHITE);
        installButton.setFocusPainted(false);
        installButton.setEnabled(!allInstalled); // Disable if all tools are installed
        installButton.addActionListener(new InstallActionListener());
        
        skipButton = new JButton(allInstalled ? "Close" : "Skip");
        skipButton.setPreferredSize(new Dimension(80, 35));
        skipButton.addActionListener(e -> {
            if (!allInstalled) {
                TharidiaThings.LOGGER.warn("[VIDEO TOOLS] User skipped video tools installation");
                JOptionPane.showMessageDialog(frame, 
                    "Video features will not be available until the required tools are installed.\n" +
                    "You can run the installer again later when a video screen is encountered.",
                    "Installation Skipped",
                    JOptionPane.WARNING_MESSAGE);
            }
            frame.dispose();
        });
        
        buttonPanel.add(installButton);
        buttonPanel.add(skipButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        frame.add(mainPanel);
        frame.setVisible(true);
        frame.setAlwaysOnTop(true);
        frame.toFront();
        frame.requestFocus();
        frame.setAlwaysOnTop(false);
        
        // Prevent closing during installation
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                if (!isInstalling) {
                    frame.dispose();
                } else {
                    JOptionPane.showMessageDialog(frame, 
                        "Please wait for installation to complete...",
                        "Installation in Progress",
                        JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });
    }
    
    private Image createIcon() {
        // Create a simple play button icon
        BufferedImage icon = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = icon.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw a simple play triangle
        g2d.setColor(new Color(76, 175, 80));
        int[] xPoints = {8, 8, 24};
        int[] yPoints = {8, 24, 16};
        g2d.fillPolygon(xPoints, yPoints, 3);
        
        g2d.dispose();
        return icon;
    }
    
    private void updateToolStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("Tool Detection Results:\n");
        sb.append("─────────────────────────────────────\n");
        
        for (VideoToolsManager.ToolStatus status : toolStatuses) {
            String statusText = status.isInstalled ? "✓ FOUND" : "✗ MISSING";
            sb.append(String.format("%-10s %s\n", status.name + ":", statusText));
            if (status.isInstalled) {
                sb.append(String.format("           Location: Available\n"));
            } else {
                sb.append(String.format("           Will be installed to: %s\n", 
                    System.getProperty("user.home") + "\\.tharidia\\bin"));
            }
        }
        
        sb.append("\n");
        sb.append("Installation Details:\n");
        sb.append("• Tools will be installed to ~/.tharidia/bin\n");
        sb.append("• No administrator privileges required\n");
        sb.append("• Installation is automatic and safe\n");
        sb.append("• Total download size: ~100 MB\n");
        
        detailsArea.setText(sb.toString());
        detailsArea.setCaretPosition(0);
    }
    
    private class InstallActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (isInstalling) return;
            
            isInstalling = true;
            installButton.setEnabled(false);
            skipButton.setEnabled(false);
            
            // Start installation in background
            CompletableFuture.runAsync(() -> {
                try {
                    installMissingTools();
                } catch (Exception ex) {
                    TharidiaThings.LOGGER.error("[VIDEO TOOLS] Installation failed: {}", ex.getMessage());
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(frame, 
                            "Installation failed: " + ex.getMessage() + "\n\n" +
                            "Please check your internet connection and try again.\n" +
                            "You can also install the tools manually:\n" +
                            "1. FFmpeg: https://www.gyan.dev/ffmpeg/builds/\n" +
                            "2. yt-dlp: https://github.com/yt-dlp/yt-dlp/releases",
                            "Installation Error",
                            JOptionPane.ERROR_MESSAGE);
                        frame.dispose();
                    });
                }
            });
        }
    }
    
    private void installMissingTools() {
        int totalSteps = 0;
        int completedSteps = 0;
        
        // Count missing tools
        for (VideoToolsManager.ToolStatus status : toolStatuses) {
            if (!status.isInstalled) {
                totalSteps++;
            }
        }
        
        if (totalSteps == 0) {
            SwingUtilities.invokeLater(() -> {
                // Update status to show all tools are installed
                statusLabel.setText("<html><body style='width: 450px'>" +
                    "<b>All Video Tools Already Installed!</b><br><br>" +
                    "The following tools are ready for use:<br>" +
                    "• FFmpeg: Video decoding and processing<br>" +
                    "• FFplay: Audio playback<br>" +
                    "• yt-dlp: YouTube and Twitch stream extraction<br>" +
                    "<br>You can now use video features in Tharidia.</body></html>");
                
                // Update install button to be a close button
                installButton.setText("Close");
                installButton.setBackground(new Color(158, 158, 158));
                installButton.removeActionListener(installButton.getActionListeners()[0]);
                installButton.addActionListener(e -> frame.dispose());
                
                // Disable skip button
                skipButton.setEnabled(false);
                skipButton.setText("Already Installed");
                
                // Set progress bars to complete
                overallProgressBar.setValue(100);
                overallProgressBar.setString("All Tools Installed");
                currentProgressBar.setValue(100);
                currentProgressBar.setString("Ready");
                
                // Update details area
                StringBuilder sb = new StringBuilder();
                sb.append("Tool Detection Results:\n");
                sb.append("─────────────────────────────────────\n");
                
                for (VideoToolsManager.ToolStatus status : toolStatuses) {
                    sb.append(String.format("%-10s ✓ FOUND\n", status.name + ":"));
                    sb.append(String.format("           Location: Available\n"));
                }
                
                sb.append("\n");
                sb.append("Status: All tools are ready!\n");
                sb.append("• Video playback is fully functional\n");
                sb.append("• Press F10 anytime to check tool status\n");
                
                detailsArea.setText(sb.toString());
                detailsArea.setCaretPosition(0);
            });
            return;
        }
        
        // Ensure tools directory exists
        VideoToolsDownloader.ensureToolsDirectory();
        
        // Install FFmpeg if missing
        if (!toolStatuses.get(0).isInstalled) { // FFmpeg
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("<html><body style='width: 450px'>" +
                    "<b>Step 1/2:</b> Downloading FFmpeg<br>" +
                    "This includes both FFmpeg and FFplay for video and audio processing<br>" +
                    "Size: ~90 MB</body></html>");
                currentProgressBar.setString("Downloading FFmpeg...");
            });
            
            boolean success = VideoToolsDownloader.downloadFFmpeg(progress -> {
                SwingUtilities.invokeLater(() -> {
                    currentProgressBar.setValue((int) progress.percentage);
                    currentProgressBar.setString(String.format("Downloading FFmpeg... %.1f%%", progress.percentage));
                });
            }).join();
            
            if (success) {
                completedSteps++;
                updateOverallProgress(completedSteps, totalSteps);
            } else {
                throw new RuntimeException("Failed to download FFmpeg");
            }
        }
        
        // Install yt-dlp if missing
        if (!toolStatuses.get(2).isInstalled) { // yt-dlp
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("<html><body style='width: 450px'>" +
                    "<b>Step 2/3:</b> Downloading yt-dlp<br>" +
                    "Required for YouTube and Twitch video support<br>" +
                    "Size: ~10 MB</body></html>");
                currentProgressBar.setString("Downloading yt-dlp...");
            });
            
            boolean success = VideoToolsDownloader.downloadYtDlp(progress -> {
                SwingUtilities.invokeLater(() -> {
                    currentProgressBar.setValue((int) progress.percentage);
                    currentProgressBar.setString(String.format("Downloading yt-dlp... %.1f%%", progress.percentage));
                });
            }).join();
            
            if (success) {
                completedSteps++;
                updateOverallProgress(completedSteps, totalSteps);
            } else {
                throw new RuntimeException("Failed to download yt-dlp");
            }
        }
        
        // Install streamlink if missing
        if (!toolStatuses.get(3).isInstalled) { // streamlink
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("<html><body style='width: 450px'>" +
                    "<b>Step 3/3:</b> Downloading streamlink<br>" +
                    "Required for Twitch live stream support<br>" +
                    "Size: ~15 MB</body></html>");
                currentProgressBar.setString("Downloading streamlink...");
            });
            
            boolean success = VideoToolsDownloader.downloadStreamlink(progress -> {
                SwingUtilities.invokeLater(() -> {
                    currentProgressBar.setValue((int) progress.percentage);
                    currentProgressBar.setString(String.format("Downloading streamlink... %.1f%%", progress.percentage));
                });
            }).join();
            
            if (success) {
                completedSteps++;
                updateOverallProgress(completedSteps, totalSteps);
            } else {
                throw new RuntimeException("Failed to download streamlink");
            }
        }
        
        // Installation complete
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("<html><body style='width: 450px'>" +
                "<b>Installation Complete!</b><br><br>" +
                "All video tools have been successfully installed.<br>" +
                "You can now use video features in Tharidia.<br><br>" +
                "The game will automatically detect the new tools.</body></html>");
            currentProgressBar.setString("Done!");
            currentProgressBar.setValue(100);
            overallProgressBar.setValue(100);
            
            JOptionPane.showMessageDialog(frame, 
                "Video tools have been installed successfully!\n\n" +
                "FFmpeg, FFplay, and yt-dlp are now available.\n" +
                "You can now use video screens in Tharidia.\n\n" +
                "Click OK to continue playing.",
                "Installation Complete",
                JOptionPane.INFORMATION_MESSAGE);
            
            // Notify manager
            manager.onInstallationComplete();
            frame.dispose();
        });
    }
    
    private void updateOverallProgress(int completed, int total) {
        SwingUtilities.invokeLater(() -> {
            int percentage = (int) ((completed * 100.0) / total);
            overallProgressBar.setValue(percentage);
            overallProgressBar.setString(String.format("Overall Progress: %d/%d tools", completed, total));
        });
    }
}
