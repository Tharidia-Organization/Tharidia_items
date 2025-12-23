package com.THproject.tharidia_things.entity.projectile;

import com.THproject.tharidia_things.entity.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Heavy projectile launched by the Trebuchet entity.
 */
public class TrebuchetProjectileEntity extends ThrowableItemProjectile {

    public static final double GRAVITY = 0.035d;

    private static final float BASE_DAMAGE = 20.0f;
    private static final float EXPLOSION_STRENGTH = 2.5f;

    public TrebuchetProjectileEntity(EntityType<? extends TrebuchetProjectileEntity> type, Level level) {
        super(type, level);
    }

    public TrebuchetProjectileEntity(Level level, LivingEntity shooter) {
        super(ModEntities.TREBUCHET_PROJECTILE.get(), shooter, level);
    }

    public void setAmmoStack(ItemStack stack) {
        ItemStack ammo = stack.isEmpty() ? new ItemStack(getDefaultItem()) : stack.copyWithCount(1);
        setItem(ammo);
    }

    public ItemStack getAmmoStack() {
        return getItem();
    }

    @Override
    protected Item getDefaultItem() {
        return Items.STONE;
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            explodeAt(result);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        spawnImpactEffects();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (result.getEntity() != null) {
            result.getEntity().hurt(damageSources().thrown(this, getOwner()), BASE_DAMAGE);
        }
        spawnImpactEffects();
    }

    private void spawnImpactEffects() {
        if (level().isClientSide) {
            return;
        }
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 0.7f, 0.8f);
        ((net.minecraft.server.level.ServerLevel) level()).sendParticles(
            ParticleTypes.SMOKE,
            getX(), getY(), getZ(),
            12,
            0.2, 0.2, 0.2,
            0.05
        );
    }

    private void explodeAt(HitResult result) {
        if (level().isClientSide) {
            return;
        }
        level().explode(
            this,
            result.getLocation().x,
            result.getLocation().y,
            result.getLocation().z,
            EXPLOSION_STRENGTH,
            Level.ExplosionInteraction.TNT
        );
        discard();
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > 200) {
            discard();
        }
    }

    @Override
    protected double getDefaultGravity() {
        return GRAVITY;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity entity) {
        return new ClientboundAddEntityPacket(this, entity);
    }
}
