package com.THproject.tharidia_things.block.seed_extraction;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class CompressedLeavesBlockItem extends BlockItem {

    public CompressedLeavesBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // The default raytrace for BlockItem ignores fluids, so context.getClickedPos()
        // points to the solid block BEHIND the water. We need our own fluid-aware raytrace.
        Player player = context.getPlayer();
        Level level = context.getLevel();

        if (player != null) {
            BlockHitResult fluidHit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
            if (fluidHit.getType() == HitResult.Type.BLOCK) {
                BlockPos waterPos = fluidHit.getBlockPos();
                FluidState fluidState = level.getFluidState(waterPos);
                if (fluidState.isSource() && fluidState.getType() == Fluids.WATER) {
                    return handleWetting(level, player, context.getItemInHand(), waterPos);
                }
            }
        }

        // Not looking at water — place block normally
        return super.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // Called when the default raytrace missed all solid blocks (e.g. only water ahead).
        // Do a fluid-aware raytrace to detect water.
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult fluidHit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (fluidHit.getType() == HitResult.Type.BLOCK) {
            BlockPos waterPos = fluidHit.getBlockPos();
            FluidState fluidState = level.getFluidState(waterPos);
            if (fluidState.isSource() && fluidState.getType() == Fluids.WATER) {
                handleWetting(level, player, stack, waterPos);
                return InteractionResultHolder.success(player.getItemInHand(hand));
            }
        }
        return super.use(level, player, hand);
    }

    private InteractionResult handleWetting(Level level, Player player, ItemStack stack, BlockPos waterPos) {
        if (!level.isClientSide()) {
            stack.shrink(1);

            ItemStack wetLeaves = new ItemStack(SeedExtractionRegistry.WET_COMPRESSED_LEAVES_ITEM.get());
            if (!player.getInventory().add(wetLeaves)) {
                player.drop(wetLeaves, false);
            }

            level.playSound(null, waterPos, SoundEvents.PLAYER_SPLASH, SoundSource.BLOCKS, 1.0F, 1.0F);
        }

        if (level.isClientSide()) {
            double x = waterPos.getX() + 0.5;
            double y = waterPos.getY() + 1.0;
            double z = waterPos.getZ() + 0.5;
            for (int i = 0; i < 6; i++) {
                level.addParticle(ParticleTypes.SPLASH,
                        x + level.random.nextGaussian() * 0.3,
                        y + level.random.nextDouble() * 0.3,
                        z + level.random.nextGaussian() * 0.3,
                        0, 0, 0);
            }
        }

        return InteractionResult.SUCCESS;
    }
}
