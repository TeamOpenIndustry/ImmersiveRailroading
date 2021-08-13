package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.LocomotiveSteam;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.ValveGearType;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.model.part.*;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.registry.LocomotiveSteamDefinition;
import cam72cam.immersiverailroading.render.ExpireableList;

import java.util.List;
import java.util.UUID;

public class SteamLocomotiveModel extends LocomotiveModel<LocomotiveSteam> {
    private List<ModelComponent> components;
    private DrivingAssembly drivingWheels;
    private ModelComponent frameFront;
    private ModelComponent frameRear;
    private DrivingAssembly drivingWheelsFront;
    private DrivingAssembly drivingWheelsRear;

    private Whistle whistle;
    private SteamChimney chimney;
    private PressureValve pressureValve;

    private final ExpireableList<UUID, TrackFollower> frontTrackers = new ExpireableList<>();
    private final ExpireableList<UUID, TrackFollower> rearTrackers = new ExpireableList<>();
    private final PartSound idleSounds;
    private Cargo cargoFront;
    private Cargo cargoRear;

    public SteamLocomotiveModel(LocomotiveSteamDefinition def) throws Exception {
        super(def);
        idleSounds = new PartSound(stock -> ImmersiveRailroading.newSound(def.idle, true, 40, stock.soundGauge()));
    }

    @Override
    protected void parseComponents(ComponentProvider provider, EntityRollingStockDefinition def) {
        frameFront = provider.parse(ModelComponentType.FRONT_FRAME);
        cargoFront = Cargo.get(provider, "FRONT");
        frameRear = provider.parse(ModelComponentType.REAR_FRAME);
        cargoRear = Cargo.get(provider, "REAR");

        components = provider.parse(
                ModelComponentType.FIREBOX,
                ModelComponentType.SMOKEBOX,
                ModelComponentType.PIPING
        );

        components.addAll(provider.parseAll(
                ModelComponentType.BOILER_SEGMENT_X
        ));

        whistle = Whistle.get(provider, ((LocomotiveSteamDefinition) def).quill, ((LocomotiveSteamDefinition) def).whistle);

        chimney = SteamChimney.get(provider);
        pressureValve = PressureValve.get(provider, ((LocomotiveSteamDefinition) def).pressure);

        ValveGearType type = def.getValveGear();
        drivingWheelsFront = DrivingAssembly.get(type,provider, "FRONT", 0);
        drivingWheelsRear = DrivingAssembly.get(type, provider, "REAR", 45);
        drivingWheels = DrivingAssembly.get(type, provider, null, 0);

        super.parseComponents(provider, def);
    }

    @Override
    protected boolean unifiedBogies() {
        return false;
    }

    @Override
    protected void effects(LocomotiveSteam stock) {
        super.effects(stock);

        float throttle = stock.getThrottle();
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
        whistle.effects(stock, stock.getBoilerPressure() > 0 || !Config.isFuelRequired(stock.gauge) ? stock.getHornTime() : 0, stock.getHornPlayer());
    }

    @Override
    protected void removed(LocomotiveSteam stock) {
        super.removed(stock);

        frontTrackers.put(stock.getUUID(), null);
        rearTrackers.put(stock.getUUID(), null);
        pressureValve.removed(stock);
        idleSounds.removed(stock);
        whistle.removed(stock);
    }

    @Override
    protected void render(LocomotiveSteam stock, ComponentRenderer draw, double distanceTraveled) {
        super.render(stock, draw, distanceTraveled);
        draw.render(components);

        whistle.render(draw);

        if (drivingWheels != null) {
            drivingWheels.render(distanceTraveled, stock.getThrottle(), draw);
        }
        if (drivingWheelsFront != null) {
            try (ComponentRenderer matrix = draw.push()) {
                if (frameFront != null) {
                    TrackFollower data = frontTrackers.get(stock.getUUID());
                    if (data == null) {
                        data = new TrackFollower(frameFront.center);
                        frontTrackers.put(stock.getUUID(), data);
                    }
                    data.apply(stock);
                    matrix.render(frameFront);
                }
                drivingWheelsFront.render(distanceTraveled, stock.getThrottle(), matrix);
                if (cargoFront != null) {
                    cargoFront.render(stock.getPercentCargoFull(), stock.getDefinition().shouldShowCurrentLoadOnly(), matrix);
                }
            }
        }
        if (drivingWheelsRear != null) {
            try (ComponentRenderer matrix = draw.push()) {
                if (frameRear != null) {
                    TrackFollower data = rearTrackers.get(stock.getUUID());
                    if (data == null) {
                        data = new TrackFollower(frameRear.center);
                        rearTrackers.put(stock.getUUID(), data);
                    }
                    data.apply(stock);
                    matrix.render(frameRear);
                }
                drivingWheelsRear.render(distanceTraveled, stock.getThrottle(), matrix);
                if (cargoRear != null) {
                    cargoRear.render(stock.getPercentCargoFull(), stock.getDefinition().shouldShowCurrentLoadOnly(), matrix);
                }
            }
        }
    }
}
