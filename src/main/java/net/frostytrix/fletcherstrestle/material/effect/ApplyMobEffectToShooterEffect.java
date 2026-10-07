package net.frostytrix.fletcherstrestle.material.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.material.MaterialEffect;
import net.frostytrix.fletcherstrestle.material.MaterialEffectType;
import net.frostytrix.fletcherstrestle.material.ModMaterialEffectTypes;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * On-release: applies a MobEffect to the shooter, not the target. Used by the acacia limb's
 * brief speed buff on release.
 *
 * <p>JSON: {@code { "type": "fletcherstrestle:apply_effect_to_shooter", "effect": "minecraft:speed",
 * "duration": 30, "amplifier": 1 }}
 */
public record ApplyMobEffectToShooterEffect(
        Holder<MobEffect> effect, int duration, int amplifier) implements MaterialEffect {

    public static final MapCodec<ApplyMobEffectToShooterEffect> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            MobEffect.CODEC.fieldOf("effect").forGetter(ApplyMobEffectToShooterEffect::effect),
            Codec.INT.fieldOf("duration").forGetter(ApplyMobEffectToShooterEffect::duration),
            Codec.INT.optionalFieldOf("amplifier", 0).forGetter(ApplyMobEffectToShooterEffect::amplifier)
    ).apply(inst, ApplyMobEffectToShooterEffect::new));

    /** "Speedy" for the acacia speed buff; any other effect reads as "<effect> on release". */
    @Override
    public java.util.Optional<net.minecraft.network.chat.Component> describe() {
        String named = effect.unwrapKey()
                .map(k -> "trait.fletcherstrestle.shooter." + k.location().getPath())
                .orElse("");
        if (!named.isEmpty() && net.minecraft.locale.Language.getInstance().has(named)) {
            return java.util.Optional.of(net.minecraft.network.chat.Component.translatable(named));
        }
        return java.util.Optional.of(net.minecraft.network.chat.Component.translatable(
                "trait.fletcherstrestle.shooter_effect", effect.value().getDisplayName()));
    }

    @Override
    public MaterialEffectType<? extends MaterialEffect> type() {
        return ModMaterialEffectTypes.APPLY_EFFECT_TO_SHOOTER.get();
    }

    @Override
    public void onBowRelease(LivingEntity shooter, ItemStack weapon) {
        shooter.addEffect(new MobEffectInstance(effect, duration, amplifier, false, false, true));
    }
}
