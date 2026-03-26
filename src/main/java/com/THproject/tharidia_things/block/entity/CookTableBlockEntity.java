package com.THproject.tharidia_things.block.entity;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.cook.CookRecipeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class CookTableBlockEntity extends BlockEntity implements GeoBlockEntity {

    // ── GeckoLib ─────────────────────────────────────────────────────────────
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation REGISTRY_ANIM = RawAnimation.begin().thenPlay("registry");

    // ── Cassa (dispensa) inventory ────────────────────────────────────────────
    public static final int CASSA_SIZE = 9;
    private final SimpleContainer cassaContainer = new SimpleContainer(CASSA_SIZE);

    // ── Cooking session state (server authoritative) ──────────────────────────
    private String activeRecipeId        = "";
    private String activeResultName      = "";  // synced to client for HUD display
    private ResourceLocation activeResultItemId = null; // result item for completion polling
    private int initialResultCount       = 0;   // how many of result item player had at session start
    private int timerTicks               = 0;
    private int totalTimerTicks          = 0;
    private UUID cookingPlayerUUID       = null;
    private String sessionId             = "";   // used to tag items for anti-exploit

    // Rot range: scan for dropped items within this radius of the cook table
    private static final double ROT_RADIUS = 10.0;
    private static final String SESSION_NBT_KEY   = "tharidiathings_cooking_session";
    private static final String SESSION_COUNT_KEY = "tharidiathings_cooking_count";

    /**
     * Static map: playerUUID → BlockPos of their active cook table.
     * Used by CookingCompletionHandler to find the right BlockEntity on ItemCraftedEvent.
     */
    public static final java.util.concurrent.ConcurrentHashMap<UUID, BlockPos> ACTIVE_SESSIONS
            = new java.util.concurrent.ConcurrentHashMap<>();

    public CookTableBlockEntity(BlockPos pos, BlockState state) {
        super(TharidiaThings.COOK_TABLE_BLOCK_ENTITY.get(), pos, state);
    }

    // ── GeckoLib ──────────────────────────────────────────────────────────────
    public void playRegistryAnimation() {
        this.triggerAnim("registry_ctrl", "registry");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(
            new AnimationController<>(this, "registry_ctrl", 0, state -> PlayState.STOP)
                .triggerableAnim("registry", REGISTRY_ANIM)
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    // ── Sync ──────────────────────────────────────────────────────────────────
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putString("activeRecipeId", activeRecipeId);
        tag.putString("activeResultName", activeResultName);
        tag.putInt("timerTicks", timerTicks);
        tag.putInt("totalTimerTicks", totalTimerTicks);
        return tag;
    }

    private void syncToClients() {
        if (level instanceof ServerLevel sl) {
            sl.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ── Cooking session ───────────────────────────────────────────────────────
    public boolean isCooking() {
        return !activeRecipeId.isEmpty() && timerTicks > 0;
    }

    public String getActiveRecipeId()   { return activeRecipeId; }
    public String getActiveResultName() { return activeResultName; }
    public int getTimerTicks()          { return timerTicks; }
    public int getTotalTimerTicks()     { return totalTimerTicks; }

    /**
     * Called server-side when a player selects a recipe from the GUI.
     * Looks up the real MC RecipeHolder, checks ingredients, then tags them.
     */
    public boolean startCooking(String recipeId, net.minecraft.server.level.ServerPlayer player) {
        if (isCooking()) return false;
        if (!player.getTags().contains("cook")) return false;

        net.minecraft.resources.ResourceLocation recipeRL =
                net.minecraft.resources.ResourceLocation.tryParse(recipeId);
        if (recipeRL == null) return false;

        Optional<RecipeHolder<?>> holderOpt = CookRecipeRegistry.getHolder(recipeRL);
        if (holderOpt.isEmpty()) return false;
        RecipeHolder<?> holder = holderOpt.get();

        java.util.List<Ingredient> ingredients = holder.value().getIngredients().stream()
                .filter(ing -> !ing.isEmpty()).toList();

        if (!hasIngredients(ingredients, player)) {
            player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal("§cNon hai tutti gli ingredienti necessari."));
            return false;
        }

        // Determine cook time from the result item
        net.minecraft.world.item.ItemStack result =
                holder.value().getResultItem(player.level().registryAccess());
        net.minecraft.resources.ResourceLocation itemId =
                BuiltInRegistries.ITEM.getKey(result.getItem());

        activeRecipeId      = recipeId;
        activeResultName    = result.getHoverName().getString();
        activeResultItemId  = itemId;
        initialResultCount  = countItemInInventory(player, result.getItem());
        timerTicks          = CookRecipeRegistry.getTimeForItem(itemId);
        totalTimerTicks     = timerTicks;
        cookingPlayerUUID   = player.getUUID();
        sessionId           = UUID.randomUUID().toString();

        tagIngredients(ingredients, player);
        ACTIVE_SESSIONS.put(player.getUUID(), worldPosition);

        setChanged();
        syncToClients();

        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.FURNACE_FIRE_CRACKLE,
                    SoundSource.BLOCKS, 1.0f, 1.2f);
        }
        return true;
    }

    public void cancelCooking() {
        if (cookingPlayerUUID != null) {
            ACTIVE_SESSIONS.remove(cookingPlayerUUID);
        }
        activeRecipeId     = "";
        activeResultName   = "";
        activeResultItemId = null;
        initialResultCount = 0;
        timerTicks         = 0;
        totalTimerTicks    = 0;
        cookingPlayerUUID  = null;
        sessionId          = "";
        setChanged();
        syncToClients();
    }

    /**
     * Called when the player successfully crafts the recipe result before the timer expires.
     * Clears the session without rotting ingredients.
     */
    public void completeCooking(net.minecraft.server.level.ServerPlayer player) {
        if (!isCooking()) return; // guard against double-call (event + polling)

        // Capture name before clearing
        String completedName = activeResultName.isEmpty() ? "il piatto" : activeResultName;

        // Remove session tags from remaining items (no rot)
        clearSessionTags(player);

        if (cookingPlayerUUID != null) {
            ACTIVE_SESSIONS.remove(cookingPlayerUUID);
        }
        activeRecipeId     = "";
        activeResultName   = "";
        activeResultItemId = null;
        initialResultCount = 0;
        timerTicks         = 0;
        totalTimerTicks    = 0;
        cookingPlayerUUID  = null;
        sessionId          = "";
        setChanged();
        syncToClients();

        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.PLAYER_LEVELUP,
                    SoundSource.PLAYERS, 0.6f, 1.0f);
        }
        sendCookTitle(player,
            net.minecraft.network.chat.Component.literal("§a✦ Piatto completato ✦"),
            net.minecraft.network.chat.Component.literal("§2" + completedName));
    }

    /** Removes session tags from all items (player inv + cassa + nearby drops) without destroying them. */
    private void clearSessionTags(net.minecraft.server.level.ServerPlayer player) {
        if (sessionId.isEmpty()) return;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (hasSessionTag(stack)) removeSessionTag(stack);
        }
        for (int i = 0; i < cassaContainer.getContainerSize(); i++) {
            ItemStack stack = cassaContainer.getItem(i);
            if (hasSessionTag(stack)) removeSessionTag(stack);
        }
        if (level != null) {
            AABB searchBox = new AABB(worldPosition).inflate(ROT_RADIUS);
            List<ItemEntity> droppedItems = level.getEntitiesOfClass(ItemEntity.class, searchBox);
            for (ItemEntity entity : droppedItems) {
                if (hasSessionTag(entity.getItem())) removeSessionTag(entity.getItem());
            }
        }
    }

    private void removeSessionTag(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null) return;
        CompoundTag nbt = cd.copyTag();
        nbt.remove(SESSION_NBT_KEY);
        if (nbt.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        }
    }

    /** Server tick – called every tick when the level ticks block entities. */
    public void serverTick() {
        if (!isCooking()) return;

        // Poll player inventory for the result item — catches crafting table, furnace,
        // smoker, campfire and any other production method.
        if (cookingPlayerUUID != null && activeResultItemId != null
                && level instanceof ServerLevel sl) {
            net.minecraft.server.level.ServerPlayer cookPlayer =
                    (net.minecraft.server.level.ServerPlayer) sl.getPlayerByUUID(cookingPlayerUUID);
            if (cookPlayer != null) {
                net.minecraft.world.item.Item resultItem =
                        BuiltInRegistries.ITEM.get(activeResultItemId);
                if (countItemInInventory(cookPlayer, resultItem) > initialResultCount) {
                    completeCooking(cookPlayer);
                    return;
                }
            }
        }

        timerTicks--;

        // Countdown beep: once per second during last 5 seconds, pitch rises with urgency
        if (timerTicks > 0 && timerTicks <= 100 && timerTicks % 20 == 0 && level != null) {
            float pitch = 0.6f + ((100f - timerTicks) / 100f) * 0.9f; // 0.6 → 1.5
            level.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_BELL.value(),
                    SoundSource.BLOCKS, 0.55f, pitch);
        }

        // Sync to clients every 20 ticks for smooth HUD, immediately on last tick
        if (timerTicks % 20 == 0 || timerTicks == 0) {
            syncToClients();
        }

        if (timerTicks <= 0) {
            rotTaggedItems();
            cancelCooking();
        }
    }

    private static int countItemInInventory(net.minecraft.world.entity.player.Player player,
                                             net.minecraft.world.item.Item item) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(item)) count += s.getCount();
        }
        return count;
    }

    // ── Ingredient helpers ────────────────────────────────────────────────────

    /**
     * Checks that all required ingredients can be satisfied from player inventory + cassa
     * without reusing the same item slot for two different ingredients.
     * Uses a virtual "consumed counts" array to simulate consumption.
     */
    private boolean hasIngredients(java.util.List<Ingredient> ingredients,
                                    net.minecraft.server.level.ServerPlayer player) {
        int invSize   = player.getInventory().getContainerSize();
        int cassaSize = cassaContainer.getContainerSize();
        int[] invCounts   = new int[invSize];
        int[] cassaCounts = new int[cassaSize];
        for (int i = 0; i < invSize;   i++) invCounts[i]   = player.getInventory().getItem(i).getCount();
        for (int i = 0; i < cassaSize; i++) cassaCounts[i] = cassaContainer.getItem(i).getCount();

        for (Ingredient ingredient : ingredients) {
            if (ingredient.isEmpty()) continue;
            boolean satisfied = false;

            for (int i = 0; i < invSize && !satisfied; i++) {
                if (invCounts[i] > 0 && ingredient.test(player.getInventory().getItem(i))) {
                    invCounts[i]--;
                    satisfied = true;
                }
            }
            for (int i = 0; i < cassaSize && !satisfied; i++) {
                if (cassaCounts[i] > 0 && ingredient.test(cassaContainer.getItem(i))) {
                    cassaCounts[i]--;
                    satisfied = true;
                }
            }
            if (!satisfied) return false;
        }
        return true;
    }

    /**
     * Tags exactly the items needed for the recipe.
     * Each ingredient consumes one item from a matching slot;
     * if the same slot is used multiple times (stack > 1), the reserved count accumulates.
     */
    private void tagIngredients(java.util.List<Ingredient> ingredients,
                                 net.minecraft.server.level.ServerPlayer player) {
        for (Ingredient ingredient : ingredients) {
            if (ingredient.isEmpty()) continue;
            boolean tagged = false;

            // Player inventory – prefer slots already tagged for this session (accumulate count)
            for (int i = 0; i < player.getInventory().getContainerSize() && !tagged; i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.isEmpty() || !ingredient.test(stack)) continue;

                if (hasSessionTag(stack)) {
                    int reserved = getSessionTagCount(stack);
                    if (reserved < stack.getCount()) {
                        applySessionTag(stack, reserved + 1);
                        tagged = true;
                    }
                    // else slot exhausted, try next
                } else {
                    applySessionTag(stack, 1);
                    tagged = true;
                }
            }

            // Cassa fallback
            if (!tagged) {
                for (int i = 0; i < cassaContainer.getContainerSize() && !tagged; i++) {
                    ItemStack stack = cassaContainer.getItem(i);
                    if (stack.isEmpty() || !ingredient.test(stack)) continue;

                    if (hasSessionTag(stack)) {
                        int reserved = getSessionTagCount(stack);
                        if (reserved < stack.getCount()) {
                            applySessionTag(stack, reserved + 1);
                            tagged = true;
                        }
                    } else {
                        applySessionTag(stack, 1);
                        tagged = true;
                    }
                }
            }
        }
    }

    private void applySessionTag(ItemStack stack, int reservedCount) {
        CustomData existing = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag nbt = existing.copyTag();
        nbt.putString(SESSION_NBT_KEY, sessionId);
        nbt.putInt(SESSION_COUNT_KEY, reservedCount);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
    }

    private int getSessionTagCount(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null) return 1;
        CompoundTag nbt = cd.copyTag();
        return nbt.contains(SESSION_COUNT_KEY) ? nbt.getInt(SESSION_COUNT_KEY) : 1;
    }

    /** When the timer expires, rot (remove) all items that carry this session's tag. */
    private void rotTaggedItems() {
        if (sessionId.isEmpty() || level == null) return;

        // Rot items in cassa (shrink only the reserved count, not the whole stack)
        for (int i = 0; i < cassaContainer.getContainerSize(); i++) {
            ItemStack stack = cassaContainer.getItem(i);
            if (hasSessionTag(stack)) {
                int count = getSessionTagCount(stack);
                stack.shrink(count);
            }
        }

        // Rot items dropped on the ground nearby
        AABB searchBox = new AABB(worldPosition).inflate(ROT_RADIUS);
        List<ItemEntity> droppedItems = level.getEntitiesOfClass(ItemEntity.class, searchBox);
        for (ItemEntity entity : droppedItems) {
            ItemStack dropped = entity.getItem();
            if (hasSessionTag(dropped)) {
                int count = getSessionTagCount(dropped);
                dropped.shrink(count);
                if (dropped.isEmpty()) entity.discard();
            }
        }

        // Rot items in the cooking player's inventory
        if (cookingPlayerUUID != null && level instanceof ServerLevel sl) {
            net.minecraft.world.entity.player.Player cookPlayer = sl.getPlayerByUUID(cookingPlayerUUID);
            if (cookPlayer != null) {
                boolean rotted = false;
                for (int i = 0; i < cookPlayer.getInventory().getContainerSize(); i++) {
                    ItemStack stack = cookPlayer.getInventory().getItem(i);
                    if (hasSessionTag(stack)) {
                        int count = getSessionTagCount(stack);
                        stack.shrink(count);
                        // Drop one rotten food per ingredient slot consumed
                        ItemStack rottenFood = new ItemStack(TharidiaThings.ROTTEN_FOOD.get());
                        cookPlayer.getInventory().add(rottenFood);
                        rotted = true;
                    }
                }
                if (rotted && cookPlayer instanceof net.minecraft.server.level.ServerPlayer sp) {
                    level.playSound(null, cookPlayer.blockPosition(),
                            SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.9f, 0.8f);
                    sendCookTitle(sp,
                        net.minecraft.network.chat.Component.literal("§c✗ Ingredienti rovinati ✗"),
                        net.minecraft.network.chat.Component.literal("§4Hai impiegato troppo tempo"));
                }
            }
        }
    }

    private boolean hasSessionTag(ItemStack stack) {
        if (stack.isEmpty() || sessionId.isEmpty()) return false;
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null) return false;
        return sessionId.equals(cd.copyTag().getString(SESSION_NBT_KEY));
    }

    /** Sends an impactful title + subtitle to the player (fadeIn=8, stay=45, fadeOut=12 ticks). */
    private static void sendCookTitle(net.minecraft.server.level.ServerPlayer player,
                                      net.minecraft.network.chat.Component title,
                                      net.minecraft.network.chat.Component subtitle) {
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(8, 45, 12));
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
                title.copy().withStyle(s -> s.withBold(false))));
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(
                subtitle.copy().withStyle(s -> s.withBold(false))));
    }

    // ── Cassa ─────────────────────────────────────────────────────────────────
    public SimpleContainer getCassaContainer() {
        return cassaContainer;
    }

    // ── NBT ───────────────────────────────────────────────────────────────────
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        // Save cassa inventory
        CompoundTag cassaTag = new CompoundTag();
        for (int i = 0; i < cassaContainer.getContainerSize(); i++) {
            ItemStack stack = cassaContainer.getItem(i);
            if (!stack.isEmpty()) {
                cassaTag.put("Slot" + i, stack.save(registries));
            }
        }
        tag.put("CassaInventory", cassaTag);

        // Save session state
        tag.putString("activeRecipeId", activeRecipeId);
        tag.putString("activeResultName", activeResultName);
        tag.putInt("timerTicks", timerTicks);
        tag.putInt("totalTimerTicks", totalTimerTicks);
        tag.putString("sessionId", sessionId);
        tag.putInt("initialResultCount", initialResultCount);
        if (activeResultItemId != null) {
            tag.putString("activeResultItemId", activeResultItemId.toString());
        }
        if (cookingPlayerUUID != null) {
            tag.putUUID("cookingPlayerUUID", cookingPlayerUUID);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        // Load cassa inventory
        if (tag.contains("CassaInventory")) {
            CompoundTag cassaTag = tag.getCompound("CassaInventory");
            for (int i = 0; i < cassaContainer.getContainerSize(); i++) {
                String key = "Slot" + i;
                if (cassaTag.contains(key)) {
                    ItemStack stack = ItemStack.parseOptional(registries, cassaTag.getCompound(key));
                    cassaContainer.setItem(i, stack);
                }
            }
        }

        // Load session state
        activeRecipeId     = tag.getString("activeRecipeId");
        activeResultName   = tag.getString("activeResultName");
        timerTicks         = tag.getInt("timerTicks");
        totalTimerTicks    = tag.getInt("totalTimerTicks");
        sessionId          = tag.getString("sessionId");
        initialResultCount = tag.getInt("initialResultCount");
        if (tag.contains("activeResultItemId")) {
            activeResultItemId = ResourceLocation.tryParse(tag.getString("activeResultItemId"));
        }
        if (tag.hasUUID("cookingPlayerUUID")) {
            cookingPlayerUUID = tag.getUUID("cookingPlayerUUID");
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        // Client-side: only update display state fields
        activeRecipeId   = tag.getString("activeRecipeId");
        activeResultName = tag.getString("activeResultName");
        timerTicks       = tag.getInt("timerTicks");
        totalTimerTicks  = tag.getInt("totalTimerTicks");
    }
}
