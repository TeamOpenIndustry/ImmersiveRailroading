package cam72cam.mod.entity;

import cam72cam.mod.World;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;

import java.util.UUID;

public class Entity {
    public final net.minecraft.entity.Entity internal;

    public Entity(net.minecraft.entity.Entity entity) {
        this.internal = entity;
    }

    public World getWorld() {
        return new World(internal.world);
    }

    public UUID getUUID() {
        return internal.getPersistentID();
    }

    public Vec3d getPosition() {
        return new Vec3d(internal.getPositionVector());
    }
    public Vec3i getBlockPosition() {
        return new Vec3i(internal.getPosition());
    }
}
