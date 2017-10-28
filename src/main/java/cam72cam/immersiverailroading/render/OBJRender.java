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
		Map<Integer, List<Face>> stuff = new HashMap<Integer, List<Face>>();

		int triFaceCount = 0;
		int quadFaceCount = 0;
		boolean has_vt = true;
		boolean has_vn = true;

		for (String group : groupNames) {
			List<Face> faces = model.groups.get(group);

			for (Face face : faces) {
				Integer type = -1;
				switch (face.points.length) {
				case 3:
					type = GL11.GL_TRIANGLES;
					triFaceCount++;
					break;
				case 4:
					type = GL11.GL_QUADS;
					quadFaceCount++;
					break;
				default:
					type = GL11.GL_POLYGON;
					break;
				}

				if (!stuff.containsKey(type)) {
					stuff.put(type, new ArrayList<Face>());
				}

				stuff.get(type).add(face);
			}
		}

		FloatBuffer triBuffer = BufferUtils.createFloatBuffer(triFaceCount * 3 * 3);
		FloatBuffer triNormalBuffer = BufferUtils.createFloatBuffer(triFaceCount * 3 * 3);
		FloatBuffer triColorBuffer = BufferUtils.createFloatBuffer(triFaceCount * 3 * 4);
		FloatBuffer triTexBuffer = BufferUtils.createFloatBuffer(triFaceCount * 3 * 2);
		FloatBuffer quadBuffer = BufferUtils.createFloatBuffer(quadFaceCount * 4 * 3);
		FloatBuffer quadNormalBuffer = BufferUtils.createFloatBuffer(quadFaceCount * 4 * 3);
		FloatBuffer quadColorBuffer = BufferUtils.createFloatBuffer(quadFaceCount * 4 * 4);
		FloatBuffer quadTexBuffer = BufferUtils.createFloatBuffer(quadFaceCount * 4 * 2);

		for (Integer type : stuff.keySet()) {
			for (Face face : stuff.get(type)) {
				Material currentMTL = model.materials.get(face.mtl);
				float r = 0;
				float g = 0;
				float b = 0;
				float a = 0;
				if (currentMTL.Ka != null) {
					//GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT, currentMTL.Ka);
				}
				if (currentMTL.Kd != null) {
					//GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_DIFFUSE, currentMTL.Kd);
					r = Math.max(0, currentMTL.Kd.get(0) - model.darken);
					g = Math.max(0, currentMTL.Kd.get(1) - model.darken);
					b = Math.max(0, currentMTL.Kd.get(2) - model.darken);
					a = currentMTL.Kd.get(3);
					//GL11.glColor4f(r, g, b, currentMTL.Kd.get(3));
				}
				if (currentMTL.Ks != null) {
					//GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_SPECULAR, currentMTL.Ks);
				}
				if (type == GL11.GL_POLYGON) {
					GL11.glColor4f(r, g, b, a);
					GL11.glBegin(type);
					for (int[] point : face.points) {
						Vec3d v = model.vertices.get(point[0]);
						Vec2f vt = point[1] != -1 ? model.vertexTextures.get(point[1]) : null;
						Vec3d vn = point[2] != -1 ? model.vertexNormals.get(point[2]) : null;

						if (vt != null) {
							//GL11.glTexCoord2f(vt.x, 1 - vt.y);
						}
						if (vn != null) {
							//GL11.glNormal3f((float)vn.x, (float)vn.y, (float)vn.z);
						}
						GL11.glVertex3f((float)(v.x * scale), (float)(v.y * scale), (float)(v.z * scale));
					}
					GL11.glEnd();
				} else {
					for (int[] point : face.points) {
						Vec3d v = model.vertices.get(point[0]);
						Vec2f vt = point[1] != -1 ? model.vertexTextures.get(point[1]) : null;
						Vec3d vn = point[2] != -1 ? model.vertexNormals.get(point[2]) : null;
						
						FloatBuffer vb = type == GL11.GL_QUADS ? quadBuffer : triBuffer;
						FloatBuffer vtb = type == GL11.GL_QUADS ? quadTexBuffer : triTexBuffer;
						FloatBuffer vnb = type == GL11.GL_QUADS ? quadNormalBuffer : triNormalBuffer;
						FloatBuffer vcb = type == GL11.GL_QUADS ? quadColorBuffer : triColorBuffer;
						
						vb.put((float) (v.x * scale));
						vb.put((float) (v.y * scale));
						vb.put((float) (v.z * scale));
						if (vn != null) {
							vnb.put((float) (vn.x));
							vnb.put((float) (vn.y));
							vnb.put((float) (vn.z));
						} else {
							has_vn = false;
						}
						if (vt != null) {
							vtb.put(vt.x);
							vtb.put(-vt.y);
						} else {
							has_vt = false;
						}
						vcb.put(r);
						vcb.put(g);
						vcb.put(b);
						vcb.put(a);
					}
				}
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

		quadBuffer.flip();
		quadColorBuffer.flip();
		quadNormalBuffer.flip();
		quadTexBuffer.flip();
		if (has_vt) {
			GL11.glTexCoordPointer(2, 2 << 2, quadTexBuffer);
		}
		GL11.glColorPointer(4, 4 << 2, quadColorBuffer);
		if (has_vn) {
			GL11.glNormalPointer(3 << 2, quadNormalBuffer);
		}
		GL11.glVertexPointer(3, 3 << 2, quadBuffer);
		GL11.glDrawArrays(GL11.GL_QUADS, 0, quadFaceCount * 4);

		triBuffer.flip();
		triColorBuffer.flip();
		triNormalBuffer.flip();
		triTexBuffer.flip();
		if (has_vt) {
			GL11.glTexCoordPointer(2, 2 << 2, triTexBuffer);
		}
		GL11.glColorPointer(4, 4 << 2, triColorBuffer);
		if (has_vn) {
			GL11.glNormalPointer(3 << 2, triNormalBuffer);
		}
		GL11.glVertexPointer(3, 3 << 2, triBuffer);
		GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, triFaceCount * 3);

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
