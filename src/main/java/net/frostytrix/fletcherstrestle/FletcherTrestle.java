package net.frostytrix.fletcherstrestle;

import com.mojang.logging.LogUtils;
import net.frostytrix.fletcherstrestle.block.ModBlocks;
import net.frostytrix.fletcherstrestle.block.entity.ModBlockEntities;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.config.FletcherConfig;
import net.frostytrix.fletcherstrestle.effect.ModEffects;
import net.frostytrix.fletcherstrestle.entity.ModEntities;
import net.frostytrix.fletcherstrestle.entity.custom.EagleEntity;
import net.frostytrix.fletcherstrestle.entity.custom.HeavyDummyEntity;
import net.frostytrix.fletcherstrestle.fluid.ModFluidTypes;
import net.frostytrix.fletcherstrestle.fluid.ModFluids;
import net.frostytrix.fletcherstrestle.item.ModCreativeModeTabs;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.frostytrix.fletcherstrestle.loot.ModLootModifiers;
import net.frostytrix.fletcherstrestle.material.ModMaterialEffectTypes;
import net.frostytrix.fletcherstrestle.material.ModMaterialRegistries;
import net.frostytrix.fletcherstrestle.menu.ModMenuTypes;
import net.frostytrix.fletcherstrestle.recipe.ModRecipes;
import net.frostytrix.fletcherstrestle.sound.ModSounds;
import net.frostytrix.fletcherstrestle.worldgen.ModFeatures;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(FletcherTrestle.MOD_ID)
public class FletcherTrestle {
    public static final String MOD_ID = "fletcherstrestle";
    public static final Logger LOGGER = LogUtils.getLogger();


    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public FletcherTrestle(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ExampleMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        ModCreativeModeTabs.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModEntities.register(modEventBus);
        ModEffects.register(modEventBus);
        ModLootModifiers.register(modEventBus);
        ModFluidTypes.FLUID_TYPES.register(modEventBus);
        ModFluids.FLUIDS.register(modEventBus);
        ModSounds.register(modEventBus);
        ModFeatures.register(modEventBus);
        // MaterialEffectType registry + the six datapack-driven material
        // registries. JSON in data/<ns>/fletcherstrestle/{bow_limb,
        // arrow_head,...}/<id>.json loads into these and syncs to clients.
        ModMaterialEffectTypes.register(modEventBus);
        ModMaterialRegistries.register(modEventBus);


        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::registerEntityAttributes);
        modEventBus.addListener(this::onRegisterSpawnPlacements);

        ModRecipes.SERIALIZERS.register(modEventBus);
        ModRecipes.TYPES.register(modEventBus);
        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.SERVER, FletcherConfig.SERVER_SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, FletcherConfig.CLIENT_SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
    }

    private void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.HEAVY_DUMMY.get(), HeavyDummyEntity.createAttributes().build());
    }

    // Restrict where wild eagles spawn. The biome modifier narrows them to
    // the mountain-biome tag; this predicate adds altitude, sky-visibility,
    // and sky-light checks so they only appear on exposed ridges in daylight.
    //
    // Gated by FletcherConfig.EAGLES_NATURAL_SPAWNING — off by default while
    // the eagle model is WIP. When off, registration is skipped so the spawn
    // pool never gets told about eagles; flip the config to restore it.
    private void onRegisterSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        if (!FletcherConfig.EAGLES_NATURAL_SPAWNING.get()) {
            return;
        }
        event.register(
                ModEntities.EAGLE.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.NO_RESTRICTIONS,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                EagleEntity::checkEagleSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }


    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }
}
