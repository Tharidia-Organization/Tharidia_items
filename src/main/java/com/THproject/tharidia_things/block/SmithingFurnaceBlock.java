package com.THproject.tharidia_things.block;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.entity.SmithingFurnaceBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import com.THproject.tharidia_things.item.PinzaCrucibleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Smithing Furnace - A 5x2x3 multiblock GeckoLib animated block.
 *
 * Dimensions: 5 blocks wide (X) x 2 blocks tall (Y) x 3 blocks deep (Z)
 * The master block is at position (0,0,0) relative to the multiblock.
 *
 * Features:
 * - GeckoLib rendering with permanent "levitate2" animation
 * - Tier property (0-4) for future bone visibility upgrades
 * - Multiblock structure with dummy blocks for collision
 */
public class SmithingFurnaceBlock extends BaseEntityBlock {

    public static final MapCodec<SmithingFurnaceBlock> CODEC = simpleCodec(SmithingFurnaceBlock::new);

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty TIER = IntegerProperty.create("tier", 0, 4);

    // Multiblock dimensions: 5 wide (X), 2 tall (Y), 2 deep (Z)
    // Master is at center of X axis, corner of Z axis
    private static final int WIDTH = 5;  // X dimension
    private static final int HEIGHT = 2; // Y dimension
    private static final int DEPTH = 2;  // Z dimension (was 3, now 2)

    // Offset to center the multiblock on the master position (X axis)
    private static final int X_OFFSET = -2;  // Shifts structure 2 blocks

