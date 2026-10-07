package net.frostytrix.fletcherstrestle.material;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.component.BowAssembly;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A finished weapon with random parts, for everything that hands one out without
 * a player building it: the Fletcher's trades, armed mobs, Trial Chamber loot.
 *
 * <p>Parts come from the datapack registries, so a modpack's materials turn up on
 * their own. The one rule it enforces is the one the Fletching Table enforces: a
 * string that needs a metal riser gets one.</p>
 */
public final class RandomWeapon {
    private RandomWeapon() {
    }

    /**
     * @param limbPool limb ids to choose from; empty means any registered limb.
     *                 Ids the registry doesn't know are ignored.
     */
    public static BowAssembly assemble(RegistryAccess access, RandomSource random, List<String> limbPool,
                                       float minTuning, float maxTuning) {
        Registry<BowLimbDef> limbs = access.registryOrThrow(ModMaterialRegistries.BOW_LIMB);
        Registry<BowRiserDef> risers = access.registryOrThrow(ModMaterialRegistries.BOW_RISER);
        Registry<BowStringDef> strings = access.registryOrThrow(ModMaterialRegistries.BOW_STRING);

        List<String> known = knownLimbs(access, limbPool);
        String limb = known.isEmpty()
                ? stored(pick(new ArrayList<>(limbs.registryKeySet()), random))
                : known.get(random.nextInt(known.size()));

        ResourceKey<BowStringDef> stringKey = pick(new ArrayList<>(strings.registryKeySet()), random);
        boolean needsMetal = stringKey != null && strings.getOrThrow(stringKey).stats().requiresMetalRiser();

        List<ResourceKey<BowRiserDef>> riserChoices = new ArrayList<>();
        for (Map.Entry<ResourceKey<BowRiserDef>, BowRiserDef> entry : risers.entrySet()) {
            if (!needsMetal || entry.getValue().stats().metal()) {
                riserChoices.add(entry.getKey());
            }
        }
        if (riserChoices.isEmpty()) {
            // A pack with no metal riser at all: fall back to a string that doesn't need one.
            riserChoices.addAll(risers.registryKeySet());
            List<ResourceKey<BowStringDef>> easy = new ArrayList<>();
            for (Map.Entry<ResourceKey<BowStringDef>, BowStringDef> entry : strings.entrySet()) {
                if (!entry.getValue().stats().requiresMetalRiser()) easy.add(entry.getKey());
            }
            stringKey = pick(easy, random);
        }
        ResourceKey<BowRiserDef> riserKey = pick(riserChoices, random);

        float tuning = minTuning + random.nextFloat() * Math.max(0f, maxTuning - minTuning);
        return new BowAssembly(limb, stored(riserKey), stored(stringKey), tuning);
    }

    /**
     * The pool's ids that name a registered limb, each once, in pool order and in
     * stored form. Normalising first matters: "oak" and "fletcherstrestle:oak" are
     * the same wood, and counting them twice could laminate oak onto oak.
     */
    public static List<String> knownLimbs(RegistryAccess access, List<String> pool) {
        return knownIds(access, ModMaterialRegistries.BOW_LIMB, pool);
    }

    /** The same, for any material registry: risers, strings, … */
    public static <T> List<String> knownIds(RegistryAccess access, ResourceKey<Registry<T>> registryKey, List<String> pool) {
        Registry<T> registry = access.registryOrThrow(registryKey);
        List<String> known = new ArrayList<>();
        for (String id : pool) {
            ResourceLocation rl = id.indexOf(':') >= 0
                    ? ResourceLocation.tryParse(id)
                    : ResourceLocation.tryBuild(FletcherTrestle.MOD_ID, id);
            if (rl == null || !registry.containsKey(rl)) continue;
            String canonical = stored(ResourceKey.create(registryKey, rl));
            if (!known.contains(canonical)) {
                known.add(canonical);
            }
        }
        return known;
    }

    private static <T> ResourceKey<T> pick(List<ResourceKey<T>> keys, RandomSource random) {
        return keys.isEmpty() ? null : keys.get(random.nextInt(keys.size()));
    }

    /**
     * How a part is stored on the weapon: a bare path for this mod's own entries
     * ("oak"), the full id for anyone else's ("mypack:steel"). The resolver reads both.
     */
    private static <T> String stored(ResourceKey<T> key) {
        if (key == null) return "";
        ResourceLocation id = key.location();
        return id.getNamespace().equals(FletcherTrestle.MOD_ID) ? id.getPath() : id.toString();
    }
}
