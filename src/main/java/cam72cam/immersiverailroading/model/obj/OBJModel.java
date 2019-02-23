package cam72cam.immersiverailroading.model.obj;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import cam72cam.immersiverailroading.util.VecUtil;
import org.apache.commons.lang3.ArrayUtils;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.util.RelativeResource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

public class OBJModel {
	// LinkedHashMap is ordered
	public Map<String, int[]> groups = new LinkedHashMap<String, int[]>();
	public float[] vertices;
	public float[] vertexNormals;
	public float[] vertexTextures;
	public int[] faceVerts;
	public String[] faceMTLs;
	public byte[] offsetU;
	public byte[] offsetV;

	public Map<String, Material> materials = new HashMap<String, Material>();
	//public Map<Integer, String> mtlLookup = new HashMap<Integer, String>();
	public float darken;

	private Map<String, Vec3d> mins = new HashMap<>();
	private Map<String, Vec3d> maxs = new HashMap<>();

	public OBJModel(ResourceLocation modelLoc, float darken) throws Exception {
		this(modelLoc, darken, 1);
	}

	public OBJModel(ResourceLocation modelLoc, float darken, double scale) throws Exception {
		InputStream input = ImmersiveRailroading.proxy.getResourceStream(modelLoc);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		this.darken = darken;

		String currentGroupName = "defaultName";
		List<Integer> currentGroup = new ArrayList<Integer>();
		List<String> materialPaths = new ArrayList<String>();
		String currentMaterial = null;
		
		List<Integer> faceVerts = new ArrayList<Integer>();
		List<String> faceMTLs = new ArrayList<String>();
		List<Float> vertices = new ArrayList<Float>();
		List<Float> vertexNormals = new ArrayList<Float>();
		List<Float> vertexTextures = new ArrayList<Float>();
		
		Consumer<String[]> addFace = (String[] args) -> {
			for(int i = 0; i < args.length; i++) {
				for(int j : parsePoint(args[i])) {
					faceVerts.add(j);
				}
			}
		}; 

		String line;
		while ((line = reader.readLine()) != null) {
			
			if (line.startsWith("#")) {
				continue;
			}
			if (line.length() == 0) {
				continue;
			}
			String[] args = line.split(" ");
			String cmd = args[0];
			switch (cmd) {
			case "mtllib":
				materialPaths.add(args[1]);
				break;
			case "usemtl":
				currentMaterial = args[1].intern();
				break;
			case "o":
			case "g":
				if(currentGroup.size() > 0) {
					groups.put(currentGroupName, ArrayUtils.toPrimitive(currentGroup.toArray(new Integer[0])));
				}
				currentGroupName = args[1].intern();
				currentGroup = new ArrayList<Integer>();
				break;
			case "v":
				vertices.add(Float.parseFloat(args[1]) * (float)scale);
				vertices.add(Float.parseFloat(args[2]) * (float)scale);
				vertices.add(Float.parseFloat(args[3]) * (float)scale);
				break;
			case "vn":
				vertexNormals.add(Float.parseFloat(args[1]));
				vertexNormals.add(Float.parseFloat(args[2]));
				vertexNormals.add(Float.parseFloat(args[3]));
				break;
			case "vt":
				vertexTextures.add(Float.parseFloat(args[1]));
				vertexTextures.add(Float.parseFloat(args[2]));
				break;
			case "f":
				int idx;
				if (args.length == 4) {
					addFace.accept(new String[] {args[1], args[2], args[3]});
					idx = faceMTLs.size();
					faceMTLs.add(currentMaterial);
					currentGroup.add(idx);
				} else if (args.length == 5) {
					addFace.accept(new String[] {args[1], args[2], args[3]});
					idx = faceMTLs.size();
					faceMTLs.add(currentMaterial);
					currentGroup.add(idx);
					
					addFace.accept(new String[] {args[3], args[4], args[1]});
					idx = faceMTLs.size();
					faceMTLs.add(currentMaterial);
					currentGroup.add(idx);
				} else {
					for (int i = 2; i < args.length-1; i++) {
						addFace.accept(new String[] {args[1], args[i], args[i+1]});
						idx = faceMTLs.size();
						faceMTLs.add(currentMaterial);
						currentGroup.add(idx);
					}
				}
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
		if(currentGroup.size() > 0) {
			groups.put(currentGroupName, ArrayUtils.toPrimitive(currentGroup.toArray(new Integer[0])));
		}
		
		reader.close(); // closes input
		
		this.vertices = ArrayUtils.toPrimitive(vertices.toArray(new Float[0]));
		this.vertexNormals = ArrayUtils.toPrimitive(vertexNormals.toArray(new Float[0]));
		this.vertexTextures = ArrayUtils.toPrimitive(vertexTextures.toArray(new Float[0]));
		this.faceVerts = ArrayUtils.toPrimitive(faceVerts.toArray(new Integer[0]));
		this.faceMTLs = faceMTLs.toArray(new String[0]);

		if (materialPaths.size() == 0) {
			return;
		}

		for (String materialPath : materialPaths) {

			Material currentMTL = null;

			input = ImmersiveRailroading.proxy.getResourceStream(RelativeResource.getRelative(modelLoc, materialPath));
			reader = new BufferedReader(new InputStreamReader(input));
			while ((line = reader.readLine()) != null) {
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

		for (String group : groups()) {
			Vec3d min = null;
			int[] faces = groups.get(group);
			for (int face : faces) {
				for (int[] point : points(face)) {
					Vec3d v = vertices(point[0]);
					if (min == null) {
						min = v;
					} else {
						min = VecUtil.min(min, v);
					}
				}
			}
			mins.put(group, min);
		}
		for (String group : groups()) {
			Vec3d max = null;
			int[] faces = groups.get(group);
			for (int face : faces) {
				for (int[] point : points(face)) {
					Vec3d v = vertices(point[0]);
					if (max == null) {
						max = v;
					} else {
						max = VecUtil.max(max, v);
					}
				}
			}
			maxs.put(group, max);
		}
	}
	
	private static int[] parsePoint(String point) {
		String[] sp = point.split("/");
		int[] ret = new int[] {-1, -1, -1};
		for (int i = 0; i < sp.length; i++) {
			if (!sp[i].equals("")) {
				ret[i] = Integer.parseInt(sp[i])-1;
			}
		}
		return ret;
	}
	
	public Set<String> groups() {
		return groups.keySet();
	}
	
	public Vec3d minOfGroup(Iterable<String> groupNames) {
		Vec3d min = null;
		for (String group : groupNames) {
			Vec3d gmin = mins.get(group);
			if (min == null) {
				min = gmin;
			} else {
				min = VecUtil.min(min, gmin);
			}
		}
		return min;
	}
	public Vec3d maxOfGroup(Iterable<String> groupNames) {
		Vec3d max = null;
		for (String group : groupNames) {
			Vec3d gmax = maxs.get(group);
			if (max == null) {
				max = gmax;
			} else {
				max = VecUtil.max(max, gmax);
			}
		}
		return max;
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
	
	public int[][] points(int pointStart) {
		int[][] points = new int[3][];
		for (int i = 0; i < 3; i ++) {
			points[i] = new int[] {
				faceVerts[pointStart*9 + i*3 + 0],
				faceVerts[pointStart*9 + i*3 + 1],
				faceVerts[pointStart*9 + i*3 + 2],
			};
		}
		return points;
	}
}
