package com.tharidia.tharidia_things.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.tharidia.tharidia_things.config.FatigueConfig;
import com.tharidia.tharidia_things.fatigue.FatigueAttachments;
import com.tharidia.tharidia_things.fatigue.FatigueData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

/**
 * Admin commands for managing and viewing player fatigue
 */
public class FatigueCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tharidia")
            .requires(source -> source.hasPermission(2)) // OP level 2
            .then(Commands.literal("fatigue")
                .then(Commands.literal("check")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(FatigueCommands::checkFatigue)))
                .then(Commands.literal("checkall")
                    .executes(FatigueCommands::checkAllFatigue))
                .then(Commands.literal("config")
                    .executes(FatigueCommands::checkConfig))
                .then(Commands.literal("set")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("minutes", IntegerArgumentType.integer(0, 40))
                            .executes(FatigueCommands::setFatigue))))
                .then(Commands.literal("reset")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(FatigueCommands::resetFatigue)))
                .then(Commands.literal("resetall")
                    .executes(FatigueCommands::resetAllFatigue))
            )
        );
    }
    
    /**
     * Check fatigue for a specific player
     */
    private static int checkFatigue(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            FatigueData data = target.getData(FatigueAttachments.FATIGUE_DATA);
            
            int currentTicks = data.getFatigueTicks();
            int maxTicks = FatigueConfig.getMaxFatigueTicks();
            float percentage = data.getFatiguePercentage();
            
            // Convert to readable time
            int minutes = currentTicks / (60 * 20);
            int seconds = (currentTicks % (60 * 20)) / 20;
            
            context.getSource().sendSuccess(() -> Component.literal("§6╔══════════════════════════════╗"), false);
            context.getSource().sendSuccess(() -> Component.literal("§6║ §e§lFatigue Info: §f" + target.getName().getString() + " §6║"), false);
            context.getSource().sendSuccess(() -> Component.literal("§6╠══════════════════════════════╣"), false);
            context.getSource().sendSuccess(() -> Component.literal("§6║ §7Energy: §f" + (int)(percentage * 100) + "% §6║"), false);
            context.getSource().sendSuccess(() -> Component.literal("§6║ §7Time Left: §f" + minutes + "m " + seconds + "s §6║"), false);
            context.getSource().sendSuccess(() -> Component.literal("§6║ §7Ticks: §f" + currentTicks + "/" + maxTicks + " §6║"), false);
            
            String status;
            if (data.isExhausted()) {
                status = "§c§lEXHAUSTED";
            } else if (percentage < 0.15f) {
                status = "§e§lCRITICAL";
            } else if (percentage < 0.50f) {
                status = "§6§lTIRED";
            } else {
                status = "§a§lHEALTHY";
            }
            context.getSource().sendSuccess(() -> Component.literal("§6║ §7Status: " + status + " §6║"), false);
            
            // Bed rest info
            if (target.isSleeping()) {
                int bedTicks = data.getBedRestTicks();
                int bedSeconds = bedTicks / 20;
                context.getSource().sendSuccess(() -> Component.literal("§6║ §7Bed Rest: §f" + bedSeconds + "s / 60s §6║"), false);
            }
            
            context.getSource().sendSuccess(() -> Component.literal("§6╚══════════════════════════════╝"), false);
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError checking fatigue: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Check fatigue for all online players
     */
    private static int checkAllFatigue(CommandContext<CommandSourceStack> context) {
        Collection<ServerPlayer> players = context.getSource().getServer().getPlayerList().getPlayers();
        
        if (players.isEmpty()) {
            context.getSource().sendFailure(Component.literal("§cNo players online"));
            return 0;
        }
        
        context.getSource().sendSuccess(() -> Component.literal("§6═══════════════ §e§lAll Players Fatigue §6═══════════════"), false);
        
        for (ServerPlayer player : players) {
            FatigueData data = player.getData(FatigueAttachments.FATIGUE_DATA);
            float percentage = data.getFatiguePercentage();
            int minutes = data.getFatigueTicks() / (60 * 20);
            
            String bar = createEnergyBar(percentage);
            String status;
            if (data.isExhausted()) {
                status = "§c✗ EXHAUSTED";
            } else if (percentage < 0.15f) {
                status = "§e⚠ CRITICAL";
            } else if (percentage < 0.50f) {
                status = "§6◐ TIRED";
            } else {
                status = "§a✓ HEALTHY";
            }
            
            final String playerName = player.getName().getString();
            final String finalBar = bar;
            final String finalStatus = status;
            final int finalMinutes = minutes;
            
            context.getSource().sendSuccess(() -> Component.literal(
                "§7" + playerName + ": " + finalBar + " §f" + (int)(percentage * 100) + "% §7(" + finalMinutes + "m) " + finalStatus
            ), false);
        }
        
        context.getSource().sendSuccess(() -> Component.literal("§6═══════════════════════════════════════════════════"), false);
        
        return players.size();
    }
    
    /**
     * Set fatigue for a player
     */
    private static int setFatigue(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            int minutes = IntegerArgumentType.getInteger(context, "minutes");
            
            FatigueData data = target.getData(FatigueAttachments.FATIGUE_DATA);
            int ticks = minutes * 60 * 20;
            data.setFatigueTicks(ticks);
            
            context.getSource().sendSuccess(() -> Component.literal(
                "§aSet fatigue for §f" + target.getName().getString() + "§a to §f" + minutes + " minutes"
            ), true);
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError setting fatigue: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Reset fatigue for a player to maximum
     */
    private static int resetFatigue(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            FatigueData data = target.getData(FatigueAttachments.FATIGUE_DATA);
            data.fullyRestore();
            
            context.getSource().sendSuccess(() -> Component.literal(
                "§aReset fatigue for §f" + target.getName().getString() + "§a to maximum (40 minutes)"
            ), true);
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError resetting fatigue: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Reset fatigue for all online players
     */
    private static int resetAllFatigue(CommandContext<CommandSourceStack> context) {
        Collection<ServerPlayer> players = context.getSource().getServer().getPlayerList().getPlayers();
        
        if (players.isEmpty()) {
            context.getSource().sendFailure(Component.literal("§cNo players online"));
            return 0;
        }
        
        int count = 0;
        for (ServerPlayer player : players) {
            FatigueData data = player.getData(FatigueAttachments.FATIGUE_DATA);
            data.fullyRestore();
            count++;
        }
        
        final int finalCount = count;
        context.getSource().sendSuccess(() -> Component.literal(
            "§aReset fatigue for §f" + finalCount + "§a player(s) to maximum"
        ), true);
        
        return count;
    }
    
    /**
     * Creates a visual energy bar
     */
    private static String createEnergyBar(float percentage) {
        int filled = (int)(percentage * 20);
        int empty = 20 - filled;
        
        StringBuilder bar = new StringBuilder("§7[");
        
        // Color based on percentage
        String color;
        if (percentage >= 0.75f) {
            color = "§a"; // Green
        } else if (percentage >= 0.50f) {
            color = "§e"; // Yellow
        } else if (percentage >= 0.25f) {
            color = "§6"; // Orange
        } else {
            color = "§c"; // Red
        }
        
        bar.append(color);
        for (int i = 0; i < filled; i++) {
            bar.append("█");
        }
        
        bar.append("§8");
        for (int i = 0; i < empty; i++) {
            bar.append("█");
        }
        
        bar.append("§7]");
        
        return bar.toString();
    }
    
    /**
     * Shows current fatigue config values
     */
    private static int checkConfig(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("§6=== Fatigue Configuration ==="), false);
        source.sendSuccess(() -> Component.literal("§eMax Fatigue: §f" + (FatigueConfig.getMaxFatigueTicks() / 20 / 60) + " minutes (" + FatigueConfig.getMaxFatigueTicks() + " ticks)"), false);
        source.sendSuccess(() -> Component.literal("§eBed Rest Time: §f" + (FatigueConfig.getBedRestTime() / 20) + " seconds (" + FatigueConfig.getBedRestTime() + " ticks)"), false);
        source.sendSuccess(() -> Component.literal("§eBed Proximity Range: §f" + FatigueConfig.getBedProximityRange() + " blocks"), false);
        source.sendSuccess(() -> Component.literal("§eProximity Recovery Interval: §f" + (FatigueConfig.getProximityRecoveryInterval() / 20) + " seconds"), false);
        source.sendSuccess(() -> Component.literal("§eProximity Recovery Amount: §f" + (FatigueConfig.getProximityRecoveryAmount() / 20) + " seconds of fatigue"), false);
        source.sendSuccess(() -> Component.literal("§eMovement Check Interval: §f" + FatigueConfig.getMovementCheckInterval() + " ticks"), false);
        source.sendSuccess(() -> Component.literal("§eBed Check Interval: §f" + FatigueConfig.getBedCheckInterval() + " ticks"), false);
        source.sendSuccess(() -> Component.literal("§eDay Cycle Length: §f" + FatigueConfig.getDayCycleLength() + " ticks"), false);
        source.sendSuccess(() -> Component.literal("§eDay End Time: §f" + FatigueConfig.getDayEndTime() + " ticks"), false);
        
        return 1;
    }
}
