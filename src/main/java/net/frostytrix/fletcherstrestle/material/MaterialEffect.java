package net.frostytrix.fletcherstrestle.material;

import com.mojang.serialization.Codec;
import net.frostytrix.fletcherstrestle.entity.custom.ModularArrowEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/**
 * A declarative behavior attached to a bow/arrow material via JSON. Each implementation is a record
 * holding params parsed via its {@link MaterialEffectType#codec()}; {@link #CODEC} dispatches by the
 * effect's registered type id. The type set is open: companion mods register their own through
 * {@link ModMaterialEffectTypes#EFFECT_TYPES}. Every lifecycle hook below defaults to no-op so an
 * effect only overrides what it needs.
 */
public interface MaterialEffect {
    /** Dispatches by registered effect-type id, reading the live registry (post-init types work). */
    Codec<MaterialEffect> CODEC = ModMaterialEffectTypes.REGISTRY
            .byNameCodec()
            .dispatch(MaterialEffect::type, MaterialEffectType::codec);

    MaterialEffectType<? extends MaterialEffect> type();

    /** Once, when the arrow is added to the world. */
    default void onArrowSpawn(ModularArrowEntity arrow) {
    }

    /** Every server tick while in flight. */
    default void onArrowTick(ModularArrowEntity arrow) {
    }

    /** Before vanilla hit resolution: for damage modifiers that must affect this hit. */
    default void onPreArrowHit(ModularArrowEntity arrow, EntityHitResult result) {
    }

    /** After vanilla hit resolution: for side effects that don't change this hit's damage. */
    default void onArrowHit(ModularArrowEntity arrow, EntityHitResult result) {
    }

    default void onArrowHitBlock(ModularArrowEntity arrow, BlockHitResult result) {
    }

    /** When a bow/crossbow releases a shot. */
    default void onBowRelease(LivingEntity shooter, ItemStack weapon) {
    }

    /** From the bow/crossbow's {@code createProjectile}: mutate the fired projectile directly. */
    default void onProjectileFired(LivingEntity shooter, ItemStack weapon, Entity projectile) {
    }
}
