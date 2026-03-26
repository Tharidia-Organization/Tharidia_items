package com.THproject.tharidia_things.network;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.cook.CookRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Server → Client: opens the cook recipe book screen.
 * Carries lightweight recipe display data (result item + time) and session state.
 */
public record OpenCookRecipePacket(
        BlockPos blockPos,
        List<RecipeData> recipes,
        String activeRecipeId,
        int timerTicks,
        int totalTimerTicks
) implements CustomPacketPayload {

    public static final Type<OpenCookRecipePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "open_cook_recipe"));

    /**
     * Lightweight DTO: only what the client needs to display the recipe list.
     * The actual Ingredient data stays server-side.
     */
    public record RecipeData(
            String recipeId,    // ResourceLocation as string, used in StartCookingPacket
            ItemStack result,   // rendered as item icon in the GUI
            int timeTicks
    ) {}

    // ── Codecs ────────────────────────────────────────────────────────────────

    private static final StreamCodec<RegistryFriendlyByteBuf, RecipeData> RECIPE_CODEC =
            new StreamCodec<>() {
                @Override
                public RecipeData decode(RegistryFriendlyByteBuf buf) {
                    String id      = buf.readUtf();
                    ItemStack item = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                    int time       = buf.readVarInt();
                    return new RecipeData(id, item, time);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, RecipeData r) {
                    buf.writeUtf(r.recipeId());
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, r.result());
                    buf.writeVarInt(r.timeTicks());
                }
            };

    private static final StreamCodec<RegistryFriendlyByteBuf, List<RecipeData>> LIST_CODEC =
            new StreamCodec<>() {
                @Override
                public List<RecipeData> decode(RegistryFriendlyByteBuf buf) {
                    int size = buf.readVarInt();
                    List<RecipeData> list = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) list.add(RECIPE_CODEC.decode(buf));
                    return list;
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, List<RecipeData> list) {
                    buf.writeVarInt(list.size());
                    for (RecipeData r : list) RECIPE_CODEC.encode(buf, r);
                }
            };

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenCookRecipePacket> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public OpenCookRecipePacket decode(RegistryFriendlyByteBuf buf) {
                    return new OpenCookRecipePacket(
                            buf.readBlockPos(),
                            LIST_CODEC.decode(buf),
                            buf.readUtf(),
                            buf.readVarInt(),
                            buf.readVarInt()
                    );
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, OpenCookRecipePacket p) {
                    buf.writeBlockPos(p.blockPos());
                    LIST_CODEC.encode(buf, p.recipes());
                    buf.writeUtf(p.activeRecipeId());
                    buf.writeVarInt(p.timerTicks());
                    buf.writeVarInt(p.totalTimerTicks());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    /** Convenience: converts CookRecipe list → RecipeData list and sends to player. */
    public static void sendToPlayer(ServerPlayer player, BlockPos pos,
                                    List<CookRecipe> recipes,
                                    String activeRecipeId, int timerTicks, int totalTimerTicks) {
        List<RecipeData> data = new ArrayList<>(recipes.size());
        for (CookRecipe r : recipes) {
            data.add(new RecipeData(r.recipeId().toString(), r.result(), r.timeTicks()));
        }
        PacketDistributor.sendToPlayer(player,
                new OpenCookRecipePacket(pos, data, activeRecipeId, timerTicks, totalTimerTicks));
    }
}
