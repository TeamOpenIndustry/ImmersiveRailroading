package cam72cam.mod.entity.custom;

import cam72cam.mod.util.TagCompound;

public interface IWorldData {
    void load(TagCompound data);

    void save(TagCompound data);

    static IWorldData get(Object o) {
        if (o instanceof IWorldData) {
            return (IWorldData) o;
        }
        return IWorldData.NOP;
    }

    IWorldData NOP = new IWorldData() {
        @Override
        public void load(TagCompound data) {

        }

        @Override
        public void save(TagCompound data) {

        }
    };
}
