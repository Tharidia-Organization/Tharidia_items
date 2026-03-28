package com.THproject.tharidia_things.jei;

import com.THproject.tharidia_things.TharidiaThings;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public class TharidiaJeiPlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_ID =
            ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        TharidiaThings.LOGGER.info("[Tharidia JEI] *** onRuntimeAvailable fired ***");
        JeiTagFilterManager.onJeiRuntimeAvailable(runtime);
    }
}
