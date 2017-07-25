package cam72cam.immersiverailroading.render.obj;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import cam72cam.immersiverailroading.util.RelativeResource;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

public class OBJModel {
	List<String> materialPaths = new ArrayList<String>();
	//LinkedHashMap is ordered
	Map<String, List<Face>> groups = new LinkedHashMap<String, List<Face>>();
	List<Vector3f> vertices = new ArrayList<Vector3f>();
	List<Vector3f> vertexNormals = new ArrayList<Vector3f>();
	List<Vector2f> vertexTextures = new ArrayList<Vector2f>();
	public ResourceLocation texLoc;
	
	public OBJModel(ResourceLocation modelLoc) throws Exception {
		InputStream input = Minecraft.getMinecraft().getResourceManager().getResource(modelLoc).getInputStream();
		Scanner reader = new Scanner(input);
		
		ArrayList<Face> currentGroup = null;
		String materialPath = null;
		String material = null;
		
		while(reader.hasNextLine()) {
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
			switch(cmd) {
			case "mtllib":
				materialPath = args[0];
				break;
			case "usemtl":
				material = args[0];
				break;
			case "g":
				String groupName = args[0];
				currentGroup = new ArrayList<Face>();
				groups.put(groupName, currentGroup);
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
			default:
				System.out.println("OBJ: ignored line '" + line + "'");
				break;
			}
		}
		reader.close();
		
		if (materialPath == null) {
			return;
		}

		input = Minecraft.getMinecraft().getResourceManager().getResource(RelativeResource.getRelative(modelLoc, materialPath)).getInputStream();
		reader = new Scanner(input);
		while(reader.hasNextLine()) {
			String line = reader.nextLine();
			if (line.startsWith("#")) {
				continue;
			}
			if (line.length() == 0) {
				continue;
			}
			String[] parts = line.split(" ");
			switch(parts[0]) {
			case "map_Kd":
				texLoc = RelativeResource.getRelative(modelLoc, parts[1]);
				System.out.println(texLoc);
				break;
			default:
				System.out.println("MTL: ignored line '" + line + "'");
				break;
			}
		}
	}
	
	public void draw() {
		GL11.glBegin(GL11.GL_QUADS);
		{
			for (List<Face> faces : groups.values()) {
				for (Face face : faces) {
					
					for(int[] point : face.points) {
						Vector3f v;
						Vector2f vt;
						Vector3f vn;
						
						switch(point.length) {
						case 3:;
							vn = vertexNormals.get(point[2]);
							GL11.glNormal3f(vn.x, vn.y, vn.z);
						case 2:
							vt = vertexTextures.get(point[1]);
							GL11.glTexCoord2f(vt.x, 1-vt.y);
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
		}
		GL11.glEnd();
	}
}
