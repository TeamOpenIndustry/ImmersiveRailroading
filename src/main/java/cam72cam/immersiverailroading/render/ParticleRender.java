package cam72cam.immersiverailroading.render;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Scanner;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBFragmentShader;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntitySmokeParticle;
import cam72cam.immersiverailroading.util.GLBoolTracker;
import cam72cam.immersiverailroading.util.VecUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

public class ParticleRender extends Render<EntitySmokeParticle> {

	private int textureID;
	private int dl;
	private int program;

	public ParticleRender(RenderManager renderManager) {
		super(renderManager);
		ResourceLocation imageLoc = new ResourceLocation("immersiverailroading:particles/smoke.png");

		InputStream input;
		BufferedImage image;
		try {
			input = ImmersiveRailroading.proxy.getResourceStream(imageLoc);
			image = TextureUtil.readBufferedImage(input);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		int size = image.getWidth();

		int[] pixels = new int[size * size];
		image.getRGB(0, 0, size, size, pixels, 0, size);

		ByteBuffer buffer = BufferUtils.createByteBuffer(size * size * 4);
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				int pixel = pixels[y * size + x];
				buffer.put((byte) ((pixel >> 16) & 0xFF));
				buffer.put((byte) ((pixel >> 8) & 0xFF));
				buffer.put((byte) ((pixel >> 0) & 0xFF));
				int alpha = (pixel >> 24) & 0xFF;
				// System.out.println(alpha);
				// alpha = alpha / 5;
				buffer.put((byte) alpha);
			}
		}
		buffer.flip();

		textureID = GL11.glGenTextures();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
		TextureUtil.allocateTexture(textureID, size, size);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, size, size, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

		/*
		 * dl = GL11.glGenLists(1);
		 * 
		 * GL11.glNewList(dl, GL11.GL_COMPILE); GL11.glRotated(45, 0, 0, 1);
		 * GL11.glBegin(GL11.GL_QUADS); GL11.glTexCoord2d(0, 0);
		 * GL11.glVertex3d(-1, -1, 0); GL11.glTexCoord2d(0, 1);
		 * GL11.glVertex3d(-1, 1, 0); GL11.glTexCoord2d(1, 1);
		 * GL11.glVertex3d(1, 1, 0); GL11.glColor4f(1, 1, 1, 0);
		 * GL11.glTexCoord2d(1, 0); GL11.glVertex3d(1, -1, 0); GL11.glEnd();
		 * 
		 * GL11.glEndList();
		 */

		int vertShader = ARBShaderObjects.glCreateShaderObjectARB(ARBVertexShader.GL_VERTEX_SHADER_ARB);
		int fragShader = ARBShaderObjects.glCreateShaderObjectARB(ARBFragmentShader.GL_FRAGMENT_SHADER_ARB);
		ARBShaderObjects.glShaderSourceARB(vertShader, readShader("smoke_vert.c"));
		ARBShaderObjects.glCompileShaderARB(vertShader);
        if (ARBShaderObjects.glGetObjectParameteriARB(vertShader, ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB) == GL11.GL_FALSE)
        throw new RuntimeException("Error creating shader: " + getLogInfo(vertShader));
		
		ARBShaderObjects.glShaderSourceARB(fragShader, readShader("smoke_frag.c"));
		ARBShaderObjects.glCompileShaderARB(fragShader);
        if (ARBShaderObjects.glGetObjectParameteriARB(fragShader, ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB) == GL11.GL_FALSE)
        throw new RuntimeException("Error creating shader: " + getLogInfo(fragShader));
		
