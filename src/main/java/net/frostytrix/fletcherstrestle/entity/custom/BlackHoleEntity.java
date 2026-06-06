package net.frostytrix.fletcherstrestle.entity.custom;

import net.frostytrix.fletcherstrestle.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Creative-only "black hole" set piece spawned when a {@code black_hole} arrow lands. Runs a fixed
 * cinematic timeline — telegraph, pull, collapse, flash — pulling nearby entities and items toward
 * its centre, then vanishing. Purely a spectacle; it does no terrain damage. The visual timeline is
 * driven off a server-authoritative age that is synced to clients and persisted across reloads.
 */
public class BlackHoleEntity extends Entity {

    // Phase boundaries (ticks). 20 ticks = 1 second.
    public static final int TELEGRAPH_END = 20;
    public static final int GROW_END = 44;       // sphere reaches full size ~2.2s in, then holds
    public static final int PULL_END = 672;      // long, dramatic pull (~30s)
    public static final int COLLAPSE_END = 692;
    public static final int FLASH_END = 704;
    public static final int LIFETIME = FLASH_END;

    public static final float MAX_RADIUS = 5.6f;    // event-horizon visual radius (blocks)
    private static final double PULL_RADIUS = 36.0; // how far the gravity reaches
    private static final double MAX_REACH = 34.0;   // final block-destruction radius (grows into this)

    // Warm accretion-disk colour (Interstellar amber).
    private static final Vector3f DISK_COLOR = new Vector3f(1.0f, 0.72f, 0.32f);

    // Largest horizon radius whose volume we've already wiped (so we only re-scan as it grows).
    private double clearedRadius = 0.0;

    // Server-authoritative age, synced so every client renders the correct phase (even on
    // reload or when walking into render range mid-effect).
    private static final EntityDataAccessor<Integer> DATA_AGE =
            SynchedEntityData.defineId(BlackHoleEntity.class, EntityDataSerializers.INT);

