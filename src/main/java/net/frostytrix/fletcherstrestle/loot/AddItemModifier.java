package net.frostytrix.fletcherstrestle.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

public class AddItemModifier extends LootModifier {
    public static final MapCodec<AddItemModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            LootModifier.codecStart(inst)
                    .and(BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(e -> e.item))
                    .and(Codec.INT.optionalFieldOf("min_count", 1).forGetter(e -> e.minCount))
                    .and(Codec.INT.optionalFieldOf("max_count", 1).forGetter(e -> e.maxCount))
                    .apply(inst, AddItemModifier::new));
    private final Item item;
    private final int minCount;
    private final int maxCount;

    /** Adds exactly one of {@code item}. */
    public AddItemModifier(LootItemCondition[] conditionsIn, Item item) {
        this(conditionsIn, item, 1, 1);
    }

    /** Adds a random count in {@code [minCount, maxCount]} (inclusive). */
    public AddItemModifier(LootItemCondition[] conditionsIn, Item item, int minCount, int maxCount) {
        super(conditionsIn);
        this.item = item;
        this.minCount = minCount;
        this.maxCount = maxCount;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext lootContext) {
        for (LootItemCondition condition : this.conditions) {
            if (!condition.test(lootContext)) {
                return generatedLoot;
            }
        }
        int lo = Math.max(1, Math.min(this.minCount, this.maxCount));
        int hi = Math.max(lo, this.maxCount);
        int count = lo == hi ? lo : lo + lootContext.getRandom().nextInt(hi - lo + 1);
        generatedLoot.add(new ItemStack(this.item, count));
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
