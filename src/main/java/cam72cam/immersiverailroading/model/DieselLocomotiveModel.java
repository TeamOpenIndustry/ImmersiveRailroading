package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.LocomotiveDiesel;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.ValveGearType;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.model.part.*;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.registry.LocomotiveDieselDefinition;
import cam72cam.immersiverailroading.render.ExpireableList;

import java.util.List;
import java.util.UUID;

public class DieselLocomotiveModel extends LocomotiveModel<LocomotiveDiesel> {
    private List<ModelComponent> components;
    private DieselExhaust exhaust;
    private Horn horn;
    private final PartSound idle;
    private DrivingAssembly drivingWheels;
    private ModelComponent frameFront;
    private ModelComponent frameRear;
    private ModelComponent shellFront;
    private ModelComponent shellRear;
    private DrivingAssembly drivingWheelsFront;
    private DrivingAssembly drivingWheelsRear;
    private List<Control> engineStarters;
    private List<Control> hornControls;

    private final ExpireableList<UUID, TrackFollower> frontTrackers = new ExpireableList<>();
    private final ExpireableList<UUID, TrackFollower> rearTrackers = new ExpireableList<>();

    public DieselLocomotiveModel(LocomotiveDieselDefinition def) throws Exception {
        super(def);
        idle = new PartSound(stock -> ImmersiveRailroading.newSound(def.idle, true, 80, stock.soundGauge()));
    }

    @Override
    protected void parseComponents(ComponentProvider provider, EntityRollingStockDefinition def) {
        frameFront = provider.parse(ModelComponentType.FRONT_FRAME);
        frameRear = provider.parse(ModelComponentType.REAR_FRAME);
        shellFront = provider.parse(ModelComponentType.FRONT_SHELL);
        shellRear = provider.parse(ModelComponentType.REAR_SHELL);

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

        engineStarters = Control.get(this, provider, ModelComponentType.ENGINE_START_X);
        hornControls = Control.get(this, provider, ModelComponentType.HORN_CONTROL_X);

        exhaust = DieselExhaust.get(provider);
        horn = Horn.get(provider, ((LocomotiveDieselDefinition)def).horn, ((LocomotiveDieselDefinition)def).getHornSus());

        ValveGearType type = def.getValveGear();
        drivingWheelsFront = DrivingAssembly.get(type,provider, "FRONT", 0);
        drivingWheelsRear = DrivingAssembly.get(type, provider, "REAR", 45);
        drivingWheels = DrivingAssembly.get(type, provider, null, 0);

        super.parseComponents(provider, def);
    }

    @Override
    protected void effects(LocomotiveDiesel stock) {
        super.effects(stock);
        exhaust.effects(stock);
        horn.effects(stock,
                stock.getHornTime() > 0 && stock.isRunning()
                        ? stock.getDefinition().getHornSus() ? stock.getHornTime() / 10f : 1
                        : 0);
        idle.effects(stock, stock.isRunning() ? Math.max(0.1f, stock.getSoundThrottle()) : 0, 0.7f+stock.getSoundThrottle()/4);
    }

    @Override
    protected void removed(LocomotiveDiesel stock) {
        super.removed(stock);
        frontTrackers.put(stock.getUUID(), null);
        rearTrackers.put(stock.getUUID(), null);
        horn.removed(stock);
        idle.removed(stock);
    }

    @Override
    public List<Control> getDraggableComponents() {
        List<Control> controls = super.getDraggableComponents();
        controls.addAll(engineStarters);
        controls.addAll(hornControls);
        return controls;
    }

    @Override
    protected void render(LocomotiveDiesel stock, ComponentRenderer draw, double distanceTraveled) {
        super.render(stock, draw, distanceTraveled);
        draw.render(components);
        horn.render(draw);

        engineStarters.forEach(c -> c.render(stock, draw));
        hornControls.forEach(c -> c.render(stock, draw));

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
                if (shellFront != null) {
                    matrix.render(shellFront);
                }
                drivingWheelsFront.render(distanceTraveled, stock.getThrottle(), matrix);
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
                if (shellRear != null) {
                    matrix.render(shellRear);
                }
                drivingWheelsRear.render(distanceTraveled, stock.getThrottle(), matrix);
            }
        }
    }

    @Override
    protected void postRender(LocomotiveDiesel stock, ComponentRenderer draw, double distanceTraveled) {
        super.postRender(stock, draw, distanceTraveled);

        engineStarters.forEach(c -> c.postRender(stock));
        hornControls.forEach(c -> c.postRender(stock));
    }
}
