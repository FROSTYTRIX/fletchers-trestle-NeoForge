package net.frostytrix.fletcherstrestle.progression;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Admin/testing commands for the archery skill system (op level 2).
 *
 * <ul>
 *   <li>{@code /archery xp <amount>} — grant XP (levels = spendable points).</li>
 *   <li>{@code /archery reset} — wipe XP and spent ranks.</li>
 *   <li>{@code /archery info} — print level, points, and ranks.</li>
 * </ul>
 */
@EventBusSubscriber(modid = FletcherTrestle.MOD_ID)
public final class ModArcheryCommands {
    private ModArcheryCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("archery")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("xp")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                            ArcheryProgression.addXp(player, amount);
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "Granted " + amount + " archery XP (now level "
                                                            + ArcheryProgression.getLevel(player) + ")"), false);
                                            return 1;
                                        })))
                        .then(Commands.literal("reset")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    player.setData(ModAttachments.ARCHERY_XP.get(), 0);
                                    player.setData(ModAttachments.ARCHERY_SKILLS.get(), ArcherySkills.EMPTY);
                                    ArcheryProgression.syncToClient(player);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Archery progress reset."), false);
                                    return 1;
                                }))
                        .then(Commands.literal("info")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    ArcherySkills s = ArcheryProgression.getSkills(player);
                                    ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                                            "Level %d | Points: %d | draw %d, crit %d, aim %d",
                                            ArcheryProgression.getLevel(player),
                                            ArcheryProgression.pointsAvailable(player),
                                            s.draw(), s.crit(), s.aim())), false);
                                    return 1;
                                }))
        );
    }
}
