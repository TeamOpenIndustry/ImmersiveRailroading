package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.ConfigGraphics;
import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.Particles;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.render.ExpireableMap;
import cam72cam.immersiverailroading.render.SmokeParticle;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.sound.ISound;

import java.util.List;
import java.util.UUID;

public class PressureValve {
    private final List<ModelComponent> valves;
    private final Identifier sndFile;

    public static PressureValve get(ComponentProvider provider, Identifier sndFile) {
        List<ModelComponent> valves = provider.parseAll(ModelComponentType.PRESSURE_VALVE_X);
        return new PressureValve(valves, sndFile); // allow empty for sound only
    }

    public PressureValve(List<ModelComponent> valves, Identifier sndFile) {
        this.valves = valves;
        this.sndFile = sndFile;
    }

    private final ExpireableMap<UUID, ISound> sounds = new ExpireableMap<UUID, ISound>() {
        @Override
        public void onRemove(UUID key, ISound value) {
            value.terminate();
        }
    };

    public void effects(EntityMoveableRollingStock stock, boolean isBlowingOff) {
        if (ConfigSound.soundEnabled && ConfigSound.soundPressureValve) {
            ISound sound = sounds.get(stock.getUUID());
            if (sound == null) {
                sound = stock.createSound(sndFile, true, 40);
                sound.setVolume(0.3f);
                sounds.put(stock.getUUID(), sound);
            }

            if (isBlowingOff) {
                if (!sound.isPlaying()) {
                    sound.play(stock.getPosition());
                }

                sound.setPosition(stock.getPosition());
                sound.setVelocity(stock.getVelocity());
                sound.update();
            } else {
                sound.stop();
            }
        }

        if (ConfigGraphics.particlesEnabled && isBlowingOff) {
            Vec3d fakeMotion = stock.getVelocity();
            for (ModelComponent valve : valves) {
                Vec3d particlePos = stock.getPosition().add(VecUtil.rotateWrongYaw(valve.center.scale(stock.gauge.scale()), stock.getRotationYaw() + 180));
                particlePos = particlePos.subtract(fakeMotion);
                Particles.SMOKE.accept(new SmokeParticle.SmokeParticleData(stock.getWorld(), particlePos, new Vec3d(fakeMotion.x, fakeMotion.y + 0.2 * stock.gauge.scale(), fakeMotion.z),40, 0, 0.2f, valve.width() * stock.gauge.scale(), stock.getDefinition().steamParticleTexture));
            }
        }
    }

    public void removed(EntityMoveableRollingStock stock) {
        ISound sound = sounds.get(stock.getUUID());
        if (sound != null) {
            sound.terminate();
        }
    }
}
