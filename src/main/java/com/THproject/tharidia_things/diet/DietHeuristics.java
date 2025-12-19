package com.THproject.tharidia_things.diet;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Heuristics for determining food categories based on tags and name patterns.
 * Used as fallback when recipe analysis is not available.
 */
public class DietHeuristics {
    private static final Logger LOGGER = LoggerFactory.getLogger(DietHeuristics.class);
    
    private static final List<TagKey<Item>> MEAT_TAGS = List.of(
            ItemTags.MEAT,
            ItemTags.FISHES,
            tag("c", "meat"),
            tag("c", "cooked_meat"),
            tag("c", "raw_meat"),
            tag("c", "foods/meat"),
            tag("forge", "raw_meat"),
            tag("forge", "cooked_meat"),
            tag("forge", "meat"),
            tag("minecraft", "meat")
    );
    
    private static final List<TagKey<Item>> VEGETABLE_TAGS = List.of(
            tag("c", "vegetables"),
            tag("c", "foods/vegetable"),
            tag("forge", "vegetables"),
            tag("forge", "vegetable"),
            tag("minecraft", "vegetables")
    );
    
    private static final List<TagKey<Item>> FRUIT_TAGS = List.of(
            tag("c", "fruits"),
            tag("c", "foods/fruit"),
            tag("forge", "fruits"),
            tag("forge", "fruit"),
            tag("minecraft", "fruits")
    );
    
    private static final List<TagKey<Item>> GRAIN_TAGS = List.of(
            tag("c", "grains"),
            tag("c", "bread"),
            tag("c", "foods/grain"),
            tag("c", "crops"),
            tag("forge", "bread"),
            tag("forge", "grain"),
            tag("forge", "grains"),
            tag("forge", "crops/wheat"),
            tag("forge", "crops/rice"),
            tag("minecraft", "grains")
    );
    
    private static final List<TagKey<Item>> DRINK_TAGS = List.of(
            tag("c", "drinks"),
            tag("c", "foods/drink"),
            tag("c", "soups"),
            tag("c", "stews"),
            tag("c", "beverages"),
            tag("forge", "drinks"),
            tag("forge", "soups"),
            tag("forge", "beverages"),
            tag("minecraft", "drinks")
    );
    
    private static final String[] MEAT_KEYWORDS = {
            "meat", "beef", "pork", "chicken", "mutton", "steak",
            "bacon", "sausage", "ham", "turkey", "duck", "fish", "salmon", "cod",
            "lamb", "veal", "venison", "rabbit", "goat", "quail", "pheasant",
            "anchovy", "tuna", "trout", "halibut", "sardine", "mackerel",
            "shrimp", "crab", "lobster", "scallop", "clam", "oyster", "mussel",
            "jerky", "pepperoni", "salami", "prosciutto", "chorizo", "carnitas",
            "ribs", "chop", "cutlet", "fillet", "drumstick", "wing", "thigh"
    };
    
    private static final String[] VEGETABLE_KEYWORDS = {
            "carrot", "potato", "tomato", "lettuce", "cabbage", "onion",
            "garlic", "pepper", "mushroom", "salad", "veg", "bean",
            "broccoli", "cauliflower", "celery", "cucumber", "zucchini", "squash",
            "pumpkin", "eggplant", "asparagus", "spinach", "kale", "chard",
            "radish", "turnip", "beet", "pea", "lentil", "chickpea",
            "okra", "artichoke", "leek", "shallot", "ginger", "horseradish",
            "brussels", "sprout", "endive", "arugula", "bok", "choy", "daikon"
    };
    
    private static final String[] FRUIT_KEYWORDS = {
            "berry", "berries", "apple", "melon", "orange", "banana", "grape",
            "peach", "pear", "fruit", "cherry", "citrus", "coco",
            "strawberry", "blueberry", "raspberry", "blackberry", "cranberry",
            "sweet_berries", "glow_berries", "chorus_fruit",
            "watermelon", "cantaloupe", "honeydew", "kiwi", "pineapple", "mango",
            "papaya", "apricot", "plum", "nectarine", "fig", "date", "pomegranate",
            "lemon", "lime", "grapefruit", "tangerine", "clementine", "coconut",
            "avocado", "olive", "persimmon", "lychee", "dragonfruit", "passion"
    };
    
