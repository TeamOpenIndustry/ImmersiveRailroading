package cam72cam.mod.block;

public class BlockSettings {
    final String modID;
    final String name;
    Material material;
    float hardness;
    float resistance;
    boolean connectable;

    public BlockSettings(String modID, String name) {
        this.modID = modID;
        this.name = name;
        this.material = Material.METAL;
        this.hardness = 1F;
        this.resistance = hardness * 5;
        this.connectable = true;
    }

    public BlockSettings(BlockSettings settings) {
        this.modID = settings.modID;
        this.name = settings.name;
        this.material = settings.material;
        this.hardness = settings.hardness;
        this.resistance = settings.resistance;
        this.connectable = settings.connectable;
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
