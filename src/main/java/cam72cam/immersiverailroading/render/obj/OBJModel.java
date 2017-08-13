package cam72cam.immersiverailroading.render.obj;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.util.RelativeResource;
import net.minecraft.util.ResourceLocation;

public class OBJModel {
	List<String> materialPaths = new ArrayList<String>();
	// LinkedHashMap is ordered
	Map<String, List<Face>> groups = new LinkedHashMap<String, List<Face>>();
	List<Vector3f> vertices = new ArrayList<Vector3f>();
	List<Vector3f> vertexNormals = new ArrayList<Vector3f>();
	List<Vector2f> vertexTextures = new ArrayList<Vector2f>();

	Map<String, String> groupMtlMap = new HashMap<String, String>();
	Map<String, Material> materials = new HashMap<String, Material>();

	public OBJModel(ResourceLocation modelLoc) throws Exception {
		InputStream input = ImmersiveRailroading.proxy.getResourceStream(modelLoc);
		Scanner reader = new Scanner(input);

		String currentGroupName = "defaultName";
		List<Face> currentGroup = new ArrayList<Face>();
		groups.put(currentGroupName, currentGroup);
		List<String> materialPaths = new ArrayList<String>();
		String currentMaterial = null;

		while (reader.hasNextLine()) {
			String line = reader.nextLine();
			if (line.startsWith("#")) {
				continue;
			}
			if (line.length() == 0) {
				continue;
			}
			String[] parts = line.split(" ");
			String cmd = parts[0];
			String[] args = Arrays.copyOfRange(parts, 1, parts.length);
			switch (cmd) {
			case "mtllib":
				materialPaths.add(args[0]);
				break;
			case "usemtl":
				currentMaterial = args[0];
				groupMtlMap.put(currentGroupName, currentMaterial);
				break;
			case "o":
			case "g":
				currentGroupName = args[0];
				currentGroup = new ArrayList<Face>();
				groups.put(currentGroupName, currentGroup);
				groupMtlMap.put(currentGroupName, currentMaterial);
				break;
			case "v":
				vertices.add(new Vector3f(Float.parseFloat(args[0]), Float.parseFloat(args[1]), Float.parseFloat(args[2])));
				break;
			case "vn":
				vertexNormals.add(new Vector3f(Float.parseFloat(args[0]), Float.parseFloat(args[1]), Float.parseFloat(args[2])));
				break;
			case "vt":
				vertexTextures.add(new Vector2f(Float.parseFloat(args[0]), Float.parseFloat(args[1])));
				break;
			case "f":
				currentGroup.add(new Face(args));
				break;
			case "s":
				//Ignore
				break;
			case "l":
				// Ignore
				// TODO might be able to use this for details
				break;
			default:
				System.out.println("OBJ: ignored line '" + line + "'");
				break;
			}
		}
		reader.close();

		if (materialPaths.size() == 0) {
			return;
		}

		for (String materialPath : materialPaths) {

			Material currentMTL = null;

			input = ImmersiveRailroading.proxy.getResourceStream(RelativeResource.getRelative(modelLoc, materialPath));
			reader = new Scanner(input);
			while (reader.hasNextLine()) {
				String line = reader.nextLine();
				if (line.startsWith("#")) {
					continue;
				}
				if (line.length() == 0) {
					continue;
				}
				String[] parts = line.split(" ");
				switch (parts[0]) {
				case "newmtl":
					if (currentMTL != null) {
						materials.put(currentMTL.name, currentMTL);
					}
					currentMTL = new Material();
					currentMTL.name = parts[1];
					break;
				case "Ka":
					currentMTL.Ka = BufferUtils.createFloatBuffer(4);
					currentMTL.Ka.put(Float.parseFloat(parts[1]));
					currentMTL.Ka.put(Float.parseFloat(parts[2]));
					currentMTL.Ka.put(Float.parseFloat(parts[3]));
					if (parts.length > 4) {
						currentMTL.Ka.put(Float.parseFloat(parts[4]));
					} else {
						currentMTL.Ka.put(1.0f);
					}
					currentMTL.Ka.position(0);
					break;
				case "Kd":
					currentMTL.Kd = BufferUtils.createFloatBuffer(4);
					currentMTL.Kd.put(Float.parseFloat(parts[1]));
					currentMTL.Kd.put(Float.parseFloat(parts[2]));
					currentMTL.Kd.put(Float.parseFloat(parts[3]));
					if (parts.length > 4) {
						currentMTL.Kd.put(Float.parseFloat(parts[4]));
					} else {
						currentMTL.Kd.put(1.0f);
					}
					currentMTL.Kd.position(0);
					break;
				case "Ks":
					currentMTL.Ks = BufferUtils.createFloatBuffer(4);
					currentMTL.Ks.put(Float.parseFloat(parts[1]));
					currentMTL.Ks.put(Float.parseFloat(parts[2]));
					currentMTL.Ks.put(Float.parseFloat(parts[3]));
					if (parts.length > 4) {
						currentMTL.Ks.put(Float.parseFloat(parts[4]));
					} else {
						currentMTL.Ks.put(1.0f);
					}
					currentMTL.Ks.position(0);
					break;
				case "map_Kd":
					currentMTL.texKd = RelativeResource.getRelative(modelLoc, parts[1]);
					break;
				case "Ns":
					//Ignore
					break;
				case "Ke":
					//Ignore
					break;
				case "Ni":
					//Ignore
					break;
				case "d":
					//ignore
					break;
				case "illum":
					//ignore
					break;
				default:
					System.out.println("MTL: ignored line '" + line + "'");
					break;
				}
			}

			if (currentMTL != null) {
				materials.put(currentMTL.name, currentMTL);
			}
		}
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
		drawDirectGroups(groups.keySet());
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
		for (String group : groupNames) {
			Material currentMTL = materials.get(groupMtlMap.get(group));
			List<Face> faces = groups.get(group);

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
					GL11.glEnd();
					GL11.glBegin(GL11.GL_TRIANGLES);
					break;
				case 4:
					GL11.glEnd();
					GL11.glBegin(GL11.GL_QUADS);
					break;
				default:
					GL11.glEnd();
					GL11.glBegin(GL11.GL_POLYGON);
					break;
				}

				for (int[] point : face.points) {
					Vector3f v;
					Vector2f vt;
					Vector3f vn;

					switch (point.length) {
					case 3:
						vn = vertexNormals.get(point[2]);
						GL11.glNormal3f(vn.x, vn.y, vn.z);
					case 2:
						if (point[1] != -1) {
							vt = vertexTextures.get(point[1]);
							GL11.glTexCoord2f(vt.x, 1 - vt.y);
						}
					case 1:
						v = vertices.get(point[0]);
						GL11.glVertex3f(v.x, v.y, v.z);
						break;
					default:
						System.out.println("WATWATWAT");
					}
				}
			}
		}
		GL11.glEnd();
	}
	
	public Set<String> groups() {
		return groups.keySet();
	}
	
	public Vector3f minOfGroup(Iterable<String> groupNames) {
		Vector3f min = null;
		for (String group : groupNames) {
			List<Face> faces = groups.get(group);
			for (Face face : faces) {
				for (int[] point : face.points) {
					Vector3f v = vertices.get(point[0]);
					if (min == null) {
						min = new Vector3f(v.x, v.y, v.z);
					} else {
						if (min.x > v.x) {
							min.x = v.x;
						}
						if (min.y > v.y) {
							min.y = v.y;
						}
						if (min.z > v.z) {
							min.z = v.z;
						}
					}
				}
			}
		}
		if (min == null) {
			System.out.println("EMPTY " + groupNames);
			return new Vector3f(0, 0, 0);
		}
		return min;
	}
	public Vector3f maxOfGroup(Iterable<String> groupNames) {
		Vector3f max = null;
		for (String group : groupNames) {
			List<Face> faces = groups.get(group);
			for (Face face : faces) {
				for (int[] point : face.points) {
					Vector3f v = vertices.get(point[0]);
					if (max == null) {
						max = new Vector3f(v.x, v.y, v.z);
					} else {
						if (max.x < v.x) {
							max.x = v.x;
						}
						if (max.y < v.y) {
							max.y = v.y;
						}
						if (max.z < v.z) {
							max.z = v.z;
						}
					}
				}
			}
		}
		if (max == null) {
			System.out.println("EMPTY " + groupNames);
			return new Vector3f(0, 0, 0);
		}
		return max;
	}

	public Vector3f centerOfGroups(Iterable<String> groupNames) {
		Vector3f min = minOfGroup(groupNames);
		Vector3f max = maxOfGroup(groupNames);
		return new Vector3f((min.x + max.x)/2, (min.y + max.y)/2, (min.z + max.z)/2);
	}
	public float heightOfGroups(Iterable<String> groupNames) {
		Vector3f min = minOfGroup(groupNames);
		Vector3f max = maxOfGroup(groupNames);
		return max.y - min.y;
	}
	public float lengthOfGroups(Iterable<String> groupNames) {
		Vector3f min = minOfGroup(groupNames);
		Vector3f max = maxOfGroup(groupNames);
		return max.x - min.x;
	}

	public double widthOfGroups(Set<String> groupNames) {
		Vector3f min = minOfGroup(groupNames);
		Vector3f max = maxOfGroup(groupNames);
		return max.z - min.z;
	}
}
