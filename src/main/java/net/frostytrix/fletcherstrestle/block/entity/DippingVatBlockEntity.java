package net.frostytrix.fletcherstrestle.block.entity;

import net.frostytrix.fletcherstrestle.recipe.DippingRecipe;
import net.frostytrix.fletcherstrestle.recipe.DippingRecipeInput;
import net.frostytrix.fletcherstrestle.recipe.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.Optional;

public class DippingVatBlockEntity extends BlockEntity {

    // Only water and our own liquid-potion fluid are valid: keeps other mods'
    // pipes from shoving lava/milk/etc. into the vat (parity with the Steam Box).
    public final FluidTank fluidTank = new FluidTank(3000,
            fs -> fs.getFluid() == net.minecraft.world.level.material.Fluids.WATER
                    || fs.getFluid() == net.frostytrix.fletcherstrestle.fluid.ModFluids.LIQUID_POTION_SOURCE.get()) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            if (level != null && !level.isClientSide()) {
                // Push the new tank NBT to clients so the fluid renders (flag 3 = block update).
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    };

    public DippingVatBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DIPPING_VAT_BE.get(), pos, state);
    }

    public FluidTank getFluidTank() {
        return this.fluidTank;
    }

    // --- DATA SAVING ---
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Fluid", fluidTank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fluidTank.readFromNBT(registries, tag.getCompound("Fluid"));
    }

    // --- NETWORK SYNC FOR THE RENDERER ---
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public boolean handlePlayerInteraction(Player player, InteractionHand hand) {
        ItemStack itemInHand = player.getItemInHand(hand);

        // Buckets / fluid containers: FluidUtil fills/empties the tank and swaps the player's item.
        if (net.neoforged.neoforge.fluids.FluidUtil.interactWithFluidHandler(player, hand, this.fluidTank)) {
            return true;
        }

        // Fill an empty glass bottle from the tank.
        if (itemInHand.is(Items.GLASS_BOTTLE)) {
            FluidStack currentFluid = fluidTank.getFluid();

            // Need a full 1000mB for one bottle.
            if (currentFluid.getAmount() >= 1000) {
                boolean isOurPotion = currentFluid.getFluid() == net.frostytrix.fletcherstrestle.fluid.ModFluids.LIQUID_POTION_SOURCE.get();
                boolean isWater = currentFluid.getFluid() == net.minecraft.world.level.material.Fluids.WATER;

                if (isOurPotion || isWater) {
                    ItemStack filledBottle = new ItemStack(Items.POTION);

                    // Rebuild the Potion item with the right effect.
                    if (isOurPotion) {
                        net.minecraft.world.item.component.CustomData customData = currentFluid.get(DataComponents.CUSTOM_DATA);
                        if (customData != null && customData.contains("potion")) {
                            String potionId = customData.copyTag().getString("potion");
                            var potionHolder = net.minecraft.core.registries.BuiltInRegistries.POTION.getHolder(net.minecraft.resources.ResourceLocation.parse(potionId)).orElse(null);
                            if (potionHolder != null) {
                                filledBottle.set(DataComponents.POTION_CONTENTS, new net.minecraft.world.item.alchemy.PotionContents(potionHolder));
                            }
                        }
                    } else {
                        // Plain water (piped or bucketed in).
                        var waterHolder = net.minecraft.core.registries.BuiltInRegistries.POTION.getHolder(net.minecraft.resources.ResourceLocation.parse("minecraft:water")).orElse(null);
                        if (waterHolder != null) {
                            filledBottle.set(DataComponents.POTION_CONTENTS, new net.minecraft.world.item.alchemy.PotionContents(waterHolder));
                        }
                    }

                    fluidTank.drain(1000, IFluidHandler.FluidAction.EXECUTE);

                    // Swap the empty bottle for the filled one.
                    if (!player.getAbilities().instabuild) {
                        itemInHand.shrink(1);
                    }

                    if (itemInHand.isEmpty()) {
                        player.setItemInHand(hand, filledBottle);
                    } else if (!player.getInventory().add(filledBottle)) {
                        player.drop(filledBottle, false);
                    }

                    level.playSound(null, this.worldPosition, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                    return true;
                }
            }
        }

        // Manual fill: pour a (non-water) potion bottle into the tank.
        if (itemInHand.is(Items.POTION)) {
            PotionContents potionContents = itemInHand.get(DataComponents.POTION_CONTENTS);
            if (potionContents != null && !potionContents.is(Potions.WATER)) {

                String potionId = potionContents.potion()
                        .flatMap(holder -> holder.unwrapKey())
                        .map(key -> key.location().toString())
                        .orElse("minecraft:water");

                net.minecraft.world.item.component.CustomData customData = fluidTank.getFluid().get(DataComponents.CUSTOM_DATA);
                String savedPotionId = "";
                if (customData != null && customData.contains("potion")) {
                    savedPotionId = customData.copyTag().getString("potion");
                }

                if (fluidTank.isEmpty() || savedPotionId.equals(potionId)) {
                    if (fluidTank.getFluidAmount() <= 2000) {
                        CompoundTag fluidTag = new CompoundTag();
                        fluidTag.putString("potion", potionId);

                        FluidStack potionFluid = new FluidStack(net.frostytrix.fletcherstrestle.fluid.ModFluids.LIQUID_POTION_SOURCE.get(), 1000);
                        potionFluid.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(fluidTag));
                        fluidTank.fill(potionFluid, IFluidHandler.FluidAction.EXECUTE);

                        if (!player.getAbilities().instabuild) {
                            itemInHand.shrink(1);
                            ItemStack emptyBottle = new ItemStack(Items.GLASS_BOTTLE);
                            if (itemInHand.isEmpty()) {
                                player.setItemInHand(hand, emptyBottle);
                            } else if (!player.getInventory().add(emptyBottle)) {
                                player.drop(emptyBottle, false);
                            }
                        }

                        level.playSound(null, this.worldPosition, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                        return true;
                    }
                }
            }
        }

        // Dipping (data-driven via recipes).
        if (!fluidTank.isEmpty() && !itemInHand.isEmpty()) {
            DippingRecipeInput input = new DippingRecipeInput(itemInHand, fluidTank.getFluid());
            Optional<RecipeHolder<DippingRecipe>> match = this.level.getRecipeManager()
                    .getRecipeFor(ModRecipes.DIPPING_TYPE.get(), input, this.level);

            if (match.isPresent()) {
                DippingRecipe recipe = match.get().value();

                if (itemInHand.getCount() >= recipe.inputCount()) {
                    // Assemble (carries over the potion colour), then consume inputs.
                    ItemStack result = recipe.assemble(input, level.registryAccess());
                    itemInHand.shrink(recipe.inputCount());
                    fluidTank.drain(recipe.fluidAmount(), IFluidHandler.FluidAction.EXECUTE);

                    if (!player.getInventory().add(result)) {
                        player.drop(result, false);
                    }

                    level.playSound(null, this.worldPosition, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                    return true;
                }
            }
        }

        return false;
    }
}