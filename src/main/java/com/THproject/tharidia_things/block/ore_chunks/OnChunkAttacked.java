package com.THproject.tharidia_things.block.ore_chunks;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class OnChunkAttacked {
    public static void attackChunk(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide) {
            if (player.getMainHandItem()
                    .is(ItemTags.create(ResourceLocation.fromNamespaceAndPath("tharidiathings", "crusher_hammer")))) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof IronChunkBlockEntity ironChunkBlockEntity) {
                    ironChunkBlockEntity.hit();
                    player.getMainHandItem().hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                    if (ironChunkBlockEntity.getHit() >= ironChunkBlockEntity.getMaxHit()) {
                        level.destroyBlock(pos, false);
                        BaseEntityBlock.popResource(level, pos, Items.RAW_IRON.getDefaultInstance());
                    }
                }
            }
        }
    }
}
