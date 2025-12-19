package com.THproject.tharidia_things.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.THproject.tharidia_things.block.entity.ClaimBlockEntity;
import com.THproject.tharidia_things.claim.ClaimRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class ClaimAdminCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClaimAdminCommands.class);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("claimadmin")
                .requires(source -> source.hasPermission(4)) // Require OP level 4 (admin)
                .then(Commands.literal("info")
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(ClaimAdminCommands::executeInfo)))
                .then(Commands.literal("remove")
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(ClaimAdminCommands::executeRemove)))
                .then(Commands.literal("tp")
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(ClaimAdminCommands::executeTeleport)))
                .then(Commands.literal("playerinfo")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ClaimAdminCommands::executePlayerInfo)))
                .then(Commands.literal("stats")
                    .executes(ClaimAdminCommands::executeStats))
                .then(Commands.literal("clearexpired")
                    .executes(ClaimAdminCommands::executeClearExpired))
        );
    }

    /**
     * /claimadmin info <pos> - Shows detailed information about a specific claim
     */
    private static int executeInfo(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            ServerLevel level = source.getLevel();
            BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");

            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof ClaimBlockEntity claim)) {
                source.sendFailure(Component.literal("§cNo claim block found at " + pos.toShortString()));
                return 0;
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            
            source.sendSuccess(() -> Component.literal("§6§l=== Admin Claim Info ==="), false);
            source.sendSuccess(() -> Component.literal("§ePosition: §f" + pos.toShortString()), false);
            
            String claimName = claim.getClaimName();
            if (!claimName.isEmpty()) {
                source.sendSuccess(() -> Component.literal("§eName: §f" + claimName), false);
            }
            
            UUID ownerUUID = claim.getOwnerUUID();
            if (ownerUUID != null) {
                ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerUUID);
                String ownerName = owner != null ? owner.getName().getString() : "Offline";
                source.sendSuccess(() -> Component.literal("§eOwner: §f" + ownerName + " §7(" + ownerUUID + ")"), false);
            }
            
            if (claim.getCreationTime() > 0) {
                String creationDate = dateFormat.format(new Date(claim.getCreationTime()));
                source.sendSuccess(() -> Component.literal("§eCreated: §f" + creationDate), false);
            }
            
            if (claim.isRented()) {
                source.sendSuccess(() -> Component.literal("§eRental: §aActive"), false);
                source.sendSuccess(() -> Component.literal("§eDays: §f" + claim.getRentalDays()), false);
                source.sendSuccess(() -> Component.literal("§eCost: §f$" + claim.getRentalCost()), false);
                
                if (claim.getExpirationTime() > 0) {
                    String expirationDate = dateFormat.format(new Date(claim.getExpirationTime()));
                    source.sendSuccess(() -> Component.literal("§eExpires: §f" + expirationDate), false);
                    
                    long timeLeft = claim.getExpirationTime() - System.currentTimeMillis();
                    long daysLeft = timeLeft / (24 * 60 * 60 * 1000);
                    long hoursLeft = (timeLeft % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
                    source.sendSuccess(() -> Component.literal("§eTime Left: §f" + daysLeft + "d " + hoursLeft + "h"), false);
                    
                    if (claim.isExpired()) {
                        source.sendSuccess(() -> Component.literal("§c§lEXPIRED - Will be removed on next check"), false);
                    }
                }
            }
            
            source.sendSuccess(() -> Component.literal("§eProtection Radius: §f" + claim.getProtectionRadius() + " blocks"), false);

            source.sendSuccess(() -> Component.literal("§6§l=== Flags ==="), false);
            source.sendSuccess(() -> Component.literal("§eExplosions: " + (claim.getAllowExplosions() ? "§aAllowed" : "§cDenied")), false);
            source.sendSuccess(() -> Component.literal("§ePvP: " + (claim.getAllowPvP() ? "§aAllowed" : "§cDenied")), false);
            source.sendSuccess(() -> Component.literal("§eMobs: " + (claim.getAllowMobSpawning() ? "§aAllowed" : "§cDenied")), false);
            source.sendSuccess(() -> Component.literal("§eFire: " + (claim.getAllowFireSpread() ? "§aAllowed" : "§cDenied")), false);
            
            if (!claim.getTrustedPlayers().isEmpty()) {
                source.sendSuccess(() -> Component.literal("§6§l=== Trusted Players (" + claim.getTrustedPlayers().size() + ") ==="), false);
                for (UUID trustedUUID : claim.getTrustedPlayers()) {
                    ServerPlayer trustedPlayer = level.getServer().getPlayerList().getPlayer(trustedUUID);
                    String trustedName = trustedPlayer != null ? trustedPlayer.getName().getString() : "Offline";
                    source.sendSuccess(() -> Component.literal("§f- " + trustedName + " §7(" + trustedUUID + ")"), false);
                }
            }

            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing claimadmin info command", e);
            return 0;
        }
    }

    /**
     * /claimadmin remove <pos> - Force removes a claim
     */
    private static int executeRemove(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            ServerLevel level = source.getLevel();
            BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");

            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof ClaimBlockEntity claim)) {
                source.sendFailure(Component.literal("§cNo claim block found at " + pos.toShortString()));
                return 0;
            }

            String claimName = claim.getClaimName();
            UUID ownerUUID = claim.getOwnerUUID();
            
            level.destroyBlock(pos, true);
            
            source.sendSuccess(() -> Component.literal("§aForce removed claim at " + pos.toShortString()), true);
            if (!claimName.isEmpty()) {
                source.sendSuccess(() -> Component.literal("§7Claim name: " + claimName), false);
            }
            if (ownerUUID != null) {
                source.sendSuccess(() -> Component.literal("§7Owner: " + ownerUUID), false);
            }
            
            LOGGER.info("Admin {} removed claim at {}", source.getTextName(), pos);

            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing claimadmin remove command", e);
            return 0;
        }
    }

    /**
     * /claimadmin tp <pos> - Teleports to a claim
     */
    private static int executeTeleport(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            ServerLevel level = context.getSource().getLevel();
            BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");

            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof ClaimBlockEntity)) {
                player.sendSystemMessage(Component.literal("§cNo claim block found at " + pos.toShortString()));
                return 0;
            }

            // Teleport above the claim block
            player.teleportTo(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 
                             player.getYRot(), player.getXRot());
            
            player.sendSystemMessage(Component.literal("§aTeleported to claim at " + pos.toShortString()));
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing claimadmin tp command", e);
            return 0;
        }
    }

    /**
     * /claimadmin playerinfo <player> - Shows all claims owned by a player
     */
    private static int executePlayerInfo(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            ServerLevel level = source.getLevel();

            source.sendSuccess(() -> Component.literal("§6§l=== Claims owned by " + 
                targetPlayer.getName().getString() + " ==="), false);
            
            int claimCount = 0;
            int rentedCount = 0;
            int expiredCount = 0;
            
            // Get claims owned by this player from registry
            java.util.List<ClaimRegistry.ClaimData> playerClaims = ClaimRegistry.getPlayerClaims(targetPlayer.getUUID());
            
            for (ClaimRegistry.ClaimData claimData : playerClaims) {
                BlockPos claimPos = claimData.getPosition();
                BlockEntity blockEntity = level.getBlockEntity(claimPos);
                
                if (blockEntity instanceof ClaimBlockEntity claim) {
                    claimCount++;
                    String name = claim.getClaimName().isEmpty() ? "Unnamed" : claim.getClaimName();
                    
                    if (claim.isRented()) {
                        rentedCount++;
                        if (claim.isExpired()) {
                            expiredCount++;
                            source.sendSuccess(() -> Component.literal("§c[EXPIRED] §f" + name + 
                                " §7at " + claimPos.toShortString()), false);
                        } else {
                            long timeLeft = claim.getExpirationTime() - System.currentTimeMillis();
                            long daysLeft = timeLeft / (24 * 60 * 60 * 1000);
                            source.sendSuccess(() -> Component.literal("§e[RENTED] §f" + name + 
                                " §7at " + claimPos.toShortString() + " §e(" + daysLeft + "d left)"), false);
                        }
                    } else {
                        source.sendSuccess(() -> Component.literal("§a[OWNED] §f" + name + 
                            " §7at " + claimPos.toShortString()), false);
                    }
                }
            }
            
            final int finalClaimCount = claimCount;
            final int finalRentedCount = rentedCount;
            final int finalExpiredCount = expiredCount;
            
            source.sendSuccess(() -> Component.literal("§eTotal Claims: §f" + finalClaimCount), false);
            source.sendSuccess(() -> Component.literal("§eRented: §f" + finalRentedCount), false);
            if (finalExpiredCount > 0) {
                source.sendSuccess(() -> Component.literal("§cExpired: §f" + finalExpiredCount), false);
            }

            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing claimadmin playerinfo command", e);
            return 0;
        }
    }

    /**
     * /claimadmin stats - Shows server-wide claim statistics
     */
    private static int executeStats(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            ServerLevel level = source.getLevel();

            int totalClaims = 0;
            int rentedClaims = 0;
            int expiredClaims = 0;
            int trustedPlayers = 0;
            
            // Get all claims from registry
            String dimension = level.dimension().location().toString();
            java.util.List<ClaimRegistry.ClaimData> allClaims = ClaimRegistry.getClaimsInDimension(dimension);
            
            for (ClaimRegistry.ClaimData claimData : allClaims) {
                BlockEntity blockEntity = level.getBlockEntity(claimData.getPosition());
                
                if (blockEntity instanceof ClaimBlockEntity claim) {
                    totalClaims++;
                    trustedPlayers += claim.getTrustedPlayers().size();
                    
                    if (claim.isRented()) {
                        rentedClaims++;
                        if (claim.isExpired()) {
                            expiredClaims++;
                        }
                    }
                }
            }
            
            final int finalTotalClaims = totalClaims;
            final int finalRentedClaims = rentedClaims;
            final int finalExpiredClaims = expiredClaims;
            final int finalTrustedPlayers = trustedPlayers;
            
            source.sendSuccess(() -> Component.literal("§6§l=== Server Claim Statistics ==="), false);
            source.sendSuccess(() -> Component.literal("§eTotal Claims: §f" + finalTotalClaims), false);
            source.sendSuccess(() -> Component.literal("§eRented Claims: §f" + finalRentedClaims), false);
            source.sendSuccess(() -> Component.literal("§eExpired Claims: §f" + finalExpiredClaims), false);
            source.sendSuccess(() -> Component.literal("§eTotal Trusted Players: §f" + finalTrustedPlayers), false);

            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing claimadmin stats command", e);
            return 0;
        }
    }

    /**
     * /claimadmin clearexpired - Manually triggers expired claim cleanup
     */
    private static int executeClearExpired(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            ServerLevel level = source.getLevel();

            int removedCount = 0;
            
            // Find and remove expired claims using registry
            String dimension = level.dimension().location().toString();
            java.util.List<ClaimRegistry.ClaimData> allClaims = ClaimRegistry.getClaimsInDimension(dimension);
            
            for (ClaimRegistry.ClaimData claimData : allClaims) {
                BlockPos pos = claimData.getPosition();
                BlockEntity blockEntity = level.getBlockEntity(pos);
                
                if (blockEntity instanceof ClaimBlockEntity claim) {
                    if (claim.isRented() && claim.isExpired()) {
                        level.destroyBlock(pos, true);
                        removedCount++;
                        
                        LOGGER.info("Admin {} manually cleared expired claim at {}", source.getTextName(), pos);
                    }
                }
            }
            
            final int finalRemovedCount = removedCount;
            if (removedCount > 0) {
                source.sendSuccess(() -> Component.literal("§aRemoved " + finalRemovedCount + 
                    " expired claim" + (finalRemovedCount > 1 ? "s" : "")), true);
            } else {
                source.sendSuccess(() -> Component.literal("§eNo expired claims found"), false);
            }

            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing claimadmin clearexpired command", e);
            return 0;
        }
    }
}
