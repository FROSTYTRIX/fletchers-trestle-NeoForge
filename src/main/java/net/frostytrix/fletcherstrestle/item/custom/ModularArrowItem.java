package net.frostytrix.fletcherstrestle.item.custom;

import net.frostytrix.fletcherstrestle.component.ArrowAssembly;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.entity.custom.ModularArrowEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ModularArrowItem extends ArrowItem {


    public ModularArrowItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        ArrowAssembly assembly = stack.get(ModDataComponents.ARROW_ASSEMBLY.get());
        PotionContents potion = stack.get(DataComponents.POTION_CONTENTS);

        if (assembly == null) {
            tooltipComponents.add(Component.translatable("gui.fletcherstrestle.unfinished_arrow").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
            return;
        }

        if (!Screen.hasShiftDown()) {
            tooltipComponents.add(Component.translatable("gui.fletcherstrestle.hold_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            return;
        }

        // Assembly parts
        tooltipComponents.add(Component.translatable("gui.fletcherstrestle.arrow_parts").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        tooltipComponents.add(Component.literal("- ").append(Component.translatable("gui.fletcherstrestle.head")).append(": ")
                .append(net.frostytrix.fletcherstrestle.material.Materials.arrowHeadName(assembly.head())).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal("- ").append(Component.translatable("gui.fletcherstrestle.shaft")).append(": ")
                .append(net.frostytrix.fletcherstrestle.material.Materials.arrowShaftName(assembly.shaft())).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal("- ").append(Component.translatable("gui.fletcherstrestle.fletching")).append(": ")
                .append(net.frostytrix.fletcherstrestle.material.Materials.arrowFletchingName(assembly.fletching())).withStyle(ChatFormatting.GRAY));

        // Potion contents (when present: for glass-vial arrows after dipping)
        if (potion != null) {
            tooltipComponents.add(Component.translatable("gui.fletcherstrestle.effects").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
            // Full duration shown here; effects fall off by distance at hit-time (see applyGlassVialEffect).
            potion.addPotionTooltip(tooltipComponents::add, 1.0F, 20.0F);
        }
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack ammo, LivingEntity shooter, @Nullable ItemStack weapon) {
        // The ammo stack carries the ArrowAssembly component the entity reads.
        return new ModularArrowEntity(level, shooter, ammo.copy(), weapon);
    }

    // Lets a dispenser fire a modular arrow (carrying its assembly) instead of a vanilla arrow.
    @Override
    public Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        ModularArrowEntity arrow = new ModularArrowEntity(
                level, pos.x(), pos.y(), pos.z(), stack.copyWithCount(1), null);
        arrow.pickup = AbstractArrow.Pickup.ALLOWED;
        return arrow;
    }
}
