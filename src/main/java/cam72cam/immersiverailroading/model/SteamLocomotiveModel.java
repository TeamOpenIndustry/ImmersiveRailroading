package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.entity.LocomotiveSteam;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.ValveGearType;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.model.part.DrivingAssembly;
import cam72cam.immersiverailroading.model.part.TrackFollower;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.registry.LocomotiveSteamDefinition;
import cam72cam.immersiverailroading.render.ExpireableList;

import java.util.*;

public class SteamLocomotiveModel extends LocomotiveModel<LocomotiveSteam> {
    private List<ModelComponent> components;
    private DrivingAssembly drivingWheels;
    private ModelComponent frameFront;
    private ModelComponent frameRear;
    private DrivingAssembly drivingWheelsFront;
    private DrivingAssembly drivingWheelsRear;

    private final ExpireableList<UUID, TrackFollower> frontTrackers = new ExpireableList<>();
    private final ExpireableList<UUID, TrackFollower> rearTrackers = new ExpireableList<>();

    public SteamLocomotiveModel(LocomotiveSteamDefinition def) throws Exception {
        super(def);
    }

    @Override
    protected void parseComponents(ComponentProvider provider, EntityRollingStockDefinition def) {
        frameFront = provider.parse(ModelComponentType.FRONT_FRAME);
        frameRear = provider.parse(ModelComponentType.REAR_FRAME);

        super.parseComponents(provider, def);

        components = provider.parse(
                ModelComponentType.FIREBOX,
                ModelComponentType.SMOKEBOX,
                ModelComponentType.PIPING,
                ModelComponentType.WHISTLE
        );

        components.addAll(provider.parseAll(
                ModelComponentType.BOILER_SEGMENT_X
        ));

        provider.parseAll(ModelComponentType.PARTICLE_CHIMNEY_X);
        provider.parseAll(ModelComponentType.PRESSURE_VALVE_X);

        ValveGearType type = ((LocomotiveSteamDefinition) def).getValveGear();
        drivingWheelsFront = DrivingAssembly.get(type,provider, "FRONT");
        drivingWheelsRear = DrivingAssembly.get(type, provider, "REAR");
        drivingWheels = DrivingAssembly.get(type, provider, null);
    }

    @Override
    protected boolean unifiedBogies() {
        return false;
    }

    public double getDriverDiameter() {
        // TODO deeper integration with when sound / particles start
        double driverDiameter = drivingWheels != null ? drivingWheels.wheels.diameter() : 0;
        if (drivingWheelsFront != null) {
            driverDiameter = Math.max(driverDiameter, drivingWheelsFront.wheels.diameter());
        }
        if (drivingWheelsRear != null) {
            driverDiameter = Math.max(driverDiameter, drivingWheelsRear.wheels.diameter());
        }
        return driverDiameter;
    }

    @Override
    protected void render(LocomotiveSteam stock, ComponentRenderer draw, double distanceTraveled) {
        super.render(stock, draw, distanceTraveled);
        draw.render(components);

        if (drivingWheels != null) {
            drivingWheels.render(distanceTraveled, stock.getThrottle(), draw);
        }
        if (drivingWheelsFront != null) {
            try (ComponentRenderer matrix = draw.push()) {
                if (frameFront != null) {
                    TrackFollower data = frontTrackers.get(stock.getUUID());
                    if (data == null) {
                        data = new TrackFollower(stock, frameFront.center);
                        frontTrackers.put(stock.getUUID(), data);
                    }
                    data.apply();
                    matrix.render(frameFront);
                }
                drivingWheelsFront.render(distanceTraveled+1, stock.getThrottle(), matrix);
            }
        }
        if (drivingWheelsRear != null) {
            try (ComponentRenderer matrix = draw.push()) {
                if (frameRear != null) {
                    TrackFollower data = rearTrackers.get(stock.getUUID());
                    if (data == null) {
                        data = new TrackFollower(stock, frameRear.center);
                        rearTrackers.put(stock.getUUID(), data);
                    }
                    data.apply();
                    matrix.render(frameRear);
                }
                drivingWheelsRear.render(distanceTraveled+2, stock.getThrottle(), matrix);
            }
        }
    }
}
