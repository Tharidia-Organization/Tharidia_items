package com.THproject.tharidia_things.stable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Set;

/**
 * Helper class for detecting animal types and their associated items.
 * Supports both vanilla and modded animals through explicit checks and heuristics.
 *
 * This class centralizes all animal type detection logic to ensure consistency
 * across the stable system and to properly support modded animals.
 */
public final class AnimalTypeHelper {

    private AnimalTypeHelper() {} // Prevent instantiation

    // ==================== MILK PRODUCTION ====================

    /**
     * Checks if an entity type can produce milk.
     * Supports vanilla animals (cow, goat, mooshroom) and modded animals.
     *
     * @param entityType the entity type to check
     * @return true if the animal can produce milk
     */
    public static boolean isMilkProducingType(EntityType<?> entityType) {
        if (entityType == null) return false;

        // Vanilla milk-producing animals
        if (entityType == EntityType.COW ||
            entityType == EntityType.GOAT ||
            entityType == EntityType.MOOSHROOM) {
            return true;
        }

        // Check for modded animals by entity type ID
        // Common modded milk-producing animals often have "cow", "goat", "milk", or "buffalo" in their ID
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        if (entityId != null) {
            String path = entityId.getPath().toLowerCase();
            if (path.contains("cow") ||
                path.contains("goat") ||
                path.contains("milk") ||
                path.contains("buffalo") ||
                path.contains("yak") ||
                path.contains("cattle")) {
                return true;
            }
        }

        return false;
    }

    // ==================== EGG PRODUCTION ====================

    /**
     * Checks if an entity type can produce eggs.
     * Supports vanilla chickens and modded egg-laying animals.
     *
     * @param entityType the entity type to check
     * @return true if the animal can produce eggs
     */
    public static boolean isEggProducingType(EntityType<?> entityType) {
        if (entityType == null) return false;

        // Vanilla egg-producing animals
        if (entityType == EntityType.CHICKEN) {
            return true;
        }

        // Check for modded animals by entity type ID
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        if (entityId != null) {
            String path = entityId.getPath().toLowerCase();
            if (path.contains("chicken") ||
                path.contains("hen") ||
                path.contains("rooster") ||
                path.contains("duck") ||
                path.contains("goose") ||
                path.contains("turkey") ||
                path.contains("quail") ||
                path.contains("pheasant")) {
                return true;
            }
        }

        return false;
    }

    // ==================== BREEDING FOOD ====================

