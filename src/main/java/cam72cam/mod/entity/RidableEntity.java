package cam72cam.mod.entity;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.net.Packet;
import cam72cam.mod.util.Identifier;
import cam72cam.mod.util.TagCompound;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import java.util.*;
import java.util.function.Predicate;

public abstract class RidableEntity extends EntityBase {
    private Map<UUID, Vec3d> passengerPositions = new HashMap<>();
    private List<StaticPassenger> staticPassengers = new ArrayList<>();
    public Map<Integer, Vec3d> dismounts = new HashMap<>();

    public RidableEntity(World world) {
        super(world);
    }

    @Override
    protected void save(TagCompound data) {
        data.setMap("passengers", passengerPositions, UUID::toString, (Vec3d pos) -> {
            //TODO single encoder step
            TagCompound tmp = new TagCompound();
            tmp.setVec3d("pos", pos);
            return tmp;
        });
        data.setList("staticPassengers", staticPassengers, StaticPassenger::toTag);
    }

    @Override
    protected void load(TagCompound data) {
        passengerPositions = data.getMap("passengers", UUID::fromString, (TagCompound tag) -> tag.getVec3d("pos"));
        staticPassengers = data.getList("staticPassengers", StaticPassenger::new);
    }

    public boolean canFitPassenger(Entity passenger) {
        return true;
    }
    public final void addPassenger(Entity entity) {
        passengerPositions.put(entity.getUUID(), getMountPosition(entity));
        if (entity.is(EntityPlayer.class)) {
            entity.internal.startRiding(this);
            addPassenger(entity.internal);
        } else {
            StaticPassenger sp = new StaticPassenger(entity);
            staticPassengers.add(sp);
            entity.kill();
        }
        sendToObserving(new PassengerPositionsPacket(this));
    }

    @Override
    public final void updatePassenger(net.minecraft.entity.Entity passenger) {
        this.updatePassenger(new Entity(passenger));
    }
    public void updatePassenger(Entity passenger) {

    }

    public final Entity removePassenger() {
        // TODO players?
        return removePassenger((StaticPassenger p) -> true);
    }

    public final Entity removePassenger(Predicate<StaticPassenger> filter) {
        Optional<StaticPassenger> found = staticPassengers.stream().filter(filter).findFirst();
        if (found.isPresent()) {
            Entity ent = found.get().respawn(world.internal);

            Vec3d dismountPos = getDismountPosition(ent);
            onDismountPassenger(ent);
            ent.internal.setPosition(dismountPos.x, dismountPos.y, dismountPos.z);

            return ent;
        }
        return null;
    }

    public Vec3d getRidingOffset(Entity ent) {
        return this.passengerPositions.get(ent.getUUID());
    }
    public void setRidingOffset(Entity ent, Vec3d pos) {
        this.passengerPositions.put(ent.getUUID(), pos);
        sendToObserving(new PassengerPositionsPacket(this));
    }
    public Vec3d getMountPosition(cam72cam.mod.entity.Entity entity) {
        return Vec3d.ZERO;
    }
    public Vec3d getDismountPosition(Entity ent) {
        return self.getPosition();
    }
    public void onDismountPassenger(Entity entity) {

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

    protected boolean isPassenger(Entity passenger) {
        return super.isPassenger(passenger.internal);
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
            net.minecraft.entity.Entity ent = EntityList.createEntityByIDFromName(ident.internal, world);
            ent.readFromNBT(data.internal);
            return new cam72cam.mod.entity.Entity(ent);
        }
    }

    public static class PassengerPositionsPacket extends Packet {
        public PassengerPositionsPacket(RidableEntity stock) {
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
            RidableEntity stock = data.getEntity("stock", RidableEntity.class);
            stock.handlePassengerPositions(data.getMap("passengers", UUID::fromString, (TagCompound tag) -> tag.getVec3d("pos")));
            stock.staticPassengers = data.getList("staticPassengers", StaticPassenger::new);
        }
    }
}
