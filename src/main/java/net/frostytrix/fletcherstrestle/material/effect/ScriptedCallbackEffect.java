package net.frostytrix.fletcherstrestle.material.effect;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.entity.custom.ModularArrowEntity;
import net.frostytrix.fletcherstrestle.material.MaterialEffect;
import net.frostytrix.fletcherstrestle.material.MaterialEffectType;
import net.frostytrix.fletcherstrestle.material.ModMaterialEffectTypes;
import net.frostytrix.fletcherstrestle.material.ScriptedEffectCallbacks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Bridges a JSON-declared effect to a Java-registered callback, so KubeJS scripts and companion
 * mods can attach custom behavior to a material without a new {@link MaterialEffectType}. See
 * {@link ScriptedEffectCallbacks} for design notes and a usage example. If no handler is registered
 * for {@code id}, the effect no-ops and logs a single warning.
 *
 * <p>JSON: {@code { "type": "fletcherstrestle:scripted_callback", "id": "mypack:my_hit" }}
 */
public record ScriptedCallbackEffect(ResourceLocation id) implements MaterialEffect {

    public static final MapCodec<ScriptedCallbackEffect> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(ScriptedCallbackEffect::id)
    ).apply(inst, ScriptedCallbackEffect::new));

    @Override
    public MaterialEffectType<? extends MaterialEffect> type() {
        return ModMaterialEffectTypes.SCRIPTED_CALLBACK.get();
    }

    private ScriptedEffectCallbacks.Handler handler() {
        return ScriptedEffectCallbacks.get(id);
    }

    @Override
    public void onArrowSpawn(ModularArrowEntity arrow) {
        handler().onArrowSpawn(arrow);
    }

    @Override
    public void onArrowTick(ModularArrowEntity arrow) {
        handler().onArrowTick(arrow);
    }

    @Override
    public void onPreArrowHit(ModularArrowEntity arrow, EntityHitResult result) {
        handler().onPreArrowHit(arrow, result);
    }

    @Override
    public void onArrowHit(ModularArrowEntity arrow, EntityHitResult result) {
        handler().onArrowHit(arrow, result);
    }

    @Override
    public void onArrowHitBlock(ModularArrowEntity arrow, BlockHitResult result) {
        handler().onArrowHitBlock(arrow, result);
    }

    @Override
    public void onBowRelease(LivingEntity shooter, ItemStack weapon) {
        handler().onBowRelease(shooter, weapon);
    }

    @Override
    public void onProjectileFired(LivingEntity shooter, ItemStack weapon, Entity projectile) {
        handler().onProjectileFired(shooter, weapon, projectile);
    }
}
