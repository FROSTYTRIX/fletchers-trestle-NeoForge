package net.frostytrix.fletcherstrestle.item.custom;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

// TODO(port-26.1): HeavyDummy spawn flow stubbed.
// The 1.21.1 implementation:
//   * called EntityType.create(serverLevel) — new signature now takes
//     EntitySpawnReason as a second arg.
//   * used MinecraftServer.getProfileCache().getAsync(name, callback) — the
//     accessor was renamed/moved.
//   * called entity.moveTo(...) — renamed to snapTo(...).
//   * called HeavyDummyEntity.setSkin(name, uuid) — signature changed
//     alongside the entity's own port.
// Until HeavyDummyEntity itself is properly ported (entity API rewrite is
// pending), this item is a no-op so the build keeps compiling.
public class HeavyDummyItem extends Item {
    public HeavyDummyItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return InteractionResult.PASS;
    }
}
