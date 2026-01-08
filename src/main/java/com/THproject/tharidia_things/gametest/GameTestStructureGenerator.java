package com.THproject.tharidia_things.gametest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;

import java.nio.file.Files;
import java.nio.file.Path;

public final class GameTestStructureGenerator {
    private static final int DATA_VERSION_1_21_1 = 3955;

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Expected args: <generatedResourcesDir> <modId>");
        }

        Path generatedResourcesDir = Path.of(args[0]);
        String modId = args[1];

        Path output = generatedResourcesDir
                .resolve("data")
                .resolve(modId)
                .resolve("structures")
                .resolve("staminagametests.empty.nbt");

        Path outputInGametestFolder = generatedResourcesDir
                .resolve("data")
                .resolve(modId)
                .resolve("structures")
                .resolve("gametest")
                .resolve("staminagametests.empty.nbt");

        Files.createDirectories(output.getParent());
        Files.createDirectories(outputInGametestFolder.getParent());

        CompoundTag root = new CompoundTag();

        ListTag size = new ListTag();
        size.add(IntTag.valueOf(1));
        size.add(IntTag.valueOf(1));
        size.add(IntTag.valueOf(1));

        root.put("size", size);

        ListTag palette = new ListTag();
        CompoundTag airState = new CompoundTag();
        airState.put("Name", StringTag.valueOf("minecraft:air"));
        palette.add(airState);
        root.put("palette", palette);

        ListTag blocks = new ListTag();
        CompoundTag block0 = new CompoundTag();
        ListTag pos0 = new ListTag();
        pos0.add(IntTag.valueOf(0));
        pos0.add(IntTag.valueOf(0));
        pos0.add(IntTag.valueOf(0));
        block0.put("pos", pos0);
        block0.putInt("state", 0);
        blocks.add(block0);
        root.put("blocks", blocks);

        root.put("entities", new ListTag());
        root.putInt("DataVersion", DATA_VERSION_1_21_1);

        NbtIo.writeCompressed(root, output);
        NbtIo.writeCompressed(root, outputInGametestFolder);
    }
}