    private static final String[] GRAIN_KEYWORDS = {
            "bread", "wheat", "grain", "flour", "pasta", "rice",
            "noodle", "dough", "cereal", "oat", "bun", "cake", "cookie",
            "barley", "rye", "corn", "millet", "quinoa", "amaranth", "spelt",
            "bagel", "muffin", "croissant", "biscuit", "roll", "tortilla", "pita",
            "macaroni", "spaghetti", "linguine", "fettuccine", "penne", "rotini",
            "pancake", "waffle", "crepe", "biscotti", "pretzel", "cracker", "crisp",
            "bran", "germ", "grits", "polenta", "couscous", "bulgur", "farro"
    };
    
    private static final String[] DRINK_KEYWORDS = {
            "soup", "stew", "tea", "drink", "juice", "smoothie",
            "broth", "coffee", "milkshake", "latte", "cappuccino", "espresso",
            "cocoa", "chocolate", "hot", "cider", "punch", "lemonade",
            "soda", "pop", "cola", "ginger", "ale", "beer", "wine",
            "water", "mineral", "sparkling", "tonic", "syrup", "nectar",
            "gazpacho", "bisque", "chowder", "gumbo", "curry", "ramen", "pho"
    };
    
    private static final Map<String, ModHeuristics> MOD_HEURISTICS = new HashMap<>();
    
    static {
        MOD_HEURISTICS.put("farmersdelight", new ModHeuristics(1.2f, 1.3f, 1.4f, 1.3f, 1.1f, 1.0f));
        MOD_HEURISTICS.put("croptopia", new ModHeuristics(1.1f, 1.1f, 1.5f, 1.4f, 1.2f, 1.0f));
        MOD_HEURISTICS.put("pamhc2foodcore", new ModHeuristics(1.1f, 1.1f, 1.3f, 1.3f, 1.2f, 1.0f));
        MOD_HEURISTICS.put("cuisine", new ModHeuristics(1.3f, 1.2f, 1.3f, 1.2f, 1.1f, 1.0f));
        MOD_HEURISTICS.put("brew", new ModHeuristics(1.0f, 1.0f, 1.0f, 1.0f, 1.2f, 1.3f));
        MOD_HEURISTICS.put("quark", new ModHeuristics(1.1f, 1.0f, 1.0f, 1.0f, 1.1f, 1.0f));
        MOD_HEURISTICS.put("botania", new ModHeuristics(1.0f, 1.0f, 1.2f, 1.3f, 1.1f, 1.1f));
    }
    
    public static boolean isMeatLike(Item item) {
        if (matchesAnyTag(item, MEAT_TAGS)) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        String path = id.getPath();
        String fullId = id.toString();
        return containsKeyword(path, MEAT_KEYWORDS) || containsKeyword(fullId, MEAT_KEYWORDS);
    }
    
    public static boolean hasVegetableHints(Item item) {
        if (matchesAnyTag(item, VEGETABLE_TAGS)) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        String path = id.getPath();
        String fullId = id.toString();
        return containsKeyword(path, VEGETABLE_KEYWORDS) || containsKeyword(fullId, VEGETABLE_KEYWORDS);
    }
    
    public static boolean hasFruitHints(Item item) {
        if (matchesAnyTag(item, FRUIT_TAGS)) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        String path = id.getPath();
        String fullId = id.toString();
        return containsKeyword(path, FRUIT_KEYWORDS) || containsKeyword(fullId, FRUIT_KEYWORDS);
    }
    
    public static boolean hasGrainHints(Item item) {
        if (matchesAnyTag(item, GRAIN_TAGS)) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        String path = id.getPath();
        String fullId = id.toString();
        return containsKeyword(path, GRAIN_KEYWORDS) || containsKeyword(fullId, GRAIN_KEYWORDS);
    }
    
    public static boolean hasDrinkEffect(Item item) {
        if (matchesAnyTag(item, DRINK_TAGS)) {
            return true;
        }
        String path = BuiltInRegistries.ITEM.getKey(item).getPath();
        return containsKeyword(path, DRINK_KEYWORDS);
    }
    
