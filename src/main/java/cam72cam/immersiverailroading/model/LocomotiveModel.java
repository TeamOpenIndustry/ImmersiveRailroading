package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.entity.Locomotive;
import cam72cam.immersiverailroading.gui.overlay.Readouts;
import cam72cam.immersiverailroading.model.part.LightFlare;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.ValveGearType;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.model.part.Bell;
import cam72cam.immersiverailroading.model.part.Cargo;
import cam72cam.immersiverailroading.model.part.DrivingAssembly;
import cam72cam.immersiverailroading.model.part.TrackFollower;
import cam72cam.immersiverailroading.model.part.Control;
import cam72cam.immersiverailroading.model.part.Readout;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.registry.LocomotiveDefinition;
import cam72cam.immersiverailroading.render.ExpireableList;

import java.util.*;
import java.util.List;

public class LocomotiveModel<T extends Locomotive> extends FreightTankModel<T> {
    private List<ModelComponent> components;
    private Bell bell;

    protected DrivingAssembly drivingWheels;
    private ModelComponent frameFront;
    private ModelComponent frameRear;
    protected DrivingAssembly drivingWheelsFront;
    protected DrivingAssembly drivingWheelsRear;
    private Cargo cargoFront;
    private Cargo cargoRear;
    private ModelComponent shellFront;
    private ModelComponent shellRear;

    private final ExpireableList<UUID, TrackFollower> frontTrackers = new ExpireableList<>();
    private final ExpireableList<UUID, TrackFollower> rearTrackers = new ExpireableList<>();

    private List<LightFlare> headlightsFront;
    private List<LightFlare> headlightsRear;
    private List<Readout<T>> front_gauges;
    private List<Readout<T>> rear_gauges;

    public LocomotiveModel(LocomotiveDefinition def) throws Exception {
        super(def);
    }

    @Override
    protected void parseComponents(ComponentProvider provider, EntityRollingStockDefinition def) {
        front_gauges = Readout.getReadouts(provider, ModelComponentType.BRAKE_PRESSURE_POS_X, "FRONT_LOCOMOTIVE", Readouts.BRAKE_PRESSURE);
        rear_gauges = Readout.getReadouts(provider, ModelComponentType.BRAKE_PRESSURE_POS_X, "REAR_LOCOMOTIVE", Readouts.BRAKE_PRESSURE);

        ValveGearType type = def.getValveGear();

        drivingWheels = DrivingAssembly.get(type, provider, null, 0);

        frameFront = provider.parse(ModelComponentType.FRONT_FRAME);
        cargoFront = Cargo.get(provider, "FRONT");
        shellFront = provider.parse(ModelComponentType.FRONT_SHELL);
        drivingWheelsFront = DrivingAssembly.get(type,provider, "FRONT", 0);

        frameRear = provider.parse(ModelComponentType.REAR_FRAME);
        cargoRear = Cargo.get(provider, "REAR");
        shellRear = provider.parse(ModelComponentType.REAR_SHELL);
        drivingWheelsRear = DrivingAssembly.get(type, provider, "REAR", 45);

        components = provider.parse(
                new ModelComponentType[]{ModelComponentType.CAB}
        );
        controls.addAll(Control.get(provider, ModelComponentType.BELL_CONTROL_X));
        bell = Bell.get(
                provider,
                ((LocomotiveDefinition)def).bell
        );
        headlightsFront = LightFlare.get(def, provider, ModelComponentType.HEADLIGHT_POS_X, "FRONT");
        headlightsRear = LightFlare.get(def, provider, ModelComponentType.HEADLIGHT_POS_X, "REAR");

        gauges.addAll(Readout.getReadouts(provider, ModelComponentType.GAUGE_SPEED_X, Readouts.SPEED));
        gauges.addAll(Readout.getReadouts(provider, ModelComponentType.GAUGE_THROTTLE_X, Readouts.THROTTLE));
        gauges.addAll(Readout.getReadouts(provider, ModelComponentType.GAUGE_REVERSER_X, Readouts.REVERSER));
        gauges.addAll(Readout.getReadouts(provider, ModelComponentType.GAUGE_TRAIN_BRAKE_X, Readouts.TRAIN_BRAKE));
        if (def.hasIndependentBrake()) {
            gauges.addAll(Readout.getReadouts(provider, ModelComponentType.GAUGE_INDEPENDENT_BRAKE_X, Readouts.INDEPENDENT_BRAKE));
        }

        controls.addAll(Control.get(provider, ModelComponentType.THROTTLE_BRAKE_X));
        controls.addAll(Control.get(provider, ModelComponentType.THROTTLE_X));
        controls.addAll(Control.get(provider, ModelComponentType.REVERSER_X));
        controls.addAll(Control.get(provider, ModelComponentType.TRAIN_BRAKE_X));
        if (def.hasIndependentBrake()) {
            controls.addAll(Control.get(provider, ModelComponentType.INDEPENDENT_BRAKE_X));
        }

        super.parseComponents(provider, def);
    }

