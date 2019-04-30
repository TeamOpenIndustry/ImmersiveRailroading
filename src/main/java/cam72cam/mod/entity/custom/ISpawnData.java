package cam72cam.mod.entity.custom;

import cam72cam.mod.util.TagCompound;

public interface ISpawnData {
    void loadSpawn(TagCompound data);

    void saveSpawn(TagCompound data);

    static ISpawnData get(Object o) {
        if (o instanceof ISpawnData) {
            return (ISpawnData) o;
        }
        return ISpawnData.NOP;
    }

    ISpawnData NOP = new ISpawnData() {

        @Override
        public void loadSpawn(TagCompound data) {

        }

        @Override
        public void saveSpawn(TagCompound data) {

        }
    };
}
