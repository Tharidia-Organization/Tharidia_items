package com.THproject.tharidia_things.mixin;

import com.THproject.tharidia_things.features.Revive;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(ServerGamePacketListenerImpl.class)
public class FallenChatMixin {

    @Shadow
    public ServerPlayer player;

    private static final Random FALLEN_CHAT_RANDOM = new Random();
    private static final String[] FALLEN_CHAT_MESSAGES = {
        "*MmmMm*",
        "*Mhm-Mmm-Hhm*",
        "*Ahh-A-Ahh*",
        "*Uff-phh*"
    };

    @Inject(method = "handleChat", at = @At("HEAD"), cancellable = true)
    private void onHandleChat(ServerboundChatPacket packet, CallbackInfo ci) {
        if (player == null || !Revive.isPlayerFallen(player)) return;

        ci.cancel();

        String randomMessage = FALLEN_CHAT_MESSAGES[FALLEN_CHAT_RANDOM.nextInt(FALLEN_CHAT_MESSAGES.length)];
        Component fakeMessage = Component.translatable("chat.type.text",
                player.getDisplayName(), Component.literal(randomMessage));
        player.server.getPlayerList().broadcastSystemMessage(fakeMessage, false);
    }
}