    public BlackHoleEntity(EntityType<? extends BlackHoleEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_AGE, 0);
    }

    /** Age in ticks, authoritative on the server and synced to clients for rendering. */
    public int age() {
        return level().isClientSide ? entityData.get(DATA_AGE) : tickCount;
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            spawnClientParticles();
        } else {
            entityData.set(DATA_AGE, tickCount);
            Phase phase = phase();
            if (phase == Phase.PULL || phase == Phase.COLLAPSE) {
                applyGravity(phase == Phase.COLLAPSE);
                if (level() instanceof ServerLevel sl) {
                    clearHorizon(sl); // wipe the void's whole volume as it grows
                    // Stop devouring ~1.5s before the end so in-flight blocks can finish being eaten.
                    if (phase == Phase.PULL && tickCount < PULL_END - 30) {
                        pullBlocks(sl);
                    }
                }
            }
            playPhaseSounds();
            if (tickCount >= LIFETIME) {
                if (level() instanceof ServerLevel sl) {
                    cleanupBlocks(sl);
                }
                discard();
            }
        }
    }

    // ---- Phase timeline ----

    public enum Phase { TELEGRAPH, PULL, COLLAPSE, FLASH }

    public Phase phase() {
        int age = age();
        if (age < TELEGRAPH_END) return Phase.TELEGRAPH;
        if (age < PULL_END) return Phase.PULL;
        if (age < COLLAPSE_END) return Phase.COLLAPSE;
        return Phase.FLASH;
    }

    /** 0..1 eased growth of the event horizon: ramps up over the first ~2s, then holds. */
    public float horizonScale(float partialTick) {
        float clock = age() + partialTick;
        if (clock >= COLLAPSE_END) {
            // Snap inward during collapse, to nothing by the flash.
            float t = Math.min(1f, (clock - COLLAPSE_END) / (float) (FLASH_END - COLLAPSE_END));
            return MAX_RADIUS * (1f - t * t);
        }
        float t = Math.min(1f, clock / (float) GROW_END);
        return MAX_RADIUS * (float) (1.0 - Math.pow(1.0 - t, 3.0)); // ease-out, then holds
    }

    // ---- Server: gravity well ----

    private void applyGravity(boolean collapsing) {
        AABB box = getBoundingBox().inflate(PULL_RADIUS);
        Vec3 center = position();
        double killRadius = Math.max(1.0, horizonScale(0f)); // crossing the event horizon kills
        for (Entity e : level().getEntities(this, box, this::canPull)) {
            Vec3 toCenter = center.subtract(e.position());
            double dist = toCenter.length();
            if (dist < 1.0E-4) continue;

            // Set (not add) velocity straight at the centre each tick: this overwrites whatever
            // friction the entity applied last tick, so the pull is effectively frictionless and
            // accelerates as it closes in.
            double t = Math.max(0.0, Math.min(1.0, 1.0 - dist / PULL_RADIUS));
            double speed = (0.3 + 1.1 * t * t) * (collapsing ? 1.6 : 1.0);
            Vec3 dir = toCenter.scale(1.0 / dist); // unit vector toward centre
            e.setDeltaMovement(dir.x * speed, dir.y * speed + 0.04, dir.z * speed);
            e.setOnGround(false);
            e.fallDistance = 0.0f;
            e.hasImpulse = true;
            if (e instanceof LivingEntity) {
                e.hurtMarked = true; // force-syncs the new velocity (incl. players)
            }

            // Crossing the event horizon: living things die; everything else (items, falling
            // blocks, XP orbs, …) is simply devoured.
            if (dist < killRadius) {
                if (e instanceof LivingEntity living) {
                    if (!(e instanceof net.minecraft.world.entity.player.Player p && p.isCreative())) {
                        living.hurt(blackHoleDamage(), 1000.0f);
                    }
                } else {
                    e.discard();
                }
            }
        }
    }

    /**
     * Periodically rips a couple of nearby blocks out of the world as falling-block entities so the
     * gravity well can suck them in. Skips unbreakable blocks, fluids and block entities.
     */
    /** Tears exposed blocks out of the shell beyond the horizon as animated falling blocks. */
    private void pullBlocks(ServerLevel level) {
        // Destruction radius grows over the pull: condensed near the hole at first, then sweeping
        // outward to MAX_REACH.
        double prog = Math.max(0.0, Math.min(1.0,
                (tickCount - GROW_END) / (double) (PULL_END - GROW_END)));
        double reach = 8.0 + Math.pow(prog, 1.4) * (MAX_REACH - 8.0);
        double inner = Math.max(1.0, horizonScale(0f) + 2.0); // start at the edge of the cleared core
        var rand = level.random;
        // Eat-rate scales with the radius so destruction stays dense at every size: intense even
        // while condensed, still violent once it's huge.
        int cap = (int) Math.max(28.0, Math.min(72.0, reach * 2.0));
        int eaten = 0, attempts = 0, maxAttempts = cap * 8;
        // Sample the shell beyond the horizon, biased outward toward the un-eaten terrain.
        while (eaten < cap && attempts < maxAttempts) {
            attempts++;
            double r = inner + (reach - inner) * Math.sqrt(rand.nextDouble());
            double a = rand.nextDouble() * Math.PI * 2.0;
            double cosEl = rand.nextDouble() * 2.0 - 1.0;
            double sinEl = Math.sqrt(1.0 - cosEl * cosEl);
            BlockPos pos = BlockPos.containing(
                    getX() + r * sinEl * Math.cos(a),
                    getY() + r * cosEl,
                    getZ() + r * sinEl * Math.sin(a));

            BlockState state = level.getBlockState(pos);
            if (state.isAir() || !state.getFluidState().isEmpty()) continue;
            if (state.getDestroySpeed(level, pos) < 0) continue;   // unbreakable (e.g. bedrock)
            if (!isExposed(level, pos)) continue;                   // don't reach buried blocks

            // ~40% of plain blocks fly in as animated debris; the rest are erased outright. Block
            // entities (chests, machines) can't ride a falling block cleanly, so always erase those.
            if (level.getBlockEntity(pos) == null && rand.nextFloat() < 0.4f) {
                FallingBlockEntity block = FallingBlockEntity.fall(level, pos, state);
                block.setNoGravity(true);
                block.time = 1; // skip the "just spawned" despawn guard
                block.setDeltaMovement(position().subtract(block.position()).normalize().scale(0.5));
            } else {
                level.removeBlock(pos, false);
            }
            eaten++;
        }
    }

    /** Erases every breakable block inside the event-horizon sphere as it expands — the void itself. */
    private void clearHorizon(ServerLevel level) {
        // Clear a little beyond the visual sphere so no ring of blocks survives hugging the horizon.
        double r = horizonScale(0f) + 2.0;
        if (r <= clearedRadius + 0.01) return; // only re-scan as the sphere grows
        clearedRadius = r;
        int ri = (int) Math.ceil(r);
        double r2 = r * r;
        BlockPos center = blockPosition();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -ri; dx <= ri; dx++) {
            for (int dy = -ri; dy <= ri; dy++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    if (dx * dx + dy * dy + dz * dz > r2) continue;
                    p.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState s = level.getBlockState(p);
                    if (s.isAir()) continue;
                    if (s.getDestroySpeed(level, p) < 0) continue; // unbreakable
                    level.removeBlock(p, false);
                }
            }
        }
    }

    /** True if the block has at least one open (air / non-solid) face — i.e. it isn't fully buried. */
    private static boolean isExposed(ServerLevel level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            BlockPos np = pos.relative(d);
            BlockState n = level.getBlockState(np);
            if (n.isAir() || !n.isSolidRender(level, np)) return true;
        }
        return false;
    }

    /** On death, vanish any blocks still mid-flight so nothing is left floating. */
    private void cleanupBlocks(ServerLevel level) {
        AABB box = getBoundingBox().inflate(PULL_RADIUS);
        for (FallingBlockEntity block : level.getEntitiesOfClass(FallingBlockEntity.class, box, Entity::isNoGravity)) {
            block.discard();
        }
    }

    private boolean canPull(Entity e) {
        return !(e instanceof BlackHoleEntity) && e.isAlive() && !e.isSpectator();
    }

    private DamageSource blackHoleDamage() {
        return damageSources().fellOutOfWorld();
    }

    // ---- Sound cues at phase transitions ----

    private void playPhaseSounds() {
        switch (tickCount) {
            case 0 -> playLocal(SoundEvents.BEACON_DEACTIVATE, 1.4f, 0.4f);
            case TELEGRAPH_END -> playLocal(SoundEvents.WARDEN_HEARTBEAT, 1.6f, 0.5f);
            case COLLAPSE_END - 4 -> playLocal(SoundEvents.WARDEN_SONIC_CHARGE, 1.6f, 0.7f);
            case COLLAPSE_END -> playLocal(SoundEvents.GENERIC_EXPLODE.value(), 1.8f, 0.5f);
            default -> {
                // Low recurring rumble through the long pull.
                if (phase() == Phase.PULL && tickCount % 40 == 0) {
                    playLocal(SoundEvents.WARDEN_HEARTBEAT, 0.9f, 0.4f);
                }
            }
        }
    }

    private void playLocal(net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        level().playSound(null, getX(), getY(), getZ(), sound, SoundSource.HOSTILE, volume, pitch);
    }

    // ---- Client: infalling matter particles ----

    private void spawnClientParticles() {
        Phase phase = phase();
        if (phase == Phase.FLASH) return;

        float radius = horizonScale(0f);
        var random = level().random;

        // Glowing accretion specks orbiting the rim.
        int diskCount = phase == Phase.COLLAPSE ? 10 : 5;
        for (int i = 0; i < diskCount; i++) {
            double a = random.nextDouble() * Math.PI * 2.0;
            double r = radius * (1.4 + random.nextDouble() * 1.6);
            double px = getX() + Math.cos(a) * r;
            double pz = getZ() + Math.sin(a) * r;
            double py = getY() + (random.nextDouble() - 0.5) * 0.4;
            // Tangential drift = orbital swirl.
            double tang = 0.25;
            level().addParticle(new DustParticleOptions(DISK_COLOR, 1.3f),
                    px, py, pz,
                    -Math.sin(a) * tang, 0.0, Math.cos(a) * tang);
        }

        // Dark matter streaming inward.
        if (phase == Phase.PULL || phase == Phase.COLLAPSE) {
            for (int i = 0; i < 3; i++) {
                double a = random.nextDouble() * Math.PI * 2.0;
                double r = PULL_RADIUS * (0.3 + random.nextDouble() * 0.5);
                double px = getX() + Math.cos(a) * r;
                double pz = getZ() + Math.sin(a) * r;
                double py = getY() + (random.nextDouble() - 0.5) * 2.0;
                Vec3 inward = new Vec3(getX() - px, getY() - py, getZ() - pz).normalize().scale(0.6);
                level().addParticle(ParticleTypes.SMOKE, px, py, pz, inward.x, inward.y, inward.z);
            }
        }
    }

    // ---- Plumbing ----

    /** Spawns a black hole at the given position in the world. */
    public static void spawnAt(Level level, Vec3 pos) {
        if (level.isClientSide) return;
        BlackHoleEntity hole = new BlackHoleEntity(ModEntities.BLACK_HOLE.get(), level);
        hole.setPos(pos.x, pos.y, pos.z);
        level.addFreshEntity(hole);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distSqr) {
        return distSqr < 256.0 * 256.0;
    }

    @Override
    protected AABB makeBoundingBox() {
        // Keep the render-culling box generous so the disk/halo never pop out.
        return new AABB(position().subtract(16, 16, 16), position().add(16, 16, 16));
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // Restore the timeline so the set piece resumes (and finishes/cleans up) after a reload.
        this.tickCount = tag.getInt("Age");
        this.clearedRadius = tag.getDouble("ClearedRadius");
        // Sync the age immediately so the first frame after load renders the correct phase
        // instead of a one-tick invisible "age 0".
        this.entityData.set(DATA_AGE, this.tickCount);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", tickCount);
        tag.putDouble("ClearedRadius", clearedRadius);
    }
}
