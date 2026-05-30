package net.frostytrix.fletcherstrestle.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/** Fires when a player lands an arrow headshot; carries the shot distance. */
public class HeadshotTrigger extends SimpleCriterionTrigger<HeadshotTrigger.Instance> {

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public void trigger(ServerPlayer player, double distance) {
        double distSq = distance * distance;
        this.trigger(player, inst -> inst.matches(distSq));
    }

    public record Instance(Optional<ContextAwarePredicate> player, MinMaxBounds.Doubles distance)
            implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                MinMaxBounds.Doubles.CODEC.optionalFieldOf("distance", MinMaxBounds.Doubles.ANY).forGetter(Instance::distance)
        ).apply(i, Instance::new));

        public boolean matches(double distanceSq) {
            return this.distance.matchesSqr(distanceSq);
        }
    }
}
