package com.THproject.tharidia_things.block.ore_chunks;

import com.THproject.tharidia_things.sounds.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.VoxelShape;

public class Chunks {
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 4);

    public static final VoxelShape SHAPE_STAGE0 = Block.box(4, 0, 4, 12, 8, 12);
    public static final VoxelShape SHAPE_STAGE1 = Block.box(4, 0, 4, 12, 8, 12);
    public static final VoxelShape SHAPE_STAGE2 = Block.box(4, 0, 4, 12, 8, 12);
    public static final VoxelShape SHAPE_STAGE3 = Block.box(4, 0, 4, 12, 8, 12);
    public static final VoxelShape SHAPE_STAGE4 = Block.box(4, 0, 4, 12, 8, 12);

    public static void attackChunk(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide) {
            if (isCorrectTool(player.getMainHandItem())) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof IronChunkBlockEntity ironChunkBlockEntity) {
                    ironChunkBlockEntity.hit();
                    playHammerSound(level, pos);
                    spawnParticle(level, pos);
                    destroyHandItem(player, 1);
                    if (ironChunkBlockEntity.getHit() >= ironChunkBlockEntity.getMaxHit()) {
                        playChunkBreakSound(level, pos);
                        destroyAndPop(level, pos, ironChunkBlockEntity.getDrop());
                    }
                }
            }
        }
    }

    public static void playHammerSound(Level level, BlockPos pos) {
        level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), ModSounds.CRUSHER_HAMMER_USE.get(),
                SoundSource.AMBIENT);
    }

    public static void playChunkBreakSound(Level level, BlockPos pos) {
        level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), ModSounds.CHUNK_BREAK.get(),
                SoundSource.AMBIENT);
    }

    public static void spawnParticle(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()),
                    pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 20, 0, 0, 0, 0.05);

            if (Math.random() < 0.3) {
                serverLevel.sendParticles(
                        ParticleTypes.FLAME,
                        pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 5, 0, 0, 0, 0.05);
            }
        }
    }

    public static boolean isCorrectTool(ItemStack itemstack) {
        return itemstack.is(ItemTags.create(ResourceLocation.fromNamespaceAndPath("tharidiathings", "crusher_hammer")));
    }

    public static void destroyAndPop(Level level, BlockPos pos, ItemStack drop) {
        level.destroyBlock(pos, false);
        BaseEntityBlock.popResource(level, pos, drop);
    }

    public static void destroyHandItem(Player player, int amount) {
        player.getMainHandItem().hurtAndBreak(amount, player, EquipmentSlot.MAINHAND);
    }
}
