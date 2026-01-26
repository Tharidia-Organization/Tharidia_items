package com.THproject.tharidia_things.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Custom dungeon portal block - visually identical to a nether portal but red.
 * Triggers the vanilla portal entry effect (CONFUSION overlay).
 * Teleportation destination is managed server-side by tharidia_features.
 *
 * <p>Has an AXIS property (X or Z) that determines orientation,
 * identical to vanilla nether portal shape behavior.
 */
public class DungeonPortalBlock extends Block implements Portal {

    public static final MapCodec<DungeonPortalBlock> CODEC = simpleCodec(DungeonPortalBlock::new);

    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    // Same shapes as nether portal
    private static final VoxelShape X_AXIS_SHAPE = Block.box(0.0, 0.0, 6.0, 16.0, 16.0, 10.0);
    private static final VoxelShape Z_AXIS_SHAPE = Block.box(6.0, 0.0, 0.0, 10.0, 16.0, 16.0);

    public DungeonPortalBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.Z ? Z_AXIS_SHAPE : X_AXIS_SHAPE;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    // --- Portal interface implementation ---

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity.canUsePortal(false)) {
            entity.setAsInsidePortal(this, pos);
        }
    }

    @Override
    public int getPortalTransitionTime(ServerLevel level, Entity entity) {
        return 200; // Visual builds up over 10s; actual teleport at 80 ticks by tharidia_features handler
    }

    @Override
    @Nullable
    public DimensionTransition getPortalDestination(ServerLevel level, Entity entity, BlockPos pos) {
        // Teleportation handled by tharidia_features server-side
        return null;
    }

    @Override
    public Transition getLocalTransition() {
        return Transition.CONFUSION;
    }
}
