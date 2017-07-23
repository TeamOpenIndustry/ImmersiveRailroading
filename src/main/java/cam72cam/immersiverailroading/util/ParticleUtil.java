package cam72cam.immersiverailroading.util;

import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class ParticleUtil {
	private ParticleUtil() {
		// Disable construction since java does not have static classes
	}
	
	public static void spawnParticle(World world, EnumParticleTypes type, Vec3d position) {
		world.spawnParticle(type, position.x, position.y, position.z, 0, 0, 0);
	}
}
