package com.THproject.tharidia_things.block;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.entity.StableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class StableBlock extends BaseEntityBlock {
    
    public static final MapCodec<StableBlock> CODEC = simpleCodec(StableBlock::new);
    
    // Collision shape - 2 blocks tall to match model height
    // Note: VoxelShape cannot reliably extend beyond single block bounds (0-1 range)
    // For true 3x3 collision, would need multiblock structure with marker blocks
    private static final VoxelShape COLLISION_SHAPE = Shapes.box(0, 0, 0, 1, 2, 1);
    
    public StableBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
    
    public StableBlock() {
        this(BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .strength(2.5F)
            .noOcclusion());
    }
    
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION_SHAPE;
    }
    
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION_SHAPE;
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
    
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            formMultiblock(level, pos);
        }
    }
    
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            destroyMultiblock(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
    
    private void formMultiblock(Level level, BlockPos masterPos) {
        // Create 3x3x2 structure with dummy blocks
        // Master is at center, create dummies around it
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    // Skip center block (master)
                    if (x == 0 && y == 0 && z == 0) continue;
                    
                    BlockPos dummyPos = masterPos.offset(x, y, z);
                    BlockState existingState = level.getBlockState(dummyPos);
                    
                    // Only place dummy if space is empty or replaceable
                    if (existingState.isAir() || existingState.canBeReplaced()) {
                        level.setBlock(dummyPos, TharidiaThings.STABLE_DUMMY.get().defaultBlockState()
                            .setValue(StableDummyBlock.FACING, net.minecraft.core.Direction.NORTH), 3);
                    }
                }
            }
        }
    }
    
    private void destroyMultiblock(Level level, BlockPos masterPos) {
        // Remove all dummy blocks in 3x3x2 area
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    
                    BlockPos dummyPos = masterPos.offset(x, y, z);
                    BlockState dummyState = level.getBlockState(dummyPos);
                    if (dummyState.getBlock() instanceof StableDummyBlock) {
                        level.removeBlock(dummyPos, false);
                    }
                }
            }
        }
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StableBlockEntity(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, TharidiaThings.STABLE_BLOCK_ENTITY.get(), 
            (lvl, pos, st, entity) -> StableBlockEntity.serverTick(lvl, pos, st, entity));
    }
    
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof StableBlockEntity stable)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        
        // Check if placing baby animal
        if (stack.is(TharidiaThings.BABY_COW.get()) && stable.placeAnimal("cow")) {
            stack.shrink(1);
            return ItemInteractionResult.SUCCESS;
        } else if (stack.is(TharidiaThings.BABY_CHICKEN.get()) && stable.placeAnimal("chicken")) {
            stack.shrink(1);
            return ItemInteractionResult.SUCCESS;
        }
        
        // Check if collecting milk
        if (stack.is(Items.BUCKET) && stable.canCollectMilk()) {
            stable.collectMilk(player);
            stack.shrink(1);
            player.addItem(new ItemStack(Items.MILK_BUCKET));
            return ItemInteractionResult.SUCCESS;
        }
        
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.PASS;
        }
        
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof StableBlockEntity stable)) {
            return InteractionResult.PASS;
        }
        
        // Collect eggs
        if (stable.canCollectEggs()) {
            stable.collectEggs(player);
            return InteractionResult.SUCCESS;
        }
        
        return InteractionResult.PASS;
    }
    
    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof StableBlockEntity stable) {
            return !stable.hasAnimal();
        }
        return super.canHarvestBlock(state, level, pos, player);
    }
    
    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof StableBlockEntity stable && stable.hasAnimal()) {
            return -1.0F;
        }
        return super.getDestroyProgress(state, player, level, pos);
    }
}
