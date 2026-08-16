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
                    BlockEntityType.Builder.of(SteamBoxBlockEntity::new, ModBlocks.STEAM_BOX.get())
                            .build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShavingHorseBlockEntity>> SHAVING_HORSE_BE =
            BLOCK_ENTITIES.register("shaving_horse_be", () ->
                    BlockEntityType.Builder.of(ShavingHorseBlockEntity::new, ModBlocks.SHAVING_HORSE.get())
                            .build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ArcheryTargetBlockEntity>> ARCHERY_TARGET_BE =
            BLOCK_ENTITIES.register("archery_target_be", () ->
                    BlockEntityType.Builder.of(ArcheryTargetBlockEntity::new, ModBlocks.ARCHERY_TARGET.get())
                            .build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DippingVatBlockEntity>> DIPPING_VAT_BE =
            BLOCK_ENTITIES.register("dipping_vat_be", () ->
                    BlockEntityType.Builder.of(DippingVatBlockEntity::new, ModBlocks.DIPPING_VAT.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EaglePerchBlockEntity>> EAGLE_PERCH_BE =
            BLOCK_ENTITIES.register("eagle_perch_be", () ->
                    BlockEntityType.Builder.of(EaglePerchBlockEntity::new, ModBlocks.EAGLE_PERCH.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EagleNestBlockEntity>> EAGLE_NEST_BE =
            BLOCK_ENTITIES.register("eagle_nest_be", () ->
                    BlockEntityType.Builder.of(EagleNestBlockEntity::new, ModBlocks.EAGLE_NEST.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrossbowBenchBlockEntity>> CROSSBOW_BENCH_BE =
            BLOCK_ENTITIES.register("crossbow_bench_be", () ->
                    BlockEntityType.Builder.of(CrossbowBenchBlockEntity::new, ModBlocks.CROSSBOW_BENCH.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ArrowSlitBlockEntity>> ARROW_SLIT_BE =
            BLOCK_ENTITIES.register("arrow_slit_be", () ->
                    BlockEntityType.Builder.of(ArrowSlitBlockEntity::new, ModBlocks.ARROW_SLIT.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WeaponRackBlockEntity>> WEAPON_RACK_BE =
            BLOCK_ENTITIES.register("weapon_rack_be", () ->
                    BlockEntityType.Builder.of(WeaponRackBlockEntity::new, ModBlocks.WEAPON_RACK.get()).build(null)
            );

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
