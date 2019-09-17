package cam72cam.mod.render;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.world.World;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import org.apache.logging.log4j.util.TriConsumer;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class IParticle {
    protected Vec3d pos;
    protected long ticks;
    boolean canRender = true;

    Vec3d renderPos;

    protected abstract boolean depthTestEnabled();
    protected abstract void render(float partialTicks);

    public static class ParticleData {
        public final World world;
        public final Vec3d pos;
        public final Vec3d motion;
        public final int lifespan;

        public ParticleData(World world, Vec3d pos, Vec3d motion, int lifespan) {
            this.world = world;
            this.pos = pos;
            this.motion = motion;
            this.lifespan = lifespan;
        }
    }

    public static <P extends ParticleData> Consumer<P> register(Function<P, IParticle> ctr) {
        return register(ctr, null);
    }

    public static <P extends ParticleData, I extends IParticle> Consumer<P> register(Function<P, I> ctr, TriConsumer<List<I>, Consumer<I>,  Float> renderer) {
        List<I> particles = new ArrayList<>();

        return data -> {
            I ip = ctr.apply(data);
            Particle p = new Particle(data.world.internal, data.pos.x, data.pos.y, data.pos.z, data.motion.x, data.motion.y, data.motion.z) {
                {
                    particleMaxAge = data.lifespan;
                    motionX = data.motion.x;
                    motionY = data.motion.y;
                    motionZ = data.motion.z;
                }

                @Override
                public boolean shouldDisableDepth() {
                    return !ip.depthTestEnabled();
                }

                @Override
                public void renderParticle(BufferBuilder buffer, Entity entityIn, float partialTicks, float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
                    ip.ticks = particleAge;
                    ip.pos = new Vec3d(posX, posY, posZ);
                    ip.renderPos = new Vec3d(posX - interpPosX, posY - interpPosY, posZ - interpPosZ);
                    ip.renderPos = ip.renderPos.add(this.motionX * partialTicks, this.motionY * partialTicks, this.motionZ * partialTicks);

                    if (renderer == null) {
                        GL11.glPushMatrix();
                        {
                            GL11.glTranslated(ip.renderPos.x, ip.renderPos.y, ip.renderPos.z);
                            ip.render(partialTicks);
                        }
                        GL11.glPopMatrix();
                    } else {
                        if (!ip.canRender) {
                            renderer.accept(particles, subp -> GL11.glTranslated(subp.renderPos.x, subp.renderPos.y, subp.renderPos.z), partialTicks);
                            particles.forEach(p -> p.canRender = true);
                            particles.clear();
                        } else {
                            particles.add(ip);
                            ip.canRender = false;
                        }
                    }
                }
            };

            Minecraft.getMinecraft().effectRenderer.addEffect(p);
        };
    }
}
