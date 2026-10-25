package net.frostytrix.fletcherstrestle.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Garrison milestones, told apart by {@code event}: {@code field_of_fire} (a
 * warden you built, crewing a garrison of three or more posts, shot down a
 * captain).
 */
public class GarrisonTrigger extends SimpleCriterionTrigger<GarrisonTrigger.Instance> {

    public static final String FIELD_OF_FIRE = "field_of_fire";

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public void trigger(ServerPlayer player, String event) {
        this.trigger(player, inst -> inst.event().equals(event));
    }

    public record Instance(Optional<ContextAwarePredicate> player, String event)
            implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                Codec.STRING.fieldOf("event").forGetter(Instance::event)
        ).apply(i, Instance::new));
    }
}
