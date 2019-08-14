package cam72cam.mod.entity;

import cam72cam.mod.entity.boundingbox.BoundingBox;
import cam72cam.mod.entity.custom.*;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.net.Packet;
import cam72cam.mod.net.PacketDirection;
import cam72cam.mod.util.Hand;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.util.TagCompound;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.*;
import java.util.function.Predicate;

public class ModdedEntity extends Entity implements IEntityAdditionalSpawnData {
    private cam72cam.mod.entity.Entity self;

    private Map<UUID, Vec3d> passengerPositions = new HashMap<>();
    private List<StaticPassenger> staticPassengers = new ArrayList<>();

    private EntitySettings settings;
    private String type;
    private IWorldData iWorldData;
    private ISpawnData iSpawnData;
    private ITickable iTickable;
    private IClickable iClickable;
    private IKillable iKillable;
    private IRidable iRidable;
    private ICollision iCollision;

    public ModdedEntity(World world) {
        super(world);

        super.preventEntitySpawning = true;
    }

    @Override
    protected final void entityInit() {
    }

    /* Init Self Wrapper */

    protected final void init(String type) {
        if (self == null) {
            this.type = type;
            self = Registry.create(type, this);

            EntitySettings settings = Registry.getSettings(type);
            super.isImmuneToFire = settings.immuneToFire;
            super.entityCollisionReduction = settings.entityCollisionReduction;
            this.settings = settings;

            iWorldData = IWorldData.get(self);
            iSpawnData = ISpawnData.get(self);
            iTickable = ITickable.get(self);
            iClickable = IClickable.get(self);
            iKillable = IKillable.get(self);
            iRidable = IRidable.get(self);
            iCollision = ICollision.get(self);
        }
    }
    private final void loadSelf(TagCompound data) {
        init(data.getString("custom_mob_type"));
    }
    private final void saveSelf(TagCompound data) {
        data.setString("custom_mob_type", type);
    }

    public cam72cam.mod.entity.Entity getSelf() {
        return self;
    }

    /* IWorldData */

    @Override
    protected final void readEntityFromNBT(NBTTagCompound compound) {
        load(new TagCompound(compound));
    }
    private final void load(TagCompound data) {
        loadSelf(data);
        iWorldData.load(data);

        passengerPositions = data.getMap("passengers", UUID::fromString, (TagCompound tag) -> tag.getVec3d("pos"));
        staticPassengers = data.getList("staticPassengers", StaticPassenger::new);
    }

    @Override
    protected final void writeEntityToNBT(NBTTagCompound compound) {
        save(new TagCompound(compound));
    }
    private final void save(TagCompound data) {
        iWorldData.save(data);
        saveSelf(data);

        data.setMap("passengers", passengerPositions, UUID::toString, (Vec3d pos) -> {
            //TODO single encoder step
            TagCompound tmp = new TagCompound();
            tmp.setVec3d("pos", pos);
            return tmp;
        });
        data.setList("staticPassengers", staticPassengers, StaticPassenger::toTag);
    }

    /* ISpawnData */

    @Override
    public final void readSpawnData(ByteBuf additionalData) {
        TagCompound data = new TagCompound(ByteBufUtils.readTag(additionalData));
        loadSelf(data);
        iSpawnData.loadSpawn(data);
        self.sync.receive(data.get("sync"));
    }
    @Override
    public final void writeSpawnData(ByteBuf buffer) {
        TagCompound data = new TagCompound();
        iSpawnData.saveSpawn(data);
        saveSelf(data);
        data.set("sync", self.sync);

        ByteBufUtils.writeTag(buffer, data.internal);
    }

    /* ITickable */

    @Override
    public final void onUpdate() {
        iTickable.onTick();
        self.sync.send();
    }

    /* Player Interact */

    @Override
    public final boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
        return iClickable.onClick(new Player(player), Hand.from(hand)) == ClickResult.ACCEPTED;
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
        iKillable.onDamage(type, wrapEnt, amount);

