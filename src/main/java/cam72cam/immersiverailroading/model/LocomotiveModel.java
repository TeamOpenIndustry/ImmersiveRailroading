package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.entity.Locomotive;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.model.part.Bell;
import cam72cam.immersiverailroading.model.part.Control;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.registry.LocomotiveDefinition;
import scala.actors.threadpool.Arrays;

import java.util.ArrayList;
import java.util.List;

public class LocomotiveModel<T extends Locomotive> extends FreightModel<T> {
    private List<ModelComponent> components;
    private Bell bell;
    private Control throttle;
    private Control reverser;
    private Control train_brake;

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
        throttle = Control.get(this, provider, ModelComponentType.THROTTLE);
        reverser = Control.get(this, provider, ModelComponentType.REVERSER);
        train_brake = Control.get(this, provider, ModelComponentType.TRAIN_BRAKE);
    }

    @Override
    public List<ModelComponent> getDraggableComponents() {
        List<ModelComponent> draggable = new ArrayList<>();
        if (throttle != null) {
            draggable.add(throttle.part);
        }
        if (reverser != null) {
            draggable.add(reverser.part);
        }
        if (train_brake != null) {
            draggable.add(train_brake.part);
        }
        return draggable;
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
        if (throttle != null) {
            throttle.render(Math.abs(stock.getThrottle()), draw);
        }
        if (reverser != null) {
            reverser.render(stock.getThrottle() / 2, draw);
        }
        if (train_brake != null) {
            train_brake.render(stock.getAirBrake(), draw);
        }
    }
}
