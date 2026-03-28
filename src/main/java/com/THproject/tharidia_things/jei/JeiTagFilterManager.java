package com.THproject.tharidia_things.jei;

import com.THproject.tharidia_things.TharidiaThings;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Client-side JEI filter manager.
 *
 * The server computes which items this player is allowed to see and sends the
 * pre-computed list via {@link com.THproject.tharidia_things.network.JeiFilterSyncPacket}.
 * This class simply applies that list to JEI — no client-side tag reading.
 *
 * When a filter is active, ALL fluids are hidden (they are never part of the
 * item-based whitelist).
 */
public class JeiTagFilterManager {

    private static IJeiRuntime jeiRuntime;
    private static List<ItemStack> allOriginalItems = Collections.emptyList();
    private static List<FluidStack> allOriginalFluids = Collections.emptyList();
    private static final List<ItemStack> currentlyHiddenItems = new ArrayList<>();
    private static final List<FluidStack> currentlyHiddenFluids = new ArrayList<>();

    /**
     * Item IDs the server says this player is allowed to see.
     * {@code null}  → packet not yet received, show everything.
     * Empty list   → no filter active, show everything.
     * Non-empty    → hide everything not in this list (fluids always hidden).
     */
    private static List<ResourceLocation> allowedItems = null;
    private static boolean filterDirty = false;

    // -------------------------------------------------------------------------
    // Called by the network packet handler (client side)
    // -------------------------------------------------------------------------

    public static void setAllowedItems(List<ResourceLocation> items) {
        TharidiaThings.LOGGER.info("[Tharidia JEI] setAllowedItems: {} item(s)", items.size());
        allowedItems = items;
        filterDirty = true;
        applyFilterIfReady();
    }

    // -------------------------------------------------------------------------
    // JEI lifecycle hook
    // -------------------------------------------------------------------------

    public static void onJeiRuntimeAvailable(IJeiRuntime runtime) {
        jeiRuntime = runtime;
        IIngredientManager mgr = runtime.getIngredientManager();
        allOriginalItems = new ArrayList<>(mgr.getAllIngredients(VanillaTypes.ITEM_STACK));
        allOriginalFluids = new ArrayList<>(mgr.getAllIngredients(NeoForgeTypes.FLUID_STACK));
        currentlyHiddenItems.clear();
        currentlyHiddenFluids.clear();
        TharidiaThings.LOGGER.info("[Tharidia JEI] Runtime available. Captured {} items, {} fluids.",
                allOriginalItems.size(), allOriginalFluids.size());
        applyFilterIfReady();
    }

    // -------------------------------------------------------------------------
    // Periodic retry (called by JeiClientEventHandler every 60 client-ticks)
    // -------------------------------------------------------------------------

    public static void checkAndApplyFilter() {
        if (filterDirty) applyFilterIfReady();
    }

    // -------------------------------------------------------------------------
    // Core filter logic
    // -------------------------------------------------------------------------

    private static void applyFilterIfReady() {
        if (jeiRuntime == null || allOriginalItems.isEmpty()) {
            TharidiaThings.LOGGER.debug("[Tharidia JEI] applyFilterIfReady: JEI not ready yet");
            return;
        }

        IIngredientManager mgr = jeiRuntime.getIngredientManager();

        // Restore previously hidden items
        if (!currentlyHiddenItems.isEmpty()) {
            TharidiaThings.LOGGER.info("[Tharidia JEI] Restoring {} previously hidden items", currentlyHiddenItems.size());
            mgr.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, new ArrayList<>(currentlyHiddenItems));
            currentlyHiddenItems.clear();
        }

        // Restore previously hidden fluids
        if (!currentlyHiddenFluids.isEmpty()) {
            TharidiaThings.LOGGER.info("[Tharidia JEI] Restoring {} previously hidden fluids", currentlyHiddenFluids.size());
            mgr.addIngredientsAtRuntime(NeoForgeTypes.FLUID_STACK, new ArrayList<>(currentlyHiddenFluids));
            currentlyHiddenFluids.clear();
        }

        // Apply new filter (null or empty list = show everything)
        if (allowedItems != null && !allowedItems.isEmpty()) {
            Set<ResourceLocation> allowed = new HashSet<>(allowedItems);

            // Hide items not in the whitelist
            List<ItemStack> itemsToHide = allOriginalItems.stream()
                    .filter(stack -> !allowed.contains(BuiltInRegistries.ITEM.getKey(stack.getItem())))
                    .collect(Collectors.toList());

            if (!itemsToHide.isEmpty()) {
                mgr.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, itemsToHide);
                currentlyHiddenItems.addAll(itemsToHide);
            }

            // Hide all fluids — they are never part of the item-based whitelist
            if (!allOriginalFluids.isEmpty()) {
                mgr.removeIngredientsAtRuntime(NeoForgeTypes.FLUID_STACK, new ArrayList<>(allOriginalFluids));
                currentlyHiddenFluids.addAll(allOriginalFluids);
            }

            TharidiaThings.LOGGER.info("[Tharidia JEI] Hiding {}/{} items and {}/{} fluids",
                    itemsToHide.size(), allOriginalItems.size(),
                    allOriginalFluids.size(), allOriginalFluids.size());
        } else {
            TharidiaThings.LOGGER.info("[Tharidia JEI] No filter active, full list visible");
        }

        filterDirty = false;
    }

    // -------------------------------------------------------------------------
    // Player leave cleanup
    // -------------------------------------------------------------------------

    public static void onPlayerLeave() {
        if (jeiRuntime != null) {
            IIngredientManager mgr = jeiRuntime.getIngredientManager();
            if (!currentlyHiddenItems.isEmpty()) {
                TharidiaThings.LOGGER.info("[Tharidia JEI] Restoring {} items on player leave", currentlyHiddenItems.size());
                mgr.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, new ArrayList<>(currentlyHiddenItems));
                currentlyHiddenItems.clear();
            }
            if (!currentlyHiddenFluids.isEmpty()) {
                TharidiaThings.LOGGER.info("[Tharidia JEI] Restoring {} fluids on player leave", currentlyHiddenFluids.size());
                mgr.addIngredientsAtRuntime(NeoForgeTypes.FLUID_STACK, new ArrayList<>(currentlyHiddenFluids));
                currentlyHiddenFluids.clear();
            }
        }
        allowedItems = null;
        filterDirty = false;
    }
}
