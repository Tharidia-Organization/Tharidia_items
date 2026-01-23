package com.THproject.tharidia_things.block.ore_chunks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ChunkStage {
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 4);

    public static final VoxelShape SHAPE_STAGE0 = Block.box(4, 0, 4, 12, 8, 12);
    public static final VoxelShape SHAPE_STAGE1 = Block.box(4, 0, 4, 12, 8, 12);
    public static final VoxelShape SHAPE_STAGE2 = Block.box(4, 0, 4, 12, 8, 12);
    public static final VoxelShape SHAPE_STAGE3 = Block.box(4, 0, 4, 12, 8, 12);
    public static final VoxelShape SHAPE_STAGE4 = Block.box(4, 0, 4, 12, 8, 12);
}
