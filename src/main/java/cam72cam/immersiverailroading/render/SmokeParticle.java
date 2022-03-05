package cam72cam.immersiverailroading.render;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.GLSLShader;
import cam72cam.mod.render.Particle;
import cam72cam.mod.render.OpenGL;
import cam72cam.mod.render.opengl.BlendMode;
import cam72cam.mod.render.opengl.LegacyRenderContext;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.Texture;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.world.World;
import org.lwjgl.opengl.GL11;

import java.util.List;

public class SmokeParticle extends Particle {

	public static class SmokeParticleData extends ParticleData {
		private final float darken;
		private final float thickness;
		private final double diameter;

		public SmokeParticleData(World world, Vec3d pos, Vec3d motion, int lifespan, float darken, float thickness, double diameter) {
			super(world, pos, motion, lifespan);
			this.darken = darken;
			this.thickness = thickness;
			this.diameter = diameter;
		}
	}


	private static GLSLShader shader;
	private static int dl;


	private final double rot;
	private final SmokeParticleData data;

	public SmokeParticle(SmokeParticleData data) {
		this.data = data;
		this.rot = Math.random() * 360;
	}

	@Override
	public boolean depthTestEnabled() {
		return false;
	}

	@Override
	public void render(RenderState ctx, float partialTicks) {
	}

	public static void renderAll(List<SmokeParticle> particles, RenderState state, float partialTicks) {
		if (shader == null) {
			shader = new GLSLShader(
					new Identifier(ImmersiveRailroading.MODID, "particles/smoke_vert.c"),
					new Identifier(ImmersiveRailroading.MODID, "particles/smoke_frag.c")
			);
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

		state.lighting(false)
				.cull_face(false)
				.texture(Texture.NO_TEXTURE)
				.blend(new BlendMode(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA));
		try (
				OpenGL.With sb = shader.bind();
				OpenGL.With ctx = LegacyRenderContext.INSTANCE.apply(state)
		) {
			for (SmokeParticle particle : particles) {

				double life = particle.ticks / (float) particle.data.lifespan;

				double expansionRate = 16;

				double radius = particle.data.diameter * (Math.sqrt(life) * expansionRate + 1) * 0.5;

				float alpha = (particle.data.thickness + 0.2f) * (1 - (float) Math.sqrt(life));
				RenderState matrix = new RenderState();
				float darken = 0.9f - particle.data.darken * 0.9f;

				shader.paramFloat("ALPHA", alpha);
				shader.paramFloat("DARKEN", darken, darken, darken);

				//setPos.accept(particle);
				matrix.translate(particle.renderX, particle.renderY, particle.renderZ);

				// Rotate to look at internal
				particle.lookAtPlayer(matrix);

				// Apply size
				matrix.scale(radius, radius, radius);

				// Noise Factor
				matrix.rotate(particle.rot, 0, 0, 1);
				matrix.translate(0.5, 0, 0);
				matrix.rotate(-particle.rot, 0, 0, 1);

				// Spin
				double angle = particle.ticks + partialTicks;// + 45;
				matrix.rotate(angle, 0, 0, 1);
				//Draw
				try (OpenGL.With lc = LegacyRenderContext.INSTANCE.apply(matrix)) {
					//GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
					GL11.glCallList(dl);
				}
			}
		}
	}
}
