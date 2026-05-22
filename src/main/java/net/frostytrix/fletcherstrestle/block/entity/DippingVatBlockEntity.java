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
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.Optional;

public class DippingVatBlockEntity extends BlockEntity {

    public final FluidTank fluidTank = new FluidTank(3000) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            if (level != null && !level.isClientSide()) {
                // Force le serveur à envoyer le nouveau NBT au client (Flag 3 = Update Block)
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
    // 26.1: BlockEntity save/load is keyed off ValueOutput / ValueInput.
    // FluidStack.OPTIONAL_CODEC handles the "empty fluid" case for us, so
    // store/read by codec is enough to persist the tank contents across
    // saves and chunk reloads.
    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        output.store("Fluid", FluidStack.OPTIONAL_CODEC, fluidTank.getFluid());
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        FluidStack stored = input.read("Fluid", FluidStack.OPTIONAL_CODEC).orElse(FluidStack.EMPTY);
        fluidTank.setFluid(stored);
    }

    // 26.1: BlockEntity.getUpdateTag still returns CompoundTag; the default
    // implementation routes through saveCustomOnly → saveAdditional, so
    // as long as saveAdditional writes the fluid (it does, above), the
    // update tag carries it for free. No override needed.
    //
    // Block-entity update packet sync stays the same shape; the codec
    // walk inside ClientboundBlockEntityDataPacket.create reads the same
    // saveAdditional output.
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public boolean handlePlayerInteraction(Player player, InteractionHand hand) {
        ItemStack itemInHand = player.getItemInHand(hand);

        // --- SEAUX ET CONTENANTS OFFICIELS (Eau, Lave, Fluides moddés) ---
        // 26.1: the legacy FluidUtil.interactWithFluidHandler(player, hand,
        // IFluidHandler) overload silently no-ops in some setups because the
        // new bucket plumbing only sees fluid handlers via the
        // Capabilities.Fluid.BLOCK capability (a ResourceHandler<FluidResource>).
        // Use the new transfer-package FluidUtil that looks the capability
        // up itself at our position — that's the path vanilla buckets and
        // mod pipes both follow now.
        if (this.level != null
                && net.neoforged.neoforge.transfer.fluid.FluidUtil.interactWithFluidHandler(
                        player, hand, this.level, this.worldPosition, null)) {
            return true;
        }

        // --- NOUVEAU CAS : RÉCUPÉRER UNE POTION DANS UNE FIOLE VIDE ---
        if (itemInHand.is(Items.GLASS_BOTTLE)) {
            FluidStack currentFluid = fluidTank.getFluid();

            // Il faut au moins 1000mB pour remplir une fiole entière
            if (currentFluid.getAmount() >= 1000) {
                // On vérifie que c'est bien notre potion alchimique (ou de l'eau pure)
                boolean isOurPotion = currentFluid.getFluid() == net.frostytrix.fletcherstrestle.fluid.ModFluids.LIQUID_POTION_SOURCE.get();
                boolean isWater = currentFluid.getFluid() == net.minecraft.world.level.material.Fluids.WATER;

                if (isOurPotion || isWater) {
                    ItemStack filledBottle = new ItemStack(Items.POTION);

                    // On recrée l'item Potion avec le bon effet
                    if (isOurPotion) {
                        net.minecraft.world.item.component.CustomData customData = currentFluid.get(DataComponents.CUSTOM_DATA);
                        if (customData != null && customData.contains("potion")) {
                            String potionId = customData.copyTag().getString("potion").orElse("");
                            var potionHolder = net.minecraft.core.registries.BuiltInRegistries.POTION.get(net.minecraft.resources.Identifier.parse(potionId)).orElse(null);
                            if (potionHolder != null) {
                                filledBottle.set(DataComponents.POTION_CONTENTS, new net.minecraft.world.item.alchemy.PotionContents(potionHolder));
                            }
                        }
                    } else {
                        // Cas où le joueur a mis de l'eau pure via un tuyau ou un seau
                        var waterHolder = net.minecraft.core.registries.BuiltInRegistries.POTION.get(net.minecraft.resources.Identifier.parse("minecraft:water")).orElse(null);
                        if (waterHolder != null) {
                            filledBottle.set(DataComponents.POTION_CONTENTS, new net.minecraft.world.item.alchemy.PotionContents(waterHolder));
                        }
                    }

                    // On draine 1000mB du réservoir
                    fluidTank.drain(1000, IFluidHandler.FluidAction.EXECUTE);

                    // On gère l'inventaire du joueur (remplace la fiole vide par la pleine)
                    if (!player.getAbilities().instabuild) {
                        itemInHand.shrink(1);
                    }

                    if (itemInHand.isEmpty()) {
                        player.setItemInHand(hand, filledBottle);
                    } else if (!player.getInventory().add(filledBottle)) {
                        player.drop(filledBottle, false);
                    }

                    // On joue le son de remplissage de bouteille
                    level.playSound(null, this.worldPosition, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                    return true;
                }
            }
        }

        // --- CAS 1 : REMPLISSAGE POTION MANUEL ---
        if (itemInHand.is(Items.POTION)) {
            PotionContents potionContents = itemInHand.get(DataComponents.POTION_CONTENTS);
            if (potionContents != null && !potionContents.is(Potions.WATER)) {

                String potionId = potionContents.potion()
                        .flatMap(holder -> holder.unwrapKey())
                        .map(key -> key.identifier().toString())
                        .orElse("minecraft:water");

                net.minecraft.world.item.component.CustomData customData = fluidTank.getFluid().get(DataComponents.CUSTOM_DATA);
                String savedPotionId = "";
                if (customData != null && customData.contains("potion")) {
                    savedPotionId = customData.copyTag().getString("potion").orElse("");
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

        // --- CAS 2 : LE TREMPAGE (DATA-DRIVEN VIA RECETTES) ---
        if (!fluidTank.isEmpty() && !itemInHand.isEmpty()) {
            // 1. On crée l'objet Input pour la recette
            DippingRecipeInput input = new DippingRecipeInput(itemInHand, fluidTank.getFluid());

            // 2. On interroge le gestionnaire de recettes de Minecraft
            Optional<RecipeHolder<DippingRecipe>> match = ((net.minecraft.server.level.ServerLevel) this.level).recipeAccess()
                    .getRecipeFor(ModRecipes.DIPPING_TYPE.get(), input, this.level);

            // 3. Si une recette correspond (Ex: 16 Flèches + 1000mB correspond)
            if (match.isPresent()) {
                DippingRecipe recipe = match.get().value();

                // Vérification supplémentaire : a-t-on le bon nombre d'items en main ?
                if (itemInHand.getCount() >= recipe.inputCount) {

                    // On assemble le résultat (ce qui transfère magiquement la couleur de la potion)
                    ItemStack result = recipe.assemble(input);

                    // Consommation
                    itemInHand.shrink(recipe.inputCount);
                    fluidTank.drain(recipe.fluidAmount, IFluidHandler.FluidAction.EXECUTE);

                    // Restitution au joueur
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