package net.frostytrix.fletcherstrestle.item;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.block.ModBlocks;
import net.frostytrix.fletcherstrestle.component.ArrowAssembly;
import net.frostytrix.fletcherstrestle.component.BowAssembly;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FletcherTrestle.MOD_ID);

    public static final Supplier<CreativeModeTab> FLETCHERS_TRESTLE_TAB = CREATIVE_MODE_TAB.register("fletchers_trestle_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.MODULAR_BOW.get()))
                    .withTabsBefore(ResourceLocation.fromNamespaceAndPath("minecraft","spawn_eggs"))
                    .title(Component.translatable("creative_tab.fletcherstrestle.fletchers_trestle_tab"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModBlocks.STEAM_BOX);
                        output.accept(ModBlocks.SHAVING_HORSE);
                        output.accept(ModBlocks.DIPPING_VAT);
                        output.accept(ModBlocks.ARCHERY_TARGET);

                        output.accept(ModItems.DRAWKNIFE.get());

                        output.accept(ModItems.ROUGH_OAK_LIMB.get());
                        output.accept(ModItems.PLIABLE_OAK_LIMB.get());

                        output.accept(ModItems.ROUGH_SPRUCE_LIMB.get());
                        output.accept(ModItems.PLIABLE_SPRUCE_LIMB.get());

                        output.accept(ModItems.ROUGH_BIRCH_LIMB.get());
                        output.accept(ModItems.PLIABLE_BIRCH_LIMB.get());

                        output.accept(ModItems.ROUGH_JUNGLE_LIMB.get());
                        output.accept(ModItems.PLIABLE_JUNGLE_LIMB.get());

                        output.accept(ModItems.ROUGH_ACACIA_LIMB.get());
                        output.accept(ModItems.PLIABLE_ACACIA_LIMB.get());

                        output.accept(ModItems.ROUGH_DARK_OAK_LIMB.get());
                        output.accept(ModItems.PLIABLE_DARK_OAK_LIMB.get());

                        output.accept(ModItems.ROUGH_MANGROVE_LIMB.get());
                        output.accept(ModItems.PLIABLE_MANGROVE_LIMB.get());

                        output.accept(ModItems.ROUGH_CHERRY_LIMB.get());
                        output.accept(ModItems.PLIABLE_CHERRY_LIMB.get());

                        output.accept(ModItems.ROUGH_PALE_OAK_LIMB.get());
                        output.accept(ModItems.PLIABLE_PALE_OAK_LIMB.get());

                        output.accept(ModItems.ROUGH_CRIMSON_LIMB.get());
                        output.accept(ModItems.PLIABLE_CRIMSON_LIMB.get());

                        output.accept(ModItems.ROUGH_WARPED_LIMB.get());
                        output.accept(ModItems.PLIABLE_WARPED_LIMB.get());


                        output.accept(ModItems.WOOD_RISER.get());
                        output.accept(ModItems.IRON_RISER.get());
                        output.accept(ModItems.COPPER_RISER.get());


                        output.accept(ModItems.HIGH_TENSION_STRING.get());

                        output.accept(ModItems.FLAX_STRING.get());
                        output.accept(ModItems.FLAX.get());
                        output.accept(ModItems.FLAX_SEEDS.get());

                        ItemStack modularBowDisplay = new ItemStack(ModItems.MODULAR_BOW.get());
                        ItemStack modularCrossowDisplay = new ItemStack(ModItems.MODULAR_CROSSBOW.get());
                        ItemStack modularArrowDisplay = new ItemStack(ModItems.MODULAR_ARROW.get());


                        modularBowDisplay.set(ModDataComponents.BOW_ASSEMBLY.get(),
                                new BowAssembly("Oak", "Wood", "Spider", 1.0f));
                        output.accept(modularBowDisplay);

                        modularArrowDisplay.set(ModDataComponents.ARROW_ASSEMBLY.get(),
                                new ArrowAssembly("flint", "oak", "feather"));
                        output.accept(modularArrowDisplay);
                        output.accept(ModItems.WEIGHTED_HOOK.get());
                        output.accept(ModBlocks.ROPE.get());

                        output.accept(ModItems.MECHANICAL_TRIGGER.get());
                        modularCrossowDisplay.set(ModDataComponents.BOW_ASSEMBLY.get(),
                                new BowAssembly("Oak", "Wood", "Spider", 1.0f));
                        output.accept(modularCrossowDisplay);


                        output.accept(ModItems.LEATHER_QUIVER.get());
                        output.accept(ModItems.IRON_QUIVER.get());
                        output.accept(ModItems.FLETCHER_GUIDE.get());
                        output.accept(ModItems.HEAVY_DUMMY_ITEM.get());

                        output.accept(ModItems.EAGLE_SPAWN_EGG.get());
                        output.accept(ModItems.EAGLE_WHISTLE.get());

                    })
                    .build());



    public static void register(IEventBus bus) {CREATIVE_MODE_TAB.register(bus);}
}
