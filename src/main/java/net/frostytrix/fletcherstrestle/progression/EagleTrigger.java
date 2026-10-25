package net.frostytrix.fletcherstrestle.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Eagle milestones, told apart by {@code event}: {@code stoop_kill} (a marked
 * target died to your eagle's strike), {@code wingman} (1000 blocks of elytra
 * flight with your eagle alongside), {@code fed_eaglet}, {@code nest_defence}
 * (struck by a nesting eagle and lived) and {@code waxed}.
 */
public class EagleTrigger extends SimpleCriterionTrigger<EagleTrigger.Instance> {

    public static final String STOOP_KILL = "stoop_kill";
    public static final String WINGMAN = "wingman";
    public static final String FED_EAGLET = "fed_eaglet";
    public static final String NEST_DEFENCE = "nest_defence";
    public static final String WAXED = "waxed";

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
