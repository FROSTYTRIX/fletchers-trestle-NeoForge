package net.frostytrix.fletcherstrestle.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/** Fires when a player's archery level changes; carries the new level. */
public class ArcheryLevelTrigger extends SimpleCriterionTrigger<ArcheryLevelTrigger.Instance> {

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public void trigger(ServerPlayer player, int level) {
        this.trigger(player, inst -> inst.matches(level));
    }

    public record Instance(Optional<ContextAwarePredicate> player, MinMaxBounds.Ints level)
            implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                MinMaxBounds.Ints.CODEC.optionalFieldOf("level", MinMaxBounds.Ints.ANY).forGetter(Instance::level)
        ).apply(i, Instance::new));

        public boolean matches(int level) {
            return this.level.matches(level);
        }
    }
}
