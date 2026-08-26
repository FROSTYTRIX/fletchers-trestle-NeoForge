package net.frostytrix.fletcherstrestle.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.attachment.ModCrossbowAttachments;
import net.frostytrix.fletcherstrestle.component.ArrowAssembly;
import net.frostytrix.fletcherstrestle.component.BowAssembly;
import net.frostytrix.fletcherstrestle.config.FletcherConfig;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.frostytrix.fletcherstrestle.material.MaterialResolver;
import net.frostytrix.fletcherstrestle.material.ModMaterialRegistries;
import net.frostytrix.fletcherstrestle.progression.ArcheryProgression;
import net.frostytrix.fletcherstrestle.progression.ArcherySkill;
import net.frostytrix.fletcherstrestle.progression.ArcherySkills;
import net.frostytrix.fletcherstrestle.progression.ModAttachments;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Admin / pack-maker commands under {@code /fletcherstrestle} (alias
 * {@code /ft}), op level 2:
 *
 * <ul>
 *   <li>{@code give bow|arrow|crossbow …}: spawn finished gear with chosen
 *       materials, skipping the craft pipeline. Bows and crossbows
     *       take an optional trailing {@code lower_limb} to build a composite
     *       from two woods.</li>
 *   <li>{@code materials} / {@code attachments}: list loaded datapack defs.</li>
 *   <li>{@code dump}: write all ids + translation keys to a file.</li>
 *   <li>{@code archery xp|reset|max|info}: archery progress (also at the
 *       top-level alias {@code /archery}).</li>
 * </ul>
 */
@EventBusSubscriber(modid = FletcherTrestle.MOD_ID)
public final class ModCommands {
    private ModCommands() {
    }

    /** Stands in for "no attachment", so the arguments behind it stay reachable. */
    private static final String NO_ATTACHMENT = "none";

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();

        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("fletcherstrestle")
                .requires(s -> s.hasPermission(2))
                .then(giveTree())
                .then(Commands.literal("materials").executes(ModCommands::listMaterials))
                .then(Commands.literal("attachments").executes(ModCommands::listAttachments))
                .then(Commands.literal("dump").executes(ModCommands::dump))
                .then(archeryTree());

