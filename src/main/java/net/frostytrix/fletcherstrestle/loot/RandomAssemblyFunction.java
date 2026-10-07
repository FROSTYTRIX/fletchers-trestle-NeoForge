package net.frostytrix.fletcherstrestle.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.material.RandomWeapon;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;

/**
 * Loot function {@code fletcherstrestle:random_assembly}: turns a bare modular bow
 * or crossbow into a finished one with random parts, so a loot table can hand out
 * real weapons. Same builder as the trades and armed mobs, so the metal-riser rule
 * holds here too.
 *
 * <pre>{ "function": "fletcherstrestle:random_assembly",
 *   "min_tuning": 0.6, "max_tuning": 0.95, "limbs": ["pale_oak"] }</pre>
 */
public class RandomAssemblyFunction extends LootItemConditionalFunction {

    public static final MapCodec<RandomAssemblyFunction> CODEC = RecordCodecBuilder.mapCodec(inst ->
            commonFields(inst).and(inst.group(
                    Codec.floatRange(0.2f, 1f).optionalFieldOf("min_tuning", 0.5f).forGetter(f -> f.minTuning),
                    Codec.floatRange(0.2f, 1f).optionalFieldOf("max_tuning", 0.9f).forGetter(f -> f.maxTuning),
                    Codec.STRING.listOf().optionalFieldOf("limbs", List.of()).forGetter(f -> f.limbs)
            )).apply(inst, RandomAssemblyFunction::new));

    private final float minTuning;
    private final float maxTuning;
    private final List<String> limbs;

    private RandomAssemblyFunction(List<LootItemCondition> conditions, float minTuning, float maxTuning, List<String> limbs) {
        super(conditions);
        this.minTuning = minTuning;
        this.maxTuning = maxTuning;
        this.limbs = limbs;
    }

    @Override
    public LootItemFunctionType<RandomAssemblyFunction> getType() {
        return ModLootFunctions.RANDOM_ASSEMBLY.get();
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        stack.set(ModDataComponents.BOW_ASSEMBLY.get(), RandomWeapon.assemble(
                context.getLevel().registryAccess(), context.getRandom(), limbs, minTuning, maxTuning));
        return stack;
    }
}
