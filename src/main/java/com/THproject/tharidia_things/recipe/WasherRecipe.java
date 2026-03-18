package com.THproject.tharidia_things.recipe;

import com.THproject.tharidia_things.TharidiaThings;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

import java.util.List;
import java.util.Random;

public class WasherRecipe implements Recipe<RecipeWrapper> {
    private final Ingredient input;
    private final List<WeightedOutput> outputs;
    private final int processingTime;

    public WasherRecipe(Ingredient input, List<WeightedOutput> outputs, int processingTime) {
        this.input = input;
        this.outputs = outputs;
        this.processingTime = processingTime;
    }

    @Override
    public boolean matches(RecipeWrapper pContainer, Level pLevel) {
        // Slot 0 is input
        return this.input.test(pContainer.getItem(0));
    }

    @Override
    public ItemStack assemble(RecipeWrapper pContainer, HolderLookup.Provider pRegistries) {
        return outputs.isEmpty() ? ItemStack.EMPTY : outputs.get(0).item().copy();
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider pRegistries) {
        if (outputs.isEmpty()) {
            return ItemStack.EMPTY;
        }

        Random random = new Random();
        double totalWeight = 0.0;
        for (WeightedOutput output : outputs) {
            totalWeight += output.chance();
        }

        double randomRange = Math.max(1.0, totalWeight);

        double value = random.nextDouble() * randomRange;

        for (WeightedOutput output : outputs) {
            value -= output.chance();
            if (value <= 0)
                return output.item().copy();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TharidiaThings.WASHER_RECIPE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return TharidiaThings.WASHER_RECIPE_TYPE.get();
    }

    public Ingredient getInput() {
        return input;
    }

    public List<WeightedOutput> getOutputs() {
        return outputs;
    }

    public List<ItemStack> getOutputStacks() {
        return outputs.stream().map(WeightedOutput::item).toList();
    }

    public int getProcessingTime() {
        return processingTime;
    }

    public static record WeightedOutput(ItemStack item, float chance) {
        public static final Codec<WeightedOutput> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ItemStack.STRICT_CODEC.fieldOf("item").forGetter(WeightedOutput::item),
                Codec.FLOAT.fieldOf("chance").orElse(1.0f).forGetter(WeightedOutput::chance))
                .apply(instance, WeightedOutput::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, WeightedOutput> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, WeightedOutput::item,
                net.minecraft.network.codec.ByteBufCodecs.FLOAT, WeightedOutput::chance,
                WeightedOutput::new);
    }

    public static class Serializer implements RecipeSerializer<WasherRecipe> {
        public static final MapCodec<WasherRecipe> CODEC = RecordCodecBuilder.mapCodec(
                instance -> instance.group(
                        Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(WasherRecipe::getInput),
                        WeightedOutput.CODEC.listOf().fieldOf("outputs").forGetter(WasherRecipe::getOutputs),
                        Codec.INT.fieldOf("processingTime").orElse(40).forGetter(WasherRecipe::getProcessingTime))
                        .apply(instance, WasherRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, WasherRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, WasherRecipe::getInput,
                WeightedOutput.STREAM_CODEC.apply(net.minecraft.network.codec.ByteBufCodecs.list()),
                WasherRecipe::getOutputs,
                net.minecraft.network.codec.ByteBufCodecs.INT, WasherRecipe::getProcessingTime,
                WasherRecipe::new);

        @Override
        public MapCodec<WasherRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, WasherRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
