package com.THproject.tharidia_things.block.herbalist;

import net.minecraft.world.item.ItemStack;

public enum Plants {
    DANDELION("minecraft:dandelion", PlantTypes.FLOWER, 0xFED83D),
    POPPY("minecraft:poppy", PlantTypes.FLOWER, 0xED1C24),
    BLUE_ORCHID("minecraft:blue_orchid", PlantTypes.FLOWER, 0x2ABFFD),
    ALLIUM("minecraft:allium", PlantTypes.FLOWER, 0xB878ED),
    AZURE_BLUET("minecraft:azure_bluet", PlantTypes.FLOWER, 0xD6E8E8),
    RED_TULIP("minecraft:red_tulip", PlantTypes.FLOWER, 0xED1C24),
    ORANGE_TULIP("minecraft:orange_tulip", PlantTypes.FLOWER, 0xFF6A00),
    WHITE_TULIP("minecraft:white_tulip", PlantTypes.FLOWER, 0xFFFFFF),
    PINK_TULIP("minecraft:pink_tulip", PlantTypes.FLOWER, 0xFF69B4),
    OXEYE_DAISY("minecraft:oxeye_daisy", PlantTypes.FLOWER, 0xF0E68C),
    CORNFLOWER("minecraft:cornflower", PlantTypes.FLOWER, 0x466EEB),
    LILY_OF_THE_VALLEY("minecraft:lily_of_the_valley", PlantTypes.FLOWER, 0xF0FFF0),
    WITHER_ROSE("minecraft:wither_rose", PlantTypes.FLOWER, 0x2C2C2C),
    TORCHFLOWER("minecraft:torchflower", PlantTypes.FLOWER, 0xFF8C00);

    public final String plant;
    public final PlantTypes type;
    public final int color;

    Plants(String plant, PlantTypes type, int color) {
        this.plant = plant;
        this.type = type;
        this.color = color;
    }

    public static int getColor(ItemStack stack) {
        for (Plants value : values()) {
            if (value.plant.equals(stack.getItem().toString())) {
                return value.color;
            }
        }
        return 0xFFFFFF;
    }

    public static boolean isPlant(ItemStack stack) {
        for (Plants value : values()) {
            if (value.plant.equals(stack.getItem().toString())) {
                return true;
            }
        }
        return false;
    }

    public static PlantTypes getPlantTypes(ItemStack stack) {
        for (Plants value : values()) {
            if (value.plant.equals(stack.getItem().toString())) {
                return value.type;
            }
        }
        return null;
    }

    public enum PlantTypes {
        FLOWER,
        MUSHROOM
    }
}
