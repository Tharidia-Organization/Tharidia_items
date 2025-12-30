package com.THproject.tharidia_things.block.entity;

import com.THproject.tharidia_things.TharidiaThings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class StableBlockEntity extends BlockEntity {
    
    private String animalType = ""; // "cow" or "chicken"
    private boolean isBaby = false;
    private int growthTicks = 0;
    private int eggCount = 0;
    private int eggProductionTicks = 0;
    private boolean milkCollected = false;
    
    private static final int GROWTH_TIME = 20 * 60 * 2; // 2 minutes
    private static final int EGG_PRODUCTION_TIME = 20 * 30; // 30 seconds
    private static final int MAX_EGGS = 3;
    
    public StableBlockEntity(BlockPos pos, BlockState state) {
        super(TharidiaThings.STABLE_BLOCK_ENTITY.get(), pos, state);
    }
    
    public boolean hasAnimal() {
        return !animalType.isEmpty();
    }
    
    public String getAnimalType() {
        return animalType;
    }
    
    public boolean isBaby() {
        return isBaby;
    }
    
    public int getEggCount() {
        return eggCount;
    }
    
    public boolean placeAnimal(String type) {
        if (hasAnimal()) {
            return false;
        }
        
        this.animalType = type;
        this.isBaby = true;
        this.growthTicks = 0;
        this.eggCount = 0;
        this.eggProductionTicks = 0;
        this.milkCollected = false;
        
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.playSound(null, worldPosition, SoundEvents.CHICKEN_EGG, SoundSource.BLOCKS, 0.5F, 1.0F);
        }
        return true;
    }
    
    public boolean canCollectMilk() {
        return animalType.equals("cow") && !isBaby && !milkCollected;
    }
    
    public void collectMilk(Player player) {
        if (!canCollectMilk()) {
            return;
        }
        
        milkCollected = true;
        
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.COW_MILK, SoundSource.BLOCKS, 1.0F, 1.0F);
            
            // Drop loot: 1 beef, 2 leather
            dropLoot(player, new ItemStack(Items.BEEF, 1));
            dropLoot(player, new ItemStack(Items.LEATHER, 2));
        }
        
        // Remove animal
        clearAnimal();
    }
    
    public boolean canCollectEggs() {
        return animalType.equals("chicken") && !isBaby && eggCount > 0;
    }
    
    public void collectEggs(Player player) {
        if (!canCollectEggs()) {
            return;
        }
        
        int eggs = eggCount;
        eggCount = 0;
        
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.CHICKEN_EGG, SoundSource.BLOCKS, 1.0F, 1.0F);
            
            // Give eggs
            player.addItem(new ItemStack(Items.EGG, eggs));
            
            // Check if reached max production
            if (eggs == MAX_EGGS) {
                // Drop loot: 1 chicken, 1 feather
                dropLoot(player, new ItemStack(Items.CHICKEN, 1));
                dropLoot(player, new ItemStack(Items.FEATHER, 1));
                
                // Remove animal
                clearAnimal();
                return;
            }
        }
        
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    
    private void dropLoot(Player player, ItemStack stack) {
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }
    
    private void clearAnimal() {
        animalType = "";
        isBaby = false;
        growthTicks = 0;
        eggCount = 0;
        eggProductionTicks = 0;
        milkCollected = false;
        
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    
    public static void serverTick(Level level, BlockPos pos, BlockState state, StableBlockEntity entity) {
        if (!entity.hasAnimal()) {
            return;
        }
        
        // Handle growth
        if (entity.isBaby) {
            entity.growthTicks++;
            if (entity.growthTicks >= GROWTH_TIME) {
                entity.isBaby = false;
                entity.growthTicks = 0;
                entity.setChanged();
                level.sendBlockUpdated(pos, state, state, 3);
                level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.5F, 1.5F);
            }
        }
        
        // Handle egg production for grown chickens
        if (entity.animalType.equals("chicken") && !entity.isBaby && entity.eggCount < MAX_EGGS) {
            entity.eggProductionTicks++;
            if (entity.eggProductionTicks >= EGG_PRODUCTION_TIME) {
                entity.eggCount++;
                entity.eggProductionTicks = 0;
                entity.setChanged();
                level.sendBlockUpdated(pos, state, state, 3);
                level.playSound(null, pos, SoundEvents.CHICKEN_EGG, SoundSource.BLOCKS, 0.8F, 1.0F);
            }
        }
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("AnimalType", animalType);
        tag.putBoolean("IsBaby", isBaby);
        tag.putInt("GrowthTicks", growthTicks);
        tag.putInt("EggCount", eggCount);
        tag.putInt("EggProductionTicks", eggProductionTicks);
        tag.putBoolean("MilkCollected", milkCollected);
    }
    
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        animalType = tag.getString("AnimalType");
        isBaby = tag.getBoolean("IsBaby");
        growthTicks = tag.getInt("GrowthTicks");
        eggCount = tag.getInt("EggCount");
        eggProductionTicks = tag.getInt("EggProductionTicks");
        milkCollected = tag.getBoolean("MilkCollected");
    }
    
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }
    
    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
