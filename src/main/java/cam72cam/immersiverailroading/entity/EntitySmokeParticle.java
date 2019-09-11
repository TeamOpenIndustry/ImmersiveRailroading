package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.util.GLBoolTracker;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.GLSLShader;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

public class EntitySmokeParticle extends Particle {

	private static GLSLShader shader;
	private static int dl;

	private final double rot;
	private final float darken;
	private final float thickness;
	private final double diameter;


	public EntitySmokeParticle(World worldIn, Vec3d pos, Vec3d vel, int lifespan, float darken, float thickness, double diameter) {
		super(worldIn, pos.x, pos.y, pos.z, vel.x, vel.y, vel.z);
		this.rot = Math.random() * 360;
		this.particleMaxAge = lifespan;
		this.darken = darken;
		this.thickness = thickness;
		this.diameter = diameter;
		this.motionX = vel.x;
		this.motionY = vel.y;
		this.motionZ = vel.z;
	}

	@Override
	public boolean shouldDisableDepth() {
		return true;
	}

	@Override
	public void renderParticle(BufferBuilder buffer, Entity entityIn, float partialTicks, float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
		if (shader == null) {
			shader =new GLSLShader("immersiverailroading:particles/smoke_vert.c", "immersiverailroading:particles/smoke_frag.c");
			dl = GL11.glGenLists(1);
			GL11.glNewList(dl, GL11.GL_COMPILE);
			{
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
			}
			GL11.glEndList();
		}

		double life = particleAge  / (float)this.particleMaxAge;

		double expansionRate = 16;

		double radius = this.diameter * (Math.sqrt(life) * expansionRate + 1) * 0.5;

		float alpha = (thickness + 0.2f) * (1 - (float) Math.sqrt(life));

		shader.bind();
		GLBoolTracker light = new GLBoolTracker(GL11.GL_LIGHTING, false);
		GLBoolTracker cull = new GLBoolTracker(GL11.GL_CULL_FACE, false);
		GLBoolTracker tex = new GLBoolTracker(GL11.GL_TEXTURE_2D, false);
		GLBoolTracker blend = new GLBoolTracker(GL11.GL_BLEND, true);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glPushMatrix();
		{
			float darken = 0.9f-this.darken*0.9f;

			shader.paramFloat("ALPHA", alpha);
			shader.paramFloat("DARKEN", darken, darken, darken);

			// Move to specified position
			GL11.glTranslated(posX-interpPosX, posY-interpPosY, posZ-interpPosZ);
			GL11.glTranslated(this.motionX * partialTicks, this.motionY * partialTicks, this.motionZ * partialTicks);

			// Rotate to look at internal
			Vec3d offsetForRot = MinecraftClient.getPlayer().getPositionEyes(partialTicks).subtract(new Vec3d(posX, posY, posZ));
			GL11.glRotated(180 - VecUtil.toWrongYaw(offsetForRot), 0, 1, 0);
			GL11.glRotated(180 - VecUtil.toPitch(offsetForRot)+90, 1, 0, 0);

			// Apply size
			GL11.glScaled(radius, radius, radius);

			// Noise Factor
			GL11.glRotated(this.rot, 0, 0, 1);
			GL11.glTranslated(0.5, 0, 0);
			GL11.glRotated(-this.rot, 0, 0, 1);

			// Spin
			double angle = this.particleAge + partialTicks;// + 45;
			GL11.glRotated(angle, 0, 0, 1);

			//Draw
			GL11.glCallList(dl);
		}
		blend.restore();
		tex.restore();
		cull.restore();
		light.restore();
		shader.unbind();

		GL11.glPopMatrix();
	}
}
