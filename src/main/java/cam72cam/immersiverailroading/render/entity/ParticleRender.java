package cam72cam.immersiverailroading.render.entity;

import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.entity.EntitySmokeParticle;
import cam72cam.immersiverailroading.render.GLSLShader;
import cam72cam.immersiverailroading.util.GLBoolTracker;
import cam72cam.immersiverailroading.util.VecUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

public class ParticleRender extends Render<EntitySmokeParticle> {

	private GLSLShader shader;

	public ParticleRender(RenderManager renderManager) {
		super(renderManager);
		
		shader = new GLSLShader("smoke_vert.c", "smoke_frag.c");
	}

	@Override
	public boolean shouldRender(EntitySmokeParticle entity, ICamera camera, double camX, double camY, double camZ) {
		return true;
	}

	@Override
	public void doRender(EntitySmokeParticle particle, double x, double y, double z, float entityYaw, float partialTicks) {
		Minecraft.getMinecraft().mcProfiler.startSection("ir_particles");
		GL11.glPushMatrix();
		{
			float darken = 0.9f-particle.darken*0.9f;
			float alpha = particle.alpha;//(particle.lifespan - particle.ticksExisted - partialTicks)/(float)particle.lifespan * (0.2f + particle.thickness * 2);
			double size = particle.radius; //(particle.ticksExisted + partialTicks) / 20.0 * particle.size * (particle.motionY*8);
			
			shader.bind();
			shader.paramFloat("ALPHA", alpha);
			shader.paramFloat("DARKEN", darken, darken, darken);

			GLBoolTracker light = new GLBoolTracker(GL11.GL_LIGHTING, false);
			GLBoolTracker cull = new GLBoolTracker(GL11.GL_CULL_FACE, false);
			GLBoolTracker tex = new GLBoolTracker(GL11.GL_TEXTURE_2D, false);
			GLBoolTracker blend = new GLBoolTracker(GL11.GL_BLEND, true);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

			// Move to specified position
			GL11.glTranslated(x, y, z);
			GL11.glTranslated(particle.motionX * partialTicks, particle.motionY * partialTicks, particle.motionZ * partialTicks);
			
			// Rotate to look at player
			Vec3d offsetForRot = Minecraft.getMinecraft().player.getPositionVector().addVector(0, 1.5, 0).subtract(particle.getPositionVector());
			GL11.glRotated(180 - VecUtil.toYaw(offsetForRot), 0, 1, 0);
			GL11.glRotated(180 - VecUtil.toPitch(offsetForRot)+90, 1, 0, 0);

			// Apply size 
			GL11.glScaled(size, size, size);
			
			// Noise Factor
			GL11.glRotated(particle.rot, 0, 0, 1);
			GL11.glTranslated(0.5, 0, 0);
			GL11.glRotated(-particle.rot, 0, 0, 1);

			// Spin
			double angle = particle.ticksExisted + partialTicks;// + 45;
			GL11.glRotated(angle, 0, 0, 1);

			//Draw
			GL11.glBegin(GL11.GL_QUADS);
			{
				GL11.glTexCoord2d(0, 0);
				GL11.glVertex3d(-1, -1, 0);
				GL11.glTexCoord2d(0, 1);
				GL11.glVertex3d(-1, 1, 0);
				GL11.glTexCoord2d(1, 1);
				GL11.glVertex3d(1, 1, 0);
				GL11.glTexCoord2d(1, 0);
				GL11.glVertex3d(1, -1, 0);
			}
			GL11.glEnd();

			blend.restore();
			tex.restore();
			cull.restore();
			light.restore();
			
			shader.unbind();
		}
		GL11.glPopMatrix();
		Minecraft.getMinecraft().mcProfiler.endSection();
	}

	@Override
	protected ResourceLocation getEntityTexture(EntitySmokeParticle entity) {
		return null;
	}
}
