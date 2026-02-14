package com.THproject.tharidia_things.item;

import com.THproject.tharidia_things.block.seed_extraction.DriedCompressedLeavesBlock;
import com.THproject.tharidia_things.block.seed_extraction.SeedExtractionRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class ZoccolettaItem extends Item {

    public ZoccolettaItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (!state.is(SeedExtractionRegistry.DRIED_COMPRESSED_LEAVES.get())) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) level;

            int stompCount = state.getValue(DriedCompressedLeavesBlock.STOMP_COUNT);
            int hitsNeeded = state.getValue(DriedCompressedLeavesBlock.HITS_NEEDED);
            int newCount = stompCount + 1;

            // Sound
            serverLevel.playSound(null, pos, SoundEvents.WOOD_STEP, SoundSource.BLOCKS, 1.0F, 0.8F);

            // Block crack particles
            serverLevel.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, state),
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    10, 0.25, 0.1, 0.25, 0.05);

            if (newCount >= hitsNeeded) {
                // Destroy block and drop seeds
                level.destroyBlock(pos, false);
                dropSeeds(serverLevel, pos);
            } else {
                // Increment stomp count
                level.setBlock(pos, state.setValue(DriedCompressedLeavesBlock.STOMP_COUNT, newCount),
                        net.minecraft.world.level.block.Block.UPDATE_ALL);
            }

            // Damage the zoccoletta
            ItemStack stack = context.getItemInHand();
            if (context.getPlayer() != null) {
                stack.hurtAndBreak(1, context.getPlayer(), context.getPlayer().getEquipmentSlotForItem(stack));
            }
        }

        return InteractionResult.SUCCESS;
    }

    private void dropSeeds(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        // 1-2 beetroot seeds (always)
        int beetrootCount = 1 + level.random.nextInt(2);
        spawnItem(level, x, y, z, new ItemStack(Items.BEETROOT_SEEDS, beetrootCount));

        // 50% chance 1 bamboo
        if (level.random.nextFloat() < 0.5F) {
            spawnItem(level, x, y, z, new ItemStack(Items.BAMBOO));
        }

        // 30% chance brown mushroom
        if (level.random.nextFloat() < 0.3F) {
            spawnItem(level, x, y, z, new ItemStack(Items.BROWN_MUSHROOM));
        }

        // 20% chance red mushroom
        if (level.random.nextFloat() < 0.2F) {
            spawnItem(level, x, y, z, new ItemStack(Items.RED_MUSHROOM));
        }
    }

    private void spawnItem(ServerLevel level, double x, double y, double z, ItemStack stack) {
        ItemEntity entity = new ItemEntity(level, x, y, z, stack);
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
    }
}
