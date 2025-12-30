package com.THproject.tharidia_things.entity;

import com.THproject.tharidia_things.TharidiaThings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Physics-enabled dice entity that bounces and settles on a random face.
 */
public class DiceEntity extends Entity {
    private static final EntityDataAccessor<Boolean> SETTLED =
        SynchedEntityData.defineId(DiceEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> FACE =
        SynchedEntityData.defineId(DiceEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> ITEM =
        SynchedEntityData.defineId(DiceEntity.class, EntityDataSerializers.ITEM_STACK);

    private static final double MIN_SETTLE_SPEED_SQR = 0.001d;
    private static final double BOUNCE_DAMPING = 0.5d;
    private static final double GROUND_FRICTION = 0.8d;
    private static final double AIR_RESISTANCE = 0.99d;
    private static final float GRAVITY = 0.04F;
    private static final int MAX_LIFETIME_TICKS = 20 * 30;

    private int ageTicks;
    private int bounceCount;
    private boolean hasBouncedOnce = false;

    public DiceEntity(EntityType<? extends DiceEntity> type, Level level) {
        super(type, level);
        this.setItem(TharidiaThings.DICE.get().getDefaultInstance());
        this.blocksBuilding = true;
    }

    public DiceEntity(Level level, LivingEntity owner, Vec3 position) {
        this(ModEntities.DICE.get(), level);
        this.setPos(position);
    }

    public void shoot(Vec3 direction, float velocity) {
        Vec3 motion = direction.normalize().scale(velocity);
        this.setDeltaMovement(motion);
        double horizontalDistance = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        this.setYRot((float)(Mth.atan2(motion.x, motion.z) * (180.0 / Math.PI)));
        this.setXRot((float)(Mth.atan2(motion.y, horizontalDistance) * (180.0 / Math.PI)));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    public void randomizeInitialSpin() {
        float yaw = this.random.nextFloat() * 360.0F;
        float pitch = (this.random.nextFloat() * 120.0F) - 60.0F;
        this.setYRot(yaw);
        this.setXRot(pitch);
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    public void setItem(ItemStack stack) {
        this.getEntityData().set(ITEM, stack.copyWithCount(1));
    }

    public ItemStack getItem() {
        return this.getEntityData().get(ITEM);
    }


    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SETTLED, false);
        builder.define(FACE, 1);
        builder.define(ITEM, TharidiaThings.DICE.get().getDefaultInstance());
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(0.25F, 0.25F);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.getEntityData().set(SETTLED, tag.getBoolean("Settled"));
        this.getEntityData().set(FACE, Mth.clamp(tag.getInt("Face"), 1, 6));
        this.ageTicks = tag.getInt("AgeTicks");
        this.bounceCount = tag.getInt("BounceCount");
        this.hasBouncedOnce = tag.getBoolean("HasBouncedOnce");
        this.getEntityData().set(ITEM, ItemStack.parseOptional(level().registryAccess(), tag.getCompound("Item")));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("Settled", isSettled());
        tag.putInt("Face", getFace());
        tag.putInt("AgeTicks", ageTicks);
        tag.putInt("BounceCount", bounceCount);
        tag.putBoolean("HasBouncedOnce", hasBouncedOnce);
        tag.put("Item", getItem().saveOptional(level().registryAccess()));
    }

    @Override
    public void tick() {
        if (isSettled() && !level().isClientSide) {
            ageTicks++;
            if (ageTicks >= MAX_LIFETIME_TICKS) {
                dropAsItem();
            }
            return;
        }
        
        super.tick();
        ageTicks++;
        
        if (level().isClientSide) {
            if (isSettled()) {
                this.setDeltaMovement(Vec3.ZERO);
            } else {
                updateRotation();
            }
            return;
        }

        Vec3 motion = this.getDeltaMovement();
        
        // Apply gravity
        if (!this.isNoGravity()) {
            motion = motion.add(0.0, -GRAVITY, 0.0);
        }
        
        // Apply air resistance
        motion = motion.multiply(AIR_RESISTANCE, 0.98, AIR_RESISTANCE);
        
        // Store motion before move for bounce detection
        Vec3 preMotion = motion;
        this.setDeltaMovement(motion);
        
        // Move and handle collisions
        this.move(MoverType.SELF, motion);
        
        // Check for collisions and bounce
        boolean collided = false;
        Vec3 newMotion = this.getDeltaMovement();
        
        if (this.horizontalCollision) {
            newMotion = handleHorizontalBounce(preMotion, newMotion);
            collided = true;
        }
        
        if (this.verticalCollision) {
            newMotion = handleVerticalBounce(preMotion, newMotion);
            collided = true;
        }
        
        if (collided) {
            this.setDeltaMovement(newMotion);
            bounceCount++;
            if (!level().isClientSide) {
                level().playSound(null, blockPosition(), SoundEvents.STONE_HIT, SoundSource.BLOCKS, 
                    0.3F, 0.8F + random.nextFloat() * 0.4F);
            }
        }
        
        // Apply ground friction
        if (this.onGround()) {
            Vec3 groundMotion = this.getDeltaMovement();
            groundMotion = new Vec3(groundMotion.x * GROUND_FRICTION, groundMotion.y, groundMotion.z * GROUND_FRICTION);
            this.setDeltaMovement(groundMotion);
            
            // Check if should settle
            if (groundMotion.horizontalDistanceSqr() < MIN_SETTLE_SPEED_SQR && Math.abs(groundMotion.y) < 0.01) {
                settle();
            }
        }
        
        updateRotation();
    }
    
    private void updateRotation() {
        if (!isSettled()) {
            Vec3 motion = this.getDeltaMovement();
            double speed = motion.length();
            this.setXRot((this.getXRot() + (float)(speed * 120)) % 360F);
            this.setYRot((this.getYRot() + (float)(speed * 80)) % 360F);
        }
    }
    
    private Vec3 handleHorizontalBounce(Vec3 preMotion, Vec3 currentMotion) {
        hasBouncedOnce = true;
        double newX = currentMotion.x;
        double newZ = currentMotion.z;
        
        // Reverse horizontal motion with damping
        if (Math.abs(preMotion.x) > Math.abs(currentMotion.x)) {
            newX = -preMotion.x * BOUNCE_DAMPING;
        }
        if (Math.abs(preMotion.z) > Math.abs(currentMotion.z)) {
            newZ = -preMotion.z * BOUNCE_DAMPING;
        }
        
        return new Vec3(newX, currentMotion.y, newZ);
    }
    
    private Vec3 handleVerticalBounce(Vec3 preMotion, Vec3 currentMotion) {
        hasBouncedOnce = true;
        double newY = currentMotion.y;
        
        // Bounce up from ground
        if (preMotion.y < 0 && this.onGround()) {
            newY = -preMotion.y * BOUNCE_DAMPING;
            // Reduce bounce after first bounce
            if (bounceCount > 0) {
                newY *= 0.6;
            }
        }
        // Bounce down from ceiling
        else if (preMotion.y > 0) {
            newY = -preMotion.y * BOUNCE_DAMPING * 0.5;
        }
        
        return new Vec3(currentMotion.x, newY, currentMotion.z);
    }


    private void settle() {
        if (isSettled()) {
            return;
        }

        int face = 1 + random.nextInt(6);
        this.getEntityData().set(FACE, face);
        this.getEntityData().set(SETTLED, true);
        this.setDeltaMovement(Vec3.ZERO);
        this.setNoGravity(true);
        
        double groundY = Math.floor(this.getY()) - 0.085;
        this.setPos(this.getX(), groundY, this.getZ());
        
        if (!level().isClientSide) {
            level().playSound(null, blockPosition(), SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.6F, 0.9F);
        }
    }

    private void dropAsItem() {
        if (!level().isClientSide) {
            ItemStack stack = getItem();
            if (stack.isEmpty()) {
                stack = TharidiaThings.DICE.get().getDefaultInstance();
            }
            spawnAtLocation(stack);
            discard();
        }
    }


    public boolean isSettled() {
        return this.getEntityData().get(SETTLED);
    }
    
    @Override
    public boolean shouldBeSaved() {
        return !isRemoved();
    }
    
    @Override
    public boolean isAttackable() {
        return false;
    }

    public int getFace() {
        return this.getEntityData().get(FACE);
    }

    @Override
    public boolean isPickable() {
        return isSettled();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (isSettled() && !level().isClientSide) {
            ItemStack stack = getItem();
            if (stack.isEmpty()) {
                stack = TharidiaThings.DICE.get().getDefaultInstance();
            }

            if (!player.addItem(stack)) {
                player.drop(stack, false);
            }
            discard();
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

}
