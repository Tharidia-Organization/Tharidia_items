package com.THproject.tharidia_things.entity;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.entity.animation.TrebuchetAnimationDispatcher;
import com.THproject.tharidia_things.entity.projectile.TrebuchetProjectileEntity;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class TrebuchetEntity extends Entity {

    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(TrebuchetEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> HAS_AMMO = SynchedEntityData.defineId(TrebuchetEntity.class, EntityDataSerializers.BOOLEAN);
    private static final double SEAT_FORWARD_OFFSET = -1.6;
    private static final double SEAT_RIGHT_EXTRA = -1.8;
    private static final double SEAT_HEIGHT_OFFSET = 0.0;

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(1, ItemStack.EMPTY);
    private final TrebuchetAnimationDispatcher animationDispatcher;

    private TrebuchetState cachedState = TrebuchetState.IDLE;
    private int stateTicks = -1;
    private boolean needsStateRefresh = true;

    public TrebuchetEntity(EntityType<? extends TrebuchetEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
        this.animationDispatcher = new TrebuchetAnimationDispatcher(this);
        this.setNoGravity(false);
        this.needsStateRefresh = true;
    }

    public static TrebuchetEntity create(Level level, Vec3 pos, float yaw) {
        TrebuchetEntity trebuchet = new TrebuchetEntity(ModEntities.TREBUCHET.get(), level);
        trebuchet.moveTo(pos.x, pos.y, pos.z, yaw, 0);
        trebuchet.setState(TrebuchetState.IDLE);
        return trebuchet;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STATE, TrebuchetState.IDLE.ordinal());
        builder.define(HAS_AMMO, false);
    }

    @Override
    public void tick() {
        super.tick();

        syncOrientationWithPassenger();

        if (level().isClientSide) {
            TrebuchetState synced = TrebuchetState.fromOrdinal(entityData.get(STATE));
            if (synced != cachedState) {
                cachedState = synced;
            }
            return;
        }

        if (!level().isClientSide && needsStateRefresh) {
            animationDispatcher.reset();
            animationDispatcher.applyState(cachedState);
            needsStateRefresh = false;
        }

        if (!isNoGravity()) {
            Vec3 movement = getDeltaMovement();
            if (!onGround()) {
                movement = movement.add(0.0, -0.08, 0.0);
            } else if (movement.y < 0) {
                movement = new Vec3(movement.x, 0, movement.z);
            }
            move(MoverType.SELF, movement);
            setDeltaMovement(movement.multiply(0.91, 0.98, 0.91));
        }

        if (stateTicks >= 0) {
            stateTicks--;
        }

        switch (cachedState) {
            case LOADING -> {
                if (stateTicks <= 0) {
                    setState(TrebuchetState.LOADED);
                }
            }
            case FIRING -> {
                if (stateTicks <= 0) {
                    setState(TrebuchetState.IDLE);
                }
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        cachedState = TrebuchetState.fromOrdinal(tag.getInt("State"));
        stateTicks = tag.getInt("StateTicks");
        ContainerHelper.loadAllItems(tag, inventory, this.registryAccess());
        entityData.set(STATE, cachedState.ordinal());
        entityData.set(HAS_AMMO, hasAmmo());
        needsStateRefresh = true;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("State", cachedState.ordinal());
        tag.putInt("StateTicks", stateTicks);
        ContainerHelper.saveAllItems(tag, inventory, this.registryAccess());
    }

    @Override
    protected void removeAfterChangingDimensions() {
        dropContents();
        super.removeAfterChangingDimensions();
    }

    public void destroy(net.minecraft.world.damagesource.DamageSource source) {
        dropContents();
        Containers.dropItemStack(level(), getX(), getY() + 0.5, getZ(), new ItemStack(TharidiaThings.TREBUCHET_ITEM.get()));
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || this.isRemoved() || this.isInvulnerableTo(source)) {
            return false;
        }
        destroy(source);
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    private void dropContents() {
        if (level().isClientSide) {
            return;
        }
        for (ItemStack stack : inventory) {
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level(), getX(), getY() + 0.5, getZ(), stack);
            }
        }
        inventory.set(0, ItemStack.EMPTY);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (player.isShiftKeyDown()) {
            pickupTrebuchet(player);
            return InteractionResult.CONSUME;
        }

        boolean isRider = player.isPassengerOfSameVehicle(this);
        ItemStack held = player.getItemInHand(hand);

        if (!held.isEmpty() && !hasAmmo()) {
            loadAmmo(held, player);
            return InteractionResult.CONSUME;
        }

        if (!isRider && !hasSeatOccupied()) {
            if (player.startRiding(this, false)) {
                return InteractionResult.CONSUME;
            }
        }

        if (cachedState == TrebuchetState.LOADED && isRider) {
            beginFiring(player);
            return InteractionResult.CONSUME;
        }

        return InteractionResult.SUCCESS;
    }

    private void pickupTrebuchet(Player player) {
        ejectPassengers();
        ItemStack trebuchetItem = new ItemStack(TharidiaThings.TREBUCHET_ITEM.get());
        if (!player.addItem(trebuchetItem)) {
            Containers.dropItemStack(level(), getX(), getY(), getZ(), trebuchetItem);
        }
        dropContents();
        discard();
    }

    public void loadAmmo(ItemStack held, Player player) {
        ItemStack ammo = held.split(1);
        inventory.set(0, ammo);
        entityData.set(HAS_AMMO, true);
        setState(TrebuchetState.LOADING);
    }

    public void beginFiring(@Nullable Player player) {
        if (!hasAmmo() || (player != null && !player.isPassengerOfSameVehicle(this))) {
            return;
        }
        fireProjectile(player);
        setState(TrebuchetState.FIRING);
    }

    private void fireProjectile(@Nullable Player player) {
        if (!hasAmmo()) {
            return;
        }

        ItemStack ammo = inventory.get(0);
        TrebuchetProjectileEntity projectile = new TrebuchetProjectileEntity(ModEntities.TREBUCHET_PROJECTILE.get(), level());
        projectile.setPos(getX(), getY() + 1.25, getZ());
        if (player != null) {
            projectile.setOwner(player);
        }
        projectile.setAmmoStack(ammo.copy());

        Vec3 direction = Vec3.directionFromRotation(0, getYRot()).normalize();
        projectile.shoot(direction.x, 0.25, direction.z, 1.6f, 0.05f);

        level().addFreshEntity(projectile);
        inventory.set(0, ItemStack.EMPTY);
        entityData.set(HAS_AMMO, false);
    }

    public boolean hasAmmo() {
        return !inventory.get(0).isEmpty();
    }

    private boolean hasSeatOccupied() {
        return !getPassengers().isEmpty();
    }

    @Nullable
    public TrebuchetState getState() {
        return cachedState;
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        Entity passenger = getFirstPassenger();
        return passenger instanceof LivingEntity living ? living : null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty();
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Vec3 dismount = super.getDismountLocationForPassenger(passenger);
        if (dismount != null) {
            return dismount;
        }
        Vec3 backward = Vec3.directionFromRotation(0, getYRot()).scale(-1.2);
        return position().add(backward).add(0, 0.1, 0);
    }

    private void syncOrientationWithPassenger() {
        LivingEntity controller = getControllingPassenger();
        if (controller instanceof Player player) {
            setYRot(player.getYRot());
            setYHeadRot(getYRot());
            setYBodyRot(getYRot());
            setRot(getYRot(), 0);
        }
    }

    private boolean isTrebuchetPassenger(Entity passenger) {
        return passenger != null && getPassengers().contains(passenger);
    }

    private Vec3 calculateSeatPosition() {
        double yawRad = Math.toRadians(this.getYRot());
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);

        double lateral = (getBbWidth() * 0.5) + SEAT_RIGHT_EXTRA;

        double offsetX = (-sin * SEAT_FORWARD_OFFSET) + (cos * lateral);
        double offsetZ = (cos * SEAT_FORWARD_OFFSET) + (sin * lateral);
        double offsetY = SEAT_HEIGHT_OFFSET;

        return new Vec3(getX() + offsetX, getY() + offsetY, getZ() + offsetZ);
    }

    public Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        double yawRad = Math.toRadians(this.getYRot());
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);

        double lateral = (getBbWidth() * 0.5) + SEAT_RIGHT_EXTRA;

        double offsetX = (-sin * SEAT_FORWARD_OFFSET) + (cos * lateral);
        double offsetZ = (cos * SEAT_FORWARD_OFFSET) + (sin * lateral);
        double offsetY = SEAT_HEIGHT_OFFSET;

        return new Vec3(offsetX, offsetY, offsetZ);
    }

    private void alignPassenger(Entity passenger) {
        passenger.setYBodyRot(this.getYRot());
        passenger.setYRot(this.getYRot());
        if (passenger instanceof LivingEntity living) {
            living.setYHeadRot(this.getYRot());
        }
    }

    private void setState(TrebuchetState newState) {
        cachedState = newState;
        entityData.set(STATE, newState.ordinal());
        stateTicks = newState.defaultDuration;
        if (newState == TrebuchetState.IDLE || newState == TrebuchetState.LOADED) {
            stateTicks = -1;
        }
        if (!level().isClientSide) {
            animationDispatcher.applyState(cachedState);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity entity) {
        return new ClientboundAddEntityPacket(this, entity);
    }

    public enum TrebuchetState {
        IDLE(0),
        LOADING(202),
        LOADED(0),
        FIRING(84);

        public final int defaultDuration;

        TrebuchetState(int duration) {
            this.defaultDuration = duration;
        }

        public static TrebuchetState fromOrdinal(int ordinal) {
            TrebuchetState[] values = values();
            if (ordinal < 0 || ordinal >= values.length) {
                return IDLE;
            }
            return values[ordinal];
        }
    }
}
