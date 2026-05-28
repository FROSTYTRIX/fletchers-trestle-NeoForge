package net.frostytrix.fletcherstrestle.entity;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.entity.custom.EagleEntity;
import net.frostytrix.fletcherstrestle.entity.custom.HeavyDummyEntity;
import net.frostytrix.fletcherstrestle.entity.custom.ModularArrowEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, FletcherTrestle.MOD_ID);

    public static final Supplier<EntityType<ModularArrowEntity>> MODULAR_ARROW =
            ENTITY_TYPES.register("modular_arrow", () ->
                    EntityType.Builder.<ModularArrowEntity>of(ModularArrowEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(4)
                            .updateInterval(1)
                            .build("modular_arrow")
            );

    public static final Supplier<EntityType<HeavyDummyEntity>> HEAVY_DUMMY =
            ENTITY_TYPES.register("heavy_dummy", () ->
                    EntityType.Builder.of(HeavyDummyEntity::new, MobCategory.MISC)
                            .sized(0.6f, 1.8f)
                            .build("heavy_dummy")
            );

    public static final Supplier<EntityType<EagleEntity>> EAGLE =
            ENTITY_TYPES.register("eagle", () ->
                    EntityType.Builder.of(EagleEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 0.9F)          // roughly hawk-sized
                            .clientTrackingRange(8)      // players see it at 128 blocks
                            .updateInterval(1)           // fast sync — it flies
                            .build("eagle")
            );

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}