package com.THproject.tharidia_things.block.herbalist.pot;

import com.THproject.tharidia_things.TharidiaThings;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PotBlock extends BaseEntityBlock {
    public static final MapCodec<PotBlock> CODEC = simpleCodec(PotBlock::new);

    public PotBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide)
            return ItemInteractionResult.SUCCESS;

        PotBlockEntity pot = (PotBlockEntity) level.getBlockEntity(pos);

        if (!player.isShiftKeyDown()) {
            if (stack.is(ItemTags.create(ResourceLocation.fromNamespaceAndPath(
                    TharidiaThings.MODID,
                    "herbalist_pot_plants")))) {
                if (!pot.hasPlant() && pot.hasDirt() && pot.isFarmed()) {
                    pot.setPlant(stack.copyWithCount(1));
                    level.playSound(null, pos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS);
                    stack.shrink(1);
                }
            }
            if (stack.getItem() == Items.DIRT) {
                if (pot.setDirt()) {
                    level.playSound(null, pos, SoundEvents.GRASS_BREAK, SoundSource.BLOCKS);
                    stack.shrink(1);
                }
            } else if (stack.is(ItemTags.HOES)) {
                if (pot.setFarmed()) {
                    level.playSound(null, pos, SoundEvents.HOE_TILL, SoundSource.BLOCKS);
                    stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                }
            }
        } else {
            if (pot.hasPlant() && player.hasPermissions(4)) {
                ItemStack plant = pot.getPlant();
                pot.removePlant();
                if (!player.getInventory().add(plant)) {
                    player.drop(stack, false);
                }
            } else if (pot.removeDirt()) {
                level.playSound(null, pos, SoundEvents.GRASS_BREAK, SoundSource.BLOCKS);
                if (!player.getInventory().add(new ItemStack(Items.DIRT)))
                    player.drop(new ItemStack(Items.DIRT), false);
            }
        }

        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        PotBlockEntity pot = (PotBlockEntity) level.getBlockEntity(pos);

        if (pot.hasDirt())
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Items.DIRT));
        if (pot.hasPlant())
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), pot.getPlant());

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PotBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Block.box(0, 0, 0, 16, 8, 16);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return this.codec();
    }
}
