package com.THproject.tharidia_things.block;

import com.THproject.tharidia_things.block.entity.CookTableBlockEntity;
import com.THproject.tharidia_things.cook.CookRecipe;
import com.THproject.tharidia_things.cook.CookRecipeRegistry;
import com.THproject.tharidia_things.network.OpenCookRecipePacket;
import com.THproject.tharidia_things.trade.MarketBridge;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Invisible dummy block for the Cook Table multiblock.
 * Provides collision and forwards destruction to the master block.
 */
public class CookTableDummyBlock extends Block {

    public static final MapCodec<CookTableDummyBlock> CODEC = simpleCodec(CookTableDummyBlock::new);

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    // localX index (0-4); master is at localX=2
    public static final IntegerProperty OFFSET_X = IntegerProperty.create("offset_x", 0, 4);

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public CookTableDummyBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(OFFSET_X, 0));
    }

    public CookTableDummyBlock() {
        this(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.5F, 3.0F)
                .noOcclusion()
                .noLootTable());
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OFFSET_X);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    // offset_x values:  0=tagliere(paused)  1=ricettario  2=master(vassoio)  3=cassa  4=spezie(paused)
    private static final int OFFSET_RICETTARIO = 1;
    private static final int OFFSET_CASSA      = 3;

    /**
     * Right-click WITH item: any player can deposit currency into the cassa.
     * 10% tax is destroyed; the remaining 90% is stored in the cassa container.
     * The tax amount is recorded via MarketBridge for admin tracking.
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (hand != InteractionHand.MAIN_HAND) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        int offset = state.getValue(OFFSET_X);
        if (offset != OFFSET_CASSA) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!"numismaticoverhaul".equals(itemId.getNamespace())) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (level.isClientSide) return ItemInteractionResult.SUCCESS;

        BlockPos masterPos = findMaster(level, pos, state);
        if (masterPos == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (!(level.getBlockEntity(masterPos) instanceof CookTableBlockEntity be)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(player instanceof ServerPlayer sp)) return ItemInteractionResult.SUCCESS;

        int totalCount = stack.getCount();
        int taxCount = (int)(totalCount * 0.1); // floor 10%
        int depositCount = totalCount - taxCount;

        SimpleContainer cassa = be.getCassaContainer();
        int availableSpace = getAvailableSpace(cassa, stack);
        if (availableSpace < depositCount) {
            sp.sendSystemMessage(Component.literal("§cLa cassa non ha spazio sufficiente."));
            return ItemInteractionResult.FAIL;
        }

        // Save item type before clearing the hand stack
        ItemStack template = stack.copyWithCount(1);

        // Remove all currency from player's hand
        stack.setCount(0);

        // Deposit 90% into cassa
        cassa.addItem(template.copyWithCount(depositCount));
        be.setChanged();

        // Record tax transaction for admin tracking
        if (taxCount > 0 && level.getServer() != null) {
            ItemStack taxStack = template.copyWithCount(taxCount);
            MarketBridge.sendTransaction(
                    level.getServer(),
                    sp.getUUID(), new UUID(0, 0),
                    sp.getName().getString(), "TASSA_CASSA",
                    List.of(taxStack), List.of(),
                    false
            );
        }

        // Feedback to player
        String moneyName = template.getHoverName().getString();
        if (taxCount > 0) {
            sp.sendSystemMessage(Component.literal(
                    "§aDepositato §f" + depositCount + "x " + moneyName +
                    " §7(tassa: §c" + taxCount + "§7)"));
        } else {
            sp.sendSystemMessage(Component.literal(
                    "§aDepositato §f" + depositCount + "x " + moneyName));
        }

        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8f, 1.4f);
        return ItemInteractionResult.SUCCESS;
    }

    /** Returns how many more items of the given type the container can accept. */
    private static int getAvailableSpace(SimpleContainer container, ItemStack template) {
        int space = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack slot = container.getItem(i);
            if (slot.isEmpty()) {
                space += template.getMaxStackSize();
            } else if (ItemStack.isSameItemSameComponents(slot, template)) {
                space += Math.max(0, template.getMaxStackSize() - slot.getCount());
            }
        }
        return space;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        int offset = state.getValue(OFFSET_X);

        if (offset == OFFSET_CASSA) {
            if (!player.getTags().contains("cook")) {
                player.sendSystemMessage(Component.literal(
                        "§cSolo il cuoco può aprire la cassa. §7Tieni valuta in mano per depositare."));
                return InteractionResult.FAIL;
            }
            BlockPos masterPos = findMaster(level, pos, state);
            if (masterPos == null) return InteractionResult.PASS;
            if (level.getBlockEntity(masterPos) instanceof CookTableBlockEntity be && player instanceof ServerPlayer sp) {
                level.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.5f, 1.0f);
                sp.openMenu(new SimpleMenuProvider(
                        (id, inv, p) -> new ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x1,
                                id, inv, be.getCassaContainer(), 1),
                        Component.literal("§6Cassa del cuoco")
                ));
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        if (offset == OFFSET_RICETTARIO) {
            if (!player.getTags().contains("cook")) {
                player.sendSystemMessage(Component.literal("§cSolo un cuoco può usare questo blocco."));
                return InteractionResult.FAIL;
            }
            BlockPos masterPos = findMaster(level, pos, state);
            if (masterPos == null) return InteractionResult.PASS;
            if (level.getBlockEntity(masterPos) instanceof CookTableBlockEntity be && player instanceof ServerPlayer sp) {
                level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.6f, 1.0f);
                List<CookRecipe> available = CookRecipeRegistry.discover(
                        level.getServer().getRecipeManager(), level.registryAccess());
                OpenCookRecipePacket.sendToPlayer(sp, masterPos,
                        available, be.getActiveRecipeId(), be.getTimerTicks(), be.getTotalTimerTicks());
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            BlockPos masterPos = findMaster(level, pos, state);
            if (masterPos != null) {
                level.destroyBlock(masterPos, true);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    private BlockPos findMaster(Level level, BlockPos dummyPos, BlockState dummyState) {
        int offsetX = dummyState.getValue(OFFSET_X);
        Direction facing = dummyState.getValue(FACING);
        BlockPos masterPos = CookTableBlock.getMasterPosFromDummy(dummyPos, offsetX, facing);
        BlockState masterState = level.getBlockState(masterPos);
        return masterState.getBlock() instanceof CookTableBlock ? masterPos : null;
    }

    @Nullable
    public static BlockPos getMasterPos(net.minecraft.world.level.LevelAccessor level, BlockPos dummyPos) {
        BlockState state = level.getBlockState(dummyPos);
        if (!(state.getBlock() instanceof CookTableDummyBlock)) return null;
        int offsetX = state.getValue(OFFSET_X);
        Direction facing = state.getValue(FACING);
        BlockPos masterPos = CookTableBlock.getMasterPosFromDummy(dummyPos, offsetX, facing);
        BlockState masterState = level.getBlockState(masterPos);
        return masterState.getBlock() instanceof CookTableBlock ? masterPos : null;
    }
}