    /**
     * Checks if an item is valid breeding food for the given entity type.
     * Supports vanilla and modded animals through explicit checks and heuristics.
     *
     * @param entityType the entity type to check
     * @param stack the item stack to check
     * @return true if the item can be used to breed this animal
     */
    public static boolean isValidBreedingFood(EntityType<?> entityType, ItemStack stack) {
        if (entityType == null || stack == null || stack.isEmpty()) return false;

        Item item = stack.getItem();
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        String entityPath = entityId != null ? entityId.getPath().toLowerCase() : "";

        // === VANILLA ANIMALS ===

        // Cow, Sheep, Mooshroom -> Wheat
        if (entityType == EntityType.COW ||
            entityType == EntityType.SHEEP ||
            entityType == EntityType.MOOSHROOM) {
            return item == Items.WHEAT;
        }

        // Chicken -> Seeds (any type)
        if (entityType == EntityType.CHICKEN) {
            return isAnySeed(stack);
        }

        // Pig -> Carrot, Potato, Beetroot
        if (entityType == EntityType.PIG) {
            return item == Items.CARROT ||
                   item == Items.POTATO ||
                   item == Items.BEETROOT;
        }

        // Goat -> Wheat
        if (entityType == EntityType.GOAT) {
            return item == Items.WHEAT;
        }

        // Rabbit -> Carrot, Golden Carrot, Dandelion
        if (entityType == EntityType.RABBIT) {
            return item == Items.CARROT ||
                   item == Items.GOLDEN_CARROT ||
                   item == Items.DANDELION;
        }

        // Horse, Donkey, Mule -> Golden Apple, Golden Carrot
        if (entityType == EntityType.HORSE ||
            entityType == EntityType.DONKEY ||
            entityType == EntityType.MULE) {
            return item == Items.GOLDEN_APPLE ||
                   item == Items.ENCHANTED_GOLDEN_APPLE ||
                   item == Items.GOLDEN_CARROT;
        }

        // Llama -> Hay Bale
        if (entityType == EntityType.LLAMA ||
            entityType == EntityType.TRADER_LLAMA) {
            return item == Items.HAY_BLOCK;
        }

        // Turtle -> Seagrass
        if (entityType == EntityType.TURTLE) {
            return item == Items.SEAGRASS;
        }

        // Panda -> Bamboo
        if (entityType == EntityType.PANDA) {
            return item == Items.BAMBOO;
        }

        // Fox -> Sweet Berries, Glow Berries
        if (entityType == EntityType.FOX) {
            return item == Items.SWEET_BERRIES ||
                   item == Items.GLOW_BERRIES;
        }

        // Bee -> Any flower
        if (entityType == EntityType.BEE) {
            return isFlower(stack);
        }

        // Strider -> Warped Fungus
        if (entityType == EntityType.STRIDER) {
            return item == Items.WARPED_FUNGUS;
        }

        // Hoglin -> Crimson Fungus
        if (entityType == EntityType.HOGLIN) {
            return item == Items.CRIMSON_FUNGUS;
        }

        // Axolotl -> Bucket of Tropical Fish
        if (entityType == EntityType.AXOLOTL) {
            return item == Items.TROPICAL_FISH_BUCKET;
        }

        // Frog -> Slime Ball
        if (entityType == EntityType.FROG) {
            return item == Items.SLIME_BALL;
        }

        // Camel -> Cactus
        if (entityType == EntityType.CAMEL) {
            return item == Items.CACTUS;
        }

        // Sniffer -> Torchflower Seeds
        if (entityType == EntityType.SNIFFER) {
            return item == Items.TORCHFLOWER_SEEDS;
        }

        // Armadillo -> Spider Eye
        if (entityType == EntityType.ARMADILLO) {
            return item == Items.SPIDER_EYE;
        }

        // Cat, Ocelot -> Raw Cod, Raw Salmon
        if (entityType == EntityType.CAT || entityType == EntityType.OCELOT) {
            return item == Items.COD || item == Items.SALMON;
        }

        // Wolf -> Any meat
        if (entityType == EntityType.WOLF) {
            return isMeat(stack);
        }

        // Parrot -> Seeds (but can't actually breed, just tame)
        if (entityType == EntityType.PARROT) {
            return isAnySeed(stack);
        }

        // === MODDED ANIMALS HEURISTICS ===

        // Cow-like animals -> Wheat
        if (entityPath.contains("cow") ||
            entityPath.contains("cattle") ||
            entityPath.contains("buffalo") ||
            entityPath.contains("yak") ||
            entityPath.contains("ox")) {
            return item == Items.WHEAT;
        }

        // Sheep-like animals -> Wheat
        if (entityPath.contains("sheep") ||
            entityPath.contains("ram") ||
            entityPath.contains("lamb")) {
            return item == Items.WHEAT;
        }

        // Goat-like animals -> Wheat
        if (entityPath.contains("goat")) {
            return item == Items.WHEAT;
        }

        // Pig-like animals -> Carrot, Potato, Beetroot
        if (entityPath.contains("pig") ||
            entityPath.contains("boar") ||
            entityPath.contains("hog")) {
            return item == Items.CARROT ||
                   item == Items.POTATO ||
                   item == Items.BEETROOT;
        }

        // Chicken-like animals -> Seeds
        if (entityPath.contains("chicken") ||
            entityPath.contains("hen") ||
            entityPath.contains("rooster") ||
            entityPath.contains("duck") ||
            entityPath.contains("goose") ||
            entityPath.contains("turkey") ||
            entityPath.contains("quail") ||
            entityPath.contains("pheasant")) {
            return isAnySeed(stack);
        }

        // Rabbit-like animals -> Carrot
        if (entityPath.contains("rabbit") ||
            entityPath.contains("bunny") ||
            entityPath.contains("hare")) {
            return item == Items.CARROT || item == Items.GOLDEN_CARROT;
        }

        // Horse-like animals -> Golden Apple/Carrot
        if (entityPath.contains("horse") ||
            entityPath.contains("pony") ||
            entityPath.contains("donkey") ||
            entityPath.contains("mule") ||
            entityPath.contains("zebra")) {
            return item == Items.GOLDEN_APPLE || item == Items.GOLDEN_CARROT;
        }

        // Llama/Alpaca-like animals -> Hay
        if (entityPath.contains("llama") ||
            entityPath.contains("alpaca")) {
            return item == Items.HAY_BLOCK;
        }

        // Camel-like animals -> Cactus
        if (entityPath.contains("camel") ||
            entityPath.contains("dromedary")) {
            return item == Items.CACTUS;
        }

        // Generic fallback: try wheat for any unrecognized animal
        // This gives modded animals a chance to breed even if not explicitly supported
        return item == Items.WHEAT;
    }

