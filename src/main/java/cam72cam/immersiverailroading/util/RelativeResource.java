package cam72cam.immersiverailroading.util;

import cam72cam.mod.util.Identifier;
import org.apache.commons.io.FilenameUtils;

public class RelativeResource {
	private RelativeResource() {
		
	}
	public static Identifier getRelative(Identifier originalLoc, String path) {
		return new Identifier(originalLoc.getDomain(), FilenameUtils.concat(FilenameUtils.getPath(originalLoc.getPath()), path).replace('\\', '/'));
	}
}