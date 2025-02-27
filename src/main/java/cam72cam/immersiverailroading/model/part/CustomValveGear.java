package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.entity.Locomotive;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.ModelComponentType.ModelPosition;
import cam72cam.immersiverailroading.library.ValveGearConfig;
import cam72cam.immersiverailroading.model.ModelState;
import cam72cam.immersiverailroading.model.animation.AnimatrixSet;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.mod.math.Vec3d;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class CustomValveGear extends ValveGear {

    private final AnimatrixSet animation;

    public static CustomValveGear get(ValveGearConfig custom, WheelSet wheels, ComponentProvider provider, ModelState state, ModelPosition pos) {
        List<ModelComponent> components = new ArrayList<>();

        components.add(provider.parse(ModelComponentType.MAIN_ROD_SIDE, pos));
        components.add(provider.parse(ModelComponentType.SIDE_ROD_SIDE, pos));
        components.add(provider.parse(ModelComponentType.PISTON_ROD_SIDE, pos));
        components.add(provider.parse(ModelComponentType.CYLINDER_SIDE, pos));
        components.add(provider.parse(ModelComponentType.UNION_LINK_SIDE, pos));
        components.add(provider.parse(ModelComponentType.COMBINATION_LEVER_SIDE, pos));
        components.add(provider.parse(ModelComponentType.ECCENTRIC_CRANK_SIDE, pos));
        components.add(provider.parse(ModelComponentType.ECCENTRIC_ROD_SIDE, pos));
        components.add(provider.parse(ModelComponentType.EXPANSION_LINK_SIDE, pos));
        components.add(provider.parse(ModelComponentType.RADIUS_BAR_SIDE, pos));
        components.add(provider.parse(ModelComponentType.VALVE_STEM_SIDE, pos));
        components.add(provider.parse(ModelComponentType.REVERSING_ARM_SIDE, pos));
        components.add(provider.parse(ModelComponentType.LIFTING_LINK_SIDE, pos));
        components.add(provider.parse(ModelComponentType.REACH_ROD_SIDE, pos));
        components.addAll(provider.parseAll(ModelComponentType.VALVE_PART_SIDE_ID, pos));

        components = components.stream().filter(Objects::nonNull).collect(Collectors.toList());

        ModelComponent frontExhaust = provider.parse(ModelComponentType.CYLINDER_DRAIN_SIDE, pos.and(ModelPosition.A));
        ModelComponent rearExhaust = provider.parse(ModelComponentType.CYLINDER_DRAIN_SIDE, pos.and(ModelPosition.B));

        return !components.isEmpty() ? new CustomValveGear(state, custom, wheels, components, frontExhaust, rearExhaust, provider.internal_model_scale) : null;
    }

    public CustomValveGear(ModelState state, ValveGearConfig custom, WheelSet wheels, List<ModelComponent> components, ModelComponent frontExhaust, ModelComponent rearExhaust, double internal_model_scale) {
        super(wheels, state, 0);

        try {
            animation = new AnimatrixSet(custom.custom, internal_model_scale);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        state.push(settings -> settings.add((ModelState.GroupAnimator) (animatable, group, partialTicks) -> animation.getMatrix(
                group,
                animatable instanceof Locomotive ? ((Locomotive) animatable).getReverser() : 0.0f,
                angle(animatable.asStock().distanceTraveled) / 360,
                true
        ))).include(components);

        ModelComponent pistonRod = components.stream().filter(x -> x.type == ModelComponentType.PISTON_ROD_SIDE).findFirst().orElse(null);
        if (pistonRod != null) {
            float reverser = 1.0f;

            //noinspection OptionalGetWithoutIsPresent
            String pistonGroup = pistonRod.modelIDs.stream().findFirst().get();

            // Detect piston extents
            float pistonStart = 0f;
            Vec3d initial = animation.getMatrix(pistonGroup, reverser, 0,  true).apply(pistonRod.center);
            Vec3d pistonStartPos = initial;

            for (float i = 0; i < 1; i+= 0.05) {
                Vec3d pos = animation.getMatrix(pistonGroup, reverser, i, true).apply(pistonRod.center);
                if (pos.distanceToSquared(initial) > pistonStartPos.distanceToSquared(initial)) {
                    pistonStartPos = pos;
                    pistonStart = i;
                }
            }

            float pistonEnd = 0f;
            Vec3d pistonEndPos = pistonStartPos;
            for (float i = 0; i < 1; i+= 0.05) {
                Vec3d pos = animation.getMatrix(pistonGroup, reverser, i, true).apply(pistonRod.center);
                if (pos.distanceToSquared(pistonStartPos) > pistonEndPos.distanceToSquared(pistonStartPos)) {
                    pistonEndPos = pos;
                    pistonEnd = i;
                }
            }

            state.include(frontExhaust);
            state.include(rearExhaust);

            this.frontExhaust = frontExhaust != null ?
                    new Exhaust(frontExhaust, pistonStart * 360) :
                    new Exhaust(pistonEndPos.add(pistonStartPos.subtract(pistonEndPos).scale(2)), pistonRod.pos, pistonStart * 360);
            this.rearExhaust = rearExhaust != null ?
                    new Exhaust(rearExhaust, pistonEnd * 360) :
                    new Exhaust(pistonStartPos, pistonRod.pos, pistonEnd * 360);
        }
    }
}
