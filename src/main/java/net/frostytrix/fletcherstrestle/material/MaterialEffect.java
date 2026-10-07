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

    /**
     * A short trait name for the guidebook's material tables ("Ignited", "Punch"),
     * or empty for effects that don't summarise as a trait. Pack effects that leave
     * this empty simply don't appear there, so the tables never claim more than
     * they can describe.
     */
    default java.util.Optional<net.minecraft.network.chat.Component> describe() {
        return java.util.Optional.empty();
    }

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

    /**
     * Return true to take an entity hit over entirely: the arrow's own specials,
     * vanilla damage and every other effect are skipped. Runs before all of them,
     * so an effect that returns true must finish the arrow off itself (usually
     * with {@code arrow.discard()}).
     */
    default boolean replacesArrowHit(ModularArrowEntity arrow, EntityHitResult result) {
        return false;
    }

    /** The same for a block hit: the arrow neither sticks nor runs other block effects. */
    default boolean replacesArrowHitBlock(ModularArrowEntity arrow, BlockHitResult result) {
        return false;
    }

    /** When a bow/crossbow releases a shot. */
    default void onBowRelease(LivingEntity shooter, ItemStack weapon) {
    }

    /** From the bow/crossbow's {@code createProjectile}: mutate the fired projectile directly. */
    default void onProjectileFired(LivingEntity shooter, ItemStack weapon, Entity projectile) {
    }
}
