package cam72cam.mod.render;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.world.World;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import org.lwjgl.opengl.GL11;

import java.util.function.Consumer;
import java.util.function.Function;

public interface IParticle {
    boolean depthTestEnabled();
    void render(Vec3d pos, int ticks, float partialTicks);

    class ParticleData {
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

    static <P extends ParticleData> Consumer<P> register(Function<P, IParticle> ctr) {
        return data -> {
            IParticle ip = ctr.apply(data);
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
                    GL11.glPushMatrix();
                    {
                        GL11.glTranslated(posX - interpPosX, posY - interpPosY, posZ - interpPosZ);
                        GL11.glTranslated(this.motionX * partialTicks, this.motionY * partialTicks, this.motionZ * partialTicks);
                        ip.render(new Vec3d(posX, posY, posZ), particleAge, partialTicks);
                    }
                    GL11.glPopMatrix();
                }
            };

            Minecraft.getMinecraft().effectRenderer.addEffect(p);
        };
    }
}
