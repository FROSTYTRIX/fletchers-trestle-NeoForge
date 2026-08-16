package net.frostytrix.fletcherstrestle.attachment;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * Slot category for a crossbow attachment.
 *
 * <p>v1 ships a single universal attachment slot, so this field isn't
 * enforced yet, but every def declares one so that a future "one optic +
 * one mechanism" multi-slot rule can be added without touching existing
 * JSON.</p>
 */
public enum AttachmentType implements StringRepresentable {
    /** Sights / scopes: affect aim (zoom, sway, inaccuracy). */
    OPTIC("optic"),
    /** Firing mechanisms: affect reload / magazine behavior. */
    MECHANISM("mechanism");

    public static final Codec<AttachmentType> CODEC =
            StringRepresentable.fromEnum(AttachmentType::values);

    private final String name;

    AttachmentType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
