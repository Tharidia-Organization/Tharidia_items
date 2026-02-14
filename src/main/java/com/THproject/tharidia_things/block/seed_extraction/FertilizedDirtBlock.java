package com.THproject.tharidia_things.block.seed_extraction;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class FertilizedDirtBlock extends Block {

    public static final IntegerProperty GROWTH_STAGE = IntegerProperty.create("growth_stage", 0, 3);

    public FertilizedDirtBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(GROWTH_STAGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GROWTH_STAGE);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos above = pos.above();

        // If abnormal grass already exists above, do nothing
        if (level.getBlockState(above).is(SeedExtractionRegistry.ABNORMAL_GRASS.get())) {
            return;
        }

        // Requires sky visibility
        if (!level.canSeeSky(above)) {
            return;
        }

        int stage = state.getValue(GROWTH_STAGE);
        if (stage < 3) {
            // Increment growth stage
            level.setBlock(pos, state.setValue(GROWTH_STAGE, stage + 1), Block.UPDATE_ALL);
        } else {
            // Stage 3 reached: place abnormal grass above and reset
            if (level.isEmptyBlock(above) || level.getBlockState(above).canBeReplaced()) {
                level.setBlock(above, SeedExtractionRegistry.ABNORMAL_GRASS.get().defaultBlockState(), Block.UPDATE_ALL);
                level.setBlock(pos, state.setValue(GROWTH_STAGE, 0), Block.UPDATE_ALL);

                // Particles and sound
                level.playSound(null, above, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                double x = above.getX() + 0.5;
                double y = above.getY() + 0.5;
                double z = above.getZ() + 0.5;
                for (int i = 0; i < 8; i++) {
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                            x + random.nextGaussian() * 0.4,
                            y + random.nextDouble() * 0.4,
                            z + random.nextGaussian() * 0.4,
                            1, 0, 0, 0, 0.01);
                }
            }
        }
    }
}
