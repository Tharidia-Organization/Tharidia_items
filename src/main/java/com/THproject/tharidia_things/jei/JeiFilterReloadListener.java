package com.THproject.tharidia_things.jei;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.network.JeiFilterSyncPacket;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Server-side reload listener for {@code data/tharidiathings/jei_filters.json}.
 *
 * Reloads on server start and on every {@code /reload}. After each load it
 * re-syncs all online players.
 *
 * Per-player sync logic:
 * - The server reads the player's entity tags (server-side, always accurate).
 * - It computes the union of allowed item IDs for all active managed tags.
 * - It sends {@link JeiFilterSyncPacket} only when the active-tag set changes.
 * - An empty allowedItemIds list means "no filter active – show everything".
 *
 * JSON format (in a datapack or the mod's own {@code data/} folder):
 * <pre>
 * {
 *   "cook": ["minecraft:bread", "minecraft:cooked_beef"],
 *   "blacksmith": ["minecraft:iron_sword"]
 * }
 * </pre>
 */
public class JeiFilterReloadListener
        extends SimplePreparableReloadListener<Map<String, List<String>>> {

    public static final JeiFilterReloadListener INSTANCE = new JeiFilterReloadListener();

    private static final ResourceLocation RESOURCE =
            ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "jei_filters.json");

    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE =
            new TypeToken<Map<String, List<String>>>() {}.getType();

    /** Raw filter table: managedTag → list of item ID strings. */
    private static Map<String, List<String>> currentFilters = new HashMap<>();

    /** Last active-tag set per player UUID — avoids redundant packet sends. */
    private static final Map<UUID, Set<String>> playerTagCache = new HashMap<>();

    // -------------------------------------------------------------------------
    // SimplePreparableReloadListener
    // -------------------------------------------------------------------------

    @Override
    protected Map<String, List<String>> prepare(ResourceManager manager, ProfilerFiller profiler) {
        var resource = manager.getResource(RESOURCE);
        if (resource.isEmpty()) {
            TharidiaThings.LOGGER.warn("[Tharidia JEI] jei_filters.json not found in any datapack.");
            return new HashMap<>();
        }
        try (var reader = resource.get().openAsReader()) {
            Map<String, List<String>> result = GSON.fromJson(reader, MAP_TYPE);
            TharidiaThings.LOGGER.info("[Tharidia JEI] Loaded jei_filters.json ({} tags).",
                    result != null ? result.size() : 0);
            return result != null ? result : new HashMap<>();
        } catch (Exception e) {
            TharidiaThings.LOGGER.error("[Tharidia JEI] Failed to parse jei_filters.json: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    @Override
    protected void apply(Map<String, List<String>> result,
                         ResourceManager manager, ProfilerFiller profiler) {
        currentFilters = result;
        // Clear cache so every player gets a fresh sync with the new filter data.
        playerTagCache.clear();

        net.minecraft.server.MinecraftServer server =
                net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
            for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
                syncPlayer(sp);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Per-player sync
    // -------------------------------------------------------------------------

    /**
     * Computes the allowed item list for {@code sp} based on their current entity tags
     * and sends a packet only if the active-tag set has changed since the last sync.
     */
    public static void syncPlayer(ServerPlayer sp) {
        Set<String> playerTags = sp.getTags();
        Set<String> activeTags = playerTags.stream()
                .filter(currentFilters::containsKey)
                .collect(Collectors.toSet());

        Set<String> cached = playerTagCache.get(sp.getUUID());
        if (activeTags.equals(cached)) return;

        playerTagCache.put(sp.getUUID(), new HashSet<>(activeTags));

        List<String> allowedItemIds;
        if (activeTags.isEmpty()) {
            // No managed tag active → send empty list (= show everything)
            allowedItemIds = Collections.emptyList();
        } else {
            allowedItemIds = activeTags.stream()
                    .flatMap(tag -> currentFilters.get(tag).stream())
                    .distinct()
                    .collect(Collectors.toList());
        }

        TharidiaThings.LOGGER.info("[Tharidia JEI] Syncing player {} — activeTags={}, allowedItems={}",
                sp.getName().getString(), activeTags, allowedItemIds.size());
        PacketDistributor.sendToPlayer(sp, new JeiFilterSyncPacket(allowedItemIds));
    }

    // -------------------------------------------------------------------------
    // NeoForge event hooks
    // -------------------------------------------------------------------------

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    /** Initial sync when the player joins. */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            syncPlayer(sp);
        }
    }

    /** Remove stale cache entry when the player leaves. */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        playerTagCache.remove(event.getEntity().getUUID());
    }

    /**
     * Periodic server-side check (every 60 game ticks ≈ 3 s) so that tag changes
     * applied via {@code /tag} are picked up without requiring a /reload.
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().getGameTime() % 60 != 0) return;
        syncPlayer(sp);
    }
}
