package com.THproject.tharidia_things.event;

import com.THproject.tharidia_things.block.entity.CookTableBlockEntity;
import com.THproject.tharidia_things.cook.CookRecipeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Optional;
import java.util.UUID;

/**
 * Detects when a player successfully crafts the result item of their active cooking session
 * and completes the session without rotting the tagged ingredients.
 */
public class CookingCompletionHandler {

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() == null || event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        UUID playerUUID = sp.getUUID();
        BlockPos tablePos = CookTableBlockEntity.ACTIVE_SESSIONS.get(playerUUID);
        if (tablePos == null) return;

        if (!(sp.level() instanceof ServerLevel sl)) return;
        if (!(sl.getBlockEntity(tablePos) instanceof CookTableBlockEntity be)) return;

        String recipeId = be.getActiveRecipeId();
        if (recipeId.isEmpty()) return;

        // Look up the actual recipe to get its result item
        ResourceLocation recipeRL = ResourceLocation.tryParse(recipeId);
        if (recipeRL == null) return;

        Optional<RecipeHolder<?>> holderOpt = CookRecipeRegistry.getHolder(recipeRL);
        if (holderOpt.isEmpty()) return;

        ItemStack expectedResult = holderOpt.get().value().getResultItem(sl.registryAccess());
        ItemStack crafted = event.getCrafting();

        if (expectedResult.isEmpty() || !crafted.is(expectedResult.getItem())) return;

        // Success — complete the session without rotting
        be.completeCooking(sp);
    }
}