    @Override
    protected void effects(T stock) {
        super.effects(stock);
        bell.effects(stock, stock.getBell() > 0 ? 0.8f : 0);
        if (drivingWheelsFront != null) {
            float offset = 0;
            if (frameFront != null && frontTrackers.get(stock.getUUID()) != null) {
                offset = frontTrackers.get(stock.getUUID()).getYaw();
            }
            for (LightFlare flare : headlightsFront) {
                flare.effects(stock, offset);
            }
        }
        if (drivingWheelsRear != null && rearTrackers.get(stock.getUUID()) != null) {
            float offset = 0;
            if (frameRear != null) {
                offset = rearTrackers.get(stock.getUUID()).getYaw();
            }
            for (LightFlare flare : headlightsRear) {
                flare.effects(stock, offset);
            }
        }

        front_gauges.forEach(c -> c.effects(stock));
        rear_gauges.forEach(c -> c.effects(stock));
    }

    @Override
    protected void removed(T stock) {
        super.removed(stock);

        frontTrackers.put(stock.getUUID(), null);
        rearTrackers.put(stock.getUUID(), null);

        bell.removed(stock);
        headlightsFront.forEach(x -> x.removed(stock));
        headlightsRear.forEach(x -> x.removed(stock));
    }

    @Override
    protected void render(T stock, ComponentRenderer draw, double distanceTraveled) {
        super.render(stock, draw, distanceTraveled);
        bell.render(draw);

        if (drivingWheels != null) {
            drivingWheels.render(distanceTraveled, stock.getReverser(), draw);
        }
        if (drivingWheelsFront != null) {
            try (ComponentRenderer matrix = draw.push()) {
                if (frameFront != null) {
                    TrackFollower data = frontTrackers.get(stock.getUUID());
                    if (data == null) {
                        data = new TrackFollower(frameFront.center);
                        frontTrackers.put(stock.getUUID(), data);
                    }
                    data.apply(stock, matrix);
                    matrix.render(frameFront);
                }
                drivingWheelsFront.render(distanceTraveled, stock.getReverser(), matrix);
                matrix.render(shellFront);
                if (cargoFront != null) {
                    cargoFront.render(stock.getPercentCargoFull(), stock.getDefinition().shouldShowCurrentLoadOnly(), matrix);
                }
                if (!headlightsFront.isEmpty()) {
                    try (ComponentRenderer light = matrix.withBrightGroups(true)) {
                        headlightsFront.forEach(x -> x.render(light, stock));
                    }
                }

                front_gauges.forEach(r -> r.render(stock, matrix));
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
                    data.apply(stock, matrix);
                    matrix.render(frameRear);
                }
                drivingWheelsRear.render(distanceTraveled, stock.getReverser(), matrix);
                matrix.render(shellRear);
                if (cargoRear != null) {
                    cargoRear.render(stock.getPercentCargoFull(), stock.getDefinition().shouldShowCurrentLoadOnly(), matrix);
                }
                if (!headlightsRear.isEmpty()) {
                    try (ComponentRenderer light = matrix.withBrightGroups(true)) {
                        headlightsRear.forEach(x -> x.render(light, stock));
                    }
                }

                rear_gauges.forEach(r -> r.render(stock, matrix));
            }
        }
    }

    @Override
    protected void renderWithInteriorLighting(T stock, ComponentRenderer draw) {
        super.renderWithInteriorLighting(stock, draw);
        draw.render(components);
    }

    @Override
    protected void postRender(T stock) {
        super.postRender(stock);
        if (drivingWheelsFront != null) {
            float offset = 0;
            if (frameFront != null) {
                frontTrackers.get(stock.getUUID()).apply(stock, null);
                offset = frontTrackers.get(stock.getUUID()).getYaw();
            }
            for (LightFlare flare : headlightsFront) {
                flare.postRender(stock, offset);
            }
        }
        if (drivingWheelsRear != null) {
            float offset = 0;
            if (frameRear != null) {
                rearTrackers.get(stock.getUUID()).apply(stock, null);
                offset = rearTrackers.get(stock.getUUID()).getYaw();
            }
            for (LightFlare flare : headlightsRear) {
                flare.postRender(stock, offset);
            }
        }
    }
}