    // ==================== HELPER METHODS ====================

    /**
     * Checks if an item is any type of seed.
     */
    public static boolean isAnySeed(ItemStack stack) {
        Item item = stack.getItem();

        // Vanilla seeds
        if (item == Items.WHEAT_SEEDS ||
            item == Items.MELON_SEEDS ||
            item == Items.PUMPKIN_SEEDS ||
            item == Items.BEETROOT_SEEDS ||
            item == Items.TORCHFLOWER_SEEDS ||
            item == Items.PITCHER_POD) {
            return true;
        }

        // Check item ID for modded seeds
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        if (itemId != null) {
            String path = itemId.getPath().toLowerCase();
            return path.contains("seed");
        }

        return false;
    }

    /**
     * Checks if an item is a flower (for bees).
     */
    public static boolean isFlower(ItemStack stack) {
        Item item = stack.getItem();

        // Common vanilla flowers
        if (item == Items.DANDELION ||
            item == Items.POPPY ||
            item == Items.BLUE_ORCHID ||
            item == Items.ALLIUM ||
            item == Items.AZURE_BLUET ||
            item == Items.RED_TULIP ||
            item == Items.ORANGE_TULIP ||
            item == Items.WHITE_TULIP ||
            item == Items.PINK_TULIP ||
            item == Items.OXEYE_DAISY ||
            item == Items.CORNFLOWER ||
            item == Items.LILY_OF_THE_VALLEY ||
            item == Items.TORCHFLOWER ||
            item == Items.WITHER_ROSE ||
            item == Items.SUNFLOWER ||
            item == Items.LILAC ||
            item == Items.ROSE_BUSH ||
            item == Items.PEONY ||
            item == Items.PITCHER_PLANT) {
            return true;
        }

        // Check tags for modded flowers
        return stack.is(ItemTags.FLOWERS);
    }

    /**
     * Checks if an item is meat (for wolves).
     */
    public static boolean isMeat(ItemStack stack) {
        Item item = stack.getItem();

        // Vanilla meats
        if (item == Items.BEEF ||
            item == Items.COOKED_BEEF ||
            item == Items.PORKCHOP ||
            item == Items.COOKED_PORKCHOP ||
            item == Items.CHICKEN ||
            item == Items.COOKED_CHICKEN ||
            item == Items.MUTTON ||
            item == Items.COOKED_MUTTON ||
            item == Items.RABBIT ||
            item == Items.COOKED_RABBIT ||
            item == Items.ROTTEN_FLESH) {
            return true;
        }

        // Check item ID for modded meats
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        if (itemId != null) {
            String path = itemId.getPath().toLowerCase();
            return path.contains("meat") ||
                   path.contains("beef") ||
                   path.contains("pork") ||
                   path.contains("chicken") ||
                   path.contains("mutton") ||
                   path.contains("venison") ||
                   path.contains("steak");
        }

        return false;
    }

