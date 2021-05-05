package cam72cam.immersiverailroading.multiblock;

import cam72cam.mod.math.Rotation;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;

public class SmallRailRollerMultiblock extends RailRollerMultiblock {
	public static final String NAME = "SMALL_RAIL_MACHINE";
	private static final Vec3i render = new Vec3i(0,0,0);
	private static final Vec3i crafter = new Vec3i(0,1,5);
	private static final Vec3i input = new Vec3i(0,0,0);
	private static final Vec3i output = new Vec3i(0,0,10);
	private static final Vec3i power = new Vec3i(0,2,5);

	private static FuzzyProvider[][][] componentGenerator() {
		FuzzyProvider[][][] result = new FuzzyProvider[11][][];

		for (int i = 0; i < 11; i ++) {
			if (i >= 4 && i <= 6) {
				result[i] = new FuzzyProvider[][] { {
					L_ENG()
				}, {
					H_ENG()
				}, {
					L_ENG()
				}};
			} else {
				result[i] = new FuzzyProvider[][] { {S_SCAF()} };
			}
		}

		return result;
	}

	public SmallRailRollerMultiblock() {
		super(NAME, componentGenerator());
	}

	@Override
	protected MultiblockInstance newInstance(World world, Vec3i origin, Rotation rot) {
		return new SmallRailRollerInstance(world, origin, rot);
	}
	public class SmallRailRollerInstance extends RailRollerInstance {
		
		public SmallRailRollerInstance(World world, Vec3i origin, Rotation rot) {
			super(world, origin, rot);
		}
	}
}
