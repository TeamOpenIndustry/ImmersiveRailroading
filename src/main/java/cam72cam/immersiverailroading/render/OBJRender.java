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
import net.minecraft.util.math.Vec3d;

public class OBJRender {

	public OBJModel model;

	public OBJRender(OBJModel model) {
		this.model = model;
	}

	private Integer displayList = null;

	public void draw() {
		// TODO texture binding
		/*
		 * Idea: break model by texture groups render each in it's own display
		 * list iterate through map <material, display_list> might be more
		 * performant than baking the texture binding into the display list?
		 */
		if (displayList == null) {
			displayList = GL11.glGenLists(1);
			GL11.glNewList(displayList, GL11.GL_COMPILE);
			drawDirect();
			GL11.glEndList();
		}
		GL11.glCallList(displayList);
	}

	public void drawDirect() {
		drawDirectGroups(model.groups.keySet());
	}

	public Map<Iterable<String>, Integer> displayLists = new HashMap<Iterable<String>, Integer>();

	public void drawGroups(Iterable<String> groupNames) {
		if (!displayLists.containsKey(groupNames)) {
			int groupsDisplayList = GL11.glGenLists(1);
			GL11.glNewList(groupsDisplayList, GL11.GL_COMPILE);
			drawDirectGroups(groupNames);
			GL11.glEndList();
			displayLists.put(groupNames, groupsDisplayList);
		}
		GL11.glCallList(displayLists.get(groupNames));
	}

	public void drawDirectGroups(Iterable<String> groupNames) {
		drawDirectGroups(groupNames, 1.0);
	}

	public void drawDirectGroups(Iterable<String> groupNames, double scale) {
		List<Face> quads = new ArrayList<Face>();
		boolean has_vt = true;
		boolean has_vn = true;

		for (String group : groupNames) {
			quads.addAll(model.groups.get(group));
		}

		FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(quads.size() * 4 * 3);
		FloatBuffer normalBuffer = BufferUtils.createFloatBuffer(quads.size() * 4 * 3);
		FloatBuffer colorBuffer = BufferUtils.createFloatBuffer(quads.size() * 4 * 4);
		FloatBuffer texBuffer = BufferUtils.createFloatBuffer(quads.size() * 4 * 2);

		for (Face face : quads) {
			Material currentMTL = model.materials.get(face.mtl);
			float r = 0;
			float g = 0;
			float b = 0;
			float a = 0;
			if (currentMTL.Kd != null) {
				r = Math.max(0, currentMTL.Kd.get(0) - model.darken);
				g = Math.max(0, currentMTL.Kd.get(1) - model.darken);
				b = Math.max(0, currentMTL.Kd.get(2) - model.darken);
				a = currentMTL.Kd.get(3);
			}
			for (int[] point : face.points) {
				Vec3d v = model.vertices.get(point[0]);
				Vec2f vt = point[1] != -1 ? model.vertexTextures.get(point[1]) : null;
				Vec3d vn = point[2] != -1 ? model.vertexNormals.get(point[2]) : null;
				
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
					texBuffer.put(vt.x);
					texBuffer.put(-vt.y);
				} else {
					has_vt = false;
				}
				colorBuffer.put(r);
				colorBuffer.put(g);
				colorBuffer.put(b);
				colorBuffer.put(a);
			}
		}

		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
		if (has_vt) {
			GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
		}
		GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
		if (has_vn) {
			GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
		}

		vertexBuffer.flip();
		colorBuffer.flip();
		normalBuffer.flip();
		texBuffer.flip();
		if (has_vt) {
			GL11.glTexCoordPointer(2, 2 << 2, texBuffer);
		}
		GL11.glColorPointer(4, 4 << 2, colorBuffer);
		if (has_vn) {
			GL11.glNormalPointer(3 << 2, normalBuffer);
		}
		GL11.glVertexPointer(3, 3 << 2, vertexBuffer);
		GL11.glDrawArrays(GL11.GL_QUADS, 0, quads.size() * 4);

		GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
		if (has_vt) {
			GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
		}
		GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
		if (has_vn) {
			GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
		}
		
		// Reset draw color (IMPORTANT)
		GL11.glColor4f(1, 1, 1, 1);
	}
}
