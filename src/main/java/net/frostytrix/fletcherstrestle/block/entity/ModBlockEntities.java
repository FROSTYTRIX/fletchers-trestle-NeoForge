package net.frostytrix.fletcherstrestle.block.entity;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, FletcherTrestle.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteamBoxBlockEntity>> STEAM_BOX_BE =
            BLOCK_ENTITIES.register("steam_box_be", () ->
                    new BlockEntityType<>(SteamBoxBlockEntity::new, ModBlocks.STEAM_BOX.get())
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShavingHorseBlockEntity>> SHAVING_HORSE_BE =
            BLOCK_ENTITIES.register("shaving_horse_be", () ->
                    new BlockEntityType<>(ShavingHorseBlockEntity::new, ModBlocks.SHAVING_HORSE.get())
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ArcheryTargetBlockEntity>> ARCHERY_TARGET_BE =
            BLOCK_ENTITIES.register("archery_target_be", () ->
                    new BlockEntityType<>(ArcheryTargetBlockEntity::new, ModBlocks.ARCHERY_TARGET.get())
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DippingVatBlockEntity>> DIPPING_VAT_BE =
            BLOCK_ENTITIES.register("dipping_vat_be", () ->
                    new BlockEntityType<>(DippingVatBlockEntity::new, ModBlocks.DIPPING_VAT.get())
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EaglePerchBlockEntity>> EAGLE_PERCH_BE =
            BLOCK_ENTITIES.register("eagle_perch_be", () ->
                    new BlockEntityType<>(EaglePerchBlockEntity::new, ModBlocks.EAGLE_PERCH.get())
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EagleNestBlockEntity>> EAGLE_NEST_BE =
            BLOCK_ENTITIES.register("eagle_nest_be", () ->
                    new BlockEntityType<>(EagleNestBlockEntity::new, ModBlocks.EAGLE_NEST.get())
            );

    public static void register(IEventBus eventBus) {
        System.out.println("====== FLETCHER'S TRESTLE: REGISTERING BLOCK ENTITIES NOW ======");
        BLOCK_ENTITIES.register(eventBus);
    }
}
