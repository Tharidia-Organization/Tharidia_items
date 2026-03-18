package com.THproject.tharidia_things.block.seed_extraction;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class DriedCompressedLeavesBlock extends Block {

    public static final IntegerProperty HITS_NEEDED = IntegerProperty.create("hits_needed", 4, 6);
    public static final IntegerProperty STOMP_COUNT = IntegerProperty.create("stomp_count", 0, 5);

    public DriedCompressedLeavesBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(HITS_NEEDED, 4)
                .setValue(STOMP_COUNT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HITS_NEEDED, STOMP_COUNT);
    }
}
