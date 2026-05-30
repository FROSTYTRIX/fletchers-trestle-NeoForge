package net.frostytrix.fletcherstrestle.progression;

import com.mojang.serialization.Codec;
import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Player data attachments for the marksmanship system (Phase 2).
 *
 * <p>{@code ARCHERY_XP} stores total archery XP on the player.
 * {@code copyOnDeath()} keeps it across respawns — the design says XP is
 * never lost on death.</p>
 */
public final class ModAttachments {
    private ModAttachments() {
    }

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, FletcherTrestle.MOD_ID);

    public static final Supplier<AttachmentType<Integer>> ARCHERY_XP =
            ATTACHMENT_TYPES.register("archery_xp", () ->
                    AttachmentType.builder(() -> 0)
                            .serialize(Codec.INT)
                            .copyOnDeath()
                            .build());

    public static final Supplier<AttachmentType<ArcherySkills>> ARCHERY_SKILLS =
            ATTACHMENT_TYPES.register("archery_skills", () ->
                    AttachmentType.builder(() -> ArcherySkills.EMPTY)
                            .serialize(ArcherySkills.CODEC)
                            .copyOnDeath()
                            .build());

    public static void register(IEventBus bus) {
        ATTACHMENT_TYPES.register(bus);
    }
}
