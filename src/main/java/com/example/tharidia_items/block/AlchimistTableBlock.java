package com.example.tharidia_items.block;

import com.example.tharidia_items.screen.AlchimistTableScreenHandler;
import com.example.tharidia_items.block.entity.AlchimistTableBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class AlchimistTableBlock extends BlockWithEntity {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final EnumProperty<TablePart> PART = EnumProperty.of("part", TablePart.class);

    public AlchimistTableBlock() {
        super(FabricBlockSettings.create().strength(2.0f).nonOpaque());
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(PART, TablePart.CENTER));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        // Renderizza il modello GeckoLib SOLO per la parte centrale; le parti aggiuntive sono invisibili (solo collisione)
        return state.get(PART) == TablePart.CENTER ? BlockRenderType.ENTITYBLOCK_ANIMATED : BlockRenderType.INVISIBLE;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        // Manteniamo la direzione del giocatore (flip 180° già applicato in modifica precedente)
        Direction facing = ctx.getHorizontalPlayerFacing();
        return this.getDefaultState().with(FACING, facing).with(PART, TablePart.CENTER);
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        // Assicura spazio libero per i segmenti: sinistro, destro, e l'intera fila anteriore (top-left, top, top-right)
        if (state.get(PART) == TablePart.CENTER) {
            Direction facing = state.get(FACING);
            Direction left = getLeftDir(facing);
            Direction right = getRightDir(facing);

            BlockPos leftPos = pos.offset(left);
            BlockPos rightPos = pos.offset(right);
            BlockPos topPos = pos.offset(facing);
            BlockPos topLeftPos = topPos.offset(left);
            BlockPos topRightPos = topPos.offset(right);

            return world.getBlockState(leftPos).isAir()
                    && world.getBlockState(rightPos).isAir()
                    && world.getBlockState(topPos).isAir()
                    && world.getBlockState(topLeftPos).isAir()
                    && world.getBlockState(topRightPos).isAir();
        }
        return super.canPlaceAt(state, world, pos);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (world.isClient) return;
        if (state.get(PART) != TablePart.CENTER) return;

        Direction facing = state.get(FACING);
        Direction left = getLeftDir(facing);
        Direction right = getRightDir(facing);

        BlockPos leftPos = pos.offset(left);
        BlockPos rightPos = pos.offset(right);

        BlockPos topPos = pos.offset(facing);
        BlockPos topLeftPos = topPos.offset(left);
        BlockPos topRightPos = topPos.offset(right);

        // Se qualcosa occupa lo spazio, annulla il piazzamento e restituisci l'item
        if (!world.getBlockState(leftPos).isAir()
                || !world.getBlockState(rightPos).isAir()
                || !world.getBlockState(topPos).isAir()
                || !world.getBlockState(topLeftPos).isAir()
                || !world.getBlockState(topRightPos).isAir()) {
            world.breakBlock(pos, true);
            return;
        }

        world.setBlockState(leftPos, state.with(PART, TablePart.LEFT));
        world.setBlockState(rightPos, state.with(PART, TablePart.RIGHT));
        world.setBlockState(topLeftPos, state.with(PART, TablePart.TOP_LEFT));
        world.setBlockState(topPos, state.with(PART, TablePart.TOP));
        world.setBlockState(topRightPos, state.with(PART, TablePart.TOP_RIGHT));
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient) {
            Direction facing = state.get(FACING);
            Direction left = getLeftDir(facing);
            Direction right = getRightDir(facing);

            BlockPos centerPos;
            if (state.get(PART) == TablePart.LEFT) {
                centerPos = pos.offset(right);
            } else if (state.get(PART) == TablePart.RIGHT) {
                centerPos = pos.offset(left);
            } else if (state.get(PART) == TablePart.TOP) {
                centerPos = pos.offset(facing.getOpposite());
            } else if (state.get(PART) == TablePart.TOP_LEFT) {
                centerPos = pos.offset(facing.getOpposite()).offset(right);
            } else if (state.get(PART) == TablePart.TOP_RIGHT) {
                centerPos = pos.offset(facing.getOpposite()).offset(left);
            } else {
                centerPos = pos;
            }

            // Calcola tutte le posizioni delle parti rispetto al centro
            BlockPos leftPos = centerPos.offset(left);
            BlockPos rightPos = centerPos.offset(right);
            BlockPos topPos = centerPos.offset(facing);
            BlockPos topLeftPos = topPos.offset(left);
            BlockPos topRightPos = topPos.offset(right);

            // Se stai rompendo una parte non-centrale, rompi la parte centrale per droppare correttamente
            if (!pos.equals(centerPos) && world.getBlockState(centerPos).isOf(this)) {
                world.breakBlock(centerPos, true, player);
            }
            // Rimuovi tutte le parti senza droppare aggiuntivo
            if (world.getBlockState(leftPos).isOf(this)) {
                world.breakBlock(leftPos, false, player);
            }
            if (world.getBlockState(rightPos).isOf(this)) {
                world.breakBlock(rightPos, false, player);
            }
            if (world.getBlockState(topLeftPos).isOf(this)) {
                world.breakBlock(topLeftPos, false, player);
            }
            if (world.getBlockState(topPos).isOf(this)) {
                world.breakBlock(topPos, false, player);
            }
            if (world.getBlockState(topRightPos).isOf(this)) {
                world.breakBlock(topRightPos, false, player);
            }
        }
        super.onBreak(world, pos, state, player);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        BlockPos centerPos = getCenterPos(state, pos);
        NamedScreenHandlerFactory screenHandlerFactory = new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, p) -> new AlchimistTableScreenHandler(syncId, inventory, ScreenHandlerContext.create(world, centerPos)),
                Text.literal("Alchemist Table")
        );
        player.openHandledScreen(screenHandlerFactory);
        return ActionResult.CONSUME;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        // Forma piena per ogni parte per garantire selezione semplice
        return VoxelShapes.fullCube();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        // Collisione piena per simulare fisicamente 6 blocchi
        return VoxelShapes.fullCube();
    }

    @Override
    public VoxelShape getCullingShape(BlockState state, BlockView world, BlockPos pos) {
        // Nessuna forma di culling: non nascondere le facce adiacenti
        return VoxelShapes.empty();
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        // Crea la BlockEntity SOLO per la parte centrale, per evitare renderizzazioni multiple
        return state.get(PART) == TablePart.CENTER ? new AlchimistTableBlockEntity(pos, state) : null;
    }

    private static Direction getLeftDir(Direction facing) {
        return facing.rotateYCounterclockwise();
    }

    private static Direction getRightDir(Direction facing) {
        return facing.rotateYClockwise();
    }

    private static BlockPos getCenterPos(BlockState state, BlockPos pos) {
        TablePart part = state.get(PART);
        Direction facing = state.get(FACING);
        Direction left = getLeftDir(facing);
        Direction right = getRightDir(facing);
        switch (part) {
            case LEFT:
                return pos.offset(right);
            case RIGHT:
                return pos.offset(left);
            case TOP:
                return pos.offset(facing.getOpposite());
            case TOP_LEFT:
                return pos.offset(facing.getOpposite()).offset(right);
            case TOP_RIGHT:
                return pos.offset(facing.getOpposite()).offset(left);
            case CENTER:
            default:
                return pos;
        }
    }

    public enum TablePart implements StringIdentifiable {
        LEFT("left"), CENTER("center"), RIGHT("right"),
        TOP_LEFT("top_left"), TOP("top"), TOP_RIGHT("top_right");
        private final String name;
        TablePart(String name) { this.name = name; }
        @Override public String asString() { return this.name; }
        @Override public String toString() { return this.name; }
    }
}