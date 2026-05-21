package net.frostytrix.fletcherstrestle.entity;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.entity.custom.EagleEntity;
import net.frostytrix.fletcherstrestle.entity.custom.HeavyDummyEntity;
import net.frostytrix.fletcherstrestle.entity.custom.ModularArrowEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, FletcherTrestle.MOD_ID);

    // 26.1: EntityType.Builder.build() now takes a ResourceKey<EntityType<?>>
    // instead of a String. Helper below keeps registration calls readable.
    private static ResourceKey<EntityType<?>> key(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(FletcherTrestle.MOD_ID, name));
    }

    public static final Supplier<EntityType<ModularArrowEntity>> MODULAR_ARROW =
            ENTITY_TYPES.register("modular_arrow", () ->
                    EntityType.Builder.<ModularArrowEntity>of(ModularArrowEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(4)
                            .updateInterval(1)
                            .build(key("modular_arrow"))
            );

    public static final Supplier<EntityType<HeavyDummyEntity>> HEAVY_DUMMY =
            ENTITY_TYPES.register("heavy_dummy", () ->
                    EntityType.Builder.of(HeavyDummyEntity::new, MobCategory.MISC)
                            .sized(0.6f, 1.8f)
                            .build(key("heavy_dummy"))
            );

    public static final Supplier<EntityType<EagleEntity>> EAGLE =
            ENTITY_TYPES.register("eagle", () ->
                    EntityType.Builder.<EagleEntity>of(EagleEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 0.9F)
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .build(key("eagle"))
            );

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
