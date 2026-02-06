package com.THproject.tharidia_things.item;

import com.THproject.tharidia_things.block.entity.SmithingFurnaceBlockEntity;
import com.THproject.tharidia_things.client.renderer.HooverItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;

import java.util.function.Consumer;

/**
 * Hoover upgrade item for the Smithing Furnace.
 */
public class HooverItem extends FurnaceUpgradeItem {

    public HooverItem(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean tryInstall(SmithingFurnaceBlockEntity furnace) {
        return furnace.installHoover();
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private HooverItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (this.renderer == null) {
                    this.renderer = new HooverItemRenderer();
                }
                return this.renderer;
            }
        });
    }
}
