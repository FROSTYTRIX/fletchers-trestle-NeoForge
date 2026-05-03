package net.frostytrix.fletcherstrestle.item.custom;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.stream.Collectors;

public class FletcherGuideItem extends Item {

    public FletcherGuideItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide) {
            ItemStack manual = createManual();
            // Give the book to the player and shrink the "guide" item
            if (!player.getInventory().add(manual)) {
                player.drop(manual, false);
            }
            player.getItemInHand(hand).shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    private ItemStack createManual() {
        ItemStack manual = new ItemStack(Items.WRITTEN_BOOK);

        List<Component> pages = List.of(
                Component.literal("A true fletcher carves with precision. By sitting at the Shaving Horse with a Log and a Drawknife, you can strip the wood down into a Rough Limb. This is the very first step in crafting the limbs of your bow."),
                Component.literal("Before a bow can be shaped, the wood must be softened. The Steam Box achieves this. Fill it with water and place a heat source (like a campfire) beneath it. Once heated, insert a Rough Limb to steam it into a Pliable Limb."),
                Component.literal("The Fletching Table is no longer just for villagers. It is now your ultimate workstation. Use this table to combine your crafted components and assemble highly advanced Modular Bows and Modular Arrows tailored to your needs."),
                Component.literal("To master your craft, build the Archery Target. Unlike standard targets, this advanced dummy will show you exactly where your arrow landed and (not so) precisely how much damage it dealt. Calibrate your arsenal!"),
                Component.literal("Risers:\n- Wood: Standard (250 uses).\n- Iron: Highly accurate (400 uses).\n- Copper: Lightning during storms! (750 uses).\nStrings:\n- Spider: Standard.\n- Flax: +30% speed, shaky aim.\n- High Tension: +80% speed, costs 2 dur."),
                Component.literal("Limbs: \n - Oak: Standard (1s).\n- Spruce: Built-in Punch.\n- Birch: Fast (0.5s), less dmg.\n- Jungle: No slowdown.\n- Acacia: Speed effect on fire.\n- Dark Oak: Slow, high dmg (1.6x)."),
                Component.literal("- Mangrove: No water drag.\n- Cherry: Slow falling mid-air.\n- Crimson: Zero gravity.\n- Warped: Built-in Flame.\n- Pale Oak: Balanced."),
                Component.literal("-Heads: \n- Flint: Standard.\n- Broadhead: Bleeding (3s).\n- Bodkin: Ignores 25% Armor.\n- Resonance: Echoes 30% dmg.\n- Barbed: Slows target.\n- Blunt: Dmg scales with distance."),
                Component.literal("Shafts:\n- Oak: Standard.\n- Spruce: +10% Gravity.\n- Birch: Faster, weaker.\n- Jungle: Bounces off walls!\n- Dark Oak: +30% Knockback.\n- Acacia: Accelerates mid-flight.\n- Warped: Swaps position!"),
                Component.literal("- Crimson: +25% dmg to low HP targets.\n- Pale Oak: +30% dmg if target isn't looking.\n\nFletchings:\nAttach fletchings like Serrated (homing), Vex (phases blocks), or Bound (drops on hit).")
        );

        List<Filterable<Component>> filterablePages = pages.stream()
                .map(Filterable::passThrough)
                .collect(Collectors.toList());

        WrittenBookContent bookContent = new WrittenBookContent(
                Filterable.passThrough("The Fletcher's Manual"), // Title wrapped in Filterable
                "Master Fletcher", // Author (this remains a String)
                0, // Generation
                filterablePages, // Pages wrapped in Filterable
                true // Resolved
        );

        manual.set(DataComponents.WRITTEN_BOOK_CONTENT, bookContent);

        return manual;
    }
}