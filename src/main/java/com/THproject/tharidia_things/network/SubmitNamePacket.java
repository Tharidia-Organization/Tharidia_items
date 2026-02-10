package com.THproject.tharidia_things.network;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.character.CharacterAttachments;
import com.THproject.tharidia_things.character.CharacterData;
import com.THproject.tharidia_things.character.CharacterEventHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.regex.Pattern;

import static com.THproject.tharidia_things.TharidiaThings.MODID;

/**
 * Packet sent from client to server when player submits their chosen name.
 * Validates server-side, calls NameService, and advances character creation flow.
 */
public record SubmitNamePacket(String chosenName) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SubmitNamePacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "submit_name"));

    public static final StreamCodec<ByteBuf, SubmitNamePacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        SubmitNamePacket::chosenName,
        SubmitNamePacket::new
    );

    // Server-side validation (mirrors client rules)
    private static final int MIN_NAME_LENGTH = 3;
    private static final int MAX_NAME_LENGTH = 16;
    private static final Pattern VALID_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Handles the packet on the server side.
     * Validates name, submits to NameService, sends response, and advances character creation.
     */
    public static void handle(SubmitNamePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }

            String name = packet.chosenName() != null ? packet.chosenName().trim() : "";

            // Server-side validation — never trust the client
            String validationError = validateName(name);
            if (validationError != null) {
                TharidiaThings.LOGGER.warn("Player {} sent invalid name '{}': {}",
                    serverPlayer.getName().getString(), name, validationError);
                PacketDistributor.sendToPlayer(serverPlayer,
                    new NameResponsePacket(false, validationError));
                return;
            }

            try {
                // Use reflection to call NameService (server-side only dependency)
                Class<?> nameServiceClass = Class.forName("com.THproject.tharidia_tweaks.name.NameService");
                java.lang.reflect.Method submitMethod = nameServiceClass.getMethod(
                    "submitDisplayName", ServerPlayer.class, String.class);

                Object result = submitMethod.invoke(null, serverPlayer, name);

                // Get ValidationResult methods via reflection
                Class<?> resultClass = result.getClass();
                java.lang.reflect.Method okMethod = resultClass.getMethod("ok");
                java.lang.reflect.Method sanitizedMethod = resultClass.getMethod("sanitized");
                java.lang.reflect.Method errorMethod = resultClass.getMethod("error");

                boolean ok = (boolean) okMethod.invoke(result);

                if (ok) {
                    String sanitized = (String) sanitizedMethod.invoke(result);
                    TharidiaThings.LOGGER.info("Player {} chose name: {}",
                        serverPlayer.getName().getString(), sanitized);

                    // Send success response to client (closes the name screen)
                    PacketDistributor.sendToPlayer(serverPlayer,
                        new NameResponsePacket(true, sanitized));

                    // Advance character creation: NOT_STARTED → AWAITING_RACE
                    CharacterData characterData = serverPlayer.getData(CharacterAttachments.CHARACTER_DATA);
                    if (characterData != null && characterData.getStage() == CharacterData.CreationStage.NOT_STARTED) {
                        characterData.setStage(CharacterData.CreationStage.AWAITING_RACE);

                        // Teleport to character creation dimension after a tick
                        serverPlayer.server.execute(() -> {
                            CharacterEventHandler.teleportToCharacterDimension(serverPlayer);
                        });
                    }
                } else {
                    String error = (String) errorMethod.invoke(result);
                    TharidiaThings.LOGGER.warn("Player {} failed to set name: {}",
                        serverPlayer.getName().getString(), error);

                    // Send rejection to client (re-enables the name screen)
                    PacketDistributor.sendToPlayer(serverPlayer,
                        new NameResponsePacket(false, error));
                }
            } catch (ClassNotFoundException e) {
                TharidiaThings.LOGGER.error("tharidia_tweaks mod not found! NameService is required.", e);
                PacketDistributor.sendToPlayer(serverPlayer,
                    new NameResponsePacket(false, "Name service unavailable. Contact server admin."));
            } catch (Exception e) {
                TharidiaThings.LOGGER.error("Error submitting name for player {}",
                    serverPlayer.getName().getString(), e);
                PacketDistributor.sendToPlayer(serverPlayer,
                    new NameResponsePacket(false, "Internal error. Contact server admin."));
            }
        });
    }

    /**
     * Server-side name validation. Returns null if valid, error message if invalid.
     */
    private static String validateName(String name) {
        if (name == null || name.isEmpty()) {
            return "Il nome non può essere vuoto";
        }
        if (name.length() < MIN_NAME_LENGTH) {
            return "Il nome deve essere lungo almeno " + MIN_NAME_LENGTH + " caratteri";
        }
        if (name.length() > MAX_NAME_LENGTH) {
            return "Il nome non può superare " + MAX_NAME_LENGTH + " caratteri";
        }
        if (!VALID_NAME_PATTERN.matcher(name).matches()) {
            return "Il nome può contenere solo lettere, numeri e underscore";
        }
        return null;
    }
}
