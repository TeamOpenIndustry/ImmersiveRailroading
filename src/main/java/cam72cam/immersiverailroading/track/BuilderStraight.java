package cam72cam.immersiverailroading.track;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.util.VecUtil;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class BuilderStraight extends BuilderBase {
	private int length;
	private float angle;
	private int mainX;
	private int mainZ;

	public BuilderStraight(World world, int x, int y, int z, EnumFacing rotation, int length, int quarter, float horizOff) {
		super(world, x, y, z, rotation);
		
		this.length = length;
		
		HashSet<Pair<Integer, Integer>> positions = new HashSet<Pair<Integer, Integer>>();
		HashSet<Pair<Integer, Integer>> flexPositions = new HashSet<Pair<Integer, Integer>>();
		
		angle = quarter/4f * 90;
		
		for (float dist = 0; dist < length; dist += 0.25) {
			Vec3d pos = VecUtil.fromYaw(dist, angle);
			for (double q = -1.5; q <= 1.5; q+=0.1) {
				Vec3d nextUp = VecUtil.fromYaw(q, 90);
				int posX = (int)(pos.x+nextUp.x);
				int posZ = (int)(pos.z+nextUp.z);
				positions.add(Pair.of(posX, posZ));
				if (dist < 3 || dist > length - 3) {
					flexPositions.add(Pair.of(posX, posZ));
				}
			}
			if (Math.ceil(dist) == Math.ceil(length/2)) {
				mainX = (int) pos.x;
				mainZ = (int) pos.z;
			}
		}
		
		TrackRail main = new TrackRail(this, mainX, 0, mainZ, EnumFacing.NORTH, TrackItems.STRAIGHT, length, quarter, horizOff);
		tracks.add(main);
		
		for (Pair<Integer, Integer> pair : positions) {
			if (pair.getLeft() == mainX && pair.getRight() == mainZ) {
				// Skip parent block
				continue;
			}
			TrackBase tg = new TrackGag(this, pair.getLeft(), 0, pair.getRight());
			if (flexPositions.contains(pair)) {
				tg.setFlexible();
			}
			tracks.add(tg);
		}
	}
	
	@Override
	public BlockPos getRenderOffset() {
		return convertRelativePositions(mainX, 0, mainZ, EnumFacing.NORTH);
	}

	@Override
	public List<VecYawPitch> getRenderData() {
		List<VecYawPitch> data = new ArrayList<VecYawPitch>();
		
		data.add(new VecYawPitch(-0.5, 0, 0, -angle, 0, length, "RAIL_RIGHT", "RAIL_LEFT"));
		
		for (int i = 0; i < length; i++) {
			Vec3d pos = VecUtil.rotateYaw(new Vec3d(-0.5, 0, i), angle-90);
			data.add(new VecYawPitch(pos.x, pos.y, pos.z, -angle, "RAIL_BASE"));
		}
		return data;
	}
}