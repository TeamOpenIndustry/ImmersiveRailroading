package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.entity.Locomotive;
import cam72cam.immersiverailroading.gui.overlay.Readouts;
import cam72cam.immersiverailroading.library.ModelComponentType.ModelPosition;
import cam72cam.immersiverailroading.model.part.*;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.ValveGearType;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.model.part.TrackFollower.TrackFollowers;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.registry.LocomotiveDefinition;
import util.Matrix4;

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

    private final TrackFollowers frontTrackers;
    private final TrackFollowers rearTrackers;

    public LocomotiveModel(LocomotiveDefinition def) throws Exception {
        super(def);
        frontTrackers = frameFront != null ? new TrackFollowers(s -> frameFront.center) : null;
        rearTrackers = frameRear != null ? new TrackFollowers(s -> frameRear.center) : null;
    }

    @Override
    protected void addGauge(ComponentProvider provider, ModelComponentType type, Readouts value) {
        gauges.addAll(Readout.getReadouts(provider, type, ModelPosition.FRONT_LOCOMOTIVE, value, this::getFrontLocomotiveMatrix));
        gauges.addAll(Readout.getReadouts(provider, type, ModelPosition.REAR_LOCOMOTIVE, value, this::getRearLocomotiveMatrix));
        super.addGauge(provider, type, value);
    }

    @Override
    protected void addControl(ComponentProvider provider, ModelComponentType type) {
        controls.addAll(Control.get(provider, type, ModelPosition.FRONT_LOCOMOTIVE, this::getFrontLocomotiveMatrix));
        controls.addAll(Control.get(provider, type, ModelPosition.REAR_LOCOMOTIVE, this::getRearLocomotiveMatrix));
        super.addControl(provider, type);
    }

    @Override
    protected void addDoor(ComponentProvider provider) {
        this.doors.addAll(Door.get(provider, ModelPosition.FRONT_LOCOMOTIVE, this::getFrontLocomotiveMatrix));
        this.doors.addAll(Door.get(provider, ModelPosition.REAR_LOCOMOTIVE, this::getRearLocomotiveMatrix));
        super.addDoor(provider);
    }

    @Override
    protected void addHeadlight(EntityRollingStockDefinition def, ComponentProvider provider, ModelComponentType type) {
        headlights.addAll(LightFlare.get(def, provider, type, ModelPosition.FRONT_LOCOMOTIVE, this::getFrontLocomotiveMatrix));
        headlights.addAll(LightFlare.get(def, provider, type, ModelPosition.REAR_LOCOMOTIVE, this::getRearLocomotiveMatrix));
        headlights.addAll(LightFlare.get(def, provider, type, ModelPosition.FRONT, this::getFrontLocomotiveMatrix));
        headlights.addAll(LightFlare.get(def, provider, type, ModelPosition.REAR, this::getRearLocomotiveMatrix));
        super.addHeadlight(def, provider, type);
    }

    @Override
    protected void parseControllable(ComponentProvider provider, EntityRollingStockDefinition def) {
        gauges.addAll(Readout.getReadouts(provider, ModelComponentType.COUPLED_X, ModelPosition.FRONT_LOCOMOTIVE, Readouts.COUPLED_FRONT, this::getFrontLocomotiveMatrix));
        gauges.addAll(Readout.getReadouts(provider, ModelComponentType.COUPLED_X, ModelPosition.REAR_LOCOMOTIVE, Readouts.COUPLED_REAR, this::getRearLocomotiveMatrix));

        super.parseControllable(provider, def);

        addGauge(provider, ModelComponentType.GAUGE_SPEED_X, Readouts.SPEED);
        addGauge(provider, ModelComponentType.GAUGE_THROTTLE_X, Readouts.THROTTLE);
        addGauge(provider, ModelComponentType.GAUGE_REVERSER_X, Readouts.REVERSER);
        addGauge(provider, ModelComponentType.GAUGE_TRAIN_BRAKE_X, Readouts.TRAIN_BRAKE);
        if (def.hasIndependentBrake()) {
            addGauge(provider, ModelComponentType.GAUGE_INDEPENDENT_BRAKE_X, Readouts.INDEPENDENT_BRAKE);
        }

        addControl(provider, ModelComponentType.BELL_CONTROL_X);
        addControl(provider, ModelComponentType.THROTTLE_BRAKE_X);
        addControl(provider, ModelComponentType.THROTTLE_X);
        addControl(provider, ModelComponentType.REVERSER_X);
        addControl(provider, ModelComponentType.TRAIN_BRAKE_X);
        if (def.hasIndependentBrake()) {
            addControl(provider, ModelComponentType.INDEPENDENT_BRAKE_X);
        }
    }

    @Override
    protected void parseComponents(ComponentProvider provider, EntityRollingStockDefinition def) {
        ValveGearType type = def.getValveGear();

        frameFront = provider.parse(ModelComponentType.FRONT_FRAME);
        cargoFront = Cargo.get(provider, ModelPosition.FRONT);
        shellFront = provider.parse(ModelComponentType.FRONT_SHELL);
        drivingWheelsFront = DrivingAssembly.get(type,provider, ModelPosition.FRONT, 0);

        frameRear = provider.parse(ModelComponentType.REAR_FRAME);
        cargoRear = Cargo.get(provider, ModelPosition.REAR);
        shellRear = provider.parse(ModelComponentType.REAR_SHELL);
        drivingWheelsRear = DrivingAssembly.get(type, provider, ModelPosition.REAR, 45);

        drivingWheels = DrivingAssembly.get(type, provider, null, 0);

        components = provider.parse(
                new ModelComponentType[]{ModelComponentType.CAB}
        );
        bell = Bell.get(
                provider,
                ((LocomotiveDefinition)def).bell
        );

        super.parseComponents(provider, def);
    }

    // TODO rename to tick
    @Override
    protected void effects(T stock) {
        super.effects(stock);
        bell.effects(stock, stock.getBell() > 0 ? 0.8f : 0);
    }

    @Override
    protected void removed(T stock) {
        super.removed(stock);

        if (frontTrackers != null) {
            frontTrackers.remove(stock);
        }
        if (rearTrackers != null) {
            rearTrackers.remove(stock);
        }

        bell.removed(stock);
    }

    private Matrix4 getFrontLocomotiveMatrix(T s) {
        return frontTrackers != null ? frontTrackers.get(s).getMatrix() : null;
    }

    private Matrix4 getRearLocomotiveMatrix(T s) {
        return rearTrackers != null ? rearTrackers.get(s).getMatrix() : null;
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
                if (frontTrackers != null) {
                    matrix.mult(frontTrackers.get(stock).getMatrix());
                }
                matrix.render(frameFront);
                drivingWheelsFront.render(distanceTraveled, stock.getReverser(), matrix);
                matrix.render(shellFront);
                if (cargoFront != null) {
                    cargoFront.render(stock.getPercentCargoFull(), stock.getDefinition().shouldShowCurrentLoadOnly(), matrix);
                }
            }
        }
        if (drivingWheelsRear != null) {
            try (ComponentRenderer matrix = draw.push()) {
                if (rearTrackers != null) {
                    matrix.mult(rearTrackers.get(stock).getMatrix());
                }
                matrix.render(frameRear);
                drivingWheelsRear.render(distanceTraveled, stock.getReverser(), matrix);
                matrix.render(shellRear);
                if (cargoRear != null) {
                    cargoRear.render(stock.getPercentCargoFull(), stock.getDefinition().shouldShowCurrentLoadOnly(), matrix);
                }
            }
        }
    }

    @Override
    protected void renderWithInteriorLighting(T stock, ComponentRenderer draw) {
        super.renderWithInteriorLighting(stock, draw);
        draw.render(components);
    }
}
