package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.Particles;
import cam72cam.immersiverailroading.model.ModelState;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.registry.Quilling;
import cam72cam.immersiverailroading.registry.parts.SoundDefinition;
import cam72cam.immersiverailroading.render.ExpireableMap;
import cam72cam.immersiverailroading.render.SmokeParticle;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.sound.ISound;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Whistle {
    private final ModelComponent component;
    private final Quilling quilling;
    private final PartSound whistle;

    public static Whistle get(ComponentProvider provider, ModelState state, Quilling quilling, SoundDefinition fallback) {
        return new Whistle(provider.parse(ModelComponentType.WHISTLE), state, quilling, fallback);
    }

    public Whistle(ModelComponent whistle, ModelState state, Quilling quilling, SoundDefinition fallback) {
        this.component = whistle;
        this.quilling = quilling;
        this.whistle = new PartSound(fallback, true, 150, ConfigSound.SoundCategories.Locomotive.Steam::whistle);
        state.include(whistle);
    }

    private class SoundEffects {
        private final List<ISound> chimes;
        private float pullString = 0;
        private float soundDampener = 0;

        private SoundEffects(EntityMoveableRollingStock stock) {
            chimes = new ArrayList<>();
            for (Quilling.Chime chime : quilling.chimes) {
                chimes.add(stock.createSound(chime.sample, true, 150, ConfigSound.SoundCategories.Locomotive.Steam::whistle));
            }
        }

        public void update(EntityMoveableRollingStock stock, int hornTime, float hornPull) {
            if (hornTime < 1) {
                pullString = 0;
                soundDampener = 0;
                for (ISound chime : chimes) {
                    if (chime.isPlaying()) {
                        chime.stop();
                    }
                }
            } else {
                float maxDelta = 1 / 20f;
                float delta = 0;
                if (hornTime > 5) {
                    if (soundDampener < 0.4) {
                        soundDampener = 0.4f;
                    }
                    if (soundDampener < 1) {
                        soundDampener += 0.1;
                    }
                    delta = hornPull - pullString;
                } else {
                    if (soundDampener > 0) {
                        soundDampener -= 0.07;
                    }
                    // Player probably released key or has net lag
                    delta = -pullString;
                }

                if (pullString == 0) {
                    pullString += delta * 0.55;
                } else {
                    pullString += Math.max(Math.min(delta, maxDelta), -maxDelta);
                }
                pullString = Math.min(pullString, (float) quilling.maxPull);

                for (int i = 0; i < quilling.chimes.size(); i++) {
                    ISound sound = chimes.get(i);
                    Quilling.Chime chime = quilling.chimes.get(i);

                    double perc = pullString;
                    // Clamp to start/end
                    perc = Math.min(perc, chime.pull_end);
                    perc -= chime.pull_start;

                    //Scale to clamped range
                    perc /= chime.pull_end - chime.pull_start;

                    if (perc > 0) {
                        double pitch = (chime.pitch_end - chime.pitch_start) * perc + chime.pitch_start;

                        sound.setPitch((float) pitch);
                        sound.setVolume((float) (perc * soundDampener));
                        sound.setPosition(stock.getPosition());
                        sound.setVelocity(stock.getVelocity());

                        if (!sound.isPlaying()) {
                            sound.play(stock.getPosition());
                        }
                    } else {
                        if (sound.isPlaying()) {
                            sound.stop();
                        }
                    }
                }
            }
        }

        public void terminate() {
            if (chimes != null) {
                for (ISound chime : chimes) {
                    chime.stop();
                }
            }
        }
    }

    private final ExpireableMap<UUID, SoundEffects> sounds = new ExpireableMap<UUID, SoundEffects>() {
        @Override
        public void onRemove(UUID key, SoundEffects value) {
            value.terminate();
        }
    };

    public void effects(EntityMoveableRollingStock stock, int hornTime, float hornPull) {
        // Particles and Sound

        if (quilling != null) {
            SoundEffects sound = sounds.get(stock.getUUID());

            if (sound == null) {
                sound = new SoundEffects(stock);
                sounds.put(stock.getUUID(), sound);
            }

            sound.update(stock, hornTime, hornPull);
        } else {
            whistle.effects(stock, hornTime > 0);
        }

        Vec3d fakeMotion = stock.getVelocity();
        if (component != null && hornTime > 0) {
            Vec3d particlePos = stock.getPosition().add(VecUtil.rotateWrongYaw(component.center.scale(stock.gauge.scale()), stock.getRotationYaw() + 180));
            particlePos = particlePos.subtract(fakeMotion);

            float darken = 0;
            float thickness = 1;
            double smokeMod = Math.min(1, Math.max(0.2, Math.abs(stock.getCurrentSpeed().minecraft()) * 2));
            int lifespan = (int) (40 * (1 + smokeMod * stock.gauge.scale()));
            double verticalSpeed = 0.8f * stock.gauge.scale();
            double size = 0.3 * (0.8 + smokeMod) * stock.gauge.scale();

            Particles.SMOKE.accept(new SmokeParticle.SmokeParticleData(stock.getWorld(), particlePos, new Vec3d(fakeMotion.x, fakeMotion.y + verticalSpeed, fakeMotion.z), lifespan, darken, thickness, size, stock.getDefinition().steamParticleTexture));
        }
    }

    public void removed(EntityMoveableRollingStock stock) {
        SoundEffects sound = sounds.get(stock.getUUID());
        if (sound != null) {
            sound.terminate();
        }
        whistle.removed(stock);
    }
}
