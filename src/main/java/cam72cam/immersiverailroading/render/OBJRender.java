package cam72cam.immersiverailroading.render;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.model.obj.Material;
import cam72cam.immersiverailroading.model.obj.OBJModel;
import cam72cam.immersiverailroading.model.obj.Vec2f;
import cam72cam.immersiverailroading.proxy.ClientProxy;
import net.minecraft.util.math.Vec3d;
import util.Matrix4;

public class OBJRender {

	public OBJModel model;
	public Map<String, OBJTextureSheet> textures = new HashMap<String, OBJTextureSheet>();
	private int prevTexture = -1;

	public OBJRender(OBJModel model) {
		this(model, null);
	}
	
	public OBJRender(OBJModel model, Collection<String> textureNames) {
		this.model = model;
		if (textureNames != null && textureNames.size() > 1) {
			for (String name : textureNames) {
				this.textures.put(name, new OBJTextureSheet(model, name));
			}
		} else {
			this.textures.put(null, new OBJTextureSheet(model));
		}
	}

	public boolean hasTexture() {
		return true;
	}
	
	public void bindTexture() {
		bindTexture(null);
	}
	
	public void bindTexture(String texName) {
		if (hasTexture()) {
			int currentTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
			if (this.textures.get(texName) == null) {
				texName = null; // Default
			}
			if (currentTexture != this.textures.get(texName).textureID) {
				prevTexture  = currentTexture;
				GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.textures.get(texName).textureID);
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
		drawDirectGroups(groupNames, scale, new Matrix4());
	}
	public void drawDirectGroups(Iterable<String> groupNames, double scale, Matrix4 m) {
		List<Integer> quads = new ArrayList<Integer>();
		boolean has_vn = true;

		for (String group : groupNames) {
			if (group.contains("EXHAUST_") || group.contains("CHIMNEY_") || group.contains("PRESSURE_VALVE_") || group.contains("CHIMINEY_")) {
				//Skip particle emitters
				continue;
			}
			for (int face : model.groups.get(group)) {
				quads.add(face);
			}
		}

		FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(quads.size() * 3 * 3);
		FloatBuffer normalBuffer = BufferUtils.createFloatBuffer(quads.size() * 3 * 3);
		FloatBuffer colorBuffer = BufferUtils.createFloatBuffer(quads.size() * 3 * 4);
		FloatBuffer texBuffer = BufferUtils.createFloatBuffer(quads.size() * 3 * 2);

		for (int face : quads) {
			String mtlName = model.faceMTLs[face];
			Material currentMTL = model.materials.get(mtlName);
			float r = 0;
			float g = 0;
			float b = 0;
			float a = 1;
			
			OBJTextureSheet texture = textures.get(null);
			
			if (currentMTL != null) {
				if (currentMTL.Kd != null) {
					float mult = 1 - model.darken * 5;
					
					if (texture.isFlatMaterial(mtlName)) {
						r = 1;
						g = 1;
						b = 1;
					} else {
						r = currentMTL.Kd.get(0);
						g = currentMTL.Kd.get(1);
						b = currentMTL.Kd.get(2);
					}
					
					r = Math.max(0, r * mult);
					g = Math.max(0, g * mult);
					b = Math.max(0, b * mult);
					a = currentMTL.Kd.get(3);
				}
			} else {
				ImmersiveRailroading.warn("Missing group %s", mtlName);
			}
			
			for (int[] point : model.points(face)) {
				Vec3d v = model.vertices(point[0]);
				Vec2f vt = point[1] != -1 ? model.vertexTextures(point[1]) : null;
				Vec3d vn = point[2] != -1 ? model.vertexNormals(point[2]) : null;

				v = m.apply(v);
				
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
					texBuffer.put(texture.convertU(mtlName, vt.x - model.offsetU[face]));
					texBuffer.put(texture.convertV(mtlName, -(vt.y) - model.offsetV[face]));
				} else {
					texBuffer.put(texture.convertU(mtlName, 0));
					texBuffer.put(texture.convertV(mtlName, 0));
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
			for (OBJTextureSheet texture : textures.values()) {
				texture.freeGL();
			}
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
