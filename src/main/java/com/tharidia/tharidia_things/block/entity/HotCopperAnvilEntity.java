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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Block entity for hot copper placed on top of an anvil.
 * Tracks hammer strikes and the selected component type.
 */
public class HotCopperAnvilEntity extends BlockEntity implements IHotMetalAnvilEntity {
    
    private int hammerStrikes = 0;
    private String selectedComponent = "lama_lunga"; // Default selection
    private boolean finished = false;
    private boolean guiOpened = false; // Track if GUI has been opened
    
    public HotCopperAnvilEntity(BlockPos pos, BlockState state) {
        super(TharidiaThings.HOT_COPPER_ANVIL_ENTITY.get(), pos, state);
    }
    
    public HotCopperAnvilEntity(BlockPos pos, Level level) {
        this(pos, level.getBlockState(pos));
    }
    
    @Override
    public void onHammerStrike(Player player) {
        if (finished) {
            return;
        }
        
        hammerStrikes++;
        setChanged();
        
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            
            if (hammerStrikes >= 4) {
                finished = true;
                if (player != null) {
                    TharidiaThings.LOGGER.info(player.getName().getString() + " finished forging a copper " + selectedComponent);
                }
                level.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.5F, 1.5F);
            }
            
            if (!level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }
    
    @Override
    public int getHammerStrikes() {
        return hammerStrikes;
    }
    
    @Override
    public String getSelectedComponent() {
        return selectedComponent;
    }
    
    @Override
    public void setSelectedComponent(String component) {
        this.selectedComponent = component;
        setChanged();
        
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    
    @Override
    public boolean isFinished() {
        return finished;
    }
    
    @Override
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
        return "copper";
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
