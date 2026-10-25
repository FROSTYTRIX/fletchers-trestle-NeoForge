package net.frostytrix.fletcherstrestle.entity.custom;

import net.frostytrix.fletcherstrestle.garrison.CrewPostGoal;
import net.frostytrix.fletcherstrestle.garrison.FlinchGoal;
import net.frostytrix.fletcherstrestle.garrison.SaluteGoal;
import net.frostytrix.fletcherstrestle.garrison.TinkerGoal;
import net.frostytrix.fletcherstrestle.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Crackiness;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The garrison golem, the Bolt Warden: a clockwork crew for emplacements. It
 * has no weapon of its own and never targets players; its only job is to run to
 * a loaded post that can see a hostile mob, and work the crossbow on it.
 *
 * <p>It is made of the logs it was built from, and looks it: its wood is saved
 * and synced, and those logs are what it drops (with the Mechanical Trigger,
 * from the {@code entities/garrison_golem} loot table). Planks patch it up.</p>
 *
 * <p>It stays within {@link #homeRadius()} blocks of where it was wound up, and
 * crews any post in that area, so one golem can serve several. Arrows it fires
 * belong to it, so its kills give no archery XP.</p>
 *
 * <p>What it is doing is synced as a {@link #getGarrisonState() state} for the
 * model, and one-off motions (recoil, flinch, salute, shrug) arrive as entity
 * events.</p>
 */
public class GarrisonGolemEntity extends AbstractGolem {

    public static final byte IDLE = 0;
    public static final byte TINKERING = 1;
    /** Running to a post with a target in its cone. */
    public static final byte ALERT = 2;
    public static final byte MANNING = 3;
    public static final byte RELOADING = 4;

    public static final byte EVENT_RECOIL = 70;
    public static final byte EVENT_FLINCH = 71;
    public static final byte EVENT_SALUTE = 72;
    public static final byte EVENT_SHRUG = 73;

    private static final int RECOIL_TICKS = 6;
    private static final int FLINCH_TICKS = 16;
    private static final int SALUTE_TICKS = 40;
    private static final int SHRUG_TICKS = 30;
    /** A plank mends a quarter of its health, as an ingot does for an iron golem. */
    private static final float REPAIR_AMOUNT = 10.0f;

    /**
     * The woods it has a look for, each with a stripped log to match modded logs
     * against by map colour.
     */
    public static final Map<String, Block> WOODS = new LinkedHashMap<>();

    static {
        WOODS.put("oak", Blocks.STRIPPED_OAK_LOG);
        WOODS.put("spruce", Blocks.STRIPPED_SPRUCE_LOG);
        WOODS.put("birch", Blocks.STRIPPED_BIRCH_LOG);
        WOODS.put("jungle", Blocks.STRIPPED_JUNGLE_LOG);
        WOODS.put("acacia", Blocks.STRIPPED_ACACIA_LOG);
        WOODS.put("dark_oak", Blocks.STRIPPED_DARK_OAK_LOG);
        WOODS.put("mangrove", Blocks.STRIPPED_MANGROVE_LOG);
        WOODS.put("cherry", Blocks.STRIPPED_CHERRY_LOG);
        WOODS.put("crimson", Blocks.STRIPPED_CRIMSON_STEM);
        WOODS.put("warped", Blocks.STRIPPED_WARPED_STEM);
        WOODS.put("bamboo", Blocks.STRIPPED_BAMBOO_BLOCK);
    }

    private static final EntityDataAccessor<String> DATA_WOOD =
            SynchedEntityData.defineId(GarrisonGolemEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Byte> DATA_STATE =
            SynchedEntityData.defineId(GarrisonGolemEntity.class, EntityDataSerializers.BYTE);

    /** How far from home it wanders and looks for posts. */
    public static int homeRadius() {
        return net.frostytrix.fletcherstrestle.config.FletcherConfig.GARRISON_HOME_RADIUS.get();
    }

    @Nullable
    private BlockPos home;
    /** Where the next bolt leaves from, and the post it's fired from, while a shot is being fired. Server only. */
    @Nullable
    private Vec3 muzzle;
    @Nullable
    private BlockPos firingFrom;
    /** The post's crossbow, "held" only for the instant it fires. */
    private ItemStack firing = ItemStack.EMPTY;
    /** The logs it was built from, which it drops again. */
    @Nullable
    private ResourceLocation logs;
    /** Whoever wound it up. */
    @Nullable
    private UUID builder;

    @Nullable
    private Player flinchFrom;
    private int flinchUntil;
    private final Map<UUID, Long> saluted = new HashMap<>();

    // Client-side animation clocks.
    private float keyAngle;
    private float keyAngleO;
    private float keySpeed;
    private float run;
    private float runO;
    private float aim;
    private float aimO;
    private float crank;
    private float crankO;
    private float tinker;
    private float tinkerO;
    private int recoilTicks;
    private int flinchTicks;
    private int saluteTicks;
    private int shrugTicks;

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
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_WOOD, "oak");
        builder.define(DATA_STATE, IDLE);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new FlinchGoal(this));
        this.goalSelector.addGoal(2, new CrewPostGoal(this));
        this.goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.8));
        this.goalSelector.addGoal(5, new TinkerGoal(this));
        this.goalSelector.addGoal(6, new SaluteGoal(this));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.5));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 6.0f));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    // ---------------- identity ----------------

    public String getWood() {
        return this.entityData.get(DATA_WOOD);
    }

    public void setWood(String wood) {
        this.entityData.set(DATA_WOOD, WOODS.containsKey(wood) ? wood : "oak");
    }

    /** Builds the golem's body from these logs: its look, and what it drops. */
    public void setLogs(BlockState state, BlockGetter level, BlockPos pos) {
        this.logs = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        setWood(woodOf(state, level, pos));
    }

    /**
     * The wood a log is: by name for the vanilla stripped logs and woods, and
     * otherwise the one whose map colour is closest, so a modded log gets the
     * look nearest its own.
     */
    public static String woodOf(BlockState state, BlockGetter level, BlockPos pos) {
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        if (path.startsWith("stripped_")) path = path.substring("stripped_".length());
        for (String suffix : new String[]{"_log", "_wood", "_stem", "_hyphae", "_block"}) {
            if (path.endsWith(suffix)) {
                path = path.substring(0, path.length() - suffix.length());
                break;
            }
        }
        if (WOODS.containsKey(path)) return path;

        int color = state.getMapColor(level, pos).col;
        String best = "oak";
        int bestDistance = Integer.MAX_VALUE;
        for (Map.Entry<String, Block> wood : WOODS.entrySet()) {
            MapColor mapColor = wood.getValue().defaultBlockState().getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            int other = mapColor.col;
            int dr = (color >> 16 & 255) - (other >> 16 & 255);
            int dg = (color >> 8 & 255) - (other >> 8 & 255);
            int db = (color & 255) - (other & 255);
            int distance = dr * dr + dg * dg + db * db;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = wood.getKey();
            }
        }
        return best;
    }

    public void setBuilder(@Nullable UUID builder) {
        this.builder = builder;
    }

    @Nullable
    public UUID getBuilder() {
        return builder;
    }

    // ---------------- home and firing ----------------

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

    @Nullable
    public BlockPos firingFrom() {
        return firingFrom;
    }

    public void setMuzzle(@Nullable Vec3 muzzle, @Nullable BlockPos post) {
        this.muzzle = muzzle;
        this.firingFrom = post;
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
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND && !firing.isEmpty()) {
            return firing;
        }
        return super.getItemBySlot(slot);
    }

    // ---------------- state ----------------

    public byte getGarrisonState() {
        return this.entityData.get(DATA_STATE);
    }

    public void setGarrisonState(byte state) {
        if (getGarrisonState() != state) {
            this.entityData.set(DATA_STATE, state);
        }
    }

    /** Shows a one-off motion on every client watching. */
    public void cue(byte event) {
        this.level().broadcastEntityEvent(this, event);
    }

    @Nullable
    public Player flinchingFrom() {
        return flinchFrom != null && tickCount < flinchUntil ? flinchFrom : null;
    }

    public void clearFlinch() {
        flinchFrom = null;
    }

    /** Records a salute to this player, unless it saluted them in the last {@code cooldown} ticks. */
    public boolean salute(Player player, long cooldown) {
        long now = level().getGameTime();
        saluted.values().removeIf(at -> now - at > cooldown);
        if (saluted.containsKey(player.getUUID())) return false;
        saluted.put(player.getUUID(), now);
        return true;
    }

    public Crackiness.Level getCrackiness() {
        return Crackiness.GOLEM.byFraction(this.getHealth() / this.getMaxHealth());
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Crackiness.Level before = getCrackiness();
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide) {
            if (getCrackiness() != before) {
                playSound(ModSounds.GARRISON_CRACK.get(), 1.0f, 1.0f);
            }
            // It never fights back: a player's blow just makes it cower and back off.
            if (source.getEntity() instanceof Player player && isAlive()) {
                flinchFrom = player;
                flinchUntil = tickCount + 40;
                cue(EVENT_FLINCH);
            }
        }
        return hurt;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(ItemTags.PLANKS)) {
            return super.mobInteract(player, hand);
        }
        float before = getHealth();
        heal(REPAIR_AMOUNT);
        if (getHealth() == before) {
            return InteractionResult.PASS;
        }
        playSound(ModSounds.GARRISON_REPAIR.get(), 1.0f, 1.0f + (random.nextFloat() - random.nextFloat()) * 0.2f);
        if (level() instanceof ServerLevel server && held.getItem() instanceof BlockItem block) {
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, block.getBlock().defaultBlockState()),
                    getX(), getY() + 1.0, getZ(), 12, 0.3, 0.5, 0.3, 0.1);
        }
        held.consume(1, player);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData data) {
        // A golem from an egg or a command picks a wood; a built one already has its own.
        if (spawnType != MobSpawnType.MOB_SUMMONED) {
            setWood(WOODS.keySet().stream().skip(random.nextInt(WOODS.size())).findFirst().orElse("oak"));
        }
        return super.finalizeSpawn(level, difficulty, spawnType, data);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        Block block = logs != null ? BuiltInRegistries.BLOCK.get(logs) : WOODS.get(getWood());
        if (block != null && block.asItem() != net.minecraft.world.item.Items.AIR) {
            spawnAtLocation(new ItemStack(block, 2));
        }
    }

    // ---------------- ticking and animation ----------------

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && home == null) {
            setHome(blockPosition());
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            tickAnimation();
        }
    }

    private void tickAnimation() {
        byte state = getGarrisonState();
        boolean moving = getDeltaMovement().horizontalDistanceSqr() > 1.0E-4;
        float spinTarget = switch (state) {
            case ALERT -> 0.9f;
            case MANNING, RELOADING -> 0.45f;
            case TINKERING -> 0.12f;
            default -> moving ? 0.2f : 0.05f;
        };
        keySpeed += (spinTarget - keySpeed) * 0.1f;
        keyAngleO = keyAngle;
        keyAngle += keySpeed;
        if (keyAngle > Mth.TWO_PI) {
            keyAngle -= Mth.TWO_PI;
            keyAngleO -= Mth.TWO_PI;
        }
        runO = run;
        run = approach(run, state == ALERT && moving ? 1.0f : 0.0f, 0.15f);
        aimO = aim;
        aim = approach(aim, state == MANNING || state == RELOADING ? 1.0f : 0.0f, 0.12f);
        crankO = crank;
        crank = approach(crank, state == RELOADING ? 1.0f : 0.0f, 0.2f);
        tinkerO = tinker;
        tinker = approach(tinker, state == TINKERING ? 1.0f : 0.0f, 0.08f);
        if (recoilTicks > 0) recoilTicks--;
        if (flinchTicks > 0) flinchTicks--;
        if (saluteTicks > 0) saluteTicks--;
        if (shrugTicks > 0) shrugTicks--;
    }

    private static float approach(float value, float target, float step) {
        return value < target ? Math.min(target, value + step) : Math.max(target, value - step);
    }

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EVENT_RECOIL -> recoilTicks = RECOIL_TICKS;
            case EVENT_FLINCH -> flinchTicks = FLINCH_TICKS;
            case EVENT_SALUTE -> saluteTicks = SALUTE_TICKS;
            case EVENT_SHRUG -> shrugTicks = SHRUG_TICKS;
            default -> super.handleEntityEvent(id);
        }
    }

    public float keyAngle(float partial) {
        return Mth.lerp(partial, keyAngleO, keyAngle);
    }

    public float runAmount(float partial) {
        return Mth.lerp(partial, runO, run);
    }

    public float aimAmount(float partial) {
        return Mth.lerp(partial, aimO, aim);
    }

    public float crankAmount(float partial) {
        return Mth.lerp(partial, crankO, crank);
    }

    public float tinkerAmount(float partial) {
        return Mth.lerp(partial, tinkerO, tinker);
    }

    public float recoilAmount(float partial) {
        float t = Mth.clamp((recoilTicks - partial) / RECOIL_TICKS, 0.0f, 1.0f);
        return t * t;
    }

    public float flinchAmount(float partial) {
        return envelope(flinchTicks, FLINCH_TICKS, partial, 2.0f, 6.0f);
    }

    public float saluteAmount(float partial) {
        return envelope(saluteTicks, SALUTE_TICKS, partial, 6.0f, 8.0f);
    }

    public float shrugAmount(float partial) {
        return envelope(shrugTicks, SHRUG_TICKS, partial, 4.0f, 6.0f);
    }

    /** Eases in over {@code in} ticks, holds, and eases out over the last {@code out}. */
    private static float envelope(int remaining, int total, float partial, float in, float out) {
        if (remaining <= 0) return 0.0f;
        float left = remaining - partial;
        float elapsed = total - left;
        float t = Math.min(1.0f, Math.min(elapsed / in, left / out));
        t = Mth.clamp(t, 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    // ---------------- save ----------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (home != null) {
            tag.put("Home", NbtUtils.writeBlockPos(home));
        }
        tag.putString("Wood", getWood());
        if (logs != null) {
            tag.putString("Logs", logs.toString());
        }
        if (builder != null) {
            tag.putUUID("Builder", builder);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        NbtUtils.readBlockPos(tag, "Home").ifPresent(this::setHome);
        if (tag.contains("Wood")) {
            setWood(tag.getString("Wood"));
        }
        if (tag.contains("Logs")) {
            logs = ResourceLocation.tryParse(tag.getString("Logs"));
        }
        if (tag.hasUUID("Builder")) {
            builder = tag.getUUID("Builder");
        }
    }

    // ---------------- sounds ----------------

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.GARRISON_TICK.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 120;
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
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.WOOD_STEP, 0.4f, 1.2f);
    }
}
