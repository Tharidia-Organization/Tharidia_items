package com.tharidia.tharidia_things.block;

import com.tharidia.tharidia_things.TharidiaThings;
import com.tharidia.tharidia_things.block.entity.HotIronAnvilEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Invisible marker block that supports block entities.
 * Used to hold the HotIronAnvilEntity above anvils.
 */
public class HotIronMarkerBlock extends Block implements EntityBlock {
    
    public HotIronMarkerBlock() {
        super(BlockBehaviour.Properties.of()
            .mapColor(MapColor.NONE)
            .noCollission()
            .noOcclusion()
            .noLootTable()
            .replaceable() // Can be replaced by other blocks but not considered air
        );
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE; // Don't render the block itself
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty(); // No collision
    }
    
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty(); // No collision
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        TharidiaThings.LOGGER.info("Creating new HotIronAnvilEntity for marker block at: " + pos);
        return new HotIronAnvilEntity(pos, state);
    }
}
