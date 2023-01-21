package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.ConfigGraphics;
import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.entity.LocomotiveSteam;
import cam72cam.immersiverailroading.library.ModelComponentType.ModelPosition;
import cam72cam.immersiverailroading.library.Particles;
import cam72cam.immersiverailroading.library.ValveGearConfig;
import cam72cam.immersiverailroading.model.ModelState;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.render.ExpireableMap;
import cam72cam.immersiverailroading.render.SmokeParticle;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.sound.ISound;
import util.Matrix4;

import java.util.ArrayList;
import java.util.List;

public abstract class ValveGear {

    protected final WheelSet wheels;
    private final ModelState state;
    protected float angleOffset;
    protected Exhaust frontExhaust;
    protected Exhaust rearExhaust;

    protected ValveGear(WheelSet wheels, ModelState state, float angleOffset) {
        this.wheels = wheels;
        this.state = state;
        this.angleOffset = angleOffset;
        this.frontExhaust = null;
        this.rearExhaust = null;
    }

    static ValveGear get(WheelSet wheels, ValveGearConfig type, ComponentProvider provider, ModelState state, ModelPosition pos, float angleOffset) {
        if (type == null) {
            return null;
        }
        switch (type.type) {
            case WALSCHAERTS:
                return WalschaertsValveGear.get(wheels, provider, state, pos, angleOffset);
            case STEPHENSON:
                return StephensonValveGear.get(wheels, provider, state, pos, angleOffset);
            case CONNECTING:
                return ConnectingRodValveGear.get(wheels, provider, state, pos, angleOffset);
            case CUSTOM:
                return CustomValveGear.get(type.custom, wheels, provider, state, pos);
            case SHAY:
            case CLIMAX:
            case HIDDEN:
            default:
                return null;
        }
    }

    void effects(EntityMoveableRollingStock stock) {
        if (frontExhaust != null) {
            frontExhaust.effects(stock);
        }
        if (rearExhaust != null) {
            rearExhaust.effects(stock);
        }
    }

    public boolean isEndStroke(EntityMoveableRollingStock stock) {
        return (frontExhaust != null && frontExhaust.isEndStroke(stock)) ||
                (rearExhaust != null && rearExhaust.isEndStroke(stock));
    }

    float angle(double distance) {
        return wheels.angle(distance) + angleOffset;
    }

    public void removed(EntityMoveableRollingStock stock) {
        if (frontExhaust != null) {
            frontExhaust.removed(stock);
        }
        if (rearExhaust != null) {
            rearExhaust.removed(stock);
        }
    }

    private enum Direction {
        FRONT(new Vec3d(-1, 0, 0)),
        BACK(new Vec3d(1, 0, 0)),
        UP(new Vec3d(0, 1, 0)),
        DOWN(new Vec3d(0, -1, 0)),
        LEFT(new Vec3d(0, 0, 1)),
        RIGHT(new Vec3d(0, 0, -1));

        private final Vec3d vec;

        Direction(Vec3d vec) {
            this.vec = vec;
        }
    }

    private static Vec3d findDirection(String name) {
        Vec3d result = Vec3d.ZERO;
        for (Direction value : Direction.values()) {
            if (name.contains("__" + value.name())) {
                result = result.add(value.vec);
            }
        }
        return result;
    }

    public class Exhaust {
        public final Vec3d position;
        public final Vec3d direction;
        public final float angle;

        public Exhaust(Vec3d position, ModelPosition direction, float angle) {
            this(position,
                    new Vec3d(0, 0, direction.contains(ModelPosition.RIGHT) ? -1 :
                            direction.contains(ModelPosition.CENTER) ? 0 : 1),
                    angle);
        }

        public Exhaust(Vec3d position, Vec3d direction, float angle) {
            this.position = position;
            this.direction = direction;
            this.angle = angle;
        }


        public Exhaust(ModelComponent component, float angle) {
            this(component.center, findDirection(component.key), angle);
        }

