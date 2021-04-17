package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.entity.LocomotiveDiesel;
import cam72cam.immersiverailroading.library.RenderComponentType;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.registry.LocomotiveDieselDefinition;

import java.util.List;

public class DieselLocomotiveModel extends LocomotiveModel<LocomotiveDiesel> {
    private List<ModelComponent> components;
    private List<ModelComponent> exhaust;

    public DieselLocomotiveModel(LocomotiveDieselDefinition def) throws Exception {
        super(def);
    }

    @Override
    protected void parseComponents(ComponentProvider provider, EntityRollingStockDefinition def) {
        super.parseComponents(provider, def);

        components = provider.parse(
                RenderComponentType.FUEL_TANK,
                RenderComponentType.ALTERNATOR,
                RenderComponentType.ENGINE_BLOCK,
                RenderComponentType.CRANKSHAFT,
                RenderComponentType.GEARBOX,
                RenderComponentType.FLUID_COUPLING,
                RenderComponentType.FINAL_DRIVE,
                RenderComponentType.TORQUE_CONVERTER
        );

        components.addAll(
                provider.parseAll(
                        RenderComponentType.PISTON_X,
                        RenderComponentType.FAN_X,
                        RenderComponentType.DRIVE_SHAFT_X
                )
        );

        exhaust = provider.parseAll(RenderComponentType.DIESEL_EXHAUST_X);
    }

    @Override
    protected void render(LocomotiveDiesel stock, ComponentRenderer draw, double distanceTraveled) {
        super.render(stock, draw, distanceTraveled);
        draw.render(components);
    }

    public List<ModelComponent> getExhaust() {
        return exhaust;
    }
}
