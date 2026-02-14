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

public class WetCompressedLeavesBlock extends Block {

    public static final IntegerProperty DRY_PROGRESS = IntegerProperty.create("dry_progress", 0, 13);

    public WetCompressedLeavesBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(DRY_PROGRESS, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DRY_PROGRESS);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.canSeeSky(pos.above())) {
            return;
        }

        int progress = state.getValue(DRY_PROGRESS);
        if (progress < 13) {
            level.setBlock(pos, state.setValue(DRY_PROGRESS, progress + 1), Block.UPDATE_ALL);
        } else {
            // Fully dried — transform into dried compressed leaves with random hits_needed (4-6)
            int hitsNeeded = 4 + random.nextInt(3); // 4, 5, or 6
            BlockState driedState = SeedExtractionRegistry.DRIED_COMPRESSED_LEAVES.get().defaultBlockState()
                    .setValue(DriedCompressedLeavesBlock.HITS_NEEDED, hitsNeeded)
                    .setValue(DriedCompressedLeavesBlock.STOMP_COUNT, 0);
            level.setBlock(pos, driedState, Block.UPDATE_ALL);

            // Drying sound — dry grass crackle
            level.playSound(null, pos, SoundEvents.GRASS_BREAK, SoundSource.BLOCKS, 1.0F, 0.6F);

            // Cloud particles on transformation
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 1.0;
            double z = pos.getZ() + 0.5;
            for (int i = 0; i < 8; i++) {
                level.sendParticles(ParticleTypes.CLOUD,
                        x + random.nextGaussian() * 0.3,
                        y + random.nextDouble() * 0.3,
                        z + random.nextGaussian() * 0.3,
                        1, 0, 0, 0, 0.01);
            }
        }
    }
}
