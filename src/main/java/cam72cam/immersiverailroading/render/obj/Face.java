package cam72cam.immersiverailroading.render.obj;

public class Face {
	public int[][] points; 

	public Face(String[] args) {
		points = new int[args.length][];
		for(int i = 0; i < args.length; i++) {
			points[i] = parsePoint(args[i]);
		}
	}
	
	private static int[] parsePoint(String point) {
		String[] sp = point.split("/");
		int[] ret = new int[sp.length];
		for (int i = 0; i < sp.length; i++) {
			ret[i] = Integer.parseInt(sp[i])-1;
		}
		return ret;
	}

}
