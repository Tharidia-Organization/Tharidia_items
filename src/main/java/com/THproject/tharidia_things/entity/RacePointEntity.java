package com.THproject.tharidia_things.entity;

import com.THproject.tharidia_things.network.OpenRaceGuiPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Floating race selection point entity
 */
public class RacePointEntity extends Entity {
    private static final EntityDataAccessor<String> RACE_NAME = SynchedEntityData.defineId(RacePointEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(RacePointEntity.class, EntityDataSerializers.INT);
    
    private float animationTime = 0;
    private float baseY;
    private final float bobAmount = 0.5f;
    private final float bobSpeed = 0.05f;
    
    public RacePointEntity(EntityType<RacePointEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.baseY = (float) getY();
        // Ensure bounding box is set
        this.setBoundingBox(this.getBoundingBox());
    }
    
    public RacePointEntity(Level level, double x, double y, double z, String raceName, int color) {
        super(ModEntities.RACE_POINT.get(), level);
        this.setPos(x, y, z);
        this.baseY = (float) y;
        this.getEntityData().set(RACE_NAME, raceName);
        this.getEntityData().set(COLOR, color);
        this.noPhysics = true;
        // Ensure bounding box is set
        this.setBoundingBox(this.getBoundingBox());
    }
    
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RACE_NAME, "Umani");
        builder.define(COLOR, 0xFFFFFF);
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (!level().isClientSide) {
            // Spawn particles around the point
            if (this.tickCount % 5 == 0) {
                double angle = this.tickCount * 0.1;
                double particleX = getX() + Math.cos(angle) * 0.5;
                double particleZ = getZ() + Math.sin(angle) * 0.5;
                double particleY = getY() + Math.sin(this.tickCount * 0.05) * 0.3;
                
                ((net.minecraft.server.level.ServerLevel)level()).sendParticles(
                    ParticleTypes.ENCHANT,
                    particleX, particleY, particleZ,
                    1, 0, 0, 0, 0
                );
            }
        } else {
            // Client-side animation - don't modify position, just track animation time
            animationTime += bobSpeed;
            
            // Rotate slowly
            this.setYRot((this.getYRot() + 1) % 360);
        }
    }
    
    @Override
    public boolean isPickable() {
        return true;
    }
    
    @Override
    public boolean isPushable() {
        return false;
    }
    
    protected void doPush(Entity entity) {}
    
    protected void pushEntities() {}
    
    @Override
    public InteractionResult interact(Player player, net.minecraft.world.InteractionHand hand) {
        if (!level().isClientSide && hand == net.minecraft.world.InteractionHand.MAIN_HAND) {
            String raceName = getRaceName();
            
            // Send packet to open GUI on client
            PacketDistributor.sendToPlayer((net.minecraft.server.level.ServerPlayer) player, 
                new OpenRaceGuiPacket(raceName));
            
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
    
    public String getRaceName() {
        return this.getEntityData().get(RACE_NAME);
    }
    
    public int getColor() {
        return this.getEntityData().get(COLOR);
    }
    
    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        this.getEntityData().set(RACE_NAME, compound.getString("RaceName"));
        this.getEntityData().set(COLOR, compound.getInt("Color"));
        this.baseY = compound.getFloat("BaseY");
    }
    
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putString("RaceName", getRaceName());
        compound.putInt("Color", getColor());
        compound.putFloat("BaseY", this.baseY);
    }
}
