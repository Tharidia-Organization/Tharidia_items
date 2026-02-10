package com.THproject.tharidia_things.item;

import com.THproject.tharidia_things.block.entity.SmithingFurnaceBlockEntity;
import com.THproject.tharidia_things.client.renderer.DoorItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;

import java.util.function.Consumer;

/**
 * Door upgrade item for the Smithing Furnace (stage_5).
 */
public class DoorItem extends FurnaceUpgradeItem {

    public DoorItem(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean tryInstall(SmithingFurnaceBlockEntity furnace) {
        return furnace.installDoor();
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private DoorItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (this.renderer == null) {
                    this.renderer = new DoorItemRenderer();
                }
                return this.renderer;
            }
        });
    }
}
