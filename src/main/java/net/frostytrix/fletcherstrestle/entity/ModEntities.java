package net.frostytrix.fletcherstrestle.entity;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.entity.custom.HeavyDummyEntity;
import net.frostytrix.fletcherstrestle.entity.custom.ModularArrowEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModEntities {
    // 1. Create the DeferredRegister for Entities
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, FletcherTrestle.MOD_ID);

    // 2. Register the Modular Arrow
    public static final Supplier<EntityType<ModularArrowEntity>> MODULAR_ARROW =
            ENTITY_TYPES.register("modular_arrow", () ->
                    // Use MobCategory.MISC for projectiles!
                    EntityType.Builder.<ModularArrowEntity>of(ModularArrowEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F) // Standard arrow hitbox size
                            .clientTrackingRange(4) // How far away players can see it flying
                            .updateInterval(20) // Network sync interval
                            .build("modular_arrow") // The internal ID
            );

    public static final Supplier<EntityType<HeavyDummyEntity>> HEAVY_DUMMY =
            ENTITY_TYPES.register("heavy_dummy", () -> EntityType.Builder.of(HeavyDummyEntity::new, MobCategory.MISC)
                    .sized(0.6f,1.8f).build("heavy_dummy"));


    // 3. Register the bus
    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}