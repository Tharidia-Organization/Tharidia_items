package com.THproject.tharidia_things.entity;

import com.THproject.tharidia_things.network.OpenRaceGuiPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Floating race selection point entity with bob animation and particle effects.
 */
public class RacePointEntity extends Entity {
    private static final EntityDataAccessor<String> RACE_NAME =
            SynchedEntityData.defineId(RacePointEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> COLOR =
            SynchedEntityData.defineId(RacePointEntity.class, EntityDataSerializers.INT);

    private float animationTime = 0;
    private double baseY;
    private static final float BOB_AMOUNT = 0.3f;
    private static final float BOB_SPEED = 0.08f;
    private static final float ROTATION_SPEED = 1.5f;

    public RacePointEntity(EntityType<RacePointEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.baseY = getY();
    }

    public RacePointEntity(Level level, double x, double y, double z, String raceName, int color) {
        super(ModEntities.RACE_POINT.get(), level);
        this.setPos(x, y, z);
        this.baseY = y;
        this.getEntityData().set(RACE_NAME, raceName);
        this.getEntityData().set(COLOR, color);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RACE_NAME, "Umani");
        builder.define(COLOR, 0xFFFFFF);
    }

    @Override
    public void tick() {
        super.tick();

        // Update animation time
        animationTime += BOB_SPEED;

        if (!level().isClientSide) {
            // Server-side: spawn particles and update position
            if (this.tickCount % 5 == 0) {
                double angle = this.tickCount * 0.1;
                double particleX = getX() + Math.cos(angle) * 0.5;
                double particleZ = getZ() + Math.sin(angle) * 0.5;
                double particleY = getY() + Math.sin(this.tickCount * 0.05) * 0.3;

                ((ServerLevel) level()).sendParticles(
                        ParticleTypes.ENCHANT,
                        particleX, particleY, particleZ,
                        1, 0, 0, 0, 0
                );
            }

            // Apply bob animation - update actual Y position
            double newY = baseY + Math.sin(animationTime) * BOB_AMOUNT;
            this.setPos(getX(), newY, getZ());
        }

        // Both sides: rotate slowly
        this.setYRot((this.getYRot() + ROTATION_SPEED) % 360);
    }

    /**
     * Get the current bob offset for rendering (client-side interpolation)
     */
    public float getBobOffset(float partialTick) {
        return (float) Math.sin(animationTime + partialTick * BOB_SPEED) * BOB_AMOUNT;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    protected void doPush(Entity entity) {
        // Don't push other entities
    }

    protected void pushEntities() {
        // Don't push other entities
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!level().isClientSide && hand == InteractionHand.MAIN_HAND) {
            String raceName = getRaceName();

            // Send packet to open GUI on client
            if (player instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayer(serverPlayer, new OpenRaceGuiPacket(raceName));
            }

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

    public double getBaseY() {
        return this.baseY;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        if (compound.contains("RaceName")) {
            this.getEntityData().set(RACE_NAME, compound.getString("RaceName"));
        }
        if (compound.contains("Color")) {
            this.getEntityData().set(COLOR, compound.getInt("Color"));
        }
        if (compound.contains("BaseY")) {
            this.baseY = compound.getDouble("BaseY");
        } else {
            this.baseY = getY();
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putString("RaceName", getRaceName());
        compound.putInt("Color", getColor());
        compound.putDouble("BaseY", this.baseY);
    }
}
