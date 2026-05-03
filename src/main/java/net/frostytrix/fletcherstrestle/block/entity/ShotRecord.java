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
        return new ShotRecord(
                tag.getDouble("x"),
                tag.getDouble("y"),
                tag.getDouble("z"),
                tag.getFloat("u"),
                tag.getFloat("v"),
                tag.getFloat("estimatedDamage"),
                tag.getFloat("speed"),
                tag.getLong("timestamp")
        );
    }
}