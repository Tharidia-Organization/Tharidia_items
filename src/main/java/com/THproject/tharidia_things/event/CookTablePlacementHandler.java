package com.THproject.tharidia_things.event;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.CookTableBlock;
import com.THproject.tharidia_things.block.CookTableDummyBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class CookTablePlacementHandler {

    private static final String COOK_TAG = "cook";

    // offset_x values for the outermost dummy slots (tagliere=0, spezie=4)
    private static final int OFFSET_TAGLIERE = 0;
    private static final int OFFSET_SPEZIE   = 4;

    // X_OFFSET mirrors the private constant in CookTableBlock (master is at localX=2)
    private static final int X_OFFSET = -2;

    private static final TagKey<Block> COOK_TABLE_ADJACENT = BlockTags.create(
            ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "cook_table_adjacent")
    );

    // -------------------------------------------------------------------------
    // Placement
    // -------------------------------------------------------------------------

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel)) return;
        if (!(event.getEntity() instanceof Player player)) return;

        Block placedBlock = event.getPlacedBlock().getBlock();
        BlockPos pos = event.getPos();

        // Cook Table can only be placed by a cook
        if (placedBlock instanceof CookTableBlock) {
            if (!player.getTags().contains(COOK_TAG)) {
                event.setCanceled(true);
                player.sendSystemMessage(Component.literal(
                        "§cSolo un cuoco può posizionare il tavolo da cucina."
                ));
            }
            return;
        }

        // Blocks in cook_table_adjacent: must be adjacent to the cook table or to a connected station
        if (event.getPlacedBlock().is(COOK_TABLE_ADJACENT)) {
            if (!isValidChainPlacement(event.getLevel(), pos)) {
                event.setCanceled(true);
                player.sendSystemMessage(Component.literal(
                        "§cQuesto blocco può essere posizionato solo adiacente al tavolo da cucina o a una stazione già collegata."
                ));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Break prevention
    // -------------------------------------------------------------------------

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel)) return;

        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        LevelAccessor level = event.getLevel();
        Player player = event.getPlayer();

        // A station that has other stations depending on it (bridge) cannot be broken first
        if (state.is(COOK_TABLE_ADJACENT)) {
            if (hasDependentStations(level, pos)) {
                event.setCanceled(true);
                player.sendSystemMessage(Component.literal(
                        "§cRimuovi prima le stazioni più esterne della catena."
                ));
            }
            return;
        }

        // Cook table (master or dummy) cannot be broken while any station is connected
        if (state.getBlock() instanceof CookTableBlock || state.getBlock() instanceof CookTableDummyBlock) {
            if (hasCookTableConnectedStations(level, pos, state)) {
                event.setCanceled(true);
                player.sendSystemMessage(Component.literal(
                        "§cRimuovi prima tutte le stazioni collegate al tavolo da cucina."
                ));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Placement validation
    // -------------------------------------------------------------------------

    /**
     * A station at {@code pos} is valid if:
     * <ol>
     *   <li>It is directly adjacent (outward along the multiblock axis) to an outermost dummy
     *       (offset_x=0 or offset_x=4), OR</li>
     *   <li>It is adjacent to another station that is itself connected to the cook table chain.</li>
     * </ol>
     */
    private static boolean isValidChainPlacement(LevelAccessor level, BlockPos pos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);

            // Case 1: direct adjacency to an outermost dummy, extending the multiblock axis outward
            if (neighborState.getBlock() instanceof CookTableDummyBlock) {
                int offsetX = neighborState.getValue(CookTableDummyBlock.OFFSET_X);
                if (offsetX != OFFSET_TAGLIERE && offsetX != OFFSET_SPEZIE) continue;

                Direction cookFacing = neighborState.getValue(CookTableDummyBlock.FACING);
                Direction fromDummyToStation = dir.getOpposite();
                if (fromDummyToStation == cookFacing.getClockWise() ||
                    fromDummyToStation == cookFacing.getCounterClockWise()) {
                    return true;
                }
                continue;
            }

            // Case 2: adjacent to a station that is already connected to the cook table
            if (neighborState.is(COOK_TABLE_ADJACENT) && isConnectedToCookTable(level, neighborPos, null)) {
                return true;
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Break validation helpers
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if at least one station adjacent to {@code pos} would lose its
     * connection to the cook table if {@code pos} were removed (i.e. {@code pos} is a bridge).
     */
    private static boolean hasDependentStations(LevelAccessor level, BlockPos pos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = pos.relative(dir);
            if (level.getBlockState(neighbor).is(COOK_TABLE_ADJACENT)) {
                if (!isConnectedToCookTable(level, neighbor, pos)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if the cook table that contains the block at {@code pos}
     * has at least one station connected to either outermost dummy.
     */
    private static boolean hasCookTableConnectedStations(LevelAccessor level, BlockPos pos, BlockState state) {
        BlockPos masterPos;
        Direction facing;

        if (state.getBlock() instanceof CookTableBlock) {
            masterPos = pos;
            facing = state.getValue(CookTableBlock.FACING);
        } else if (state.getBlock() instanceof CookTableDummyBlock) {
            masterPos = CookTableDummyBlock.getMasterPos(level, pos);
            if (masterPos == null) return false;
            facing = state.getValue(CookTableDummyBlock.FACING);
        } else {
            return false;
        }

        // Both outermost dummies; if any of their horizontal neighbours is a station → blocked
        for (int offsetX : new int[]{OFFSET_TAGLIERE, OFFSET_SPEZIE}) {
            BlockPos dummyPos = computeDummyPos(masterPos, offsetX, facing);
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                if (level.getBlockState(dummyPos.relative(dir)).is(COOK_TABLE_ADJACENT)) {
                    return true;
                }
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // BFS connectivity
    // -------------------------------------------------------------------------

    /**
     * BFS from {@code startPos} through neighbouring station blocks.
     * Returns {@code true} if an outermost cook table dummy (offset_x=0 or 4) is reachable.
     *
     * @param excludePos if non-null, this position is treated as a wall (used to simulate removal)
     */
    private static boolean isConnectedToCookTable(LevelAccessor level, BlockPos startPos, BlockPos excludePos) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();

        visited.add(startPos);
        if (excludePos != null) visited.add(excludePos);
        queue.add(startPos);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos neighbor = current.relative(dir);
                if (visited.contains(neighbor)) continue;

                BlockState neighborState = level.getBlockState(neighbor);

                // Reached an outermost dummy → the chain is connected
                if (neighborState.getBlock() instanceof CookTableDummyBlock) {
                    int offsetX = neighborState.getValue(CookTableDummyBlock.OFFSET_X);
                    if (offsetX == OFFSET_TAGLIERE || offsetX == OFFSET_SPEZIE) {
                        return true;
                    }
                    continue;
                }

                // Traverse through other stations in the tag
                if (neighborState.is(COOK_TABLE_ADJACENT)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Geometry helpers
    // -------------------------------------------------------------------------

    /**
     * Computes the world position of a cook table dummy given the master position.
     * Mirrors {@code CookTableBlock.getOffsetPos} for localY=0, localZ=0.
     */
    private static BlockPos computeDummyPos(BlockPos masterPos, int offsetX, Direction facing) {
        int centeredX = offsetX + X_OFFSET;
        int worldX, worldZ;
        switch (facing) {
            case NORTH -> { worldX = -centeredX; worldZ = 0; }
            case SOUTH -> { worldX =  centeredX; worldZ = 0; }
            case EAST  -> { worldX = 0;           worldZ = -centeredX; }
            case WEST  -> { worldX = 0;           worldZ =  centeredX; }
            default    -> { worldX =  centeredX; worldZ = 0; }
        }
        return masterPos.offset(worldX, 0, worldZ);
    }
}
