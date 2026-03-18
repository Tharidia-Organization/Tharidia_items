package com.THproject.tharidia_things.mixin;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(HumanoidArmorLayer.class)
public interface HumanoidArmorLayerAccessor<M extends HumanoidModel<?>> {
    
    // The @Invoker tag acts as a bridge to the protected vanilla method
    @Invoker("getArmorModel")
    M invokeGetArmorModel(EquipmentSlot slot);
}