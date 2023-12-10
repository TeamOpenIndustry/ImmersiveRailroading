package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.entity.Locomotive;
import cam72cam.immersiverailroading.gui.overlay.Readouts;
import cam72cam.immersiverailroading.library.ModelComponentType.ModelPosition;
import cam72cam.immersiverailroading.model.part.*;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.ValveGearConfig;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.model.part.TrackFollower.TrackFollowers;
import cam72cam.immersiverailroading.registry.LocomotiveDefinition;
import util.Matrix4;

import java.util.List;

public class LocomotiveModel<ENTITY extends Locomotive, DEFINITION extends LocomotiveDefinition> extends FreightTankModel<ENTITY, DEFINITION> {
    private List<ModelComponent> components;
    private Bell bell;

    private ModelComponent frameFront;
    private ModelComponent frameRear;
    protected DrivingAssembly drivingWheelsFront;
    protected DrivingAssembly drivingWheelsRear;
    private CargoFill cargoFillFront;
    private CargoFill cargoFillRear;
    private ModelComponent shellFront;
    private ModelComponent shellRear;

    protected ModelState frontLocomotive;
    protected ModelState frontLocomotiveRocking;
    protected ModelState rearLocomotive;
    protected ModelState rearLocomotiveRocking;
    private final TrackFollowers frontTrackers;
    private final TrackFollowers rearTrackers;

    public LocomotiveModel(DEFINITION def) throws Exception {
        super(def);
        frontTrackers = new TrackFollowers(s -> new TrackFollower(s, frameFront, drivingWheelsFront != null ? drivingWheelsFront.wheels : null, true));
        rearTrackers = new TrackFollowers(s -> new TrackFollower(s, frameRear, drivingWheelsRear != null ? drivingWheelsRear.wheels : null, false));
    }

    @Override
    protected void addGauge(ComponentProvider provider, ModelComponentType type, Readouts value) {
        gauges.addAll(Readout.getReadouts(provider, frontLocomotiveRocking, type, ModelPosition.FRONT_LOCOMOTIVE, value));
        gauges.addAll(Readout.getReadouts(provider, rearLocomotiveRocking, type, ModelPosition.REAR_LOCOMOTIVE, value));
        super.addGauge(provider, type, value);
    }

    @Override
    protected void addControl(ComponentProvider provider, ModelComponentType type) {
        controls.addAll(Control.get(provider, frontLocomotiveRocking, type, ModelPosition.FRONT_LOCOMOTIVE));
        controls.addAll(Control.get(provider, rearLocomotiveRocking, type, ModelPosition.REAR_LOCOMOTIVE));
        super.addControl(provider, type);
    }

    @Override
    protected void addDoor(ComponentProvider provider) {
        this.doors.addAll(Door.get(provider, frontLocomotiveRocking, ModelPosition.FRONT_LOCOMOTIVE));
        this.doors.addAll(Door.get(provider, rearLocomotiveRocking, ModelPosition.REAR_LOCOMOTIVE));
        super.addDoor(provider);
    }

    @Override
    protected void addHeadlight(DEFINITION def, ComponentProvider provider, ModelComponentType type) {
        headlights.addAll(LightFlare.get(def, provider, frontLocomotiveRocking, type, ModelPosition.FRONT_LOCOMOTIVE));
        headlights.addAll(LightFlare.get(def, provider, rearLocomotiveRocking, type, ModelPosition.REAR_LOCOMOTIVE));
        headlights.addAll(LightFlare.get(def, provider, frontLocomotiveRocking, type, ModelPosition.FRONT));
        headlights.addAll(LightFlare.get(def, provider, rearLocomotiveRocking, type, ModelPosition.REAR));
        super.addHeadlight(def, provider, type);
    }

    @Override
    protected void parseControllable(ComponentProvider provider, DEFINITION def) {
        gauges.addAll(Readout.getReadouts(provider, frontLocomotiveRocking, ModelComponentType.COUPLED_X, ModelPosition.FRONT_LOCOMOTIVE, Readouts.COUPLED_FRONT));
        gauges.addAll(Readout.getReadouts(provider, rearLocomotiveRocking, ModelComponentType.COUPLED_X, ModelPosition.REAR_LOCOMOTIVE, Readouts.COUPLED_REAR));

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
    protected void initStates() {
        super.initStates();
        frontLocomotive = base.push(settings -> settings.add((EntityMoveableRollingStock s, float partialTicks) -> getFrontLocomotiveMatrix(s)));
        frontLocomotiveRocking = addRoll(frontLocomotive);
        rearLocomotive = base.push(settings -> settings.add((EntityMoveableRollingStock s, float partialTicks) -> getRearLocomotiveMatrix(s)));
        rearLocomotiveRocking = addRoll(rearLocomotive);
    }

    @Override
    protected void parseComponents(ComponentProvider provider, DEFINITION def) {
        ValveGearConfig type = def.getValveGear();
        boolean showCurrentLoadOnly = def.shouldShowCurrentLoadOnly();

        frameFront = provider.parse(ModelComponentType.FRONT_FRAME);
        shellFront = provider.parse(ModelComponentType.FRONT_SHELL);
        frontLocomotiveRocking.include(frameFront);
        frontLocomotiveRocking.include(shellFront);

        cargoFillFront = CargoFill.get(provider, frontLocomotiveRocking, showCurrentLoadOnly, ModelPosition.FRONT);
        drivingWheelsFront = DrivingAssembly.get(type, provider, frontLocomotive, ModelPosition.FRONT, 0);


        frameRear = provider.parse(ModelComponentType.REAR_FRAME);
        shellRear = provider.parse(ModelComponentType.REAR_SHELL);
        rearLocomotiveRocking.include(frameRear);
        rearLocomotiveRocking.include(shellRear);

        cargoFillRear = CargoFill.get(provider, rearLocomotiveRocking, showCurrentLoadOnly, ModelPosition.REAR);
        drivingWheelsRear = DrivingAssembly.get(type, provider, rearLocomotive, ModelPosition.REAR, 45);

        components = provider.parse(
                new ModelComponentType[]{ModelComponentType.CAB}
        );
        rocking.include(components);
        bell = Bell.get(
                provider, rocking,
                def.bell);

        super.parseComponents(provider, def);
    }

    // TODO rename to tick
    @Override
    protected void effects(ENTITY stock) {
        super.effects(stock);
        bell.effects(stock, stock.getBell() > 0 ? 0.8f : 0);
    }

    @Override
    protected void removed(ENTITY stock) {
        super.removed(stock);

        if (frontTrackers != null) {
            frontTrackers.remove(stock);
        }
        if (rearTrackers != null) {
            rearTrackers.remove(stock);
        }

        bell.removed(stock);
    }

    private Matrix4 getFrontLocomotiveMatrix(EntityMoveableRollingStock s) {
        return frontTrackers.get(s).getMatrix();
    }

    public float getFrontLocomotiveYaw(EntityMoveableRollingStock s) {
        return frontTrackers.get(s).getYawReadout();
    }

    private Matrix4 getRearLocomotiveMatrix(EntityMoveableRollingStock s) {
        return rearTrackers.get(s).getMatrix();
    }

    public float getRearLocomotiveYaw(EntityMoveableRollingStock s) {
        return rearTrackers.get(s).getYawReadout();
    }
}
