package com.THproject.tharidia_things.block;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.entity.HotGoldAnvilEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Invisible marker block that supports block entities.
 * Used to hold the HotGoldAnvilEntity above anvils.
 */
public class HotGoldMarkerBlock extends Block implements EntityBlock {

    public HotGoldMarkerBlock() {
        super(BlockBehaviour.Properties.of()
            .mapColor(MapColor.NONE)
            .noCollission()
            .noOcclusion()
            .noLootTable()
            .pushReaction(PushReaction.BLOCK)
        );
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide) {
            BlockState below = level.getBlockState(pos.below());
            if (!(below.getBlock() instanceof AnvilBlock)) {
                level.removeBlock(pos, false);
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 0.5F);
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        10, 0.3, 0.2, 0.3, 0.02);
                }
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HotGoldAnvilEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        if (type == TharidiaThings.HOT_GOLD_ANVIL_ENTITY.get()) {
            return (lvl, pos, st, be) -> ((HotGoldAnvilEntity) be).serverTick();
        }
        return null;
    }
}
