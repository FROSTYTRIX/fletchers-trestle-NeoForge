package net.frostytrix.fletcherstrestle.item.custom;

import net.frostytrix.fletcherstrestle.entity.ModEntities;
import net.frostytrix.fletcherstrestle.entity.custom.HeavyDummyEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

// Places a HeavyDummy where the player right-clicks. If the held item has
// a CUSTOM_NAME, treats it as a player name and:
//   * sets the dummy's display name to the same component, and
//   * resolves the name to a Mojang profile via the server's
//     ProfileResolver so the dummy adopts that player's skin.
//
// 26.1 changes from the 1.21.1 implementation:
//   * EntityType.create(Level) → EntityType.create(Level, EntitySpawnReason).
//   * Entity.moveTo(double,double,double,float,float) renamed to snapTo.
//   * MinecraftServer.getProfileCache() is gone; profile lookup goes
//     through server.services().profileResolver().fetchByName(String)
//     which is now SYNCHRONOUS (returns Optional immediately).
//   * HeavyDummyEntity.setSkin signature now requires (String, UUID).
public class HeavyDummyItem extends Item {

    public HeavyDummyItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = context.getItemInHand();
        BlockPos clickedPos = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockPos spawnPos = clickedPos.relative(face);

        HeavyDummyEntity dummy = ModEntities.HEAVY_DUMMY.get().create(serverLevel, EntitySpawnReason.MOB_SUMMONED);
        if (dummy == null) return InteractionResult.FAIL;

        // Custom-name → skin lookup.
        if (stack.has(DataComponents.CUSTOM_NAME)) {
            net.minecraft.network.chat.Component nameComponent = stack.get(DataComponents.CUSTOM_NAME);
            dummy.setCustomName(nameComponent);
            dummy.setCustomNameVisible(true);

            String plainName = nameComponent.getString();
            MinecraftServer server = serverLevel.getServer();
            if (server != null && !plainName.isEmpty()) {
                // 26.1: synchronous ProfileResolver lookup. Vanilla caches
                // by name+id internally so calling on the server thread is
                // cheap after the first hit. If the resolver returns empty
                // we still set the name; the renderer will fall back to the
                // default Steve skin.
                java.util.Optional<com.mojang.authlib.GameProfile> profile =
                        server.services().profileResolver().fetchByName(plainName);
                // 26.1: GameProfile became a record — getName/getId
                // → name()/id().
                profile.ifPresentOrElse(
                        p -> dummy.setSkin(p.name(), p.id()),
                        () -> dummy.setSkin(plainName, null));
            } else {
                dummy.setSkin(plainName, null);
            }
        }

        // Snap rotation to the nearest 45° toward the player so the dummy
        // faces away from them.
        Player player = context.getPlayer();
        float yRot = player != null ? player.getYRot() + 180.0F : 0.0F;
        float snappedRot = (float) Mth.floor((Mth.wrapDegrees(yRot) + 22.5F) / 45.0F) * 45.0F;

        // 26.1: moveTo → snapTo.
        dummy.snapTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, snappedRot, 0.0F);
        dummy.setYBodyRot(snappedRot);
        dummy.setYHeadRot(snappedRot);

        serverLevel.addFreshEntity(dummy);

        if (player != null && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResult.SUCCESS;
    }
}
