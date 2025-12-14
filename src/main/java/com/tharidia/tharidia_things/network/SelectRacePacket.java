package com.tharidia.tharidia_things.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.tharidia.tharidia_things.TharidiaThings.MODID;

/**
 * Packet sent when player selects a race
 */
public record SelectRacePacket(String raceName) implements CustomPacketPayload {
    public static final Type<SelectRacePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "select_race"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectRacePacket> STREAM_CODEC = StreamCodec.ofMember(
        SelectRacePacket::write, SelectRacePacket::new
    );
    
    public SelectRacePacket(FriendlyByteBuf buf) {
        this(buf.readUtf());
    }
    
    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(raceName);
    }
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public static void handle(SelectRacePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (player != null) {
                // Set the race using the same logic as the command
                player.getServer().getCommands().performPrefixedCommand(
                    player.createCommandSourceStack(),
                    "tharidia race set " + packet.raceName()
                );
                
                // Create the character
                player.getServer().getCommands().performPrefixedCommand(
                    player.createCommandSourceStack(),
                    "tharidia character create"
                );
                
                // Clean up the player's area in the character dimension
                cleanupPlayerArea((net.minecraft.server.level.ServerPlayer) player);
                
                // Teleport player back to spawn/world
                var server = player.getServer();
                var overworld = server.overworld();
                var spawnPos = overworld.getSharedSpawnPos();
                player.teleportTo(overworld, spawnPos.getX() + 0.5, spawnPos.getY() + 1, spawnPos.getZ() + 0.5,
                    java.util.Set.of(), player.getYRot(), player.getXRot());
                
                // Send welcome title and race-specific sound after a short delay
                var raceInfo = com.tharidia.tharidia_things.character.RaceData.getRaceInfo(packet.raceName());
                String displayName = raceInfo != null ? raceInfo.name : packet.raceName();
                var serverPlayer = (net.minecraft.server.level.ServerPlayer) player;
                
                // Apply realistic wake-up effect using multiple potion effects
                var blindness = net.minecraft.world.effect.MobEffects.BLINDNESS;
                var darkness = net.minecraft.world.effect.MobEffects.DARKNESS;
                var slowness = net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN;
                var nausea = net.minecraft.world.effect.MobEffects.CONFUSION;
                
                // Schedule wake-up effects to be sent after 1 tick to ensure player is fully loaded
                server.execute(() -> {
                    // Send the packets on the main thread after teleport completes
                    com.tharidia.tharidia_things.TharidiaThings.LOGGER.info("Sending race selection title and sound for player: {}", player.getName().getString());
                    
                    // Phase 1: Complete darkness (first 4 seconds) - like being unconscious
                    serverPlayer.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        blindness, 80, 1, false, false, false
                    ));
                    serverPlayer.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        darkness, 80, 1, false, false, false
                    ));
                    serverPlayer.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        slowness, 140, 2, false, false, false
                    ));
                    serverPlayer.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        nausea, 100, 0, false, false, false
                    ));
                    
                    // Set title animation (fadeIn: 10, stay: 70, fadeOut: 20 ticks)
                    serverPlayer.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
                    
                    // Send title
                    serverPlayer.connection.send(new ClientboundSetTitleTextPacket(
                        Component.literal("§6§lBenvenuto in Tharidia!").withStyle(net.minecraft.ChatFormatting.BOLD)
                    ));
                    
                    // Send subtitle with race
                    serverPlayer.connection.send(new ClientboundSetSubtitleTextPacket(
                        Component.literal("§eHai scelto la via dei §f" + displayName + "§e.")
                    ));
                    
                    // Play race-specific sound
                    net.minecraft.sounds.SoundEvent sound = switch (packet.raceName()) {
                        case "umano" -> SoundEvents.VILLAGER_YES;
                        case "elfo" -> SoundEvents.AMETHYST_BLOCK_CHIME;
                        case "nano" -> SoundEvents.ANVIL_USE;
                        case "dragonide" -> SoundEvents.ENDER_DRAGON_GROWL;
                        case "orcho" -> SoundEvents.WITHER_SPAWN;
                        default -> SoundEvents.PLAYER_LEVELUP;
                    };
                    
                    serverPlayer.level().playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(), 
                        sound, SoundSource.MASTER, 1.0f, 1.0f);
                });
            }
        });
    }
    
    private static void cleanupPlayerArea(net.minecraft.server.level.ServerPlayer player) {
        var characterLevel = player.getServer().getLevel(
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, 
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("tharidiathings", "character_creation"))
        );
        
        if (characterLevel != null) {
            // Calculate player's area based on UUID
            int playerHash = player.getUUID().hashCode();
            int chunkSpacing = 100;
            int playerChunkX = (playerHash % 1000) * chunkSpacing;
            int playerChunkZ = ((playerHash / 1000) % 1000) * chunkSpacing;
            
            // Clear chunks in the player's area
            int radius = 2; // Clear 5x5 chunks around center
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    ChunkPos chunkPos = new ChunkPos(playerChunkX + x, playerChunkZ + z);
                    if (characterLevel.hasChunk(chunkPos.x, chunkPos.z)) {
                        LevelChunk chunk = characterLevel.getChunk(chunkPos.x, chunkPos.z);
                        // Clear all entities
                        characterLevel.getEntities().getAll().forEach(entity -> {
                            if (entity.chunkPosition().equals(chunkPos)) {
                                entity.discard();
                            }
                        });
                        // Reset all blocks to air (limited Y range for performance)
                        for (int bx = 0; bx < 16; bx++) {
                            for (int bz = 0; bz < 16; bz++) {
                                for (int by = 80; by < 120; by++) { // Only clear reasonable build height
                                    characterLevel.setBlock(
                                        new BlockPos(chunkPos.getMinBlockX() + bx, by, chunkPos.getMinBlockZ() + bz),
                                        net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3
                                    );
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
