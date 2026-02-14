package com.THproject.tharidia_things.block.washer;

import java.util.List;

import com.THproject.tharidia_things.block.washer.sieve.SieveBlockEntity;
import com.THproject.tharidia_things.block.washer.sink.SinkBlockEntity;
import com.THproject.tharidia_things.block.washer.tank.TankBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class MultiblockGetter {
    public static void fromSieve(Level level, SieveBlockEntity sieve, BlockPos pos) {
        Direction sieveDirection = getDirection(sieve);
        switch (sieveDirection) {
            case NORTH:
                sieve.sink = getSink(level, pos.north(-2), sieveDirection);
                sieve.tank1 = getTank(level, pos.north(1).west(1), sieveDirection.getCounterClockWise());
                sieve.tank2 = getTank(level, pos.north(2), sieveDirection);
                sieve.tank3 = getTank(level, pos.north(1).east(1), sieveDirection.getClockWise());
                break;
            case EAST:
                sieve.sink = getSink(level, pos.east(-2), sieveDirection);
                sieve.tank1 = getTank(level, pos.east(1).north(1), sieveDirection.getCounterClockWise());
                sieve.tank2 = getTank(level, pos.east(2), sieveDirection);
                sieve.tank3 = getTank(level, pos.east(1).south(1), sieveDirection.getClockWise());
                break;
            case SOUTH:
                sieve.sink = getSink(level, pos.south(-2), sieveDirection);
                sieve.tank1 = getTank(level, pos.south(1).east(1), sieveDirection.getCounterClockWise());
                sieve.tank2 = getTank(level, pos.south(2), sieveDirection);
                sieve.tank3 = getTank(level, pos.south(1).west(1), sieveDirection.getClockWise());
                break;
            case WEST:
                sieve.sink = getSink(level, pos.west(-2), sieveDirection);
                sieve.tank1 = getTank(level, pos.west(1).south(1), sieveDirection.getCounterClockWise());
                sieve.tank2 = getTank(level, pos.west(2), sieveDirection);
                sieve.tank3 = getTank(level, pos.west(1).north(1), sieveDirection.getClockWise());
                break;

            default:
                break;
        }
    }

    public static void fromSink(Level level, SinkBlockEntity sink, BlockPos pos) {
        Direction sinkDirection = getDirection(sink);
        switch (sinkDirection) {
            case NORTH:
                sink.sieve = getSieve(level, pos.north(2), sinkDirection);
                sink.tank1 = getTank(level, pos.north(3).west(1), sinkDirection.getCounterClockWise());
                sink.tank2 = getTank(level, pos.north(4), sinkDirection);
                sink.tank3 = getTank(level, pos.north(3).east(1), sinkDirection.getClockWise());
                break;
            case EAST:
                sink.sieve = getSieve(level, pos.east(2), sinkDirection);
                sink.tank1 = getTank(level, pos.east(3).north(1), sinkDirection.getCounterClockWise());
                sink.tank2 = getTank(level, pos.east(4), sinkDirection);
                sink.tank3 = getTank(level, pos.east(3).south(1), sinkDirection.getClockWise());
                break;
            case SOUTH:
                sink.sieve = getSieve(level, pos.south(2), sinkDirection);
                sink.tank1 = getTank(level, pos.south(3).east(1), sinkDirection.getCounterClockWise());
                sink.tank2 = getTank(level, pos.south(4), sinkDirection);
                sink.tank3 = getTank(level, pos.south(3).west(1), sinkDirection.getClockWise());
                break;
            case WEST:
                sink.sieve = getSieve(level, pos.west(2), sinkDirection);
                sink.tank1 = getTank(level, pos.west(3).south(1), sinkDirection.getCounterClockWise());
                sink.tank2 = getTank(level, pos.west(4), sinkDirection);
                sink.tank3 = getTank(level, pos.west(3).north(1), sinkDirection.getClockWise());
                break;

            default:
                break;
        }
    }

    private static SieveBlockEntity getSieve(Level level, BlockPos pos, Direction direction) {
        if (level.getBlockEntity(pos) instanceof SieveBlockEntity sieve
                && getDirection(sieve) == direction) {
            return sieve;
        }
        return null;
    }

    private static TankBlockEntity getTank(Level level, BlockPos pos, Direction direction) {
        if (level.getBlockEntity(pos) instanceof TankBlockEntity tank
                && getDirection(tank) == direction) {
            return tank;
        }
        return null;
    }

    private static SinkBlockEntity getSink(Level level, BlockPos pos, Direction direction) {
        if (level.getBlockEntity(pos) instanceof SinkBlockEntity tank
                && getDirection(tank) == direction) {
            return tank;
        }
        return null;
    }

    private static Direction getDirection(BlockEntity blockEntity) {
        return blockEntity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    public static int getWorkingTanks(List<TankBlockEntity> tanks) {
        int result = 0;

        for (int i = 0; i < tanks.size(); i++) {
            TankBlockEntity tank = tanks.get(i);

            if (tank.tank.getFluidAmount() > 0 && tank.isOpen()) {
                result++;
            }
        }

        return result;
    }
}
