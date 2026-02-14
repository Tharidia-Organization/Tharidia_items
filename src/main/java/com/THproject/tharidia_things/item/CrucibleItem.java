package com.THproject.tharidia_things.item;

import com.THproject.tharidia_things.block.entity.SmithingFurnaceBlockEntity;
import com.THproject.tharidia_things.client.renderer.CrucibleItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;

import java.util.function.Consumer;

/**
 * Crucible upgrade item for the Smithing Furnace.
 * Enables the cogiuolo animation when installed.
 */
public class CrucibleItem extends FurnaceUpgradeItem {

    public CrucibleItem(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean tryInstall(SmithingFurnaceBlockEntity furnace) {
        return furnace.installCrucible();
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private CrucibleItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (this.renderer == null) {
                    this.renderer = new CrucibleItemRenderer();
                }
                return this.renderer;
            }
        });
    }
}
