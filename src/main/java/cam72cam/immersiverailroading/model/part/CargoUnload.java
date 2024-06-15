package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.Freight;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.mod.math.Vec3d;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CargoUnload {
    private final Vec3d pos;
    private final String controlGroup;
    private double size;//TODO Perhaps we should use the component's size to calculate output speed?

    public static List<CargoUnload> get(ComponentProvider provider) {
        return provider.parseAll(ModelComponentType.CARGO_UNLOAD_X)
                .stream().map(CargoUnload::new).collect(Collectors.toList());
    }

    public CargoUnload(ModelComponent component) {
        this.pos = component.center;
        this.controlGroup = component.modelIDs.stream()
                .map(Pattern.compile("_CG_([^_]+)")::matcher).filter(Matcher::find).map(m -> m.group(1)).findFirst().orElse(null);
    }

    public Vec3d getPos() {
        return pos;
    }

    public double getOpenFactor(EntityRollingStock stock){
        return stock.getControlPosition(this.controlGroup);
    }

    public void tryToUnload(Freight freight){
        int slotIndex = 0;
        int count = freight.cargoItems.getSlotCount();
        while (freight.cargoItems.get(slotIndex).getCount() == 0){
            slotIndex++;
            if(slotIndex >= count) {
                return;
            }
        }
        if (this.getOpenFactor(freight) != 0) {
            Vec3d offset = this.getPos().z > 0 ? new Vec3d(0,0,1) : new Vec3d(0,0,-1);
            freight.getWorld().dropItem(freight.cargoItems.extract(slotIndex,(int)Math.floor(this.getOpenFactor(freight) * 3),false),//20~60 per sec
                    freight.getModelMatrix().apply(this.getPos().add(offset)));
        }
    }
}
