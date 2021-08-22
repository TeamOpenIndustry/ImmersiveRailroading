package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.entity.Locomotive;
import cam72cam.immersiverailroading.library.LightFlare;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.ValveGearType;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.model.part.Bell;
import cam72cam.immersiverailroading.model.part.Cargo;
import cam72cam.immersiverailroading.model.part.DrivingAssembly;
import cam72cam.immersiverailroading.model.part.TrackFollower;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.registry.LocomotiveDefinition;
import cam72cam.immersiverailroading.render.ExpireableList;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.Light;

import java.util.*;

public class LocomotiveModel<T extends Locomotive> extends FreightModel<T> {
    private List<ModelComponent> components;
    private Bell bell;

    protected DrivingAssembly drivingWheels;
    protected ModelComponent frameFront;
    protected ModelComponent frameRear;
    protected DrivingAssembly drivingWheelsFront;
    protected DrivingAssembly drivingWheelsRear;
    protected Cargo cargoFront;
    protected Cargo cargoRear;

    private final ExpireableList<UUID, TrackFollower> frontTrackers = new ExpireableList<>();
    private final ExpireableList<UUID, TrackFollower> rearTrackers = new ExpireableList<>();

    private List<LightFlare> headlightsFront;
    private List<LightFlare> headlightsRear;

    public LocomotiveModel(LocomotiveDefinition def) throws Exception {
        super(def);
    }

    @Override
    protected void parseComponents(ComponentProvider provider, EntityRollingStockDefinition def) {
        ValveGearType type = def.getValveGear();

        drivingWheels = DrivingAssembly.get(type, provider, null, 0);

        frameFront = provider.parse(ModelComponentType.FRONT_FRAME);
        cargoFront = Cargo.get(provider, "FRONT");
        drivingWheelsFront = DrivingAssembly.get(type,provider, "FRONT", 0);

        frameRear = provider.parse(ModelComponentType.REAR_FRAME);
        cargoRear = Cargo.get(provider, "REAR");
        drivingWheelsRear = DrivingAssembly.get(type, provider, "REAR", 45);

        components = provider.parse(
                new ModelComponentType[]{ModelComponentType.CAB}
        );
        bell = Bell.get(
                provider,
                ((LocomotiveDefinition)def).bell
        );
        headlightsFront = LightFlare.get(provider, ModelComponentType.HEADLIGHT_POS_X, "FRONT");
        headlightsRear = LightFlare.get(provider, ModelComponentType.HEADLIGHT_POS_X, "REAR");

        super.parseComponents(provider, def);
    }

    @Override
    protected void effects(T stock) {
        super.effects(stock);
        bell.effects(stock, stock.getBell() > 0 ? 0.8f : 0);
        if (stock.externalLightsEnabled()) {
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
        } else {
            headlightsFront.forEach(x -> x.removed(stock));
            headlightsRear.forEach(x -> x.removed(stock));
        }
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
                if (!headlightsFront.isEmpty()) {
                    try (ComponentRenderer light = matrix.withBrightGroups(true)) {
                        headlightsFront.forEach(x -> x.render(light));
                    }
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
                if (!headlightsRear.isEmpty()) {
                    try (ComponentRenderer light = matrix.withBrightGroups(true)) {
                        headlightsRear.forEach(x -> x.render(light));
                    }
                }
            }
        }
    }

    @Override
    protected void renderWithInteriorLighting(T stock, ComponentRenderer draw) {
        super.renderWithInteriorLighting(stock, draw);
        draw.render(components);
    }

    @Override
    protected void postRender(T stock, ComponentRenderer draw, double distanceTraveled) {
        super.postRender(stock, draw, distanceTraveled);
        if (stock.externalLightsEnabled()) {
            if (drivingWheelsFront != null) {
                float offset = 0;
                if (frameFront != null) {
                    frontTrackers.get(stock.getUUID()).apply(stock);
                    offset = frontTrackers.get(stock.getUUID()).getYaw();
                }
                for (LightFlare flare : headlightsFront) {
                    flare.postRender(stock, offset);
                }
            }
            if (drivingWheelsRear != null) {
                float offset = 0;
                if (frameRear != null) {
                    rearTrackers.get(stock.getUUID()).apply(stock);
                    offset = rearTrackers.get(stock.getUUID()).getYaw();
                }
                for (LightFlare flare : headlightsRear) {
                    flare.postRender(stock, offset);
                }
            }
        }
    }
}
