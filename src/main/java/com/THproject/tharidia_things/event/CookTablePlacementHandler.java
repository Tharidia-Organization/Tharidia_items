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
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

public class CookTablePlacementHandler {

    private static final String COOK_TAG = "cook";

    private static final TagKey<Block> COOK_TABLE_ADJACENT = BlockTags.create(
            ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "cook_table_adjacent")
    );

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

        // Blocks in the cook_table_adjacent tag can only be placed next to a cook table
        if (event.getPlacedBlock().is(COOK_TABLE_ADJACENT)) {
            if (!isAdjacentToCookTable(event.getLevel(), pos)) {
                event.setCanceled(true);
                player.sendSystemMessage(Component.literal(
                        "§cQuesto blocco può essere posizionato solo adiacente al tavolo da cucina."
                ));
            }
        }
    }

    // offset_x values for the outermost dummy slots (tagliere=0, spezie=4)
    private static final int OFFSET_TAGLIERE = 0;
    private static final int OFFSET_SPEZIE   = 4;

    /**
     * A station block is valid only if it is placed directly to the LEFT or RIGHT of one of
     * the two outermost dummy blocks (offset_x=0 or offset_x=4) — i.e. it extends the
     * multiblock line outward from one of its ends.
     *
     * Placement next to the master block or any inner dummy is rejected.
     */
    private static boolean isAdjacentToCookTable(net.minecraft.world.level.LevelAccessor level, BlockPos pos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            net.minecraft.world.level.block.state.BlockState neighborState =
                    level.getBlockState(pos.relative(dir));

            if (!(neighborState.getBlock() instanceof CookTableDummyBlock)) continue;

            int offsetX = neighborState.getValue(CookTableDummyBlock.OFFSET_X);
            if (offsetX != OFFSET_TAGLIERE && offsetX != OFFSET_SPEZIE) continue;

            Direction cookFacing = neighborState.getValue(CookTableDummyBlock.FACING);

            // dir = direction from station to the dummy.
            // dir.getOpposite() = direction FROM dummy TOWARD the station.
            // Valid only if the station extends outward along the multiblock axis.
            Direction fromDummyToStation = dir.getOpposite();
            if (fromDummyToStation == cookFacing.getClockWise() ||
                fromDummyToStation == cookFacing.getCounterClockWise()) {
                return true;
            }
        }
        return false;
    }
}
