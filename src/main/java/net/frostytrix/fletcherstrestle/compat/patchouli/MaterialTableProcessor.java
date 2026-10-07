package net.frostytrix.fletcherstrestle.compat.patchouli;

import net.frostytrix.fletcherstrestle.material.ArrowFletchingDef;
import net.frostytrix.fletcherstrestle.material.ArrowHeadDef;
import net.frostytrix.fletcherstrestle.material.ArrowShaftDef;
import net.frostytrix.fletcherstrestle.material.BowLimbDef;
import net.frostytrix.fletcherstrestle.material.BowRiserDef;
import net.frostytrix.fletcherstrestle.material.BowStringDef;
import net.frostytrix.fletcherstrestle.material.MaterialEffect;
import net.frostytrix.fletcherstrestle.material.MaterialResolver;
import net.frostytrix.fletcherstrestle.material.Materials;
import net.frostytrix.fletcherstrestle.material.ModMaterialRegistries;
import net.frostytrix.fletcherstrestle.material.stats.BowLimbStats;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import vazkii.patchouli.api.IComponentProcessor;
import vazkii.patchouli.api.IVariable;
import vazkii.patchouli.api.IVariableProvider;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Builds the guidebook's material tables from the live registries, so the numbers
 * can never drift from the game: a rebalance, or a modpack's own limbs, show up in
 * the book on their own.
 *
 * <p>Used by the {@code fletcherstrestle:material_table} template. Page variables:</p>
 * <ul>
 *   <li>{@code kind}: {@code bow_limb}, {@code bow_riser}, {@code bow_string},
 *       {@code arrow_head}, {@code arrow_shaft} or {@code arrow_fletching};</li>
 *   <li>{@code intro} / {@code outro}: optional text before and after the list;</li>
 *   <li>{@code from} / {@code to}: which rows to show, so a long list can span pages.</li>
 * </ul>
 * <p>Rows are sorted by display name. Only Patchouli ever loads this class.</p>
 */
public class MaterialTableProcessor implements IComponentProcessor {

    private String text = "";

    @Override
    public void setup(Level level, IVariableProvider variables) {
        var lookup = level.registryAccess();
        String kind = variables.has("kind") ? variables.get("kind", lookup).asString() : "";
        int from = variables.has("from") ? variables.get("from", lookup).asNumber().intValue() : 0;
        int to = variables.has("to") ? variables.get("to", lookup).asNumber().intValue() : Integer.MAX_VALUE;
        String intro = variables.has("intro") ? variables.get("intro", lookup).asString() : "";
        String outro = variables.has("outro") ? variables.get("outro", lookup).asString() : "";

        List<String> rows = switch (kind) {
            case "bow_limb" -> rows(level.registryAccess().registryOrThrow(ModMaterialRegistries.BOW_LIMB),
                    MaterialTableProcessor::limbRow);
            case "bow_riser" -> rows(level.registryAccess().registryOrThrow(ModMaterialRegistries.BOW_RISER),
                    MaterialTableProcessor::riserRow);
            case "bow_string" -> rows(level.registryAccess().registryOrThrow(ModMaterialRegistries.BOW_STRING),
                    MaterialTableProcessor::stringRow);
            case "arrow_head" -> rows(level.registryAccess().registryOrThrow(ModMaterialRegistries.ARROW_HEAD),
                    MaterialTableProcessor::headRow);
            case "arrow_shaft" -> rows(level.registryAccess().registryOrThrow(ModMaterialRegistries.ARROW_SHAFT),
                    MaterialTableProcessor::shaftRow);
            case "arrow_fletching" -> rows(level.registryAccess().registryOrThrow(ModMaterialRegistries.ARROW_FLETCHING),
                    MaterialTableProcessor::fletchingRow);
            default -> List.of("Unknown material kind: " + kind);
        };

        StringBuilder out = new StringBuilder(intro);
        if (!intro.isEmpty()) out.append("$(br)");
        for (int i = Math.max(0, from); i < Math.min(to, rows.size()); i++) {
            out.append("$(li)").append(rows.get(i));
        }
        out.append(outro);
        this.text = out.toString();
    }

    @Override
    public IVariable process(Level level, String key) {
        // Anything else (the header, say) falls through to the page's own variables.
        return "text".equals(key) ? IVariable.wrap(text) : null;
    }

    @FunctionalInterface
    private interface RowWriter<T> {
        String row(ResourceLocation id, T def);
    }

