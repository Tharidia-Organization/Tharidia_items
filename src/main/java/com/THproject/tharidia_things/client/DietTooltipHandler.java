package com.THproject.tharidia_things.client;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.diet.DietCategory;
import com.THproject.tharidia_things.diet.DietProfile;
import com.THproject.tharidia_things.diet.DietRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adds diet contribution lines to food tooltips.
 */
@EventBusSubscriber(modid = TharidiaThings.MODID, value = Dist.CLIENT)
public class DietTooltipHandler {
    private static final Map<ResourceLocation, List<Component>> CACHE = new HashMap<>();
    private static final EnumMap<DietCategory, TextColor> CATEGORY_COLORS = new EnumMap<>(DietCategory.class);

    static {
        CATEGORY_COLORS.put(DietCategory.GRAIN, TextColor.fromRgb(0xDAA520));
        CATEGORY_COLORS.put(DietCategory.PROTEIN, TextColor.fromRgb(0xCD5C5C));
        CATEGORY_COLORS.put(DietCategory.VEGETABLE, TextColor.fromRgb(0x228B22));
        CATEGORY_COLORS.put(DietCategory.FRUIT, TextColor.fromRgb(0xFF6347));
        CATEGORY_COLORS.put(DietCategory.SUGAR, TextColor.fromRgb(0xFFB6C1));
        CATEGORY_COLORS.put(DietCategory.WATER, TextColor.fromRgb(0x1E90FF));
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || stack.getItem().getFoodProperties(stack, event.getEntity()) == null) {
            return;
        }

        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        List<Component> cached = CACHE.get(id);
        if (cached == null) {
            cached = buildTooltipLines(stack);
            CACHE.put(id, cached);
        }

        if (!cached.isEmpty()) {
            event.getToolTip().add(Component.literal(" "));
            event.getToolTip().add(Component.literal("§6Valori dieta:"));
            event.getToolTip().addAll(cached);
        }
    }

    private static List<Component> buildTooltipLines(ItemStack stack) {
        DietProfile profile = DietRegistry.getProfile(stack);
        if (profile.isEmpty()) {
            return List.of();
        }

        EnumMap<DietCategory, String> labels = new EnumMap<>(DietCategory.class);
        labels.put(DietCategory.GRAIN, "Cereali");
        labels.put(DietCategory.PROTEIN, "Proteine");
        labels.put(DietCategory.VEGETABLE, "Verdure");
        labels.put(DietCategory.FRUIT, "Frutta");
        labels.put(DietCategory.SUGAR, "Zuccheri");
        labels.put(DietCategory.WATER, "Idratazione");

        List<Component> lines = new java.util.ArrayList<>();
        for (DietCategory category : DietCategory.VALUES) {
            float value = profile.get(category);
            if (value <= 0.0f) {
                continue;
            }
            String label = labels.getOrDefault(category, category.name());
            TextColor color = CATEGORY_COLORS.getOrDefault(category, TextColor.fromRgb(0xFFFFFF));
            lines.add(createDietLine(label, value, color));
        }
        return lines;
    }

    private static Component createDietLine(String label, float value, TextColor color) {
        MutableComponent line = Component.literal("");
        line.append(Component.literal(" - ").withStyle(ChatFormatting.GRAY));
        line.append(Component.literal(label + ": ").withStyle(Style.EMPTY.withColor(color)));
        line.append(Component.literal(String.format("+%.1f", value)).withStyle(ChatFormatting.WHITE));
        return line;
    }
}