		program = ARBShaderObjects.glCreateProgramObjectARB();
		ARBShaderObjects.glAttachObjectARB(program, vertShader);
		ARBShaderObjects.glAttachObjectARB(program, fragShader);
		ARBShaderObjects.glLinkProgramARB(program);
	    if (ARBShaderObjects.glGetObjectParameteriARB(program, ARBShaderObjects.GL_OBJECT_LINK_STATUS_ARB) == GL11.GL_FALSE) {
            System.err.println(getLogInfo(program));
        }
	    ARBShaderObjects.glValidateProgramARB(program);
        if (ARBShaderObjects.glGetObjectParameteriARB(program, ARBShaderObjects.GL_OBJECT_VALIDATE_STATUS_ARB) == GL11.GL_FALSE) {
            System.err.println(getLogInfo(program));
            return;
        }
	}
	private String readShader(String fname) {
		InputStream input;
		try {
			input = ImmersiveRailroading.proxy.getResourceStream(new ResourceLocation("immersiverailroading:particles/" + fname));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
		Scanner reader = new Scanner(input);
		String text = "";
		while(reader.hasNextLine()) {
			text = text + reader.nextLine();
		}
		reader.close();
		return text;
	}
	private static String getLogInfo(int obj) {
	    return ARBShaderObjects.glGetInfoLogARB(obj, ARBShaderObjects.glGetObjectParameteriARB(obj, ARBShaderObjects.GL_OBJECT_INFO_LOG_LENGTH_ARB));
	    }

	@Override
	public boolean shouldRender(EntitySmokeParticle entity, ICamera camera, double camX, double camY, double camZ) {
		return true;
	}

	@Override
	public void doRender(EntitySmokeParticle particle, double x, double y, double z, float entityYaw, float partialTicks) {
		GL11.glPushMatrix();
		{
			ARBShaderObjects.glUseProgramObjectARB(program);
			float darken = 0.9f;
			float alpha = (160 - particle.ticksExisted)/160f * 2;
			ARBShaderObjects.glUniform1fARB(ARBShaderObjects.glGetUniformLocationARB(program, "FOO"), alpha);
			ARBShaderObjects.glUniform3fARB(ARBShaderObjects.glGetUniformLocationARB(program, "DARKEN"), darken, darken, darken);

			GLBoolTracker light = new GLBoolTracker(GL11.GL_LIGHTING, false);
			GLBoolTracker cull = new GLBoolTracker(GL11.GL_CULL_FACE, false);
			GLBoolTracker tex = new GLBoolTracker(GL11.GL_TEXTURE_2D, true);
			GLBoolTracker blend = new GLBoolTracker(GL11.GL_BLEND, true);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

			GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);

			GL11.glColor4f(0.3f, 0.3f, 0.3f, 0.5f);

			// Move to specified position
			GlStateManager.translate(x, y, z);
			GlStateManager.translate(0, partialTicks * 0.2, 0);

			GL11.glRotated(180 - Minecraft.getMinecraft().player.rotationYaw, 0, 1, 0);

			int te = 4;
			// double rad = Math.sqrt(particle.ticksExisted/160.0);
			for (int i = 0; i < te; i++) {
				Vec3d offset = VecUtil.fromYaw(particle.ticksExisted / 40.0, i * 360.0f / te);
				GL11.glPushMatrix();
				{
					GL11.glTranslated(offset.x, 0, offset.z);
					double scale = (particle.ticksExisted) / 20.0;
					GL11.glScaled(scale, scale, scale);
					// GL11.glCallList(this.dl);
					
					GL11.glRotated(particle.rot, 0, 0, 1);
					GL11.glTranslated(0.5, 0, 0);
					GL11.glRotated(-particle.rot, 0, 0, 1);

					double angle = particle.ticksExisted;
					if (i == 1) {
						GL11.glRotated(angle, 0, 0, 1);	
					}
					if (i == 3) {
						GL11.glRotated(-angle, 0, 0, 1);	
					}
	
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
				GL11.glPopMatrix();
				// GL11.glTranslated(0, 0.02, 0);
			}

			blend.restore();
			tex.restore();
			cull.restore();
			light.restore();
			
			ARBShaderObjects.glUseProgramObjectARB(0);
		}
		GL11.glPopMatrix();
	}

	@Override
	protected ResourceLocation getEntityTexture(EntitySmokeParticle entity) {
		return null;
	}
}
