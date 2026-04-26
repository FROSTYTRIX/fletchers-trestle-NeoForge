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

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
