package net.frostytrix.fletcherstrestle.block.entity;

import net.minecraft.nbt.CompoundTag;

public record ShotRecord(
        double x,
        double y,
        double z,
        float u,
        float v,
        float estimatedDamage,
        float speed,
        long timestamp
) {
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("x", x);
        tag.putDouble("y", y);
        tag.putDouble("z", z);
        tag.putFloat("u", u);
        tag.putFloat("v", v);
        tag.putFloat("estimatedDamage", estimatedDamage);
        tag.putFloat("speed", speed);
        tag.putLong("timestamp", timestamp);
        return tag;
    }

    public static ShotRecord fromNBT(CompoundTag tag) {
        // 26.1: CompoundTag getters return Optional<T>; unwrap with orElse.
        return new ShotRecord(
                tag.getDouble("x").orElse(0.0),
                tag.getDouble("y").orElse(0.0),
                tag.getDouble("z").orElse(0.0),
                tag.getFloat("u").orElse(0f),
                tag.getFloat("v").orElse(0f),
                tag.getFloat("estimatedDamage").orElse(0f),
                tag.getFloat("speed").orElse(0f),
                tag.getLong("timestamp").orElse(0L)
        );
    }
}