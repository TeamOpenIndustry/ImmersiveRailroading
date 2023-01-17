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

    public SteamLocomotiveModel(LocomotiveSteamDefinition def) throws Exception {
        super(def);
        idleSounds = new PartSound(stock -> stock.createSound(def.idle, true, 40));
    }

    @Override
    protected void parseControllable(ComponentProvider provider, LocomotiveSteamDefinition def) {
        super.parseControllable(provider, def);
        if (!def.isCabCar()) {
            addGauge(provider, ModelComponentType.GAUGE_TEMPERATURE_X, Readouts.TEMPERATURE);
            addGauge(provider, ModelComponentType.GAUGE_BOILER_PRESSURE_X, Readouts.BOILER_PRESSURE);
        }

        addControl(provider, ModelComponentType.WHISTLE_CONTROL_X);
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

        float throttle = stock.getThrottle() * stock.getReverser();
        if (drivingWheels != null) {
            drivingWheels.effects(stock, throttle);
        }
        if (drivingWheelsFront != null) {
            drivingWheelsFront.effects(stock, throttle);
        }
        if (drivingWheelsRear != null) {
            drivingWheelsRear.effects(stock, throttle);
        }
        if (chimney != null) {
            chimney.effects(stock,
                    (drivingWheels != null && drivingWheels.isEndStroke(stock, throttle)) ||
                            (drivingWheelsFront != null && drivingWheelsFront.isEndStroke(stock, throttle)) ||
                            (drivingWheelsRear != null && drivingWheelsRear.isEndStroke(stock, throttle)));
        }
        pressureValve.effects(stock, stock.isOverpressure() && Config.isFuelRequired(stock.gauge));

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
