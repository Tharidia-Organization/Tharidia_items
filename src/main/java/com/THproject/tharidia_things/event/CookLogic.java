package com.THproject.tharidia_things.event;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joml.Vector3f;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.diet.DietAttachments;
import com.THproject.tharidia_things.diet.DietCategory;
import com.THproject.tharidia_things.diet.DietData;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = TharidiaThings.MODID)
public class CookLogic {
    private static final Map<DietCategory, Integer> DIET_PARTICLES = new HashMap<>() {
        {
            put(DietCategory.FRUIT, 0xFA73EF);
            put(DietCategory.GRAIN, 0xDEC71D);
            put(DietCategory.PROTEIN, 0x4DE0FA);
            put(DietCategory.SUGAR, 0xE6FFF9);
            put(DietCategory.VEGETABLE, 0x62E35F);
            put(DietCategory.WATER, 0x2C7BF2);
        }
    };

    @SubscribeEvent
    public static void onCookLogic(PlayerTickEvent.Post event) {
        Level level = event.getEntity().level();
        Player player = event.getEntity();

        // Works only on ServerSide
        if(!level.isClientSide) return;
        if (level instanceof ServerLevel) return;
        
        // Only run every 1 second
        if (player.tickCount % 20 != 0) return;

        // Check if player has a stick to see particles (placeholder, maybe we use a cook hat)
        if (player.getMainHandItem().getItem() != Items.STICK) return;

        if (player != Minecraft.getInstance().player) return;

        double radius = 10.0;
        AABB searchArea = player.getBoundingBox().inflate(radius);
        List<Player> nearbyEntities = level.getEntitiesOfClass(Player.class, searchArea);

        nearbyEntities.forEach(nearPlayer -> {
            DietCategory diet_category = getLowDietCategory(nearPlayer);
            if (diet_category == null) return;

            int color = DIET_PARTICLES.getOrDefault(diet_category, 0xFFFFFF);

            float red = ((color >> 16) & 0xFF) / 255f;
            float green = ((color >> 8) & 0xFF) / 255f;
            float blue = (color & 0xFF) / 255f;
            float scale = 0.5f;

            DustParticleOptions particleData = new DustParticleOptions(new Vector3f(red, green, blue), scale);

            level.addParticle(
                    particleData,
                    nearPlayer.getX(), nearPlayer.getY() + 2.0, nearPlayer.getZ(),
                    0.2, 0.2, 0.2);
        });
    }

    public static DietCategory getLowDietCategory(Player player) {
        DietData diet_data = player.getData(DietAttachments.DIET_DATA.get());

        float last_value = 100; // Max value for diet
        DietCategory low_diet_category = null;
        for (DietCategory diet_category : DietCategory.VALUES) {
            if (diet_data.get(diet_category) < last_value) {
                last_value = diet_data.get(diet_category);
                low_diet_category = diet_category;
            }
        }

        return low_diet_category;
    }
}
