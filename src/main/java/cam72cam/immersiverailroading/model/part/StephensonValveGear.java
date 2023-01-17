package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.ConfigGraphics;
import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.entity.LocomotiveSteam;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.ModelComponentType.ModelPosition;
import cam72cam.immersiverailroading.library.Particles;
import cam72cam.immersiverailroading.model.ModelState;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.render.ExpireableMap;
import cam72cam.immersiverailroading.render.SmokeParticle;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.sound.ISound;
import util.Matrix4;

import java.util.*;
import java.util.stream.Collectors;

public class StephensonValveGear extends ConnectingRodValveGear {
    protected final ModelComponent drivingRod;
    protected final ModelComponent pistonRod;
    protected final ModelComponent cylinder;
    protected final boolean reverse;

    protected final Vec3d drivenWheel;

    public static StephensonValveGear get(WheelSet wheels, ComponentProvider provider, ModelState state, ModelPosition pos, float angleOffset) {
        ModelComponent drivingRod = provider.parse(ModelComponentType.MAIN_ROD_SIDE, pos);
        ModelComponent connectingRod = provider.parse(ModelComponentType.SIDE_ROD_SIDE, pos);
        ModelComponent pistonRod = provider.parse(ModelComponentType.PISTON_ROD_SIDE, pos);
        ModelComponent cylinder = provider.parse(ModelComponentType.CYLINDER_SIDE, pos);
        return drivingRod != null && connectingRod != null && pistonRod != null ?
                new StephensonValveGear(wheels, state, drivingRod, connectingRod, pistonRod, cylinder, angleOffset) : null;
    }
    public StephensonValveGear(WheelSet wheels, ModelState state, ModelComponent drivingRod, ModelComponent connectingRod, ModelComponent pistonRod, ModelComponent cylinder, float angleOffset) {
        super(wheels, state, connectingRod, angleOffset);
        this.drivingRod = drivingRod;
        this.pistonRod = pistonRod;
        this.cylinder = cylinder;
        Vec3d center = ModelComponent.center(wheels.wheels.stream().map(x -> x.wheel).collect(Collectors.toList()));
        this.reverse = pistonRod.center.x > center.x;
        this.angleOffset = angleOffset + (reverse ? -90 : 0);


        drivenWheel = wheels.wheels.stream().map(w -> w.wheel.center).min(Comparator.comparingDouble(w -> w.distanceTo(reverse ? drivingRod.min : drivingRod.max))).get();
        centerOfWheels = drivingRod.pos.equals(ModelPosition.CENTER) ? drivenWheel : center; // Bad hack for old TRI_WALSCHERTS code

        state.include(cylinder);

        state.push(builder -> builder.add((ModelState.Animator) stock -> {
            Matrix4 matrix = new Matrix4();

            Vec3d connRodMovment = connRodMovement(stock);

            // X: rear driving rod X - driving rod height/2 (hack assuming diameter == height)
            // Y: Center of the rod
            // Z: does not really matter due to rotation axis
            Vec3d drivingRodRotPoint = new Vec3d((reverse ? drivingRod.min.x + drivingRod.height()/2 : drivingRod.max.x - drivingRod.height()/2), drivingRod.center.y, reverse ? drivingRod.min.z : drivingRod.max.z);
            // Angle for movement height vs driving rod length (adjusted for assumed diameter == height, both sides == 2r)
            float drivingRodAngle = (float) Math.toDegrees(Math.atan2((reverse ? -connRodMovment.z : connRodMovment.z), drivingRod.length() - drivingRod.height()));

            // Move to conn rod center
            matrix.translate(-connRodRadius, 0, 0);
            // Apply conn rod movement
            matrix.translate(connRodMovment.x, connRodMovment.z, 0);

            // Move to rot point center
            matrix.translate(drivingRodRotPoint.x, drivingRodRotPoint.y, drivingRodRotPoint.z);
            // Rotate rod angle
            matrix.rotate(Math.toRadians(drivingRodAngle), 0, 0, 1);
            // Move back from rot point center
            matrix.translate(-drivingRodRotPoint.x, -drivingRodRotPoint.y, -drivingRodRotPoint.z);

            return matrix;
        })).include(drivingRod);

        state.push(builder -> builder.add((ModelState.Animator) stock -> {
            Matrix4 matrix = new Matrix4();

            Vec3d connRodMovment = connRodMovement(stock);

            // Piston movement is rod movement offset by the rotation radius
            // Not 100% accurate, missing the offset due to angled driving rod
            double pistonDelta = connRodMovment.x - connRodRadius;
            matrix.translate(pistonDelta, 0, 0);
            return matrix;
        })).include(pistonRod);
    }

