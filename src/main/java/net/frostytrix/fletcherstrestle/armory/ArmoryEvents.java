package net.frostytrix.fletcherstrestle.armory;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.component.BowAssembly;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.config.FletcherConfig;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.frostytrix.fletcherstrestle.material.Materials;
import net.frostytrix.fletcherstrestle.material.ModMaterialRegistries;
import net.frostytrix.fletcherstrestle.material.RandomWeapon;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Hands modular weapons to mobs as they spawn, per {@link MobArmory}.
 *
 * <p>Runs when a mob first joins the world, after its own spawn logic has given it
 * a vanilla bow or crossbow (and possibly enchanted it). Every listed mob gets the
 * modular equivalent: the plain oak weapon, or, more often in harder regions, one
 * cut from its signature wood. Enchantments carry over, so difficulty still scales
 * the way vanilla intends, and the mob's usual hand drop chance applies.</p>
 */
@EventBusSubscriber(modid = FletcherTrestle.MOD_ID)
public final class ArmoryEvents {
    private ArmoryEvents() {
    }

    /** Marks a mob that already had its roll, so a trip through a portal doesn't reroll it. */
    private static final String ROLLED = "fletcherstrestle:armory_rolled";

    @SubscribeEvent
    public static void registerDataMaps(RegisterDataMapTypesEvent event) {
        event.register(MobArmory.DATA_MAP);
        event.register(NativeWoods.DATA_MAP);
        event.register(net.frostytrix.fletcherstrestle.component.GarlandFeather.DATA_MAP);
    }

