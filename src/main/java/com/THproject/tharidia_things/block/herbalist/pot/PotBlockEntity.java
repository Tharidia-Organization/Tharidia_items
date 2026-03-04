package com.THproject.tharidia_things.block.herbalist.pot;

import com.THproject.tharidia_things.TharidiaThings;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.util.GeckoLibUtil;

public class PotBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private ItemStack plant = ItemStack.EMPTY;
    private boolean hasDirt = false;
    private boolean isFarmed = false;
    private BlockPos treePos = null;

    public PotBlockEntity(BlockPos pos, BlockState state) {
        super(TharidiaThings.POT_BLOCK_ENTITY.get(), pos, state);
    }

    public void setPlant(ItemStack stack) {
        this.plant = stack.copy();
        this.setChanged();
    }

    public ItemStack removePlant() {
        ItemStack returnStack = this.plant.copy();
        this.plant = ItemStack.EMPTY;
        this.setChanged();
        return returnStack;
    }

    public ItemStack getPlant() {
        return this.plant.copy();
    }

    public boolean hasPlant() {
        return !this.plant.isEmpty();
    }

    public boolean setDirt() {
        if (!this.hasDirt()) {
            this.hasDirt = true;
            this.setChanged();
            return true;
        }
        return false;
    }

    public boolean removeDirt() {
        if (this.hasDirt()) {
            this.removeFarmed();
            this.hasDirt = false;
            this.setChanged();
            return true;
        }
        return false;
    }

    public boolean hasDirt() {
        return this.hasDirt;
    }

    public boolean setFarmed() {
        if (this.hasDirt() && !this.isFarmed()) {
            this.isFarmed = true;
            this.setChanged();
            return true;
        }
        return false;
    }

    public boolean removeFarmed() {
        if (this.hasDirt) {
            this.isFarmed = false;
            this.setChanged();
            return true;
        }
        return false;
    }

    public boolean isFarmed() {
        return this.isFarmed;
    }

    public void setTreePos(BlockPos pos) {
        this.treePos = pos;
        this.setChanged();
    }

    public BlockPos getTreePos() {
        return this.treePos;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.invalidateCapabilities(worldPosition);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("hasDirt", this.hasDirt);
        tag.putBoolean("isFarmed", this.isFarmed);
        if (this.treePos != null)
            tag.putLong("treePos", this.treePos.asLong());
        if (!this.plant.isEmpty())
            tag.put("plant", this.plant.save(registries));
        else
            tag.remove("plant");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.hasDirt = tag.getBoolean("hasDirt");
        this.isFarmed = tag.getBoolean("isFarmed");
        if (tag.contains("treePos"))
            this.treePos = BlockPos.of(tag.getLong("treePos"));
        else
            this.treePos = null;
        if (tag.contains("plant"))
            this.plant = ItemStack.parse(registries, tag.get("plant")).orElse(ItemStack.EMPTY);
        else
            this.plant = ItemStack.EMPTY;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt,
            HolderLookup.Provider lookupProvider) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            handleUpdateTag(tag, lookupProvider);
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        super.loadAdditional(tag, lookupProvider);
        loadAdditional(tag, lookupProvider);
    }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {
        return;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
