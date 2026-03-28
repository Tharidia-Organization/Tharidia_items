package com.THproject.tharidia_things.event;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joml.Vector3f;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.spice.PlayerSpiceData;
import com.THproject.tharidia_things.spice.SpiceAttachments;
import com.THproject.tharidia_things.spice.SpiceType;
import com.THproject.tharidia_things.util.CookHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = TharidiaThings.MODID)
public class CookLogic {
    private static final Map<SpiceType, Integer> SPICE_PARTICLES = new HashMap<>() {
        {
            put(SpiceType.COCA, 0xFA73EF);
            put(SpiceType.SPIRU, 0xDEC71D);
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

        // Check if player has a cook hat to see particles
        if (!CookHelper.hasCookHat(player)) return;

        if (player != Minecraft.getInstance().player) return;

        double radius = 10.0;
        AABB searchArea = player.getBoundingBox().inflate(radius);
        List<Player> nearbyEntities = level.getEntitiesOfClass(Player.class, searchArea, p -> p != player);

        nearbyEntities.forEach(nearPlayer -> {
            SpiceType spice_type = getLowSpiceType(nearPlayer);
            if (spice_type == null) return;

            int color = SPICE_PARTICLES.getOrDefault(spice_type, 0xFFFFFF);

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

    public static SpiceType getLowSpiceType(Player player) {
        PlayerSpiceData spice_data = player.getData(SpiceAttachments.PLAYER_SPICE_DATA.get());

        float last_value = 100; // Max value for spice
        SpiceType low_spice_category = null;
        for (SpiceType spice_type : SpiceType.VALUES) {
            if (spice_data.get(spice_type) < last_value) {
                last_value = spice_data.get(spice_type);
                low_spice_category = spice_type;
            }
        }

        return low_spice_category;
    }
}
