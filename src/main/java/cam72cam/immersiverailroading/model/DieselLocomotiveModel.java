package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.LocomotiveDiesel;
import cam72cam.immersiverailroading.gui.overlay.Readouts;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.model.part.*;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.registry.LocomotiveDieselDefinition;

import java.util.List;

public class DieselLocomotiveModel extends LocomotiveModel<LocomotiveDiesel> {
    private List<ModelComponent> components;
    private DieselExhaust exhaust;
    private Horn horn;
    private final PartSound idle;
    private List<Control> engineStarters;
    private List<Control> hornControls;
    private List<Readout<LocomotiveDiesel>> gauges;

    public DieselLocomotiveModel(LocomotiveDieselDefinition def) throws Exception {
        super(def);
        idle = def.isCabCar() ? null : new PartSound(stock -> ImmersiveRailroading.newSound(def.idle, true, 80, stock.soundGauge()));
    }

    @Override
    protected void parseComponents(ComponentProvider provider, EntityRollingStockDefinition def) {
        gauges = Readout.getReadouts(provider, ModelComponentType.GAUGE_TEMPERATURE_X, Readouts.TEMPERATURE);

        components = provider.parse(
                ModelComponentType.FUEL_TANK,
                ModelComponentType.ALTERNATOR,
                ModelComponentType.ENGINE_BLOCK,
                ModelComponentType.CRANKSHAFT,
                ModelComponentType.GEARBOX,
                ModelComponentType.FLUID_COUPLING,
                ModelComponentType.FINAL_DRIVE,
                ModelComponentType.TORQUE_CONVERTER
        );

        components.addAll(
                provider.parseAll(
                        ModelComponentType.PISTON_X,
                        ModelComponentType.FAN_X,
                        ModelComponentType.DRIVE_SHAFT_X
                )
        );

        engineStarters = Control.get(provider, ModelComponentType.ENGINE_START_X);
        hornControls = Control.get(provider, ModelComponentType.HORN_CONTROL_X);

        exhaust = DieselExhaust.get(provider);
        horn = Horn.get(provider, ((LocomotiveDieselDefinition)def).horn, ((LocomotiveDieselDefinition)def).getHornSus());

        super.parseComponents(provider, def);
    }

    @Override
    protected void effects(LocomotiveDiesel stock) {
        super.effects(stock);
        exhaust.effects(stock);
        horn.effects(stock,
                stock.getHornTime() > 0 && (stock.isRunning() || stock.getDefinition().isCabCar())
                        ? stock.getDefinition().getHornSus() ? stock.getHornTime() / 10f : 1
                        : 0);
        if (idle != null) {
            idle.effects(stock, stock.isRunning() ? Math.max(0.1f, stock.getSoundThrottle()) : 0, 0.7f + stock.getSoundThrottle() / 4);
        }
    }

    @Override
    protected void removed(LocomotiveDiesel stock) {
        super.removed(stock);
        horn.removed(stock);
        if (idle != null) {
            idle.removed(stock);
        }
    }

    @Override
    public List<Control> getDraggableComponents() {
        List<Control> controls = super.getDraggableComponents();
        controls.addAll(engineStarters);
        controls.addAll(hornControls);
        return controls;
    }

    @Override
    public List<Readout<LocomotiveDiesel>> getReadouts() {
        List<Readout<LocomotiveDiesel>> readouts = super.getReadouts();
        readouts.addAll(gauges);
        return readouts;
    }

    @Override
    protected void render(LocomotiveDiesel stock, ComponentRenderer draw, double distanceTraveled) {
        super.render(stock, draw, distanceTraveled);
        draw.render(components);
        horn.render(draw);
    }
}
