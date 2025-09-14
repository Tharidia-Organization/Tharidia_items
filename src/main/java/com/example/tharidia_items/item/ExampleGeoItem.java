package com.example.tharidia_items.item;

import net.minecraft.item.Item;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.function.Supplier;
import java.util.function.Consumer;

public class ExampleGeoItem extends Item implements GeoItem {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Supplier<Object> renderProvider = GeoItem.makeRenderer(this);

    public ExampleGeoItem(Settings settings) {
        super(settings);
        // Abilita la sincronizzazione lato server per le animazioni
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            return state.setAndContinue(IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // Forniamo il renderer lato client senza referenziare classi client nel source-set main

    public void createRenderer(Consumer<Object> consumer) {
        try {
            Class<?> helper = Class.forName("com.example.tharidia_items.client.ClientRendererHelper");
            helper.getMethod("provideExampleItemRenderer", Consumer.class).invoke(null, consumer);
        } catch (Throwable t) {
            // No-op su server o se la classe client non è presente
        }
    }

    @Override
    public Supplier<Object> getRenderProvider() {
        return this.renderProvider;
    }
}