package net.frostytrix.fletcherstrestle.block.custom;

import com.mojang.serialization.MapCodec;
import net.frostytrix.fletcherstrestle.block.entity.DippingVatBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof DippingVatBlockEntity vat) {
                // On délègue tout le traitement logique au Block Entity
                boolean success = vat.handlePlayerInteraction(player, player.getUsedItemHand());

                if (success) {
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
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
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
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