package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.entity.LocomotiveSteam;
import cam72cam.immersiverailroading.gui.overlay.Readouts;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.model.part.*;
import cam72cam.immersiverailroading.registry.LocomotiveSteamDefinition;

import java.util.List;

public class SteamLocomotiveModel extends LocomotiveModel<LocomotiveSteam, LocomotiveSteamDefinition> {
    private List<ModelComponent> components;

    private Whistle whistle;
    private SteamChimney chimney;
    private PressureValve pressureValve;
    private ModelComponent firebox;

    private final PartSound idleSounds;
    private final PartSound drainSounds;

    public SteamLocomotiveModel(LocomotiveSteamDefinition def) throws Exception {
        super(def);
        idleSounds = new PartSound(stock -> stock.createSound(def.idle, true, 40));
        drainSounds = new PartSound(stock -> stock.createSound(def.cyliner_drain, true, 40));
    }

    @Override
    protected void parseControllable(ComponentProvider provider, LocomotiveSteamDefinition def) {
        super.parseControllable(provider, def);
        if (!def.isCabCar()) {
            addGauge(provider, ModelComponentType.GAUGE_TEMPERATURE_X, Readouts.TEMPERATURE);
            addGauge(provider, ModelComponentType.GAUGE_BOILER_PRESSURE_X, Readouts.BOILER_PRESSURE);
        }

        addControl(provider, ModelComponentType.WHISTLE_CONTROL_X);
        addControl(provider, ModelComponentType.CYLINDER_DRAIN_CONTROL_X);
    }

    @Override
    protected void parseComponents(ComponentProvider provider, LocomotiveSteamDefinition def) {
        firebox = provider.parse(ModelComponentType.FIREBOX);
        rocking.push(builder -> {
            builder.add((ModelState.Lighter) stock -> {
                return new ModelState.LightState(null, null, !Config.isFuelRequired(stock.gauge) || ((LocomotiveSteam)stock).getBurnTime().values().stream().anyMatch(x -> x > 1), null);
            });
        }).include(firebox);

        components = provider.parse(
                ModelComponentType.SMOKEBOX,
                ModelComponentType.PIPING
        );

        components.addAll(provider.parseAll(
                ModelComponentType.BOILER_SEGMENT_X
        ));
        rocking.include(components);

        whistle = Whistle.get(provider, rocking, def.quill, def.whistle);

        chimney = SteamChimney.get(provider);
        pressureValve = PressureValve.get(provider, def.pressure);

        super.parseComponents(provider, def);
    }

    @Override
    protected boolean unifiedBogies() {
        return false;
    }

    @Override
    protected void effects(LocomotiveSteam stock) {
        super.effects(stock);

        boolean isEndStroke = (drivingWheels != null && drivingWheels.isEndStroke(stock)) ||
                (drivingWheelsFront != null && drivingWheelsFront.isEndStroke(stock)) ||
                (drivingWheelsRear != null && drivingWheelsRear.isEndStroke(stock));
        if (drivingWheels != null) {
            drivingWheels.effects(stock);
        }
        if (drivingWheelsFront != null) {
            drivingWheelsFront.effects(stock);
        }
        if (drivingWheelsRear != null) {
            drivingWheelsRear.effects(stock);
        }
        if (chimney != null) {
            chimney.effects(stock, isEndStroke);
        }
        pressureValve.effects(stock, stock.isOverpressure() && Config.isFuelRequired(stock.gauge));
        drainSounds.effects(stock, isEndStroke && stock.cylinderDrainsEnabled() ? stock.getThrottle() : 0, 1);
        idleSounds.effects(stock, stock.getBoilerTemperature() > stock.ambientTemperature() + 5 ? 0.1f : 0);
        whistle.effects(stock, stock.getBoilerPressure() > 0 || !Config.isFuelRequired(stock.gauge) ? stock.getHornTime() : 0, stock.isAutomatedHorn(), stock.getHornPlayer());
    }

    @Override
    protected void removed(LocomotiveSteam stock) {
        super.removed(stock);

        pressureValve.removed(stock);
        idleSounds.removed(stock);
        whistle.removed(stock);
    }
}
