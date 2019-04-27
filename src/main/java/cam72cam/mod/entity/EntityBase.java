package cam72cam.mod.entity;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.mod.World;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.net.Packet;
import cam72cam.mod.util.Hand;
import cam72cam.mod.util.TagCompound;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;

import java.util.ArrayList;
import java.util.Collection;

public abstract class EntityBase extends Entity implements IEntityAdditionalSpawnData {
    public final World world;
    public final cam72cam.mod.entity.Entity self;

    private EntitySettings settings;

    public EntityBase(net.minecraft.world.World world) {
        super(world);

        this.world = new World(world);
        this.self = new cam72cam.mod.entity.Entity(this);

        super.preventEntitySpawning = true;
    }

    @Override
    protected final void entityInit() {
        EntitySettings settings = TempReg.getSettings(this.getClass());
        init(settings);

        super.isImmuneToFire = settings.immuneToFire;
        super.entityCollisionReduction = settings.entityCollisionReduction;
        this.settings = settings;
    }
    protected void init(EntitySettings settings) {

    }

    /* Data loading */

    @Override
    protected final void readEntityFromNBT(NBTTagCompound compound) {
        load(new TagCompound(compound));
    }
    @Override
    public final void readSpawnData(ByteBuf additionalData) {
        load(new TagCompound(ByteBufUtils.readTag(additionalData)));
    }
    protected abstract void load(TagCompound compound);


    @Override
    protected final void writeEntityToNBT(NBTTagCompound compound) {
        save(new TagCompound(compound));
    }
    @Override
    public final void writeSpawnData(ByteBuf buffer) {
        TagCompound compound = new TagCompound();
        save(compound);
        ByteBufUtils.writeTag(buffer, compound.internal);
    }
    protected abstract void save(TagCompound compound);

    @Override
    public final void onUpdate() {
        onTick();
    }
    protected abstract void onTick();

    /* Player Interact */

    @Override
    public final boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
        return onClick(new Player(player), Hand.from(hand)) == ClickResult.ACCEPTED;
    }
    public ClickResult onClick(Player player, Hand hand) {
        return ClickResult.PASS;
    }

    @Override
    public boolean canBePushed() {
        return settings.canBePushed;
    }

    /* Death */

    @Override
    public final boolean attackEntityFrom(DamageSource damagesource, float amount) {
        cam72cam.mod.entity.Entity wrapEnt = new cam72cam.mod.entity.Entity(damagesource.getTrueSource());
        DamageType type;
        if (damagesource.isExplosion() && !(damagesource.getTrueSource() instanceof EntityMob)) {
            type = DamageType.EXPLOSION;
        } else if (damagesource.getTrueSource() instanceof EntityPlayer) {
            type = damagesource.isProjectile() ? DamageType.PROJECTILE : DamageType.PLAYER;
        } else {
            type = DamageType.OTHER;
        }
        onDamage(type, wrapEnt, amount);

        return false;
    }
    public void onDamage(DamageType type, cam72cam.mod.entity.Entity source, float amount) {

    }

    @Override
    public final void setDead() {
        if (!this.isDead) {
            super.setDead();
            onRemoved();
        }
    }
    public final boolean isDead() {
        return this.isDead;
    }
    public void onRemoved() {

    }

    public final void kill() {
        setDead();
        world.removeEntity(this);
    }

    /* Riders */
    @Override
    public <T extends Entity> Collection<T> getRecursivePassengersByType(Class<T> entityClass) {
        if (!settings.attachedToPlayer) {
            try {
                throw new Exception("Hack the planet");
            } catch (Exception ex) {
                for (StackTraceElement tl : ex.getStackTrace()) {
                    if (tl.getFileName().contains("PlayerList.java")) {
                        return new ArrayList<>();
                    }
                }
            }
        }
        return super.getRecursivePassengersByType(entityClass);
    }

    /* Networking */

    public void sendToObserving(Packet packet) {
        boolean found = false;
        for (EntityPlayer player : world.internal.playerEntities) {
            if (player.getPositionVector().distanceTo(this.getPositionVector()) < ImmersiveRailroading.ENTITY_SYNC_DISTANCE) {
                found = true;
                break;
            }
        }
        if (found) {
            ImmersiveRailroading.net.sendToAllAround(packet,
                    new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, ImmersiveRailroading.ENTITY_SYNC_DISTANCE));
        }
    }

    /* Hacks */
    @Override
    public boolean canBeCollidedWith() {
        // Needed for right click, probably a forge or MC bug
        return true;
    }
}
