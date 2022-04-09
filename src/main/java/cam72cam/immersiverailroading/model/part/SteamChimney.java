package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ConfigGraphics;
import cam72cam.immersiverailroading.entity.LocomotiveSteam;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.Particles;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.render.SmokeParticle;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.resource.Identifier;

import java.util.List;

public class SteamChimney {
    private final List<ModelComponent> emitter;

    public static SteamChimney get(ComponentProvider provider) {
        List<ModelComponent> chimneyEmitters = provider.parseAll(ModelComponentType.PARTICLE_CHIMNEY_X);
        return chimneyEmitters.isEmpty() ? null : new SteamChimney(chimneyEmitters);
    }

    public SteamChimney(List<ModelComponent> emitter) {
        this.emitter = emitter;
    }

    private boolean isSmokeParticle = false;
    public void effects(LocomotiveSteam stock, boolean isEndStroke) {
        Vec3d fakeMotion = stock.getVelocity();
        float exhaust = stock.getThrottle() * Math.abs(stock.getReverser());
        if (emitter != null && ConfigGraphics.particlesEnabled) {
            float darken = 0;
            float thickness = exhaust/2;
            for (int i : stock.getBurnTime().values()) {
                darken += i >= 1 ? 1 : 0;
            }
            if (darken == 0 && Config.isFuelRequired(stock.gauge)) {
                return;
            }
            darken /= stock.getInventorySize() - 2.0;
            darken *= 0.75;
            for (ModelComponent smoke : emitter) {
                Vec3d particlePos = stock.getPosition().add(VecUtil.rotateWrongYaw(smoke.center.scale(stock.gauge.scale()), stock.getRotationYaw() + 180));
                particlePos = particlePos.subtract(fakeMotion);

                double smokeMod = Math.min(1, Math.max(0.2, Math.abs(stock.getCurrentSpeed().minecraft())*2));

                int lifespan = (int) (200 * (1 + exhaust) * smokeMod * stock.gauge.scale());

                double verticalSpeed = 0.5 * stock.gauge.scale();
                double size = smoke.width() * stock.gauge.scale() * (0.8 + smokeMod);

                if (isEndStroke) {
                    double phaseSpike = 1.75;
                    size *= phaseSpike;
                    verticalSpeed *= phaseSpike;
                }
                particlePos = particlePos.subtract(fakeMotion);
                isSmokeParticle = !isSmokeParticle;
                Identifier particleTex = isSmokeParticle ? stock.getDefinition().smokeParticleTexture : stock.getDefinition().steamParticleTexture;
                Particles.SMOKE.accept(new SmokeParticle.SmokeParticleData(stock.getWorld(), particlePos, new Vec3d(fakeMotion.x, fakeMotion.y + verticalSpeed, fakeMotion.z), lifespan , isSmokeParticle ? darken : 0, thickness, size, particleTex));
            }
        }
    }
}
