package com.THproject.tharidia_things.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

import java.util.List;

/**
 * Pinza Crucible (Crucible Tongs) - Used to pick up molten metal from the smithing furnace crucible.
 * Stores fluid type in custom data and uses custom_model_data for visual variants.
 * custom_model_data: 1=iron, 2=gold, 3=copper
 */
public class PinzaCrucibleItem extends Item {

    private static final String TAG_FLUID = "FluidType";

    public PinzaCrucibleItem(Properties properties) {
        super(properties);
    }

    /**
     * Gets the fluid type held by this pinza crucible.
     * @return "iron", "gold", "copper", or "" if empty
     */
    public static String getFluidType(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (!customData.contains(TAG_FLUID)) {
            return "";
        }
        return customData.copyTag().getString(TAG_FLUID);
    }

    /**
     * Sets the fluid type and updates custom_model_data for the visual variant.
     */
    public static void setFluidType(ItemStack stack, String type) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        customData = customData.update(tag -> tag.putString(TAG_FLUID, type));
        stack.set(DataComponents.CUSTOM_DATA, customData);

        // Set custom model data for variant model
        int modelData = switch (type) {
            case "iron" -> 1;
            case "gold" -> 2;
            case "copper" -> 3;
            default -> 0;
        };
        if (modelData > 0) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(modelData));
        }
    }

    /**
     * Clears the fluid from the pinza crucible.
     */
    public static void clearFluid(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        customData = customData.update(tag -> tag.remove(TAG_FLUID));
        stack.set(DataComponents.CUSTOM_DATA, customData);
        stack.remove(DataComponents.CUSTOM_MODEL_DATA);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        String fluidType = getFluidType(stack);
        if (!fluidType.isEmpty()) {
            String translationKey = "item.tharidiathings.pinza_crucible.fluid." + fluidType;
            tooltipComponents.add(Component.translatable(translationKey));
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
