package cam72cam.immersiverailroading.model.part;

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

    public void tryToUnload(Freight freight){
        int slotIndex = 0;
        int count = freight.cargoItems.getSlotCount();
        while (freight.cargoItems.get(slotIndex).getCount() == 0){
            slotIndex++;
            if(slotIndex >= count) {
                return;
            }
        }
        double openFactor = freight.getControlPosition(this.controlGroup);
        if (openFactor != 0) {
            //For some stocks if we directly drop the items it will enter a cycle of being loaded and unloaded, so we'd better add an offset here
            Vec3d offset = this.getPos().z > 0 ? new Vec3d(0,0,0.25) : new Vec3d(0,0,-0.25);
            freight.getWorld().dropItem(
                    freight.cargoItems.extract(slotIndex,(int)Math.floor(openFactor * 3),false),//20~60 per sec
                    freight.getModelMatrix().apply(this.getPos().add(offset)),
                    offset.scale(0.2).rotateYaw(freight.getRotationYaw() - 90));
        }
    }
}
