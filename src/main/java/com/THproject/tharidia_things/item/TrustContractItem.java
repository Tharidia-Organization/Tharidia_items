package com.THproject.tharidia_things.item;

import com.THproject.tharidia_things.block.ClaimBlock;
import com.THproject.tharidia_things.block.entity.ClaimBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Trust Contract Item - Allows players to grant trust access to their claims.
 *
 * Flow:
 * 1. Player crafts "Blank Contract" (Contratto Vuoto)
 * 2. Owner right-clicks their Claim Block with contract -> Contract binds to claim
 * 3. Owner gives bound contract to another player
 * 4. Other player right-clicks (uses) the bound contract -> Becomes trusted
 * 5. Contract is consumed
 */
public class TrustContractItem extends Item {

    public TrustContractItem(Properties properties) {
        super(properties);
    }

    /**
     * When right-clicking on a block (specifically a Claim Block)
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (level.isClientSide || player == null) {
            return InteractionResult.PASS;
        }

        // Check if clicked on a claim block
        if (!(level.getBlockState(pos).getBlock() instanceof ClaimBlock)) {
            return InteractionResult.PASS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ClaimBlockEntity claim)) {
            return InteractionResult.PASS;
        }

        // Check if contract is already bound
        if (isBound(stack)) {
            player.sendSystemMessage(Component.translatable("message.tharidiathings.contract.already_bound"));
            return InteractionResult.FAIL;
        }

        // Only the owner can bind contracts to their claim
        if (!claim.getOwnerUUID().equals(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("message.tharidiathings.contract.not_owner"));
            return InteractionResult.FAIL;
        }

        // Bind the contract to this claim
        bindToClaim(stack, claim, (ServerLevel) level);

        player.sendSystemMessage(Component.translatable("message.tharidiathings.contract.bound_success", claim.getClaimName()));
        player.sendSystemMessage(Component.translatable("message.tharidiathings.contract.give_to_player"));

        return InteractionResult.SUCCESS;
    }

    /**
     * When right-clicking in air (to activate a bound contract)
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.pass(stack);
        }

        // Check if contract is bound
        if (!isBound(stack)) {
            player.sendSystemMessage(Component.translatable("message.tharidiathings.contract.empty"));
            return InteractionResultHolder.pass(stack);
        }

        // Get contract data
        CompoundTag contractData = getContractData(stack);
        if (contractData == null) {
            player.sendSystemMessage(Component.translatable("message.tharidiathings.contract.corrupted"));
            return InteractionResultHolder.fail(stack);
        }

        UUID ownerUUID = contractData.getUUID("OwnerUUID");
        BlockPos claimPos = BlockPos.of(contractData.getLong("ClaimPos"));
        String claimDimension = contractData.getString("ClaimDimension");

        // Cannot use on yourself
        if (ownerUUID.equals(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("message.tharidiathings.contract.self_use"));
            return InteractionResultHolder.fail(stack);
        }

        // Check dimension
        String currentDimension = level.dimension().location().toString();
        if (!claimDimension.equals(currentDimension)) {
            player.sendSystemMessage(Component.translatable("message.tharidiathings.contract.wrong_dimension"));
            return InteractionResultHolder.fail(stack);
        }

        // Find and validate the claim
        ServerLevel serverLevel = (ServerLevel) level;
        if (!serverLevel.hasChunkAt(claimPos)) {
            player.sendSystemMessage(Component.translatable("message.tharidiathings.contract.chunk_not_loaded"));
            return InteractionResultHolder.fail(stack);
        }

        BlockEntity be = serverLevel.getBlockEntity(claimPos);
        if (!(be instanceof ClaimBlockEntity claim)) {
            player.sendSystemMessage(Component.translatable("message.tharidiathings.contract.claim_removed"));
            // Consume the contract anyway since it's invalid
            stack.shrink(1);
            return InteractionResultHolder.success(stack);
        }

        // Verify ownership hasn't changed
        if (!claim.getOwnerUUID().equals(ownerUUID)) {
            player.sendSystemMessage(Component.translatable("message.tharidiathings.contract.owner_changed"));
            stack.shrink(1);
            return InteractionResultHolder.success(stack);
        }

        // Check if player is already trusted
        if (claim.isTrusted(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("message.tharidiathings.contract.already_trusted"));
            return InteractionResultHolder.fail(stack);
        }

        // Add player to trusted list
        claim.addTrustedPlayer(player.getUUID());

        String claimName = contractData.getString("ClaimName");
        String ownerName = contractData.getString("OwnerName");

        player.sendSystemMessage(Component.translatable("message.tharidiathings.contract.trust_granted", claimName));

        // Notify owner if online
        ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(ownerUUID);
        if (owner != null) {
            owner.sendSystemMessage(Component.translatable("message.tharidiathings.contract.player_trusted_notify",
                player.getName().getString(), claimName));
        }

        // Consume the contract
        stack.shrink(1);

        return InteractionResultHolder.success(stack);
    }

    /**
     * Binds this contract to a claim
     */
    private void bindToClaim(ItemStack stack, ClaimBlockEntity claim, ServerLevel level) {
        CompoundTag tag = new CompoundTag();

        tag.putUUID("OwnerUUID", claim.getOwnerUUID());
        tag.putString("OwnerName", claim.getOwnerName());
        tag.putString("ClaimName", claim.getClaimName());
        tag.putLong("ClaimPos", claim.getBlockPos().asLong());
        tag.putString("ClaimDimension", level.dimension().location().toString());
        tag.putLong("CreationTime", System.currentTimeMillis());
        tag.putBoolean("Bound", true);

        // Store in CustomData component
        CustomData existingData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag mergedTag = existingData.copyTag();
        mergedTag.merge(tag);

        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(mergedTag));
    }

    /**
     * Checks if this contract is bound to a claim
     */
    public static boolean isBound(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return false;
        }
        return data.copyTag().getBoolean("Bound");
    }

    /**
     * Gets the contract data NBT
     */
    private CompoundTag getContractData(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        return data.copyTag();
    }

    /**
     * Adds tooltip with contract information
     */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        if (isBound(stack)) {
            CompoundTag data = getContractData(stack);
            if (data != null) {
                String claimName = data.getString("ClaimName");
                String ownerName = data.getString("OwnerName");
                long creationTime = data.getLong("CreationTime");

                tooltipComponents.add(Component.translatable("item.tharidiathings.trust_contract.bound_title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                tooltipComponents.add(Component.translatable("item.tharidiathings.trust_contract.claim_label", claimName));
                tooltipComponents.add(Component.translatable("item.tharidiathings.trust_contract.owner_label", ownerName));

                if (creationTime > 0) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                    tooltipComponents.add(Component.translatable("item.tharidiathings.trust_contract.created_label", sdf.format(new Date(creationTime))));
                }

                tooltipComponents.add(Component.empty());
                tooltipComponents.add(Component.translatable("item.tharidiathings.trust_contract.use_hint"));
                tooltipComponents.add(Component.translatable("item.tharidiathings.trust_contract.use_hint_2"));
            }
        } else {
            tooltipComponents.add(Component.translatable("item.tharidiathings.trust_contract.empty_title").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.translatable("item.tharidiathings.trust_contract.bind_hint"));
            tooltipComponents.add(Component.translatable("item.tharidiathings.trust_contract.bind_hint_2"));
        }
    }

    /**
     * Makes bound contracts appear with enchantment glint
     */
    @Override
    public boolean isFoil(ItemStack stack) {
        return isBound(stack);
    }
}
