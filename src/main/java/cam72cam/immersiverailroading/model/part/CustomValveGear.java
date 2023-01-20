package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.ConfigGraphics;
import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.entity.LocomotiveSteam;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.ModelComponentType.ModelPosition;
import cam72cam.immersiverailroading.library.Particles;
import cam72cam.immersiverailroading.model.ModelState;
import cam72cam.immersiverailroading.model.animation.Animatrix;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.render.SmokeParticle;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.resource.Identifier;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class CustomValveGear implements ValveGear {
    private final WheelSet wheels;
    private final List<ModelComponent> components;

    private final Animatrix animation;
    private final ModelComponent pistonRod;
    private Float pistonStart;
    private Float pistonEnd;

    public static CustomValveGear get(Identifier custom, WheelSet wheels, ComponentProvider provider, ModelState state, ModelPosition pos) {
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

        return !components.isEmpty() ? new CustomValveGear(state, custom, wheels, components) : null;
    }

    public CustomValveGear(ModelState state, Identifier custom, WheelSet wheels, List<ModelComponent> components) {
        this.wheels = wheels;
        this.components = components;

        try {
            animation = new Animatrix(custom.getResourceStream(), true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        state.push(settings -> settings.add((ModelState.GroupAnimator) (stock, group) ->
                animation.groups().contains(group) ? animation.getMatrix(group, angle(stock.distanceTraveled) / 360) : null)
        ).include(components);

        pistonRod = components.stream().filter(x -> x.type == ModelComponentType.PISTON_ROD_SIDE).findFirst().orElse(null);
        if (pistonRod != null) {
            //noinspection OptionalGetWithoutIsPresent
            String pistonGroup = pistonRod.modelIDs.stream().findFirst().get();

            // Detect piston extents
            pistonStart = 0f;
            Vec3d initial = animation.getMatrix(pistonGroup, 0).apply(pistonRod.center);
            Vec3d pistonStartPos = initial;

            for (float i = 0; i < 1; i+= 0.05) {
                Vec3d pos = animation.getMatrix(pistonGroup, i).apply(pistonRod.center);
                if (pos.distanceToSquared(initial) > pistonStartPos.distanceToSquared(initial)) {
                    pistonStartPos = pos;
                    pistonStart = i;
                }
            }

            pistonEnd = 0f;
            Vec3d pistonEndPos = pistonStartPos;
            for (float i = 0; i < 1; i+= 0.05) {
                Vec3d pos = animation.getMatrix(pistonGroup, i).apply(pistonRod.center);
                if (pos.distanceToSquared(pistonStartPos) > pistonEndPos.distanceToSquared(pistonStartPos)) {
                    pistonEndPos = pos;
                    pistonEnd = i;
                }
            }
        } else {
            pistonStart = null;
            pistonEnd = null;
        }
    }

    @Override
    public void effects(EntityMoveableRollingStock stock, float throttle) {
        if (pistonRod == null) {
            return;
        }

        if (ConfigGraphics.particlesEnabled && isEndStroke(stock, throttle)) {
            Vec3d particlePos = stock.getPosition().add(VecUtil.rotateWrongYaw(pistonRod.min.scale(stock.gauge.scale()), stock.getRotationYaw() + 180));
            double accell = 0.3 * stock.gauge.scale();
            if (pistonRod.pos.contains(ModelPosition.LEFT)) {
                accell = -accell;
            }
            if (pistonRod.pos.contains(ModelPosition.CENTER)) {
                accell = 0;
            }
            Vec3d sideMotion = stock.getVelocity().add(VecUtil.fromWrongYaw(accell, stock.getRotationYaw()+90));
            Particles.SMOKE.accept(new SmokeParticle.SmokeParticleData(stock.getWorld(), particlePos, new Vec3d(sideMotion.x, sideMotion.y+0.01 * stock.gauge.scale(), sideMotion.z), 80, 0, 0.6f, 0.2 * stock.gauge.scale(), stock.getDefinition().steamParticleTexture));
        }

        if (ConfigSound.soundEnabled && stock instanceof LocomotiveSteam) {
            String key = String.format("%s-%s", stock.getUUID(), pistonRod.pos);
            StephensonValveGear.ChuffSound sound = StephensonValveGear.chuffSounds.get(key);
            if (sound == null) {
                sound = new StephensonValveGear.ChuffSound((LocomotiveSteam) stock);
                StephensonValveGear.chuffSounds.put(key, sound);
            }
            sound.update(isEndStroke(stock, throttle, 0.125f));
        }
    }

    @Override
    public boolean isEndStroke(EntityMoveableRollingStock stock, float throttle) {
        if (pistonRod == null) {
            return false;
        }

        float delta = 0.03f;
        if (stock instanceof LocomotiveSteam) {
            LocomotiveSteam loco = (LocomotiveSteam) stock;
            if (Math.abs(loco.getThrottle() * loco.getReverser()) == 0) {
                return false;
            }

            delta = Math.abs(loco.getReverser())/4;
        }
        return isEndStroke(stock, throttle, delta);
    }
    public boolean isEndStroke(EntityMoveableRollingStock stock, float throttle, float delta) {
        if (pistonRod == null) {
            return false;
        }

        double percent = angle(stock.distanceTraveled / stock.gauge.scale()) / 360;

        // There's probably a much better way of doing this...
        return Math.abs(percent - pistonStart) < delta ||
                Math.abs(percent - pistonStart - 1) < delta ||
                Math.abs(percent - pistonStart + 1) < delta ||
                Math.abs(percent - pistonEnd) < delta ||
                Math.abs(percent - pistonEnd - 1) < delta ||
                Math.abs(percent - pistonEnd + 1) < delta;
    }

    @Override
    public float angle(double distance) {
        return wheels.angle(distance);
    }
}
