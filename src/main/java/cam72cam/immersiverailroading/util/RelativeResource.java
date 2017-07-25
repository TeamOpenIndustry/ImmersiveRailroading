package cam72cam.immersiverailroading.util;

import org.apache.commons.io.FilenameUtils;

import net.minecraft.util.ResourceLocation;

public class RelativeResource {
	private RelativeResource() {
		
	}
	public static ResourceLocation getRelative(ResourceLocation originalLoc, String path) {
		return new ResourceLocation(originalLoc.getResourceDomain(), FilenameUtils.concat(FilenameUtils.getPath(originalLoc.getResourcePath()), path).replace('\\', '/'));
	}
}