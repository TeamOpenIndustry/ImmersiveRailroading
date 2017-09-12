package cam72cam.immersiverailroading.render.obj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoader.White;

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
		Map<Integer, Map<Material, List<Face>>> stuff = new HashMap<Integer, Map<Material, List<Face>>>();
		
		for (String group : groupNames) {
			Material currentMTL = model.materials.get(model.groupMtlMap.get(group));
			List<Face> faces = model.groups.get(group);
			
			for (Face face : faces) {
				Integer type = -1;
				switch (face.points.length) {
				case 3:
					type = GL11.GL_TRIANGLES;
					break;
				case 4:
					type = GL11.GL_QUADS;
					break;
				default:
					type = GL11.GL_POLYGON;
					break;
				}
				
				if (!stuff.containsKey(type)) {
					stuff.put(type, new HashMap<Material, List<Face>>());
				}
				
				if (!stuff.get(type).containsKey(currentMTL)) {
					stuff.get(type).put(currentMTL, new ArrayList<Face>());
				}
				
				stuff.get(type).get(currentMTL).add(face);
			}
		}
		
		for (Integer type : stuff.keySet()) {
			GL11.glBegin(type);
			boolean first = false;
			Map<Material, List<Face>> stuffType = stuff.get(type);
			for (Material currentMTL : stuffType.keySet()) {
				List<Face> faces = stuffType.get(currentMTL);
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
				for (Face face : faces) {
					if (type == GL11.GL_POLYGON && !first) {
						GL11.glEnd();
						GL11.glBegin(type);
					}
					first = false;
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
							GL11.glVertex3d(v.x*scale, v.y*scale, v.z*scale);
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
}
