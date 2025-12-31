package com.THproject.tharidia_things.block.entity;

import com.THproject.tharidia_things.TharidiaThings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class StableBlockEntity extends BlockEntity {
    
    private static final int MAX_ANIMALS = 3;
    private static final int FEED_REQUIRED = 3;
    private static final int GROWTH_TIME = 20 * 60 * 2; // 2 minutes
    private static final int EGG_PRODUCTION_TIME = 20 * 30; // 30 seconds
    private static final int MAX_EGGS_PER_CHICKEN = 3;
    private static final int WATER_DURATION = 20 * 60 * 10; // 10 minutes
    private static final int MAX_FOOD_ITEMS = 64; // Maximum food items in feeder
    private static final int FOOD_CONSUMPTION_RATE = 20 * 10; // Consume 1 food every 10 seconds when animals present
    private static final int FEED_USES_REQUIRED = 5; // Number of animal feed uses to fill feeder
    
    private final List<AnimalData> animals = new ArrayList<>();
    private int waterTicks = 0; // Time remaining for water
    private int foodAmount = 0; // Amount of food in feeder (0-64)
    private int foodConsumptionTicks = 0; // Ticks until next food consumption
    private int feedUses = 0; // Number of times animal feed has been used (0-5)
    
    public static class AnimalData {
        public EntityType<?> entityType;
        public boolean isBaby;
        public int growthTicks; // Time until animal grows up
        public int feedCount; // How much food has been given (for breeding)
        public int eggCount; // Current eggs available for collection
        public int totalEggsProduced; // Total eggs produced in lifetime (max 3)
        public int eggProductionTicks;
        public boolean resourceCollected; // Milk collected or final eggs collected
        
        public AnimalData(EntityType<?> entityType) {
            this.entityType = entityType;
            this.isBaby = true;
            this.growthTicks = 0;
            this.feedCount = 0;
            this.eggCount = 0;
            this.totalEggsProduced = 0;
            this.eggProductionTicks = 0;
            this.resourceCollected = false;
        }
        
        public AnimalData() {
            this(EntityType.PIG);
        }
        
        public void save(CompoundTag tag) {
            tag.putString("EntityType", BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString());
            tag.putBoolean("IsBaby", isBaby);
            tag.putInt("GrowthTicks", growthTicks);
            tag.putInt("FeedCount", feedCount);
            tag.putInt("EggCount", eggCount);
            tag.putInt("TotalEggsProduced", totalEggsProduced);
            tag.putInt("EggProductionTicks", eggProductionTicks);
            tag.putBoolean("ResourceCollected", resourceCollected);
        }
        
        public void load(CompoundTag tag) {
            ResourceLocation entityId = ResourceLocation.parse(tag.getString("EntityType"));
            entityType = BuiltInRegistries.ENTITY_TYPE.get(entityId);
            isBaby = tag.getBoolean("IsBaby");
            growthTicks = tag.getInt("GrowthTicks");
            feedCount = tag.getInt("FeedCount");
            eggCount = tag.getInt("EggCount");
            totalEggsProduced = tag.getInt("TotalEggsProduced");
            eggProductionTicks = tag.getInt("EggProductionTicks");
            resourceCollected = tag.getBoolean("ResourceCollected");
        }
    }
    
    public StableBlockEntity(BlockPos pos, BlockState state) {
        super(TharidiaThings.STABLE_BLOCK_ENTITY.get(), pos, state);
    }
    
    public boolean hasAnimal() {
        return !animals.isEmpty();
    }
    
    public List<AnimalData> getAnimals() {
        return animals;
    }
    
    public EntityType<?> getAnimalType() {
        return animals.isEmpty() ? null : animals.get(0).entityType;
    }
    
    public boolean isBaby() {
        return animals.isEmpty() ? false : animals.get(0).isBaby;
    }
    
    public int getEggCount() {
        return animals.isEmpty() ? 0 : animals.get(0).eggCount;
    }
    
    public boolean hasWater() {
        return waterTicks > 0;
    }
    
    public boolean canRefillWater() {
        return waterTicks == 0;
    }
    
    public void refillWater() {
        waterTicks = WATER_DURATION;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }
    
    public int getFoodAmount() {
        return foodAmount;
    }
    
    public boolean canAddAnimalFeed() {
        return feedUses < FEED_USES_REQUIRED;
    }
    
    public void addAnimalFeed() {
        if (feedUses < FEED_USES_REQUIRED) {
            feedUses++;
            if (feedUses >= FEED_USES_REQUIRED) {
                // Fill feeder completely when 5 uses reached
                foodAmount = MAX_FOOD_ITEMS;
                feedUses = 0; // Reset uses
            }
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                level.playSound(null, worldPosition, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }
    }
    
    public boolean placeAnimal(EntityType<?> entityType) {
        if (animals.size() >= 2) {
            return false;
        }
        
        if (!animals.isEmpty() && !animals.get(0).entityType.equals(entityType)) {
            return false;
        }
        
        animals.add(new AnimalData(entityType));
        
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.playSound(null, worldPosition, SoundEvents.CHICKEN_EGG, SoundSource.BLOCKS, 0.5F, 1.0F);
        }
        return true;
    }
    
    public boolean canFeed(ItemStack stack) {
        if (animals.size() != 2) {
            return false;
        }
        
        EntityType<?> entityType = animals.get(0).entityType;
        
        // Check if both animals are adults
        boolean bothAdult = true;
        for (AnimalData animal : animals) {
            if (animal.isBaby) {
                bothAdult = false;
                break;
            }
        }
        
        if (!bothAdult) {
            return false;
        }
        
        // Check if correct food type and not fully fed
        if (entityType == EntityType.COW && stack.is(Items.WHEAT)) {
            for (AnimalData animal : animals) {
                if (animal.feedCount < FEED_REQUIRED) {
                    return true;
                }
            }
        } else if (entityType == EntityType.CHICKEN && stack.is(Items.WHEAT_SEEDS)) {
            for (AnimalData animal : animals) {
                if (animal.feedCount < FEED_REQUIRED) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    public void feed(ItemStack stack) {
        if (!canFeed(stack)) {
            return;
        }
        
        // Feed the first animal that needs food
        for (AnimalData animal : animals) {
            if (!animal.isBaby && animal.feedCount < FEED_REQUIRED) {
                animal.feedCount++;
                
                if (level != null) {
                    level.playSound(null, worldPosition, SoundEvents.GENERIC_EAT, SoundSource.BLOCKS, 0.5F, 1.0F);
                }
                
                // Check if both animals are fully fed for breeding
                checkBreeding();
                
                setChanged();
                if (level != null) {
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }
                break;
            }
        }
    }
    
    private void checkBreeding() {
        if (animals.size() != 2 || animals.size() >= MAX_ANIMALS) {
            return;
        }
        
        // Check if both animals are adults and fully fed
        boolean bothAdultAndFed = true;
        for (AnimalData animal : animals) {
            if (animal.isBaby || animal.feedCount < FEED_REQUIRED) {
                bothAdultAndFed = false;
                break;
            }
        }
        
        if (bothAdultAndFed) {
            // Reset feed counts
            for (AnimalData animal : animals) {
                animal.feedCount = 0;
            }
            
            // Create baby
            animals.add(new AnimalData(animals.get(0).entityType));
            
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                level.playSound(null, worldPosition, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.0F, 2.0F);
            }
        }
    }
    
    public boolean canCollectMilk() {
        if (animals.isEmpty() || animals.get(0).entityType != EntityType.COW) {
            return false;
        }
        
        for (AnimalData animal : animals) {
            if (!animal.isBaby && !animal.resourceCollected) {
                return true;
            }
        }
        
        return false;
    }
    
    public void collectMilk(Player player) {
        if (!canCollectMilk()) {
            return;
        }
        
        for (int i = 0; i < animals.size(); i++) {
            AnimalData animal = animals.get(i);
            if (!animal.isBaby && !animal.resourceCollected) {
                animal.resourceCollected = true;
                
                if (level != null) {
                    level.playSound(null, worldPosition, SoundEvents.COW_MILK, SoundSource.BLOCKS, 1.0F, 1.0F);
                    
                    dropLoot(player, new ItemStack(Items.BEEF, 1));
                    dropLoot(player, new ItemStack(Items.LEATHER, 2));
                }
                
                animals.remove(i);
                
                setChanged();
                if (level != null) {
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }
                break;
            }
        }
    }
    
    public boolean canCollectEggs() {
        if (animals.isEmpty() || animals.get(0).entityType != EntityType.CHICKEN) {
            return false;
        }
        
        for (AnimalData animal : animals) {
            if (!animal.isBaby && animal.eggCount > 0) {
                return true;
            }
        }
        
        return false;
    }
    
    public void collectEggs(Player player) {
        if (!canCollectEggs()) {
            return;
        }
        
        for (int i = 0; i < animals.size(); i++) {
            AnimalData animal = animals.get(i);
            if (!animal.isBaby && animal.eggCount > 0) {
                int eggs = animal.eggCount;
                animal.eggCount = 0;
                animal.eggProductionTicks = 0;
                
                if (level != null) {
                    level.playSound(null, worldPosition, SoundEvents.CHICKEN_EGG, SoundSource.BLOCKS, 1.0F, 1.0F);
                    
                    player.addItem(new ItemStack(Items.EGG, eggs));
                    
                    // Check if chicken has produced 3 eggs total in its lifetime
                    if (animal.totalEggsProduced >= MAX_EGGS_PER_CHICKEN) {
                        dropLoot(player, new ItemStack(Items.CHICKEN, 1));
                        dropLoot(player, new ItemStack(Items.FEATHER, 1));
                        
                        animals.remove(i);
                    }
                }
                
                setChanged();
                if (level != null) {
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }
                break;
            }
        }
    }
    
    private void dropLoot(Player player, ItemStack stack) {
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }
    
    public static void serverTick(Level level, BlockPos pos, BlockState state, StableBlockEntity entity) {
        boolean changed = false;
        
        // Handle water consumption only if there are animals
        if (!entity.animals.isEmpty() && entity.waterTicks > 0) {
            entity.waterTicks--;
            if (entity.waterTicks == 0) {
                changed = true;
            }
        }
        
        // Handle food consumption only if there are animals
        if (!entity.animals.isEmpty() && entity.foodAmount > 0) {
            entity.foodConsumptionTicks++;
            if (entity.foodConsumptionTicks >= FOOD_CONSUMPTION_RATE) {
                entity.foodAmount--;
                entity.foodConsumptionTicks = 0;
                changed = true;
            }
        }
        
        if (entity.animals.isEmpty()) {
            if (changed) {
                entity.setChanged();
                level.sendBlockUpdated(pos, state, state, 3);
            }
            return;
        }
        
        // Only allow growth and production if there is water and food
        boolean hasWater = entity.waterTicks > 0;
        boolean hasFood = entity.foodAmount > 0;
        
        for (AnimalData animal : entity.animals) {
            // Handle growth for baby animals (only with water and food)
            if (animal.isBaby && hasWater && hasFood) {
                animal.growthTicks++;
                if (animal.growthTicks >= GROWTH_TIME) {
                    animal.isBaby = false;
                    animal.growthTicks = 0;
                    changed = true;
                    level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.5F, 1.5F);
                }
            }
            
            // Handle egg production for adult chickens (max 3 eggs total in lifetime, only with water and food)
            if (animal.entityType == EntityType.CHICKEN && !animal.isBaby && animal.totalEggsProduced < MAX_EGGS_PER_CHICKEN && hasWater && hasFood) {
                animal.eggProductionTicks++;
                if (animal.eggProductionTicks >= EGG_PRODUCTION_TIME) {
                    animal.eggCount++;
                    animal.totalEggsProduced++;
                    animal.eggProductionTicks = 0;
                    changed = true;
                    level.playSound(null, pos, SoundEvents.CHICKEN_EGG, SoundSource.BLOCKS, 0.8F, 1.0F);
                }
            }
        }
        
        if (changed) {
            entity.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        
        ListTag animalList = new ListTag();
        for (AnimalData animal : animals) {
            CompoundTag animalTag = new CompoundTag();
            animal.save(animalTag);
            animalList.add(animalTag);
        }
        tag.put("Animals", animalList);
        tag.putInt("WaterTicks", waterTicks);
        tag.putInt("FoodAmount", foodAmount);
        tag.putInt("FoodConsumptionTicks", foodConsumptionTicks);
        tag.putInt("FeedUses", feedUses);
    }
    
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        
        animals.clear();
        ListTag animalList = tag.getList("Animals", Tag.TAG_COMPOUND);
        for (int i = 0; i < animalList.size(); i++) {
            CompoundTag animalTag = animalList.getCompound(i);
            AnimalData animal = new AnimalData();
            animal.load(animalTag);
            animals.add(animal);
        }
        waterTicks = tag.getInt("WaterTicks");
        foodAmount = tag.getInt("FoodAmount");
        foodConsumptionTicks = tag.getInt("FoodConsumptionTicks");
        feedUses = tag.getInt("FeedUses");
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
