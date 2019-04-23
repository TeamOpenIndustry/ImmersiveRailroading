package cam72cam.mod.block;

import cam72cam.mod.tile.TileEntity;

import java.util.function.Supplier;

public class BlockSettings {
    final String modID;
    final String name;
    Material material;
    float hardness;
    float resistance;
    boolean connectable;
    Supplier<TileEntity> entity;

    public BlockSettings(String modID, String name) {
        this.modID = modID;
        this.name = name;
        this.material = Material.METAL;
        this.hardness = 1F;
        this.resistance = hardness * 5;
        this.connectable = true;
        this.entity = null;
    }

    public BlockSettings(BlockSettings settings) {
        this.modID = settings.modID;
        this.name = settings.name;
        this.material = settings.material;
        this.hardness = settings.hardness;
        this.resistance = settings.resistance;
        this.connectable = settings.connectable;
        this.entity = settings.entity;
    }

    public BlockSettings withMaterial(Material material) {
        BlockSettings settings = new BlockSettings(this);
        settings.material = material;
        return settings;
    }

    public BlockSettings withHardness(float hardness) {
        BlockSettings settings = new BlockSettings(this);
        settings.hardness = hardness;
        settings.resistance = hardness * 5;
        return settings;
    }

    public BlockSettings withBlockEntity(Supplier<TileEntity> entity) {
        BlockSettings settings = new BlockSettings(this);
        settings.entity = entity;
        return settings;
    }

    public BlockSettings withExplosionResistance(int resistance) {
        BlockSettings settings = new BlockSettings(this);
        settings.resistance = resistance;
        return settings;
    }

    public BlockSettings withConnectable(boolean connectable) {
        BlockSettings settings = new BlockSettings(this);
        settings.connectable = connectable;
        return settings;
    }
}
