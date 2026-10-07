package net.frostytrix.fletcherstrestle.entity.custom;

import net.frostytrix.fletcherstrestle.garrison.CrewPostGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The garrison golem: a clockwork crew for emplacements. It has no weapon of its
 * own and never targets players; its only job is to run to a loaded post that can
 * see a hostile mob, and work the crossbow on it.
 *
 * <p>Its drops (the Mechanical Trigger that wound it up, and a couple of logs) are
 * the {@code entities/garrison_golem} loot table.</p>
 *
 * <p>It stays within {@link #homeRadius()} blocks of where it was wound up, and
 * crews any post in that area, so one golem can serve several. Arrows it fires
 * belong to it, so its kills give no archery XP.</p>
 */
public class GarrisonGolemEntity extends AbstractGolem {

    /** How far from home it wanders and looks for posts. */
    public static int homeRadius() {
        return net.frostytrix.fletcherstrestle.config.FletcherConfig.GARRISON_HOME_RADIUS.get();
    }

    @Nullable
    private BlockPos home;
    /** Where the next bolt leaves from, while a shot is being fired. Server only. */
    @Nullable
    private Vec3 muzzle;
    /** The post's crossbow, "held" only for the instant it fires. */
    private ItemStack firing = ItemStack.EMPTY;

    public GarrisonGolemEntity(EntityType<? extends AbstractGolem> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractGolem.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new CrewPostGoal(this));
        this.goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.8));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.5));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    public void setHome(BlockPos pos) {
        this.home = pos.immutable();
        this.restrictTo(this.home, homeRadius());
    }

    public BlockPos home() {
        if (home == null) {
            setHome(blockPosition());
        }
        return home;
    }

    @Nullable
    public Vec3 muzzle() {
        return muzzle;
    }

    public void setMuzzle(@Nullable Vec3 muzzle) {
        this.muzzle = muzzle;
    }

    /**
     * Lends the golem a post's crossbow for one shot. The crossbow's firing code
     * reads the shooter's main hand, so it has to look held; going through the
     * equipment slots instead would sync it, play equip events and swap attributes.
     */
    public void setFiring(ItemStack crossbow) {
        this.firing = crossbow;
    }

    @Override
    public ItemStack getItemBySlot(net.minecraft.world.entity.EquipmentSlot slot) {
        if (slot == net.minecraft.world.entity.EquipmentSlot.MAINHAND && !firing.isEmpty()) {
            return firing;
        }
        return super.getItemBySlot(slot);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && home == null) {
            setHome(blockPosition());
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (home != null) {
            tag.put("Home", NbtUtils.writeBlockPos(home));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        NbtUtils.readBlockPos(tag, "Home").ifPresent(this::setHome);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WOOD_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WOOD_BREAK;
    }

    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        this.playSound(SoundEvents.WOOD_STEP, 0.4f, 1.2f);
    }
}
