package net.frostytrix.fletcherstrestle.block.custom;

import com.mojang.serialization.MapCodec;
import net.frostytrix.fletcherstrestle.block.entity.DippingVatBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class DippingVatBlock extends BaseEntityBlock {

    public DippingVatBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return null;
    }

    // 26.1: a block sees two distinct interaction callbacks —
    //   * useItemOn(stack, …)         — fired when the player's main hand is
    //                                   holding something (bucket, glass bottle,
    //                                   potion, arrow stack to dip, etc.).
    //   * useWithoutItem(…)           — fired when the main hand is empty.
    // Previously only useWithoutItem was wired, so dipping a bucket into the
    // vat or feeding it a potion silently did nothing. Both now delegate to
    // the same handlePlayerInteraction on the BE; player.getUsedItemHand()
    // doesn't work here (the player isn't actively *using* an item), so we
    // pass the hand the method already gives us.
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof DippingVatBlockEntity vat) {
            if (vat.handlePlayerInteraction(player, hand)) {
                return InteractionResult.SUCCESS;
            }
        }
        // Returning PASS lets vanilla item logic try (e.g. bucket placing
        // a fluid in the world if our handler didn't claim the click).
        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof DippingVatBlockEntity vat) {
            // Empty-handed click: only the "extract potion via glass bottle"
            // branch in handlePlayerInteraction will hit anything useful, but
            // calling it is harmless if the conditions don't match.
            vat.handlePlayerInteraction(player, InteractionHand.MAIN_HAND);
        }
        return InteractionResult.SUCCESS;
    }
    @Override
    public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DippingVatBlockEntity(pos, state);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    // 26.1: getAnalogOutputSignal now takes a Direction (side it's measured from).
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, net.minecraft.core.Direction side) {
        if (level.getBlockEntity(pos) instanceof DippingVatBlockEntity vat) {
            int amount = vat.fluidTank.getFluidAmount();
            int capacity = vat.fluidTank.getCapacity();

            if (amount == 0) {
                return 0; // Vide = Pas de signal
            }
            return 1 + (amount * 14) / capacity;
        }
        return 0;
    }
}