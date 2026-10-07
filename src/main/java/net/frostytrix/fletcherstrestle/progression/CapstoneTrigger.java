package net.frostytrix.fletcherstrestle.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/** Fires when a player buys a capstone; {@code owned} matches how many they now have. */
public class CapstoneTrigger extends SimpleCriterionTrigger<CapstoneTrigger.Instance> {

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public void trigger(ServerPlayer player, int owned) {
        this.trigger(player, inst -> inst.owned().matches(owned));
    }

    public record Instance(Optional<ContextAwarePredicate> player, MinMaxBounds.Ints owned)
            implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                MinMaxBounds.Ints.CODEC.optionalFieldOf("owned", MinMaxBounds.Ints.ANY).forGetter(Instance::owned)
        ).apply(i, Instance::new));
    }
}
