package cam72cam.mod.entity;

import cam72cam.mod.world.World;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.entity.sync.EntitySync;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.net.Packet;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.world.Explosion;
import net.minecraftforge.fml.common.registry.EntityRegistry;

import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Entity {
    public net.minecraft.entity.Entity internal;
    private ModdedEntity modded;
    public final EntitySync sync;

    protected Entity() {
        this.sync = new EntitySync(this);
    }
    public Entity(net.minecraft.entity.Entity entity) {
        this();
        setup(entity);
    }
    Entity setup(net.minecraft.entity.Entity entity) {
        this.internal = entity;
        this.modded = entity instanceof ModdedEntity ? (ModdedEntity)entity : null;
        return this;
    }

    public String tryJoinWorld() {
        return null;
    }

    public World getWorld() {
        return World.get(internal.world);
    }

    public UUID getUUID() {
        return internal.getPersistentID();
    }

    /* Position / Rotation */

    public Vec3i getBlockPosition() {
        return new Vec3i(internal.getPosition());
    }
    public Vec3d getPosition() {
        return new Vec3d(internal.getPositionVector());
    }
    public void setPosition(Vec3d pos) {
        internal.setPosition(pos.x, pos.y, pos.z);
    }

    public Vec3d getVelocity() {
        return new Vec3d(internal.motionX, internal.motionY, internal.motionZ);
    }
    public void setVelocity(Vec3d motion) {
        internal.motionX = motion.x;
        internal.motionY = motion.y;
        internal.motionZ = motion.z;
    }

    public float getRotationYaw() {
        return internal.rotationYaw;
    }
    public float getRotationPitch() {
        return internal.rotationPitch;
    }
    public float getPrevRotationYaw() {
        return internal.prevRotationYaw;
    }
    public float getPrevRotationPitch() {
        return internal.prevRotationPitch;
    }
    public void setRotationYaw(float yaw) {
        internal.prevRotationYaw = internal.rotationYaw;
        internal.rotationYaw = yaw;
    }
    public void setRotationPitch(float pitch) {
        internal.prevRotationPitch = internal.rotationPitch;
        internal.rotationPitch = pitch;
    }

    public Vec3d getPositionEyes(float partialTicks) {
        return new Vec3d(internal.getPositionEyes(partialTicks));
    }


    /* Casting */


    public Player asPlayer() {
        if (internal instanceof EntityPlayer) {
            return new Player((EntityPlayer) internal);
        }
        return null;
    }

    public boolean is(Class<? extends net.minecraft.entity.Entity> entity) {
        return internal.getClass().isInstance(entity);
    }

    public <T extends net.minecraft.entity.Entity> T asInternal(Class<T> entity) {
        if (internal.getClass().isInstance(entity)) {
            return (T)internal;
        }
        return null;
    }

    public <T extends Entity> T as(Class<T> type) {
        if (type.isInstance(this)) {
            return (T)this;
        }
        return null;
    }

    public boolean isVillager() {
        return this.is(EntityVillager.class);
    }

    public void kill() {
        internal.setDead();
        internal.world.removeEntity(internal);
    }
    public final boolean isDead() {
        return internal.isDead;
    }


    /* Networking */

    public void sendToObserving(Packet packet) {
        boolean found = false;
        int syncDist = EntityRegistry.instance().lookupModSpawn(internal.getClass(), true).getTrackingRange();
        for (EntityPlayer player : internal.world.playerEntities) {
            if (player.getPositionVector().distanceTo(internal.getPositionVector()) < syncDist) {
                found = true;
                break;
            }
        }
        if (found) {
            packet.sendToAllAround(getWorld(), getPosition(), syncDist);
        }
    }

    public int getTickCount() {
        return internal.ticksExisted;
    }

    public int getPassengerCount() {
        if (modded != null) {
            return modded.getPassengerCount();
        } else {
            return internal.getPassengers().size();
        }
    }

    public final void addPassenger(cam72cam.mod.entity.Entity entity) {
        entity.internal.startRiding(internal);
    }

    public final boolean isPassenger(cam72cam.mod.entity.Entity passenger) {
        return internal.isPassenger(passenger.internal);
    }

    public final Vec3d getRidingOffset(Entity source) {
        return modded.getRidingOffset(source);
    }
    public final void setRidingOffset(Entity source, Vec3d pos) {
        modded.setRidingOffset(source, pos);
    }

    public Entity removePassenger(Predicate<ModdedEntity.StaticPassenger> o) {
        return modded.removePassenger(o);
    }

    protected List<Entity> getPassengers() {
        return internal.getPassengers().stream().map(Entity::new).collect(Collectors.toList());
    }

    public boolean isPlayer() {
        return internal instanceof EntityPlayer;
    }

    public Entity getRiding() {
        if (internal.getRidingEntity() != null) {
            return getWorld().getEntity(internal.getRidingEntity().getUniqueID(), Entity.class);
        }
        return null;
    }

    public IBoundingBox getBounds() {
        return IBoundingBox.from(internal.getEntityBoundingBox());
    }

    public float getRotationYawHead() {
        return internal.getRotationYawHead();
    }

    public Vec3d getLastTickPos() {
        return new Vec3d(internal.lastTickPosX, internal.lastTickPosY, internal.lastTickPosZ);
    }

    public boolean isLiving() {
        return internal instanceof EntityLivingBase;
    }

    public void startRiding(Entity entity) {
        internal.startRiding(entity.internal);
    }

    public float getRidingSoundModifier() {
        return 1;
    }

    public void directDamage(String msg, double damage) {
        internal.attackEntityFrom((new DamageSource(msg)).setDamageBypassesArmor(), (float) damage);
    }

    protected void createExplosion(Vec3d pos, float size, boolean damageTerrain) {
        Explosion explosion = new Explosion(getWorld().internal, this.internal, pos.x, pos.y, pos.z, size, false, damageTerrain);
        if (net.minecraftforge.event.ForgeEventFactory.onExplosionStart(getWorld().internal, explosion)) return;
        explosion.doExplosionA();
        explosion.doExplosionB(true);

    }
}
