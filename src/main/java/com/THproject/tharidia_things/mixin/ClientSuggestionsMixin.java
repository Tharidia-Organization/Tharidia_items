package com.THproject.tharidia_things.mixin;

import com.THproject.tharidia_things.TharidiaThings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.Collections;

/**
 * Client-side mixin to block player name suggestions in chat tab completion.
 * This intercepts getOnlinePlayerNames() and getAllPlayerNames() on ClientSuggestionProvider
 * to return empty collections for non-admin players.
 * Players with OP level 4 can still see player name suggestions.
 */
@Mixin(value = ClientSuggestionProvider.class, priority = 500)
public class ClientSuggestionsMixin {

    private static final int REQUIRED_OP_LEVEL = 4;

    /**
     * Check if the local player has admin permissions (OP level 4)
     * Uses hasPermissions() which checks permission level on the client
     */
    private boolean isAdmin() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            TharidiaThings.LOGGER.debug("[ClientSuggestions] Player is null, denying access");
            return false;
        }

        int permLevel = player.getPermissionLevel();
        boolean hasPerms = player.hasPermissions(REQUIRED_OP_LEVEL);

        TharidiaThings.LOGGER.debug("[ClientSuggestions] Permission check - Level: {}, hasPermissions(4): {}",
            permLevel, hasPerms);

        return hasPerms;
    }

    @Inject(method = "getOnlinePlayerNames", at = @At("HEAD"), cancellable = true)
    private void onGetOnlinePlayerNames(CallbackInfoReturnable<Collection<String>> cir) {
        boolean admin = isAdmin();
        TharidiaThings.LOGGER.debug("[ClientSuggestions] getOnlinePlayerNames called, isAdmin: {}", admin);

        // Allow admins to see player names
        if (admin) {
            return;
        }
        // Block for non-admins
        cir.setReturnValue(Collections.emptyList());
    }

    @Inject(method = "getAllPlayerNames", at = @At("HEAD"), cancellable = true)
    private void onGetAllPlayerNames(CallbackInfoReturnable<Collection<String>> cir) {
        boolean admin = isAdmin();
        TharidiaThings.LOGGER.debug("[ClientSuggestions] getAllPlayerNames called, isAdmin: {}", admin);

        // Allow admins to see player names
        if (admin) {
            return;
        }
        // Block for non-admins
        cir.setReturnValue(Collections.emptyList());
    }
}
