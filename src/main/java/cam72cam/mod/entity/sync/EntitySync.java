package cam72cam.mod.entity.sync;

import cam72cam.mod.entity.Entity;
import cam72cam.mod.util.TagCompound;
import net.minecraft.nbt.NBTBase;

public class EntitySync extends TagCompound {
    private final Entity entity;
    private TagCompound old;

    public EntitySync(Entity entity) {
        super();
        this.entity = entity;
    }

    public void send() {
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

        old = this;

        if (sync.internal.getKeySet().size() != 0) {
            // TODO send data
            //entity.sendToObserving(new Packet);
        }
    }

    public void receive(TagCompound sync) {
        for (String key : sync.internal.getKeySet()) {
            internal.setTag(key, sync.internal.getTag(key));
        }
        old = this;
    }
}