        return false;
    }

    @Override
    public final void setDead() {
        if (!this.isDead) {
            super.setDead();
            iKillable.onRemoved();
        }
    }

    /* Ridable */

    @Override
    public boolean canFitPassenger(Entity passenger) {
        return iRidable.canFitPassenger(new cam72cam.mod.entity.Entity(passenger));
    }

    @Override
    public final void addPassenger(Entity entityIn) {
        cam72cam.mod.entity.Entity entity = new cam72cam.mod.entity.Entity(entityIn);
        passengerPositions.put(entity.getUUID(), iRidable.getMountPosition(entity));
        if (entity.isPlayer()) {
            super.addPassenger(entityIn);
        } else {
            StaticPassenger sp = new StaticPassenger(entity);
            staticPassengers.add(sp);
            entity.kill();
        }
        self.sendToObserving(new PassengerPositionsPacket(this));
    }

    @Override
    public final void updatePassenger(net.minecraft.entity.Entity passenger) {
        iRidable.updatePassenger(new cam72cam.mod.entity.Entity(passenger));
    }

    @Override
    public final void removePassenger(net.minecraft.entity.Entity ent) {
        super.removePassenger(ent);
        iRidable.onDismountPassenger(new cam72cam.mod.entity.Entity(ent));
        Vec3d dismountPos = iRidable.getDismountPosition(new cam72cam.mod.entity.Entity(ent));
        ent.setPosition(dismountPos.x, dismountPos.y, dismountPos.z);
    }

    public final cam72cam.mod.entity.Entity removePassenger() {
        // TODO players?
        return removePassenger((StaticPassenger p) -> true);
    }

    public final cam72cam.mod.entity.Entity removePassenger(Predicate<StaticPassenger> filter) {
        Optional<StaticPassenger> found = staticPassengers.stream().filter(filter).findFirst();
        if (found.isPresent()) {
            Entity ent = found.get().respawn(world);

            Vec3d dismountPos = iRidable.getDismountPosition(new cam72cam.mod.entity.Entity(ent));
            iRidable.onDismountPassenger(new cam72cam.mod.entity.Entity(ent));
            ent.setPosition(dismountPos.x, dismountPos.y, dismountPos.z);

            return self.getWorld().getEntity(ent.getUniqueID(), cam72cam.mod.entity.Entity.class);
        }
        return null;
    }

    public Vec3d getRidingOffset(UUID id) {
        return this.passengerPositions.get(id);
    }
    public Vec3d getRidingOffset(cam72cam.mod.entity.Entity ent) {
        return this.passengerPositions.get(ent.getUUID());
    }
    public void setRidingOffset(cam72cam.mod.entity.Entity ent, Vec3d pos) {
        this.passengerPositions.put(ent.getUUID(), pos);
        self.sendToObserving(new PassengerPositionsPacket(this));
    }

    @Override
    public final boolean shouldRiderSit() {
        return shouldRiderSit(null);
    }
    public boolean shouldRiderSit(Entity ent) {
        return false;
    }

    @Override
    public boolean canRiderInteract() {
        return false;
    }

    public void handlePassengerPositions(Map<UUID, Vec3d> passengerPositions) {
        this.passengerPositions = passengerPositions;
        for (net.minecraft.entity.Entity passenger : this.getPassengers()) {
            this.updatePassenger(passenger);
        }
    }

    public int getPassengerCount() {
        return this.staticPassengers.size() + this.getPassengers().size();
    }

    public List<StaticPassenger> getStaticPassengers() {
        return staticPassengers;
    }

    public static class StaticPassenger {
        public Identifier ident;
        public TagCompound data;
        public UUID uuid;
        public float rotation;
        private Vec3i startPos;
        public boolean isVillager;
        public Object cache;

        public StaticPassenger(cam72cam.mod.entity.Entity entityliving ) {
            ident = new Identifier(EntityList.getKey(entityliving.internal));
            data = new TagCompound(entityliving.internal.writeToNBT(new NBTTagCompound()));
            uuid = entityliving.getUUID();
            startPos = new Vec3i(entityliving.getPosition());
            isVillager = entityliving.isVillager();
            rotation = (float) (Math.random() * 360);
        }

        public StaticPassenger(TagCompound init) {
            ident = new Identifier(init.getString("ident"));
            data = init.get("data");
            uuid = UUID.fromString(init.getString("uuid"));
            rotation = init.getFloat("rotation");
            startPos = init.getVec3i("startPos");
            isVillager = init.getBoolean("isVillager");
        }

        public TagCompound toTag() {
            TagCompound init = new TagCompound();
            init.setString("ident", ident.toString());
            init.set("data", data);
            init.setString("uuid", uuid.toString());
            init.setFloat("rotation", rotation);
            init.setVec3i("startPos", startPos);
            init.setBoolean("isVillager", isVillager);
            return init;
        }

        public Entity respawn(World world) {
            Entity ent = EntityList.createEntityByIDFromName(ident.internal, world);
            ent.readFromNBT(data.internal);
            return ent;
        }
    }

    public static class PassengerPositionsPacket extends Packet {
        static {
            Packet.register(ModdedEntity.PassengerPositionsPacket::new, PacketDirection.ServerToClient);
        }
        public PassengerPositionsPacket() {
            // Forge Reflection
        }

        public PassengerPositionsPacket(ModdedEntity stock) {
            data.setEntity("stock", stock.self);

            data.setMap("passengers", stock.passengerPositions, UUID::toString, (Vec3d pos) -> {
                //TODO single encoder step
                TagCompound tmp = new TagCompound();
                tmp.setVec3d("pos", pos);
                return tmp;
            });

            data.setList("staticPassengers", stock.staticPassengers, StaticPassenger::toTag);
        }

        @Override
        public void handle() {
            cam72cam.mod.entity.Entity entity = data.getEntity("stock", getWorld());
            if (entity != null && entity.internal instanceof ModdedEntity) {
                ModdedEntity stock = (ModdedEntity) entity.internal;
                stock.handlePassengerPositions(data.getMap("passengers", UUID::fromString, (TagCompound tag) -> tag.getVec3d("pos")));
                stock.staticPassengers = data.getList("staticPassengers", StaticPassenger::new);
            }
        }
    }

    /* ICollision */
    @Override
    public AxisAlignedBB getCollisionBoundingBox() {
        return new BoundingBox(iCollision.getCollision());
    }
    @Override
    public AxisAlignedBB getEntityBoundingBox() {
        return new BoundingBox(iCollision.getCollision());
    }
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        AxisAlignedBB bb = this.getEntityBoundingBox();
        return new AxisAlignedBB(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
    }


    /* Hacks */
    @Override
    public boolean canBeCollidedWith() {
        // Needed for right click, probably a forge or MC bug
        return true;
    }

    @Override
    public boolean canBePushed() {
        return settings.canBePushed;
    }
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

    /*
     * Disable standard entity sync
     */

    @Override
    @SideOnly(Side.CLIENT)
    public void setPositionAndRotationDirect(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean teleport) {
        if (settings.defaultMovement) {
            super.setPositionAndRotationDirect(x, y, z, yaw, pitch, posRotationIncrements, teleport);
        }
    }

    @Override
    public void setVelocity(double x, double y, double z) {
        if (settings.defaultMovement) {
            super.setVelocity(x, y, z);
        }
    }

    /*
    * TODO!!!
     */
    /*
    //@Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return true;
        }
        return false;//super.hasCapability(energyCapability, facing);
    }

    @SuppressWarnings("unchecked")
	//@Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return (T) cargoItems;
        }
        return null;//super.getCapability(energyCapability, facing);
    }

	@Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @SuppressWarnings("unchecked")
	@Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return (T) theTank;
        }
        return super.getCapability(capability, facing);
    }
     */
}
