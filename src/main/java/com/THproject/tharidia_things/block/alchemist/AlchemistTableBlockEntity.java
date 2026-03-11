package com.THproject.tharidia_things.block.alchemist;

import com.THproject.tharidia_things.TharidiaThings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Block entity for the Alchemist Table multiblock.
 * Handles GeckoLib animations, crafting state, and network sync.
 *
 * <p>
 * Crafting is initiated by right-clicking dummy index 1 (most external block
 * on the short arm), which calls {@link #startCraftingSequence()}. The
 * {@link AlchemistCraftingHandler} drives phase transitions and this class
 * reacts via {@link #onCraftingPhaseChanged} to play animations and sync state.
 */
public class AlchemistTableBlockEntity extends BlockEntity implements GeoBlockEntity {

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    // GeckoLib animations
    private static final RawAnimation MANTICE_ANIM = RawAnimation.begin().thenLoop("mantice");
    private static final RawAnimation BOOK_ANIM    = RawAnimation.begin().thenPlay("book");
    private static final RawAnimation PESTEL_ANIM  = RawAnimation.begin().thenPlay("pestel");

    // Independent toggle (not tied to crafting)
    private boolean manticeActive = false;

    // Crafting state machine
    private final AlchemistCraftingHandler craftingHandler = new AlchemistCraftingHandler(this);

    // ==================== Jar Storage ====================

    /** Maximum items each jar can hold. */
    public static final int JAR_CAPACITY = 5;

    /**
     * Item tag accepted by jar slots 0 and 1.
     * Any flower recognized by the minecraft:flowers tag is valid.
     */
    private static final TagKey<Item> FLOWER_TAG = ItemTags.FLOWERS;

    /**
     * Item tag accepted by jar slots 2 and 3.
     * Add items to data/tharidiathings/tags/item/manure.json to populate this tag.
     */
    private static final TagKey<Item> MANURE_TAG =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath("tharidiathings", "manure"));

    /**
     * Four jar slots. Each stores a single item type with a count in [0, JAR_CAPACITY].
     * Slots 0-1 accept flowers; slots 2-3 accept manure.
     */
    private final ItemStack[] jars = new ItemStack[]{
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
    };

    public AlchemistTableBlockEntity(BlockPos pos, BlockState state) {
        super(TharidiaThings.ALCHEMIST_TABLE_BLOCK_ENTITY.get(), pos, state);
    }

    public void addInteraction(Player player) {
        player.displayClientMessage(Component.literal("Add interaction triggered!"), true);
    }

    public void subtractInteraction(Player player) {
        player.displayClientMessage(Component.literal("Subtract interaction triggered!"), true);
    }

    public void divideInteraction(Player player) {
        player.displayClientMessage(Component.literal("Divide interaction triggered!"), true);
    }

    public void multiplyInteraction(Player player) {
        player.displayClientMessage(Component.literal("Multiply interaction triggered!"), true);
    }

    // ==================== Server Tick ====================

    /**
     * Static ticker method registered by {@link AlchemistTableBlock#getTicker}.
     * Drives the crafting state machine every server tick.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, AlchemistTableBlockEntity be) {
        be.craftingHandler.serverTick();
    }

    // ==================== Crafting Entry Point ====================

    /**
     * Called when dummy index 1 (most external block on the short arm) is
     * right-clicked.
     * Hands off to the crafting handler; returns {@code true} if the sequence
     * started.
     */
    public boolean startCraftingSequence() {
        return craftingHandler.startSequence();
    }

    // ========== Crafting Callbacks (called by AlchemistCraftingHandler) ==========

    /**
     * Reacts to a phase transition in the crafting handler.
     * This is the central coordinator: trigger the appropriate animations
     * for each phase and notify the client.
     */
    void onCraftingPhaseChanged(AlchemistCraftingPhase newPhase) {
        switch (newPhase) {
            case PROCESSING -> {
                // TODO: activate cauldron/fire animations
            }
            case FINISHING -> {
                // TODO: play output-ready animation
            }
            case IDLE -> {
                // Sequence ended (success or abort); nothing extra needed here
            }
        }
        syncToClient();
    }

    /**
     * Called by the crafting handler at the end of the FINISHING phase.
     * TODO: consume ingredients from inventory, drop/insert the output item.
     */
    void onCraftingComplete() {
        // TODO: implement output logic
    }

    /** Accessor for the crafting handler (e.g. for GUI or recipe validation). */
    public AlchemistCraftingHandler getCraftingHandler() {
        return craftingHandler;
    }

    // ==================== Jar Filling ====================

    /**
     * Tries to insert one item from the player's hand into the first valid, non-full jar.
     * Jars are tried in order 0 → 3. A jar accepts an item only if:
     * <ul>
     *   <li>the jar's category matches the item type (flowers for 0-1, manure for 2-3), AND</li>
     *   <li>the jar is empty OR already contains that exact item, AND</li>
     *   <li>the jar is not full (count &lt; JAR_CAPACITY).</li>
     * </ul>
     *
     * @return {@code true} if an item was successfully inserted.
     */
    public boolean tryInsertIntoJar(ItemStack stack, Player player) {
        for (int i = 0; i < jars.length; i++) {
            if (!jarAccepts(i, stack)) continue;

            ItemStack jar = jars[i];
            boolean sameType = !jar.isEmpty() && jar.is(stack.getItem());
            boolean notFull  = jar.getCount() < JAR_CAPACITY;

            if (jar.isEmpty() || (sameType && notFull)) {
                // Insert one item
                if (jar.isEmpty()) {
                    jars[i] = new ItemStack(stack.getItem(), 1);
                } else {
                    jars[i].grow(1);
                }

                // Consume from player inventory (skip in creative)
                if (!player.isCreative()) {
                    stack.shrink(1);
                }

                // Notify if now full
                if (jars[i].getCount() >= JAR_CAPACITY) {
                    player.displayClientMessage(
                            Component.literal("Jar " + (i + 1) + " is full!"), true);
                }

                syncToClient();
                return true;
            }
        }
        return false;
    }

    /** Returns whether jar slot {@code index} is willing to accept {@code stack}. */
    private boolean jarAccepts(int index, ItemStack stack) {
        if (index < 2) return stack.is(FLOWER_TAG);
        return stack.is(MANURE_TAG);
    }

    /** Returns the current contents of jar slot {@code index} (may be {@link ItemStack#EMPTY}). */
    public ItemStack getJar(int index) {
        return jars[index];
    }

    // ==================== Independent Animation Triggers ====================

    public void toggleMantice() {
        this.manticeActive = !this.manticeActive;
        syncToClient();
    }

    /**
     * Directly triggers the pestle animation
     * (independent of crafting, e.g. from dummy index 3).
     */
    public void triggerPestelAnimation() {
        triggerAnim("pestel_controller", "grind");
        syncToClient();
    }

    // ==================== GeckoLib ====================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Mantice (looping, state-driven)
        controllers.add(new AnimationController<>(this, "mantice_controller", 5, state -> {
            if (this.manticeActive) {
                state.getController().setAnimation(MANTICE_ANIM);
                return PlayState.CONTINUE;
            }
            return PlayState.STOP;
        }));

        // Book (one-shot, triggered)
        controllers.add(new AnimationController<>(this, "book_controller", 5,
                state -> PlayState.STOP)
                .triggerableAnim("flip", BOOK_ANIM));

        // Pestel (one-shot, triggered)
        controllers.add(new AnimationController<>(this, "pestel_controller", 5,
                state -> PlayState.STOP)
                .triggerableAnim("grind", PESTEL_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("ManticeActive", manticeActive);
        craftingHandler.save(tag);
        // Save jars: only write non-empty slots
        for (int i = 0; i < jars.length; i++) {
            if (!jars[i].isEmpty()) {
                tag.put("Jar" + i, jars[i].save(registries));
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        manticeActive = tag.getBoolean("ManticeActive");
        craftingHandler.load(tag);
        for (int i = 0; i < jars.length; i++) {
            String key = "Jar" + i;
            jars[i] = tag.contains(key)
                    ? ItemStack.parseOptional(registries, tag.getCompound(key))
                    : ItemStack.EMPTY;
        }
    }

    // ==================== Network Sync ====================

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    void syncToClient() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