    protected double getStroke(EntityMoveableRollingStock stock, float throttle, int shift, boolean speedLimit) {
        double csm = Math.abs(stock.getCurrentSpeed().metric()) / stock.gauge.scale();
        if ((csm > 0.1 || throttle > 0) && (csm < 20 || !speedLimit)) {
            float currentAngle = angle(stock.distanceTraveled / stock.gauge.scale()) + shift;
            return Math.abs(Math.sin(Math.toRadians(currentAngle)));
        }
        return 0;
    }

    public boolean isEndStroke(EntityMoveableRollingStock stock, float throttle) {
        double stroke = getStroke(stock, throttle, 0, true);
        if (stock instanceof LocomotiveSteam) {
            LocomotiveSteam loco = (LocomotiveSteam) stock;
            return Math.abs(loco.getThrottle() * loco.getReverser()) > 0 && stroke > 1.05 - Math.abs(loco.getReverser());
        }
        return stroke > 0.97;
    }

    private static class ChuffSound {
        private final LocomotiveSteam stock;
        private final float pitchOffset;
        private boolean pitchStroke;
        private boolean chuffOn;
        private final List<ISound> chuffs;
        private int chuffId;

        ChuffSound(LocomotiveSteam stock) {
            chuffOn = false;
            chuffId = 0;
            chuffs = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                chuffs.add(stock.createSound(stock.getDefinition().chuff, false, 80));
            }
            this.stock = stock;
            this.pitchOffset = (float) (Math.random() / 50);
            this.pitchStroke = false;
        }

        void update(boolean enteredStroke) {
            if (!chuffOn) {
                if (enteredStroke && Math.abs(stock.getThrottle() * stock.getReverser()) > 0) {
                    chuffOn = true;
                    pitchStroke = !pitchStroke;

                    double speed = Math.abs(stock.getCurrentSpeed().minecraft());
                    double maxSpeed = Math.abs(stock.getDefinition().getMaxSpeed(stock.gauge).minecraft());
                    float volume = (float) Math.max(1-speed/maxSpeed, 0.3) * Math.abs(stock.getThrottle() * stock.getReverser());
                    volume = (float) Math.sqrt(volume);
                    double fraction = 3;
                    float pitch = 0.8f + (float) (speed/maxSpeed/fraction * 0.2);
                    float delta = pitchOffset - (pitchStroke ? -0.02f : 0);
                    ISound chuff = chuffs.get(chuffId);
                    chuff.setPitch(pitch + delta);
                    chuff.setVolume(volume + delta);
                    chuff.play(stock.getPosition());

                    chuffId = (chuffId + 1) % chuffs.size();
                }
            } else {
                if (!enteredStroke) {
                    // Reset for next stroke
                    chuffOn = false;
                }
            }
            for (ISound chuff : chuffs) {
                if (chuff.isPlaying()) {
                    chuff.setPosition(stock.getPosition());
                    chuff.setVelocity(stock.getVelocity());
                    chuff.update();
                }
            }
        }

        void free() {
            for (ISound chuff : chuffs) {
                chuff.terminate();
            }
        }
    }

    ExpireableMap<String, ChuffSound> chuffSounds = new ExpireableMap<String, ChuffSound>() {
        @Override
        public void onRemove(String key, ChuffSound value) {
            value.free();
        }
    };

    public void effects(EntityMoveableRollingStock stock, float throttle) {
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
            ChuffSound sound = chuffSounds.get(key);
            if (sound == null) {
                sound = new ChuffSound((LocomotiveSteam) stock);
                chuffSounds.put(key, sound);
            }
            sound.update(getStroke(stock, throttle, -45, false) > 0.5);
        }
    }
}
