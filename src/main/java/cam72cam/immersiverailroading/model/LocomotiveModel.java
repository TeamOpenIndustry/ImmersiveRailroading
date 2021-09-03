package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.entity.Locomotive;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.model.part.Bell;
import cam72cam.immersiverailroading.model.part.Control;
import cam72cam.immersiverailroading.model.part.Readout;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.registry.LocomotiveDefinition;

import java.util.ArrayList;
import java.util.List;

public class LocomotiveModel<T extends Locomotive> extends FreightTankModel<T> {
    private List<ModelComponent> components;
    private Bell bell;
    private List<Control> throttles;
    private List<Control> reversers;
    private List<Control> train_brakes;
    private List<Control> throttle_brakes;
    private List<Readout<T>> gauges;

    public LocomotiveModel(LocomotiveDefinition def) throws Exception {
        super(def);
    }

    @Override
    protected void parseComponents(ComponentProvider provider, EntityRollingStockDefinition def) {
        super.parseComponents(provider, def);

        components = provider.parse(
                new ModelComponentType[]{ModelComponentType.CAB}
        );
        bell = Bell.get(
                provider,
                ((LocomotiveDefinition)def).bell
        );
        throttle_brakes = Control.get(provider, ModelComponentType.THROTTLE_BRAKE_X);
        throttles = Control.get(provider, ModelComponentType.THROTTLE_X);
        reversers = Control.get(provider, ModelComponentType.REVERSER_X);
        train_brakes = Control.get(provider, ModelComponentType.TRAIN_BRAKE_X);
        gauges = new ArrayList<>();
        gauges.addAll(Readout.getReadouts(provider, ModelComponentType.GAUGE_SPEED_X,
                stock -> (float) (Math.abs(stock.getCurrentSpeed().metric()) / stock.getDefinition().getMaxSpeed(stock.gauge).metric()))
        );
        gauges.addAll(Readout.getReadouts(provider, ModelComponentType.GAUGE_THROTTLE_X, Locomotive::getThrottle));
        gauges.addAll(Readout.getReadouts(provider, ModelComponentType.GAUGE_REVERSER_X, Locomotive::getReverser));
        gauges.addAll(Readout.getReadouts(provider, ModelComponentType.GAUGE_TRAIN_BRAKE_X, Locomotive::getAirBrake));
    }

    @Override
    public List<Control> getDraggableComponents() {
        List<Control> draggable = super.getDraggableComponents();
        draggable.addAll(throttle_brakes);
        draggable.addAll(throttles);
        draggable.addAll(reversers);
        draggable.addAll(train_brakes);
        return draggable;
    }

    @Override
    public List<Readout<T>> getReadouts() {
        List<Readout<T>> readouts = super.getReadouts();
        readouts.addAll(gauges);
        return readouts;
    }

    @Override
    protected void effects(T stock) {
        super.effects(stock);
        bell.effects(stock, stock.getBell() > 0 ? 0.8f : 0);
    }

    @Override
    protected void removed(T stock) {
        super.removed(stock);
        bell.removed(stock);
    }

    @Override
    protected void render(T stock, ComponentRenderer draw, double distanceTraveled) {
        super.render(stock, draw, distanceTraveled);
        draw.render(components);
        bell.render(draw);
    }
}
