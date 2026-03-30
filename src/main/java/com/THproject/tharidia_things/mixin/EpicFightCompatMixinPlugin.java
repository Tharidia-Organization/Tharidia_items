package com.THproject.tharidia_things.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.util.List;
import java.util.Set;

public class EpicFightCompatMixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains("EpicFightArmorLayerMixin")) {
            try {
                Class.forName("yesman.epicfight.client.renderer.patched.layer.WearableItemLayer", false,
                        this.getClass().getClassLoader());
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
        if (mixinClassName.contains("EpicFightFirstPersonScaleMixin")) {
            try {
                Class.forName("yesman.epicfight.client.renderer.FirstPersonRenderer", false,
                        this.getClass().getClassLoader());
                // Note: 'capabilites' is an intentional typo in Epic Fight's source
                Class.forName("virtuoel.pehkui.api.ScaleTypes", false,
                        this.getClass().getClassLoader());
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}