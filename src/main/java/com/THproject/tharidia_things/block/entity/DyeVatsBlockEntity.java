package com.THproject.tharidia_things.block.entity;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.DyeVatsBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class DyeVatsBlockEntity extends BlockEntity {

    // Light water color (visual indicator only, conceptually colorless)
    public static final int DEFAULT_WATER_COLOR = 0x87CEEB;
    // How much each dye addition blends (5%)
    private static final float BLEND_FACTOR = 0.05f;
    // Minimum ticks between dye processing (cooldown)
    private static final int PROCESS_COOLDOWN = 4;

    private int currentColor = DEFAULT_WATER_COLOR;
    private boolean hasDye = false;
    private long lastProcessTick = 0;

    public DyeVatsBlockEntity(BlockPos pos, BlockState state) {
        super(TharidiaThings.DYE_VATS_BLOCK_ENTITY.get(), pos, state);
    }

    public int getCurrentColor() {
        return currentColor;
    }

    public void setCurrentColor(int color) {
        this.currentColor = color;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void resetColor() {
        this.currentColor = DEFAULT_WATER_COLOR;
        this.hasDye = false;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * Called from entityInside when an ItemEntity enters the vat.
     */
    public void processItemEntity(ItemEntity itemEntity) {
        if (level == null || level.isClientSide) return;
        if (itemEntity.isRemoved()) return;

        BlockState state = getBlockState();
        if (!state.getValue(DyeVatsBlock.FILLED)) return;

        ItemStack stack = itemEntity.getItem();

        if (stack.getItem() instanceof DyeItem dyeItem) {
            long currentTick = level.getGameTime();
            if (currentTick - lastProcessTick < PROCESS_COOLDOWN) return;
            lastProcessTick = currentTick;

            int dyeRgb = dyeItem.getDyeColor().getFireworkColor();
            currentColor = blendColors(currentColor, dyeRgb, BLEND_FACTOR);
            hasDye = true;
            stack.shrink(1);
            if (stack.isEmpty()) {
                itemEntity.discard();
            } else {
                itemEntity.setItem(stack);
            }
            setChanged();
            level.sendBlockUpdated(worldPosition, state, state, 3);
            level.playSound(null, worldPosition, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.3F, 1.2F);
        } else if (isDyeable(stack)) {
            if (hasDye) {
                // Dyed water: apply color to item
                stack.set(DataComponents.DYED_COLOR, new DyedItemColor(currentColor, true));
            } else {
                // Clean water: wash item (remove dye)
                stack.remove(DataComponents.DYED_COLOR);
            }
            itemEntity.setItem(stack);

            level.setBlock(worldPosition, state.setValue(DyeVatsBlock.FILLED, false), 3);
            resetColor();
            level.playSound(null, worldPosition, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.5F, 0.8F);
        }
    }

    /**
     * Checks if an item can be dyed using the vanilla DYEABLE item tag.
     */
    public static boolean isDyeable(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(ItemTags.DYEABLE);
    }

    /**
     * Blends two RGB colors by a given factor (0.0 = keep current, 1.0 = full new color).
     */
    public static int blendColors(int current, int added, float factor) {
        int r1 = (current >> 16) & 0xFF;
        int g1 = (current >> 8) & 0xFF;
        int b1 = current & 0xFF;

        int r2 = (added >> 16) & 0xFF;
        int g2 = (added >> 8) & 0xFF;
        int b2 = added & 0xFF;

        int r = (int) (r1 + (r2 - r1) * factor);
        int g = (int) (g1 + (g2 - g1) * factor);
        int b = (int) (b1 + (b2 - b1) * factor);

        return (r << 16) | (g << 8) | b;
    }

    // ---- NBT Save/Load ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Color", currentColor);
        tag.putBoolean("HasDye", hasDye);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Color")) {
            currentColor = tag.getInt("Color");
        }
        hasDye = tag.getBoolean("HasDye");
    }

    // ---- Client Sync ----

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("Color", currentColor);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookupProvider) {
        super.onDataPacket(net, pkt, lookupProvider);
        // Force chunk re-render so the tint color updates visually
        if (level != null && level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