    // ==================== SLAUGHTER LOOT ====================

    /**
     * Gets the primary meat item for an entity type.
     * Returns null if the animal doesn't drop meat.
     */
    public static Item getMeatDrop(EntityType<?> entityType) {
        if (entityType == null) return null;

        // Vanilla animals
        if (entityType == EntityType.COW || entityType == EntityType.MOOSHROOM) {
            return Items.BEEF;
        }
        if (entityType == EntityType.PIG) {
            return Items.PORKCHOP;
        }
        if (entityType == EntityType.CHICKEN) {
            return Items.CHICKEN;
        }
        if (entityType == EntityType.SHEEP) {
            return Items.MUTTON;
        }
        if (entityType == EntityType.RABBIT) {
            return Items.RABBIT;
        }

        // Modded animal heuristics
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        if (entityId != null) {
            String path = entityId.getPath().toLowerCase();

            if (path.contains("cow") || path.contains("cattle") ||
                path.contains("buffalo") || path.contains("yak") || path.contains("ox")) {
                return Items.BEEF;
            }
            if (path.contains("pig") || path.contains("boar") || path.contains("hog")) {
                return Items.PORKCHOP;
            }
            if (path.contains("chicken") || path.contains("hen") ||
                path.contains("duck") || path.contains("turkey")) {
                return Items.CHICKEN;
            }
            if (path.contains("sheep") || path.contains("lamb") || path.contains("goat")) {
                return Items.MUTTON;
            }
            if (path.contains("rabbit") || path.contains("bunny")) {
                return Items.RABBIT;
            }
        }

        // Default: no meat drop
        return null;
    }

    /**
     * Gets the secondary drop item for an entity type (leather, feathers, wool, etc).
     * Returns null if the animal doesn't have a secondary drop.
     */
    public static Item getSecondaryDrop(EntityType<?> entityType) {
        if (entityType == null) return null;

        // Vanilla animals
        if (entityType == EntityType.COW || entityType == EntityType.MOOSHROOM) {
            return Items.LEATHER;
        }
        if (entityType == EntityType.CHICKEN) {
            return Items.FEATHER;
        }
        if (entityType == EntityType.SHEEP) {
            return Items.WHITE_WOOL;
        }
        if (entityType == EntityType.RABBIT) {
            return Items.RABBIT_HIDE;
        }
        if (entityType == EntityType.GOAT) {
            return Items.LEATHER; // Goats don't have specific drops in vanilla
        }

        // Modded animal heuristics
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        if (entityId != null) {
            String path = entityId.getPath().toLowerCase();

            if (path.contains("cow") || path.contains("cattle") ||
                path.contains("buffalo") || path.contains("yak") ||
                path.contains("ox") || path.contains("horse") ||
                path.contains("donkey") || path.contains("pig") ||
                path.contains("boar") || path.contains("goat")) {
                return Items.LEATHER;
            }
            if (path.contains("chicken") || path.contains("duck") ||
                path.contains("turkey") || path.contains("goose") ||
                path.contains("bird")) {
                return Items.FEATHER;
            }
            if (path.contains("sheep") || path.contains("lamb") ||
                path.contains("alpaca") || path.contains("llama")) {
                return Items.WHITE_WOOL;
            }
            if (path.contains("rabbit") || path.contains("bunny")) {
                return Items.RABBIT_HIDE;
            }
        }

        // Default: leather as generic fallback
        return Items.LEATHER;
    }
}
