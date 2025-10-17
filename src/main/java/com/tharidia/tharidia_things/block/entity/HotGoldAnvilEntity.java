package com.tharidia.tharidia_things.block.entity;

import com.tharidia.tharidia_things.TharidiaThings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Block entity for hot gold placed on top of an anvil.
 * Tracks hammer strikes and the selected component type.
 */
public class HotGoldAnvilEntity extends BlockEntity implements IHotMetalAnvilEntity {
    
    private int hammerStrikes = 0;
    private String selectedComponent = "lama_lunga"; // Default selection
    private boolean finished = false;
    private boolean guiOpened = false; // Track if GUI has been opened
    
    public HotGoldAnvilEntity(BlockPos pos, BlockState state) {
        super(TharidiaThings.HOT_GOLD_ANVIL_ENTITY.get(), pos, state);
    }
    
    // Constructor for when creating without BlockState
    public HotGoldAnvilEntity(BlockPos pos, Level level) {
        this(pos, level.getBlockState(pos));
    }
    
    public void onHammerStrike(net.minecraft.world.entity.player.Player player) {
        if (finished) {
            TharidiaThings.LOGGER.debug("Already finished");
            return;
        }
        
        hammerStrikes++;
        setChanged();
        
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            
            if (hammerStrikes >= 4) {
                finished = true;
                if (player != null) {
                    TharidiaThings.LOGGER.info(player.getName().getString() + " forged a gold piece");
                }
                level.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.5F, 1.5F);
            }
            
            // Sync to client
            if (!level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }
    
    public int getHammerStrikes() {
        return hammerStrikes;
    }
    
    public String getSelectedComponent() {
        return selectedComponent;
    }
    
    public void setSelectedComponent(String component) {
        this.selectedComponent = component;
        setChanged();
        
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    
    public boolean isFinished() {
        return finished;
    }
    
    public float getProgress() {
        return hammerStrikes / 4.0f;
    }
    
    public boolean hasGuiBeenOpened() {
        return guiOpened;
    }
    
    public void setGuiOpened(boolean opened) {
        this.guiOpened = opened;
        setChanged();
        
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    
    @Override
    public String getMaterialType() {
        return "gold";
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("HammerStrikes", hammerStrikes);
        tag.putString("SelectedComponent", selectedComponent);
        tag.putBoolean("Finished", finished);
        tag.putBoolean("GuiOpened", guiOpened);
    }
    
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        hammerStrikes = tag.getInt("HammerStrikes");
        selectedComponent = tag.getString("SelectedComponent");
        finished = tag.getBoolean("Finished");
        guiOpened = tag.getBoolean("GuiOpened");
    }
    
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }
    
    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