    // Full block collision shape for the master block position
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public SmithingFurnaceBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(TIER, 0));
    }

    public SmithingFurnaceBlock() {
        this(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(5.0F, 6.0F)
                .noOcclusion()
                .lightLevel(state -> 7));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TIER);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // GeckoLib handles rendering
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SmithingFurnaceBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) {
            // Client tick for particle effects
            return createTickerHelper(blockEntityType, TharidiaThings.SMITHING_FURNACE_BLOCK_ENTITY.get(),
                    SmithingFurnaceBlockEntity::clientTick);
        } else {
            // Server tick for coal consumption
            return createTickerHelper(blockEntityType, TharidiaThings.SMITHING_FURNACE_BLOCK_ENTITY.get(),
                    SmithingFurnaceBlockEntity::serverTick);
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof SmithingFurnaceBlockEntity furnace) {
            // Handle coal insertion
            if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) {
                if (furnace.canAddCoal()) {
                    if (!level.isClientSide) {
                        // Add coal (2 items = 1 visual coal bone)
                        int toAdd = Math.min(stack.getCount(), furnace.canAddCoal() ? 8 - furnace.getCoalCount() : 0);
                        int added = furnace.addCoal(toAdd);
                        if (added > 0 && !player.getAbilities().instabuild) {
                            stack.shrink(added);
                        }
                        // Play sound
                        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5f, 0.8f);
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide);
                }
                return ItemInteractionResult.FAIL;
            }

            // Handle flint and steel to light the furnace
            if (stack.is(Items.FLINT_AND_STEEL)) {
                if (furnace.hasCoal() && !furnace.isActive()) {
                    if (!level.isClientSide) {
                        furnace.lightFurnace();
                        // Damage flint and steel
                        stack.hurtAndBreak(1, player, player.getEquipmentSlotForItem(stack));
                        // Play fire ignite sound
                        level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide);
                }
                return ItemInteractionResult.FAIL;
            }

            // Handle shovel to remove ash (only when door is open)
            if (stack.is(ItemTags.SHOVELS)) {
                if (furnace.isDoorOpen() && furnace.getAshCount() > 0) {
                    if (!level.isClientSide) {
                        furnace.removeAsh();
                        // Give 1-3 ash items directly to player inventory
                        int ashAmount = 1 + level.getRandom().nextInt(3);
                        ItemStack ashStack = new ItemStack(TharidiaThings.ASH.get(), ashAmount);
                        player.addItem(ashStack);
                        // Damage shovel
                        stack.hurtAndBreak(1, player, player.getEquipmentSlotForItem(stack));
                        // Play shovel sound
                        level.playSound(null, pos, SoundEvents.SAND_BREAK, SoundSource.BLOCKS, 0.5f, 0.8f);
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide);
                }
                return ItemInteractionResult.FAIL;
            }

            // Raw ore insertion (furnace must be active, no smelting in progress, no molten metal)
            if (stack.is(Items.RAW_IRON) || stack.is(Items.RAW_GOLD) || stack.is(Items.RAW_COPPER)) {
                if (furnace.isActive() && !furnace.isSmeltingInProgress() && !furnace.hasMoltenMetal()) {
                    if (!level.isClientSide) {
                        String type = stack.is(Items.RAW_IRON) ? "iron"
                                    : stack.is(Items.RAW_GOLD) ? "gold" : "copper";
                        furnace.insertRawOre(type);
                        if (!player.getAbilities().instabuild) {
                            stack.shrink(1);
                        }
                        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5f, 0.8f);
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide);
                }
                return ItemInteractionResult.FAIL;
            }

            // Pinza crucible: pick up or pour molten metal
            if (stack.getItem() instanceof PinzaCrucibleItem) {
                String heldFluid = PinzaCrucibleItem.getFluidType(stack);

                if (heldFluid.isEmpty()) {
                    // PINZA VUOTA: pick up from tiny crucible (existing behavior)
                    if (furnace.hasMoltenMetal()) {
                        if (!level.isClientSide) {
                            String metalType = furnace.removeMoltenMetal();
                            PinzaCrucibleItem.setFluidType(stack, metalType);
                            level.playSound(null, pos, SoundEvents.BUCKET_FILL_LAVA, SoundSource.BLOCKS, 0.5f, 1.0f);
                        }
                        return ItemInteractionResult.sidedSuccess(level.isClientSide);
                    }
                    return ItemInteractionResult.FAIL;
                } else {
                    // PINZA PIENA: pour into big crucible or cast_ingot
                    if (furnace.hasCrucible()) {
                        // Big crucible installed → pour into big crucible
                        if (!level.isClientSide) {
                            if (furnace.pourIntoBigCrucible(heldFluid)) {
                                PinzaCrucibleItem.clearFluid(stack);
                                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 0.5f, 1.0f);
                            } else {
                                return ItemInteractionResult.FAIL;
                            }
                        }
                        return ItemInteractionResult.sidedSuccess(level.isClientSide);
                    } else {
                        // No big crucible → pour directly into cast_ingot
                        if (!level.isClientSide) {
                            if (furnace.pourIntoCast(heldFluid)) {
                                PinzaCrucibleItem.clearFluid(stack);
                                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 0.5f, 1.0f);
                            } else {
                                return ItemInteractionResult.FAIL;
                            }
                        }
                        return ItemInteractionResult.sidedSuccess(level.isClientSide);
                    }
                }
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        // Empty hand interaction - no longer toggles active state directly
        // Coal must be added with coal item, lit with flint and steel
        return InteractionResult.PASS;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            if (!canFormMultiblock(level, pos, state.getValue(FACING))) {
                // Can't form multiblock, remove the block and drop item
                level.destroyBlock(pos, true);
                return;
            }
            formMultiblock(level, pos, state.getValue(FACING));
        }
    }

    /**
     * Checks if the multiblock can be formed at the given position
     */
    private boolean canFormMultiblock(Level level, BlockPos masterPos, Direction facing) {
        // Check all positions in the 5x2x3 area
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int z = 0; z < DEPTH; z++) {
                    // Skip master position (after offset, x=2 maps to center 0)
                    if (isMasterPosition(x, y, z)) continue;

                    BlockPos checkPos = getOffsetPos(masterPos, x, y, z, facing);
                    BlockState existingState = level.getBlockState(checkPos);

                    // Check if space is clear or replaceable
                    if (!existingState.isAir() && !existingState.canBeReplaced()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Checks if the given local coordinates map to the master position after offset
     */
    private boolean isMasterPosition(int localX, int localY, int localZ) {
        return (localX + X_OFFSET) == 0 && localY == 0 && localZ == 0;
    }

    /**
     * Forms the multiblock by placing dummy blocks
     */
    private void formMultiblock(Level level, BlockPos masterPos, Direction facing) {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int z = 0; z < DEPTH; z++) {
                    // Skip master position (after offset, x=2 maps to center 0)
                    if (isMasterPosition(x, y, z)) continue;

                    BlockPos dummyPos = getOffsetPos(masterPos, x, y, z, facing);

                    level.setBlock(dummyPos, TharidiaThings.SMITHING_FURNACE_DUMMY.get().defaultBlockState()
                            .setValue(SmithingFurnaceDummyBlock.OFFSET_X, x)
                            .setValue(SmithingFurnaceDummyBlock.OFFSET_Y, y)
                            .setValue(SmithingFurnaceDummyBlock.OFFSET_Z, z)
                            .setValue(SmithingFurnaceDummyBlock.FACING, facing), 3);
                }
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            destroyMultiblock(level, pos, state.getValue(FACING));
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    /**
     * Destroys all dummy blocks belonging to this multiblock
     */
    private void destroyMultiblock(Level level, BlockPos masterPos, Direction facing) {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int z = 0; z < DEPTH; z++) {
                    // Skip master position (after offset, x=2 maps to center 0)
                    if (isMasterPosition(x, y, z)) continue;

                    BlockPos dummyPos = getOffsetPos(masterPos, x, y, z, facing);
                    BlockState dummyState = level.getBlockState(dummyPos);

                    if (dummyState.getBlock() instanceof SmithingFurnaceDummyBlock) {
                        // Verify this dummy belongs to this master
                        int storedX = dummyState.getValue(SmithingFurnaceDummyBlock.OFFSET_X);
                        int storedY = dummyState.getValue(SmithingFurnaceDummyBlock.OFFSET_Y);
                        int storedZ = dummyState.getValue(SmithingFurnaceDummyBlock.OFFSET_Z);

                        if (storedX == x && storedY == y && storedZ == z) {
                            level.removeBlock(dummyPos, false);
                        }
                    }
                }
            }
        }
    }

    /**
     * Calculates world position from local offset based on facing direction.
     *
     * The model faces SOUTH in Blockbench. GeckoLib applies rotation:
     * - NORTH: 0° (but we add 180° compensation, so model faces NORTH)
     * - SOUTH: 180° (+ 180° = 360° = 0°, model faces SOUTH)
     * - EAST: -90° (+ 180° = 90°)
     * - WEST: 90° (+ 180° = 270°)
     *
     * Dummy blocks must follow the same rotation pattern.
     */
    private BlockPos getOffsetPos(BlockPos masterPos, int localX, int localY, int localZ, Direction facing) {
        // Apply X offset to center the structure
        int centeredX = localX + X_OFFSET;

        // Transform local coordinates based on facing direction
        // Local X = left/right (perpendicular to facing)
        // Local Z = front/back (along facing direction)
        int worldX, worldZ;

        switch (facing) {
            case NORTH -> {
                // Block faces north: local +Z goes to world -Z
                worldX = -centeredX;
                worldZ = -localZ;
            }
            case SOUTH -> {
                // Block faces south: local +Z goes to world +Z
                worldX = centeredX;
                worldZ = localZ;
            }
            case EAST -> {
                // Block faces east: local +Z goes to world +X
                worldX = localZ;
                worldZ = -centeredX;
            }
            case WEST -> {
                // Block faces west: local +Z goes to world -X
                worldX = -localZ;
                worldZ = centeredX;
            }
            default -> {
                worldX = centeredX;
                worldZ = localZ;
            }
        }

        return masterPos.offset(worldX, localY, worldZ);
    }

    /**
     * Calculates the master position from a dummy position and its offsets.
     * This is the reverse transformation of getOffsetPos.
     */
    public static BlockPos getMasterPosFromDummy(BlockPos dummyPos, int offsetX, int offsetY, int offsetZ, Direction facing) {
        // Apply X offset to get the centered position
        int centeredX = offsetX + X_OFFSET;

        // Reverse transformation to get back to master position
        int worldX, worldZ;

        switch (facing) {
            case NORTH -> {
                // Reverse of: worldX = -centeredX, worldZ = -localZ
                worldX = centeredX;
                worldZ = offsetZ;
            }
            case SOUTH -> {
                // Reverse of: worldX = centeredX, worldZ = localZ
                worldX = -centeredX;
                worldZ = -offsetZ;
            }
            case EAST -> {
                // Reverse of: worldX = localZ, worldZ = -centeredX
                worldX = -offsetZ;
                worldZ = centeredX;
            }
            case WEST -> {
                // Reverse of: worldX = -localZ, worldZ = centeredX
                worldX = offsetZ;
                worldZ = -centeredX;
            }
            default -> {
                worldX = -centeredX;
                worldZ = -offsetZ;
            }
        }

        return dummyPos.offset(worldX, -offsetY, worldZ);
    }
}
