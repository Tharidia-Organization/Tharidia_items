package com.THproject.tharidia_things.network;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.weight.WeightData;
import com.THproject.tharidia_things.weight.WeightRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

public record WeightConfigSyncPacket(
        Map<String, Double> itemWeights,
        double light,
        double medium,
        double heavy,
        double overencumbered,
        double lightSpeedMultiplier,
        double mediumSpeedMultiplier,
        double heavySpeedMultiplier,
        double overencumberedSpeedMultiplier,
        boolean heavyDisableSwimUp,
        boolean overencumberedDisableSwimUp
) implements CustomPacketPayload {
    public static final Type<WeightConfigSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "weight_config_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WeightConfigSyncPacket> STREAM_CODEC =
            StreamCodec.of(WeightConfigSyncPacket::encode, WeightConfigSyncPacket::decode);

    public static WeightConfigSyncPacket fromCurrentRegistry() {
        WeightData data = WeightRegistry.getWeightData();
        WeightData.WeightThresholds thresholds = WeightRegistry.getThresholds();
        WeightData.WeightDebuffs debuffs = WeightRegistry.getDebuffs();

        Map<String, Double> weights = data != null ? data.getItemWeights() : Map.of();

        return new WeightConfigSyncPacket(
                weights,
                thresholds.getLight(),
                thresholds.getMedium(),
                thresholds.getHeavy(),
                thresholds.getOverencumbered(),
                debuffs.getSpeedMultiplier(WeightData.WeightStatus.LIGHT),
                debuffs.getSpeedMultiplier(WeightData.WeightStatus.MEDIUM),
                debuffs.getSpeedMultiplier(WeightData.WeightStatus.HEAVY),
                debuffs.getSpeedMultiplier(WeightData.WeightStatus.OVERENCUMBERED),
                debuffs.isSwimUpDisabled(WeightData.WeightStatus.HEAVY),
                debuffs.isSwimUpDisabled(WeightData.WeightStatus.OVERENCUMBERED)
        );
    }

    private static void encode(RegistryFriendlyByteBuf buf, WeightConfigSyncPacket packet) {
        buf.writeVarInt(packet.itemWeights().size());
        for (Map.Entry<String, Double> entry : packet.itemWeights().entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeDouble(entry.getValue());
        }

        buf.writeDouble(packet.light());
        buf.writeDouble(packet.medium());
        buf.writeDouble(packet.heavy());
        buf.writeDouble(packet.overencumbered());

        buf.writeDouble(packet.lightSpeedMultiplier());
        buf.writeDouble(packet.mediumSpeedMultiplier());
        buf.writeDouble(packet.heavySpeedMultiplier());
        buf.writeDouble(packet.overencumberedSpeedMultiplier());

        buf.writeBoolean(packet.heavyDisableSwimUp());
        buf.writeBoolean(packet.overencumberedDisableSwimUp());
    }

    private static WeightConfigSyncPacket decode(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<String, Double> weights = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String key = buf.readUtf();
            double value = buf.readDouble();
            weights.put(key, value);
        }

        double light = buf.readDouble();
        double medium = buf.readDouble();
        double heavy = buf.readDouble();
        double overencumbered = buf.readDouble();

        double lightSpeedMultiplier = buf.readDouble();
        double mediumSpeedMultiplier = buf.readDouble();
        double heavySpeedMultiplier = buf.readDouble();
        double overencumberedSpeedMultiplier = buf.readDouble();

        boolean heavyDisableSwimUp = buf.readBoolean();
        boolean overencumberedDisableSwimUp = buf.readBoolean();

        return new WeightConfigSyncPacket(
                weights,
                light,
                medium,
                heavy,
                overencumbered,
                lightSpeedMultiplier,
                mediumSpeedMultiplier,
                heavySpeedMultiplier,
                overencumberedSpeedMultiplier,
                heavyDisableSwimUp,
                overencumberedDisableSwimUp
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WeightConfigSyncPacket packet, Player player) {
        WeightData.WeightThresholds thresholds = new WeightData.WeightThresholds(
                packet.light(),
                packet.medium(),
                packet.heavy(),
                packet.overencumbered()
        );

        WeightData.WeightDebuffs debuffs = new WeightData.WeightDebuffs(
                packet.lightSpeedMultiplier(),
                packet.mediumSpeedMultiplier(),
                packet.heavySpeedMultiplier(),
                packet.overencumberedSpeedMultiplier(),
                packet.heavyDisableSwimUp(),
                packet.overencumberedDisableSwimUp()
        );

        WeightData data = new WeightData(packet.itemWeights(), thresholds, debuffs);
        WeightRegistry.setWeightData(data);
    }
}

