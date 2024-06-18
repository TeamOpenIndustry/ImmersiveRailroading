package cam72cam.immersiverailroading.multiblock;

import cam72cam.immersiverailroading.registry.MultiblockDefinition;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Rotation;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;

public class CustomMultiblock extends Multiblock{
    /*TODO:
    * Define multiblock through json
    * use Animatrix to Produce animation(I/O for factory)
    * new readouts
    */
    private MultiblockDefinition refer;

    public CustomMultiblock(MultiblockDefinition def){
        super(def.name.toUpperCase(), parseStructure(def));
        this.refer = def;
    }

    protected CustomMultiblock(String name, FuzzyProvider[][][] components) {
        super(name, components);
    }

    private static FuzzyProvider[][][] parseStructure(MultiblockDefinition def){
        FuzzyProvider[][][] provider = new FuzzyProvider[def.width][def.height][def.length];
        for(Vec3i entry : def.structure.keySet()){
            provider[entry.z][entry.y][entry.x] = parseFuzzy(def.structure.get(entry));
        }
        return provider;
    }

    private static FuzzyProvider parseFuzzy(String def){
        def = def.toUpperCase();
        if(def.equals("L_ENG"))
            return L_ENG();
        else if(def.equals("H_ENG"))
            return H_ENG();
        return STEEL();
    }

    @Override
    public Vec3i placementPos() {
        return Vec3i.ZERO;
    }

    @Override
    protected MultiblockInstance newInstance(World world, Vec3i origin, Rotation rot) {
        return new CustomMultiblockInstance(world,origin,rot);
    }

    public class CustomMultiblockInstance extends MultiblockInstance{

        public CustomMultiblockInstance(World world, Vec3i origin, Rotation rot) {
            super(world, origin, rot);
        }

        @Override
        public boolean onBlockActivated(Player player, Player.Hand hand, Vec3i offset) {
            return false;
        }

        @Override
        public int getInvSize(Vec3i offset) {
            return 0;
        }

        @Override
        public boolean isRender(Vec3i offset) {
            return true;
        }

        @Override
        public void tick(Vec3i offset) {

        }

        @Override
        public boolean canInsertItem(Vec3i offset, int slot, ItemStack stack) {
            return false;
        }

        @Override
        public boolean isOutputSlot(Vec3i offset, int slot) {
            return false;
        }

        @Override
        public int getSlotLimit(Vec3i offset, int slot) {
            return 0;
        }

        @Override
        public boolean canRecievePower(Vec3i offset) {
            return false;
        }
    }
}