    public static DietProfile applyModHeuristics(ResourceLocation itemId, DietProfile profile) {
        String modId = itemId.getNamespace();
        
        if ("minecraft".equals(modId)) {
            return profile;
        }
        
        ModHeuristics heuristics = MOD_HEURISTICS.get(modId);
        if (heuristics != null) {
            LOGGER.debug("[DIET] Applying {} mod heuristics to {}", modId, itemId);
            return applyHeuristics(profile, heuristics);
        }
        
        for (Map.Entry<String, ModHeuristics> entry : MOD_HEURISTICS.entrySet()) {
            if (modId.contains(entry.getKey())) {
                LOGGER.debug("[DIET] Applying {} mod heuristics (partial match) to {}", entry.getKey(), itemId);
                return applyHeuristics(profile, entry.getValue());
            }
        }
        
        if (modId.contains("farm") || modId.contains("harvest") || modId.contains("grow") || modId.contains("agricraft")) {
            LOGGER.debug("[DIET] Applying farm heuristics to {}", itemId);
            ModHeuristics farmHeuristics = new ModHeuristics(1.2f, 1.1f, 1.3f, 1.2f, 1.1f, 1.0f);
            return applyHeuristics(profile, farmHeuristics);
        }
        
        if (modId.contains("food") || modId.contains("cook") || modId.contains("kitchen") || 
            modId.contains("culinary") || modId.contains("cuisine")) {
            LOGGER.debug("[DIET] Applying food heuristics to {}", itemId);
            ModHeuristics foodHeuristics = new ModHeuristics(1.1f, 1.1f, 1.2f, 1.2f, 1.1f, 1.0f);
            return applyHeuristics(profile, foodHeuristics);
        }
        
        return profile;
    }
    
    private static DietProfile applyHeuristics(DietProfile profile, ModHeuristics heuristics) {
        return DietProfile.of(
                profile.get(DietCategory.GRAIN) * heuristics.grainMultiplier,
                profile.get(DietCategory.PROTEIN) * heuristics.proteinMultiplier,
                profile.get(DietCategory.VEGETABLE) * heuristics.vegetableMultiplier,
                profile.get(DietCategory.FRUIT) * heuristics.fruitMultiplier,
                profile.get(DietCategory.SUGAR) * heuristics.sugarMultiplier,
                profile.get(DietCategory.WATER) * heuristics.waterMultiplier
        );
    }
    
    private static boolean matchesAnyTag(Item item, List<TagKey<Item>> tags) {
        var holder = item.builtInRegistryHolder();
        for (TagKey<Item> tag : tags) {
            if (holder.is(tag)) {
                return true;
            }
        }
        return false;
    }
    
    private static boolean containsKeyword(String value, String[] keywords) {
        String lower = value.toLowerCase();
        for (String keyword : keywords) {
            if (containsAsWord(lower, keyword)) {
                return true;
            }
        }
        return false;
    }
    
    private static boolean containsAsWord(String text, String keyword) {
        int index = text.indexOf(keyword);
        if (index == -1) {
            return false;
        }
        
        while (index >= 0) {
            boolean validStart = index == 0 || !Character.isLetterOrDigit(text.charAt(index - 1));
            boolean validEnd = (index + keyword.length() >= text.length()) || 
                              !Character.isLetterOrDigit(text.charAt(index + keyword.length()));
            
            if (index > 0 && text.charAt(index - 1) == '_') {
                validStart = true;
            }
            if (index + keyword.length() < text.length() && text.charAt(index + keyword.length()) == '_') {
                validEnd = true;
            }
            
            if (validStart && validEnd) {
                return true;
            }
            
            index = text.indexOf(keyword, index + 1);
        }
        
        return false;
    }
    
    private static TagKey<Item> tag(String namespace, String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }
    
    private static record ModHeuristics(
        float grainMultiplier,
        float proteinMultiplier,
        float vegetableMultiplier,
        float fruitMultiplier,
        float sugarMultiplier,
        float waterMultiplier
    ) {}
}
