package net.frostytrix.fletcherstrestle.client;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;

// Custom ItemTintSource that reads the PotionContents data component from
// a stack and returns its display colour. Used by the modular arrow's
// glass-vial liquid layer so dipping the arrow into a coloured potion
// changes the overlay tint to match.
//
// 26.1 ITEM TINT REGISTRATION
//   * RegisterColorHandlersEvent.Item is gone.
//   * Tint sources are now value-typed MapCodec entries registered via
//     RegisterColorHandlersEvent.ItemTintSources (see ModColorHandlers).
//   * The model JSON references this by id with a "tints" array entry
//     like {"type": "fletcherstrestle:potion", "default": -13083194}.
public record PotionTintSource(int defaultColor) implements ItemTintSource {

    public static final MapCodec<PotionTintSource> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            ByteBufCodecs.INT.toCodec().fieldOf("default").forGetter(PotionTintSource::defaultColor)
    ).apply(inst, PotionTintSource::new));

    @Override
    public int calculate(ItemStack stack, ClientLevel level, LivingEntity holder) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) {
            return this.defaultColor;
        }
        // PotionContents.getColor() returns an opaque RGB; OR in full alpha
        // so the layer doesn't render as transparent.
        return 0xFF000000 | (contents.getColor() & 0xFFFFFF);
    }

    @Override
    public MapCodec<? extends ItemTintSource> type() {
        return MAP_CODEC;
    }
}