    @SubscribeEvent
    public static void onMobJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || !(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getEntity() instanceof Mob mob) || !FletcherConfig.ARMED_MOBS.get()) return;
        if (mob.getPersistentData().getBoolean(ROLLED)) return;
        mob.getPersistentData().putBoolean(ROLLED, true);
        // Only gear the game picked itself. NeoForge records the spawn type in
        // finalizeSpawn, which /summon skips when it's given NBT: whatever that
        // NBT put in the mob's hands is what the player asked for.
        if (mob.getSpawnType() == null) return;

        var key = BuiltInRegistries.ENTITY_TYPE.getResourceKey(mob.getType());
        if (key.isEmpty()) return;
        MobArmory armory = BuiltInRegistries.ENTITY_TYPE.getData(MobArmory.DATA_MAP, key.get());
        if (armory == null) return;

        ItemStack held = mob.getItemInHand(InteractionHand.MAIN_HAND);
        Item modular;
        if (held.is(Items.BOW)) {
            modular = ModItems.MODULAR_BOW.get();
        } else if (held.is(Items.CROSSBOW)) {
            modular = ModItems.MODULAR_CROSSBOW.get();
        } else {
            return;
        }

        ItemStack weapon = new ItemStack(modular);
        weapon.set(ModDataComponents.BOW_ASSEMBLY.get(), buildAssembly(armory, level, mob));
        var enchantments = held.get(DataComponents.ENCHANTMENTS);
        if (enchantments != null) {
            weapon.set(DataComponents.ENCHANTMENTS, enchantments);
        }
        mob.setItemInHand(InteractionHand.MAIN_HAND, weapon);
    }

    /**
     * Every armed mob starts from the plain weapon: both limbs from the base wood,
     * with a riser and string rolled from the entry's weighted tables. Then, more
     * often in harder regions, it's modified: at least one limb becomes a signature
     * wood, and sometimes the two limbs are laminated into a composite with a
     * common wood or a second signature one.
     */
    private static BowAssembly buildAssembly(MobArmory armory, ServerLevel level, Mob mob) {
        RegistryAccess access = level.registryAccess();
        RandomSource random = mob.getRandom();

        List<String> common = RandomWeapon.knownLimbs(access, armory.common());
        String base = common.isEmpty() ? "oak" : common.get(0);
        String top = base;
        String bottom = base;

        float special = level.getCurrentDifficultyAt(mob.blockPosition()).getSpecialMultiplier();
        float chance = Mth.lerp(special, armory.minChance(), armory.maxChance());
        if (random.nextFloat() < chance) {
            List<String> signature = signatureWoods(armory, level, mob, common);
            if (!signature.isEmpty()) {
                top = signature.get(random.nextInt(signature.size()));
                bottom = top;
                // Composites only while the server allows them, since mobs can drop
                // their weapon.
                if (FletcherConfig.COMPOSITE_BOWS.get() && random.nextFloat() < armory.compositeChance()) {
                    List<String> partners = new ArrayList<>(common);
                    for (String wood : signature) {
                        if (!partners.contains(wood)) partners.add(wood);
                    }
                    partners.remove(top);
                    if (!partners.isEmpty()) {
                        String partner = partners.get(random.nextInt(partners.size()));
                        // Either wood may take the upper limb.
                        if (random.nextBoolean()) {
                            bottom = partner;
                        } else {
                            bottom = top;
                            top = partner;
                        }
                    }
                }
            }
        }

        // Riser first, then a string it can carry: the bench's metal-riser rule
        // applies to mobs too, so a heavy string's weight goes to the others on wood.
        String riser = weighted(access, ModMaterialRegistries.BOW_RISER, armory.risers(), id -> true, random, "wood");
        boolean metal = Materials.bowRiser(riser).stats().metal();
        String string = weighted(access, ModMaterialRegistries.BOW_STRING, armory.strings(),
                id -> metal || !Materials.bowString(id).stats().requiresMetalRiser(), random, "spider");
        float tuning = Mth.lerp(random.nextFloat(), armory.minTuning(), armory.maxTuning());
        return top.equals(bottom)
                ? new BowAssembly(top, riser, string, tuning)
                : new BowAssembly(top, Optional.of(bottom), riser, string, tuning);
    }

    /**
     * A weighted pick from an id table, keeping only ids the registry knows and the
     * filter allows. Falls back to {@code fallback} if nothing is left.
     */
    private static <T> String weighted(RegistryAccess access, ResourceKey<Registry<T>> registry,
                                       Map<String, Float> weights, Predicate<String> allowed,
                                       RandomSource random, String fallback) {
        List<String> ids = new ArrayList<>();
        List<Float> chances = new ArrayList<>();
        float total = 0f;
        for (Map.Entry<String, Float> entry : weights.entrySet()) {
            List<String> known = RandomWeapon.knownIds(access, registry, List.of(entry.getKey()));
            if (known.isEmpty() || entry.getValue() <= 0f || !allowed.test(known.get(0))) continue;
            ids.add(known.get(0));
            chances.add(entry.getValue());
            total += entry.getValue();
        }
        if (ids.isEmpty()) return fallback;
        float roll = random.nextFloat() * total;
        for (int i = 0; i < ids.size(); i++) {
            roll -= chances.get(i);
            if (roll < 0f) return ids.get(i);
        }
        return ids.get(ids.size() - 1);
    }

    /**
     * The woods that make a weapon this mob's own: its listed woods, plus the trees
     * of its spawn biome when its entry asks for them, or its fallback when that
     * gives nothing. The common woods are never signature, however they got in.
     */
    private static List<String> signatureWoods(MobArmory armory, ServerLevel level, Mob mob, List<String> common) {
        List<String> woods = new ArrayList<>(armory.woods());
        if (armory.nativeWoods()) {
            NativeWoods growsHere = level.getBiome(mob.blockPosition()).getData(NativeWoods.DATA_MAP);
            if (growsHere != null) {
                woods.addAll(growsHere.woods());
            }
        }
        List<String> signature = RandomWeapon.knownLimbs(level.registryAccess(), woods);
        signature.removeAll(common);
        if (signature.isEmpty()) {
            signature = RandomWeapon.knownLimbs(level.registryAccess(), armory.fallback());
            signature.removeAll(common);
        }
        return signature;
    }
}
