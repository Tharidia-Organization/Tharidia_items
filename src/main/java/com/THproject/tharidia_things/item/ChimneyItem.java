package com.THproject.tharidia_things.item;

import com.THproject.tharidia_things.block.entity.SmithingFurnaceBlockEntity;
import com.THproject.tharidia_things.client.renderer.ChimneyItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;

import java.util.function.Consumer;

/**
 * Chimney upgrade item for the Smithing Furnace.
 */
public class ChimneyItem extends FurnaceUpgradeItem {

    public ChimneyItem(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean tryInstall(SmithingFurnaceBlockEntity furnace) {
        return furnace.installChimney();
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private ChimneyItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (this.renderer == null) {
                    this.renderer = new ChimneyItemRenderer();
                }
                return this.renderer;
            }
        });
    }
}
