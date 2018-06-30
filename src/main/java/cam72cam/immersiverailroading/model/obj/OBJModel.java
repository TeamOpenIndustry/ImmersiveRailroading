package cam72cam.immersiverailroading.model.obj;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.util.RelativeResource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

public class OBJModel {
	public List<String> materialPaths = new ArrayList<String>();
	// LinkedHashMap is ordered
	public Map<String, List<Face>> groups = new LinkedHashMap<String, List<Face>>();
	public double[] vertices;
	public double[] vertexNormals;
	public float[] vertexTextures;
	public int[] faces;

	public Map<String, Material> materials = new HashMap<String, Material>();
	//public Map<Integer, String> mtlLookup = new HashMap<Integer, String>();
	public float darken;
	
	public OBJModel(ResourceLocation modelLoc, float darken) throws Exception {
		this(modelLoc, darken, 1);
	}

	public OBJModel(ResourceLocation modelLoc, float darken, double scale) throws Exception {
		InputStream input = ImmersiveRailroading.proxy.getResourceStream(modelLoc);
		Scanner reader = new Scanner(input);
		this.darken = darken;

		String currentGroupName = "defaultName";
		List<Face> currentGroup = new ArrayList<Face>();
		groups.put(currentGroupName, currentGroup);
		List<String> materialPaths = new ArrayList<String>();
		String currentMaterial = null;
		
		List<Integer> faces = new ArrayList<Integer>();
		List<Double> vertices = new ArrayList<Double>();
		List<Double> vertexNormals = new ArrayList<Double>();
		List<Float> vertexTextures = new ArrayList<Float>();

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
				break;
			case "o":
			case "g":
				groups.put(currentGroupName, currentGroup);
				currentGroupName = args[0];
				currentGroup = new ArrayList<Face>();
				break;
			case "v":
				vertices.add(Double.parseDouble(args[0]) * scale);
				vertices.add(Double.parseDouble(args[1]) * scale);
				vertices.add(Double.parseDouble(args[2]) * scale);
				break;
			case "vn":
				vertexNormals.add(Double.parseDouble(args[0]));
				vertexNormals.add(Double.parseDouble(args[1]));
				vertexNormals.add(Double.parseDouble(args[2]));
				break;
			case "vt":
				vertexTextures.add(Float.parseFloat(args[0]));
				vertexTextures.add(Float.parseFloat(args[1]));
				break;
			case "f":
				currentGroup.addAll(Face.parse(this, args, currentMaterial, faces));
				break;
			case "s":
				//Ignore
				break;
			case "l":
				// Ignore
				// TODO might be able to use this for details
				break;
			default:
				ImmersiveRailroading.debug("OBJ: ignored line '" + line + "'");
				break;
			}
		}
		groups.put(currentGroupName, currentGroup);
		
		reader.close(); // closes input
		
		this.vertices = ArrayUtils.toPrimitive(vertices.toArray(new Double[0]));
		this.vertexNormals = ArrayUtils.toPrimitive(vertexNormals.toArray(new Double[0]));
		this.vertexTextures = ArrayUtils.toPrimitive(vertexTextures.toArray(new Float[0]));
		this.faces = ArrayUtils.toPrimitive(faces.toArray(new Integer[0]));

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
					currentMTL.Ka = ByteBuffer.allocateDirect(4*4).asFloatBuffer();
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
					currentMTL.Kd = ByteBuffer.allocateDirect(4*4).asFloatBuffer();
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
					currentMTL.Ks = ByteBuffer.allocateDirect(4*4).asFloatBuffer();
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
					ImmersiveRailroading.debug("MTL: ignored line '" + line + "'");
					break;
				}
			}

			if (currentMTL != null) {
				materials.put(currentMTL.name, currentMTL);
			}
			reader.close(); // closes input
		}
	}
	
	public Set<String> groups() {
		return groups.keySet();
	}
	
	private Map<Iterable<String>, Vec3d> mins = new HashMap<Iterable<String>, Vec3d>();
	public Vec3d minOfGroup(Iterable<String> groupNames) {
		if (!mins.containsKey(groupNames)) {
			Vec3d min = null;
			for (String group : groupNames) {
				List<Face> faces = groups.get(group);
				for (Face face : faces) {
					for (int[] point : face.points()) {
						Vec3d v = vertices(point[0]);
						if (min == null) {
							min = new Vec3d(v.x, v.y, v.z);
						} else {
							if (min.x > v.x) {
								min = new Vec3d(v.x, min.y, min.z);
							}
							if (min.y > v.y) {
								min = new Vec3d(min.x, v.y, min.z);
							}
							if (min.z > v.z) {
								min = new Vec3d(min.x, min.y, v.z);
							}
						}
					}
				}
			}
			if (min == null) {
				ImmersiveRailroading.error("EMPTY " + groupNames);
				min = new Vec3d(0, 0, 0);
			}
			mins.put(groupNames, min);
		}
		return mins.get(groupNames);
	}
	private Map<Iterable<String>, Vec3d> maxs = new HashMap<Iterable<String>, Vec3d>();
	public Vec3d maxOfGroup(Iterable<String> groupNames) {
		if (!maxs.containsKey(groupNames)) {
			Vec3d max = null;
			for (String group : groupNames) {
				List<Face> faces = groups.get(group);
				for (Face face : faces) {
					for (int[] point : face.points()) {
						Vec3d v = vertices(point[0]);
						if (max == null) {
							max = new Vec3d(v.x, v.y, v.z);
						} else {
							if (max.x < v.x) {
								max = new Vec3d(v.x, max.y, max.z);
							}
							if (max.y < v.y) {
								max = new Vec3d(max.x, v.y, max.z);
							}
							if (max.z < v.z) {
								max = new Vec3d(max.x, max.y, v.z);
							}
						}
					}
				}
			}
			if (max == null) {
				ImmersiveRailroading.error("EMPTY " + groupNames);
				max = new Vec3d(0, 0, 0);
			}
			maxs.put(groupNames, max);
		}
		return maxs.get(groupNames);
	}

	public Vec3d centerOfGroups(Iterable<String> groupNames) {
		Vec3d min = minOfGroup(groupNames);
		Vec3d max = maxOfGroup(groupNames);
		return new Vec3d((min.x + max.x)/2, (min.y + max.y)/2, (min.z + max.z)/2);
	}
	public double heightOfGroups(Iterable<String> groupNames) {
		Vec3d min = minOfGroup(groupNames);
		Vec3d max = maxOfGroup(groupNames);
		return max.y - min.y;
	}
	public double lengthOfGroups(Iterable<String> groupNames) {
		Vec3d min = minOfGroup(groupNames);
		Vec3d max = maxOfGroup(groupNames);
		return max.x - min.x;
	}

	public double widthOfGroups(Iterable<String> groupNames) {
		Vec3d min = minOfGroup(groupNames);
		Vec3d max = maxOfGroup(groupNames);
		return max.z - min.z;
	}
	
	public Vec3d vertices(int i) {
		return new Vec3d(vertices[i*3+0], vertices[i*3+1], vertices[i*3+2]);
	}
	public Vec3d vertexNormals(int i) {
		return new Vec3d(vertexNormals[i*3+0], vertexNormals[i*3+1], vertexNormals[i*3+2]);
	}
	public Vec2f vertexTextures(int i) {
		return new Vec2f(vertexTextures[i*2+0], vertexTextures[i*2+1]);
	}
}
