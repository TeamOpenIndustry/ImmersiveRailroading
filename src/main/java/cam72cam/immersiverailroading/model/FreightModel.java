package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.entity.Freight;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.part.CargoFill;
import cam72cam.immersiverailroading.model.part.CargoItems;
import cam72cam.immersiverailroading.model.part.CargoUnload;
import cam72cam.immersiverailroading.registry.FreightDefinition;
import cam72cam.mod.ModCore;
import cam72cam.mod.entity.ItemEntity;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.opengl.RenderState;
import util.Matrix4;

import javax.vecmath.Matrix4f;
import java.util.List;
import java.util.stream.Collectors;

public class FreightModel<ENTITY extends Freight, DEFINITION extends FreightDefinition> extends StockModel<ENTITY, DEFINITION> {
    private CargoFill cargoFill;
    private CargoItems cargoItems;
    private List<CargoUnload> cargoUnload;

    public FreightModel(DEFINITION def) throws Exception {
        super(def);
    }

    protected void parseComponents(ComponentProvider provider, DEFINITION def) {
        super.parseComponents(provider, def);
        this.cargoFill = CargoFill.get(provider, rocking, def.shouldShowCurrentLoadOnly(), null);
        this.cargoItems = CargoItems.get(provider);
        this.cargoUnload = CargoUnload.get(provider);
    }

    public List<ItemStack> checkItems(Freight stock, List<ItemEntity> entities) {
        Matrix4f stockMatrix = stock.getModelMatrix().toMatrix4f();
        stockMatrix.invert();
        Matrix4 matrix4 = new Matrix4(stockMatrix);
        //Transform into stock's relative coordinate
        return entities.stream()
                .filter(entity -> {
                    //Transform into stock's relative coordinate
                    Vec3d pos = matrix4.apply(entity.getPosition());
                    ModCore.info(String.valueOf(pos));
                    boolean flag = false;
                    if (this.cargoFill != null) {
                        flag |= this.cargoFill.checkInBound(pos);
                    }
                    if (this.cargoItems != null) {
                        flag |= this.cargoItems.checkInBound(pos);
                    }
                    return flag;
                })
                .filter(entity -> !(entity.getThrower().equals(stock.getUUID()) && entity.getTickCount() >= 20))
                .map(ItemEntity::getContent)
                .collect(Collectors.toList());
    }

    public List<CargoUnload> getUnloadingPoints() {
        return cargoUnload;
    }

    @Override
    protected void postRender(ENTITY stock, RenderState state, float partialTicks) {
        super.postRender(stock, state, partialTicks);

        if (cargoItems != null) {
            cargoItems.postRender(stock, state);
        }
    }
}
