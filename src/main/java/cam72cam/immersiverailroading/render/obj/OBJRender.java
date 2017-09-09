package cam72cam.immersiverailroading.render.obj;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
		GL11.glBegin(GL11.GL_QUADS);
		int last = 4;
		for (String group : groupNames) {
			Material currentMTL = model.materials.get(model.groupMtlMap.get(group));
			List<Face> faces = model.groups.get(group);

			if (currentMTL == null) {
				//ImmersiveRailroading.logger.warn(String.format("Missing mtl %s %s", group, groupMtlMap.get(group)));
			} else {
				if (currentMTL.Ka != null) {
					GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT, currentMTL.Ka);
				}
				if (currentMTL.Kd != null) {
					GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_DIFFUSE, currentMTL.Kd);
					float darken = 0.06f;
					float r = Math.max(0, currentMTL.Kd.get(0)-darken);
					float g = Math.max(0, currentMTL.Kd.get(1)-darken);
					float b = Math.max(0, currentMTL.Kd.get(2)-darken);
					GL11.glColor4f(r, g, b, currentMTL.Kd.get(3));
				}
				if (currentMTL.Ks != null) {
					GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_SPECULAR, currentMTL.Ks);
				}
			}

			for (Face face : faces) {
				switch (face.points.length) {
				case 3:
					if (face.points.length != last) {
						GL11.glEnd();
						GL11.glBegin(GL11.GL_TRIANGLES);
					}
					break;
				case 4:
					if (face.points.length != last) {
						GL11.glEnd();
						GL11.glBegin(GL11.GL_QUADS);
					}
					break;
				default:
					GL11.glEnd();
					GL11.glBegin(GL11.GL_POLYGON);
					break;
				}
				last = face.points.length; 

				for (int[] point : face.points) {
					Vec3d v;
					Vec2f vt;
					Vec3d vn;

					switch (point.length) {
					case 3:
						vn = model.vertexNormals.get(point[2]);
						GL11.glNormal3d(vn.x, vn.y, vn.z);
					case 2:
						if (point[1] != -1) {
							vt = model.vertexTextures.get(point[1]);
							GL11.glTexCoord2f(vt.x, 1 - vt.y);
						}
					case 1:
						v = model.vertices.get(point[0]);
						GL11.glVertex3d(v.x, v.y, v.z);
						break;
					default:
						System.out.println("WATWATWAT");
					}
				}
			}
		}
		GL11.glEnd();
	}
}
