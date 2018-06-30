package cam72cam.immersiverailroading.render;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.model.obj.Face;
import cam72cam.immersiverailroading.model.obj.Material;
import cam72cam.immersiverailroading.model.obj.OBJModel;
import cam72cam.immersiverailroading.model.obj.Vec2f;
import cam72cam.immersiverailroading.proxy.ClientProxy;
import net.minecraft.util.math.Vec3d;

public class OBJRender {

	public OBJModel model;
	public OBJTextureSheet texture;
	private int prevTexture = -1;

	public OBJRender(OBJModel model) {
		this.model = model;
		this.texture = new OBJTextureSheet(model);
	}

	public boolean hasTexture() {
		return texture.mappings.size() != 0;
	}
	public void bindTexture() {
		if (hasTexture()) {
			int currentTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
			if (currentTexture != this.texture.textureID) {
				prevTexture  = currentTexture;
				GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.texture.textureID);
			}
		}
	}
	public void restoreTexture() {
		if (hasTexture()) {
			if (prevTexture != -1) {
				GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTexture);
				prevTexture = -1;
			}
		}
	}

	private Integer displayList = null;

	public void draw() {
		if (displayList == null) {
			if (!ClientProxy.renderCacheLimiter.canRender()) {
				return;
			}
			
			displayList = ClientProxy.renderCacheLimiter.newList(() -> drawDirect());
		}
		GL11.glCallList(displayList);
	}

	public void drawDirect() {
		drawDirectGroups(model.groups.keySet());
	}
	public void drawDirect(double scale) {
		drawDirectGroups(model.groups.keySet(), scale);
	}

	public Map<Double, Map<Iterable<String>, Integer>> displayLists = new HashMap<Double, Map<Iterable<String>, Integer>>();

	public void drawGroups(Iterable<String> groupNames, double scale) {
		if (!displayLists.containsKey(scale)) {
			displayLists.put(scale, new HashMap<Iterable<String>, Integer>());
		}
		
		if (!displayLists.get(scale).containsKey(groupNames)) {
			if (!ClientProxy.renderCacheLimiter.canRender()) {
				return;
			}
			
			int groupsDisplayList = GL11.glGenLists(1);
			groupsDisplayList = ClientProxy.renderCacheLimiter.newList(() -> drawDirectGroups(groupNames, scale));
			displayLists.get(scale).put(groupNames, groupsDisplayList);
		}
		GL11.glCallList(displayLists.get(scale).get(groupNames));
	}
	public void drawGroups(Iterable<String> groupNames) {
		drawGroups(groupNames, 1);
	}

	public void drawDirectGroups(Iterable<String> groupNames) {
		drawDirectGroups(groupNames, 1.0);
	}

	public void drawDirectGroups(Iterable<String> groupNames, double scale) {
		List<Face> quads = new ArrayList<Face>();
		boolean has_vn = true;

		for (String group : groupNames) {
			if (group.contains("EXHAUST_") || group.contains("CHIMNEY_") || group.contains("PRESSURE_VALVE_") || group.contains("CHIMINEY_")) {
				//Skip particle emitters
				continue;
			}
			quads.addAll(model.groups.get(group));
		}

		FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(quads.size() * 3 * 3);
		FloatBuffer normalBuffer = BufferUtils.createFloatBuffer(quads.size() * 3 * 3);
		FloatBuffer colorBuffer = BufferUtils.createFloatBuffer(quads.size() * 3 * 4);
		FloatBuffer texBuffer = BufferUtils.createFloatBuffer(quads.size() * 3 * 2);

		for (Face face : quads) {
			Material currentMTL = model.materials.get(face.mtl);
			float r = 0;
			float g = 0;
			float b = 0;
			float a = 0;
			if (currentMTL.Kd != null) {
				float mult = 1 - model.darken * 5;
				r = Math.max(0, currentMTL.Kd.get(0) * mult);
				g = Math.max(0, currentMTL.Kd.get(1) * mult);
				b = Math.max(0, currentMTL.Kd.get(2) * mult);
				a = currentMTL.Kd.get(3);
			}
			for (int[] point : face.points()) {
				Vec3d v = model.vertices(point[0]);
				Vec2f vt = point[1] != -1 ? model.vertexTextures(point[1]) : null;
				Vec3d vn = point[2] != -1 ? model.vertexNormals(point[2]) : null;
				
				vertexBuffer.put((float) (v.x * scale));
				vertexBuffer.put((float) (v.y * scale));
				vertexBuffer.put((float) (v.z * scale));
				if (vn != null) {
					normalBuffer.put((float) (vn.x));
					normalBuffer.put((float) (vn.y));
					normalBuffer.put((float) (vn.z));
				} else {
					has_vn = false;
				}
				if (vt != null) {
					texBuffer.put(texture.convertU(face.mtl, vt.x - face.offsetUV.x));
					texBuffer.put(texture.convertV(face.mtl, -(vt.y) - face.offsetUV.y));
				} else {
					texBuffer.put(texture.convertU(face.mtl, 0));
					texBuffer.put(texture.convertV(face.mtl, 0));
				}
				colorBuffer.put(r);
				colorBuffer.put(g);
				colorBuffer.put(b);
				colorBuffer.put(a);
			}
		}

		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
		GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
		GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
		if (has_vn) {
			GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
		}

		vertexBuffer.flip();
		colorBuffer.flip();
		normalBuffer.flip();
		texBuffer.flip();
		GL11.glTexCoordPointer(2, 2 << 2, texBuffer);
		GL11.glColorPointer(4, 4 << 2, colorBuffer);
		if (has_vn) {
			GL11.glNormalPointer(3 << 2, normalBuffer);
		}
		GL11.glVertexPointer(3, 3 << 2, vertexBuffer);
		GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, quads.size() * 3);

		GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
		GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
		GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
		if (has_vn) {
			GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
		}
		
		// Reset draw color (IMPORTANT)
		GL11.glColor4f(1, 1, 1, 1);
	}

	public void freeGL() {
		if (this.hasTexture()) {
			this.texture.freeGL();
		}
		if (this.displayList != null) {
			GL11.glDeleteLists(this.displayList, 1);
		}
		for (Map<Iterable<String>, Integer> list : this.displayLists.values()) {
			for (Integer dl : list.values()) {
				GL11.glDeleteLists(dl, 1);
			}
		}
	}
}
