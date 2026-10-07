package net.frostytrix.fletcherstrestle.armory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

import java.util.List;
import java.util.Map;

/**
 * Which mobs carry modular weapons, and what they're made of.
 *
 * <p>A NeoForge data map on entity types, at
 * {@code data/<ns>/data_maps/entity_type/mob_armory.json}. Every listed mob that
 * spawns holding a vanilla bow or crossbow gets the modular equivalent instead.</p>
 *
 * <p><b>The base weapon</b> has both limbs from the first {@code common} wood (oak).
 * Its riser and string are rolled from the weighted {@code risers} and
 * {@code strings} tables (by default wood 4 : copper 1 : iron 1, and spider 3 :
 * flax 2 : high tension 1). A string that needs a metal riser is only rolled on
 * one, its weight shared out among the others on a wooden riser.</p>
 *
 * <p><b>Modified</b>, with a chance between {@code min_chance} and {@code max_chance}
 * depending on local difficulty, at least one limb becomes a <b>signature</b> wood:
 * one of the mob's own {@code woods}, or (with {@code native_woods}) one that grows
 * in the biome it spawned in, or failing both, one from {@code fallback}. The
 * {@code common} woods are only ever a composite's partner, so a modified stray
 * draws spruce or oak-and-spruce, never plain oak.</p>
 *
 * @param minChance       chance the weapon is modified in the easiest regions
 * @param maxChance       chance in the hardest; local difficulty slides between the two
 * @param common          the base wood, and the only woods a modified weapon may pair with
 * @param woods           signature woods this mob always may carry
 * @param nativeWoods     also count the woods that grow in the spawn biome as signature
 * @param fallback        signature woods for when neither of the above gives any
 * @param compositeChance chance a modified weapon laminates two woods. Only honoured
 *                        while the server allows composite bows at all.
 * @param risers          riser ids and their weights
 * @param strings         string ids and their weights
 * @param minTuning       lowest tuning the weapon rolls
 * @param maxTuning       highest tuning the weapon rolls
 */
public record MobArmory(float minChance, float maxChance, List<String> common, List<String> woods,
                        boolean nativeWoods, List<String> fallback, float compositeChance,
                        Map<String, Float> risers, Map<String, Float> strings,
                        float minTuning, float maxTuning) {

    public static final Map<String, Float> DEFAULT_RISERS = Map.of("wood", 4f, "copper", 1f, "iron", 1f);
    public static final Map<String, Float> DEFAULT_STRINGS = Map.of("spider", 3f, "flax", 2f, "high_tension", 1f);

    private static final Codec<Map<String, Float>> WEIGHTS =
            Codec.unboundedMap(Codec.STRING, Codec.floatRange(0f, Float.MAX_VALUE));

    public static final Codec<MobArmory> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.floatRange(0f, 1f).optionalFieldOf("min_chance", 0.33f).forGetter(MobArmory::minChance),
            Codec.floatRange(0f, 1f).optionalFieldOf("max_chance", 0.66f).forGetter(MobArmory::maxChance),
            Codec.STRING.listOf().optionalFieldOf("common", List.of("oak")).forGetter(MobArmory::common),
            Codec.STRING.listOf().optionalFieldOf("woods", List.of()).forGetter(MobArmory::woods),
            Codec.BOOL.optionalFieldOf("native_woods", false).forGetter(MobArmory::nativeWoods),
            Codec.STRING.listOf().optionalFieldOf("fallback", List.of()).forGetter(MobArmory::fallback),
            Codec.floatRange(0f, 1f).optionalFieldOf("composite_chance", 0f).forGetter(MobArmory::compositeChance),
            WEIGHTS.optionalFieldOf("risers", DEFAULT_RISERS).forGetter(MobArmory::risers),
            WEIGHTS.optionalFieldOf("strings", DEFAULT_STRINGS).forGetter(MobArmory::strings),
            Codec.floatRange(0.2f, 1f).optionalFieldOf("min_tuning", 0.4f).forGetter(MobArmory::minTuning),
            Codec.floatRange(0.2f, 1f).optionalFieldOf("max_tuning", 0.8f).forGetter(MobArmory::maxTuning)
    ).apply(inst, MobArmory::new));

    public static final DataMapType<EntityType<?>, MobArmory> DATA_MAP = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "mob_armory"),
            Registries.ENTITY_TYPE, CODEC).build();
}