    private static <T> List<String> rows(Registry<T> registry, RowWriter<T> writer) {
        List<Map.Entry<ResourceKey<T>, T>> entries = new ArrayList<>(registry.entrySet());
        entries.sort(Comparator.comparing(e -> name(e.getKey().location())));
        List<String> rows = new ArrayList<>();
        for (Map.Entry<ResourceKey<T>, T> entry : entries) {
            rows.add(writer.row(entry.getKey().location(), entry.getValue()));
        }
        return rows;
    }

    // ---------------- rows ----------------

    private static String limbRow(ResourceLocation id, BowLimbDef def) {
        BowLimbStats stats = def.stats();
        List<String> parts = new ArrayList<>();
        parts.add(number(stats.drawTimeTicks()) + "t");
        parts.add(multiplier(stats.damageMultiplier()) + "x");
        if (stats.agility()) parts.add(tr("trait.fletcherstrestle.agility"));
        if (stats.amphibious()) parts.add(tr("trait.fletcherstrestle.amphibious"));
        if (stats.givesSlowFalling()) parts.add(tr("trait.fletcherstrestle.slow_fall"));
        if (!stats.photosynthetic()) parts.add(tr("trait.fletcherstrestle.no_photosynthesis"));
        traits(def.effects(), parts);
        return row(id, parts);
    }

    private static String riserRow(ResourceLocation id, BowRiserDef def) {
        List<String> parts = new ArrayList<>();
        parts.add(Component.translatable("trait.fletcherstrestle.durability", def.stats().maxDurability()).getString());
        if (def.stats().inaccuracyMultiplier() != 1.0f) {
            parts.add(Component.translatable("trait.fletcherstrestle.spread",
                    multiplier(def.stats().inaccuracyMultiplier())).getString());
        }
        traits(def.effects(), parts);
        return row(id, parts);
    }

    private static String stringRow(ResourceLocation id, BowStringDef def) {
        List<String> parts = new ArrayList<>();
        parts.add(multiplier(def.stats().velocityMultiplier()) + "x");
        if (def.stats().durabilityCost() > 1) {
            parts.add(Component.translatable("trait.fletcherstrestle.per_shot", def.stats().durabilityCost()).getString());
        }
        if (def.stats().requiresMetalRiser()) parts.add(tr("trait.fletcherstrestle.needs_metal"));
        if (def.stats().overdrawShake()) parts.add(tr("trait.fletcherstrestle.shaky"));
        traits(def.effects(), parts);
        return row(id, parts);
    }

    private static String headRow(ResourceLocation id, ArrowHeadDef def) {
        List<String> parts = new ArrayList<>();
        parts.add(multiplier(def.stats().damageMultiplier()) + "x");
        traits(def.effects(), parts);
        return row(id, parts);
    }

    private static String shaftRow(ResourceLocation id, ArrowShaftDef def) {
        List<String> parts = new ArrayList<>();
        if (def.stats().velocityMultiplier() != 1.0f) {
            parts.add(Component.translatable("trait.fletcherstrestle.arrow_speed",
                    multiplier(def.stats().velocityMultiplier())).getString());
        }
        if (def.stats().gravityMultiplier() != 1.0f) {
            parts.add(Component.translatable("trait.fletcherstrestle.arrow_gravity",
                    multiplier(def.stats().gravityMultiplier())).getString());
        }
        traits(def.effects(), parts);
        if (parts.isEmpty()) parts.add("-");
        return row(id, parts);
    }

    private static String fletchingRow(ResourceLocation id, ArrowFletchingDef def) {
        List<String> parts = new ArrayList<>();
        parts.add(Component.translatable("trait.fletcherstrestle.spread",
                multiplier(def.stats().inaccuracyMultiplier())).getString());
        traits(def.effects(), parts);
        return row(id, parts);
    }

    // ---------------- helpers ----------------

    private static void traits(List<MaterialEffect> effects, List<String> parts) {
        for (MaterialEffect effect : effects) {
            effect.describe().ifPresent(c -> parts.add(c.getString()));
        }
    }

    private static String row(ResourceLocation id, List<String> parts) {
        return "$(l)" + name(id) + "$(): " + String.join(", ", parts);
    }

    private static String name(ResourceLocation id) {
        return MaterialResolver.displayName(id).getString();
    }

    private static String tr(String key) {
        return Component.translatable(key).getString();
    }

    /** Ticks: 20 rather than 20.0, but 22.5 kept as is. */
    private static String number(float value) {
        return value == Math.rint(value) ? String.valueOf((int) value) : String.valueOf(value);
    }

    /** Multipliers keep a decimal: 1.0x, 0.85x. */
    private static String multiplier(float value) {
        return value == Math.rint(value) ? String.format(java.util.Locale.ROOT, "%.1f", value) : String.valueOf(value);
    }
}
