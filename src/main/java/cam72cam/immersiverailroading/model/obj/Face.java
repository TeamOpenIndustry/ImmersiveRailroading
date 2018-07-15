package cam72cam.immersiverailroading.model.obj;

import java.util.ArrayList;
import java.util.List;

public class Face {
	public String mtl;
	public Vec2f offsetUV = new Vec2f(0, 0);
	public Float depthCache;
	private int pointStart;
	private OBJModel model;

	public Face(OBJModel model, String mtl, List<Integer> faces, String...args) {
		this.mtl = mtl;
		this.model = model;
		this.pointStart = faces.size();
		for(int i = 0; i < args.length; i++) {
			//points[i] = parsePoint(args[i]);
			for(int j : parsePoint(args[i])) {
				faces.add(j);
			}
		}
	}

	public int[][] points() {
		int[][] points = new int[3][];
		for (int i = 0; i < 3; i ++) {
			points[i] = new int[] {
				model.faces[pointStart + i*3 + 0],
				model.faces[pointStart + i*3 + 1],
				model.faces[pointStart + i*3 + 2],
			};
		}
		return points;
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

	public static List<Face> parse(OBJModel model, String[] args, String currentMaterial, List<Integer> faces) {
		List<Face> res = new ArrayList<Face>();
		if (args.length == 3) {
			res.add(new Face(model, currentMaterial, faces, args[0], args[1], args[2]));
		} else if (args.length == 4) {
			res.add(new Face(model, currentMaterial, faces, args[0], args[1], args[2]));
			res.add(new Face(model, currentMaterial, faces, args[2], args[3], args[0]));
		} else {
			for (int i = 2; i < args.length; i++) {
				res.add(new Face(model, currentMaterial, faces, args[0], args[i-1], args[i]));
			}
		}
		return res;
	}

}
