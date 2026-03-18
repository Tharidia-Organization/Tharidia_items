package com.THproject.tharidia_things.item;

import com.THproject.tharidia_things.block.entity.SmithingFurnaceBlockEntity;
import com.THproject.tharidia_things.client.renderer.BellowsItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;

import java.util.function.Consumer;

/**
 * Bellows upgrade item for the Smithing Furnace (stage_1 - top part).
 */
public class BellowsItem extends FurnaceUpgradeItem {

    public BellowsItem(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean tryInstall(SmithingFurnaceBlockEntity furnace) {
        return furnace.installBellows();
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private BellowsItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (this.renderer == null) {
                    this.renderer = new BellowsItemRenderer();
                }
                return this.renderer;
            }
        });
    }
}
