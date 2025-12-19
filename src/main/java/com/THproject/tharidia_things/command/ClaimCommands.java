package com.THproject.tharidia_things.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.THproject.tharidia_things.block.ClaimBlock;
import com.THproject.tharidia_things.block.entity.ClaimBlockEntity;
import com.THproject.tharidia_things.claim.ClaimRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class ClaimCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClaimCommands.class);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("claim")
                .then(Commands.literal("trust")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ClaimCommands::executeTrust)))
                .then(Commands.literal("untrust")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ClaimCommands::executeUntrust)))
                .requires(source -> source.hasPermission(4)) // Admin only
                .then(Commands.literal("info")
                        .executes(ClaimCommands::executeInfo))
                .then(Commands.literal("flag")
                    .then(Commands.literal("explosions")
                        .then(Commands.literal("allow").executes(ctx -> executeFlag(ctx, "explosions", true)))
                        .then(Commands.literal("deny").executes(ctx -> executeFlag(ctx, "explosions", false))))
                    .then(Commands.literal("pvp")
                        .then(Commands.literal("allow").executes(ctx -> executeFlag(ctx, "pvp", true)))
                        .then(Commands.literal("deny").executes(ctx -> executeFlag(ctx, "pvp", false))))
                    .then(Commands.literal("mobs")
                        .then(Commands.literal("allow").executes(ctx -> executeFlag(ctx, "mobs", true)))
                        .then(Commands.literal("deny").executes(ctx -> executeFlag(ctx, "mobs", false))))
                    .then(Commands.literal("fire")
                        .then(Commands.literal("allow").executes(ctx -> executeFlag(ctx, "fire", true)))
                        .then(Commands.literal("deny").executes(ctx -> executeFlag(ctx, "fire", false)))))
                .then(Commands.literal("abandon")
                    .executes(ClaimCommands::executeAbandon))
                .then(Commands.literal("list")
                    .executes(ClaimCommands::executeList))
        );
    }

    /**
     * /claim info - Shows information about the claim at the player's position
     */
    private static int executeInfo(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            ServerLevel level = player.serverLevel();
            BlockPos pos = player.blockPosition();

            ClaimBlockEntity claim = findClaimForPosition(level, pos);

            if (claim == null) {
                player.sendSystemMessage(Component.literal("§eNo claim found at your location."));
                return 0;
            }

            // Format claim information
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            
            player.sendSystemMessage(Component.literal("§6§l=== Claim Information ==="));
            
            // Claim name
            String claimName = claim.getClaimName();
            if (!claimName.isEmpty()) {
                player.sendSystemMessage(Component.literal("§eName: §f" + claimName));
            }
            
            // Owner
            UUID ownerUUID = claim.getOwnerUUID();
            if (ownerUUID != null) {
                ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerUUID);
                String ownerName = owner != null ? owner.getName().getString() : ownerUUID.toString();
                player.sendSystemMessage(Component.literal("§eOwner: §f" + ownerName));
            }
            
            // Creation time
            if (claim.getCreationTime() > 0) {
                String creationDate = dateFormat.format(new Date(claim.getCreationTime()));
                player.sendSystemMessage(Component.literal("§eCreated: §f" + creationDate));
            }
            
            // Expiration
            if (claim.isRented()) {
                player.sendSystemMessage(Component.literal("§eRental: §aActive"));
                player.sendSystemMessage(Component.literal("§eDays: §f" + claim.getRentalDays()));
                if (claim.getExpirationTime() > 0) {
                    String expirationDate = dateFormat.format(new Date(claim.getExpirationTime()));
                    player.sendSystemMessage(Component.literal("§eExpires: §f" + expirationDate));
                    
                    long timeLeft = claim.getExpirationTime() - System.currentTimeMillis();
                    long daysLeft = timeLeft / (24 * 60 * 60 * 1000);
                    player.sendSystemMessage(Component.literal("§eTime Left: §f" + daysLeft + " days"));
                }
            }
            
            // Protection radius
            player.sendSystemMessage(Component.literal("§eProtection Radius: §f" + claim.getProtectionRadius() + " blocks"));

            // Flags
            player.sendSystemMessage(Component.literal("§6§l=== Claim Flags ==="));
            player.sendSystemMessage(Component.literal("§eExplosions: " + (claim.getAllowExplosions() ? "§aAllowed" : "§cDenied")));
            player.sendSystemMessage(Component.literal("§ePvP: " + (claim.getAllowPvP() ? "§aAllowed" : "§cDenied")));
            player.sendSystemMessage(Component.literal("§eMob Spawning: " + (claim.getAllowMobSpawning() ? "§aAllowed" : "§cDenied")));
            player.sendSystemMessage(Component.literal("§eFire Spread: " + (claim.getAllowFireSpread() ? "§aAllowed" : "§cDenied")));
            
            // Trusted players
            if (!claim.getTrustedPlayers().isEmpty()) {
                player.sendSystemMessage(Component.literal("§6§l=== Trusted Players ==="));
                for (UUID trustedUUID : claim.getTrustedPlayers()) {
                    ServerPlayer trustedPlayer = level.getServer().getPlayerList().getPlayer(trustedUUID);
                    String trustedName = trustedPlayer != null ? trustedPlayer.getName().getString() : trustedUUID.toString();
                    player.sendSystemMessage(Component.literal("§f- " + trustedName));
                }
            }

            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing claim info command", e);
            return 0;
        }
    }

    /**
     * /claim trust <player> - Adds a player to the trusted list
     */
    private static int executeTrust(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            ServerLevel level = player.serverLevel();
            BlockPos pos = player.blockPosition();

            ClaimBlockEntity claim = findClaimForPosition(level, pos);

            if (claim == null) {
                player.sendSystemMessage(Component.literal("§cNo claim found at your location."));
                return 0;
            }

            if (!claim.getOwnerUUID().equals(player.getUUID())) {
                player.sendSystemMessage(Component.literal("§cYou don't own this claim!"));
                return 0;
            }

            claim.addTrustedPlayer(targetPlayer.getUUID());
            player.sendSystemMessage(Component.literal("§aAdded §f" + targetPlayer.getName().getString() + "§a to the trusted list!"));
            
            // Notify the trusted player if online
            targetPlayer.sendSystemMessage(Component.literal("§aYou have been trusted in a claim by §f" + player.getName().getString()));

            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing claim trust command", e);
            return 0;
        }
    }

    /**
     * /claim untrust <player> - Removes a player from the trusted list
     */
    private static int executeUntrust(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            ServerLevel level = player.serverLevel();
            BlockPos pos = player.blockPosition();

            ClaimBlockEntity claim = findClaimForPosition(level, pos);

            if (claim == null) {
                player.sendSystemMessage(Component.literal("§cNo claim found at your location."));
                return 0;
            }

            if (!claim.getOwnerUUID().equals(player.getUUID())) {
                player.sendSystemMessage(Component.literal("§cYou don't own this claim!"));
                return 0;
            }

            claim.removeTrustedPlayer(targetPlayer.getUUID());
            player.sendSystemMessage(Component.literal("§aRemoved §f" + targetPlayer.getName().getString() + "§a from the trusted list!"));
            
            // Notify the untrusted player if online
            targetPlayer.sendSystemMessage(Component.literal("§cYou have been untrusted from a claim by §f" + player.getName().getString()));

            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing claim untrust command", e);
            return 0;
        }
    }

    /**
     * /claim flag <flag> <allow|deny> - Toggles claim flags
     */
    private static int executeFlag(CommandContext<CommandSourceStack> context, String flagName, boolean value) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            ServerLevel level = player.serverLevel();
            BlockPos pos = player.blockPosition();

            ClaimBlockEntity claim = findClaimForPosition(level, pos);

            if (claim == null) {
                player.sendSystemMessage(Component.literal("§cNo claim found at your location."));
                return 0;
            }

            if (!claim.getOwnerUUID().equals(player.getUUID())) {
                player.sendSystemMessage(Component.literal("§cYou don't own this claim!"));
                return 0;
            }

            switch (flagName.toLowerCase()) {
                case "explosions":
                    claim.setAllowExplosions(value);
                    break;
                case "pvp":
                    claim.setAllowPvP(value);
                    break;
                case "mobs":
                    claim.setAllowMobSpawning(value);
                    break;
                case "fire":
                    claim.setAllowFireSpread(value);
                    break;
                default:
                    player.sendSystemMessage(Component.literal("§cUnknown flag: " + flagName));
                    return 0;
            }

            String status = value ? "§aallowed" : "§cdenied";
            player.sendSystemMessage(Component.literal("§eFlag §f" + flagName + "§e is now " + status));

            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing claim flag command", e);
            return 0;
        }
    }

    /**
     * /claim rent <days> - Converts claim to rental for specified days
     */
    private static int executeRent(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            int days = IntegerArgumentType.getInteger(context, "days");
            ServerLevel level = player.serverLevel();
            BlockPos pos = player.blockPosition();

            ClaimBlockEntity claim = findClaimForPosition(level, pos);

            if (claim == null) {
                player.sendSystemMessage(Component.literal("§cNo claim found at your location."));
                return 0;
            }

            if (!claim.getOwnerUUID().equals(player.getUUID())) {
                player.sendSystemMessage(Component.literal("§cYou don't own this claim!"));
                return 0;
            }

            // Calculate rental cost (you can modify this formula)
            double costPerDay = 10.0; // Base cost per day
            double totalCost = days * costPerDay;

            claim.setRented(true, days, totalCost);
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String expirationDate = dateFormat.format(new Date(claim.getExpirationTime()));

            player.sendSystemMessage(Component.literal("§aClaim converted to rental!"));
            player.sendSystemMessage(Component.literal("§eDuration: §f" + days + " days"));
            player.sendSystemMessage(Component.literal("§eCost: §f$" + totalCost));
            player.sendSystemMessage(Component.literal("§eExpires: §f" + expirationDate));

            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing claim rent command", e);
            return 0;
        }
    }

    /**
     * /claim abandon - Removes the claim
     */
    private static int executeAbandon(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            ServerLevel level = player.serverLevel();
            BlockPos pos = player.blockPosition();

            ClaimBlockEntity claim = findClaimForPosition(level, pos);

            if (claim == null) {
                player.sendSystemMessage(Component.literal("§cNo claim found at your location."));
                return 0;
            }

            if (!claim.getOwnerUUID().equals(player.getUUID())) {
                player.sendSystemMessage(Component.literal("§cYou don't own this claim!"));
                return 0;
            }

            // Find the claim block and break it
            BlockPos claimBlockPos = findClaimBlockPosition(level, pos);
            if (claimBlockPos != null) {
                level.destroyBlock(claimBlockPos, true);
                player.sendSystemMessage(Component.literal("§aClaim abandoned successfully!"));
                return 1;
            } else {
                player.sendSystemMessage(Component.literal("§cCouldn't find claim block!"));
                return 0;
            }

        } catch (Exception e) {
            LOGGER.error("Error executing claim abandon command", e);
            return 0;
        }
    }

    /**
     * /claim list - Lists all claims owned by the player
     */
    private static int executeList(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            ServerLevel level = player.serverLevel();

            player.sendSystemMessage(Component.literal("§6§l=== Your Claims ==="));

            List<ClaimRegistry.ClaimData> playerClaims = ClaimRegistry.getPlayerClaims(player.getUUID());
            
            if (playerClaims.isEmpty()) {
                player.sendSystemMessage(Component.literal("§eYou don't own any claims."));
                return 0;
            }
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            
            for (ClaimRegistry.ClaimData claimData : playerClaims) {
                BlockPos pos = claimData.getPosition();
                String name = claimData.getClaimName().isEmpty() ? "§7Unnamed" : "§f" + claimData.getClaimName();
                String location = "§7[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]";
                
                // Try to get the actual claim entity for more details
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof ClaimBlockEntity claim) {
                    if (claim.isRented()) {
                        long timeLeft = claim.getExpirationTime() - System.currentTimeMillis();
                        long daysLeft = timeLeft / (24 * 60 * 60 * 1000);
                        
                        if (claim.isExpired()) {
                            player.sendSystemMessage(Component.literal("§c[EXPIRED] " + name + " " + location));
                        } else {
                            player.sendSystemMessage(Component.literal("§e[RENTED] " + name + " " + location + " §e(" + daysLeft + "d left)"));
                        }
                    } else {
                        player.sendSystemMessage(Component.literal("§a[OWNED] " + name + " " + location));
                    }
                } else {
                    // Claim not loaded, show basic info
                    player.sendSystemMessage(Component.literal("§f" + name + " " + location + " §7(unloaded)"));
                }
            }
            
            player.sendSystemMessage(Component.literal("§eTotal: §f" + playerClaims.size() + " claim" + (playerClaims.size() > 1 ? "s" : "")));

            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing claim list command", e);
            return 0;
        }
    }

    /**
     * Helper: Finds a claim for the given position (same logic as ClaimProtectionHandler)
     */
    private static ClaimBlockEntity findClaimForPosition(ServerLevel level, BlockPos pos) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;

        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        int maxX = minX + 15;
        int maxZ = minZ + 15;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                    BlockPos checkPos = new BlockPos(x, y, z);
                    if (level.getBlockState(checkPos).getBlock() instanceof ClaimBlock) {
                        BlockEntity blockEntity = level.getBlockEntity(checkPos);
                        if (blockEntity instanceof ClaimBlockEntity claimEntity) {
                            int minY = checkPos.getY() - 20;
                            int maxY = checkPos.getY() + 40;

                            if (pos.getY() >= minY && pos.getY() <= maxY) {
                                return claimEntity;
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Helper: Finds the actual claim block position
     */
    private static BlockPos findClaimBlockPosition(ServerLevel level, BlockPos pos) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;

        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        int maxX = minX + 15;
        int maxZ = minZ + 15;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                    BlockPos checkPos = new BlockPos(x, y, z);
                    if (level.getBlockState(checkPos).getBlock() instanceof ClaimBlock) {
                        BlockEntity blockEntity = level.getBlockEntity(checkPos);
                        if (blockEntity instanceof ClaimBlockEntity claimEntity) {
                            int minY = checkPos.getY() - 20;
                            int maxY = checkPos.getY() + 40;

                            if (pos.getY() >= minY && pos.getY() <= maxY) {
                                return checkPos;
                            }
                        }
                    }
                }
            }
        }

        return null;
    }
}
