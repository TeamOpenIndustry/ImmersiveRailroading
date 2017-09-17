package cam72cam.immersiverailroading.render.obj;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

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
		FloatBuffer quadBuffer = BufferUtils.createFloatBuffer(quadFaceCount * 4 * 3);
		FloatBuffer quadNormalBuffer = BufferUtils.createFloatBuffer(quadFaceCount * 4 * 3);
		FloatBuffer quadColorBuffer = BufferUtils.createFloatBuffer(quadFaceCount * 4 * 4);

		for (Integer type : stuff.keySet()) {
			for (Face face : stuff.get(type)) {
				Material currentMTL = model.materials.get(face.mtl);
				if (currentMTL.Ka != null) {
					GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT, currentMTL.Ka);
				}
				float r = 0;
				float g = 0;
				float b = 0;
				float a = 0;
				if (currentMTL.Kd != null) {
					GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_DIFFUSE, currentMTL.Kd);
					r = Math.max(0, currentMTL.Kd.get(0) - model.darken);
					g = Math.max(0, currentMTL.Kd.get(1) - model.darken);
					b = Math.max(0, currentMTL.Kd.get(2) - model.darken);
					a = currentMTL.Kd.get(3);
					GL11.glColor4f(r, g, b, currentMTL.Kd.get(3));
				}
				if (currentMTL.Ks != null) {
					GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_SPECULAR, currentMTL.Ks);
				}
				if (type == GL11.GL_POLYGON) {
					GL11.glBegin(type);
					//GL11.glColor4f(r, g, b, currentMTL.Kd.get(3));
					for (int[] point : face.points) {
						Vec3d v;
						//Vec2f vt;
						//Vec3d vn;

						switch (point.length) {
						case 3:
							//vn = model.vertexNormals.get(point[2]);
							//GL11.glNormal3d(vn.x, vn.y, vn.z);
							//System.out.println(String.format("%s %s %s", vn.x, vn.y, vn.z));
						case 2:
							if (point[1] != -1) {
								//vt = model.vertexTextures.get(point[1]);
								//GL11.glTexCoord2f(vt.x, 1 - vt.y);
							}
						case 1:
							v = model.vertices.get(point[0]);
							GL11.glVertex3d(v.x * scale, v.y * scale, v.z * scale);
							break;
						default:
							break;
						}
					}
					GL11.glEnd();
				} else {
					for (int[] point : face.points) {
						if (type == GL11.GL_QUADS) {
							Vec3d v = model.vertices.get(point[0]);
							Vec3d vn = model.vertexNormals.get(point[2]);
							quadBuffer.put((float) (v.x * scale));
							quadBuffer.put((float) (v.y * scale));
							quadBuffer.put((float) (v.z * scale));
							quadNormalBuffer.put((float) (vn.x));
							quadNormalBuffer.put((float) (vn.y));
							quadNormalBuffer.put((float) (vn.z));
							quadColorBuffer.put(r);
							quadColorBuffer.put(g);
							quadColorBuffer.put(b);
							quadColorBuffer.put(a);
						} else {
							Vec3d v = model.vertices.get(point[0]);
							Vec3d vn = model.vertexNormals.get(point[2]);
							triBuffer.put((float) (v.x * scale));
							triBuffer.put((float) (v.y * scale));
							triBuffer.put((float) (v.z * scale));
							triNormalBuffer.put((float) (vn.x));
							triNormalBuffer.put((float) (vn.y));
							triNormalBuffer.put((float) (vn.z));
							triColorBuffer.put(r);
							triColorBuffer.put(g);
							triColorBuffer.put(b);
							triColorBuffer.put(a);
						}
					}
				}
			}
		}

		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
		GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
		GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);

		quadBuffer.flip();
		quadColorBuffer.flip();
		quadNormalBuffer.flip();
		GL11.glColorPointer(4, 4 << 2, quadColorBuffer);
		GL11.glNormalPointer(3 << 2, quadNormalBuffer);
		GL11.glVertexPointer(3, 3 << 2, quadBuffer);
		GL11.glDrawArrays(GL11.GL_QUADS, 0, quadFaceCount * 4);

		triBuffer.flip();
		triColorBuffer.flip();
		triNormalBuffer.flip();
		GL11.glColorPointer(4, 4 << 2, triColorBuffer);
		GL11.glNormalPointer(3 << 2, triNormalBuffer);
		GL11.glVertexPointer(3, 3 << 2, triBuffer);
		GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, triFaceCount * 3);

		GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
		GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
		GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
		
		// Reset draw color (IMPORTANT)
		GL11.glColor4f(1, 1, 1, 1);
	}
}
