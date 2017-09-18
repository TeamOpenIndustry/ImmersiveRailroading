package cam72cam.immersiverailroading.model.obj;

public class Face {
	public int[][] points; 
	public String mtl;

	public Face(String[] args, String mtl) {
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

}
