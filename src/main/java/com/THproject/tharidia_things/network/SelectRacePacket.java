package com.THproject.tharidia_things.network;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.character.CharacterEventHandler;
import com.THproject.tharidia_things.character.RaceData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.THproject.tharidia_things.TharidiaThings.MODID;

/**
 * Packet sent when player selects a race.
 * Handles race selection and character creation completion.
 */
public record SelectRacePacket(String raceName) implements CustomPacketPayload {
    public static final Type<SelectRacePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MODID, "select_race"));

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
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return;
            }

            String selectedRace = packet.raceName();

            // Validate race selection
            if (!RaceData.isValidRace(selectedRace)) {
                TharidiaThings.LOGGER.warn("Player {} tried to select invalid race: {}",
                        serverPlayer.getName().getString(), selectedRace);
                serverPlayer.sendSystemMessage(Component.literal("§cRazza non valida: " + selectedRace));
                return;
            }

            // Check if player already completed character creation
            if (CharacterEventHandler.hasCompletedCharacterCreation(serverPlayer)) {
                TharidiaThings.LOGGER.warn("Player {} tried to select race but already has character",
                        serverPlayer.getName().getString());
                serverPlayer.sendSystemMessage(Component.literal("§cHai già creato il tuo personaggio!"));
                return;
            }

            TharidiaThings.LOGGER.info("Player {} selected race: {}",
                    serverPlayer.getName().getString(), selectedRace);

            // Set the race using the tharidia command (from tharidia_features mod)
            // This is the official way to set the race
            try {
                serverPlayer.getServer().getCommands().performPrefixedCommand(
                        serverPlayer.createCommandSourceStack().withSuppressedOutput(),
                        "tharidia race set " + selectedRace
                );
            } catch (Exception e) {
                TharidiaThings.LOGGER.warn("Failed to set race for player {}: {}",
                        serverPlayer.getName().getString(), e.getMessage());
            }

            // Complete character creation using centralized method
            // This handles: marking as created, removing border, cleanup, teleport, game mode
            CharacterEventHandler.completeCharacterCreation(serverPlayer);

            // Send welcome effects after a short delay to ensure player is fully teleported
            serverPlayer.getServer().execute(() -> {
                applyWelcomeEffects(serverPlayer, selectedRace);
            });
        });
    }

    /**
     * Apply visual and audio welcome effects after character creation
     */
    private static void applyWelcomeEffects(ServerPlayer player, String raceName) {
        TharidiaThings.LOGGER.info("Sending race selection effects for player: {}",
                player.getName().getString());

        // Get race display name
        var raceInfo = RaceData.getRaceInfo(raceName);
        String displayName = raceInfo != null ? raceInfo.name : raceName;

        // Apply wake-up effects
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 1, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 1, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 140, 2, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, false, false));

        // Set title animation
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));

        // Send title
        player.connection.send(new ClientboundSetTitleTextPacket(
                Component.literal("§6§lBenvenuto in Tharidia!")
        ));

        // Send subtitle with race
        player.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.literal("§eHai scelto la via dei §f" + displayName + "§e.")
        ));

        // Play race-specific sound - FIXED: "orcho" -> "orco"
        var sound = switch (raceName.toLowerCase()) {
            case "umano" -> SoundEvents.VILLAGER_YES;
            case "elfo" -> SoundEvents.AMETHYST_BLOCK_CHIME;
            case "nano" -> SoundEvents.ANVIL_USE;
            case "dragonide" -> SoundEvents.ENDER_DRAGON_GROWL;
            case "orco" -> SoundEvents.WITHER_SPAWN;  // FIXED: was "orcho"
            default -> SoundEvents.PLAYER_LEVELUP;
        };

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                sound, SoundSource.MASTER, 1.0f, 1.0f);
    }
}
