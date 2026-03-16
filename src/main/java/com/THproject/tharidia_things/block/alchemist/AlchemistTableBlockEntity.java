package com.THproject.tharidia_things.block.alchemist;

import com.THproject.tharidia_things.TharidiaThings;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
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

    // Independent toggle (not tied to crafting)
    private boolean manticeActive = false;

    // ==================== Stirring Angle ====================

    /** Accumulated rotation angle of the Mestolone in degrees. Advances only while the player stirs correctly. */
    private float craftingAngle = 0f;
    /** Angle from the previous server tick — used by the renderer for partialTick interpolation. */
    private float prevCraftingAngle = 0f;
    /** Set to true by tryStir() each tick the player hits the hotspot; consumed and reset in serverTick. */
    private boolean isBeingStirred = false;

    // Crafting state machine
    private final AlchemistCraftingHandler craftingHandler = new AlchemistCraftingHandler(this);

    // ==================== Interactive Crafting Session ====================

    /**
     * Dummy index that corresponds to the result table with 3 jars.
     * Adjust this constant to match the physical position of the result table in the model.
     */
    public static final int RESULT_TABLE_DUMMY_INDEX = 5;

    private final AlchemistCraftingSession session = new AlchemistCraftingSession();

    /**
     * Values stored in the 3 result jars of the result table.
     * Numbers are intentionally hidden from the player — only filled/empty status is shown.
     */
    private final int[] resultJarValues = new int[3];
    private int resultJarCount = 0;

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
    private static final TagKey<Item> MANURE_TAG = ItemTags
            .create(ResourceLocation.fromNamespaceAndPath("tharidiathings", "manure"));

    /**
     * Four jar slots. Each stores a single item type with a count in [0,
     * JAR_CAPACITY].
     * Slots 0-1 accept flowers; slots 2-3 accept manure.
     */
    private final ItemStack[] jars = new ItemStack[] {
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
    };

    // ==================== Jar Interaction Entities ====================

    /**
     * Per-jar data: { localX, Y_base, localZ, width, height }
     * localX/Z = offset from D1 dummy block centre (arm/column directions).
     * Y_base = entity feet height above block floor (= jar model bottom).
     * width/height = Interaction entity hitbox size derived from Blockbench bounding boxes.
     * Bone order: Jar_start1, Jar_start2, Jar_start3, Jar_start4.
     */
    private static final double[][] INPUT_JAR_OFFSETS = {
        {  0.247, 1.1246,  0.149, 0.3626, 0.3120 }, // Jar_start1
        { -0.119, 1.1246,  0.178, 0.2231, 0.2401 }, // Jar_start2 (swapped)
        { -0.246, 1.1246, -0.181, 0.3626, 0.3901 }, // Jar_start3 (swapped)
        {  0.117, 1.1246, -0.145, 0.3626, 0.4463 }, // Jar_start4
    };

    /**
     * Per-jar data: { localX, Y_base, localZ, width, height }
     * localX/Z = offset from D5 dummy block centre.
     * Bone order: Jar_final1, Jar_final2, Jar_final3.
     */
    private static final double[][] OUTPUT_JAR_OFFSETS = {
        { -0.280, 1.1246, 0.36, 0.21, 0.3588 }, // Jar_final1
        { -0.021, 1.1246, 0.36, 0.21, 0.3588 }, // Jar_final2
        {  0.242, 1.1246, 0.36, 0.21, 0.3588 }, // Jar_final3
    };

    private UUID[] inputJarEntityIds  = new UUID[4];
    private UUID[] outputJarEntityIds = new UUID[3];

    private UUID cauldronEntityId;

    private final AlchemistStirringPhase stirringPhase = new AlchemistStirringPhase(this);

    // ==================== Potion Extraction ====================

    private enum PotionState { NONE, READY, DILUTED }
    private PotionState potionState = PotionState.NONE;
    /** The matched output stack (1 dose), set in onStirringComplete(). */
    private ItemStack potionStack = ItemStack.EMPTY;
    /** Remaining doses (0-4) while potionState == DILUTED. */
    private int potionDoses = 0;

    public AlchemistTableBlockEntity(BlockPos pos, BlockState state) {
        super(TharidiaThings.ALCHEMIST_TABLE_BLOCK_ENTITY.get(), pos, state);
    }

    // ==================== Server Tick ====================

    /**
     * Static ticker method registered by {@link AlchemistTableBlock#getTicker}.
     * Drives the crafting state machine every server tick.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, AlchemistTableBlockEntity be) {
        be.craftingHandler.serverTick();

        // Advance stirring angle only when the player is actively hitting the hotspot
        be.prevCraftingAngle = be.craftingAngle;
        if (be.isBeingStirred) {
            be.craftingAngle += 20.0f;
            be.moveCauldronInteraction(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
            be.syncToClient(); // send updated angle to renderer
        }
        be.stirringPhase.tick(be.isBeingStirred);
        be.isBeingStirred = false; // reset every tick — must be refreshed by player input
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
     * Tries to insert one item into the jar at the given index.
     * Used when the player clicks a specific jar's Interaction entity.
     *
     * @return {@code true} if an item was successfully inserted.
     */
    public boolean tryInsertIntoJar(ItemStack stack, Player player, int jarIndex) {
        if (jarIndex < 0 || jarIndex >= jars.length) return false;
        if (resultJarCount >= resultJarValues.length) {
            player.displayClientMessage(Component.literal("The result table is full — collect the results first!"), true);
            return false;
        }
        if (session.isActive()) {
            player.displayClientMessage(Component.literal("Cannot change jars during an active session!"), true);
            return false;
        }
        if (!jarAccepts(jarIndex, stack)) {
            player.displayClientMessage(
                    Component.literal("Jar " + (jarIndex + 1) + " does not accept that item!"), true);
            return false;
        }
        ItemStack jar = jars[jarIndex];
        boolean sameType = !jar.isEmpty() && jar.is(stack.getItem());
        boolean notFull  = jar.getCount() < JAR_CAPACITY;
        if (!jar.isEmpty() && (!sameType || !notFull)) {
            if (!notFull)
                player.displayClientMessage(Component.literal("Jar " + (jarIndex + 1) + " is already full!"), true);
            else
                player.displayClientMessage(Component.literal("Jar " + (jarIndex + 1) + " already contains a different item!"), true);
            return false;
        }
        if (jar.isEmpty()) {
            jars[jarIndex] = new ItemStack(stack.getItem(), 1);
        } else {
            jars[jarIndex].grow(1);
        }
        if (!player.isCreative()) stack.shrink(1);
        if (jars[jarIndex].getCount() >= JAR_CAPACITY)
            player.displayClientMessage(Component.literal("Jar " + (jarIndex + 1) + " is full!"), true);
        syncToClient();
        return true;
    }

    /**
     * Tries to insert one item from the player's hand into the first valid,
     * non-full jar (fallback used by the dummy block).
     * Jars are tried in order 0 → 3.
     *
     * @return {@code true} if an item was successfully inserted.
     */
    public boolean tryInsertIntoJar(ItemStack stack, Player player) {
        if (resultJarCount >= resultJarValues.length) {
            player.displayClientMessage(Component.literal("The result table is full — collect the results first!"), true);
            return false;
        }
        if (session.isActive()) {
            player.displayClientMessage(Component.literal("Cannot change jars during an active session!"), true);
            return false;
        }
        for (int i = 0; i < jars.length; i++) {
            if (!jarAccepts(i, stack))
                continue;

            ItemStack jar = jars[i];
            boolean sameType = !jar.isEmpty() && jar.is(stack.getItem());
            boolean notFull = jar.getCount() < JAR_CAPACITY;

            if (jar.isEmpty() || (sameType && notFull)) {
                if (jar.isEmpty()) {
                    jars[i] = new ItemStack(stack.getItem(), 1);
                } else {
                    jars[i].grow(1);
                }
                if (!player.isCreative()) stack.shrink(1);
                if (jars[i].getCount() >= JAR_CAPACITY)
                    player.displayClientMessage(Component.literal("Jar " + (i + 1) + " is full!"), true);
                syncToClient();
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether jar slot {@code index} is willing to accept {@code stack}.
     */
    private boolean jarAccepts(int index, ItemStack stack) {
        if (index < 2)
            return stack.is(FLOWER_TAG);
        return stack.is(MANURE_TAG);
    }

    /**
     * Returns the current contents of jar slot {@code index} (may be
     * {@link ItemStack#EMPTY}).
     */
    public ItemStack getJar(int index) {
        return jars[index];
    }

    /**
     * Returns the world-space hotspot position driven by the accumulated {@link #craftingAngle}.
     * Pass {@code partialTick = 1.0f} for server-side checks; use the actual partialTick on the client.
     *
     * @return float[4] — {offsetX, offsetY, offsetZ, radius}, relative to the cauldron dummy block centre.
     */
    public float[] getCauldronHotspot(float partialTick) {
        float radius = 0.15f;
        BlockPos masterPos = AlchemistTableBlock.getMasterPosFromDummy(worldPosition, 6,
                getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING));
        BlockPos dummyPos = worldPosition;

        float relX = dummyPos.getX() - masterPos.getX();
        float relZ = dummyPos.getZ() - masterPos.getZ();

        relX += 0.5f;
        relZ += 0.5f;

        // Interpolate between prev and current angle for smooth client rendering
        float angle = prevCraftingAngle + (craftingAngle - prevCraftingAngle) * partialTick;
        double radians = Math.toRadians(angle);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double rotatedX = relX * cos - relZ * sin;
        double rotatedZ = relX * sin + relZ * cos;

        rotatedX /= 16.0f;
        rotatedZ /= 16.0f;

        return new float[] { (float) rotatedX, 1.05f, (float) rotatedZ, radius };
    }

    /**
     * Returns the interpolated Mestolone angle in degrees for use in the renderer.
     */
    public float getInterpolatedCraftingAngle(float partialTick) {
        return prevCraftingAngle + (craftingAngle - prevCraftingAngle) * partialTick;
    }

    public void stir(Player player) {
        ItemStack held = player.getMainHandItem();

        // ── Potion ready: needs water bottle to dilute ────────────────────────
        if (potionState == PotionState.READY) {
            if (isWaterBottle(held)) {
                dilute(player, held);
            } else {
                player.displayClientMessage(
                        Component.literal("La pozione è pronta — aggiungi una boccetta d'acqua per diluirla!")
                                .withColor(0x00FFCC), true);
            }
            return;
        }

        // ── Potion diluted: needs glass bottle to collect ─────────────────────
        if (potionState == PotionState.DILUTED) {
            if (held.is(Items.GLASS_BOTTLE)) {
                collectDose(player, held);
            } else {
                player.displayClientMessage(
                        Component.literal("Usa una boccetta vuota per raccogliere la pozione! (" + potionDoses + " dosi rimaste)")
                                .withColor(0xFFAA00), true);
            }
            return;
        }

        // ── Normal stirring ───────────────────────────────────────────────────
        isBeingStirred = true;
        stirringPhase.onStir(player);
        moveCauldronInteraction(getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING));
    }

    private void dilute(Player player, ItemStack waterBottle) {
        if (!player.isCreative()) {
            waterBottle.shrink(1);
            player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE));
        }
        potionState = PotionState.DILUTED;
        potionDoses = 4;
        player.displayClientMessage(
                Component.literal("Pozione diluita! Usa 4 boccette vuote per raccogliere le dosi.").withColor(0x00FFCC), true);
        syncToClient();
    }

    private void collectDose(Player player, ItemStack bottle) {
        if (!player.isCreative()) {
            bottle.shrink(1);
        }
        player.getInventory().add(potionStack.copy());
        potionDoses--;
        if (potionDoses <= 0) {
            resetAfterCrafting();
            player.displayClientMessage(
                    Component.literal("Crafting completato!").withColor(0x00FF88), true);
        } else {
            player.displayClientMessage(
                    Component.literal("Raccolta 1 dose! (" + potionDoses + " rimaste)").withColor(0x00FFAA), true);
        }
        syncToClient();
    }

    private void resetAfterCrafting() {
        potionState = PotionState.NONE;
        potionStack = ItemStack.EMPTY;
        potionDoses = 0;
        resultJarCount = 0;
        java.util.Arrays.fill(resultJarValues, 0);
    }

    private static boolean isWaterBottle(ItemStack stack) {
        if (!stack.is(Items.POTION)) return false;
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents == null || !contents.getAllEffects().iterator().hasNext();
    }

    /**
     * Called when the player clicks an output-jar Interaction entity.
     * Delegates to the stirring phase if active, otherwise shows jar status.
     */
    public void onOutputJarClicked(int jarIndex, Player player) {
        stirringPhase.onJarClicked(jarIndex, player);
    }

    /** Called by AlchemistStirringPhase after the COMPLETING countdown ends. */
    void onStirringComplete() {
        ItemStack matched = AlchemistPotionRegistry.findPotion(resultJarValues);
        if (matched != null && !matched.isEmpty()) {
            potionStack = matched;
            potionState = PotionState.READY;
        } else {
            resetAfterCrafting();
        }
        syncToClient();
    }

    /** Spawns magic completion particles at the cauldron. Called from AlchemistStirringPhase. */
    void spawnCompletionParticles() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        BlockPos cauldronPos = AlchemistTableBlock.getDummyPos(worldPosition, 6,
                getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING));
        double cx = cauldronPos.getX() + 0.5;
        double cy = cauldronPos.getY() + 1.5;
        double cz = cauldronPos.getZ() + 0.5;
        serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, cx, cy, cz, 80, 0.5, 0.5, 0.5, 0.3);
        serverLevel.sendParticles(ParticleTypes.WITCH, cx, cy, cz, 40, 0.3, 0.3, 0.3, 0.1);
    }

    // ==================== Jar Interaction Entities ====================

    /**
     * Spawns invisible Interaction entities positioned over each jar slot.
     * Call this server-side when the multiblock forms.
     */
    public void spawnJarInteractions(Direction facing) {
        Level level = getLevel();
        if (level == null || level.isClientSide) return;

        BlockPos masterPos = worldPosition;

        // 4 input jars on D1 (dummy index 1, arm at localX=2)
        BlockPos d1 = AlchemistTableBlock.getDummyPos(masterPos, 1, facing);
        for (int i = 0; i < INPUT_JAR_OFFSETS.length; i++) {
            inputJarEntityIds[i] = spawnSingleJarEntity(level, d1, INPUT_JAR_OFFSETS[i], facing, true, i);
        }

        // 3 output jars on D5 (dummy index 5, column at localZ=4)
        BlockPos d5 = AlchemistTableBlock.getDummyPos(masterPos, 5, facing);
        for (int i = 0; i < OUTPUT_JAR_OFFSETS.length; i++) {
            outputJarEntityIds[i] = spawnSingleJarEntity(level, d5, OUTPUT_JAR_OFFSETS[i], facing, false, i);
        }

        setChanged();
    }

    private UUID spawnSingleJarEntity(Level level, BlockPos blockPos, double[] localOffset,
                                       Direction facing, boolean isInput, int jarIndex) {
        double[] wo = transformLocalOffset(localOffset, facing);
        double wx = blockPos.getX() + 0.5 + wo[0];
        double wy = blockPos.getY() + wo[1];
        double wz = blockPos.getZ() + 0.5 + wo[2];

        net.minecraft.world.entity.Interaction entity =
                net.minecraft.world.entity.EntityType.INTERACTION.create(level);
        if (entity == null) return null;

        // Set size BEFORE setPos so the AABB is computed with the correct dimensions
        setInteractionSize(entity, (float) localOffset[3], (float) localOffset[4]);
        entity.setPos(wx, wy, wz);

        net.minecraft.nbt.CompoundTag data = entity.getPersistentData();
        data.putInt("AlchemistMasterX", worldPosition.getX());
        data.putInt("AlchemistMasterY", worldPosition.getY());
        data.putInt("AlchemistMasterZ", worldPosition.getZ());
        data.putBoolean("AlchemistIsInput", isInput);
        data.putInt("AlchemistJarIndex", jarIndex);

        level.addFreshEntity(entity);
        return entity.getUUID();
    }

    /** Removes all jar Interaction entities owned by this block entity. */
    public void removeJarInteractions() {
        Level level = getLevel();
        if (level == null || level.isClientSide || !(level instanceof net.minecraft.server.level.ServerLevel serverLevel))
            return;
        discardEntities(serverLevel, inputJarEntityIds);
        discardEntities(serverLevel, outputJarEntityIds);
        java.util.Arrays.fill(inputJarEntityIds, null);
        java.util.Arrays.fill(outputJarEntityIds, null);
    }

    private static void discardEntities(net.minecraft.server.level.ServerLevel serverLevel, UUID[] ids) {
        for (UUID uuid : ids) {
            if (uuid == null) continue;
            net.minecraft.world.entity.Entity e = serverLevel.getEntity(uuid);
            if (e != null) e.discard();
        }
    }

    /**
     * Sets the hitbox size on an Interaction entity via reflection, because
     * {@code setWidth}/{@code setHeight} are private in {@link net.minecraft.world.entity.Interaction}.
     */
    @SuppressWarnings("unchecked")
    private static void setInteractionSize(net.minecraft.world.entity.Interaction entity, float width, float height) {
        try {
            java.lang.reflect.Field wf = net.minecraft.world.entity.Interaction.class.getDeclaredField("DATA_WIDTH_ID");
            wf.setAccessible(true);
            java.lang.reflect.Field hf = net.minecraft.world.entity.Interaction.class.getDeclaredField("DATA_HEIGHT_ID");
            hf.setAccessible(true);
            var wa = (net.minecraft.network.syncher.EntityDataAccessor<Float>) wf.get(null);
            var ha = (net.minecraft.network.syncher.EntityDataAccessor<Float>) hf.get(null);
            entity.getEntityData().set(wa, width);
            entity.getEntityData().set(ha, height);
        } catch (Exception e) {
            com.THproject.tharidia_things.TharidiaThings.LOGGER.warn(
                    "Could not set Interaction entity size via reflection: {}", e.getMessage());
        }
    }

    /**
     * Transforms a local-space offset (localX = arm dir, localZ = col dir) to world-space
     * based on the multiblock facing, consistent with {@link AlchemistTableBlock#getWorldPos}.
     */
    private static double[] transformLocalOffset(double[] local, Direction facing) {
        double lx = local[0], ly = local[1], lz = local[2];
        double wx, wz;
        switch (facing) {
            case NORTH -> { wx = -lz; wz = -lx; }
            case SOUTH -> { wx =  lz; wz =  lx; }
            case EAST  -> { wx =  lx; wz = -lz; }
            case WEST  -> { wx = -lx; wz =  lz; }
            default    -> { wx = -lz; wz = -lx; }
        }
        return new double[]{ wx, ly, wz };
    }

    public void spawnCauldronInteraction(Direction facing) {
        Level level = getLevel();
        if (level == null || level.isClientSide) return;

        BlockPos masterPos = worldPosition;

        BlockPos cauldronPos = AlchemistTableBlock.getDummyPos(masterPos, 6, facing);

        Interaction entity = EntityType.INTERACTION.create(level);
        if (entity == null) return;
        entity.setPos(cauldronPos.getX() + 0.5, cauldronPos.getY() + 1.05, cauldronPos.getZ() + 0.5);
        setInteractionSize(entity, 0.2f, 0.3f);
        
        CompoundTag data = entity.getPersistentData();
        data.putInt("AlchemistMasterX", worldPosition.getX());
        data.putInt("AlchemistMasterY", worldPosition.getY());
        data.putInt("AlchemistMasterZ", worldPosition.getZ());
        data.putBoolean("AlchemistIsCauldron", true);
        level.addFreshEntity(entity);

        cauldronEntityId = entity.getUUID();

        moveCauldronInteraction(facing);
    }

    public void removeCauldronInteraction() {
        if (cauldronEntityId != null) {
            Level level = getLevel();
            if (level instanceof ServerLevel serverLevel) {
                Entity entity = serverLevel.getEntity(cauldronEntityId);
                if (entity != null) {
                    entity.discard();
                }
            }
            cauldronEntityId = null;
        }
    }

    public void moveCauldronInteraction(Direction facing) {
        float[] hotspot = getCauldronHotspot(1.0f);
        BlockPos dummyPos = AlchemistTableBlock.getDummyPos(worldPosition, 6, facing);

        float hotspotX = (float) dummyPos.getX() + hotspot[0] + 0.5f;
        float hotspotY = (float) dummyPos.getY() + hotspot[1];
        float hotspotZ = (float) dummyPos.getZ() + hotspot[2] + 0.5f;
        
        if (cauldronEntityId != null) {
            Level level = getLevel();
            if (level instanceof ServerLevel serverLevel){
                Entity entity = serverLevel.getEntity(cauldronEntityId);
                if(entity != null){
                    entity.setPos(hotspotX, hotspotY, hotspotZ);
                }
            }
        }
    }

    // ==================== Crafting Session — Jar Picking (empty hand on D1) ====================

    /**
     * Called when the player right-clicks a specific input-jar Interaction entity with an empty hand.
     * Activates the session on first call, then gives the player the token for that exact jar.
     */
    public void tryPickJar(Player player, int jarIndex) {
        if (!player.getMainHandItem().isEmpty()) {
            player.displayClientMessage(Component.literal("Empty your hand first!"), true);
            return;
        }
        if (!allJarsFull()) {
            player.displayClientMessage(Component.literal("Fill all 4 jars first!"), true);
            return;
        }
        if (session.isTokenOut()) {
            player.displayClientMessage(
                    Component.literal("Use your current token on an operation dummy first!"), true);
            return;
        }
        if (jarIndex < 0 || jarIndex >= jars.length) return;
        if (session.isJarUsed(jarIndex)) {
            player.displayClientMessage(Component.literal("Jar " + (jarIndex + 1) + " was already picked!"), true);
            return;
        }
        if (!session.isActive()) {
            session.activate();
            triggerAnim("book_controller", "flip");
        }
        session.pickJar(jarIndex);
        int value = AlchemistJarRegistry.getItemValue(jars[jarIndex]);
        player.setItemInHand(InteractionHand.MAIN_HAND, AlchemistTokenItem.create(value));
        player.displayClientMessage(
                Component.literal("Picked jar " + (jarIndex + 1) + " ["
                        + AlchemistJarRegistry.describeJar(jars[jarIndex]) + "]"), true);
        syncToClient();
    }

    /**
     * Called when the player right-clicks dummy 1 with an empty hand.
     * Activates the session on first call, then gives the player an
     * {@link AlchemistTokenItem} carrying the next jar's value.
     */
    public void tryPickJar(Player player) {
        if (!player.getMainHandItem().isEmpty()) {
            player.displayClientMessage(Component.literal("Empty your hand first!"), true);
            return;
        }
        if (!allJarsFull()) {
            player.displayClientMessage(Component.literal("Fill all 4 jars first!"), true);
            return;
        }
        if (session.isTokenOut()) {
            player.displayClientMessage(
                    Component.literal("Use your current token on an operation dummy first!"), true);
            return;
        }
        if (!session.isActive()) {
            session.activate();
            triggerAnim("book_controller", "flip");
        }
        int jarIndex = session.pickNextJar();
        if (jarIndex < 0) {
            player.displayClientMessage(Component.literal("All jars have already been used!"), true);
            return;
        }
        int value = AlchemistJarRegistry.getItemValue(jars[jarIndex]);
        player.setItemInHand(InteractionHand.MAIN_HAND, AlchemistTokenItem.create(value));
        player.displayClientMessage(
                Component.literal("Picked jar " + (jarIndex + 1) + " ["
                        + AlchemistJarRegistry.describeJar(jars[jarIndex]) + "]"), true);
        syncToClient();
    }

    // ==================== Crafting Session — Operations (token on operation dummy) ====================

    /**
     * Called from empty-hand right-click on an operation dummy.
     * Shows the dummy's current state without consuming anything.
     */
    public void addInteraction(Player player)      { showOperationStatus(player, AlchemistOperation.ADD); }
    public void subtractInteraction(Player player) { showOperationStatus(player, AlchemistOperation.SUBTRACT); }
    public void divideInteraction(Player player)   { showOperationStatus(player, AlchemistOperation.DIVIDE); }
    public void multiplyInteraction(Player player) { showOperationStatus(player, AlchemistOperation.MULTIPLY); }

    private void showOperationStatus(Player player, AlchemistOperation op) {
        if (!session.isActive()) {
            player.displayClientMessage(
                    Component.literal("[" + op.name() + "] Idle — start by picking a jar from D1."), true);
            return;
        }
        Integer first = session.getDummyOperand(op.dummyIndex);
        if (first == null) {
            player.displayClientMessage(
                    Component.literal("[" + op.name() + "] Empty — bring a token here to set the first operand."), true);
        } else {
            player.displayClientMessage(
                    Component.literal("[" + op.name() + "] Primed: _ " + op.symbol + " ? — bring a token to complete."), true);
        }
    }

    /**
     * Called from {@link AlchemistTableDummyBlock#useItemOn} when the player uses an
     * {@link AlchemistTokenItem} on an operation dummy.
     * Consumes the token and either sets the first operand or executes the operation.
     */
    public void handleOperationInteraction(Player player, InteractionHand hand,
                                           ItemStack tokenStack, AlchemistOperation op) {
        if (!session.isActive()) {
            player.displayClientMessage(Component.literal("Pick a jar from D1 first."), true);
            return;
        }
        int held       = AlchemistTokenItem.getValue(tokenStack);
        int dummyIndex = op.dummyIndex;

        if (!session.hasDummyOperand(dummyIndex)) {
            if (session.isFinalResult()) {
                player.displayClientMessage(
                        Component.literal("All jars used — complete an existing operation first."), true);
                return;
            }
            // Store as first operand, consume token
            session.setDummyFirstOperand(dummyIndex, held);
            player.setItemInHand(hand, ItemStack.EMPTY);
            player.displayClientMessage(
                    Component.literal("[" + op.name() + "] Stored " + held + " — pick another jar."), true);
        } else {
            // Execute: first op second
            int firstOp = session.getDummyOperand(dummyIndex);
            int result  = session.executeOperation(dummyIndex, op, held);
            player.displayClientMessage(
                    Component.literal(firstOp + " " + op.symbol + " " + held + " = " + result), true);

            if (session.isFinalResult()) {
                // All jars consumed → store result, clear jars, end session
                player.setItemInHand(hand, ItemStack.EMPTY);
                session.setTokenOut(false);
                storeResultInJar(result, player);
                java.util.Arrays.fill(jars, ItemStack.EMPTY);
                session.reset();
            } else {
                // Give result token back to the player
                player.setItemInHand(hand, AlchemistTokenItem.create(result));
                session.setTokenOut(true);
            }
        }
        syncToClient();
    }

    // ==================== Result Table Jars ====================

    /**
     * Stores a final crafting result into the next empty result jar.
     * The value is saved internally but never shown to the player.
     */
    private void storeResultInJar(int result, Player player) {
        if (resultJarCount >= resultJarValues.length) {
            player.displayClientMessage(
                    Component.literal("Result table is full! (" + resultJarCount + "/3)"), true);
            return;
        }
        resultJarValues[resultJarCount++] = result;
        player.displayClientMessage(
                Component.literal("Result jar " + resultJarCount + " filled! (" + resultJarCount + "/3)"), true);
        // TODO: trigger jar-fill animation on result table dummy
    }

    /**
     * Shows only the fill status of the result table jars — values are intentionally hidden.
     * Called when dummy {@link #RESULT_TABLE_DUMMY_INDEX} is right-clicked empty-handed.
     */
    public void displayResultJars(Player player) {
        StringBuilder sb = new StringBuilder("Result jars: ");
        for (int i = 0; i < resultJarValues.length; i++) {
            if (i > 0) sb.append("  ");
            sb.append("[").append(i + 1).append("] ")
              .append(i < resultJarCount ? "● " + resultJarValues[i] : "○");
        }
        player.displayClientMessage(Component.literal(sb.toString()), true);
    }

    /** Returns the number of result jars currently filled (0-3). */
    public int getResultJarCount() { return resultJarCount; }

    /** Returns the value stored in result jar {@code index}. Only meaningful if {@code index < resultJarCount}. */
    public int getResultJarValue(int index) { return resultJarValues[index]; }

    // ==================== Helpers ====================

    /** Returns true only when all 4 jars are filled to capacity with a known-value item. */
    private boolean allJarsFull() {
        for (ItemStack jar : jars) {
            if (jar.isEmpty() || jar.getCount() < JAR_CAPACITY) return false;
            if (AlchemistJarRegistry.getItemValue(jar) == 0) return false;
        }
        return true;
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
        // Mestolone rotation is driven procedurally in AlchemistTableRenderer — no controller needed.

        // Book (flip animation on dummy index 1)
        controllers.add(new AnimationController<>(this, "book_controller", 0, s -> PlayState.STOP)
                .triggerableAnim("flip", RawAnimation.begin().thenPlay("Libro")));

        // Pestle (grind animation on dummy index 3)
        controllers.add(new AnimationController<>(this, "pestel_controller", 0, s -> PlayState.STOP)
                .triggerableAnim("grind", RawAnimation.begin().thenPlay("Pestel")));

        // Final output jars — shake and pour animations
        controllers.add(new AnimationController<>(this, "jar_f_controller", 0, s -> PlayState.STOP)
                .triggerableAnim("jar_f_1",      RawAnimation.begin().thenPlay("jar_f_1"))
                .triggerableAnim("jar_f_2",      RawAnimation.begin().thenPlay("jar_f_2"))
                .triggerableAnim("jar_f_3",      RawAnimation.begin().thenPlay("jar_f_3"))
                .triggerableAnim("jar_f_1_drop", RawAnimation.begin().thenPlay("jar_f_1_drop"))
                .triggerableAnim("jar_f_2_drop", RawAnimation.begin().thenPlay("jar_f_2_drop"))
                .triggerableAnim("jar_f_3_drop", RawAnimation.begin().thenPlay("jar_f_3_drop")));
        controllers.add(new AnimationController<>(this, "mantice_controller", 0, state -> {
            if(manticeActive){
                state.setAnimation(RawAnimation.begin().thenLoop("mantice"));
                return PlayState.CONTINUE;
            }else{
                state.resetCurrentAnimation();
            }
            return PlayState.STOP;
        }));
        
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
        tag.putFloat("CraftingAngle", craftingAngle);
        craftingHandler.save(tag);
        // Jars
        for (int i = 0; i < jars.length; i++) {
            if (!jars[i].isEmpty()) tag.put("Jar" + i, jars[i].save(registries));
        }
        // Jar interaction entity UUIDs
        for (int i = 0; i < inputJarEntityIds.length; i++)
            if (inputJarEntityIds[i] != null) tag.putUUID("InputJarEnt" + i, inputJarEntityIds[i]);
        for (int i = 0; i < outputJarEntityIds.length; i++)
            if (outputJarEntityIds[i] != null) tag.putUUID("OutputJarEnt" + i, outputJarEntityIds[i]);

        //Cauldron interaction entity UUID
        if (cauldronEntityId != null) tag.putUUID("CauldronEnt", cauldronEntityId);
        // Stirring phase
        stirringPhase.save(tag);
        // Session
        session.save(tag);
        // Result table jars
        tag.putIntArray("ResultJarValues", java.util.Arrays.copyOf(resultJarValues, resultJarCount));
        // Potion extraction state
        tag.putString("PotionState", potionState.name());
        tag.putInt("PotionDoses", potionDoses);
        if (!potionStack.isEmpty())
            tag.put("PotionStack", potionStack.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        manticeActive = tag.getBoolean("ManticeActive");
        craftingAngle = tag.getFloat("CraftingAngle");
        prevCraftingAngle = craftingAngle;
        craftingHandler.load(tag);
        // Jars
        for (int i = 0; i < jars.length; i++) {
            String key = "Jar" + i;
            jars[i] = tag.contains(key)
                    ? ItemStack.parseOptional(registries, tag.getCompound(key))
                    : ItemStack.EMPTY;
        }
        // Jar interaction entity UUIDs
        for (int i = 0; i < inputJarEntityIds.length; i++)
            if (tag.contains("InputJarEnt" + i)) inputJarEntityIds[i] = tag.getUUID("InputJarEnt" + i);
        for (int i = 0; i < outputJarEntityIds.length; i++)
            if (tag.contains("OutputJarEnt" + i)) outputJarEntityIds[i] = tag.getUUID("OutputJarEnt" + i);
        // Cauldron interaction entity UUID
        if (tag.contains("CauldronEnt")) cauldronEntityId = tag.getUUID("CauldronEnt");
        // Stirring phase
        stirringPhase.load(tag);
        // Session
        session.load(tag);
        // Result table jars
        int[] saved = tag.getIntArray("ResultJarValues");
        resultJarCount = Math.min(saved.length, resultJarValues.length);
        System.arraycopy(saved, 0, resultJarValues, 0, resultJarCount);
        // Potion extraction state
        try { potionState = PotionState.valueOf(tag.getString("PotionState")); }
        catch (Exception e) { potionState = PotionState.NONE; }
        potionDoses = tag.getInt("PotionDoses");
        potionStack = tag.contains("PotionStack")
                ? ItemStack.parseOptional(registries, tag.getCompound("PotionStack"))
                : ItemStack.EMPTY;
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
