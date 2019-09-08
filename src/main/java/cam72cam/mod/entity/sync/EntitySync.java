package cam72cam.mod.entity.sync;

import cam72cam.mod.entity.Entity;
import cam72cam.mod.net.Packet;
import cam72cam.mod.util.TagCompound;
import net.minecraft.nbt.NBTBase;

public class EntitySync extends TagCompound {
    private final Entity entity;
    private TagCompound old;
    private String oldString;

    public EntitySync(Entity entity) {
        super();
        this.entity = entity;
        this.old = new TagCompound();
        this.oldString = old.toString();
    }

    public void send() {
        // Is this faster than the below check?
        // Could also put a bool tracker in TagCompound
        if (oldString.equals(this.toString())) {
            return;
        }

        TagCompound sync = new TagCompound();
        for (String key : internal.getKeySet()) {
            NBTBase newVal = internal.getTag(key);
            if (old.internal.hasKey(key)) {
                NBTBase oldVal = old.internal.getTag(key);
                if (newVal.equals(oldVal)) {
                    continue;
                }
            }
            sync.internal.setTag(key, newVal);
        }
        //TODO removed keys!

        if (sync.internal.getKeySet().size() != 0) {
            old = new TagCompound(this.internal.copy());
            oldString = old.toString();

            entity.sendToObserving(new EntitySyncPacket(entity, sync));
        }
    }

    public void receive(TagCompound sync) {
        for (String key : sync.internal.getKeySet()) {
            internal.setTag(key, sync.internal.getTag(key));
        }
        old = this;
    }

    public static class EntitySyncPacket extends Packet {
        public EntitySyncPacket() {
            // Reflection
        }

        public EntitySyncPacket(Entity entity, TagCompound sync) {
            data.setEntity("target", entity);
            data.set("info", sync);
        }

        @Override
        public void handle() {
            Entity stock = data.getEntity("target", getWorld(), Entity.class);
            if (stock != null) {
                stock.sync.receive(data.get("info"));
            }
        }
    }
}