        LiteralCommandNode<CommandSourceStack> node = d.register(root);
        d.register(Commands.literal("ft").requires(s -> s.hasPermission(2)).redirect(node));
        d.register(archeryTree()); // keep /archery as a top-level alias
    }

    // ---------------- give ----------------

    private static LiteralArgumentBuilder<CommandSourceStack> giveTree() {
        LiteralArgumentBuilder<CommandSourceStack> bow = Commands.literal("bow")
                .then(matArg("limb", ModMaterialRegistries.BOW_LIMB)
                        .then(matArg("riser", ModMaterialRegistries.BOW_RISER)
                                .then(matArg("string", ModMaterialRegistries.BOW_STRING)
                                        .executes(ModCommands::giveBow)
                                        .then(Commands.argument("tuning", FloatArgumentType.floatArg(0f, 1f))
                                                .executes(ModCommands::giveBow)
                                                .then(matArg("lower_limb", ModMaterialRegistries.BOW_LIMB)
                                                        .executes(ModCommands::giveBow))))));

        LiteralArgumentBuilder<CommandSourceStack> arrow = Commands.literal("arrow")
                .then(matArg("head", ModMaterialRegistries.ARROW_HEAD)
                        .then(matArg("shaft", ModMaterialRegistries.ARROW_SHAFT)
                                .then(matArg("fletching", ModMaterialRegistries.ARROW_FLETCHING)
                                        .executes(c -> giveArrow(c, 1))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                                .executes(c -> giveArrow(c, IntegerArgumentType.getInteger(c, "count")))))));

        // The crossbow takes its attachment where the bow takes its tuning, so
        // tuning follows it rather than leading: existing invocations keep
        // working, and the optional tail reads attachment, tuning, lower_limb.
        LiteralArgumentBuilder<CommandSourceStack> crossbow = Commands.literal("crossbow")
                .then(matArg("limb", ModMaterialRegistries.BOW_LIMB)
                        .then(matArg("riser", ModMaterialRegistries.BOW_RISER)
                                .then(matArg("string", ModMaterialRegistries.BOW_STRING)
                                        .executes(ModCommands::giveCrossbow)
                                        .then(Commands.argument("attachment", StringArgumentType.string())
                                                .suggests(suggestAttachments())
                                                .executes(ModCommands::giveCrossbow)
                                                .then(Commands.argument("tuning", FloatArgumentType.floatArg(0f, 1f))
                                                        .executes(ModCommands::giveCrossbow)
                                                        .then(matArg("lower_limb", ModMaterialRegistries.BOW_LIMB)
                                                                .executes(ModCommands::giveCrossbow)))))));

        return Commands.literal("give").then(bow).then(arrow).then(crossbow);
    }

    private static int giveBow(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer player = c.getSource().getPlayerOrException();
        ItemStack stack = new ItemStack(ModItems.MODULAR_BOW.get());
        BowAssembly assembly = readAssembly(c);
        stack.set(ModDataComponents.BOW_ASSEMBLY.get(), assembly);
        return giveStack(c, player, stack, describe(assembly, "modular bow"));
    }

    /**
     * Builds an assembly from the give arguments. {@code tuning} defaults to a
     * perfect 1.0, and supplying {@code lower_limb} with a different wood makes
     * the weapon a composite.
     *
     * <p>Deliberately not gated on the {@code composite_bows} config: this is
     * an op-level tool for testing and pack authoring, so it hands over what
     * was asked for and says so, rather than refusing.</p>
     */
    private static BowAssembly readAssembly(CommandContext<CommandSourceStack> c) {
        String limb = StringArgumentType.getString(c, "limb");
        String riser = StringArgumentType.getString(c, "riser");
        String string = StringArgumentType.getString(c, "string");
        float tuning = optional(c, "tuning", Float.class, 1.0f);
        String lower = optional(c, "lower_limb", String.class, null);

        return lower == null || lower.equals(limb)
                ? new BowAssembly(limb, riser, string, tuning)
                : new BowAssembly(limb, Optional.of(lower), riser, string, tuning);
    }

    /**
     * Reads an argument that may not be on the branch that was run. Brigadier
     * has no "is this present" query: asking for an argument the parsed command
     * does not have throws, so the throw is the test.
     */
    private static <T> T optional(CommandContext<CommandSourceStack> c, String name, Class<T> type, T fallback) {
        try {
            return c.getArgument(name, type);
        } catch (IllegalArgumentException absent) {
            return fallback;
        }
    }

    /** Names the weapon, calling out a composite and whether the config allows one. */
    private static String describe(BowAssembly assembly, String label) {
        if (!assembly.isComposite()) {
            return label;
        }
        String note = FletcherConfig.COMPOSITE_BOWS.get()
                ? ""
                : " (note: composite_bows is off, so this one cannot be crafted)";
        return "composite " + label + " (" + assembly.limbMaterial() + " over "
                + assembly.secondLimb().orElseThrow() + ")" + note;
    }

    private static int giveArrow(CommandContext<CommandSourceStack> c, int count) throws CommandSyntaxException {
        ServerPlayer player = c.getSource().getPlayerOrException();
        ItemStack stack = new ItemStack(ModItems.MODULAR_ARROW.get(), count);
        stack.set(ModDataComponents.ARROW_ASSEMBLY.get(), new ArrowAssembly(
                StringArgumentType.getString(c, "head"),
                StringArgumentType.getString(c, "shaft"),
                StringArgumentType.getString(c, "fletching")));
        return giveStack(c, player, stack, count + "x modular arrow");
    }

    private static int giveCrossbow(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer player = c.getSource().getPlayerOrException();
        String attachment = optional(c, "attachment", String.class, null);
        if (NO_ATTACHMENT.equalsIgnoreCase(attachment)) {
            attachment = null;
        }
        ItemStack stack = new ItemStack(ModItems.MODULAR_CROSSBOW.get());
        BowAssembly assembly = readAssembly(c);
        stack.set(ModDataComponents.BOW_ASSEMBLY.get(), assembly);
        if (attachment != null) {
            // Bare path -> this mod's namespace; explicit "ns:path" parsed as-is.
            ResourceLocation id = attachment.indexOf(':') >= 0
                    ? ResourceLocation.tryParse(attachment)
                    : ResourceLocation.tryBuild(FletcherTrestle.MOD_ID, attachment);
            if (id == null) {
                c.getSource().sendFailure(Component.literal("Invalid attachment id: " + attachment));
                return 0;
            }
            stack.set(ModDataComponents.CROSSBOW_ATTACHMENT.get(), id);
        }
        return giveStack(c, player, stack, describe(assembly, "modular crossbow"));
    }

    private static int giveStack(CommandContext<CommandSourceStack> c, ServerPlayer player, ItemStack stack, String label) {
        player.getInventory().placeItemBackInInventory(stack);
        c.getSource().sendSuccess(() -> Component.literal("Gave a " + label + "."), false);
        return 1;
    }

    // ---------------- inspection ----------------

    private static int listMaterials(CommandContext<CommandSourceStack> c) {
        CommandSourceStack src = c.getSource();
        listRegistry(src, "bow_limb", ModMaterialRegistries.BOW_LIMB);
        listRegistry(src, "bow_riser", ModMaterialRegistries.BOW_RISER);
        listRegistry(src, "bow_string", ModMaterialRegistries.BOW_STRING);
        listRegistry(src, "arrow_head", ModMaterialRegistries.ARROW_HEAD);
        listRegistry(src, "arrow_shaft", ModMaterialRegistries.ARROW_SHAFT);
        listRegistry(src, "arrow_fletching", ModMaterialRegistries.ARROW_FLETCHING);
        return 1;
    }

    private static int listAttachments(CommandContext<CommandSourceStack> c) {
        listRegistry(c.getSource(), "crossbow_attachment", ModCrossbowAttachments.CROSSBOW_ATTACHMENT);
        return 1;
    }

    private static <T> void listRegistry(CommandSourceStack src, String label, ResourceKey<Registry<T>> key) {
        Registry<T> reg = src.registryAccess().registryOrThrow(key);
        String ids = reg.keySet().stream().map(ResourceLocation::toString).sorted().collect(Collectors.joining(", "));
        src.sendSuccess(() -> Component.literal(label + " (" + reg.size() + "): " + (ids.isEmpty() ? "<none>" : ids)), false);
    }

    private static int dump(CommandContext<CommandSourceStack> c) {
        CommandSourceStack src = c.getSource();
        StringBuilder sb = new StringBuilder("Fletcher's Trestle registry dump\n");
        appendDump(src, sb, "bow_limb", ModMaterialRegistries.BOW_LIMB, true);
        appendDump(src, sb, "bow_riser", ModMaterialRegistries.BOW_RISER, true);
        appendDump(src, sb, "bow_string", ModMaterialRegistries.BOW_STRING, true);
        appendDump(src, sb, "arrow_head", ModMaterialRegistries.ARROW_HEAD, true);
        appendDump(src, sb, "arrow_shaft", ModMaterialRegistries.ARROW_SHAFT, true);
        appendDump(src, sb, "arrow_fletching", ModMaterialRegistries.ARROW_FLETCHING, true);
        appendDump(src, sb, "crossbow_attachment", ModCrossbowAttachments.CROSSBOW_ATTACHMENT, false);

        Path path = Path.of("fletcherstrestle_registry_dump.txt").toAbsolutePath();
        try {
            Files.writeString(path, sb.toString());
            src.sendSuccess(() -> Component.literal("Wrote registry dump to " + path), false);
            return 1;
        } catch (IOException e) {
            src.sendFailure(Component.literal("Dump failed: " + e.getMessage()));
            return 0;
        }
    }

    private static <T> void appendDump(CommandSourceStack src, StringBuilder sb, String label,
                                       ResourceKey<Registry<T>> key, boolean material) {
        Registry<T> reg = src.registryAccess().registryOrThrow(key);
        sb.append("\n# ").append(label).append(" (").append(reg.size()).append(")\n");
        reg.keySet().stream().map(ResourceLocation::toString).sorted().forEach(id -> {
            ResourceLocation rl = ResourceLocation.parse(id);
            String langKey = material ? MaterialResolver.displayKey(rl) : ModCrossbowAttachments.displayKey(rl);
            sb.append("  ").append(id).append("    \"").append(langKey).append("\"\n");
        });
    }

    // ---------------- archery ----------------

    private static LiteralArgumentBuilder<CommandSourceStack> archeryTree() {
        return Commands.literal("archery").requires(s -> s.hasPermission(2))
                .then(Commands.literal("xp")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(c -> {
                                    ServerPlayer p = c.getSource().getPlayerOrException();
                                    int amount = IntegerArgumentType.getInteger(c, "amount");
                                    ArcheryProgression.addXp(p, amount);
                                    c.getSource().sendSuccess(() -> Component.literal(
                                            "Granted " + amount + " XP (level " + ArcheryProgression.getLevel(p) + ")"), false);
                                    return 1;
                                })))
                .then(Commands.literal("reset").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    p.setData(ModAttachments.ARCHERY_XP.get(), 0);
                    p.setData(ModAttachments.ARCHERY_SKILLS.get(), ArcherySkills.EMPTY);
                    ArcheryProgression.syncToClient(p);
                    c.getSource().sendSuccess(() -> Component.literal("Archery progress reset."), false);
                    return 1;
                }))
                .then(Commands.literal("max").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    int maxRanks = ArcherySkill.MAX_RANK;
                    p.setData(ModAttachments.ARCHERY_XP.get(),
                            ArcheryProgression.xpForLevel(maxRanks * ArcherySkill.values().length));
                    p.setData(ModAttachments.ARCHERY_SKILLS.get(),
                            new ArcherySkills(maxRanks, maxRanks, maxRanks));
                    ArcheryProgression.syncToClient(p);
                    c.getSource().sendSuccess(() -> Component.literal("Maxed all archery skills."), false);
                    return 1;
                }))
                .then(Commands.literal("info").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    ArcherySkills s = ArcheryProgression.getSkills(p);
                    c.getSource().sendSuccess(() -> Component.literal(String.format(
                            "Level %d | Points: %d | draw %d, crit %d, aim %d",
                            ArcheryProgression.getLevel(p), ArcheryProgression.pointsAvailable(p),
                            s.draw(), s.crit(), s.aim())), false);
                    return 1;
                }));
    }

    // ---------------- helpers ----------------

    private static <T> RequiredArgumentBuilder<CommandSourceStack, String> matArg(String name, ResourceKey<Registry<T>> key) {
        return Commands.argument(name, StringArgumentType.string()).suggests(suggest(key));
    }

    /**
     * Attachment ids, plus {@code none}.
     *
     * <p>The attachment sits ahead of {@code tuning} on the branch, so without
     * a way to say "no attachment" there is no way to reach the arguments
     * behind it. {@code none} is that way through.</p>
     */
    private static SuggestionProvider<CommandSourceStack> suggestAttachments() {
        SuggestionProvider<CommandSourceStack> ids = suggest(ModCrossbowAttachments.CROSSBOW_ATTACHMENT);
        return (ctx, builder) -> {
            if (NO_ATTACHMENT.startsWith(builder.getRemaining().toLowerCase(Locale.ROOT))) {
                builder.suggest(NO_ATTACHMENT);
            }
            return ids.getSuggestions(ctx, builder);
        };
    }

    private static <T> SuggestionProvider<CommandSourceStack> suggest(ResourceKey<Registry<T>> key) {
        // Suggest the bare path for this mod's own entries (a String arg can't
        // contain an unquoted colon); other namespaces are quoted so they
        // still parse. The resolver defaults a bare id to this mod's namespace.
        return (ctx, builder) -> SharedSuggestionProvider.suggest(
                ctx.getSource().registryAccess().registryOrThrow(key).keySet().stream()
                        .map(rl -> rl.getNamespace().equals(FletcherTrestle.MOD_ID)
                                ? rl.getPath() : "\"" + rl + "\""),
                builder);
    }
}
