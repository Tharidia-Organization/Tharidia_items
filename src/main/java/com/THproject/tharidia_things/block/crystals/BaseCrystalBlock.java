package com.THproject.tharidia_things.block.crystals;

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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class BaseCrystalBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 14, 14);

    public BaseCrystalBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(CrystalsRegistry.STAGE, 0));
    }

    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide) {
            if (isCorrectTool(player.getMainHandItem())) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof BaseCrystalBlockEntity crystalBlockEntity) {
                    crystalBlockEntity.hit();
                    playHammerSound(level, pos);
                    spawnParticle(level, pos);
                    destroyHandItem(player, 1);
                    if (crystalBlockEntity.getHit() >= crystalBlockEntity.getMaxHit()) {
                        playChunkBreakSound(level, pos);
                        destroyAndPop(level, pos, crystalBlockEntity.getDrop());
                    }
                }
            }
        }
        super.attack(state, level, pos, player);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CrystalsRegistry.STAGE);
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
            // Spawn crystal-themed particles (amethyst block particles)
            serverLevel.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_BLOCK.defaultBlockState()),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 15, 0.3, 0.3, 0.3, 0.05);

            // Sparkle effect
            serverLevel.sendParticles(
                    ParticleTypes.END_ROD,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5, 0.2, 0.2, 0.2, 0.02);
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
