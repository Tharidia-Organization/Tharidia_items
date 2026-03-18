package com.THproject.tharidia_things.network;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.character.CharacterAttachments;
import com.THproject.tharidia_things.character.CharacterData;
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

import java.lang.reflect.Method;
import java.util.UUID;

import static com.THproject.tharidia_things.TharidiaThings.MODID;

/**
 * Packet sent when player selects a race.
 * Validates the race, saves it in CharacterData, syncs to GodEyeDB, and completes creation.
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
                serverPlayer.sendSystemMessage(Component.literal("\u00a7cRazza non valida: " + selectedRace));
                return;
            }

            // Check that the player is in the correct stage
            CharacterData characterData = serverPlayer.getData(CharacterAttachments.CHARACTER_DATA);
            if (characterData == null) return;

            if (characterData.getStage() != CharacterData.CreationStage.AWAITING_RACE) {
                TharidiaThings.LOGGER.warn("Player {} tried to select race but stage is {} (expected AWAITING_RACE)",
                        serverPlayer.getName().getString(), characterData.getStage());
                serverPlayer.sendSystemMessage(Component.literal("\u00a7cHai già creato il tuo personaggio!"));
                return;
            }

            TharidiaThings.LOGGER.info("Player {} selected race: {}",
                    serverPlayer.getName().getString(), selectedRace);

            // Save race in CharacterData (persistent via NBT)
            characterData.setSelectedRace(selectedRace);

            // Sync race to GodEye database via reflection (tharidia_features)
            syncRaceToDatabase(serverPlayer.getUUID(), selectedRace);

            // Complete character creation (marks COMPLETED, cleans up, teleports to overworld)
            CharacterEventHandler.completeCharacterCreation(serverPlayer);

            // Send welcome effects after teleport
            serverPlayer.getServer().execute(() -> {
                applyWelcomeEffects(serverPlayer, selectedRace);
            });
        });
    }

    /**
     * Syncs the selected race to GodEyeDatabase via reflection.
     * Fails silently if tharidia_features is not loaded.
     */
    private static void syncRaceToDatabase(UUID playerUUID, String race) {
        try {
            Class<?> mainClass = Class.forName("com.THproject.tharidia_features.main");
            Method getDb = mainClass.getMethod("getGodEyeDatabase");
            Object db = getDb.invoke(null);
            if (db != null) {
                Method updateRace = db.getClass().getMethod("updatePlayerRace", UUID.class, String.class);
                updateRace.invoke(db, playerUUID, race);
                TharidiaThings.LOGGER.info("Race '{}' synced to GodEye database for UUID {}", race, playerUUID);
            }
        } catch (ClassNotFoundException e) {
            TharidiaThings.LOGGER.debug("tharidia_features not loaded - skipping race DB sync");
        } catch (Exception e) {
            TharidiaThings.LOGGER.warn("Failed to sync race to GodEye database", e);
        }
    }

    /**
     * Apply visual and audio welcome effects after character creation
     */
    private static void applyWelcomeEffects(ServerPlayer player, String raceName) {
        var raceInfo = RaceData.getRaceInfo(raceName);
        String displayName = raceInfo != null ? raceInfo.name : raceName;

        // Apply wake-up effects
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 1, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 1, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 140, 2, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, false, false));

        // Title animation
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(
                Component.literal("\u00a76\u00a7lBenvenuto in Tharidia!")
        ));
        player.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.literal("\u00a7eHai scelto la via dei \u00a7f" + displayName + "\u00a7e.")
        ));

        // Race-specific sound
        var sound = switch (raceName.toLowerCase()) {
            case "umano" -> SoundEvents.VILLAGER_YES;
            case "elfo" -> SoundEvents.AMETHYST_BLOCK_CHIME;
            case "nano" -> SoundEvents.ANVIL_USE;
            case "dragonide" -> SoundEvents.ENDER_DRAGON_GROWL;
            case "orco" -> SoundEvents.WITHER_SPAWN;
            default -> SoundEvents.PLAYER_LEVELUP;
        };

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                sound, SoundSource.MASTER, 1.0f, 1.0f);
    }
}
