package com.THproject.tharidia_things.block;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.entity.StableBlockEntity;
import com.THproject.tharidia_things.registry.BabyMobRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShovelItem;
import com.THproject.tharidia_things.item.PitchforkItem;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class StableBlock extends BaseEntityBlock {

    public static final MapCodec<StableBlock> CODEC = simpleCodec(StableBlock::new);

    // Floor collision shape - approximately 6 pixels high to match the scaled floor/path height (base at Y 2.8)
    private static final VoxelShape FLOOR_SHAPE = Shapes.box(0, 0, 0, 1, 0.4, 1);
    
    public StableBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
    
    public StableBlock() {
        this(BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .strength(2.5F)
            .noOcclusion()
            .noLootTable()  // We handle drops manually in onRemove
            .lightLevel(state -> 15));
    }
    
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
    
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FLOOR_SHAPE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FLOOR_SHAPE;
    }

    @Override
    public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        if (level.getBlockEntity(pos) instanceof StableBlockEntity stable && stable.getManureAmount() > 0) {
            return SoundType.MUD;
        }
        return super.getSoundType(state, level, pos, entity);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }
    
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            if (!canFormMultiblock(level, pos)) {
                // Can't form multiblock, remove the block and drop item
                level.destroyBlock(pos, true);
                return;
            }
            formMultiblock(level, pos);
        }
    }

    private boolean canFormMultiblock(Level level, BlockPos masterPos) {
        // Check the 5x5 multiblock area (-2 to +2) for conflicts with existing stables
        // Also check 1 block buffer around to prevent overlapping dummies
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                if (x == 0 && z == 0) continue;

                BlockPos checkPos = masterPos.offset(x, 0, z);
                BlockState existingState = level.getBlockState(checkPos);

                // If there's already a stable master or dummy here, can't form
                if (existingState.getBlock() instanceof StableBlock ||
                    existingState.getBlock() instanceof StableDummyBlock) {
                    return false;
                }
            }
        }
        return true;
    }
    
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            // Drop the stable item - handles both direct breaking and destruction via dummy
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof StableBlockEntity) {
                // Only drop if block entity exists (first removal)
                net.minecraft.world.level.block.Block.popResource(level, pos, new ItemStack(TharidiaThings.STABLE.get()));
            }
            destroyMultiblock(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
    
    private void formMultiblock(Level level, BlockPos masterPos) {
        // Create 5x5 ground-level interaction layer only (no upper blocks)
        // Master is at center, create dummies around it at ground level only
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                // Skip center block (master)
                if (x == 0 && z == 0) continue;

                BlockPos dummyPos = masterPos.offset(x, 0, z);
                BlockState existingState = level.getBlockState(dummyPos);

                // Only place dummy if space is empty or replaceable
                if (existingState.isAir() || existingState.canBeReplaced()) {
                    // Offset to master is (-x, -z), stored as (-x+2, -z+2) for 0-4 range
                    level.setBlock(dummyPos, TharidiaThings.STABLE_DUMMY.get().defaultBlockState()
                        .setValue(StableDummyBlock.FACING, net.minecraft.core.Direction.NORTH)
                        .setValue(StableDummyBlock.OFFSET_X, -x + 2)
                        .setValue(StableDummyBlock.OFFSET_Z, -z + 2), 3);
                }
            }
        }
    }
    
    private void destroyMultiblock(Level level, BlockPos masterPos) {
        // Remove all dummy blocks in 5x5 ground-level area that belong to this master
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (x == 0 && z == 0) continue;

                BlockPos dummyPos = masterPos.offset(x, 0, z);
                BlockState dummyState = level.getBlockState(dummyPos);
                if (dummyState.getBlock() instanceof StableDummyBlock) {
                    // Verify this dummy belongs to this master by checking offsets
                    int expectedOffsetX = -x + 2;
                    int expectedOffsetZ = -z + 2;
                    if (dummyState.getValue(StableDummyBlock.OFFSET_X) == expectedOffsetX &&
                        dummyState.getValue(StableDummyBlock.OFFSET_Z) == expectedOffsetZ) {
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
        EntityType<?> entityType = BabyMobRegistry.getEntityTypeForItem(stack.getItem());
        if (entityType != null && stable.placeAnimal(entityType)) {
            stack.shrink(1);
            return ItemInteractionResult.SUCCESS;
        }
        
        // Check if adding animal feed to feeder
        if (stack.is(TharidiaThings.ANIMAL_FEED.get()) && stable.canAddAnimalFeed()) {
            stable.addAnimalFeed();
            stack.shrink(1);
            return ItemInteractionResult.SUCCESS;
        }
        
        // Check if feeding animals for breeding
        // Now supports all animals via AnimalTypeHelper
        if (stable.canFeed(stack)) {
            stable.feed(stack);
            stack.shrink(1);
            return ItemInteractionResult.SUCCESS;
        }

        // Provide feedback if player tries to feed but cannot (breeding requirements not met)
        // Only trigger if this looks like a breeding food attempt
        if (stable.hasAnimal() && stable.getAnimals().size() == 2 &&
            com.THproject.tharidia_things.stable.AnimalTypeHelper.isValidBreedingFood(stable.getAnimalType(), stack)) {
            // Check why breeding failed and give appropriate feedback
            level.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.5F, 1.2F);
            return ItemInteractionResult.FAIL;
        }
        
        // Check if refilling water with water bucket
        if (stack.is(Items.WATER_BUCKET) && stable.canRefillWater()) {
            stable.refillWater();
            stack.shrink(1);
            player.addItem(new ItemStack(Items.BUCKET));
            return ItemInteractionResult.SUCCESS;
        }
        
        // Check if collecting milk
        if (stack.is(Items.BUCKET) && stable.canCollectMilk()) {
            stable.collectMilk(player);
            stack.shrink(1);
            // Milk bucket is given inside collectMilk() - don't duplicate
            return ItemInteractionResult.SUCCESS;
        }

        // Check if collecting manure with shovel
        if (stack.getItem() instanceof ShovelItem && stable.canCollectManure()) {
            stable.collectManure(player);
            return ItemInteractionResult.SUCCESS;
        }

        // Check if placing fresh straw for bedding (Houseboundry)
        // Only allow if bedding freshness is below 70% (needs replacement)
        if (stack.is(TharidiaThings.FRESH_STRAW.get())) {
            if (stable.getBeddingFreshness() < 70) {
                int cfg_freshness = com.THproject.tharidia_things.stable.StableConfigLoader.getConfig().beddingStartFreshness();
                stable.setBeddingFreshness(cfg_freshness);
                stack.shrink(1);
                level.playSound(null, pos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                return ItemInteractionResult.SUCCESS;
            } else {
                // Bedding is still fresh, don't waste straw
                level.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.5F, 1.2F);
                return ItemInteractionResult.FAIL;
            }
        }

        // Check if removing bedding with pitchfork (Houseboundry)
        if (stack.getItem() instanceof PitchforkItem && stable.hasBedding()) {
            int currentFreshness = stable.getBeddingFreshness();
            if (stable.removeBedding()) {
                // Drop fresh straw if freshness was >= 50%, otherwise dirty straw
                if (currentFreshness >= 50) {
                    player.addItem(new ItemStack(TharidiaThings.FRESH_STRAW.get()));
                    level.playSound(null, pos, SoundEvents.GRASS_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
                } else {
                    player.addItem(new ItemStack(TharidiaThings.DIRTY_STRAW.get()));
                    level.playSound(null, pos, SoundEvents.GRASS_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F);
                }
                // Damage pitchfork
                stack.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
                return ItemInteractionResult.SUCCESS;
            }
        }

        // Check if brushing animals (Houseboundry wellness)
        if (stack.is(TharidiaThings.ANIMAL_BRUSH.get()) && stable.hasAnimal()) {
            if (stable.applyBrush()) {
                // Damage brush
                stack.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
                return ItemInteractionResult.SUCCESS;
            } else {
                // On cooldown - indicate to player
                level.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.5F, 1.2F);
                return ItemInteractionResult.FAIL;
            }
        }

        // Check if curing disease with honey bottle (Houseboundry)
        if (stack.is(Items.HONEY_BOTTLE) && stable.hasDiseasedAnimal()) {
            boolean cured = stable.tryHoneyCure();
            stack.shrink(1);
            player.addItem(new ItemStack(Items.GLASS_BOTTLE));
            if (cured) {
                level.playSound(null, pos, SoundEvents.PLAYER_BURP, SoundSource.BLOCKS, 1.0F, 1.2F);
            } else {
                level.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.8F, 0.8F);
            }
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
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof StableBlockEntity stable && stable.getManureAmount() > 0) {
            // Spawn stink particles based on manure level
            int manureLevel = stable.getManureAmount();
            // More particles with more manure
            int particleCount = manureLevel >= 66 ? 3 : (manureLevel >= 33 ? 2 : 1);

            for (int i = 0; i < particleCount; i++) {
                if (random.nextInt(15) < manureLevel / 25) { // Higher chance with more manure
                    double x = pos.getX() + random.nextDouble() * 2 - 0.5; // Spread across the stable area
                    double y = pos.getY() + 0.5 + random.nextDouble() * 0.5;
                    double z = pos.getZ() + random.nextDouble() * 2 - 0.5;
                    // Use campfire smoke for stink effect (greenish-brown tint visual)
                    level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 0.0, 0.02, 0.0);
                }
            }
        }
    }

    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {
        return true;
    }
    
    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof StableBlockEntity stable && stable.hasAnimal()) {
            return 0.0F;
        }
        return super.getDestroyProgress(state, player, level, pos);
    }
    
    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, net.minecraft.world.level.material.FluidState fluid) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof StableBlockEntity stable && stable.hasAnimal()) {
            return false;
        }
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }
}
