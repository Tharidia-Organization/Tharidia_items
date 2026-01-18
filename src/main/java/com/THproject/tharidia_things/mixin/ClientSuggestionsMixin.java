package com.THproject.tharidia_things.mixin;

import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.Collections;

/**
 * Client-side mixin to block player name suggestions in chat tab completion.
 * This intercepts getOnlinePlayers() and getAllPlayers() on ClientSuggestionProvider
 * to return empty collections, preventing player names from appearing when pressing TAB in chat.
 */
@Mixin(value = ClientSuggestionProvider.class, priority = 500)
public class ClientSuggestionsMixin {

    @Inject(method = "getOnlinePlayerNames", at = @At("HEAD"), cancellable = true)
    private void onGetOnlinePlayerNames(CallbackInfoReturnable<Collection<String>> cir) {
        System.out.println("[TharidiaThings] MIXIN: Blocking getOnlinePlayerNames");
        cir.setReturnValue(Collections.emptyList());
    }

    @Inject(method = "getAllPlayerNames", at = @At("HEAD"), cancellable = true)
    private void onGetAllPlayerNames(CallbackInfoReturnable<Collection<String>> cir) {
        System.out.println("[TharidiaThings] MIXIN: Blocking getAllPlayerNames");
        cir.setReturnValue(Collections.emptyList());
    }
}
