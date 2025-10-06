package com.tharidia.tharidia_things.block;

import com.mojang.serialization.MapCodec;
import com.tharidia.tharidia_things.block.entity.PietroBlockEntity;
import com.tharidia.tharidia_things.realm.RealmManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PietroBlock extends BaseEntityBlock {
    public static final MapCodec<PietroBlock> CODEC = simpleCodec(PietroBlock::new);
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    // Minimum distance is 9 chunks (2 * max_radius where max_radius = 5)
    // This allows both realms to expand to 9x9 and just touch at the edges
    public static final int MIN_DISTANCE_CHUNKS = 50;

    public PietroBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // Only create block entity for lower half
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new PietroBlockEntity(pos, state) : null;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos blockpos = context.getClickedPos();
        Level level = context.getLevel();
        if (blockpos.getY() < level.getMaxBuildHeight() - 1 && level.getBlockState(blockpos.above()).canBeReplaced(context)) {
            return this.defaultBlockState().setValue(HALF, DoubleBlockHalf.LOWER);
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        // Place upper half
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);

        // Store the player's name who placed the block
        if (!level.isClientSide && placer instanceof Player player) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof PietroBlockEntity pietroBlockEntity) {
                pietroBlockEntity.setOwner(player.getName().getString());
            }
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        DoubleBlockHalf half = state.getValue(HALF);
        if (direction.getAxis() == Direction.Axis.Y) {
            if (half == DoubleBlockHalf.LOWER && direction == Direction.UP) {
                return neighborState.is(this) && neighborState.getValue(HALF) == DoubleBlockHalf.UPPER ? state : Blocks.AIR.defaultBlockState();
            }
            if (half == DoubleBlockHalf.UPPER && direction == Direction.DOWN) {
                return neighborState.is(this) && neighborState.getValue(HALF) == DoubleBlockHalf.LOWER ? state : Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Block.box(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            if (player.isCreative()) {
                preventDropFromBottomPart(level, pos, state, player);
            }
        }
        super.playerWillDestroy(level, pos, state, player);
        return state;
    }

    protected static void preventDropFromBottomPart(Level level, BlockPos pos, BlockState state, Player player) {
        DoubleBlockHalf half = state.getValue(HALF);
        if (half == DoubleBlockHalf.UPPER) {
            BlockPos blockpos = pos.below();
            BlockState blockstate = level.getBlockState(blockpos);
            if (blockstate.is(state.getBlock()) && blockstate.getValue(HALF) == DoubleBlockHalf.LOWER) {
                level.setBlock(blockpos, Blocks.AIR.defaultBlockState(), 35);
                level.levelEvent(player, 2001, blockpos, Block.getId(blockstate));
            }
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            // If upper half, redirect to lower half
            BlockPos lowerPos = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
            BlockEntity blockEntity = level.getBlockEntity(lowerPos);
            if (blockEntity instanceof PietroBlockEntity pietroBlockEntity) {
                // Check if player is holding potatoes
                if (stack.is(Items.POTATO)) {
                    int currentSize = pietroBlockEntity.getRealmSize();

                    // Check if already at max size
                    if (currentSize >= 15) {
                        player.sendSystemMessage(Component.literal("§cRealm is already at maximum size (15x15 chunks)!"));
                        return ItemInteractionResult.FAIL;
                    }

                    // Get potato info
                    int potatoAmount = stack.getCount();
                    int currentStored = pietroBlockEntity.getStoredPotatoes();
                    int required = pietroBlockEntity.getPotatoCostForNextLevel();
                    int remaining = pietroBlockEntity.getRemainingPotatoesNeeded();

                    // Calculate how many potatoes to consume
                    int toConsume = Math.min(potatoAmount, remaining);

                    // Add potatoes and check if expansion occurred
                    boolean expanded = pietroBlockEntity.addPotatoes(toConsume);

                    // Consume the potatoes from player's hand
                    if (!player.isCreative()) {
                        stack.shrink(toConsume);
                    }

                    if (expanded) {
                        // Expansion occurred!
                        player.sendSystemMessage(Component.literal("§a✓ Regno espanso a " + pietroBlockEntity.getRealmSize() + "x" + pietroBlockEntity.getRealmSize() + " chunks!"));

                        // Show info for next level if not at max
                        if (pietroBlockEntity.getRealmSize() < 10) {
                            int nextRequired = pietroBlockEntity.getPotatoCostForNextLevel();
                            int nextStored = pietroBlockEntity.getStoredPotatoes();
                            int nextRemaining = pietroBlockEntity.getRemainingPotatoesNeeded();
                            player.sendSystemMessage(Component.literal("§7Prossimo livello: §e" + nextStored + "§7/§e" + nextRequired + " §7patate (§6" + nextRemaining + " §7necessarie)"));
                        }
                    } else {
                        // Show progress
                        int newStored = pietroBlockEntity.getStoredPotatoes();
                        int newRemaining = pietroBlockEntity.getRemainingPotatoesNeeded();
                        player.sendSystemMessage(Component.literal("§e+" + toConsume + " patate §7aggiunte!"));
                        player.sendSystemMessage(Component.literal("§7Progresso: §e" + newStored + "§7/§e" + required + " §7(§6" + newRemaining + " §7necessarie per espandere)"));
                    }

                    return ItemInteractionResult.SUCCESS;
                }
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            // If upper half, redirect to lower half
            BlockPos lowerPos = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
            BlockEntity blockEntity = level.getBlockEntity(lowerPos);
            if (blockEntity instanceof PietroBlockEntity pietroBlockEntity) {
                // Show current progress when clicked without item
                int currentSize = pietroBlockEntity.getRealmSize();

                if (currentSize >= 10) {
                    player.sendSystemMessage(Component.literal("§6Regno al massimo livello (10x10 chunks)!"));
                } else {
                    int stored = pietroBlockEntity.getStoredPotatoes();
                    int required = pietroBlockEntity.getPotatoCostForNextLevel();
                    int remaining = pietroBlockEntity.getRemainingPotatoesNeeded();

                    player.sendSystemMessage(Component.literal("§6Regno: §e" + currentSize + "x" + currentSize + " chunks"));
                    player.sendSystemMessage(Component.literal("§7Patate: §e" + stored + "§7/§e" + required + " §7(§6" + remaining + " §7necessarie)"));
                }

                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    /**
     * Checks if a Pietro block can be placed at the given position
     * @param level The server level
     * @param pos The position to check
     * @return true if the block can be placed, false otherwise
     */
    public static boolean canPlacePietroBlock(ServerLevel level, BlockPos pos) {
        ChunkPos newChunkPos = new ChunkPos(pos);
        List<PietroBlockEntity> existingRealms = RealmManager.getRealms(level);

        for (PietroBlockEntity realm : existingRealms) {
            // Skip if checking the same block (for updates)
            if (realm.getBlockPos().equals(pos)) {
                continue;
            }

            ChunkPos realmChunkPos = realm.getCenterChunk();

            // Calculate distance in chunks (using Chebyshev distance for square realms)
            int dx = Math.abs(newChunkPos.x - realmChunkPos.x);
            int dz = Math.abs(newChunkPos.z - realmChunkPos.z);
            int distance = Math.max(dx, dz);

            if (distance < MIN_DISTANCE_CHUNKS) {
                return false;
            }
        }

        return true;
    }

    /**
     * Gets the distance to the nearest Pietro block
     * @param level The server level
     * @param pos The position to check from
     * @return The distance in chunks, or -1 if no realms exist
     */
    public static int getDistanceToNearestRealm(ServerLevel level, BlockPos pos) {
        ChunkPos checkPos = new ChunkPos(pos);
        List<PietroBlockEntity> existingRealms = RealmManager.getRealms(level);

        if (existingRealms.isEmpty()) {
            return -1;
        }

        int minDistance = Integer.MAX_VALUE;

        for (PietroBlockEntity realm : existingRealms) {
            ChunkPos realmChunkPos = realm.getCenterChunk();
            int dx = Math.abs(checkPos.x - realmChunkPos.x);
            int dz = Math.abs(checkPos.z - realmChunkPos.z);
            int distance = Math.max(dx, dz);
            minDistance = Math.min(minDistance, distance);
        }

        return minDistance;
    }
}
