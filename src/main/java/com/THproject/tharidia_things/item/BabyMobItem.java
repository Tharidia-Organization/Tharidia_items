package com.THproject.tharidia_things.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class BabyMobItem extends Item {
    private final EntityType<?> entityType;
    
    public BabyMobItem(EntityType<?> entityType, Properties properties) {
        super(properties);
        this.entityType = entityType;
    }
    
    public EntityType<?> getEntityType() {
        return entityType;
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("item.tharidiathings.baby_mob.tooltip", entityType.getDescription()));
    }
}
