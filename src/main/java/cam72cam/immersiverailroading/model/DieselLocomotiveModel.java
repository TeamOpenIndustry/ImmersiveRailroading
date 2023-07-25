package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.entity.LocomotiveDiesel;
import cam72cam.immersiverailroading.gui.overlay.Readouts;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.model.part.*;
import cam72cam.immersiverailroading.registry.LocomotiveDieselDefinition;

import java.util.List;

public class DieselLocomotiveModel extends LocomotiveModel<LocomotiveDiesel, LocomotiveDieselDefinition> {
    private List<ModelComponent> components;
    private DieselExhaust exhaust;
    private Horn horn;
    private final PartSound idle;
    private final PartSound running;

    public DieselLocomotiveModel(LocomotiveDieselDefinition def) throws Exception {
        super(def);
        idle = def.isCabCar() ? null : new PartSound(def.idle, true, 80);
        running = def.isCabCar() ? null : new PartSound(def.running, true, 80);
    }

    @Override
    protected void parseControllable(ComponentProvider provider, LocomotiveDieselDefinition def) {
        super.parseControllable(provider, def);
        addGauge(provider, ModelComponentType.GAUGE_TEMPERATURE_X, Readouts.TEMPERATURE);
        addControl(provider, ModelComponentType.ENGINE_START_X);
        addControl(provider, ModelComponentType.HORN_CONTROL_X);
    }

    @Override
    protected void parseComponents(ComponentProvider provider, LocomotiveDieselDefinition def) {
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

        rocking.include(components);

        exhaust = DieselExhaust.get(provider);
        horn = Horn.get(provider, rocking, def.horn, def.getHornSus());

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
            if (stock.isRunning()) {
                float volume = Math.max(0.1f, stock.getSoundThrottle());
                float pitch = 0.7f + stock.getSoundThrottle() / 4;
                if (running == null) {
                    // Simple
                    idle.effects(stock, volume, pitch);
                } else {
                    // Switching between two effects
                    if (stock.getSoundThrottle() < 0.01) {
                        // Idling
                        idle.effects(stock, 1, 1);
                        running.effects(stock, false);
                    } else {
                        idle.effects(stock, 0.01f);
                        running.effects(stock, volume, pitch);
                    }
                }
            } else {
                idle.effects(stock, false);
                if (running != null) {
                    running.effects(stock, false);
                }
            }
        }
    }

    @Override
    protected void removed(LocomotiveDiesel stock) {
        super.removed(stock);
        horn.removed(stock);
        if (idle != null) {
            idle.removed(stock);
        }
        if (running != null) {
            running.removed(stock);
        }
    }
}
