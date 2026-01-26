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

public class WasherRecipe implements Recipe<RecipeWrapper> {

    private final Ingredient input;
    private final ItemStack output;
    private final int fluidAmount;
    private final int processingTime;

    public WasherRecipe(Ingredient input, ItemStack output, int fluidAmount, int processingTime) {
        this.input = input;
        this.output = output;
        this.fluidAmount = fluidAmount;
        this.processingTime = processingTime;
    }

    @Override
    public boolean matches(RecipeWrapper pContainer, Level pLevel) {
        // Slot 0 is input
        return this.input.test(pContainer.getItem(0));
    }

    @Override
    public ItemStack assemble(RecipeWrapper pContainer, HolderLookup.Provider pRegistries) {
        return this.output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider pRegistries) {
        return this.output;
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

    public int getFluidAmount() {
        return fluidAmount;
    }

    public int getProcessingTime() {
        return processingTime;
    }

    public static class Serializer implements RecipeSerializer<WasherRecipe> {
        public static final MapCodec<WasherRecipe> CODEC = RecordCodecBuilder.mapCodec(
                instance -> instance.group(
                        Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(WasherRecipe::getInput),
                        ItemStack.STRICT_CODEC.fieldOf("output").forGetter(r -> r.output),
                        Codec.INT.fieldOf("fluidAmount").orElse(50).forGetter(WasherRecipe::getFluidAmount),
                        Codec.INT.fieldOf("processingTime").orElse(40).forGetter(WasherRecipe::getProcessingTime))
                        .apply(instance, WasherRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, WasherRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, WasherRecipe::getInput,
                ItemStack.STREAM_CODEC, r -> r.output,
                net.minecraft.network.codec.ByteBufCodecs.INT, WasherRecipe::getFluidAmount,
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
