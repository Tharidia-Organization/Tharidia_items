package com.THproject.tharidia_things.block.ore_chunks;

import com.THproject.tharidia_things.sounds.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;

public class OnChunkAttacked {
    public static void attackChunk(Level level, BlockPos pos, Player player, ItemStack drop) {
        if (!level.isClientSide) {
            if (player.getMainHandItem()
                    .is(ItemTags.create(ResourceLocation.fromNamespaceAndPath("tharidiathings", "crusher_hammer")))) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof IronChunkBlockEntity ironChunkBlockEntity) {
                    level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), ModSounds.CRUSHER_HAMMER_USE.get(),
                            SoundSource.AMBIENT);
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(
                                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()),
                                pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 20, 0, 0, 0, 0.05);
                    }
                    ironChunkBlockEntity.hit();
                    player.getMainHandItem().hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                    if (ironChunkBlockEntity.getHit() >= ironChunkBlockEntity.getMaxHit()) {
                        level.destroyBlock(pos, false);
                        BaseEntityBlock.popResource(level, pos, drop);
                    }
                }
            }
        }
    }
}