        public void effects(EntityMoveableRollingStock stock) {
            boolean drains_enabled = isEndStroke(stock) && stock instanceof LocomotiveSteam && ((LocomotiveSteam) stock).cylinderDrainsEnabled();
            if (ConfigGraphics.particlesEnabled && drains_enabled) {
                Matrix4 m = state.getMatrix(stock);
                if (m == null) {
                    // Just in case...
                    m = new Matrix4();
                }
                Vec3d particlePos = stock.getPosition().add(VecUtil.rotateWrongYaw(m.apply(position).scale(stock.gauge.scale()), stock.getRotationYaw() + 180));
                double accell = 0.3 * stock.gauge.scale();
                Vec3d sideMotion = stock.getVelocity().add(VecUtil.rotateWrongYaw(m.apply(direction).scale(accell), stock.getRotationYaw()+180));
                Particles.SMOKE.accept(new SmokeParticle.SmokeParticleData(stock.getWorld(), particlePos, new Vec3d(sideMotion.x, sideMotion.y+0.01 * stock.gauge.scale(), sideMotion.z), 80, 0, 0.6f, 0.2 * stock.gauge.scale(), stock.getDefinition().steamParticleTexture));
            }

            if (ConfigSound.soundEnabled && stock instanceof LocomotiveSteam) {
                String key = String.format("%s-%s", stock.getUUID(), this.hashCode());
                ChuffSound sound = chuffSounds.get(key);
                if (sound == null) {
                    sound = new ChuffSound((LocomotiveSteam) stock);
                    chuffSounds.put(key, sound);
                }
                sound.update(isEndStroke(stock, 0.125f), drains_enabled);
            }
        }

        public void removed(EntityMoveableRollingStock stock) {
            String key = String.format("%s-%s", stock.getUUID(), this.hashCode());
            chuffSounds.remove(key);
        }

        public boolean isEndStroke(EntityMoveableRollingStock stock) {
            float delta = 0.03f;
            if (stock instanceof LocomotiveSteam) {
                LocomotiveSteam loco = (LocomotiveSteam) stock;
                if (Math.abs(loco.getThrottle() * loco.getReverser()) == 0) {
                    return false;
                }

                delta = Math.abs(loco.getReverser())/4;
            }
            return isEndStroke(stock, delta);
        }
        public boolean isEndStroke(EntityMoveableRollingStock stock, float delta) {
            double percent = angle(stock.distanceTraveled / stock.gauge.scale()) / 360;
            double pistonPos = this.angle / 360;

            // There's probably a much better way of doing this...
            return Math.abs(percent - pistonPos) < delta ||
                    Math.abs(percent - pistonPos - 1) < delta ||
                    Math.abs(percent - pistonPos + 1) < delta;
        }
    }

    private static class ChuffSound {
        private final LocomotiveSteam stock;
        private final float pitchOffset;
        private boolean pitchStroke;
        private boolean chuffOn;
        private final List<ISound> chuffs;
        private final ISound cylinder_drain;
        private int chuffId;

        public ChuffSound(LocomotiveSteam stock) {
            chuffOn = false;
            chuffId = 0;
            chuffs = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                chuffs.add(stock.createSound(stock.getDefinition().chuff, false, 80));
            }
            cylinder_drain = stock.createSound(stock.getDefinition().cyliner_drain, true, 40);
            this.stock = stock;
            this.pitchOffset = (float) (Math.random() / 50);
            this.pitchStroke = false;
        }

        public void update(boolean enteredStroke, boolean drain_enabled) {
            if (drain_enabled && !cylinder_drain.isPlaying()) {
                cylinder_drain.play(stock.getPosition());
            }
            if (!drain_enabled && cylinder_drain.isPlaying()) {
                cylinder_drain.stop();
            }

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
                if (cylinder_drain.isPlaying()) {
                    cylinder_drain.setVolume(stock.getThrottle());
                    cylinder_drain.setPosition(stock.getPosition());
                    cylinder_drain.setVelocity(stock.getVelocity());
                    cylinder_drain.update();
                }
            }
        }

        void free() {
            for (ISound chuff : chuffs) {
                chuff.terminate();
            }
            cylinder_drain.terminate();
        }
    }

    private static ExpireableMap<String, ChuffSound> chuffSounds = new ExpireableMap<String, ChuffSound>() {
        @Override
        public void onRemove(String key, ChuffSound value) {
            value.free();
        }
    };
}
