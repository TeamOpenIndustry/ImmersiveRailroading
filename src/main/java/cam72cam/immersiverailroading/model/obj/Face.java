package cam72cam.immersiverailroading.model.obj;

import java.util.ArrayList;
import java.util.List;

public class Face {
	public int[][] points; 
	public String mtl;
	public Vec2f offsetUV = new Vec2f(0, 0);
	public Float depthCache;

	public Face(String mtl, String...args) {
		this.mtl = mtl;
		points = new int[args.length][];
		for(int i = 0; i < args.length; i++) {
			points[i] = parsePoint(args[i]);
		}
	}
	
	private static int[] parsePoint(String point) {
		String[] sp = point.split("/");
		int[] ret = new int[sp.length];
		for (int i = 0; i < sp.length; i++) {
			if (!sp[i].equals("")) {
				ret[i] = Integer.parseInt(sp[i])-1;
			} else {
				ret[i] = -1;
			}
		}
		return ret;
	}

	public static List<Face> parse(String[] args, String currentMaterial) {
		List<Face> res = new ArrayList<Face>();
		if (args.length == 3) {
			res.add(new Face(currentMaterial, args[0], args[1], args[2]));
		} else if (args.length == 4) {
			res.add(new Face(currentMaterial, args[0], args[1], args[2]));
			res.add(new Face(currentMaterial, args[2], args[3], args[0]));
		} else {
			for (int i = 2; i < args.length; i++) {
				res.add(new Face(currentMaterial, args[0], args[i-1], args[i]));
			}
		}
		return res;
	}

}
