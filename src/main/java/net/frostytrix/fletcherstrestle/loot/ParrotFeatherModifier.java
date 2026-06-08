package net.frostytrix.fletcherstrestle.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/** Replaces a parrot's plain feather drops with a coloured feather matching its colour. */
public class ParrotFeatherModifier extends LootModifier {
    public static final MapCodec<ParrotFeatherModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            LootModifier.codecStart(inst).apply(inst, ParrotFeatherModifier::new));

    public ParrotFeatherModifier(LootItemCondition[] conditionsIn) {
        super(conditionsIn);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        for (LootItemCondition condition : this.conditions) {
            if (!condition.test(context)) {
                return generatedLoot;
            }
        }
        if (context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof Parrot parrot) {
            Item feather = switch (parrot.getVariant()) {
                case RED_BLUE -> ModItems.RED_FEATHER.get();
                case BLUE -> ModItems.BLUE_FEATHER.get();
                case GREEN -> ModItems.GREEN_FEATHER.get();
                case YELLOW_BLUE -> ModItems.CYAN_FEATHER.get();
                case GRAY -> ModItems.LIGHT_GRAY_FEATHER.get();
            };
            // Replace the parrot's plain feather drops with the coloured equivalent (same count).
            for (int i = 0; i < generatedLoot.size(); i++) {
                ItemStack stack = generatedLoot.get(i);
                if (stack.is(Items.FEATHER)) {
                    generatedLoot.set(i, new ItemStack(feather, stack.getCount()));
                }
            }
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
