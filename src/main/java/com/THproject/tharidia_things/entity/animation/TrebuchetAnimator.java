package com.THproject.tharidia_things.entity.animation;

import com.THproject.tharidia_things.client.model.TrebuchetModel;
import com.THproject.tharidia_things.entity.TrebuchetEntity;
import mod.azure.azurelib.common.animation.AzAnimatorConfig;
import mod.azure.azurelib.common.animation.controller.AzAnimationController;
import mod.azure.azurelib.common.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.common.animation.impl.AzEntityAnimator;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * Wires Trebuchet animations into AzureLib 3.x animator pipeline.
 */
public class TrebuchetAnimator extends AzEntityAnimator<TrebuchetEntity> {

    public TrebuchetAnimator() {
        super(AzAnimatorConfig.defaultConfig());
    }

    @Override
    public void registerControllers(AzAnimationControllerContainer<TrebuchetEntity> container) {
        container.add(AzAnimationController.builder(this, "base_controller").build());
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(TrebuchetEntity animatable) {
        return TrebuchetModel.ANIMATION;
    }
}
