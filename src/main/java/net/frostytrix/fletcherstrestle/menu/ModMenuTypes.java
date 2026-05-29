package net.frostytrix.fletcherstrestle.menu;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, FletcherTrestle.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<FletchingMenu>> FLETCHING_MENU =
            MENUS.register("fletching_menu", () -> new MenuType<>(FletchingMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final java.util.function.Supplier<net.minecraft.world.inventory.MenuType<net.frostytrix.fletcherstrestle.menu.QuiverMenu>> QUIVER_MENU =
            MENUS.register("quiver_menu", () -> new net.minecraft.world.inventory.MenuType<>(
                    net.frostytrix.fletcherstrestle.menu.QuiverMenu::new,
                    net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS
            ));

    public static final DeferredHolder<MenuType<?>, MenuType<ArcheryTargetMenu>> ARCHERY_TARGET_MENU =
            MENUS.register("archery_target_menu",
                    () -> new MenuType<>(ArcheryTargetMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<CrossbowBenchMenu>> CROSSBOW_BENCH_MENU =
            MENUS.register("crossbow_bench_menu",
                    () -> new MenuType<>(CrossbowBenchMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}